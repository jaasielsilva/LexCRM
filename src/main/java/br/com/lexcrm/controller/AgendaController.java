package br.com.lexcrm.controller;

import br.com.lexcrm.model.AgendaEvento;
import br.com.lexcrm.model.NotificacaoTipo;
import br.com.lexcrm.repository.ClienteRepository;
import br.com.lexcrm.repository.ProcessoRepository;
import br.com.lexcrm.repository.UsuarioRepository;
import br.com.lexcrm.service.AgendaConflictException;
import br.com.lexcrm.service.AgendaService;
import br.com.lexcrm.service.NotificacaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.TextStyle;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Locale;

@Controller
@RequestMapping("/agenda")
public class AgendaController {
    private static final String TENANT_FIXO = "T001";

    @Autowired
    private AgendaService agendaService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private NotificacaoService notificacaoService;

    @GetMapping
    @PreAuthorize("hasAuthority('AGENDA_VIEW')")
    public String index(@RequestParam(required = false) String data,
                        @RequestParam(required = false) Long responsavelId,
                        Model model) {
        LocalDate dia = (data == null || data.isBlank()) ? LocalDate.now() : LocalDate.parse(data);
        model.addAttribute("activePage", "agenda");
        model.addAttribute("data", dia.toString());
        model.addAttribute("responsavelId", responsavelId);
        model.addAttribute("eventos", agendaService.listarDoDia(TENANT_FIXO, dia, responsavelId));
        model.addAttribute("calMonthLabel", dia.getMonth().getDisplayName(TextStyle.FULL, new Locale("pt", "BR")) + " " + dia.getYear());
        model.addAttribute("calMonth", dia.getMonthValue());
        model.addAttribute("calYear", dia.getYear());
        model.addAttribute("calPrev", dia.withDayOfMonth(1).minusMonths(1).toString());
        model.addAttribute("calNext", dia.withDayOfMonth(1).plusMonths(1).toString());
        model.addAttribute("calDays", buildCalendarDays(dia));
        model.addAttribute("calCounts", buildMonthCounts(dia, responsavelId));
        model.addAttribute("usuarios", usuarioRepository.findAll());
        model.addAttribute("clientes", clienteRepository.findAll());
        model.addAttribute("processos", processoRepository.findAll());
        return "agenda/index";
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('AGENDA_VIEW')")
    public String list(@RequestParam(required = false) String data,
                       @RequestParam(required = false) Long responsavelId,
                       Model model) {
        LocalDate dia = (data == null || data.isBlank()) ? LocalDate.now() : LocalDate.parse(data);
        model.addAttribute("data", dia.toString());
        model.addAttribute("responsavelId", responsavelId);
        model.addAttribute("eventos", agendaService.listarDoDia(TENANT_FIXO, dia, responsavelId));
        return "agenda/index :: list";
    }

    @GetMapping("/events")
    @ResponseBody
    @PreAuthorize("hasAuthority('AGENDA_VIEW')")
    public List<Map<String, Object>> events(@RequestParam String start,
                                            @RequestParam String end,
                                            @RequestParam(required = false) Long responsavelId) {
        LocalDateTime from = parseDateTime(start);
        LocalDateTime to = parseDateTime(end);
        List<AgendaEvento> eventos = agendaService.listarDoIntervalo(TENANT_FIXO, from, to, responsavelId);
        return eventos.stream().map(e -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", e.getId());
            m.put("title", e.getTitulo());
            m.put("start", e.getInicio() != null ? e.getInicio().toString() : null);
            m.put("end", e.getFim() != null ? e.getFim().toString() : null);
            m.put("allDay", false);
            String status = e.getStatus() != null ? e.getStatus() : "ATIVO";
            String bg = "ATIVO".equalsIgnoreCase(status) ? "#1e3a8a" : ("CONCLUIDO".equalsIgnoreCase(status) ? "#16a34a" : "#64748b");
            m.put("backgroundColor", bg);
            m.put("borderColor", bg);
            Map<String, Object> ext = new HashMap<>();
            ext.put("local", e.getLocal());
            ext.put("descricao", e.getDescricao());
            ext.put("status", e.getStatus());
            ext.put("responsavelId", e.getResponsavel() != null ? e.getResponsavel().getId() : null);
            ext.put("clienteId", e.getCliente() != null ? e.getCliente().getId() : null);
            ext.put("processoId", e.getProcesso() != null ? e.getProcesso().getId() : null);
            m.put("extendedProps", ext);
            return m;
        }).toList();
    }

