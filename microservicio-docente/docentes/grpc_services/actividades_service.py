import grpc
from django.conf import settings
from django.db import transaction
from django.db.models import Sum
from django.core.exceptions import ObjectDoesNotExist
from docentes.models import Actividad, PeriodoEvaluacion
from . import actividades_pb2
from . import actividades_pb2_grpc
from docentes.grpc_clients.principal_client import validate_teacher_assignment
from docentes.auditoria import auditar_evento
from docentes.auditoria.payloads import payload_instancia

class ActividadServiceServicer(actividades_pb2_grpc.ActividadServiceServicer):
    def _validar_ponderacion(self, id_asignacion, id_periodo, es_sumativa, ponderacion, excluir_id=None):
        if ponderacion < 0:
            raise ValueError("La ponderación no puede ser negativa")
        query = Actividad.objects.filter(
            id_asignacion=id_asignacion,
            id_periodo_id=id_periodo,
            es_sumativa=es_sumativa,
        )
        if excluir_id:
            query = query.exclude(id_actividad=excluir_id)
        total = query.aggregate(total=Sum("ponderacion"))["total"] or 0
        limite = 30 if es_sumativa else 70
        if total + ponderacion > limite:
            categoria = "sumativa" if es_sumativa else "formativa"
            raise ValueError(f"La ponderación total {categoria} no puede superar {limite}%")

    def _validate_auth(self, context, id_asignacion):
        metadata = dict(context.invocation_metadata())
        id_docente = metadata.get('docente_id')
        internal_token = metadata.get('internal_token')

        if internal_token != settings.GRPC_INTERNAL_TOKEN:
            context.abort(grpc.StatusCode.UNAUTHENTICATED, "Token interno inválido o ausente")

        if not id_docente:
            context.abort(grpc.StatusCode.UNAUTHENTICATED, "docente_id requerido en metadatos")

        validation = validate_teacher_assignment(int(id_docente), id_asignacion)
        if not validation or not validation.get('is_valid'):
            context.abort(grpc.StatusCode.PERMISSION_DENIED, "El docente no tiene acceso a esta asignación")

        return True

    def CrearActividad(self, request, context):
        try:
            self._validate_auth(context, request.id_asignacion)

            periodo = PeriodoEvaluacion.objects.get(id_periodo=request.id_periodo)
            self._validar_ponderacion(request.id_asignacion, request.id_periodo, request.es_sumativa, request.ponderacion)

            with transaction.atomic():
                actividad = Actividad.objects.create(
                    id_asignacion=request.id_asignacion,
                    id_periodo=periodo,
                    tipo=request.tipo,
                    nombre=request.nombre,
                    descripcion=request.descripcion,
                    fecha_entrega=request.fecha_entrega,
                    ponderacion=request.ponderacion,
                    nota_maxima=request.nota_maxima,
                    es_sumativa=request.es_sumativa
                )
                auditar_evento(
                    tipo_evento="ACTIVIDAD_CREADA", entidad="Actividad",
                    entidad_id=actividad.id_actividad, operacion="CREAR", actor_id=None,
                    payload=payload_instancia(actividad),
                )

            dto = actividades_pb2.ActividadDto(
                id_actividad=actividad.id_actividad,
                id_asignacion=actividad.id_asignacion,
                id_periodo=actividad.id_periodo_id,
                tipo=actividad.tipo,
                nombre=actividad.nombre,
                descripcion=actividad.descripcion or "",
                fecha_entrega=str(actividad.fecha_entrega),
                ponderacion=float(actividad.ponderacion),
                nota_maxima=float(actividad.nota_maxima),
                es_sumativa=actividad.es_sumativa
            )

            return actividades_pb2.ActividadResponse(
                exitoso=True,
                mensaje="Actividad creada exitosamente",
                actividad=dto
            )
        except ObjectDoesNotExist:
            context.abort(grpc.StatusCode.NOT_FOUND, "Periodo de evaluación no encontrado")
        except ValueError as e:
            context.abort(grpc.StatusCode.INVALID_ARGUMENT, str(e))
        except grpc.RpcError:
            raise
        except Exception as e:
            context.abort(grpc.StatusCode.INTERNAL, str(e))

    def EditarActividad(self, request, context):
        try:
            actividad = Actividad.objects.get(id_actividad=request.id_actividad)
            self._validate_auth(context, actividad.id_asignacion)
            self._validar_ponderacion(
                actividad.id_asignacion, actividad.id_periodo_id,
                request.es_sumativa, request.ponderacion, actividad.id_actividad,
            )

            with transaction.atomic():
                actividad.tipo = request.tipo
                actividad.nombre = request.nombre
                actividad.descripcion = request.descripcion
                actividad.fecha_entrega = request.fecha_entrega
                actividad.ponderacion = request.ponderacion
                actividad.nota_maxima = request.nota_maxima
                actividad.es_sumativa = request.es_sumativa
                actividad.save()
                auditar_evento(
                    tipo_evento="ACTIVIDAD_ACTUALIZADA", entidad="Actividad",
                    entidad_id=actividad.id_actividad, operacion="ACTUALIZAR", actor_id=None,
                    payload=payload_instancia(actividad),
                )

            dto = actividades_pb2.ActividadDto(
                id_actividad=actividad.id_actividad,
                id_asignacion=actividad.id_asignacion,
                id_periodo=actividad.id_periodo_id,
                tipo=actividad.tipo,
                nombre=actividad.nombre,
                descripcion=actividad.descripcion or "",
                fecha_entrega=str(actividad.fecha_entrega),
                ponderacion=float(actividad.ponderacion),
                nota_maxima=float(actividad.nota_maxima),
                es_sumativa=actividad.es_sumativa
            )

            return actividades_pb2.ActividadResponse(
                exitoso=True,
                mensaje="Actividad actualizada exitosamente",
                actividad=dto
            )
        except ObjectDoesNotExist:
            context.abort(grpc.StatusCode.NOT_FOUND, "Actividad no encontrada")
        except ValueError as e:
            context.abort(grpc.StatusCode.INVALID_ARGUMENT, str(e))
        except grpc.RpcError:
            raise
        except Exception as e:
            context.abort(grpc.StatusCode.INTERNAL, str(e))

    def ObtenerActividad(self, request, context):
        try:
            actividad = Actividad.objects.get(id_actividad=request.id_actividad)
            self._validate_auth(context, actividad.id_asignacion)

            dto = actividades_pb2.ActividadDto(
                id_actividad=actividad.id_actividad,
                id_asignacion=actividad.id_asignacion,
                id_periodo=actividad.id_periodo_id,
                tipo=actividad.tipo,
                nombre=actividad.nombre,
                descripcion=actividad.descripcion or "",
                fecha_entrega=str(actividad.fecha_entrega),
                ponderacion=float(actividad.ponderacion),
                nota_maxima=float(actividad.nota_maxima),
                es_sumativa=actividad.es_sumativa
            )

            return actividades_pb2.ActividadResponse(
                exitoso=True,
                mensaje="Actividad obtenida",
                actividad=dto
            )
        except ObjectDoesNotExist:
            context.abort(grpc.StatusCode.NOT_FOUND, "Actividad no encontrada")
        except Exception as e:
            context.abort(grpc.StatusCode.INTERNAL, str(e))

    def ListarActividades(self, request, context):
        try:
            self._validate_auth(context, request.id_asignacion)

            query = Actividad.objects.filter(id_asignacion=request.id_asignacion)
            if request.id_periodo > 0:
                query = query.filter(id_periodo_id=request.id_periodo)

            actividades_list = []
            for actividad in query:
                dto = actividades_pb2.ActividadDto(
                    id_actividad=actividad.id_actividad,
                    id_asignacion=actividad.id_asignacion,
                    id_periodo=actividad.id_periodo_id,
                    tipo=actividad.tipo,
                    nombre=actividad.nombre,
                    descripcion=actividad.descripcion or "",
                    fecha_entrega=str(actividad.fecha_entrega),
                    ponderacion=float(actividad.ponderacion),
                    nota_maxima=float(actividad.nota_maxima),
                    es_sumativa=actividad.es_sumativa
                )
                actividades_list.append(dto)

            return actividades_pb2.ListarActividadesResponse(
                exitoso=True,
                mensaje=f"{len(actividades_list)} actividades encontradas",
                actividades=actividades_list
            )
        except grpc.RpcError:
            raise
        except Exception as e:
            import traceback
            traceback.print_exc()
            context.abort(grpc.StatusCode.INTERNAL, f"{type(e).__name__}: {e}")

    def EliminarActividad(self, request, context):
        try:
            actividad = Actividad.objects.get(id_actividad=request.id_actividad)
            self._validate_auth(context, actividad.id_asignacion)

            with transaction.atomic():
                entidad_id = actividad.id_actividad
                payload = payload_instancia(actividad)
                actividad.delete()
                auditar_evento(
                    tipo_evento="ACTIVIDAD_ELIMINADA", entidad="Actividad",
                    entidad_id=entidad_id, operacion="ELIMINAR", actor_id=None,
                    payload=payload,
                )

            return actividades_pb2.EliminarActividadResponse(
                exitoso=True,
                mensaje="Actividad eliminada exitosamente"
            )
        except ObjectDoesNotExist:
            context.abort(grpc.StatusCode.NOT_FOUND, "Actividad no encontrada")
        except Exception as e:
            context.abort(grpc.StatusCode.INTERNAL, str(e))
