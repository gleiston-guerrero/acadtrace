# GUÍA MAESTRA DE DEFENSA ORAL INDIVIDUAL — PROYECTO FIN DE CURSO (PFC E4)
## AcadTrace: Gestión Académica Distribuida con Auditoría Criptográfica Verificable
**Asignatura:** Aplicaciones Distribuidas (ISR-701) — Escuela de Ingeniería de Software — UTEQ  
**Docente Evaluador:** Prof. Ing. Gleiston C. Guerrero-Ulloa, Mgs.  
**Estudiante:** Ernesto Gregory Luna Mora (`Ernesto835 <elunam4@uteq.edu.ec>`)  
**Rol Oficial en el Informe:** *Responsable de Microservicio Secretaría y Calidad*  
**Equipo:** BCEL (Bedón, Castro, Emanuel, Luna)  
**Criterio Rúbrica Clave:** Criterio C10 — *Defensa oral y dominio individual* (Nivel 4: Sobresaliente)

---

## 🧭 1. ESTRATEGIA DE PRESENTACIÓN (CÓMO DAR LA DEFENSA)

### El Guion de Apertura (Tu discurso inicial de 2 a 3 minutos)
Si el docente te pide: *"Luna, presénteme su parte y su aporte al proyecto"*, responde con seguridad y este hilo conductor:

> *"Buenos días Ingeniero Guerrero. En el equipo BCEL mi rol formal ha sido **Responsable del Microservicio de Secretaría y Aseguramiento de la Calidad**.*
>
> *Mi trabajo se centró en resolver el desafío nuclear que le da nombre a AcadTrace: **¿cómo garantizar que los registros académicos y notas no puedan ser alterados ni repudiados, incluso si un atacante obtiene privilegios de superusuario en la base de datos relacional?***
>
> *Para responder a esto, implementé una arquitectura de **defensa en profundidad** con 4 componentes:*
> 1. *A nivel de persistencia: un disparador PostgreSQL **Append-Only** que bloquea físicamente cualquier `UPDATE` o `DELETE`.*
> 2. *A nivel de lógica: un sellado criptográfico mediante **HMAC-SHA256 con encadenamiento sucesivo de bloques ($H_i$)** y cifrado simétrico autenticado **AES-256-GCM** para proteger los datos personales de menores de edad (Criterio Ético C9).*
> 3. *A nivel de calidad (Criterio C7 y Listado 3): diseñé e integré la **suite de pruebas automatizadas completa** en `tests/contract`, `tests/integration` y `tests/e2e`, verificando contratos Protobuf v3, balanceo en HAProxy y reconciliación causal.*
> 4. *A nivel empírico (Criterios C2, C3, P4): certifiqué experimentalmente una tasa de falsos positivos **FPR = 0.00%** en 30 cadenas legítimas con un IC al 95% de $[0.0\%, 11.6\%]$, respaldado por un certificado de reproducibilidad SHA-256.*
>
> *A continuación, tengo en pantalla el código fuente y las pruebas en vivo para sustentar cada evidencia."*

---

## 📂 2. MAPA DE ARCHIVOS Y SNIPPETS EXACTOS DE CÓDIGO (DÓNDE SE UBICAN Y QUÉ HACEN)

---

### PIEZA 1: Disparador Append-Only en PostgreSQL (Inmutabilidad en Base de Datos)
* **Archivo:** `sga-principal/sql/V9__trigger_auditoria_append_only.sql` (Líneas 11 a 26)  
  *También referenciado en:* `docs/db/schema.sql`
* **Qué hace:** Actúa como la primera línea de defensa a nivel de persistencia. Intercepta cualquier intento de modificación o borrado de la bitácora transaccional y aborta la operación con una excepción SQL, garantizando que la tabla sea estrictamente de adición (*append-only*).

```sql
CREATE OR REPLACE FUNCTION sga_principal.prohibir_modificacion_auditoria()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Operacion rechazada: La tabla sga_principal.auditoria es una bitacora inmutable de solo adicion (append-only) protegida bajo estandares ISO/IEC 25010';
END;
$$ LANGUAGE plpgsql;

-- Asociar el disparador a eventos de UPDATE y DELETE por cada fila
CREATE TRIGGER tg_auditoria_append_only
BEFORE UPDATE OR DELETE ON sga_principal.auditoria
FOR EACH ROW
EXECUTE FUNCTION sga_principal.prohibir_modificacion_auditoria();
```

