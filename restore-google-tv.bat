@echo off
chcp 65001 >nul
echo =======================================================
echo   Restauration de Google TV d'origine (Secours)
echo =======================================================
echo.

set ADB="%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
if not exist %ADB% set ADB="%LOCALAPPDATA%\Microsoft\WinGet\Packages\Google.PlatformTools_Microsoft.Winget.Source_8wekyb3d8bbwe\platform-tools\adb.exe"

set DEVICE=192.168.2.100:42961

echo Connexion au Chromecast (%DEVICE%)...
%ADB% connect %DEVICE%

echo.
echo Re-activation des services Google TV...
%ADB% -s %DEVICE% shell pm enable com.google.android.apps.tv.launcherx
%ADB% -s %DEVICE% shell pm enable com.google.android.tvrecommendations
%ADB% -s %DEVICE% shell pm enable com.google.android.videos
%ADB% -s %DEVICE% shell pm enable com.google.android.play.games
%ADB% -s %DEVICE% shell pm enable com.android.printspooler
%ADB% -s %DEVICE% shell pm enable com.google.android.adservices.api
%ADB% -s %DEVICE% shell pm enable com.google.android.ext.adservices.api

echo.
echo =======================================================
echo   Google TV d'origine est reactive avec succes !
echo =======================================================
pause
