# MyTP —— 自定义限距 TP 指令（Fabric MOD）

一个同时支持**服务端**与**客户端**的 Fabric 模组，提供类似原生 `/tp` 的自定义指令
`/mytp <x> <y> <z>`，并强制限制传送距离：**玩家只能传送到以自身当前位置为球心、
半径 1000 格（方块）的范围内**。超出 1000 格将拒绝传送，并向玩家返回：

> 您当前 TP 已超出最大值，请在 1000 格内进行 TP

**当前版本目标：Minecraft 26.2（Fabric，Java 25）**

---

## 1. 功能特性

- 指令：`/mytp <x> <y> <z>`
  - 绝对坐标：`/mytp 100 64 -200`
  - 相对坐标：`/mytp ~100 ~5 ~-200`（以自己当前位置为基准，与原生 `/tp` 一致）
- 所有玩家均可使用（无需 OP 权限），只能传送**自己**，不能传送他人
- 距离校验完全在**服务端**执行，客户端无法绕过
- 传送后保持原有朝向，不跨维度
- 上限距离为代码常量 `MAX_TP_DISTANCE`，改一处即可调整

## 2. 工程结构

```
mytp/
├── build.gradle                 # Gradle 构建脚本（Fabric Loom 1.17.20 + 恒等映射）
├── settings.gradle
├── gradle.properties            # ★ 版本号 & 本机 JVM 修复都在这里 ★
├── gradlew / gradlew.bat        # Gradle Wrapper（指向腾讯镜像，本机可达）
├── identity-26.2.jar            # ★ 26.2 恒等映射（Mojang 官方命名，见 §3）
├── build.bat                    # 一键构建（已内置本机三个 JVM 修复）
├── LICENSE
├── README.md
└── src/main/
    ├── java/com/mytp/
    │   ├── MyTpMod.java         # 模组主入口（main entrypoint，双端加载）
    │   └── TpCommand.java       # 指令注册 + 距离校验逻辑（Mojang 官方命名写法）
    └── resources/
        └── fabric.mod.json      # 模组元数据（双端 environment:"*"）
```

## 3. 关于 26.2 的映射（重要）

Minecraft **从 1.21.11 起不再混淆**，26.2 的官方 jar 直接以 Mojang 官方命名发布
（`net/minecraft/commands/CommandSourceStack` 等），**不再提供独立的映射文件**
（版本清单里没有 `client_mappings` / `server_mappings`，也没有 yarn 映射）。

因此：
- **不能用** `mappings loom.officialMojangMappings()`（Loom 会去下载不存在的映射而报错）；
- 工程用 `mappings files("identity-26.2.jar")` —— 一份**恒等映射**（官方命名→官方命名，
  由 26.2 游戏 jar 的全部 7445 个类生成），配合 `loom { noIntermediateMappings() }`，
  编译与打包都按官方命名走，与运行时完全一致；
- `build.gradle` 里已禁用 `remapSourcesJar`（26.2 没有 intermediary 中间命名空间，
  源码 jar 无需也无法重映射）。

> 这份 `identity-26.2.jar` 只对 26.2 有效。做 1.20 ~ 1.21.x 分支时改回 yarn 映射
> （见 §7）。

## 4. 使用的 26.2 官方构建参数

| 项 | 值 |
|---|---|
| Minecraft | 26.2（stable） |
| Java | 25（26.2 的最低 Java 要求） |
| 映射 | Mojang 官方命名（恒等映射，26.2 起无 yarn） |
| Fabric Loader | 0.19.5 |
| Fabric API | 0.159.0+26.2 |
| Fabric Loom | 1.17.20 |
| Gradle | 9.5.0 |

以上版本号来自 Fabric 官方元数据接口（meta.fabricmc.net / maven.fabricmc.net），
均已写入 `gradle.properties` / `build.gradle` / `fabric.mod.json`。

## 5. 编译（打包成 .jar）

> 需要 **JDK 25**。

**最简单方式**：双击运行工程根目录的 `build.bat`。

或手动执行：

```powershell
.\gradlew.bat build
```

> ⚠️ 本机 JDK 25 + Gradle 有三个必须的 JVM 修复，`build.bat` 与 `gradle.properties`
> 已全部配好，**请勿删除**：
> 1. `-Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.WindowsSelectorProvider`
>    （WEPollSelector 在 Windows 上初始化崩溃）；
> 2. `-Djava.net.preferIPv4Stack=true`（本机 IPv6 环回 ::1 不通，否则报
>    "Unable to establish loopback connection"）；
> 3. `--add-modules=jdk.zipfs`（Java 25 默认不含 jar 文件系统，否则报
>    Provider "jar" not found）。
>
> 首次构建会下载 Minecraft 26.2 与依赖，耗时较长属正常。

