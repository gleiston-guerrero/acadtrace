# E48: medición nominal actual y evidencia anterior

Los CSV candidatos validados manualmente se promovieron mediante copia binaria a
`microservicio-soporte/locust_esc1_stats.csv` y
`microservicio-soporte/locust_esc1_stats_history.csv`. Los candidatos originales
permanecen en `microservicio-soporte/resultados_candidatos/`.

La evidencia nominal actual se basa en `locust_esc1_stats.csv`,
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
Existe una corrida oficial separada de estrés hasta 200 usuarios, documentada abajo. El estado de HikariCP no está acreditado.

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

## Capturas nominales disponibles

Conservadas en el commit `332158e4`:

- [CSV nominal actual](E48_actual_csv.png).
- [Matriz nominal actual](E48_actual_matriz.png).
- [Usuarios e historial nominal](E48_actual_usuarios_historial.png).

Estas tres capturas ya no están pendientes. La evidencia original de
configuración del perfil nominal sigue sin estar disponible; no se reconstruye
retrospectivamente ni se sustituye con el log/JSON de la corrida anterior.

## Corrida oficial de estrés de 200 usuarios

Evidencia: `microservicio-soporte/resultados_estres/20260920_164722/`.
Código ejecutado `88649f3f`; conservación `b43008e5`.
Host `http://localhost:8085`, spawn rate 1 usuario/s, 10 minutos configurados,
599 s observados (timestamps 1789940845–1789941444), 200 usuarios máximos.
Agregado: 98.684 peticiones, 0 fallos, RPS=164.86740285525565,
media=7.86715629893033 ms, P50=7 ms, P95=15 ms, P99=23 ms,
máximo=324.8848000075668 ms; `EXIT_CODE=0`.
Criterio: P95 < 500 ms y 0 fallos. Resultado: **CUMPLE**.

La carpeta contiene `perfil.txt`, `resumen_validacion.txt` y los cuatro CSV
`locust_estres_200_stats.csv`, `locust_estres_200_stats_history.csv`,
`locust_estres_200_failures.csv` y `locust_estres_200_exceptions.csv`.
Captura conservada en `b43008e5`: [resumen de estrés](E48_estres_200_resumen.png).
Es una corrida distinta de A nominal y de D FALLIDA/HISTÓRICA (106.735 peticiones,
26 fallos). La incompleta `20260920_163041/` no es oficial ni se utiliza.
El agregado de la rampa no certifica por sí solo el estado de HikariCP,
ni disponibilidad de producción, ni equivalencia de datasets históricos.

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
