## Nova regra (padronização)
- Substituir modais/alerts “na mão” por SweetAlert2 v11 (Swal.fire / toast) como padrão do projeto.

## Padronização global (base do projeto)
1) Carregar SweetAlert2 v11 globalmente
- Incluir o CDN do SweetAlert2 em um fragmento comum usado em todas as páginas internas (ex.: no sidebar fragment) para evitar repetição.
- Criar um JS utilitário único (ex.: `static/js/ui-swal.js`) com funções padronizadas:
  - `uiToastSuccess(message)` / `uiToastError(message)`
  - `uiConfirmDelete({title, text})`
  - `uiFormModal({title, html, confirmText, onConfirm})`

2) Padronizar “alerts” (feedback)
- Padrão de feedback passa a ser:
  - Toast para sucesso/erro
  - Dialog para confirmação (delete)
- No backend, continuar usando `RedirectAttributes` para flash messages, mas a página vai ler esses valores e disparar `Swal.fire`/toast automaticamente (sem Bootstrap alert fixo na tela).

## Página /clientes (CRM)
3) Criar módulo real de Clientes
- Criar `ClienteController` com:
  - `GET /clientes` (lista)
  - `GET /clientes/search?query=` (HTMX/fragmento)
  - `POST /clientes` (criar)
  - `POST /clientes/{id}` (editar)
  - `POST /clientes/{id}/delete` (excluir)
- Remover `/clientes` do [PageController.java](file:///d:/Projetos/lexcrm/src/main/java/br/com/lexcrm/controller/PageController.java) (para não conflitar com a página real).

4) UI da página de Clientes (sem modal HTML próprio)
- Criar `templates/clientes/index.html` no padrão do projeto (sidebar + Bootstrap + Tailwind + CSS do módulo).
- “Novo Cliente” e “Editar” abrem um SweetAlert2 com formulário (campos: nome, cpf/cnpj, email, telefone).
  - Validação básica no `preConfirm` e no backend.
  - Submissão via `fetch` (POST) e, ao sucesso, atualizar a lista (recarregar fragmento ou reload da página) + toast.
- “Excluir” usa SweetAlert2 confirm e faz POST de delete.

5) Lista/Busca
- Implementar barra de busca com HTMX como em Processos:
  - Digitar filtra a lista (`hx-get=/clientes/search`)
  - `hx-target` aponta para um fragmento `clientes/index :: list`.

## Migração de modais existentes (para cumprir “em todo projeto”)
6) Processos (Novo Processo)
- Substituir o modal custom (openModal/closeModal) por SweetAlert2 com o mesmo formulário.
- Manter HTMX para busca/lista de processos como está (apenas troca a apresentação do modal).

7) Checklist modal (timeline)
- Trocar a abertura do modal custom por SweetAlert2.
- Carregar o conteúdo do checklist via `fetch`/HTMX e injetar no `html` do Swal.

8) Login alerts
- Substituir o Bootstrap alert do login por SweetAlert2 toast (erro de login/logout), mantendo compatibilidade com os parâmetros atuais.

## Arquivos que serão criados/alterados
- Novo: `templates/clientes/index.html` + fragmentos de lista.
- Novo: `static/css/clientes.css` (opcional, só para layout do módulo).
- Novo: `static/js/ui-swal.js` (helpers padronizados).
- Alterar: `fragments/sidebar.html` (incluir SweetAlert2 + ui-swal.js uma vez).
- Alterar: `PageController.java` (remover /clientes).
- Alterar: `ProcessoController`/`processos/index.html` (migrar modal de Novo Processo).
- Alterar: `timeline-view.html`/`checklist-modal` (migrar checklist modal).
- Alterar: `login.html` (migrar alert para toast).

## Validação
- Rodar `mvnw test`.
- Testar manualmente:
  - `/clientes` criar/editar/excluir + busca
  - `/processos` abrir “Novo Processo” via SweetAlert2
  - Timeline abre checklist via SweetAlert2
  - Login mostra toast em erro/logout
