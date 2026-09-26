PROJECT KNOWLEDGE: CALISTHENICA CEZAR
Update terakhir: 4 September 2026
1. Identitas Proyek
Nama Aplikasi: Calisthenica Cezar
Platform: Eksklusif Android (Native)
Gaya Desain: Material 3 (M3) Guidelines
Fokus Utama: "Buku Catatan Latihan Digital" — aplikasi pelacak kalistenik dengan kustomisasi mutlak untuk user tingkat lanjut, dilengkapi sistem manajemen konten mandiri (Admin).
2. Aturan Teknologi & Perangkat Pengembangan
Bahasa Pemrograman: Kotlin
Antarmuka (UI): Jetpack Compose (Material 3)
Perangkat Kode (Laptop): Advan Soulmate Plus (RAM 8GB, 256GB NVMe). Development manual via VS Code (bukan Android Studio) untuk menjaga RAM tetap ringan.
Perangkat Pengujian (HP): Poco F5 (RAM 12GB, Custom ROM). Build & install APK via adb (USB/Wireless Debugging).
Strategi: MVP (Minimum Viable Product) ketat — fitur besar ditunda sampai fondasi stabil.
3. Status Environment (per 4 Sept 2026)
Semua tools berikut sudah terinstall dan terverifikasi jalan di laptop:
JDK 17 (Temurin) — default sistem, JAVA_HOME mengarah ke sini
JDK 25 (Temurin) — terinstall sebagai cadangan, tidak dipakai default
Android SDK Command-Line Tools (`C:\Android\cmdline-tools\latest`)
platform-tools (adb) — `C:\Android\platform-tools`
Android Platform 14 (API 34) + build-tools 34.0.0
Gradle 9.7.1 standalone — `C:\Gradle\gradle-9.7.1` (TIDAK dipakai untuk build; hanya untuk generate wrapper)
Git 2.55.0
VS Code + extension: Kotlin Language, Kotlin, Kotlin Formatter, Gradle for Java
Struktur project ada di: `C:\Users\Sakai\Documents\CalisthenicaCezar`

TOOLCHAIN YANG DIPIN (jangan diubah tanpa alasan kuat):
Gradle 8.9 via Gradle Wrapper (`gradlew.bat`) — SELALU pakai ini, jangan gradle global. Gradle 9.7.1 tidak kompatibel dengan AGP 8.6.0.
AGP 8.6.0, Kotlin 2.0.20, plugin `org.jetbrains.kotlin.plugin.compose` 2.0.20 (versi harus sama dengan Kotlin).
compileSdk 34, targetSdk 34, minSdk 26 (minSdk 26 dipilih agar `java.time.LocalDate` jalan native tanpa core library desugaring — penting untuk fitur kalender & streak).
Dependency Compose dipin eksplisit, bukan BOM: ui 1.7.0, material3 1.3.0, activity-compose 1.9.2, core-ktx 1.13.1, lifecycle-runtime-ktx 2.8.4.
namespace / applicationId: `com.cezar.calisthenica`.

