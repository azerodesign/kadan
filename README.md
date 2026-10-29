# Kadan — v1

Kadan adalah aplikasi pengingat pribadi, full Kotlin, untuk satu masalah
spesifik: Wildan sering lupa apa yang harus dikerjakan hari itu. Bukan
todo list pasif yang harus dibuka dulu baru ingat, tapi **reminder-first**:
aplikasi yang nyamperin lewat notifikasi.

- **Nama:** Kadan (sebelumnya Disela)
- **Paket:** `com.azerostudio.kadan`
- **Bahasa:** 100% Kotlin
- **UI:** Jetpack Compose, Material 3 Expressive **saja** via
  `androidx.compose.material3`. Jangan impor `androidx.compose.material`
  (Material 2). Tidak ada "Material 4" — itu bukan nama resmi Google.
- **Data:** Room, lokal saja. Tanpa login, tanpa sync, tanpa iklan.
- **Ringan:** tanpa backend, tanpa gambar berat, target HP mid-range
  dengan storage terbatas.

## Yang ada di v1

### Kategori (persis, Bahasa Indonesia)
PR, Pekerjaan Sekolah, Pekerjaan Bisnis, Plan, Rutinitas. PR adalah
kategori prioritas utama.

### Layar "Hari Ini"
Scaffold + TopAppBar, baris FilterChip (Semua + 5 kategori), LazyColumn
berisi Card/ListItem dengan Checkbox (seluruh baris bisa diketuk, target
minimal 48 dp), judul di `titleMedium`, waktu/kategori di `bodySmall`
dengan `onSurfaceVariant`. Di atas list ada hitungan: berapa yang jatuh
tempo hari ini, berapa yang telat, berapa yang belum selesai. Tugas yang
telat tampil dengan `errorContainer` **dan** kata "Telat" — tidak pernah
warna saja.

### Capture (tambah tugas)
Satu ExtendedFloatingActionButton "Tambah" membuka ModalBottomSheet:
judul, FilterChip kategori, tanggal (DatePickerDialog), jam opsional
(TimePicker yang dibungkus AlertDialog — tidak ada TimePickerDialog di
Material 3 Compose), catatan opsional, Simpan (filled Button) dan Batal
(TextButton). Target: judul + kategori + tanggal selesai dalam ~10 detik.
Kategori default adalah PR. Mengetuk sebuah tugas membuka sheet yang
sama dalam mode ubah, termasuk tombol Hapus.

### Hapus dengan Undo
Menghapus tugas menampilkan Snackbar "… dihapus — Urungkan". Mengetuk
Urungkan mengembalikan tugas (dengan id baru, dan alarmnya dijadwalkan
ulang). Menutup Snackbar tanpa Urungkan berarti hapus permanen.

## Pengingat — bagian tersulit, dan yang paling jujur dibatasi

### 1. Nudge capture PR — Senin–Jumat 15:15 WIB
Setiap hari kerja, Kadan mengirim notifikasi: **"Hari ini ada PR apa?"**
Mengetuknya membuka capture sheet dengan kategori PR sudah terpilih.
Ini menjawab jebakan utama PR: kalau PR-nya saja lupa, maka PR itu juga
tidak akan pernah diinput. Nudge ini adalah pengingat untuk *mencatat*,
bukan pengingat untuk tugas yang sudah dicatat.

Jadwalnya adalah rantai, bukan alarm berulang: `PrNudgeReceiver`
menampilkan nudge lalu memanggil `ReminderScheduler.schedulePrNudge()`
lagi untuk hari kerja berikutnya. Alarm berulang tidak bisa melewati
Sabtu/Minggu, jadi rantai adalah bentuk yang benar. Semua waktu dihitung
di zona `Asia/Jakarta`, bukan UTC atau default perangkat.

### 2. Pengingat jatuh tempo
Setiap tugas mendapat satu alarm tepat (exact alarm) di tanggal/jamnya.
Tugas tanpa jam diingatkan jam 08:00 WIB di hari jatuh temponya.
Alarm disimpan per id tugas, dibatalkan saat tugas selesai/dihapus, dan
diperiksa ulang ke database sebelum notifikasi tampil (tugas yang sudah
selesai/dihapus tidak memunculkan notifikasi basi).

