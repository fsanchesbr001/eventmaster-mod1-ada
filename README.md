# eventmaster-mod1-ada

Projeto Final - Módulo 01 - Arquitetura

Monorepo Maven para os servicos do projeto EventMaster.

## Estrutura

```text
eventmaster-mod1-ada/
  pom.xml                 # agregador (packaging pom)
  services/
    user-service/         # modulo Maven do servico de usuarios
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

### 4) Empacotar apenas o `user-service`

```powershell
.\mvnw.cmd -pl services/user-service clean package
```

## Sobre o monorepo

- A raiz (`pom.xml`) agrega modulos com `<packaging>pom</packaging>`.
- O modulo atual registrado e `services/user-service`.
- Novos servicos devem ser criados em `services/<nome-do-servico>` e adicionados em `<modules>` no `pom.xml` da raiz.

## Dicas para IntelliJ

- Abra a pasta raiz `eventmaster-mod1-ada` (nao apenas `services/user-service`).
- Reimporte o projeto Maven se um modulo nao aparecer na janela Maven.
- Se houver cache desatualizado: `File > Invalidate Caches...`.

