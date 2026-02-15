package br.com.lexcrm.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "documentos_checklist")
public class DocumentoChecklist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private boolean entregue;

    @ManyToOne
    @JoinColumn(name = "etapa_id")
    private EtapaProcesso etapa;
}
