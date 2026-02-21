package br.com.lexcrm.controller;

import br.com.lexcrm.model.Cliente;
import br.com.lexcrm.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.security.access.prepost.PreAuthorize;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/clientes")
public class ClienteController {

    @Autowired
    private ClienteRepository clienteRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('CLIENTES_VIEW')")
    public String index(@RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        Model model) {
        model.addAttribute("activePage", "clientes");
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(Sort.Direction.ASC, "nome"));
        Page<Cliente> clientesPage = clienteRepository.findAll(pageable);
        model.addAttribute("clientes", clientesPage.getContent());
        model.addAttribute("clientesPage", clientesPage);
        model.addAttribute("currentPage", clientesPage.getNumber());
        model.addAttribute("totalPages", clientesPage.getTotalPages());
        model.addAttribute("pageSize", clientesPage.getSize());
        model.addAttribute("totalElements", clientesPage.getTotalElements());
        long from = clientesPage.getTotalElements() == 0 ? 0 : (long) clientesPage.getNumber() * clientesPage.getSize() + 1;
        long to = (long) clientesPage.getNumber() * clientesPage.getSize() + clientesPage.getNumberOfElements();
        model.addAttribute("fromItem", from);
        model.addAttribute("toItem", to);
        return "clientes/index";
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('CLIENTES_VIEW')")
    public String search(@RequestParam(required = false) String query, Model model) {
        model.addAttribute("activePage", "clientes");
        if (query == null || query.trim().isEmpty()) {
            model.addAttribute("clientes", clienteRepository.findAll(Sort.by(Sort.Direction.ASC, "nome")));
        } else {
            String q = query.trim();
            List<Cliente> clientes = clienteRepository
                    .findTop50ByNomeContainingIgnoreCaseOrCpfCnpjContainingIgnoreCaseOrEmailContainingIgnoreCaseOrTelefoneContainingIgnoreCase(q, q, q, q);
            model.addAttribute("clientes", clientes);
        }
        return "clientes/index :: list";
    }

    @PostMapping
    @ResponseBody
    @PreAuthorize("hasAuthority('CLIENTES_CREATE')")
    public ResponseEntity<Map<String, Object>> create(Cliente cliente) {
        Map<String, Object> resp = new HashMap<>();

        String nome = cliente.getNome() == null ? "" : cliente.getNome().trim();
        String cpfCnpjRaw = cliente.getCpfCnpj() == null ? "" : cliente.getCpfCnpj().trim();
        String cpfCnpj = somenteDigitos(cpfCnpjRaw);

        if (!cpfCnpj.isEmpty()) {
            if (!cpfCnpjValido(cpfCnpj)) {
                resp.put("ok", false);
                resp.put("message", "CPF/CNPJ inválido.");
                return ResponseEntity.badRequest().body(resp);
            }
            if (clienteRepository.existsByCpfCnpj(cpfCnpj)) {
                resp.put("ok", false);
                resp.put("message", "Já existe um cliente cadastrado com este CPF/CNPJ.");
                return ResponseEntity.badRequest().body(resp);
            }
        }

        cliente.setNome(nome.isEmpty() ? null : nome);
        cliente.setCpfCnpj(cpfCnpj.isEmpty() ? null : cpfCnpj);
        if (cliente.getEmail() != null) {
            String email = cliente.getEmail().trim().toLowerCase();
            if (!email.isEmpty() && !emailValido(email)) {
                resp.put("ok", false);
                resp.put("message", "E-mail inválido.");
                return ResponseEntity.badRequest().body(resp);
            }
            cliente.setEmail(email.isEmpty() ? null : email);
        }
        if (cliente.getTelefone() != null) {
            String tel = somenteDigitos(cliente.getTelefone());
            if (!tel.isEmpty() && !(tel.length() == 10 || tel.length() == 11)) {
                resp.put("ok", false);
                resp.put("message", "Telefone inválido. Informe DDD + número.");
                return ResponseEntity.badRequest().body(resp);
            }
            cliente.setTelefone(tel.isEmpty() ? null : tel);
        }
        if (cliente.getIndicacao() != null) {
            String indicacao = cliente.getIndicacao().trim();
            cliente.setIndicacao(indicacao.isEmpty() ? null : indicacao);
        }
        cliente.setTenantId("T001");
        cliente.setCreatedAt(LocalDateTime.now());

        clienteRepository.save(cliente);

        resp.put("ok", true);
        resp.put("message", "Cliente cadastrado com sucesso.");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/{id}")
    @ResponseBody
    @PreAuthorize("hasAuthority('CLIENTES_EDIT')")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id, Cliente payload) {
        Map<String, Object> resp = new HashMap<>();
        Cliente cliente = clienteRepository.findById(id).orElse(null);
        if (cliente == null) {
            resp.put("ok", false);
            resp.put("message", "Cliente não encontrado.");
            return ResponseEntity.badRequest().body(resp);
        }

        String nome = payload.getNome() == null ? "" : payload.getNome().trim();
        String cpfCnpjRaw = payload.getCpfCnpj() == null ? "" : payload.getCpfCnpj().trim();
        String cpfCnpj = somenteDigitos(cpfCnpjRaw);

        if (!cpfCnpj.isEmpty()) {
            if (!cpfCnpjValido(cpfCnpj)) {
                resp.put("ok", false);
                resp.put("message", "CPF/CNPJ inválido.");
                return ResponseEntity.badRequest().body(resp);
            }
            if (clienteRepository.existsByCpfCnpjAndIdNot(cpfCnpj, id)) {
                resp.put("ok", false);
                resp.put("message", "Já existe outro cliente cadastrado com este CPF/CNPJ.");
                return ResponseEntity.badRequest().body(resp);
            }
        }

        cliente.setNome(nome.isEmpty() ? null : nome);
        cliente.setCpfCnpj(cpfCnpj.isEmpty() ? null : cpfCnpj);
        if (payload.getEmail() != null) {
            String email = payload.getEmail().trim().toLowerCase();
            if (!email.isEmpty() && !emailValido(email)) {
                resp.put("ok", false);
                resp.put("message", "E-mail inválido.");
                return ResponseEntity.badRequest().body(resp);
            }
            cliente.setEmail(email.isEmpty() ? null : email);
        } else {
            cliente.setEmail(null);
        }
        if (payload.getTelefone() != null) {
            String tel = somenteDigitos(payload.getTelefone());
            if (!tel.isEmpty() && !(tel.length() == 10 || tel.length() == 11)) {
                resp.put("ok", false);
                resp.put("message", "Telefone inválido. Informe DDD + número.");
                return ResponseEntity.badRequest().body(resp);
            }
            cliente.setTelefone(tel.isEmpty() ? null : tel);
        } else {
            cliente.setTelefone(null);
        }
        if (payload.getIndicacao() != null) {
            String indicacao = payload.getIndicacao().trim();
            cliente.setIndicacao(indicacao.isEmpty() ? null : indicacao);
        } else {
            cliente.setIndicacao(null);
        }

        clienteRepository.save(cliente);

        resp.put("ok", true);
        resp.put("message", "Cliente atualizado com sucesso.");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/{id}/delete")
    @ResponseBody
    @PreAuthorize("hasAuthority('CLIENTES_DELETE')")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        Map<String, Object> resp = new HashMap<>();
        try {
            clienteRepository.deleteById(id);
            resp.put("ok", true);
            resp.put("message", "Cliente excluído com sucesso.");
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            resp.put("ok", false);
            resp.put("message", "Não foi possível excluir o cliente. Verifique vínculos com processos.");
            return ResponseEntity.badRequest().body(resp);
        }
    }

