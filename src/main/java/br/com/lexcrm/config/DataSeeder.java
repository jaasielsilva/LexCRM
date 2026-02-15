package br.com.lexcrm.config;

import br.com.lexcrm.model.*;
import br.com.lexcrm.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import br.com.lexcrm.model.Financeiro;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private ProcessoRepository processoRepository;
    @Autowired
    private FinanceiroRepository financeiroRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (usuarioRepository.count() == 0) {
            seedData();
        }
        
        // Ensure financial data exists even if users exist
        if (financeiroRepository.count() == 0 && processoRepository.count() > 0) {
             Processo p1 = processoRepository.findAll().get(0);
             createFinanceiro("Honorários Iniciais", new BigDecimal("5000.00"), "Pago", p1);
             createFinanceiro("Custas Processuais", new BigDecimal("1200.50"), "Pago", p1);
             createFinanceiro("Honorários Finais", new BigDecimal("15000.00"), "Pendente", p1);
             createFinanceiro("Diligência", new BigDecimal("350.00"), "Pendente", p1);
        }
    }

    private void seedData() {
        // --- Create Lawyers (Users) ---
        List<Usuario> advogados = new ArrayList<>();
        
        Usuario admin = createUsuario("admin", "Administrador do Sistema");
        advogados.add(admin);
        
        advogados.add(createUsuario("adv1", "Dr. Carlos Mendes"));
        advogados.add(createUsuario("adv2", "Dra. Ana Paula Souza"));
        advogados.add(createUsuario("adv3", "Dr. Roberto Campos"));

        // --- Create Clients ---
        List<Cliente> clientes = new ArrayList<>();
        clientes.add(createCliente("Hospital Santa Clara", "12.345.678/0001-99", "contato@santaclara.com.br"));
        clientes.add(createCliente("Indústrias Metalúrgicas S.A.", "98.765.432/0001-10", "juridico@metalurgica.com"));
        clientes.add(createCliente("João da Silva", "111.222.333-44", "joao.silva@email.com"));
        clientes.add(createCliente("Maria Oliveira", "555.666.777-88", "maria.oliveira@email.com"));
        clientes.add(createCliente("Construtora Viver Bem", "44.555.666/0001-22", "sac@viverbem.com.br"));
        clientes.add(createCliente("Pedro Santos", "999.888.777-66", "pedro.santos@email.com"));

        // --- Create Processes ---
        
        // Process 1: Initial Stage
        createProcesso(
            "0001234-89.2024.8.26.0100", 
            "Ação de Cobrança Indevida", 
            "Cível", 
            "1ª Vara Cível",
            "São Paulo - Capital",
            clientes.get(0), 
            advogados.get(1), 
            new BigDecimal("150000.00"),
            1 // First stage completed
        );

        // Process 2: Mid Stage
        createProcesso(
            "0005678-12.2024.8.26.0100", 
            "Reclamação Trabalhista - Horas Extras", 
            "Trabalhista", 
            "5ª Vara do Trabalho",
            "Osasco - SP",
            clientes.get(2), 
            advogados.get(2), 
            new BigDecimal("80000.00"),
            6 // Halfway
        );

        // Process 3: Advanced Stage
        createProcesso(
            "5009876-33.2023.4.03.6100", 
            "Execução Fiscal - IPTU", 
            "Tributário", 
            "Vara de Execuções Fiscais",
            "Campinas - SP",
            clientes.get(4), 
            advogados.get(3), 
            new BigDecimal("500000.00"),
            10 // Almost done
        );

        System.out.println("Dados iniciais carregados com sucesso!");
        // --- Create Financial Records ---
        if (processoRepository.count() > 0) {
            Processo p1 = processoRepository.findAll().get(0);
            createFinanceiro("Honorários Iniciais", new BigDecimal("5000.00"), "Pago", p1);
            createFinanceiro("Custas Processuais", new BigDecimal("1200.50"), "Pago", p1);
            createFinanceiro("Honorários Finais", new BigDecimal("15000.00"), "Pendente", p1);
            createFinanceiro("Diligência", new BigDecimal("350.00"), "Pendente", p1);
        }
    }

    private void createFinanceiro(String descricao, BigDecimal valor, String status, Processo processo) {
        Financeiro f = new Financeiro();
        f.setDescricao(descricao);
        f.setValor(valor);
        f.setStatus(status);
        f.setDataVencimento(LocalDate.now().plusDays(10));
        f.setProcesso(processo);
        f.setTenantId("T001");
        financeiroRepository.save(f);
    }

    private Usuario createUsuario(String username, String nome) {
        Usuario u = new Usuario();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode("password")); // Default password
        u.setRole(Role.ADMIN);
        u.setNomeCompleto(nome);
        u.setTenantId("T001");
        return usuarioRepository.save(u);
    }

    private Cliente createCliente(String nome, String doc, String email) {
        Cliente c = new Cliente();
        c.setNome(nome);
        c.setCpfCnpj(doc);
        c.setEmail(email);
        c.setTenantId("T001");
        return clienteRepository.save(c);
    }

    private void createProcesso(
            String numero, String titulo, String tipo, String vara, String comarca,
            Cliente cliente, Usuario advogado, BigDecimal valor, int completedStages) {
        
        Processo p = new Processo();
        p.setNumeroProcesso(numero);
        p.setTitulo(titulo);
        p.setStatus("Em Andamento");
        p.setTipo(tipo);
        p.setVara(vara);
        p.setComarca(comarca);
        p.setCliente(cliente);
        p.setAdvogadoResponsavel(advogado);
        p.setDataAbertura(LocalDate.now().minusMonths(completedStages));
        p.setValorCausa(valor);
        p.setTenantId("T001");

        // Create Stages
        List<String> stageNames = Arrays.asList(
            "Consulta Inicial", "Documentação", "Petição Inicial", "Citação", 
            "Contestação", "Audiência", "Perícia", "Alegações Finais", 
            "Sentença", "Recurso", "Trânsito em Julgado", "Execução"
        );

        for (int i = 0; i < stageNames.size(); i++) {
            EtapaProcesso etapa = new EtapaProcesso();
            etapa.setNome(stageNames.get(i));
            etapa.setDescricao("Etapa padrão do fluxo processual");
            etapa.setOrdem(i + 1);
            etapa.setProcesso(p);
            
            if (i < completedStages) {
                etapa.setStatus("Concluído");
                etapa.setData(LocalDate.now().minusWeeks(stageNames.size() - i));
            } else if (i == completedStages) {
                etapa.setStatus("Em Andamento");
                etapa.setData(LocalDate.now());
            } else {
                etapa.setStatus("Pendente");
            }
            
            p.getEtapas().add(etapa);
        }

        processoRepository.save(p);
    }
}
