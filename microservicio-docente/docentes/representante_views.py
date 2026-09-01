from collections import Counter

from rest_framework.response import Response
from rest_framework.views import APIView

from .models import Asistencia, Calificacion, PromedioTrimestral
from .representante_security import (
    EsRepresentante,
    RepresentanteJWTAuthentication,
    matriculas_autorizadas,
)


class RepresentanteBaseView(APIView):
    authentication_classes = [RepresentanteJWTAuthentication]
    permission_classes = [EsRepresentante]

    def matriculas(self, request, id_estudiante):
        return matriculas_autorizadas(request.user.username, id_estudiante)


class CalificacionesRepresentadoView(RepresentanteBaseView):
    def get(self, request, id_estudiante):
        ids = self.matriculas(request, id_estudiante)
        calificaciones = Calificacion.objects.filter(id_matricula__in=ids).select_related(
            "id_actividad", "id_actividad__id_periodo"
        )
        promedios = PromedioTrimestral.objects.filter(id_matricula__in=ids).select_related("id_periodo")
        return Response({
            "calificaciones": [{
                "id_calificacion": item.id_calificacion,
                "id_matricula": item.id_matricula,
                "id_actividad": item.id_actividad_id,
                "actividad": item.id_actividad.nombre,
                "id_asignacion": item.id_actividad.id_asignacion,
                "id_periodo": item.id_actividad.id_periodo_id,
                "periodo": item.id_actividad.id_periodo.nombre,
                "nota": item.nota,
                "nota_cualitativa": item.nota_cualitativa,
            } for item in calificaciones],
            "promedios": [{
                "id_matricula": item.id_matricula,
                "id_asignacion": item.id_asignacion,
                "id_periodo": item.id_periodo_id,
                "periodo": item.id_periodo.nombre,
                "promedio_formativo": item.promedio_formativo,
                "nota_sumativa": item.nota_sumativa,
                "promedio_trimestral": item.promedio_trimestral,
                "nota_cualitativa": item.nota_cualitativa,
            } for item in promedios],
        })


class AsistenciaRepresentadoView(RepresentanteBaseView):
    def get(self, request, id_estudiante):
        ids = self.matriculas(request, id_estudiante)
        asistencias = list(Asistencia.objects.filter(id_matricula__in=ids).select_related("id_periodo"))
        conteo = Counter(item.estado for item in asistencias)
        total = len(asistencias)
        return Response({
            "asistencias": [{
                "id_asistencia": item.id_asistencia,
                "id_matricula": item.id_matricula,
                "id_asignacion": item.id_asignacion,
                "id_periodo": item.id_periodo_id,
                "periodo": item.id_periodo.nombre,
                "fecha": item.fecha,
                "estado": item.estado,
            } for item in asistencias],
            "resumen": {
                "total": total,
                "presentes": conteo["PRESENTE"],
                "ausentes": conteo["AUSENTE"],
                "justificados": conteo["JUSTIFICADO"],
                "atrasos": conteo["ATRASO"],
                "porcentaje_asistencia": round((conteo["PRESENTE"] / total) * 100, 2) if total else 0,
            },
        })