    private static String somenteDigitos(String s) {
        if (s == null) return "";
        return s.replaceAll("\\D+", "");
    }

    private static boolean cpfCnpjValido(String s) {
        if (s == null) return false;
        if (s.length() == 11) return cpfValido(s);
        if (s.length() == 14) return cnpjValido(s);
        return false;
    }

    private static boolean cpfValido(String cpf) {
        if (cpf.chars().distinct().count() == 1) return false;
        int d1 = 0, d2 = 0;
        for (int i = 0; i < 9; i++) {
            int dig = cpf.charAt(i) - '0';
            d1 += dig * (10 - i);
            d2 += dig * (11 - i);
        }
        d1 = 11 - (d1 % 11);
        if (d1 >= 10) d1 = 0;
        d2 += d1 * 2;
        d2 = 11 - (d2 % 11);
        if (d2 >= 10) d2 = 0;
        return d1 == (cpf.charAt(9) - '0') && d2 == (cpf.charAt(10) - '0');
    }

    private static boolean cnpjValido(String cnpj) {
        if (cnpj.chars().distinct().count() == 1) return false;
        int[] w1 = {5,4,3,2,9,8,7,6,5,4,3,2};
        int[] w2 = {6,5,4,3,2,9,8,7,6,5,4,3,2};
        int s1 = 0;
        for (int i = 0; i < 12; i++) {
            s1 += (cnpj.charAt(i) - '0') * w1[i];
        }
        int r1 = s1 % 11;
        int d1 = r1 < 2 ? 0 : 11 - r1;
        int s2 = 0;
        for (int i = 0; i < 13; i++) {
            int val = (i < 12) ? (cnpj.charAt(i) - '0') : d1;
            s2 += val * w2[i];
        }
        int r2 = s2 % 11;
        int d2 = r2 < 2 ? 0 : 11 - r2;
        return d1 == (cnpj.charAt(12) - '0') && d2 == (cnpj.charAt(13) - '0');
    }

    private static boolean emailValido(String email) {
        return email != null && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }
}
