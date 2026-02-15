package br.com.lexcrm.controller;

import br.com.lexcrm.repository.FinanceiroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/financeiro")
public class FinanceiroController {

    @Autowired
    private FinanceiroRepository financeiroRepository;

    @GetMapping("/medicos-pendentes/fragment")
    public String getMedicosPendentesFragment(Model model) {
        model.addAttribute("financeiros", financeiroRepository.findByStatusAndDescricaoContaining("Pendente", "Médico"));
        return "fragments/lista-financeiro :: lista";
    }
}
