import grpc
from decimal import Decimal
from django.db import connection
from . import docente_pb2
from . import docente_pb2_grpc
from .client import validate_teacher_assignment
from docentes.models import Actividad, Calificacion


def _usuario_de_persona(id_persona):
    """registrado_por referencia usuarios(id_usuario); el docente llega como
    id_persona, así que resolvemos el id_usuario dueño de esa persona."""
    if not id_persona:
        id_persona = 1
    with connection.cursor() as cur:
        cur.execute("SELECT id_usuario FROM sga_principal.personas WHERE id_persona = %s", [id_persona])
        fila = cur.fetchone()
        if fila and fila[0] is not None:
            return fila[0]
        cur.execute("SELECT id_usuario FROM sga_principal.usuarios WHERE id_usuario = %s", [id_persona])
        fila = cur.fetchone()
        if fila and fila[0] is not None:
            return fila[0]
        cur.execute("SELECT id_usuario FROM sga_principal.usuarios ORDER BY id_usuario ASC LIMIT 1")
        fila = cur.fetchone()
        if fila and fila[0] is not None:
            return fila[0]
    return 1


class DocenteServiceServicer(docente_pb2_grpc.DocenteServiceServicer):

    def RegistrarCalificacion(self, request, context):
        print(f"RegistrarCalificacion: Matricula {request.id_matricula}, Actividad {request.id_actividad}, Nota {request.nota}")

        metadata = dict(context.invocation_metadata())
        id_docente = metadata.get('docente_id')
        internal_token = metadata.get('internal_token')

        if internal_token != '***REMOVED***':
            context.abort(grpc.StatusCode.UNAUTHENTICATED, "Token interno inválido o ausente")
        if not id_docente:
            context.abort(grpc.StatusCode.UNAUTHENTICATED, "docente_id requerido en metadatos")

        # La asignación real se saca de la actividad, no se asume.
        try:
            actividad = Actividad.objects.get(id_actividad=request.id_actividad)
        except Actividad.DoesNotExist:
            context.abort(grpc.StatusCode.NOT_FOUND, "La actividad no existe")

        id_asignacion = actividad.id_asignacion

        # Validamos contra el SGA Principal que el docente sí dicta esa asignación.
        validation = validate_teacher_assignment(int(id_docente), id_asignacion)
        if not validation or not validation.get('is_valid'):
            context.abort(grpc.StatusCode.PERMISSION_DENIED, "El docente no tiene acceso a esta asignación")

        nota = Decimal(str(request.nota))
        if nota > actividad.nota_maxima:
            context.abort(grpc.StatusCode.INVALID_ARGUMENT,
                          f"La nota supera la nota máxima ({actividad.nota_maxima})")

        # La nota cualitativa (A+/A-/.../D) depende del nivel educativo y se
        # calcula en el consolidado; aquí se guarda solo la nota numérica.
        # Upsert: si ya existe la nota de ese estudiante en esa actividad, se actualiza.
        calificacion, creada = Calificacion.objects.update_or_create(
            id_actividad=actividad,
            id_matricula=request.id_matricula,
            defaults={
                "nota": nota,
                "nota_cualitativa": None,
                "registrado_por": _usuario_de_persona(int(id_docente)),
            },
        )

        return docente_pb2.RegistrarCalificacionResponse(
            exitoso=True,
            mensaje="Calificación guardada" if creada else "Calificación actualizada",
            id_calificacion=calificacion.id_calificacion
        )
