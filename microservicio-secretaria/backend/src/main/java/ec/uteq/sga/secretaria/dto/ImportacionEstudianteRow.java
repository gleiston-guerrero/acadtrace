package ec.uteq.sga.secretaria.dto;

public class ImportacionEstudianteRow {
    private int fila;
    private String cedula;
    private String nombres;
    private String apellidos;
    private String correo;
    private boolean yaExiste;
    private String error;

    public int getFila() { return fila; }
    public void setFila(int fila) { this.fila = fila; }
    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }
    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public boolean isYaExiste() { return yaExiste; }
    public void setYaExiste(boolean yaExiste) { this.yaExiste = yaExiste; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
