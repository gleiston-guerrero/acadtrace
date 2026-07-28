package ec.uteq.sga.secretaria.dto;

public record FichaEstudianteRequest(
        String tipo_sangre,
        Boolean enfermedad_catastrofica,
        String detalle_enfermedad,
        String medicacion_permanente,
        String alergias,
        String contacto_emergencia,
        String telefono_emergencia,
        String direccion_referencia
) {}
