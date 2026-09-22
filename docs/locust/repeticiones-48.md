# Repeticiones de carga — #48

Conjunto complementario final, medido el 22 de septiembre de 2026:

| Escenario | Corridas seleccionadas | n |
|---|---|---:|
| Nominal | nominal_02, nominal_03, nominal_06 | 3 |
| Estrés | estres_01, estres_05, estres_06 | 3 |

El [resumen reproducible](../../microservicio-soporte/resultados_repeticiones/resumen_repeticiones.md)
y su [JSON](../../microservicio-soporte/resultados_repeticiones/resumen_repeticiones.json)
se calculan exclusivamente desde esas seis carpetas. Los conjuntos históricos
del manifiesto anterior permanecen intactos y no se suman a este n.

## Diagnóstico y estabilización

Los [datos del diagnóstico](../../microservicio-soporte/resultados_repeticiones/diagnostico_20260922_082830/analisis_temporal.json)
y los [eventos de energía de Windows](../../microservicio-soporte/resultados_repeticiones/diagnostico_20260922_082830/eventos_energia.json)
documentan las discontinuidades anteriores (horas UTC del 22 de septiembre):

| Corrida | Inicio / final | Hueco >5 s y máximo | Usuarios antes / después |
|---|---|---|---|
| nominal_05 | 12:07:47 / 12:14:37 | Uno: 12:10:55–12:14:36, 221 s | 50 / 50 |
| estres_03 | 12:16:00 / 12:29:41 | Uno: 12:19:06–12:29:41, 635 s | 185 / 186 |

Windows registró entrada en espera moderna por inactividad durante ambos
intervalos. Las salidas coinciden con el final de los huecos; las consolas
registran después cierre de Locust por tiempo cumplido, sin errores de conexión
ni pausas explícitas. Esto acredita suspensión concurrente, sin demostrar que
cada segundo del desfase tenga una única causa. Las consolas no contienen los
logs completos del backend o Docker. `RemoteException` al inicio corresponde
al envoltorio PowerShell de stderr, no a una excepción de usuario Locust.

El diagnóstico encontró 8 núcleos / 16 procesadores lógicos, 25 % de CPU ocupada,
2,54 GiB de RAM disponible y 7,43 GiB asignados a Docker. Son muestras puntuales.
El plan Equilibrado tenía suspensión a 180 s con batería y 300 s conectado.
No se cambiaron esos valores, hibernación ni Modern Standby.

Con autorización se usó `SetThreadExecutionState` con solicitudes temporales
de sistema y pantalla. El supervisor Python las mantiene en su hilo y libera
con `ES_CONTINUOUS` en `finally`, después de cerrar el hijo Locust incluso ante
interrupción. El fin del hilo también elimina la solicitud. El inhibidor se
liberó al terminar cada corrida, antes de validar e iniciar la siguiente.
No quedaron procesos Locust ni supervisores activos al finalizar.

## Nuevas corridas

Antes de cada corrida se comprobaron los tres contenedores, `pg_isready`, health
de etcd, `/actuator/health=UP` y puertos 5433, 2379 y 8085. El JWT real se obtuvo
en memoria del contenedor y se usó en la misma sesión PowerShell que lanzó
Locust; se retiró del entorno al finalizar, sin imprimirlo ni guardarlo.

| Corrida | Usuarios / spawn | Configurada | Observada | EXIT_CODE / fallos | Huecos >5 s |
|---|---|---|---|---|---:|
| nominal_06 | 50 / 5 por s | 300 s | 298 s | 0 / 0 | 0 |
| estres_05 | 200 / 1 por s | 600 s | 599 s | 0 / 0 | 0 |
| estres_06 | 200 / 1 por s | 600 s | 598 s | 0 / 0 | 0 |

Las tres tienen consola no vacía, cero excepciones, sin errores HTTP 401/500/503
registrados y separación máxima de 2 s entre muestras. Cada una se ejecutó una
sola vez. La primera sesión se detuvo antes de iniciar estres_05 por un fallo
en la extracción del JWT; recoger toda la salida de Docker antes de filtrarla
permitió continuar sin repetir nominal_06 ni crear una corrida parcial de estrés.

La duración observada se calcula entre primera y última muestra Aggregated,
con tolerancia de ±5 s respecto a la configuración. Se exigen máximo de usuarios
exacto, cero fallos, código de salida 0, consola no vacía y ningún hueco >5 s.

Quedan excluidas y conservadas: nominal_01 (validación incompleta), nominal_04
con sufijo `_configuracion_invalida`, nominal_05 (duración), estres_02 (inválida),
estres_03 (duración y usuarios), estres_04 con sufijo `_configuracion_invalida`,
y las carpetas nominales incompletas/interrumpidas. Sus métricas no se usan.

## Estadística, integridad y verificación

Se publican media, mediana, desviación estándar muestral, mínimo, máximo e IC95
percentil bootstrap de la media entre corridas: **10000 remuestras, semilla
12345**, muestreo con reemplazo de las tres corridas, interpolación lineal en
los percentiles 2,5 y 97,5. La semilla se reinicia para cada métrica, de modo
que se usan las mismas selecciones de corridas. n=3 limita fuertemente la
precisión; los percentiles de latencia de cada CSV no equivalen a percentiles
de la población conjunta de peticiones. No se atribuyen diferencias a causas
del backend ni se afirma independencia estadística garantizada entre sesiones.

[SHA256SUMS.txt](../../microservicio-soporte/resultados_repeticiones/SHA256SUMS.txt)
cubre los archivos de las seis corridas y ambos resúmenes. Se preservan bytes
exactos mediante `.gitattributes`; el verificador rechaza entradas faltantes,
cambios de bytes, discrepancias métricas y resúmenes obsoletos sin regenerarlos.
Solamente los hashes publicados de los dos TXT históricos derivados de
`20260920_164722` se verifican con finales LF, como sus blobs Git; esto no aplica
a CSV ni a la nueva evidencia y no reescribe aquellos archivos.

```bash
python scripts/recalcular_metricas_carga.py --check-repeticiones --check-latex
python scripts/generar_sha256.py --verify
python scripts/test_recalcular_metricas_carga.py
python scripts/test_repeticiones_carga.py
```

Las mutaciones ocurren exclusivamente en directorios temporales: bytes, hashes,
conteos, usuarios, tiempos, fallos/excepciones, consola, código de salida,
liberación del inhibidor, semilla y valores publicados. La CI de
`.github/workflows/evidencia-carga.yml` ejecuta verificaciones y pruebas, no
genera cargas ni regenera referencias para ocultar diferencias. Su ejecución
remota requiere publicar los cambios; en esta sesión solo se valida localmente.
