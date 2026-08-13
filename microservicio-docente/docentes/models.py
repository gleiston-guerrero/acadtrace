from django.db import models


class TipoPeriodo(models.TextChoices):
    PRIMER_TRIMESTRE = "PRIMER_TRIMESTRE", "Primer trimestre"
    SEGUNDO_TRIMESTRE = "SEGUNDO_TRIMESTRE", "Segundo trimestre"
    TERCER_TRIMESTRE = "TERCER_TRIMESTRE", "Tercer trimestre"


TipoPeriodo.TRIMESTRE = TipoPeriodo.PRIMER_TRIMESTRE
TipoPeriodo.QUIMESTRE = TipoPeriodo.SEGUNDO_TRIMESTRE
TipoPeriodo.PARCIAL = TipoPeriodo.TERCER_TRIMESTRE


class TipoActividad(models.TextChoices):
    LECCION_ORAL = "LECCION_ORAL", "Lección oral"
    LECCION_ESCRITA = "LECCION_ESCRITA", "Lección escrita"
    TAREA = "TAREA", "Tarea"
    TALLER = "TALLER", "Taller"
    CUADERNO = "CUADERNO", "Cuaderno"
    TRABAJO_INDIVIDUAL = "TRABAJO_INDIVIDUAL", "Trabajo individual"
    EXPOSICION = "EXPOSICION", "Exposición"
    PROYECTO_INTERDISCIPLINARIO = "PROYECTO_INTERDISCIPLINARIO", "Proyecto interdisciplinario"
    EXAMEN_TRIMESTRAL = "EXAMEN_TRIMESTRAL", "Examen trimestral"


class NotaCualitativa(models.TextChoices):
    A_MAS = "A_MAS", "A+"
    A_MENOS = "A_MENOS", "A-"
    B_MAS = "B_MAS", "B+"
    B_MENOS = "B_MENOS", "B-"
    C_MAS = "C_MAS", "C+"
    C_MENOS = "C_MENOS", "C-"
    D = "D", "D"


class EstadoAsistencia(models.TextChoices):
    PRESENTE = "PRESENTE", "Presente"
    AUSENTE = "AUSENTE", "Ausente"
    JUSTIFICADO = "JUSTIFICADO", "Justificado"
    ATRASO = "ATRASO", "Atraso"


class CategoriaSeguimiento(models.TextChoices):
    ACADEMICO = "ACADEMICO", "Academico"
    CONDUCTUAL = "CONDUCTUAL", "Conductual"
    DECE = "DECE", "DECE"
    MEDICO = "MEDICO", "Medico"
    FAMILIAR = "FAMILIAR", "Familiar"
    OTRO = "OTRO", "Otro"


class PeriodoEvaluacion(models.Model):
    id_periodo = models.AutoField(primary_key=True)
    id_ano_lectivo = models.IntegerField()
    tipo = models.CharField(max_length=20, choices=TipoPeriodo.choices)
    nombre = models.CharField(max_length=40)
    fecha_inicio = models.DateField()
    fecha_fin = models.DateField()
    activo = models.BooleanField(default=True)

    class Meta:
        db_table = 'sga_docente"."periodos_evaluacion'
        ordering = ["id_ano_lectivo", "fecha_inicio"]

    def __str__(self):
        return self.nombre


class Actividad(models.Model):
    id_actividad = models.AutoField(primary_key=True)
    id_asignacion = models.IntegerField()
    id_periodo = models.ForeignKey(
        PeriodoEvaluacion,
        models.PROTECT,
        db_column="id_periodo",
        related_name="actividades",
    )
    tipo = models.CharField(max_length=30, choices=TipoActividad.choices)
    nombre = models.CharField(max_length=200)
    descripcion = models.TextField(blank=True, null=True)
    fecha_entrega = models.DateField()
    ponderacion = models.DecimalField(max_digits=5, decimal_places=2, default=0)
    nota_maxima = models.DecimalField(max_digits=4, decimal_places=2, default=10)
    es_sumativa = models.BooleanField(default=False)
    fecha_creacion = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = 'sga_docente"."actividades'
        ordering = ["-fecha_entrega", "id_actividad"]

    def __str__(self):
        return self.nombre


class Calificacion(models.Model):
    id_calificacion = models.BigAutoField(primary_key=True)
    id_actividad = models.ForeignKey(
        Actividad,
        models.CASCADE,
        db_column="id_actividad",
        related_name="calificaciones",
    )
    id_matricula = models.IntegerField()
    nota = models.DecimalField(max_digits=4, decimal_places=2)
    nota_cualitativa = models.CharField(
        max_length=7, choices=NotaCualitativa.choices, blank=True, null=True
    )
    observacion = models.TextField(blank=True, null=True)
    registrado_por = models.IntegerField(blank=True, null=True)
    fecha_registro = models.DateTimeField(auto_now_add=True)
    fecha_actualizacion = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = 'sga_docente"."calificaciones'
        ordering = ["-fecha_registro"]
        constraints = [
            models.UniqueConstraint(
                fields=["id_actividad", "id_matricula"],
                name="uq_calificacion_actividad_matricula",
            )
        ]

    def __str__(self):
        return f"{self.id_matricula} - {self.id_actividad_id}: {self.nota}"


class Asistencia(models.Model):
    id_asistencia = models.BigAutoField(primary_key=True)
    id_matricula = models.IntegerField()
    id_asignacion = models.IntegerField()
    id_periodo = models.ForeignKey(
        PeriodoEvaluacion,
        models.PROTECT,
        db_column="id_periodo",
        related_name="asistencias",
    )
    fecha = models.DateField()
    estado = models.CharField(max_length=20, choices=EstadoAsistencia.choices)
    justificacion = models.TextField(blank=True, null=True)
    registrado_por = models.IntegerField(blank=True, null=True)
    fecha_registro = models.DateTimeField(auto_now_add=True)
    fecha_actualizacion = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = 'sga_docente"."asistencias'
        ordering = ["-fecha", "id_matricula"]
        constraints = [
            models.UniqueConstraint(
                fields=["id_matricula", "id_asignacion", "id_periodo", "fecha"],
                name="uq_asistencia_matricula_asignacion_periodo_fecha",
            )
        ]


