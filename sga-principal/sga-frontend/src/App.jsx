import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { ToastProvider } from "./context/ToastContext";
import { ConfirmProvider } from "./context/ConfirmContext";
import { ThemeProvider } from "./context/ThemeContext";
import { I18nProvider } from "./context/I18nContext";
import ProtectedRoute from "./components/ProtectedRoute";

import Login from "./pages/login/Login";
import Portales from "./pages/portales/Portales";
import Dashboard from "./pages/dashboard/Dashboard";
import Usuarios from "./pages/usuarios/Usuarios";
import CambiarPassword from "./pages/cambiar-password/CambiarPassword";
import Calificaciones from "./pages/calificaciones/Calificaciones";
import AnosLectivos from "./pages/anos-lectivos/AnosLectivos";
import Grados from "./pages/grados/Grados";
import Asignaciones from "./pages/asignaciones/Asignaciones";
import Asignaturas from "./pages/asignaturas/Asignaturas";
import ConfiguracionGeneral from "./pages/configuracion/ConfiguracionGeneral";
import Estudiantes from "./pages/estudiantes/Estudiantes";
import ConsultaAsistencias from "./pages/asistencias/ConsultaAsistencias";
import Horarios from "./pages/horarios/Horarios";
import Auditoria from "./pages/auditoria/Auditoria";
import Matriculas from "./pages/matriculas/Matriculas";
import About from "./pages/about/About";

function App() {
  return (
    <ThemeProvider>
      <I18nProvider>
        <ToastProvider>
          <ConfirmProvider>
            <BrowserRouter>
              <Routes>
                {/* 5 Rutas Canónicas del Módulo B */}
                <Route path="/" element={<ProtectedRoute><Dashboard /></ProtectedRoute>} />
                <Route path="/login" element={<Login />} />
                <Route path="/calificaciones" element={<ProtectedRoute><Calificaciones /></ProtectedRoute>} />
                <Route path="/settings" element={<ProtectedRoute><ConfiguracionGeneral /></ProtectedRoute>} />
                <Route path="/about" element={<About />} />

                {/* Rutas Operativas y de Gestión */}
                <Route path="/dashboard" element={<ProtectedRoute><Dashboard /></ProtectedRoute>} />
                <Route path="/portales" element={<Portales />} />
                <Route path="/cambiar-password" element={<ProtectedRoute><CambiarPassword /></ProtectedRoute>} />
                <Route path="/usuarios" element={<ProtectedRoute><Usuarios /></ProtectedRoute>} />
                <Route path="/estudiantes" element={<ProtectedRoute><Estudiantes /></ProtectedRoute>} />
                <Route path="/asistencias" element={<ProtectedRoute><ConsultaAsistencias /></ProtectedRoute>} />
                <Route path="/anos-lectivos" element={<ProtectedRoute><AnosLectivos /></ProtectedRoute>} />
                <Route path="/grados" element={<ProtectedRoute><Grados /></ProtectedRoute>} />
                <Route path="/asignaciones" element={<ProtectedRoute><Asignaciones /></ProtectedRoute>} />
                <Route path="/asignaturas" element={<ProtectedRoute><Asignaturas /></ProtectedRoute>} />
                <Route path="/horarios" element={<ProtectedRoute><Horarios /></ProtectedRoute>} />
                <Route path="/configuracion" element={<ProtectedRoute><ConfiguracionGeneral /></ProtectedRoute>} />
                <Route path="/configuracion/calificacion" element={<ProtectedRoute><ConfiguracionGeneral /></ProtectedRoute>} />
                <Route path="/auditoria" element={<ProtectedRoute><Auditoria /></ProtectedRoute>} />
                <Route path="/matriculas" element={<ProtectedRoute><Matriculas /></ProtectedRoute>} />

                {/* Fallback */}
                <Route path="*" element={<Navigate to="/dashboard" />} />
              </Routes>
            </BrowserRouter>
          </ConfirmProvider>
        </ToastProvider>
      </I18nProvider>
    </ThemeProvider>
  );
}

export default App;
