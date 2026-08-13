$fecha = Get-Date -Format "yyyyMMdd_HHmmss"
$log = "Informe-E4_BCEL\evidencias\tolerancia\evidencia_tolerancia_$fecha.log"
$ok = 0
$fail = 0

"=== Prueba de tolerancia HAProxy + 2 replicas sga-principal ===" | Tee-Object -FilePath $log
"Fecha: $(Get-Date)" | Tee-Object -FilePath $log -Append
"" | Tee-Object -FilePath $log -Append

"[1/4] Estado inicial del clúster:" | Tee-Object -FilePath $log -Append
docker ps --filter "name=sga-principal" --format "  {{.Names}}  ->  {{.Status}}" | Tee-Object -FilePath $log -Append
"" | Tee-Object -FilePath $log -Append

"[2/4] Enviando 100 peticiones a http://localhost:8080/actuator/health/liveness" | Tee-Object -FilePath $log -Append
"Al request 15 se detiene sga-principal-1 para simular caída." | Tee-Object -FilePath $log -Append
"" | Tee-Object -FilePath $log -Append

$killed = $false
for ($i = 1; $i -le 100; $i++) {
    if ($i -eq 15 -and -not $killed) {
        docker kill sga-sistema-distribuido-sga-principal-1 | Out-Null
        "  >>> req $i : docker kill sga-principal-1 <<<" | Tee-Object -FilePath $log -Append
        $killed = $true
    }
    try {
        $r = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health/liveness" -UseBasicParsing -TimeoutSec 3
        $code = $r.StatusCode
        if ($code -eq 200) { $ok++ } else { $fail++ }
        "  req $i -> $code" | Tee-Object -FilePath $log -Append
    } catch {
        $fail++
        "  req $i -> FAIL ($($_.Exception.Message))" | Tee-Object -FilePath $log -Append
    }
    Start-Sleep -Milliseconds 200
}
"" | Tee-Object -FilePath $log -Append

"[3/4] Resultado: OK=$ok  FAIL=$fail  (total 100)" | Tee-Object -FilePath $log -Append
"" | Tee-Object -FilePath $log -Append

"[4/4] Restaurando el clúster a 2 replicas..." | Tee-Object -FilePath $log -Append
docker compose up -d --scale sga-principal=2 sga-principal 2>&1 | Tee-Object -FilePath $log -Append

"" | Tee-Object -FilePath $log -Append
"=== Evidencia guardada en $log ===" | Tee-Object -FilePath $log -Append
