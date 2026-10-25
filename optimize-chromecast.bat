@echo off
chcp 65001 >nul
echo =======================================================
echo   Optimisation & Debloat Chromecast with Google TV 4K
echo =======================================================
echo.

set ADB="%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
if not exist %ADB% set ADB="%LOCALAPPDATA%\Microsoft\WinGet\Packages\Google.PlatformTools_Microsoft.Winget.Source_8wekyb3d8bbwe\platform-tools\adb.exe"

set DEVICE=192.168.2.100:42961

echo Connexion au Chromecast (%DEVICE%)...
%ADB% connect %DEVICE%

echo.
echo [1/11] Desactivation du launcher Google TV publicitaire (Active le Minimal Launcher par defaut)...
%ADB% -s %DEVICE% shell pm disable-user --user 0 com.google.android.apps.tv.launcherx

echo [2/11] Desactivation du service de recommandations d'annonces...
%ADB% -s %DEVICE% shell pm disable-user --user 0 com.google.android.tvrecommendations

echo [3/11] Desactivation du suivi publicitaire AdServices...
%ADB% -s %DEVICE% shell pm disable-user --user 0 com.google.android.adservices.api
%ADB% -s %DEVICE% shell pm disable-user --user 0 com.google.android.ext.adservices.api

echo [4/11] Desactivation du profilage local...
%ADB% -s %DEVICE% shell pm disable-user --user 0 com.google.android.ondevicepersonalization.services

echo [5/11] Desactivation du calcul analytique en tache de fond...
%ADB% -s %DEVICE% shell pm disable-user --user 0 com.google.android.federatedcompute

echo [6/11] Desactivation de la telemetrie reseau Google...
%ADB% -s %DEVICE% shell pm disable-user --user 0 com.google.android.apps.tv.netoscope

echo [7/11] Desactivation de Google Play Films / TV Store...
%ADB% -s %DEVICE% shell pm disable-user --user 0 com.google.android.videos

echo [8/11] Desactivation de Google Play Jeux...
%ADB% -s %DEVICE% shell pm disable-user --user 0 com.google.android.play.games

echo [9/11] Desactivation du spouleur d'impression papier...
%ADB% -s %DEVICE% shell pm disable-user --user 0 com.android.printspooler

echo [10/11] Desactivation de Cast2Class...
%ADB% -s %DEVICE% shell pm disable-user --user 0 com.google.android.apps.education.cast2class

echo [11/11] Lancement du Launcher Minimal Style Apple TV...
%ADB% -s %DEVICE% shell am start -n com.minimal.tvlauncher.debug/com.minimal.tvlauncher.MainActivity

echo.
echo =======================================================
echo   Optimisation terminee ! ~400 Mo de RAM liberes.
echo   Appuyez sur la touche Accueil de votre telecommande.
echo =======================================================
pause
