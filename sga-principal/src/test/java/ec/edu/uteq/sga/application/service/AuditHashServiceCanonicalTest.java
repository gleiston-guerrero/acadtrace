package ec.edu.uteq.sga.application.service;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AuditHashServiceCanonicalTest {

    @Test
    void contratoCanonicoV1CoincideConDocente() {
        AuditHashService service = new AuditHashService();

        Map<String, Object> payload = new TreeMap<>();
        payload.put("descripcion", "Ajuste");
        payload.put("resultado", "EXITO");
        payload.put("schema_origen", "PRINCIPAL");
        payload.put(
                "trace_id",
                "11111111-1111-1111-1111-111111111111"
        );

        Map<String, Long> vector = new TreeMap<>();
        vector.put("docente", 2L);
        vector.put("principal", 5L);
        vector.put("secretaria", 1L);

        Map<String, Object> contenido = service.contenidoEvento(
                "AUDITORIA",
                "calificacion",
                "456",
                "EDITAR",
                "123",
                "2026-09-13T08:30:00Z",
                payload,
                "m3",
                42L,
                vector,
                "APLICADO"
        );

        String esperado =
                "{\"actor_id\":\"123\","
                + "\"entidad\":\"calificacion\","
                + "\"entidad_id\":\"456\","
                + "\"estado_reconciliacion\":\"APLICADO\","
                + "\"modo\":\"m3\","
                + "\"operacion\":\"EDITAR\","
                + "\"payload\":{\"descripcion\":\"Ajuste\","
                + "\"resultado\":\"EXITO\","
                + "\"schema_origen\":\"PRINCIPAL\","
                + "\"trace_id\":\"11111111-1111-1111-1111-111111111111\"},"
                + "\"reloj_lamport\":42,"
                + "\"reloj_vectorial\":{\"docente\":2,"
                + "\"principal\":5,\"secretaria\":1},"
                + "\"timestamp\":\"2026-09-13T08:30:00Z\","
                + "\"tipo_evento\":\"AUDITORIA\"}";

        assertEquals(esperado, service.jsonCanonico(contenido));

        assertEquals(
                "4d440be187e77bc07dd34422dea07ccdc94f07c6deb4318b3c10196de81a88b6",
                service.calcularHash(
                        AuditHashService.GENESIS_HASH,
                        contenido
                )
        );
    }

    @Test
    void materialSensibleNoFormaParteDelCanonico() {
        AuditHashService service = new AuditHashService();

        Map<String, Object> payload = new TreeMap<>();
        payload.put("dato", "permitido");
        payload.put("password", "NO-DEBE-APARECER");
        payload.put("token", "NO-DEBE-APARECER");

        Map<String, Object> contenido = service.contenidoEvento(
                "AUDITORIA",
                "usuario",
                "1",
                "EDITAR",
                "actor",
                "2026-09-13T08:30:00Z",
                payload,
                "m2",
                1L,
                null,
                "NO_APLICA"
        );

        String canonico = service.jsonCanonico(contenido);

        assertFalse(canonico.contains("password"));
        assertFalse(canonico.contains("token"));
        assertFalse(canonico.contains("NO-DEBE-APARECER"));
    }

    @Test
    void materialSensibleExcluyeContrasenaConTilde() {
        AuditHashService service = new AuditHashService();
        Map<String, Object> payload = new TreeMap<>();
        payload.put("contraseña", "secreto123");
        payload.put("contrase\u00f1a", "secreto456");
        payload.put("campo_valido", "ok");

        Map<String, Object> contenido = service.contenidoEvento(
                "AUDITORIA", "estudiante", "10", "LOGIN", "admin",
                "2026-09-18T10:00:00Z", payload, "m1", 1L, null, "APLICADO"
        );

        String canonico = service.jsonCanonico(contenido);
        assertFalse(canonico.contains("secreto123"));
        assertFalse(canonico.contains("secreto456"));
        assertFalse(canonico.contains("contraseña"));
    }

    @Test
    void normalizarSoportaFechasSinFailOpen() {
        AuditHashService service = new AuditHashService();
        Map<String, Object> payload = new TreeMap<>();
        payload.put("fecha_nacimiento", java.time.LocalDate.of(2005, 5, 20));
        payload.put("fecha_registro", java.time.LocalDateTime.of(2026, 9, 18, 14, 30, 0));

        Map<String, Object> contenido = service.contenidoEvento(
                "AUDITORIA", "estudiante", "10", "CREAR", "admin",
                "2026-09-18T10:00:00Z", payload, "m1", 1L, null, "APLICADO"
        );

        String canonico = service.jsonCanonico(contenido);
        org.junit.jupiter.api.Assertions.assertTrue(canonico.contains("\"fecha_nacimiento\":\"2005-05-20\""));
        org.junit.jupiter.api.Assertions.assertTrue(canonico.contains("\"fecha_registro\":\"2026-09-18T14:30"));
    }
}
