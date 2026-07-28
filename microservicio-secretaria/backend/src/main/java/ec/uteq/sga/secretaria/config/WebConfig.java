package ec.uteq.sga.secretaria.config;

import ec.uteq.sga.secretaria.security.CurrentUserArgumentResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.cors.origin}")
    private String corsOrigin;

    @Value("${app.frontend.dist-path}")
    private String frontendDistPath;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(corsOrigin.split(","))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE")
                .allowedHeaders("Content-Type", "Authorization");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentUserArgumentResolver());
    }

    /**
     * ResourceHttpRequestHandler rechaza un resourcePath vacio (la ruta "/") antes de invocar
     * al resolver, asi que "/" nunca llegaria al fallback de abajo. Se reenvia explicitamente
     * a /index.html, que si resuelve como un recurso estatico normal.
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
    }

    /**
     * Sirve client/dist sin copiarlo al classpath (no hay que rebuildear el backend al cambiar
     * el frontend). Cualquier ruta que no sea un archivo real ni empiece con api/ devuelve
     * index.html, replicando el catch-all SPA de Express. Los controllers @RestController
     * (mapeados con mayor precedencia que este resource handler) siempre se evaluan primero.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + frontendDistPath.replaceAll("/+$", "") + "/";
        registry.addResourceHandler("/**")
                .addResourceLocations(location)
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource baseLocation) throws IOException {
                        if (resourcePath.startsWith("api/") || resourcePath.equals("health")) {
                            return null;
                        }
                        Resource requested = super.getResource(resourcePath, baseLocation);
                        if (requested != null && requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        return new FileSystemResource(frontendDistPath.replaceAll("/+$", "") + "/index.html");
                    }
                });
    }
}
