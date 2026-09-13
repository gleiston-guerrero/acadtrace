# E7 — Generación e integridad de una ejecución

## Entorno y comandos

Se espera Python 3.12 con pip y las dependencias de `experimentos/requirements.txt`. En Windows debe estar disponible `python` en PATH; en Linux puede usarse `python3`. No se necesita backend, PostgreSQL ni conectividad con producción para el modo predeterminado.

Desde la raíz, preparar el entorno y generar:

```sh
python -m pip install -r experimentos/requirements.txt
python experimentos/run_experimentos.py --mode local
python experimentos/verificar_reproducibilidad.py
python -m unittest discover -s experimentos -p test_reproducibilidad.py -v
```

La generación y certificación se completan en una sola invocación de `run_experimentos.py`. `bash experimentos/reproducibilidad.sh --mode local` es un wrapper del mismo proceso, sin lógica duplicada. Usa `python3` o el ejecutable indicado por `PYTHON_CMD`.

`--mode` prevalece sobre `EXPERIMENT_MODE`; sin ambos, el modo es `local`. El modo local no prueba disponibilidad HTTP. El modo `--mode http` debe solicitarse explícitamente y usa `BACKEND_URL` (por defecto localhost:8080): modifica el modo de auditoría y envía calificaciones al backend experimental. Si el backend, la conmutación o una transacción fallan, aborta; nunca cambia automáticamente a local. No se ejecuta HTTP en CI. Las credenciales existentes no se alteran.

## Artefactos oficiales E7

La salida predeterminada es `experimentos/resultados/`; `--output-dir` permite otra carpeta. El certificado `REPRODUCIBILIDAD.txt` incluye exactamente:

- `deteccion.csv`
- `manipulaciones.csv`
- `exp1_concurrencia.csv`
- `exp3_reconciliacion.csv`
- `iso25010.csv`
- `boxplot_latencia.png`

Cada línea contiene `<SHA-256 de 64 hexadecimales>`, dos espacios y el nombre. No hay encabezados ni fecha variable en el certificado. No incluye Locust ni `falsos_positivos.csv`. El proceso E7 no actualiza las copias de `docs/experimentos/resultados/` ni los resultados de carga E5.

Los seis artefactos se generan en una carpeta temporal nueva y vacía. La dependencia gráfica se comprueba antes; los errores de Matplotlib son fatales. Solo tras comprobar existencia, tamaño no nulo, LF en CSV y los seis hashes se publican los archivos, con el certificado al final. Los siete archivos anteriores, si existen, se conservan en una carpeta `e7-anterior-*` dentro del destino; no deben confundirse con la ejecución vigente. Una interrupción durante publicación puede dejar un conjunto incompleto, que el verificador rechazará; la copia anterior queda conservada.

Todos los escritores CSV especifican `newline=""` y `lineterminator="\n"`. La comprobación rechaza CR antes de firmar, en consonancia con `.gitattributes`; no se normalizan datos después de calcular SHA-256.

## Alcance y datos variables

Reproducibilidad del proceso no significa identidad byte a byte entre ejecuciones. La semilla `20260831` fija secuencias pseudoaleatorias; `time.time()`, `perf_counter()` y `perf_counter_ns()` siguen aportando timestamps y tiempos medidos. No se sustituyen por latencias ficticias. El PNG depende de esas mediciones y del entorno gráfico. SHA-256 acredita integridad de los archivos de una ejecución concreta, no equivalencia con otras ejecuciones ni validez científica de sus conclusiones.

Detección, manipulaciones y reconciliación usan datos sintéticos en memoria. El experimento 1 local mide operaciones secuenciales del motor criptográfico; el parámetro de concurrencia no demuestra usuarios HTTP simultáneos. Su modo HTTP mide solicitudes al backend experimental. El entorno y modo se imprimen al inicio.

`iso25010.csv` se deriva de los historiales conservados `experimentos/resultados/locust_esc1_stats_history.csv` y `locust_esc3_stats_history.csv`, y de los CSV JaCoCo locales en `docs/cobertura/`. No produce una nueva corrida de carga ni modifica qué corrida es oficial en E5. Las filas son ventanas derivadas, no ejecuciones independientes. Los promedios de percentiles de muestras no son percentiles agregados. Las peticiones se etiquetan como estimadas; el porcentaje de fallos es general, no HTTP 5xx. Promedio de latencia, porcentaje HTTP 5xx, disponibilidad de producción y rechazo HTTP 401 se marcan `No disponible`. JaCoCo ausente o ilegible tampoco se sustituye por una cifra configurada.

## Verificación y prueba negativa

El verificador no necesita Matplotlib ni servicios externos. Rechaza certificado ausente, ilegible, mal formado, parcial, duplicado, con nombres ajenos, archivos ausentes/vacíos, CSV con CR y cualquier hash distinto. Imprime `OK` o `ERROR`; devuelve 0 exclusivamente cuando todo coincide. Se puede verificar una copia con `--directorio RUTA`.

Las pruebas de `test_reproducibilidad.py` usan fixtures sintéticos en carpetas temporales. La prueba de manipulación exige 0 → 1 → 0 y restaura los bytes con `finally`. No certifica que los artefactos versionados hayan sido regenerados: esa comprobación se ejecuta por separado.

El job existente `lint` ejecuta las pruebas y `python experimentos/verificar_reproducibilidad.py` sobre artefactos versionados. No regenera ni firma archivos antes de verificar, para no ocultar inconsistencias. Los certificados históricos con encabezados serán rechazados: deben sustituirse únicamente tras una generación E7 completa válida.

## Estado de validación de esta modificación

Python no está disponible en PATH en la sesión de implementación. No se ejecutaron generación, verificador Python ni prueba negativa. Los resultados y el certificado anteriores permanecen conservados, sin recertificarlos manualmente. El certificado principal anterior tiene cuatro discrepancias SHA-256 y no cumple el formato nuevo; el paso CI debe fallar hasta regenerar los seis artefactos y versionar el certificado válido. E7 sigue PARCIAL.

Pendiente configurar Python 3.12 y pip en PATH, instalar `experimentos/requirements.txt`, ejecutar los comandos anteriores y comprobar también en un clon limpio. No se afirma que las pruebas hayan pasado ni que los hashes actuales pertenezcan al proceso corregido.
