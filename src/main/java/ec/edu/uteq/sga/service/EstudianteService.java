package ec.edu.uteq.sga.service;

import ec.edu.uteq.sga.dto.CrearEstudianteDTO;
import ec.edu.uteq.sga.dto.EstudianteDetalleDTO;
import ec.edu.uteq.sga.dto.EstudianteListDTO;
import ec.edu.uteq.sga.dto.RepresentanteInputDTO;
import ec.edu.uteq.sga.entity.Estudiante;
import ec.edu.uteq.sga.entity.Representante;
import ec.edu.uteq.sga.repository.EstudianteRepository;
import ec.edu.uteq.sga.repository.RepresentanteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EstudianteService {

    private final EstudianteRepository estudianteRepo;
    private final RepresentanteRepository representanteRepo;

    public List<EstudianteListDTO> listar(String q) {
        List<Estudiante> estudiantes = (q == null || q.isBlank())
                ? estudianteRepo.findAllWithRepresentante()
                : estudianteRepo.searchWithRepresentante(q.trim());

        return estudiantes.stream().map(this::toDTO).toList();
    }

    public EstudianteDetalleDTO obtener(Long id) {
        Estudiante e = estudianteRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudiante no encontrado"));
        return toDetalleDTO(e);
    }

    @Transactional
    public EstudianteListDTO crear(CrearEstudianteDTO dto) {
        String cedula = validarCedula(dto.getCedula());
        String nombres = validarNombres(dto.getNombres());
        String apellidos = validarApellidos(dto.getApellidos());

        if (estudianteRepo.existsByCedula(cedula))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un estudiante con esa cédula");

        Estudiante estudiante = new Estudiante();
        estudiante.setEstado("ACTIVO");
        estudiante.setOrigenListado("MANUAL");
        estudiante.setFechaCreacion(Instant.now());
        aplicarCampos(estudiante, dto, cedula, nombres, apellidos);

        return toDTO(estudianteRepo.save(estudiante));
    }

    @Transactional
    public EstudianteListDTO actualizar(Long id, CrearEstudianteDTO dto) {
        Estudiante estudiante = estudianteRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudiante no encontrado"));

        String cedula = validarCedula(dto.getCedula());
        String nombres = validarNombres(dto.getNombres());
        String apellidos = validarApellidos(dto.getApellidos());

        Optional<Estudiante> otro = estudianteRepo.findByCedula(cedula);
        if (otro.isPresent() && !otro.get().getIdEstudiante().equals(id))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe otro estudiante con esa cédula");

        aplicarCampos(estudiante, dto, cedula, nombres, apellidos);

        return toDTO(estudianteRepo.save(estudiante));
    }

    private String validarCedula(String cedula) {
        String limpia = cedula != null ? cedula.trim() : "";
        if (!limpia.matches("\\d{10}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cédula inválida (debe tener 10 dígitos)");
        return limpia;
    }

    private String validarNombres(String nombres) {
        String limpio = nombres != null ? nombres.trim() : "";
        if (limpio.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombres y apellidos son obligatorios");
        return limpio;
    }

    private String validarApellidos(String apellidos) {
        String limpio = apellidos != null ? apellidos.trim() : "";
        if (limpio.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombres y apellidos son obligatorios");
        return limpio;
    }

    private void aplicarCampos(Estudiante estudiante, CrearEstudianteDTO dto, String cedula, String nombres, String apellidos) {
        Representante representante = resolverRepresentante(dto.getRepresentante());

        estudiante.setCedula(cedula);
        estudiante.setNombres(nombres);
        estudiante.setApellidos(apellidos);
        estudiante.setCorreo(dto.getCorreo());
        estudiante.setTelefono(dto.getTelefono());
        estudiante.setTelefonoAlt(dto.getTelefonoAlt());
        estudiante.setFechaNacimiento(dto.getFechaNacimiento());
        estudiante.setGenero(dto.getGenero());
        estudiante.setDireccion(dto.getDireccion());
        estudiante.setNacionalidad(dto.getNacionalidad() != null && !dto.getNacionalidad().isBlank() ? dto.getNacionalidad() : "Ecuatoriana");
        estudiante.setEtnia(dto.getEtnia());
        estudiante.setLugarNacimiento(dto.getLugarNacimiento());
        estudiante.setViveCon(dto.getViveCon());
        estudiante.setNumerosHermanos(dto.getNumerosHermanos());
        estudiante.setBeneficioSocial(dto.isBeneficioSocial());
        estudiante.setDiscapacidad(dto.isDiscapacidad());
        estudiante.setTipoDiscapacidad(dto.getTipoDiscapacidad());
        estudiante.setPorcentajeDisc(dto.getPorcentajeDisc());
        estudiante.setCarnetConadis(dto.getCarnetConadis());
        estudiante.setFotoUrl(dto.getFotoUrl());
        if (representante != null) estudiante.setRepresentante(representante);
        estudiante.setFechaActualizacion(Instant.now());
    }

    private Representante resolverRepresentante(RepresentanteInputDTO r) {
        if (r == null) return null;

        String cedula = r.getCedula() != null ? r.getCedula().trim() : "";
        String nombres = r.getNombres() != null ? r.getNombres().trim() : "";
        String apellidos = r.getApellidos() != null ? r.getApellidos().trim() : "";
        String telefono = r.getTelefonoPrincipal() != null ? r.getTelefonoPrincipal().trim() : "";

        boolean algunCampo = !cedula.isEmpty() || !nombres.isEmpty() || !apellidos.isEmpty() || !telefono.isEmpty();
        if (!algunCampo) return null;

        if (!cedula.isEmpty()) {
            var existente = representanteRepo.findByCedula(cedula);
            if (existente.isPresent()) return existente.get();
        }

        if (nombres.isEmpty() || apellidos.isEmpty() || telefono.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Para registrar un representante nuevo se requiere nombres, apellidos y teléfono principal");

        Representante nuevo = Representante.builder()
                .cedula(cedula.isEmpty() ? null : cedula)
                .nombres(nombres)
                .apellidos(apellidos)
                .parentesco(r.getParentesco())
                .telefonoPrincipal(telefono)
                .telefonoAlt(r.getTelefonoAlt())
                .correo(r.getCorreo())
                .direccion(r.getDireccion())
                .fechaCreacion(Instant.now())
                .build();

        return representanteRepo.save(nuevo);
    }

    private EstudianteListDTO toDTO(Estudiante e) {
        return EstudianteListDTO.builder()
                .idEstudiante(e.getIdEstudiante())
                .cedula(e.getCedula())
                .codigoEstudiante(e.getCodigoEstudiante())
                .nombres(e.getNombres())
                .apellidos(e.getApellidos())
                .correo(e.getCorreo())
                .estado(e.getEstado())
                .origenListado(e.getOrigenListado())
                .representante(e.getRepresentante() != null
                        ? e.getRepresentante().getNombres() + " " + e.getRepresentante().getApellidos()
                        : null)
                .fotoUrl(e.getFotoUrl())
                .build();
    }

    private EstudianteDetalleDTO toDetalleDTO(Estudiante e) {
        Representante r = e.getRepresentante();
        return EstudianteDetalleDTO.builder()
                .idEstudiante(e.getIdEstudiante())
                .cedula(e.getCedula())
                .codigoEstudiante(e.getCodigoEstudiante())
                .nombres(e.getNombres())
                .apellidos(e.getApellidos())
                .correo(e.getCorreo())
                .telefono(e.getTelefono())
                .telefonoAlt(e.getTelefonoAlt())
                .fechaNacimiento(e.getFechaNacimiento())
                .genero(e.getGenero())
                .direccion(e.getDireccion())
                .nacionalidad(e.getNacionalidad())
                .etnia(e.getEtnia())
                .lugarNacimiento(e.getLugarNacimiento())
                .viveCon(e.getViveCon())
                .numerosHermanos(e.getNumerosHermanos())
                .beneficioSocial(e.isBeneficioSocial())
                .discapacidad(e.isDiscapacidad())
                .tipoDiscapacidad(e.getTipoDiscapacidad())
                .porcentajeDisc(e.getPorcentajeDisc())
                .carnetConadis(e.getCarnetConadis())
                .fotoUrl(e.getFotoUrl())
                .estado(e.getEstado())
                .origenListado(e.getOrigenListado())
                .representante(r != null
                        ? new RepresentanteInputDTO(r.getCedula(), r.getNombres(), r.getApellidos(), r.getParentesco(),
                                r.getTelefonoPrincipal(), r.getTelefonoAlt(), r.getCorreo(), r.getDireccion())
                        : null)
                .build();
    }
}
