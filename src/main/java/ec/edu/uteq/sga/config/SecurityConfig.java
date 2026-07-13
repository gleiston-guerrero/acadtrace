package ec.edu.uteq.sga.config;

import ec.edu.uteq.sga.security.JwtFilter;
import ec.edu.uteq.sga.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtFilter jwtFilter;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public CorsFilter corsFilter() {
        return new CorsFilter(corsConfigurationSource());
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/anos-lectivos/actual").authenticated()
                        .requestMatchers("/api/usuarios/**").hasAnyAuthority("ROLE_DIRECTOR", "ROLE_SOPORTE_TECNICO")
                        .requestMatchers("/api/admin/**").hasAnyAuthority("ROLE_DIRECTOR")
                        .requestMatchers("/api/anos-lectivos/**").hasAnyAuthority("ROLE_DIRECTOR")
                        .requestMatchers("/api/grados/**").hasAnyAuthority("ROLE_DIRECTOR", "ROLE_SECRETARIA")
                        .requestMatchers("/api/asignaturas/**").hasAnyAuthority("ROLE_DIRECTOR", "ROLE_SECRETARIA")
                        .requestMatchers("/api/estudiantes/**").hasAnyAuthority("ROLE_DIRECTOR", "ROLE_SECRETARIA")
                        .requestMatchers("/api/representantes/**").hasAnyAuthority("ROLE_DIRECTOR", "ROLE_SECRETARIA")
                        .requestMatchers("/api/matriculas/**").hasAnyAuthority("ROLE_DIRECTOR", "ROLE_SECRETARIA", "ROLE_DOCENTE")
                        .requestMatchers("/api/asignaciones/**").hasAnyAuthority("ROLE_DIRECTOR")
                        .requestMatchers("/api/calificaciones/**").hasAnyAuthority("ROLE_DIRECTOR", "ROLE_DOCENTE", "ROLE_SECRETARIA")
                        .requestMatchers("/api/docente/**").hasAnyAuthority("ROLE_DOCENTE")
                        .requestMatchers("/api/docentes/**").hasAnyAuthority("ROLE_DOCENTE")
                        .requestMatchers("/api/rpc/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}