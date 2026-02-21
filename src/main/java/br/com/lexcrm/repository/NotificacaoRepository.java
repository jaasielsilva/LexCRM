package br.com.lexcrm.repository;

import br.com.lexcrm.model.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {
    boolean existsByCodigo(String codigo);

    @Query("""
            select n from Notificacao n
            where n.tenantId = :tenantId
              and n.lidaEm is null
              and (n.usuario is null or n.usuario.username = :username)
            order by n.createdAt desc
            """)
    List<Notificacao> findUnreadForUser(@Param("tenantId") String tenantId, @Param("username") String username);

    @Query("""
            select count(n) from Notificacao n
            where n.tenantId = :tenantId
              and n.lidaEm is null
              and (n.usuario is null or n.usuario.username = :username)
            """)
    long countUnreadForUser(@Param("tenantId") String tenantId, @Param("username") String username);
}
