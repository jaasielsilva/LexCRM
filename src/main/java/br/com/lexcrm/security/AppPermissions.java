package br.com.lexcrm.security;

import br.com.lexcrm.model.Role;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class AppPermissions {

    public static final String CLIENTES_CREATE = "CLIENTES_CREATE";
    public static final String CLIENTES_EDIT = "CLIENTES_EDIT";
    public static final String CLIENTES_DELETE = "CLIENTES_DELETE";
    public static final String CLIENTES_VIEW = "CLIENTES_VIEW";
    public static final String CLIENTES_EXPORT = "CLIENTES_EXPORT";

    private AppPermissions() {}

    public static Set<String> permissionsFor(Role role) {
        if (role == null) return Collections.emptySet();
        Set<String> p = new LinkedHashSet<>();
        switch (role) {
            case ADMIN -> {
                p.add(CLIENTES_CREATE);
                p.add(CLIENTES_EDIT);
                p.add(CLIENTES_DELETE);
                p.add(CLIENTES_VIEW);
                p.add(CLIENTES_EXPORT);
            }
            case ADVOGADO -> {
                p.add(CLIENTES_CREATE);
                p.add(CLIENTES_EDIT);
                p.add(CLIENTES_VIEW);
                p.add(CLIENTES_EXPORT);
            }
            case FINANCEIRO -> {
                p.add(CLIENTES_VIEW);
                p.add(CLIENTES_EXPORT);
            }
            case ATENDIMENTO -> {
                p.add(CLIENTES_VIEW);
            }
            default -> {
                // no-op
            }
        }
        return Collections.unmodifiableSet(p);
    }
}
