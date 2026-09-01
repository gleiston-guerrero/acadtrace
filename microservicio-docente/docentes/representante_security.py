import os

import jwt
from django.db import connection
from rest_framework import authentication, exceptions, permissions


class RepresentanteJWTAuthentication(authentication.BaseAuthentication):
    def authenticate(self, request):
        try:
            header = authentication.get_authorization_header(request).decode("ascii")
        except UnicodeDecodeError as exc:
            raise exceptions.AuthenticationFailed("Encabezado Bearer inválido") from exc
        if not header:
            return None
        parts = header.split()
        if len(parts) != 2 or parts[0].lower() != "bearer":
            raise exceptions.AuthenticationFailed("Encabezado Bearer inválido")
        secret = os.environ.get("JWT_SECRET")
        if not secret:
            raise exceptions.AuthenticationFailed("JWT_SECRET no configurado")
        try:
            claims = jwt.decode(parts[1], secret, algorithms=["HS384"])
        except jwt.ExpiredSignatureError as exc:
            raise exceptions.AuthenticationFailed("Token expirado") from exc
        except jwt.InvalidTokenError as exc:
            raise exceptions.AuthenticationFailed("Token inválido") from exc
        username = claims.get("sub")
        roles = claims.get("roles", [])
        if not username:
            raise exceptions.AuthenticationFailed("Token sin sujeto")
        user = type("AuthenticatedUser", (), {
            "is_authenticated": True, "username": username, "roles": roles
        })()
        return user, claims


class EsRepresentante(permissions.BasePermission):
    message = "Se requiere REPRESENTANTE"

    def has_permission(self, request, view):
        return bool(request.user and "REPRESENTANTE" in request.user.roles)


def matriculas_autorizadas(username, id_estudiante):
    with connection.cursor() as cursor:
        cursor.execute(
            "SELECT EXISTS (SELECT 1 FROM sga_principal.estudiantes WHERE id_estudiante = %s)",
            [id_estudiante],
        )
        if not cursor.fetchone()[0]:
            raise exceptions.NotFound("Estudiante no encontrado")
        cursor.execute(
            """
            SELECT m.id_matricula
            FROM sga_principal.matriculas m
            JOIN sga_principal.estudiantes e ON e.id_estudiante = m.id_estudiante
            JOIN sga_principal.representantes r ON r.id_representante = e.id_representante
            JOIN sga_principal.usuarios u ON u.id_usuario = r.id_usuario
            WHERE u.username = %s AND e.id_estudiante = %s AND m.estado = 'ACTIVA'
            """,
            [username, id_estudiante],
        )
        ids = [row[0] for row in cursor.fetchall()]
    if not ids:
        raise exceptions.PermissionDenied("Estudiante no asociado al representante")
    return ids
