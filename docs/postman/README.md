# Postman - User Service, Event Service, Ticket Service, Order Service e Gateway Service

Arquivos de apoio para testar manualmente os módulos `user-service`, `event-service`, `ticket-service`, `order-service` e `gateway-service`.

Observação: o `payment-service` permanece interno, sem API HTTP exposta e sem collection Postman própria. O comportamento dele é validado indiretamente pelas collections do `order-service` e pela collection `platform-e2e-via-gateway`, que agora verificam o estado final do fluxo assíncrono.

## Convenção de nomes

- `*-local`: fluxo chamando o serviço diretamente na porta local do próprio módulo
- `*-via-gateway`: fluxo chamando o ecossistema através do `gateway-service`
- `platform-e2e-via-gateway`: fluxo ponta a ponta atravessando o gateway e validando integrações síncronas e assíncronas

## Arquivos

- `eventmaster-user-service-local.postman_collection.json`
- `eventmaster-user-service-local.postman_environment.json`
- `eventmaster-user-service-crud-local.postman_collection.json`
- `eventmaster-user-service-crud-local.postman_environment.json`
- `eventmaster-user-service-crud-via-gateway.postman_collection.json`
- `eventmaster-user-service-crud-via-gateway.postman_environment.json`
- `eventmaster-event-service-local.postman_collection.json`
- `eventmaster-event-service-local.postman_environment.json`
- `eventmaster-event-service-via-gateway.postman_collection.json`
- `eventmaster-event-service-via-gateway.postman_environment.json`
- `eventmaster-ticket-service-via-gateway.postman_collection.json`
- `eventmaster-ticket-service-via-gateway.postman_environment.json`
- `eventmaster-order-service-via-gateway.postman_collection.json`
- `eventmaster-order-service-via-gateway.postman_environment.json`
- `eventmaster-gateway-service-local.postman_collection.json`
- `eventmaster-gateway-service-local.postman_environment.json`
- `eventmaster-platform-e2e-via-gateway.postman_collection.json`
- `eventmaster-platform-e2e-via-gateway.postman_environment.json`
- `eventmaster-platform-e2e-payment-denied-via-gateway.postman_collection.json`
- `eventmaster-platform-e2e-payment-denied-via-gateway.postman_environment.json`

## Collection do user-service

A collection possui 4 requests em sequência:

1. `1 - Login`
2. `2 - Chamada protegida (roles)`
3. `3 - Logout`
4. `4 - Chamada pós-logout`

## Variáveis do environment

- `baseUrl`: URL base da aplicação
- `jwt`: token ativo da sessão atual
- `revokedJwt`: token revogado, usado apenas no teste pós-logout
- `login`: usuário de teste
- `senha`: senha do usuário de teste

Valor padrão atual:

- `baseUrl = http://localhost:8080`

## Como importar no Postman

1. Abra o Postman
2. Clique em **Import**
3. Importe os arquivos da pasta `docs/postman/`
4. Selecione o environment **EventMaster - User Service Local**

## Como executar

Execute os requests na ordem:

1. `1 - Login`
2. `2 - Chamada protegida (roles)`
3. `3 - Logout`
4. `4 - Chamada pós-logout`

## Comportamento esperado

### 1 - Login
- status `200`
- salva o token em `jwt`
- limpa `revokedJwt`

### 2 - Chamada protegida
- status `200`
- usa `Authorization: Bearer {{jwt}}`

### 3 - Logout
- status `200`
- move `jwt` para `revokedJwt`
- limpa `jwt`

### 4 - Chamada pós-logout
- status `401`
- usa `Authorization: Bearer {{revokedJwt}}`
- valida que o token foi revogado

## Observação

Se futuramente o acesso passar pelo API Gateway, normalmente basta alterar a variável `baseUrl` no environment para a URL do gateway.

## Collection do user-service CRUD local

A collection `eventmaster-user-service-crud-local.postman_collection.json` cobre o CRUD administrativo de usuários no `user-service` local, reutilizando o fluxo de autenticação JWT no início e encerrando a sessão ao final.

Ela executa os seguintes requests em sequência:

