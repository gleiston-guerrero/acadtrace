import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import Login from "./pages/login/Login";
import Portales from "./pages/portales/Portales";
import Dashboard from "./pages/dashboard/Dashboard";
import Usuarios from "./pages/usuarios/Usuarios";
import CambiarPassword from "./pages/cambiar-password/CambiarPassword"
import Calificaciones from "./pages/calificaciones/Calificaciones";
import AnosLectivos from "./pages/anos-lectivos/AnosLectivos";
import Grados from "./pages/grados/Grados";
import Asignaciones from "./pages/asignaciones/Asignaciones";
import Asignaturas from "./pages/asignaturas/Asignaturas";
import ConfiguracionCalificacion from "./pages/configuracion/ConfiguracionCalificacion";
import ConsultaAsistencias from "./pages/asistencias/ConsultaAsistencias";

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
                {/* /estudiantes se retiro: sga-secretaria es la unica duena de ese dominio (ver Dashboard.jsx handleModulo) */}
                <Route path="/asistencias" element={<ConsultaAsistencias />} />
                <Route path="/calificaciones" element={<Calificaciones />} />
                <Route path="/anos-lectivos" element={<AnosLectivos />} />
                <Route path="/grados" element={<Grados />} />
                <Route path="/asignaciones" element={<Asignaciones />} />
                <Route path="/asignaturas" element={<Asignaturas />} />
                <Route path="/configuracion/calificacion" element={<ConfiguracionCalificacion />} />
                <Route path="*" element={<Navigate to="/dashboard" />} />
            </Routes>
        </BrowserRouter>
    );
}

export default App;