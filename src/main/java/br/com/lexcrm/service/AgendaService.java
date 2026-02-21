package br.com.lexcrm.service;

import br.com.lexcrm.model.AgendaEvento;
import br.com.lexcrm.model.NotificacaoTipo;
import br.com.lexcrm.model.Usuario;
import br.com.lexcrm.repository.AgendaEventoRepository;
import br.com.lexcrm.repository.ClienteRepository;
import br.com.lexcrm.repository.ProcessoRepository;
import br.com.lexcrm.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AgendaService {
    private static final int DURACAO_PADRAO_MINUTOS = 30;

    @Autowired
    private AgendaEventoRepository agendaEventoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private NotificacaoService notificacaoService;

    public List<AgendaEvento> listarDoDia(String tenantId, LocalDate dia, Long responsavelId) {
        LocalDate d = dia != null ? dia : LocalDate.now();
        LocalDateTime from = d.atStartOfDay();
        LocalDateTime to = d.plusDays(1).atStartOfDay();
        return agendaEventoRepository.listByRange(tenantId, from, to, responsavelId);
    }

    public List<AgendaEvento> listarDoMes(String tenantId, LocalDate anyDayInMonth, Long responsavelId) {
        LocalDate d = anyDayInMonth != null ? anyDayInMonth : LocalDate.now();
        LocalDate first = d.withDayOfMonth(1);
        LocalDateTime from = first.atStartOfDay();
        LocalDateTime to = first.plusMonths(1).atStartOfDay();
        return agendaEventoRepository.listByRange(tenantId, from, to, responsavelId);
    }

    public List<AgendaEvento> listarDoIntervalo(String tenantId, LocalDateTime from, LocalDateTime to, Long responsavelId) {
        LocalDateTime f = from != null ? from : LocalDateTime.now().minusDays(1);
        LocalDateTime t = to != null ? to : LocalDateTime.now().plusDays(1);
        return agendaEventoRepository.listOverlappingRange(tenantId, f, t, responsavelId);
    }

    public AgendaEvento criar(String tenantId, AgendaEvento payload) {
        AgendaEvento e = new AgendaEvento();
        aplicarPayload(tenantId, e, payload);
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        if (e.getStatus() == null || e.getStatus().trim().isEmpty()) {
            e.setStatus("ATIVO");
        }
        validarConflito(tenantId, e, null);
        return agendaEventoRepository.save(e);
    }

    public AgendaEvento atualizar(String tenantId, Long id, AgendaEvento payload) {
        AgendaEvento e = agendaEventoRepository.findById(id).orElse(null);
        if (e == null) {
            return null;
        }
        if (e.getTenantId() != null && !e.getTenantId().equals(tenantId)) {
            return null;
        }
        aplicarPayload(tenantId, e, payload);
        e.setUpdatedAt(LocalDateTime.now());
        validarConflito(tenantId, e, id);
        return agendaEventoRepository.save(e);
    }

    public boolean cancelar(String tenantId, Long id) {
        AgendaEvento e = agendaEventoRepository.findById(id).orElse(null);
        if (e == null) {
            return false;
        }
        if (e.getTenantId() != null && !e.getTenantId().equals(tenantId)) {
            return false;
        }
        e.setStatus("CANCELADO");
        e.setUpdatedAt(LocalDateTime.now());
        agendaEventoRepository.save(e);
        return true;
    }

    private void aplicarPayload(String tenantId, AgendaEvento target, AgendaEvento payload) {
        target.setTenantId(tenantId);

        String titulo = payload.getTitulo() == null ? "" : payload.getTitulo().trim();
        target.setTitulo(titulo.isEmpty() ? "Compromisso" : titulo);

        target.setDescricao(payload.getDescricao() != null && !payload.getDescricao().trim().isEmpty() ? payload.getDescricao().trim() : null);
        target.setLocal(payload.getLocal() != null && !payload.getLocal().trim().isEmpty() ? payload.getLocal().trim() : null);

        if (payload.getInicio() == null) {
            target.setInicio(null);
            target.setFim(null);
        } else {
            target.setInicio(payload.getInicio());
            LocalDateTime fim = payload.getFim() != null ? payload.getFim() : payload.getInicio().plusMinutes(DURACAO_PADRAO_MINUTOS);
            if (fim.isBefore(payload.getInicio())) {
                fim = payload.getInicio().plusMinutes(DURACAO_PADRAO_MINUTOS);
            }
            target.setFim(fim);
        }

        if (payload.getResponsavel() != null && payload.getResponsavel().getId() != null) {
            Usuario u = usuarioRepository.findById(payload.getResponsavel().getId()).orElse(null);
            target.setResponsavel(u);
        } else {
            target.setResponsavel(currentUsuario());
        }

        if (payload.getCliente() != null && payload.getCliente().getId() != null) {
            target.setCliente(clienteRepository.findById(payload.getCliente().getId()).orElse(null));
        } else {
            target.setCliente(null);
        }

        if (payload.getProcesso() != null && payload.getProcesso().getId() != null) {
            target.setProcesso(processoRepository.findById(payload.getProcesso().getId()).orElse(null));
        } else {
            target.setProcesso(null);
        }

        if (payload.getStatus() != null && !payload.getStatus().trim().isEmpty()) {
            target.setStatus(payload.getStatus().trim().toUpperCase());
        }
    }

    private void validarConflito(String tenantId, AgendaEvento e, Long excludeId) {
        if (e.getInicio() == null || e.getFim() == null || e.getResponsavel() == null || e.getResponsavel().getId() == null) {
            if (e.getInicio() == null) {
                throw new IllegalArgumentException("Informe a data/hora de início.");
            }
            return;
        }
        List<AgendaEvento> conflicts = agendaEventoRepository.findConflicts(tenantId, e.getResponsavel().getId(), e.getInicio(), e.getFim(), excludeId);
        if (conflicts == null || conflicts.isEmpty()) {
            return;
        }
        String data = e.getInicio().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String link = "/agenda?data=" + data + "&responsavelId=" + e.getResponsavel().getId();
        String codigo = "AGENDA_CONFLICT_" + e.getResponsavel().getId() + "_" + e.getInicio().toString() + "_" + e.getFim().toString();
        notificacaoService.criarSeNaoExistir(tenantId, e.getResponsavel().getUsername(), NotificacaoTipo.WARNING,
                "Conflito de agenda",
                "Existe outro compromisso no mesmo horário.",
                link,
                codigo);
        throw new AgendaConflictException("Conflito: já existe compromisso no mesmo horário.", conflicts);
    }

    private Usuario currentUsuario() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : null;
        if (username == null) {
            return null;
        }
        return usuarioRepository.findByUsername(username).orElse(null);
    }
}
