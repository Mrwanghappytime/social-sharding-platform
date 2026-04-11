@echo off
chcp 65001 > nul
echo ========================================
echo 停止所有后端服务...
echo ========================================

echo 停止 Java 进程...
taskkill /F /FI "IMAGENAME eq java.exe" > nul 2>&1

echo.
echo ========================================
echo 服务已停止
echo ========================================
pause
