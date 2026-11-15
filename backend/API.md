# API do CONTAMIL

Backend local para uso individual, sem login. Valores monetários são números decimais em reais; datas usam `AAAA-MM-DD` e meses usam `AAAA-MM`. O catálogo fixo de categorias e suas cores iniciais foram aprovados pelo usuário. Nenhuma informação financeira de demonstração é inserida no banco.

## Execução e configuração

O Dockerfile compila e executa os testes antes de produzir a imagem. Sem Docker, use Java 25 e Maven 3.9 ou superior: `mvn verify` e `mvn spring-boot:run`.

| Variável | Uso | Padrão |
| --- | --- | --- |
| `DB_URL` | Conexão JDBC PostgreSQL | `jdbc:postgresql://localhost:5432/contamil` |
| `DB_USERNAME` | Usuário do banco | `contamil` |
| `DB_PASSWORD` | Senha do banco | Obrigatória |
| `PORT` | Porta HTTP | `8080` |
| `CONTAMIL_TIME_ZONE` | Fuso usado para o dia atual e a rotina diária | `America/Sao_Paulo` |

O Flyway cria o esquema no primeiro início e versiona alterações posteriores. O Hibernate somente valida o esquema. Os testes usam H2 em modo de compatibilidade PostgreSQL para verificar as regras e a API; a execução real usa PostgreSQL.

A V1 permanece intacta. A migração Java V2 valida todos os rótulos antigos antes de qualquer alteração de esquema, inclusive nos gastos excluídos e nas recorrências canceladas. Nomes conhecidos são convertidos em referências ao catálogo sem modificar valores, datas, identificadores, exclusões ou posição da geração. Qualquer nome fora do catálogo interrompe a migração com uma mensagem para confirmar seu mapeamento; o sistema não cria categorias, renomeia rótulos ou apaga dados para contornar isso.

## Endpoints

| Método | Caminho | Comportamento |
| --- | --- | --- |
| GET | `/api/health` | Retorna `{"status":"UP"}` após consultar o banco. |
| GET | `/api/categories` | Lista as dez categorias fixas, com `id`, `name` e `color`. |
| PATCH | `/api/categories/{id}/color` | Altera somente a cor. Corpo: `{"color":"#8B65ED"}`. |
| GET | `/api/salary?month=2026-11` | Salário aplicável ao mês e mês de início da versão. |
| PUT | `/api/salary?month=2026-11` | Define uma versão salarial a partir do mês. Corpo: `{"amount":5000.00}`. |
| GET | `/api/salaries` | Histórico das versões salariais, da mais recente para a mais antiga. |
| GET | `/api/expenses?month=2026-11` | Gastos visíveis do mês, ordenados por data e identificador decrescentes. |
| GET | `/api/expenses/{id}` | Consulta um gasto visível. |
| POST | `/api/expenses` | Registra um gasto e retorna `201`. |
| PUT | `/api/expenses/{id}` | Edita um gasto individualmente. |
| DELETE | `/api/expenses/{id}` | Exclui um gasto da visualização e retorna `204`. |
| GET | `/api/recurrences` | Lista recorrências ativas e canceladas. |
| GET | `/api/recurrences/{id}` | Consulta uma recorrência, inclusive após cancelamento. |
| POST | `/api/recurrences` | Cria uma recorrência mensal e retorna `201`. |
| PUT | `/api/recurrences/{id}` | Atualiza descrição, valor e categoria dos próximos lançamentos. |
| DELETE | `/api/recurrences/{id}` | Cancela novos lançamentos e retorna `204`. Preserva os anteriores. |
| GET | `/api/summary?month=2026-11` | Salário, total de gastos, saldo, totais por categoria e totais diários. |

Não existem endpoints para criar, renomear ou excluir categorias. O corpo do `PATCH` aceita somente `color`; campos como `name` são rejeitados. Cores usam exatamente seis dígitos hexadecimais, no formato `#RRGGBB`, com letras maiúsculas ou minúsculas.

