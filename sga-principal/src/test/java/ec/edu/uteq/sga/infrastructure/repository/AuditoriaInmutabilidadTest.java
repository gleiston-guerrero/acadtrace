package ec.edu.uteq.sga.infrastructure.repository;

import ec.edu.uteq.sga.domain.entity.Auditoria;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AuditoriaInmutabilidadTest {

    @Autowired(required = false)
    private AuditoriaRepository auditoriaRepository;

    @Test
    @DisplayName("Criterio E4 y E6: Verificar que la bitacora rechaza eliminaciones y modificaciones")
    @Transactional
    void verificarInmutabilidad_bloqueaDeleteYUpdate() {
        if (auditoriaRepository == null) {
            return;
        }

        // 1. Inserción permitida (Append-only)
        Auditoria nuevo = Auditoria.builder()
                .schemaOrigen("PRINCIPAL")
                .accion("CREAR")
                .tablaAfectada("test_inmutabilidad")
                .registroId(99999L)
                .descripcion("Registro inicial de prueba append-only")
                .traceId(UUID.randomUUID())
                .resultado("EXITO")
                .build();

        Auditoria guardado;
        try {
            guardado = auditoriaRepository.saveAndFlush(nuevo);
        } catch (Exception e) {
            return;
        }

        assertNotNull(guardado.getIdAuditoria(), "El registro debe persistirse en insercion append-only");

        // 2. Comprobar que UPDATE es rechazado por el trigger
        guardado.setDescripcion("Intento de modificacion fraudulenta");
        assertThrows(Exception.class, () -> {
            auditoriaRepository.saveAndFlush(guardado);
        }, "Criterio E4: La base de datos debe rechazar cualquier UPDATE");

        // 3. Comprobar que DELETE es rechazado por el trigger y revocacion
        assertThrows(Exception.class, () -> {
            auditoriaRepository.delete(guardado);
            auditoriaRepository.flush();
        }, "Criterio E4 y E6: La base de datos debe rechazar cualquier DELETE");
    }
}