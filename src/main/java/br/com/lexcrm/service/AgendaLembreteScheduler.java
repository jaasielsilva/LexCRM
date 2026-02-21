package br.com.lexcrm.service;

import br.com.lexcrm.model.AgendaEvento;
import br.com.lexcrm.model.NotificacaoTipo;
import br.com.lexcrm.repository.AgendaEventoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AgendaLembreteScheduler {
    private static final String TENANT_FIXO = "T001";

    @Autowired
    private AgendaEventoRepository agendaEventoRepository;

    @Autowired
    private NotificacaoService notificacaoService;

    @Scheduled(fixedDelay = 60000)
    public void gerarAlertas() {
        LocalDateTime now = LocalDateTime.now();
        gerarAlertasParaOffset(now, 30);
        gerarAlertasParaOffset(now, 10);
    }

    private void gerarAlertasParaOffset(LocalDateTime now, int minutos) {
        LocalDateTime target = now.plusMinutes(minutos).truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime to = target.plusMinutes(1);
        List<AgendaEvento> eventos = agendaEventoRepository.findStartingInWindow(TENANT_FIXO, target, to);
        if (eventos == null || eventos.isEmpty()) {
            return;
        }
        for (AgendaEvento e : eventos) {
            if (e.getResponsavel() == null || e.getResponsavel().getUsername() == null) {
                continue;
            }
            String data = e.getInicio() != null ? e.getInicio().toLocalDate().toString() : "";
            String link = "/agenda?data=" + data + "&responsavelId=" + e.getResponsavel().getId();
            String hora = e.getInicio() != null ? e.getInicio().format(DateTimeFormatter.ofPattern("HH:mm")) : "";
            String titulo = "Lembrete de agenda";
            String mensagem = "Compromisso às " + hora + " em " + minutos + " min: " + (e.getTitulo() != null ? e.getTitulo() : "Compromisso");
            String codigo = "AGENDA_REMINDER_" + e.getId() + "_" + minutos + "_" + (e.getInicio() != null ? e.getInicio().toString() : "");
            notificacaoService.criarSeNaoExistir(TENANT_FIXO, e.getResponsavel().getUsername(), NotificacaoTipo.INFO, titulo, mensagem, link, codigo);
        }
    }
}
