package ec.edu.uteq.sga.service;

import ec.edu.uteq.sga.dto.persona.PersonaRequestDTO;
import ec.edu.uteq.sga.dto.persona.PersonaResponseDTO;
import ec.edu.uteq.sga.entity.Persona;
import ec.edu.uteq.sga.entity.Rol;
import ec.edu.uteq.sga.entity.Usuario;
import ec.edu.uteq.sga.repository.PersonaRepository;
import ec.edu.uteq.sga.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PersonaService {

    private final PersonaRepository personaRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<PersonaResponseDTO> listar() {
        return personaRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PersonaResponseDTO buscarPorCedula(String cedula) {
        Persona p = personaRepository.findByCedula(cedula)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró una persona con la cédula " + cedula));
        return toResponse(p);
    }

    @Transactional(readOnly = true)
    public PersonaResponseDTO obtenerPorUsuario(Long idUsuario) {
        Persona p = personaRepository.findByUsuario_IdUsuario(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El usuario no tiene datos personales registrados"));
        return toResponse(p);
    }

    @Transactional
    public PersonaResponseDTO crear(PersonaRequestDTO dto) {
        if (dto.getIdUsuario() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "id_usuario es requerido");
        }
        Usuario usuario = usuarioRepository.findById(dto.getIdUsuario())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no existe"));

        personaRepository.findByUsuario_IdUsuario(usuario.getIdUsuario()).ifPresent(existente -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El usuario ya tiene datos personales registrados");
        });

        if (personaRepository.existsByCedula(dto.getCedula())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe una persona registrada con esa cédula");
        }

        Persona p = Persona.builder()
                .usuario(usuario)
                .cedula(dto.getCedula())
                .nombres(dto.getNombres())
                .apellidos(dto.getApellidos())
                .fechaNacimiento(dto.getFechaNacimiento())
                .genero(dto.getGenero())
                .telefono(dto.getTelefono())
                .telefonoAlt(dto.getTelefonoAlt())
                .direccion(dto.getDireccion())
                .correoPersonal(dto.getCorreoPersonal())
                .tituloAcademico(dto.getTituloAcademico())
                .especializacion(dto.getEspecializacion())
                .fotoUrl(dto.getFotoUrl())
                .build();

        return toResponse(personaRepository.save(p));
    }

    @Transactional
    public PersonaResponseDTO actualizar(Long idPersona, PersonaRequestDTO dto) {
        Persona p = personaRepository.findById(idPersona)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Persona no existe"));

        if (dto.getCedula() != null && !dto.getCedula().equals(p.getCedula())
                && personaRepository.existsByCedula(dto.getCedula())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Otra persona ya tiene esa cédula");
        }

        if (dto.getCedula() != null) p.setCedula(dto.getCedula());
        if (dto.getNombres() != null) p.setNombres(dto.getNombres());
        if (dto.getApellidos() != null) p.setApellidos(dto.getApellidos());
        p.setFechaNacimiento(dto.getFechaNacimiento());
        p.setGenero(dto.getGenero());
        p.setTelefono(dto.getTelefono());
        p.setTelefonoAlt(dto.getTelefonoAlt());
        p.setDireccion(dto.getDireccion());
        p.setCorreoPersonal(dto.getCorreoPersonal());
        p.setTituloAcademico(dto.getTituloAcademico());
        p.setEspecializacion(dto.getEspecializacion());
        if (dto.getFotoUrl() != null) p.setFotoUrl(dto.getFotoUrl());
        p.setFechaActualizacion(Instant.now());

        return toResponse(p);
    }

    @Transactional
    public void eliminar(Long idPersona) {
        if (!personaRepository.existsById(idPersona)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Persona no existe");
        }
        personaRepository.deleteById(idPersona);
    }

    private PersonaResponseDTO toResponse(Persona p) {
        Usuario u = p.getUsuario();
        return PersonaResponseDTO.builder()
                .idPersona(p.getIdPersona())
                .idUsuario(u != null ? u.getIdUsuario() : null)
                .username(u != null ? u.getUsername() : null)
                .correo(u != null ? u.getCorreo() : null)
                .roles(u != null
                        ? u.getRoles().stream().map(Rol::getNombre).collect(Collectors.toSet())
                        : null)
                .cedula(p.getCedula())
                .nombres(p.getNombres())
                .apellidos(p.getApellidos())
                .fechaNacimiento(p.getFechaNacimiento())
                .genero(p.getGenero())
                .telefono(p.getTelefono())
                .telefonoAlt(p.getTelefonoAlt())
                .direccion(p.getDireccion())
                .correoPersonal(p.getCorreoPersonal())
                .tituloAcademico(p.getTituloAcademico())
                .especializacion(p.getEspecializacion())
                .fotoUrl(p.getFotoUrl())
                .build();
    }
}
