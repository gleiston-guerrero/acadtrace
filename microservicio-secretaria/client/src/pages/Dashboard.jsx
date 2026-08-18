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
    id: 'asignaciones', label: 'Asignaciones', path: '/asignaciones',
    desc: 'Docentes por curso, materia y paralelo',
    color: 'bg-fuchsia-50', iconColor: 'text-fuchsia-500',
    icon: <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" /></svg>,
  },
  {
    id: 'usuarios', label: 'Usuarios', path: '/usuarios',
    desc: 'Gestión de docentes y usuarios',
    color: 'bg-blue-50', iconColor: 'text-blue-500',
    icon: <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" /></svg>,
  },
  {
    id: 'promocion', label: 'Promoción', path: '/promocion',
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
  {
    id: 'calendario', label: 'Calendario', path: '/calendario',
    desc: 'Eventos, periodos y feriados',
    color: 'bg-orange-50', iconColor: 'text-orange-500',
    icon: <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" /></svg>,
  },
  {
    id: 'representantes', label: 'Representantes', path: '/representantes',
    desc: 'Padres, madres y tutores de estudiantes',
    color: 'bg-cyan-50', iconColor: 'text-cyan-500',
    icon: <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M17 20h5v-2a4 4 0 00-3-3.87M9 20H4v-2a4 4 0 013-3.87m6-1.13a4 4 0 10-4-4 4 4 0 004 4zm6 0a4 4 0 10-4-4" /></svg>,
  },
  {
    id: 'importacion', label: 'Importación masiva', path: '/importacion-masiva',
    desc: 'Carga de estudiantes desde Excel, CSV o PDF',
    color: 'bg-lime-50', iconColor: 'text-lime-600',
    icon: <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M12 12v9m0-9l-3 3m3-3l3 3" /></svg>,
  },
  {
    id: 'historial', label: 'Historial académico', path: '/historial',
    desc: 'Años cursados, notas y promociones por estudiante',
    color: 'bg-indigo-50', iconColor: 'text-indigo-500',
    icon: <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>,
  },
  {
    id: 'auditoria', label: 'Auditoría', path: '/auditoria',
    desc: 'CRUD sensible, accesos y llamadas entre microservicios',
    color: 'bg-red-50', iconColor: 'text-red-500', soloDirector: true,
    icon: <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>,
  },
];

export default function Dashboard() {
  const [stats, setStats] = useState(null);
  const [anoActual, setAnoActual] = useState(null);
  const [busqueda, setBusqueda] = useState('');
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

  const roles = JSON.parse(localStorage.getItem('roles') || '[]');
  const esDirector = roles.includes('DIRECTOR');

  const modulosFiltrados = MODULOS
    .filter(m => !m.soloDirector || esDirector)
    .filter(m =>
      m.label.toLowerCase().includes(busqueda.toLowerCase()) ||
      m.desc.toLowerCase().includes(busqueda.toLowerCase())
    );

  return (
    <Layout breadcrumb={['Inicio']} headerRight={
      <div className="relative">
        <input
          type="text"
          placeholder="Buscar..."
          value={busqueda}
          onChange={e => setBusqueda(e.target.value)}
          className="pl-3 pr-8 py-1.5 text-xs border border-slate-200 rounded-lg focus:outline-none bg-slate-50 w-44"
        />
        <svg className="w-4 h-4 text-slate-400 absolute right-2 top-1/2 -translate-y-1/2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
      </div>
    }>
      <div className="flex gap-4 items-start">

        {/* PANEL IZQUIERDO — igual al de sga-principal */}
        <aside className="w-64 flex-shrink-0 hidden lg:block space-y-3">
          <div className="rounded-xl border-2 border-dashed border-slate-200 p-4 flex flex-col items-center justify-center text-center h-48 text-slate-400 hover:border-opacity-60 transition cursor-pointer">
            <svg className="w-8 h-8 mb-2 text-slate-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
            <p className="text-xs">Imagen informativa</p>
            <p className="text-xs text-slate-300 mt-1">Avisos y comunicados</p>
          </div>

          <div className="rounded-xl border-2 border-dashed border-slate-200 p-4 flex flex-col items-center justify-center text-center h-48 text-slate-400 transition cursor-pointer">
            <svg className="w-8 h-8 mb-2 text-slate-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
            <p className="text-xs">Imagen informativa</p>
            <p className="text-xs text-slate-300 mt-1">Eventos y noticias</p>
          </div>
        </aside>

        {/* CONTENIDO */}
        <div className="flex-1 min-w-0">
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

          {/* Módulos — mismo estilo de card que sga-principal (icono arriba, centrado) */}
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4">
            {modulosFiltrados.map((m) => (
              <button
                key={m.id}
                onClick={() => navigate(m.path)}
                className="bg-white border border-slate-200 rounded-xl p-5 flex flex-col items-center gap-3 hover:shadow-md transition-all group text-center"
                onMouseEnter={e => e.currentTarget.style.borderColor = PRIMARY}
                onMouseLeave={e => e.currentTarget.style.borderColor = ''}
              >
                <div className={`${m.color} p-3 rounded-xl ${m.iconColor}`}>{m.icon}</div>
                <div>
                  <p className="text-sm font-semibold text-slate-700 group-hover:text-[#243A76] transition">{m.label}</p>
                  <p className="text-xs text-slate-400 mt-0.5 leading-tight">{m.desc}</p>
                </div>
              </button>
            ))}
            {modulosFiltrados.length === 0 && (
              <div className="col-span-2 sm:col-span-3 md:col-span-4 text-center py-10 text-slate-400 text-sm">
                No se encontraron módulos con "{busqueda}"
              </div>
            )}
          </div>
        </div>
      </div>
    </Layout>
  );
}
