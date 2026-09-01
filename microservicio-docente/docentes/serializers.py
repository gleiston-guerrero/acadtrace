import mimetypes
from pathlib import Path
from uuid import uuid4

from django.db import DatabaseError, connection, transaction
from django.core.files.storage import default_storage
from rest_framework import serializers
from django.db.models import Sum

from .models import (
    Actividad,
    Anuncio,
    Asistencia,
    Calificacion,
    Material,
    PeriodoEvaluacion,
    PromedioAnual,
    PromedioAnualDetalle,
    PromedioTrimestral,
    ResumenAsistencia,
    SeguimientoAcademico,
)
from .services import convertir_nota_cualitativa
from .auditoria import auditar_evento
from .auditoria.payloads import payload_instancia


class PeriodoEvaluacionSerializer(serializers.ModelSerializer):
    class Meta:
        model = PeriodoEvaluacion
        fields = "__all__"


class ActividadSerializer(serializers.ModelSerializer):
    class Meta:
        model = Actividad
        fields = "__all__"

    def validate(self, attrs):
        asignacion = attrs.get("id_asignacion", getattr(self.instance, "id_asignacion", None))
        periodo = attrs.get("id_periodo", getattr(self.instance, "id_periodo", None))
        es_sumativa = attrs.get("es_sumativa", getattr(self.instance, "es_sumativa", False))
        ponderacion = attrs.get("ponderacion", getattr(self.instance, "ponderacion", 0))
        if ponderacion is None or ponderacion < 0:
            raise serializers.ValidationError({"ponderacion": "La ponderación no puede ser negativa."})
        if asignacion and periodo:
            consulta = Actividad.objects.filter(
                id_asignacion=asignacion,
                id_periodo=periodo,
                es_sumativa=es_sumativa,
            )
            if self.instance:
                consulta = consulta.exclude(pk=self.instance.pk)
            total = consulta.aggregate(total=Sum("ponderacion"))["total"] or 0
            limite = 30 if es_sumativa else 70
            if total + ponderacion > limite:
                categoria = "sumativa" if es_sumativa else "formativa"
                raise serializers.ValidationError({
                    "ponderacion": f"La ponderación total {categoria} no puede superar {limite}%."
                })
        return attrs

    def create(self, validated_data):
        with transaction.atomic():
            instancia = super().create(validated_data)
            auditar_evento(
                tipo_evento="ACTIVIDAD_CREADA", entidad="Actividad",
                entidad_id=instancia.pk, operacion="CREAR", actor_id=None,
                payload=payload_instancia(instancia),
            )
            return instancia

    def update(self, instance, validated_data):
        with transaction.atomic():
            instancia = super().update(instance, validated_data)
            auditar_evento(
                tipo_evento="ACTIVIDAD_ACTUALIZADA", entidad="Actividad",
                entidad_id=instancia.pk, operacion="ACTUALIZAR", actor_id=None,
                payload=payload_instancia(instancia),
            )
            return instancia


class CalificacionSerializer(serializers.ModelSerializer):
    nivel = serializers.CharField(write_only=True, required=False, default="EGB")

    class Meta:
        model = Calificacion
        fields = "__all__"
        read_only_fields = ["nota_cualitativa", "fecha_registro", "fecha_actualizacion"]

    def validate(self, attrs):
        actividad = attrs.get("id_actividad") or getattr(self.instance, "id_actividad", None)
        nota = attrs.get("nota") or getattr(self.instance, "nota", None)
        if actividad and nota is not None and nota > actividad.nota_maxima:
            raise serializers.ValidationError(
                {"nota": "La nota no puede superar la nota maxima de la actividad."}
            )
        return attrs

    def create(self, validated_data):
        with transaction.atomic():
            nivel = validated_data.pop("nivel", "EGB")
            validated_data["nota_cualitativa"] = convertir_nota_cualitativa(
                validated_data["nota"], nivel
            )
            instancia = super().create(validated_data)
            auditar_evento(
                tipo_evento="CALIFICACION_CREADA", entidad="Calificacion",
                entidad_id=instancia.pk, operacion="CREAR",
                actor_id=instancia.registrado_por, payload=payload_instancia(instancia),
            )
            return instancia

    def update(self, instance, validated_data):
        with transaction.atomic():
            nivel = validated_data.pop("nivel", "EGB")
            if "nota" in validated_data:
                validated_data["nota_cualitativa"] = convertir_nota_cualitativa(
                    validated_data["nota"], nivel
                )
            instancia = super().update(instance, validated_data)
            auditar_evento(
                tipo_evento="CALIFICACION_ACTUALIZADA", entidad="Calificacion",
                entidad_id=instancia.pk, operacion="ACTUALIZAR",
                actor_id=instancia.registrado_por, payload=payload_instancia(instancia),
            )
            return instancia


