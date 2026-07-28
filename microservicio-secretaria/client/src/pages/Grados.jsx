import { useState, useEffect } from 'react';
import api from '../utils/api';
import Layout from '../components/Layout';

const PRIMARY = '#243A76';
const modalBg = { backgroundColor: 'rgba(36, 58, 118, 0.5)' };

const NIVELES_CONFIG = {
  Inicial:             { accent: '#c4956a', accentLight: '#faf5ef', accentMid: '#e8d5c0', textAccent: '#8b6842' },
  Preparatoria:        { accent: '#7c8a6e', accentLight: '#f3f6ef', accentMid: '#d4ddc8', textAccent: '#5a6b48' },
  'Básica Elemental':  { accent: '#6a8a9a', accentLight: '#eef4f7', accentMid: '#c5d9e2', textAccent: '#446778' },
  'Básica Media':      { accent: '#6e7499', accentLight: '#f0f1f7', accentMid: '#c8cce0', textAccent: '#4a4f78' },
  'Básica Superior':   { accent: '#243A76', accentLight: '#eef0f7', accentMid: '#c0c8e0', textAccent: '#1a2d5f' },
};

const nivelConfig = (nivel) => NIVELES_CONFIG[nivel] || { accent: '#64748b', accentLight: '#f1f5f9', accentMid: '#cbd5e1', textAccent: '#475569' };

