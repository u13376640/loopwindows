@echo off
echo ========================================================
echo   Loop Windows Desktop Application Build Tool
echo ========================================================
echo.

:: Check for Rust
where cargo >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Rust / Cargo is not installed.
    echo Please install Rust from https://rustup.rs/ and try again.
    pause
    exit /b 1
)

:: Check for Node.js
where npm >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Node.js is not installed.
    echo Please install Node.js from https://nodejs.org/ and try again.
    pause
    exit /b 1
)

echo [1/3] Installing frontend dependencies...
call npm install
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Failed to install npm dependencies.
    pause
    exit /b 1
)

echo.
echo [2/3] Building UI bundles...
call npm run build
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Frontend build failed.
    pause
    exit /b 1
)

echo.
echo [3/3] Compiling Tauri v2 Rust executable and installer...
call npm run tauri build
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Tauri release build failed.
    pause
    exit /b 1
)

echo.
echo ========================================================
echo   SUCCESS! Loop Windows Installer Generated:
echo   src-tauri\target\release\bundle\msi\
echo ========================================================
pause