class AsistenciaSerializer(serializers.ModelSerializer):
    class Meta:
        model = Asistencia
        fields = "__all__"

    def create(self, validated_data):
        with transaction.atomic():
            instancia = super().create(validated_data)
            auditar_evento(
                tipo_evento="ASISTENCIA_REGISTRADA", entidad="Asistencia",
                entidad_id=instancia.pk, operacion="CREAR",
                actor_id=instancia.registrado_por, payload=payload_instancia(instancia),
            )
            return instancia

    def update(self, instance, validated_data):
        with transaction.atomic():
            instancia = super().update(instance, validated_data)
            auditar_evento(
                tipo_evento="ASISTENCIA_ACTUALIZADA", entidad="Asistencia",
                entidad_id=instancia.pk, operacion="ACTUALIZAR",
                actor_id=instancia.registrado_por, payload=payload_instancia(instancia),
            )
            return instancia


class ResumenAsistenciaSerializer(serializers.ModelSerializer):
    class Meta:
        model = ResumenAsistencia
        fields = "__all__"
        read_only_fields = ["calculado_en"]


class PromedioTrimestralSerializer(serializers.ModelSerializer):
    class Meta:
        model = PromedioTrimestral
        fields = "__all__"
        read_only_fields = [
            "promedio_formativo",
            "nota_sumativa",
            "promedio_trimestral",
            "nota_cualitativa",
            "calculado_en",
        ]


class PromedioAnualSerializer(serializers.ModelSerializer):
    class Meta:
        model = PromedioAnual
        fields = "__all__"
        read_only_fields = ["promedio_anual", "nota_cualitativa", "calculado_en"]


class PromedioAnualDetalleSerializer(serializers.ModelSerializer):
    class Meta:
        model = PromedioAnualDetalle
        fields = "__all__"


class SeguimientoAcademicoSerializer(serializers.ModelSerializer):
    registrado_por = serializers.IntegerField(required=True, allow_null=False)

    class Meta:
        model = SeguimientoAcademico
        fields = "__all__"
        read_only_fields = ["id_seguimiento", "fecha_registro"]

    def validate_registrado_por(self, value):
        try:
            with connection.cursor() as cursor:
                cursor.execute(
                    "SELECT EXISTS ("
                    "SELECT 1 FROM sga_principal.usuarios WHERE id_usuario = %s"
                    ")",
                    [value],
                )
                user_exists = cursor.fetchone()[0]
        except DatabaseError:
            raise serializers.ValidationError(
                "No se pudo validar el usuario que registra el seguimiento."
            )

        if not user_exists:
            raise serializers.ValidationError(
                "El usuario que registra el seguimiento no existe."
            )
        return value


class AnuncioSerializer(serializers.ModelSerializer):
    class Meta:
        model = Anuncio
        fields = "__all__"
        read_only_fields = ["id_anuncio", "fecha"]


class MaterialSerializer(serializers.ModelSerializer):
    archivo = serializers.FileField(write_only=True, required=False)
    url = serializers.CharField(required=False, allow_blank=True)

    class Meta:
        model = Material
        fields = [
            "id_material",
            "id_asignacion",
            "tipo",
            "titulo",
            "descripcion",
            "url",
            "tamano_bytes",
            "fecha",
            "archivo",
        ]
        read_only_fields = ["id_material", "tamano_bytes", "fecha"]

    def validate(self, attrs):
        if not self.instance and not attrs.get("archivo") and not attrs.get("url"):
            raise serializers.ValidationError(
                {"archivo": "Adjunta un archivo o proporciona una URL."}
            )
        return attrs

    def _guardar_archivo(self, archivo):
        extension = Path(archivo.name).suffix.lower()
        nombre = f"materiales/{uuid4().hex}{extension}"
        ruta = default_storage.save(nombre, archivo)
        tipo = archivo.content_type or mimetypes.guess_type(archivo.name)[0] or "ARCHIVO"
        return default_storage.url(ruta), archivo.size, tipo

    def create(self, validated_data):
        archivo = validated_data.pop("archivo", None)
        if archivo:
            url, tamano_bytes, tipo_detectado = self._guardar_archivo(archivo)
            validated_data["url"] = url
            validated_data["tamano_bytes"] = tamano_bytes
            if not validated_data.get("tipo"):
                validated_data["tipo"] = tipo_detectado
        elif not validated_data.get("tipo"):
            validated_data["tipo"] = "ENLACE"
        return super().create(validated_data)

    def update(self, instance, validated_data):
        archivo = validated_data.pop("archivo", None)
        if archivo:
            url, tamano_bytes, tipo_detectado = self._guardar_archivo(archivo)
            validated_data["url"] = url
            validated_data["tamano_bytes"] = tamano_bytes
            if not validated_data.get("tipo"):
                validated_data["tipo"] = tipo_detectado
        return super().update(instance, validated_data)
