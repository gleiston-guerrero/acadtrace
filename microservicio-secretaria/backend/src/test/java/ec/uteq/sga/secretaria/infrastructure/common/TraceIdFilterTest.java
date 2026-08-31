package ec.uteq.sga.secretaria.infrastructure.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("Pruebas Unitarias: TraceIdFilter (Microservicio Secretaría)")
class TraceIdFilterTest {

    @Test
    @DisplayName("Propaga X-Trace-Id existente de la cabecera")
    void test_doFilter_withExistingHeader() throws ServletException, IOException {
        TraceIdFilter filter = new TraceIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Trace-Id", "existing-trace-id-1234");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("X-Trace-Id")).isEqualTo("existing-trace-id-1234");
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("Genera nuevo X-Trace-Id si la cabecera no está presente")
    void test_doFilter_withoutHeader_generatesNewTraceId() throws ServletException, IOException {
        TraceIdFilter filter = new TraceIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        String traceId = response.getHeader("X-Trace-Id");
        assertThat(traceId).isNotBlank();
        verify(chain).doFilter(request, response);
    }
}
