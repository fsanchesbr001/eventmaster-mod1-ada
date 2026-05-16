# Postman - User Service, Event Service e Ticket Service

Arquivos de apoio para testar manualmente os módulos `user-service`, `event-service` e `ticket-service`.

## Arquivos

- `eventmaster-user-service-auth.postman_collection.json`
- `eventmaster-user-service-local.postman_environment.json`
- `eventmaster-event-service-crud.postman_collection.json`
- `eventmaster-event-service-local.postman_environment.json`
- `eventmaster-event-service-via-gateway.postman_collection.json`
- `eventmaster-event-service-via-gateway.postman_environment.json`
- `eventmaster-ticket-service-crud.postman_collection.json`
- `eventmaster-ticket-service-local.postman_environment.json`

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

1. Importe a collection `eventmaster-event-service-crud.postman_collection.json`
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

A collection de ingressos executa o CRUD completo via API Gateway, incluindo autenticação no início e logout no final:

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

1. Importe a collection `eventmaster-ticket-service-crud.postman_collection.json`
2. Importe o environment `eventmaster-ticket-service-local.postman_environment.json`
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

