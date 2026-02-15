package br.com.lexcrm.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "clientes")
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String cpfCnpj;
    private String email;
    private String telefone;

    // Multi-tenancy
    private String tenantId;
}
