import { useState, useEffect } from 'react';
import Layout from '../components/Layout';
import api, { apiPrincipal } from '../utils/api';

const PRIMARY = '#243A76';

export default function Reportes() {
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
          <div className="bg-white border border-slate-200 rounded-2xl p-5">
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
        <div className="bg-white border border-slate-200 rounded-2xl p-5">
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
        <div className="bg-white border border-slate-200 rounded-2xl p-5">
          <h2 className="text-sm font-bold text-slate-700 mb-2">Documentos individuales</h2>
          <p className="text-xs text-slate-400 mb-4">Ingresa el ID del registro para generar el documento.</p>
          <div className="grid sm:grid-cols-2 gap-4">
            {[
              { label: 'Certificado de matrícula', hint: 'ID de matrícula', url: (id) => `${BASE}/certificado-matricula/${id}` },
              { label: 'Ficha del estudiante', hint: 'ID de estudiante', url: (id) => `${BASE}/ficha-estudiante/${id}` },
              {
                label: 'Libreta de calificaciones', hint: 'ID de matrícula',
                url: (id, periodo) => `${BASE}/libreta/${id}${periodo ? `?idPeriodo=${periodo}` : ''}`,
                extraField: { type: 'number', placeholder: 'ID período (opc.)' },
              },
              {
                label: 'Asistencia mensual', hint: 'ID de matrícula',
                url: (id, mes) => `${BASE}/asistencia-mensual/${id}?mes=${mes}`,
                extraField: { type: 'month', placeholder: 'Mes', required: true },
              },
              { label: 'Ficha del representante', hint: 'ID de representante', url: (id) => `${BASE}/ficha-representante/${id}` },
            ].map(r => (
              <DocIndividual key={r.label} label={r.label} hint={r.hint} buildUrl={r.url} extraField={r.extraField} />
            ))}
          </div>
        </div>
      </div>
    </Layout>
  );
}

function DocIndividual({ label, hint, buildUrl, extraField }) {
  const [id, setId] = useState('');
  const [extra, setExtra] = useState('');
  const listo = id && (!extraField?.required || extra);
  return (
    <div className="border border-slate-200 rounded-xl p-4">
      <p className="text-sm font-semibold text-slate-700 mb-3">{label}</p>
      <div className="flex gap-2">
        <input type="number" value={id} onChange={e => setId(e.target.value)} placeholder={hint}
          className="flex-1 px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-slate-50" />
        {extraField && (
          <input type={extraField.type} value={extra} onChange={e => setExtra(e.target.value)} placeholder={extraField.placeholder}
            className="w-32 px-2 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-slate-50" />
        )}
        <button disabled={!listo} onClick={() => window.open(buildUrl(id, extra), '_blank')}
          style={listo ? { backgroundColor: PRIMARY } : {}}
          className="px-3 py-2 rounded-lg text-white text-sm hover:opacity-90 transition disabled:opacity-40 disabled:bg-slate-300 disabled:cursor-not-allowed">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" /></svg>
        </button>
      </div>
    </div>
  );
}
