package com.jason.publisher.main.utils

/**
 * Helper untuk format waktu, khususnya untuk "late for next run".
 * 
 * Format:
 * - Jika >= 60 detik: "xx mins yy seconds"
 * - Jika < 60 detik: "xx seconds"
 */
object TimeFormatHelper {
    
    /**
     * Format waktu dalam detik menjadi string yang mudah dibaca.
     * 
     * @param seconds Jumlah detik (bisa negatif, akan diambil nilai absolutnya)
     * @return String yang diformat sesuai aturan
     */
    fun formatLateTime(seconds: Int): String {
        val absSeconds = kotlin.math.abs(seconds)
        
        return if (absSeconds >= 60) {
            val mins = absSeconds / 60
            val secs = absSeconds % 60
            if (secs == 0) {
                "${mins} min${if (mins != 1) "s" else ""}"
            } else {
                "${mins} min${if (mins != 1) "s" else ""} ${secs} second${if (secs != 1) "s" else ""}"
            }
        } else {
            "${absSeconds} second${if (absSeconds != 1) "s" else ""}"
        }
    }
}
