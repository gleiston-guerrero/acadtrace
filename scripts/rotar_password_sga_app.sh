#!/usr/bin/env bash
# =============================================================================
# scripts/rotar_password_sga_app.sh
#
# Rotacion administrativa de la contrasena del rol de aplicacion sga_app.
#
# Justificacion: la migracion institucional V18 aprovisiona el rol con
# CREATE ROLE ... IF NOT EXISTS para garantizar idempotencia en bases nuevas,
# de modo que, si el rol ya existe, su contrasena no se actualiza. La rotacion
# periodica o la remediacion de credenciales exigen por tanto una operacion
# administrativa explicita, versionada y verificable, como la que implementa
# este script (procedimiento documentado en docs/db/ROTACION_SGA_APP.md).
#
# El script NO modifica esquemas, migraciones ni datos: solo altera la
# credencial del rol y verifica la restriccion append-only sobre la bitacora
# desde la propia sesion de aplicacion. No emite sentencias de escritura
# contra sga_principal.auditoria.
#
# Uso:
#   NUEVA_PASSWORD='<valor>' DB_HOST=192.0.2.1 scripts/rotar_password_sga_app.sh
#   scripts/rotar_password_sga_app.sh --nueva-password '<valor>'
#
# Parametros (entorno o banderas):
#   NUEVA_PASSWORD      Nueva contrasena del rol (obligatoria; minimo 12
#                       caracteres). Tambien via -n/--nueva-password.
#   DB_HOST             Host del motor PostgreSQL (obligatorio).
#   DB_PORT             Puerto (por defecto 5433, convencion del proyecto).
#   DB_NAME             Base de datos (por defecto sga).
#   DB_ADMIN_USER       Rol administrador que ejecuta ALTER ROLE (por defecto
#                       postgres).
#   DB_ADMIN_PASSWORD   Contrasena del rol administrador (opcional; si se
#                       omite se recurre a la autenticacion del entorno).
#
# Codigo de salida: 0 si la rotacion y la verificacion son correctas;
# 1 si la verificacion falla o la operacion SQL no puede completarse;
# 2 si los parametros de entrada son invalidos.
# =============================================================================

set -euo pipefail

readonly ROL_APLICACION="sga_app"
readonly TABLA_BITACORA="sga_principal.auditoria"
readonly LONGITUD_MINIMA=12

NUEVA_PASSWORD="${NUEVA_PASSWORD:-}"
DB_HOST="${DB_HOST:-}"
DB_PORT="${DB_PORT:-5433}"
DB_NAME="${DB_NAME:-sga}"
DB_ADMIN_USER="${DB_ADMIN_USER:-postgres}"
DB_ADMIN_PASSWORD="${DB_ADMIN_PASSWORD:-}"

muestra_uso() {
    cat <<'EOF'
Uso: scripts/rotar_password_sga_app.sh [--nueva-password <valor>]

Rotacion de la contrasena del rol sga_app con verificacion inmediata de la
restriccion append-only sobre sga_principal.auditoria.

Variables de entorno admitidas:
  NUEVA_PASSWORD      Nueva contrasena (obligatoria si no se usa la bandera)
  DB_HOST             Host PostgreSQL (obligatoria)
  DB_PORT             Puerto (defecto 5433)
  DB_NAME             Base de datos (defecto sga)
  DB_ADMIN_USER       Rol administrador (defecto postgres)
  DB_ADMIN_PASSWORD   Contrasena del rol administrador (opcional)

Ejemplo:
  NUEVA_PASSWORD='...' DB_HOST=192.0.2.1 \
      scripts/rotar_password_sga_app.sh
EOF
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        -n|--nueva-password)
            [[ $# -ge 2 ]] || { echo "ERROR: la bandera $1 requiere un valor." >&2; exit 2; }
            NUEVA_PASSWORD="$2"
            shift 2
            ;;
        -h|--help)
            muestra_uso
            exit 0
            ;;
        *)
            echo "ERROR: parametro desconocido: $1" >&2
            muestra_uso >&2
            exit 2
            ;;
    esac
done

# --- Limpieza de credenciales al terminar (mejor esfuerzo a nivel de shell) --
limpiar_variables_sensibles() {
    NUEVA_PASSWORD=""
    PASSWORD_ESCAPADA=""
    DB_ADMIN_PASSWORD=""
    unset NUEVA_PASSWORD PASSWORD_ESCAPADA DB_ADMIN_PASSWORD PGPASSWORD 2>/dev/null || true
}
trap limpiar_variables_sensibles EXIT

fallos() {
    echo "ERROR: $1" >&2
    exit "${2:-1}"
}

