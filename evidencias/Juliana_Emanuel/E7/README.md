# Evidencias — Criterio E7: Certificado de Reproducibilidad

**Responsable:** Juliana  
**Criterio:** E7 – Certificado de reproducibilidad  
**Rama:** `Juliana-Emanuel`  
**Repositorio:** `acadtrace`  

---

## 1. Objetivo de la Evidencia

Demostrar la verificación criptográfica estricta de los seis artefactos experimentales oficiales del proyecto AcadTrace mediante la ejecución de `sha256sum -c` a través del script `reproducibilidad.sh`, y validar la suite completa de pruebas unitarias y de integración del verificador de reproducibilidad (`test_reproducibilidad.py`) con `pytest`.

---

## 2. Lista de Capturas

1. `e7_sha256_verificacion.png`
2. `e7_pruebas_pytest.png`

---

## 3. Descripción de las Capturas

### `e7_sha256_verificacion.png`
Muestra la ejecución en la terminal de `experimentos/reproducibilidad.sh` invocada a través de Git Bash, donde se verifica el certificado oficial `REPRODUCIBILIDAD.txt` mediante `sha256sum -c`. Se observa la comprobación exitosa con estado `OK` para los seis artefactos oficiales:
- `deteccion.csv: OK`
- `manipulaciones.csv: OK`
- `exp1_concurrencia.csv: OK`
- `exp3_reconciliacion.csv: OK`
- `iso25010.csv: OK`
- `boxplot_latencia.png: OK`

Finalizando con el mensaje de confirmación:
`OK: Los 6 artefactos oficiales coinciden con el certificado de reproducibilidad.`

### `e7_pruebas_pytest.png`
Muestra la ejecución en terminal de la suite de pruebas del verificador de reproducibilidad mediante el comando:
`py -m pytest experimentos/test_reproducibilidad.py -v`

Se observa la ejecución de las 9 pruebas de integridad y modos (`CertificateTests` y `ModeTests`), reportando un resultado final de `9 passed in 1.35s` con estado 100% exitoso.