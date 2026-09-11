import grpc
from django.db import transaction, connection
from django.db.models import Count
from micro_docente.middleware import registrar_asistencias_exitosas
from docentes.auditoria import auditar_evento
from docentes.auditoria.payloads import payload_instancia
from django.core.exceptions import ObjectDoesNotExist
from docentes.models import Asistencia, ResumenAsistencia, PeriodoEvaluacion, EstadoAsistencia
from docentes.notifications import enqueue_attendance
from . import asistencia_pb2
from . import asistencia_pb2_grpc
from docentes.grpc_clients.principal_client import validate_teacher_assignment, get_students_by_assignment


def _usuario_de_persona(id_persona):
    # registrado_por referencia usuarios(id_usuario); el docente llega como id_persona.
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


def _asegurar_asignacion(id_asignacion, id_docente=None):
    if not id_asignacion:
        return
    with connection.cursor() as cur:
        cur.execute("SELECT 1 FROM sga_principal.asignaciones WHERE id_asignacion = %s", [id_asignacion])
        if cur.fetchone():
            return
        doc_usuario = _usuario_de_persona(id_docente) if id_docente else 11
        cur.execute("""
            INSERT INTO sga_principal.asignaciones
            (id_asignacion, id_docente, id_asignatura, id_grado, id_paralelo, id_ano_lectivo, es_tutor, activo, fecha_asignacion)
            VALUES (%s, %s, 1, 1, 3, 1, False, True, NOW())
            ON CONFLICT (id_asignacion) DO NOTHING
        """, [id_asignacion, doc_usuario])

