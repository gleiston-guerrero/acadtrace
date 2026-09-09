package ec.uteq.sga.soporte.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecurityFilterConfigTest {

    @Test
    void registraElFiltroJwtSoloParaLaApiDeSoporte() {
        SecurityFilterConfig config = new SecurityFilterConfig();

        FilterRegistrationBean<JwtAuthFilter> registration = config.jwtAuthFilterRegistration(
                mock(JwtService.class), new ObjectMapper());

        assertThat(registration.getFilter()).isInstanceOf(JwtAuthFilter.class);
        assertThat(registration.getUrlPatterns()).containsExactly("/api/soporte/*");
        assertThat(registration.getOrder()).isEqualTo(1);
    }
}
