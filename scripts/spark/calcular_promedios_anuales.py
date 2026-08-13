"""Calcula promedios anuales y sus detalles a partir de tres trimestres."""

import json

from pyspark.sql import SparkSession, functions as F

from common import database_config, read_query, read_table


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


def resolver_usuario_registro(spark):
    configured = __import__("os").environ.get("SPARK_REGISTRADO_POR")
    if configured:
        return int(configured)
    rows = read_query(
        spark,
        "SELECT id_usuario FROM sga_principal.usuarios ORDER BY id_usuario LIMIT 1",
    ).collect()
    if not rows:
        raise RuntimeError("No existe un usuario para registrar los promedios anuales.")
    return int(rows[0].id_usuario)


def cerrar_statement(statement, label):
    if statement is None:
        return
    try:
        statement.close()
    except Exception as close_error:
        print(f"[anuales] No se pudo cerrar {label}: {type(close_error).__name__}")


def consultar_contador(connection, query, label):
    statement = None
    result = None
    try:
        statement = connection.createStatement()
        result = statement.executeQuery(query)
        if not result.next():
            raise RuntimeError(f"La consulta de {label} no devolvio un contador.")
        return result.getLong(1)
    finally:
        cerrar_statement(result, f"resultado de {label}")
        cerrar_statement(statement, f"consulta de {label}")


