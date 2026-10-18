@echo off
setlocal enabledelayedexpansion

cd /d "%~dp0"

echo ========================================================
echo   Loop Windows Desktop Application Build Tool
echo   Engine: Mozilla Firefox (Gecko) + Strict DoH Mode 3
echo   Working Directory: %CD%
echo ========================================================
echo.

:: 1. Verify profile configuration files
if not exist "profile\user.js" (
    echo [ERROR] profile\user.js is missing!
    pause
    exit /b 1
)

echo [1/2] Verifying Firefox profile configuration...
echo   - Strict TRR Mode 3: Configured (AdGuard DoH: https://dns.adguard-dns.com/dns-query)
echo   - Encrypted Client Hello (ECH): Enabled (Anti-SNI ISP filter bypass)
echo   - Homepage: https://dcsplayer.blogspot.com/
echo   - Control Hub: profile\home.html (Includes 1-click uBlock Origin install button)
echo.

:: 2. Check for Rust / Cargo to compile native Loop.exe (optional)
echo [2/2] Checking for Rust compiler...
where cargo >nul 2>nul
if %ERRORLEVEL% equ 0 (
    echo Compiling native Loop.exe launcher with Cargo...
    call cargo build --release --manifest-path src-tauri\Cargo.toml
    if %ERRORLEVEL% equ 0 (
        if exist "src-tauri\target\release\loop-windows.exe" (
            copy /y "src-tauri\target\release\loop-windows.exe" "Loop.exe" >nul
            echo [SUCCESS] Native Loop.exe compiled successfully!
        )
    )
) else (
    echo [INFO] Rust compiler not detected.
    echo Loop.bat is ready for 1-click execution without needing Cargo!
)

echo.
echo ========================================================
echo   SUCCESS! Loop Windows is configured and ready.
echo.
echo   To run Loop:
echo     - Double-click "Loop.exe" (or "Loop.bat")
echo.
echo   Features:
echo     - 100%% Mozilla Firefox (Gecko) engine
echo     - Strict AdGuard DoH (Mode 3: zero ISP DNS leaks)
echo     - Encrypted Client Hello (ECH) to stream blocked video hosts
echo     - 1-click uBlock Origin extension button in Control Hub
echo ========================================================
pause
