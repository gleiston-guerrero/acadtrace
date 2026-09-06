import grpc
from django.conf import settings

from docentes.models import Anuncio, Asistencia, Calificacion, PromedioTrimestral
from . import representante_academico_pb2 as pb2
from . import representante_academico_pb2_grpc as pb2_grpc


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
        notas = Calificacion.objects.filter(id_matricula__in=ids).select_related(
            "id_actividad", "id_actividad__id_periodo"
        )
        promedios = PromedioTrimestral.objects.filter(id_matricula__in=ids).select_related("id_periodo")
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
            ) for item in promedios],
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
