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
}
