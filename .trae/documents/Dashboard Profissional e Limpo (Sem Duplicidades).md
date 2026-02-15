## Faz sentido ter tudo isso no detalhe da etapa?
- Para um dashboard profissional e limpo, não faz sentido repetir muita “explicação” (Valor atual, Tendência, Próximo passo) para TODA etapa, porque:
  - O card já mostra o número (contagem) e o nome da etapa.
  - O usuário quer ação rápida (ver a lista filtrada e trabalhar nos itens).
  - Texto longo em todas as etapas vira ruído visual.

## Ajuste de UX (o que vamos mudar)
### 1) Painel de detalhe enxuto para ETAPAS
- Quando clicar em uma etapa do Fluxo, o painel abaixo vai mostrar apenas:
  - Título da etapa + contagem
  - Tabela “Itens relacionados” (com botão “Ver todos”)
- Remover do painel (para etapas):
  - “Valor atual” (redundante com a contagem)
  - “Tendência”
  - “Próximo passo”
  - Textos longos de descrição (no máximo 1 linha opcional)

### 2) Painel de detalhe enxuto para KPIs (topo)
- Para KPIs executivos, manter no painel:
  - Título + valor
  - Tabela relacionada (quando fizer sentido)
- Se não houver tabela útil para um KPI (ex.: “Novos contatos”), o painel pode mostrar só o link de navegação.

### 3) Clique abre/fecha (toggle)
- Implementar o comportamento:
  - 1º clique abre o painel para aquele card.
  - 2º clique no mesmo card fecha o painel e remove o destaque.
  - Clique em outro card troca o filtro e mantém aberto.

### 4) Redução de duplicidade do topo vs Fluxo
- Para reduzir repetição, manter no topo apenas KPIs macro (não-etapa):
  - Novos Contatos, Processos Ativos, Finalizados/Arquivados, A Receber (Seg), A Pagar (Médicos), Pendências Críticas (agregado)
- Deixar “etapas” somente no Fluxo.

## Implementação (onde mexer)
- [dashboard/index.html](file:///d:/Projetos/lexcrm/src/main/resources/templates/dashboard/index.html)
  - Simplificar o HTML do painel (um header compacto + tabela)
  - Ajustar o JS do `toggleDetails` para suportar abrir/fechar
  - Remover/ocultar cards duplicados do topo
- [DashboardController.java](file:///d:/Projetos/lexcrm/src/main/java/br/com/lexcrm/controller/DashboardController.java)
  - KPI agregado “Pendências Críticas”
  - Garantir contagens e preview coerentes com o painel enxuto
- [fragments/dashboard-list.html](file:///d:/Projetos/lexcrm/src/main/resources/templates/fragments/dashboard-list.html)
  - Manter “Ver todos” no cabeçalho da tabela (já está), e padronizar colunas (ex.: formatar valor)
- [style.css](file:///d:/Projetos/lexcrm/src/main/resources/static/css/style.css)
  - Ajustar espaçamentos do painel para ficar mais compacto

## Resultado esperado
- Dashboard com menos texto repetido.
- Foco em: cards pequenos + lista filtrada.
- Interação natural: clicar abre e clicar de novo recolhe.