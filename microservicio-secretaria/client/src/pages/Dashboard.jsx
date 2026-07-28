import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Layout from '../components/Layout';
import api, { apiPrincipal } from '../utils/api';

const PRIMARY = '#243A76';

const MODULOS = [
  {
    id: 'estudiantes', label: 'Estudiantes', path: '/estudiantes',
    desc: 'Registro y gestión de estudiantes',
    color: 'bg-green-50', iconColor: 'text-green-500',
    icon: <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 14l9-5-9-5-9 5 9 5zm0 0l6.16-3.422a12.083 12.083 0 01.665 6.479A11.952 11.952 0 0012 20.055a11.952 11.952 0 00-6.824-2.998 12.078 12.078 0 01.665-6.479L12 14z" /></svg>,
  },
  {
    id: 'grados', label: 'Grados y Cursos', path: '/grados',
    desc: 'Gestión de grados y paralelos',
    color: 'bg-teal-50', iconColor: 'text-teal-500',
    icon: <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" /></svg>,
  },
  {
    id: 'matriculas', label: 'Matrículas', path: '/matriculas',
    desc: 'Registro y consulta de matrículas',
    color: 'bg-purple-50', iconColor: 'text-purple-500',
    icon: <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>,
  },
  {
    id: 'usuarios', label: 'Usuarios', path: '/usuarios',
    desc: 'Gestión de docentes y usuarios',
    color: 'bg-blue-50', iconColor: 'text-blue-500',
    icon: <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" /></svg>,
  },
  {
    id: 'historial', label: 'Promoción', path: '/historial',
    desc: 'Registro de resultados y promoción',
    color: 'bg-amber-50', iconColor: 'text-amber-500',
    icon: <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" /></svg>,
  },
  {
    id: 'reportes', label: 'Reportes', path: '/reportes',
    desc: 'Certificados y documentos en PDF',
    color: 'bg-rose-50', iconColor: 'text-rose-500',
    icon: <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>,
  },
];

export default function Dashboard() {
  const [stats, setStats] = useState(null);
  const [anoActual, setAnoActual] = useState(null);
  const navigate = useNavigate();
  const username = localStorage.getItem('username') || 'Secretario';

  useEffect(() => {
    apiPrincipal.get('/anos-lectivos/actual').then(r => {
      setAnoActual(r.data);
      if (r.data?.idAnoLectivo) {
        api.get(`/reportes/estadisticas/${r.data.idAnoLectivo}`).then(s => setStats(s.data)).catch(() => {});
      }
    }).catch(() => {});
  }, []);

  return (
    <Layout breadcrumb={['Inicio']}>
      <div className="mb-5">
        <h1 className="text-lg font-bold text-slate-700">
          Bienvenido, <span style={{ color: PRIMARY }} className="capitalize">{username}</span>
        </h1>
        {anoActual && (
          <p className="text-slate-400 text-xs mt-0.5">
            Año lectivo activo: <span style={{ color: PRIMARY }} className="font-semibold">{anoActual.nombre}</span>
          </p>
        )}
      </div>

      {/* Stats */}
      {stats && (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-6">
          {[
            { label: 'Total matrículas', val: stats.totales?.total || 0, color: 'text-blue-600', bg: 'bg-blue-50' },
            { label: 'Activas', val: stats.totales?.activas || 0, color: 'text-green-600', bg: 'bg-green-50' },
            { label: 'Masculino', val: stats.totales?.masculino || 0, color: 'text-indigo-600', bg: 'bg-indigo-50' },
            { label: 'Femenino', val: stats.totales?.femenino || 0, color: 'text-pink-600', bg: 'bg-pink-50' },
          ].map(s => (
            <div key={s.label} className={`${s.bg} rounded-xl p-4 border border-white shadow-sm`}>
              <p className={`text-2xl font-bold ${s.color}`}>{s.val}</p>
              <p className="text-xs text-slate-500 mt-0.5">{s.label}</p>
            </div>
          ))}
        </div>
      )}

      {/* Módulos */}
      <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
        {MODULOS.map((m) => (
          <button
            key={m.id}
            onClick={() => navigate(m.path)}
            className="bg-white border border-slate-200 rounded-xl p-5 flex flex-col items-center gap-3 hover:shadow-md transition-all group text-center"
            onMouseEnter={e => e.currentTarget.style.borderColor = PRIMARY}
            onMouseLeave={e => e.currentTarget.style.borderColor = ''}
          >
            <div className={`${m.color} p-3 rounded-xl ${m.iconColor}`}>{m.icon}</div>
            <div>
              <p className="text-sm font-semibold text-slate-700">{m.label}</p>
              <p className="text-xs text-slate-400 mt-0.5 leading-tight">{m.desc}</p>
            </div>
          </button>
        ))}
      </div>
    </Layout>
  );
}
