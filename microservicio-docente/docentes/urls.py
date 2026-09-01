from django.urls import path
from rest_framework.routers import DefaultRouter
from .representante_views import AsistenciaRepresentadoView, CalificacionesRepresentadoView

from .views import (
    ActividadViewSet,
    AnuncioViewSet,
    AsistenciaViewSet,
    AulaVirtualResumenView,
    AulaVirtualSemanasView,
    CalificacionViewSet,
    MaterialViewSet,
    PeriodoEvaluacionViewSet,
    PromedioAnualDetalleViewSet,
    PromedioAnualViewSet,
    PromedioTrimestralViewSet,
    ResumenAsistenciaViewSet,
    SeguimientoAcademicoViewSet,
)


router = DefaultRouter()
router.register("periodos-evaluacion", PeriodoEvaluacionViewSet)
router.register("actividades", ActividadViewSet)
router.register("calificaciones", CalificacionViewSet)
router.register("asistencias", AsistenciaViewSet)
router.register("resumen-asistencia", ResumenAsistenciaViewSet)
router.register("promedios-trimestrales", PromedioTrimestralViewSet)
router.register("promedios-anuales", PromedioAnualViewSet)
router.register("promedios-anuales-detalle", PromedioAnualDetalleViewSet)
router.register("seguimiento-academico", SeguimientoAcademicoViewSet)
router.register("seguimiento", SeguimientoAcademicoViewSet, basename="seguimiento")
router.register("anuncios", AnuncioViewSet)
router.register("materiales", MaterialViewSet)

urlpatterns = [
    path("representante/me/estudiantes/<int:id_estudiante>/calificaciones/", CalificacionesRepresentadoView.as_view()),
    path("representante/me/estudiantes/<int:id_estudiante>/asistencia/", AsistenciaRepresentadoView.as_view()),
    path("aula-virtual/resumen/", AulaVirtualResumenView.as_view()),
    path(
        "aula-virtual/<int:id_asignacion>/semanas/",
        AulaVirtualSemanasView.as_view(),
    ),
] + router.urls
