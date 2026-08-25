import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Layout from '../components/Layout';
import { apiPrincipal } from '../utils/api';

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
    id: 'asignaturas', label: 'Asignaturas y Malla', path: '/asignaturas',
    desc: 'Catálogo de materias y malla curricular por grado',
    color: 'bg-yellow-50', iconColor: 'text-yellow-600',
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
    id: 'horarios', label: 'Horarios', path: '/horarios',
    desc: 'Grilla de horarios por curso y por docente',
    color: 'bg-sky-50', iconColor: 'text-sky-500',
    icon: <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>,
  },
  {
    id: 'asistencias', label: 'Consulta de Asistencias', path: '/asistencias',
    desc: 'Grilla de asistencia por materia y por estudiante',
    color: 'bg-emerald-50', iconColor: 'text-emerald-500',
    icon: <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 17v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" /></svg>,
  },
  {
    id: 'calificaciones', label: 'Calificaciones', path: '/calificaciones',
    desc: 'Sábana oficial de notas por curso y trimestre',
    color: 'bg-orange-50', iconColor: 'text-orange-500',
    icon: <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>,
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
    id: 'anos-lectivos', label: 'Años Lectivos y Periodos', path: '/anos-lectivos',
    desc: 'Gestión de períodos escolares, trimestres y fechas de corte',
    color: 'bg-teal-50', iconColor: 'text-teal-600',
    icon: <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" /></svg>,
  },
  {
    id: 'auditoria', label: 'Auditoría', path: '/auditoria',
    desc: 'CRUD sensible, accesos y llamadas entre microservicios',
    color: 'bg-red-50', iconColor: 'text-red-500', soloDirector: true,
    icon: <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>,
  },
];

