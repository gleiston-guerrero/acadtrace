package ec.uteq.sga.soporte.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtAuthFilterTest {

    private final JwtService jwtService = mock(JwtService.class);
    private final JwtAuthFilter filter = new JwtAuthFilter(jwtService, new ObjectMapper());

    @Test
    void rechazaCabeceraAusenteYTokensInvalidos() throws Exception {
        MockHttpServletResponse missingResponse = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest(), missingResponse, new MockFilterChain());
        assertThat(missingResponse.getStatus()).isEqualTo(401);
        assertThat(missingResponse.getContentAsString()).contains("Token no proporcionado");

        MockHttpServletRequest invalidRequest = new MockHttpServletRequest();
        invalidRequest.addHeader("Authorization", "Bearer invalido");
        doThrow(new JwtException("expirado")).when(jwtService).parse("invalido");
        MockHttpServletResponse invalidResponse = new MockHttpServletResponse();
        filter.doFilter(invalidRequest, invalidResponse, new MockFilterChain());
        assertThat(invalidResponse.getStatus()).isEqualTo(401);
        assertThat(invalidResponse.getContentAsString()).contains("Token invalido o expirado");
    }

    @Test
    void agregaUsuarioAutenticadoYContinuaLaCadena() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valido");
        AuthenticatedUser user = new AuthenticatedUser("ana", List.of("DOCENTE"));
        when(jwtService.parse("valido")).thenReturn(user);
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(request.getAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE)).isEqualTo(user);
        assertThat(chain.getRequest()).isSameAs(request);
    }
}