### 3. Setelah reboot
Alarm Android hilang saat reboot. `BootReceiver` (dengan izin
`RECEIVE_BOOT_COMPLETED`) menjadwalkan ulang semua pengingat tugas yang
belum selesai dan rantai nudge PR setelah `BOOT_COMPLETED`.

### Izin yang harus aktif agar pengingat benar-benar bunyi
1. **Notifikasi** (`POST_NOTIFICATIONS`, Android 13+): diminta sekali
   saat pertama dibuka. Tanpa ini, tidak ada pengingat yang bisa tampil.
2. **Alarm tepat** (`SCHEDULE_EXACT_ALARM`, Android 12+): ini bukan
   izin runtime, melainkan pengaturan sistem "Alarm & pengingat". Jika
   belum aktif, layar Hari Ini menampilkan banner dengan tombol ke
   Pengaturan. Jika izin ini ditolak, alarm jatuh kembali ke penjadwalan
   tidak-tepat (inexact) — aplikasi tidak boleh mengklaim pengingat
   tepat sudah terpasang dalam kondisi itu.

### Peringatan MIUI / Redmi — baca sebelum mengklaim pengingat "berfungsi"
Xiaomi (MIUI) agresif mematikan alarm/notifikasi aplikasi demi hemat
baterai, terlepas dari apakah kode di atas sudah benar. **Pengingat
Kadan belum terbukti berfungsi sampai diuji di HP Redmi yang nyata,
dengan aplikasi dikecualikan dari optimasi baterai.** Build yang lolos
di VM atau di CI tidak membuktikan apa-apa soal ini. Lihat "Yang harus
diuji pertama" di bawah.

## Yang TIDAK ada di v1 (sengaja dipotong)

- **Pengingat H-1 malam hari:** dijadwalkan sebagai v1.1. Alasannya
  sederhana: satu alarm ekstra per tugas menggandakan logika
  penjadwalan/pembatalan/reschedule, dan v1 harus membuktikan dulu bahwa
  satu alarm jatuh tempo + nudge PR benar-benar bunyi di HP nyata.
- **Auto-roll Rutinitas:** kolom `isRecurring` disimpan, tapi tugas
  Rutinitas belum otomatis bergulir ke hari berikutnya. Di v1 ia
  berperilaku seperti tugas biasa; pengguna mengubah tanggalnya atau
  membuat ulang. Jangan mengklaim sebaliknya.
- **Login, sync, backup cloud, widget, statistik, dan tema terang/gelap
  manual:** tidak ada. Tema mengikuti sistem (terang/gelap otomatis +
  dynamic colour di Android 12+ dengan fallback statis). Widget dan
  statistik adalah kandidat v1.1 setelah loop inti terbukti.
- **Edit lewat gestur swipe:** tidak ada. Edit lewat ketukan pada tugas.
- **Ikon launcher final:** ikon yang ada sekarang adalah placeholder
  (cakram putih dengan celah di tengah, di
  `res/drawable/ic_launcher_foreground.xml`). Notifikasi v1 memakai ikon
  info sistem. Ganti keduanya begitu aset ikon Kadan yang sebenarnya
  tersedia.

## Cara build

### Opsi A — GitHub Actions (cara yang direkomendasikan untuk v1)
1. Buat repo GitHub baru (jangan push dulu sampai pemilik repo
   memutuskan nama repo dan visibilitasnya).
2. Salin seluruh isi folder ini ke root repo.
3. Push ke branch `main`. Workflow `.github/workflows/android.yml`
   otomatis berjalan dan mengunggah artefak `kadan-debug-apk`
   (file `app-debug.apk`, debug, belum ditandatangani rilis).
4. Unduh APK itu dari tab Actions repo, instal di HP Redmi, dan uji
   sesuai daftar di bawah.

Catatan: workflow ini tidak memakai `./gradlew` karena
`gradle-wrapper.jar` sengaja tidak disertakan (lihat
`gradle/wrapper/gradle-wrapper.properties`). Ia menginstal Gradle 8.7
lewat `gradle/actions/setup-gradle`. Jika wrapper ditambahkan nanti,
ganti langkah build menjadi `./gradlew assembleDebug`.

### Opsi B — Lokal (butuh Android SDK + JDK 17)
1. Instal Android Studio / command-line tools, terima lisensi SDK,
   dan instal platform Android API 34.
