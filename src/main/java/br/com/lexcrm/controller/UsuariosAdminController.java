package br.com.lexcrm.controller;

import br.com.lexcrm.model.Role;
import br.com.lexcrm.model.Usuario;
import br.com.lexcrm.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/configuracoes/usuarios")
public class UsuariosAdminController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String index(Model model) {
        model.addAttribute("activePage", "configuracoes");
        model.addAttribute("usuarios", usuarioRepository.findAll());
        model.addAttribute("roles", Role.values());
        return "configuracoes/usuarios";
    }

    @PostMapping("/criar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> criar(@RequestParam String username,
                                                     @RequestParam String senha,
                                                     @RequestParam String nomeCompleto,
                                                     @RequestParam Role role) {
        Map<String, Object> resp = new HashMap<>();
        if (username == null || username.trim().isEmpty() || senha == null || senha.isEmpty()) {
            resp.put("ok", false);
            resp.put("message", "Informe usuário e senha.");
            return ResponseEntity.badRequest().body(resp);
        }
        if (usuarioRepository.findByUsername(username).isPresent()) {
            resp.put("ok", false);
            resp.put("message", "Já existe um usuário com este username.");
            return ResponseEntity.badRequest().body(resp);
        }
        Usuario u = new Usuario();
        u.setUsername(username.trim());
        u.setPassword(passwordEncoder.encode(senha));
        u.setNomeCompleto(nomeCompleto != null ? nomeCompleto.trim() : null);
        u.setRole(role);
        u.setTenantId("T001");
        u.setAtivo(true);
        usuarioRepository.save(u);
        resp.put("ok", true);
        resp.put("message", "Usuário criado com sucesso.");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/{id}/editar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> editar(@PathVariable Long id,
                                                      @RequestParam String username,
                                                      @RequestParam(required = false) String senha,
                                                      @RequestParam String nomeCompleto,
                                                      @RequestParam Role role) {
        Map<String, Object> resp = new HashMap<>();
        Usuario u = usuarioRepository.findById(id).orElse(null);
        if (u == null) {
            resp.put("ok", false);
            resp.put("message", "Usuário não encontrado.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(resp);
        }
        if (username == null || username.trim().isEmpty()) {
            resp.put("ok", false);
            resp.put("message", "Informe um username.");
            return ResponseEntity.badRequest().body(resp);
        }
        usuarioRepository.findByUsername(username.trim()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                resp.put("ok", false);
                resp.put("message", "Já existe um usuário com este username.");
            }
        });
        if (resp.containsKey("ok") && !(Boolean) resp.get("ok")) {
            return ResponseEntity.badRequest().body(resp);
        }
        boolean eraAdminAtivo = u.getRole() == Role.ADMIN && u.isAtivo();
        if (eraAdminAtivo && role != Role.ADMIN) {
            long adminsAtivos = usuarioRepository.countByRoleAndAtivo(Role.ADMIN, true);
            if (adminsAtivos <= 1) {
                resp.put("ok", false);
                resp.put("message", "É necessário manter pelo menos um ADMIN ativo.");
                return ResponseEntity.badRequest().body(resp);
            }
        }
        u.setUsername(username.trim());
        u.setNomeCompleto(nomeCompleto != null ? nomeCompleto.trim() : null);
        u.setRole(role);
        if (senha != null && !senha.isBlank()) {
            u.setPassword(passwordEncoder.encode(senha));
        }
        usuarioRepository.save(u);
        resp.put("ok", true);
        resp.put("message", "Usuário atualizado com sucesso.");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/{id}/toggle-ativo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> toggleAtivo(@PathVariable Long id, Principal principal) {
        Map<String, Object> resp = new HashMap<>();
        Usuario u = usuarioRepository.findById(id).orElse(null);
        if (u == null) {
            resp.put("ok", false);
            resp.put("message", "Usuário não encontrado.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(resp);
        }
        if (principal != null && principal.getName() != null && principal.getName().equals(u.getUsername())) {
            resp.put("ok", false);
            resp.put("message", "Você não pode bloquear a própria conta.");
            return ResponseEntity.badRequest().body(resp);
        }
        if (u.getRole() == Role.ADMIN && u.isAtivo()) {
            long adminsAtivos = usuarioRepository.countByRoleAndAtivo(Role.ADMIN, true);
            if (adminsAtivos <= 1) {
                resp.put("ok", false);
                resp.put("message", "É necessário manter pelo menos um ADMIN ativo.");
                return ResponseEntity.badRequest().body(resp);
            }
        }
        boolean novoStatus = !u.isAtivo();
        u.setAtivo(novoStatus);
        usuarioRepository.save(u);
        resp.put("ok", true);
        resp.put("ativo", novoStatus);
        resp.put("message", novoStatus ? "Usuário ativado com sucesso." : "Usuário bloqueado com sucesso.");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/{id}/excluir")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> excluir(@PathVariable Long id, Principal principal) {
        Map<String, Object> resp = new HashMap<>();
        Usuario u = usuarioRepository.findById(id).orElse(null);
        if (u == null) {
            resp.put("ok", false);
            resp.put("message", "Usuário não encontrado.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(resp);
        }
        if (principal != null && principal.getName() != null && principal.getName().equals(u.getUsername())) {
            resp.put("ok", false);
            resp.put("message", "Você não pode excluir a própria conta.");
            return ResponseEntity.badRequest().body(resp);
        }
        if (u.getRole() == Role.ADMIN && u.isAtivo()) {
            long adminsAtivos = usuarioRepository.countByRoleAndAtivo(Role.ADMIN, true);
            if (adminsAtivos <= 1) {
                resp.put("ok", false);
                resp.put("message", "É necessário manter pelo menos um ADMIN ativo.");
                return ResponseEntity.badRequest().body(resp);
            }
        }
        usuarioRepository.delete(u);
        resp.put("ok", true);
        resp.put("message", "Usuário excluído com sucesso.");
        return ResponseEntity.ok(resp);
    }
}
