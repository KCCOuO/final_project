@echo off
title 72-Hour Prediction Preprocessing Suite Launcher
color 0B
echo ==================================================
echo   72-Hour Prediction Preprocessing GUI Launcher
echo ==================================================
echo.

cd /d "%~dp0"

:: Check python installation
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Python is not installed or not in PATH!
    echo Please install Python and try again.
    pause
    exit /b 1
)

echo [INFO] Launching Premium Preprocessing GUI...
start "" pythonw data_preprocessing_gui.py
exit
