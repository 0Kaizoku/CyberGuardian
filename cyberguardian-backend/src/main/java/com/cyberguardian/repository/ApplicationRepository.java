package com.cyberguardian.repository;

import com.cyberguardian.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    Optional<Application> findByPackageName(String packageName);
    boolean existsByPackageName(String packageName);
}
