package br.com.lexcrm.repository;

import br.com.lexcrm.model.AccessPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccessPolicyRepository extends JpaRepository<AccessPolicy, Long> {
    Optional<AccessPolicy> findFirstByTenantId(String tenantId);
}

