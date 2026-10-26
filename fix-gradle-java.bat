@echo off
echo === Gradle Java Compatibility Fix Script ===
echo.

cd /d "%~dp0"

echo Step 1: Stopping Gradle daemons...
call gradlew.bat --stop 2>nul
echo Done
echo.

echo Step 2: Cleaning corrupted Gradle 8.5 cache...
if exist "%USERPROFILE%\.gradle\caches\8.5" (
    rmdir /s /q "%USERPROFILE%\.gradle\caches\8.5"
    echo Removed corrupted cache
) else (
    echo Cache already clean
)
echo.

echo Step 3: Please configure your IDE manually:
echo 1. Go to File ^> Settings ^> Build, Execution, Deployment ^> Build Tools ^> Gradle
echo 2. Set 'Gradle JVM' to Java 17 or Java 21 (NOT Java 25)
echo 3. Click Apply and OK
echo 4. Sync your project
echo.

pause

