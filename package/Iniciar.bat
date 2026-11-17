@echo off
rem TrabajoIntegrador - no necesita Java instalado. Ver LEEME.txt
"%~dp0runtime\bin\java.exe" --module-path "%~dp0app" --add-modules javafx.controls,javafx.fxml -cp "%~dp0app\resolucion-sistemas-lineales-1.0-SNAPSHOT.jar" unsa.sistemas.view.Launcher
if errorlevel 1 (
  echo.
  echo La aplicacion no pudo iniciar. Reporte este mensaje con CHECKLIST-WINDOWS.txt
  pause
)
