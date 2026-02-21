package br.com.lexcrm.controller;

import br.com.lexcrm.model.Role;
import br.com.lexcrm.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;

@Controller
@RequestMapping("/configuracoes/permissoes")
public class PermissoesAdminController {

    private static final String[] ALL_PERMS = new String[] {
            "CLIENTES_CREATE","CLIENTES_EDIT","CLIENTES_DELETE","CLIENTES_VIEW","CLIENTES_EXPORT",
            "PROCESSOS_CREATE","PROCESSOS_EDIT","PROCESSOS_DELETE","PROCESSOS_VIEW","PROCESSOS_EXPORT",
            "FINANCEIRO_CREATE","FINANCEIRO_EDIT","FINANCEIRO_DELETE","FINANCEIRO_VIEW","FINANCEIRO_EXPORT",
            "RELATORIOS_CREATE","RELATORIOS_EDIT","RELATORIOS_DELETE","RELATORIOS_VIEW","RELATORIOS_EXPORT",
            "AGENDA_CREATE","AGENDA_EDIT","AGENDA_DELETE","AGENDA_VIEW","AGENDA_EXPORT",
            "CONFIG_CREATE","CONFIG_EDIT","CONFIG_DELETE","CONFIG_VIEW","CONFIG_EXPORT"
    };

    @Autowired
    private PermissionService permissionService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String index(@RequestParam(value = "salvo", required = false) String salvo, Model model) {
        model.addAttribute("activePage", "configuracoes");
        model.addAttribute("roles", Role.values());
        model.addAttribute("allPerms", Arrays.asList(ALL_PERMS));
        Map<Role, Set<String>> matrix = new LinkedHashMap<>();
        for (Role r : Role.values()) {
            matrix.put(r, permissionService.getAuthoritiesForRole(r));
        }
        model.addAttribute("matrix", matrix);
        if (salvo != null) {
            model.addAttribute("flashSuccessMessage", "Permissões salvas com sucesso.");
        }
        return "configuracoes/permissoes";
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String salvar(@RequestParam(name = "perm", required = false) List<String> perms, Model model) {
        Map<Role, Set<String>> newMatrix = new LinkedHashMap<>();
        if (perms != null) {
            for (String entry : perms) {
                String[] parts = entry.split(":", 2);
                if (parts.length != 2) continue;
                Role role = Role.valueOf(parts[0]);
                String perm = parts[1];
                newMatrix.computeIfAbsent(role, k -> new LinkedHashSet<>()).add(perm);
            }
        }
        for (Role r : Role.values()) {
            Set<String> set = newMatrix.getOrDefault(r, Collections.emptySet());
            permissionService.replacePermissions(r, set);
        }
        return "redirect:/configuracoes/permissoes?salvo=1";
    }
}
