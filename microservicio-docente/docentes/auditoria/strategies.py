from django.db import transaction
from django.utils import timezone

from docentes.models import EventoAuditoria

from .central_ledger import (
    actualizar_estado_global,
    bloquear_estado_global,
    insertar_evento_global,
)
from .clocks import (
    incrementar_lamport,
    incrementar_vector,
    reconciliar_vectores,
)
from .hashing import (
    calcular_hash,
    contenido_evento,
    json_canonico,
    normalizar,
)


class NoAuditStrategy:
    def registrar(self, **_evento):
        return None


class FlatAuditStrategy:
    modo = "m1"

    def registrar(self, **evento):
        instante = evento.get("timestamp") or timezone.now()
        payload = normalizar(evento.get("payload") or {})

        return EventoAuditoria.objects.create(
            tipo_evento=evento["tipo_evento"],
            entidad=evento["entidad"],
            entidad_id=str(evento["entidad_id"]),
            operacion=evento["operacion"],
            actor_id=evento.get("actor_id"),
            timestamp=instante,
            payload_canonico=json_canonico(payload),
            modo=self.modo,
            estado_reconciliacion="NO_APLICA",
        )


class HashChainAuditStrategy:
    modo = "m2"
    usa_vector = False

    def registrar(self, **evento):
        with transaction.atomic():
            # E3: una sola cabeza fisica para Principal, Secretaria y Docente.
            estado = bloquear_estado_global()

            anterior = estado.ultimo_hash

            lamport = incrementar_lamport(
                estado.ultimo_lamport,
                evento.get("lamport_recibido"),
            )

            vector, reconciliacion = self._vector_y_reconciliacion(
                estado,
                evento,
            )

            instante = evento.get("timestamp") or timezone.now()
            payload = normalizar(evento.get("payload") or {})

            contenido = contenido_evento(
                tipo_evento=evento["tipo_evento"],
                entidad=evento["entidad"],
                entidad_id=evento["entidad_id"],
                operacion=evento["operacion"],
                actor_id=evento.get("actor_id"),
                timestamp=instante,
                payload=payload,
                modo=self.modo,
                reloj_lamport=lamport,
                reloj_vectorial=vector,
                estado_reconciliacion=reconciliacion,
            )

            canonico = json_canonico(contenido)
            actual = calcular_hash(anterior, contenido)

            # Escritura autoritativa institucional.
            insertar_evento_global(
                evento=evento,
                instante=instante,
                hash_anterior=anterior,
                hash_actual=actual,
                reloj_lamport=lamport,
                reloj_vectorial=vector,
                contenido_canonico=canonico,
            )

            # Proyeccion local conservada para compatibilidad con E2,
            # consultas academicas y evidencia historica de Docente.
            registro = EventoAuditoria.objects.create(
                tipo_evento=evento["tipo_evento"],
                entidad=evento["entidad"],
                entidad_id=str(evento["entidad_id"]),
                operacion=evento["operacion"],
                actor_id=evento.get("actor_id"),
                timestamp=instante,
                payload_canonico=json_canonico(payload),
                modo=self.modo,
                hash_anterior=anterior,
                hash_actual=actual,
                reloj_lamport=lamport,
                reloj_vectorial=vector,
                estado_reconciliacion=reconciliacion,
            )

            actualizar_estado_global(
                hash_actual=actual,
                reloj_lamport=lamport,
                reloj_vectorial=vector,
            )

            # Mantener el objeto en memoria coherente para pruebas y para
            # cualquier consumidor que reutilice la instancia durante
            # la misma unidad de trabajo.
            estado.ultimo_hash = actual
            estado.ultimo_lamport = lamport
            if vector is not None:
                estado.reloj_vectorial = vector

            return registro

    def _vector_y_reconciliacion(self, _estado, _evento):
        return None, "NO_APLICA"


class VectorClockAuditStrategy(HashChainAuditStrategy):
    modo = "m3"
    usa_vector = True

    def _vector_y_reconciliacion(self, estado, evento):
        remoto = normalizar(
            evento.get("reloj_vectorial_recibido") or {}
        )

        nodo = (
            evento.get("nodo")
            or f"docente-{evento.get('actor_id') or 'sistema'}"
        )

        # estado.reloj_vectorial ya contiene la combinacion global
        # confirmada por Principal, Secretaria y Docente.
        resultado = reconciliar_vectores(
            estado.reloj_vectorial,
            remoto,
        )

        combinado = resultado["reloj_combinado"]

        return (
            incrementar_vector(combinado, nodo),
            resultado["estado"],
        )


STRATEGIES = {
    "m0": NoAuditStrategy,
    "m1": FlatAuditStrategy,
    "m2": HashChainAuditStrategy,
    "m3": VectorClockAuditStrategy,
}
