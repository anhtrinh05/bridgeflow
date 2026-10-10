@echo off
rem Generates production Java/Surefire evidence, then aggregates the AI report.
setlocal
cd /d "%~dp0\.."
call scripts\test-backend.cmd
if errorlevel 1 exit /b %ERRORLEVEL%
node scripts\evaluate-ai-offline.mjs %*
exit /b %ERRORLEVEL%
