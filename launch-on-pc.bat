@echo off
set "ADB=C:\Users\jdgen\AppData\Local\Microsoft\WinGet\Packages\Google.PlatformTools_Microsoft.Winget.Source_8wekyb3d8bbwe\platform-tools\adb.exe"
set "SCRCPY=C:\Users\jdgen\AppData\Local\Microsoft\WinGet\Packages\Genymobile.scrcpy_Microsoft.Winget.Source_8wekyb3d8bbwe\scrcpy-win64-v5.0.1\scrcpy.exe"
set "DEVICE=192.168.2.100:42961"

echo ----------------------------------------------------
echo Connexion au Chromecast: %DEVICE%
echo ----------------------------------------------------
"%ADB%" connect %DEVICE%

if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo Installation de app-debug.apk...
    "%ADB%" -s %DEVICE% install -r app\build\outputs\apk\debug\app-debug.apk
    "%ADB%" -s %DEVICE% shell am start -n com.minimal.tvlauncher/.MainActivity
)

echo Ouverture de la fenetre Scrcpy sur votre ecran PC...
"%SCRCPY%" -s %DEVICE% --window-title "Google TV 4K - Apple TV Minimal Launcher" --max-size 1920 --max-fps 60 --video-bit-rate 16M
