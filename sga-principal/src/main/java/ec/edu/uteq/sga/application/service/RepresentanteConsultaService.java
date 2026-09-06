package ec.edu.uteq.sga.application.service;

import ec.edu.uteq.sga.domain.dto.representante.RepresentadoResponseDTO;
import ec.edu.uteq.sga.domain.entity.Matricula;
import ec.edu.uteq.sga.domain.entity.Representante;
import ec.edu.uteq.sga.infrastructure.repository.EstudianteRepository;
import ec.edu.uteq.sga.infrastructure.repository.MatriculaRepository;
import ec.edu.uteq.sga.infrastructure.repository.RepresentanteRepository;
import ec.edu.uteq.sga.infrastructure.repository.AsignacionRepository;
import ec.edu.uteq.sga.infrastructure.grpc.RepresentanteAcademicoGrpcClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RepresentanteConsultaService {
    private final RepresentanteRepository representantes;
    private final EstudianteRepository estudiantes;
    private final MatriculaRepository matriculas;
    private final AsignacionRepository asignaciones;
    private final RepresentanteAcademicoGrpcClient docente;

    public List<RepresentadoResponseDTO> listar(String username) {
        Representante representante = representante(username);
        Map<Long, RepresentadoResponseDTO> resultado = new LinkedHashMap<>();
        for (Matricula matricula : matriculas.findActivasByRepresentante(representante.getIdRepresentante())) {
            var estudiante = matricula.getEstudiante();
            resultado.computeIfAbsent(estudiante.getIdEstudiante(), ignored -> RepresentadoResponseDTO.builder()
                    .idEstudiante(estudiante.getIdEstudiante())
                    .nombres(estudiante.getNombres())
                    .apellidos(estudiante.getApellidos())
                    .curso(matricula.getGrado().getNombre())
                    .paralelo(matricula.getParalelo().getLetra())
                    .matriculas(new java.util.ArrayList<>())
                    .build()).getMatriculas().add(matricula.getIdMatricula());
        }
        return List.copyOf(resultado.values());
    }

    public RepresentadoResponseDTO autorizar(String username, Long idEstudiante) {
        if (!estudiantes.existsById(idEstudiante)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudiante no encontrado");
        }
        return listar(username).stream()
                .filter(item -> item.getIdEstudiante().equals(idEstudiante))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Estudiante no asociado al representante"));
    }

    public Map<String, Object> calificaciones(String username, Long idEstudiante) {
        var ids = matriculasAutorizadas(username, idEstudiante).stream().map(Matricula::getIdMatricula).toList();
        var response = docente.consultarCalificaciones(ids);
        return Map.of(
                "calificaciones", response.getCalificacionesList().stream().map(item -> Map.of(
                        "id_calificacion", item.getIdCalificacion(), "id_matricula", item.getIdMatricula(),
                        "id_actividad", item.getIdActividad(), "actividad", item.getActividad(),
                        "id_asignacion", item.getIdAsignacion(), "id_periodo", item.getIdPeriodo(),
                        "periodo", item.getPeriodo(), "nota", item.getNota(),
                        "nota_cualitativa", item.getNotaCualitativa())).toList(),
                "promedios", response.getPromediosList().stream().map(item -> Map.of(
                        "id_matricula", item.getIdMatricula(), "id_asignacion", item.getIdAsignacion(),
                        "id_periodo", item.getIdPeriodo(), "periodo", item.getPeriodo(),
                        "promedio_formativo", item.getPromedioFormativo(), "nota_sumativa", item.getNotaSumativa(),
                        "promedio_trimestral", item.getPromedioTrimestral(), "nota_cualitativa", item.getNotaCualitativa())).toList());
    }

    public Map<String, Object> asistencia(String username, Long idEstudiante) {
        var ids = matriculasAutorizadas(username, idEstudiante).stream().map(Matricula::getIdMatricula).toList();
        var response = docente.consultarAsistencia(ids);
        var resumen = response.getResumen();
        return Map.of(
                "asistencias", response.getAsistenciasList().stream().map(item -> Map.of(
                        "id_asistencia", item.getIdAsistencia(), "id_matricula", item.getIdMatricula(),
                        "id_asignacion", item.getIdAsignacion(), "id_periodo", item.getIdPeriodo(),
                        "periodo", item.getPeriodo(), "fecha", item.getFecha(), "estado", item.getEstado())).toList(),
                "resumen", Map.of("total", resumen.getTotal(), "presentes", resumen.getPresentes(),
                        "ausentes", resumen.getAusentes(), "justificados", resumen.getJustificados(),
                        "atrasos", resumen.getAtrasos(), "porcentaje_asistencia", resumen.getPorcentajeAsistencia()));
    }

    public List<Map<String, Object>> comunicados(String username) {
        var matriculasActivas = matriculas.findActivasByRepresentante(representante(username).getIdRepresentante());
        var idsAsignacion = matriculasActivas.stream().flatMap(m -> asignaciones
                .findByGrado_IdGradoAndParalelo_IdParaleloAndAnoLectivo_IdAnoLectivoAndActivoTrue(
                        m.getGrado().getIdGrado(), m.getParalelo().getIdParalelo(), m.getAnoLectivo().getIdAnoLectivo())
                .stream()).map(a -> a.getIdAsignacion()).distinct().toList();
        return docente.consultarComunicados(idsAsignacion).getComunicadosList().stream().map(item -> Map.<String, Object>of(
                "id", item.getId(), "titulo", item.getTitulo(), "contenido", item.getContenido(),
                "fecha", item.getFecha(), "fijado", item.getFijado())).toList();
    }

    private List<Matricula> matriculasAutorizadas(String username, Long idEstudiante) {
        if (!estudiantes.existsById(idEstudiante)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudiante no encontrado");
        }
        var result = matriculas.findActivasByRepresentanteAndEstudiante(
                representante(username).getIdRepresentante(), idEstudiante);
        if (result.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Estudiante no asociado al representante");
        }
        return result;
    }

    private Representante representante(String username) {
        return representantes.findByUsuario_Username(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario sin representante asociado"));
    }
}
