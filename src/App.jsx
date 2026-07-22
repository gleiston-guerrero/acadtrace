import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import Soporte  from "./pages/Soporte";
import Usuarios from "./pages/Usuarios";
import Dashboard from "./pages/Dashboard";

// El login vive únicamente en el SGA Principal. Aquí solo se entra por handoff SSO
// (ver capturarSesionSSO en main.jsx). Sin token, se redirige al login del principal.
function PrivateRoute({ children }) {
    const token = localStorage.getItem("token");
    if (!token) {
        window.location.href = "http://localhost:5173/login";
        return null;
    }
    return children;
}

function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/"          element={<Navigate to="/dashboard" />} />
                <Route path="/soporte"   element={<PrivateRoute><Soporte /></PrivateRoute>} />
                <Route path="/usuarios"  element={<PrivateRoute><Usuarios /></PrivateRoute>} />
                <Route path="/dashboard" element={<PrivateRoute><Dashboard /></PrivateRoute>} />
                <Route path="*"          element={<Navigate to="/dashboard" />} />
            </Routes>
        </BrowserRouter>
    );
}

export default App;