* **Punto de defensa ante el docente:** Si el profesor pregunta: *"¿Y si el DBA deshabilita el trigger?"*, explicas que por eso existe la **Pieza 2**: la auditoría criptográfica externa que detecta el fraude matemático.

---

### PIEZA 2: Firma HMAC-SHA256 en Tiempo Constante (Auditoría Criptográfica)
* **Archivo:** `microservicio-secretaria/backend/src/main/java/ec/uteq/sga/secretaria/infrastructure/security/HmacService.java` (Líneas 33 a 54)
* **Qué hace:** Genera la firma digital de 64 caracteres hexadecimales para cada evento. Incluye `MessageDigest.isEqual` que ejecuta la comparación en tiempo constante ($O(1)$) para prevenir ataques de canal lateral por análisis de tiempo (*Timing Attacks*).

```java
    public synchronized String firmar(String... campos) {
        String canonical = String.join("|", campos);
        byte[] digest = mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte b : digest) hex.append(String.format("%02x", b));
        return hex.toString();
    }

    /**
     * Verifica si el HMAC coincide con los campos computados.
     * Utiliza MessageDigest.isEqual en tiempo constante para prevenir ataques de temporizacion.
     */
    public boolean verificar(String hmacEsperado, String... campos) {
        if (hmacEsperado == null || hmacEsperado.isBlank()) {
            return false;
        }
        String calculado = firmar(campos);
        return java.security.MessageDigest.isEqual(
                hmacEsperado.getBytes(StandardCharsets.UTF_8),
                calculado.getBytes(StandardCharsets.UTF_8)
        );
    }
```

---

### PIEZA 3: Cifrado Simétrico AES-256-GCM para Datos de Menores (Ética y Privacidad C9)
* **Archivo:** `microservicio-secretaria/backend/src/main/java/ec/uteq/sga/secretaria/infrastructure/security/CryptoService.java` (Líneas 25 a 61)
* **Qué hace:** Cifra información sensible de estudiantes de escuela (nombres de representantes, números de cédula, direcciones). Utiliza AES en modo GCM (Galois/Counter Mode) con vector de inicialización (IV) de 12 bytes aleatorio por operación y tag de autenticación de 128 bits.

```java
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    public String encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] output = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, output, 0, iv.length);
            System.arraycopy(ciphertext, 0, output, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(output);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Error cifrando dato sensible", e);
        }
    }
```

* **Punto de defensa ante el docente:** Demuestra cumplimiento del principio de privacidad por diseño (*Privacy by Design*) y de la Ley Orgánica de Protección de Datos Personales (LOPDP).

---

### PIEZA 4: Pruebas Unitarias de Integridad y Detección de Ataque T1 (Java / JUnit 5)
* **Archivo:** `microservicio-secretaria/backend/src/test/java/ec/uteq/sga/secretaria/infrastructure/security/AuditoriaIntegridadTest.java` (Líneas 62 a 81)
* **Qué hace:** Prueba unitaria automatizada que simula la alteración maliciosa del ID de registro en base de datos (Ataque T1) y certifica que el validador rechaza la transacción inmediatamente.

```java
    @Test
    @DisplayName("Debe detectar inmediatamente manipulacion directa en id_registro (Ataque T1)")
    void testDeteccionManipulacion_RegistroIdAlterado() {
        String schema = "SECRETARIA";
        String traceId = UUID.randomUUID().toString();
        String username = "admin";
        String accion = "MODIFICAR_ESTUDIANTE";
        String tabla = "estudiante";
        String registroIdOriginal = "200";
        String registroIdAdulterado = "999"; // Atacante intenta redirigir el evento a otro registro
        String descripcion = "Actualizacion de datos";
        String resultado = "EXITO";
        String timestamp = "1725482000000";

        String firmaOriginal = hmacService.firmar(schema, traceId, username, accion, tabla, registroIdOriginal, descripcion, resultado, timestamp);

        // Al contrastar la firma frente a los datos adulterados en base de datos, DEBE ser rechazada
        boolean esValido = hmacService.verificar(firmaOriginal, schema, traceId, username, accion, tabla, registroIdAdulterado, descripcion, resultado, timestamp);
        assertFalse(esValido, "El sistema debe detectar la manipulacion del id de registro (integridad violada)");
    }
```

---

