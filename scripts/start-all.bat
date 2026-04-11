@echo off
chcp 65001 > nul
echo ========================================
echo 启动所有后端服务...
echo ========================================

cd /d %~dp0

echo 启动 Nacos (Docker)...
docker start nacos 2>nul || docker run --name nacos -p 8848:8848 -p 9848:9848 -p 9849:9849 -e MODE=standalone -d nacos/nacos-server:v2.2.3 > nul

echo 启动 Gateway (8080)...
start "Gateway" cmd /c "cd /d %~dp0gateway && java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -jar target\gateway-1.0.0-SNAPSHOT.jar --server.port=8080"

echo 启动 User Service (8081)...
start "UserService" cmd /c "cd /d %~dp0user-service && java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5006 -jar target\user-service-1.0.0-SNAPSHOT.jar --server.port=8081"

echo 启动 Post Service (8082)...
start "PostService" cmd /c "cd /d %~dp0post-service && java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5007 -jar target\post-service-1.0.0-SNAPSHOT.jar --server.port=8082"

echo 启动 Interaction Service (8083)...
start "InteractionService" cmd /c "cd /d %~dp0interaction-service && java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5008 -jar target\interaction-service-1.0.0-SNAPSHOT.jar --server.port=8083"

echo 启动 Relation Service (8084)...
start "RelationService" cmd /c "cd /d %~dp0relation-service && java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5009 -jar target\relation-service-1.0.0-SNAPSHOT.jar --server.port=8084"

echo 启动 Notification Service (8085)...
start "NotificationService" cmd /c "cd /d %~dp0notification-service && java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5010 -jar target\notification-service-1.0.0-SNAPSHOT.jar --server.port=8085"

echo 启动 File Service (8086)...
start "FileService" cmd /c "cd /d %~dp0file-service && java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5011 -jar target\file-service-1.0.0-SNAPSHOT.jar --server.port=8086"

echo.
echo ========================================
echo 等待服务启动...
echo ========================================
timeout /t 25 /nobreak > nul

echo.
echo ========================================
echo 服务状态:
echo ========================================
netstat -ano | findstr ":8080 :8081 :8082 :8083 :8084 :8085 :8086 " | findstr LISTENING

echo.
echo 启动前端 (npm run dev)...
cd /d %~dp0frontend
start "Frontend" cmd /c "npm run dev"

echo.
echo ========================================
echo 启动完成!
echo ========================================
echo 后端服务: http://localhost:8080
echo 前端服务: http://localhost:3000
echo Nacos:    http://localhost:8848/nacos
pause
