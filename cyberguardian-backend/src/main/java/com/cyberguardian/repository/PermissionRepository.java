package com.cyberguardian.repository;

import com.cyberguardian.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    List<Permission> findByApplicationId(Long applicationId);
    List<Permission> findByIsSuspiciousTrue();
}
