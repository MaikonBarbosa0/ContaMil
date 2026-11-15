# CONTAMIL · Frontend

Interface web de finanças pessoais feita com Angular. O CONTAMIL reúne salário mensal, despesas e gráficos para mostrar, de forma simples, para onde vai o dinheiro.

Esta aplicação é de uso individual, sem login, e consome a API Java/Spring Boot pelo caminho `/api`. Os dados financeiros da versão web ficam no PostgreSQL, por meio do backend.

## Funcionalidades

- Visão geral por mês, com salário, total de gastos, saldo e últimos lançamentos.
- Cadastro, edição e exclusão de despesas com descrição, valor em reais, data e categoria.
- Despesas recorrentes mensais, com edição ou cancelamento da recorrência e preservação dos lançamentos anteriores.
- Salário com mês de início de vigência e histórico das alterações.
- Gráfico de distribuição dos gastos por categoria e evolução acumulada com uma linha por categoria, até a data atual.
- Categorias prontas para seleção e cores editáveis pelo seletor ou por código hexadecimal.
- Temas claro e escuro, com cores suaves na interface e cores vibrantes nos gráficos.
- Layout responsivo e navegação entre visão geral, gastos, salário e categorias.

O catálogo possui dez nomes fixos: **Moradia, Mercado, Carro, Restaurantes, Lojas, Saúde, Educação, Lazer, Assinaturas e Outros**. A cor de cada categoria acompanha os gastos, os gráficos e as legendas nos dois temas. Os nomes não são cadastrados, renomeados ou excluídos pela interface.

## Tecnologias

Versões declaradas no [`package.json`](package.json); as resoluções das dependências estão no [`package-lock.json`](package-lock.json).

| Tecnologia | Versão declarada / uso |
| --- | --- |
| Angular | `^22.1.0` — componentes, formulários, roteamento e HTTP |
| Angular CLI / Build | `^22.1.6` — desenvolvimento, build e testes |
| TypeScript | `~6.0.2` |
| RxJS | `~7.8.0` — comunicação e atualização dos dados |
| Lucide | `^1.72.0` — ícones incluídos no pacote da aplicação |
| Vitest / jsdom | `^4.0.8` / `^28.0.0` — testes unitários |
| npm | `12.2.2`, informado em `packageManager` |
| Node.js | Série `26`, utilizada no Dockerfile |

Os gráficos são desenhados em SVG. Os estilos ficam no projeto; a pilha tipográfica é `Inter`, `Segoe UI`, `Arial` e `sans-serif`, usando as fontes disponíveis no ambiente. Não há carregamento de fontes ou ícones por CDN.

## Desenvolvimento local

### Pré-requisitos

- Node.js 26 e npm.
- API do CONTAMIL em execução, com seu PostgreSQL configurado.
- Docker Desktop com o mecanismo Linux ativo, caso use a infraestrutura da raiz.

Os exemplos abaixo usam PowerShell e partem da raiz do repositório completo, onde estão `frontend/`, `backend/` e `compose.yaml`. Não é necessário instalar o Angular CLI globalmente: os scripts usam a versão local do projeto.

### 1. Iniciar a API e o banco

Se estiver usando o Docker Compose do repositório, prepare o arquivo de configuração local na raiz:

```powershell
if (-not (Test-Path -LiteralPath .env)) {
    Copy-Item -LiteralPath .env.example -Destination .env
}
```

Edite `.env` e defina uma senha local em `DB_PASSWORD`. Em seguida, ainda na raiz:

```powershell
docker compose up --build -d database backend
```

