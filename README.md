# ContaMil
Aplicativo de finanças pessoais para organizar salário, gastos e despesas recorrentes, com gráficos por categoria, cores personalizáveis e temas claro e escuro. Versões web e mobile, com armazenamento local no celular e funcionamento offline.
=======
# CONTAMIL

Aplicativo de finanças pessoais para uso individual. Informe seu salário, registre despesas e acompanhe o orçamento mensal com gráficos por categoria.

O projeto oferece uma versão web com Angular, Spring Boot e PostgreSQL e um aplicativo Android/iOS com React Native, Expo e SQLite. O mobile salva seus próprios dados no aparelho e funciona de forma independente da versão web.

## Partes do projeto

| Parte | Tecnologias | Responsabilidade | Documentação |
| --- | --- | --- | --- |
| Frontend web | Angular 22 e TypeScript | Interface no navegador, integrada à API | [frontend/README.md](frontend/README.md) |
| Backend | Java 25, Spring Boot 4.1.2 e PostgreSQL 18 | API REST, regras financeiras e persistência da versão web | [backend/README.md](backend/README.md) |
| Mobile | React Native, Expo e SQLite | Aplicativo Android/iOS com persistência local, sem chamadas à API | [mobile/README.md](mobile/README.md) |

## Funcionalidades

- Salário mensal em reais, com alterações a partir do mês escolhido e histórico preservado.
- Registro, edição e exclusão de gastos com descrição, valor, data e categoria.
- Despesas recorrentes mensais, com recuperação de vencimentos pendentes sem duplicação.
- Recorrências dos dias 29, 30 e 31 ajustadas ao último dia dos meses mais curtos, mantendo o dia original nos meses seguintes.
- Categorias com nomes fixos e cores editáveis, usadas também nas legendas e nos gráficos.
- Visão mensal com salário, gastos, saldo, distribuição por categoria e uma linha de evolução para cada categoria.
- Temas claro e escuro, com troca manual e preferência persistida.

As categorias são **Moradia, Mercado, Carro, Restaurantes, Lojas, Saúde, Educação, Lazer, Assinaturas e Outros**. A seleção é feita por lista; não há cadastro ou renomeação de categorias.

O uso é individual, sem login. A configuração da versão web está preparada para execução local.

## Executar a versão web com Docker

Requisito: Docker com suporte a Docker Compose e contêineres Linux. No Windows, use o Docker Desktop com o mecanismo Linux em execução.

Na raiz do projeto, crie o arquivo de configuração caso ainda não exista:

```powershell
if (-not (Test-Path -LiteralPath .env)) {
    Copy-Item -LiteralPath .env.example -Destination .env
}
```

Edite `.env` e defina sua senha local em `DB_PASSWORD`. O arquivo `.env.example` contém apenas valores de exemplo; `.env` fica fora do versionamento.

Inicie os serviços:

```powershell
docker compose up --build -d
```

- Interface web: [http://localhost:4200](http://localhost:4200).
- Base da API REST: `http://localhost:8080/api`; verificação em [GET /api/health](http://localhost:8080/api/health).
- Contrato e endpoints: [backend/API.md](backend/API.md).

O PostgreSQL usa o volume persistente `postgres_data` e não publica uma porta para acesso pelo host. Interface e API ficam vinculadas a `127.0.0.1`. As portas da aplicação podem ser alteradas por `FRONTEND_PORT` e `BACKEND_PORT` no `.env`.

O primeiro acesso começa sem lançamentos financeiros fictícios. Informe o salário e adicione os gastos pela interface.

Para consultar os serviços, acompanhar logs e parar a aplicação mantendo o volume:

```powershell
docker compose ps
docker compose logs backend frontend
docker compose down
```

As instruções para desenvolvimento com Node.js ou Maven estão nos READMEs de [frontend](frontend/README.md) e [backend](backend/README.md).

## Executar o aplicativo mobile

O aplicativo mobile não depende dos serviços Docker. Na raiz do projeto:

```powershell
cd mobile
npm ci
npm start
```

O comando inicia o Expo em `localhost`. Consulte o [README mobile](mobile/README.md) para abrir no Expo Go ou em um emulador, executar testes e exportar os bundles.

Os dados mobile ficam em um banco SQLite local e não são sincronizados com a versão web. A exportação de bundles não gera um instalável APK/IPA.

## Estrutura

```text
CONTAMIL/
├── backend/          # API Java e migrações do PostgreSQL
├── frontend/         # Aplicação Angular
├── mobile/           # Aplicativo React Native + Expo
├── compose.yaml      # Ambiente local da versão web
├── .env.example      # Exemplo de configuração do Docker Compose
├── IDEA.md           # Ideia original
└── Agente.md         # Instruções para agentes IA e decisões do projeto
```

## Desenvolvimento

Cada parte tem seus próprios comandos de instalação, testes e compilação. Consulte a documentação correspondente antes de executá-los.

A descrição original está em [IDEA.md](IDEA.md). Para trabalho assistido por agentes IA, leia [Agente.md](Agente.md), que registra as regras e decisões confirmadas.