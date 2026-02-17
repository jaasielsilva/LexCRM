package br.com.lexcrm.controller;

import br.com.lexcrm.model.*;
import br.com.lexcrm.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/processos")
public class ProcessoController {

    @Autowired
    private ProcessoRepository processoRepository;
    @Autowired
    private FinanceiroRepository financeiroRepository;
    @Autowired
    private EtapaProcessoRepository etapaProcessoRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private DocumentoChecklistRepository documentoChecklistRepository;
    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @GetMapping
    public String index(@RequestParam(required = false) String filtro,
            @RequestParam(required = false) Long clienteId,
            Model model) {
        List<Processo> processos;

        String filtroKey = filtro == null ? "" : filtro.trim().toLowerCase();
        List<Processo> todos = processoRepository.findAllWithDetails();

        processos = switch (filtroKey) {
            case "pagar_medico" -> {
                var fins = financeiroRepository.findByStatusAndDescricaoContaining("Pendente", "Médico");
                var ids = fins.stream()
                        .filter(f -> f.getProcesso() != null && f.getProcesso().getId() != null)
                        .map(f -> f.getProcesso().getId())
                        .collect(java.util.stream.Collectors.toSet());
                yield todos.stream()
                        .filter(p -> p.getId() != null && ids.contains(p.getId()))
                        .toList();
            }
            case "pendencias_criticas" -> todos.stream()
                    .filter(p -> {
                        String key = currentStageKey(p);
                        return "etapa_pendencia_documental".equals(key) || "etapa_pendencia_seguradora".equals(key);
                    })
                    .toList();
            case "pendencias_docs" -> todos.stream()
                    .filter(p -> p.getEtapas() != null && p.getEtapas().stream()
                            .anyMatch(e -> (("Pendência Documental".equalsIgnoreCase(e.getNome())
                                    || "Documentos Solicitados".equalsIgnoreCase(e.getNome())
                                    || "Documentação".equalsIgnoreCase(e.getNome()))
                                    && !"Concluído".equalsIgnoreCase(e.getStatus()))))
                    .toList();
            case "aguardando_laudo" -> todos.stream()
                    .filter(p -> p.getEtapas() != null && p.getEtapas().stream()
                            .anyMatch(e -> "Laudo Médico Recebido".equalsIgnoreCase(e.getNome())
                                    && !"Concluído".equalsIgnoreCase(e.getStatus())))
                    .toList();
            case "aguardando_assinatura" -> todos.stream()
                    .filter(p -> p.getEtapas() != null && p.getEtapas().stream()
                            .anyMatch(e -> "Aguardando Assinatura".equalsIgnoreCase(e.getNome())
                                    && !"Concluído".equalsIgnoreCase(e.getStatus())))
                    .toList();
            case "pericia_medica" -> todos.stream()
                    .filter(p -> p.getEtapas() != null && p.getEtapas().stream()
                            .anyMatch(e -> ("Perícia Médica".equalsIgnoreCase(e.getNome())
                                    || "Perícia".equalsIgnoreCase(e.getNome()))
                                    && !"Concluído".equalsIgnoreCase(e.getStatus())))
                    .toList();
            case "seguradora" -> todos.stream()
                    .filter(p -> p.getEtapas() != null && p.getEtapas().stream()
                            .anyMatch(e -> ("Enviado para Seguradora".equalsIgnoreCase(e.getNome())
                                    || "Em Análise pela Seguradora".equalsIgnoreCase(e.getNome())
                                    || "Pendência com Seguradora".equalsIgnoreCase(e.getNome()))
                                    && !"Concluído".equalsIgnoreCase(e.getStatus())))
                    .toList();
            case "ativos" -> todos.stream()
                    .filter(p -> !isConcluido(p))
                    .toList();
            case "arquivados" -> todos.stream()
                    .filter(p -> "Concluído".equalsIgnoreCase(p.getStatus())
                            || (p.getEtapas() != null && p.getEtapas().stream()
                                    .anyMatch(e -> "Processo Finalizado".equalsIgnoreCase(e.getNome())
                                            && "Concluído".equalsIgnoreCase(e.getStatus()))))
                    .toList();
            case "civil" -> todos.stream()
                    .filter(p -> p.getTipo() != null && "Cível".equalsIgnoreCase(p.getTipo()))
                    .toList();
            case "etapa_cadastro_cliente", "etapa_documentos_solicitados", "etapa_documentos_recebidos",
                    "etapa_analise_documental", "etapa_pendencia_documental", "etapa_aguardando_assinatura",
                    "etapa_pericia_medica", "etapa_laudo_recebido", "etapa_enviado_seguradora",
                    "etapa_analise_seguradora", "etapa_pendencia_seguradora", "etapa_sinistro_gerado",
                    "etapa_processo_finalizado" ->
                todos.stream()
                        .filter(p -> filtroKey.equals(currentStageKey(p)))
                        .toList();
            default -> todos;
        };

        if (clienteId != null) {
            processos = todos.stream()
                    .filter(p -> p.getCliente() != null && clienteId.equals(p.getCliente().getId()))
                    .toList();
        }

        model.addAttribute("processos", processos);
        model.addAttribute("clientes", clienteRepository.findAll());
        model.addAttribute("advogados", usuarioRepository.findAll());
        model.addAttribute("filtro", filtro);
        model.addAttribute("clienteId", clienteId);
        return "processos/index";
    }

