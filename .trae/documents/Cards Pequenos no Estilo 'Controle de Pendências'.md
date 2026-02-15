## Objetivo
- Deixar os cards do Dashboard Executivo exatamente no estilo da imagem: vários cards pequenos, com borda/realce, ícone à direita e leitura rápida.
- Ao clicar em um card, mostrar um painel resumido abaixo e oferecer navegação para a lista filtrada ligada às etapas do processo.

## Ajustes Visuais (Dashboard)
- Refatorar o markup dos cards em [dashboard/index.html](file:///d:/Projetos/lexcrm/src/main/resources/templates/dashboard/index.html) para um “card KPI” consistente, sem depender de classes utilitárias.
- Corrigir a estrutura HTML atual (há tags de fechamento faltando no último card, o que pode quebrar o layout).
- Implementar CSS dedicado em [style.css](file:///d:/Projetos/lexcrm/src/main/resources/static/css/style.css) para reproduzir o visual do print:
  - Card compacto (altura menor, padding reduzido).
  - Borda 1px + sombra leve.
  - Faixa/realce colorido (ex.: `border-left` ou pseudo-elemento) para indicar categoria (bom/atenção/risco).
  - Ícone à direita dentro de um “badge” arredondado.
  - Tipografia: número/valor em destaque, label menor embaixo.
  - Estado ativo no clique (borda destacada como no print).

## Painel Resumido Abaixo dos Cards
- Manter o painel abaixo (já existe) e reestilizar para ficar no mesmo padrão do print:
  - Cabeçalho com título do card + pílula de status.
  - Resumo curto (1–2 linhas) + “Próximo passo”.
  - Link de ação (ex.: “Ver lista relacionada”) com aparência de ação secundária.

## Ligação com Etapas do Processo (o que você descreveu)
- Ajustar o fluxo padrão de etapas para refletir as etapas reais que você listou (ex.: Cadastro do Cliente, Documentos Solicitados, Documentos Recebidos, Análise Documental, Pendência Documental, Aguardando Assinatura, Perícia Médica, Laudo Médico Recebido, Enviado para Seguradora, Em Análise pela Seguradora, Pendência com Seguradora, Sinistro Gerado, Processo Finalizado).
- Atualizar o ponto onde o sistema cria etapas padrão (em [ProcessoController](file:///d:/Projetos/lexcrm/src/main/java/br/com/lexcrm/controller/ProcessoController.java)) para usar esse novo fluxo.
- Adaptar a timeline em [timeline-view.html](file:///d:/Projetos/lexcrm/src/main/resources/templates/fragments/timeline-view.html) para que os nomes/descrições batam com as novas etapas (incluindo o estágio que abre checklist/modal, se aplicável).

## Filtros por Card (navegação para listas)
- Padronizar filtros via querystring (ex.: `/processos?filtro=pendencias_docs`, `/processos?filtro=aguardando_laudo` etc.).
- Implementar filtros no backend para cada card, baseado em etapa + status:
  - Ex.: “Pendências (Docs)” → processos com etapa “Pendência Documental” ≠ Concluído.
  - Ex.: “Aguardando Laudo” → processos com etapa “Laudo Médico Recebido” ≠ Concluído.
  - Ex.: “Contratos a Assinar” → processos com etapa “Aguardando Assinatura” ≠ Concluído.
  - Ex.: “Processos Ativos” → status geral Pendente/Em Andamento.
- Ajustar [DashboardController](file:///d:/Projetos/lexcrm/src/main/java/br/com/lexcrm/controller/DashboardController.java) para calcular contagens reais com base nas etapas/status (tirando valores fixos como 42), e alimentar os cards.

## (Opcional) Prévia abaixo (estilo da imagem de “controle”)
- Quando clicar num card, além do resumo, carregar uma pequena tabela (3–10 itens) relacionada ao filtro do card (via HTMX), para ficar idêntico ao padrão “cards em cima, lista embaixo”.

## Validação
- Rodar testes (`mvnw test`).
- Conferir layout do dashboard em diferentes larguras (desktop/tablet), e verificar se os filtros por card retornam as listas corretas.
- Verificar que processos já existentes continuam navegáveis; se necessário, criar uma rotina de migração simples para preencher etapas faltantes no novo padrão.
