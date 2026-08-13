from collections import defaultdict
from datetime import date, datetime, timedelta

from django.db import DatabaseError, connection
from django.db.models import Avg, Count, Q
from rest_framework import status, viewsets
from rest_framework.decorators import action
from rest_framework.parsers import FormParser, JSONParser, MultiPartParser
from rest_framework.response import Response
from rest_framework.views import APIView

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


TRIMESTRES = (
    "PRIMER_TRIMESTRE",
    "SEGUNDO_TRIMESTRE",
    "TERCER_TRIMESTRE",
)


def _fecha(valor):
    if isinstance(valor, datetime):
        return valor.date()
    return valor


def _periodos_trimestrales(id_asignacion):
    periodos = PeriodoEvaluacion.objects.filter(tipo__in=TRIMESTRES)
    anos_con_actividad = list(
        Actividad.objects.filter(id_asignacion=id_asignacion)
        .values_list("id_periodo__id_ano_lectivo", flat=True)
        .distinct()
    )
    if anos_con_actividad:
        periodos = periodos.filter(id_ano_lectivo=max(anos_con_actividad))
    else:
        ano_actual = periodos.order_by("-id_ano_lectivo").values_list(
            "id_ano_lectivo", flat=True
        ).first()
        if ano_actual is not None:
            periodos = periodos.filter(id_ano_lectivo=ano_actual)
    return list(periodos.order_by("fecha_inicio", "id_periodo"))


def _semanas_lunes_viernes(fecha_inicio, fecha_fin):
    semanas = []
    lunes = fecha_inicio - timedelta(days=fecha_inicio.weekday())
    numero = 1
    while lunes <= fecha_fin:
        inicio_semana = max(fecha_inicio, lunes)
        fin_semana = min(fecha_fin, lunes + timedelta(days=4))
        if inicio_semana <= fin_semana:
            semanas.append(
                {
                    "numero": numero,
                    "fecha_inicio": inicio_semana,
                    "fecha_fin": fin_semana,
                    "actividades": [],
                    "asistencias": [],
                    "materiales": [],
                    "anuncios": [],
                    "calificaciones": [],
                }
            )
            numero += 1
        lunes += timedelta(days=7)
    return semanas


def _agrupar_por_semana(semanas, registros, campo_fecha, serializar):
    for registro in registros:
        fecha_registro = _fecha(getattr(registro, campo_fecha))
        if not fecha_registro:
            continue
        for semana in semanas:
            if semana["fecha_inicio"] <= fecha_registro <= semana["fecha_fin"]:
                semana[serializar[0]].append(serializar[1](registro))
                break


def _actividad_data(actividad):
    return {
        "id_actividad": actividad.id_actividad,
        "nombre": actividad.nombre,
        "tipo": actividad.tipo,
        "descripcion": actividad.descripcion,
        "fecha_entrega": actividad.fecha_entrega,
        "ponderacion": actividad.ponderacion,
        "nota_maxima": actividad.nota_maxima,
        "es_sumativa": actividad.es_sumativa,
    }


def _asistencia_data(asistencia):
    return {
        "id_asistencia": asistencia.id_asistencia,
        "id_matricula": asistencia.id_matricula,
        "fecha": asistencia.fecha,
        "estado": asistencia.estado,
        "justificacion": asistencia.justificacion,
    }


def _material_data(material):
    return {
        "id_material": material.id_material,
        "titulo": material.titulo,
        "descripcion": material.descripcion,
        "tipo": material.tipo,
        "url": material.url,
        "tamano_bytes": material.tamano_bytes,
        "fecha": material.fecha,
    }


def _anuncio_data(anuncio):
    return {
        "id_anuncio": anuncio.id_anuncio,
        "titulo": anuncio.titulo,
        "contenido": anuncio.contenido,
        "fecha": anuncio.fecha,
        "fijado": anuncio.fijado,
    }


def _calificacion_data(calificacion):
    return {
        "id_calificacion": calificacion.id_calificacion,
        "id_matricula": calificacion.id_matricula,
        "id_actividad": calificacion.id_actividad_id,
        "actividad": calificacion.id_actividad.nombre,
        "nota": calificacion.nota,
        "nota_cualitativa": calificacion.nota_cualitativa,
        "fecha_registro": calificacion.fecha_registro,
    }
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