1. `1 - Login admin`
2. `2 - Listar roles`
3. `3 - Registrar usuario`
4. `4 - Buscar usuario criado`
5. `5 - Atualizar usuario`
6. `6 - Listar todos os usuarios`
7. `7 - Excluir usuario`
8. `8 - Buscar usuario excluido`
9. `9 - Logout`

### Variáveis do environment do user-service CRUD local

- `baseUrl`: URL base do `user-service`
- `jwt`: token JWT obtido no login administrativo
- `login`: usuário administrador usado no login
- `senha`: senha do usuário administrador
- `crudRunId`: identificador único da execução atual
- `crudUserLogin`: login do usuário de teste criado dinamicamente
- `crudUserName`: nome inicial do usuário criado
- `crudUpdatedName`: nome esperado após a atualização
- `crudUserRole`: role inicial usada no cadastro
- `crudUpdatedRole`: role usada na atualização
- `crudUserCpf`: CPF enviado no cadastro
- `crudUserPassword`: campo de senha enviado no payload de cadastro

Valores padrão atuais:

- `baseUrl = http://localhost:8080`
- `login = fsanchesbr001@gmail.com`

### Como executar o fluxo CRUD do user-service

1. Importe a collection `eventmaster-user-service-crud-local.postman_collection.json`
2. Importe o environment `eventmaster-user-service-crud-local.postman_environment.json`
3. Selecione o environment **EventMaster - User Service CRUD Local**
4. Garanta que o `user-service` esteja rodando em `localhost:8080`
5. Execute os requests na ordem definida na collection

### Comportamento esperado no user-service CRUD local

#### 1 - Login admin
- status `200`
- salva o token em `jwt`
- inicializa variáveis dinâmicas para evitar conflito entre execuções

#### 2 - Listar roles
- status `200`
- retorna a lista de perfis disponíveis

#### 3 - Registrar usuario
- status `201`
- cria um usuário dinâmico com login único por execução

#### 4 - Buscar usuario criado
- status `200`
- retorna o usuário recém-criado com `login`, `nome` e `role` esperados

#### 5 - Atualizar usuario
- status `200`
- retorna o usuário com `nome` e `role` atualizados

#### 6 - Listar todos os usuarios
- status `200`
- valida o contrato atual do endpoint como uma lista JSON não vazia

#### 7 - Excluir usuario
- status `204`
- remove o usuário criado na execução

#### 8 - Buscar usuario excluido
- status `404`
- valida o payload padrão de recurso não encontrado após a exclusão

#### 9 - Logout
- status `200`
- limpa a variável `jwt`

## Collection do user-service CRUD via gateway

A collection `eventmaster-user-service-crud-via-gateway.postman_collection.json` cobre o mesmo CRUD administrativo de usuários, mas trafegando pelo `gateway-service` e usando as rotas públicas `/api/auth/**` e `/api/users/**`.

Ela executa os seguintes requests em sequência:

1. `1 - Login via Gateway`
2. `2 - Listar roles via Gateway`
3. `3 - Registrar usuario via Gateway`
4. `4 - Buscar usuario criado via Gateway`
5. `5 - Atualizar usuario via Gateway`
6. `6 - Listar todos os usuarios via Gateway`
7. `7 - Excluir usuario via Gateway`
8. `8 - Buscar usuario excluido via Gateway`
9. `9 - Logout via Gateway`

### Variáveis do environment do user-service CRUD via gateway

- `baseUrl`: URL base do `gateway-service`
- `jwt`: token JWT obtido no login administrativo via gateway
- `login`: usuário administrador usado no login
- `senha`: senha do usuário administrador
- `crudRunId`: identificador único da execução atual
- `crudUserLogin`: login do usuário de teste criado dinamicamente
- `crudUserName`: nome inicial do usuário criado
- `crudUpdatedName`: nome esperado após a atualização
- `crudUserRole`: role inicial usada no cadastro
- `crudUpdatedRole`: role usada na atualização
- `crudUserCpf`: CPF enviado no cadastro
- `crudUserPassword`: campo de senha enviado no payload de cadastro

Valores padrão atuais:

- `baseUrl = http://localhost:8081`
- `login = fsanchesbr001@gmail.com`

