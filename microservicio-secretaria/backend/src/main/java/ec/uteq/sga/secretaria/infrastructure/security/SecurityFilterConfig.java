package ec.uteq.sga.secretaria.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.uteq.sga.secretaria.infrastructure.common.TraceIdFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityFilterConfig {

    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration() {
        FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TraceIdFilter());
        registration.addUrlPatterns("/api/secretario/*");
        registration.setOrder(0);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtAuthFilterRegistration(
            JwtService jwtService, ObjectMapper objectMapper) {
        FilterRegistrationBean<JwtAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new JwtAuthFilter(jwtService, objectMapper));
        registration.addUrlPatterns("/api/secretario/*");
        registration.setOrder(1);
        return registration;
    }
}
