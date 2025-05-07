package com.cyberguardian.repository;

import com.cyberguardian.model.RiskPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RiskPredictionRepository extends JpaRepository<RiskPrediction, Long> {
    List<RiskPrediction> findByApplicationId(Long applicationId);
    
    @Query("SELECT rp FROM RiskPrediction rp WHERE rp.application.id = :applicationId ORDER BY rp.timestamp DESC")
    List<RiskPrediction> findLatestByApplicationId(Long applicationId);
    
    @Query("SELECT rp FROM RiskPrediction rp WHERE rp.application.packageName = :packageName ORDER BY rp.timestamp DESC")
    Optional<RiskPrediction> findLatestByPackageName(String packageName);
}
