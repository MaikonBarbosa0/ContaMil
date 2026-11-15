# CONTAMIL — Backend

API REST da versão web do CONTAMIL, um aplicativo de finanças pessoais para uso individual. O backend registra salários e despesas, gera recorrências mensais e fornece os dados usados nos gráficos da interface Angular.

Esta parte utiliza PostgreSQL e foi configurada para execução local, sem cadastro ou login, em conjunto com a interface Angular deste repositório.

## Tecnologias

| Tecnologia | Versão/configuração no projeto |
| --- | --- |
| Java | 25, definido no `pom.xml`; Eclipse Temurin nas imagens Docker |
| Spring Boot | 4.1.2 |
| Maven | 3.9.16 na imagem de build; desenvolvimento local com Maven 3.9 ou superior |
| PostgreSQL | 18.6 no `compose.yaml` da raiz |
| Persistência | Spring Data JPA e migrações Flyway |
| Testes | Spring Boot Test, MockMvc e H2 em modo de compatibilidade PostgreSQL |

As versões das dependências Spring, Flyway e H2 são gerenciadas pelo Spring Boot. Não há Maven Wrapper no repositório.

## Funcionalidades

- Salário mensal em reais, com versões que passam a valer a partir do mês escolhido.
- Cadastro, consulta, edição e exclusão individual de despesas.
- Recorrências mensais com geração dos vencimentos até a data atual, sem duplicação.
- Preservação do dia original: uma recorrência do dia 31 usa o último dia de fevereiro e volta ao dia 31 em março.
- Edição e cancelamento de recorrências com preservação dos lançamentos anteriores.
- Dez categorias fixas, com alteração somente de suas cores.
- Resumo mensal com salário, total de gastos, saldo, totais por categoria e séries diárias por categoria.
- Validação das entradas e mensagens de erro em português.

Os valores financeiros usam `BigDecimal` no Java e `NUMERIC(14, 2)` no banco. Datas são representadas por `AAAA-MM-DD`; os filtros mensais usam `AAAA-MM`.

## Executar com Docker Compose

É necessário ter Docker Desktop com o mecanismo Linux e Docker Compose disponíveis. Java, Maven e PostgreSQL são fornecidos pelas imagens; não precisam estar instalados no Windows para esta opção.

Execute os comandos a seguir na **raiz do repositório**, onde estão `compose.yaml` e `.env.example`. Os exemplos usam PowerShell.

1. Crie o arquivo de configuração somente se ele ainda não existir:

   ```powershell
   if (-not (Test-Path -LiteralPath .env)) {
       Copy-Item -LiteralPath .env.example -Destination .env
   }
   ```

2. Edite `.env` e substitua o valor de exemplo de `DB_PASSWORD` por uma senha local. O arquivo contém também `DB_DATABASE`, `DB_USERNAME` e `BACKEND_PORT`. Não inclua o `.env` no GitHub; ele já está no `.gitignore`.

3. Suba somente a API e seu banco:

   ```powershell
   docker compose up --build -d database backend
   ```

O build do backend executa `mvn verify` antes de gerar a imagem de execução. O serviço `backend` aguarda o banco passar pela verificação de saúde.

Com as portas padrão, a API responde em `http://127.0.0.1:8080/api`. Para verificar a conexão com o banco:

```powershell
Invoke-RestMethod -Uri 'http://127.0.0.1:8080/api/health'
```

A resposta esperada é `{"status":"UP"}`. Esse é um endpoint de verificação; `/api` sozinho não oferece uma página de interface. Se alterar `BACKEND_PORT`, use a nova porta nas chamadas.

Estado e logs, ainda na raiz:

```powershell
docker compose ps
docker compose logs -f backend
```

Para parar somente a API:

```powershell
docker compose stop backend
```

Para encerrar os serviços da composição, preservando o volume do banco:

```powershell
docker compose down
```

Esse último comando encerra também o frontend caso esteja em execução no mesmo projeto. Para subir a aplicação web completa, consulte o [README da raiz](../README.md).

## Desenvolvimento local com Maven

Requisitos:

- JDK 25 e Maven disponíveis no `PATH`.
- PostgreSQL 18 acessível pelo computador, com banco e usuário já criados e permissão para aplicar as migrações.
- Uma porta HTTP livre para a API.

O PostgreSQL do Compose padrão **não publica a porta 5432** para o Windows. O nome `database` funciona somente dentro da rede Docker. Para executar o Java fora do Docker, use uma instância PostgreSQL acessível pelo host ou uma conexão explicitamente configurada para isso.

O Spring Boot não carrega automaticamente o `.env` da raiz quando iniciado pelo Maven. Configure as variáveis na mesma sessão PowerShell que executará a aplicação. O exemplo abaixo parte da raiz e pressupõe um banco acessível em `127.0.0.1:5432`:

```powershell
Set-Location .\backend
$env:DB_URL = 'jdbc:postgresql://127.0.0.1:5432/contamil'
$env:DB_USERNAME = 'contamil'
$contamilDbPassword = Read-Host 'Senha do PostgreSQL' -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $contamilDbPassword).Password
$env:PORT = '8080'
$env:CONTAMIL_TIME_ZONE = 'America/Sao_Paulo'
mvn spring-boot:run
```

