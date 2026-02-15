package br.com.lexcrm.controller;

import br.com.lexcrm.repository.ProcessoRepository;
import br.com.lexcrm.repository.FinanceiroRepository;
import br.com.lexcrm.repository.ClienteRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@Controller
public class DashboardController {

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private FinanceiroRepository financeiroRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {

        model.addAttribute("activePage", "dashboard");

        BigDecimal totalMedicosPendentes = financeiroRepository.findByStatusAndDescricaoContaining("Pendente", "Médico")
                .stream()
                .map(f -> f.getValor() != null ? f.getValor() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        String medicosAPagarTotal = currencyFormat.format(totalMedicosPendentes);

        long processosPendentes = processoRepository.countByStatus("Pendente");
        long processosEmAndamento = processoRepository.countByStatus("Em Andamento");
        long processosConcluidos = processoRepository.countByStatus("Concluído");

        model.addAttribute("medicosAPagarTotal", medicosAPagarTotal);
        model.addAttribute("pendenciasDocsCount", 42);
        model.addAttribute("contratosPendentesCount", 0);
        model.addAttribute("processosAtivosCount", processosPendentes + processosEmAndamento);
        model.addAttribute("processosArquivadosCount", processosConcluidos);

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
}
