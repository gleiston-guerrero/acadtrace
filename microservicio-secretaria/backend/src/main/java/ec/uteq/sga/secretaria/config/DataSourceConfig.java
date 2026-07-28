package ec.uteq.sga.secretaria.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

/**
 * DB_SSL es booleano en .env pero JDBC necesita sslmode=require|disable en la URL,
 * por eso se arma a mano en vez de con un placeholder simple de application.properties.
 */
@Configuration
public class DataSourceConfig {

    @Value("${db.host}")
    private String host;

    @Value("${db.port}")
    private int port;

    @Value("${db.name}")
    private String name;

    @Value("${db.user}")
    private String user;

    @Value("${db.password}")
    private String password;

    @Value("${db.ssl}")
    private boolean ssl;

    @Bean
    public DataSource dataSource() {
        String sslmode = ssl ? "require" : "disable";
        String url = "jdbc:postgresql://%s:%d/%s?sslmode=%s".formatted(host, port, name, sslmode);

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(user);
        ds.setPassword(password);
        ds.setMaximumPoolSize(10);
        ds.setMinimumIdle(2);
        ds.setConnectionTimeout(5000);
        ds.setIdleTimeout(30000);
        return ds;
    }

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }
}
