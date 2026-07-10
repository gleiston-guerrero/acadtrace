package ec.edu.uteq.sga.soporte.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * En este microservicio la autenticación es 100% stateless vía JWT.
 * No se consulta la BD de usuarios — el token ya viene validado por JwtFilter.
 * Esta clase existe solo para satisfacer el contrato de Spring Security.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {
        // El JwtFilter ya autenticó al usuario antes de llegar aquí.
        // Retornamos un usuario mínimo para que Spring no lance error.
        return User.withUsername(username)
                .password("")
                .authorities("AUTHENTICATED")
                .build();
    }
}