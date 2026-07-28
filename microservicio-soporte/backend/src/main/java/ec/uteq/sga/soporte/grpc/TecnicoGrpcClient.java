package ec.uteq.sga.soporte.grpc;

import ec.uteq.sga.soporte.grpc.principal.ListarUsuariosRequest;
import ec.uteq.sga.soporte.grpc.principal.ListarUsuariosResponse;
import ec.uteq.sga.soporte.grpc.principal.Usuario;
import ec.uteq.sga.soporte.grpc.principal.UsuarioServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 */
@Service
public class TecnicoGrpcClient {

    private static final String INTERNAL_TOKEN_VALUE = "***REMOVED***";

    private final UsuarioServiceGrpc.UsuarioServiceBlockingStub stub;

    public TecnicoGrpcClient(ManagedChannel principalGrpcChannel) {
        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of("internal_token", Metadata.ASCII_STRING_MARSHALLER), INTERNAL_TOKEN_VALUE);

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

        ListarUsuariosResponse response = stub.listarUsuarios(request);

        return response.getUsuariosList().stream()
                .map(this::aMapa)
                .toList();
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
