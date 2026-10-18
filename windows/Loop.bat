@echo off
setlocal enabledelayedexpansion

cd /d "%~dp0"

echo ========================================================
echo   Loop Windows Desktop Client
echo   Engine: Mozilla Firefox (Gecko)
echo   Anti-Block: Strict TRR Mode 3 (AdGuard DoH) + ECH
echo ========================================================
echo.

:: 1. Check local portable Firefox folder
set "FF_BIN="
if exist "%~dp0Firefox\firefox.exe" set "FF_BIN=%~dp0Firefox\firefox.exe"
if exist "%~dp0firefox\firefox.exe" set "FF_BIN=%~dp0firefox\firefox.exe"

:: 2. Check standard Windows installations
if not defined FF_BIN (
    if exist "C:\Program Files\Mozilla Firefox\firefox.exe" set "FF_BIN=C:\Program Files\Mozilla Firefox\firefox.exe"
)
if not defined FF_BIN (
    if exist "C:\Program Files (x86)\Mozilla Firefox\firefox.exe" set "FF_BIN=C:\Program Files (x86)\Mozilla Firefox\firefox.exe"
)
if not defined FF_BIN (
    if exist "%LOCALAPPDATA%\Mozilla Firefox\firefox.exe" set "FF_BIN=%LOCALAPPDATA%\Mozilla Firefox\firefox.exe"
)

:: 3. If Firefox is not found, offer 1-click portable download
if not defined FF_BIN (
    echo [NOTICE] Mozilla Firefox binary was not found on your system.
    echo.
    echo Would you like Loop to download and extract portable Firefox?
    echo (Sets up a dedicated, isolated browser engine in %~dp0Firefox\)
    echo.
    set /p "CHOICE=Download portable Firefox now? (Y/N): "
    if /i "!CHOICE!"=="Y" (
        echo.
        echo Downloading Mozilla Firefox...
        powershell -NoProfile -ExecutionPolicy Bypass -Command ^
            "$url = 'https://download.mozilla.org/?product=firefox-latest-ssl&os=win64&lang=en-US'; " ^
            "Write-Host 'Downloading setup...'; " ^
            "Invoke-WebRequest -Uri $url -OutFile '%~dp0firefox-setup.exe'; " ^
            "Write-Host 'Extracting to %~dp0Firefox...'; " ^
            "Start-Process -FilePath '%~dp0firefox-setup.exe' -ArgumentList '/S /D=%~dp0Firefox' -Wait; " ^
            "Remove-Item -Path '%~dp0firefox-setup.exe' -Force -ErrorAction SilentlyContinue; " ^
            "Write-Host 'Firefox ready!'"

        if exist "%~dp0Firefox\firefox.exe" (
            set "FF_BIN=%~dp0Firefox\firefox.exe"
        )
    )
)

if not defined FF_BIN (
    echo [ERROR] Could not locate or install Firefox.
    echo Please install Firefox from https://www.mozilla.org/firefox/
    pause
    exit /b 1
)

:: Read active URL or use default
set "TARGET_URL=https://dcsplayer.blogspot.com/"
if exist "%~dp0profile\active_url.txt" (
    set /p TARGET_URL=<"%~dp0profile\active_url.txt"
)

echo Starting Loop with dedicated Firefox engine profile...
echo Profile: "%~dp0profile"
echo URL: !TARGET_URL!
echo Protection: AdGuard DoH (Strict TRR 3), Encrypted Client Hello (Anti-SNI) active.
echo.

start "" "!FF_BIN!" -no-remote -profile "%~dp0profile" -new-window "!TARGET_URL!"
exit /b 0
