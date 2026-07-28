package ec.uteq.sga.soporte.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Canal gRPC compartido hacia sga-principal. usePlaintext() porque la
 * comunicacion entre microservicios es interna (sin TLS); si en produccion
 * se expone gRPC hacia afuera, cambiar a TLS.
 */
@Configuration
public class GrpcChannelConfig {

    @Value("${principal.grpc.host}")
    private String host;

    @Value("${principal.grpc.port}")
    private int port;

    private ManagedChannel channel;

    @Bean
    public ManagedChannel principalGrpcChannel() {
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        return channel;
    }

    @PreDestroy
    public void cerrarCanal() {
        if (channel != null) {
            channel.shutdown();
        }
    }
}
