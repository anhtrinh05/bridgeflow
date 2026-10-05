@echo off
rem Runs test-backend.ps1 without changing the machine or user PowerShell execution policy.
rem Bypass applies only to this powershell.exe process. Extra arguments are forwarded,
rem for example: scripts\test-backend.cmd -PostgresBin "C:\Program Files\PostgreSQL\18\bin"
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0test-backend.ps1" %*
exit /b %ERRORLEVEL%
