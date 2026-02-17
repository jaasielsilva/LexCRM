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

    @Override
    public void run(String... args) {

        // cria dados apenas se não existir nada
        if (usuarioRepository.count() == 0) {

            System.out.println("Criando dados iniciais...");

            seedUsers();

            System.out.println("Dados iniciais criados com sucesso!");
        }
    }

    private void seedUsers() {
        Usuario admin = createUsuario(
                "admin",
                "Administrador do Sistema");
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
