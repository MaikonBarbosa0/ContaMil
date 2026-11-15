# ContaMil

Aplicativo web de finanças pessoais para uso individual. Organize seu salário, registre gastos e despesas recorrentes e acompanhe seu orçamento com gráficos por categoria, cores personalizáveis e temas claro e escuro.

Este repositório reúne o **frontend Angular** e o **backend Spring Boot**, com persistência em **PostgreSQL**. O aplicativo mobile é mantido em um projeto separado.

## Funcionalidades

- Visão mensal com salário, total de gastos, saldo e últimos lançamentos.
- Registro, edição e exclusão de gastos com descrição, valor em reais, data e categoria.
- Histórico salarial, com mudanças a partir do mês escolhido e preservação dos meses anteriores.
- Recorrências mensais com geração dos vencimentos pendentes sem duplicação.
- Ajuste dos dias 29, 30 e 31 ao último dia dos meses mais curtos, mantendo o dia original nos meses seguintes.
- Edição e cancelamento de recorrências com histórico preservado.
- Categorias com nomes fixos e cores editáveis, utilizadas nos gastos, gráficos e legendas.
- Gráfico de distribuição e evolução acumulada com uma linha por categoria.
- Temas claro e escuro, com troca manual e preferência salva no navegador.

As categorias são **Moradia, Mercado, Carro, Restaurantes, Lojas, Saúde, Educação, Lazer, Assinaturas e Outros**. A seleção é feita por lista; não há cadastro ou renomeação de categorias.

A aplicação é de uso individual, sem login, e a configuração disponível é destinada à execução local.

## Tecnologias e documentação

| Parte | Tecnologias | Documentação |
| --- | --- | --- |
| Frontend | Angular 22, TypeScript e SVG | [frontend/README.md](frontend/README.md) |
| Backend | Java 25, Spring Boot 4.1.2, Maven e Flyway | [backend/README.md](backend/README.md) |
| Banco de dados | PostgreSQL 18.6 | Configurado em [compose.yaml](compose.yaml) |

A interface consulta a API pelo caminho `/api`. Salários, gastos, recorrências e cores das categorias ficam no PostgreSQL. A preferência de tema fica no `localStorage` do navegador.

## Executar com Docker

### 1. Pré-requisitos

Tenha o Docker com suporte a Docker Compose e contêineres Linux. No Windows, mantenha o Docker Desktop em execução com o mecanismo Linux ativo.

Nesta opção, Java, Maven, Node.js e PostgreSQL são fornecidos pelas imagens Docker; não é necessário instalá-los separadamente no computador.

Execute os comandos a seguir na **raiz do projeto**, onde está o arquivo `compose.yaml`.

### 2. Configurar o ambiente

Crie o arquivo `.env` caso ainda não exista. Exemplo em PowerShell:

```powershell
if (-not (Test-Path -LiteralPath .env)) {
    Copy-Item -LiteralPath .env.example -Destination .env
}
```

Edite `.env` e substitua o valor de exemplo de `DB_PASSWORD` por uma senha local. Use [.env.example](.env.example) como referência:

| Variável | Finalidade | Valor de exemplo |
| --- | --- | --- |
| `DB_DATABASE` | Nome do banco | `contamil` |
| `DB_USERNAME` | Usuário do banco | `contamil` |
| `DB_PASSWORD` | Senha local do banco | Defina sua própria senha |
| `BACKEND_PORT` | Porta publicada da API | `8080` |
| `FRONTEND_PORT` | Porta publicada da interface | `4200` |

O `.env` fica fora do versionamento; o `.env.example` contém somente valores de exemplo e acompanha o código.

### 3. Iniciar a aplicação

```powershell
docker compose up --build -d
```

O comando prepara o frontend, a API e o banco. A API aguarda o PostgreSQL passar pela verificação de saúde antes de iniciar.

Com as portas padrão:

- Interface: [http://localhost:4200](http://localhost:4200).
- Base da API: `http://localhost:8080/api`.
- Verificação da API e conexão com o banco: [GET /api/health](http://localhost:8080/api/health).
- Contrato dos endpoints: [backend/API.md](backend/API.md).

Se alterar as portas no `.env`, utilize os novos valores nos endereços de acesso.

### 4. Começar a usar

No primeiro acesso, o banco contém as categorias, sem lançamentos financeiros fictícios:

1. Na tela **Salário**, informe o valor e o mês de início de vigência.
2. Em **Gastos**, registre uma despesa e selecione uma categoria da lista.
3. Marque **Repetir todo mês** para criar uma despesa recorrente.
4. Consulte os gráficos em **Visão geral** e personalize as cores em **Categorias**.

### 5. Consultar logs e parar os serviços

```powershell
docker compose ps
docker compose logs frontend backend
docker compose down
```

O PostgreSQL utiliza o volume persistente `postgres_data`; reiniciar os serviços ou executar `docker compose down` mantém os dados nesse volume. Interface e API ficam vinculadas a `127.0.0.1`, e o banco não publica a porta 5432 para o computador.

## Desenvolvimento local

### Frontend Angular

Com Node.js 26 e npm disponíveis, prepare o `.env` conforme as instruções acima e inicie somente o banco e a API a partir da raiz:

```powershell
docker compose up --build -d database backend
```

Em outro terminal, partindo da raiz:

```powershell
cd frontend
npm ci
npm start
```

O Angular inicia em `http://127.0.0.1:4200`. O proxy de desenvolvimento encaminha `/api/**` para `http://127.0.0.1:8080`. Se alterar a porta da API, ajuste também o `target` de [frontend/proxy.conf.json](frontend/proxy.conf.json).

Se a interface Docker já ocupar a porta 4200, use uma porta alternativa no servidor Angular:

```powershell
npm start -- --port 4201
```

Consulte o [README do frontend](frontend/README.md) para detalhes de build, testes e organização do código.

### Backend Spring Boot

O desenvolvimento fora do Docker exige JDK 25, Maven e uma instância PostgreSQL acessível pelo computador. O banco do Compose padrão não publica sua porta para uso pelo Maven no host.

As variáveis de ambiente precisam estar configuradas na sessão que iniciará o Java; o Spring Boot não carrega automaticamente o `.env` da raiz nessa modalidade. O [README do backend](backend/README.md) contém os comandos, as variáveis necessárias e as instruções para testes e empacotamento.

## Testes e compilação

Na pasta `frontend`, com as dependências instaladas:

```powershell
npm test -- --watch=false
npm run build
```

Na pasta `backend`, com JDK 25 e Maven disponíveis:

```powershell
mvn verify
```

O `mvn verify` compila, executa os testes e gera o JAR. Os testes do backend usam H2 em memória e não exigem um PostgreSQL em execução. Os testes de utilidades do frontend também não precisam da API.

## Estrutura do repositório

```text
ContaMil/
├── backend/          # API REST, regras financeiras e migrações
├── frontend/         # Interface Angular
├── compose.yaml      # Ambiente local com frontend, backend e PostgreSQL
├── .env.example      # Exemplo de configuração local
├── .gitignore        # Exclusões de credenciais, dependências e arquivos gerados
├── IDEA.md           # Ideia original do projeto
└── Agente.md         # Orientações para agentes IA e decisões registradas
```

Para trabalho assistido por agentes IA, consulte [Agente.md](Agente.md). A descrição original está em [IDEA.md](IDEA.md).
