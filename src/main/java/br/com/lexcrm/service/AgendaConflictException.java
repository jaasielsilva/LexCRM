package br.com.lexcrm.service;

import br.com.lexcrm.model.AgendaEvento;
import java.util.List;

public class AgendaConflictException extends RuntimeException {
    private final List<AgendaEvento> conflicts;

    public AgendaConflictException(String message, List<AgendaEvento> conflicts) {
        super(message);
        this.conflicts = conflicts;
    }

    public List<AgendaEvento> getConflicts() {
        return conflicts;
    }
}
