# eventmaster-mod1-ada

Projeto Final - Módulo 01 - Arquitetura

Monorepo Maven para os servicos do projeto EventMaster.

## Estrutura

```text
eventmaster-mod1-ada/
  pom.xml                 # agregador (packaging pom)
  services/
    user-service/         # modulo Maven do servico de usuarios
    event-service/        # modulo Maven do servico de eventos
    ticket-service/       # modulo Maven do servico de ingressos
    order-service/        # modulo Maven do servico de pedidos
    payment-service/      # modulo Maven do servico interno de pagamentos
    gateway-service/      # API Gateway (Spring Cloud Gateway)
```

## Pre-requisitos

- Java 21
- Maven Wrapper (ja incluso no repositorio: `mvnw` / `mvnw.cmd`)
- Apache Kafka disponivel em `localhost:9092`
- Redis disponivel em `localhost:6379`

## Comandos principais

Execute os comandos a partir da raiz do repositorio (`eventmaster-mod1-ada`).

### 1) Build e testes de todos os modulos

```powershell
.\mvnw.cmd clean test
```

### 2) Testar apenas o `user-service`

```powershell
.\mvnw.cmd -pl services/user-service test
```

### 2.1) Testar apenas o `payment-service`

```powershell
.\mvnw.cmd -pl services/payment-service test
```

### 3) Rodar apenas o `user-service`

```powershell
.\mvnw.cmd -pl services/user-service spring-boot:run
```

## Documentacao da API (Swagger)

Com o `user-service` em execucao, acesse:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Com o `event-service` em execucao, acesse:

- Swagger UI: `http://localhost:8082/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8082/v3/api-docs`

Com o `ticket-service` em execucao, acesse:

- Swagger UI: `http://localhost:8083/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8083/v3/api-docs`

Com o `order-service` em execucao, acesse:

- Swagger UI: `http://localhost:8084/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8084/v3/api-docs`

Com o `gateway-service` em execucao:

- Swagger UI propria: `http://localhost:8081/swagger-ui.html`
- OpenAPI JSON proprio: `http://localhost:8081/v3/api-docs`
- a Swagger UI do gateway tambem agrega links para as documentacoes downstream publicadas em `/api/users/**`, `/api/events/**`, `/api/tickets/**` e `/api/orders/**`

Observacao: o `payment-service` nao expoe API HTTP publica e, por isso, nao possui rota no `gateway-service`. Ele atua apenas como consumidor/publicador interno via Kafka.

### Autenticacao no Swagger

- A API usa JWT Bearer.
- Clique em `Authorize` no Swagger UI e informe o token JWT.
- O esquema configurado e `bearerAuth`.

Fluxo recomendado:

1. Execute `POST /auth/login` e copie o campo `jwt` da resposta.
2. Clique em `Authorize` no Swagger UI e cole o token.
3. Chame os endpoints protegidos de `user-service/usuarios`.
4. Para encerrar sessao, execute `POST /auth/logout` com o mesmo token.

### Exemplos rapidos (payload)

`POST /user-service/usuarios/registrar-usuario`

```json
{
  "login": "novo.usuario@eventmaster.com",
  "senha": "Senha@123",
  "role": "USER",
  "nome": "Novo Usuario",
  "cpf": "12345678900"
}
```

`POST /user-service/usuarios/excluir-usuario`

```json
{
  "nome": "Usuario Alvo",
  "role": "USER",
  "login": "usuario.alvo@eventmaster.com"
}
```

### Padrao de erro

As respostas de erro dos endpoints documentados seguem o formato:

```json
{
  "error": "BAD_REQUEST",
  "message": "Descricao da falha"
}
```

## Testes via Postman

Os arquivos de apoio para testes manuais do `user-service` estao em `docs/postman/`:

- Collection: `docs/postman/eventmaster-user-service-local.postman_collection.json`
- Environment: `docs/postman/eventmaster-user-service-local.postman_environment.json`
- Guia rapido: `docs/postman/README.md`

Os arquivos de apoio para testes manuais do `event-service` tambem estao em `docs/postman/`:

- Collection: `docs/postman/eventmaster-event-service-local.postman_collection.json`
- Environment: `docs/postman/eventmaster-event-service-local.postman_environment.json`
- Collection via gateway: `docs/postman/eventmaster-event-service-via-gateway.postman_collection.json`
- Environment via gateway: `docs/postman/eventmaster-event-service-via-gateway.postman_environment.json`
- Guia rapido: `docs/postman/README.md`

Observacao: a collection de eventos foi atualizada para incluir o campo monetario `precoBase` nos payloads e nas validacoes do fluxo CRUD.
Observacao: tambem foi adicionada uma collection equivalente via `gateway-service`, usando `POST /api/auth/login` e os endpoints protegidos em `/api/events/**`.