# --- Validacion de requisitos previos ---------------------------------------
[[ -n "$DB_HOST" ]] || fallos "DB_HOST no esta definido." 2
[[ "$DB_PORT" =~ ^[0-9]+$ ]] || fallos "DB_PORT debe ser numerico: ${DB_PORT}" 2
[[ -n "$DB_NAME" ]] || fallos "DB_NAME no puede estar vacio." 2
[[ -n "$DB_ADMIN_USER" ]] || fallos "DB_ADMIN_USER no puede estar vacio." 2
[[ -n "$NUEVA_PASSWORD" ]] || {
    echo "ERROR: NUEVA_PASSWORD no esta definida." >&2
    muestra_uso >&2
    exit 2
}
[[ ${#NUEVA_PASSWORD} -ge $LONGITUD_MINIMA ]] || \
    fallos "la nueva contrasena debe tener al menos ${LONGITUD_MINIMA} caracteres." 2

command -v psql >/dev/null 2>&1 || fallos "no se encontro 'psql' en el PATH." 2

# Escapado SQL estandar de comillas simples para el literal de PASSWORD.
COMILLA="'"
PASSWORD_ESCAPADA="${NUEVA_PASSWORD//"$COMILLA"/"$COMILLA$COMILLA"}"

PSQL_BASE=(-h "$DB_HOST" -p "$DB_PORT" -d "$DB_NAME" -v ON_ERROR_STOP=1 -tA)

# --- Conexiones --------------------------------------------------------------
psql_admin() {
    if [[ -n "$DB_ADMIN_PASSWORD" ]]; then
        env PGPASSWORD="$DB_ADMIN_PASSWORD" psql "${PSQL_BASE[@]}" -U "$DB_ADMIN_USER" "$@"
    else
        psql "${PSQL_BASE[@]}" -U "$DB_ADMIN_USER" "$@"
    fi
}

psql_aplicacion() {
    env PGPASSWORD="$NUEVA_PASSWORD" psql "${PSQL_BASE[@]}" -U "$ROL_APLICACION" "$@"
}

valor_aplicacion() {
    psql_aplicacion -c "$1"
}

esperado() {
    # $1: descripcion; $2: valor obtenido; $3: valor esperado
    if [[ "$2" != "$3" ]]; then
        fallos "verificacion fallida: $1 (obtenido: '${2}', esperado: '${3}')."
    fi
    echo "OK: $1 -> $2"
}

echo "Rotacion de contrasena del rol ${ROL_APLICACION} sobre ${DB_HOST}:${DB_PORT}/${DB_NAME}"

# --- 1. Precondicion: conectividad administrativa y existencia del rol ------
if ! psql_admin -c "SELECT 1" >/dev/null 2>&1; then
    fallos "no hay conexion administrativa como ${DB_ADMIN_USER} en ${DB_HOST}:${DB_PORT}/${DB_NAME}."
fi

EXSISTE_ROL="$(psql_admin -c \
    "SELECT count(*) FROM pg_roles WHERE rolname = '${ROL_APLICACION}'")"
if [[ "$EXSISTE_ROL" != "1" ]]; then
    fallos "el rol ${ROL_APLICACION} no existe; debe aprovisionarlo la migracion V18 antes de rotar."
fi
echo "OK: conexion administrativa verificada y rol ${ROL_APLICACION} presente."

# --- 2. Rotacion de la credencial -------------------------------------------
printf 'ALTER ROLE %s WITH PASSWORD %s;\n' \
    "$ROL_APLICACION" "'${PASSWORD_ESCAPADA}'" | psql_admin >/dev/null
echo "OK: contrasena del rol ${ROL_APLICACION} actualizada."

# --- 3. Verificacion inmediata autenticando como sga_app --------------------
# Solo consultas de catalogo y de privilegios: no se envian sentencias de
# escritura contra la bitacora institucional.
USUARIO_EFECTIVO="$(valor_aplicacion "SELECT current_user")"
esperado "autenticacion del rol con la nueva contrasena" "$USUARIO_EFECTIVO" "$ROL_APLICACION"

ROLSUPER="$(valor_aplicacion \
    "SELECT rolsuper FROM pg_roles WHERE rolname = current_user")"
esperado "el rol de aplicacion no es superusuario (rolsuper = false)" "$ROLSUPER" "f"

PERM_INSERT="$(valor_aplicacion \
    "SELECT has_table_privilege(current_user, '${TABLA_BITACORA}', 'INSERT')")"
esperado "INSERT permitido sobre ${TABLA_BITACORA} (append-only)" "$PERM_INSERT" "t"

for PRIVILEGIO in UPDATE DELETE TRUNCATE; do
    PERM_NEGADO="$(valor_aplicacion \
        "SELECT has_table_privilege(current_user, '${TABLA_BITACORA}', '${PRIVILEGIO}')")"
    esperado "${PRIVILEGIO} denegado sobre ${TABLA_BITACORA}" "$PERM_NEGADO" "f"
done

echo "Verificacion completa: ${ROL_APLICACION} autentica con la nueva credencial y no dispone de privilegios de modificacion sobre ${TABLA_BITACORA}."
echo "Siguiente paso: actualizar las variables de entorno y reiniciar los servicios conforme a docs/db/ROTACION_SGA_APP.md."
exit 0
