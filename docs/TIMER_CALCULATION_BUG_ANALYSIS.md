# Analisis Bug: Next Run & Late Calculation

## Masalah yang Ditemukan

Dari screenshot, terlihat:
- **"Next run in: 1250 mins 21 seconds"** - Sangat tidak normal (lebih dari 20 jam!)
- **"Late for next run by 220 mins"** - Sangat tidak normal (lebih dari 3 jam!)

## Root Cause Analysis

### 🔴 Masalah Utama: Mismatch antara Current Time Display dan Calculation Time

**Penyebab:**
Setelah fix timer menggunakan `startCurrentTimeUpdater()`, ada **mismatch** antara:
1. **Waktu yang ditampilkan di UI** → Menggunakan `Date()` (waktu real tablet)
2. **Waktu yang digunakan untuk perhitungan** → Masih menggunakan `simulatedStartTime` (waktu lama/tidak di-update)

### Detail Masalah

#### 1. "Next run in: 1250 mins" - TimeManager.kt

**Lokasi:** `TimeManager.kt` line 127 dan `MapActivity.kt` line 2013

**Kode Masalah:**
```kotlin
// TimeManager.kt startNextTripCountdownUpdater()
val currentTime = simulatedStartTime.clone() as Calendar  // ❌ MASALAH DI SINI
val nextTripStartTime = getNextScheduleStartTime()

if (nextTripStartTime != null) {
    val timeParts = nextTripStartTime.split(":").map { it.toInt() }
    val nextTripCalendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, currentTime.get(Calendar.YEAR))
        set(Calendar.MONTH, currentTime.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, currentTime.get(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, timeParts[0])
        set(Calendar.MINUTE, timeParts[1])
        set(Calendar.SECOND, 0)
        if (timeInMillis <= currentTime.timeInMillis) add(Calendar.DATE, 1)
    }
    val diff = nextTripCalendar.timeInMillis - currentTime.timeInMillis
    // ... calculate mins and secs
}
```

**Masalah:**
- `simulatedStartTime` **TIDAK di-update** ketika menggunakan `startCurrentTimeUpdater()`
- `startCurrentTimeUpdater()` hanya update `currentTimeTextView.text` dengan `Date()`, tapi **TIDAK update `simulatedStartTime`**
- `simulatedStartTime` masih berisi waktu lama (mungkin schedule start time atau waktu saat pertama kali di-set)
- Ketika menghitung "Next run in", menggunakan `simulatedStartTime` yang sudah **ketinggalan jauh** dari waktu real

**Contoh:**
- Current time real tablet: **13:51:14** (dari screenshot)
- `simulatedStartTime` mungkin masih: **10:00:00** (schedule start time)
- Next trip: **10:00:00** (besok)
- Perhitungan: Next trip (besok 10:00) - `simulatedStartTime` (hari ini 10:00) = **24 jam = 1440 menit**
- Tapi karena `simulatedStartTime` mungkin lebih lama lagi, bisa jadi **1250 menit**

#### 2. "Late for next run by 220 mins" - ScheduleStatusManager.kt

**Lokasi:** `ScheduleStatusManager.kt` line 380

**Kode Masalah:**
```kotlin
// ScheduleStatusManager.kt overrideLateStatusForNextSchedule()
val predictedArrival = Calendar.getInstance().apply {
    time = activity.timeManager.simulatedStartTime.time  // ❌ MASALAH DI SINI
    add(Calendar.SECOND, t1.toInt())
}

val nextScheduleStartTime = activity.timeManager.parseTimeToday(nextScheduleStartStr)
val deltaNextSec = ((nextScheduleStartTime.time - predictedArrival.time.time) / 1000).toInt()
```

**Masalah:**
- `predictedArrival` menggunakan `simulatedStartTime.time` sebagai base
- Tapi `simulatedStartTime` tidak di-update, jadi base time-nya salah
- Predicted arrival menjadi tidak akurat
- Perhitungan "late" menjadi salah

**Contoh:**
- Current time real: **13:51:14**
- `simulatedStartTime`: **10:00:00** (ketinggalan ~4 jam)
- Predicted arrival dihitung dari **10:00:00** + t1, bukan dari **13:51:14** + t1
- Hasilnya predicted arrival terlalu awal
- Next trip start time - predicted arrival = **220 menit** (karena base time salah)

### 🔍 Analisis Kode

#### startCurrentTimeUpdater() - TimeManager.kt