### Como executar o fluxo CRUD do user-service via gateway

1. Importe a collection `eventmaster-user-service-crud-via-gateway.postman_collection.json`
2. Importe o environment `eventmaster-user-service-crud-via-gateway.postman_environment.json`
3. Selecione o environment **EventMaster - User Service CRUD via Gateway**
4. Garanta que `gateway-service` e `user-service` estejam rodando
5. Execute os requests na ordem definida na collection

### Comportamento esperado no user-service CRUD via gateway

#### 1 - Login via Gateway
- status `200`
- salva o token em `jwt`
- inicializa variáveis dinâmicas para evitar conflito entre execuções

#### 2 - Listar roles via Gateway
- status `200`
- retorna a lista de perfis disponíveis

#### 3 - Registrar usuario via Gateway
- status `201`
- cria um usuário dinâmico com login único por execução

#### 4 - Buscar usuario criado via Gateway
- status `200`
- retorna o usuário recém-criado com `login`, `nome` e `role` esperados

#### 5 - Atualizar usuario via Gateway
- status `200`
- retorna o usuário com `nome` e `role` atualizados

#### 6 - Listar todos os usuarios via Gateway
- status `200`
- valida o contrato atual do endpoint como uma lista JSON não vazia

#### 7 - Excluir usuario via Gateway
- status `204`
- remove o usuário criado na execução

#### 8 - Buscar usuario excluido via Gateway
- status `404`
- valida o payload padrão de recurso não encontrado após a exclusão

#### 9 - Logout via Gateway
- status `200`
- limpa a variável `jwt`

## Collection do event-service

A collection de eventos cobre o CRUD completo:

1. `1 - Criar evento`
2. `2 - Listar eventos`
3. `3 - Buscar evento por id`
4. `4 - Atualizar evento`
5. `5 - Excluir evento`
6. `6 - Buscar evento excluído`

### Variáveis do environment do event-service

- `baseUrl`: URL base do `event-service`
- `eventId`: id do evento criado no primeiro request

Valor padrão atual:

- `baseUrl = http://localhost:8082`

### Como executar o fluxo do event-service

1. Importe a collection `eventmaster-event-service-local.postman_collection.json`
2. Importe o environment `eventmaster-event-service-local.postman_environment.json`
3. Selecione o environment **EventMaster - Event Service Local**
4. Execute os requests na ordem definida na collection

### Comportamento esperado no event-service

#### 1 - Criar evento
- status `201`
- salva o identificador do evento em `eventId`
- envia e valida o campo `precoBase`

#### 2 - Listar eventos
- status `200`
- retorna uma lista com ao menos um evento

#### 3 - Buscar evento por id
- status `200`
- retorna o evento salvo em `eventId`
- valida o valor de `precoBase`

#### 4 - Atualizar evento
- status `200`
- retorna os dados atualizados
- valida a alteração de `precoBase`

#### 5 - Excluir evento
- status `204`

#### 6 - Buscar evento excluído
- status `404`
- valida que o recurso não existe mais

## Collection do event-service via gateway

A collection de eventos via gateway executa o CRUD autenticado completo, incluindo login no início e logout no final:

1. `1 - Login via Gateway`
2. `2 - Criar evento via Gateway`
3. `3 - Listar eventos via Gateway`
4. `4 - Buscar evento por id via Gateway`
5. `5 - Atualizar evento via Gateway`
6. `6 - Excluir evento via Gateway`
7. `7 - Buscar evento excluído via Gateway`
8. `8 - Logout via Gateway`

### Variáveis do environment do event-service via gateway

- `baseUrl`: URL base do `gateway-service`
- `jwt`: token JWT obtido no login
- `eventId`: id interno do evento criado durante o fluxo
- `login`: usuário usado no login
- `senha`: senha usada no login

Valor padrão atual:

- `baseUrl = http://localhost:8081`

### Como executar o fluxo do event-service via gateway

1. Importe a collection `eventmaster-event-service-via-gateway.postman_collection.json`
2. Importe o environment `eventmaster-event-service-via-gateway.postman_environment.json`
3. Selecione o environment **EventMaster - Event Service Local via Gateway**
4. Garanta que `gateway-service`, `user-service` e `event-service` estejam rodando
5. Execute os requests na ordem definida na collection

