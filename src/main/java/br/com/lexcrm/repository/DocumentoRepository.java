package br.com.lexcrm.repository;

import br.com.lexcrm.model.Documento;
import br.com.lexcrm.model.Processo;
import br.com.lexcrm.model.EtapaProcesso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Long> {

    List<Documento> findByProcesso(Processo processo);

    List<Documento> findByEtapa(EtapaProcesso etapa);

    List<Documento> findByProcessoAndTipo(Processo processo, String tipo);

    List<Documento> findByTenantId(String tenantId);
}
