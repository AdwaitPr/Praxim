package com.praxim.core.resilience

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

class OemResilienceDispatcher(private val context: Context) {
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    fun dispatchBatteryOptimizationIntent(): BatteryOptimizationResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
            return BatteryOptimizationResult.AlreadyExempt
        }

        val intents = getOemIntents()
        val packageManager = context.packageManager

        for (intent in intents) {
            val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolveInfo != null) {
                try {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return BatteryOptimizationResult.OemIntentDispatched(intent.component?.packageName ?: "unknown")
                } catch (e: Exception) {
                    Log.e("OemResilienceDispatcher", "Failed to dispatch intent", e)
                }
            }
        }

        // Fallback to standard settings
        return try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            BatteryOptimizationResult.FallbackStandardSettingsDispatched
        } catch (e: Exception) {
            Log.e("OemResilienceDispatcher", "Failed to dispatch fallback intent", e)
            BatteryOptimizationResult.DispatchFailed
        }
    }

    private fun getOemIntents(): List<Intent> {
        return listOf(
            // Xiaomi / Redmi (MIUI/HyperOS)
            Intent().setClassName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
            Intent().setClassName("com.miui.securitycenter", "com.miui.securityscan.MainActivity"),
            // Samsung (OneUI)
            Intent().setClassName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"),
            Intent().setClassName("com.samsung.android.lool", "com.samsung.android.sm"),
            // OPPO / OnePlus (ColorOS/OxygenOS)
            Intent().setClassName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
            Intent().setClassName("com.coloros.safecenter", "com.oppo.safe"),
            // Vivo / iQOO (Funtouch/OriginOS)
            Intent().setClassName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
            Intent().setClassName("com.iqoo.secure", "com.iqoo.secure.MainGuideActivity"),
            // Huawei / Honor (EMUI/MagicOS)
            Intent().setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")
        )
    }
}