Adapte endereço, banco e usuário à sua instância. Se a API Docker já ocupar a porta 8080, pare esse serviço na raiz ou escolha outra porta em `PORT`. Interrompa a execução local com `Ctrl+C`.

### Variáveis da aplicação

| Variável | Função | Padrão |
| --- | --- | --- |
| `DB_URL` | URL JDBC PostgreSQL, incluindo o nome do banco | `jdbc:postgresql://localhost:5432/contamil` |
| `DB_USERNAME` | Usuário do banco | `contamil` |
| `DB_PASSWORD` | Senha do banco | Obrigatória, sem valor padrão |
| `PORT` | Porta HTTP do processo Java | `8080` |
| `CONTAMIL_TIME_ZONE` | Fuso usado pelo relógio e pela geração diária | `America/Sao_Paulo` |

No Compose, `DB_DATABASE` escolhe o nome do banco e a composição monta `DB_URL` usando o endereço interno `database:5432`. `BACKEND_PORT` controla a porta publicada no computador; o processo Java no contêiner continua na porta 8080. Essas duas variáveis pertencem à configuração Docker, não substituem `DB_URL` e `PORT` na execução Maven.

## Testes e build

Execute dentro de `backend`:

```powershell
mvn test
```

Para compilar, executar os testes e empacotar o JAR:

```powershell
mvn verify
```

Os testes usam H2 em memória e não exigem um PostgreSQL em execução. A suíte cobre regras de salário e recorrência, centavos, histórico, exclusão de ocorrências, categorias fixas, cores, validação da API e migração de dados antigos. H2 é usado somente nos testes; a aplicação utiliza PostgreSQL.

O pacote gerado é `target/contamil-0.1.0-SNAPSHOT.jar`. Com o banco acessível e as variáveis da aplicação configuradas, também é possível executá-lo diretamente:

```powershell
java -jar .\target\contamil-0.1.0-SNAPSHOT.jar
```

## API

O contrato completo, os formatos das respostas, as cores iniciais e os exemplos de JSON estão em [API.md](API.md).

| Recurso | Principais rotas |
| --- | --- |
| Saúde | `GET /api/health` |
| Categorias | `GET /api/categories`, `PATCH /api/categories/{id}/color` |
| Salário | `GET /api/salary?month=AAAA-MM`, `PUT /api/salary?month=AAAA-MM`, `GET /api/salaries` |
| Gastos | `GET /api/expenses?month=AAAA-MM`, `POST /api/expenses`, `GET/PUT/DELETE /api/expenses/{id}` |
| Recorrências | `GET/POST /api/recurrences`, `GET/PUT/DELETE /api/recurrences/{id}` |
| Resumo | `GET /api/summary?month=AAAA-MM` |

Gastos e recorrências recebem `categoryId`, escolhido no catálogo. Categorias não podem ser criadas, renomeadas ou excluídas pela API. As respostas incluem nome e cor atuais da categoria, inclusive nas consultas ao histórico. Propriedades desconhecidas no JSON são rejeitadas.

## Migrações e persistência

O Flyway aplica as migrações no início da aplicação; o Hibernate está configurado para **validar** o esquema, sem recriá-lo a cada execução.

- **V1 — esquema inicial:** salários, recorrências e despesas.
- **V2 — categorias fixas:** catálogo aprovado e referências por identificador. Valida os rótulos antigos antes de alterar o esquema e interrompe a migração se encontrar um nome fora do catálogo, sem mapeamento automático.

No Docker, o PostgreSQL usa o volume `postgres_data`, montado em `/var/lib/postgresql`. Reiniciar a API ou executar `docker compose down` mantém os dados nesse volume. O banco não é publicado na rede do computador e a porta HTTP da API é vinculada a `127.0.0.1`.

As recorrências vencidas são geradas na inicialização, à meia-noite no fuso configurado e nas consultas financeiras pertinentes. Antes de editar ou cancelar uma regra, são materializados os vencimentos pendentes com os dados antigos. A exclusão de uma ocorrência conserva sua identidade interna, e uma restrição única por recorrência/mês impede sua recriação.

O primeiro início adiciona o catálogo de categorias, sem lançamentos financeiros fictícios. Este backend atende ao uso individual e local descrito no projeto; a configuração atual não implementa autenticação nem implantação em produção.

## Estrutura

```text
backend/
├── pom.xml                          # Dependências e build Maven
├── Dockerfile                       # Build com testes e imagem Java de execução
├── API.md                           # Contrato HTTP detalhado
└── src/
    ├── main/
    │   ├── java/br/com/contamil/     # Aplicação, controladores, serviços e persistência
    │   ├── java/db/migration/       # Migração Java V2
    │   └── resources/
    │       ├── application.yml      # Configuração e variáveis de ambiente
    │       └── db/migration/        # Migração SQL V1
    └── test/java/br/com/contamil/    # Testes de regras, API e migração
```