打包产物在 `build/libs/` 下，只需关注：

```
mytp-1.0.0.jar
```

（带 `-dev` / `-sources` 的是开发用，忽略即可）

## 6. 安装

- **服务端**：把 `mytp-1.0.0.jar` 放入服务端 `mods/` 文件夹，重启服务端。
- **客户端**：把同一个 `mytp-1.0.0.jar` 放入客户端 `.minecraft/mods/` 文件夹。
- Fabric 服务端默认要求客户端与服务器安装相同的模组，因此装到服务端后，
  玩家客户端也**必须**安装本模组才能进入——这正好保证限制无法被绕过。
- 单机 / 局域网联机同样生效（集成服务器也会注册该指令）。
- 注意：本模组依赖 **Fabric API**（`fabric-command-api-v2`），服务端与客户端
  的 `mods/` 里都要有对应 26.2 版本的 Fabric API（如 `fabric-api-0.159.0+26.2.jar`）。

## 7. 使用

```
/mytp 0 64 0          # 传送到绝对坐标 (0, 64, 0)
/mytp ~100 ~5 ~-100   # 向东南方向相对移动 100 格、向上 5 格
```

- 目标在 1000 格内 → 传送成功。
- 目标超出 1000 格 → 传送被拒绝，聊天栏返回：`您当前 TP 已超出最大值，请在 1000 格内进行 TP`

## 8. 修改传送距离上限

打开 `src/main/java/com/mytp/TpCommand.java`，改这一行：

```java
public static final double MAX_TP_DISTANCE = 1000.0;
```

改成你需要的数值（如 2000.0），重新 `.\gradlew.bat build` 打包即可。

## 9. 多版本支持（1.20 ~ 26.2）

本模组**不包含任何 Mixin**，只用到跨版本稳定 API：

- `CommandRegistrationCallback`（fabric-command-api-v2）
- `Vec3Argument` / `Commands.literal` / `Commands.argument`（1.19.4 起稳定，支持 `~` 相对坐标）
- `ServerPlayer` 的 `position()` / `teleportTo()` / `sendSystemMessage()`

**1.20 ~ 1.21.x 用的是 yarn 映射**，类名/方法名与 Mojang 命名写法不同，因此
Java 源码**不能一份通用**，需要按映射切换写法。出其他版本时：

1. 复制工程到新目录；
2. 改 `gradle.properties`：`minecraft_version`、`loader_version`、`fabric_version`；
3. `build.gradle` 里按版本选择映射：
   - **26.2（含 1.21.11+ 同机制版本）**：保留本工程的恒等映射写法
     `mappings files("identity-26.2.jar")` + `noIntermediateMappings()`；
   - **1.20 ~ 1.21.x**：改用
     `mappings "net.fabricmc:yarn:${project.yarn_mappings}:v2"`（需在
     gradle.properties 加回 `yarn_mappings`，去 <https://fabricmc.net/develop/> 查），
     并去掉 `noIntermediateMappings()`、恢复 `remapSourcesJar`；
4. 按对应映射改写 `TpCommand.java` 里的类名/方法名（Mojang↔yarn 对照见下）；
5. `fabric.mod.json` 里改 `"minecraft"` 范围与 `"java"` 要求；
6. 重新 `build`。

**Mojang ↔ yarn 类名对照（本指令用到的）**

| Mojang（26.2 本工程） | yarn（1.20~1.21.x） |
|---|---|
| `net.minecraft.commands.CommandSourceStack` | `net.minecraft.server.command.ServerCommandSource` |
| `net.minecraft.commands.Commands` | `net.minecraft.server.command.CommandManager` |
| `net.minecraft.commands.arguments.coordinates.Vec3Argument` | `net.minecraft.command.argument.Vec3ArgumentType` |
| `net.minecraft.server.level.ServerPlayer` | `net.minecraft.server.network.ServerPlayerEntity` |
| `net.minecraft.network.chat.Component` | `net.minecraft.text.Text` |
| `net.minecraft.world.phys.Vec3` | `net.minecraft.util.math.Vec3d` |
| `source.getPlayerOrException()` | `source.getPlayerOrThrow()` |
| `player.position()` | `player.getPos()` |
| `player.teleportTo(x,y,z)` | `player.teleport(x,y,z)` |
| `player.sendSystemMessage(comp)` | `player.sendMessage(text,false)` |
| `Vec3Argument.getVec3(ctx,"pos")` | `Vec3ArgumentType.getPos(ctx,"pos")` |

> 注：`ServerPlayer` 发送系统消息的方法在 26.2 由 `displayClientMessage(Component,boolean)`
> 改名为 `sendSystemMessage(Component)`，跨版本改源码时注意。

---

*许可证：MIT*