Os arquivos de apoio para testes manuais do `ticket-service` via gateway tambem estao em `docs/postman/`:

- Collection: `docs/postman/eventmaster-ticket-service-via-gateway.postman_collection.json`
- Environment: `docs/postman/eventmaster-ticket-service-via-gateway.postman_environment.json`
- Guia rapido: `docs/postman/README.md`

Observacao: a nomenclatura dos arquivos Postman foi padronizada para usar `-local` quando o acesso e direto ao servico e `-via-gateway` quando o acesso atravessa o `gateway-service`.

Os arquivos de apoio para testes manuais do `order-service` via gateway tambem estao em `docs/postman/`:

- Collection: `docs/postman/eventmaster-order-service-via-gateway.postman_collection.json`
- Environment: `docs/postman/eventmaster-order-service-via-gateway.postman_environment.json`
- Guia rapido: `docs/postman/README.md`

Os arquivos de apoio para validacao manual do `gateway-service` tambem estao em `docs/postman/`:

- Collection: `docs/postman/eventmaster-gateway-service-local.postman_collection.json`
- Environment: `docs/postman/eventmaster-gateway-service-local.postman_environment.json`
- Guia rapido: `docs/postman/README.md`

Tambem existe uma collection ponta a ponta via gateway para validar o fluxo principal da plataforma:

- Collection: `docs/postman/eventmaster-platform-e2e-via-gateway.postman_collection.json`
- Environment: `docs/postman/eventmaster-platform-e2e-via-gateway.postman_environment.json`
- Guia rapido: `docs/postman/README.md`

Tambem existe uma collection ponta a ponta via gateway dedicada ao cenário de pagamento negado:

- Collection: `docs/postman/eventmaster-platform-e2e-payment-denied-via-gateway.postman_collection.json`
- Environment: `docs/postman/eventmaster-platform-e2e-payment-denied-via-gateway.postman_environment.json`
- Guia rapido: `docs/postman/README.md`

### Fluxo coberto na collection

1. `Login`
2. `Chamada protegida`
3. `Logout`
4. `Chamada pos-logout`

### Como usar

1. Importe a collection e o environment no Postman.
2. Selecione o environment `EventMaster - User Service Local`.
3. Garanta que o `user-service` esteja rodando em `http://localhost:8080`.
4. Execute os requests na ordem definida na collection.

Observacao: apos o logout, a collection limpa a variavel `jwt` e preserva o token revogado em `revokedJwt` apenas para validar o teste final de token revogado.

### 4) Empacotar apenas o `user-service`

```powershell
.\mvnw.cmd -pl services/user-service clean package
```

### 5) Rodar apenas o `gateway-service`

```powershell
.\mvnw.cmd -pl services/gateway-service spring-boot:run
```

## Gateway routes

Com o `gateway-service` rodando na porta `8081` e o `user-service` na `8080`, as rotas principais sao:

- API usuarios: `http://localhost:8081/api/users/**` -> `http://localhost:8080/user-service/usuarios/**`
- Auth user-service: `http://localhost:8081/api/auth/**` -> `http://localhost:8080/auth/**`
- OpenAPI user-service: `http://localhost:8081/api/users/v3/api-docs`
- Swagger UI user-service via gateway: `http://localhost:8081/api/users/swagger-ui/index.html`
- API eventos: `http://localhost:8081/api/events/**` -> `http://localhost:8082/event-service/eventos/**`
- OpenAPI event-service: `http://localhost:8081/api/events/v3/api-docs`
- Swagger UI event-service via gateway: `http://localhost:8081/api/events/swagger-ui/index.html`
- API ingressos: `http://localhost:8081/api/tickets/**` -> `http://localhost:8083/ticket-service/ingressos/**`
- OpenAPI ticket-service: `http://localhost:8081/api/tickets/v3/api-docs`
- Swagger UI ticket-service via gateway: `http://localhost:8081/api/tickets/swagger-ui/index.html`
- API pedidos: `http://localhost:8081/api/orders/**` -> `http://localhost:8084/order-service/pedidos/**`
- OpenAPI order-service: `http://localhost:8081/api/orders/v3/api-docs`
- Swagger UI order-service via gateway: `http://localhost:8081/api/orders/swagger-ui/index.html`

Variaveis uteis:

- `USER_SERVICE_URL` (default: `http://localhost:8080`)
- `EVENT_SERVICE_URL` (default: `http://localhost:8082`)
- `TICKET_SERVICE_URL` (default: `http://localhost:8083`)
- `ORDER_SERVICE_URL` (default: `http://localhost:8084`)
- `PORT` para porta do gateway (default: `8081`)
- `JWT_SECRET` segredo do token (deve ser igual ao `user-service`)
- `JWT_ISSUER` issuer do token (default: `API Event Master`)

Observacao sobre payloads via gateway:

