package ec.edu.uteq.sga.application.service;

import ec.edu.uteq.sga.domain.dto.representante.RepresentadoResponseDTO;
import ec.edu.uteq.sga.domain.entity.Matricula;
import ec.edu.uteq.sga.domain.entity.Representante;
import ec.edu.uteq.sga.infrastructure.repository.EstudianteRepository;
import ec.edu.uteq.sga.infrastructure.repository.MatriculaRepository;
import ec.edu.uteq.sga.infrastructure.repository.RepresentanteRepository;
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

    private Representante representante(String username) {
        return representantes.findByUsuario_Username(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario sin representante asociado"));
    }
}
