package com.cyberguardian.controller;

import com.cyberguardian.model.Application;
import com.cyberguardian.model.RiskPrediction;
import com.cyberguardian.service.AppScanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class ScanController {

    @Autowired
    private AppScanService appScanService;

    @PostMapping("/analyzeApp")
    public ResponseEntity<?> analyzeApp(@RequestBody Map<String, Object> request) {
        try {
            String packageName = (String) request.get("package_name");
            String appName = (String) request.get("app_name");
            @SuppressWarnings("unchecked")
            List<String> permissions = (List<String>) request.get("permissions");
            Integer versionCode = request.get("version_code") != null ? 
                Integer.parseInt(request.get("version_code").toString()) : 1;
            
            if (packageName == null || appName == null || permissions == null) {
                return ResponseEntity.badRequest().body("Missing required fields");
            }
            
            RiskPrediction prediction = appScanService.analyzeApp(packageName, appName, permissions, versionCode);
            
            return ResponseEntity.ok(Map.of(
                "package_name", packageName,
                "risk_score", prediction.getRiskScore(),
                "risk_label", prediction.getRiskLabel(),
                "timestamp", prediction.getTimestamp()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error analyzing app: " + e.getMessage());
        }
    }
    
    @GetMapping("/riskReport/{packageName}")
    public ResponseEntity<?> getRiskReport(@PathVariable String packageName) {
        return appScanService.getRiskReport(packageName)
            .map(prediction -> ResponseEntity.ok(Map.of(
                "package_name", packageName,
                "risk_score", prediction.getRiskScore(),
                "risk_label", prediction.getRiskLabel(),
                "timestamp", prediction.getTimestamp()
            )))
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/apps/risky")
    public ResponseEntity<List<Application>> getRiskyApps() {
        List<Application> riskyApps = appScanService.getRiskyApps();
        return ResponseEntity.ok(riskyApps);
    }
    
    @GetMapping("/permissions/suspicious")
    public ResponseEntity<?> getSuspiciousPermissions() {
        // For future implementation
        return ResponseEntity.ok(Map.of(
            "message", "This endpoint will return a list of suspicious permissions (to be implemented)"
        ));
    }
}
