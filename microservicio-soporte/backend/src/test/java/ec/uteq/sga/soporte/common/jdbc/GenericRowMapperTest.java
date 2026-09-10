package ec.uteq.sga.soporte.common.jdbc;

import org.junit.jupiter.api.Test;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class GenericRowMapperTest {

    @Test
    void convierteFechasYArregloSqlAValoresSerializables() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        ResultSetMetaData metadata = mock(ResultSetMetaData.class);
        LocalDate date = LocalDate.of(2026, 9, 10);
        LocalDateTime timestamp = LocalDateTime.of(2026, 9, 10, 8, 30);
        OffsetDateTime timestampTz = OffsetDateTime.parse("2026-09-10T08:30:00-05:00");
        Array array = mock(Array.class);
        given(resultSet.getMetaData()).willReturn(metadata);
        given(metadata.getColumnCount()).willReturn(4);
        given(metadata.getColumnLabel(1)).willReturn("fecha");
        given(metadata.getColumnLabel(2)).willReturn("creado");
        given(metadata.getColumnLabel(3)).willReturn("actualizado");
        given(metadata.getColumnLabel(4)).willReturn("roles");
        given(metadata.getColumnType(1)).willReturn(Types.DATE);
        given(metadata.getColumnType(2)).willReturn(Types.TIMESTAMP);
        given(metadata.getColumnType(3)).willReturn(Types.TIMESTAMP_WITH_TIMEZONE);
        given(metadata.getColumnType(4)).willReturn(Types.ARRAY);
        given(resultSet.getObject(1, LocalDate.class)).willReturn(date);
        given(resultSet.getObject(2, LocalDateTime.class)).willReturn(timestamp);
        given(resultSet.getObject(3, OffsetDateTime.class)).willReturn(timestampTz);
        given(resultSet.getArray(4)).willReturn(array);
        given(array.getArray()).willReturn(new String[]{"DIRECTOR", "SOPORTE_TECNICO"});

        Map<String, Object> row = GenericRowMapper.INSTANCE.mapRow(resultSet, 1);

        assertThat(row).containsEntry("fecha", date).containsEntry("creado", timestamp)
                .containsEntry("actualizado", timestampTz).containsEntry("roles", java.util.List.of("DIRECTOR", "SOPORTE_TECNICO"));
    }

    @Test
    void arregloNuloYArregloPrimitivo_seMantienenSinTransformacionIncorrecta() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        ResultSetMetaData metadata = mock(ResultSetMetaData.class);
        Array primitiveArray = mock(Array.class);
        int[] values = {1, 2};
        given(resultSet.getMetaData()).willReturn(metadata);
        given(metadata.getColumnCount()).willReturn(2);
        given(metadata.getColumnLabel(1)).willReturn("vacio");
        given(metadata.getColumnLabel(2)).willReturn("numeros");
        given(metadata.getColumnType(1)).willReturn(Types.ARRAY);
        given(metadata.getColumnType(2)).willReturn(Types.ARRAY);
        given(resultSet.getArray(1)).willReturn(null);
        given(resultSet.getArray(2)).willReturn(primitiveArray);
        given(primitiveArray.getArray()).willReturn(values);

        Map<String, Object> row = GenericRowMapper.INSTANCE.mapRow(resultSet, 1);

        assertThat(row.get("vacio")).isNull();
        assertThat(row.get("numeros")).isSameAs(values);
    }
}
