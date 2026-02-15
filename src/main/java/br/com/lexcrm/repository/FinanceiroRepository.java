package br.com.lexcrm.repository;

import br.com.lexcrm.model.Financeiro;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import java.math.BigDecimal;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FinanceiroRepository extends JpaRepository<Financeiro, Long> {
    List<Financeiro> findByStatusAndDescricaoContaining(String status, String descricao);

    @Query("SELECT COALESCE(SUM(f.valor), 0) FROM Financeiro f WHERE f.status = :status")
    BigDecimal sumValorByStatus(@Param("status") String status);
}
