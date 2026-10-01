package com.mytp;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MyTP 模组主入口（Forge 1.20.1）。
 * <p>
 * 使用 @Mod 注解标记，构造函数中注册指令注册事件。
 * 服务端与客户端通用：指令在服务端注册并强制执行。
 */
@Mod(MyTpMod.MOD_ID)
public class MyTpMod {

    public static final String MOD_ID = "mytp";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public MyTpMod() {
        ModConfig config = ModConfig.load();
        TpCommand.setConfig(config);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        LOGGER.info("[MyTP] 已加载：/mytp <x> <y> <z> | 频率限制=1分钟10次，TP消息={}，可见范围={}",
            config.messageEnabled ? "显示" : "关闭",
            "admin".equals(config.messageVisibility) ? "管理员可见" : "全体可见");
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        TpCommand.register(event.getDispatcher());
    }
}
