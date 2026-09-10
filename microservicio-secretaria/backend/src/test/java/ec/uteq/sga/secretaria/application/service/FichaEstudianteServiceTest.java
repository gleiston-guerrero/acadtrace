package ec.uteq.sga.secretaria.application.service;

import ec.edu.uteq.sga.grpc.principal.FichaProto;
import ec.edu.uteq.sga.grpc.principal.GuardarFichaRequest;
import ec.uteq.sga.secretaria.domain.dto.FichaEstudianteRequest;
import ec.uteq.sga.secretaria.infrastructure.grpc.PrincipalGrpcClient;
import ec.uteq.sga.secretaria.infrastructure.security.CryptoService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FichaEstudianteServiceTest {
    private CryptoService crypto() {
        byte[] key = new byte[32]; System.arraycopy("sga-secretaria-test-key-32bytes!".getBytes(), 0, key, 0, 32);
        return new CryptoService(Base64.getEncoder().encodeToString(key));
    }

    @Test
    void guardaCamposSensiblesCifradosYRecuperaSuContenido() {
        PrincipalGrpcClient client = mock(PrincipalGrpcClient.class);
        CryptoService crypto = crypto();
        FichaEstudianteService service = new FichaEstudianteService(client, crypto);
        when(client.guardarFicha(any())).thenAnswer(i -> FichaProto.newBuilder().setIdFicha(2).setIdEstudiante(7)
                .setTipoSangre("O+").setDetalleEnfermedad(i.<GuardarFichaRequest>getArgument(0).getDetalleEnfermedad())
                .setAlergias(i.<GuardarFichaRequest>getArgument(0).getAlergias()).setContactoEmergencia("Ana").build());

        var result = service.guardar(7, new FichaEstudianteRequest("O+", true, "Asma", null, "Penicilina", "Ana", null, null));
        ArgumentCaptor<GuardarFichaRequest> captor = ArgumentCaptor.forClass(GuardarFichaRequest.class);
        verify(client).guardarFicha(captor.capture());
        assertThat(captor.getValue().getDetalleEnfermedad()).isNotEqualTo("Asma");
        assertThat(result).containsEntry("detalle_enfermedad", "Asma").containsEntry("alergias", "Penicilina");
    }

    @Test
    void aceptaDatosHistoricosEnClaroYValoresVacios() {
        PrincipalGrpcClient client = mock(PrincipalGrpcClient.class);
        FichaEstudianteService service = new FichaEstudianteService(client, crypto());
        when(client.obtenerFicha(1)).thenReturn(FichaProto.newBuilder().setIdFicha(1).setIdEstudiante(1)
                .setDetalleEnfermedad("texto legado").setTipoSangre("").setContactoEmergencia("").build());
        assertThat(service.obtenerPorEstudiante(1)).containsEntry("detalle_enfermedad", "texto legado")
                .containsEntry("tipo_sangre", null).containsEntry("contacto_emergencia", null);
    }
}