### Comportamento esperado no event-service via gateway

#### 1 - Login via Gateway
- status `200`
- salva o token em `jwt`

#### 2 - Criar evento via Gateway
- status `201`
- salva o identificador interno em `eventId`
- envia e valida o campo `precoBase`

#### 3 - Listar eventos via Gateway
- status `200`
- retorna uma lista com ao menos um evento
- valida a presença de `precoBase`

#### 4 - Buscar evento por id via Gateway
- status `200`
- retorna o evento salvo em `eventId`
- valida o valor de `precoBase`

#### 5 - Atualizar evento via Gateway
- status `200`
- retorna os dados atualizados
- valida a alteração de `precoBase`

#### 6 - Excluir evento via Gateway
- status `204`

#### 7 - Buscar evento excluído via Gateway
- status `404`
- valida que o recurso não existe mais

#### 8 - Logout via Gateway
- status `200`
- limpa a variável `jwt`

## Collection do ticket-service

A collection de ingressos executa o CRUD completo via API Gateway, incluindo autenticação no início e logout no final.

Os requests disponíveis são:

1. `1 - Login via Gateway`
2. `2 - Criar ingresso via Gateway`
3. `3 - Listar ingressos via Gateway`
4. `4 - Buscar ingresso por id via Gateway`
5. `5 - Atualizar ingresso via Gateway`
6. `6 - Excluir ingresso via Gateway`
7. `7 - Buscar ingresso excluído via Gateway`
8. `8 - Logout via Gateway`

### Variáveis do environment do ticket-service

- `baseUrl`: URL base do `gateway-service`
- `jwt`: token JWT obtido no login
- `ticketId`: id interno do ingresso criado durante o fluxo
- `login`: usuário usado no login
- `senha`: senha usada no login

Valor padrão atual:

- `baseUrl = http://localhost:8081`

### Como executar o fluxo do ticket-service

1. Importe a collection `eventmaster-ticket-service-via-gateway.postman_collection.json`
2. Importe o environment `eventmaster-ticket-service-via-gateway.postman_environment.json`
3. Selecione o environment **EventMaster - Ticket Service Local via Gateway**
4. Garanta que `gateway-service`, `user-service` e `ticket-service` estejam rodando
5. Execute os requests na ordem definida na collection

### Comportamento esperado no ticket-service

#### 1 - Login via Gateway
- status `200`
- salva o token em `jwt`

#### 2 - Criar ingresso via Gateway
- status `201`
- salva o identificador interno em `ticketId`
- envia e valida o campo `situacao`

#### 3 - Listar ingressos via Gateway
- status `200`
- retorna uma lista com ao menos um ingresso

#### 4 - Buscar ingresso por id via Gateway
- status `200`
- retorna o ingresso salvo em `ticketId`
- valida o valor de `situacao`

#### 5 - Atualizar ingresso via Gateway
- status `200`
- retorna os dados atualizados
- valida a alteração de `situacao`

#### 6 - Excluir ingresso via Gateway
- status `204`

#### 7 - Buscar ingresso excluído via Gateway
- status `404`
- valida que o recurso não existe mais

#### 8 - Logout via Gateway
- status `200`
- limpa a variável `jwt`

## Collection do order-service via gateway

A collection de pedidos valida o fluxo transacional principal do `order-service` através do `gateway-service`, incluindo a conclusão assíncrona do pagamento refletida no `ticket-service`.

Ela executa o seguinte roteiro:

1. `1 - Login via Gateway`
2. `2 - Criar evento de apoio via Gateway`
3. `3 - Buscar evento criado via Gateway`
4. `4 - Criar pedido via Gateway`
5. `5 - Listar ingressos finalizados via Gateway`
6. `6 - Logout via Gateway`

### Variáveis do environment do order-service

