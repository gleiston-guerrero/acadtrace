import json

from django.db import transaction
from django.utils import timezone

from docentes.models import EstadoCadenaAuditoria, EventoAuditoria

from .clocks import incrementar_vector, reconciliar_vectores
from .hashing import GENESIS_HASH, calcular_hash, contenido_evento, json_canonico, normalizar


class NoAuditStrategy:
    def registrar(self, **_evento):
        return None


class FlatAuditStrategy:
    modo = "m1"

    def registrar(self, **evento):
        instante = evento.get("timestamp") or timezone.now()
        payload = normalizar(evento.get("payload") or {})
        return EventoAuditoria.objects.create(
            tipo_evento=evento["tipo_evento"], entidad=evento["entidad"],
            entidad_id=str(evento["entidad_id"]), operacion=evento["operacion"],
            actor_id=evento.get("actor_id"), timestamp=instante,
            payload_canonico=json_canonico(payload), modo=self.modo,
            estado_reconciliacion="NO_APLICA",
        )


class HashChainAuditStrategy:
    modo = "m2"
    usa_vector = False

    def registrar(self, **evento):
        with transaction.atomic():
            estado, _ = EstadoCadenaAuditoria.objects.select_for_update().get_or_create(
                id_estado=1,
                defaults={"ultimo_hash": None, "ultimo_lamport": 0, "reloj_vectorial": {}},
            )
            anterior = estado.ultimo_hash or GENESIS_HASH
            lamport = max(int(estado.ultimo_lamport), int(evento.get("lamport_recibido") or 0)) + 1
            vector, reconciliacion = self._vector_y_reconciliacion(estado, evento)
            instante = evento.get("timestamp") or timezone.now()
            payload = normalizar(evento.get("payload") or {})
            contenido = contenido_evento(
                tipo_evento=evento["tipo_evento"], entidad=evento["entidad"],
                entidad_id=evento["entidad_id"], operacion=evento["operacion"],
                actor_id=evento.get("actor_id"), timestamp=instante, payload=payload,
                modo=self.modo, reloj_lamport=lamport, reloj_vectorial=vector,
                estado_reconciliacion=reconciliacion,
            )
            actual = calcular_hash(anterior, contenido)
            registro = EventoAuditoria.objects.create(
                tipo_evento=evento["tipo_evento"], entidad=evento["entidad"],
                entidad_id=str(evento["entidad_id"]), operacion=evento["operacion"],
                actor_id=evento.get("actor_id"), timestamp=instante,
                payload_canonico=json_canonico(payload), modo=self.modo,
                hash_anterior=anterior, hash_actual=actual, reloj_lamport=lamport,
                reloj_vectorial=vector, estado_reconciliacion=reconciliacion,
            )
            estado.ultimo_hash = actual
            estado.ultimo_lamport = lamport
            estado.reloj_vectorial = vector or estado.reloj_vectorial
            estado.save(update_fields=["ultimo_hash", "ultimo_lamport", "reloj_vectorial"])
            return registro

    def _vector_y_reconciliacion(self, _estado, _evento):
        return None, "NO_APLICA"


class VectorClockAuditStrategy(HashChainAuditStrategy):
    modo = "m3"
    usa_vector = True

    def _vector_y_reconciliacion(self, estado, evento):
        remoto = normalizar(evento.get("reloj_vectorial_recibido") or {})
        nodo = evento.get("nodo") or f"docente-{evento.get('actor_id') or 'sistema'}"
        resultado = reconciliar_vectores(estado.reloj_vectorial, remoto)
        combinado = resultado["reloj_combinado"]
        return incrementar_vector(combinado, nodo), resultado["estado"]


STRATEGIES = {
    "m0": NoAuditStrategy,
    "m1": FlatAuditStrategy,
    "m2": HashChainAuditStrategy,
    "m3": VectorClockAuditStrategy,
}
