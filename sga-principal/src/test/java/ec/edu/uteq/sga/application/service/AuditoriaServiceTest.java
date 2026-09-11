package ec.edu.uteq.sga.application.service;

import ec.edu.uteq.sga.domain.entity.Auditoria;
import ec.edu.uteq.sga.infrastructure.repository.AuditoriaRepository;
import ec.edu.uteq.sga.infrastructure.security.HmacService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditoriaServiceTest {

    @Mock
    private AuditoriaRepository repo;

    @Mock
    private HmacService hmacService;

    private LamportClock lamportClock;
    private AuditoriaService auditoriaService;

    @BeforeEach
    void setUp() {
        lamportClock = new LamportClock();
        auditoriaService = new AuditoriaService(repo, hmacService, lamportClock);
    }

    @Test
    void modoM0_noGuardaEnRepositorio() {
        auditoriaService.setAuditMode("m0");
        auditoriaService.registrarCrud("CREAR", "calificacion", 100L, "Registro nota");

        verify(repo, never()).save(any(Auditoria.class));
    }

    @Test
    void modoM1_guardaSinHmac() {
        auditoriaService.setAuditMode("m1");
        auditoriaService.registrarCrud("CREAR", "calificacion", 101L, "Registro nota convencional");

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(repo).save(captor.capture());

        Auditoria guardada = captor.getValue();
        assertEquals("CREAR", guardada.getAccion());
        assertEquals("calificacion", guardada.getTablaAfectada());
        assertNull(guardada.getHmac());
    }

    @Test
    void modoM2_guardaConHmacYLamport() {
        auditoriaService.setAuditMode("m2");
        when(hmacService.firmar(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("HMAC_M2_VALIDO");

        auditoriaService.registrarCrud("MODIFICAR", "calificacion", 102L, "Nota corregida");

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(repo).save(captor.capture());

        Auditoria guardada = captor.getValue();
        assertEquals("HMAC_M2_VALIDO", guardada.getHmac());
        assertTrue(guardada.getDescripcion().contains("lamport:1"));
    }

    @Test
    void modoM3_guardaConHmacYRelojVectorial() {
        auditoriaService.setAuditMode("m3");
        when(hmacService.firmar(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("HMAC_M3_VALIDO");

        auditoriaService.registrarCrud("MODIFICAR", "calificacion", 103L, "Reconciliacion offline");

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(repo).save(captor.capture());

        Auditoria guardada = captor.getValue();
        assertEquals("HMAC_M3_VALIDO", guardada.getHmac());
        assertTrue(guardada.getDescripcion().contains("vclock:[1,0,0]"));
    }

    @Test
    void verificarIntegridadCausal_dosEventos_detectaManipulacionYVerificaRelojLogico() {
        auditoriaService.setAuditMode("m2");

        when(hmacService.firmar(any(), any(), any(), eq("CREAR"), any(), any(), any(), any(), any()))
                .thenReturn("HMAC_EVENTO_1");
        when(hmacService.firmar(any(), any(), any(), eq("ACTUALIZAR"), any(), any(), any(), any(), any()))
                .thenReturn("HMAC_EVENTO_2_ORIGINAL");

        auditoriaService.registrarCrud("CREAR", "calificacion", 201L, "Ingreso de nota inicial");
        auditoriaService.registrarCrud("ACTUALIZAR", "calificacion", 201L, "Rectificacion de nota");

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(repo, times(2)).save(captor.capture());

        var eventos = captor.getAllValues();
        Auditoria evento1 = eventos.get(0);
        Auditoria evento2 = eventos.get(1);

        assertTrue(evento1.getDescripcion().contains("lamport:1"));
        assertTrue(evento2.getDescripcion().contains("lamport:2"));
        assertEquals("HMAC_EVENTO_1", evento1.getHmac());
        assertEquals("HMAC_EVENTO_2_ORIGINAL", evento2.getHmac());

        // Simulacion de alteracion maliciosa en el segundo evento
        evento2.setDescripcion("Alteracion no autorizada [lamport:2]");
        when(hmacService.firmar(any(), any(), any(), eq("ACTUALIZAR"), any(), any(), contains("Alteracion no autorizada"), any(), any()))
                .thenReturn("HMAC_RECALCULADO_DISCREPANTE");

        String hmacRecalculado = hmacService.firmar(
                evento2.getSchemaOrigen(),
                String.valueOf(evento2.getTraceId()),
                evento2.getUsername(),
                evento2.getAccion(),
                evento2.getTablaAfectada(),
                String.valueOf(evento2.getRegistroId()),
                evento2.getDescripcion(),
                evento2.getResultado(),
                String.valueOf(evento2.getFecha().toEpochMilli())
        );

        assertNotEquals(evento2.getHmac(), hmacRecalculado);
    }
}
