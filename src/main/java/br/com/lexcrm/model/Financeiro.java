package br.com.lexcrm.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "financeiro")
public class Financeiro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String descricao; // Ex: "Honorários Médicos"
    private BigDecimal valor;
    private LocalDate dataVencimento;
    private String status; // Pendente, Pago, Atrasado
    
    @ManyToOne
    @JoinColumn(name = "processo_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Processo processo;

    // Multi-tenancy
    private String tenantId;
}
