import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ToastProvider } from './components/Toast';
import { ThemeProvider } from './context/ThemeContext';
import { I18nProvider } from './context/I18nContext';
import Login from './pages/Login';
import Portales from './pages/Portales';
import Dashboard from './pages/Dashboard';
import Estudiantes from './pages/Estudiantes';
import Grados from './pages/Grados';
import Asignaturas from './pages/Asignaturas';
import Asignaciones from './pages/Asignaciones';
import Calificaciones from './pages/Calificaciones';
import Horarios from './pages/Horarios';
import ConsultaAsistencias from './pages/ConsultaAsistencias';
import Matriculas from './pages/Matriculas';
import Usuarios from './pages/Usuarios';
import Calendario from './pages/Calendario';
import Promocion from './pages/Promocion';
import Reportes from './pages/Reportes';
import Representantes from './pages/Representantes';
import ImportacionMasiva from './pages/ImportacionMasiva';
import Historial from './pages/Historial';
import AnosLectivos from './pages/AnosLectivos';
import Auditoria from './pages/Auditoria';
import { CambiarPassword } from './pages/Extras';

function PrivateRoute({ children }) {
  const token = localStorage.getItem('token');
  if (!token) {
    return <Navigate to="/login" replace />;
  }
  return children;
}

export default function App() {
  return (
    <ThemeProvider>
      <I18nProvider>
        <ToastProvider>
          <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/portales" element={<Portales />} />
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<PrivateRoute><Dashboard /></PrivateRoute>} />
        <Route path="/estudiantes" element={<PrivateRoute><Estudiantes /></PrivateRoute>} />
        <Route path="/grados" element={<PrivateRoute><Grados /></PrivateRoute>} />
        <Route path="/asignaturas" element={<PrivateRoute><Asignaturas /></PrivateRoute>} />
        <Route path="/asignaciones" element={<PrivateRoute><Asignaciones /></PrivateRoute>} />
        <Route path="/calificaciones" element={<PrivateRoute><Calificaciones /></PrivateRoute>} />
        <Route path="/horarios" element={<PrivateRoute><Horarios /></PrivateRoute>} />
        <Route path="/asistencias" element={<PrivateRoute><ConsultaAsistencias /></PrivateRoute>} />
        <Route path="/matriculas" element={<PrivateRoute><Matriculas /></PrivateRoute>} />
        <Route path="/usuarios" element={<PrivateRoute><Usuarios /></PrivateRoute>} />
        <Route path="/promocion" element={<PrivateRoute><Promocion /></PrivateRoute>} />
        <Route path="/reportes" element={<PrivateRoute><Reportes /></PrivateRoute>} />
        <Route path="/representantes" element={<PrivateRoute><Representantes /></PrivateRoute>} />
        <Route path="/importacion-masiva" element={<PrivateRoute><ImportacionMasiva /></PrivateRoute>} />
        <Route path="/historial" element={<PrivateRoute><Historial /></PrivateRoute>} />
        <Route path="/calendario" element={<PrivateRoute><Calendario /></PrivateRoute>} />
        <Route path="/anos-lectivos" element={<PrivateRoute><AnosLectivos /></PrivateRoute>} />
        <Route path="/auditoria" element={<PrivateRoute><Auditoria /></PrivateRoute>} />
        <Route path="/cambiar-password" element={<PrivateRoute><CambiarPassword /></PrivateRoute>} />
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
          </BrowserRouter>
        </ToastProvider>
      </I18nProvider>
    </ThemeProvider>
  );
}
