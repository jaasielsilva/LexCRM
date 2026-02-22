package br.com.lexcrm.controller;

import br.com.lexcrm.model.AccessPolicy;
import br.com.lexcrm.repository.AccessPolicyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/configuracoes/politicas")
public class AccessPolicyController {

    private static final String TENANT_FIXO = "T001";

    @Autowired
    private AccessPolicyRepository accessPolicyRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String index(Model model) {
        model.addAttribute("activePage", "configuracoes");
        AccessPolicy policy = accessPolicyRepository.findFirstByTenantId(TENANT_FIXO)
                .orElseGet(() -> {
                    AccessPolicy p = new AccessPolicy();
                    p.setTenantId(TENANT_FIXO);
                    p.setMfaEnabled(false);
                    p.setMaxLoginAttempts(5);
                    p.setPasswordExpiryDays(90);
                    return accessPolicyRepository.save(p);
                });
        model.addAttribute("policy", policy);
        return "configuracoes/politicas";
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String salvar(@RequestParam(name = "mfaEnabled", required = false) String mfaEnabled,
                         @RequestParam(name = "maxLoginAttempts", required = false) Integer maxLoginAttempts,
                         @RequestParam(name = "passwordExpiryDays", required = false) Integer passwordExpiryDays,
                         Model model) {
        AccessPolicy policy = accessPolicyRepository.findFirstByTenantId(TENANT_FIXO)
                .orElseGet(() -> {
                    AccessPolicy p = new AccessPolicy();
                    p.setTenantId(TENANT_FIXO);
                    return p;
                });
        policy.setMfaEnabled(mfaEnabled != null);
        policy.setMaxLoginAttempts(maxLoginAttempts != null ? maxLoginAttempts : 5);
        policy.setPasswordExpiryDays(passwordExpiryDays != null ? passwordExpiryDays : 90);
        accessPolicyRepository.save(policy);
        model.addAttribute("flashSuccessMessage", "Políticas de acesso atualizadas com sucesso.");
        return "redirect:/configuracoes/politicas";
    }
}