def guardar_anuales_y_detalles(spark, rows, registrado_por):
    if not rows:
        return

    annual_payload = []
    detail_payload = []
    for row in rows:
        id_matricula = int(row["id_matricula"])
        id_asignacion = int(row["id_asignacion"])
        id_ano_lectivo = int(row["id_ano_lectivo"])
        annual_payload.append(
            {
                "id_matricula": id_matricula,
                "id_asignacion": id_asignacion,
                "id_ano_lectivo": id_ano_lectivo,
                "promedio_anual": str(row["promedio_anual"]),
                "nota_cualitativa": row["nota_cualitativa"],
                "registrado_por": int(registrado_por),
            }
        )
        detail_payload.extend(
            {
                "id_matricula": id_matricula,
                "id_asignacion": id_asignacion,
                "id_ano_lectivo": id_ano_lectivo,
                "id_promedio_trim": int(id_promedio_trim),
            }
            for id_promedio_trim in row["id_promedios_trim"]
        )

    annual_json = json.dumps(annual_payload)
    detail_json = json.dumps(detail_payload)
    expected_annual_count = len(annual_payload)
    expected_detail_count = len(detail_payload)
    print(f"[anuales] Detalles calculados: {expected_detail_count}")

    config = database_config()
    jvm = spark.sparkContext._gateway.jvm
    jvm.java.lang.Class.forName(config["driver"])
    connection = None
    auto_commit = None
    create_temporales = None
    cargar_anuales = None
    cargar_detalles = None
    upsert_anuales = None
    eliminar_detalles = None
    insertar_detalles = None
    try:
        connection = jvm.java.sql.DriverManager.getConnection(
            config["url"], config["user"], config["password"]
        )
        auto_commit = connection.getAutoCommit()
        connection.setAutoCommit(False)

        create_temporales = connection.createStatement()
        create_temporales.executeUpdate(
            """
            CREATE TEMP TABLE tmp_sga_promedios_anuales (
                id_matricula integer NOT NULL,
                id_asignacion integer NOT NULL,
                id_ano_lectivo integer NOT NULL,
                promedio_anual numeric NOT NULL,
                nota_cualitativa text,
                registrado_por integer NOT NULL,
                PRIMARY KEY (id_matricula, id_asignacion, id_ano_lectivo)
            ) ON COMMIT DROP
            """
        )
        create_temporales.executeUpdate(
            """
            CREATE TEMP TABLE tmp_sga_promedios_anuales_detalle (
                id_matricula integer NOT NULL,
                id_asignacion integer NOT NULL,
                id_ano_lectivo integer NOT NULL,
                id_promedio_trim integer NOT NULL,
                PRIMARY KEY (
                    id_matricula,
                    id_asignacion,
                    id_ano_lectivo,
                    id_promedio_trim
                )
            ) ON COMMIT DROP
            """
        )

        cargar_anuales = connection.prepareStatement(
            """
            INSERT INTO tmp_sga_promedios_anuales
              (id_matricula, id_asignacion, id_ano_lectivo, promedio_anual,
               nota_cualitativa, registrado_por)
            SELECT x.id_matricula, x.id_asignacion, x.id_ano_lectivo,
                   x.promedio_anual::numeric, x.nota_cualitativa,
                   x.registrado_por
            FROM jsonb_to_recordset(CAST(? AS jsonb)) AS x(
                id_matricula integer,
                id_asignacion integer,
                id_ano_lectivo integer,
                promedio_anual text,
                nota_cualitativa text,
                registrado_por integer
            )
            """
        )
        cargar_anuales.setString(1, annual_json)
        cargar_anuales.executeUpdate()
        cerrar_statement(cargar_anuales, "carga temporal anual")
        cargar_anuales = None

        cargar_detalles = connection.prepareStatement(
            """
            INSERT INTO tmp_sga_promedios_anuales_detalle
              (id_matricula, id_asignacion, id_ano_lectivo, id_promedio_trim)
            SELECT x.id_matricula, x.id_asignacion, x.id_ano_lectivo,
                   x.id_promedio_trim
            FROM jsonb_to_recordset(CAST(? AS jsonb)) AS x(
                id_matricula integer,
                id_asignacion integer,
                id_ano_lectivo integer,
                id_promedio_trim integer
            )
            """
        )
        cargar_detalles.setString(1, detail_json)
        cargar_detalles.executeUpdate()
        cerrar_statement(cargar_detalles, "carga temporal de detalles")
        cargar_detalles = None

        annual_temp_count = consultar_contador(
            connection,
            "SELECT COUNT(*) FROM tmp_sga_promedios_anuales",
            "anuales temporales",
        )
        detail_temp_count = consultar_contador(
            connection,
            "SELECT COUNT(*) FROM tmp_sga_promedios_anuales_detalle",
            "detalles temporales",
        )
        if annual_temp_count != expected_annual_count:
            raise RuntimeError(
                "La cantidad de promedios anuales temporales no coincide con el payload."
            )
        if detail_temp_count != expected_detail_count:
            raise RuntimeError(
                "La cantidad de detalles temporales no coincide con el payload."
            )
        print("[anuales] Temporales cargadas")

        upsert_anuales = connection.createStatement()
        print("[anuales] UPSERT anual iniciado")
        annual_count = upsert_anuales.executeUpdate(
            """
            INSERT INTO sga_docente.promedios_anuales
              (id_matricula, id_asignacion, id_ano_lectivo, promedio_anual,
               nota_cualitativa, registrado_por, calculado_en)
            SELECT t.id_matricula, t.id_asignacion, t.id_ano_lectivo,
                   t.promedio_anual,
                   CAST(t.nota_cualitativa AS sga_docente.nota_cualitativa_t),
                   t.registrado_por, NOW()
            FROM tmp_sga_promedios_anuales t
            ON CONFLICT (id_matricula, id_asignacion, id_ano_lectivo)
            DO UPDATE SET
              promedio_anual = EXCLUDED.promedio_anual,
              nota_cualitativa = EXCLUDED.nota_cualitativa,
              registrado_por = EXCLUDED.registrado_por,
              calculado_en = NOW()
            """
        )
        if annual_count != expected_annual_count:
            raise RuntimeError("El UPSERT anual no proceso todos los promedios calculados.")
        print(f"[anuales] UPSERT anual terminado: {annual_count}")

        eliminar_detalles = connection.createStatement()
        deleted_count = eliminar_detalles.executeUpdate(
            f"""
            DELETE FROM sga_docente.promedios_anuales_detalle d
            USING sga_docente.promedios_anuales pa,
                  tmp_sga_promedios_anuales t
            WHERE d.id_promedio_anual = pa.id_promedio_anual
              AND pa.id_matricula = t.id_matricula
              AND pa.id_asignacion = t.id_asignacion
              AND pa.id_ano_lectivo = t.id_ano_lectivo
            """
        )
        print(f"[anuales] Detalles anteriores eliminados: {deleted_count}")

        insertar_detalles = connection.createStatement()
        detail_count = insertar_detalles.executeUpdate(
            """
            INSERT INTO sga_docente.promedios_anuales_detalle
              (id_promedio_anual, id_promedio_trim)
            SELECT pa.id_promedio_anual, td.id_promedio_trim
            FROM tmp_sga_promedios_anuales_detalle td
            JOIN sga_docente.promedios_anuales pa
              ON pa.id_matricula = td.id_matricula
             AND pa.id_asignacion = td.id_asignacion
             AND pa.id_ano_lectivo = td.id_ano_lectivo
            ON CONFLICT (id_promedio_anual, id_promedio_trim) DO NOTHING
            """
        )
        if detail_count != expected_detail_count:
            raise RuntimeError("La insercion de detalles no coincide con el payload.")
        print(f"[anuales] Detalles escritos: {detail_count}")

        inconsistent_count = consultar_contador(
            connection,
            """
            SELECT COUNT(*)
            FROM (
                SELECT pa.id_promedio_anual
                FROM sga_docente.promedios_anuales pa
                JOIN tmp_sga_promedios_anuales t
                  ON pa.id_matricula = t.id_matricula
                 AND pa.id_asignacion = t.id_asignacion
                 AND pa.id_ano_lectivo = t.id_ano_lectivo
                LEFT JOIN sga_docente.promedios_anuales_detalle d
                  ON d.id_promedio_anual = pa.id_promedio_anual
                GROUP BY pa.id_promedio_anual
                HAVING COUNT(d.id_promedio_trim) <> 3
            ) inconsistentes
            """,
            "validacion de tres trimestres",
        )
        if inconsistent_count != 0:
            raise RuntimeError(
                f"La validacion encontro {inconsistent_count} promedios sin tres detalles."
            )
        print("[anuales] Validacion 3 trimestres: OK")
        connection.commit()
        print("[anuales] Transaccion confirmada")
    except Exception:
        try:
            if connection is not None and not connection.isClosed():
                connection.rollback()
        except Exception as rollback_error:
            print(
                "[anuales] Rollback no pudo ejecutarse: "
                f"{type(rollback_error).__name__}"
            )
        raise
    finally:
        cerrar_statement(insertar_detalles, "insert de detalles")
        cerrar_statement(eliminar_detalles, "delete de detalles")
        cerrar_statement(upsert_anuales, "upsert anual")
        cerrar_statement(cargar_detalles, "carga temporal de detalles")
        cerrar_statement(cargar_anuales, "carga temporal anual")
        cerrar_statement(create_temporales, "creacion de temporales")
        try:
            if connection is not None and not connection.isClosed():
                if auto_commit is not None:
                    connection.setAutoCommit(auto_commit)
                connection.close()
        except Exception as close_error:
            print(
                "[anuales] Cierre JDBC no pudo completarse: "
                f"{type(close_error).__name__}"
            )


