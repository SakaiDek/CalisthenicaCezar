@echo off
setlocal

REM ============================================================
REM  6-rilis-ke-hp.bat -- dibuat 5 Sept 2026.
REM
REM  BEDANYA DENGAN 3-update-ke-hp.bat: yang itu memasang APK
REM  DEBUG, yang ini memasang APK RILIS.
REM
REM  Kenapa file ini lahir: kamu mengeluh scroll patah-patah. Saya
REM  ukur dua rekamanmu frame per frame, dan yang paling parah
REM  bukan daftar alat -- tapi DASBOR, layar yang isinya cuma
REM  kartu teks tanpa satu pun gambar. Kalau layar tanpa gambar
REM  juga patah-patah, penyebabnya bukan gambarnya, dan bukan
REM  daftarnya. Penyebabnya sesuatu yang berlaku di SEMUA layar.
REM  Tersangka nomor satu: APK debug itu sendiri.
REM
REM  APK debug bukan "APK rilis yang belum dirapikan". Dia app
REM  yang sengaja dibuat lambat supaya bisa ditempeli debugger:
REM  ART mematikan sebagian optimasi mesinnya, compiler Compose
REM  menyisipkan penanda posisi kode di setiap composable, dan
REM  library ui-tooling ikut masuk untuk merekam pohon UI. Semua
REM  itu ongkos yang HILANG di APK rilis.
REM
REM  Jadi tugas file ini cuma satu: memisahkan tuduhan. Kalau
REM  rilis mulus, kodenya tidak salah dan saya TIDAK akan merombak
REM  UI yang sudah rapi tanpa alasan. Kalau rilis masih patah,
REM  berarti memang ada yang salah di kode dan saya sudah punya
REM  daftar tersangka berikutnya.
REM
REM  DATAMU AMAN: APK rilis ini ditandatangani dengan kunci debug
REM  yang sama, applicationId-nya sama, jadi Android menganggapnya
REM  app yang sama -- program dan gerakan yang sudah kamu simpan
REM  tidak hilang. Mau balik ke debug? Jalankan 3-update-ke-hp.bat
REM  seperti biasa.
REM
REM  BUILD PERTAMA LAMA (bisa 3-6 menit di Advan-mu), karena varian
REM  "release" belum pernah dikompilasi sama sekali -- Gradle mulai
REM  dari nol. Yang kedua dan seterusnya kembali normal.
REM ============================================================

set "PROJECT=C:\Users\Sakai\Documents\CalisthenicaCezar"
set "ADB=C:\Android\platform-tools\adb.exe"
set "TMPF=%TEMP%\cezar_devices.txt"

REM Log sengaja memakai file yang SAMA dengan build debug, supaya
REM aba-aba "baca log" ke Claude tidak perlu berubah.
set "LOG=%PROJECT%\build-log.txt"

echo ==========================================
echo  Pasang APK RILIS ke Poco F5 (uji performa)
echo ==========================================
echo.

if not exist "%ADB%" (
    echo ===== BERHENTI ===== adb.exe tidak ada di:
    echo    %ADB%
    goto end
)

echo [Pra-cek] Menyalakan server adb dan mencari HP...
"%ADB%" start-server >nul 2>nul
"%ADB%" devices > "%TMPF%" 2>&1
findstr /r /c:"device$" "%TMPF%" >nul
if errorlevel 1 (
    echo.
    echo ===== BERHENTI SEBELUM BUILD =====
    echo Poco F5 belum siap. Yang kebaca sekarang:
    echo.
    type "%TMPF%"
    echo.
    echo Cek berurutan:
    echo    1. Kabel nyolok, dan layar HP dalam keadaan TERBUKA / unlock.
    echo    2. Status "unauthorized" -^> di HP ada popup, tekan ALLOW.
    echo    3. Daftarnya kosong -^> Opsi pengembang: USB debugging ON,
    echo       mode USB jangan Charging only, pilih Transfer file.
    goto end
)
for /f "tokens=1" %%d in ('findstr /r /c:"device$" "%TMPF%"') do echo           OK, HP terbaca: %%d
echo.
echo [Catatan] Build RILIS pertama memang lama. Biarkan saja jalan.
echo.

pushd "%PROJECT%"

set "BUILD_OK=1"
where powershell >nul 2>nul
if errorlevel 1 goto build_polos

powershell -NoProfile -Command "& '.\gradlew.bat' installRelease --console=plain 2>&1 | Tee-Object -FilePath '%LOG%'; exit $LASTEXITCODE"
if errorlevel 1 set "BUILD_OK=0"
goto build_selesai

:build_polos
echo [Catatan] powershell tidak ketemu. Output ditahan sampai build selesai.
call gradlew.bat installRelease --console=plain > "%LOG%" 2>&1
if errorlevel 1 set "BUILD_OK=0"
type "%LOG%"

:build_selesai
popd
if "%BUILD_OK%"=="0" (
    echo.
    echo ===== GAGAL =====
    echo Log lengkap sudah tersimpan di:
    echo    %LOG%
    echo.
    echo Cukup bilang ke Claude: "baca log".
    goto end
)

echo.
echo Membuka app di HP...
"%ADB%" shell am start -n com.cezar.calisthenica/.MainActivity >nul 2>nul

echo.
echo ===== SUKSES ===== Yang terpasang sekarang APK RILIS.
echo.
echo Sekarang coba scroll daftar alat dan dasbor pakai jempol,
echo TANPA merekam layar (rekaman itu sendiri makan tenaga HP).
echo Lalu lapor: masih patah-patah, atau sudah mulus?
echo.

:end
pause
endlocal
