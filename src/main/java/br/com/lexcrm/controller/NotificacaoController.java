package br.com.lexcrm.controller;

import br.com.lexcrm.model.Notificacao;
import br.com.lexcrm.service.NotificacaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/notificacoes")
public class NotificacaoController {
    private static final String TENANT_FIXO = "T001";

    @Autowired
    private NotificacaoService notificacaoService;

    @GetMapping("/unread")
    @ResponseBody
    public Map<String, Object> unread() {
        String username = notificacaoService.currentUsername();
        if (username == null) {
            return Map.of("count", 0, "items", List.of());
        }
        long count = notificacaoService.contarNaoLidasDoUsuario(TENANT_FIXO, username);
        List<Notificacao> items = notificacaoService.listarNaoLidasDoUsuario(TENANT_FIXO, username).stream().limit(10).toList();

        List<Map<String, Object>> dto = items.stream().map(n -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", n.getId());
            m.put("tipo", n.getTipo() != null ? n.getTipo().name() : "INFO");
            m.put("titulo", n.getTitulo());
            m.put("mensagem", n.getMensagem());
            m.put("link", n.getLink());
            m.put("createdAt", n.getCreatedAt() != null ? n.getCreatedAt().toString() : null);
            return m;
        }).toList();

        return Map.of("count", count, "items", dto);
    }

    @PostMapping("/{id}/read")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markRead(@PathVariable Long id) {
        String username = notificacaoService.currentUsername();
        boolean ok = notificacaoService.marcarComoLida(TENANT_FIXO, username, id);
        Map<String, Object> resp = new HashMap<>();
        resp.put("ok", ok);
        resp.put("message", ok ? "OK" : "Não foi possível marcar como lida.");
        return ok ? ResponseEntity.ok(resp) : ResponseEntity.badRequest().body(resp);
    }
}
