package br.com.lexcrm.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "etapas_processo")
public class EtapaProcesso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String descricao;
    private String status; // Pendente, Em Andamento, Concluido
    private Integer ordem;
    private LocalDate data;

    @ManyToOne
    @JoinColumn(name = "processo_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Processo processo;

    @OneToMany(mappedBy = "etapa", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private java.util.List<DocumentoChecklist> checklist = new java.util.ArrayList<>();
}
