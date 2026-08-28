# Script de ejecucion rapida para la prueba de carga con Locust
param (
    [string]$HostUrl = "http://localhost:8083",
    [int]$Users = 50,
    [int]$SpawnRate = 10,
    [string]$RunTime = "60s"
)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  PRUEBA DE CARGA: MICROSERVICIO SOPORTE (LOCUST)         " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "Host objetivo:     $HostUrl" -ForegroundColor Yellow
Write-Host "Usuarios:          $Users" -ForegroundColor Yellow
Write-Host "Tasa de rampa:     $SpawnRate usuarios/seg" -ForegroundColor Yellow
Write-Host "Duracion:          $RunTime" -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Cyan

# Validar que locust este instalado
if (-not (Get-Command locust -ErrorAction SilentlyContinue)) {
    Write-Host "Locust no esta instalado en Python. Instalando..." -ForegroundColor Yellow
    pip install locust
}

# Ejecutar la prueba
locust -f locustfile.py --headless -u $Users -r $SpawnRate --run-time $RunTime --host $HostUrl --html reporte_soporte.html --csv resultados_soporte

Write-Host ""
Write-Host "Prueba completada." -ForegroundColor Green
Write-Host "Reporte HTML generado en: reporte_soporte.html" -ForegroundColor Green
Write-Host "Resultados CSV en:        resultados_soporte_stats.csv" -ForegroundColor Green