    private static List<LocalDate> buildCalendarDays(LocalDate selected) {
        LocalDate firstOfMonth = selected.withDayOfMonth(1);
        LocalDate start = firstOfMonth.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return java.util.stream.IntStream.range(0, 42)
                .mapToObj(i -> start.plusDays(i))
                .toList();
    }

    private Map<String, Long> buildMonthCounts(LocalDate selected, Long responsavelId) {
        Map<String, Long> counts = new TreeMap<>();
        for (AgendaEvento e : agendaService.listarDoMes(TENANT_FIXO, selected, responsavelId)) {
            if (e.getInicio() == null) {
                continue;
            }
            String key = e.getInicio().toLocalDate().toString();
            counts.put(key, counts.getOrDefault(key, 0L) + 1L);
        }
        return counts;
    }

    private static LocalDateTime parseDateTime(String s) {
        if (s == null || s.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return OffsetDateTime.parse(s).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(s);
        } catch (DateTimeParseException ignored) {
        }
        String cleaned = s.trim();
        int idx = cleaned.indexOf('[');
        if (idx > 0) {
            cleaned = cleaned.substring(0, idx);
        }
        return LocalDateTime.parse(cleaned);
    }

    @PostMapping
    @ResponseBody
    @PreAuthorize("hasAuthority('AGENDA_CREATE')")
    public ResponseEntity<Map<String, Object>> create(AgendaEvento evento) {
        Map<String, Object> resp = new HashMap<>();
        try {
            agendaService.criar(TENANT_FIXO, evento);
            resp.put("ok", true);
            resp.put("message", "Evento criado com sucesso.");
            return ResponseEntity.ok(resp);
        } catch (AgendaConflictException e) {
            resp.put("ok", false);
            resp.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        } catch (Exception e) {
            String username = notificacaoService.currentUsername();
            notificacaoService.criarSeNaoExistir(TENANT_FIXO, username, NotificacaoTipo.ERROR,
                    "Erro ao criar evento",
                    e.getMessage() != null ? e.getMessage() : "Falha ao criar evento.",
                    "/agenda",
                    null);
            resp.put("ok", false);
            resp.put("message", e.getMessage() != null ? e.getMessage() : "Não foi possível salvar.");
            return ResponseEntity.badRequest().body(resp);
        }
    }

    @PostMapping("/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('AGENDA_EDIT')")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id, AgendaEvento evento) {
        Map<String, Object> resp = new HashMap<>();
        try {
            AgendaEvento updated = agendaService.atualizar(TENANT_FIXO, id, evento);
            if (updated == null) {
                resp.put("ok", false);
                resp.put("message", "Evento não encontrado.");
                return ResponseEntity.badRequest().body(resp);
            }
            resp.put("ok", true);
            resp.put("message", "Evento atualizado com sucesso.");
            return ResponseEntity.ok(resp);
        } catch (AgendaConflictException e) {
            resp.put("ok", false);
            resp.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        } catch (Exception e) {
            String username = notificacaoService.currentUsername();
            notificacaoService.criarSeNaoExistir(TENANT_FIXO, username, NotificacaoTipo.ERROR,
                    "Erro ao atualizar evento",
                    e.getMessage() != null ? e.getMessage() : "Falha ao atualizar evento.",
                    "/agenda",
                    null);
            resp.put("ok", false);
            resp.put("message", e.getMessage() != null ? e.getMessage() : "Não foi possível salvar.");
            return ResponseEntity.badRequest().body(resp);
        }
    }

    @PostMapping("/{id}/delete")
    @ResponseBody
    @PreAuthorize("hasAuthority('AGENDA_DELETE')")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        Map<String, Object> resp = new HashMap<>();
        try {
            boolean ok = agendaService.cancelar(TENANT_FIXO, id);
            if (!ok) {
                resp.put("ok", false);
                resp.put("message", "Evento não encontrado.");
                return ResponseEntity.badRequest().body(resp);
            }
            resp.put("ok", true);
            resp.put("message", "Evento cancelado com sucesso.");
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            String username = notificacaoService.currentUsername();
            notificacaoService.criarSeNaoExistir(TENANT_FIXO, username, NotificacaoTipo.ERROR,
                    "Erro ao cancelar evento",
                    e.getMessage() != null ? e.getMessage() : "Falha ao cancelar evento.",
                    "/agenda",
                    null);
            resp.put("ok", false);
            resp.put("message", e.getMessage() != null ? e.getMessage() : "Não foi possível cancelar.");
            return ResponseEntity.badRequest().body(resp);
        }
    }
}
