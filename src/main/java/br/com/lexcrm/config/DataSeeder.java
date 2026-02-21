package br.com.lexcrm.config;

import br.com.lexcrm.model.*;
import br.com.lexcrm.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Override
    public void run(String... args) {

        // cria dados apenas se não existir nada
        if (usuarioRepository.count() == 0) {

            System.out.println("Criando dados iniciais...");

            seedUsers();

            System.out.println("Dados iniciais criados com sucesso!");
        }

        if (rolePermissionRepository.count() == 0) {
            seedRolePermissionsDefaults();
        }
    }

    private void seedUsers() {
        Usuario admin = createUsuario(
                "admin",
                "Administrador do Sistema");
    }

    private void seedRolePermissionsDefaults() {
        // ADMIN: tudo
        for (String p : new String[]{
                "CLIENTES_CREATE","CLIENTES_EDIT","CLIENTES_DELETE","CLIENTES_VIEW","CLIENTES_EXPORT",
                "PROCESSOS_CREATE","PROCESSOS_EDIT","PROCESSOS_DELETE","PROCESSOS_VIEW","PROCESSOS_EXPORT",
                "FINANCEIRO_CREATE","FINANCEIRO_EDIT","FINANCEIRO_DELETE","FINANCEIRO_VIEW","FINANCEIRO_EXPORT",
                "RELATORIOS_CREATE","RELATORIOS_EDIT","RELATORIOS_DELETE","RELATORIOS_VIEW","RELATORIOS_EXPORT",
                "AGENDA_CREATE","AGENDA_EDIT","AGENDA_DELETE","AGENDA_VIEW","AGENDA_EXPORT",
                "CONFIG_CREATE","CONFIG_EDIT","CONFIG_DELETE","CONFIG_VIEW","CONFIG_EXPORT"
        }) addPerm(Role.ADMIN, p);
        // ADVOGADO: clientes (criar, editar, ver, exportar), processos (criar, editar, ver, exportar), relatórios (ver, exportar), agenda (criar, editar, ver)
        for (String p : new String[]{
                "CLIENTES_CREATE","CLIENTES_EDIT","CLIENTES_VIEW","CLIENTES_EXPORT",
                "PROCESSOS_CREATE","PROCESSOS_EDIT","PROCESSOS_VIEW","PROCESSOS_EXPORT",
                "RELATORIOS_VIEW","RELATORIOS_EXPORT",
                "AGENDA_CREATE","AGENDA_EDIT","AGENDA_VIEW"
        }) addPerm(Role.ADVOGADO, p);
        // FINANCEIRO: clientes (ver, exportar), financeiro (criar, editar, excluir, ver, exportar), relatórios (ver, exportar), agenda (ver)
        for (String p : new String[]{
                "CLIENTES_VIEW","CLIENTES_EXPORT",
                "FINANCEIRO_CREATE","FINANCEIRO_EDIT","FINANCEIRO_DELETE","FINANCEIRO_VIEW","FINANCEIRO_EXPORT",
                "RELATORIOS_VIEW","RELATORIOS_EXPORT",
                "AGENDA_VIEW"
        }) addPerm(Role.FINANCEIRO, p);
        // ATENDIMENTO: clientes (ver), processos (ver), financeiro (ver), relatórios (ver), agenda (criar, editar, excluir, ver)
        for (String p : new String[]{
                "CLIENTES_VIEW",
                "PROCESSOS_VIEW",
                "FINANCEIRO_VIEW",
                "RELATORIOS_VIEW",
                "AGENDA_CREATE","AGENDA_EDIT","AGENDA_DELETE","AGENDA_VIEW"
        }) addPerm(Role.ATENDIMENTO, p);
    }

    private void addPerm(Role role, String perm) {
        br.com.lexcrm.model.RolePermission rp = new br.com.lexcrm.model.RolePermission();
        rp.setRole(role);
        rp.setPermission(perm);
        rolePermissionRepository.save(rp);
    }

    private Usuario createUsuario(String username, String nome) {

        Usuario u = new Usuario();

        u.setUsername(username);

        u.setPassword(
                passwordEncoder.encode("123456"));

        u.setRole(Role.ADMIN);

        u.setNomeCompleto(nome);

        u.setTenantId("T001");

        return usuarioRepository.save(u);
    }
}
