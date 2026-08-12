from django.db import DatabaseError, connection
from rest_framework import status, viewsets
from rest_framework.decorators import action
from rest_framework.parsers import FormParser, JSONParser, MultiPartParser
from rest_framework.response import Response

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
from .serializers import (
    ActividadSerializer,
    AnuncioSerializer,
    AsistenciaSerializer,
    CalificacionSerializer,
    MaterialSerializer,
    PeriodoEvaluacionSerializer,
    PromedioAnualDetalleSerializer,
    PromedioAnualSerializer,
    PromedioTrimestralSerializer,
    ResumenAsistenciaSerializer,
    SeguimientoAcademicoSerializer,
)
from .services import (
    calcular_promedio_anual,
    calcular_promedio_formativo,
    calcular_promedio_trimestral,
    calcular_resumen_asistencia,
    convertir_nota_cualitativa,
)


def requeridos(data, campos):
    faltantes = [campo for campo in campos if data.get(campo) in ("", None)]
    if faltantes:
        return Response(
            {"detail": "Campos requeridos.", "campos": faltantes},
            status=status.HTTP_400_BAD_REQUEST,
        )
    return None


class PeriodoEvaluacionViewSet(viewsets.ModelViewSet):
    queryset = PeriodoEvaluacion.objects.all()
    serializer_class = PeriodoEvaluacionSerializer


class ActividadViewSet(viewsets.ModelViewSet):
    queryset = Actividad.objects.select_related("id_periodo").all()
    serializer_class = ActividadSerializer

    def get_queryset(self):
        queryset = super().get_queryset()
        id_asignacion = self.request.query_params.get("id_asignacion")
        id_periodo = self.request.query_params.get("id_periodo")
        es_sumativa = self.request.query_params.get("es_sumativa")
        if id_asignacion:
            queryset = queryset.filter(id_asignacion=id_asignacion)
        if id_periodo:
            queryset = queryset.filter(id_periodo_id=id_periodo)
        if es_sumativa is not None:
            queryset = queryset.filter(es_sumativa=es_sumativa.lower() == "true")
        return queryset


class CalificacionViewSet(viewsets.ModelViewSet):
    queryset = Calificacion.objects.select_related("id_actividad").all()
    serializer_class = CalificacionSerializer

    def get_queryset(self):
        queryset = super().get_queryset()
        id_matricula = self.request.query_params.get("id_matricula")
        id_actividad = self.request.query_params.get("id_actividad")
        id_asignacion = self.request.query_params.get("id_asignacion")
        id_periodo = self.request.query_params.get("id_periodo")
        if id_matricula:
            queryset = queryset.filter(id_matricula=id_matricula)
        if id_actividad:
            queryset = queryset.filter(id_actividad_id=id_actividad)
        if id_asignacion:
            queryset = queryset.filter(id_actividad__id_asignacion=id_asignacion)
        if id_periodo:
            queryset = queryset.filter(id_actividad__id_periodo_id=id_periodo)
        return queryset

    @action(detail=False, methods=["get"], url_path="promedio-formativo")
    def promedio_formativo(self, request):
        error = requeridos(
            request.query_params, ["id_matricula", "id_asignacion", "id_periodo"]
        )
        if error:
            return error
        promedio = calcular_promedio_formativo(
            request.query_params["id_matricula"],
            request.query_params["id_asignacion"],
            request.query_params["id_periodo"],
        )
        return Response(
            {
                "id_matricula": int(request.query_params["id_matricula"]),
                "id_asignacion": int(request.query_params["id_asignacion"]),
                "id_periodo": int(request.query_params["id_periodo"]),
                "promedio_formativo": promedio,
                "nota_cualitativa": convertir_nota_cualitativa(
                    promedio, request.query_params.get("nivel", "EGB")
                ),
            }
        )


class AsistenciaViewSet(viewsets.ModelViewSet):
    queryset = Asistencia.objects.select_related("id_periodo").all()
    serializer_class = AsistenciaSerializer

    def get_queryset(self):
        queryset = super().get_queryset()
        for campo in ["id_matricula", "id_asignacion", "id_periodo", "fecha", "estado"]:
            valor = self.request.query_params.get(campo)
            if valor:
                filtro = {"id_periodo_id" if campo == "id_periodo" else campo: valor}
                queryset = queryset.filter(**filtro)
        return queryset