A base da API é `http://127.0.0.1:8080/api` por padrão. O endpoint [GET /api/health](http://127.0.0.1:8080/api/health) verifica seu funcionamento. Para outras formas de executá-la, consulte a [documentação do backend](../backend/README.md) e o [contrato da API](../backend/API.md).

### 2. Iniciar o Angular

Em outro terminal, a partir da raiz:

```powershell
Set-Location frontend
npm ci
npm start
```

Abra [http://127.0.0.1:4200](http://127.0.0.1:4200). O servidor de desenvolvimento escuta somente no computador local e atualiza a página conforme o código muda.

Se a porta 4200 já estiver ocupada, use outra porta:

```powershell
npm start -- --port 4201
```

Nesse caso, abra [http://127.0.0.1:4201](http://127.0.0.1:4201).

### Comunicação com a API

O cliente HTTP em [`src/app/finance-api.ts`](src/app/finance-api.ts) usa URLs relativas, como `/api/expenses`. Durante o desenvolvimento, [`proxy.conf.json`](proxy.conf.json) encaminha `/api/**` para `http://127.0.0.1:8080`.

Se a API estiver em outro endereço ou porta, ajuste o `target` desse arquivo e reinicie `npm start`. Alterar `BACKEND_PORT` no Compose não modifica automaticamente o proxy de desenvolvimento. O frontend não usa um arquivo `.env` próprio para definir o endereço da API.

## Scripts

Execute os comandos nesta pasta, `frontend/`:

| Comando | Resultado |
| --- | --- |
| `npm ci` | Instala as dependências conforme o lockfile. |
| `npm start` | Inicia o servidor Angular com o proxy da API. |
| `npm run build` | Gera o build de produção em `dist/contamil/browser/`. |
| `npm run watch` | Recompila os arquivos em modo de desenvolvimento; não inicia um servidor HTTP. |
| `npm test` | Inicia o executor de testes unitários. |
| `npm test -- --watch=false` | Executa os testes uma vez, sem acompanhar alterações. |

Os testes atuais de [`finance-utils.spec.ts`](src/app/finance-utils.spec.ts) cobrem conversão de valores para centavos, validação de calendário, referência de data em São Paulo e acumulação das séries sem incluir gastos futuros. Esses testes de utilidades não precisam da API em execução.

## Aplicação completa com Docker

Para executar frontend, backend e banco juntos, use a configuração local `.env` preparada anteriormente e rode **na raiz do repositório**:

```powershell
docker compose up --build -d
docker compose ps
```

A interface fica em [http://127.0.0.1:4200](http://127.0.0.1:4200) por padrão. `FRONTEND_PORT` e `BACKEND_PORT`, definidos na configuração da raiz, controlam as portas locais publicadas pelo Compose.

O [`Dockerfile`](Dockerfile) compila o Angular com `node:26-alpine` e entrega os arquivos estáticos com `nginx:stable-alpine`. O [`nginx.conf`](nginx.conf) mantém a navegação da aplicação e encaminha `/api/` para `http://backend:8080` dentro da rede Docker. Assim, o navegador acessa interface e API pela mesma origem.

Para acompanhar os logs ou parar os serviços preservando o volume do banco:

```powershell
docker compose logs frontend
docker compose logs backend
docker compose down
```

Se a interface Docker já estiver usando a porta 4200 durante o desenvolvimento, use a porta alternativa do Angular ou pare apenas o frontend Docker, na raiz:

```powershell
docker compose stop frontend
```

## Persistência dos dados

| Informação | Onde fica salva na versão web |
| --- | --- |
| Salário e histórico, despesas, recorrências e cores das categorias | PostgreSQL, acessado pela API Spring Boot. No Compose, o banco usa um volume persistente. |
| Preferência de tema | `localStorage` do navegador, na chave `contamil-theme`, por origem acessada. Sem preferência salva, a interface inicia no tema claro. |

A versão web depende da API para consultar e salvar as informações financeiras. O [aplicativo mobile](../mobile/README.md) é um projeto separado com React Native/Expo e SQLite no aparelho; funciona de forma independente e não sincroniza seus dados com esta interface.

## Organização do código

```text
frontend/
├── src/
│   ├── app/
│   │   ├── app.ts / app.html       # Estrutura, navegação e tema
│   │   ├── app.routes.ts          # Rotas da aplicação
│   │   ├── pages.ts               # Visão geral, gastos, salário e categorias
│   │   ├── finance-api.ts         # Requisições HTTP para /api
│   │   ├── finance-store.ts       # Estado e atualização dos dados
│   │   ├── models.ts              # Tipos do contrato da API
│   │   ├── expense-dialog.ts      # Formulários de gastos e recorrências
│   │   ├── expense-table.ts       # Lista de lançamentos
│   │   ├── spending-charts.ts     # Gráficos por categoria
│   │   └── finance-utils*.ts      # Utilidades financeiras e testes
│   ├── main.ts                   # Inicialização do Angular
│   └── styles.css                # Estilos, temas e responsividade
├── public/                       # Ícones da aplicação
├── angular.json                  # Configuração de build e testes
├── proxy.conf.json               # Proxy da API no desenvolvimento
├── Dockerfile
└── nginx.conf
```

Para a visão completa do CONTAMIL, consulte o [README principal](../README.md). A ideia original está em [IDEA.md](../IDEA.md), e as instruções para agentes IA estão em [Agente.md](../Agente.md).
