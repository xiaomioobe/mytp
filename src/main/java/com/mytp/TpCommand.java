package com.mytp;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * 自定义限距 TP 指令：/mytp &lt;x&gt; &lt;y&gt; &lt;z&gt;
 * <p>
 * 规则：玩家只能传送到以自身当前位置为球心、半径 MAX_TP_DISTANCE 的球体内；
 * 目标超出 1000 格时拒绝传送，并向玩家返回中文提示文本。
 * <p>
 * 坐标写法与原生 /tp 一致，支持：
 * <ul>
 *   <li>绝对坐标：/mytp 100 64 -200</li>
 *   <li>相对坐标：/mytp ~100 ~5 ~-200（以自己当前位置为基准）</li>
 * </ul>
 */
public class TpCommand {

    /** 最大传送距离（格 / 方块）。改这里即可调整上限。 */
    public static final double MAX_TP_DISTANCE = 1000.0;

    /** 超出上限时返回给玩家的提示文本 */
    public static final String TOO_FAR_MESSAGE = "您当前 TP 已超出最大值，请在 1000 格内进行 TP";

    public static void register() {
        // 服务端注册指令：CommandRegistrationCallback 在专用服务器与集成服务器(单机/局域网)都会触发，
        // 距离校验全部在服务端执行，客户端无法绕过限制。
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("mytp")
                        .then(Commands.argument("pos", Vec3Argument.vec3())
                                .executes(TpCommand::execute))));
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

        // 解析目标坐标（支持 ~ 相对坐标，返回绝对坐标 Vec3）
        Vec3 target = Vec3Argument.getVec3(context, "pos");
        Vec3 current = player.position();

        // 三维欧几里得距离：球体半径校验
        double distance = current.distanceTo(target);

        if (distance > MAX_TP_DISTANCE) {
            // 超出 1000 格：不传送，返回提示文本（普通聊天消息）
            // 26.2 起 ServerPlayer 用 sendSystemMessage 替代了旧版 displayClientMessage
            player.sendSystemMessage(Component.literal(TOO_FAR_MESSAGE));
            return 0;
        }

        // 在允许范围内：传送到目标坐标（保持原有朝向）
        player.teleportTo(target.x, target.y, target.z);
        return 1;
    }
}
