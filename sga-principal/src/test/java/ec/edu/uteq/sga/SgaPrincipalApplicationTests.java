package ec.edu.uteq.sga;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SgaPrincipalApplicationTests {

    @Test
    void contextLoads() {
        assertNotNull(SgaPrincipalApplication.class.getAnnotation(SpringBootApplication.class));
    }

}
