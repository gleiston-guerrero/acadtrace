package ec.edu.uteq.sga.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Component
@Profile("!test")
public class RestriccionBitacoraValidator {

    private static final Logger log = LoggerFactory.getLogger(RestriccionBitacoraValidator.class);
    private static final String TABLA_BITACORA = "sga_principal.auditoria";

    private final DataSource dataSource;
    private final String usuarioConfigurado;

    public RestriccionBitacoraValidator(DataSource dataSource,
                                        @Value("${spring.datasource.username}") String usuarioConfigurado) {
        this.dataSource = dataSource;
        this.usuarioConfigurado = usuarioConfigurado;
    }

    @PostConstruct
    void verificar() {
        try (Connection con = dataSource.getConnection()) {
            String usuarioEfectivo = leerUsuarioEfectivo(con);
            if (esSuperusuario(con)) {
                log.warn("Validacion de restriccion de bitacora omitida: el usuario efectivo {} es superusuario. " +
                        "Este arranque no acredita la restriccion.", usuarioEfectivo);
                return;
            }
            comprobarSinPrivilegio(con, usuarioEfectivo, "UPDATE");
            comprobarSinPrivilegio(con, usuarioEfectivo, "DELETE");
            comprobarSinPrivilegio(con, usuarioEfectivo, "TRUNCATE");
            log.info("Verificado: usuario {} no puede modificar la bitacora", usuarioEfectivo);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo verificar la restriccion de bitacora", e);
        }
    }

    private String leerUsuarioEfectivo(Connection con) throws SQLException {
        try (Statement s = con.createStatement();
             ResultSet rs = s.executeQuery("SELECT current_user")) {
            rs.next();
            return rs.getString(1);
        }
    }

    private boolean esSuperusuario(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT rolsuper FROM pg_roles WHERE rolname = current_user")) {
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        }
    }

    private void comprobarSinPrivilegio(Connection con, String usuario, String privilegio) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT has_table_privilege(current_user, ?, ?)")) {
            ps.setString(1, TABLA_BITACORA);
            ps.setString(2, privilegio);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getBoolean(1)) {
                    throw new IllegalStateException(
                            "El usuario " + usuario + " tiene privilegio " + privilegio +
                            " sobre " + TABLA_BITACORA + ". La restriccion no se cumple.");
                }
            }
        }
    }
}