- `baseUrl`: URL base do `gateway-service`
- `jwt`: token JWT obtido no login
- `eventId`: id do evento criado para suportar o pedido
- `orderId`: id do pedido criado no fluxo
- `eventName`: nome dinâmico do evento criado para a execução corrente
- `attendeeOneName`: nome dinâmico do primeiro portador
- `attendeeTwoName`: nome dinâmico do segundo portador
- `ticketPollAttempts`: contador de retentativas até os ingressos chegarem ao estado final
- `expectedPaymentOutcome`: resultado assíncrono esperado (`PAGAMENTO_CONFIRMADO` ou `PAGAMENTO_NEGADO`)
- `expectedOrderOutcome`: estado final esperado do pedido (`CONFIRMADO` ou `CANCELADO`)
- `expectedTicketStatus`: situação final esperada dos ingressos (`Confirmado` ou `Disponivel`)
- `login`: usuário usado no login
- `senha`: senha usada no login

Valor padrão atual:

- `baseUrl = http://localhost:8081`

### Como executar o fluxo do order-service

1. Importe a collection `eventmaster-order-service-via-gateway.postman_collection.json`
2. Importe o environment `eventmaster-order-service-via-gateway.postman_environment.json`
3. Selecione o environment **EventMaster - Order Service Local via Gateway**
4. Garanta que `gateway-service`, `user-service`, `event-service`, `ticket-service`, `order-service`, `payment-service`, `Kafka` e `Redis` estejam rodando
5. Execute os requests na ordem definida na collection

### Comportamento esperado no order-service

#### 1 - Login via Gateway
- status `200`
- salva o token em `jwt`

#### 2 - Criar evento de apoio via Gateway
- status `201`
- salva o identificador interno em `eventId`

#### 3 - Buscar evento criado via Gateway
- status `200`
- confirma o evento usado no pedido

#### 4 - Criar pedido via Gateway
- status `201`
- salva o identificador interno em `orderId`
- valida `valorTotal = 134.85`
- valida `status = REALIZADO`
- calcula o desfecho assíncrono esperado a partir do `orderId`
- se o `orderId` terminar em `6` ou `9`, o fluxo esperado passa a ser `PAGAMENTO_NEGADO` -> `CANCELADO`
- para os demais casos, o fluxo esperado passa a ser `PAGAMENTO_CONFIRMADO` -> `CONFIRMADO`

#### 5 - Listar ingressos finalizados via Gateway
- status `200`
- faz retentativas automáticas até os dois ingressos chegarem ao estado final esperado
- se o pagamento for confirmado, valida `situacao = Confirmado`
- se o pagamento for negado, valida `situacao = Disponivel`
- mantém as validações de tipos e valores `89.90` e `44.95`

#### 6 - Logout via Gateway
- status `200`
- limpa a variável `jwt`

## Collection end-to-end de pagamento negado via gateway

A collection `eventmaster-platform-e2e-payment-denied-via-gateway.postman_collection.json` valida especificamente o cenário final de pagamento negado atravessando o `gateway-service`.

Ela executa o seguinte roteiro:

1. `1 - Login via Gateway`
2. `2 - Criar evento candidato via Gateway`
3. `3 - Buscar evento candidato via Gateway`
4. `4 - Criar pedido candidato via Gateway`
5. `5 - Listar ingressos cancelados via Gateway`
6. `6 - Logout via Gateway`

### Estratégia adotada

- O `payment-service` nega pagamentos quando o `orderId` termina em `6` ou `9`.
- Como o `orderId` é gerado pelo banco, a collection cria pedidos candidatos automaticamente até encontrar um identificador com esse final.
- Quando encontra esse caso, passa a validar o estado final assíncrono do fluxo.

### Variáveis do environment da collection de pagamento negado

- `baseUrl`: URL base do `gateway-service`
- `jwt`: token JWT obtido no login
- `runId`: identificador único da execução atual
- `eventSequence`: contador de eventos candidatos criados na execução
- `eventId`: id do evento candidato atual
- `orderId`: id do pedido candidato que atingiu o cenário negado
- `eventName`: nome dinâmico do evento candidato vigente
- `attendeeOneName`: primeiro portador do cenário negado
- `attendeeTwoName`: segundo portador do cenário negado
- `deniedOrderAttempts`: quantidade de pedidos já tentados sem final `6` ou `9`
- `orderCreateAttempts`: retentativas por atraso de propagação do estoque no Redis
- `ticketPollAttempts`: retentativas para aguardar o estado final dos ingressos
- `expectedPaymentOutcome`: fixado em `PAGAMENTO_NEGADO`
- `expectedOrderOutcome`: fixado em `CANCELADO`
- `expectedTicketStatus`: fixado em `Disponivel`
- `login`: usuário usado no login
- `senha`: senha usada no login

