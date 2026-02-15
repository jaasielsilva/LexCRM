package br.com.lexcrm.repository;

import br.com.lexcrm.model.Processo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ProcessoRepository extends JpaRepository<Processo, Long> {
    List<Processo> findByStatus(String status);
    
    long countByStatus(String status);

    boolean existsByNumeroProcessoAndTenantId(String numeroProcesso, String tenantId);

    @EntityGraph(attributePaths = {"cliente", "advogadoResponsavel", "etapas"})
    @Query("SELECT p FROM Processo p")
    List<Processo> findAllWithDetails();
    
    // Top 5 Recents
    @EntityGraph(attributePaths = {"cliente", "advogadoResponsavel"})
    List<Processo> findTop5ByStatusOrderByDataAberturaDesc(String status);

    @EntityGraph(attributePaths = {"cliente", "advogadoResponsavel"})
    List<Processo> findTop5ByOrderByDataAberturaDesc();

    @Query("SELECT p FROM Processo p WHERE " +
           "(:termo IS NULL OR LOWER(p.numeroProcesso) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(p.cliente.nome) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(p.tipo) LIKE LOWER(CONCAT('%', :termo, '%')))")
    List<Processo> buscarPorTermo(@Param("termo") String termo);
}
