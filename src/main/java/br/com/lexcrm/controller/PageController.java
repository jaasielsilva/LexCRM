package br.com.lexcrm.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/test-alert")
    public String testAlert() {
        return "test-alert";
    }

    @GetMapping({
            "/documentacoes",
            "/financeiro",
            "/relatorios",
            "/configuracoes"
    })
    @PreAuthorize("hasAnyAuthority('PROCESSOS_VIEW','FINANCEIRO_VIEW','RELATORIOS_VIEW','CONFIG_VIEW')")
    public String underConstruction(HttpServletRequest request, Model model) {
        String uri = request.getRequestURI();
        String activePage = uri.substring(1);
        model.addAttribute("activePage", activePage);
        if ("configuracoes".equalsIgnoreCase(activePage)) {
            return "configuracoes/index";
        }
        return "construction";
    }
}
