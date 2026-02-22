package br.com.lexcrm.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "access_policies")
@Data
public class AccessPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private boolean mfaEnabled;

    private Integer maxLoginAttempts;

    private Integer passwordExpiryDays;

    private String tenantId;
}