Valor padrão atual:

- `baseUrl = http://localhost:8081`

### Como executar o fluxo de pagamento negado

1. Importe a collection `eventmaster-platform-e2e-payment-denied-via-gateway.postman_collection.json`
2. Importe o environment `eventmaster-platform-e2e-payment-denied-via-gateway.postman_environment.json`
3. Selecione o environment **EventMaster - Platform E2E Payment Denied via Gateway**
4. Garanta que `gateway-service`, `user-service`, `event-service`, `ticket-service`, `order-service`, `payment-service`, `Kafka` e `Redis` estejam rodando
5. Execute a collection inteira pelo Collection Runner para permitir as retentativas automáticas

### Comportamento esperado no fluxo de pagamento negado

#### 1 - Login via Gateway
- status `200`
- salva o token em `jwt`
- inicializa os contadores da execução

#### 2 - Criar evento candidato via Gateway
- status `201`
- cria um evento com nome único para aquela tentativa

#### 3 - Buscar evento candidato via Gateway
- status `200`
- confirma o evento usado no pedido candidato atual

#### 4 - Criar pedido candidato via Gateway
- status final `201`
- pode fazer retentativas automáticas em caso de `409` enquanto o estoque ainda propaga para o Redis
- se o `orderId` não terminar em `6` ou `9`, a collection reinicia automaticamente o ciclo com um novo evento candidato
- quando encontra `orderId` terminando em `6` ou `9`, fixa esse pedido como cenário negado alvo da validação

#### 5 - Listar ingressos cancelados via Gateway
- status `200`
- faz polling até encontrar os 2 ingressos do pedido negado
- valida `situacao = Disponivel`
- valida os valores `89.90` e `44.95`

#### 6 - Logout via Gateway
- status `200`
- limpa a variável `jwt`

## Collection end-to-end da plataforma via gateway

A collection `eventmaster-platform-e2e-via-gateway.postman_collection.json` valida o fluxo principal ponta a ponta atravessando o `gateway-service`:

1. `1 - Login via Gateway`
2. `2 - Criar evento via Gateway`
3. `3 - Buscar evento via Gateway`
4. `4 - Criar pedido via Gateway`
5. `5 - Listar ingressos materializados via Gateway`
6. `6 - Logout via Gateway`

Esse fluxo cobre:

- autenticação pelo `user-service` via gateway
- criação de evento no `event-service`
- reserva transacional no `order-service`
- processamento assíncrono de pagamento no `payment-service`
- uso assíncrono de Kafka + Redis
- materialização física e finalização dos ingressos no `ticket-service`

### Variáveis do environment da collection end-to-end

- `baseUrl`: URL base do `gateway-service`
- `jwt`: token JWT obtido no login
- `eventId`: id do evento criado no fluxo
- `orderId`: id do pedido criado no fluxo
- `eventName`: nome dinâmico do evento criado para a execução corrente
- `attendeeOneName`: portador do ingresso inteira
- `attendeeTwoName`: portador do ingresso meia
- `orderCreateAttempts`: contador de retentativas para a criação do pedido
- `ticketPollAttempts`: contador de retentativas para a materialização dos ingressos
- `expectedPaymentOutcome`: resultado esperado do pagamento conforme o `orderId`
- `expectedOrderOutcome`: estado final esperado do pedido conforme o `orderId`
- `expectedTicketStatus`: situação final esperada dos ingressos conforme o `orderId`
- `login`: usuário usado no login
- `senha`: senha usada no login

Valor padrão atual:

- `baseUrl = http://localhost:8081`

### Como executar o fluxo end-to-end

