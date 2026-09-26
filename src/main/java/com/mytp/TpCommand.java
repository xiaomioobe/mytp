package com.mytp;

import com.mojang.brigadier.arguments.DoubleArgumentType;
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

import java.util.List;

/**
 * MyTP 指令注册与执行。
 * <p>
 * 指令结构：
 * <pre>
 * /mytp &lt;x&gt; &lt;y&gt; &lt;z&gt;              传送到指定坐标（支持 ~ 相对坐标）
 * /mytp config                       查看当前配置（仅 OP）
 * /mytp config limit &lt;true|false&gt;   开关传送范围限制（仅 OP）
 * /mytp config range &lt;数值&gt;          设置最大传送半径（仅 OP）
 * /mytp config message &lt;on|off&gt;      开关 TP 坐标消息（仅 OP）
 * /mytp config message visibility &lt;all|admin&gt;  设置消息可见范围（仅 OP）
 * </pre>
 * <p>
 * 默认行为：不限制传送距离；传送后向公屏广播坐标消息（全体可见）。
 * 所有校验与广播均在服务端执行，客户端无法绕过。
 */
public class TpCommand {

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
                    // 范围限制开关
                    .then(Commands.literal("limit")
                        .then(Commands.literal("true").executes(ctx -> setLimit(ctx, true)))
                        .then(Commands.literal("false").executes(ctx -> setLimit(ctx, false))))
                    // 最大距离
                    .then(Commands.literal("range")
                        .then(Commands.argument("distance", DoubleArgumentType.doubleArg(0.0))
                            .executes(TpCommand::setRange)))
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
     * 执行传送：解析坐标 → 可选范围校验 → 传送 → 可选广播。
     */
    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

        // 解析目标坐标（支持 ~ 相对坐标，返回绝对坐标 Vec3）
        Vec3 target = Vec3Argument.getVec3(context, "pos");
        Vec3 current = player.position();

        // 范围限制：仅当配置启用时才校验
        if (config.limitEnabled) {
            double distance = current.distanceTo(target);
            if (distance > config.maxDistance) {
                player.sendSystemMessage(Component.literal(
                    "您当前 TP 已超出最大值，请在 " + (long) config.maxDistance + " 格内进行 TP"));
                return 0;
            }
        }

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
            "[MyTP] 当前配置：范围限制=%s，最大距离=%.0f格，TP消息=%s，可见范围=%s",
            config.limitEnabled ? "开启" : "关闭",
            config.maxDistance,
            config.messageEnabled ? "显示" : "关闭",
            "admin".equals(config.messageVisibility) ? "管理员可见" : "全体可见");
        context.getSource().sendSuccess(() -> Component.literal(info), false);
        return 1;
    }

    private static int setLimit(CommandContext<CommandSourceStack> context, boolean value) {
        config.limitEnabled = value;
        config.save();
        context.getSource().sendSuccess(() -> Component.literal(
            "[MyTP] 传送范围限制已" + (value ? "开启" : "关闭")), false);
        return 1;
    }

    private static int setRange(CommandContext<CommandSourceStack> context) {
        double distance = DoubleArgumentType.getDouble(context, "distance");
        config.maxDistance = distance;
        config.save();
        context.getSource().sendSuccess(() -> Component.literal(
            "[MyTP] 最大传送距离已设为 " + (long) distance + " 格"), false);
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
