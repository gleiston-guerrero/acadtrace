import { useState, useEffect, useCallback } from 'react';
import Layout from '../components/Layout';
import api, { apiPrincipal } from '../utils/api';

const PRIMARY = '#243A76';

const menuItems = [
  { id: 'lista', label: 'Lista de matrículas', icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 10h16M4 14h16M4 18h16" /></svg> },
  { id: 'nueva', label: 'Nueva matrícula', icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg> },
];

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
  const [pdfError, setPdfError] = useState('');
  const [verModal, setVerModal] = useState(null);
  const [vistaModal, setVistaModal] = useState('detalle');
  const [pdfBlobUrl, setPdfBlobUrl] = useState(null);

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

  const descargarPdf = async (idMatricula, nombreEstudiante) => {
    setPdfError('');
    try {
      const res = await apiPrincipal.get(`/matriculas/${idMatricula}/pdf`, { responseType: 'blob' });
      const url = window.URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `Ficha_Matricula_${(nombreEstudiante || idMatricula).replace(/\s+/g, '_')}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch {
      setPdfError('No se pudo descargar la Ficha de Matrícula.');
    }
  };

  const abrirVer = async (m) => {
    setVerModal(m);
    setVistaModal('detalle');
    setPdfBlobUrl(null);
    try {
      const res = await apiPrincipal.get(`/matriculas/${m.id_matricula}/pdf`, { responseType: 'blob' });
      setPdfBlobUrl(window.URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' })));
    } catch {
      setPdfBlobUrl(null);
    }
  };

  const cerrarVer = () => {
    if (pdfBlobUrl) window.URL.revokeObjectURL(pdfBlobUrl);
    setPdfBlobUrl(null);
    setVerModal(null);
  };

  const handleSeccion = (id) => {
    if (id === 'nueva') { abrirModal(); return; }
  };

  return (
    <Layout breadcrumb={['Inicio', 'Matrículas']} sidebarTitle="Matrículas" menuItems={menuItems} seccion="lista" onSeccionChange={handleSeccion}>
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

      {pdfError && (
        <div className="mb-4 bg-red-50 border border-red-200 rounded-lg px-4 py-3 flex items-center justify-between">
          <span className="text-red-600 text-sm">{pdfError}</span>
          <button onClick={() => setPdfError('')} className="text-red-400 hover:text-red-600">✕</button>
        </div>
      )}

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
              <th className="px-4 py-3 text-center">Ficha</th>
            </tr>
          </thead>
          <tbody>
            {!anoSel ? (
              <tr><td colSpan={8} className="text-center py-12 text-slate-400 text-sm">
                Selecciona un año lectivo para ver las matrículas
              </td></tr>
            ) : loading ? (
              <tr><td colSpan={8} className="text-center py-12">
                <div className="flex items-center justify-center gap-2 text-slate-400">
                  <svg className="w-5 h-5 animate-spin" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z"/>
                  </svg>
                  <span className="text-sm">Cargando matrículas...</span>
                </div>
              </td></tr>
            ) : matriculas.length === 0 ? (
              <tr><td colSpan={8} className="text-center py-12 text-slate-400 text-sm">
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
                <td className="px-4 py-2.5 text-center">
                  <div className="flex items-center justify-center gap-1">
                    <button onClick={() => abrirVer(m)} title="Ver ficha / vista previa PDF"
                      className="p-1.5 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition">
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                      </svg>
                    </button>
                    <button onClick={() => descargarPdf(m.id_matricula, m.estudiante)} title="Descargar Ficha PDF"
                      className="p-1.5 rounded-lg text-slate-400 hover:text-blue-600 hover:bg-blue-50 transition">
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H8a2 2 0 01-2-2V5a2 2 0 012-2h6l6 6v11a2 2 0 01-2 2z" />
                      </svg>
                    </button>
                  </div>
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

      {/* MODAL VER FICHA / PREVIEW PDF */}
      {verModal && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4" onClick={cerrarVer}>
          <div className="bg-slate-50 rounded-2xl shadow-2xl w-full max-w-3xl overflow-hidden flex flex-col max-h-[92vh]" onClick={e => e.stopPropagation()}>
            <div style={{ background: `linear-gradient(135deg, ${PRIMARY} 0%, #3d5a9e 100%)` }} className="px-6 py-4 text-white flex items-center justify-between flex-shrink-0">
              <div className="min-w-0">
                <h3 className="font-bold text-base truncate">{verModal.estudiante}</h3>
                <p className="text-xs text-white/80 mt-0.5">{verModal.grado} "{verModal.paralelo}" · N° {verModal.numero_orden}</p>
              </div>
              <div className="flex items-center gap-3">
                <div className="flex bg-white/15 p-1 rounded-xl gap-1 text-xs font-semibold">
                  <button onClick={() => setVistaModal('detalle')}
                    className={`px-3 py-1.5 rounded-lg transition ${vistaModal === 'detalle' ? 'bg-white text-slate-800 shadow' : 'text-white/80 hover:text-white'}`}>
                    Detalle
                  </button>
                  <button onClick={() => setVistaModal('pdf')}
                    className={`px-3 py-1.5 rounded-lg transition ${vistaModal === 'pdf' ? 'bg-white text-slate-800 shadow' : 'text-white/80 hover:text-white'}`}>
                    Visualizar PDF
                  </button>
                </div>
                <button onClick={cerrarVer} className="text-white/70 hover:text-white text-xl flex-shrink-0 w-8 h-8 rounded-lg hover:bg-white/10 transition">✕</button>
              </div>
            </div>

            {vistaModal === 'detalle' ? (
              <div className="p-6 overflow-y-auto space-y-4 flex-1">
                <div className="bg-white border border-slate-200 rounded-xl p-5 grid grid-cols-2 gap-4">
                  <div>
                    <p className="text-[10px] font-semibold text-slate-400 uppercase tracking-wider mb-1">Estudiante</p>
                    <p className="text-sm text-slate-700">{verModal.estudiante}</p>
                  </div>
                  <div>
                    <p className="text-[10px] font-semibold text-slate-400 uppercase tracking-wider mb-1">Cédula</p>
                    <p className="text-sm text-slate-700 font-mono">{verModal.cedula || '—'}</p>
                  </div>
                  <div>
                    <p className="text-[10px] font-semibold text-slate-400 uppercase tracking-wider mb-1">Grado / Paralelo</p>
                    <p className="text-sm text-slate-700">{verModal.grado} "{verModal.paralelo}"</p>
                  </div>
                  <div>
                    <p className="text-[10px] font-semibold text-slate-400 uppercase tracking-wider mb-1">Fecha de registro</p>
                    <p className="text-sm text-slate-700 font-mono">{verModal.fecha_registro ? new Date(verModal.fecha_registro).toLocaleDateString('es-EC') : '—'}</p>
                  </div>
                  <div>
                    <p className="text-[10px] font-semibold text-slate-400 uppercase tracking-wider mb-1">Estado</p>
                    <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${ESTADO_BADGE[verModal.estado] || 'bg-slate-100 text-slate-600'}`}>{verModal.estado}</span>
                  </div>
                </div>
                <p className="text-xs text-slate-400 px-1">Para cambiar el estado (retiro, promoción, traslado) esa acción la realiza Dirección.</p>
              </div>
            ) : (
              <div className="flex-1 bg-slate-100 p-3 h-[500px]">
                {pdfBlobUrl ? (
                  <iframe src={pdfBlobUrl} title="Ficha de Matrícula PDF" className="w-full h-full border border-slate-200 rounded-xl bg-white shadow-inner" />
                ) : (
                  <div className="flex flex-col items-center justify-center h-full text-slate-400 gap-2">
                    <svg className="w-8 h-8 animate-spin text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" /></svg>
                    <span className="text-sm font-medium text-slate-600">Generando vista previa oficial del PDF...</span>
                  </div>
                )}
              </div>
            )}

            <div className="px-6 py-4 border-t border-slate-200 flex justify-between items-center bg-white flex-shrink-0">
              <button onClick={() => descargarPdf(verModal.id_matricula, verModal.estudiante)}
                className="flex items-center gap-2 px-4 py-2 rounded-lg text-sm text-blue-700 bg-blue-50 border border-blue-200 hover:bg-blue-100 transition font-semibold">
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H8a2 2 0 01-2-2V5a2 2 0 012-2h6l6 6v11a2 2 0 01-2 2z" />
                </svg>
                Descargar Ficha PDF
              </button>
              <button onClick={cerrarVer} className="px-6 py-2 rounded-lg text-sm text-slate-600 border border-slate-200 hover:bg-slate-50 transition">
                Cerrar
              </button>
            </div>
          </div>
        </div>
      )}
    </Layout>
  );
}
