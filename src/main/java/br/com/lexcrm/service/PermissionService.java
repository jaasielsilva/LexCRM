package br.com.lexcrm.service;

import br.com.lexcrm.model.Role;
import br.com.lexcrm.model.RolePermission;
import br.com.lexcrm.repository.RolePermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class PermissionService {

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    public Set<String> getAuthoritiesForRole(Role role) {
        List<RolePermission> list = rolePermissionRepository.findByRole(role);
        Set<String> out = new LinkedHashSet<>();
        for (RolePermission rp : list) {
            out.add(rp.getPermission());
        }
        return out;
    }

    public void replacePermissions(Role role, Set<String> permissions) {
        // Remove current
        List<RolePermission> current = rolePermissionRepository.findByRole(role);
        rolePermissionRepository.deleteAll(current);
        // Add new
        for (String p : permissions) {
            RolePermission rp = new RolePermission();
            rp.setRole(role);
            rp.setPermission(p);
            rolePermissionRepository.save(rp);
        }
    }
}
