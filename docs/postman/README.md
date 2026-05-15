# Postman - User Service

Arquivos de apoio para testar o fluxo de autenticação do `user-service`.

## Arquivos

- `eventmaster-user-service-auth.postman_collection.json`
- `eventmaster-user-service-local.postman_environment.json`

## O que a collection testa

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

