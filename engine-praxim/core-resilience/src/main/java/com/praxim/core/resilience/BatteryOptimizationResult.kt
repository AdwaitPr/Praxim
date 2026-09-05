package com.praxim.core.resilience

sealed class BatteryOptimizationResult {
    object AlreadyExempt : BatteryOptimizationResult()
    data class OemIntentDispatched(val intentPackage: String) : BatteryOptimizationResult()
    object FallbackStandardSettingsDispatched : BatteryOptimizationResult()
    object DispatchFailed : BatteryOptimizationResult()
}
