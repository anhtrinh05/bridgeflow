@echo off
rem Runs evaluate-ai-offline.mjs from any working directory.
setlocal
cd /d "%~dp0\.."
node scripts\evaluate-ai-offline.mjs %*
exit /b %ERRORLEVEL%
