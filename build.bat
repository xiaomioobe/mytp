@echo off
REM ============================================================
REM  MyTP 一键构建脚本（Windows）
REM
REM  本机 JDK 25 + Gradle 需要三个 JVM 修复（均已在下面 GRADLE_OPTS
REM  和 gradle.properties 的 org.gradle.jvmargs 中配置）：
REM   1) WEPollSelector 在 Windows 初始化失败
REM      -> -Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.WindowsSelectorProvider
REM   2) 本机 IPv6 环回(::1) 不通，Gradle 连 daemon 报
REM      "Unable to establish loopback connection"
REM      -> -Djava.net.preferIPv4Stack=true
REM   3) Java 25 默认不含 jdk.zipfs，Loom 打开 minecraft jar 报
REM      Provider "jar" not found
REM      -> --add-modules=jdk.zipfs
REM ============================================================
setlocal
set "GRADLE_OPTS=-Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.WindowsSelectorProvider -Djava.net.preferIPv4Stack=true --add-modules=jdk.zipfs"

echo [MyTP] 开始构建 (Gradle 9.5.0 / MC 26.2) ...
call gradlew.bat build %*

echo.
echo [MyTP] 构建完成，产物在 build\libs\ 下
echo        请认准 mytp-1.0.0.jar（-dev / -sources 为开发用）
pause
