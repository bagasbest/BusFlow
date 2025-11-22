# Screen Recording Configuration Guide

## Overview

Screen recording telah dioptimalkan untuk mencegah masalah performa (lag, penggunaan RAM tinggi). **Built-in screen recording sekarang DISABLED secara default** untuk memastikan aplikasi berjalan dengan performa optimal.

## Masalah yang Diselesaikan

- ✅ **Built-in recording menyebabkan lag dan stuck** - Sekarang DISABLED by default
- ✅ **Penggunaan RAM tinggi** - Recording hanya aktif jika benar-benar diperlukan
- ✅ **UI elements tidak update** - Dengan recording disabled, aplikasi berjalan lebih lancar

## Konfigurasi

### Opsi 1: Gunakan 3rd Party App (RECOMMENDED)

**Menggunakan aplikasi pihak ketiga seperti AZ Screen Recorder memberikan performa lebih baik** karena tidak membebani aplikasi utama.

#### Cara Enable 3rd Party Recording:

```kotlin
// Dari kode manapun dalam aplikasi
import com.jason.publisher.main.activity.SplashActivity

// Enable auto-launch AZ Screen Recorder saat app start
SplashActivity.setThirdPartyRecordingEnabled(this, true)

// Atau gunakan aplikasi lain
SplashActivity.setThirdPartyRecordingEnabled(
    this, 
    true, 
    "com.hecorat.screenrecorder.free" // AZ Screen Recorder
)
```

#### Aplikasi yang Didukung:

- **AZ Screen Recorder** (default): `com.hecorat.screenrecorder.free`
- **Mobizen**: `com.rsupport.mvagent`
- **DU Recorder**: `com.duapps.recorder`
- Atau package name aplikasi lain yang diinginkan

### Opsi 2: Enable Built-in Recording (NOT RECOMMENDED)

**Hanya gunakan jika benar-benar diperlukan** karena akan mempengaruhi performa aplikasi.

```kotlin
// Enable built-in recording (tidak disarankan karena mempengaruhi performa)
SplashActivity.setBuiltinRecordingEnabled(this, true)
```

## Cara Menggunakan untuk Dokumentasi Driver

### Setup untuk Testing di Tablet Driver:

1. **Install AZ Screen Recorder** di tablet (atau aplikasi recording lain)
2. **Enable 3rd party recording** dengan kode berikut:

```kotlin
// Tambahkan di onCreate() MapActivity atau SplashActivity
SplashActivity.setThirdPartyRecordingEnabled(this, true)
```

3. **Saat aplikasi dibuka**, AZ Screen Recorder akan otomatis ter-launch
4. **Driver tinggal tekan record** di aplikasi AZ Screen Recorder
5. **Aplikasi BusFlow akan berjalan dengan performa optimal** tanpa lag

### Check Status:

```kotlin
// Check apakah built-in recording enabled
val isBuiltinEnabled = SplashActivity.isBuiltinRecordingEnabled(this)

// Check apakah 3rd party recording enabled  
val is3rdPartyEnabled = SplashActivity.isThirdPartyRecordingEnabled(this)
```

## Default Behavior

- ✅ **Built-in recording: DISABLED** (default)
- ✅ **3rd party recording: DISABLED** (default)
- ✅ **Aplikasi berjalan dengan performa optimal** tanpa recording overhead

## Troubleshooting

### Jika 3rd Party App Tidak Ter-launch:

1. Pastikan aplikasi sudah terinstall di device
2. Check package name yang digunakan
3. Jika aplikasi tidak ditemukan, Play Store akan otomatis terbuka

### Jika Masih Ada Lag:

1. Pastikan built-in recording **DISABLED**:
   ```kotlin
   SplashActivity.setBuiltinRecordingEnabled(this, false)
   ```

2. Pastikan tidak ada service recording yang masih berjalan
3. Restart aplikasi setelah mengubah konfigurasi

## Performance Impact

| Mode | CPU Usage | RAM Usage | Performance |
|------|-----------|-----------|-------------|
| **No Recording** (default) | Normal | Normal | ✅ Optimal |
| **3rd Party App** | Normal | Normal | ✅ Optimal |
| **Built-in Recording** | High | High | ⚠️ May cause lag |

## Notes

- Built-in recording memakan banyak resource karena encoding video real-time
- 3rd party apps biasanya lebih efisien karena dioptimalkan khusus untuk recording
- Untuk dokumentasi driver, gunakan 3rd party app untuk hasil terbaik

