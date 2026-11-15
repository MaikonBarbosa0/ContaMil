# Instruções para os agentes IA — CONTAMIL

## Leia antes de trabalhar

Leia este arquivo e o `IDEA.md` antes de iniciar qualquer tarefa. O `IDEA.md` descreve a ideia original do projeto. Siga também as decisões que o usuário confirmar na conversa; se houver conflito ou informação insuficiente, pergunte antes de agir.

## Regra principal do usuário

> Conforme surgir dúvida, pergunte. Não tome decisões por conta; pergunte antes de fazer.

- Não preencha lacunas dos requisitos com suposições.
- Antes de implementar algo que dependa de uma escolha ainda não confirmada, apresente a dúvida e aguarde a resposta do usuário.
- Quando ajudar, apresente opções concretas e explique brevemente suas diferenças. Uma sugestão não equivale a uma aprovação.
- Não considere silêncio, passagem do tempo ou ausência de preferência como autorização.
- Prossiga com leitura, análise e trabalho já autorizado que não dependa da resposta pendente.
- Não adicione funcionalidades, dependências, serviços ou mudanças de arquitetura que não tenham sido aprovados.
- Não instale ferramentas, publique o projeto ou altere serviços externos sem autorização para essas ações.
- Preserve arquivos e alterações existentes do usuário.
- Converse e escreva a documentação do projeto em português.

## Especificações já informadas

- **Nome:** CONTAMIL.
- **Objetivo:** ajudar o usuário a compreender suas finanças de maneira prática e simples.
- **Fluxo descrito:** o usuário informa seu salário e registra seus gastos.
- **Apresentação:** mostrar os gastos de maneira intuitiva, simples e visualmente bonita, com gráficos e imagens.
- **Backend:** Java com Spring Boot.
- **Frontend:** Angular.
- **Banco de dados:** PostgreSQL.

## Decisões confirmadas pelo usuário

Confirmadas em 11/11/2026:

- O aplicativo será de uso individual.
- O PLUS entra na primeira versão: categorias e despesas recorrentes.
- As despesas recorrentes devem gerar os lançamentos automaticamente.
- O salário será mensal, em reais (BRL).
- Cada gasto terá descrição, valor, data e categoria.
- O usuário não tem preferência de ferramentas adicionais ou versões. Isso não representa aprovação de uma proposta técnica ainda não apresentada.

Confirmadas na sequência da conversa:

- Base técnica aprovada: Java 25, Spring Boot 4.1.2, Maven, Angular 22 e PostgreSQL 18.
- Execução local pelo Docker Compose, com persistência dos dados entre reinicializações.
- Uso individual sem login.
- Alterações no salário passam a valer a partir do mês escolhido, preservando meses anteriores.
- Recorrências mensais continuam até serem canceladas. Quando o dia original não existir no mês, usar o último dia e manter o dia original nos meses seguintes.
- Gerar lançamentos recorrentes até a data atual, inclusive depois de períodos sem abrir o sistema, sem duplicação.
- Permitir editar e excluir gastos individualmente. Alterações e cancelamentos das recorrências afetam somente os próximos lançamentos e preservam o histórico.
- Incluir temas claro e escuro, com cores suaves na interface e cores vibrantes nos gráficos.
- Apresentar uma proposta visual para aprovação antes de implementar a interface. O usuário sugeriu opcionalmente o Figma; uma prévia ainda não constitui aprovação do design.

Confirmadas na revisão das categorias:

- No gasto, escolher a categoria em uma lista pronta; não permitir digitação livre de nomes.
- Nomes fixos: Moradia, Mercado, Carro, Restaurantes, Lojas, Saúde, Educação, Lazer, Assinaturas e Outros.
- Permitir editar apenas a cor de cada categoria, sem cadastro, renomeação ou exclusão de categorias.
- Gráficos e legendas devem usar a cor vinculada à categoria, mantendo a associação nos temas claro e escuro.
- O gráfico de evolução terá uma linha por categoria, usando a respectiva cor.
- Paleta inicial aprovada: Moradia roxo, Mercado turquesa, Carro laranja, Restaurantes rosa, Lojas azul, Saúde verde, Educação índigo, Lazer magenta, Assinaturas ciano e Outros cinza. As cores poderão ser alteradas pelo usuário.
- O usuário aprovou a prévia revisada e autorizou implementar a interface Angular. Em seguida dispensou a exportação em HTML e pediu para seguir com o projeto.

