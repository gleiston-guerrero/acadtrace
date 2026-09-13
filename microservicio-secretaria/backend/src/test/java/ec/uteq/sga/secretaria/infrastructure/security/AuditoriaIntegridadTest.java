package ec.uteq.sga.secretaria.infrastructure.security;

import ec.uteq.sga.secretaria.application.service.AuditoriaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Pruebas automatizadas de integridad criptografica y deteccion de manipulacion
 * en la bitacora de auditoria (Criterio RRL / Seccion 3 Guia de Consolidacion BCEL).
 */
@ExtendWith(MockitoExtension.class)
class AuditoriaIntegridadTest {

    private static final String SECRET = "test-only-jwt-secret-key-32-chars-long-minimum";
    private HmacService hmacService;

    @Mock
    private NamedParameterJdbcTemplate jdbc;

    private AuditoriaService auditoriaService;

    @BeforeEach
    void setUp() {
        hmacService = new HmacService(SECRET);
        auditoriaService = new AuditoriaService(jdbc, hmacService);
    }

    @Test
    @DisplayName("Debe verificar exitosamente un registro con firma HMAC-SHA256 integra")
    void testVerificacionExitosa_RegistroIntegro() {
        String schema = "SECRETARIA";
        String traceId = UUID.randomUUID().toString();
        String username = "secretaria1";
        String accion = "MATRICULAR_ESTUDIANTE";
        String tabla = "matricula";
        String registroId = "105";
        String descripcion = "Matricula aprobada para periodo 2026";
        String resultado = "EXITO";
        String timestamp = "1725482000000";

        String firma = hmacService.firmar(schema, traceId, username, accion, tabla, registroId, descripcion, resultado, timestamp);

        assertNotNull(firma);
        assertEquals(64, firma.length(), "HMAC-SHA256 debe tener longitud exacta de 64 caracteres hexadecimales");
        assertTrue(hmacService.verificar(firma, schema, traceId, username, accion, tabla, registroId, descripcion, resultado, timestamp));
    }

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

    @Test
    @DisplayName("Debe detectar alteracion en la accion ejecutada o el resultado")
    void testDeteccionManipulacion_AccionOResultadoAlterado() {
        String schema = "SECRETARIA";
        String traceId = UUID.randomUUID().toString();
        String username = "docente1";
        String accion = "ELIMINAR_ESTUDIANTE";
        String tabla = "estudiante";
        String registroId = "300";
        String descripcion = "Eliminacion preventiva";
        String resultado = "EXITO";
        String timestamp = "1725482000000";

        String firma = hmacService.firmar(schema, traceId, username, accion, tabla, registroId, descripcion, resultado, timestamp);

        // Atacante modifica resultado de EXITO a FALLO para encubrir una accion
        assertFalse(hmacService.verificar(firma, schema, traceId, username, accion, tabla, registroId, descripcion, "FALLO", timestamp));

        // Atacante disfraza una ELIMINACION como una CONSULTA
        assertFalse(hmacService.verificar(firma, schema, traceId, username, "CONSULTAR", tabla, registroId, descripcion, resultado, timestamp));
    }

    @Test
    @DisplayName("Debe rechazar firmas forjadas con otra clave secreta (Resistencia a falsificacion)")
    void testRechazoFirmaForjada() {
        HmacService forjador = new HmacService("clave-falsa-atacante-12345678901234567890");
        String[] campos = {"SECRETARIA", UUID.randomUUID().toString(), "hacker", "CREAR", "usuario", "1", "Inyeccion", "EXITO", "1725482000000"};

        String firmaForjada = forjador.firmar(campos);

        // Nuestro validador oficial con el secreto institucional debe rechazarla
        assertFalse(hmacService.verificar(firmaForjada, campos), "Firma generada con clave desconocida debe ser rechazada");
    }

    @Test
    @DisplayName("AuditoriaService debe generar los parametros SQL correctos con firma HMAC")
    void testAuditoriaService_RegistroCorrecto() {
        auditoriaService.registrarCrud("CREAR", "estudiante", 450L, "Nuevo estudiante registrado");

        ArgumentCaptor<MapSqlParameterSource> captor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc, times(1)).update(anyString(), captor.capture());

        MapSqlParameterSource params = captor.getValue();
        assertEquals("CREAR", params.getValue("accion"));
        assertEquals("estudiante", params.getValue("tablaAfectada"));
        assertEquals(450L, params.getValue("registroId"));
        assertEquals("EXITO", params.getValue("resultado"));
        assertNotNull(params.getValue("hmac"), "Debe calcular y asignar la firma HMAC");
        assertNotNull(params.getValue("traceId"), "Debe asignar un UUID de trace");
    }

    @Test
    @DisplayName("Fallo en la base de datos de auditoria no debe propagar excepcion al flujo principal")
    void testAuditoriaService_ResilienciaAnteFalloBD() {
        doThrow(new RuntimeException("Fallo de conexion simulado en Postgres"))
                .when(jdbc).update(anyString(), any(MapSqlParameterSource.class));

        // No debe lanzar excepcion hacia el llamador (degradacion elegante)
        assertDoesNotThrow(() -> auditoriaService.registrarCrud("CREAR", "estudiante", 500L, "Prueba resiliencia"));
    }
}