- as rotas `POST/PUT /api/events/**` encaminham o campo `precoBase` sem alteracoes para o `event-service`
- as rotas `POST/PUT /api/tickets/**` encaminham o campo `situacao` sem alteracoes para o `ticket-service`
- as rotas `POST /api/orders/**` encaminham o payload de pedido sem alteracoes para o `order-service`

### Estrategia de autenticacao adotada

- O `gateway-service` valida JWT em rotas protegidas (assinatura, issuer e expiracao).
- O `user-service` tambem valida o JWT (defesa em profundidade).
- O `event-service` tambem valida o JWT (defesa em profundidade).
- O `ticket-service` tambem valida o JWT (defesa em profundidade).
- O `order-service` tambem valida o JWT (defesa em profundidade).
- Rotas publicas no gateway: `POST /api/auth/login`, docs Swagger e `OPTIONS`.
- `POST /api/auth/logout`, `/api/users/**`, `/api/events/**`, `/api/tickets/**` e `/api/orders/**` exigem token.

## Mensageria assíncrona entre `event-service` e `ticket-service`

Quando um novo evento e criado no `event-service`, o servico publica uma mensagem Kafka no topico `EVENTO_CRIADO` com:

- `eventId`
- `nome`
- `capacidade`

O `ticket-service` consome essa mensagem de forma assincrona e prepara o Redis com a quantidade inicial de ingressos disponiveis usando a regra:

```text
ingressos_disponiveis = floor(capacidade * 0.95)
```

Ou seja, 5% da capacidade ficam fora do cache operacional como margem de seguranca.

### Chave Redis utilizada

O `ticket-service` grava o estoque inicial na chave:

```text
evento:<eventId>:ingressos_disponiveis
```

Exemplo:

```text
evento:1:ingressos_disponiveis
```

### Variaveis de ambiente uteis para Kafka/Redis

No `event-service`:

- `KAFKA_BOOTSTRAP_SERVERS` (default: `localhost:9092`)
- `KAFKA_TOPIC_EVENTO_CRIADO` (default: `EVENTO_CRIADO`)
- `EVENT_CREATED_PUBLISHING_ENABLED` (default: `true`)

No `ticket-service`:

- `KAFKA_BOOTSTRAP_SERVERS` (default: `localhost:9092`)
- `KAFKA_CONSUMER_GROUP` (default: `ticket-service-group`)
- `KAFKA_TOPIC_EVENTO_CRIADO` (default: `EVENTO_CRIADO`)
- `REDIS_HOST` (default: `localhost`)
- `REDIS_PORT` (default: `6379`)

### Fluxo resumido

1. Cliente cria um evento no `event-service`
2. O evento e persistido no MySQL
3. O `event-service` publica `EVENTO_CRIADO` no Kafka
4. O `ticket-service` consome a mensagem
5. O `ticket-service` calcula `95%` da capacidade
6. O `ticket-service` grava o estoque inicial no Redis

## `order-service`: núcleo transacional do fluxo de compra

O `order-service` centraliza a criacao do pedido e executa a orquestracao do fluxo de compra:

1. recebe a intencao de compra do cliente autenticado
2. consulta o `event-service` para obter o `precoBase` do evento
3. chama o `ticket-service` de forma sincrona para reservar posicoes no Redis
4. calcula o valor de cada item (`INTEIRA = precoBase`, `MEIA = precoBase / 2`)
5. persiste o pedido e seus itens no MySQL
6. publica o evento Kafka `PEDIDO_REALIZADO`

### Tabelas do `order-service`

- `pedidos`
- `pedido_itens`

### Endpoint principal

- direto: `POST http://localhost:8084/order-service/pedidos`
- via gateway: `POST http://localhost:8081/api/orders`

### Variaveis de ambiente uteis do `order-service`

- `PORT` (default: `8084`)
- `DB_IP` (default: `localhost`)
- `DB_PORT` (default: `3306`)
- `DB_NAME` (default: `eventmaster_order`)
- `DB_USER` (default: `root`)
- `DB_PWD` (default: `root`)
- `KAFKA_BOOTSTRAP_SERVERS` (default: `localhost:9092`)
- `KAFKA_TOPIC_PEDIDO_REALIZADO` (default: `PEDIDO_REALIZADO`)
- `KAFKA_TOPIC_PAGAMENTO_CONFIRMADO` (default: `PAGAMENTO_CONFIRMADO`)
- `KAFKA_TOPIC_PAGAMENTO_NEGADO` (default: `PAGAMENTO_NEGADO`)
- `KAFKA_TOPIC_PEDIDO_CONFIRMADO` (default: `PEDIDO_CONFIRMADO`)
- `KAFKA_TOPIC_PEDIDO_CANCELADO` (default: `PEDIDO_CANCELADO`)
- `ORDER_EVENT_PUBLISHING_ENABLED` (default: `true`)
- `EVENT_SERVICE_ENDPOINT` (default: `http://localhost:8082/event-service/eventos`)
- `TICKET_SERVICE_ENDPOINT` (default: `http://localhost:8083/ticket-service/ingressos`)

