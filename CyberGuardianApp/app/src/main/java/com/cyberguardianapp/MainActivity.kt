package com.cyberguardianapp

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cyberguardianapp.api.ApiClient
import com.cyberguardianapp.model.AppInfo
import com.cyberguardianapp.model.RiskLevel
import com.cyberguardianapp.service.AppMonitorAccessibilityService
import com.cyberguardianapp.service.AppScannerService
import com.cyberguardianapp.util.PermissionChecker
import kotlinx.coroutines.launch
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    
    private lateinit var scanButton: Button
    private lateinit var accessibilityButton: Button
    private lateinit var usageStatsButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var appListRecyclerView: RecyclerView
    
    private val appScannerService = AppScannerService()
    private val permissionChecker = PermissionChecker()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize views
        scanButton = findViewById(R.id.scanButton)
        accessibilityButton = findViewById(R.id.accessibilityButton)
        usageStatsButton = findViewById(R.id.usageStatsButton)
        progressBar = findViewById(R.id.progressBar)
        statusText = findViewById(R.id.statusText)
        appListRecyclerView = findViewById(R.id.appListRecyclerView)
        
        // Set up RecyclerView
        appListRecyclerView.layoutManager = LinearLayoutManager(this)
        
        // Set up button click listeners
        setupButtonListeners()
        
        // Check required permissions
        updatePermissionStatus()
    }
    
    private fun setupButtonListeners() {
        scanButton.setOnClickListener {
            if (!hasRequiredPermissions()) {
                Toast.makeText(this, "Please enable all required permissions first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            scanInstalledApps()
        }
        
        accessibilityButton.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }
        
        usageStatsButton.setOnClickListener {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startActivity(intent)
        }
    }
    
    private fun updatePermissionStatus() {
        val accessibilityEnabled = permissionChecker.isAccessibilityServiceEnabled(
            this, 
            AppMonitorAccessibilityService::class.java
        )
        val usageStatsEnabled = hasUsageStatsPermission()
        
        accessibilityButton.isEnabled = !accessibilityEnabled
        usageStatsButton.isEnabled = !usageStatsEnabled
        
        val allPermissionsGranted = accessibilityEnabled && usageStatsEnabled
        scanButton.isEnabled = allPermissionsGranted
        
        statusText.text = if (allPermissionsGranted) {
            "All permissions granted. Ready to scan."
        } else {
            "Please enable required permissions"
        }
    }
    
    private fun hasRequiredPermissions(): Boolean {
        val accessibilityEnabled = permissionChecker.isAccessibilityServiceEnabled(
            this, 
            AppMonitorAccessibilityService::class.java
        )
        val usageStatsEnabled = hasUsageStatsPermission()
        
        return accessibilityEnabled && usageStatsEnabled
    }
    
    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(), packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
    
    private fun scanInstalledApps() {
        progressBar.visibility = View.VISIBLE
        statusText.text = "Scanning installed applications..."
        
        lifecycleScope.launch {
            try {
                // Scan installed apps
                val installedApps = appScannerService.getInstalledApps(applicationContext)
                
                // Analyze apps and send to backend
                val analyzedApps = appScannerService.analyzeApps(installedApps)
                
                // Update UI with results
                updateAppList(analyzedApps)
                
                progressBar.visibility = View.GONE
                statusText.text = "Scan completed. Found ${analyzedApps.size} apps."
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                statusText.text = "Error: ${e.message}"
                Toast.makeText(this@MainActivity, "Scan failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun updateAppList(apps: List<AppInfo>) {
        appListRecyclerView.adapter = AppListAdapter(apps)
    }
    
    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }
}
