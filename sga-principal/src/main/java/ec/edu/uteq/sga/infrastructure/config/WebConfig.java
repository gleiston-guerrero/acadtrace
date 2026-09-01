package ec.edu.uteq.sga.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.uploads.dir:uploads}")
    private String baseDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String ruta = Paths.get(baseDir).toAbsolutePath().toUri().toString();
        if (!ruta.endsWith("/")) {
            ruta += "/";
        }
        registry.addResourceHandler("/uploads/**").addResourceLocations(ruta);
    }
}
