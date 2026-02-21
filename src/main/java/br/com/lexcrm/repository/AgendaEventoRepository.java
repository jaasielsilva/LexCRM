package br.com.lexcrm.repository;

import br.com.lexcrm.model.AgendaEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AgendaEventoRepository extends JpaRepository<AgendaEvento, Long> {
    @Query("""
            select e from AgendaEvento e
            where e.tenantId = :tenantId
              and (:responsavelId is null or (e.responsavel is not null and e.responsavel.id = :responsavelId))
              and e.inicio >= :from
              and e.inicio < :to
            order by e.inicio asc
            """)
    List<AgendaEvento> listByRange(@Param("tenantId") String tenantId,
                                  @Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to,
                                  @Param("responsavelId") Long responsavelId);

    @Query("""
            select e from AgendaEvento e
            where e.tenantId = :tenantId
              and (:responsavelId is null or (e.responsavel is not null and e.responsavel.id = :responsavelId))
              and e.inicio < :to
              and e.fim > :from
            order by e.inicio asc
            """)
    List<AgendaEvento> listOverlappingRange(@Param("tenantId") String tenantId,
                                           @Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to,
                                           @Param("responsavelId") Long responsavelId);

    @Query("""
            select e from AgendaEvento e
            where e.tenantId = :tenantId
              and e.status <> 'CANCELADO'
              and e.responsavel is not null
              and e.responsavel.id = :responsavelId
              and e.inicio < :fim
              and e.fim > :inicio
              and (:excludeId is null or e.id <> :excludeId)
            """)
    List<AgendaEvento> findConflicts(@Param("tenantId") String tenantId,
                                    @Param("responsavelId") Long responsavelId,
                                    @Param("inicio") LocalDateTime inicio,
                                    @Param("fim") LocalDateTime fim,
                                    @Param("excludeId") Long excludeId);

    @Query("""
            select e from AgendaEvento e
            where e.tenantId = :tenantId
              and e.status <> 'CANCELADO'
              and e.responsavel is not null
              and e.inicio >= :from
              and e.inicio < :to
            order by e.inicio asc
            """)
    List<AgendaEvento> findStartingInWindow(@Param("tenantId") String tenantId,
                                           @Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to);
}
