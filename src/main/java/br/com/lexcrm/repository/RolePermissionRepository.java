package br.com.lexcrm.repository;

import br.com.lexcrm.model.Role;
import br.com.lexcrm.model.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    List<RolePermission> findByRole(Role role);
    boolean existsByRoleAndPermission(Role role, String permission);
}
