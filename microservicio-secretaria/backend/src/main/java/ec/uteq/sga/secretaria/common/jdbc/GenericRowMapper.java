package ec.uteq.sga.secretaria.common.jdbc;

import org.springframework.jdbc.core.RowMapper;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RowMapper generico para queries con columnas dinamicas (SELECT e.*, joins...).
 * A diferencia del ColumnMapRowMapper por defecto de Spring, convierte explicitamente:
 * - DATE/TIMESTAMP -> LocalDate/LocalDateTime/OffsetDateTime (para que Jackson serialice ISO-8601)
 * - java.sql.Array (ej. array_agg de roles) -> List<Object>
 * Los tipos enum custom de Postgres (genero_t, estado_matricula_t) llegan como String sin tratamiento especial.
 */
public class GenericRowMapper implements RowMapper<Map<String, Object>> {

    public static final GenericRowMapper INSTANCE = new GenericRowMapper();

    @Override
    public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 1; i <= columnCount; i++) {
            row.put(meta.getColumnLabel(i), extractValue(rs, meta, i));
        }
        return row;
    }

    private Object extractValue(ResultSet rs, ResultSetMetaData meta, int index) throws SQLException {
        int type = meta.getColumnType(index);
        switch (type) {
            case Types.DATE:
                return rs.getObject(index, LocalDate.class);
            case Types.TIMESTAMP:
            case Types.TIMESTAMP_WITH_TIMEZONE:
                // pgjdbc reporta "timestamptz" como Types.TIMESTAMP (no TIMESTAMP_WITH_TIMEZONE) segun la
                // version del driver, asi que el codigo JDBC no alcanza para distinguirlo: hay que mirar el
                // nombre real de tipo de Postgres para no pedirle LocalDateTime a una columna con offset.
                return "timestamptz".equalsIgnoreCase(meta.getColumnTypeName(index))
                        ? rs.getObject(index, OffsetDateTime.class)
                        : rs.getObject(index, LocalDateTime.class);
            case Types.ARRAY:
                Array array = rs.getArray(index);
                if (array == null) return null;
                Object javaArray = array.getArray();
                if (javaArray instanceof Object[] objects) {
                    return Arrays.asList(objects);
                }
                return javaArray;
            default:
                return rs.getObject(index);
        }
    }
}
