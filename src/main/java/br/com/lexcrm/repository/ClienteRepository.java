package br.com.lexcrm.repository;

import br.com.lexcrm.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    List<Cliente> findTop50ByNomeContainingIgnoreCaseOrCpfCnpjContainingIgnoreCaseOrEmailContainingIgnoreCaseOrTelefoneContainingIgnoreCase(
            String nome,
            String cpfCnpj,
            String email,
            String telefone);

    boolean existsByCpfCnpj(String cpfCnpj);

    boolean existsByCpfCnpjAndIdNot(String cpfCnpj, Long id);

    List<br.com.lexcrm.model.Cliente> findTop3ByOrderByIdDesc();

    List<br.com.lexcrm.model.Cliente> findTop5ByOrderByIdDesc();

    long countByCreatedAtAfter(java.time.LocalDateTime createdAt);
}
