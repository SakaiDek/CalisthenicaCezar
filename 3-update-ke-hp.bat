@echo off
setlocal

set "PROJECT=C:\Users\Sakai\Documents\CalisthenicaCezar"
set "ADB=C:\Android\platform-tools\adb.exe"

set "TMPF=%TEMP%\cezar_devices.txt"

REM ============================================================
REM  build-log.txt -- ditambahkan 5 Sept 2026.
REM
REM  KENAPA: selama ini kalau build merah, kamu harus blok teks di
REM  jendela hitam ini lalu menempelkannya ke saya. Itu kerjaan
REM  tambahan untuk kamu, dan sering kepotong di bagian yang justru
REM  penting. Sekarang seluruh keluaran Gradle ditulis ke file di
REM  dalam folder proyek -- folder yang sama yang saya pakai untuk
REM  menulis kode. Jadi saya bisa MEMBACANYA SENDIRI.
REM  Kamu cuma perlu bilang: "baca log".
REM ============================================================
set "LOG=%PROJECT%\build-log.txt"

echo ==========================================
echo  Update app di Poco F5 (build + install)
echo ==========================================
echo.

REM ============================================================
REM  PRA-CEK HP. Ditambahkan setelah kejadian 4 Sept 2026.
REM
REM  Kenapa perlu: Gradle baru menyentuh HP di task PALING AKHIR
REM  (installDebug). Jadi kalau kabel copot atau adb rewel, kamu
REM  tetap menunggu kompilasi selesai dulu -- baru dikasih tahu
REM  gagal. Satu menit terbuang untuk kabel yang tidak nyolok.
REM  Sekarang HP diperiksa dulu, build-nya belakangan.
REM ============================================================
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
    echo    4. Ada tulisan "server version ... doesn't match this client"
    echo       -^> itu adb kembar. Jalankan ulang file ini, biasanya beres.
    goto end
)
for /f "tokens=1" %%d in ('findstr /r /c:"device$" "%TMPF%"') do echo           OK, HP terbaca: %%d
echo.

pushd "%PROJECT%"

REM Kenapa lewat powershell: cmd tidak punya "tee". Kalau saya pakai
REM redirect biasa (> file), kamu duduk 25 detik melihat layar kosong
REM karena semua output masuk file. Tee-Object menulis ke DUA tempat
REM sekaligus: layarmu tetap jalan, filenya tetap terisi.
REM --console=plain mematikan animasi progres Gradle. Animasi itu
REM bagus dilihat mata, tapi di dalam file dia jadi ribuan baris sampah
REM yang bikin log 10x lebih besar tanpa menambah satu pun informasi.
set "BUILD_OK=1"
where powershell >nul 2>nul
if errorlevel 1 goto build_polos

powershell -NoProfile -Command "& '.\gradlew.bat' installDebug --console=plain 2>&1 | Tee-Object -FilePath '%LOG%'; exit $LASTEXITCODE"
if errorlevel 1 set "BUILD_OK=0"
goto build_selesai

REM Jalur cadangan kalau powershell tidak ada di PATH. Outputnya ditahan
REM dulu lalu ditumpahkan sekaligus di akhir. Lebih membosankan, tapi
REM lognya tetap kebentuk -- dan itu yang penting.
:build_polos
echo [Catatan] powershell tidak ketemu. Output ditahan sampai build selesai.
call gradlew.bat installDebug --console=plain > "%LOG%" 2>&1
if errorlevel 1 set "BUILD_OK=0"
type "%LOG%"

:build_selesai
popd
if "%BUILD_OK%"=="0" (
    echo.
    echo ===== GAGAL =====
    echo Log lengkap sudah tersimpan otomatis di:
    echo    %LOG%
    echo.
    echo Kamu TIDAK perlu copy-paste apa pun lagi.
    echo Cukup bilang ke Claude: "baca log".
    goto end
)

echo.
echo Membuka app di HP...
"%ADB%" shell am start -n com.cezar.calisthenica/.MainActivity >nul 2>nul

echo.
echo ===== SUKSES ===== Lihat layar Poco F5.
echo.
echo (Log build tersimpan di build-log.txt -- kalau ada yang aneh di
echo  layar HP walau build sukses, bilang "baca log" ke Claude.)
echo.

:end
pause
endlocal
