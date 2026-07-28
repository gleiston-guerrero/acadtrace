package ec.uteq.sga.soporte.dto;

import jakarta.validation.constraints.NotBlank;

public class AgregarComentarioDTO {

    @NotBlank(message = "El comentario no puede estar vacío")
    private String comentario;

    private Boolean esRespuestaInterna = false;

    // Getters y Setters
    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }

    public Boolean getEsRespuestaInterna() { return esRespuestaInterna; }
    public void setEsRespuestaInterna(Boolean esRespuestaInterna) { this.esRespuestaInterna = esRespuestaInterna; }
}