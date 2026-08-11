package ec.edu.uteq.sga.service;

import ec.edu.uteq.sga.dto.notificacion.NotificacionMasivoRequestDTO;
import ec.edu.uteq.sga.dto.notificacion.NotificacionMiasResponseDTO;
import ec.edu.uteq.sga.dto.notificacion.NotificacionResponseDTO;
import ec.edu.uteq.sga.entity.Notificacion;
import ec.edu.uteq.sga.entity.Usuario;
import ec.edu.uteq.sga.repository.NotificacionRepository;
import ec.edu.uteq.sga.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificacionService {

    private static final int LIMITE_MIAS = 10;

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;

    public NotificacionMiasResponseDTO mias(Authentication auth) {
        Usuario usuario = resolverUsuario(auth);
        List<NotificacionResponseDTO> notificaciones = notificacionRepository
                .findByUsuario_IdUsuarioOrderByFechaDesc(usuario.getIdUsuario(), PageRequest.of(0, LIMITE_MIAS, Sort.by("fecha").descending()))
                .map(this::toDTO)
                .toList();
        long noLeidas = notificacionRepository.countByUsuario_IdUsuarioAndLeidaFalse(usuario.getIdUsuario());
        return NotificacionMiasResponseDTO.builder().notificaciones(notificaciones).noLeidas(noLeidas).build();
    }

    @Transactional
    public void marcarLeida(Authentication auth, Long idNotificacion) {
        Usuario usuario = resolverUsuario(auth);
        Notificacion notificacion = notificacionRepository.findById(idNotificacion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notificación no encontrada"));
        if (notificacion.getUsuario() == null || !notificacion.getUsuario().getIdUsuario().equals(usuario.getIdUsuario())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La notificación no pertenece al usuario autenticado");
        }
        notificacion.setLeida(true);
        notificacionRepository.save(notificacion);
    }

    @Transactional
    public void marcarTodasLeidas(Authentication auth) {
        Usuario usuario = resolverUsuario(auth);
        notificacionRepository.marcarTodasLeidas(usuario.getIdUsuario());
    }

    /** Llamado por otros microservicios (secretaria/docente/soporte) para notificar a varios usuarios. */
    @Transactional
    public void enviarMasivo(NotificacionMasivoRequestDTO dto) {
        for (Long idUsuario : dto.getIdsUsuarios()) {
            Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);
            if (usuario == null) continue;
            Notificacion notificacion = Notificacion.builder()
                    .usuario(usuario)
                    .tipo(dto.getTipo())
                    .titulo(dto.getTitulo())
                    .mensaje(dto.getMensaje())
                    .urlDestino(dto.getUrlDestino())
                    .build();
            notificacionRepository.save(notificacion);
        }
    }

    private Usuario resolverUsuario(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No hay usuario autenticado en el contexto");
        }
        return usuarioRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private NotificacionResponseDTO toDTO(Notificacion n) {
        return NotificacionResponseDTO.builder()
                .idNotificacion(n.getIdNotificacion())
                .tipo(n.getTipo())
                .titulo(n.getTitulo())
                .mensaje(n.getMensaje())
                .urlDestino(n.getUrlDestino())
                .leida(n.isLeida())
                .fecha(n.getFecha())
                .build();
    }
}