class AulaVirtualResumenView(APIView):
    """Métricas reales por curso para las tarjetas del Aula Virtual."""

    def get(self, request):
        ids = []
        for valor in request.query_params.getlist("id_asignacion"):
            try:
                id_asignacion = int(valor)
            except (TypeError, ValueError):
                continue
            if id_asignacion > 0:
                ids.append(id_asignacion)

        ids = list(dict.fromkeys(ids))
        if not ids:
            return Response({"cursos": []})

        notas = {
            fila["id_actividad__id_asignacion"]: fila["promedio"]
            for fila in Calificacion.objects.filter(
                id_actividad__id_asignacion__in=ids
            )
            .values("id_actividad__id_asignacion")
            .annotate(promedio=Avg("nota"))
        }
        asistencias = {
            fila["id_asignacion"]: fila
            for fila in Asistencia.objects.filter(id_asignacion__in=ids)
            .values("id_asignacion")
            .annotate(
                total=Count("id_asistencia"),
                presentes=Count(
                    "id_asistencia",
                    filter=Q(estado__in=["PRESENTE", "ATRASO"]),
                ),
            )
        }

        actividades_ano = defaultdict(set)
        for fila in Actividad.objects.filter(id_asignacion__in=ids).values(
            "id_asignacion", "id_periodo__id_ano_lectivo"
        ):
            actividades_ano[fila["id_asignacion"]].add(
                fila["id_periodo__id_ano_lectivo"]
            )

        periodos_base = PeriodoEvaluacion.objects.filter(tipo__in=TRIMESTRES)
        ano_predeterminado = periodos_base.order_by("-id_ano_lectivo").values_list(
            "id_ano_lectivo", flat=True
        ).first()
        periodos_por_ano = defaultdict(list)
        for periodo in periodos_base.order_by("fecha_inicio", "id_periodo"):
            periodos_por_ano[periodo.id_ano_lectivo].append(periodo)

        cursos = []
        for id_asignacion in ids:
            ano = max(actividades_ano[id_asignacion], default=ano_predeterminado)
            periodos = periodos_por_ano.get(ano, [])
            asistencia = asistencias.get(id_asignacion, {})
            total_asistencias = asistencia.get("total", 0)
            porcentaje_asistencia = (
                round((asistencia.get("presentes", 0) * 100) / total_asistencias, 2)
                if total_asistencias
                else None
            )
            cursos.append(
                {
                    "id_asignacion": id_asignacion,
                    "id_ano_lectivo": ano,
                    "promedio_curso": notas.get(id_asignacion),
                    "porcentaje_asistencia": porcentaje_asistencia,
                    "registros_asistencia": total_asistencias,
                    "indicador_minimos": (
                        "DATOS_DISPONIBLES"
                        if notas.get(id_asignacion) is not None and total_asistencias
                        else "SIN_DATOS_SUFFICIENTES"
                    ),
                    "fecha_inicio": periodos[0].fecha_inicio if periodos else None,
                    "fecha_fin": periodos[-1].fecha_fin if periodos else None,
                    "periodos": [
                        {
                            "id_periodo": periodo.id_periodo,
                            "nombre": periodo.nombre,
                            "fecha_inicio": periodo.fecha_inicio,
                            "fecha_fin": periodo.fecha_fin,
                        }
                        for periodo in periodos
                    ],
                }
            )
        return Response({"cursos": cursos})


class AulaVirtualSemanasView(APIView):
    """Agenda de un curso agrupada por trimestres y semanas académicas L-V."""

    def get(self, request, id_asignacion):
        periodos = _periodos_trimestrales(id_asignacion)
        resultado = []
        pendientes = []
        hoy = date.today()

        for periodo in periodos:
            semanas = _semanas_lunes_viernes(periodo.fecha_inicio, periodo.fecha_fin)
            actividades = list(
                Actividad.objects.filter(
                    id_asignacion=id_asignacion, id_periodo=periodo
                ).order_by("fecha_entrega", "id_actividad")
            )
            asistencias = list(
                Asistencia.objects.filter(
                    id_asignacion=id_asignacion,
                    id_periodo=periodo,
                    fecha__range=(periodo.fecha_inicio, periodo.fecha_fin),
                ).order_by("fecha", "id_asistencia")
            )
            materiales = list(
                Material.objects.filter(
                    id_asignacion=id_asignacion,
                    fecha__date__range=(periodo.fecha_inicio, periodo.fecha_fin),
                ).order_by("fecha", "id_material")
            )
            anuncios = list(
                Anuncio.objects.filter(
                    id_asignacion=id_asignacion,
                    fecha__date__range=(periodo.fecha_inicio, periodo.fecha_fin),
                ).order_by("-fijado", "fecha", "id_anuncio")
            )
            calificaciones = list(
                Calificacion.objects.select_related("id_actividad")
                .filter(
                    id_actividad__id_asignacion=id_asignacion,
                    id_actividad__id_periodo=periodo,
                    fecha_registro__date__range=(periodo.fecha_inicio, periodo.fecha_fin),
                )
                .order_by("fecha_registro", "id_calificacion")
            )

            _agrupar_por_semana(semanas, actividades, "fecha_entrega", ("actividades", _actividad_data))
            _agrupar_por_semana(semanas, asistencias, "fecha", ("asistencias", _asistencia_data))
            _agrupar_por_semana(semanas, materiales, "fecha", ("materiales", _material_data))
            _agrupar_por_semana(semanas, anuncios, "fecha", ("anuncios", _anuncio_data))
            _agrupar_por_semana(semanas, calificaciones, "fecha_registro", ("calificaciones", _calificacion_data))

            for actividad in actividades:
                fecha_entrega = actividad.fecha_entrega
                if fecha_entrega is not None and fecha_entrega >= hoy:
                    pendientes.append(
                        {
                            "tipo": actividad.tipo,
                            "titulo": actividad.nombre,
                            "fecha": fecha_entrega,
                            "dias_restantes": (fecha_entrega - hoy).days,
                        }
                    )
            for anuncio in anuncios:
                pendientes.append(
                    {
                        "tipo": "ANUNCIO",
                        "titulo": anuncio.titulo or "Anuncio del curso",
                        "fecha": anuncio.fecha,
                        "dias_restantes": None,
                    }
                )

            resultado.append(
                {
                    "id_periodo": periodo.id_periodo,
                    "trimestre": periodo.nombre,
                    "tipo": periodo.tipo,
                    "fecha_inicio": periodo.fecha_inicio,
                    "fecha_fin": periodo.fecha_fin,
                    "semanas": semanas,
                }
            )

        pendientes.sort(
            key=lambda item: (
                _fecha(item["fecha"]) is None,
                _fecha(item["fecha"]) or date.max,
                item["tipo"] != "ANUNCIO",
            )
        )
        return Response({"trimestres": resultado, "pendientes": pendientes[:12]})
