# E48: medición nominal actual y evidencia anterior

Los CSV candidatos validados manualmente se promovieron mediante copia binaria a
`microservicio-soporte/locust_esc1_stats.csv` y
`microservicio-soporte/locust_esc1_stats_history.csv`. Los candidatos originales
permanecen en `microservicio-soporte/resultados_candidatos/`.

La evidencia válida actual se limita a `locust_esc1_stats.csv`,
`locust_esc1_stats_history.csv` (ambos en `microservicio-soporte/`) y la matriz
ISO derivada de ellos: `docs/experimentos/resultados/matriz_iso25010.csv` e
`Informe-E4_BCEL/matriz_iso25010_generada.tex`. Las macros de
`Informe-E4_BCEL/cifras_carga_generadas.tex` también son una salida derivada,
no una validación independiente de la ejecución.

La ejecución histórica registró P99=850 ms y no cumplía PI-1. En una nueva
ejecución nominal válida sobre el entorno local actual, con 50 usuarios,
spawn rate de 5 usuarios/s y duración configurada de 5 minutos, se registró
P99=23 ms, 12.217 peticiones y 0 fallos, por lo que el escenario actual cumple
el criterio de latencia PI-1. La comparación no se utiliza para atribuir
causalidad exclusiva a un cambio concreto, incluido `JwtParser`, porque no
está acreditada la equivalencia del dataset histórico y el actual.
No se acredita estrés hasta 200 usuarios ni el estado de HikariCP.

El máximo observado de 47889.95589996921 ms pertenece a
`/api/soporte/tickets`: evento aislado según la validación manual, conservado
sin borrar ni filtrar. El endpoint tiene P95=13 ms, P99=32 ms y 0 fallos.
El máximo no es el P99; el P99 agregado es 23 ms y el P95 agregado es 9 ms.

El historial contiene timestamps 1789893976–1789894274: 298 segundos entre
muestras, distintos de los 300 segundos configurados. Corresponde al
2026-09-20 de 08:46:16 a 08:51:14 UTC (03:46:16–03:51:14 UTC−05:00).
`resultados_candidatos/locust_esc1_candidato_console.log` y
`resultados_candidatos/locust_esc1_candidato_validation.json` pertenecen a una
ejecución ANTERIOR de las 07:09 UTC. NO corresponden a la corrida promovida,
NO acreditan temporalmente esta ejecución y quedan excluidos del conjunto
de evidencia actual. Se conservan sin modificar. No se ha creado una
validación retrospectiva. El perfil y la duración configurada se apoyan
únicamente en la validación manual comunicada; falta conservar la evidencia
de configuración de esta ejecución concreta.

Los candidatos `locust_esc1_candidato_stats.csv` y
`locust_esc1_candidato_stats_history.csv` son idénticos byte por byte a los
CSV oficiales actuales. Los candidatos `locust_esc1_candidato_failures.csv`
y `locust_esc1_candidato_exceptions.csv` solo contienen encabezados: no
permiten determinar su corrida de origen. Se excluyen de la evidencia
actual hasta acreditar su procedencia. Los cero fallos actuales se derivan
de los CSV actuales de estadísticas, no de estos archivos vacíos.

## Capturas manuales pendientes

1. `E48_actual_csv.png`: abrir el CSV oficial con ruta, encabezados y fila
   `Aggregated` visibles: Request Count=12217, Failure Count=0, 95%=9 y 99%=23.
   Incluir la fila tickets con su máximo, P95 y P99 si cabe; de lo contrario,
   tomar una captura adicional `E48_actual_tickets.png`.
2. `E48_actual_usuarios_historial.png`: mostrar la ruta del historial actual,
   User Count máximo=50 y los timestamps inicial y final. Se puede capturar
   la salida de `scripts/recalcular_metricas_carga.py`, que calcula usuarios
   y duración observada directamente de los CSV.
3. `E48_actual_perfil.png`: capturar la configuración o consola ORIGINAL de
   esta corrida con 50 usuarios, 5 usuarios/s y `--run-time 5m`, junto con
   fechas que permitan vincularla al historial. Si ya no está disponible,
   declarar la evidencia faltante; una ejecución nueva tendrá sus propios
   resultados y no debe presentarse como esta corrida de 12.217 peticiones.
4. `E48_actual_matriz.png`: ejecutar `generar_matriz.py --preview --format csv`
   y capturar la fila de rendimiento con p99_ms=23, criterio `p99 < 500 ms`
   y veredicto de cumplimiento nominal.

No se han creado ni alterado imágenes. `Evidencia_11_E48_CSV_Oficial.png`
se conserva como captura histórica anterior a la promoción, no como CSV
actual. `Evidencia_10_E48_Codigo.png` documenta código y por sí sola no
acredita los resultados actuales. La figura `locust_resultados.png` del
informe sigue identificada como histórica/complementaria.

## Cobertura de Soporte

El reporte local `microservicio-soporte/backend/target/site/jacoco/jacoco.xml`
contiene LINE=458/640: 71,5625 %, superior al umbral del 70 %; compuerta
aprobada. Se copió sin edición a
`docs/cobertura/soporte/actual/jacoco.xml`, fuente actual de la matriz.
La matriz redondea a 71,56 %. El XML anterior 459/641 sigue intacto en
`docs/cobertura/soporte/jacoco.xml`. En la revisión final se ejecutó
`mvn -o verify` con Maven 3.9.9 y Java 21 (release 17): 74 pruebas,
0 fallos, 0 errores, 0 omitidas, `BUILD SUCCESS` y compuerta aprobada.
El XML recién generado confirma los mismos contadores; la copia conservada
no se reemplazó ni se modificaron umbrales. Esta comprobación de código y
cobertura no constituye una validación retrospectiva de la corrida Locust.
