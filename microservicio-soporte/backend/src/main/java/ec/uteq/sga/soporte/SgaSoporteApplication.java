package ec.uteq.sga.soporte;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SgaSoporteApplication {

    public static void main(String[] args) {
        SpringApplication.run(SgaSoporteApplication.class, args);
    }
}