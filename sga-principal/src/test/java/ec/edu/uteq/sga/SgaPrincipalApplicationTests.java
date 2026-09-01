package ec.edu.uteq.sga;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "grpc.server.port=-1",
    "spring.main.banner-mode=off"
})
class SgaPrincipalApplicationTests {

    @MockBean
    private DataSource dataSource;

    @MockBean
    private Flyway flyway;

    @Test
    void contextLoads() {
        assertNotNull(
            SgaPrincipalApplication.class.getAnnotation(SpringBootApplication.class)
        );
    }
}