# Arnes ejecutable de conformidad canonica v1 (12 vectores)

El arnes compila e invoca ambas clases Java del repositorio y el modulo
Python del microservicio Docente. No simula ningun lenguaje. Los
porcentajes son calculados, no literales.

## Que ejerce

- `ec.edu.uteq.sga.application.service.AuditHashService#jsonCanonico`
  (sga-principal), compilada desde su fuente y invocada por reflexion.
- `ec.uteq.sga.secretaria.application.service.AuditHashService#jsonCanonico`
  (secretaria), igual.
- `docentes/auditoria/hashing.py#json_canonico` (microservicio-docente),
  importado directo por ruta de archivo.

## Autocompilacion y deteccion de clases desactualizadas

`arnes_12_vectores.py` **autocompila**: antes de ejecutar los tests
verifica la frescura de las clases en
`experimentos/java_harness/build` frente a las tres fuentes que las
producen:

- `sga-principal/src/main/java/ec/edu/uteq/sga/application/service/AuditHashService.java`
- `microservicio-secretaria/backend/src/main/java/ec/uteq/sga/secretaria/application/service/AuditHashService.java`
- `experimentos/java_harness/CanonicoRunner.java`

Si el directorio `build` no existe, falta alguna clase, o el `mtime` de
cualquier `.java` es mayor que el de su respectivo `.class`, el script
invoca `javac` automaticamente (`subprocess.run()` con UTF-8 y classpath
resuelto desde `~/.m2`). Si la compilacion falla, el arnes aborta de
inmediato con codigo de salida no cero e imprime el error de compilacion.

Consecuencia: modificar (o sabortear) cualquier `AuditHashService.java`
**sin recompilar a mano** se detecta, se recompila de forma transparente
y el arnes sale con codigo 1 si hay una regresion. No hay que ejecutar
`javac` manualmente salvo que se desee un build explicito.

## Integracion en CI (GitHub Actions)

El job `arnes-canonico` de `.github/workflows/ci-cd.yml` ejecuta
`python experimentos/arnes_12_vectores.py` en cada `push`/`pull request`
sobre la rama principal y las ramas de entrega:

- Configura **JDK 17** (Temurin, con cache Maven) y **Python 3.12**.
- Resuelve las dependencias minimas de Jackson y `spring-context` en
  `~/.m2` con `./mvnw -q -DskipTests test-compile` dentro de
  `sga-principal`.
- Ejecuta el arnes; si este falla (exit 1), el job se pinta de rojo y,
  ademas, es compuerta del job `build-images` (`needs`), de modo que el
  despliegue no avanza ante una regresion del contrato canonico.

## Requisitos

- JDK 17 o 21 con `javac` y `java` en el PATH.
- Python 3.12 (solo biblioteca estandar para el arnes).
- Los jars Jackson 2.15.4 (`core`, `annotations`, `databind`, `jsr310`) y
  `spring-context` 6.1.6 en `~/.m2/repository` (los deja
  `./mvnw -q -DskipTests test-compile` en cada modulo; el modulo de
  secretaria requiere JDK 21 para compilarse completo, pero el arnes solo
  necesita los jars ya resueltos).
- En Windows, `os.pathsep` resuelve el separador de classpath; los comandos
  de ejemplo usan sintaxis de cada consola.

## Procedimiento desde clon limpio

```bash
# 1. (Opcional) Clases Java del arnes; el arnes las autocompila si no existen
javac -encoding UTF-8 -cp "<jackson-core>:<jackson-annotations>:<jackson-databind>:<jsr310>:<spring-context>" \
  -d experimentos/java_harness/build \
  sga-principal/src/main/java/ec/edu/uteq/sga/application/service/AuditHashService.java \
  microservicio-secretaria/backend/src/main/java/ec/uteq/sga/secretaria/application/service/AuditHashService.java \
  experimentos/java_harness/CanonicoRunner.java

# 2. Camino canonico con Maven donde haya JDK 21 (verificacion completa):
cd sga-principal && ./mvnw -q -DskipTests test-compile && cd ..
cd microservicio-secretaria/backend && ./mvnw -q -DskipTests test-compile && cd ../..

# 3. Ejecutar el arnes (autocompila y verifica frescura por su cuenta)
python experimentos/arnes_12_vectores.py
```

## Salida esperada (evidencia congelada)

```
Clases Java del arnes actualizadas (sin recompilacion).
...
  * Principal == Secretaria (Java == Java): 12/12 (100.0%)
  * Principal == Secretaria == Python:      8/12 (66.7%)
  * Vectores con alguna divergencia:        4/12
Aserciones OK: sin regresion respecto a la evidencia congelada.
```

Divergencias medidas (vectores 7, 10, 11, 12): flotantes de rango extremo
(`1.0E-7` vs `1e-07`, `1.0E21` vs `1e+21`), orden de claves suplementarias
(UTF-16 vs puntos de codigo) y NaN (`"NaN"` Java vs `NaN` Python).

## Consideracion: divergencia de marcas de tiempo en produccion

Los vectores JSON entregan el `timestamp` **preformateado como cadena**
(por ejemplo `2026-09-21T20:00:00Z`), por lo que esa fila coincide entre
lenguajes. En produccion, en cambio, los servicios manejan la marca de
tiempo como objeto nativo y las representaciones difieren:

- Java `Instant.toString()` produce ISO-8601 con sufijo `Z`
  (`2026-09-21T20:00:00Z`).
- Python `datetime.isoformat()` con timezone UTC produce offset explicito
  (`2026-09-21T20:00:00+00:00`).

Esta divergencia de tipos esta fuera del vector patron v1 y no cambia el
conteo 12/12 y 8/12 del arnes. Refuerza la decision arquitectonica de
persistir y verificar el `contenido_canonico` textual emitido por cada
servicio (rehash del texto almacenado) en lugar de exigir identidad
caracter por caracter entre lenguajes. Documentada en
`docs/adr/ADR-007-auditoria-unificada.md` y en la seccion de
`subsec:unificacion-canonica` del informe E4.

## Sensibilidad (prueba de sabotaje)

- Cambiar una clave de `SECRET_KEYS` en cualquier `AuditHashService.java`
  y correr `python experimentos/arnes_12_vectores.py` **sin recompilar a
  mano**: el arnes detecta el desfase, recompila automaticamente y sale
  con codigo 1 (`Java Principal != Java Secretaria`).
- Cambiar una clave de `SECRET_KEYS` en `hashing.py` y correr: sale con
  codigo 1 (`Java == Python cambio de 8 a ...`).
- Romper la sintaxis de un `.java`: el arnes aborta con codigo 1
  imprimiendo el error de `javac`.
- Revertir siempre el sabotaje despues de verificar.
