package com.mytp;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * MyTP 指令注册与执行。
 * <p>
 * 指令结构：
 * <pre>
 * /mytp &lt;x&gt; &lt;y&gt; &lt;z&gt;                          传送到指定坐标（支持 ~ 相对坐标）
 * /mytp config                                   查看当前配置（仅 OP）
 * /mytp config message &lt;on|off&gt;                  开关 TP 坐标消息（仅 OP）
 * /mytp config message visibility &lt;all|admin&gt;    设置消息可见范围（仅 OP）
 * </pre>
 * <p>
 * 频率限制：每个玩家 1 分钟内最多 TP 10 次，超出则拒绝并在聊天栏提示。
 * 所有校验与广播均在服务端执行，客户端无法绕过。
 */
public class TpCommand {

    /** 频率限制：时间窗口（毫秒），1 分钟 */
    private static final long TIME_WINDOW_MS = 60_000L;

    /** 频率限制：时间窗口内最大 TP 次数 */
    private static final int MAX_TP_PER_WINDOW = 10;

    /** 每个玩家的 TP 时间戳队列（key: 玩家 UUID） */
    private static final Map<UUID, Deque<Long>> tpTimestamps = new HashMap<>();

    private static ModConfig config;

    /** 由 MyTpMod 在初始化时注入配置实例 */
    public static void setConfig(ModConfig cfg) {
        config = cfg;
    }

    /**
     * 判断命令源是否为管理员（OP）。控制台/命令方块默认放行。
     * 26.2 起用 PlayerList.isOp(NameAndId) 替代旧版 hasPermission(int)。
     */
    private static boolean isOp(CommandSourceStack source) {
        if (!source.isPlayer()) return true;
        ServerPlayer p = source.getPlayer();
        return p != null && source.getServer().getPlayerList()
                .isOp(new NameAndId(p.getGameProfile()));
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(Commands.literal("mytp")
                // 传送：/mytp <x> <y> <z>
                .then(Commands.argument("pos", Vec3Argument.vec3())
                    .executes(TpCommand::execute))
                // 配置：仅 OP（管理员）可用；控制台/命令方块默认放行
                .then(Commands.literal("config")
                    .requires(TpCommand::isOp)
                    .executes(TpCommand::showConfig)
                    // 消息开关 + 可见范围
                    .then(Commands.literal("message")
                        .then(Commands.literal("on").executes(ctx -> setMessage(ctx, true)))
                        .then(Commands.literal("off").executes(ctx -> setMessage(ctx, false)))
                        .then(Commands.literal("visibility")
                            .then(Commands.literal("all").executes(ctx -> setVisibility(ctx, "all")))
                            .then(Commands.literal("admin").executes(ctx -> setVisibility(ctx, "admin"))))))
            ));
    }

    /**
     * 执行传送：频率校验 → 解析坐标 → 记录时间戳 → 传送 → 可选广播。
     */
    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        UUID uuid = player.getUUID();

        // 频率限制检查：1 分钟内最多 10 次
        long now = System.currentTimeMillis();
        Deque<Long> timestamps = tpTimestamps.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        // 清理超过 1 分钟的旧记录
        while (!timestamps.isEmpty() && now - timestamps.peekFirst() > TIME_WINDOW_MS) {
            timestamps.pollFirst();
        }
        if (timestamps.size() >= MAX_TP_PER_WINDOW) {
            player.sendSystemMessage(Component.literal(
                "您 TP 过于频繁，1 分钟内最多 10 次，请稍后再试"));
            return 0;
        }

        // 解析目标坐标（支持 ~ 相对坐标，返回绝对坐标 Vec3）
        Vec3 target = Vec3Argument.getVec3(context, "pos");

        // 记录本次 TP 时间
        timestamps.addLast(now);

        // 执行传送（保持原有朝向）
        player.teleportTo(target.x, target.y, target.z);

        // 广播坐标消息（原版风格："传送 玩家名 到 x, y, z"）
        if (config.messageEnabled) {
            String msg = String.format("传送 %s 到 %.1f, %.1f, %.1f",
                player.getName().getString(), target.x, target.y, target.z);
            broadcast(context, Component.literal(msg));
        }
        return 1;
    }

    /**
     * 根据配置将消息广播给全体玩家，或仅发送给管理员。
     */
    private static void broadcast(CommandContext<CommandSourceStack> context, Component component) {
        var server = context.getSource().getServer();
        if ("admin".equals(config.messageVisibility)) {
            // 仅管理员可见：遍历在线玩家，发给 OP
            List<ServerPlayer> players = server.getPlayerList().getPlayers();
            for (ServerPlayer p : players) {
                if (server.getPlayerList().isOp(new NameAndId(p.getGameProfile()))) {
                    p.sendSystemMessage(component);
                }
            }
            // 同时输出到服务器控制台
            server.sendSystemMessage(component);
        } else {
            // 全体可见：公屏广播（false = 聊天框，非 action bar）
            server.getPlayerList().broadcastSystemMessage(component, false);
        }
    }

    // ==================== 配置命令实现 ====================

    private static int showConfig(CommandContext<CommandSourceStack> context) {
        String info = String.format(
            "[MyTP] 当前配置：TP消息=%s，可见范围=%s，频率限制=1分钟10次",
            config.messageEnabled ? "显示" : "关闭",
            "admin".equals(config.messageVisibility) ? "管理员可见" : "全体可见");
        context.getSource().sendSuccess(() -> Component.literal(info), false);
        return 1;
    }

    private static int setMessage(CommandContext<CommandSourceStack> context, boolean value) {
        config.messageEnabled = value;
        config.save();
        context.getSource().sendSuccess(() -> Component.literal(
            "[MyTP] TP 坐标消息已" + (value ? "开启" : "关闭")), false);
        return 1;
    }

    private static int setVisibility(CommandContext<CommandSourceStack> context, String value) {
        config.messageVisibility = value;
        config.save();
        context.getSource().sendSuccess(() -> Component.literal(
            "[MyTP] 消息可见范围已设为" + ("admin".equals(value) ? "管理员可见" : "全体可见")), false);
        return 1;
    }
}