    private String currentStageKey(Processo processo) {
        if (processo == null || processo.getEtapas() == null || processo.getEtapas().isEmpty()) {
            return null;
        }

        EtapaProcesso current = null;
        for (EtapaProcesso e : processo.getEtapas()) {
            if (e == null) {
                continue;
            }
            if (!"Concluído".equalsIgnoreCase(e.getStatus())) {
                current = e;
                break;
            }
        }
        if (current == null) {
            current = processo.getEtapas().get(processo.getEtapas().size() - 1);
        }
        return stageKeyForName(current.getNome());
    }

    private String stageKeyForName(String nome) {
        if (nome == null) {
            return null;
        }
        return switch (nome.trim().toLowerCase()) {
            case "cadastro do cliente" -> "etapa_cadastro_cliente";
            case "documentos solicitados", "documentação" -> "etapa_documentos_solicitados";
            case "documentos recebidos" -> "etapa_documentos_recebidos";
            case "análise documental", "analise documental" -> "etapa_analise_documental";
            case "pendência documental", "pendencia documental" -> "etapa_pendencia_documental";
            case "aguardando assinatura" -> "etapa_aguardando_assinatura";
            case "perícia médica", "pericia medica", "perícia" -> "etapa_pericia_medica";
            case "laudo médico recebido", "laudo medico recebido" -> "etapa_laudo_recebido";
            case "enviado para seguradora" -> "etapa_enviado_seguradora";
            case "em análise pela seguradora", "em analise pela seguradora" -> "etapa_analise_seguradora";
            case "pendência com seguradora", "pendencia com seguradora" -> "etapa_pendencia_seguradora";
            case "sinistro gerado" -> "etapa_sinistro_gerado";
            case "processo finalizado" -> "etapa_processo_finalizado";
            default -> null;
        };
    }

    private boolean isConcluido(Processo processo) {
        String s = processo != null && processo.getStatus() != null ? processo.getStatus().trim().toLowerCase() : "";
        boolean statusConcluido = "concluído".equals(s) || "concluido".equals(s);
        boolean etapaFinalConcluida = processo != null
                && processo.getEtapas() != null
                && processo.getEtapas().stream().anyMatch(e -> e != null
                        && e.getNome() != null
                        && e.getStatus() != null
                        && "processo finalizado".equalsIgnoreCase(e.getNome())
                        && "concluído".equalsIgnoreCase(e.getStatus()));
        return statusConcluido || etapaFinalConcluida;
    }

