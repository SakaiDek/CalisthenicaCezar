@echo off
setlocal

set "ADB=C:\Android\platform-tools\adb.exe"
set "APK=C:\Users\Sakai\Documents\CalisthenicaCezar\app\build\outputs\apk\debug\app-debug.apk"

echo ==========================================
echo  Pasang Calisthenica Cezar ke Poco F5
echo ==========================================
echo.

if not exist "%ADB%" (
    echo GAGAL: adb tidak ketemu di %ADB%
    goto fail
)
if not exist "%APK%" (
    echo GAGAL: APK belum ada. Jalankan dulu 1-setup-dan-build.bat
    goto fail
)

echo [1/3] Cek HP tersambung...
"%ADB%" devices
echo.
echo      Kalau di atas TIDAK ada baris "device", berarti HP belum kebaca.
echo      Nyalakan USB Debugging di Poco F5, lalu ACC popup "Allow USB debugging".
echo.

echo [2/3] Install APK...
"%ADB%" install -r "%APK%"
if errorlevel 1 (
    echo.
    echo GAGAL di langkah 2: install.
    goto fail
)
echo.

echo [3/3] Buka app di HP...
"%ADB%" shell am start -n com.cezar.calisthenica/.MainActivity
echo.

echo ==========================================
echo               S U K S E S
echo ==========================================
echo Lihat layar Poco F5. Harus muncul judul "Calisthenica Cezar"
echo dan tulisan "Fondasi siap."
echo.
goto end

:fail
echo.
echo Blok semua teks di jendela ini, tempel ke Claude.
echo.

:end
pause
endlocal
