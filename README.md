# Sequência de Inicialização da Plataforma — EventMaster

Para que o ecossistema distribuído do **EventMaster** suba redondo — sem que nenhum microsserviço derrube a inicialização do Spring Boot por falta de conexão —, é fundamental seguir uma sequência lógica de dependências.

Aqui está o roteiro passo a passo com a ordem recomendada para inicialização da plataforma:

---

### 1ª Fase: Infraestrutura e Pré-requisitos

Antes de iniciar qualquer código Java, os barramentos e bancos de dados externos precisam estar operacionais. Caso contrário, os serviços falharão imediatamente (*Connection Refused*).

* **Mensageria (Apache Kafka):** Deve ser o primeiro componente a subir na porta `9092`, pois quase todos os seus serviços dependem dele para registrar os ouvintes (*Listeners*).
* **Cache em Memória (Redis):** Suba o Redis na porta `6379` para que o `ticket-service` consiga se conectar e mapear os estoques.
* **Banco de Dados (MySQL):** Certifique-se de que a instância do MySQL esteja ativa para rodar as migrações automáticas do Flyway.

> Se você estiver usando o `docker-compose.yml` deste repositório, o MySQL do container fica exposto em `localhost:3307` para evitar conflito com um MySQL local já rodando em `3306`.

### Comando da infraestrutura com Docker Compose

```powershell
docker compose up -d
```

### Convenção recomendada para subir os microsserviços

- Abra **um terminal por serviço**.
- Nos serviços com banco, use `DB_PORT=3307` para apontar para o MySQL do Compose.
- Kafka e Redis continuam acessíveis em `localhost:9092` e `localhost:6379`.

---

### 2ª Fase: O Provedor de Identidade (Core de Segurança)

Com a infraestrutura de pé, iniciamos os serviços Java. O primeiro obrigatoriamente deve ser o dono das credenciais:

* **`user-service` (Porta 8080):**
```powershell
$env:DB_PORT="3307"
.\mvnw.cmd -pl services/user-service spring-boot:run

```


* **Por que agora?** Ele gerencia a segurança e os segredos do JWT. É a base fundacional para qualquer teste ou chamada protegida na plataforma.



---

### 3ª Fase: Catálogo e Inventário (Serviços de Domínio)

Agora subimos os serviços que sustentam as informações de negócio do ecossistema:

* **`event-service` (Porta 8082):**
```powershell
$env:DB_PORT="3307"
$env:KAFKA_BOOTSTRAP_SERVERS="localhost:9092"
.\mvnw.cmd -pl services/event-service spring-boot:run

```


* **`ticket-service` (Porta 8083):**
```powershell
$env:DB_PORT="3307"
$env:KAFKA_BOOTSTRAP_SERVERS="localhost:9092"
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="6379"
.\mvnw.cmd -pl services/ticket-service spring-boot:run

```



> ⚙️ **Dinâmica assíncrona ativa:** Assim que esses dois estiverem de pé, qualquer evento criado no `event-service` disparará o gatilho `EVENTO_CRIADO` via Kafka, e o `ticket-service` fará a carga de 95% automaticamente no Redis.

---

### 4ª Fase: O Coração Transacional e Processamento Assíncrono

Com o catálogo e o inventário prontos, subimos a esteira de compras e a SAGA de pagamentos:

* **`order-service` (Porta 8084):**
```powershell
$env:DB_PORT="3307"
$env:KAFKA_BOOTSTRAP_SERVERS="localhost:9092"
.\mvnw.cmd -pl services/order-service spring-boot:run

```


* *Nota:* Ele precisa que o `event-service` e o `ticket-service` estejam online para realizar as chamadas síncronas de validação via `RestTemplate`.


* **`payment-service` (Porta 8086):**
```powershell
$env:KAFKA_BOOTSTRAP_SERVERS="localhost:9092"
.\mvnw.cmd -pl services/payment-service spring-boot:run

```


* Ele atuará em background escutando o tópico `PEDIDO_REALIZADO`.



---

### 5ª Fase: A Porta de Entrada (Borda da Aplicação)

Por último, subimos o componente reativo que unifica o acesso a todo o sistema:

* **`gateway-service` (Porta 8081):**
```powershell
.\mvnw.cmd -pl services/gateway-service spring-boot:run

```


* **Por que por último?** O gateway funciona como o proxy reverso inteligente e agregador de documentação. Subindo ele no final, quando as APIs downstream já estão saudáveis (*Healthy*), o Swagger integrado (`http://localhost:8081/swagger-ui.html`) carregará todos os links e documentações das rotas automaticamente e sem falhas de timeout.



---

### 💡 Dica de Ouro:

Se você quiser automatizar essa subida para o seu grupo da ADA ou para rodar tudo de forma limpa, pode criar um arquivo de script simples na raiz do monorepo chamado `subir-plataforma.bat` (Windows):

```batch
@echo off
echo [1/5] Certifique-se de que o Docker (Kafka/Redis/MySQL) ja esta rodando...
pause

echo [2/5] Subindo User Service...
start cmd /k "set DB_PORT=3307 && .\mvnw.cmd -pl services/user-service spring-boot:run"
timeout /t 15

echo [3/5] Subindo Event e Ticket Services...
start cmd /k "set DB_PORT=3307 && set KAFKA_BOOTSTRAP_SERVERS=localhost:9092 && .\mvnw.cmd -pl services/event-service spring-boot:run"
start cmd /k "set DB_PORT=3307 && set KAFKA_BOOTSTRAP_SERVERS=localhost:9092 && set REDIS_HOST=localhost && set REDIS_PORT=6379 && .\mvnw.cmd -pl services/ticket-service spring-boot:run"
timeout /t 15

echo [4/5] Subindo Order e Payment Services...
start cmd /k "set DB_PORT=3307 && set KAFKA_BOOTSTRAP_SERVERS=localhost:9092 && .\mvnw.cmd -pl services/order-service spring-boot:run"
start cmd /k "set KAFKA_BOOTSTRAP_SERVERS=localhost:9092 && .\mvnw.cmd -pl services/payment-service spring-boot:run"
timeout /t 15

echo [5/5] Finalizando com o Gateway...
start .\mvnw.cmd -pl services/gateway-service spring-boot:run

echo Ecossistema EventMaster inicializado com sucesso!

```
