package com.mytp;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MyTP 模组主入口。
 * <p>
 * 本模组 environment 为 "*"（通用），服务端与客户端都能安装：
 * <ul>
 *   <li>服务端：指令 /mytp 在这里被注册，距离上限在服务端强制执行，客户端无法绕过；</li>
 *   <li>客户端：正常加载，联机时由服务端完成校验；单机/局域网下同样可用（集成服务器也会注册该指令）。</li>
 * </ul>
 * <p>
 * 注：本工程使用 Mojang 官方映射（Mojang Mappings，26.2 起 Fabric 不再提供 yarn 映射）。
 */
public class MyTpMod implements ModInitializer {

    public static final String MOD_ID = "mytp";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        TpCommand.register();
        LOGGER.info("[MyTP] 已加载：/mytp <x> <y> <z>，最大传送距离 {} 格", (long) TpCommand.MAX_TP_DISTANCE);
    }
}
