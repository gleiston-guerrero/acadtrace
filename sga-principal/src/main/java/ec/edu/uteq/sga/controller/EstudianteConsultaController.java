package ec.edu.uteq.sga.controller;

import ec.edu.uteq.sga.entity.Matricula;
import ec.edu.uteq.sga.repository.MatriculaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Consulta de solo lectura de estudiantes matriculados por grado/paralelo.
 * La escritura de estudiantes vive en Secretaría; aquí solo se lee de la BD
 * compartida para el Director (módulos Grados y vista de cursos).
 */
@RestController
@RequestMapping("/api/estudiantes")
@RequiredArgsConstructor
public class EstudianteConsultaController {

    private final MatriculaRepository matriculaRepository;

    @GetMapping("/por-grado")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> porGrado(
            @RequestParam Long idGrado,
            @RequestParam Long idAnoLectivo,
            @RequestParam(required = false) Long idParalelo) {

        List<Matricula> matriculas = (idParalelo != null)
                ? matriculaRepository.findByGradoParaleloAndAnoLectivoWithEstudiante(idGrado, idParalelo, idAnoLectivo)
                : matriculaRepository.findByGradoAndAnoLectivoWithEstudiante(idGrado, idAnoLectivo);

        List<Map<String, Object>> resp = matriculas.stream().map(m -> {
            var e = m.getEstudiante();
            var r = e.getRepresentante();
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("idMatricula", m.getIdMatricula());
            map.put("idEstudiante", e.getIdEstudiante());
            map.put("cedula", e.getCedula());
            map.put("codigoEstudiante", e.getCodigoEstudiante());
            map.put("nombres", e.getNombres());
            map.put("apellidos", e.getApellidos());
            map.put("genero", e.getGenero());
            map.put("telefono", e.getTelefono());
            map.put("telefonoAlt", e.getTelefonoAlt());
            map.put("correo", e.getCorreo());
            map.put("direccion", e.getDireccion());
            map.put("fechaNacimiento", e.getFechaNacimiento() != null ? e.getFechaNacimiento().toString() : null);
            map.put("nacionalidad", e.getNacionalidad());
            map.put("etnia", e.getEtnia());
            map.put("lugarNacimiento", e.getLugarNacimiento());
            map.put("viveCon", e.getViveCon());
            map.put("numerosHermanos", e.getNumerosHermanos());
            map.put("beneficioSocial", e.isBeneficioSocial());
            map.put("discapacidad", e.isDiscapacidad());
            map.put("tipoDiscapacidad", e.getTipoDiscapacidad());
            map.put("porcentajeDisc", e.getPorcentajeDisc());
            map.put("carnetConadis", e.getCarnetConadis());
            map.put("estado", m.getEstado());
            map.put("numeroOrden", m.getNumeroOrden());
            if (r != null) {
                map.put("representante", (r.getNombres() + " " + r.getApellidos()).trim());
                Map<String, Object> repMap = new java.util.HashMap<>();
                repMap.put("idRepresentante", r.getIdRepresentante());
                repMap.put("cedula", r.getCedula());
                repMap.put("nombres", r.getNombres());
                repMap.put("apellidos", r.getApellidos());
                repMap.put("parentesco", r.getParentesco());
                repMap.put("telefonoPrincipal", r.getTelefonoPrincipal());
                repMap.put("telefonoAlt", r.getTelefonoAlt());
                repMap.put("correo", r.getCorreo());
                repMap.put("direccion", r.getDireccion());
                repMap.put("fechaNacimiento", r.getFechaNacimiento() != null ? r.getFechaNacimiento().toString() : null);
                repMap.put("genero", r.getGenero());
                repMap.put("estadoCivil", r.getEstadoCivil());
                repMap.put("nacionalidad", r.getNacionalidad());
                repMap.put("ocupacion", r.getOcupacion());
                repMap.put("lugarTrabajo", r.getLugarTrabajo());
                repMap.put("telefonoTrabajo", r.getTelefonoTrabajo());
                repMap.put("cargo", r.getCargo());
                repMap.put("nivelInstruccion", r.getNivelInstruccion());
                repMap.put("ingresoMensual", r.getIngresoMensual());
                repMap.put("conviveConEstudiante", r.getConviveConEstudiante());
                repMap.put("contactoEmergenciaNombre", r.getContactoEmergenciaNombre());
                repMap.put("contactoEmergenciaTelefono", r.getContactoEmergenciaTelefono());
                repMap.put("observaciones", r.getObservaciones());
                map.put("representanteDetalle", repMap);
            } else {
                map.put("representante", null);
                map.put("representanteDetalle", null);
            }
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(resp);
    }
}
