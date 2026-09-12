package ec.uteq.sga.soporte.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WebConfigTest {

    @Test
    void corsYArgumentResolver_reflejanLaConfiguracionDeclarada() {
        WebConfig config = new WebConfig();
        ReflectionTestUtils.setField(config, "corsOrigin", "https://portal.example,https://admin.example");
        TestCorsRegistry corsRegistry = new TestCorsRegistry();
        List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();

        config.addCorsMappings(corsRegistry);
        config.addArgumentResolvers(resolvers);

        assertThat(corsRegistry.configurations()).containsKey("/**");
        assertThat(corsRegistry.configurations().get("/**").getAllowedOriginPatterns())
                .containsExactly("https://portal.example", "https://admin.example");
        assertThat(corsRegistry.configurations().get("/**").getAllowedMethods())
                .containsExactly("GET", "POST", "PUT", "PATCH", "DELETE");
        assertThat(resolvers).hasSize(1);
        assertThat(resolvers.get(0).getClass().getSimpleName()).isEqualTo("CurrentUserArgumentResolver");
    }

    private static class TestCorsRegistry extends CorsRegistry {
        java.util.Map<String, org.springframework.web.cors.CorsConfiguration> configurations() {
            return getCorsConfigurations();
        }
    }
}