class ResumenAsistenciaViewSet(viewsets.ModelViewSet):
    queryset = ResumenAsistencia.objects.select_related("id_periodo").all()
    serializer_class = ResumenAsistenciaSerializer

    def get_queryset(self):
        queryset = super().get_queryset()
        for campo in ["id_matricula", "id_asignacion", "id_periodo"]:
            valor = self.request.query_params.get(campo)
            if valor:
                filtro = {"id_periodo_id" if campo == "id_periodo" else campo: valor}
                queryset = queryset.filter(**filtro)
        return queryset

    @action(detail=False, methods=["post"], url_path="calcular")
    def calcular(self, request):
        error = requeridos(request.data, ["id_matricula", "id_asignacion", "id_periodo"])
        if error:
            return error
        resumen = calcular_resumen_asistencia(
            request.data["id_matricula"],
            request.data["id_asignacion"],
            request.data["id_periodo"],
        )
        return Response(self.get_serializer(resumen).data)


class PromedioTrimestralViewSet(viewsets.ModelViewSet):
    queryset = PromedioTrimestral.objects.select_related("id_periodo").all()
    serializer_class = PromedioTrimestralSerializer

    @action(detail=False, methods=["post"], url_path="calcular")
    def calcular(self, request):
        error = requeridos(request.data, ["id_matricula", "id_asignacion", "id_periodo"])
        if error:
            return error
        promedio = calcular_promedio_trimestral(
            request.data["id_matricula"],
            request.data["id_asignacion"],
            request.data["id_periodo"],
            request.data.get("nivel", "EGB"),
        )
        return Response(self.get_serializer(promedio).data)


class PromedioAnualViewSet(viewsets.ModelViewSet):
    queryset = PromedioAnual.objects.all()
    serializer_class = PromedioAnualSerializer

    @action(detail=False, methods=["post"], url_path="calcular")
    def calcular(self, request):
        error = requeridos(
            request.data, ["id_matricula", "id_asignacion", "id_ano_lectivo"]
        )
        if error:
            return error
        promedio = calcular_promedio_anual(
            request.data["id_matricula"],
            request.data["id_asignacion"],
            request.data["id_ano_lectivo"],
            request.data.get("nivel", "EGB"),
            request.data.get("registrado_por"),
        )
        return Response(self.get_serializer(promedio).data)


class PromedioAnualDetalleViewSet(viewsets.ModelViewSet):
    queryset = PromedioAnualDetalle.objects.select_related(
        "id_promedio_anual", "id_promedio_trim"
    ).all()
    serializer_class = PromedioAnualDetalleSerializer


class SeguimientoAcademicoViewSet(viewsets.ModelViewSet):
    queryset = SeguimientoAcademico.objects.select_related("id_periodo").all()
    serializer_class = SeguimientoAcademicoSerializer

    def get_queryset(self):
        queryset = super().get_queryset()
        for campo in ["id_matricula", "id_periodo", "categoria", "requiere_followup"]:
            valor = self.request.query_params.get(campo)
            if valor is None:
                continue
            if campo == "id_periodo":
                queryset = queryset.filter(id_periodo_id=valor)
            elif campo == "requiere_followup":
                queryset = queryset.filter(requiere_followup=valor.lower() == "true")
            else:
                queryset = queryset.filter(**{campo: valor})
        id_paralelo = self.request.query_params.get("id_paralelo")
        if id_paralelo:
            try:
                with connection.cursor() as cursor:
                    cursor.execute(
                        "SELECT id_matricula FROM sga_principal.matriculas "
                        "WHERE id_paralelo = %s",
                        [id_paralelo],
                    )
                    matriculas = [fila[0] for fila in cursor.fetchall()]
                queryset = queryset.filter(id_matricula__in=matriculas)
            except DatabaseError:
                queryset = queryset.none()
        return queryset


class AnuncioViewSet(viewsets.ModelViewSet):
    queryset = Anuncio.objects.all()
    serializer_class = AnuncioSerializer
    http_method_names = ["get", "post", "delete", "head", "options"]

    def get_queryset(self):
        queryset = super().get_queryset()
        id_asignacion = self.request.query_params.get("id_asignacion")
        if id_asignacion:
            queryset = queryset.filter(id_asignacion=id_asignacion)
        return queryset


class MaterialViewSet(viewsets.ModelViewSet):
    queryset = Material.objects.all()
    serializer_class = MaterialSerializer
    parser_classes = [MultiPartParser, FormParser, JSONParser]

    def get_queryset(self):
        queryset = super().get_queryset()
        id_asignacion = self.request.query_params.get("id_asignacion")
        if id_asignacion:
            queryset = queryset.filter(id_asignacion=id_asignacion)
        return queryset
