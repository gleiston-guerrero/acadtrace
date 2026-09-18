# Paquete móvil Release — AcadTrace Representante

Este directorio funciona como índice del paquete móvil de producción. Los binarios de Release no se versionan directamente en Git: son generados, firmados, verificados y publicados por GitHub Actions.

## Paquete de producción

- **Aplicación:** AcadTrace Representante
- **Package ID:** `ec.edu.uteq.sga.representante`
- **Versión preparada:** `1.0.1` (`versionCode: 2`)
- **Variante:** `release`
- **APK:** `app-release.apk`
- **Android App Bundle:** `app-release.aab`
- **Sumas:** `SHA256SUMS.txt`

El tag de publicación debe coincidir con `versionName`. Para esta versión corresponde `v1.0.1`.

## Firma de Release

El keystore y sus contraseñas no se almacenan en Git. El flujo `build-mobile-apk` recibe la configuración de firma mediante GitHub Actions Secrets y falla si la configuración requerida no está disponible.

Antes de publicar, el flujo valida el keystore y genera los paquetes mediante `assembleRelease` y `bundleRelease`.

La firma se verifica sobre los artefactos finales mediante:

- `apksigner verify --verbose` para `app-release.apk`.
- `jarsigner -verify` para `app-release.aab`.

Después de verificar las firmas, el mismo job calcula SHA-256 sobre ambos artefactos y genera `SHA256SUMS.txt`.

## Publicación y trazabilidad

Los binarios publicados en GitHub Release son los mismos archivos generados y verificados dentro del job `build-mobile-apk`; no existe una copia manual intermedia dentro de este directorio.

La publicación se ejecuta únicamente para un tag de Release con formato `vN.N.N`. Antes de publicar, CI comprueba que la versión del tag coincida con `versionName` de Android.

El GitHub Release conserva:

- `app-release.apk`
- `app-release.aab`
- `SHA256SUMS.txt`

El tag identifica el commit exacto utilizado por GitHub Actions para construir los artefactos.

Los valores SHA-256 específicos de `v1.0.1` deben obtenerse de `SHA256SUMS.txt` generado por CI; no se documentan anticipadamente en este índice.
