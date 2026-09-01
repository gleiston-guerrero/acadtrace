package ec.edu.uteq.sga.infrastructure.security;

import ec.edu.uteq.sga.domain.entity.Usuario;
import ec.edu.uteq.sga.infrastructure.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado: " + username));

        List<SimpleGrantedAuthority> authorities = usuario.getRoles().stream()
                .map(rol -> {
                    String nombre = rol.getNombre();
                    return new SimpleGrantedAuthority(nombre.startsWith("ROLE_") ? nombre : "ROLE_" + nombre);
                })
                .collect(Collectors.toList());

        return new User(
                usuario.getUsername(),
                usuario.getPasswordHash(),
                authorities
        );
    }
}