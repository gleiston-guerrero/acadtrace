import { useState, useEffect } from 'react';
import api from '../utils/api';
import Layout from '../components/Layout';

const PRIMARY = '#243A76';

const menuItems = [
  { id: 'grados', label: 'Grados y niveles', icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" /></svg> },
  { id: 'nuevo', label: 'Nuevo grado', icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg> },
];
const modalBg = { backgroundColor: 'rgba(36, 58, 118, 0.5)' };

const CARD_PALETTES = [
  { id: 'navy', header: 'bg-[#2b3c66]' },
  { id: 'slate', header: 'bg-[#475569]' },
  { id: 'indigo', header: 'bg-[#3b4266]' },
  { id: 'teal', header: 'bg-[#33535e]' },
  { id: 'olive', header: 'bg-[#4a5840]' },
  { id: 'zinc', header: 'bg-[#52525b]' },
];

export default function Grados() {
  const [grados, setGrados] = useState([]);
  const [loading, setLoading] = useState(true);
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

  const handleSeccion = (id) => {
    if (id === 'grados') { setGradoSel(null); return; }
    if (id === 'nuevo') { setGradoEditar(null); setShowModal(true); return; }
  };

  return (
    <Layout
      breadcrumb={gradoSel ? ['Inicio', 'Grados', gradoSel.nombre] : ['Inicio', 'Grados']}
      sidebarTitle="Grados"
      menuItems={menuItems}
      seccion="grados"
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
              <p className="text-sm text-slate-400 mt-0.5">{grados.length} grados configurados — Escuela Provincias Unidas</p>
            </div>
          </div>

          {grados.length === 0 ? (
            <div className="p-12 text-center text-slate-400 text-sm bg-white rounded-xl border border-slate-200">
              No hay grados configurados todavía.
            </div>
          ) : Object.entries(nivelesAgrupados).map(([nivel, gradosNivel]) => (
            <div key={nivel} className="mb-6">
              <div className="flex items-center gap-2 mb-3">
                <div className="w-1.5 h-5 rounded-full" style={{ backgroundColor: PRIMARY }} />
                <h2 className="text-sm font-bold uppercase tracking-wide" style={{ color: PRIMARY }}>{nivel}</h2>
                <span className="text-xs text-slate-400 ml-1">({gradosNivel.length} grado{gradosNivel.length !== 1 ? 's' : ''})</span>
              </div>
              <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3">
                {gradosNivel.map(g => (
                  <GradoCard key={g.id_grado} grado={g}
                    onClick={() => setGradoSel(g)}
                    onEditar={() => { setGradoEditar(g); setShowModal(true); }}
                    onToggleEstado={() => cambiarEstadoGrado(g)} />
                ))}
              </div>
            </div>
          ))}
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
              {gradoSel.paralelos.map(p => (
                <ParaleloCard key={p.id_paralelo} paralelo={p} grado={gradoSel} onToggleEstado={() => cambiarEstadoParalelo(p)} />
              ))}
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
              <button onClick={() => setShowModal(false)} className="text-white text-opacity-70 hover:text-opacity-100">✕</button>
            </div>
            <GradoForm
              grado={gradoEditar}
              niveles={niveles}
              onCancel={() => setShowModal(false)}
              onSuccess={(msg) => { setShowModal(false); setSuccess(msg); cargar(); }}
              onError={setError}
            />
          </div>
        </div>
      )}
    </Layout>
  );
}

