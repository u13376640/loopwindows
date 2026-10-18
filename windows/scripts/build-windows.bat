@echo off
setlocal enabledelayedexpansion

cd /d "%~dp0\.."

call "%~dp0..\build-windows.bat"
