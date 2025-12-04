# Fix: Timer Calculation Bug - Next Run & Late Calculation

## Masalah yang Diperbaiki

- **"Next run in: 1250 mins"** - Waktu tidak masuk akal (lebih dari 20 jam)
- **"Late for next run by 220 mins"** - Waktu tidak masuk akal (lebih dari 3 jam)

## Root Cause

Setelah fix timer menggunakan `startCurrentTimeUpdater()`, terjadi **mismatch** antara:
1. **Waktu yang ditampilkan di UI** → Menggunakan `Date()` (waktu real tablet) ✅
2. **Waktu yang digunakan untuk perhitungan** → Masih menggunakan `simulatedStartTime` yang **TIDAK di-update** ❌

### Detail Masalah

**TimeManager.kt - startCurrentTimeUpdater():**
```kotlin
// SEBELUM FIX
fun startCurrentTimeUpdater() {
    currentTimeRunnable = object : Runnable {
        override fun run() {
            val nowStr = currentTimeFormat.format(Date())  // ✅ Update UI
            owner.currentTimeTextView.text = nowStr
            // ❌ TIDAK update simulatedStartTime!
        }
    }
}
```

**TimeManager.kt - startNextTripCountdownUpdater():**
```kotlin
// SEBELUM FIX
val currentTime = simulatedStartTime.clone() as Calendar  // ❌ Masih pakai waktu lama
```

**ScheduleStatusManager.kt - overrideLateStatusForNextSchedule():**
```kotlin
// SEBELUM FIX
val predictedArrival = Calendar.getInstance().apply {
    time = activity.timeManager.simulatedStartTime.time  // ❌ Masih pakai waktu lama
    add(Calendar.SECOND, t1.toInt())
}
```

## Solusi yang Diimplementasikan

### Fix 1: Update simulatedStartTime di startCurrentTimeUpdater()

**File:** `TimeManager.kt`

**Perubahan:**
```kotlin
// SESUDAH FIX
fun startCurrentTimeUpdater() {
    // ✅ FIX: Sync simulatedStartTime with current tablet time at initialization
    val now = Date()
    simulatedStartTime.time = now
    
    currentTimeHandler = Handler(Looper.getMainLooper())
    currentTimeRunnable = object : Runnable {
        override fun run() {
            try {
                val now = Date()
                val currentTimeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val nowStr = currentTimeFormat.format(now)
                owner.currentTimeTextView.text = nowStr
                
                // ✅ FIX: Update simulatedStartTime to keep it in sync with real time
                // This ensures calculations use the same time as displayed in UI
                simulatedStartTime.time = now
                
                currentTimeHandler?.postDelayed(this, 1000)
            } catch (e: Exception) {
                Log.e("TimeManager", "Error in current time runnable: ${e.message}", e)
            }
        }
    }
    currentTimeHandler?.post(currentTimeRunnable!!)
}
```

**Penjelasan:**
- Saat `startCurrentTimeUpdater()` dipanggil, langsung sync `simulatedStartTime` dengan waktu real tablet
- Setiap detik, update `simulatedStartTime` agar selalu sinkron dengan waktu real
- Sekarang semua perhitungan yang menggunakan `simulatedStartTime` akan menggunakan waktu yang benar

## Dampak Fix

### Sebelum Fix:
- ❌ `simulatedStartTime` tidak di-update, tetap berisi waktu lama
- ❌ Perhitungan "Next run in" menggunakan waktu lama → hasil tidak masuk akal (1250 menit)
- ❌ Perhitungan "Late for next run" menggunakan waktu lama → hasil tidak masuk akal (220 menit)
- ❌ Mismatch antara waktu yang ditampilkan dan waktu yang digunakan untuk perhitungan

### Sesudah Fix:
- ✅ `simulatedStartTime` selalu sinkron dengan waktu real tablet
- ✅ Perhitungan "Next run in" menggunakan waktu yang benar → hasil masuk akal
- ✅ Perhitungan "Late for next run" menggunakan waktu yang benar → hasil masuk akal
- ✅ Tidak ada mismatch antara waktu yang ditampilkan dan waktu yang digunakan untuk perhitungan

## Files Modified

1. **TimeManager.kt**
   - `startCurrentTimeUpdater()` - Update `simulatedStartTime` setiap detik

## Testing Checklist

- [x] `simulatedStartTime` di-update setiap detik saat `startCurrentTimeUpdater()` aktif
- [ ] "Next run in" menampilkan waktu yang masuk akal (< 24 jam)
- [ ] "Late for next run" menampilkan waktu yang masuk akal
- [ ] Perhitungan menggunakan waktu yang sama dengan yang ditampilkan di UI
- [ ] Tidak ada mismatch antara display time dan calculation time

## Catatan

- Fix ini memastikan `simulatedStartTime` selalu sinkron dengan waktu real tablet
- Semua perhitungan yang menggunakan `simulatedStartTime` akan otomatis menggunakan waktu yang benar
- Tidak perlu mengubah kode di tempat lain karena mereka sudah menggunakan `simulatedStartTime` dengan benar

## Related Files

- `TimeManager.kt` - Fix utama
- `ScheduleStatusManager.kt` - Menggunakan `simulatedStartTime` (akan otomatis benar)
- `MapActivity.kt` - Menggunakan `simulatedStartTime` (akan otomatis benar)