/* ── TARJETA DE GRADO — mismo diseño de dos tonos que sga-principal ── */
function GradoCard({ grado, onClick, onEditar, onToggleEstado }) {
  const totalParalelos = grado.paralelos?.length || 0;
  const paralelosActivos = grado.paralelos?.filter(p => p.activo).length || 0;

  const storageKey = `grado_color_${grado.id_grado}`;
  const [colorIdx, setColorIdx] = useState(() => {
    const saved = localStorage.getItem(storageKey);
    return saved !== null ? parseInt(saved, 10) : (grado.id_grado % CARD_PALETTES.length);
  });
  const [showMenu, setShowMenu] = useState(false);

  const cambiarColor = (e) => {
    e.stopPropagation();
    const nextIdx = (colorIdx + 1) % CARD_PALETTES.length;
    setColorIdx(nextIdx);
    localStorage.setItem(storageKey, String(nextIdx));
    setShowMenu(false);
  };

  const currentPalette = CARD_PALETTES[colorIdx % CARD_PALETTES.length];

  return (
    <div onClick={onClick}
      className="bg-white border border-slate-200/90 rounded-2xl overflow-hidden shadow-sm hover:shadow-md hover:border-slate-400 transition-all duration-200 flex flex-col text-left group cursor-pointer relative">
      {/* BANDA DE ENCABEZADO */}
      <div className={`${currentPalette.header} p-4 text-white relative`}>
        <div className="flex items-center justify-between gap-2 mb-2">
          <span className="bg-white/20 text-white text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded">
            {grado.nivel_educativo || 'EGB'}
          </span>

          <div className="flex items-center gap-2">
            <span className={`text-[10px] font-bold uppercase px-2 py-0.5 rounded ${grado.activo ? 'bg-emerald-600 text-white' : 'bg-slate-500 text-white'}`}>
              {grado.activo ? 'ACTIVO' : 'INACTIVO'}
            </span>

            <div className="relative">
              <button type="button" onClick={e => { e.stopPropagation(); setShowMenu(!showMenu); }}
                className="w-6 h-6 rounded bg-white/20 hover:bg-white/30 text-white flex items-center justify-center text-xs font-bold transition cursor-pointer"
                title="Opciones">
                •••
              </button>

              {showMenu && (
                <div className="absolute right-0 top-7 w-40 bg-white rounded-lg shadow-xl border border-slate-200 py-1 z-30 text-slate-700"
                  onClick={e => e.stopPropagation()}>
                  <button type="button" onClick={cambiarColor} className="w-full text-left px-3 py-1.5 text-xs hover:bg-slate-50 text-slate-700 transition">
                    Cambiar color
                  </button>
                  <button type="button" onClick={e => { e.stopPropagation(); setShowMenu(false); onEditar(); }}
                    className="w-full text-left px-3 py-1.5 text-xs hover:bg-slate-50 text-slate-700 transition">
                    Editar grado
                  </button>
                  <button type="button" onClick={e => { e.stopPropagation(); setShowMenu(false); onToggleEstado(); }}
                    className="w-full text-left px-3 py-1.5 text-xs hover:bg-slate-50 text-slate-700 transition">
                    {grado.activo ? 'Desactivar' : 'Activar'}
                  </button>
                  <button type="button" onClick={e => { e.stopPropagation(); setShowMenu(false); onClick(); }}
                    className="w-full text-left px-3 py-1.5 text-xs hover:bg-slate-50 text-slate-700 transition">
                    Abrir paralelos
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>

        <h3 className="text-base font-bold uppercase tracking-wide leading-snug line-clamp-2">{grado.nombre}</h3>
        <p className="text-[11px] text-slate-200 font-medium mt-0.5">Escuela Provincias Unidas</p>
      </div>

      {/* CUERPO INFERIOR */}
      <div className="p-4 bg-white flex-1 flex flex-col justify-between space-y-3">
        <div className="grid grid-cols-3 gap-2">
          <div className="bg-slate-50 border border-slate-100 rounded-lg p-2 text-center">
            <span className="block text-[10px] font-semibold text-slate-400 uppercase">PARALELOS</span>
            <span className="block text-xs font-bold text-slate-700 mt-0.5">{totalParalelos}</span>
          </div>
          <div className="bg-slate-50 border border-slate-100 rounded-lg p-2 text-center">
            <span className="block text-[10px] font-semibold text-slate-400 uppercase">ACTIVOS</span>
            <span className="block text-xs font-bold text-slate-700 mt-0.5">{paralelosActivos}</span>
          </div>
          <div className="bg-slate-50 border border-slate-100 rounded-lg p-2 text-center">
            <span className="block text-[10px] font-semibold text-slate-400 uppercase">ESTADO</span>
            <span className="block text-xs font-bold text-emerald-600 mt-0.5">{grado.activo ? 'Vigente' : 'Inactivo'}</span>
          </div>
        </div>

        <div className="pt-2 border-t border-slate-100 flex items-center justify-between">
          <span className="text-xs font-medium text-slate-600 group-hover:text-[#243A76] transition-colors">Abrir paralelos</span>
          <span className="text-xs font-bold text-slate-400 group-hover:text-[#243A76] transition-colors">→</span>
        </div>
      </div>

      {showMenu && (
        <div className="fixed inset-0 z-20 cursor-default" onClick={e => { e.stopPropagation(); setShowMenu(false); }} />
      )}
    </div>
  );
}

/* ── TARJETA DE PARALELO — mismo diseño de dos tonos que sga-principal ── */
function ParaleloCard({ paralelo, grado, onToggleEstado }) {
  const storageKey = `paralelo_color_${paralelo.id_paralelo}`;
  const [colorIdx, setColorIdx] = useState(() => {
    const saved = localStorage.getItem(storageKey);
    return saved !== null ? parseInt(saved, 10) : (paralelo.id_paralelo % CARD_PALETTES.length);
  });
  const [showMenu, setShowMenu] = useState(false);

  const cambiarColor = (e) => {
    e.stopPropagation();
    const nextIdx = (colorIdx + 1) % CARD_PALETTES.length;
    setColorIdx(nextIdx);
    localStorage.setItem(storageKey, String(nextIdx));
    setShowMenu(false);
  };

  const currentPalette = CARD_PALETTES[colorIdx % CARD_PALETTES.length];

  return (
    <div className="bg-white border border-slate-200/90 rounded-2xl overflow-hidden shadow-sm hover:shadow-md hover:border-slate-400 transition-all duration-200 flex flex-col text-left group relative">
      <div className={`${currentPalette.header} p-4 text-white relative`}>
        <div className="flex items-center justify-between gap-2 mb-2">
          <div className="flex items-center gap-2">
            <span className="bg-white/20 text-white text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded">
              {grado.nivel_educativo || 'Sin nivel'}
            </span>
            <span className={`text-[10px] font-bold uppercase px-2 py-0.5 rounded ${paralelo.activo ? 'bg-emerald-600 text-white' : 'bg-slate-500 text-white'}`}>
              {paralelo.activo ? 'ACTIVO' : 'INACTIVO'}
            </span>
          </div>

          <div className="relative">
            <button type="button" onClick={e => { e.stopPropagation(); setShowMenu(!showMenu); }}
              className="w-6 h-6 rounded bg-white/20 hover:bg-white/30 text-white flex items-center justify-center text-xs font-bold transition cursor-pointer"
              title="Opciones">
              •••
            </button>

            {showMenu && (
              <div className="absolute right-0 top-7 w-40 bg-white rounded-lg shadow-xl border border-slate-200 py-1 z-30 text-slate-700"
                onClick={e => e.stopPropagation()}>
                <button type="button" onClick={cambiarColor} className="w-full text-left px-3 py-1.5 text-xs hover:bg-slate-50 text-slate-700 transition">
                  Cambiar color
                </button>
                <button type="button" onClick={e => { e.stopPropagation(); setShowMenu(false); onToggleEstado(); }}
                  className="w-full text-left px-3 py-1.5 text-xs hover:bg-slate-50 text-slate-700 transition">
                  {paralelo.activo ? 'Desactivar' : 'Activar'}
                </button>
              </div>
            )}
          </div>
        </div>

        <div className="flex items-center gap-3 mt-1">
          <div className="w-10 h-10 rounded-lg bg-white/20 flex items-center justify-center text-white text-lg font-bold flex-shrink-0">
            {paralelo.letra}
          </div>
          <div>
            <h3 className="text-base font-bold uppercase tracking-wide leading-tight">{grado.nombre} "{paralelo.letra}"</h3>
            <p className="text-[11px] text-slate-200 font-medium mt-0.5">Escuela Provincias Unidas</p>
          </div>
        </div>
      </div>

      <div className="p-4 bg-white flex-1 flex flex-col justify-between space-y-3">
        <div className="grid grid-cols-3 gap-2">
          <div className="bg-slate-50 border border-slate-100 rounded-lg p-2 text-center">
            <span className="block text-[10px] font-semibold text-slate-400 uppercase">ALUMNOS</span>
            <span className="block text-xs font-bold text-slate-700 mt-0.5">{paralelo.total_estudiantes ?? 0}</span>
          </div>
          <div className="bg-slate-50 border border-slate-100 rounded-lg p-2 text-center">
            <span className="block text-[10px] font-semibold text-slate-400 uppercase">CAPACIDAD</span>
            <span className="block text-xs font-bold text-slate-700 mt-0.5">{grado.capacidad_max || 35}</span>
          </div>
          <div className="bg-slate-50 border border-slate-100 rounded-lg p-2 text-center">
            <span className="block text-[10px] font-semibold text-slate-400 uppercase">ESCALA</span>
            <span className="block text-xs font-bold text-slate-700 mt-0.5">{grado.tipo_escala === 'CUALITATIVA' ? 'C' : 'N'}</span>
          </div>
        </div>

        <div className="pt-2 border-t border-slate-100 flex items-center justify-between">
          <button type="button" onClick={onToggleEstado} className="text-xs font-medium text-slate-600 group-hover:text-[#243A76] transition-colors">
            {paralelo.activo ? 'Desactivar curso' : 'Activar curso'}
          </button>
        </div>
      </div>

      {showMenu && (
        <div className="fixed inset-0 z-20 cursor-default" onClick={e => { e.stopPropagation(); setShowMenu(false); }} />
      )}
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
