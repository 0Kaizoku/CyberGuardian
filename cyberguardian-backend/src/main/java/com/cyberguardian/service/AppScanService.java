package com.cyberguardian.service;

import  com.cyberguardian.model.Application;
import  com.cyberguardian.model.Permission;
import  com.cyberguardian.model.RiskPrediction;
import  com.cyberguardian.repository.ApplicationRepository;
import  com.cyberguardian.repository.PermissionRepository;
import  com.cyberguardian.repository.RiskPredictionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AppScanService {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private RiskPredictionRepository riskPredictionRepository;

    @Autowired
    private WebClient.Builder webClientBuilder;

    /**
     * Analyze an application by its permissions and other metadata.
     * Sends data to ML service and saves the prediction result.
     */
    public RiskPrediction analyzeApp(String packageName, String appName, List<String> permissionNames, Integer versionCode) {
        // Find or create application
        Application application = findOrCreateApplication(packageName, appName, versionCode);
        
        // Save permissions
        savePermissions(application, permissionNames);
        
        // Send to ML service for prediction
        RiskPrediction prediction = predictRisk(application, permissionNames);
        
        // Update app risk level based on prediction
        application.setRiskLevel(prediction.getRiskLabel());
        application.setLastScanDate(LocalDateTime.now());
        applicationRepository.save(application);
        
        return prediction;
    }
    
    /**
     * Find application by package name or create a new one
     */
    private Application findOrCreateApplication(String packageName, String appName, Integer versionCode) {
        return applicationRepository.findByPackageName(packageName)
                .map(app -> {
                    app.setAppName(appName);
                    app.setVersionCode(versionCode);
                    return app;
                })
                .orElseGet(() -> {
                    Application newApp = new Application();
                    newApp.setPackageName(packageName);
                    newApp.setAppName(appName);
                    newApp.setVersionCode(versionCode);
                    return applicationRepository.save(newApp);
                });
    }
    
    /**
     * Save or update permissions for an application
     */
    private void savePermissions(Application application, List<String> permissionNames) {
        // Clear existing permissions
        List<Permission> existingPermissions = permissionRepository.findByApplicationId(application.getId());
        if (!existingPermissions.isEmpty()) {
            permissionRepository.deleteAll(existingPermissions);
        }
        
        // Add new permissions
        List<Permission> permissions = new ArrayList<>();
        for (String name : permissionNames) {
            Permission permission = new Permission();
            permission.setApplication(application);
            permission.setPermissionName(name);
            permission.setIsSuspicious(isDangerousPermission(name));
            permissions.add(permission);
        }
        
        permissionRepository.saveAll(permissions);
    }
    
    /**
     * Check if a permission is considered dangerous
     */
    private boolean isDangerousPermission(String permissionName) {
        return permissionName.contains("SMS") || 
               permissionName.contains("CALL") ||
               permissionName.contains("LOCATION") ||
               permissionName.contains("CAMERA") ||
               permissionName.contains("RECORD_AUDIO") ||
               permissionName.contains("CONTACTS") ||
               permissionName.contains("STORAGE");
    }
    
    /**
     * Predict risk by calling ML Service
     */
    private RiskPrediction predictRisk(Application application, List<String> permissions) {
        // Create request payload
        Map<String, Object> requestData = Map.of(
            "package_name", application.getPackageName(),
            "app_name", application.getAppName(),
            "permissions", permissions
        );
        
        // Call ML service (FastAPI)
        Map<String, Object> response = webClientBuilder.build()
            .post()
            .uri("http://localhost:8000/predict")
            .bodyValue(requestData)
            .retrieve()
            .bodyToMono(Map.class)
            .block();
        
        // Extract prediction results
        Double riskScore = (Double) response.get("risk_score");
        String riskLabel = (String) response.get("risk_label");
        
        // Create and save risk prediction
        RiskPrediction prediction = new RiskPrediction();
        prediction.setApplication(application);
        prediction.setRiskScore(riskScore);
        prediction.setRiskLabel(riskLabel);
        prediction.setTimestamp(LocalDateTime.now());
        
        return riskPredictionRepository.save(prediction);
    }
    
    /**
     * Get latest risk report for an app by package name
     */
    public Optional<RiskPrediction> getRiskReport(String packageName) {
        return riskPredictionRepository.findLatestByPackageName(packageName);
    }
    
    /**
     * Get all high-risk applications
     */
    public List<Application> getRiskyApps() {
        return applicationRepository.findAll().stream()
            .filter(app -> "suspicious".equals(app.getRiskLevel()) || "malicious".equals(app.getRiskLevel()))
            .toList();
    }
}
