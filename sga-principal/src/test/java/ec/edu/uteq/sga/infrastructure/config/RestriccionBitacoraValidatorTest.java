package ec.edu.uteq.sga.infrastructure.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestriccionBitacoraValidatorTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private Statement statement;

    @Mock
    private PreparedStatement psSuperusuario;

    @Mock
    private PreparedStatement psPrivilegio;

    @Mock
    private ResultSet rsUser;

    @Mock
    private ResultSet rsSuper;

    @Mock
    private ResultSet rsPrivilegio;

    private RestriccionBitacoraValidator validator;

    @BeforeEach
    void setUp() throws Exception {
        validator = new RestriccionBitacoraValidator(dataSource, "test_user");
        when(dataSource.getConnection()).thenReturn(connection);

        // Setup current_user
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery("SELECT current_user")).thenReturn(rsUser);
        when(rsUser.next()).thenReturn(true);
        when(rsUser.getString(1)).thenReturn("test_user");

        // Setup rolsuper query
        when(connection.prepareStatement("SELECT rolsuper FROM pg_roles WHERE rolname = current_user"))
                .thenReturn(psSuperusuario);
        when(psSuperusuario.executeQuery()).thenReturn(rsSuper);
        when(rsSuper.next()).thenReturn(true);
    }

    @Test
    void verificar_ConSuperusuario_LanzaExcepcion() throws Exception {
        // Arrange
        when(rsSuper.getBoolean(1)).thenReturn(true); // Es superusuario

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> validator.verificar());
    }

    @Test
    void verificar_SinPrivilegios_NoLanzaExcepcion() throws Exception {
        // Arrange
        when(rsSuper.getBoolean(1)).thenReturn(false); // No es superusuario

        when(connection.prepareStatement("SELECT has_table_privilege(current_user, ?, ?)"))
                .thenReturn(psPrivilegio);
        when(psPrivilegio.executeQuery()).thenReturn(rsPrivilegio);
        when(rsPrivilegio.next()).thenReturn(true);
        when(rsPrivilegio.getBoolean(1)).thenReturn(false); // Falso para UPDATE, DELETE, TRUNCATE

        // Act & Assert
        assertDoesNotThrow(() -> validator.verificar());
        verify(psPrivilegio, times(3)).executeQuery();
    }

    @Test
    void verificar_ConPrivilegioUpdate_LanzaExcepcion() throws Exception {
        // Arrange
        when(rsSuper.getBoolean(1)).thenReturn(false); // No es superusuario

        when(connection.prepareStatement("SELECT has_table_privilege(current_user, ?, ?)"))
                .thenReturn(psPrivilegio);
        when(psPrivilegio.executeQuery()).thenReturn(rsPrivilegio);
        when(rsPrivilegio.next()).thenReturn(true);
        // Devolver true en el primer check (UPDATE)
        when(rsPrivilegio.getBoolean(1)).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> validator.verificar());
    }
}
