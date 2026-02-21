package br.com.lexcrm.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "notificacoes")
public class Notificacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private NotificacaoTipo tipo;

    private String titulo;

    @Column(length = 2000)
    private String mensagem;

    private String link;

    @Column(unique = true)
    private String codigo;

    private LocalDateTime createdAt;

    private LocalDateTime lidaEm;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    private String tenantId;
}
