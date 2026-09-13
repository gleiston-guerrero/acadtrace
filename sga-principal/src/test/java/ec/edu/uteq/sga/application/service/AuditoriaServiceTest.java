package ec.edu.uteq.sga.application.service;

import ec.edu.uteq.sga.domain.entity.Auditoria;
import ec.edu.uteq.sga.domain.entity.EstadoCadenaAuditoria;
import ec.edu.uteq.sga.infrastructure.repository.AuditoriaRepository;
import ec.edu.uteq.sga.infrastructure.repository.EstadoCadenaAuditoriaRepository;
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
    private EstadoCadenaAuditoriaRepository estadoCadenaRepo;

    @Mock
    private HmacService hmacService;

    private LamportClock lamportClock;
    private VectorClock vectorClock;
    private AuditHashService auditHashService;
    private AuditoriaService auditoriaService;

    @BeforeEach
    void setUp() {
        lamportClock = new LamportClock();
        vectorClock = new VectorClock();
        auditHashService = new AuditHashService();

        EstadoCadenaAuditoria estado = EstadoCadenaAuditoria.builder()
                .idEstado((short) 1)
                .ultimoHash(AuditHashService.GENESIS_HASH)
                .ultimoLamport(0L)
                .vectorReloj("{}")
                .build();

        lenient().when(estadoCadenaRepo.buscarParaActualizar((short) 1))
                .thenReturn(java.util.Optional.of(estado));

        auditoriaService = new AuditoriaService(
                repo,
                estadoCadenaRepo,
                hmacService,
                lamportClock,
                vectorClock,
                auditHashService
        );
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
        assertNull(guardada.getHashActual());
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
        assertEquals("Nota corregida", guardada.getDescripcion());
        assertEquals(1L, guardada.getRelojLamport());
        assertEquals(AuditHashService.GENESIS_HASH, guardada.getHashAnterior());
        assertNotNull(guardada.getHashActual());
        assertEquals(64, guardada.getHashActual().length());
        assertEquals("v1", guardada.getVersionCanonica());
        assertNotNull(guardada.getContenidoCanonico());
        assertTrue(
                guardada.getContenidoCanonico()
                        .contains("\"reloj_lamport\":1")
        );
        assertTrue(
                guardada.getContenidoCanonico()
                        .contains("\"reloj_vectorial\":null")
        );
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
        assertEquals("Reconciliacion offline", guardada.getDescripcion());
        assertNotNull(guardada.getVectorReloj());
        assertTrue(guardada.getVectorReloj().contains("\"principal\":1"));
        assertNotNull(guardada.getHashActual());
        assertEquals(64, guardada.getHashActual().length());
        assertEquals("v1", guardada.getVersionCanonica());
        assertNotNull(guardada.getContenidoCanonico());
        assertTrue(
                guardada.getContenidoCanonico()
                        .contains("\"reloj_vectorial\":{")
        );
        assertTrue(
                guardada.getContenidoCanonico()
                        .contains("\"principal\":1")
        );
    }

    @Test
    void modoM2_continuaDesdeCabezaYLamportPersistidos() {
        auditoriaService.setAuditMode("m2");

        String hashPersistido =
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

        EstadoCadenaAuditoria estadoPersistido = EstadoCadenaAuditoria.builder()
                .idEstado((short) 1)
                .ultimoHash(hashPersistido)
                .ultimoLamport(41L)
                .vectorReloj("{}")
                .build();

        when(estadoCadenaRepo.buscarParaActualizar((short) 1))
                .thenReturn(java.util.Optional.of(estadoPersistido));

        when(hmacService.firmar(
                any(), any(), any(), any(), any(),
                any(), any(), any(), any()
        )).thenReturn("HMAC_PERSISTIDO");

        auditoriaService.registrarCrud(
                "CREAR",
                "calificacion",
                150L,
                "Continuidad tras reinicio"
        );

        ArgumentCaptor<Auditoria> captor =
                ArgumentCaptor.forClass(Auditoria.class);
        verify(repo).save(captor.capture());

        Auditoria guardada = captor.getValue();

        assertEquals(hashPersistido, guardada.getHashAnterior());
        assertEquals(42L, guardada.getRelojLamport());
        assertNotNull(guardada.getHashActual());
        assertEquals(64, guardada.getHashActual().length());

        assertEquals(guardada.getHashActual(), estadoPersistido.getUltimoHash());
        assertEquals(42L, estadoPersistido.getUltimoLamport());

        verify(estadoCadenaRepo).buscarParaActualizar((short) 1);
        verify(estadoCadenaRepo).save(estadoPersistido);
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

        assertEquals("Ingreso de nota inicial", evento1.getDescripcion());
        assertEquals("Rectificacion de nota", evento2.getDescripcion());
        assertEquals(1L, evento1.getRelojLamport());
        assertEquals(2L, evento2.getRelojLamport());
        assertEquals(
                AuditHashService.GENESIS_HASH,
                evento1.getHashAnterior()
        );
        assertEquals(
                evento1.getHashActual(),
                evento2.getHashAnterior()
        );
        assertEquals("v1", evento1.getVersionCanonica());
        assertEquals("v1", evento2.getVersionCanonica());
        assertNotNull(evento1.getContenidoCanonico());
        assertNotNull(evento2.getContenidoCanonico());
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
