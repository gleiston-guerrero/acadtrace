package ec.uteq.sga.soporte.infrastructure.grpc;

import ec.uteq.sga.soporte.grpc.principal.ListarUsuariosRequest;
import ec.uteq.sga.soporte.grpc.principal.ListarUsuariosResponse;
import ec.uteq.sga.soporte.grpc.principal.Usuario;
import ec.uteq.sga.soporte.grpc.principal.UsuarioServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Cliente gRPC hacia sga-principal. Reemplaza el llamado REST directo que
 * antes hacia el frontend (Usuarios.jsx -> http://localhost:8080/api) por un
 * llamado gRPC desde el backend de soporte, para no depender de acceso
 * directo entre servicios que no sea a traves del contrato definido aqui.
 *
 * Los stubs (UsuarioServiceGrpc, Usuario, etc.) se generan automaticamente
 * al compilar, a partir de src/main/proto/usuarios.proto.
 *
 * sga-principal exige un header "internal_token" en todo llamado gRPC
 * entrante (ver InternalAuthInterceptor.java alla); se lo agregamos aqui con
 * el mismo patron que ya usa DocenteGrpcClient del lado de sga-principal
 * para llamar a MICRO-DOCENTE.
 *
 * Resiliencia (issue #8): deadline de 3s por intento, maximo 2 reintentos
 * con backoff simple, y degradacion a lista vacia si sga-principal no
 * responde, en vez de romper el request que disparo esta llamada.
 */
@Slf4j
@Service
public class TecnicoGrpcClient {


    private static final long   DEADLINE_SECONDS      = 3;
    private static final int    MAX_REINTENTOS        = 2; // reintentos ADEMAS del intento inicial
    private static final long   BACKOFF_BASE_MS        = 300; // 300ms, 600ms...

    private final UsuarioServiceGrpc.UsuarioServiceBlockingStub stub;

    public TecnicoGrpcClient(ManagedChannel principalGrpcChannel, @Value("${app.grpc.internal-token}") String internalToken) {
        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of("internal_token", Metadata.ASCII_STRING_MARSHALLER), internalToken);

        this.stub = UsuarioServiceGrpc.newBlockingStub(principalGrpcChannel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));
    }

    /** Lista solo los usuarios con rol SOPORTE_TECNICO (para el dropdown de asignacion). */
    public List<Map<String, Object>> listarTecnicos() {
        return listarPorRol("SOPORTE_TECNICO");
    }

    public List<Map<String, Object>> listarPorRol(String rol) {
        ListarUsuariosRequest request = ListarUsuariosRequest.newBuilder()
                .setRol(rol == null ? "" : rol)
                .build();

        for (int intento = 0; intento <= MAX_REINTENTOS; intento++) {
            try {
                ListarUsuariosResponse response = stub
                        .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)
                        .listarUsuarios(request);

                return response.getUsuariosList().stream()
                        .map(this::aMapa)
                        .toList();

            } catch (StatusRuntimeException e) {
                boolean quedanReintentos = intento < MAX_REINTENTOS;
                log.error("Fallo al llamar a sga-principal (gRPC listarUsuarios, rol={}, intento={}/{}): {}",
                        rol, intento + 1, MAX_REINTENTOS + 1, e.getStatus());

                if (!quedanReintentos) {
                    log.error("Se agotaron los reintentos contra sga-principal, se degrada a lista vacia.");
                    return Collections.emptyList();
                }

                esperarBackoff(intento);
            }
        }

        // Inalcanzable en la practica (el for siempre retorna o cae al degradado arriba),
        // pero se deja como red de seguridad.
        return Collections.emptyList();
    }

    private void esperarBackoff(int intento) {
        long esperaMs = BACKOFF_BASE_MS * (long) Math.pow(2, intento); // 300ms, 600ms...
        try {
            Thread.sleep(esperaMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private Map<String, Object> aMapa(Usuario u) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("nombre", u.getNombre());
        m.put("apellido", u.getApellido());
        m.put("correo", u.getCorreo());
        m.put("activo", u.getActivo());
        m.put("roles", u.getRolesList());
        String nombreCompleto = (u.getNombre() + " " + u.getApellido()).trim();
        m.put("nombreCompleto", nombreCompleto.isEmpty() ? null : nombreCompleto);
        return m;
    }
}