| ID | Nome fixo | Cor inicial |
| --- | --- | --- |
| 1 | Moradia | `#8B65ED` |
| 2 | Mercado | `#19A5A1` |
| 3 | Carro | `#F1AB38` |
| 4 | Restaurantes | `#ED6E88` |
| 5 | Lojas | `#4C8DE7` |
| 6 | Saúde | `#3B9E6F` |
| 7 | Educação | `#6366F1` |
| 8 | Lazer | `#D946EF` |
| 9 | Assinaturas | `#06B6D4` |
| 10 | Outros | `#7C8795` |

Gastos e recorrências recebem `categoryId`, selecionado nessa lista. Texto livre no campo `category`, identificador inexistente e propriedades desconhecidas são rejeitados com HTTP `400`. Nas respostas, `category` é o objeto `{"id":2,"name":"Mercado","color":"#19A5A1"}`. A cor atual da categoria é usada inclusive para gastos antigos, recorrências e gráficos de meses anteriores; mudar a cor não altera a categoria nem os valores financeiros.

Gasto (`POST` e `PUT`):

```json
{"description":"Compra no mercado","amount":120.50,"date":"2026-11-11","categoryId":2}
```

Recorrência (`POST`):

```json
{"description":"Internet","amount":99.90,"startDate":"2026-11-10","categoryId":9}
```

Edição de recorrência (`PUT`):

```json
{"description":"Internet","amount":109.90,"categoryId":9}
```

O resumo retorna `month`, `salary`, `totalExpenses`, `balance` e:

- `byCategory: [{category: {id, name, color}, amount}]`: totais por categoria, do maior para o menor valor; empates usam o nome.
- `dailyExpenses: [{date, amount}]`: total de cada dia, preservado no contrato.
- `dailyByCategory: [{category: {id, name, color}, points: [{date, amount}]}]`: uma série para cada categoria com lançamentos no mês, na mesma ordem de `byCategory`.

Cada série contém todos os dias do mês, com zero nos dias sem gastos. Os valores são diários, sem acumulação; para exibir uma linha acumulada, a interface soma os pontos anteriores dentro de cada categoria e usa a cor fornecida. Gastos excluídos e gastos de outros meses não entram nessas séries. Sem salário informado, o valor é zero; em `/api/salary`, `effectiveMonth` será `null`, permitindo à interface identificar a ausência de configuração.

## Regras implementadas

- Cada versão salarial passa a valer no mês escolhido e permanece até a próxima versão. Alterar um mês não altera os meses anteriores.
- Recorrências mantêm a data inicial como âncora. Um lançamento iniciado no dia 31 usa 28 ou 29 em fevereiro e volta ao dia 31 em março.
- Os vencimentos são registrados até o dia atual ao iniciar o backend, à meia-noite no fuso configurado e ao consultar gastos, resumo ou recorrências.
- Antes de editar ou cancelar uma recorrência, o backend registra os vencimentos pendentes com a regra antiga. A mudança afeta os próximos vencimentos.
- Cada ocorrência copia descrição, valor e referência da categoria; alterações posteriores da regra não modificam esses lançamentos. A cor é consultada na categoria e permanece consistente em todos os gráficos.
- A exclusão de um gasto conserva internamente a identidade de sua ocorrência. Uma chave única no banco impede duplicação por recorrência e mês, mesmo quando o gasto é editado ou excluído.
- Categorias são escolhidas pelo identificador do catálogo fixo; somente suas cores podem ser alteradas.

Erros de entrada usam HTTP `400`, registros ausentes usam `404` e conflitos de persistência usam `409`, com mensagens em português. Valores aceitam até 12 dígitos inteiros e 2 casas decimais. Gastos devem ser maiores que zero; salário pode ser zero. Descrições têm limite de 180 caracteres.
