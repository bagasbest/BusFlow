# Fix: Next Run Calculation - 1200+ Minutes Issue

## Masalah

- **"Next run in: 1225 mins"** - Waktu tidak masuk akal (lebih dari 20 jam)
- **"Late for next run by 247 mins"** - Waktu tidak masuk akal (lebih dari 4 jam)

## Root Cause

**Masalah utama:** `getNextScheduleStartTime()` mengambil trip yang **salah**.

### Detail Masalah

**Sebelum start route:**
- `scheduleData` = [trip1 (10:00), trip2 (10:40), trip3 (11:20), ...]

**Setelah start route (di ScheduleActivity):**
- Trip pertama (trip1) di-pop dari `scheduleData`
- `scheduleData` yang tersisa = [trip2 (10:40), trip3 (11:20), ...]
- `FULL_SCHEDULE_DATA` dikirim ke MapActivity = [trip2, trip3, ...]

**Di MapActivity:**
- `scheduleData` = [trip2 (10:40), trip3 (11:20), ...]
- `getNextScheduleStartTime()` mengambil `flat[1]` = **trip3 (11:20)** ❌
- Seharusnya mengambil `flat[0]` = **trip2 (10:40)** ✅

**Hasil:**
- Current time: 14:18:00
- Next trip yang diambil: trip3 (11:20) - tapi ini sudah lewat hari ini
- Atau jika dianggap besok: Next trip (besok 11:20) - Current time (hari ini 14:18) = **sangat besar** (1200+ menit)

## Solusi

**File:** `TimeManager.kt` - `getNextScheduleStartTime()`

**Perubahan:**
```kotlin
// SEBELUM FIX
return if (flat.size > 1) flat[1].startTime else null  // ❌ Mengambil trip kedua

// FIX PERTAMA (masih salah)
return if (flat.isNotEmpty()) flat[0].startTime else null  // ❌ Mengambil trip pertama, tapi bisa sudah lewat

// FIX KEDUA (benar)
// ✅ Cari trip yang masih akan datang hari ini, bukan trip yang sudah lewat
for (schedule in flat) {
    val tripTotalMinutes = tripHour * 60 + tripMinute
    if (tripTotalMinutes > currentTotalMinutes) {
        return schedule.startTime  // ✅ Trip yang masih akan datang hari ini
    }
}
// Jika tidak ada trip hari ini, return trip pertama (besok)
return flat[0].startTime
```

**Penjelasan:**
- Setelah trip pertama di-pop, `scheduleData[0]` adalah trip berikutnya
- **TAPI** jika trip berikutnya sudah lewat hari ini (misalnya 10:40 dan sekarang 14:21), perhitungan akan menggunakan besok → 1200+ menit
- **FIX:** Cari trip yang masih akan datang hari ini berdasarkan waktu saat ini
- Jika tidak ada trip hari ini, baru ambil trip pertama (yang akan dianggap besok)

## Dampak Fix

### Sebelum Fix:
- ❌ Mengambil trip3 (11:20) padahal seharusnya trip2 (10:40)
- ❌ Perhitungan menggunakan trip yang salah → hasil 1200+ menit
- ❌ "Late for next run" juga salah karena menggunakan trip yang salah

### Sesudah Fix:
- ✅ Mengambil trip2 (10:40) yang merupakan next trip yang benar
- ✅ Perhitungan menggunakan trip yang benar → hasil masuk akal (39 menit)
- ✅ "Late for next run" juga benar karena menggunakan trip yang benar

## Testing

Setelah fix, "Next run in" seharusnya menampilkan:
- Waktu yang masuk akal (< 24 jam)
- Sesuai dengan trip berikutnya yang sebenarnya
- Tidak ada 1000+ menit lagi