### PIEZA 5: Suite Automatizada de Integración y E2E (Python / Pytest - C7 Listado 3)
* **Archivos:**
  1. `tests/contract/test_contracts.py` (7 tests: sintaxis `proto3`, paridad de métodos RPC y OpenAPI 3.0)
  2. `tests/integration/test_cross_service_integration.py` (8 tests: HAProxy roundrobin/leastconn, multi-schema y `X-Trace-Id`)
  3. `tests/e2e/test_e2e_lifecycle.py` (6 tests: HTTP 401 gate, JWT HMAC-SHA256, detección T1/T2, y reconciliación causal)
* **Qué hace:** Responde directamente al reclamo de la evaluación provisional. Automatiza la verificación de extremo a extremo sin depender de navegadores manuales.

Fragmento de detección de borrado T2 en `tests/e2e/test_e2e_lifecycle.py` (Líneas 146 a 152):
```python
    # Ataque T2: Se elimina el evento 1 (el del medio de una cadena de 3)
    eventos_mutilados = [eventos[0], eventos[2]]

    valida, rule_desc, broken_id, elapsed_us = verificar_cadena_eventos(eventos_mutilados, "M2")
    assert not valida, "El borrado de eventos intermedios (T2) debe romper la cadena SHA-256"
    assert "BROKEN_HASH_CHAIN" in rule_desc or "HASH" in rule_desc
```

Fragmento de reconciliación de Relojes Vectoriales en `tests/e2e/test_e2e_lifecycle.py` (Líneas 160 a 172):
```python
    # Detección de concurrencia causal: A || B
    es_concurrente = not (
        all(x >= y for x, y in zip(v_docA, v_docB)) or 
        all(x <= y for x, y in zip(v_docA, v_docB))
    )
    assert es_concurrente, "Debe identificarse conflicto causal concurrente (A || B)"

    # Reconciliación determinista M3: merge componente a componente
    v_reconciliado = [max(x, y) for x, y in zip(v_docA, v_docB)]
    nota_reconciliada = max(nota_A, nota_B)
```

---

### PIEZA 6: Certificación de Cero Falsos Positivos y Reproducibilidad (C2, C4, P4)
* **Archivos:**
  * `experimentos/resultados/falsos_positivos.csv`
  * `experimentos/resultados/REPRODUCIBILIDAD.txt`
  * `docs/TRAZABILIDAD.md`
* **Qué hace:** Demuestra que el algoritmo criptográfico no arroja falsas alarmas ante cadenas legítimas. Evaluó 30 cadenas de 50 eventos cada una, certificando:
  $$\text{FPR} = \frac{\text{FP}}{\text{FP} + \text{TN}} = \frac{0}{0 + 30} = 0.00\%$$
  Con un intervalo de confianza binomial exacto (Clopper-Pearson) al 95% de $[0.0\%, 11.6\%]$.

---

## 🥊 3. PREGUNTAS TRAMPA DEL PROFESOR Y RESPUESTAS MODELO

### ❓ P1: *"¿Por qué usan HMAC-SHA256 con clave simétrica en vez de firmas asimétricas RSA o ECDSA?"*
> **Tu respuesta:**
> *"Por tres razones técnicas fundamentales evaluadas en la arquitectura:
> 1. **Rendimiento y Throughput:** En el escenario de estrés de Locust procesamos 106,735 transacciones. RSA/ECDSA requiere operaciones de exponenciación modular sobre curvas elípticas que incrementan la latencia en un factor de $10\times$ a $50\times$. HMAC opera con funciones hash simétricas a nivel de microsegundos ($0.15\text{ ms}$).
> 2. **Contexto de Confianza Inter-servicios:** En una arquitectura de microservicios dentro de una red privada (VPC en AWS), los servicios comparten secretos institucionales rotados periódicamente mediante variables de entorno en Kubernetes/Docker Compose.
> 3. **Encadenamiento Sucesivo:** La seguridad no depende únicamente de la firma individual, sino de la transitividad de los bloques ($H_i = f(H_{i-1})$), lo que hace computacionalmente inviable reconstruir la cadena hacia atrás."*

---

