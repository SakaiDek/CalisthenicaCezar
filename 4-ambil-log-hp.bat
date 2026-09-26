@echo off
setlocal

REM ============================================================
REM  4-ambil-log-hp.bat -- dibuat 5 Sept 2026.
REM
REM  BEDANYA DENGAN build-log.txt (baca ini sekali, biar tidak
REM  tertukar seumur proyek):
REM
REM    build-log.txt  = log LAPTOP. Isinya kerja Gradle: kompilasi
REM                     Kotlin, KSP, packaging APK. Yang muncul di
REM                     sini adalah salah TULIS kode. Ditulis oleh
REM                     3-update-ke-hp.bat.
REM
REM    logcat.txt     = log HP. Isinya apa yang terjadi SAAT app
REM                     JALAN: exception, crash, ANR, pesan sistem.
REM                     Yang muncul di sini adalah salah LOGIKA.
REM                     Ditulis oleh file ini.
REM
REM  Build bisa hijau sempurna dan app tetap crash saat dibuka.
REM  Dua log yang beda, dua penyakit yang beda.
REM ============================================================

set "PROJECT=C:\Users\Sakai\Documents\CalisthenicaCezar"
set "ADB=C:\Android\platform-tools\adb.exe"
set "PKG=com.cezar.calisthenica"
set "LOG=%PROJECT%\logcat.txt"
set "TMPF=%TEMP%\cezar_devices.txt"

echo ==========================================
echo  Ambil log dari Poco F5 (logcat)
echo ==========================================
echo.

if not exist "%ADB%" (
    echo ===== BERHENTI ===== adb.exe tidak ada di:
    echo    %ADB%
    goto end
)

echo [Pra-cek] Mencari HP...
"%ADB%" start-server >nul 2>nul
"%ADB%" devices > "%TMPF%" 2>&1
findstr /r /c:"device$" "%TMPF%" >nul
if errorlevel 1 (
    echo.
    echo ===== BERHENTI ===== Poco F5 belum terbaca:
    echo.
    type "%TMPF%"
    echo.
    echo Kabel nyolok? Layar HP terbuka? USB debugging ON?
    goto end
)
for /f "tokens=1" %%d in ('findstr /r /c:"device$" "%TMPF%"') do echo           OK, HP terbaca: %%d
echo.

REM ============================================================
REM  LANGKAH 1: KOSONGKAN dulu.
REM
REM  Buffer logcat itu ember bergilir -- isinya SEMUA app di HP,
REM  dan yang lama didorong keluar oleh yang baru. Kalau langsung
REM  di-dump tanpa dikosongkan, kamu mengirim saya puluhan ribu
REM  baris milik WhatsApp, launcher, dan sistem, lalu baris app
REM  kita tenggelam di dalamnya.
REM
REM  Kosongkan -> reproduksi masalahnya -> dump. Hasilnya log
REM  kecil yang isinya HAMPIR SEMUA relevan.
REM ============================================================
echo [1/3] Mengosongkan log lama di HP...
"%ADB%" logcat -c -b all >nul 2>nul
"%ADB%" logcat -c >nul 2>nul

REM force-stop dulu, supaya prosesnya benar-benar baru. Kalau app
REM cuma di-background lalu dibuka lagi, kejadian saat STARTUP
REM (yang paling sering jadi biang crash) tidak akan terekam.
echo [2/3] Menjalankan app dari nol di HP...
"%ADB%" shell am force-stop %PKG% >nul 2>nul
"%ADB%" shell am start -n %PKG%/.MainActivity >nul 2>nul

echo.
echo ------------------------------------------------------------
echo  SEKARANG PEGANG HP-MU.
echo.
echo  Lakukan hal yang mau kamu laporkan: buka katalog, tekan
echo  lama kartu, pilih foto dari galeri, apa pun yang aneh atau
echo  bikin app tertutup sendiri.
echo.
echo  Selesai? kembali ke sini dan tekan tombol apa saja.
echo ------------------------------------------------------------
pause

echo.
echo [3/3] Menarik log dari HP...
> "%LOG%" echo ===== INFO PERANGKAT =====
for /f "delims=" %%v in ('"%ADB%" shell getprop ro.product.model 2^>nul') do >> "%LOG%" echo Model        : %%v
for /f "delims=" %%v in ('"%ADB%" shell getprop ro.build.version.release 2^>nul') do >> "%LOG%" echo Android      : %%v
for /f "delims=" %%v in ('"%ADB%" shell getprop ro.build.display.id 2^>nul') do >> "%LOG%" echo ROM          : %%v
>> "%LOG%" echo.

REM Buffer "crash" diambil UTUH, tanpa filter. Isinya sudah cuma
REM crash, dan jejak tumpukannya (stack trace) HARUS lengkap --
REM baris "at androidx.compose..." itu justru yang memberi tahu
REM saya di composable mana yang jebol. Kalau difilter per nama
REM package kita, baris-baris itu ikut terbuang dan jejaknya jadi
REM sepotong.
>> "%LOG%" echo ===== BUFFER CRASH (utuh) =====
"%ADB%" logcat -d -b crash >> "%LOG%" 2>&1
>> "%LOG%" echo.

REM Buffer "main" JUSTRU difilter, karena isinya seluruh HP.
REM findstr dengan beberapa kata dipisah spasi = ATAU.
REM
REM "Cezar" ditambahkan 5 Sept 2026, dan ini bukan kata hiasan --
REM dia yang menangkap pesan Log.e/Log.w BUATAN KITA SENDIRI.
REM Semua tag log di app ini diawali "Cezar" (contoh: CezarMedia di
REM data/MediaFiles.kt). Tanpa kata ini, pesan yang sengaja kita
REM tulis untuk mendiagnosis foto gagal tersalin akan lolos dari
REM saringan dan tidak pernah sampai ke saya -- padahal justru itu
REM baris yang paling saya butuh baca.
>> "%LOG%" echo ===== BUFFER MAIN (disaring: app kita + error runtime) =====
"%ADB%" logcat -d -b main 2>&1 | findstr /i "Cezar calisthenica AndroidRuntime FATAL ActivityManager Coil Room SQLite" >> "%LOG%"

echo.
for %%A in ("%LOG%") do set "UKURAN=%%~zA"
echo Log tersimpan: %LOG%
echo Ukuran       : %UKURAN% byte
echo.

REM ============================================================
REM  UJI EMPIRIS, bukan menebak nama menu.
REM
REM  ROM custom punya nama tombol yang beda-beda ("kill logs",
REM  "disable debug logging", "logd off"). Daripada saya menebak
REM  menu di ROM-mu, ukurannya saja yang kita lihat: kalau HP-mu
REM  memang mencatat, angka di atas pasti ribuan byte. Kalau cuma
REM  ratusan, berarti pencatatannya sedang dimatikan.
REM ============================================================
if %UKURAN% LSS 800 (
    echo ===== LOGNYA HAMPIR KOSONG =====
    echo Kemungkinan besar pencatatan log di HP sedang DIMATIKAN.
    echo Yang perlu dinyalakan di Poco F5:
    echo    1. Opsi pengembang -^> "Logger buffer sizes" jangan Off,
    echo       set minimal 1M.
    echo    2. Kalau ROM-mu punya tombol "kill logs" / "disable
    echo       debug logging" -^> MATIKAN tombol pematinya, biar
    echo       lognya hidup.
    echo    3. Jalankan ulang file ini.
    echo.
) else (
    echo Isinya masuk. Bilang ke Claude: "baca logcat".
    echo.
)

:end
pause
endlocal
