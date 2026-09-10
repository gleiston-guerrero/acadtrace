package ec.uteq.sga.soporte.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class JwtAuthFilterTest {

    @Test
    void sinBearer_rechazaAntesDeInvocarLaCadena() throws Exception {
        JwtAuthFilter filter = new JwtAuthFilter(mock(JwtService.class), new ObjectMapper());
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Token no proporcionado");
    }

    @Test
    void tokenInvalido_retorna401ConJson() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        given(jwtService.parse("invalido")).willThrow(new JwtException("firma"));
        JwtAuthFilter filter = new JwtAuthFilter(jwtService, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalido");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Token invalido o expirado");
    }

    @Test
    void tokenValido_guardaUsuarioYContinuaLaCadena() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        AuthenticatedUser user = new AuthenticatedUser("ana", List.of("SOPORTE_TECNICO"));
        given(jwtService.parse("valido")).willReturn(user);
        JwtAuthFilter filter = new JwtAuthFilter(jwtService, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/soporte/tickets");
        request.addHeader("Authorization", "Bearer valido");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(request.getAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE)).isEqualTo(user);
        assertThat(chain.getRequest()).isSameAs(request);
    }
}
