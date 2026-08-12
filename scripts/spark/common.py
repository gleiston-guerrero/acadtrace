"""Utilidades compartidas para los procesos Spark de E3.

Las credenciales nunca se guardan en el repositorio: el contenedor recibe las
variables DB_* definidas por el entorno de despliegue.
"""

import os
from decimal import Decimal


def database_config():
    required = ("DB_HOST", "DB_PORT", "DB_NAME", "DB_USER", "DB_PASSWORD")
    missing = [name for name in required if not os.environ.get(name)]
    if missing:
        raise RuntimeError(
            "Faltan variables de conexión PostgreSQL: " + ", ".join(missing)
        )

    return {
        "url": os.environ.get(
            "DB_JDBC_URL",
            "jdbc:postgresql://{host}:{port}/{database}".format(
                host=os.environ["DB_HOST"],
                port=os.environ["DB_PORT"],
                database=os.environ["DB_NAME"],
            ),
        ),
        "user": os.environ["DB_USER"],
        "password": os.environ["DB_PASSWORD"],
        "driver": "org.postgresql.Driver",
    }


def read_table(spark, table_name):
    config = database_config()
    return (
        spark.read.format("jdbc")
        .option("url", config["url"])
        .option("dbtable", table_name)
        .option("user", config["user"])
        .option("password", config["password"])
        .option("driver", config["driver"])
        .load()
    )


def read_query(spark, query):
    config = database_config()
    return (
        spark.read.format("jdbc")
        .option("url", config["url"])
        .option("dbtable", f"({query}) AS source_query")
        .option("user", config["user"])
        .option("password", config["password"])
        .option("driver", config["driver"])
        .load()
    )


def _connection(spark):
    config = database_config()
    jvm = spark.sparkContext._gateway.jvm
    jvm.java.lang.Class.forName(config["driver"])
    return jvm.java.sql.DriverManager.getConnection(
        config["url"], config["user"], config["password"]
    )


def execute_batch(spark, statement, rows, batch_size=1000):
    """Ejecuta batches JDBC en una unica transaccion PostgreSQL."""
    connection = _connection(spark)
    auto_commit = connection.getAutoCommit()
    prepared = None
    rows_written = 0
    pending_rows = 0
    try:
        connection.setAutoCommit(False)
        prepared = connection.prepareStatement(statement)
        for row in rows:
            for index, value in enumerate(row, start=1):
                if value is None:
                    prepared.setObject(index, None)
                elif isinstance(value, Decimal):
                    prepared.setBigDecimal(
                        index, spark.sparkContext._gateway.jvm.java.math.BigDecimal(str(value))
                    )
                else:
                    prepared.setObject(index, value)
            prepared.addBatch()
            pending_rows += 1
            rows_written += 1
            if pending_rows == batch_size:
                prepared.executeBatch()
                prepared.clearBatch()
                pending_rows = 0
        if pending_rows:
            prepared.executeBatch()
        connection.commit()
        return rows_written
    except Exception:
        connection.rollback()
        raise
    finally:
        try:
            if prepared is not None:
                prepared.close()
        finally:
            try:
                connection.setAutoCommit(auto_commit)
            finally:
                connection.close()


def query_rows(spark, statement):
    connection = _connection(spark)
    try:
        cursor = connection.createStatement()
        result = cursor.executeQuery(statement)
        rows = []
        columns = result.getMetaData().getColumnCount()
        while result.next():
            rows.append(tuple(result.getObject(index) for index in range(1, columns + 1)))
        cursor.close()
        return rows
    finally:
        connection.close()