### Exemplo de payload de pedido

```json
{
  "eventId": 1,
  "formaPagamento": "PIX",
  "itens": [
    {
      "nomePortador": "Fabricio Sanches",
      "cpfPortador": "123.456.789-00",
      "tipoIngresso": "INTEIRA"
    },
    {
      "nomePortador": "Ada Lovelace",
      "cpfPortador": "987.654.321-00",
      "tipoIngresso": "MEIA"
    }
  ]
}
```

## Fluxo assíncrono de pagamento e finalizacao do pedido

O fluxo ponta a ponta de compra fica distribuido entre `order-service`, `payment-service` e `ticket-service` da seguinte forma:

1. o `order-service` cria o pedido com status `REALIZADO`
2. o `order-service` publica `PEDIDO_REALIZADO`
3. o `ticket-service` materializa os ingressos do pedido com situacao `Reservado`
4. o `payment-service` consome `PEDIDO_REALIZADO`
5. o `payment-service` aplica a regra simulada de pagamento:
   - pedidos cujo `pedidoId` termina em `6` ou `9` publicam `PAGAMENTO_NEGADO`
   - todos os demais publicam `PAGAMENTO_CONFIRMADO`
6. o `order-service` consome o resultado do pagamento:
   - `PAGAMENTO_CONFIRMADO` -> pedido vai para `CONFIRMADO` e o servico publica `PEDIDO_CONFIRMADO`
   - `PAGAMENTO_NEGADO` -> pedido vai para `CANCELADO` e o servico publica `PEDIDO_CANCELADO`
7. o `ticket-service` consome o evento final:
   - `PEDIDO_CONFIRMADO` -> ingressos passam de `Reservado` para `Confirmado`
   - `PEDIDO_CANCELADO` -> ingressos passam para `Disponivel` e a quantidade e devolvida ao Redis

### Observacoes de resiliencia no `ticket-service`

- O fluxo foi implementado para tolerar entrega fora de ordem entre topicos Kafka.
- Se `PEDIDO_CONFIRMADO` chegar antes de `PEDIDO_REALIZADO`, o `ticket-service` materializa diretamente os ingressos como `Confirmado`.
- Se `PEDIDO_CANCELADO` chegar antes de `PEDIDO_REALIZADO`, o `ticket-service` materializa os ingressos como `Disponivel` e estorna a reserva no Redis.
- Reentregas do mesmo evento sao tratadas de forma idempotente para evitar confirmacoes ou estornos duplicados.

### Variaveis de ambiente uteis do `payment-service`

- `PORT` (default: `8086`)
- `KAFKA_BOOTSTRAP_SERVERS` (default: `localhost:9092`)
- `KAFKA_CONSUMER_GROUP` (default: `payment-service-group`)
- `KAFKA_TOPIC_PEDIDO_REALIZADO` (default: `PEDIDO_REALIZADO`)
- `KAFKA_TOPIC_PAGAMENTO_CONFIRMADO` (default: `PAGAMENTO_CONFIRMADO`)
- `KAFKA_TOPIC_PAGAMENTO_NEGADO` (default: `PAGAMENTO_NEGADO`)

### Variaveis de ambiente uteis adicionais do `ticket-service`

- `KAFKA_TOPIC_PEDIDO_CONFIRMADO` (default: `PEDIDO_CONFIRMADO`)
- `KAFKA_TOPIC_PEDIDO_CANCELADO` (default: `PEDIDO_CANCELADO`)

## Observabilidade no Gateway

O `gateway-service` possui filtro global de log para todas as requisicoes com:

- metodo HTTP
- path
- status de resposta
- latencia em milissegundos
- `X-Correlation-Id`

Se o cliente nao enviar `X-Correlation-Id`, o gateway gera automaticamente e devolve o mesmo header na resposta.

## Sobre o monorepo

- A raiz (`pom.xml`) agrega modulos com `<packaging>pom</packaging>`.
- Os modulos atualmente registrados sao `services/user-service`, `services/event-service`, `services/ticket-service`, `services/order-service`, `services/payment-service` e `services/gateway-service`.
- Novos servicos devem ser criados em `services/<nome-do-servico>` e adicionados em `<modules>` no `pom.xml` da raiz.

## Dicas para IntelliJ

- Abra a pasta raiz `eventmaster-mod1-ada` (nao apenas `services/user-service`).
- Reimporte o projeto Maven se um modulo nao aparecer na janela Maven.
- Se houver cache desatualizado: `File > Invalidate Caches...`.

