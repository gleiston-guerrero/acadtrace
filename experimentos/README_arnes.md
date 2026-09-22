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
# 1. Clases Java del arnes (fuentes reales + runner auxiliar)
javac -encoding UTF-8 -cp "<jackson-core>:<jackson-annotations>:<jackson-databind>:<jsr310>:<spring-context>" \
  -d experimentos/java_harness/build \
  sga-principal/src/main/java/ec/edu/uteq/sga/application/service/AuditHashService.java \
  microservicio-secretaria/backend/src/main/java/ec/uteq/sga/secretaria/application/service/AuditHashService.java \
  experimentos/java_harness/CanonicoRunner.java

# 2. Camino canonico con Maven donde haya JDK 21 (verificacion completa):
cd sga-principal && ./mvnw -q -DskipTests test-compile && cd ..
cd microservicio-secretaria/backend && ./mvnw -q -DskipTests test-compile && cd ../..

# 3. Ejecutar el arnes
python experimentos/arnes_12_vectores.py
```

## Salida esperada (evidencia congelada)

```
  * Principal == Secretaria (Java == Java): 12/12 (100.0%)
  * Principal == Secretaria == Python:      8/12 (66.7%)
  * Vectores con alguna divergencia:        4/12
Aserciones OK: sin regresion respecto a la evidencia congelada.
```

Divergencias medidas (vectores 7, 10, 11, 12): flotantes de rango extremo
(`1.0E-7` vs `1e-07`, `1.0E21` vs `1e+21`), orden de claves suplementarias
(UTF-16 vs puntos de codigo) y NaN (`"NaN"` Java vs `NaN` Python).

## Sensibilidad (prueba de sabotaje)

- Cambiar una clave de `SECRET_KEYS` en cualquier `AuditHashService.java`,
  recompilar y correr: el arnes sale con codigo 1
  (`Java Principal != Java Secretaria`).
- Cambiar una clave de `SECRET_KEYS` en `hashing.py` y correr: sale con
  codigo 1 (`Java == Python cambio de 8 a ...`).
- Revertir siempre el sabotaje despues de verificar.
