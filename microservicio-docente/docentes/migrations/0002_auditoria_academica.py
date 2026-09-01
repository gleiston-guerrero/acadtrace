from django.db import migrations, models


def crear_estado_inicial(apps, _schema_editor):
    apps.get_model("docentes", "EstadoCadenaAuditoria").objects.get_or_create(id_estado=1)


def eliminar_estado_inicial(apps, _schema_editor):
    apps.get_model("docentes", "EstadoCadenaAuditoria").objects.filter(id_estado=1).delete()


class Migration(migrations.Migration):
    dependencies = [("docentes", "0001_initial")]

    operations = [
        migrations.CreateModel(
            name="Anuncio",
            fields=[
                ("id_anuncio", models.AutoField(primary_key=True, serialize=False)),
                ("id_asignacion", models.IntegerField()),
                ("titulo", models.CharField(blank=True, max_length=150, null=True)),
                ("contenido", models.TextField(blank=True, null=True)),
                ("autor_id", models.IntegerField(blank=True, null=True)),
                ("fecha", models.DateTimeField(auto_now_add=True)),
                ("fijado", models.BooleanField(default=False)),
            ],
            options={"db_table": 'sga_docente"."anuncios', "ordering": ["-fijado", "-fecha"], "managed": False},
        ),
        migrations.CreateModel(
            name="Material",
            fields=[
                ("id_material", models.AutoField(primary_key=True, serialize=False)),
                ("id_asignacion", models.IntegerField()),
                ("tipo", models.CharField(blank=True, max_length=20, null=True)),
                ("titulo", models.CharField(blank=True, max_length=150, null=True)),
                ("descripcion", models.TextField(blank=True, null=True)),
                ("url", models.TextField()),
                ("tamano_bytes", models.BigIntegerField(blank=True, null=True)),
                ("fecha", models.DateTimeField(auto_now_add=True)),
            ],
            options={"db_table": 'sga_docente"."materiales', "ordering": ["-fecha"], "managed": False},
        ),
        migrations.CreateModel(
            name="EstadoCadenaAuditoria",
            fields=[
                ("id_estado", models.PositiveSmallIntegerField(default=1, primary_key=True, serialize=False)),
                ("ultimo_hash", models.CharField(blank=True, max_length=64, null=True)),
                ("ultimo_lamport", models.BigIntegerField(default=0)),
                ("reloj_vectorial", models.JSONField(default=dict)),
            ],
            options={"db_table": "estado_cadena_auditoria"},
        ),
        migrations.CreateModel(
            name="EventoAuditoria",
            fields=[
                ("id_evento", models.BigAutoField(primary_key=True, serialize=False)),
                ("tipo_evento", models.CharField(max_length=80)),
                ("entidad", models.CharField(max_length=80)),
                ("entidad_id", models.CharField(max_length=100)),
                ("operacion", models.CharField(max_length=30)),
                ("actor_id", models.IntegerField(blank=True, null=True)),
                ("timestamp", models.DateTimeField()),
                ("payload_canonico", models.TextField()),
                ("modo", models.CharField(max_length=2)),
                ("hash_anterior", models.CharField(blank=True, max_length=64, null=True)),
                ("hash_actual", models.CharField(blank=True, max_length=64, null=True)),
                ("reloj_lamport", models.BigIntegerField(blank=True, null=True)),
                ("reloj_vectorial", models.JSONField(blank=True, null=True)),
                ("estado_reconciliacion", models.CharField(default="NO_APLICA", max_length=20)),
            ],
            options={"db_table": "eventos_auditoria", "ordering": ["id_evento"]},
        ),
        migrations.RunPython(crear_estado_inicial, eliminar_estado_inicial),
        # La migración inicial quedó desalineada de modelos ya desplegados. Se
        # reconcilia solo el estado de Django, sin ALTER/RENAME destructivos.
        migrations.SeparateDatabaseAndState(state_operations=[
            migrations.AlterField(model_name="actividad", name="tipo", field=models.CharField(choices=[("LECCION_ORAL", "Lección oral"), ("LECCION_ESCRITA", "Lección escrita"), ("TAREA", "Tarea"), ("TALLER", "Taller"), ("CUADERNO", "Cuaderno"), ("TRABAJO_INDIVIDUAL", "Trabajo individual"), ("EXPOSICION", "Exposición"), ("PROYECTO_INTERDISCIPLINARIO", "Proyecto interdisciplinario"), ("EXAMEN_TRIMESTRAL", "Examen trimestral")], max_length=30)),
            migrations.AlterField(model_name="calificacion", name="nota_cualitativa", field=models.CharField(blank=True, choices=[("A_MAS", "A+"), ("A_MENOS", "A-"), ("B_MAS", "B+"), ("B_MENOS", "B-"), ("C_MAS", "C+"), ("C_MENOS", "C-"), ("D", "D")], max_length=7, null=True)),
            migrations.AlterField(model_name="periodoevaluacion", name="tipo", field=models.CharField(choices=[("PRIMER_TRIMESTRE", "Primer trimestre"), ("SEGUNDO_TRIMESTRE", "Segundo trimestre"), ("TERCER_TRIMESTRE", "Tercer trimestre")], max_length=20)),
            migrations.AlterField(model_name="promedioanual", name="nota_cualitativa", field=models.CharField(choices=[("A_MAS", "A+"), ("A_MENOS", "A-"), ("B_MAS", "B+"), ("B_MENOS", "B-"), ("C_MAS", "C+"), ("C_MENOS", "C-"), ("D", "D")], max_length=7)),
            migrations.AlterField(model_name="promediotrimestral", name="nota_cualitativa", field=models.CharField(choices=[("A_MAS", "A+"), ("A_MENOS", "A-"), ("B_MAS", "B+"), ("B_MENOS", "B-"), ("C_MAS", "C+"), ("C_MENOS", "C-"), ("D", "D")], max_length=7)),
            migrations.AlterField(model_name="seguimientoacademico", name="categoria", field=models.CharField(choices=[("ACADEMICO", "Academico"), ("CONDUCTUAL", "Conductual"), ("DECE", "DECE"), ("MEDICO", "Medico"), ("FAMILIAR", "Familiar"), ("OTRO", "Otro")], max_length=20)),
            migrations.AlterModelTable(name="actividad", table='sga_docente"."actividades'),
            migrations.AlterModelTable(name="asistencia", table='sga_docente"."asistencias'),
            migrations.AlterModelTable(name="calificacion", table='sga_docente"."calificaciones'),
            migrations.AlterModelTable(name="periodoevaluacion", table='sga_docente"."periodos_evaluacion'),
            migrations.AlterModelTable(name="promedioanual", table='sga_docente"."promedios_anuales'),
            migrations.AlterModelTable(name="promedioanualdetalle", table='sga_docente"."promedios_anuales_detalle'),
            migrations.AlterModelTable(name="promediotrimestral", table='sga_docente"."promedios_trimestrales'),
            migrations.AlterModelTable(name="resumenasistencia", table='sga_docente"."resumen_asistencia'),
            migrations.AlterModelTable(name="seguimientoacademico", table='sga_docente"."seguimiento_academico'),
        ]),
    ]