def main():
    spark = SparkSession.builder.appName("sga-promedios-anuales").getOrCreate()
    trimestrales = read_table(spark, "sga_docente.promedios_trimestrales").alias("pt")
    periodos = read_table(spark, "sga_docente.periodos_evaluacion").alias("p")
    asignaciones = read_table(spark, "sga_principal.asignaciones").alias("a")
    grados = read_table(spark, "sga_principal.grados").alias("g")
    escala = read_table(spark, "sga_principal.escala_calificaciones").alias("ec")

    base = (
        trimestrales.join(periodos, F.col("pt.id_periodo") == F.col("p.id_periodo"))
        .join(asignaciones, F.col("pt.id_asignacion") == F.col("a.id_asignacion"))
        .join(grados, F.col("a.id_grado") == F.col("g.id_grado"))
        .select(
            F.col("pt.id_promedio").alias("id_promedio_trim"),
            F.col("pt.id_matricula"),
            F.col("pt.id_asignacion"),
            F.col("p.id_ano_lectivo"),
            F.col("g.id_nivel"),
            F.col("pt.promedio_trimestral").cast("double").alias("promedio_trimestral"),
        )
    )

    anuales = (
        base.groupBy("id_matricula", "id_asignacion", "id_ano_lectivo", "id_nivel")
        .agg(
            F.countDistinct("id_promedio_trim").alias("cantidad_trimestres"),
            F.round(F.avg("promedio_trimestral"), 2).alias("promedio_anual"),
            F.collect_list("id_promedio_trim").alias("id_promedios_trim"),
        )
        .filter(F.col("cantidad_trimestres") == 3)
        .alias("pa")
    )

    resultados = (
        anuales.join(
            escala,
            (F.col("ec.id_ano_lectivo") == F.col("pa.id_ano_lectivo"))
            & (F.col("ec.id_nivel") == F.col("pa.id_nivel"))
            & (F.col("pa.promedio_anual") >= F.col("ec.nota_minima"))
            & (F.col("pa.promedio_anual") <= F.col("ec.nota_maxima")),
            "left",
        )
        .select(
            "pa.id_matricula",
            "pa.id_asignacion",
            "pa.id_ano_lectivo",
            "pa.promedio_anual",
            "pa.id_promedios_trim",
            escala_enum(F.col("ec.equivalente_cualitativo")).alias("nota_cualitativa"),
        )
    )

    rows = [row.asDict(recursive=True) for row in resultados.toLocalIterator()]
    print(f"[anuales] Promedios calculados: {len(rows)}")
    guardar_anuales_y_detalles(spark, rows, resolver_usuario_registro(spark))
    spark.stop()


if __name__ == "__main__":
    main()