```kotlin
fun startCurrentTimeUpdater() {
    currentTimeHandler = Handler(Looper.getMainLooper())
    currentTimeRunnable = object : Runnable {
        override fun run() {
            val currentTimeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val nowStr = currentTimeFormat.format(Date())  // ✅ Menggunakan Date()
            owner.currentTimeTextView.text = nowStr
            // ❌ TIDAK UPDATE simulatedStartTime!
            currentTimeHandler?.postDelayed(this, 1000)
        }
    }
    currentTimeHandler?.post(currentTimeRunnable!!)
}
```

**Masalah:**
- Hanya update UI text dengan `Date()`
- **TIDAK update `simulatedStartTime`**
- `simulatedStartTime` tetap berisi nilai lama

#### startNextTripCountdownUpdater() - TimeManager.kt

```kotlin
fun startNextTripCountdownUpdater() {
    nextTripRunnable = object : Runnable {
        override fun run() {
            val currentTime = simulatedStartTime.clone() as Calendar  // ❌ Menggunakan simulatedStartTime
            val nextTripStartTime = getNextScheduleStartTime()
            // ... calculate diff
        }
    }
}
```

**Masalah:**
- Menggunakan `simulatedStartTime` yang tidak di-update
- Seharusnya menggunakan waktu real tablet (`Date()` atau `Calendar.getInstance()`)

## Kesimpulan

### Apakah ini masalah dari data?
**TIDAK.** Data schedule-nya kemungkinan benar. Masalahnya ada di **logic perhitungan**.

### Apakah ini bug di get/set?
**YA.** Ada bug di:
1. **Get:** Menggunakan `simulatedStartTime` yang tidak di-update
2. **Set:** `startCurrentTimeUpdater()` tidak update `simulatedStartTime`

### Root Cause Summary

1. **Timer Display Fix** menggunakan `startCurrentTimeUpdater()` yang hanya update UI, tidak update `simulatedStartTime`
2. **Calculation masih menggunakan `simulatedStartTime`** yang tidak di-update
3. **Mismatch** antara waktu yang ditampilkan (real time) dan waktu yang digunakan untuk perhitungan (stale time)
4. Hasilnya: Perhitungan "Next run in" dan "Late for next run" menjadi **sangat tidak akurat**

## Solusi yang Diperlukan

### Opsi 1: Update simulatedStartTime di startCurrentTimeUpdater()
```kotlin
fun startCurrentTimeUpdater() {
    currentTimeRunnable = object : Runnable {
        override fun run() {
            val now = Date()
            val currentTimeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val nowStr = currentTimeFormat.format(now)
            owner.currentTimeTextView.text = nowStr
            
            // ✅ UPDATE simulatedStartTime juga
            simulatedStartTime.time = now
            simulatedStartTime.add(Calendar.SECOND, 1)
            
            currentTimeHandler?.postDelayed(this, 1000)
        }
    }
}
```

### Opsi 2: Gunakan Date() langsung untuk perhitungan
```kotlin
// Di startNextTripCountdownUpdater()
val currentTime = Calendar.getInstance()  // ✅ Gunakan waktu real, bukan simulatedStartTime
```

### Opsi 3: Sync simulatedStartTime dengan current time saat startCurrentTimeUpdater() dipanggil
```kotlin
fun startCurrentTimeUpdater() {
    // ✅ Sync simulatedStartTime dengan current time saat ini
    simulatedStartTime.time = Date()
    
    currentTimeRunnable = object : Runnable {
        override fun run() {
            val now = Date()
            simulatedStartTime.time = now
            owner.currentTimeTextView.text = timeFormat.format(now)
            currentTimeHandler?.postDelayed(this, 1000)
        }
    }
}
```

## Rekomendasi

**Rekomendasi:** Gunakan **Opsi 1** atau **Opsi 3** karena:
- `simulatedStartTime` masih digunakan di banyak tempat untuk perhitungan
- Lebih aman untuk update `simulatedStartTime` agar sinkron dengan waktu real
- Tidak perlu mengubah semua tempat yang menggunakan `simulatedStartTime`

## Testing Checklist Setelah Fix

- [ ] "Next run in" menampilkan waktu yang masuk akal (< 24 jam)
- [ ] "Late for next run" menampilkan waktu yang masuk akal
- [ ] Perhitungan menggunakan waktu yang sama dengan yang ditampilkan di UI
- [ ] Tidak ada mismatch antara display time dan calculation time


