import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import Login from "./pages/login/Login";
import Portales from "./pages/portales/Portales";
import Dashboard from "./pages/dashboard/Dashboard";
import Usuarios from "./pages/usuarios/Usuarios";
import CambiarPassword from "./pages/cambiar-password/CambiarPassword"
import Estudiantes from "./pages/estudiantes/Estudiantes";
import Calificaciones from "./pages/calificaciones/Calificaciones";
import AnosLectivos from "./pages/anos-lectivos/AnosLectivos";
import Grados from "./pages/grados/Grados";
<<<<<<< HEAD
import Asignaciones from "./pages/asignaciones/Asignaciones";
=======
>>>>>>> 4e160d4f9a8a27c9b4c5f287a4a91ac9032cee60
import ConfiguracionCalificacion from "./pages/configuracion/ConfiguracionCalificacion";

function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/" element={<Navigate to="/login" />} />
                <Route path="/login" element={<Login />} />
                <Route path="/portales" element={<Portales />} />
                <Route path="/dashboard" element={<Dashboard />} />
                <Route path="/cambiar-password" element={<CambiarPassword />} />
                <Route path="/usuarios" element={<Usuarios />} />
                <Route path="/estudiantes" element={<Estudiantes />} />
                <Route path="/calificaciones" element={<Calificaciones />} />
                <Route path="/anos-lectivos" element={<AnosLectivos />} />
                <Route path="/grados" element={<Grados />} />
<<<<<<< HEAD
                <Route path="/asignaciones" element={<Asignaciones />} />
=======
>>>>>>> 4e160d4f9a8a27c9b4c5f287a4a91ac9032cee60
                <Route path="/configuracion/calificacion" element={<ConfiguracionCalificacion />} />
                <Route path="*" element={<Navigate to="/dashboard" />} />
            </Routes>
        </BrowserRouter>
    );
}

export default App;