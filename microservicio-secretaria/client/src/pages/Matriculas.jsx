import { useState, useEffect, useCallback } from 'react';
import Layout from '../components/Layout';
import api, { apiPrincipal } from '../utils/api';

const PRIMARY = '#243A76';

const ESTADO_BADGE = {
  ACTIVA:       'bg-green-100 text-green-700',
  RETIRADA:     'bg-red-100 text-red-600',
  EGRESADA:     'bg-blue-100 text-blue-700',
  PROMOVIDA:    'bg-purple-100 text-purple-700',
  NO_PROMOVIDA: 'bg-amber-100 text-amber-700',
};

export default function Matriculas() {
  const [matriculas, setMatriculas] = useState([]);
  const [meta, setMeta] = useState({});
  const [anos, setAnos] = useState([]);
  const [grados, setGrados] = useState([]);
  const [paralelos, setParalelos] = useState([]);
  const [anoSel, setAnoSel] = useState('');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(false);
  const [stats, setStats] = useState([]);
  const [modal, setModal] = useState(false);
  const [estudiantes, setEstudiantes] = useState([]);
  const [form, setForm] = useState({ id_estudiante: '', id_grado: '', id_paralelo: '', id_ano_lectivo: '', observaciones: '' });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  // Cargar años lectivos y grados de sga-principal (camelCase)
  useEffect(() => {
    apiPrincipal.get('/anos-lectivos').then(r => {
      const lista = r.data || [];
      setAnos(lista);
      // Seleccionar el año actual automáticamente
      const actual = lista.find(a => a.esActual);
      if (actual) setAnoSel(String(actual.idAnoLectivo));
    }).catch(e => console.error('Error años lectivos:', e));

    apiPrincipal.get('/grados').then(r => {
      setGrados(r.data || []);
    }).catch(e => console.error('Error grados:', e));
  }, []);

  // Cargar paralelos desde nuestro propio backend (snake_case) cuando cambia el grado
  const cargarParalelos = async (idGrado) => {
    if (!idGrado) { setParalelos([]); return; }
    try {
      const res = await api.get(`/matriculas/paralelos/${idGrado}`);
      setParalelos(res.data || []);
    } catch {
      setParalelos([]);
    }
  };

  const cargar = useCallback(async () => {
    if (!anoSel) return;
    setLoading(true);
    try {
      const [m, s] = await Promise.all([
        api.get(`/matriculas/ano-lectivo/${anoSel}`, {
          params: { q: search || undefined, page, limit: 20 },
        }),
        api.get(`/matriculas/ano-lectivo/${anoSel}/estadisticas`),
      ]);
      setMatriculas(m.data.data || []);
      setMeta(m.data.meta || {});
      setStats(s.data || []);
    } catch (e) {
      console.error('Error cargando matrículas:', e);
    } finally {
      setLoading(false);
    }
  }, [anoSel, search, page]);

  useEffect(() => { cargar(); }, [cargar]);

  const abrirModal = async () => {
    setForm({ id_estudiante: '', id_grado: '', id_paralelo: '', id_ano_lectivo: anoSel, observaciones: '' });
    setError('');
    setParalelos([]);
    try {
      const r = await api.get('/estudiantes', { params: { limit: 200 } });
      setEstudiantes(r.data.data || []);
    } catch { setEstudiantes([]); }
    setModal(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    try {
      await api.post('/matriculas', form);
      setModal(false);
      cargar();
    } catch (err) {
      setError(err.response?.data?.error || err.response?.data?.detalles?.[0]?.mensaje || 'Error al registrar matrícula');
    } finally {
      setSaving(false);
    }
  };

  const cambiarEstado = async (id, estado) => {
    try {
      await api.patch(`/matriculas/${id}/estado`, { estado });
      cargar();
    } catch (e) {
      console.error('Error cambiando estado:', e);
    }
  };

  return (
    <Layout breadcrumb={['Inicio', 'Matrículas']}>
      <div className="flex items-center justify-between mb-4">
        <div>
          <h1 className="text-base font-bold text-slate-700">Matrículas</h1>
          <p className="text-xs text-slate-400">Registro y consulta de matrículas</p>
        </div>
        <button onClick={abrirModal} style={{ backgroundColor: PRIMARY }}
          className="flex items-center gap-2 text-white text-sm px-4 py-2 rounded-lg hover:opacity-90 transition shadow-sm">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
          </svg>
          Nueva matrícula
        </button>
      </div>

      {/* Filtros */}
      <div className="bg-white border border-slate-200 rounded-xl p-3 mb-4 flex flex-wrap gap-3 items-center">
        <select value={anoSel} onChange={e => { setAnoSel(e.target.value); setPage(1); }}
          className="px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-slate-50 min-w-40">
          <option value="">— Año lectivo —</option>
          {anos.map(a => (
            <option key={a.idAnoLectivo} value={a.idAnoLectivo}>
              {a.nombre}{a.esActual ? ' (Actual)' : ''}
            </option>
          ))}
        </select>
        <div className="relative flex-1 min-w-52">
          <svg className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          <input value={search} onChange={e => { setSearch(e.target.value); setPage(1); }}
            placeholder="Buscar estudiante..."
            className="w-full pl-9 pr-4 py-2 text-sm border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 bg-slate-50" />
        </div>
      </div>

      {/* Stats mini por grado */}
      {stats.length > 0 && (
        <div className="grid grid-cols-3 sm:grid-cols-6 gap-2 mb-4">
          {stats.slice(0, 6).map(s => (
            <div key={`${s.grado}-${s.paralelo}`} className="bg-white border border-slate-200 rounded-xl p-3 text-center">
              <p className="text-xl font-bold text-slate-700">{s.total}</p>
              <p className="text-xs text-slate-400 leading-tight">{s.grado} {s.paralelo}</p>
            </div>
          ))}
        </div>
      )}

      {/* Tabla */}
      <div className="bg-white border border-slate-200 rounded-xl overflow-hidden">
        <table className="w-full text-sm">
          <thead>
            <tr style={{ backgroundColor: PRIMARY }} className="text-white text-xs">
              <th className="px-4 py-3 text-left">N°</th>
              <th className="px-4 py-3 text-left">Estudiante</th>
              <th className="px-4 py-3 text-left">Cédula</th>
              <th className="px-4 py-3 text-left">Grado / Paralelo</th>
              <th className="px-4 py-3 text-left">F. Matrícula</th>
              <th className="px-4 py-3 text-center">Estado</th>
              <th className="px-4 py-3 text-center">Cambiar estado</th>
            </tr>
          </thead>
          <tbody>
            {!anoSel ? (
              <tr><td colSpan={7} className="text-center py-12 text-slate-400 text-sm">
                Selecciona un año lectivo para ver las matrículas
              </td></tr>
            ) : loading ? (
              <tr><td colSpan={7} className="text-center py-12">
                <div className="flex items-center justify-center gap-2 text-slate-400">
                  <svg className="w-5 h-5 animate-spin" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z"/>
                  </svg>
                  <span className="text-sm">Cargando matrículas...</span>
                </div>
              </td></tr>
            ) : matriculas.length === 0 ? (
              <tr><td colSpan={7} className="text-center py-12 text-slate-400 text-sm">
                No se encontraron matrículas
              </td></tr>
            ) : matriculas.map((m, i) => (
              <tr key={m.id_matricula}
                className={`border-t border-slate-100 ${i % 2 === 1 ? 'bg-slate-50/40' : ''} hover:bg-blue-50/30 transition`}>
                <td className="px-4 py-2.5 text-xs text-slate-400 font-mono">{m.numero_orden}</td>
                <td className="px-4 py-2.5 font-medium text-slate-700">{m.estudiante}</td>
                <td className="px-4 py-2.5 text-slate-500 text-xs">{m.cedula || '—'}</td>
                <td className="px-4 py-2.5 text-slate-600">{m.grado} "{m.paralelo}"</td>
                <td className="px-4 py-2.5 text-slate-500 text-xs">
                  {m.fecha_registro ? new Date(m.fecha_registro).toLocaleDateString('es-EC') : '—'}
                </td>
                <td className="px-4 py-2.5 text-center">
                  <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${ESTADO_BADGE[m.estado] || 'bg-slate-100 text-slate-600'}`}>
                    {m.estado}
                  </span>
                </td>
                <td className="px-4 py-2.5 text-center">
                  <select value={m.estado}
                    onChange={e => cambiarEstado(m.id_matricula, e.target.value)}
                    className="text-xs border border-slate-200 rounded-lg px-2 py-1 focus:outline-none bg-white">
                    {['ACTIVA', 'RETIRADA', 'EGRESADA', 'PROMOVIDA', 'NO_PROMOVIDA'].map(s => (
                      <option key={s} value={s}>{s}</option>
                    ))}
                  </select>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {/* Footer con total y paginación */}
        <div className="border-t border-slate-100 px-4 py-3 flex items-center justify-between text-xs text-slate-500">
          <span>{meta.total ? `${meta.total} matrículas en total` : ''}</span>
          {meta.pages > 1 && (
            <div className="flex gap-1">
              {Array.from({ length: meta.pages }, (_, i) => i + 1).map(p => (
                <button key={p} onClick={() => setPage(p)}
                  style={p === page ? { backgroundColor: PRIMARY, color: 'white' } : {}}
                  className={`w-7 h-7 rounded-lg text-xs font-medium transition ${p !== page ? 'hover:bg-slate-100' : ''}`}>
                  {p}
                </button>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* MODAL NUEVA MATRÍCULA */}
      {modal && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md">
            <div style={{ backgroundColor: PRIMARY }} className="px-6 py-4 flex items-center justify-between rounded-t-2xl">
              <h2 className="text-white font-semibold text-sm">Nueva Matrícula</h2>
              <button onClick={() => setModal(false)} className="text-white/70 hover:text-white">
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </div>
            <form onSubmit={handleSubmit} className="p-6 space-y-4">
              {/* Estudiante */}
              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1">Estudiante *</label>
                <select required value={form.id_estudiante}
                  onChange={e => setForm({ ...form, id_estudiante: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-slate-50">
                  <option value="">— Seleccionar estudiante —</option>
                  {estudiantes.map(e => (
                    <option key={e.id_estudiante} value={e.id_estudiante}>
                      {e.apellidos}, {e.nombres} {e.cedula ? `(${e.cedula})` : ''}
                    </option>
                  ))}
                </select>
              </div>

              {/* Año lectivo */}
              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1">Año lectivo *</label>
                <select required value={form.id_ano_lectivo}
                  onChange={e => setForm({ ...form, id_ano_lectivo: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-slate-50">
                  <option value="">— Seleccionar —</option>
                  {anos.map(a => (
                    <option key={a.idAnoLectivo} value={a.idAnoLectivo}>
                      {a.nombre}{a.esActual ? ' (Actual)' : ''}
                    </option>
                  ))}
                </select>
              </div>

              {/* Grado */}
              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1">Grado *</label>
                <select required value={form.id_grado}
                  onChange={e => {
                    setForm({ ...form, id_grado: e.target.value, id_paralelo: '' });
                    cargarParalelos(e.target.value);
                  }}
                  className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-slate-50">
                  <option value="">— Seleccionar grado —</option>
                  {grados.map(g => (
                    <option key={g.idGrado} value={g.idGrado}>{g.nombre}</option>
                  ))}
                </select>
              </div>

              {/* Paralelo */}
              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1">Paralelo *</label>
                <select required value={form.id_paralelo}
                  onChange={e => setForm({ ...form, id_paralelo: e.target.value })}
                  disabled={!form.id_grado || paralelos.length === 0}
                  className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-slate-50 disabled:opacity-50">
                  <option value="">{form.id_grado && paralelos.length === 0 ? 'Cargando...' : '— Seleccionar paralelo —'}</option>
                  {paralelos.map(p => (
                    <option key={p.id_paralelo} value={p.id_paralelo}>Paralelo {p.letra}</option>
                  ))}
                </select>
              </div>

              {/* Observaciones */}
              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1">Observaciones</label>
                <textarea value={form.observaciones}
                  onChange={e => setForm({ ...form, observaciones: e.target.value })}
                  rows={2} className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-slate-50 resize-none" />
              </div>

              {error && <div className="bg-red-50 border border-red-200 text-red-600 text-xs px-3 py-2 rounded-lg">{error}</div>}

              <div className="flex gap-3 pt-2">
                <button type="button" onClick={() => setModal(false)}
                  className="flex-1 py-2.5 border border-slate-200 rounded-lg text-sm text-slate-600 hover:bg-slate-50 transition">Cancelar</button>
                <button type="submit" disabled={saving} style={{ backgroundColor: PRIMARY }}
                  className="flex-1 py-2.5 rounded-lg text-sm text-white font-semibold hover:opacity-90 transition disabled:opacity-60">
                  {saving ? 'Registrando...' : 'Registrar matrícula'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </Layout>
  );
}
