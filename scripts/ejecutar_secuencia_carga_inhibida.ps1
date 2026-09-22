param(
    [ValidateSet('nominal_06','estres_05','estres_06')]
    [string[]]$RunNames = @('nominal_06','estres_05','estres_06')
)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $root
$docker = 'C:/Users/Juliana/AppData/Local/Programs/DockerDesktop/resources/bin/docker.exe'
$python = 'C:/Users/Juliana/AppData/Local/Programs/Python/Python312/python.exe'
$batch = Join-Path $root ('microservicio-soporte/resultados_repeticiones/sesion_inhibida_' + (Get-Date -Format 'yyyyMMdd_HHmmss'))
New-Item -ItemType Directory -Path $batch | Out-Null
$log = Join-Path $batch 'sesion.log'
function Record([string]$message) {
    "$([DateTimeOffset]::UtcNow.ToString('o')) $message" | Tee-Object -FilePath $log -Append
}
try {
    foreach ($run in @(@('nominal_06',50,5,300), @('estres_05',200,1,600), @('estres_06',200,1,600))) {
        $name = $run[0]
        if ($name -notin $RunNames) { continue }
        if (Test-Path -LiteralPath (Join-Path $root "microservicio-soporte/resultados_repeticiones/$name")) {
            throw "Carpeta $name existente; no se sobrescribe"
        }
        $running = @(Get-CimInstance Win32_Process | Where-Object { $_.Name -match 'python|locust' -and $_.CommandLine -match 'locust|ejecutar_carga_inhibida' })
        if ($running.Count) {
            $running | ForEach-Object { Record "PID=$($_.ProcessId); estado=existente" }
            throw 'Hay otro proceso de carga'
        }
        Record "$name PRECHECK PID=ninguno; estado=sin otra carga"
        foreach ($container in 'sga-postgres','etcd','soporte-e48-prueba') {
            $state = & $docker inspect --format '{{.State.Running}}' $container
            if ($LASTEXITCODE -ne 0 -or $state -ne 'true') { throw "Contenedor no UP: $container" }
            Record "$container=UP"
        }
        $pg = & $docker exec sga-postgres pg_isready
        if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL no acepta conexiones' }
        Record 'PostgreSQL=acepta conexiones'
        $etcd = Invoke-RestMethod http://localhost:2379/health -TimeoutSec 10
        if ([string]$etcd.health -ne 'true') { throw 'etcd no saludable' }
        Record 'etcd health=true'
        $health = Invoke-RestMethod http://localhost:8085/actuator/health -TimeoutSec 10
        if ($health.status -ne 'UP') { throw 'Backend no UP' }
        Record 'backend health=UP'
        foreach ($port in 5433,2379,8085) {
            $client = New-Object Net.Sockets.TcpClient
            try {
                $connection = $client.ConnectAsync('localhost',$port)
                if (-not $connection.Wait(5000) -or -not $client.Connected) { throw "Puerto $port inaccesible" }
                Record "localhost:${port}=accesible"
            } finally { $client.Dispose() }
        }
        # Consumir toda la salida evita cerrar anticipadamente stdout de Docker.
        $containerEnv = @(& $docker inspect --format '{{range .Config.Env}}{{println .}}{{end}}' soporte-e48-prueba)
        $inspectCode = $LASTEXITCODE
        $jwtLine = $containerEnv | Where-Object { $_ -like 'JWT_SECRET=*' } | Select-Object -First 1
        $containerEnv = $null
        if ($inspectCode -ne 0 -or -not $jwtLine) { throw "JWT_SECRET no encontrado; docker inspect exit=$inspectCode" }
        $env:JWT_SECRET = $jwtLine.Substring('JWT_SECRET='.Length)
        $jwtLine = $null
        if ([string]::IsNullOrWhiteSpace($env:JWT_SECRET) -or $env:JWT_SECRET -eq 'dummy_secret') { throw 'JWT_SECRET invalido' }
        Remove-Item Env:SOPORTE_JWT_TOKEN -ErrorAction SilentlyContinue
        try {
            & $python (Join-Path $PSScriptRoot 'ejecutar_carga_inhibida.py') $name $run[1] $run[2] $run[3] | Tee-Object -FilePath $log -Append
            $validationCode = $LASTEXITCODE
        } finally { $env:JWT_SECRET = $null }
        if ($validationCode -ne 0) { throw "$name no validada: detener y diagnosticar antes de repetir" }
        Record "$name=VALIDA"
    }
    Record 'SECUENCIA_COMPLETA; inhibidores liberados por cada corrida'
} catch {
    Record "SECUENCIA_DETENIDA: $($_.Exception.Message)"
    exit 1
} finally {
    $env:JWT_SECRET = $null
    $containerEnv = $null
    $jwtLine = $null
    Record 'SESION_FINALIZADA; JWT retirado del entorno'
}
