# eventmaster-mod1-ada

Projeto Final - Módulo 01 - Arquitetura

Monorepo Maven para os servicos do projeto EventMaster.

## Estrutura

```text
eventmaster-mod1-ada/
  pom.xml                 # agregador (packaging pom)
  services/
    user-service/         # modulo Maven do servico de usuarios
    gateway-service/      # API Gateway (Spring Cloud Gateway)
```

## Pre-requisitos

- Java 21
- Maven Wrapper (ja incluso no repositorio: `mvnw` / `mvnw.cmd`)

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

### 3) Rodar apenas o `user-service`

```powershell
.\mvnw.cmd -pl services/user-service spring-boot:run
```

## Documentacao da API (Swagger)

Com o `user-service` em execucao, acesse:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

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
- OpenAPI user-service: `http://localhost:8081/api/users/v3/api-docs`
- Swagger UI user-service via gateway: `http://localhost:8081/api/users/swagger-ui/index.html`

Variaveis uteis:

- `USER_SERVICE_URL` (default: `http://localhost:8080`)
- `PORT` para porta do gateway (default: `8081`)

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
- O modulo atual registrado e `services/user-service`.
- Novos servicos devem ser criados em `services/<nome-do-servico>` e adicionados em `<modules>` no `pom.xml` da raiz.

## Dicas para IntelliJ

- Abra a pasta raiz `eventmaster-mod1-ada` (nao apenas `services/user-service`).
- Reimporte o projeto Maven se um modulo nao aparecer na janela Maven.
- Se houver cache desatualizado: `File > Invalidate Caches...`.

