# Paquete móvil Release — AcadTrace Representante

Este directorio funciona como índice técnico y guía de verificación independiente del paquete móvil de producción. Los binarios ejecutables de Release no se versionan directamente en el árbol de Git para mantener la higiene del repositorio; son construidos, firmados criptográficamente, verificados y publicados automáticamente por el flujo de integración continua (CI) de GitHub Actions.

## Paquete de producción oficial

- **Aplicación:** AcadTrace Representante
- **Package ID:** `ec.edu.uteq.sga.representante`
- **Versión oficial:** `1.0.1` (`versionCode: 2`)
- **Variante de compilación:** `release`
- **Tag oficial de Release:** `v1.0.1`
- **Artefactos publicados en GitHub Release:**
  - `app-release.apk` (Paquete instalable para dispositivos Android)
  - `app-release.aab` (Android App Bundle para distribución optimizada)
  - `SHA256SUMS.txt` (Manifiesto de sumas de comprobación criptográfica SHA-256)

El tag de publicación en el repositorio coincide exactamente con el valor `versionName = "1.0.1"` configurado en `app-movil-docente/app/build.gradle.kts`.

## Certificado y firma de Release

La firma del paquete de entrega utiliza una clave privada de producción propia, no una clave de depuración (`debug`). El almacén de claves (keystore) y sus credenciales se inyectan mediante GitHub Actions Secrets en el entorno de ejecución efímero del flujo de CI.

### Especificaciones del certificado de firma

- **Tipo de certificado:** X.509 v3 autofirmado
- **Propietario (Subject DN):** `CN=Keyla Bedón, OU=BCEL, O=UTEQ`
- **Emisor (Issuer DN):** `CN=Keyla Bedón, OU=BCEL, O=UTEQ`
- **Vigencia:** Desde el 2026-09-16 hasta el año 2054
- **Algoritmo de clave:** RSA de 2048 bits
- **Algoritmo de firma:** SHA256withRSA
- **Huella digital SHA-256 del certificado:** Inicia en `F7:7C:A1:2B` y finaliza en `A1:79` (huella completa: `F7:7C:A1:2B:...:A1:79`)
- **Tipo de keystore:** PKCS12 (`acadtrace-release.jks`)
- **Validación del keystore en CI:** Tamaño exacto de 2782 bytes y SHA-256 `C076C5BDB2B017E8F2FC8F3AE5D1228B7B01331B032397EBBC2058DAC4DF9E9A`.

La configuración de Gradle en `app-movil-docente/app/build.gradle.kts` lanza expresamente una excepción (`GradleException`) si se intenta generar la variante `release` sin un keystore válido, impidiendo la emisión de binarios de producción sin firmar.

## Verificación independiente por terceros

Cualquier evaluador o usuario puede verificar la integridad, autenticidad y titularidad de los binarios descargados desde el GitHub Release `v1.0.1` mediante los siguientes comandos estándar.

### 1. Verificación de sumas de integridad SHA-256

Con los archivos `app-release.apk`, `app-release.aab` y `SHA256SUMS.txt` en el mismo directorio:

```bash
sha256sum -c SHA256SUMS.txt
```

La salida esperada debe certificar `app-release.apk: OK` y `app-release.aab: OK`.

### 2. Verificación de firma del APK con `apksigner`

Utilizando la herramienta oficial de Android SDK Build-Tools:

```bash
apksigner verify --verbose --print-certs app-release.apk
```

Salida esperada:
- `Verifies: true`
- `Verified using v1 scheme (JAR signing): true`
- `Verified using v2 scheme (APK Signature Scheme v2): true`
- `Signer #1 certificate DN: CN=Keyla Bedón, OU=BCEL, O=UTEQ`
- `Signer #1 certificate SHA-256 digest: F7:7C:A1:2B:...:A1:79`

Esto demuestra fehacientemente que el APK está firmado válidamente, no ha sido alterado y fue emitido por el titular institucional correspondiente.

### 3. Verificación de firma del AAB con `jarsigner`

Utilizando la herramienta estándar de JDK:

```bash
jarsigner -verify -verbose -certs app-release.aab
```

La salida esperada debe concluir con `jar verified` y listar en los bloques de firma del manifiesto (`META-INF/ACADTRAC.RSA`) el certificado correspondiente a `CN=Keyla Bedón, OU=BCEL, O=UTEQ`.

## Automatización y trazabilidad en CI

El proceso de construcción y entrega está completamente automatizado en el job `build-mobile-apk` (`6. Compilación de Paquete APK Android`) del archivo `.github/workflows/ci-cd.yml`:

1. Restaura y valida el keystore PKCS12 mediante suma SHA-256 y comando `keytool -list`.
2. Restaura la configuración de Firebase `google-services.json`.
3. Ejecuta `./gradlew clean assembleRelease bundleRelease --no-daemon`.
4. Ejecuta `apksigner verify --verbose` y `jarsigner -verify` inmediatamente después de la compilación.
5. Genera el manifiesto `SHA256SUMS.txt` y publica los artefactos en el GitHub Release mediante `gh release upload --clobber` para garantizar idempotencia.
