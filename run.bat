@echo off
setlocal
pushd "%~dp0"
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0run.ps1"
set "RUN_EXIT_CODE=%ERRORLEVEL%"
popd
if not "%RUN_EXIT_CODE%"=="0" pause
exit /b %RUN_EXIT_CODE%
