@echo off
setlocal enabledelayedexpansion

set "PROJECT=C:\Users\Sakai\Documents\CalisthenicaCezar"
set "GRADLE_BAT=C:\Gradle\gradle-9.7.1\bin\gradle.bat"
set "WRAPGEN=%TEMP%\ccz-wrapgen"

echo ==========================================
echo  Calisthenica Cezar - Setup Build Pertama
echo ==========================================
echo.

if not exist "%GRADLE_BAT%" (
    echo GAGAL: tidak ketemu %GRADLE_BAT%
    echo Cek lagi lokasi folder Gradle di laptopmu.
    goto fail
)

echo [1/3] Bikin Gradle Wrapper versi 8.9 di folder sementara...
if exist "%WRAPGEN%" rmdir /S /Q "%WRAPGEN%"
mkdir "%WRAPGEN%"
rem Gradle 9 menolak jalan di folder kosong: dia butuh minimal 1 settings file.
rem Isinya sengaja kosong (tanpa plugin, tanpa subproject) supaya AGP 8.6
rem tidak pernah ikut dimuat oleh Gradle 9.
>"%WRAPGEN%\settings.gradle.kts" echo rootProject.name = "wrapgen"
pushd "%WRAPGEN%"
call "%GRADLE_BAT%" wrapper --gradle-version 8.9 --distribution-type bin
if errorlevel 1 (
    popd
    echo.
    echo GAGAL di langkah 1: pembuatan wrapper.
    goto fail
)
popd
echo      OK.
echo.

echo [2/3] Menyalin wrapper ke folder project...
xcopy /E /I /Y "%WRAPGEN%\gradle" "%PROJECT%\gradle" >nul
copy /Y "%WRAPGEN%\gradlew.bat" "%PROJECT%\" >nul
copy /Y "%WRAPGEN%\gradlew" "%PROJECT%\" >nul 2>nul
if not exist "%PROJECT%\gradlew.bat" (
    echo GAGAL di langkah 2: gradlew.bat tidak tersalin.
    goto fail
)
echo      OK.
echo.

echo [3/3] Build APK debug.
echo      Run PERTAMA ini lama (download Gradle 8.9 + dependency, ratusan MB).
echo      Biarkan jalan, jangan ditutup. Ambil kopi dulu.
echo.
pushd "%PROJECT%"
call gradlew.bat assembleDebug
if errorlevel 1 (
    popd
    echo.
    echo GAGAL di langkah 3: build.
    goto fail
)
popd
echo.

echo ==========================================
echo               S U K S E S
echo ==========================================
echo APK kamu ada di:
echo %PROJECT%\app\build\outputs\apk\debug\app-debug.apk
echo.
echo Langkah berikutnya: colok Poco F5, lalu jalankan
echo   2-pasang-ke-hp.bat
echo.
goto end

:fail
echo.
echo ==========================================
echo               G A G A L
echo ==========================================
echo Blok semua teks di jendela ini (klik kanan - Select All - Enter),
echo lalu tempel ke Claude. Jangan diakalin sendiri dulu.
echo.

:end
pause
endlocal
