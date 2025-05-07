package com.cyberguardian.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "applications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "package_name", unique = true, nullable = false)
    private String packageName;
    
    @Column(name = "app_name", nullable = false)
    private String appName;
    
    @Column(name = "version_code")
    private Integer versionCode;
    
    @Column(name = "risk_level")
    private String riskLevel;
    
    @Column(name = "last_scan_date")
    private LocalDateTime lastScanDate = LocalDateTime.now();
    
    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Permission> permissions = new ArrayList<>();
    
    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RiskPrediction> riskPredictions = new ArrayList<>();
}
