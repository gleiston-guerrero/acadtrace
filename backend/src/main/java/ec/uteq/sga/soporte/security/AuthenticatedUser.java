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
}
