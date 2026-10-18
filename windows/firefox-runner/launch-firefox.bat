@echo off
setlocal enabledelayedexpansion

cd /d "%~dp0"

echo ========================================================
echo   Loop - Standalone Firefox Engine Launcher
echo   Engine: Mozilla Firefox (Gecko) + uBlock Origin
echo   Anti-Block: Strict TRR Mode 3 + Encrypted Client Hello
echo ========================================================
echo.

:: 1. Check for dedicated local Firefox Portable in current folder
set "FF_BIN=%~dp0Firefox\firefox.exe"
if exist "!FF_BIN!" goto :LAUNCH

:: 2. Check standard system Firefox installations
if exist "C:\Program Files\Mozilla Firefox\firefox.exe" (
    set "FF_BIN=C:\Program Files\Mozilla Firefox\firefox.exe"
    goto :LAUNCH
)
if exist "C:\Program Files (x86)\Mozilla Firefox\firefox.exe" (
    set "FF_BIN=C:\Program Files (x86)\Mozilla Firefox\firefox.exe"
    goto :LAUNCH
)
if exist "%LOCALAPPDATA%\Mozilla Firefox\firefox.exe" (
    set "FF_BIN=%LOCALAPPDATA%\Mozilla Firefox\firefox.exe"
    goto :LAUNCH
)

:: 3. Not found - Prompt user to download portable Firefox or install
echo [NOTICE] Standalone Firefox binary not found on standard paths.
echo.
echo Would you like to automatically download and extract Firefox Portable?
echo (This sets up a 100%% isolated Firefox engine with uBlock Origin)
echo.
set /p "CHOICE=Download Firefox Portable now? (Y/N): "
if /i "!CHOICE!"=="Y" (
    echo.
    echo Downloading Firefox Portable...
    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
        "$url = 'https://download.mozilla.org/?product=firefox-latest-ssl&os=win64&lang=en-US'; " ^
        "Write-Host 'Downloading installer...'; " ^
        "Invoke-WebRequest -Uri $url -OutFile '%~dp0firefox-setup.exe'; " ^
        "Write-Host 'Installing to %~dp0Firefox...'; " ^
        "Start-Process -FilePath '%~dp0firefox-setup.exe' -ArgumentList '/S /D=%~dp0Firefox' -Wait; " ^
        "Remove-Item -Path '%~dp0firefox-setup.exe' -Force -ErrorAction SilentlyContinue; " ^
        "Write-Host 'Ready!'"
    
    if exist "%~dp0Firefox\firefox.exe" (
        set "FF_BIN=%~dp0Firefox\firefox.exe"
        goto :LAUNCH
    )
)

echo [ERROR] Mozilla Firefox could not be located.
echo Please install Firefox from https://www.mozilla.org/firefox/
echo or place firefox.exe inside %~dp0Firefox\
pause
exit /b 1

:LAUNCH
echo Found Firefox at: "!FF_BIN!"

:: Copy policies.json to Firefox distribution directory if possible
if exist "%~dp0Firefox\" (
    if not exist "%~dp0Firefox\distribution" mkdir "%~dp0Firefox\distribution"
    copy /y "%~dp0distribution\policies.json" "%~dp0Firefox\distribution\policies.json" >nul 2>nul
)

:: Read active URL or use default
set "TARGET_URL=https://dcsplayer.blogspot.com/"
if exist "%~dp0..\active_url.txt" (
    set /p TARGET_URL=<"%~dp0..\active_url.txt"
)

echo Starting Loop in isolated Firefox engine...
echo URL: !TARGET_URL!
echo Protection: uBlock Origin active, DoH TRR Mode 3, ECH Anti-SNI active.
echo.

start "" "!FF_BIN!" -no-remote -profile "%~dp0profile" -new-window "!TARGET_URL!"
exit /b 0