class AsistenciaServiceServicer(asistencia_pb2_grpc.AsistenciaServiceServicer):
    
    def _validate_auth(self, context, id_asignacion):
        metadata = dict(context.invocation_metadata())
        id_docente = metadata.get('docente_id')
        internal_token = metadata.get('internal_token')
        
        if internal_token != '***REMOVED***':
            context.abort(grpc.StatusCode.UNAUTHENTICATED, "Token interno inválido o ausente")

        if not id_docente:
            context.abort(grpc.StatusCode.UNAUTHENTICATED, "docente_id requerido en metadatos")
            
        validation = validate_teacher_assignment(int(id_docente), id_asignacion)
        if not validation or not validation.get('is_valid'):
            context.abort(grpc.StatusCode.PERMISSION_DENIED, "El docente no tiene acceso a esta asignación")
            
        return int(id_docente)

    def _actualizar_resumen(self, id_matricula, id_asignacion, periodo):
        # Cuenta todos los estados para este estudiante en esta asignación y periodo
        asistencias = Asistencia.objects.filter(
            id_matricula=id_matricula,
            id_asignacion=id_asignacion,
            id_periodo=periodo
        )
        
        total_presentes = 0
        total_ausentes = 0
        total_justificados = 0
        total_atrasos = 0
        
        for a in asistencias:
            if a.estado == EstadoAsistencia.PRESENTE:
                total_presentes += 1
            elif a.estado == EstadoAsistencia.AUSENTE:
                total_ausentes += 1
            elif a.estado == EstadoAsistencia.JUSTIFICADO:
                total_justificados += 1
            elif a.estado == EstadoAsistencia.ATRASO:
                total_atrasos += 1
                
        # Upsert del resumen
        resumen, created = ResumenAsistencia.objects.get_or_create(
            id_matricula=id_matricula,
            id_asignacion=id_asignacion,
            id_periodo=periodo,
            defaults={
                'total_presentes': total_presentes,
                'total_ausentes': total_ausentes,
                'total_justificados': total_justificados,
                'total_atrasos': total_atrasos
            }
        )
        
        if not created:
            resumen.total_presentes = total_presentes
            resumen.total_ausentes = total_ausentes
            resumen.total_justificados = total_justificados
            resumen.total_atrasos = total_atrasos
            resumen.save()

    def _recalcular_resumen_bulk(self, id_asignacion, periodo, matriculas):
        # Cuenta por (matrícula, estado) de todo el periodo en UNA consulta.
        conteos = (
            Asistencia.objects
            .filter(id_asignacion=id_asignacion, id_periodo=periodo, id_matricula__in=matriculas)
            .values("id_matricula", "estado")
            .annotate(n=Count("id_asistencia"))
        )
        acum = {m: {"PRESENTE": 0, "AUSENTE": 0, "JUSTIFICADO": 0, "ATRASO": 0} for m in matriculas}
        for c in conteos:
            acum[c["id_matricula"]][c["estado"]] = c["n"]

        ResumenAsistencia.objects.filter(
            id_asignacion=id_asignacion, id_periodo=periodo, id_matricula__in=matriculas
        ).delete()
        ResumenAsistencia.objects.bulk_create([
            ResumenAsistencia(
                id_matricula=m,
                id_asignacion=id_asignacion,
                id_periodo=periodo,
                total_presentes=v["PRESENTE"],
                total_ausentes=v["AUSENTE"],
                total_justificados=v["JUSTIFICADO"],
                total_atrasos=v["ATRASO"],
            )
            for m, v in acum.items()
        ])

    def RegistrarAsistenciaGrupal(self, request, context):
        try:
            id_docente = self._validate_auth(context, request.id_asignacion)
            _asegurar_asignacion(request.id_asignacion, id_docente)
            
            try:
                periodo = PeriodoEvaluacion.objects.get(id_periodo=request.id_periodo)
            except ObjectDoesNotExist:
                context.abort(grpc.StatusCode.NOT_FOUND, "Periodo de evaluación no encontrado")
                
            # Evitar N+1 y llamadas a gRPC individuales obteniendo los estudiantes válidos
            estudiantes = get_students_by_assignment(request.id_asignacion)
            matriculas_validas = {est['id_matricula'] for est in estudiantes}
            estados_validos = {e.value for e in EstadoAsistencia}

            # Validación previa (sin tocar BD).
            items = list(request.asistencias)
            for item in items:
                if item.id_matricula not in matriculas_validas:
                    context.abort(grpc.StatusCode.INVALID_ARGUMENT, f"Estudiante con matrícula {item.id_matricula} no pertenece a la asignación")
                if item.estado not in estados_validos:
                    context.abort(grpc.StatusCode.INVALID_ARGUMENT, f"Estado {item.estado} inválido")

            # El usuario que registra es el mismo para todos: se resuelve una sola vez.
            usuario_registra = _usuario_de_persona(id_docente)
            matriculas = [item.id_matricula for item in items]

            with transaction.atomic():
                # Reemplazo del día en bloque: borrar lo previo y crear todo de una
                # (evita cientos de idas y vueltas a la BD por estudiante).
                asistencias_anteriores = Asistencia.objects.filter(
                    id_asignacion=request.id_asignacion, id_periodo=periodo,
                    fecha=request.fecha, id_matricula__in=matriculas,
                )
                estados_anteriores = dict(
                    asistencias_anteriores.values_list("id_matricula", "estado")
                )
                asistencias_anteriores.delete()

                # Se usa INSERT raw con cast explicito a estado_asistencia_t porque
                # la columna es un ENUM nativo de Postgres y Django bulk_create genera
                # UNNEST(text[]) sin cast, lo que falla con "column is of type
                # estado_asistencia_t but expression is of type character varying".
                from django.db import connection
                filas = [
                    (
                        item.id_matricula,
                        request.id_asignacion,
                        periodo.id_periodo,
                        request.fecha,
                        item.estado,
                        item.justificacion or None,
                        usuario_registra,
                    )
                    for item in items
                ]
                sql = (
                    'INSERT INTO sga_docente.asistencias '
                    '(id_matricula, id_asignacion, id_periodo, fecha, estado, '
                    ' justificacion, registrado_por, fecha_registro, fecha_actualizacion) '
                    'VALUES (%s, %s, %s, %s, %s::sga_docente.estado_asistencia_t, %s, %s, NOW(), NOW()) '
                    'RETURNING id_asistencia'
                )
                ids_creados = []
                with connection.cursor() as cursor:
                    for fila in filas:
                        cursor.execute(sql, fila)
                        ids_creados.append(cursor.fetchone()[0])
                nuevas = list(
                    Asistencia.objects.filter(id_asistencia__in=ids_creados).order_by("id_asistencia")
                )

                # Resumen del periodo recalculado con UNA sola consulta agregada.
                self._recalcular_resumen_bulk(request.id_asignacion, periodo, matriculas)
                for asistencia in nuevas:
                    auditar_evento(
                        tipo_evento="ASISTENCIA_REGISTRADA", entidad="Asistencia",
                        entidad_id=asistencia.id_asistencia, operacion="CREAR",
                        actor_id=usuario_registra, payload=payload_instancia(asistencia),
                        nodo=f"docente-{id_docente}",
                    )
                    if (
                        asistencia.estado in ("AUSENTE", "ATRASO")
                        and estados_anteriores.get(asistencia.id_matricula) != asistencia.estado
                    ):
                        enqueue_attendance(asistencia)

            asistencias_creadas = [
                asistencia_pb2.AsistenciaDTO(
                    id_asistencia=a.id_asistencia,
                    id_matricula=a.id_matricula,
                    id_asignacion=a.id_asignacion,
                    id_periodo=a.id_periodo_id,
                    fecha=str(a.fecha),
                    estado=a.estado,
                    justificacion=a.justificacion or "",
                )
                for a in nuevas
            ]
            response = asistencia_pb2.AsistenciaListResponse(
                success=True,
                message=f"Se registraron {len(asistencias_creadas)} asistencias correctamente.",
                asistencias=asistencias_creadas
            )
            registrar_asistencias_exitosas(len(asistencias_creadas))
            return response
            
        except grpc.RpcError:
            raise
        except Exception as e:
            context.abort(grpc.StatusCode.INTERNAL, str(e))

    def ActualizarAsistencia(self, request, context):
        try:
            try:
                asistencia = Asistencia.objects.get(id_asistencia=request.id_asistencia)
            except ObjectDoesNotExist:
                context.abort(grpc.StatusCode.NOT_FOUND, "Registro de asistencia no encontrado")
                
            self._validate_auth(context, asistencia.id_asignacion)
            
            if request.estado not in [e.value for e in EstadoAsistencia]:
                context.abort(grpc.StatusCode.INVALID_ARGUMENT, f"Estado {request.estado} inválido")
                
            with transaction.atomic():
                asistencia.estado = request.estado
                asistencia.justificacion = request.justificacion
                asistencia.save()
                
                self._actualizar_resumen(asistencia.id_matricula, asistencia.id_asignacion, asistencia.id_periodo)
                auditar_evento(
                    tipo_evento="ASISTENCIA_ACTUALIZADA", entidad="Asistencia",
                    entidad_id=asistencia.id_asistencia, operacion="ACTUALIZAR",
                    actor_id=asistencia.registrado_por,
                    payload=payload_instancia(asistencia),
                )
                
            dto = asistencia_pb2.AsistenciaDTO(
                id_asistencia=asistencia.id_asistencia,
                id_matricula=asistencia.id_matricula,
                id_asignacion=asistencia.id_asignacion,
                id_periodo=asistencia.id_periodo_id,
                fecha=str(asistencia.fecha),
                estado=asistencia.estado,
                justificacion=asistencia.justificacion or ""
            )
            
            return asistencia_pb2.AsistenciaResponse(
                success=True,
                message="Asistencia actualizada exitosamente",
                asistencia=dto
            )
            
        except grpc.RpcError:
            raise
        except Exception as e:
            context.abort(grpc.StatusCode.INTERNAL, str(e))

    def ConsultarAsistencia(self, request, context):
        try:
            self._validate_auth(context, request.id_asignacion)
            
            query = Asistencia.objects.filter(id_asignacion=request.id_asignacion)
            
            if request.fecha:
                query = query.filter(fecha=request.fecha)
            if request.id_periodo > 0:
                query = query.filter(id_periodo_id=request.id_periodo)
            if request.id_matricula > 0:
                query = query.filter(id_matricula=request.id_matricula)
                
            resultados = []
            for a in query:
                resultados.append(
                    asistencia_pb2.AsistenciaDTO(
                        id_asistencia=a.id_asistencia,
                        id_matricula=a.id_matricula,
                        id_asignacion=a.id_asignacion,
                        id_periodo=a.id_periodo_id,
                        fecha=str(a.fecha),
                        estado=a.estado,
                        justificacion=a.justificacion or ""
                    )
                )
                
            return asistencia_pb2.AsistenciaListResponse(
                success=True,
                message=f"{len(resultados)} registros encontrados",
                asistencias=resultados
            )
            
        except grpc.RpcError:
            raise
        except Exception as e:
            context.abort(grpc.StatusCode.INTERNAL, str(e))

    def ConsultarResumenAsistencia(self, request, context):
        try:
            self._validate_auth(context, request.id_asignacion)
            
            estudiantes = get_students_by_assignment(request.id_asignacion)
            matriculas_validas = [est['id_matricula'] for est in estudiantes]
            
            query = ResumenAsistencia.objects.filter(
                id_asignacion=request.id_asignacion,
                id_matricula__in=matriculas_validas
            )
            
            if request.id_periodo > 0:
                query = query.filter(id_periodo_id=request.id_periodo)
            if request.id_matricula > 0:
                query = query.filter(id_matricula=request.id_matricula)
                
            resultados = []
            for r in query:
                total = r.total_presentes + r.total_ausentes + r.total_justificados + r.total_atrasos
                porcentaje = 0.0
                if total > 0:
                    porcentaje = ((r.total_presentes + r.total_justificados + r.total_atrasos) / total) * 100.0
                    
                resultados.append(
                    asistencia_pb2.ResumenAsistenciaDTO(
                        id_resumen=r.id_resumen,
                        id_matricula=r.id_matricula,
                        id_asignacion=r.id_asignacion,
                        id_periodo=r.id_periodo_id,
                        total_presentes=r.total_presentes,
                        total_ausentes=r.total_ausentes,
                        total_justificados=r.total_justificados,
                        total_atrasos=r.total_atrasos,
                        porcentaje_asistencia=porcentaje
                    )
                )
                
            return asistencia_pb2.ResumenAsistenciaListResponse(
                success=True,
                message=f"{len(resultados)} resúmenes encontrados",
                resumenes=resultados
            )
            
        except grpc.RpcError:
            raise
        except Exception as e:
            context.abort(grpc.StatusCode.INTERNAL, str(e))
