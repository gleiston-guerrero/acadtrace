<#
    Script de Demostracion en Vivo para la Defensa Oral - PFC Entrega 4 (BCEL)
    Estudiante: Ernesto Gregory Luna Mora (Responsable de Secretaria y Calidad)
#>

param(
    [switch]$Tests,
    [switch]$Experimentos,
    [switch]$Certificado,
    [switch]$Git,
    [switch]$All
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $RepoRoot

function Show-Banner {
    Clear-Host
    Write-Host "==========================================================================" -ForegroundColor Cyan
    Write-Host "     ACADTRACE - DEMOSTRACION EN VIVO DE DEFENSA ORAL (PFC ENTREGA 4)     " -ForegroundColor White
    Write-Host "     Estudiante: Ernesto Gregory Luna Mora | Secretaria y Calidad         " -ForegroundColor Yellow
    Write-Host "==========================================================================" -ForegroundColor Cyan
    Write-Host ""
}

function Run-Tests {
    Write-Host "`n[1/4] EJECUTANDO SUITE DE 21 PRUEBAS AUTOMATIZADAS (C7 / LISTADO 3)..." -ForegroundColor Green
    Write-Host "Comando: python -m pytest tests/contract tests/integration tests/e2e -v`n" -ForegroundColor Gray
    python -m pytest tests/contract tests/integration tests/e2e -v
}

function Run-Experimentos {
    Write-Host "`n[2/4] EJECUTANDO BANCO EXPERIMENTAL Y COMPROBACION DE FALSOS POSITIVOS (C2/C3)..." -ForegroundColor Green
    Write-Host "Comando: python experimentos/run_experimentos.py`n" -ForegroundColor Gray
    python experimentos/run_experimentos.py
}

function Run-Certificado {
    Write-Host "`n[3/4] MOSTRANDO CERTIFICADO DE REPRODUCIBILIDAD CRIPTOGRAFICA (C4)..." -ForegroundColor Green
    Write-Host "Comando: cat experimentos/resultados/REPRODUCIBILIDAD.txt`n" -ForegroundColor Gray
    Get-Content "experimentos/resultados/REPRODUCIBILIDAD.txt"
}

function Run-Git {
    Write-Host "`n[4/4] COMPROBANDO FUSION EN LA RAMA MAIN (DOMINIO INDIVIDUAL C10)..." -ForegroundColor Green
    Write-Host "Comando: git log -n 3 --pretty=format:`"%h | %an | %s`"`n" -ForegroundColor Gray
    git log -n 3 --pretty=format:"%h | %an | %s"
    Write-Host ""
}

if ($Tests) {
    Show-Banner
    Run-Tests
    exit
}

if ($Experimentos) {
    Show-Banner
    Run-Experimentos
    exit
}

if ($Certificado) {
    Show-Banner
    Run-Certificado
    exit
}

if ($Git) {
    Show-Banner
    Run-Git
    exit
}

if ($All) {
    Show-Banner
    Run-Tests
    Run-Experimentos
    Run-Certificado
    Run-Git
    Write-Host "`n[OK] Demostracion completa finalizada con exito." -ForegroundColor Green
    exit
}

# Menu interactivo
Show-Banner
Write-Host "Selecciona la prueba que deseas ejecutar:" -ForegroundColor White
Write-Host "  1. Ejecutar las 21 pruebas automatizadas (C7)" -ForegroundColor Cyan
Write-Host "  2. Ejecutar experimentos y calculo de FPR (C2/C3)" -ForegroundColor Cyan
Write-Host "  3. Ver certificado de reproducibilidad SHA-256 (C4)" -ForegroundColor Cyan
Write-Host "  4. Ver ultimos commits en main (PR #60 fusionado)" -ForegroundColor Cyan
Write-Host "  5. Ejecutar la demostracion COMPLETA (Todo de corrido)" -ForegroundColor Green
Write-Host "  6. Salir" -ForegroundColor Gray
Write-Host ""

$opcion = Read-Host "Ingresa una opcion (1-6)"

switch ($opcion) {
    "1" { Run-Tests }
    "2" { Run-Experimentos }
    "3" { Run-Certificado }
    "4" { Run-Git }
    "5" { 
        Run-Tests
        Run-Experimentos
        Run-Certificado
        Run-Git
    }
    Default { Write-Host "Saliendo..." -ForegroundColor Gray }
}
