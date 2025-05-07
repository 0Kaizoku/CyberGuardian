package com.cyberguardian.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "risk_predictions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskPrediction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;
    
    @Column(name = "risk_score")
    private Double riskScore;
    
    @Column(name = "risk_label")
    private String riskLabel;
    
    @Column(name = "timestamp")
    private LocalDateTime timestamp = LocalDateTime.now();
}
