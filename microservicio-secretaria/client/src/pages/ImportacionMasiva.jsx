import { useState, useEffect } from 'react';
import Layout from '../components/Layout';
import { MENU_PRINCIPAL } from '../config/menu';
import api, { apiPrincipal } from '../utils/api';

const PRIMARY = '#243A76';

export default function ImportacionMasiva() {
  const [archivo, setArchivo] = useState(null);
  const [preview, setPreview] = useState(null);
  const [cargando, setCargando] = useState(false);
  const [confirmando, setConfirmando] = useState(false);
  const [error, setError] = useState('');
  const [resultado, setResultado] = useState(null);

  const [anoActual, setAnoActual] = useState(null);
  const [grados, setGrados] = useState([]);
  const [paralelos, setParalelos] = useState([]);
  const [idGrado, setIdGrado] = useState('');
  const [idParalelo, setIdParalelo] = useState('');
  const idAno = anoActual?.idAnoLectivo || '';

  useEffect(() => {
    // El listado completo de años lectivos (GET /anos-lectivos) es solo para DIRECTOR;
    // Secretaria solo puede consultar el actual, que es el único relevante para matricular al importar.
    apiPrincipal.get('/anos-lectivos/actual').then(r => setAnoActual(r.data)).catch(() => {});
    apiPrincipal.get('/grados').then(r => setGrados(r.data || [])).catch(() => {});
  }, []);

  useEffect(() => {
    setIdParalelo('');
    setParalelos([]);
    if (!idGrado) return;
    api.get(`/matriculas/paralelos/${idGrado}`).then(r => setParalelos(r.data || [])).catch(() => {});
  }, [idGrado]);

  const handlePrevisualizar = async (e) => {
    e.preventDefault();
    if (!archivo) { setError('Selecciona un archivo .csv, .xlsx, .xls o .pdf'); return; }
    setError(''); setCargando(true);
    try {
      const formData = new FormData();
      formData.append('archivo', archivo);
      const r = await api.post('/importacion-estudiantes/parsear', formData, { headers: { 'Content-Type': 'multipart/form-data' } });
      setPreview(r.data);
    } catch (err) {
      setError(err.response?.data?.error || 'No se pudo procesar el archivo');
      setPreview(null);
    } finally {
      setCargando(false);
    }
  };

  const handleConfirmar = async () => {
    setConfirmando(true); setError('');
    try {
      const r = await api.post('/importacion-estudiantes/confirmar', {
        estudiantes: preview.estudiantes,
        id_grado: idGrado || null,
        id_paralelo: idParalelo || null,
        id_ano_lectivo: idAno || null,
      });
      setResultado(r.data);
      setPreview(null);
      setArchivo(null);
    } catch (err) {
      setError(err.response?.data?.error || 'Error al confirmar la importación');
    } finally {
      setConfirmando(false);
    }
  };

  const reiniciar = () => {
    setResultado(null);
    setPreview(null);
    setArchivo(null);
    setError('');
    setIdGrado('');
  };

  return (
    <Layout breadcrumb={['Inicio', 'Importación masiva']} menuItems={MENU_PRINCIPAL} seccion="importacion">
      <div className="mb-4">
        <h1 className="text-base font-bold text-slate-700">Importación masiva de estudiantes</h1>
        <p className="text-xs text-slate-400">Desde Excel, CSV o PDF (listado CAS)</p>
      </div>

      {error && (
        <div className="mb-4 bg-red-50 border border-red-200 rounded-xl px-4 py-3 flex justify-between">
          <span className="text-red-600 text-sm">{error}</span>
          <button onClick={() => setError('')} className="text-red-400 ml-4">✕</button>
        </div>
      )}

      {resultado ? (
        <div className="bg-white border border-slate-200 rounded-2xl p-8 text-center max-w-lg mx-auto">
          <div className="w-14 h-14 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <svg className="w-7 h-7 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <h3 className="font-bold text-slate-700 mb-1">Importación completada</h3>
          <div className="grid grid-cols-2 gap-3 mt-5 text-left">
            <div className="bg-green-50 rounded-xl p-4">
              <p className="text-2xl font-bold text-green-600">{resultado.creados}</p>
              <p className="text-xs text-slate-500">Nuevos estudiantes</p>
            </div>
            <div className="bg-amber-50 rounded-xl p-4">
              <p className="text-2xl font-bold text-amber-600">{resultado.existentes}</p>
              <p className="text-xs text-slate-500">Ya existían</p>
            </div>
            {idGrado && idParalelo && (
              <div className="bg-blue-50 rounded-xl p-4">
                <p className="text-2xl font-bold text-blue-600">{resultado.matriculados}</p>
                <p className="text-xs text-slate-500">Matriculados</p>
              </div>
            )}
            {resultado.omitidos > 0 && (
              <div className="bg-red-50 rounded-xl p-4">
                <p className="text-2xl font-bold text-red-500">{resultado.omitidos}</p>
                <p className="text-xs text-slate-500">Omitidos por error</p>
              </div>
            )}
          </div>
          <button onClick={reiniciar} style={{ backgroundColor: PRIMARY }}
            className="w-full mt-6 py-2.5 rounded-lg text-sm text-white font-semibold hover:opacity-90 transition">
            Importar otro archivo
          </button>
        </div>
      ) : !preview ? (
        <div className="bg-white border border-slate-200 rounded-2xl p-6 max-w-2xl">
          <form onSubmit={handlePrevisualizar} className="space-y-4">
            <div className="border-2 border-dashed border-slate-200 rounded-xl p-6 text-center hover:border-blue-300 transition">
              <input type="file" accept=".csv,.xlsx,.xls,.pdf" onChange={e => setArchivo(e.target.files?.[0] || null)}
                className="w-full text-sm text-slate-600 file:mr-3 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-medium file:text-white file:cursor-pointer"
                style={{ '--tw-file-bg': PRIMARY }} />
              <p className="text-xs text-slate-400 mt-2">Columnas esperadas: CEDULA, NOMBRES, APELLIDOS, CORREO (opcional)</p>
            </div>

            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-2">Matricular al importar (opcional)</p>
              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="block text-xs text-slate-400 mb-1">Año lectivo</label>
                  <div className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-slate-100 text-slate-500">
                    {anoActual ? anoActual.nombre : 'Cargando...'}
                  </div>
                </div>
                <div>
                  <label className="block text-xs text-slate-400 mb-1">Grado</label>
                  <select value={idGrado} onChange={e => setIdGrado(e.target.value)} disabled={!idAno}
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-slate-50 disabled:opacity-50">
                    <option value="">— Seleccionar —</option>
                    {grados.map(g => <option key={g.idGrado} value={g.idGrado}>{g.nombre}</option>)}
                  </select>
                </div>
                <div>
                  <label className="block text-xs text-slate-400 mb-1">Paralelo</label>
                  <select value={idParalelo} onChange={e => setIdParalelo(e.target.value)} disabled={!idGrado || paralelos.length === 0}
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-slate-50 disabled:opacity-50">
                    <option value="">{idGrado && paralelos.length === 0 ? 'Cargando...' : '— Seleccionar —'}</option>
                    {paralelos.map(p => <option key={p.id_paralelo} value={p.id_paralelo}>{p.letra}</option>)}
                  </select>
                </div>
              </div>
              <p className="text-xs text-slate-400 mt-1.5">Si dejas esto vacío, los estudiantes se crean sin matrícula (puedes matricularlos luego desde el módulo de Matrículas).</p>
            </div>

            <div className="flex justify-end pt-2">
              <button type="submit" disabled={cargando} style={{ backgroundColor: PRIMARY }}
                className="px-6 py-2.5 text-white rounded-lg text-sm font-medium hover:opacity-90 transition disabled:opacity-60">
                {cargando ? 'Procesando...' : 'Previsualizar'}
              </button>
            </div>
          </form>
        </div>
      ) : (
        <div className="bg-white border border-slate-200 rounded-2xl p-6 max-w-3xl">
          <div className="flex items-center justify-between mb-3">
            <p className="text-sm text-slate-500">
              {preview.totalFilas} fila{preview.totalFilas !== 1 ? 's' : ''} — <span className="text-green-600 font-medium">{preview.filasValidas} válida{preview.filasValidas !== 1 ? 's' : ''}</span>
              {preview.filasConError > 0 && <> — <span className="text-red-600 font-medium">{preview.filasConError} con error</span></>}
            </p>
            <button onClick={() => setPreview(null)} className="text-xs text-slate-400 hover:text-slate-600 underline">Volver</button>
          </div>

          <div className="bg-white rounded-xl border border-slate-200 overflow-hidden mb-4">
            <div className="overflow-x-auto" style={{ maxHeight: '24rem', overflowY: 'auto' }}>
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-slate-100 sticky top-0" style={{ backgroundColor: '#f8f9fc' }}>
                    <th className="text-left px-3 py-2.5 text-xs font-semibold text-slate-500 uppercase tracking-wide">Cédula</th>
                    <th className="text-left px-3 py-2.5 text-xs font-semibold text-slate-500 uppercase tracking-wide">Apellidos</th>
                    <th className="text-left px-3 py-2.5 text-xs font-semibold text-slate-500 uppercase tracking-wide">Nombres</th>
                    <th className="text-center px-3 py-2.5 text-xs font-semibold text-slate-500 uppercase tracking-wide">Estado</th>
                  </tr>
                </thead>
                <tbody>
                  {preview.estudiantes.map((est, i) => (
                    <tr key={i} className="border-b border-slate-50">
                      <td className="px-3 py-2 text-slate-600">{est.cedula || '—'}</td>
                      <td className="px-3 py-2 text-slate-600">{est.apellidos || '—'}</td>
                      <td className="px-3 py-2 text-slate-600">{est.nombres || '—'}</td>
                      <td className="px-3 py-2 text-center">
                        {est.error ? (
                          <span className="text-[11px] font-semibold px-2 py-0.5 rounded-md bg-red-100 text-red-700" title={est.error}>Error</span>
                        ) : est.yaExiste ? (
                          <span className="text-[11px] font-semibold px-2 py-0.5 rounded-md bg-amber-100 text-amber-700">Ya existe</span>
                        ) : (
                          <span className="text-[11px] font-semibold px-2 py-0.5 rounded-md bg-green-100 text-green-700">Nuevo</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          <div className="flex justify-end gap-3">
            <button onClick={() => setPreview(null)} className="px-5 py-2 border border-slate-200 rounded-lg text-sm text-slate-600 hover:bg-slate-50 transition">
              Cancelar
            </button>
            <button onClick={handleConfirmar} disabled={confirmando || preview.filasValidas === 0}
              style={{ backgroundColor: PRIMARY }}
              className="px-5 py-2 text-white rounded-lg text-sm font-medium hover:opacity-90 transition disabled:opacity-60">
              {confirmando ? 'Confirmando...' : `Confirmar (${preview.filasValidas})`}
            </button>
          </div>
        </div>
      )}
    </Layout>
  );
}
