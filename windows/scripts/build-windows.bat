@echo off
setlocal enabledelayedexpansion

:: Switch to the windows project directory regardless of where this script was launched from
cd /d "%~dp0"
if exist "..\package.json" (
    cd /d "%~dp0\.."
)

echo ========================================================
echo   Loop Windows Desktop Application Build Tool
echo   Working Directory: %CD%
echo ========================================================
echo.

:: 1. Verify Node.js
where npm >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Node.js is not found in your PATH.
    echo Please install Node.js (LTS version) from https://nodejs.org/
    echo and re-run this script.
    pause
    exit /b 1
)

:: 2. Verify Rust / Cargo
where cargo >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Rust / Cargo compiler is not found in your PATH.
    echo Please install Rust via https://rustup.rs/ (select default MSVC toolchain)
    echo and re-run this script.
    pause
    exit /b 1
)

echo [1/3] Installing dependencies...
call npm install
if %ERRORLEVEL% neq 0 (
    echo [ERROR] npm install failed.
    pause
    exit /b 1
)

echo.
echo [2/3] Building UI bundles with Vite...
call npm run build
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Frontend build failed.
    pause
    exit /b 1
)

echo.
echo [3/3] Compiling Tauri v2 Windows binary (.exe / .msi)...
call npx @tauri-apps/cli build
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Tauri build failed.
    pause
    exit /b 1
)

echo.
echo ========================================================
echo   SUCCESS! Standalone Windows installer generated at:
echo   %CD%\src-tauri\target\release\bundle\msi\
echo ========================================================
pause
