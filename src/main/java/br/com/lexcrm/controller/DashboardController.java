package br.com.lexcrm.controller;

import br.com.lexcrm.repository.ProcessoRepository;
import br.com.lexcrm.repository.FinanceiroRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;

@Controller
public class DashboardController {

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private FinanceiroRepository financeiroRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {

        model.addAttribute("activePage", "dashboard");

        BigDecimal totalMedicosPendentes = financeiroRepository.findByStatusAndDescricaoContaining("Pendente", "Médico")
                .stream()
                .map(f -> f.getValor() != null ? f.getValor() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        String medicosAPagarTotal = currencyFormat.format(totalMedicosPendentes);

        BigDecimal totalSeguradoraReceber = financeiroRepository.findByStatusAndDescricaoContaining("Pendente", "Seg")
                .stream()
                .map(f -> f.getValor() != null ? f.getValor() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String seguradoraAReceberTotal = currencyFormat.format(totalSeguradoraReceber);

        List<br.com.lexcrm.model.Processo> processos = processoRepository.findAllWithDetails();
        long processosConcluidos = processos.stream().filter(this::isConcluido).count();
        long processosAtivos = processos.stream().filter(p -> !isConcluido(p)).count();

        Map<String, Long> flowCounts = new HashMap<>();
        String[] flowKeys = new String[]{
                "etapa_cadastro_cliente",
                "etapa_documentos_solicitados",
                "etapa_documentos_recebidos",
                "etapa_analise_documental",
                "etapa_pendencia_documental",
                "etapa_aguardando_assinatura",
                "etapa_pericia_medica",
                "etapa_laudo_recebido",
                "etapa_enviado_seguradora",
                "etapa_analise_seguradora",
                "etapa_pendencia_seguradora",
                "etapa_sinistro_gerado",
                "etapa_processo_finalizado"
        };
        for (String k : flowKeys) {
            flowCounts.put(k, 0L);
        }
        for (br.com.lexcrm.model.Processo p : processos) {
            String currentKey = currentStageKey(p);
            if (currentKey != null && flowCounts.containsKey(currentKey)) {
                flowCounts.put(currentKey, flowCounts.get(currentKey) + 1);
            }
        }

        long pendenciasCriticasCount = flowCounts.get("etapa_pendencia_documental")
                + flowCounts.get("etapa_pendencia_seguradora")
                + flowCounts.get("etapa_documentos_solicitados");

        model.addAttribute("medicosAPagarTotal", medicosAPagarTotal);
        model.addAttribute("seguradoraAReceberTotal", seguradoraAReceberTotal);
        model.addAttribute("pendenciasCriticasCount", pendenciasCriticasCount);
        model.addAttribute("processosAtivosCount", processosAtivos);
        model.addAttribute("processosArquivadosCount", processosConcluidos);
        model.addAttribute("flowCounts", flowCounts);

        model.addAttribute("kpiBacklog", 318);
        model.addAttribute("kpiBacklogDelta", "+6%");
        model.addAttribute("kpiNewCases", 46);
        model.addAttribute("kpiNewCasesDelta", "+11%");
        model.addAttribute("kpiAvgCycle", "12,4d");
        model.addAttribute("kpiAvgCycleDelta", "-0,8d");
        model.addAttribute("kpiSla", "94,1%");
        model.addAttribute("kpiSlaBar", 94);
        model.addAttribute("kpiSlaTarget", "92%");
        model.addAttribute("kpiRevenueMtd", "R$ 128,4k");
        model.addAttribute("kpiRevenueDelta", "+9%");
        model.addAttribute("kpiMargin", "37,6%");
        model.addAttribute("kpiMarginDelta", "+1,2pp");
        model.addAttribute("kpiActiveAccounts", 62);
        model.addAttribute("kpiActiveAccountsDelta", "+3");
        model.addAttribute("kpiChurn", "1,1%");
        model.addAttribute("kpiChurnDelta", "+0,2pp");
        model.addAttribute("kpiOverdue", "3,2%");
        model.addAttribute("kpiOverdueDelta", "-0,4pp");
        model.addAttribute("kpiBillableHours", "412h");
        model.addAttribute("kpiBillableHoursDelta", "+8%");
        model.addAttribute("kpiUtilization", "79%");
        model.addAttribute("kpiUtilizationBar", 79);
        model.addAttribute("kpiCapacity", "520h");
        model.addAttribute("kpiRiskIndex", 68);
        model.addAttribute("kpiRiskDelta", "+5");

        return "dashboard/index";
    }

    @GetMapping("/dashboard/preview")
    public String preview(@RequestParam(required = false) String filtro, Model model) {
        String filtroKey = filtro == null ? "" : filtro.trim().toLowerCase();
        List<br.com.lexcrm.model.Processo> todos = processoRepository.findAllWithDetails();

        List<br.com.lexcrm.model.Processo> processos = switch (filtroKey) {
            case "pagar_medico" -> {
                var fins = financeiroRepository.findByStatusAndDescricaoContaining("Pendente", "Médico");
                var ids = fins.stream()
                        .filter(f -> f.getProcesso() != null && f.getProcesso().getId() != null)
                        .map(f -> f.getProcesso().getId())
                        .collect(java.util.stream.Collectors.toSet());
                yield todos.stream()
                        .filter(p -> p.getId() != null && ids.contains(p.getId()))
                        .limit(10)
                        .toList();
            }
            case "pendencias_criticas" -> todos.stream()
                    .filter(p -> {
                        String key = currentStageKey(p);
                        return "etapa_pendencia_documental".equals(key)
                                || "etapa_pendencia_seguradora".equals(key)
                                || "etapa_documentos_solicitados".equals(key);
                    })
                    .limit(10)
                    .toList();
            case "pendencias_docs" -> todos.stream()
                    .filter(p -> p.getEtapas() != null && p.getEtapas().stream()
                            .anyMatch(e -> (("Pendência Documental".equalsIgnoreCase(e.getNome())
                                    || "Documentos Solicitados".equalsIgnoreCase(e.getNome())
                                    || "Documentação".equalsIgnoreCase(e.getNome()))
                                    && !"Concluído".equalsIgnoreCase(e.getStatus()))))
                    .limit(10)
                    .toList();
            case "aguardando_laudo" -> todos.stream()
                    .filter(p -> p.getEtapas() != null && p.getEtapas().stream()
                            .anyMatch(e -> "Laudo Médico Recebido".equalsIgnoreCase(e.getNome())
                                    && !"Concluído".equalsIgnoreCase(e.getStatus())))
                    .limit(10)
                    .toList();
            case "aguardando_assinatura" -> todos.stream()
                    .filter(p -> p.getEtapas() != null && p.getEtapas().stream()
                            .anyMatch(e -> "Aguardando Assinatura".equalsIgnoreCase(e.getNome())
                                    && !"Concluído".equalsIgnoreCase(e.getStatus())))
                    .limit(10)
                    .toList();
            case "pericia_medica" -> todos.stream()
                    .filter(p -> p.getEtapas() != null && p.getEtapas().stream()
                            .anyMatch(e -> ("Perícia Médica".equalsIgnoreCase(e.getNome())
                                    || "Perícia".equalsIgnoreCase(e.getNome()))
                                    && !"Concluído".equalsIgnoreCase(e.getStatus())))
                    .limit(10)
                    .toList();
            case "seguradora" -> todos.stream()
                    .filter(p -> p.getEtapas() != null && p.getEtapas().stream()
                            .anyMatch(e -> ("Enviado para Seguradora".equalsIgnoreCase(e.getNome())
                                    || "Em Análise pela Seguradora".equalsIgnoreCase(e.getNome())
                                    || "Pendência com Seguradora".equalsIgnoreCase(e.getNome()))
                                    && !"Concluído".equalsIgnoreCase(e.getStatus())))
                    .limit(10)
                    .toList();
            case "ativos" -> todos.stream()
                    .filter(p -> !isConcluido(p))
                    .limit(10)
                    .toList();
            case "arquivados" -> todos.stream()
                    .filter(p -> "Concluído".equalsIgnoreCase(p.getStatus())
                            || (p.getEtapas() != null && p.getEtapas().stream()
                                    .anyMatch(e -> "Processo Finalizado".equalsIgnoreCase(e.getNome())
                                            && "Concluído".equalsIgnoreCase(e.getStatus()))))
                    .limit(10)
                    .toList();
            case "civil" -> todos.stream()
                    .filter(p -> p.getTipo() != null && "Cível".equalsIgnoreCase(p.getTipo()))
                    .limit(10)
                    .toList();
            case "etapa_cadastro_cliente", "etapa_documentos_solicitados", "etapa_documentos_recebidos",
                 "etapa_analise_documental", "etapa_pendencia_documental", "etapa_aguardando_assinatura",
                 "etapa_pericia_medica", "etapa_laudo_recebido", "etapa_enviado_seguradora",
                 "etapa_analise_seguradora", "etapa_pendencia_seguradora", "etapa_sinistro_gerado",
                 "etapa_processo_finalizado" -> todos.stream()
                    .filter(p -> filtroKey.equals(currentStageKey(p)))
                    .limit(10)
                    .toList();
            default -> todos.stream()
                    .limit(10)
                    .toList();
        };

        model.addAttribute("processos", processos);
        model.addAttribute("tituloLista", "Itens relacionados");
        model.addAttribute("linkVerTodos", filtroKey.isEmpty() ? "/processos" : "/processos?filtro=" + filtroKey);
        return "fragments/dashboard-list :: table";
    }

    private String currentStageKey(br.com.lexcrm.model.Processo processo) {
        if (processo == null || processo.getEtapas() == null || processo.getEtapas().isEmpty()) {
            return null;
        }

        br.com.lexcrm.model.EtapaProcesso current = null;
        for (br.com.lexcrm.model.EtapaProcesso e : processo.getEtapas()) {
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

    private boolean isConcluido(br.com.lexcrm.model.Processo processo) {
        String s = processo != null && processo.getStatus() != null ? processo.getStatus().trim().toLowerCase() : "";
        boolean statusConcluido = "concluído".equals(s) || "concluido".equals(s);
        boolean etapaFinalConcluida = processo != null
                && processo.getEtapas() != null
                && processo.getEtapas().stream().anyMatch(e ->
                    e != null
                            && e.getNome() != null
                            && e.getStatus() != null
                            && "processo finalizado".equalsIgnoreCase(e.getNome())
                            && "concluído".equalsIgnoreCase(e.getStatus()));
        return statusConcluido || etapaFinalConcluida;
    }
}
