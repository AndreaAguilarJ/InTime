@echo off
echo Renombrando paquete de com.momentummm.app a com.momentummm.app
echo.

REM 1. Cambiar todas las referencias en archivos .kt
echo Paso 1: Actualizando archivos Kotlin...
powershell -Command "(Get-ChildItem -Path '%~dp0app\src' -Filter *.kt -Recurse) | ForEach-Object { (Get-Content $_.FullName) -replace 'com\.momentum\.app', 'com.momentummm.app' | Set-Content $_.FullName }"

REM 2. Cambiar referencias en AndroidManifest.xml
echo Paso 2: Actualizando AndroidManifest.xml...
powershell -Command "(Get-ChildItem -Path '%~dp0app\src' -Filter AndroidManifest.xml -Recurse) | ForEach-Object { (Get-Content $_.FullName) -replace 'com\.momentum\.app', 'com.momentummm.app' | Set-Content $_.FullName }"

REM 3. Cambiar referencias en archivos XML
echo Paso 3: Actualizando archivos XML...
powershell -Command "(Get-ChildItem -Path '%~dp0app\src' -Filter *.xml -Recurse) | ForEach-Object { (Get-Content $_.FullName) -replace 'com\.momentum\.app', 'com.momentummm.app' | Set-Content $_.FullName }"

echo.
echo Paso 4: Ahora debes renombrar manualmente la carpeta:
echo   DESDE: app\src\main\java\com\momentum
echo   HACIA: app\src\main\java\com\momentummm
echo.
echo Puedes hacerlo en Android Studio:
echo   1. Click derecho en la carpeta 'momentum' en el panel de proyecto
echo   2. Selecciona Refactor ^> Rename
echo   3. Cambia 'momentum' a 'momentummm'
echo   4. Marca 'Search in comments and strings'
echo   5. Click en 'Refactor'
echo.
echo O manualmente con el explorador de archivos.
echo.
pause

