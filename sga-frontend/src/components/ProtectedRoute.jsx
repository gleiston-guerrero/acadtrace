import { Navigate } from "react-router-dom";

export default function ProtectedRoute({ children, requiredRoles }) {
    const token = localStorage.getItem("token");
    const rolesStr = localStorage.getItem("roles");
    const roles = rolesStr ? JSON.parse(rolesStr) : [];

    if (!token) {
        return <Navigate to="/login" />;
    }

    if (requiredRoles && requiredRoles.length > 0) {
        const hasRole = requiredRoles.some(role => roles.includes(role));
        if (!hasRole) {
            // Redirigir al inicio correspondiente según el rol
            if (roles.includes("DOCENTE")) {
                return <Navigate to="/docente" />;
            } else {
                return <Navigate to="/dashboard" />;
            }
        }
    }

    return children;
}
