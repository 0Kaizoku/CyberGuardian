package com.cyberguardianapp.util

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.provider.Settings
import android.text.TextUtils

/**
 * Utility class to check permissions and app states
 */
class PermissionChecker {
    
    // List of dangerous permissions that may indicate malicious behavior
    private val dangerousPermissions = listOf(
        "android.permission.READ_SMS",
        "android.permission.SEND_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.CALL_PHONE",
        "android.permission.READ_CONTACTS",
        "android.permission.WRITE_CONTACTS",
        "android.permission.RECORD_AUDIO",
        "android.permission.CAMERA",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.READ_PHONE_STATE",
        "android.permission.READ_CALL_LOG",
        "android.permission.WRITE_CALL_LOG",
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE"
    )
    
    /**
     * Check if a permission is considered dangerous
     */
    fun isDangerousPermission(permission: String): Boolean {
        return dangerousPermissions.contains(permission)
    }
    
    /**
     * Check if an accessibility service is enabled
     */
    fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<out AccessibilityService>): Boolean {
        val expectedServiceName = context.packageName + "/" + serviceClass.name
        
        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)
        
        while (colonSplitter.hasNext()) {
            val enabledService = colonSplitter.next()
            if (enabledService.equals(expectedServiceName, ignoreCase = true)) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Calculate permission risk score based on the permissions list
     */
    fun calculatePermissionBasedRiskScore(permissions: List<String>): Double {
        if (permissions.isEmpty()) {
            return 0.0
        }
        
        val dangerousCount = permissions.count { isDangerousPermission(it) }
        
        // Risk score calculation formula:
        // 1. Base score from percentage of dangerous permissions
        // 2. Weighted by specific combinations of permissions
        
        val baseScore = dangerousCount.toDouble() / permissions.size.toDouble()
        
        // Check for risky permission combinations (e.g., SMS + INTERNET)
        val hasInternetPerm = permissions.any { it.contains("INTERNET") }
        val hasSmsRelatedPerm = permissions.any { it.contains("SMS") }
        val hasLocationPerm = permissions.any { it.contains("LOCATION") }
        
        var combinationBonus = 0.0
        
        // SMS + INTERNET is a classic combination for data exfiltration
        if (hasInternetPerm && hasSmsRelatedPerm) {
            combinationBonus += 0.2
        }
        
        // Location + INTERNET can indicate location tracking
        if (hasInternetPerm && hasLocationPerm) {
            combinationBonus += 0.1
        }
        
        return (baseScore + combinationBonus).coerceAtMost(1.0)
    }
}