export default function Dashboard() {
  const [anoActual, setAnoActual] = useState(null);
  const [busqueda, setBusqueda] = useState('');
  const [banner1, setBanner1] = useState(localStorage.getItem("sga_banner_1") || null);
  const [banner2, setBanner2] = useState(localStorage.getItem("sga_banner_2") || null);
  const [modalImagen, setModalImagen] = useState(null);
  const navigate = useNavigate();
  const username = localStorage.getItem('username') || 'Secretario';

  useEffect(() => {
    apiPrincipal.get('/anos-lectivos/actual').then(r => setAnoActual(r.data)).catch(() => {});
  }, []);

  const roles = JSON.parse(localStorage.getItem('roles') || '[]');
  const esDirector = roles.includes('DIRECTOR');
  const esAdmin = roles.length === 0 || roles.some(r => {
    const ro = (r.nombre || r.name || r || "").toString().toUpperCase();
    return ro.includes("ADMIN") || ro.includes("DIRECTOR") || ro.includes("SECRETAR") || ro.includes("RECTOR");
  });

  const handleUploadBanner = (num, e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (ev) => {
      const base64 = ev.target.result;
      if (num === 1) {
        setBanner1(base64);
        localStorage.setItem("sga_banner_1", base64);
      } else {
        setBanner2(base64);
        localStorage.setItem("sga_banner_2", base64);
      }
    };
    reader.readAsDataURL(file);
  };

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

        {/* PANEL IZQUIERDO — AFICHES Y BANNERS INFORMATIVOS */}
        <aside className="w-80 flex-shrink-0 bg-white border border-slate-200 rounded-2xl overflow-hidden p-4 hidden lg:flex flex-col gap-4">
          {/* Afiche 1: Avisos y Comunicados Oficiales */}
          <div className="relative group rounded-2xl overflow-hidden shadow-xs border border-slate-200 bg-white transition hover:shadow-md cursor-pointer">
            {banner1 ? (
              <div className="relative w-full">
                <img
                  src={banner1}
                  alt="Avisos y Comunicados"
                  onClick={() => setModalImagen({ src: banner1, title: "Aviso Importante — Escuela Provincias Unidas" })}
                  className="w-full h-auto object-contain block rounded-2xl"
                />
                <div className="absolute inset-0 bg-slate-950/40 opacity-0 group-hover:opacity-100 transition-all flex items-center justify-center gap-2 backdrop-blur-xs rounded-2xl">
                  <button
                    onClick={() => setModalImagen({ src: banner1, title: "Aviso Importante — Escuela Provincias Unidas" })}
                    className="px-3 py-1.5 bg-white text-slate-800 rounded-xl text-xs font-bold shadow-lg hover:bg-slate-50 transition flex items-center gap-1">
                    🔍 Ver
                  </button>
                  {esAdmin && (
                    <>
                      <label className="cursor-pointer px-3 py-1.5 bg-blue-600 text-white rounded-xl text-xs font-bold shadow-lg hover:bg-blue-700 transition flex items-center gap-1">
                        📷 Cambiar
                        <input type="file" accept="image/*" className="hidden" onChange={e => handleUploadBanner(1, e)} />
                      </label>
                      <button
                        onClick={(e) => { e.stopPropagation(); setBanner1(null); localStorage.removeItem("sga_banner_1"); }}
                        className="px-2.5 py-1.5 bg-rose-600 text-white rounded-xl text-xs font-bold shadow-lg hover:bg-rose-700 transition">
                        ✕
                      </button>
                    </>
                  )}
                </div>
              </div>
            ) : (
              <div
                onClick={() => !esAdmin && setModalImagen({ src: null, title: "Período Lectivo 2026-2027", desc: "Sistema de matrículas y calificaciones activo." })}
                className="bg-gradient-to-br from-[#1a2d5f] via-[#243A76] to-[#1e3a8a] p-5 text-white flex flex-col justify-between min-h-[220px]"
              >
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-[10px] font-black uppercase tracking-wider bg-white/20 px-2.5 py-0.5 rounded-full text-blue-100 backdrop-blur-xs">
                      📢 AVISO OFICIAL
                    </span>
                    <span className="text-[10px] text-blue-200 font-bold">2026 - 2027</span>
                  </div>
                  <h4 className="font-bold text-base leading-snug text-white mt-1">
                    Período Lectivo 2026-2027
                  </h4>
                  <p className="text-xs text-blue-100/90 mt-2 leading-relaxed font-sans">
                    Sistema de matrículas y registro de calificaciones 70/30 activo en toda la institución.
                  </p>
                </div>
                <div className="pt-3 border-t border-white/10 flex items-center justify-between text-xs text-blue-200">
                  <span className="font-medium text-[11px]">Escuela Provincias Unidas</span>
                  {esAdmin && (
                    <label className="cursor-pointer font-bold text-white bg-white/20 hover:bg-white/30 px-3 py-1 rounded-lg text-xs transition flex items-center gap-1">
                      📷 Subir afiche
                      <input type="file" accept="image/*" className="hidden" onChange={e => handleUploadBanner(1, e)} />
                    </label>
                  )}
                </div>
              </div>
            )}
          </div>

          {/* Afiche 2: Calendario y Eventos Académicos */}
          <div className="relative group rounded-2xl overflow-hidden shadow-xs border border-slate-200 bg-white transition hover:shadow-md cursor-pointer">
            {banner2 ? (
              <div className="relative w-full">
                <img
                  src={banner2}
                  alt="Calendario y Eventos"
                  onClick={() => setModalImagen({ src: banner2, title: "Calendario Académico — Escuela Provincias Unidas" })}
                  className="w-full h-auto object-contain block rounded-2xl"
                />
                <div className="absolute inset-0 bg-slate-950/40 opacity-0 group-hover:opacity-100 transition-all flex items-center justify-center gap-2 backdrop-blur-xs rounded-2xl">
                  <button
                    onClick={() => setModalImagen({ src: banner2, title: "Calendario Académico — Escuela Provincias Unidas" })}
                    className="px-3 py-1.5 bg-white text-slate-800 rounded-xl text-xs font-bold shadow-lg hover:bg-slate-50 transition flex items-center gap-1">
                    🔍 Ver
                  </button>
                  {esAdmin && (
                    <>
                      <label className="cursor-pointer px-3 py-1.5 bg-teal-600 text-white rounded-xl text-xs font-bold shadow-lg hover:bg-teal-700 transition flex items-center gap-1">
                        📷 Cambiar
                        <input type="file" accept="image/*" className="hidden" onChange={e => handleUploadBanner(2, e)} />
                      </label>
                      <button
                        onClick={(e) => { e.stopPropagation(); setBanner2(null); localStorage.removeItem("sga_banner_2"); }}
                        className="px-2.5 py-1.5 bg-rose-600 text-white rounded-xl text-xs font-bold shadow-lg hover:bg-rose-700 transition">
                        ✕
                      </button>
                    </>
                  )}
                </div>
              </div>
            ) : (
              <div
                onClick={() => !esAdmin && setModalImagen({ src: null, title: "Calendario Académico", desc: "Cronograma de exámenes y asentamiento de notas." })}
                className="bg-gradient-to-br from-[#0f766e] via-[#115e59] to-[#134e4a] p-5 text-white flex flex-col justify-between min-h-[220px]"
              >
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-[10px] font-black uppercase tracking-wider bg-white/20 px-2.5 py-0.5 rounded-full text-teal-100 backdrop-blur-xs">
                      🗓️ CALENDARIO
                    </span>
                    <span className="text-[10px] text-teal-200 font-bold">Trimestre 1</span>
                  </div>
                  <h4 className="font-bold text-base leading-snug text-white mt-1">
                    Asentamiento de Notas
                  </h4>
                  <p className="text-xs text-teal-100/90 mt-2 leading-relaxed font-sans">
                    Registro de aportes formativos (70%) y examen sumativo (30%) por docentes titulares.
                  </p>
                </div>
                <div className="pt-3 border-t border-white/10 flex items-center justify-between text-xs text-teal-200">
                  <span className="font-medium text-[11px]">Tutoría con IA Activa</span>
                  {esAdmin && (
                    <label className="cursor-pointer font-bold text-white bg-white/20 hover:bg-white/30 px-3 py-1 rounded-lg text-xs transition flex items-center gap-1">
                      📷 Subir afiche
                      <input type="file" accept="image/*" className="hidden" onChange={e => handleUploadBanner(2, e)} />
                    </label>
                  )}
                </div>
              </div>
            )}
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

      {/* MODAL VISOR LIGHTBOX DE AFICHE */}
      {modalImagen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-fade-in" onClick={() => setModalImagen(null)}>
          <div className="relative bg-white rounded-3xl overflow-hidden shadow-2xl max-w-2xl w-full max-h-[90vh] flex flex-col border border-slate-200" onClick={e => e.stopPropagation()}>
            <div style={{ backgroundColor: PRIMARY }} className="px-5 py-3.5 text-white flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="text-base">📢</span>
                <h3 className="font-bold text-sm">{modalImagen.title}</h3>
              </div>
              <button onClick={() => setModalImagen(null)} className="p-1 rounded-lg text-white/80 hover:text-white hover:bg-white/10 transition">
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
            <div className="p-4 flex items-center justify-center bg-slate-50 overflow-y-auto max-h-[calc(90vh-60px)]">
              {modalImagen.src ? (
                <img src={modalImagen.src} alt={modalImagen.title} className="max-w-full h-auto rounded-2xl shadow-sm object-contain" />
              ) : (
                <div className="p-8 text-center text-slate-600">
                  <p className="font-bold text-base">{modalImagen.title}</p>
                  <p className="text-sm mt-2">{modalImagen.desc}</p>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </Layout>
  );
}