1. Importe a collection `eventmaster-platform-e2e-via-gateway.postman_collection.json`
2. Importe o environment `eventmaster-platform-e2e-via-gateway.postman_environment.json`
3. Selecione o environment **EventMaster - Platform E2E via Gateway**
4. Garanta que `gateway-service`, `user-service`, `event-service`, `ticket-service`, `order-service`, `payment-service`, `Kafka` e `Redis` estejam rodando
5. Execute a collection inteira pelo Collection Runner para permitir as retentativas automáticas dos passos assíncronos

### Comportamento esperado no fluxo end-to-end

#### 1 - Login via Gateway
- status `200`
- salva o token em `jwt`
- inicializa variáveis dinâmicas da execução

#### 2 - Criar evento via Gateway
- status `201`
- salva o identificador em `eventId`
- envia e valida o campo `precoBase`

#### 3 - Buscar evento via Gateway
- status `200`
- confirma o evento recém-criado

#### 4 - Criar pedido via Gateway
- status final `201`
- pode fazer retentativas automáticas em caso de `409` enquanto a carga de estoque assíncrona ainda propaga para o Redis
- salva o identificador em `orderId`
- valida `valorTotal = 134.85`
- valida `status = REALIZADO`
- calcula automaticamente o resultado final esperado do fluxo com base no `orderId`

#### 5 - Listar ingressos materializados via Gateway
- status `200`
- pode fazer retentativas automáticas até a conclusão assíncrona do pagamento e a finalização dos ingressos físicos
- encontra 2 ingressos do pedido recém-criado
- valida `situacao = Confirmado` para pedidos aprovados
- valida `situacao = Disponivel` para pedidos negados (ids terminados em `6` ou `9`)
- valida os valores `89.90` e `44.95`

#### 6 - Logout via Gateway
- status `200`
- limpa a variável `jwt`

## Swagger do gateway-service

O `gateway-service` agora possui documentação OpenAPI e Swagger UI próprias, além de continuar expondo as documentações roteadas dos serviços downstream.

URLs principais:

- Swagger UI própria do gateway: `http://localhost:8081/swagger-ui.html`
- OpenAPI JSON própria do gateway: `http://localhost:8081/v3/api-docs`
- OpenAPI do `user-service` via gateway: `http://localhost:8081/api/users/v3/api-docs`
- OpenAPI do `event-service` via gateway: `http://localhost:8081/api/events/v3/api-docs`
- OpenAPI do `ticket-service` via gateway: `http://localhost:8081/api/tickets/v3/api-docs`
- OpenAPI do `order-service` via gateway: `http://localhost:8081/api/orders/v3/api-docs`

## Collection do gateway-service

A collection do `gateway-service` valida os endpoints de observabilidade do próprio gateway e também a disponibilização de documentação OpenAPI roteada por ele.

Os requests disponíveis são:

1. `1 - Healthcheck do Gateway`
2. `2 - OpenAPI do Order Service via Gateway`
3. `3 - Login via Gateway`
4. `4 - Listar rotas do Gateway`
5. `5 - Logout via Gateway`

### Variáveis do environment do gateway-service

- `baseUrl`: URL base do `gateway-service`
- `jwt`: token JWT obtido no login
- `login`: usuário usado no login
- `senha`: senha usada no login

Valor padrão atual:

- `baseUrl = http://localhost:8081`

### Como executar o fluxo do gateway-service

1. Importe a collection `eventmaster-gateway-service-local.postman_collection.json`
2. Importe o environment `eventmaster-gateway-service-local.postman_environment.json`
3. Selecione o environment **EventMaster - Gateway Service Local**
4. Garanta que `gateway-service`, `user-service` e `order-service` estejam rodando
5. Execute os requests na ordem definida na collection

### Comportamento esperado no gateway-service

#### 1 - Healthcheck do Gateway
- status `200`
- retorna `UP`

#### 2 - OpenAPI do Order Service via Gateway
- status `200`
- retorna um documento OpenAPI válido roteado pelo gateway

#### 3 - Login via Gateway
- status `200`
- salva o token em `jwt`

#### 4 - Listar rotas do Gateway
- status `200`
- retorna as rotas atualmente registradas no gateway, incluindo `order-service`

#### 5 - Logout via Gateway
- status `200`
- limpa a variável `jwt`

