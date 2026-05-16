@echo off
echo [1/5] Certifique-se de que o Docker (Kafka/Redis/MySQL) ja esta rodando...
pause

echo [2/5] Subindo User Service...
start .\mvnw.cmd -pl services/user-service spring-boot:run
timeout /t 15

echo [3/5] Subindo Event e Ticket Services...
start .\mvnw.cmd -pl services/event-service spring-boot:run
start .\mvnw.cmd -pl services/ticket-service spring-boot:run
timeout /t 15

echo [4/5] Subindo Order e Payment Services...
start .\mvnw.cmd -pl services/order-service spring-boot:run
start .\mvnw.cmd -pl services/payment-service spring-boot:run
timeout /t 15

echo [5/5] Finalizando com o Gateway...
start .\mvnw.cmd -pl services/gateway-service spring-boot:run

echo Ecossistema EventMaster inicializado com sucesso!