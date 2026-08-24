import { useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ToastProvider } from './components/Toast';
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
  const [token, setToken] = useState(() => localStorage.getItem('token'));

  if (!token) {
    const handleLoginDev = (role = 'SECRETARIO') => {
      localStorage.setItem('token', 'dev-token-secretaria-2026');
      localStorage.setItem('username', role.toLowerCase());
      localStorage.setItem('roles', JSON.stringify([role, 'ADMIN']));
      setToken('dev-token-secretaria-2026');
    };

    return (
      <div className="min-h-screen bg-slate-900 flex items-center justify-center p-4">
        <div className="bg-white rounded-3xl max-w-sm w-full p-8 shadow-2xl text-center space-y-6 animate-in fade-in zoom-in duration-200">
          <div className="w-16 h-16 rounded-2xl bg-[#243A76] text-white flex items-center justify-center mx-auto shadow-lg text-2xl font-bold">
            🏢
          </div>
          <div>
            <h1 className="text-lg font-extrabold text-slate-800">Módulo de Secretaría</h1>
            <p className="text-xs text-slate-500 mt-1">Escuela Provincias Unidas</p>
          </div>
          <div className="p-4 bg-slate-50 border border-slate-200 rounded-2xl text-left space-y-1">
            <p className="text-xs font-bold text-slate-700">Acceso Directo</p>
            <p className="text-[11px] text-slate-500">Haz clic para entrar al módulo de secretaría y comenzar a probar.</p>
          </div>
          <button
            onClick={() => handleLoginDev('SECRETARIO')}
            className="w-full py-3 px-4 rounded-xl text-xs font-bold text-white bg-[#243A76] hover:bg-[#1b2b58] shadow-md transition transform active:scale-98 cursor-pointer"
          >
            Ingresar como Secretario(a) →
          </button>
        </div>
      </div>
    );
  }
  return children;
}

export default function App() {
  return (
    <ToastProvider>
    <BrowserRouter>
      <Routes>
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
  );
}
