package ec.uteq.sga.soporte.infrastructure.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class DataSourceConfigTest {

    @Test
    void construyeUrlPostgresqlConSslYPoolEsperado() {
        DataSourceConfig config = new DataSourceConfig();
        ReflectionTestUtils.setField(config, "host", "db.local");
        ReflectionTestUtils.setField(config, "port", 5432);
        ReflectionTestUtils.setField(config, "name", "soporte");
        ReflectionTestUtils.setField(config, "user", "usuario");
        ReflectionTestUtils.setField(config, "password", "clave");
        ReflectionTestUtils.setField(config, "ssl", true);

        HikariDataSource source = (HikariDataSource) config.dataSource();
        try {
            assertThat(source.getJdbcUrl()).isEqualTo("jdbc:postgresql://db.local:5432/soporte?sslmode=require");
            assertThat(source.getUsername()).isEqualTo("usuario");
            assertThat(source.getMaximumPoolSize()).isEqualTo(10);
            assertThat(config.namedParameterJdbcTemplate(source)).isInstanceOf(NamedParameterJdbcTemplate.class);
        } finally {
            source.close();
        }
    }

    @Test
    void desactivaSslCuandoLaPropiedadEsFalsa() {
        DataSourceConfig config = new DataSourceConfig();
        ReflectionTestUtils.setField(config, "host", "localhost");
        ReflectionTestUtils.setField(config, "port", 5432);
        ReflectionTestUtils.setField(config, "name", "db");
        ReflectionTestUtils.setField(config, "user", "u");
        ReflectionTestUtils.setField(config, "password", "p");
        ReflectionTestUtils.setField(config, "ssl", false);

        HikariDataSource source = (HikariDataSource) config.dataSource();
        try {
            assertThat(source.getJdbcUrl()).endsWith("sslmode=disable");
        } finally {
            source.close();
        }
    }
}
