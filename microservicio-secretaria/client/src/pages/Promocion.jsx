import { useState, useEffect } from 'react';
import Layout from '../components/Layout';
import api, { apiPrincipal } from '../utils/api';

const PRIMARY = '#243A76';

const menuItems = [
  { id: 'resumen', label: 'Resumen de promoción', icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg> },
  { id: 'nuevo', label: 'Registrar resultado', icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg> },
];

export default function Promocion() {
  const [anos, setAnos] = useState([]);
  const [anoSel, setAnoSel] = useState('');
  const [resumen, setResumen] = useState([]);
  const [pendientes, setPendientes] = useState([]);
  const [modal, setModal] = useState(false);
  const [form, setForm] = useState({ id_matricula: '', resultado: '', promedio_anual: '', observaciones: '' });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    apiPrincipal.get('/anos-lectivos').then(r => {
      setAnos(r.data || []);
      const actual = (r.data || []).find(a => a.esActual);
      if (actual) setAnoSel(String(actual.idAnoLectivo));
    }).catch(() => {});
  }, []);

  useEffect(() => {
    if (!anoSel) return;
    Promise.all([
      api.get(`/historial/ano-lectivo/${anoSel}/resumen`),
      api.get(`/historial/ano-lectivo/${anoSel}/sin-promocion`),
    ]).then(([r, p]) => { setResumen(r.data); setPendientes(p.data); }).catch(() => {});
  }, [anoSel]);

  const handleSubmit = async (e) => {
    e.preventDefault(); setSaving(true); setError('');
    try {
      await api.post('/historial', form);
      setModal(false);
      if (anoSel) {
        const [r, p] = await Promise.all([
          api.get(`/historial/ano-lectivo/${anoSel}/resumen`),
          api.get(`/historial/ano-lectivo/${anoSel}/sin-promocion`),
        ]);
        setResumen(r.data); setPendientes(p.data);
      }
    } catch (err) {
      setError(err.response?.data?.error || 'Error al registrar');
    } finally { setSaving(false); }
  };

  const abrirModalPara = (idMatricula) => {
    setForm({ id_matricula: idMatricula ? String(idMatricula) : '', resultado: '', promedio_anual: '', observaciones: '' });
    setError('');
    setModal(true);
  };

  const handleSeccion = (id) => {
    if (id === 'nuevo') { abrirModalPara(null); return; }
  };

  return (
    <Layout breadcrumb={['Inicio', 'Promoción']} sidebarTitle="Promoción" menuItems={menuItems} seccion="resumen" onSeccionChange={handleSeccion}>
      <div className="flex items-center justify-between mb-4">
        <div>
          <h1 className="text-base font-bold text-slate-700">Promoción de estudiantes</h1>
          <p className="text-xs text-slate-400">Registro de resultados académicos al terminar el año lectivo</p>
        </div>
        <button onClick={() => abrirModalPara(null)}
          style={{ backgroundColor: PRIMARY }}
          className="flex items-center gap-2 text-white text-sm px-4 py-2 rounded-lg hover:opacity-90 transition shadow-sm">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg>
          Registrar resultado
        </button>
      </div>

      <div className="mb-4">
        <select value={anoSel} onChange={e => setAnoSel(e.target.value)}
          className="px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white">
          <option value="">— Año lectivo —</option>
          {anos.map(a => <option key={a.idAnoLectivo} value={a.idAnoLectivo}>{a.nombre}</option>)}
        </select>
      </div>

      {/* Resumen */}
      {resumen.length > 0 && (
        <div className="bg-white border border-slate-200 rounded-2xl overflow-hidden mb-4">
          <div style={{ backgroundColor: PRIMARY }} className="px-4 py-3">
            <p className="text-white font-semibold text-sm">Resumen por grado</p>
          </div>
          <table className="w-full text-sm">
            <thead><tr className="bg-slate-50 text-xs text-slate-500"><th className="px-4 py-2 text-left">Grado</th><th className="px-4 py-2 text-center">Promovidos</th><th className="px-4 py-2 text-center">No promovidos</th><th className="px-4 py-2 text-center">Retirados</th><th className="px-4 py-2 text-center">Prom. general</th></tr></thead>
            <tbody>
              {resumen.map((r, i) => (
                <tr key={r.grado} className={`border-t border-slate-100 ${i % 2 === 1 ? 'bg-slate-50/50' : ''}`}>
                  <td className="px-4 py-2.5 font-medium text-slate-700">{r.grado}</td>
                  <td className="px-4 py-2.5 text-center text-green-600 font-semibold">{r.promovidos}</td>
                  <td className="px-4 py-2.5 text-center text-red-500 font-semibold">{r.no_promovidos}</td>
                  <td className="px-4 py-2.5 text-center text-slate-500">{r.retirados}</td>
                  <td className="px-4 py-2.5 text-center font-mono text-slate-700">{r.promedio_general || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Pendientes */}
      {pendientes.length > 0 && (
        <div className="bg-white border border-amber-200 rounded-2xl overflow-hidden">
          <div className="bg-amber-50 px-4 py-3 flex items-center gap-2">
            <svg className="w-4 h-4 text-amber-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" /></svg>
            <p className="text-amber-700 font-semibold text-sm">Matrículas sin resultado: {pendientes.length}</p>
          </div>
          <table className="w-full text-sm">
            <thead><tr className="bg-slate-50 text-xs text-slate-500"><th className="px-4 py-2 text-left">Estudiante</th><th className="px-4 py-2 text-left">Cédula</th><th className="px-4 py-2 text-left">Grado</th><th className="px-4 py-2 text-center">ID Matrícula</th><th className="px-4 py-2 text-center">Acción</th></tr></thead>
            <tbody>
              {pendientes.slice(0, 20).map((p, i) => (
                <tr key={p.id_matricula} className={`border-t border-slate-100 ${i % 2 === 1 ? 'bg-slate-50/50' : ''}`}>
                  <td className="px-4 py-2.5 text-slate-700">{p.estudiante}</td>
                  <td className="px-4 py-2.5 text-slate-500">{p.cedula || '—'}</td>
                  <td className="px-4 py-2.5 text-slate-500">{p.grado} "{p.paralelo}"</td>
                  <td className="px-4 py-2.5 text-center font-mono text-xs text-slate-400">{p.id_matricula}</td>
                  <td className="px-4 py-2.5 text-center">
                    <button onClick={() => abrirModalPara(p.id_matricula)} style={{ color: PRIMARY }} className="text-xs font-semibold hover:underline">
                      Registrar
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* MODAL */}
      {modal && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md">
            <div style={{ backgroundColor: PRIMARY }} className="px-6 py-4 flex items-center justify-between rounded-t-2xl">
              <h2 className="text-white font-semibold text-sm">Registrar Resultado</h2>
              <button onClick={() => setModal(false)} className="text-white/70 hover:text-white"><svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg></button>
            </div>
            <form onSubmit={handleSubmit} className="p-6 space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1">ID de matrícula *</label>
                <input type="number" required value={form.id_matricula} onChange={e => setForm({ ...form, id_matricula: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-slate-50" />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1">Resultado *</label>
                <select required value={form.resultado} onChange={e => setForm({ ...form, resultado: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-slate-50">
                  <option value="">— Seleccionar —</option>
                  <option value="PROMOVIDO">Promovido</option>
                  <option value="NO_PROMOVIDO">No promovido</option>
                  <option value="RETIRADO">Retirado</option>
                </select>
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1">Promedio anual</label>
                <input type="number" min="0" max="10" step="0.01" value={form.promedio_anual} onChange={e => setForm({ ...form, promedio_anual: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-slate-50" />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1">Observaciones</label>
                <textarea value={form.observaciones} onChange={e => setForm({ ...form, observaciones: e.target.value })} rows={2}
                  className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-slate-50 resize-none" />
              </div>
              {error && <p className="text-red-600 text-xs bg-red-50 px-3 py-2 rounded-lg">{error}</p>}
              <div className="flex gap-3 pt-2">
                <button type="button" onClick={() => setModal(false)} className="flex-1 py-2.5 border border-slate-200 rounded-lg text-sm text-slate-600 hover:bg-slate-50 transition">Cancelar</button>
                <button type="submit" disabled={saving} style={{ backgroundColor: PRIMARY }} className="flex-1 py-2.5 rounded-lg text-sm text-white font-semibold hover:opacity-90 transition disabled:opacity-60">{saving ? 'Guardando...' : 'Registrar'}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </Layout>
  );
}
