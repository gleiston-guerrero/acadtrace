package ec.edu.uteq.sga.service;

import ec.edu.uteq.sga.dto.grado.*;
import ec.edu.uteq.sga.entity.Grado;
import ec.edu.uteq.sga.entity.NivelEducativo;
import ec.edu.uteq.sga.entity.Paralelo;
import ec.edu.uteq.sga.repository.AnoLectivoRepository;
import ec.edu.uteq.sga.repository.GradoRepository;
import ec.edu.uteq.sga.repository.MatriculaRepository;
import ec.edu.uteq.sga.repository.NivelEducativoRepository;
import ec.edu.uteq.sga.repository.ParaleloRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GradoService {

    private final GradoRepository gradoRepo;
    private final NivelEducativoRepository nivelRepo;
    private final ParaleloRepository paraleloRepo;
    private final MatriculaRepository matriculaRepo;
    private final AnoLectivoRepository anoLectivoRepo;

    @Transactional(readOnly = true)
    public List<GradoResponseDTO> listarTodos() {
        Long idAnoActivo = obtenerIdAnoLectivoActivo();
        return gradoRepo.findAllByOrderByOrden().stream().map(g -> toDTO(g, idAnoActivo)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GradoResponseDTO> listarActivos() {
        Long idAnoActivo = obtenerIdAnoLectivoActivo();
        return gradoRepo.findByActivoTrueOrderByOrden().stream().map(g -> toDTO(g, idAnoActivo)).collect(Collectors.toList());
    }

    private Long obtenerIdAnoLectivoActivo() {
        return anoLectivoRepo.findByEsActualTrue().map(a -> a.getIdAnoLectivo()).orElse(null);
    }

    public GradoResponseDTO obtenerPorId(Long id) {
        Long idAnoActivo = obtenerIdAnoLectivoActivo();
        return toDTO(buscarPorId(id), idAnoActivo);
    }

    @Transactional
    public GradoResponseDTO crear(GradoRequestDTO dto) {
        if (gradoRepo.existsByNombre(dto.getNombre()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe ese grado");

        NivelEducativo nivel = null;
        if (dto.getIdNivel() != null)
            nivel = nivelRepo.findById(dto.getIdNivel())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nivel educativo no encontrado"));

        Grado grado = Grado.builder()
                .nombre(dto.getNombre())
                .orden(dto.getOrden())
                .capacidadMax(dto.getCapacidadMax())
                .activo(true)
                .nivelEducativo(nivel)
                .build();

        Long idAnoActivo = obtenerIdAnoLectivoActivo();
        return toDTO(gradoRepo.save(grado), idAnoActivo);
    }

    @Transactional
    public GradoResponseDTO actualizar(Long id, GradoRequestDTO dto) {
        Grado grado = buscarPorId(id);
        grado.setNombre(dto.getNombre());
        grado.setOrden(dto.getOrden());
        grado.setCapacidadMax(dto.getCapacidadMax());
        if (dto.getIdNivel() != null) {
            NivelEducativo nivel = nivelRepo.findById(dto.getIdNivel())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nivel educativo no encontrado"));
            grado.setNivelEducativo(nivel);
        }
        Long idAnoActivo = obtenerIdAnoLectivoActivo();
        return toDTO(gradoRepo.save(grado), idAnoActivo);
    }

    @Transactional
    public void cambiarEstado(Long id, boolean activo) {
        Grado grado = buscarPorId(id);
        grado.setActivo(activo);
        gradoRepo.save(grado);
    }

    @Transactional
    public ParaleloDTO crearParalelo(Long idGrado, String letra) {
        Grado grado = buscarPorId(idGrado);
        letra = letra.toUpperCase().trim();
        if (paraleloRepo.existsByGradoIdGradoAndLetra(idGrado, letra))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe el paralelo " + letra + " en ese grado");

        Paralelo paralelo = Paralelo.builder()
                .grado(grado)
                .letra(letra)
                .activo(true)
                .build();
        return toParaleloDTO(paraleloRepo.save(paralelo), null);
    }

    @Transactional
    public void cambiarEstadoParalelo(Long idParalelo, boolean activo) {
        Paralelo p = paraleloRepo.findById(idParalelo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paralelo no encontrado"));
        p.setActivo(activo);
        paraleloRepo.save(p);
    }

    private Grado buscarPorId(Long id) {
        return gradoRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grado no encontrado"));
    }

    private GradoResponseDTO toDTO(Grado g, Long idAnoActivo) {
        List<ParaleloDTO> paralelos = paraleloRepo.findByGradoIdGradoOrderByLetra(g.getIdGrado())
                .stream().map(p -> toParaleloDTO(p, idAnoActivo)).collect(Collectors.toList());

        return GradoResponseDTO.builder()
                .idGrado(g.getIdGrado())
                .nombre(g.getNombre())
                .orden(g.getOrden())
                .capacidadMax(g.getCapacidadMax())
                .activo(g.isActivo())
                .nivelEducativo(g.getNivelEducativo() != null ? g.getNivelEducativo().getNombre() : null)
                .idNivel(g.getNivelEducativo() != null ? g.getNivelEducativo().getIdNivel() : null)
                .tipoEscala(g.getNivelEducativo() != null ? g.getNivelEducativo().getTipoEscala() : null)
                .paralelos(paralelos)
                .build();
    }

    private ParaleloDTO toParaleloDTO(Paralelo p, Long idAnoActivo) {
        long total = 0;
        if (idAnoActivo != null) {
            total = matriculaRepo.countByGradoParaleloAnoLectivo(p.getGrado().getIdGrado(), p.getIdParalelo(), idAnoActivo);
        }
        return ParaleloDTO.builder()
                .idParalelo(p.getIdParalelo())
                .letra(p.getLetra())
                .activo(p.isActivo())
                .idGrado(p.getGrado().getIdGrado())
                .nombreGrado(p.getGrado().getNombre())
                .totalEstudiantes(total)
                .build();
    }
}
