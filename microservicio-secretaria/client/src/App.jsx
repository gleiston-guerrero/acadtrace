import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Dashboard from './pages/Dashboard';
import Estudiantes from './pages/Estudiantes';
import Grados from './pages/Grados';
import Matriculas from './pages/Matriculas';
import Usuarios from './pages/Usuarios';
import Calendario from './pages/Calendario';
import Promocion from './pages/Promocion';
import Reportes from './pages/Reportes';
import Representantes from './pages/Representantes';
import ImportacionMasiva from './pages/ImportacionMasiva';
import Historial from './pages/Historial';
import Auditoria from './pages/Auditoria';
import { CambiarPassword } from './pages/Extras';

// El login vive únicamente en el SGA Principal. Aquí solo se entra por handoff SSO
// (ver capturarSesionSSO en main.jsx). Sin token, se redirige al login del principal.
function PrivateRoute({ children }) {
  const token = localStorage.getItem('token');
  if (!token) {
    window.location.href = 'http://localhost:5174/login';
    return null;
  }
  return children;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<PrivateRoute><Dashboard /></PrivateRoute>} />
        <Route path="/estudiantes" element={<PrivateRoute><Estudiantes /></PrivateRoute>} />
        <Route path="/grados" element={<PrivateRoute><Grados /></PrivateRoute>} />
        <Route path="/matriculas" element={<PrivateRoute><Matriculas /></PrivateRoute>} />
        <Route path="/usuarios" element={<PrivateRoute><Usuarios /></PrivateRoute>} />
        <Route path="/promocion" element={<PrivateRoute><Promocion /></PrivateRoute>} />
        <Route path="/reportes" element={<PrivateRoute><Reportes /></PrivateRoute>} />
        <Route path="/representantes" element={<PrivateRoute><Representantes /></PrivateRoute>} />
        <Route path="/importacion-masiva" element={<PrivateRoute><ImportacionMasiva /></PrivateRoute>} />
        <Route path="/historial" element={<PrivateRoute><Historial /></PrivateRoute>} />
        <Route path="/calendario" element={<PrivateRoute><Calendario /></PrivateRoute>} />
        <Route path="/auditoria" element={<PrivateRoute><Auditoria /></PrivateRoute>} />
        <Route path="/cambiar-password" element={<PrivateRoute><CambiarPassword /></PrivateRoute>} />
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
