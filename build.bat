@echo off
REM ============================================================
REM  MyTP 一键构建脚本（Windows / Minecraft 26.3）
REM
REM  需要 JDK 25。Java 25 默认模块集不含 jdk.zipfs，Loom 打开
REM  minecraft jar 时会报 Provider "jar" not found，因此追加
REM  --add-modules=jdk.zipfs。
REM ============================================================
setlocal
set "GRADLE_OPTS=--add-modules=jdk.zipfs"

echo [MyTP] 开始构建 (Gradle 9.8.0 / MC 26.3) ...
call gradlew.bat build %*

echo.
echo [MyTP] 构建完成，产物在 build\libs\ 下
echo        请认准 mytp-1.3.0.jar（-sources 为开发用）
pause
