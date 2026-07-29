package ec.uteq.sga.secretaria.service;

import ec.edu.uteq.sga.grpc.principal.FichaProto;
import ec.edu.uteq.sga.grpc.principal.GuardarFichaRequest;
import ec.uteq.sga.secretaria.dto.FichaEstudianteRequest;
import ec.uteq.sga.secretaria.grpc.PrincipalGrpcClient;
import ec.uteq.sga.secretaria.security.CryptoService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ficha medica/de emergencia del estudiante (sga_secretaria.fichas_estudiante,
 * entidad JPA de sga-principal); se gestiona por completo via gRPC, igual
 * que Estudiante. detalle_enfermedad/medicacion_permanente/alergias/
 * direccion_referencia se cifran aqui (CryptoService) antes de mandarlas por
 * gRPC — sga-principal guarda y devuelve el texto cifrado a ciegas, sin
 * conocer la clave, igual que ya pasa con los campos sensibles de
 * Estudiante. enfermedad_catastrofica es una bandera boolean y
 * tipo_sangre/contacto_emergencia/telefono_emergencia quedan en claro
 * (necesarios en texto plano ante una emergencia).
 */
@Service
public class FichaEstudianteService {

    private final PrincipalGrpcClient client;
    private final CryptoService crypto;

    public FichaEstudianteService(PrincipalGrpcClient client, CryptoService crypto) {
        this.client = client;
        this.crypto = crypto;
    }

    public Map<String, Object> obtenerPorEstudiante(long idEstudiante) {
        return fromProto(client.obtenerFicha(idEstudiante));
    }

    public Map<String, Object> guardar(long idEstudiante, FichaEstudianteRequest dto) {
        GuardarFichaRequest request = GuardarFichaRequest.newBuilder()
                .setIdEstudiante(idEstudiante)
                .setTipoSangre(blankToEmpty(dto.tipo_sangre()))
                .setEnfermedadCatastrofica(dto.enfermedad_catastrofica() != null && dto.enfermedad_catastrofica())
                .setDetalleEnfermedad(cifrar(dto.detalle_enfermedad()))
                .setMedicacionPermanente(cifrar(dto.medicacion_permanente()))
                .setAlergias(cifrar(dto.alergias()))
                .setContactoEmergencia(blankToEmpty(dto.contacto_emergencia()))
                .setTelefonoEmergencia(blankToEmpty(dto.telefono_emergencia()))
                .setDireccionReferencia(cifrar(dto.direccion_referencia()))
                .build();
        return fromProto(client.guardarFicha(request));
    }

    private String cifrar(String valor) {
        return (valor == null || valor.isBlank()) ? "" : crypto.encrypt(valor);
    }

    private String descifrar(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return crypto.decrypt(valor);
        } catch (RuntimeException e) {
            return valor; // dato en texto plano previo al cifrado: se devuelve tal cual
        }
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value;
    }

    private Map<String, Object> fromProto(FichaProto f) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id_ficha", f.getIdFicha());
        row.put("id_estudiante", f.getIdEstudiante());
        row.put("tipo_sangre", f.getTipoSangre().isBlank() ? null : f.getTipoSangre());
        row.put("enfermedad_catastrofica", f.getEnfermedadCatastrofica());
        row.put("detalle_enfermedad", descifrar(f.getDetalleEnfermedad()));
        row.put("medicacion_permanente", descifrar(f.getMedicacionPermanente()));
        row.put("alergias", descifrar(f.getAlergias()));
        row.put("contacto_emergencia", f.getContactoEmergencia().isBlank() ? null : f.getContactoEmergencia());
        row.put("telefono_emergencia", f.getTelefonoEmergencia().isBlank() ? null : f.getTelefonoEmergencia());
        row.put("direccion_referencia", descifrar(f.getDireccionReferencia()));
        row.put("fecha_actualizacion", f.getFechaActualizacion().isBlank() ? null : f.getFechaActualizacion());
        return row;
    }
}
