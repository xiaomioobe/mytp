package com.mytp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * MyTP 运行时配置，持久化到服务器 config 目录下的 mytp.json（Forge 1.20.1）。
 * <p>
 * 配置项：
 * <ul>
 *   <li>messageEnabled：是否在传送后发送坐标消息（默认 true）</li>
 *   <li>messageVisibility：消息可见范围，"all" 全体可见 / "admin" 仅管理员可见（默认 all）</li>
 * </ul>
 */
public class ModConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path configPath;

    /** 是否显示 TP 坐标消息 */
    public boolean messageEnabled = true;

    /** 消息可见范围："all" 全体可见，"admin" 仅管理员可见 */
    public String messageVisibility = "all";

    /**
     * 从 config/mytp.json 加载配置；文件不存在时写入默认配置。
     */
    public static ModConfig load() {
        configPath = FMLPaths.CONFIGDIR.get().resolve("mytp.json");
        ModConfig config = new ModConfig();
        if (Files.exists(configPath)) {
            try {
                String content = Files.readString(configPath);
                JsonObject obj = JsonParser.parseString(content).getAsJsonObject();
                if (obj.has("messageEnabled")) {
                    config.messageEnabled = obj.get("messageEnabled").getAsBoolean();
                }
                if (obj.has("messageVisibility")) {
                    String v = obj.get("messageVisibility").getAsString();
                    if ("admin".equals(v) || "all".equals(v)) {
                        config.messageVisibility = v;
                    }
                }
            } catch (Exception e) {
                MyTpMod.LOGGER.warn("[MyTP] 配置文件读取失败，使用默认配置", e);
            }
        } else {
            config.save();
        }
        return config;
    }

    /** 将当前配置写入 config/mytp.json */
    public void save() {
        try {
            JsonObject obj = new JsonObject();
            obj.addProperty("messageEnabled", messageEnabled);
            obj.addProperty("messageVisibility", messageVisibility);
            Files.writeString(configPath, GSON.toJson(obj));
        } catch (IOException e) {
            MyTpMod.LOGGER.error("[MyTP] 配置文件写入失败", e);
        }
    }
}