Os nomes fixos acima substituem a proposta anterior de categoria como texto livre. Os exemplos do `IDEA.md` continuam como referência da ideia original; a recorrência é uma característica do gasto, separada de sua categoria.

Confirmadas em 12/11/2026 para a versão mobile:

- Criar um aplicativo Android/iOS com React Native e Expo, em um projeto separado na pasta `mobile/`.
- Preservar a aplicação Angular existente e sua infraestrutura.
- Salvar todas as informações no próprio aparelho. O usuário dispensou expressamente qualquer requisição à API; a versão mobile funciona de forma independente, sem backend ou sincronização com a versão web.
- A persistência local usa SQLite (`contamil.db`), incluindo salário e histórico, gastos, recorrências, cores e tema. Valores financeiros são armazenados em centavos inteiros.
- Manter as regras financeiras, categorias fixas e proposta visual já aprovadas, adaptando a navegação e os controles ao aplicativo nativo.
- Gerar as recorrências vencidas ao abrir ou retomar o aplicativo e ao atualizar o dia enquanto ele estiver aberto, sem depender de servidor ou execução em segundo plano.
- Não adicionar cliente HTTP, endereço de API, autenticação, serviço de nuvem, sincronização ou publicação ao projeto mobile sem uma nova solicitação do usuário.

Essas decisões substituem qualquer proposta anterior de reutilizar a API no aplicativo mobile. O backend continua pertencendo à aplicação web existente.

## Decisões pendentes

Os itens abaixo são perguntas em aberto, não requisitos aprovados:

- Funcionalidades e mudanças visuais além da prévia aprovada.
- Preferência automática pelo tema do dispositivo, caso seja solicitada; a prévia aprovada inicia no tema claro e oferece troca manual.
- Qualquer implantação fora do ambiente local aprovado.

Não transforme nenhum desses itens em uma decisão sem consultar o usuário. Se surgir outra dúvida durante o desenvolvimento, aplique a mesma regra.

## Durante a implementação autorizada

- Implemente somente o escopo confirmado e mantenha a simplicidade solicitada.
- Registre as decisões aprovadas na documentação, sem misturá-las com propostas pendentes.
- Não inclua credenciais reais no código ou na documentação.
- Verifique o funcionamento das alterações com os recursos disponíveis e informe o que foi validado e o que não pôde ser executado.
- Ao concluir uma etapa, descreva de forma breve o que foi entregue e quais dúvidas ainda precisam de resposta.

## Estado atual

A base técnica e as regras principais foram aprovadas. A revisão das categorias fixas e cores editáveis está implementada no backend e validada com 24 testes automatizados e 119 verificações adicionais contra PostgreSQL real. A persistência das cores e dos dados financeiros também foi confirmada após reiniciar a API de teste. As migrações V1 e V2 estão aplicadas no banco local, sem lançamentos fictícios. A API e o banco rodam pelo Docker Compose.

A proposta visual inicial dos temas claro e escuro está no [Figma — CONTAMIL](https://www.figma.com/design/xNkDZXLlc1qsu2uDy9hgiG). A atualização do Figma foi bloqueada pelo limite de chamadas do plano Starter: o arquivo continua com a proposta anterior, sem as mudanças de categorias.

A prévia interativa revisada foi aprovada. A interface Angular está implementada e integrada à API, com navegação entre visão geral, gastos, salário e categorias; formulários reais, edição e cancelamento de recorrências, gráficos por categoria, seleção fixa de categorias, cores persistidas e temas claro/escuro com preferência salva no navegador.

O build da interface e quatro testes de utilidades passaram. O aplicativo completo também passou por 25 verificações no Chrome, usando um ambiente isolado com PostgreSQL, incluindo fluxos financeiros, persistência de cores e tema e layout de 320 pixels. O resultado é executado localmente pelo Docker Compose; as instruções estão no README.

Não presuma que o Figma foi atualizado sem verificar o resultado. Não solicite novamente a aprovação do layout já concedida. Não exporte um HTML de demonstração: o usuário dispensou essa exportação e pediu para seguir com o projeto.

A versão nativa está em `mobile/`, separada da interface Angular. Leia também `mobile/README.md` ao trabalhar nela. A validação por testes de componentes e exportação de bundles não equivale a executar o app em um aparelho real; informe essa limitação e não descreva bundles como APK ou IPA.

Na versão mobile, 17 testes do núcleo com SQLite real e seis testes de interface passaram, cobrindo também ausência de requisições de rede. A checagem TypeScript e a exportação dos bundles Android/iOS passaram. Não foi realizado teste em aparelho ou emulador, nem gerado um instalável APK/IPA.
