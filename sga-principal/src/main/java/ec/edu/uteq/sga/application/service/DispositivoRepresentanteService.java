package ec.edu.uteq.sga.application.service;
import ec.edu.uteq.sga.domain.dto.notificacion.DispositivoRequestDTO;
import ec.edu.uteq.sga.domain.entity.*;
import ec.edu.uteq.sga.infrastructure.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;

@Service @RequiredArgsConstructor
public class DispositivoRepresentanteService {
 private final UsuarioRepository usuarios; private final RepresentanteRepository representantes;
 private final DispositivoRepresentanteRepository dispositivos;
 @Transactional public void registrar(String username, DispositivoRequestDTO dto) {
   Usuario usuario=usuarios.findByUsername(username).orElseThrow();
   if(representantes.findByUsuario_Username(username).isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
   DispositivoRepresentante d=dispositivos.findByToken(dto.token()).orElseGet(()->DispositivoRepresentante.builder().token(dto.token()).build());
   d.setUsuario(usuario); d.setPlataforma(dto.plataforma()); d.setActivo(true); d.setFechaActualizacion(Instant.now()); dispositivos.save(d);
 }
 @Transactional public void eliminar(String username, DispositivoRequestDTO dto) {
   dispositivos.findByToken(dto.token()).filter(d->d.getUsuario().getUsername().equals(username)).ifPresent(d->{d.setActivo(false); d.setFechaActualizacion(Instant.now()); dispositivos.save(d);});
 }
}
