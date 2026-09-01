package ec.uteq.sga.soporte.security;

import java.util.List;

/**
 * Usuario autenticado (username + roles del JWT emitido por sga-principal).
 */
public record AuthenticatedUser(String username, List<String> roles) {

    public static final String REQUEST_ATTRIBUTE = "authenticatedUser";

    /**
     * DIRECTOR es el rol de mayor jerarquia en sga_principal.roles: puede
     * reasignar y cerrar cualquier ticket. SOPORTE_TECNICO gestiona los suyos.
     */
    public boolean isDirector() {
        return roles != null && roles.contains("DIRECTOR");
    }

    /**
     * True si el usuario es del equipo de soporte (SOPORTE_TECNICO,
     * DIRECTOR o ADMINISTRADOR). Se usa para las acciones exclusivas del
     * modulo: ver TODOS los tickets, asignar, escalar, cerrar y dejar notas
     * internas. Cualquier otro usuario autenticado solo puede crear tickets
     * y conversar en ellos (ver mis-tickets / comentarios).
     */
    public boolean isTecnicoOrDirector() {
        return roles != null
                && (roles.contains("SOPORTE_TECNICO") || roles.contains("DIRECTOR") || roles.contains("ADMINISTRADOR"));
    }
}