class ResumenAsistencia(models.Model):
    id_resumen = models.AutoField(primary_key=True)
    id_matricula = models.IntegerField()
    id_asignacion = models.IntegerField()
    id_periodo = models.ForeignKey(
        PeriodoEvaluacion,
        models.PROTECT,
        db_column="id_periodo",
        related_name="resumenes_asistencia",
    )
    total_presentes = models.SmallIntegerField(default=0)
    total_ausentes = models.SmallIntegerField(default=0)
    total_justificados = models.SmallIntegerField(default=0)
    total_atrasos = models.SmallIntegerField(default=0)
    calculado_en = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = 'sga_docente"."resumen_asistencia'
        constraints = [
            models.UniqueConstraint(
                fields=["id_matricula", "id_asignacion", "id_periodo"],
                name="uq_resumen_asistencia_matricula_asignacion_periodo",
            )
        ]


class PromedioTrimestral(models.Model):
    id_promedio = models.AutoField(primary_key=True)
    id_matricula = models.IntegerField()
    id_asignacion = models.IntegerField()
    id_periodo = models.ForeignKey(
        PeriodoEvaluacion,
        models.PROTECT,
        db_column="id_periodo",
        related_name="promedios_trimestrales",
    )
    promedio_formativo = models.DecimalField(max_digits=4, decimal_places=2, default=0)
    nota_sumativa = models.DecimalField(max_digits=4, decimal_places=2, default=0)
    promedio_trimestral = models.DecimalField(max_digits=4, decimal_places=2, default=0)
    nota_cualitativa = models.CharField(max_length=7, choices=NotaCualitativa.choices)
    calculado_en = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = 'sga_docente"."promedios_trimestrales'
        constraints = [
            models.UniqueConstraint(
                fields=["id_matricula", "id_asignacion", "id_periodo"],
                name="uq_promedio_trim_matricula_asignacion_periodo",
            )
        ]


class PromedioAnual(models.Model):
    id_promedio_anual = models.AutoField(primary_key=True)
    id_matricula = models.IntegerField()
    id_asignacion = models.IntegerField()
    id_ano_lectivo = models.IntegerField()
    promedio_anual = models.DecimalField(max_digits=4, decimal_places=2, default=0)
    nota_cualitativa = models.CharField(max_length=7, choices=NotaCualitativa.choices)
    registrado_por = models.IntegerField(blank=True, null=True)
    calculado_en = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = 'sga_docente"."promedios_anuales'
        constraints = [
            models.UniqueConstraint(
                fields=["id_matricula", "id_asignacion", "id_ano_lectivo"],
                name="uq_promedio_anual_matricula_asignacion_ano",
            )
        ]


class PromedioAnualDetalle(models.Model):
    id_detalle = models.AutoField(primary_key=True)
    id_promedio_anual = models.ForeignKey(
        PromedioAnual,
        models.CASCADE,
        db_column="id_promedio_anual",
        related_name="detalles",
    )
    id_promedio_trim = models.ForeignKey(
        PromedioTrimestral,
        models.PROTECT,
        db_column="id_promedio_trim",
        related_name="detalles_anuales",
    )

    class Meta:
        db_table = 'sga_docente"."promedios_anuales_detalle'
        constraints = [
            models.UniqueConstraint(
                fields=["id_promedio_anual", "id_promedio_trim"],
                name="uq_promedio_anual_detalle",
            )
        ]


class SeguimientoAcademico(models.Model):
    id_seguimiento = models.BigAutoField(primary_key=True)
    id_matricula = models.IntegerField()
    id_periodo = models.ForeignKey(
        PeriodoEvaluacion,
        models.PROTECT,
        db_column="id_periodo",
        related_name="seguimientos",
    )
    categoria = models.CharField(max_length=20, choices=CategoriaSeguimiento.choices)
    descripcion = models.TextField()
    acciones_tomadas = models.TextField(blank=True, null=True)
    requiere_followup = models.BooleanField(default=False)
    fecha_evento = models.DateField()
    registrado_por = models.IntegerField(blank=True, null=True)
    fecha_registro = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = 'sga_docente"."seguimiento_academico'
        ordering = ["-fecha_evento", "-fecha_registro"]


class Anuncio(models.Model):
    id_anuncio = models.AutoField(primary_key=True)
    id_asignacion = models.IntegerField()
    titulo = models.CharField(max_length=150, blank=True, null=True)
    contenido = models.TextField(blank=True, null=True)
    autor_id = models.IntegerField(blank=True, null=True)
    fecha = models.DateTimeField(auto_now_add=True)
    fijado = models.BooleanField(default=False)

    class Meta:
        db_table = 'sga_docente"."anuncios'
        managed = False
        ordering = ["-fijado", "-fecha"]


class Material(models.Model):
    id_material = models.AutoField(primary_key=True)
    id_asignacion = models.IntegerField()
    tipo = models.CharField(max_length=20, blank=True, null=True)
    titulo = models.CharField(max_length=150, blank=True, null=True)
    descripcion = models.TextField(blank=True, null=True)
    url = models.TextField()
    tamano_bytes = models.BigIntegerField(blank=True, null=True)
    fecha = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = 'sga_docente"."materiales'
        managed = False
        ordering = ["-fecha"]
