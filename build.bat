@echo off
REM ============================================================
REM  MyTP 一键构建脚本（Forge 1.20.1 / Windows）
REM ============================================================
setlocal
set "GRADLE_OPTS=-Djava.net.preferIPv4Stack=true"

echo [MyTP] 开始构建 (Forge 1.20.1) ...
call gradlew.bat build %*

echo.
echo [MyTP] 构建完成，产物在 build\libs\ 下
echo        请认准 mytp-1.0.0.jar（-sources 为开发用）
pause
