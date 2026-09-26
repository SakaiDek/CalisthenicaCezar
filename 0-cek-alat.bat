@echo off
setlocal EnableDelayedExpansion
title Cek Alat - Calisthenica Cezar
cd /d "%~dp0"

REM ============================================================
REM  0-cek-alat.bat
REM  Tugasnya cuma satu: MELAPOR. Semua perintah di dalam sini
REM  hanya membaca. Tidak ada yang diinstall, diubah, atau dihapus.
REM  Jalankan ini kapan pun kamu ragu "alatku masih lengkap nggak ya".
REM ============================================================

set "MASALAH=0"
set "TMPF=%TEMP%\cezar_cek.txt"

echo.
echo ============================================================
echo    CEK ALAT  -  CALISTHENICA CEZAR
echo    Hanya membaca. Tidak menginstall apa pun.
echo ============================================================
echo.

call :cek_jdk
call :cek_javahome
call :cek_sdk
call :cek_adb
call :cek_wrapper
call :cek_proyek
call :cek_git

echo ============================================================
if "!MASALAH!"=="0" (
    echo    HASIL : SIAP.
    echo    Tidak ada tool tambahan yang perlu kamu install.
    echo    Lanjut kerja: klik 3-update-ke-hp.bat
) else (
    echo    HASIL : ada !MASALAH! hal yang perlu dilihat.
    echo    Cari baris bertanda GAGAL atau PERIKSA di atas.
)
echo ============================================================
echo.
echo Salin SELURUH teks di atas dan kirim ke mentor.
echo.
pause
exit /b 0

:cek_jdk
echo [1/7] JDK ^(mesin yang mengompilasi Kotlin^)
where java >nul 2>nul
if errorlevel 1 (
    echo       GAGAL  : perintah "java" tidak ada di PATH
    set /a MASALAH+=1
    echo.
    goto :eof
)
java -version > "%TMPF%" 2>&1
for /f "delims=" %%a in ('findstr /i /c:"version" "%TMPF%"') do echo       %%a
findstr /c:"17." "%TMPF%" >nul
if errorlevel 1 (
    echo       PERIKSA: JDK yang aktif sepertinya BUKAN 17.
    echo                Proyek ini dipin ke JDK 17.
    set /a MASALAH+=1
) else (
    echo       OK     : JDK 17 aktif
)
echo.
goto :eof

:cek_javahome
echo [2/7] JAVA_HOME
set "JH=%JAVA_HOME%"
if not defined JH (
    echo       PERIKSA: JAVA_HOME kosong ^(Gradle biasanya masih jalan,
    echo                tapi lebih aman kalau diisi^)
    echo.
    goto :eof
)
echo       isi    : !JH!
if exist "!JH!\bin\java.exe" (
    echo       OK     : folder JAVA_HOME benar
) else (
    echo       GAGAL  : di dalam JAVA_HOME tidak ada bin\java.exe
    set /a MASALAH+=1
)
echo.
goto :eof

:cek_sdk
echo [3/7] Android SDK ^(informasi saja, penentunya adb di bawah^)
if exist "local.properties" (
    echo       local.properties : ADA
    for /f "delims=" %%a in ('findstr /i /c:"sdk.dir" "local.properties"') do echo       %%a
) else (
    echo       local.properties : TIDAK ADA
)
if defined ANDROID_HOME (
    echo       ANDROID_HOME     : !ANDROID_HOME!
) else (
    echo       ANDROID_HOME     : kosong
)
if exist "C:\Android\platform-tools\adb.exe" (
    echo       OK     : C:\Android\platform-tools ketemu
) else (
    echo       catatan: C:\Android\platform-tools tidak ketemu
)
if exist "C:\Android\platforms\android-34" (
    echo       OK     : platform android-34 ketemu
) else (
    echo       catatan: C:\Android\platforms\android-34 tidak ketemu
)
echo.
goto :eof

:cek_adb
echo [4/7] adb dan HP Poco F5
set "ADB="
where adb >nul 2>nul && set "ADB=adb"
if not defined ADB if exist "C:\Android\platform-tools\adb.exe" set "ADB=C:\Android\platform-tools\adb.exe"
if not defined ADB (
    echo       GAGAL  : adb.exe tidak ketemu, baik di PATH maupun di
    echo                C:\Android\platform-tools
    set /a MASALAH+=1
    echo.
    goto :eof
)
echo       adb    : !ADB!
"!ADB!" devices > "%TMPF%" 2>&1
findstr /r /c:"device$" "%TMPF%" >nul
if errorlevel 1 (
    echo       PERIKSA: belum ada HP yang siap dipakai. Nyalakan USB
    echo                debugging, colok kabel, lalu tekan Allow di HP.
    for /f "delims=" %%a in ('type "%TMPF%"') do echo                ^| %%a
    set /a MASALAH+=1
) else (
    for /f "tokens=1" %%a in ('findstr /r /c:"device$" "%TMPF%"') do echo       OK     : HP %%a tersambung
)
echo.
goto :eof

:cek_wrapper
echo [5/7] Gradle Wrapper ^(HARUS 8.9, jangan gradle global^)
if exist "gradlew.bat" (
    echo       OK     : gradlew.bat ada
) else (
    echo       GAGAL  : gradlew.bat tidak ada. Jalankan 1-setup-dan-build.bat
    set /a MASALAH+=1
)
if exist "gradle\wrapper\gradle-wrapper.properties" (
    for /f "delims=" %%a in ('findstr /i /c:"distributionUrl" "gradle\wrapper\gradle-wrapper.properties"') do echo       %%a
) else (
    echo       GAGAL  : gradle-wrapper.properties tidak ada
    set /a MASALAH+=1
)
echo.
goto :eof

:cek_proyek
echo [6/7] Berkas proyek
call :ada_file "settings.gradle.kts"
call :ada_file "build.gradle.kts"
call :ada_file "app\build.gradle.kts"
call :ada_file "app\src\main\AndroidManifest.xml"
call :ada_file "3-update-ke-hp.bat"
echo.
goto :eof

:ada_file
if exist "%~1" (
    echo       OK     : %~1
) else (
    echo       GAGAL  : %~1 TIDAK ADA
    set /a MASALAH+=1
)
goto :eof

:cek_git
echo [7/7] Git ^(opsional, cuma buat mencadangkan kode^)
where git >nul 2>nul
if errorlevel 1 (
    echo       catatan: git tidak ada di PATH. Tidak wajib untuk build.
) else (
    for /f "delims=" %%a in ('git --version') do echo       OK     : %%a
)
echo.
goto :eof
