package com.jason.publisher.main.utils

import android.util.Log
import com.jason.publisher.main.activity.MapActivity
import java.text.SimpleDateFormat
import java.util.*

/**
 * Sistem logging lifecycle terstruktur untuk tracking lifecycle aplikasi.
 * 
 * Track lifecycle: SplashActivity → ScheduleActivity → MapActivity → BreakActivity
 * Log mencakup: route info, current stop, upcoming stop, ETA, schedule status
 * 
 * Throttling intervals:
 * - Location update: setiap 30 detik (bukan setiap detik)
 * - Stop passed: setiap 5 detik
 * - API time update: setiap 10 detik
 */
class LifecycleLogger(private val activity: MapActivity) {
    
    private var lastLocationLogTime = 0L
    private val locationLogInterval = 30_000L // 30 detik dalam milliseconds (diperlambat dari 10 detik)
    
    private var lastStopPassedLogTime = 0L
    private val stopPassedLogInterval = 5_000L // 5 detik untuk log stop passed
    
    private var lastApiTimeLogTime = 0L
    private val apiTimeLogInterval = 10_000L // 10 detik untuk log API time
    
    /**
     * Log lifecycle event dengan informasi lengkap.
     * 
     * @param event Nama event (contoh: "onCreate", "onResume", "onPause")
     * @param routeInfo Informasi route
     * @param currentStop Stop saat ini
     * @param upcomingStop Stop berikutnya
     * @param eta Estimated time of arrival
     * @param scheduleStatus Status schedule
     */
    fun logLifecycleEvent(
        event: String,
        routeInfo: String? = null,
        currentStop: String? = null,
        upcomingStop: String? = null,
        eta: String? = null,
        scheduleStatus: String? = null
    ) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val sb = StringBuilder()
        sb.appendLine("[$timestamp] Lifecycle: $event")
        
        routeInfo?.let { sb.appendLine("  Route: $it") }
        currentStop?.let { sb.appendLine("  Current Stop: $it") }
        upcomingStop?.let { sb.appendLine("  Upcoming Stop: $it") }
        eta?.let { sb.appendLine("  ETA: $it") }
        scheduleStatus?.let { sb.appendLine("  Schedule Status: $it") }
        
        FileLogger.d("LifecycleLogger", sb.toString().trimEnd())
    }
    
    /**
     * Log location update dengan throttling (setiap 10 detik).
     * 
     * @param latitude Latitude saat ini
     * @param longitude Longitude saat ini
     * @param currentStop Stop saat ini
     * @param upcomingStop Stop berikutnya
     * @param eta Estimated time of arrival
     * @param scheduleStatus Status schedule
     */
    fun logLocationUpdate(
        latitude: Double,
        longitude: Double,
        currentStop: String? = null,
        upcomingStop: String? = null,
        eta: String? = null,
        scheduleStatus: String? = null
    ) {
        val currentTime = System.currentTimeMillis()
        
        // Throttle: hanya log setiap 10 detik
        if (currentTime - lastLocationLogTime < locationLogInterval) {
            return
        }
        
        lastLocationLogTime = currentTime
        
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val sb = StringBuilder()
        sb.appendLine("[$timestamp] Location Update")
        sb.appendLine("  Lat: $latitude, Lng: $longitude")
        
        currentStop?.let { sb.appendLine("  Current Stop: $it") }
        upcomingStop?.let { sb.appendLine("  Upcoming Stop: $it") }
        eta?.let { sb.appendLine("  ETA: $it") }
        scheduleStatus?.let { sb.appendLine("  Schedule Status: $it") }
        
        FileLogger.d("LifecycleLogger", sb.toString().trimEnd())
    }
    
    /**
     * Log route info saat route berubah.
     */
    fun logRouteInfo(routeInfo: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        FileLogger.d("LifecycleLogger", "[$timestamp] Route Info: $routeInfo")
    }
    
    /**
     * Log stop passed dengan throttling (setiap 5 detik).
     * Hanya log jika sudah cukup waktu berlalu sejak log terakhir.
     */
    fun shouldLogStopPassed(): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastStopPassedLogTime < stopPassedLogInterval) {
            return false
        }
        lastStopPassedLogTime = currentTime
        return true
    }
    
    /**
     * Log API time update dengan throttling (setiap 10 detik).
     * Hanya log jika sudah cukup waktu berlalu sejak log terakhir.
     */
    fun shouldLogApiTime(): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastApiTimeLogTime < apiTimeLogInterval) {
            return false
        }
        lastApiTimeLogTime = currentTime
        return true
    }
}
