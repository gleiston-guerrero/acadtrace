param(
    [string]$TargetHost = "http://localhost:8080"
)

Write-Host "=========================================================" -ForegroundColor Cyan
Write-Host " AUTOMATIZACION DE PRUEBAS Y EXPERIMENTOS - ACADTRACE E4" -ForegroundColor Cyan
Write-Host "=========================================================" -ForegroundColor Cyan
Write-Host "Destino: $TargetHost" -ForegroundColor Yellow

# 1. Configurar la clave secreta EXACTA que usa el backend para que Locust no falle
$env:JWT_SECRET = "test-only-jwt-secret"
$env:BACKEND_URL = $TargetHost

# 2. Ejecutar Locust (Genera los CSV crudos)
Write-Host "`n[Paso 1/2] Ejecutando Locust contra $TargetHost..." -ForegroundColor Green
python tests/load/escenario1_carga_nominal.py $TargetHost

# 3. Ejecutar script de experimentos (Lee los CSV de Locust, calcula ISO y FPR)
Write-Host "`n[Paso 2/2] Generando estadisticas y metricas ISO 25010..." -ForegroundColor Green
python experimentos/run_experimentos.py

Write-Host "`n[EXITO] Todas las pruebas han sido automatizadas." -ForegroundColor Cyan
Write-Host "Los CSV corregidos ya estan listos en experimentos/resultados/ para subirse a GitHub." -ForegroundColor Cyan

