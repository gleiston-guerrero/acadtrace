# E46 — Registro de rotación

Fecha de revisión: 2026-09-23.

**Estado general: PARCIAL.**

JWT_SECRET fue rotado y verificado tanto en GitHub Actions como en el entorno operativo de AWS EC2. Los hallazgos oficiales restantes de Gitleaks corresponden a fixtures de pruebas y valores sintéticos utilizados exclusivamente en CI; no constituyen credenciales de producción. Las categorías procedentes de la auditoría ampliada se mantienen como potenciales hasta que exista evidencia suficiente para determinar si correspondieron a secretos reales.

| Sistema | Tipo | Exposición histórica | Acción | Fecha | Evidencia | Estado |
|---|---|---|---|---|---|---|
| PostgreSQL | Contraseña de acceso | Variables en archivos de entorno históricos; revisar también propiedades | Juliana: identificar cuentas, cambiar contraseña en PostgreSQL/proveedor, actualizar secretos de cada servicio y verificar rechazo de la anterior y acceso con la nueva | Pendiente | Inventario histórico; falta evidencia externa | PENDIENTE |
| Principal / Soporte | JWT_SECRET / firma | Configuración histórica detectada | Se rotó la clave en GitHub Actions y AWS EC2 y se recrearon los servicios consumidores | 2026-09-23 | Workflow manual #960; commit 3ab0a368; servicios operativos después de la rotación | ROTADO Y VERIFICADO |
| Servicios internos | Token gRPC | Categoría por contrastar con configuración y despliegues históricos | Juliana: identificar tokens expuestos, rotarlos coordinadamente en clientes y servidores; acreditar rechazo del anterior | Pendiente | Sin evidencia de rotación | PENDIENTE |
| Correo | SMTP | Propiedades históricas de Principal | Juliana: revocar contraseña de aplicación en el proveedor, emitir otra, actualizar el secreto externo y probar envío | Pendiente | Hallazgos e46-properties-literal-secret; falta evidencia externa | PENDIENTE |
| API de administración | Usuario/contraseña operativa | Cuatro ocurrencias en script histórico | Juliana: cambiar contraseña de la cuenta y revocar sus sesiones/tokens; probar mediante mecanismo autorizado | Pendiente | Hallazgos e46-operational-python-password | PENDIENTE |
| Usuarios del sistema | Hashes de contraseñas | Dumps y baselines SQL históricos | Juliana: identificar cuentas reales afectadas, forzar cambio de contraseña e invalidar sesiones; tratar los dumps como datos sensibles | Pendiente | 65 ocurrencias de hashes; no son 65 cuentas únicas | PENDIENTE |
| APIs externas / IA | API keys | Inventario y proveedor pendientes de contraste | Juliana: identificar claves reales expuestas y revocarlas en el proveedor; actualizar gestor de secretos | Pendiente | No se acredita una clave externa concreta vigente | PENDIENTE |
| Firebase | Cuenta de servicio / clave privada | Confirmación de exposición pendiente; referencias a Firebase no prueban exposición de clave privada | Juliana: revisar claves de cuentas de servicio y auditoría del proveedor; revocar las expuestas, si existen | Pendiente | Sin evidencia externa | PENDIENTE |
| Secretaría / otros | Claves de cifrado | Alcance histórico y datos cifrados pendientes de contraste | Juliana: inventariar claves, respaldar recuperación, migrar datos y retirar la clave anterior con prueba de lectura | Pendiente | Sin evidencia externa; no cambiar a ciegas | PENDIENTE |
| CI y pruebas | Once coincidencias originales de Gitleaks | SecurityTest.java, test_sga_navegador.py y ci-cd.yml | Se verificó su procedencia como constantes JWT de pruebas, tokens de escenarios de navegador y valores sintéticos de CI | 2026-09-23 | gitleaks-history-baseline.json, e46_inventario_historico.md y código clasificado | FIXTURE/TEST — NO REQUIERE ROTACIÓN |

Para cambiar un estado a ROTADO o REVOCADO: adjuntar fecha, identificador no sensible y evidencia del proveedor o prueba sanitizada del rechazo del valor anterior y funcionamiento del nuevo. No adjuntar valores, hashes derivados de secretos ni capturas sin redactar. NO ERA CREDENCIAL REAL requiere prueba de procedencia; la apariencia de fixture no basta.
