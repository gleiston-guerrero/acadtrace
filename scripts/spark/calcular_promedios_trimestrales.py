"""Calcula y persiste promedios trimestrales reales mediante Spark.

La fuente de ponderación preferida es ``sga_principal.tipos_aporte.porcentaje``
cuando esa columna exista. El respaldo compatible con el esquema actual es
``sga_docente.actividades.ponderacion``; no se aplica una regla fija 70/30.
"""

from pyspark.sql import SparkSession, functions as F

from common import execute_batch, read_query


def escala_enum(column):
    return (
        F.when(column == "A+", "A_MAS")
        .when(column == "A-", "A_MENOS")
        .when(column == "B+", "B_MAS")
        .when(column == "B-", "B_MENOS")
        .when(column == "C+", "C_MAS")
        .when(column == "C-", "C_MENOS")
        .when(column.startswith("D"), "D")
    )


def main():
    spark = SparkSession.builder.appName("sga-promedios-trimestrales").getOrCreate()

    print("[trimestrales] Lectura JDBC de escala iniciada")
    escala = read_query(
        spark,
        """
        SELECT id_ano_lectivo, id_nivel, nota_minima, nota_maxima,
               equivalente_cualitativo
        FROM sga_principal.escala_calificaciones
        """,
    ).cache()
    escala_count = escala.count()
    print(f"[trimestrales] Escala cargada/materializada: {escala_count}")

    print("[trimestrales] Lectura JDBC principal iniciada")
    datos = read_query(
        spark,
        """
        SELECT
            c.id_matricula::integer AS id_matricula,
            a.id_asignacion::integer AS id_asignacion,
            a.id_periodo::integer AS id_periodo,
            p.id_ano_lectivo::integer AS id_ano_lectivo,
            g.id_nivel::integer AS id_nivel,
            a.es_sumativa AS es_sumativa,
            c.nota::double precision AS nota,
            COALESCE(
                NULLIF(to_jsonb(ta) ->> 'porcentaje', '')::double precision,
                a.ponderacion::double precision,
                1.0::double precision
            ) AS peso
        FROM sga_docente.calificaciones c
        JOIN sga_docente.actividades a ON a.id_actividad = c.id_actividad
        JOIN sga_docente.periodos_evaluacion p ON p.id_periodo = a.id_periodo
        JOIN sga_principal.asignaciones asg ON asg.id_asignacion = a.id_asignacion
        JOIN sga_principal.grados g ON g.id_grado = asg.id_grado
        LEFT JOIN sga_principal.tipos_aporte ta
          ON ta.id_ano_lectivo = p.id_ano_lectivo
         AND lower(trim(split_part(a.nombre, ' - ', 1))) = lower(trim(ta.nombre))
         AND ta.activo = true
        WHERE c.nota IS NOT NULL
          AND COALESCE(
                NULLIF(to_jsonb(ta) ->> 'porcentaje', '')::double precision,
                a.ponderacion::double precision,
                1.0::double precision
              ) > 0
        """,
    ).cache()
    datos_count = datos.count()
    print(f"[trimestrales] Datos base cargados/materializados: {datos_count}")

    def weighted_average(condition):
        numerator = F.sum(F.when(condition, F.col("nota") * F.col("peso")))
        denominator = F.sum(F.when(condition, F.col("peso")))
        return F.round(
            F.when(denominator > F.lit(0.0), numerator / denominator),
            2,
        )

    promedios = datos.groupBy(
        "id_matricula", "id_asignacion", "id_periodo", "id_ano_lectivo", "id_nivel"
    ).agg(
        weighted_average(~F.col("es_sumativa")).alias("promedio_formativo"),
        weighted_average(F.col("es_sumativa")).alias("nota_sumativa"),
        weighted_average(F.lit(True)).alias("promedio_trimestral"),
    )

    calificadas = (
        promedios.alias("pt")
        .join(
            F.broadcast(escala).alias("ec"),
            (F.col("ec.id_ano_lectivo") == F.col("pt.id_ano_lectivo"))
            & (F.col("ec.id_nivel") == F.col("pt.id_nivel"))
            & (F.col("pt.promedio_trimestral") >= F.col("ec.nota_minima"))
            & (F.col("pt.promedio_trimestral") <= F.col("ec.nota_maxima")),
            "left",
        )
        .select(
            "pt.id_matricula",
            "pt.id_asignacion",
            "pt.id_periodo",
            "pt.promedio_formativo",
            "pt.nota_sumativa",
            "pt.promedio_trimestral",
            escala_enum(F.col("ec.equivalente_cualitativo")).alias("nota_cualitativa"),
        )
    )

    rows = [
        (
            row.id_matricula,
            row.id_asignacion,
            row.id_periodo,
            row.promedio_formativo,
            row.nota_sumativa,
            row.promedio_trimestral,
            row.nota_cualitativa,
        )
        for row in calificadas.toLocalIterator()
    ]
    print("[trimestrales] Calculo Spark terminado")
    print(f"[trimestrales] Promedios preparados: {len(rows)}")
    print("[trimestrales] Escritura JDBC iniciada")
    rows_written = execute_batch(
        spark,
        """
        INSERT INTO sga_docente.promedios_trimestrales
          (id_matricula, id_asignacion, id_periodo, promedio_formativo,
           nota_sumativa, promedio_trimestral, nota_cualitativa, calculado_en)
        VALUES (?, ?, ?, ?, ?, ?, CAST(? AS sga_docente.nota_cualitativa_t), NOW())
        ON CONFLICT (id_matricula, id_asignacion, id_periodo)
        DO UPDATE SET
          promedio_formativo = EXCLUDED.promedio_formativo,
          nota_sumativa = EXCLUDED.nota_sumativa,
          promedio_trimestral = EXCLUDED.promedio_trimestral,
          nota_cualitativa = EXCLUDED.nota_cualitativa,
          calculado_en = NOW()
        """,
        rows,
    )
    print(f"[trimestrales] Escritura JDBC terminada: {rows_written}")
    datos.unpersist()
    escala.unpersist()
    spark.stop()


if __name__ == "__main__":
    main()
