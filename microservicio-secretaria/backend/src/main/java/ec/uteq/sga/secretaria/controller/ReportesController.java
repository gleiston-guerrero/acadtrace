package ec.uteq.sga.secretaria.controller;

import ec.uteq.sga.secretaria.service.ReportesService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/secretario/reportes")
public class ReportesController {

    private final ReportesService service;

    public ReportesController(ReportesService service) {
        this.service = service;
    }

    @GetMapping("/certificado-matricula/{id}")
    public ResponseEntity<byte[]> certificadoMatricula(@PathVariable Long id) throws IOException {
        return pdfResponse(service.certificadoMatricula(id), "certificado-matricula-" + id + ".pdf");
    }

    @GetMapping("/nomina-matriculas/{idAno}")
    public ResponseEntity<byte[]> nominaMatriculas(
            @PathVariable Long idAno,
            @RequestParam(name = "id_grado", required = false) Long idGrado,
            @RequestParam(name = "id_paralelo", required = false) Long idParalelo) throws IOException {
        byte[] pdf = service.nominaMatriculas(idAno, idGrado, idParalelo);
        return pdfResponse(pdf, "nomina-matriculas-" + idAno + ".pdf");
    }

    @GetMapping("/ficha-estudiante/{id}")
    public ResponseEntity<byte[]> fichaEstudiante(@PathVariable Long id) throws IOException {
        return pdfResponse(service.fichaEstudiante(id), "ficha-estudiante-" + id + ".pdf");
    }

    @GetMapping("/libreta/{idMatricula}")
    public ResponseEntity<byte[]> libreta(
            @PathVariable Long idMatricula,
            @RequestParam(name = "idPeriodo", required = false) Long idPeriodo) throws IOException {
        return pdfResponse(service.libreta(idMatricula, idPeriodo), "libreta-" + idMatricula + ".pdf");
    }

    @GetMapping("/asistencia-mensual/{idMatricula}")
    public ResponseEntity<byte[]> asistenciaMensual(
            @PathVariable Long idMatricula,
            @RequestParam(name = "mes") String mes) throws IOException {
        return pdfResponse(service.asistenciaMensual(idMatricula, mes), "asistencia-" + idMatricula + "-" + mes + ".pdf");
    }

    @GetMapping("/ficha-representante/{idRepresentante}")
    public ResponseEntity<byte[]> fichaRepresentante(@PathVariable Long idRepresentante) throws IOException {
        return pdfResponse(service.fichaRepresentante(idRepresentante), "ficha-representante-" + idRepresentante + ".pdf");
    }

    @GetMapping("/estadisticas/{idAno}")
    public Map<String, Object> estadisticas(@PathVariable Long idAno) {
        return service.estadisticas(idAno);
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filename) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(pdf);
    }
}
