@echo off
setlocal

set "PROJECT=C:\Users\Sakai\Documents\CalisthenicaCezar"
set "RJAR=%PROJECT%\app\build\intermediates\compile_and_runtime_not_namespaced_r_class_jar"

REM ============================================================
REM  5-bebaskan-kunci-dan-build.bat   dibuat 5 September 2026
REM
REM  KENAPA FILE INI ADA:
REM  Build tanggal 5 Sept 2026 mati BUKAN karena kode salah. Pesan
REM  aslinya begini:
REM
REM     R.jar: The process cannot access the file because it is
REM            being used by another process
REM
REM  Itu bahasa Windows untuk "ada program lain yang masih memegang
REM  file ini, jadi saya tidak boleh menimpanya". Tersangkanya cuma
REM  tiga, dan tidak satu pun kodemu:
REM    1. Gradle daemon sisa build sebelumnya yang belum mati.
REM    2. Extension "Gradle for Java" di VS Code. Dia menjalankan
REM       Gradle daemon SENDIRI yang ikut memegang folder build.
REM    3. Windows Defender sedang memindai .jar yang baru ditulis.
REM
REM  Skrip ini menutup tersangka nomor 1, membuang file yang macet,
REM  lalu langsung menyambung ke build biasa. Satu klik, bukan lima.
REM ============================================================

echo ==========================================
echo  Bebaskan file terkunci, lalu build ulang
echo ==========================================
echo.

if not exist "%PROJECT%\gradlew.bat" (
    echo ===== BERHENTI ===== gradlew.bat tidak ada di:
    echo    %PROJECT%
    goto gagal
)

pushd "%PROJECT%"

echo [1/4] Mematikan semua Gradle daemon...
call gradlew.bat --stop
echo.

REM Kenapa harus menunggu: perintah --stop cuma MENGIRIM sinyal berhenti.
REM Proses JVM-nya butuh sedikit waktu untuk benar-benar mati dan
REM melepaskan handle file. Kalau langsung dihapus, kadang masih kena
REM kunci yang sama dan kita cuma mengulang kegagalan yang sama.
echo [2/4] Memberi Windows waktu melepas file...
timeout /t 4 /nobreak >nul
echo.

echo [3/4] Membuang R.jar yang macet...
if exist "%RJAR%" rmdir /s /q "%RJAR%" 2>nul

REM Kalau folder sempitnya masih terkunci, taruhannya dinaikkan: buang
REM seluruh app\build. Ongkosnya cuma WAKTU   build berikutnya jadi
REM sekitar 2 menit, bukan 30 detik   karena isi folder itu semuanya
REM hasil olahan yang bisa dibuat ulang. Nol risiko ke kode sumbermu.
if exist "%RJAR%" (
    echo       Masih terkunci. Membuang seluruh folder app\build...
    rmdir /s /q "%PROJECT%\app\build" 2>nul
)

if exist "%RJAR%" (
    echo.
    echo ===== MASIH TERKUNCI =====
    echo Berarti yang memegang file itu BUKAN Gradle daemon.
    echo Proses Java yang masih hidup sekarang:
    echo.
    tasklist /fi "imagename eq java.exe" 2>nul
    tasklist /fi "imagename eq javaw.exe" 2>nul
    echo.
    echo Lakukan ini berurutan:
    echo    1. TUTUP VS CODE sepenuhnya, lalu jalankan file ini lagi.
    echo       Extension "Gradle for Java" ikut memegang folder build.
    echo    2. Kalau masih terkunci, daftarkan folder proyek ke daftar
    echo       kecuali Windows Defender. Jalurnya: Virus ^& threat
    echo       protection -^> Manage settings -^> Exclusions -^>
    echo       Add folder. Yang didaftarkan:
    echo          %PROJECT%
    echo       Bonus: build-mu juga jadi lebih cepat setelah itu.
    popd
    goto gagal
)
echo       OK, bersih.
echo.

popd
echo [4/4] Lanjut ke build + install seperti biasa...
echo.
call "%PROJECT%\3-update-ke-hp.bat"
goto selesai

:gagal
echo.
pause

:selesai
endlocal
