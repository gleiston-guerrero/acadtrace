import grpc
from django.conf import settings

from django.db import connection
from docentes.models import Anuncio, Asistencia, Calificacion, PeriodoEvaluacion, PromedioAnual, PromedioTrimestral, TipoPeriodo
from . import representante_academico_pb2 as pb2
from . import representante_academico_pb2_grpc as pb2_grpc


def annual_grades_visible(periodos, anuales):
    required = {TipoPeriodo.PRIMER_TRIMESTRE, TipoPeriodo.SEGUNDO_TRIMESTRE, TipoPeriodo.TERCER_TRIMESTRE}
    selected = [p for p in periodos if p.tipo in required]
    return {p.tipo for p in selected} == required and all(not p.activo for p in selected) and bool(anuales)


class RepresentanteAcademicoServiceServicer(pb2_grpc.RepresentanteAcademicoServiceServicer):
    """Consultas internas de solo lectura; Principal ya autorizó las matrículas."""

    @staticmethod
    def _authorize(context):
        supplied = dict(context.invocation_metadata()).get("internal_token")
        expected = settings.GRPC_INTERNAL_TOKEN
        if not expected or supplied != expected:
            context.abort(grpc.StatusCode.UNAUTHENTICATED, "Autenticación interna inválida")

    def ConsultarCalificaciones(self, request, context):
        self._authorize(context)
        ids = list(request.id_matriculas)
        notas = list(Calificacion.objects.filter(id_matricula__in=ids).select_related(
            "id_actividad", "id_actividad__id_periodo"
        ))
        promedios = list(PromedioTrimestral.objects.filter(id_matricula__in=ids).select_related("id_periodo"))
        with connection.cursor() as cursor:
            cursor.execute(
                "SELECT DISTINCT m.id_ano_lectivo FROM sga_principal.matriculas m WHERE m.id_matricula = ANY(%s)",
                [ids],
            )
            anos = [row[0] for row in cursor.fetchall()]
        periodos = list(PeriodoEvaluacion.objects.filter(id_ano_lectivo__in=anos).order_by("fecha_inicio", "id_periodo"))
        tipos_requeridos = {TipoPeriodo.PRIMER_TRIMESTRE, TipoPeriodo.SEGUNDO_TRIMESTRE, TipoPeriodo.TERCER_TRIMESTRE}
        periodos_requeridos = [p for p in periodos if p.tipo in tipos_requeridos]
        todos_cerrados = {p.tipo for p in periodos_requeridos} == tipos_requeridos and all(not p.activo for p in periodos_requeridos)
        anuales = list(PromedioAnual.objects.filter(id_matricula__in=ids, id_ano_lectivo__in=anos)) if todos_cerrados else []
        asignacion_ids = {n.id_actividad.id_asignacion for n in notas} | {
            p.id_asignacion for p in promedios
        } | {p.id_asignacion for p in anuales}
        nombres_asignatura = {}
        if asignacion_ids:
            with connection.cursor() as cursor:
                cursor.execute(
                    "SELECT a.id_asignacion, s.nombre FROM sga_principal.asignaciones a "
                    "JOIN sga_principal.asignaturas s ON s.id_asignatura=a.id_asignatura "
                    "WHERE a.id_asignacion = ANY(%s)", [list(asignacion_ids)]
                )
                nombres_asignatura = dict(cursor.fetchall())
        return pb2.CalificacionesResponse(
            calificaciones=[pb2.CalificacionRepresentante(
                id_calificacion=item.id_calificacion,
                id_matricula=item.id_matricula,
                id_actividad=item.id_actividad_id,
                actividad=item.id_actividad.nombre,
                id_asignacion=item.id_actividad.id_asignacion,
                id_periodo=item.id_actividad.id_periodo_id,
                periodo=item.id_actividad.id_periodo.nombre,
                nota=float(item.nota),
                nota_cualitativa=item.nota_cualitativa or "",
                asignatura=nombres_asignatura.get(item.id_actividad.id_asignacion, f"Asignatura {item.id_actividad.id_asignacion}"),
            ) for item in notas],
            promedios=[pb2.PromedioRepresentante(
                id_matricula=item.id_matricula,
                id_asignacion=item.id_asignacion,
                id_periodo=item.id_periodo_id,
                periodo=item.id_periodo.nombre,
                promedio_formativo=float(item.promedio_formativo),
                nota_sumativa=float(item.nota_sumativa),
                promedio_trimestral=float(item.promedio_trimestral),
                nota_cualitativa=item.nota_cualitativa,
                asignatura=nombres_asignatura.get(item.id_asignacion, f"Asignatura {item.id_asignacion}"),
            ) for item in promedios],
            periodos=[pb2.PeriodoRepresentante(
                id_periodo=p.id_periodo, nombre=p.nombre, activo=p.activo,
                fecha_inicio=p.fecha_inicio.isoformat(),
            ) for p in periodos_requeridos],
            promedios_anuales=[pb2.PromedioAnualRepresentante(
                id_asignacion=p.id_asignacion,
                asignatura=nombres_asignatura.get(p.id_asignacion, f"Asignatura {p.id_asignacion}"),
                promedio_anual=float(p.promedio_anual), nota_cualitativa=p.nota_cualitativa,
            ) for p in anuales],
            mostrar_promedios_anuales=annual_grades_visible(periodos_requeridos, anuales),
        )

    def ConsultarAsistencia(self, request, context):
        self._authorize(context)
        registros = list(Asistencia.objects.filter(id_matricula__in=list(request.id_matriculas)).select_related("id_periodo"))
        counts = {"PRESENTE": 0, "AUSENTE": 0, "JUSTIFICADO": 0, "ATRASO": 0}
        for item in registros:
            counts[item.estado] = counts.get(item.estado, 0) + 1
        total = len(registros)
        return pb2.AsistenciaResponse(
            asistencias=[pb2.AsistenciaRepresentante(
                id_asistencia=item.id_asistencia,
                id_matricula=item.id_matricula,
                id_asignacion=item.id_asignacion,
                id_periodo=item.id_periodo_id,
                periodo=item.id_periodo.nombre,
                fecha=item.fecha.isoformat(),
                estado=item.estado,
            ) for item in registros],
            resumen=pb2.ResumenAsistenciaRepresentante(
                total=total,
                presentes=counts["PRESENTE"],
                ausentes=counts["AUSENTE"],
                justificados=counts["JUSTIFICADO"],
                atrasos=counts["ATRASO"],
                porcentaje_asistencia=round(counts["PRESENTE"] * 100.0 / total, 2) if total else 0.0,
            ),
        )

    def ConsultarComunicados(self, request, context):
        self._authorize(context)
        anuncios = Anuncio.objects.filter(id_asignacion__in=list(request.id_asignaciones))
        return pb2.ComunicadosResponse(comunicados=[pb2.ComunicadoRepresentante(
            id=item.id_anuncio,
            titulo=item.titulo or "",
            contenido=item.contenido or "",
            fecha=item.fecha.isoformat(),
            fijado=item.fijado,
        ) for item in anuncios])
