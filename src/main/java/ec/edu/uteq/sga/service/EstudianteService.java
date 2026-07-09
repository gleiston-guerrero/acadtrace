package ec.edu.uteq.sga.service;

import ec.edu.uteq.sga.dto.estudiante.*;
import ec.edu.uteq.sga.entity.Estudiante;
import ec.edu.uteq.sga.entity.FichaEstudiante;
import ec.edu.uteq.sga.entity.Representante;
import ec.edu.uteq.sga.entity.Usuario;
import ec.edu.uteq.sga.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EstudianteService {

    private final EstudianteRepository estudianteRepo;
    private final RepresentanteRepository representanteRepo;
    private final UsuarioRepository usuarioRepo;
    private final FichaEstudianteRepository fichaRepo;
    private final MatriculaRepository matriculaRepo;

    @Transactional(readOnly = true)
    public List<EstudianteResponseDTO> listarTodos() {
        List<Estudiante> estudiantes = estudianteRepo.findAllWithRepresentante();
        return toDTOList(estudiantes);
    }

    @Transactional(readOnly = true)
    public List<EstudianteResponseDTO> listarPorGrado(Long idGrado, Long idAnoLectivo, Long idParalelo) {
        var matriculas = idParalelo != null
                ? matriculaRepo.findByGradoParaleloAndAnoLectivoWithEstudiante(idGrado, idParalelo, idAnoLectivo)
                : matriculaRepo.findByGradoAndAnoLectivoWithEstudiante(idGrado, idAnoLectivo);
        List<Estudiante> estudiantes = matriculas.stream()
                .map(m -> m.getEstudiante())
                .collect(Collectors.toList());
        return toDTOList(estudiantes);
    }

    @Transactional(readOnly = true)
    public List<EstudianteResponseDTO> buscar(String query) {
        List<Estudiante> estudiantes = estudianteRepo.searchWithRepresentante(query);
        return toDTOList(estudiantes);
    }

    @Transactional(readOnly = true)
    public EstudianteResponseDTO obtenerPorId(Long id) {
        return toDTO(buscarPorId(id));
    }

    @Transactional
    public EstudianteResponseDTO crear(EstudianteRequestDTO dto, String username) {
        if (dto.getCedula() != null && !dto.getCedula().isBlank() && estudianteRepo.existsByCedula(dto.getCedula()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un estudiante con esa cédula");

        Representante representante = null;
        if (dto.getIdRepresentante() != null)
            representante = representanteRepo.findById(dto.getIdRepresentante())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Representante no encontrado"));

        Usuario creador = usuarioRepo.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Estudiante estudiante = Estudiante.builder()
                .cedula(dto.getCedula())
                .nombres(dto.getNombres())
                .apellidos(dto.getApellidos())
                .fechaNacimiento(dto.getFechaNacimiento())
                .genero(dto.getGenero())
                .direccion(dto.getDireccion())
                .telefono(dto.getTelefono())
                .correo(dto.getCorreo())
                .discapacidad(dto.isDiscapacidad())
                .tipoDiscapacidad(dto.getTipoDiscapacidad())
                .porcentajeDisc(dto.getPorcentajeDisc())
                .carnetConadis(dto.getCarnetConadis())
                .nacionalidad(dto.getNacionalidad())
                .etnia(dto.getEtnia())
                .lugarNacimiento(dto.getLugarNacimiento())
                .viveCon(dto.getViveCon())
                .numerosHermanos(dto.getNumerosHermanos())
                .beneficioSocial(dto.isBeneficioSocial())
                .estado("ACTIVO")
                .representante(representante)
                .creadoPor(creador)
                .build();

        estudiante = estudianteRepo.save(estudiante);

        guardarFicha(estudiante, dto);

        return toDTO(estudiante);
    }

    @Transactional
    public EstudianteResponseDTO actualizar(Long id, EstudianteRequestDTO dto) {
        Estudiante estudiante = buscarPorId(id);

        estudiante.setNombres(dto.getNombres());
        estudiante.setApellidos(dto.getApellidos());
        estudiante.setFechaNacimiento(dto.getFechaNacimiento());
        estudiante.setGenero(dto.getGenero());
        estudiante.setDireccion(dto.getDireccion());
        estudiante.setTelefono(dto.getTelefono());
        estudiante.setCorreo(dto.getCorreo());
        estudiante.setDiscapacidad(dto.isDiscapacidad());
        estudiante.setTipoDiscapacidad(dto.getTipoDiscapacidad());
        estudiante.setPorcentajeDisc(dto.getPorcentajeDisc());
        estudiante.setCarnetConadis(dto.getCarnetConadis());
        estudiante.setNacionalidad(dto.getNacionalidad());
        estudiante.setEtnia(dto.getEtnia());
        estudiante.setLugarNacimiento(dto.getLugarNacimiento());
        estudiante.setViveCon(dto.getViveCon());
        estudiante.setNumerosHermanos(dto.getNumerosHermanos());
        estudiante.setBeneficioSocial(dto.isBeneficioSocial());
        estudiante.setFechaActualizacion(Instant.now());

        if (dto.getIdRepresentante() != null) {
            Representante rep = representanteRepo.findById(dto.getIdRepresentante())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Representante no encontrado"));
            estudiante.setRepresentante(rep);
        }

        estudiante = estudianteRepo.save(estudiante);

        guardarFicha(estudiante, dto);

        return toDTO(estudiante);
    }

    @Transactional
    public void cambiarEstado(Long id, String estado) {
        Estudiante estudiante = buscarPorId(id);
        estudiante.setEstado(estado);
        estudianteRepo.save(estudiante);
    }

    private void guardarFicha(Estudiante estudiante, EstudianteRequestDTO dto) {
        boolean tieneDatosMedicos = dto.getTipoSangre() != null || dto.getAlergias() != null
                || dto.getEnfermedadesCronicas() != null || dto.getMedicamentos() != null
                || dto.getContactoEmergenciaNombre() != null || dto.getObservacionesMedicas() != null;

        if (!tieneDatosMedicos) return;

        FichaEstudiante ficha = fichaRepo.findByEstudianteIdEstudiante(estudiante.getIdEstudiante())
                .orElse(FichaEstudiante.builder().estudiante(estudiante).build());

        ficha.setTipoSangre(dto.getTipoSangre());
        ficha.setAlergias(dto.getAlergias());
        ficha.setDetalleEnfermedad(dto.getEnfermedadesCronicas());
        ficha.setMedicacionPermanente(dto.getMedicamentos());
        ficha.setContactoEmergencia(dto.getContactoEmergenciaNombre());
        ficha.setTelefonoEmergencia(dto.getContactoEmergenciaTelefono());
        ficha.setDireccionReferencia(dto.getObservacionesMedicas());
        ficha.setFechaActualizacion(Instant.now());

        fichaRepo.save(ficha);
    }

    private Estudiante buscarPorId(Long id) {
        return estudianteRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudiante no encontrado"));
    }

    private List<EstudianteResponseDTO> toDTOList(List<Estudiante> estudiantes) {
        if (estudiantes.isEmpty()) return Collections.emptyList();

        List<Long> ids = estudiantes.stream().map(Estudiante::getIdEstudiante).collect(Collectors.toList());
        Map<Long, FichaEstudiante> fichasMap = fichaRepo.findByEstudiante_IdEstudianteIn(ids).stream()
                .collect(Collectors.toMap(f -> f.getEstudiante().getIdEstudiante(), Function.identity()));

        return estudiantes.stream()
                .map(e -> toDTO(e, fichasMap.get(e.getIdEstudiante())))
                .collect(Collectors.toList());
    }

    private EstudianteResponseDTO toDTO(Estudiante e) {
        FichaEstudiante ficha = fichaRepo.findByEstudianteIdEstudiante(e.getIdEstudiante()).orElse(null);
        return toDTO(e, ficha);
    }

    private EstudianteResponseDTO toDTO(Estudiante e, FichaEstudiante ficha) {
        EstudianteResponseDTO.EstudianteResponseDTOBuilder builder = EstudianteResponseDTO.builder()
                .idEstudiante(e.getIdEstudiante())
                .cedula(e.getCedula())
                .codigoEstudiante(e.getCodigoEstudiante())
                .nombres(e.getNombres())
                .apellidos(e.getApellidos())
                .fechaNacimiento(e.getFechaNacimiento())
                .genero(e.getGenero())
                .direccion(e.getDireccion())
                .telefono(e.getTelefono())
                .correo(e.getCorreo())
                .discapacidad(e.isDiscapacidad())
                .tipoDiscapacidad(e.getTipoDiscapacidad())
                .porcentajeDisc(e.getPorcentajeDisc())
                .carnetConadis(e.getCarnetConadis())
                .nacionalidad(e.getNacionalidad())
                .etnia(e.getEtnia())
                .lugarNacimiento(e.getLugarNacimiento())
                .viveCon(e.getViveCon())
                .numerosHermanos(e.getNumerosHermanos())
                .beneficioSocial(e.isBeneficioSocial())
                .estado(e.getEstado())
                .fotoUrl(e.getFotoUrl())
                .origenListado(e.getOrigenListado())
                .representante(e.getRepresentante() != null ?
                        e.getRepresentante().getNombres() + " " + e.getRepresentante().getApellidos() : null)
                .idRepresentante(e.getRepresentante() != null ? e.getRepresentante().getIdRepresentante() : null)
                .fechaCreacion(e.getFechaCreacion());

        if (ficha != null) {
            builder.tipoSangre(ficha.getTipoSangre())
                    .alergias(ficha.getAlergias())
                    .enfermedadesCronicas(ficha.getDetalleEnfermedad())
                    .medicamentos(ficha.getMedicacionPermanente())
                    .contactoEmergenciaNombre(ficha.getContactoEmergencia())
                    .contactoEmergenciaTelefono(ficha.getTelefonoEmergencia())
                    .observacionesMedicas(ficha.getDireccionReferencia());
        }

        return builder.build();
    }
}
