package br.com.lexcrm.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "processos")
public class Processo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String numeroProcesso;
    private String titulo;
    private String status; // Pendente, Em Andamento, Concluído
    private String tipo; // Cível, Trabalhista, Médico

    private String vara;
    private String comarca;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "advogado_responsavel_id")
    private Usuario advogadoResponsavel;

    private LocalDate dataAbertura;
    private BigDecimal valorCausa;

    @OneToMany(mappedBy = "processo", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<EtapaProcesso> etapas = new ArrayList<>();

    // Multi-tenancy
    private String tenantId;

    @Transient
    public int getProgresso() {
        if (etapas == null || etapas.isEmpty()) {
            return 0;
        }
        long concluidas = etapas.stream()
                .filter(e -> "Concluído".equalsIgnoreCase(e.getStatus()))
                .count();
        return (int) ((concluidas * 100) / etapas.size());
    }
}
