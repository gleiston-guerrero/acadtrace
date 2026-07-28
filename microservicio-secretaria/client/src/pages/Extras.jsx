import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Layout from '../components/Layout';
import api, { apiPrincipal } from '../utils/api';

const PRIMARY = '#243A76';

export function Reportes() {
  const [anos, setAnos] = useState([]);
  const [anoSel, setAnoSel] = useState('');
  const [grados, setGrados] = useState([]);
  const [gradoSel, setGradoSel] = useState('');
  const [stats, setStats] = useState(null);
  const token = localStorage.getItem('token');

  useEffect(() => {
    apiPrincipal.get('/anos-lectivos').then(r => {
      setAnos(r.data || []);
      const actual = (r.data || []).find(a => a.esActual);
      if (actual) setAnoSel(String(actual.idAnoLectivo));
    }).catch(() => {});
    apiPrincipal.get('/grados').then(r => setGrados(r.data || [])).catch(() => {});
  }, []);

  useEffect(() => {
    if (anoSel) {
      api.get(`/reportes/estadisticas/${anoSel}`).then(r => setStats(r.data)).catch(() => {});
    }
  }, [anoSel]);

  const abrirPDF = (url) => {
    window.open(`${url}?token=${token}`, '_blank');
  };

  const BASE = 'http://localhost:3000/api/secretario/reportes';

  return (
    <Layout breadcrumb={['Inicio', 'Reportes']}>
      <div className="mb-4">
        <h1 className="text-base font-bold text-slate-700">Reportes y Certificados</h1>
        <p className="text-xs text-slate-400">Genera documentos PDF del sistema</p>
      </div>

      <div className="grid gap-4">
        {/* Estadísticas */}
        {stats && (
          <div className="bg-white border border-slate-200 rounded-xl p-5">
            <h2 className="text-sm font-bold text-slate-700 mb-4">Resumen del año lectivo</h2>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              {[
                { label: 'Total matrículas', val: stats.totales?.total, color: 'text-blue-600', bg: 'bg-blue-50' },
                { label: 'Activas', val: stats.totales?.activas, color: 'text-green-600', bg: 'bg-green-50' },
                { label: 'Masculino', val: stats.totales?.masculino, color: 'text-indigo-600', bg: 'bg-indigo-50' },
                { label: 'Femenino', val: stats.totales?.femenino, color: 'text-pink-600', bg: 'bg-pink-50' },
              ].map(s => (
                <div key={s.label} className={`${s.bg} rounded-xl p-4`}>
                  <p className={`text-2xl font-bold ${s.color}`}>{s.val || 0}</p>
                  <p className="text-xs text-slate-500">{s.label}</p>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Reportes disponibles */}
        <div className="bg-white border border-slate-200 rounded-xl p-5">
          <h2 className="text-sm font-bold text-slate-700 mb-4">Nómina de matrículas</h2>
          <div className="flex flex-wrap gap-3 mb-4">
            <select value={anoSel} onChange={e => setAnoSel(e.target.value)}
              className="px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-slate-50">
              <option value="">— Año lectivo —</option>
              {anos.map(a => <option key={a.idAnoLectivo} value={a.idAnoLectivo}>{a.nombre}</option>)}
            </select>
            <select value={gradoSel} onChange={e => setGradoSel(e.target.value)}
              className="px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-slate-50">
              <option value="">— Todos los grados —</option>
              {grados.map(g => <option key={g.idGrado} value={g.idGrado}>{g.nombre}</option>)}
            </select>
          </div>
          <button
            disabled={!anoSel}
            onClick={() => {
              const url = `${BASE}/nomina-matriculas/${anoSel}${gradoSel ? `?id_grado=${gradoSel}` : ''}`;
              window.open(url, '_blank');
            }}
            style={anoSel ? { backgroundColor: PRIMARY } : {}}
            className="flex items-center gap-2 text-white text-sm px-5 py-2.5 rounded-lg hover:opacity-90 transition disabled:opacity-40 disabled:bg-slate-300 disabled:cursor-not-allowed">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" /></svg>
            Descargar nómina PDF
          </button>
        </div>

        {/* Certificado / Ficha individual */}
        <div className="bg-white border border-slate-200 rounded-xl p-5">
          <h2 className="text-sm font-bold text-slate-700 mb-2">Documentos individuales</h2>
          <p className="text-xs text-slate-400 mb-4">Ingresa el ID del registro para generar el documento.</p>
          <div className="grid sm:grid-cols-2 gap-4">
            {[
              { label: 'Certificado de matrícula', hint: 'ID de matrícula', url: (id) => `${BASE}/certificado-matricula/${id}` },
              { label: 'Ficha del estudiante', hint: 'ID de estudiante', url: (id) => `${BASE}/ficha-estudiante/${id}` },
            ].map(r => (
              <DocIndividual key={r.label} label={r.label} hint={r.hint} buildUrl={r.url} />
            ))}
          </div>
        </div>
      </div>
    </Layout>
  );
}

function DocIndividual({ label, hint, buildUrl }) {
  const [id, setId] = useState('');
  return (
    <div className="border border-slate-200 rounded-xl p-4">
      <p className="text-sm font-semibold text-slate-700 mb-3">{label}</p>
      <div className="flex gap-2">
        <input type="number" value={id} onChange={e => setId(e.target.value)} placeholder={hint}
          className="flex-1 px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-slate-50" />
        <button disabled={!id} onClick={() => window.open(buildUrl(id), '_blank')}
          style={id ? { backgroundColor: PRIMARY } : {}}
          className="px-3 py-2 rounded-lg text-white text-sm hover:opacity-90 transition disabled:opacity-40 disabled:bg-slate-300 disabled:cursor-not-allowed">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" /></svg>
        </button>
      </div>
    </div>
  );
}

// ──────────────────────────────────────────────
export function Historial() {
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

  return (
    <Layout breadcrumb={['Inicio', 'Promoción']}>
      <div className="flex items-center justify-between mb-4">
        <div>
          <h1 className="text-base font-bold text-slate-700">Registro de Promoción</h1>
          <p className="text-xs text-slate-400">Resultados académicos por año lectivo</p>
        </div>
        <button onClick={() => { setForm({ id_matricula: '', resultado: '', promedio_anual: '', observaciones: '' }); setError(''); setModal(true); }}
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
        <div className="bg-white border border-slate-200 rounded-xl overflow-hidden mb-4">
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
        <div className="bg-white border border-amber-200 rounded-xl overflow-hidden">
          <div className="bg-amber-50 px-4 py-3 flex items-center gap-2">
            <svg className="w-4 h-4 text-amber-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" /></svg>
            <p className="text-amber-700 font-semibold text-sm">Matrículas sin resultado: {pendientes.length}</p>
          </div>
          <table className="w-full text-sm">
            <thead><tr className="bg-slate-50 text-xs text-slate-500"><th className="px-4 py-2 text-left">Estudiante</th><th className="px-4 py-2 text-left">Cédula</th><th className="px-4 py-2 text-left">Grado</th><th className="px-4 py-2 text-center">ID Matrícula</th></tr></thead>
            <tbody>
              {pendientes.slice(0, 20).map((p, i) => (
                <tr key={p.id_matricula} className={`border-t border-slate-100 ${i % 2 === 1 ? 'bg-slate-50/50' : ''}`}>
                  <td className="px-4 py-2.5 text-slate-700">{p.estudiante}</td>
                  <td className="px-4 py-2.5 text-slate-500">{p.cedula || '—'}</td>
                  <td className="px-4 py-2.5 text-slate-500">{p.grado} "{p.paralelo}"</td>
                  <td className="px-4 py-2.5 text-center font-mono text-xs text-slate-400">{p.id_matricula}</td>
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

// ──────────────────────────────────────────────
export function CambiarPassword() {
  const [form, setForm] = useState({ actual: '', nueva: '', confirmar: '' });
  const [loading, setLoading] = useState(false);
  const [msg, setMsg] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (form.nueva.length < 6) { setError('La nueva contraseña debe tener mínimo 6 caracteres'); return; }
    if (form.nueva !== form.confirmar) { setError('Las contraseñas no coinciden'); return; }
    setLoading(true); setError(''); setMsg('');
    try {
      // Endpoint: PATCH /api/auth/cambiar-password en sga-principal
      // Campo correcto del DTO: passwordNuevo (no passwordNueva)
      await apiPrincipal.patch('/auth/cambiar-password', {
        passwordActual: form.actual,
        passwordNuevo: form.nueva,
      });
      setMsg('¡Contraseña actualizada! Redirigiendo...');
      setForm({ actual: '', nueva: '', confirmar: '' });
      localStorage.setItem('primerIngreso', 'false');
      setTimeout(() => navigate('/dashboard'), 1500);
    } catch (err) {
      const mensaje = err.response?.data?.message || err.response?.data?.error || 'Contraseña actual incorrecta';
      setError(mensaje);
    } finally { setLoading(false); }
  };

  return (
    <Layout breadcrumb={['Inicio', 'Cambiar contraseña']}>
      <div className="max-w-md mx-auto">
        <div className="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
          <div style={{ backgroundColor: PRIMARY }} className="px-6 py-5">
            <h1 className="text-white font-bold text-sm">Cambiar contraseña</h1>
            <p className="text-white/60 text-xs mt-0.5">Mantén tu cuenta segura</p>
          </div>
          <form onSubmit={handleSubmit} className="p-6 space-y-4">
            {['actual', 'nueva', 'confirmar'].map((k, i) => (
              <div key={k}>
                <label className="block text-xs font-semibold text-slate-500 mb-1">
                  {['Contraseña actual', 'Nueva contraseña', 'Confirmar nueva contraseña'][i]}
                </label>
                <input type="password" required value={form[k]} onChange={e => setForm({ ...form, [k]: e.target.value })}
                  className="w-full px-3 py-2.5 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-slate-50" />
              </div>
            ))}
            {error && <div className="bg-red-50 border border-red-200 rounded-lg px-3 py-2 text-red-600 text-xs">{error}</div>}
            {msg && <div className="bg-green-50 border border-green-200 rounded-lg px-3 py-2 text-green-600 text-xs">{msg}</div>}
            <button type="submit" disabled={loading} style={{ backgroundColor: PRIMARY }}
              className="w-full py-2.5 rounded-lg text-sm text-white font-semibold hover:opacity-90 transition disabled:opacity-60">
              {loading ? 'Actualizando...' : 'Actualizar contraseña'}
            </button>
          </form>
        </div>
      </div>
    </Layout>
  );
}