SCRIPT SEKALI KLIK (di root project):
`1-setup-dan-build.bat` — setup wrapper + build pertama. Hanya perlu dijalankan sekali.
`2-pasang-ke-hp.bat` — install APK yang sudah ada ke Poco F5 + buka app.
`3-update-ke-hp.bat` — build + install + buka. INI yang dipakai sehari-hari untuk iterasi.
4. Fitur MVP (Prioritas Sekarang)
Dasbor Utama: Program latihan dalam bentuk Elevated/Filled Cards Material 3.
Custom Exercise & Grouping: User bisa menyusun program dengan pembagian Group 1 (pemanasan), Group 2 (inti), Group 3 (pendinginan). Parameter mikro per gerakan: rep/detik, waktu istirahat, penanda beban tambahan.
Referensi Gerakan via Link: Tautan URL eksternal (YouTube) untuk panduan gerakan — BUKAN upload foto/video langsung (ditunda, lihat section 6).
Skill Mastery Tracker: Pelacakan progres penguasaan skill (contoh: vertical pull, horizontal pull) dengan skala persentase 0–100%.
Kalender & Streak Sederhana: Kalender dinamis untuk memantau streak harian dan riwayat sesi latihan (tanpa analitik statistik kompleks).
Sistem Tracking: Timer, set, repetisi, durasi gerakan interaktif.
Riwayat Log: Catatan riwayat latihan yang telah diselesaikan.
Data disimpan lokal di Room database (SQLite) — tidak ada dependency cloud di tahap ini.
5. Fitur Admin (Hidden/Tersembunyi) — MVP
Akses tersembunyi via long-press di menu Settings, dilindungi PIN lokal (EncryptedSharedPreferences) — bukan sistem login/auth penuh, karena aplikasi single-device.
CRUD Program: tambah, lihat, ubah, hapus program latihan.
CRUD Exercise & Dynamic Add: manajemen data latihan + fitur "+ Add New Exercise" on the fly saat merakit program.
Manajemen Video: tautan URL eksternal (YouTube) untuk panduan gerakan.
6. Fitur yang DITUNDA (bukan untuk MVP)
Fitur berikut dievaluasi bersama pada 4 Sept 2026 dan sengaja ditunda ke fase berikutnya agar MVP tidak overengineered:
Upload Foto/Video Referensi Gerakan — butuh handling file storage kompleks. MVP tetap pakai link YouTube dulu.
E-Commerce (Katalog Alat Kalistenik) — butuh payment gateway, manajemen stok, keamanan transaksi. Ini proyek terpisah dari core app tracking.
Cloud Sync + Google Sign-In + OTP Email — ALASAN TEKNIS PENTING:
Cloud Sync butuh backend/server, bukan sekadar fitur Android.
OTP via email TIDAK BOLEH dikirim langsung dari app Android (kredensial email akan tertanam di APK dan bisa dicuri lewat decompile) — harus lewat server terlebih dahulu.
Google Sign-In butuh setup Google Cloud Console + `google-services.json`.
Rencana jangka panjang: migrasi ke Firebase (Auth + Firestore) setelah versi lokal (Room database) stabil dan sudah dipakai sehari-hari. Firebase dipilih karena menyediakan Auth + Database + OTP infrastructure tanpa perlu membangun server sendiri dari nol — jalan paling realistis untuk solo developer.
7. Arsitektur Database Dasar (MVP, Room/SQLite lokal)
Tabel Program: judul program, deskripsi, kategori.
Tabel Exercise: daftar gerakan, instruksi, tautan video, kolom skill mastery (0–100%).
Tabel Relasi (CrossRef): menghubungkan program dengan urutan gerakan + grouping (warmup/inti/cooldown).
Tabel Log/Riwayat: riwayat sesi latihan untuk kalender & streak.
8. Catatan Keamanan Penting
JANGAN pernah gunakan API key/gateway pihak ketiga tidak resmi untuk akses AI model (misal layanan reseller non-Anthropic) — risiko keamanan data dan pelanggaran ToS.
Untuk automasi coding lewat AI (jika suatu saat dibutuhkan), gunakan Claude Code resmi dari Anthropic dengan akun sendiri (Pro/Max atau API key resmi), bukan layanan gateway tidak dikenal.
9. Progres Nyata (log jujur, update 4 Sept 2026)
SUDAH TERBUKTI JALAN (dites langsung di Poco F5):
Build pertama sukses (cold build 16m49s), iterasi berikutnya ~29 detik.
APK terinstall & app kebuka di Poco F5 lewat `3-update-ke-hp.bat`.
Tema Material 3 jalan: Material You (dynamic color) sebagai UTAMA, palet lime buatan sendiri hanya CADANGAN kalau HP di bawah Android 12 — keputusan ini disetujui Sakai 4 Sept 2026.
Dark mode ikut setting HP otomatis (`isSystemInDarkTheme()`), tanpa flash putih (ada `values-night/themes.xml`).
Adaptive launcher icon murni XML vector, tanpa file gambar biner.
Dasbor: `LazyColumn` berisi kartu program, kartu bisa ditekan untuk buka/tutup detail struktur sesi.
Room database JALAN (diverifikasi 4 Sept 2026 via rekaman layar Poco F5): tabel `programs`, tambah program lewat FAB + AlertDialog, data selamat setelah app ditutup DAN setelah APK di-install ulang. Daftar auto-refresh dari `Flow` tanpa refresh manual, urutan `ORDER BY id DESC` (terbaru di atas). Tombol Simpan mati kalau nama kosong.
Hapus program JALAN (diverifikasi 4 Sept 2026): tombol "Hapus program" disembunyikan di dalam area detail kartu, warna `colorScheme.error`, dialog konfirmasi menyebut nama programnya. Hapus semua program -> EmptyState muncul kembali. Data yang dihapus tidak balik setelah app di-kill dari recent apps.

KEPUTUSAN ARSITEKTUR ROOM (sengaja disederhanakan, jangan "dibetulkan" tanpa alasan):
KSP (`com.google.devtools.ksp` 2.0.20-1.0.25), BUKAN KAPT — KAPT harus bikin stub Java dulu, terlalu berat untuk RAM 8GB.
Room 2.6.1: `room-runtime` + `room-ktx` + `ksp("room-compiler")`.
`WorkoutProgram` dipakai GANDA: sekaligus `@Entity` dan model UI. Belum dipisah karena isinya masih identik; pisahkan nanti kalau kartu perlu data yang tidak ada di tabel mana pun.
TANPA ViewModel dulu. `HomeScreen` baca `dao.observeAll()` langsung lewat `collectAsState`. Sah karena sumber kebenaran ada di file database, jadi state memori yang hilang saat rotasi langsung terisi ulang. Pasang ViewModel nanti saat ada state yang TIDAK ada di database (contoh: timer sedang berjalan).
TANPA seeding data awal. Daftar mulai kosong + EmptyState, supaya persistensi benar-benar terbukti dari tangan user.
`model/SampleData.kt` masih ada tapi SUDAH TIDAK DIPAKAI — cuma sisa data dummy. Aman dihapus kapan saja.

BELUM DIBUAT SAMA SEKALI (ini isi MVP yang sebenarnya):
Tabel Exercise + tabel relasi (CrossRef) — baru tabel Program yang ada.
Custom exercise & grouping + parameter mikro (rep/detik, istirahat, beban tambahan).
Tautan YouTube per gerakan.
Skill Mastery Tracker 0–100%.
Kalender & streak.
Timer / set / repetisi interaktif.
Riwayat log latihan.
Area Admin tersembunyi + PIN lokal.

CATATAN: file `C:\Users\Sakai\Documents\Panduan\Project_Blueprint_Calisthenica_Cezar.md` adalah duplikat lama dan SUDAH TIDAK SINKRON. Sumber kebenaran tunggal = CLAUDE.md ini.