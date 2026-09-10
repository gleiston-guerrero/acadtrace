package ec.uteq.sga.soporte.infrastructure.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

class DataSourceConfigTest {

    @Test
    void dataSource_conSslConstruyeUrlYConservaLimitesDelPool() {
        DataSourceConfig config = configured(true);
        HikariDataSource dataSource = (HikariDataSource) config.dataSource();
        try {
            assertThat(dataSource.getJdbcUrl()).isEqualTo("jdbc:postgresql://db.example:5432/sga?sslmode=require");
            assertThat(dataSource.getUsername()).isEqualTo("soporte");
            assertThat(dataSource.getMaximumPoolSize()).isEqualTo(10);
            assertThat(dataSource.getMinimumIdle()).isEqualTo(2);
            assertThat(dataSource.getConnectionTimeout()).isEqualTo(5_000L);
            assertThat(dataSource.getIdleTimeout()).isEqualTo(30_000L);
        } finally {
            dataSource.close();
        }
    }

    @Test
    void dataSource_sinSslUsaModoDisableYProveeTemplate() {
        DataSourceConfig config = configured(false);
        HikariDataSource dataSource = (HikariDataSource) config.dataSource();
        try {
            assertThat(dataSource.getJdbcUrl()).endsWith("sslmode=disable");
            NamedParameterJdbcTemplate template = config.namedParameterJdbcTemplate(dataSource);
            assertThat(template.getJdbcTemplate().getDataSource()).isSameAs((DataSource) dataSource);
        } finally {
            dataSource.close();
        }
    }

    private DataSourceConfig configured(boolean ssl) {
        DataSourceConfig config = new DataSourceConfig();
        ReflectionTestUtils.setField(config, "host", "db.example");
        ReflectionTestUtils.setField(config, "port", 5432);
        ReflectionTestUtils.setField(config, "name", "sga");
        ReflectionTestUtils.setField(config, "user", "soporte");
        ReflectionTestUtils.setField(config, "password", "test-password");
        ReflectionTestUtils.setField(config, "ssl", ssl);
        return config;
    }
}