    @GetMapping("/pendentes/fragment")
    public String getProcessosPendentesFragment(Model model) {
        model.addAttribute("processos", processoRepository.findByStatus("Em Andamento"));
        return "processos/index :: list";
    }

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String query, Model model) {
        if (query != null && !query.isEmpty()) {
            model.addAttribute("processos", processoRepository.buscarPorTermo(query));
        } else {
            model.addAttribute("processos", processoRepository.findAll());
        }
        return "processos/index :: list";
    }

    @PostMapping("/novo")
    public String criarProcesso(Processo processo, Model model) {
        // Set defaults
        processo.setStatus("Em Andamento");
        processo.setDataAbertura(LocalDate.now());
        processo.setTenantId("T001"); // Fixed for now

        // Unique check for process number per tenant
        if (processo.getNumeroProcesso() != null
                && processoRepository.existsByNumeroProcessoAndTenantId(processo.getNumeroProcesso().trim(),
                        processo.getTenantId())) {
            model.addAttribute("processos", processoRepository.findAllWithDetails());
            model.addAttribute("clientes", clienteRepository.findAll());
            model.addAttribute("advogados", usuarioRepository.findAll());
            model.addAttribute("novoProcessoErro", "Já existe um processo com este número.");
            model.addAttribute("processoDraft", processo);
            return "processos/index";
        }

        List<String[]> etapasPadrao = Arrays.asList(
                new String[] { "Cadastro do Cliente", "Cliente cadastrado no sistema" },
                new String[] { "Documentos Solicitados", "Lista de documentos enviada ao cliente" },
                new String[] { "Documentos Recebidos", "Cliente enviou os documentos" },
                new String[] { "Análise Documental", "Análise dos documentos" },
                new String[] { "Pendência Documental", "Existem documentos pendentes" },
                new String[] { "Aguardando Assinatura", "Cliente precisa assinar documentos" },
                new String[] { "Perícia Médica", "Documentos enviados ao médico" },
                new String[] { "Laudo Médico Recebido", "Laudo médico recebido" },
                new String[] { "Enviado para Seguradora", "Processo enviado à seguradora" },
                new String[] { "Em Análise pela Seguradora", "Seguradora analisando" },
                new String[] { "Pendência com Seguradora", "Seguradora solicitou correção" },
                new String[] { "Sinistro Gerado", "Sinistro registrado" },
                new String[] { "Processo Finalizado", "Processo concluído" });

        for (int i = 0; i < etapasPadrao.size(); i++) {
            EtapaProcesso etapa = new EtapaProcesso();
            etapa.setNome(etapasPadrao.get(i)[0]);
            etapa.setDescricao(etapasPadrao.get(i)[1]);
            etapa.setOrdem(i + 1);
            etapa.setProcesso(processo);

            // A primeira etapa (Cadastro do Cliente) já começa "Em Andamento" com a data de
            // hoje
            if (i == 0) {
                etapa.setStatus("Em Andamento");
                etapa.setData(LocalDate.now());
            } else {
                etapa.setStatus("Pendente");
            }

            if ("Documentos Recebidos".equals(etapa.getNome())) {
                List<String> docs = Arrays.asList(
                        "RG ou CNH",
                        "Comprovante de residência",
                        "Carteira de trabalho (Digital/PDF)",
                        "Últimos 3 holerites antes do diagnóstico",
                        "Documentos médicos (Laudos, Tomografia, Raio-X, Prontuário)",
                        "Dados bancários para indenização",
                        "E-mail para envio de documentação",
                        "B.O ou C.A.T (caso tenha)");

                for (String docName : docs) {
                    DocumentoChecklist doc = new DocumentoChecklist();
                    doc.setNome(docName);
                    doc.setEntregue(false);
                    doc.setEtapa(etapa);
                    etapa.getChecklist().add(doc);
                }
            }

            processo.getEtapas().add(etapa);
        }

        processoRepository.save(processo);

        return "redirect:/processos?sucesso";
    }

    @GetMapping("/{id}/timeline")
    public String getTimeline(@PathVariable Long id, Model model) {
        Processo processo = processoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + id));

        // Adiciona atributo HTMX OOB para atualizar a barra de progresso
        model.addAttribute("processo", processo);
        model.addAttribute("processoSelecionado", processo);
        model.addAttribute("temPendenciaMedica", hasPendenciaMedica(processo.getId()));
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("hxOobSwap", true); // Flag auxiliar se necessário
        model.addAttribute("timelineOob", false);
        model.addAttribute("canEnviadoSeg", canEnviadoSeg(processo));

        // Retorna um template wrapper que inclui os dois fragmentos
        // Como o Thymeleaf normal retorna um único template, vamos retornar um template
        // especial 'fragments/htmx-updates'
        // Ou podemos concatenar manualmente se não quisermos criar um novo arquivo, mas
        // criar um arquivo é mais limpo.
        // Opção B: Retornar o timeline e adicionar o progress-bar como OOB no mesmo
        // response.
        // O Thymeleaf + HTMX geralmente requer que retornemos múltiplos fragmentos.
        // Vamos usar um fragmento wrapper simples.

        return "fragments/htmx-response-wrapper";
    }

    @PostMapping("/etapa/{etapaId}/reabrir")
    public String reabrirEtapa(@PathVariable Long etapaId,
            @RequestParam String password,
            jakarta.servlet.http.HttpServletResponse response,
            Model model) {
        EtapaProcesso etapa = etapaProcessoRepository.findById(etapaId).orElseThrow();
        Processo processo = etapa.getProcesso();

        // 1. Obter usuário logado
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        String currentPrincipalName = auth.getName();
        Usuario admin = usuarioRepository.findByUsername(currentPrincipalName).orElse(null);

        // 2. Validações de Segurança
        if (admin == null || admin.getRole() != Role.ADMIN) {
            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN);
            return null;
        }

        if (!passwordEncoder.matches(password, admin.getPassword())) {
            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        }

        // 3. Reabrir Etapa
        etapa.setStatus("Em Andamento");
        etapa.setData(LocalDate.now());
        etapa.setDescricao(etapa.getDescricao() + " (Reaberto em " + LocalDate.now() + ")");
        etapaProcessoRepository.save(etapa);

        // 4. Sincronizar Processo (Garante que volte para Em Andamento e reseta
        // finalização)
        processo.setStatus("Em Andamento");
        // Se o processo estava finalizado, reseta a etapa de finalização
        processo.getEtapas().stream()
                .filter(e -> "Processo Finalizado".equalsIgnoreCase(e.getNome()))
                .filter(e -> "Concluído".equalsIgnoreCase(e.getStatus()))
                .forEach(e -> {
                    e.setStatus("Pendente");
                    e.setData(null);
                    etapaProcessoRepository.save(e);
                });
        processoRepository.save(processo);

        // 5. Preparar Resposta OOB
        model.addAttribute("processo", processo);
        model.addAttribute("processoSelecionado", processo);
        model.addAttribute("temPendenciaMedica", hasPendenciaMedica(processo.getId()));
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("hxOobSwap", true);
        model.addAttribute("timelineOob", true);
        model.addAttribute("canEnviadoSeg", canEnviadoSeg(processo));

        // Retorna o fragmento específico do modal atual para atualizar o botão de
        // reabrir para salvar
        model.addAttribute("etapa", etapa);
        model.addAttribute("successMessage", "Etapa reaberta com sucesso!");

        String fragment = switch (etapa.getNome()) {
            case "Documentação", "Documentos Recebidos" -> "fragments/checklist-modal :: content";
            case "Aguardando Assinatura" -> "fragments/assinatura-modal :: content";
            case "Laudo Médico Recebido" -> "fragments/laudo-modal :: content";
            case "Enviado para Seguradora" -> "fragments/enviado-seg-modal :: content";
            default -> "fragments/generic-modal :: content";
        };

        if (fragment != null) {
            return fragment;
        }

        return "fragments/htmx-response-wrapper";
    }

    @PostMapping("/{processoId}/etapa/{etapaId}/toggle")
    public String toggleEtapa(@PathVariable Long processoId, @PathVariable Long etapaId, Model model) {
        EtapaProcesso etapa = etapaProcessoRepository.findById(etapaId).orElseThrow();

        // Se a etapa já estiver concluída, bloqueia o toggle (Segurança/Integridade)
        if ("Concluído".equalsIgnoreCase(etapa.getStatus())) {
            Processo processo = processoRepository.findById(processoId).orElseThrow();
            model.addAttribute("processo", processo);
            model.addAttribute("processoSelecionado", processo);
            model.addAttribute("temPendenciaMedica", hasPendenciaMedica(processo.getId()));
            model.addAttribute("today", LocalDate.now());
            model.addAttribute("hxOobSwap", true);
            model.addAttribute("canEnviadoSeg", canEnviadoSeg(processo));
            return "fragments/htmx-response-wrapper";
        }

        // Cycle Status
        switch (etapa.getStatus()) {
            case "Pendente":
                etapa.setStatus("Em Andamento");
                etapa.setData(LocalDate.now());
                break;
            case "Em Andamento":
                etapa.setStatus("Concluído");
                etapa.setData(LocalDate.now());
                break;
            case "Concluído":
                etapa.setStatus("Pendente");
                etapa.setData(null);
                break;
            default:
                etapa.setStatus("Pendente");
        }
        aplicarRegrasDeEtapaAoConcluir(etapa);
        etapaProcessoRepository.save(etapa);

        // Return the updated timeline for this process
        Processo processo = processoRepository.findById(processoId).orElseThrow();
        boolean finalizado = processo.getEtapas() != null && processo.getEtapas().stream()
                .anyMatch(e -> e != null
                        && e.getNome() != null
                        && "Processo Finalizado".equalsIgnoreCase(e.getNome())
                        && "Concluído".equalsIgnoreCase(e.getStatus()));
        if (finalizado) {
            processo.setStatus("Concluído");
        } else {
            boolean emAndamento = processo.getEtapas() != null && processo.getEtapas().stream()
                    .anyMatch(e -> "Em Andamento".equalsIgnoreCase(e.getStatus()));
            processo.setStatus(emAndamento ? "Em Andamento" : "Pendente");
        }
        processoRepository.save(processo);

        // Prepare response with both timeline and progress bar (OOB)
        model.addAttribute("processo", processo);
        model.addAttribute("processoSelecionado", processo);
        model.addAttribute("temPendenciaMedica", hasPendenciaMedica(processo.getId()));
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("hxOobSwap", true);
        model.addAttribute("canEnviadoSeg", canEnviadoSeg(processo));

        return "fragments/htmx-response-wrapper";
    }

    @PostMapping("/{processoId}/etapa/{etapaId}/checklist/{docId}/toggle")
    @jakarta.transaction.Transactional
    public String toggleChecklist(@PathVariable Long processoId,
            @PathVariable Long etapaId,
            @PathVariable Long docId,
            Model model) {

        // 1. Busca apenas a etapa (que já traz o checklist via EAGER)
        EtapaProcesso etapa = etapaProcessoRepository.findById(etapaId)
                .orElseThrow(() -> new IllegalArgumentException("Etapa não encontrada"));

        // 2. Validação de Segurança: A etapa pertence ao processo informado?
        if (!etapa.getProcesso().getId().equals(processoId)) {
            throw new IllegalArgumentException("Etapa não pertence ao processo informado");
        }

        // 3. Encontra o documento DENTRO da lista da etapa (garante consistência)
        DocumentoChecklist doc = etapa.getChecklist().stream()
                .filter(d -> d.getId().equals(docId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Documento não pertence a esta etapa"));

        // 4. Atualiza o estado
        doc.setEntregue(!doc.isEntregue());

        // 5. Recalcula status da etapa
        boolean allCompleted = etapa.getChecklist().stream().allMatch(DocumentoChecklist::isEntregue);

        if (allCompleted) {
            etapa.setStatus("Concluído");
            etapa.setData(LocalDate.now());
        } else {
            // Se estava concluído mas desmarcou um item, volta para Em Andamento (ou
            // Pendente se nenhum marcado)
            if ("Concluído".equals(etapa.getStatus())) {
                etapa.setStatus("Em Andamento");
            } else if (etapa.getChecklist().stream().noneMatch(DocumentoChecklist::isEntregue)) {
                etapa.setStatus("Pendente");
            } else {
                etapa.setStatus("Em Andamento");
            }
        }

        // 6. Salva apenas a etapa. O CascadeType.ALL salvará o documento
        // automaticamente.
        etapaProcessoRepository.save(etapa);

        // Removida a sincronização com 'Documentos Recebidos' (etapa oculta no fluxo)

        // Retorna o fragmento atualizado
        model.addAttribute("etapa", etapa);
        Processo processoAtualizado = processoRepository.findById(etapa.getProcesso().getId()).orElseThrow();
        model.addAttribute("processo", processoAtualizado);
        model.addAttribute("processoSelecionado", processoAtualizado);
        model.addAttribute("temPendenciaMedica", hasPendenciaMedica(etapa.getProcesso().getId()));
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("hxOobSwap", true);
        model.addAttribute("timelineOob", true);
        model.addAttribute("canEnviadoSeg", canEnviadoSeg(processoAtualizado));

        return "fragments/checklist-modal :: content";
    }

    @GetMapping("/etapa/{etapaId}/checklist")
    public String getChecklistModal(@PathVariable Long etapaId, Model model) {
        EtapaProcesso etapa = etapaProcessoRepository.findById(etapaId).orElseThrow();
        model.addAttribute("etapa", etapa);
        return "fragments/checklist-modal :: content";
    }

    @PostMapping("/etapa/{etapaId}/checklist/save")
    public String salvarChecklist(@PathVariable Long etapaId, Model model) {
        EtapaProcesso etapa = etapaProcessoRepository.findById(etapaId).orElseThrow();
        boolean allCompleted = etapa.getChecklist().stream().allMatch(DocumentoChecklist::isEntregue);
        if (allCompleted) {
            etapa.setStatus("Concluído");
            etapa.setData(LocalDate.now());
        } else if (etapa.getChecklist().stream().noneMatch(DocumentoChecklist::isEntregue)) {
            etapa.setStatus("Pendente");
            etapa.setData(null);
        } else {
            etapa.setStatus("Em Andamento");
            etapa.setData(LocalDate.now());
        }
        etapaProcessoRepository.save(etapa);

        // Removida a sincronização com 'Documentos Recebidos' (etapa oculta no fluxo)

        model.addAttribute("etapa", etapa);
        Processo processoAtualizado = processoRepository.findById(etapa.getProcesso().getId()).orElseThrow();
        model.addAttribute("processo", processoAtualizado);
        model.addAttribute("processoSelecionado", processoAtualizado);
        model.addAttribute("temPendenciaMedica", hasPendenciaMedica(etapa.getProcesso().getId()));
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("hxOobSwap", true);
        model.addAttribute("timelineOob", true);
        model.addAttribute("canEnviadoSeg", canEnviadoSeg(processoAtualizado));
        return "fragments/checklist-modal :: content";
    }

    @GetMapping("/etapa/{etapaId}/assinatura")
    public String getAssinaturaModal(@PathVariable Long etapaId, Model model) {
        EtapaProcesso etapa = etapaProcessoRepository.findById(etapaId).orElseThrow();
        model.addAttribute("etapa", etapa);
        return "fragments/assinatura-modal :: content";
    }

    @PostMapping("/etapa/{etapaId}/assinatura/enviar")
    public String enviarParaAssinatura(@PathVariable Long etapaId, Model model) {
        EtapaProcesso etapa = etapaProcessoRepository.findById(etapaId).orElseThrow();
        etapa.setStatus("Pendente"); // aguardando retorno do cliente
        etapa.setData(LocalDate.now());
        etapa.setDescricao("Documentação enviada ao cliente. Aguardando retorno.");
        etapaProcessoRepository.save(etapa);

        Processo processo = processoRepository.findById(etapa.getProcesso().getId()).orElseThrow();
        model.addAttribute("processo", processo);
        model.addAttribute("processoSelecionado", processo);
        model.addAttribute("temPendenciaMedica", hasPendenciaMedica(processo.getId()));
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("hxOobSwap", true);
        model.addAttribute("timelineOob", true);
        model.addAttribute("canEnviadoSeg", canEnviadoSeg(processo));
        model.addAttribute("etapa", etapa);
        model.addAttribute("successMessage", "Enviado ao cliente. Aguardando retorno.");
        return "fragments/assinatura-modal :: content";
    }

    @PostMapping("/etapa/{etapaId}/assinatura/anexar")
    public String anexarAssinatura(@PathVariable Long etapaId, Model model) {
        EtapaProcesso etapa = etapaProcessoRepository.findById(etapaId).orElseThrow();
        etapa.setStatus("Concluído");
        etapa.setData(LocalDate.now());
        etapa.setDescricao("Recebeu a documentação assinada");
        etapaProcessoRepository.save(etapa);

        Processo processo = processoRepository.findById(etapa.getProcesso().getId()).orElseThrow();
        model.addAttribute("processo", processo);
        model.addAttribute("processoSelecionado", processo);
        model.addAttribute("temPendenciaMedica", hasPendenciaMedica(processo.getId()));
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("hxOobSwap", true);
        model.addAttribute("timelineOob", true);
        model.addAttribute("canEnviadoSeg", canEnviadoSeg(processo));
        model.addAttribute("etapa", etapa);
        model.addAttribute("successMessage", "Assinatura confirmada com sucesso.");
        return "fragments/assinatura-modal :: content";
    }

    @GetMapping("/etapa/{etapaId}/laudo")
    public String getLaudoModal(@PathVariable Long etapaId, Model model) {
        EtapaProcesso etapa = etapaProcessoRepository.findById(etapaId).orElseThrow();
        Financeiro fin = financeiroRepository.findByStatusAndDescricaoContaining("Pendente", "Médico")
                .stream()
                .filter(f -> f.getProcesso() != null && f.getProcesso().getId().equals(etapa.getProcesso().getId()))
                .findFirst()
                .orElse(null);
        model.addAttribute("etapa", etapa);
        model.addAttribute("processo", etapa.getProcesso());
        model.addAttribute("financeiroPendente", fin);
        model.addAttribute("hxOobSwap", false);
        return "fragments/laudo-modal :: content";
    }

    @PostMapping("/etapa/{etapaId}/laudo/save")
    public String salvarLaudo(@PathVariable Long etapaId,
            @RequestParam String nomeMedico,
            @RequestParam String valorLaudo,
            @RequestParam(value = "arquivoLaudo", required = false) org.springframework.web.multipart.MultipartFile arquivoLaudo,
            Model model) {
        EtapaProcesso etapa = etapaProcessoRepository.findById(etapaId).orElseThrow();
        Processo processo = etapa.getProcesso();

        java.math.BigDecimal valor = parseBRL(valorLaudo);

        Financeiro fin = new Financeiro();
        fin.setDescricao("Médico - Laudo: " + nomeMedico);
        fin.setValor(valor);
        fin.setStatus("Pendente");
        fin.setProcesso(processo);
        fin.setTenantId(processo.getTenantId());
        financeiroRepository.save(fin);

        etapa.setStatus("Em Andamento");
        etapa.setData(LocalDate.now());
        etapa.setDescricao("Dados registrados. Aguardando pagamento do laudo.");
        etapaProcessoRepository.save(etapa);

        model.addAttribute("etapa", etapa);
        Processo p = processoRepository.findById(processo.getId()).orElseThrow();
        model.addAttribute("processo", p);
        model.addAttribute("processoSelecionado", p);
        model.addAttribute("temPendenciaMedica", hasPendenciaMedica(p.getId()));
        model.addAttribute("financeiroPendente", fin);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("hxOobSwap", true);
        model.addAttribute("timelineOob", true);
        model.addAttribute("canEnviadoSeg", canEnviadoSeg(p));
        return "fragments/laudo-modal :: content";
    }

    @PostMapping("/etapa/{etapaId}/laudo/pay")
    public String pagarLaudo(@PathVariable Long etapaId,
            @RequestParam Long fid,
            @RequestParam String dataPagamento,
            @RequestParam(value = "comprovante", required = false) org.springframework.web.multipart.MultipartFile comprovante,
            Model model) {
        EtapaProcesso etapa = etapaProcessoRepository.findById(etapaId).orElseThrow();
        Financeiro fin = financeiroRepository.findById(fid).orElseThrow();
        fin.setStatus("Pago");
        try {
            fin.setDataVencimento(java.time.LocalDate.parse(dataPagamento));
        } catch (Exception ignored) {
        }
        financeiroRepository.save(fin);

        // Agora sim concluímos a etapa
        etapa.setStatus("Concluído");
        etapa.setData(LocalDate.now());
        etapa.setDescricao("Laudo pago e registrado");
        etapaProcessoRepository.save(etapa);

        Processo processo = processoRepository.findById(etapa.getProcesso().getId()).orElseThrow();
        model.addAttribute("processo", processo);
        model.addAttribute("processoSelecionado", processo);
        model.addAttribute("temPendenciaMedica", hasPendenciaMedica(processo.getId()));
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("hxOobSwap", true);
        model.addAttribute("timelineOob", true);
        model.addAttribute("canEnviadoSeg", canEnviadoSeg(processo));
        model.addAttribute("etapa", etapa);
        model.addAttribute("successMessage", "Laudo registrado e pago com sucesso.");

        return "fragments/laudo-modal :: content";
    }

    private boolean hasPendenciaMedica(Long processoId) {
        return financeiroRepository.findByStatusAndDescricaoContaining("Pendente", "Médico")
                .stream()
                .anyMatch(f -> f.getProcesso() != null && f.getProcesso().getId().equals(processoId));
    }

    @GetMapping("/etapa/{etapaId}/seguradora")
    public String getSeguradoraModal(@PathVariable Long etapaId, Model model) {
        EtapaProcesso etapa = etapaProcessoRepository.findById(etapaId).orElseThrow();
        Financeiro fin = financeiroRepository.findByStatusAndDescricaoContaining("Pendente", "Seg")
                .stream()
                .filter(f -> f.getProcesso() != null && f.getProcesso().getId().equals(etapa.getProcesso().getId()))
                .findFirst()
                .orElse(null);
        model.addAttribute("etapa", etapa);
        model.addAttribute("processo", etapa.getProcesso());
        model.addAttribute("financeiroPendente", fin);
        model.addAttribute("hxOobSwap", false);
        return "fragments/seg-modal :: content";
    }

    @GetMapping("/etapa/{etapaId}/enviado-seg")
    public String getEnviadoSegModal(@PathVariable Long etapaId, Model model) {
        EtapaProcesso etapa = etapaProcessoRepository.findById(etapaId).orElseThrow();
        model.addAttribute("etapa", etapa);
        model.addAttribute("processo", etapa.getProcesso());
        model.addAttribute("hxOobSwap", false);
        return "fragments/enviado-seg-modal :: content";
    }

    @PostMapping("/etapa/{etapaId}/enviado-seg/sinistro")
    public String registrarSinistro(@PathVariable Long etapaId,
            @RequestParam String sinistroNumero,
            @RequestParam(value = "arquivo", required = false) org.springframework.web.multipart.MultipartFile arquivo,
            Model model) {
        EtapaProcesso enviado = etapaProcessoRepository.findById(etapaId).orElseThrow();
        Processo processo = processoRepository.findById(enviado.getProcesso().getId()).orElseThrow();
        processo.setSinistroNumero(sinistroNumero != null ? sinistroNumero.trim() : null);
        processoRepository.save(processo);

        EtapaProcesso sinistro = processo.getEtapas().stream()
                .filter(e -> e.getNome() != null && "Sinistro Gerado".equalsIgnoreCase(e.getNome()))
                .findFirst().orElse(null);
        if (sinistro != null) {
            sinistro.setStatus("Em Andamento");
            sinistro.setData(LocalDate.now());
            etapaProcessoRepository.save(sinistro);
        }
        enviado.setStatus("Concluído");
        enviado.setData(LocalDate.now());
        etapaProcessoRepository.save(enviado);

        model.addAttribute("processo", processo);
        model.addAttribute("processoSelecionado", processo);
        model.addAttribute("temPendenciaMedica", hasPendenciaMedica(processo.getId()));
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("hxOobSwap", true);
        model.addAttribute("timelineOob", true);
        model.addAttribute("canEnviadoSeg", canEnviadoSeg(processo));
        model.addAttribute("etapa", enviado);
        model.addAttribute("successMessage", "Sinistro registrado no processo.");
        return "fragments/enviado-seg-modal :: content";
    }

    @PostMapping("/etapa/{etapaId}/enviado-seg/aprovado")
    public String seguradoraAprovou(@PathVariable Long etapaId, Model model) {
        EtapaProcesso enviado = etapaProcessoRepository.findById(etapaId).orElseThrow();
        Processo processo = processoRepository.findById(enviado.getProcesso().getId()).orElseThrow();
        EtapaProcesso analise = processo.getEtapas().stream()
                .filter(e -> e.getNome() != null && "Em Análise pela Seguradora".equalsIgnoreCase(e.getNome()))
                .findFirst().orElse(null);
        if (analise != null) {
            analise.setStatus("Concluído");
            analise.setData(LocalDate.now());
            etapaProcessoRepository.save(analise);
        }
        enviado.setStatus("Concluído");
        enviado.setData(LocalDate.now());
        etapaProcessoRepository.save(enviado);

        model.addAttribute("processo", processo);
        model.addAttribute("processoSelecionado", processo);
        model.addAttribute("temPendenciaMedica", hasPendenciaMedica(processo.getId()));
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("hxOobSwap", true);
        model.addAttribute("timelineOob", true);
        model.addAttribute("canEnviadoSeg", canEnviadoSeg(processo));
        model.addAttribute("etapa", enviado);
        model.addAttribute("successMessage", "Aprovado pela seguradora.");
        return "fragments/enviado-seg-modal :: content";
    }

    @PostMapping("/etapa/{etapaId}/enviado-seg/pendencia")
    public String registrarPendenciaSeg(@PathVariable Long etapaId,
            @RequestParam String pendenciaDescricao,
            @RequestParam(value = "arquivo", required = false) org.springframework.web.multipart.MultipartFile arquivo,
            Model model) {
        EtapaProcesso enviado = etapaProcessoRepository.findById(etapaId).orElseThrow();
        Processo processo = processoRepository.findById(enviado.getProcesso().getId()).orElseThrow();
        EtapaProcesso pendSeg = processo.getEtapas().stream()
                .filter(e -> e.getNome() != null && "Pendência com Seguradora".equalsIgnoreCase(e.getNome()))
                .findFirst().orElse(null);
        if (pendSeg != null) {
            pendSeg.setStatus("Em Andamento");
            pendSeg.setData(LocalDate.now());
            if (pendenciaDescricao != null && !pendenciaDescricao.isBlank()) {
                pendSeg.setDescricao(pendenciaDescricao.trim());
            }
            etapaProcessoRepository.save(pendSeg);
        }
        enviado.setStatus("Concluído");
        enviado.setData(LocalDate.now());
        etapaProcessoRepository.save(enviado);

        model.addAttribute("processo", processo);
        model.addAttribute("processoSelecionado", processo);
        model.addAttribute("temPendenciaMedica", hasPendenciaMedica(processo.getId()));
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("hxOobSwap", true);
        model.addAttribute("timelineOob", true);
        model.addAttribute("canEnviadoSeg", canEnviadoSeg(processo));
        model.addAttribute("etapa", enviado);
        model.addAttribute("successMessage", "Pendência registrada junto à seguradora.");
        return "fragments/enviado-seg-modal :: content";
    }

    @PostMapping("/etapa/{etapaId}/seguradora/salvar")
    public String salvarRecebivelSeguradora(@PathVariable Long etapaId,
            @RequestParam String valorReceber,
            Model model) {
        EtapaProcesso etapa = etapaProcessoRepository.findById(etapaId).orElseThrow();
        Processo processo = etapa.getProcesso();
        java.math.BigDecimal valor = parseBRL(valorReceber);
        Financeiro fin = new Financeiro();
        fin.setDescricao("Seguradora - Receber");
        fin.setValor(valor);
        fin.setStatus("Pendente");
        fin.setProcesso(processo);
        fin.setTenantId(processo.getTenantId());
        financeiroRepository.save(fin);

        model.addAttribute("processo", processoRepository.findById(processo.getId()).orElseThrow());
        model.addAttribute("processoSelecionado", processo);
        model.addAttribute("temPendenciaMedica", hasPendenciaMedica(processo.getId()));
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("hxOobSwap", true);
        model.addAttribute("timelineOob", true);
        model.addAttribute("financeiroPendente", fin);
        model.addAttribute("etapa", etapa);
        model.addAttribute("successMessage", "Recebível criado.");
        model.addAttribute("canEnviadoSeg", canEnviadoSeg(processo));
        return "fragments/seg-modal :: content";
    }

    @PostMapping("/etapa/{etapaId}/seguradora/receber")
    public String receberSeguradora(@PathVariable Long etapaId,
            @RequestParam Long fid,
            @RequestParam String dataRecebimento,
            Model model) {
        EtapaProcesso etapa = etapaProcessoRepository.findById(etapaId).orElseThrow();
        Financeiro fin = financeiroRepository.findById(fid).orElseThrow();
        fin.setStatus("Pago");
        try {
            fin.setDataVencimento(java.time.LocalDate.parse(dataRecebimento));
        } catch (Exception ignored) {
        }
        financeiroRepository.save(fin);

        etapa.setStatus("Concluído");
        etapa.setData(LocalDate.now());
        etapaProcessoRepository.save(etapa);

        Processo processo = processoRepository.findById(etapa.getProcesso().getId()).orElseThrow();
        model.addAttribute("processo", processo);
        model.addAttribute("processoSelecionado", processo);
        model.addAttribute("temPendenciaMedica", hasPendenciaMedica(processo.getId()));
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("hxOobSwap", true);
        model.addAttribute("timelineOob", true);
        model.addAttribute("financeiroPendente", null);
        model.addAttribute("etapa", etapa);
        model.addAttribute("successMessage", "Recebimento registrado.");
        model.addAttribute("canEnviadoSeg", canEnviadoSeg(processo));
        return "fragments/seg-modal :: content";
    }

    @GetMapping("/etapa/{etapaId}/generic-modal")
    public String getGenericModal(@PathVariable Long etapaId, Model model) {
        EtapaProcesso etapa = etapaProcessoRepository.findById(etapaId).orElseThrow();
        model.addAttribute("etapa", etapa);
        model.addAttribute("processo", etapa.getProcesso());
        model.addAttribute("today", LocalDate.now());
        return "fragments/generic-modal :: content";
    }

    private java.math.BigDecimal parseBRL(String v) {
        if (v == null)
            return java.math.BigDecimal.ZERO;
        String s = v.replace("R$", "").replace(".", "").replace(",", ".").replaceAll("\\s+", "");
        try {
            return new java.math.BigDecimal(s);
        } catch (Exception e) {
            return java.math.BigDecimal.ZERO;
        }
    }

    private void aplicarRegrasDeEtapaAoConcluir(EtapaProcesso etapa) {
        if (!"Concluído".equalsIgnoreCase(etapa.getStatus())) {
            return;
        }
        String nome = etapa.getNome() != null ? etapa.getNome().trim().toLowerCase() : "";
        Processo p = etapa.getProcesso();
        java.util.function.Predicate<EtapaProcesso> isConcluida = e -> e != null
                && "Concluído".equalsIgnoreCase(e.getStatus());
        java.util.function.Function<String, EtapaProcesso> findByName = (n) -> p.getEtapas().stream()
                .filter(e -> e.getNome() != null && e.getNome().equalsIgnoreCase(n))
                .findFirst().orElse(null);

        if (nome.contains("análise documental") || nome.contains("analise documental")) {
            EtapaProcesso docsSolic = findByName.apply("Documentos Solicitados");
            if (docsSolic == null || !isConcluida.test(docsSolic)) {
                etapa.setStatus("Em Andamento");
                etapa.setData(LocalDate.now());
            }
        } else if (nome.contains("em análise pela seguradora") || nome.contains("em analise pela seguradora")) {
            EtapaProcesso enviado = findByName.apply("Enviado para Seguradora");
            if (enviado == null || !isConcluida.test(enviado)) {
                etapa.setStatus("Em Andamento");
                etapa.setData(LocalDate.now());
            }
        } else if (nome.contains("processo finalizado")) {
            boolean anterioresConcluidas = p.getEtapas().stream()
                    .filter(e -> e.getOrdem() != null && etapa.getOrdem() != null && e.getOrdem() < etapa.getOrdem())
                    .filter(e -> {
                        String n = e.getNome() != null ? e.getNome() : "";
                        // Ignora etapas que são ocultas ou opcionais se estiverem pendentes
                        boolean opcional = n.equalsIgnoreCase("Documentos Recebidos")
                                || n.equalsIgnoreCase("Pendência Documental")
                                || n.equalsIgnoreCase("Pendência com Seguradora");
                        return !opcional || "Concluído".equalsIgnoreCase(e.getStatus())
                                || "Em Andamento".equalsIgnoreCase(e.getStatus());
                    })
                    .allMatch(isConcluida);
            if (!anterioresConcluidas) {
                etapa.setStatus("Em Andamento");
                etapa.setData(LocalDate.now());
            }
        }
    }

    private boolean canEnviadoSeg(Processo p) {
        if (p == null || p.getEtapas() == null)
            return false;
        EtapaProcesso analise = p.getEtapas().stream()
                .filter(e -> e.getNome() != null && "Análise Documental".equalsIgnoreCase(e.getNome()))
                .findFirst().orElse(null);
        EtapaProcesso laudo = p.getEtapas().stream()
                .filter(e -> e.getNome() != null && "Laudo Médico Recebido".equalsIgnoreCase(e.getNome()))
                .findFirst().orElse(null);
        return analise != null && "Concluído".equalsIgnoreCase(analise.getStatus())
                && laudo != null && "Concluído".equalsIgnoreCase(laudo.getStatus());
    }
}
