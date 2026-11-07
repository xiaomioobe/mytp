# MyTP —— 自定义 TP 指令（Fabric MOD）

一个同时支持**服务端**与**客户端**的 Fabric 模组，提供类似原生 `/tp` 的 `/mytp <x> <y> <z>` 指令。

- **当前版本**：v1.3.0
- **目标版本**：Minecraft 26.3（Fabric，Java 25）
- **历史版本**：v1.2.0 / 26.2、v1.1.0 / 26.2、v1.0.0 / 26.2
- **Forge 分支**：`forge-1.20.1` 分支，Forge 1.20.1-47.3.0

## 1. 功能

- 指令 `/mytp <x> <y> <z>`，支持绝对坐标与 `~` 相对坐标
- **不限距离**：v1.2.0 起已移除 1000 格限制
- **频率限制**：每个玩家 1 分钟内最多 10 次，超出则拒绝并在聊天栏提示
  `您 TP 过于频繁，1 分钟内最多 10 次，请稍后再试`
- **公屏广播**：每次成功传送后，向全服广播 `传送 玩家名 到 x.x, y.y, z.z`
- **消息可配置**：可关闭广播，或改为仅管理员可见
- 所有校验与广播都在服务端执行，客户端无法绕过
- 传送后保持原有朝向

## 2. 命令

| 命令 | 权限 | 说明 |
|---|---|---|
| `/mytp <x> <y> <z>` | 所有玩家 | 传送到指定坐标，支持 `~` 相对坐标 |
| `/mytp config` | OP | 查看当前配置 |
| `/mytp config message on` | OP | 开启坐标消息 |
| `/mytp config message off` | OP | 关闭坐标消息 |
| `/mytp config message visibility all` | OP | 消息全体可见（公屏） |
| `/mytp config message visibility admin` | OP | 消息仅管理员可见 |

示例：

```
/mytp 100 64 -200
/mytp ~100 ~5 ~-200
```

## 3. 配置文件

首次启动会生成 `config/mytp.json`：

```json
{
  "messageEnabled": true,
  "messageVisibility": "all"
}
```

- `messageEnabled`：是否发送坐标消息
- `messageVisibility`：`all` 全体可见 / `admin` 仅管理员可见

用 `/mytp config` 系列命令修改后会自动写回该文件。

## 4. 工程结构

```
mytp/
├── build.gradle                 # Fabric Loom 1.18.4
├── gradle.properties            # 版本号与 JVM 参数
├── gradlew / gradlew.bat        # Gradle Wrapper（9.8.0，腾讯镜像）
├── build.bat                    # Windows 一键构建
├── LICENSE
├── README.md
└── src/main/
    ├── java/com/mytp/
    │   ├── MyTpMod.java         # 模组主入口
    │   ├── TpCommand.java       # 指令、频率限制、广播
    │   └── ModConfig.java       # 配置读写
    └── resources/
        └── fabric.mod.json      # 双端 environment:"*"
```

## 5. 构建参数（26.3）

| 项 | 值 | 来源 |
|---|---|---|
| Minecraft | 26.3（stable） | `meta.fabricmc.net/v2/versions/game` |
| Fabric Loader | 0.20.1 | `meta.fabricmc.net/v2/versions/loader/26.3` |
| Fabric API | 0.165.2+26.3 | `maven.fabricmc.net` |
| Fabric Loom | 1.18.4 | `maven.fabricmc.net` |
| Gradle | 9.8.0 | 官方发行版 |
| Java | 25 | 26.x 要求 |

Minecraft 26.1 起游戏 jar 直接以 Mojang 官方命名发布，没有混淆，也没有
`client_mappings` / `server_mappings`，因此 `build.gradle` 里**不声明 `mappings` 依赖**，
也不需要 `noIntermediateMappings()` 和 `remapSourcesJar`。

## 6. 编译

需要 JDK 25。

**Windows**：双击 `build.bat`，或执行

```powershell
.\gradlew.bat build
```

**Linux / macOS**：

```bash
JAVA_HOME=/path/to/jdk-25 ./gradlew build
```

首次构建会下载 Minecraft 26.3 与依赖，耗时几分钟属正常。

产物在 `build/libs/`：

```
mytp-1.3.0.jar           # 装这个
mytp-1.3.0-sources.jar   # 开发用，不用装
```

## 7. 安装

- 服务端：`mytp-1.3.0.jar` 放进服务端 `mods/`
- 客户端：同一个 jar 放进客户端 `.minecraft/mods/`
- 需要 **Fabric API**（`fabric-command-api-v2`），服务端与客户端都要装对应 26.3 版本
- 需要 Fabric Loader ≥ 0.20.1、Java 25

## 8. 其他版本

- **Forge 1.20.1**：见 `forge-1.20.1` 分支，功能与本版一致
- **1.20 ~ 1.21.x（Fabric）**：这些版本仍使用 yarn 映射，类名与方法名不同，源码需要
  按映射改写（对照表见 git 历史中的旧 README）

---

*许可证：MIT*