2. Sebelum build pertama, verifikasi versi di
   `gradle/libs.versions.toml` di halaman rilis resmi (AGP, Kotlin,
   Compose BOM, Room). Jangan menyalin versi dari blog tanpa memeriksa.
3. Jalankan `gradle assembleDebug` dari root proyek.
4. APK debug ada di `app/build/outputs/apk/debug/app-debug.apk`.

### Kenapa tidak ada APK di folder ini
Proyek ini ditulis di VM tanpa Android SDK dan dengan RAM bebas
~400 MB — build Gradle penuh di sana tidak realistis (download SDK
besar + OOM). Jadi yang dikirim adalah kode lengkap + workflow CI,
bukan APK biner. Lihat "Status verifikasi" di bawah.

## Status verifikasi (jujur, per 2026-10-25)

**Sudah diverifikasi di VM ini:**
- Seluruh file Kotlin/Gradle/Manifest/workflow ada dan terbaca; tidak
  ada impor `androidx.compose.material` (M2) di kode mana pun.
- Tidak ada warna hex di file layar (`ui/HomeScreen.kt`,
  `ui/CaptureSheet.kt`) — semua warna lewat peran `colorScheme`.
  Hex hanya ada di `ui/theme/Color.kt`, satu-satunya tempat yang
  diizinkan.
- Struktur proyek lengkap: settings, version catalog, manifest dengan
  tiga izin pengingat, Application, database Room, ViewModel, dua layar,
  tiga receiver notifikasi, dan workflow CI.

**BELUM diverifikasi (jangan diklaim sudah):**
- APK biner belum pernah di-build — tidak lokal (tanpa SDK), tidak
  lewat CI (belum ada repo / belum ada push).
- Notifikasi belum pernah diuji di HP nyata, apalagi di Redmi/MIUI.
- Nomor versi `material3` (lewat Compose BOM di catalog) harus dicek
  ulang di halaman rilis saat build pertama; jika BOM itu sudah tidak
  tersedia, bump catalog dan build ulang.
- Sintaks Kotlin belum dikompilasi oleh kotlinc/Gradle di VM ini
  (tidak ada kompiler Kotlin di PATH). Kode ditulis dengan hati-hati
  mengikuti API Material 3 yang terdokumentasi (termasuk tidak adanya
  TimePickerDialog), tetapi kompilasi pertama yang sah adalah build CI
  atau build lokal dengan SDK.

## Yang harus diuji pertama di HP Redmi

1. **Nudge PR:** atur jam sistem ke hari kerja sebelum 15:15, pastikan
   notifikasi "Hari ini ada PR apa?" muncul jam 15:15 WIB, dan
   mengetuknya membuka capture dengan kategori PR terpilih.
2. **Pengingat jatuh tempo:** buat tugas dengan jam 5 menit ke depan,
   tutup aplikasi, pastikan notifikasinya bunyi tepat waktu.
3. **Setelah reboot:** buat tugas, reboot HP, pastikan pengingatnya
   masih terjadwal (BootReceiver).
4. **Tema gelap + font besar:** aktifkan dark mode sistem dan ukuran
   font terbesar, pastikan tidak ada teks terpotong dan tidak ada warna
   yang hilang.
5. **Optimasi baterai MIUI:** jika notifikasi tidak bunyi, cek
   Pengaturan → Baterai → Kadan → "Tanpa batasan" / whitelist, lalu
   ulangi tes 1–2. Ini adalah langkah yang paling sering menentukan
   apakah reminder app "berfungsi" di Xiaomi.

## Struktur file

```
settings.gradle.kts, build.gradle.kts, gradle.properties
gradle/libs.versions.toml, gradle/wrapper/gradle-wrapper.properties
app/build.gradle.kts, app/proguard-rules.pro
app/src/main/AndroidManifest.xml
app/src/main/java/com/azerostudio/kadan/
  MainActivity.kt, KadanApplication.kt
  data/ Category.kt, Task.kt, TaskDao.kt, KadanDatabase.kt, TaskRepository.kt
  notifications/ NotificationHelper.kt, ReminderScheduler.kt,
                 ReminderReceiver.kt, PrNudgeReceiver.kt, BootReceiver.kt
  ui/ KadanViewModel.kt, HomeScreen.kt, CaptureSheet.kt
  ui/theme/ Color.kt, Type.kt, Shape.kt, Theme.kt
.github/workflows/android.yml
```
