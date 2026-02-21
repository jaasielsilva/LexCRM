package br.com.lexcrm.service;

import br.com.lexcrm.model.Notificacao;
import br.com.lexcrm.model.NotificacaoTipo;
import br.com.lexcrm.model.Usuario;
import br.com.lexcrm.repository.NotificacaoRepository;
import br.com.lexcrm.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificacaoService {
    @Autowired
    private NotificacaoRepository notificacaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    public Notificacao criarSeNaoExistir(String tenantId, String username, NotificacaoTipo tipo, String titulo, String mensagem, String link, String codigo) {
        if (codigo != null && !codigo.isEmpty() && notificacaoRepository.existsByCodigo(codigo)) {
            return null;
        }

        Notificacao n = new Notificacao();
        n.setTenantId(tenantId);
        n.setTipo(tipo);
        n.setTitulo(titulo);
        n.setMensagem(mensagem);
        n.setLink(link);
        n.setCodigo(codigo);
        n.setCreatedAt(LocalDateTime.now());
        if (username != null && !username.isEmpty()) {
            Usuario u = usuarioRepository.findByUsername(username).orElse(null);
            n.setUsuario(u);
        }
        return notificacaoRepository.save(n);
    }

    public List<Notificacao> listarNaoLidasDoUsuario(String tenantId, String username) {
        return notificacaoRepository.findUnreadForUser(tenantId, username);
    }

    public long contarNaoLidasDoUsuario(String tenantId, String username) {
        return notificacaoRepository.countUnreadForUser(tenantId, username);
    }

    public boolean marcarComoLida(String tenantId, String username, Long id) {
        Notificacao n = notificacaoRepository.findById(id).orElse(null);
        if (n == null) {
            return false;
        }
        if (n.getTenantId() != null && !n.getTenantId().equals(tenantId)) {
            return false;
        }
        if (n.getUsuario() != null && (username == null || !n.getUsuario().getUsername().equals(username))) {
            return false;
        }
        if (n.getLidaEm() == null) {
            n.setLidaEm(LocalDateTime.now());
            notificacaoRepository.save(n);
        }
        return true;
    }

    public String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }
}
