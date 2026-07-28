package ec.uteq.sga.secretaria.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EstudianteRequest(
        String cedula,
        @NotBlank(message = "Nombres requeridos") String nombres,
        @NotBlank(message = "Apellidos requeridos") String apellidos,
        String fecha_nacimiento,
        @Pattern(regexp = "MASCULINO|FEMENINO|OTRO", message = "genero inválido") String genero,
        @Email(message = "correo inválido") String correo,
        String direccion,
        String telefono,
        Boolean discapacidad,
        String tipo_discapacidad,
        Integer porcentaje_disc,
        Long id_representante,
        String nacionalidad,
        String etnia,
        String lugar_nacimiento,
        String vive_con,
        Integer numeros_hermanos,
        Boolean beneficio_social,
        String carnet_conadis,
        String foto_url
) {}
