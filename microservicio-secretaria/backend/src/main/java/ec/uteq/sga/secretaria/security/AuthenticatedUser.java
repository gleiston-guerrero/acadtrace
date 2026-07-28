package ec.uteq.sga.secretaria.security;

import java.util.List;

/**
 * Equivalente a req.user en el middleware Node (username + roles del JWT).
 */
public record AuthenticatedUser(String username, List<String> roles) {

    public static final String REQUEST_ATTRIBUTE = "authenticatedUser";

    /**
     * DIRECTOR es el rol de mayor jerarquia en sga_principal.roles (no existe
     * un rol "ADMIN" separado): gestion de usuarios (crear, resetear
     * password, asignar roles, cambiar estado) queda reservada a DIRECTOR.
     */
    public boolean isDirector() {
        return roles != null && roles.contains("DIRECTOR");
    }
}
