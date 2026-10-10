@echo off
where mvn >nul 2>&1
if %ERRORLEVEL% == 0 (
    mvn %*
) else (
    echo Maven not found. Please install Maven.
    exit /b 1
)
