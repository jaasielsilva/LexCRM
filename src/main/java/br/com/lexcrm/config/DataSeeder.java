package br.com.lexcrm.config;

import br.com.lexcrm.model.*;
import br.com.lexcrm.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

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
    public void run(String... args) {

        // cria dados apenas se não existir nada
        if (usuarioRepository.count() == 0) {

            System.out.println("Criando dados iniciais...");

            seedData();

            System.out.println("Dados iniciais criados com sucesso!");
        }

        // garante financeiro
        if (financeiroRepository.count() == 0 && processoRepository.count() > 0) {

            Processo processo = processoRepository.findAll().get(0);

            createFinanceiro("Honorários Iniciais",
                    new BigDecimal("5000.00"),
                    "Pago",
                    processo);

            createFinanceiro("Honorários Finais",
                    new BigDecimal("15000.00"),
                    "Pendente",
                    processo);
        }
    }

    private void seedData() {

        // USUÁRIOS

        Usuario admin = createUsuario(
                "admin",
                "Administrador do Sistema"
        );

        Usuario advogado = createUsuario(
                "advogado",
                "Dr. João Advogado"
        );

        // CLIENTE

        Cliente cliente = createCliente(
                "Jaasiel Miranda da Silva",
                "123.456.789-00",
                "jaasiel@email.com"
        );

        // PROCESSO

        createProcesso(

                "765767",
                "Seguro de Vida - Sinistro",
                "Seguro",
                "Seguradora XPTO",
                "São Paulo - SP",

                cliente,
                advogado,

                new BigDecimal("50000"),

                3 // etapas já concluídas
        );
    }

    private Usuario createUsuario(String username, String nome) {

        Usuario u = new Usuario();

        u.setUsername(username);

        u.setPassword(
                passwordEncoder.encode("123456")
        );

        u.setRole(Role.ADMIN);

        u.setNomeCompleto(nome);

        u.setTenantId("T001");

        return usuarioRepository.save(u);
    }

    private Cliente createCliente(String nome,
                                  String documento,
                                  String email) {

        Cliente c = new Cliente();

        c.setNome(nome);

        c.setCpfCnpj(documento);

        c.setEmail(email);

        c.setTenantId("T001");

        return clienteRepository.save(c);
    }

    private void createFinanceiro(String descricao,
                                  BigDecimal valor,
                                  String status,
                                  Processo processo) {

        Financeiro f = new Financeiro();

        f.setDescricao(descricao);

        f.setValor(valor);

        f.setStatus(status);

        f.setDataVencimento(
                LocalDate.now().plusDays(10)
        );

        f.setProcesso(processo);

        f.setTenantId("T001");

        financeiroRepository.save(f);
    }

    private void createProcesso(

            String numero,
            String titulo,
            String tipo,
            String seguradora,
            String cidade,

            Cliente cliente,
            Usuario advogado,

            BigDecimal valor,

            int etapasConcluidas
    ) {

        Processo p = new Processo();

        p.setNumeroProcesso(numero);

        p.setTitulo(titulo);

        p.setStatus("EM_ANDAMENTO");

        p.setTipo(tipo);

        p.setVara(seguradora);

        p.setComarca(cidade);

        p.setCliente(cliente);

        p.setAdvogadoResponsavel(advogado);

        p.setValorCausa(valor);

        p.setDataAbertura(LocalDate.now());

        p.setTenantId("T001");

        // IMPORTANTE
        p.setEtapas(new ArrayList<>());

        List<String> etapas = Arrays.asList(

                "Cadastro do Cliente",

                "Documentos Solicitados",

                "Documentos Recebidos",

                "Análise Documental",

                "Pendência Documental",

                "Aguardando Assinatura",

                "Perícia Médica",

                "Laudo Médico Recebido",

                "Enviado para Seguradora",

                "Em Análise pela Seguradora",

                "Pendência com Seguradora",

                "Sinistro Gerado",

                "Processo Finalizado"
        );

        for (int i = 0; i < etapas.size(); i++) {

            EtapaProcesso etapa = new EtapaProcesso();

            String nome = etapas.get(i);

            etapa.setNome(nome);

            etapa.setDescricao(
                    getDescricaoEtapa(nome)
            );

            etapa.setOrdem(i + 1);

            etapa.setProcesso(p);

            if (i < etapasConcluidas) {

                etapa.setStatus("CONCLUIDO");

                etapa.setData(
                        LocalDate.now().minusDays(10 - i)
                );

            } else if (i == etapasConcluidas) {

                etapa.setStatus("EM_ANDAMENTO");

                etapa.setData(LocalDate.now());

            } else {

                etapa.setStatus("PENDENTE");
            }

            p.getEtapas().add(etapa);
        }

        processoRepository.save(p);
    }

    private String getDescricaoEtapa(String etapa) {

        switch (etapa) {

            case "Cadastro do Cliente":
                return "Cliente cadastrado no sistema";

            case "Documentos Solicitados":
                return "Lista de documentos enviada ao cliente";

            case "Documentos Recebidos":
                return "Cliente enviou os documentos";

            case "Análise Documental":
                return "Análise dos documentos";

            case "Pendência Documental":
                return "Existem documentos pendentes";

            case "Aguardando Assinatura":
                return "Cliente precisa assinar documentos";

            case "Perícia Médica":
                return "Documentos enviados ao médico";

            case "Laudo Médico Recebido":
                return "Laudo médico recebido";

            case "Enviado para Seguradora":
                return "Processo enviado à seguradora";

            case "Em Análise pela Seguradora":
                return "Seguradora analisando";

            case "Pendência com Seguradora":
                return "Seguradora solicitou correção";

            case "Sinistro Gerado":
                return "Sinistro registrado";

            case "Processo Finalizado":
                return "Processo concluído";

            default:
                return "";
        }
    }
}
