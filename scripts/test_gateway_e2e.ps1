# ==============================================================================
# Script de Pruebas de Integración End-to-End para el API Gateway (HAProxy)
# SGA Escuela - Microservicio de Secretaría (Matrículas y Gateway)
# ==============================================================================
param (
    [string]$GatewayHost = "http://localhost:8082",
    [string]$HaProxyStats = "http://localhost:8404"
)

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "   EJECUCIÓN DE PRUEBAS DE INTEGRACIÓN E2E: API GATEWAY (HAPROXY)    " -ForegroundColor Cyan
Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "Target Gateway: $GatewayHost"
Write-Host "HAProxy Stats : $HaProxyStats"
Write-Host ""

$testsPassed = 0
$testsFailed = 0

function Assert-Test {
    param (
        [string]$TestName,
        [bool]$Condition,
        [string]$Details = ""
    )
    if ($Condition) {
        Write-Host "[PASSED] $TestName" -ForegroundColor Green
        if ($Details) { Write-Host "         $Details" -ForegroundColor DarkGray }
        $global:testsPassed++
    } else {
        Write-Host "[FAILED] $TestName" -ForegroundColor Red
        if ($Details) { Write-Host "         Detalle: $Details" -ForegroundColor Yellow }
        $global:testsFailed++
    }
}

# ------------------------------------------------------------------------------
# TEST 1: Health Check y Conectividad del API Gateway
# ------------------------------------------------------------------------------
Write-Host "--- TEST 1: Conectividad y Health Actuator a través del Gateway ---" -ForegroundColor Yellow
try {
    $resHealth = Invoke-WebRequest -Uri "$GatewayHost/actuator/health" -Method Get -TimeoutSec 5 -UseBasicParsing
    $statusCode = $resHealth.StatusCode
    Assert-Test "1.1 Health endpoint responde 200 OK" ($statusCode -eq 200) "HTTP $statusCode"
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Assert-Test "1.1 Health endpoint responde 200 OK" ($statusCode -eq 200) "Excepción: $($_.Message)"
}

# ------------------------------------------------------------------------------
# TEST 2: Atributo de Seguridad - 401 Unauthorized sin Token JWT
# ------------------------------------------------------------------------------
Write-Host "`n--- TEST 2: Validación del Atributo de Seguridad (401 Unauthorized) ---" -ForegroundColor Yellow

$endpoints = @(
    "/api/secretario/estudiantes",
    "/api/secretario/matriculas",
    "/api/secretario/usuarios",
    "/api/secretario/reportes/nomina-matriculas"
)

foreach ($ep in $endpoints) {
    try {
        $res = Invoke-WebRequest -Uri "$GatewayHost$ep" -Method Get -TimeoutSec 5 -UseBasicParsing
        Assert-Test "2. Endpoint $ep sin token debe responder 401" ($false) "Respondio inesperadamente 200 OK"
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        $is401 = ($code -eq 401)
        Assert-Test "2. Endpoint $ep sin token -> 401 Unauthorized" $is401 "Codigo HTTP obtenido: $code"
    }
}

# ------------------------------------------------------------------------------
# TEST 3: Atributo de Seguridad - 401 Unauthorized con Token Inválido/Manipulado
# ------------------------------------------------------------------------------
Write-Host "`n--- TEST 3: Token Inválido o Manipulado ---" -ForegroundColor Yellow
try {
    $headers = @{ "Authorization" = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.token_falso.invalido" }
    $res = Invoke-WebRequest -Uri "$GatewayHost/api/secretario/estudiantes" -Headers $headers -Method Get -TimeoutSec 5 -UseBasicParsing
    Assert-Test "3. Token corrupto debe rechazar con 401" ($false) "Respondio inesperadamente 200 OK"
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    $is401 = ($code -eq 401)
    Assert-Test "3. Token corrupto -> 401 Unauthorized" $is401 "Codigo HTTP obtenido: $code"
}

# ------------------------------------------------------------------------------
# RESUMEN FINAL
# ------------------------------------------------------------------------------
Write-Host "`n======================================================================" -ForegroundColor Cyan
Write-Host "   RESUMEN FINAL DE PRUEBAS E2E DEL GATEWAY: $testsPassed PASSED, $testsFailed FAILED" -ForegroundColor Cyan
Write-Host "======================================================================" -ForegroundColor Cyan
