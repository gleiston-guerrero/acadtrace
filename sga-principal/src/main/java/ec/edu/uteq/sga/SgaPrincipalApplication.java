package ec.edu.uteq.sga;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@ComponentScan(excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "ec\\.edu\\.uteq\\.sga\\.grpc\\..*"
))
@EnableAsync
public class SgaPrincipalApplication {

    public static void main(String[] args) {
        SpringApplication.run(SgaPrincipalApplication.class, args);
    }
}