const menuItems = [
  { id: 'cursos', label: 'Cursos', icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" /></svg> },
  { id: 'nuevo', label: 'Nuevo grado', icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg> },
];

export default function Grados() {
  const [grados, setGrados] = useState([]);
  const [loading, setLoading] = useState(true);
  const [seccion, setSeccion] = useState('cursos');
  const [gradoSel, setGradoSel] = useState(null);
  const [showCrearParalelo, setShowCrearParalelo] = useState(false);
  const [nuevaLetra, setNuevaLetra] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [gradoEditar, setGradoEditar] = useState(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [saving, setSaving] = useState(false);

  const cargar = () => {
    setLoading(true);
    api.get('/grados')
      .then(r => {
        setGrados(r.data || []);
        if (gradoSel) {
          const actualizado = (r.data || []).find(g => g.id_grado === gradoSel.id_grado);
          if (actualizado) setGradoSel(actualizado);
        }
      })
      .catch(() => setError('Error al cargar grados'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { cargar(); }, []);

  useEffect(() => {
    if (success) { const t = setTimeout(() => setSuccess(''), 4000); return () => clearTimeout(t); }
  }, [success]);

  const handleSeccion = (id) => {
    setSeccion(id);
    if (id === 'nuevo') { setGradoEditar(null); setShowModal(true); setError(''); }
    if (id === 'cursos') { setGradoSel(null); }
  };

  const nivelesAgrupados = {};
  grados.forEach(g => {
    const nivel = g.nivel_educativo || 'Sin nivel';
    if (!nivelesAgrupados[nivel]) nivelesAgrupados[nivel] = [];
    nivelesAgrupados[nivel].push(g);
  });

  const niveles = (() => {
    const seen = new Set();
    const nivs = [];
    grados.forEach(g => {
      if (g.id_nivel && !seen.has(g.id_nivel)) {
        seen.add(g.id_nivel);
        nivs.push({ id: g.id_nivel, nombre: g.nivel_educativo });
      }
    });
    return nivs;
  })();

  const crearParalelo = async () => {
    if (!nuevaLetra.trim()) return;
    setSaving(true);
    try {
      await api.post(`/grados/${gradoSel.id_grado}/paralelos`, {}, { params: { letra: nuevaLetra.trim().toUpperCase() } });
      setSuccess(`Paralelo ${nuevaLetra.toUpperCase()} creado`);
      setShowCrearParalelo(false);
      setNuevaLetra('');
      cargar();
    } catch (e) {
      setError(e.response?.data?.error || 'Error al crear paralelo');
    } finally { setSaving(false); }
  };

  const cambiarEstadoGrado = async (grado) => {
    try {
      await api.patch(`/grados/${grado.id_grado}/estado`, {}, { params: { activo: !grado.activo } });
      cargar();
    } catch {
      setError('Error al cambiar el estado del grado');
    }
  };

  const cambiarEstadoParalelo = async (paralelo) => {
    try {
      await api.patch(`/grados/paralelos/${paralelo.id_paralelo}/estado`, {}, { params: { activo: !paralelo.activo } });
      cargar();
    } catch {
      setError('Error al cambiar el estado del paralelo');
    }
  };

  return (
    <Layout
      breadcrumb={gradoSel ? ['Inicio', 'Grados', gradoSel.nombre] : ['Inicio', 'Grados']}
      sidebarTitle="Grados"
      menuItems={menuItems}
      seccion={seccion}
      onSeccionChange={handleSeccion}
    >
      {error && <div className="mb-4 bg-red-50 border border-red-200 rounded-xl px-4 py-3 flex justify-between"><span className="text-red-600 text-sm">{error}</span><button onClick={() => setError('')} className="text-red-400 ml-4">✕</button></div>}
      {success && <div className="mb-4 bg-green-50 border border-green-200 rounded-xl px-4 py-3 flex justify-between"><span className="text-green-600 text-sm">{success}</span><button onClick={() => setSuccess('')} className="text-green-400 ml-4">✕</button></div>}

      {loading ? (
        <div className="p-12 text-center text-slate-400 text-sm">Cargando grados...</div>
      ) : !gradoSel ? (
        /* ── VISTA PRINCIPAL: Tarjetas agrupadas por nivel ── */
        <div>
          <div className="flex items-center justify-between mb-6">
            <div>
              <h1 className="text-xl font-bold text-slate-700">Grados y Cursos</h1>
              <p className="text-sm text-slate-400 mt-0.5">{grados.length} grados configurados</p>
            </div>
            <button onClick={() => { setGradoEditar(null); setShowModal(true); }} style={{ backgroundColor: PRIMARY }}
              className="flex items-center gap-2 text-white px-5 py-2.5 rounded-lg text-sm font-medium hover:opacity-90 transition shadow-sm">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg>
              Nuevo grado
            </button>
          </div>

          {grados.length === 0 ? (
            <div className="p-12 text-center text-slate-400 text-sm bg-white rounded-xl border border-slate-200">
              No hay grados configurados todavía.
            </div>
          ) : Object.entries(nivelesAgrupados).map(([nivel, gradosNivel]) => {
            const c = nivelConfig(nivel);
            return (
              <div key={nivel} className="mb-8">
                <div className="flex items-center gap-2 mb-4">
                  <div className="w-1.5 h-7 rounded-full" style={{ backgroundColor: c.accent }} />
                  <h2 className="text-base font-bold uppercase tracking-wide" style={{ color: c.textAccent }}>{nivel}</h2>
                  <span className="text-sm text-slate-400 ml-1">({gradosNivel.length} grado{gradosNivel.length !== 1 ? 's' : ''})</span>
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
                  {gradosNivel.map(g => (
                    <GradoCard key={g.id_grado} grado={g} config={c}
                      onClick={() => setGradoSel(g)}
                      onEditar={() => { setGradoEditar(g); setShowModal(true); }}
                      onToggleEstado={() => cambiarEstadoGrado(g)} />
                  ))}
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        /* ── VISTA DETALLE: Paralelos del grado seleccionado ── */
        <div>
          <button onClick={() => setGradoSel(null)} className="flex items-center gap-1 text-sm text-slate-500 hover:text-slate-700 mb-4 transition">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" /></svg>
            Volver a grados
          </button>

          <div className="flex items-center justify-between mb-5">
            <div>
              <h1 className="text-xl font-bold text-slate-700">{gradoSel.nombre}</h1>
              <p className="text-sm text-slate-400 mt-0.5">
                {gradoSel.nivel_educativo} — {gradoSel.tipo_escala === 'CUALITATIVA' ? 'Escala cualitativa' : 'Escala cuantitativa'} — Capacidad máx: {gradoSel.capacidad_max || 35} alumnos
              </p>
            </div>
            <button onClick={() => { setShowCrearParalelo(true); setNuevaLetra(''); setError(''); }} style={{ backgroundColor: PRIMARY }}
              className="flex items-center gap-2 text-white px-5 py-2.5 rounded-lg text-sm font-medium hover:opacity-90 transition shadow-sm">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg>
              Nuevo paralelo
            </button>
          </div>

          {(!gradoSel.paralelos || gradoSel.paralelos.length === 0) ? (
            <div className="p-12 text-center text-slate-400 text-sm bg-white rounded-xl border border-slate-200">
              No hay paralelos configurados para este grado.
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
              {gradoSel.paralelos.map(p => {
                const c = nivelConfig(gradoSel.nivel_educativo);
                return <ParaleloCard key={p.id_paralelo} paralelo={p} grado={gradoSel} config={c} onToggleEstado={() => cambiarEstadoParalelo(p)} />;
              })}
            </div>
          )}
        </div>
      )}

      {/* MODAL CREAR PARALELO */}
      {showCrearParalelo && (
        <div className="fixed inset-0 z-50 flex items-center justify-center px-4" style={modalBg}>
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-sm overflow-hidden">
            <div style={{ backgroundColor: PRIMARY }} className="px-6 py-4 flex items-center justify-between">
              <h2 className="text-white font-bold text-base">Nuevo Paralelo — {gradoSel.nombre}</h2>
              <button onClick={() => setShowCrearParalelo(false)} className="text-white text-opacity-70 hover:text-opacity-100">✕</button>
            </div>
            <div className="p-6">
              {error && <div className="mb-3 bg-red-50 border border-red-200 rounded-lg px-3 py-2 text-red-600 text-xs">{error}</div>}
              <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase tracking-wide">Letra del paralelo</label>
              <input type="text" maxLength={1} value={nuevaLetra} onChange={e => setNuevaLetra(e.target.value.toUpperCase())}
                placeholder="Ej: D"
                className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none bg-slate-50 text-center text-2xl font-bold uppercase"
                style={{ color: PRIMARY }} />
              <div className="flex gap-3 mt-5">
                <button onClick={() => setShowCrearParalelo(false)} className="flex-1 py-2 border border-slate-200 rounded-lg text-sm text-slate-600 hover:bg-slate-50 transition">
                  Cancelar
                </button>
                <button onClick={crearParalelo} disabled={!nuevaLetra.trim() || saving} style={{ backgroundColor: PRIMARY }}
                  className="flex-1 py-2 text-white rounded-lg text-sm font-medium hover:opacity-90 transition disabled:opacity-60">
                  {saving ? 'Creando...' : 'Crear paralelo'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* MODAL NUEVO/EDITAR GRADO */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center px-4" style={modalBg}>
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden">
            <div style={{ backgroundColor: PRIMARY }} className="px-6 py-4 flex items-center justify-between">
              <h2 className="text-white font-bold text-base">{gradoEditar ? 'Editar Grado' : 'Nuevo Grado'}</h2>
              <button onClick={() => { setShowModal(false); setSeccion('cursos'); }} className="text-white text-opacity-70 hover:text-opacity-100">✕</button>
            </div>
            <GradoForm
              grado={gradoEditar}
              niveles={niveles}
              onCancel={() => { setShowModal(false); setSeccion('cursos'); }}
              onSuccess={(msg) => { setShowModal(false); setSeccion('cursos'); setSuccess(msg); cargar(); }}
              onError={setError}
            />
          </div>
        </div>
      )}
    </Layout>
  );
}

/* ── TARJETA DE GRADO ── */
function GradoCard({ grado, config, onClick, onEditar, onToggleEstado }) {
  const totalParalelos = grado.paralelos?.length || 0;
  const paralelosActivos = grado.paralelos?.filter(p => p.activo).length || 0;

  return (
    <div onClick={onClick} className="bg-white rounded-2xl overflow-hidden hover:shadow-lg transition-all cursor-pointer group"
      style={{ border: `1px solid ${config.accentMid}`, borderLeft: `5px solid ${config.accent}` }}>
      <div className="px-5 pt-5 pb-4" style={{ backgroundColor: config.accentLight }}>
        <div className="flex items-start gap-3.5">
          <div className="w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0 shadow-sm" style={{ backgroundColor: config.accent }}>
            <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
            </svg>
          </div>
          <div className="min-w-0">
            <h3 className="font-bold text-[15px] leading-snug text-slate-800">{grado.nombre}</h3>
            <div className="flex items-center gap-2 mt-1.5 flex-wrap">
              <span className="text-[11px] font-medium px-2 py-0.5 rounded-md" style={{ backgroundColor: config.accentMid, color: config.textAccent }}>
                {grado.nivel_educativo || 'Sin nivel'}
              </span>
              <span className={`text-[11px] font-semibold px-2 py-0.5 rounded-md ${grado.activo ? 'bg-green-100 text-green-700' : 'bg-slate-200 text-slate-500'}`}>
                {grado.activo ? 'Activo' : 'Inactivo'}
              </span>
            </div>
          </div>
        </div>
      </div>

      <div className="px-5 py-4 flex items-center divide-x divide-slate-100">
        <div className="flex-1 text-center pr-3">
          <p className="text-2xl font-bold" style={{ color: config.accent }}>{totalParalelos}</p>
          <p className="text-[10px] text-slate-400 font-semibold uppercase mt-0.5">Paralelos</p>
        </div>
        <div className="flex-1 text-center px-3">
          <p className="text-2xl font-bold" style={{ color: config.accent }}>{paralelosActivos}</p>
          <p className="text-[10px] text-slate-400 font-semibold uppercase mt-0.5">Activos</p>
        </div>
        <div className="flex-1 text-center pl-3">
          <p className="text-2xl font-bold" style={{ color: config.accent }}>{grado.capacidad_max || 35}</p>
          <p className="text-[10px] text-slate-400 font-semibold uppercase mt-0.5">Cap. Máx</p>
        </div>
      </div>

      <div className="px-5 pb-3 flex items-center gap-4 text-xs text-slate-400">
        <span className="flex items-center gap-1">
          <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" /></svg>
          {grado.tipo_escala === 'CUALITATIVA' ? 'Cualitativa' : 'Cuantitativa'}
        </span>
        <span className="flex items-center gap-1">
          <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 20l4-16m2 16l4-16M6 9h14M4 15h14" /></svg>
          Orden {grado.orden}
        </span>
      </div>

      <div className="px-5 py-3 flex items-center justify-between" style={{ borderTop: `1px solid ${config.accentMid}` }}>
        <div className="flex items-center gap-1">
          <button className="p-2 rounded-lg hover:bg-slate-100 transition text-slate-400" title="Editar"
            onClick={e => { e.stopPropagation(); onEditar(); }}>
            <svg className="w-[18px] h-[18px]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" /></svg>
          </button>
          <button className={`p-2 rounded-lg hover:bg-slate-100 transition ${grado.activo ? 'text-red-400' : 'text-green-500'}`}
            title={grado.activo ? 'Desactivar' : 'Activar'} onClick={e => { e.stopPropagation(); onToggleEstado(); }}>
            {grado.activo
              ? <svg className="w-[18px] h-[18px]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" /></svg>
              : <svg className="w-[18px] h-[18px]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>}
          </button>
        </div>
        <button className="flex items-center gap-2 text-white text-sm font-medium px-4 py-2 rounded-lg hover:opacity-90 transition shadow-sm"
          style={{ backgroundColor: config.accent }} onClick={e => { e.stopPropagation(); onClick(); }}>
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" /></svg>
          Ver paralelos
        </button>
      </div>
    </div>
  );
}

/* ── TARJETA DE PARALELO ── */
function ParaleloCard({ paralelo, grado, config, onToggleEstado }) {
  return (
    <div className="bg-white rounded-2xl overflow-hidden hover:shadow-lg transition-all"
      style={{ border: `1px solid ${config.accentMid}`, borderLeft: `5px solid ${config.accent}` }}>
      <div className="px-5 pt-5 pb-4" style={{ backgroundColor: config.accentLight }}>
        <div className="flex items-start gap-3.5">
          <div className="w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0 shadow-sm" style={{ backgroundColor: config.accent }}>
            <span className="text-white text-xl font-bold">{paralelo.letra}</span>
          </div>
          <div className="min-w-0">
            <h3 className="font-bold text-[15px] leading-snug text-slate-800">{grado.nombre} "{paralelo.letra}"</h3>
            <div className="flex items-center gap-2 mt-1.5 flex-wrap">
              <span className="text-[11px] font-medium px-2 py-0.5 rounded-md" style={{ backgroundColor: config.accentMid, color: config.textAccent }}>
                {grado.nivel_educativo || 'Sin nivel'}
              </span>
              <span className={`text-[11px] font-semibold px-2 py-0.5 rounded-md ${paralelo.activo ? 'bg-green-100 text-green-700' : 'bg-slate-200 text-slate-500'}`}>
                {paralelo.activo ? 'Activo' : 'Inactivo'}
              </span>
            </div>
          </div>
        </div>
      </div>

      <div className="px-5 py-4 flex items-center divide-x divide-slate-100">
        <div className="flex-1 text-center pr-3">
          <p className="text-2xl font-bold" style={{ color: config.accent }}>{paralelo.total_estudiantes ?? 0}</p>
          <p className="text-[10px] text-slate-400 font-semibold uppercase mt-0.5">Estudiantes</p>
        </div>
        <div className="flex-1 text-center px-3">
          <p className="text-2xl font-bold" style={{ color: config.accent }}>{grado.capacidad_max || 35}</p>
          <p className="text-[10px] text-slate-400 font-semibold uppercase mt-0.5">Capacidad</p>
        </div>
        <div className="flex-1 text-center pl-3">
          <p className="text-2xl font-bold" style={{ color: config.accent }}>{grado.tipo_escala === 'CUALITATIVA' ? 'C' : 'N'}</p>
          <p className="text-[10px] text-slate-400 font-semibold uppercase mt-0.5">Escala</p>
        </div>
      </div>

      <div className="px-5 pb-4 flex items-center justify-end" style={{ borderTop: `1px solid ${config.accentMid}`, paddingTop: '0.75rem' }}>
        <button className={`flex items-center gap-2 text-sm font-medium px-4 py-2 rounded-lg transition ${paralelo.activo ? 'text-red-500 border border-red-200 hover:bg-red-50' : 'text-white hover:opacity-90'}`}
          style={!paralelo.activo ? { backgroundColor: config.accent } : {}} onClick={onToggleEstado}>
          {paralelo.activo ? 'Desactivar' : 'Activar'}
        </button>
      </div>
    </div>
  );
}

function GradoForm({ grado, niveles, onCancel, onSuccess, onError }) {
  const esEdicion = !!grado;
  const [nombre, setNombre] = useState(grado?.nombre || '');
  const [orden, setOrden] = useState(grado?.orden ?? '');
  const [capacidad, setCapacidad] = useState(String(grado?.capacidad_max || 35));
  const [idNivel, setIdNivel] = useState(grado?.id_nivel || '');
  const [saving, setSaving] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true); onError('');
    const payload = {
      nombre,
      orden: Number(orden),
      capacidad_max: Number(capacidad),
      id_nivel: idNivel ? Number(idNivel) : null,
    };
    try {
      if (esEdicion) {
        await api.put(`/grados/${grado.id_grado}`, payload);
        onSuccess('Grado actualizado correctamente');
      } else {
        await api.post('/grados', payload);
        onSuccess('Grado creado correctamente');
      }
    } catch (err) {
      onError(err.response?.data?.error || 'Error al guardar el grado');
    } finally { setSaving(false); }
  };

  return (
    <form onSubmit={handleSubmit} className="p-6 space-y-4">
      <div>
        <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase tracking-wide">Nombre del grado</label>
        <input type="text" value={nombre} onChange={e => setNombre(e.target.value)} required placeholder="Ej: Quinto año EGB"
          className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none bg-slate-50" />
      </div>
      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase tracking-wide">Orden</label>
          <input type="number" value={orden} onChange={e => setOrden(e.target.value)} required placeholder="1"
            className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none bg-slate-50" />
        </div>
        <div>
          <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase tracking-wide">Capacidad máx.</label>
          <input type="number" value={capacidad} onChange={e => setCapacidad(e.target.value)} placeholder="35"
            className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none bg-slate-50" />
        </div>
      </div>
      <div>
        <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase tracking-wide">Nivel educativo</label>
        <select value={idNivel} onChange={e => setIdNivel(e.target.value)}
          className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none bg-slate-50">
          <option value="">Seleccionar nivel</option>
          {niveles.map(n => <option key={n.id} value={n.id}>{n.nombre}</option>)}
        </select>
        <p className="text-xs text-slate-400 mt-1">Los niveles disponibles se toman de los grados ya existentes.</p>
      </div>
      <div className="flex gap-3 pt-2">
        <button type="button" onClick={onCancel} className="flex-1 py-2 border border-slate-200 rounded-lg text-sm text-slate-600 hover:bg-slate-50 transition">
          Cancelar
        </button>
        <button type="submit" disabled={saving} style={{ backgroundColor: PRIMARY }}
          className="flex-1 py-2 text-white rounded-lg text-sm font-medium hover:opacity-90 transition disabled:opacity-60">
          {saving ? 'Guardando...' : esEdicion ? 'Guardar cambios' : 'Crear grado'}
        </button>
      </div>
    </form>
  );
}