### ❓ P2: *"¿Dónde está la trazabilidad entre las revisiones de pares y los issues de GitHub? (Criterio C8)"*
> **Tu respuesta:**
> *"Está formalmente documentada en la **Sección 8 del informe LaTeX** (`Informe-E4_BCEL/TA-PFC-E4_BCEL.tex`) y sincronizada en el repositorio.
> Mapeamos cada issue cerrado con su respectivo Pull Request y revisor:
> - **Issue #38 y #41:** Auditoría append-only e inmutabilidad (Revisado por Castro y Luna).
> - **Issue #44:** Cobertura de pruebas unitarias JaCoCo (Revisado por Bedón).
> - **Issue #47:** Integración gRPC entre SGA Principal y Docente (Revisado por Emanuel).
> - **Issue #50 y #53:** Pruebas de estrés Locust y observabilidad en Grafana (Revisado por el equipo).
> - **Issue #57:** Certificación de reproducibilidad experimental C4.
> Además, los 4 integrantes del equipo BCEL nos co-evaluamos con rúbrica ciega obteniendo consenso unánime."*

---

### ❓ P3: *"Si la cobertura de JaCoCo en SGA Principal es tan solo del 0.51%, ¿cómo afirman que el sistema tiene calidad?"*
> **Tu respuesta:**
> *"Esa métrica refleja nuestro compromiso con la **transparencia y el sinceramiento empírico (Criterio C3 y C6)** exigidos en la Guía de Consolidación:
> - En entregas anteriores se presentaban estimaciones teóricas infladas al 85%. En esta Entrega 4 auditamos los reportes reales de JaCoCo (`docs/cobertura/`).
> - `sga-principal` tiene bajo porcentaje porque el 90% de sus clases son entidades JPA, configuraciones y stubs gRPC generados por `protoc` que no deben ser probados unitariamente.
> - En **Secretaría** (mi microservicio) alcanzamos el **34.52% global y 100% en las clases críticas de negocio** (`AuditoriaIntegridadTest`, `CryptoServiceTest`, `JwtServiceTest`).
> - Y para compensar el aislamiento de los procesos Java, construimos la suite externa en Python (`tests/contract`, `tests/integration`, `tests/e2e`) con 21 pruebas que validan los contratos, la integración en red y los flujos E2E."*

---

### ❓ P4: *"¿Qué ocurre si dos docentes califican al mismo estudiante al mismo tiempo mientras no tienen internet en la app móvil?"*
> **Tu respuesta:**
> *"Eso corresponde a la sincronización offline evaluada en el Experimento 3.
> Cada dispositivo mantiene un **Reloj Vectorial** $\vec{V} = [v_{\text{doc1}}, v_{\text{doc2}}, v_{\text{servidor}}]$. 
> Cuando ambos suben las notas:
> 1. El servidor compara los vectores. Si ni $V_1 \le V_2$ ni $V_2 \le V_1$, se detecta una **bifurcación concurrente ($A \parallel B$)**.
> 2. El sistema aplica la regla de reconciliación determinista M3: combina los relojes tomando el máximo componente a componente ($\max(v_{1,k}, v_{2,k})$) y preserva la versión con mayor sello causal o marca de tiempo determinista, registrando la discrepancia en la bitácora criptográfica."*

---

## 💻 4. TERMINAL CHEAT SHEET (COMANDOS PARA CORRER EN VIVO)

Abre la terminal de PowerShell en `C:\Users\DEYNER\acadtrace` y ten listos estos 4 comandos:

### 1. Ejecutar las 21 pruebas automatizadas (C7 / Listado 3):
```powershell
python -m pytest tests/contract tests/integration tests/e2e -v
```
*(Demuestra que tardan menos de 0.40 segundos y todas pasan en verde).*

### 2. Ejecutar el banco experimental y cálculo de falsos positivos (C2 / C4):
```powershell
python experimentos/run_experimentos.py
```
*(Genera en vivo `falsos_positivos.csv` con FPR = 0.00% y lee las estadísticas de Locust).*

### 3. Mostrar el certificado de reproducibilidad criptográfica (C4):
```powershell
cat experimentos/resultados/REPRODUCIBILIDAD.txt
```
*(Demuestra los hashes SHA-256 inmutables de los CSVs generados).*

### 4. Mostrar el último commit en main (tu Pull Request #60 fusionado):
```powershell
git log -n 3 --pretty=format:"%h | %an | %s"
```
*(Verás en la cima `dcbf3384 | Ernesto835 | Merge pull request #60 from LEO23as/Ernesto-Luna`).*

---
*¡Mucho éxito en la defensa, Ernesto! Tienes el código, las pruebas, los números empíricos y la justificación teórica exacta para alcanzar el 10/10 en el Criterio C10.*
