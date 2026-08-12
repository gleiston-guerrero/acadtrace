import { useState, useEffect, useCallback } from 'react';
import Layout from '../components/Layout';
import { MENU_PRINCIPAL } from '../config/menu';
import api from '../utils/api';

const PRIMARY = '#243A76';

const RESULTADO_BADGE = {
  PROMOVIDO: 'bg-green-100 text-green-700',
  NO_PROMOVIDO: 'bg-red-100 text-red-600',
  RETIRADO: 'bg-slate-100 text-slate-600',
};

export default function Historial() {
  const [search, setSearch] = useState('');
  const [resultados, setResultados] = useState([]);
  const [buscando, setBuscando] = useState(false);
  const [seleccionado, setSeleccionado] = useState(null);
  const [datos, setDatos] = useState(null);
  const [cargandoHistorial, setCargandoHistorial] = useState(false);
  const [error, setError] = useState('');

  const buscar = useCallback(async () => {
    if (!search.trim()) { setResultados([]); return; }
    setBuscando(true);
    try {
      const res = await api.get('/estudiantes', { params: { q: search, page: 1, limit: 10 } });
      setResultados(res.data.data || []);
    } catch (e) {
      console.error('Error buscando estudiantes:', e);
    } finally {
      setBuscando(false);
    }
  }, [search]);

  useEffect(() => {
    const t = setTimeout(buscar, 350);
    return () => clearTimeout(t);
  }, [buscar]);

  const verHistorial = async (estudiante) => {
    setSeleccionado(estudiante);
    setDatos(null);
    setError('');
    setCargandoHistorial(true);
    try {
      const res = await api.get(`/historial/estudiante/${estudiante.id_estudiante}`);
      setDatos(res.data);
    } catch (e) {
      setError(e.response?.data?.error || 'No se pudo cargar el historial del estudiante');
    } finally {
      setCargandoHistorial(false);
    }
  };

  return (
    <Layout breadcrumb={['Inicio', 'Historial académico']} menuItems={MENU_PRINCIPAL} seccion="historial">
      <div className="mb-4">
        <h1 className="text-base font-bold text-slate-700">Historial académico</h1>
        <p className="text-xs text-slate-400">Años cursados, promedios anuales y promociones de un estudiante</p>
      </div>

      <div className="grid md:grid-cols-[20rem_1fr] gap-4 items-start">
        {/* Buscador + resultados */}
        <div className="bg-white border border-slate-200 rounded-2xl p-4">
          <div className="relative mb-3">
            <svg className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <input value={search} onChange={e => setSearch(e.target.value)}
              placeholder="Buscar por nombre o cédula..."
              className="w-full pl-9 pr-4 py-2 text-sm border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 bg-slate-50" />
          </div>

          {buscando ? (
            <p className="text-xs text-slate-400 text-center py-6">Buscando...</p>
          ) : resultados.length === 0 ? (
            <p className="text-xs text-slate-400 text-center py-6">
              {search ? 'Sin resultados' : 'Escribe para buscar un estudiante'}
            </p>
          ) : (
            <div className="space-y-1 max-h-[28rem] overflow-y-auto">
              {resultados.map(est => (
                <button key={est.id_estudiante} onClick={() => verHistorial(est)}
                  className={`w-full text-left px-3 py-2 rounded-lg transition ${
                    seleccionado?.id_estudiante === est.id_estudiante ? 'bg-blue-50' : 'hover:bg-slate-50'
                  }`}
                  style={seleccionado?.id_estudiante === est.id_estudiante ? { color: PRIMARY } : {}}
                >
                  <p className="text-sm font-medium">{est.apellidos}, {est.nombres}</p>
                  <p className="text-xs text-slate-400 font-mono">{est.cedula || '—'}</p>
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Historial del seleccionado */}
        <div>
          {!seleccionado ? (
            <div className="bg-white border border-dashed border-slate-200 rounded-2xl p-12 text-center text-slate-400 text-sm">
              Selecciona un estudiante para ver su historial académico
            </div>
          ) : cargandoHistorial ? (
            <div className="bg-white border border-slate-200 rounded-2xl p-12 text-center text-slate-400 text-sm">
              Cargando historial...
            </div>
          ) : error ? (
            <div className="bg-red-50 border border-red-200 rounded-2xl p-6 text-red-600 text-sm">{error}</div>
          ) : datos && (
            <div className="space-y-4">
              <div className="bg-white border border-slate-200 rounded-2xl p-5 flex items-center gap-4">
                <div className="w-12 h-12 rounded-full flex items-center justify-center flex-shrink-0" style={{ backgroundColor: '#e8edf7' }}>
                  <span style={{ color: PRIMARY }} className="text-lg font-bold">
                    {datos.estudiante.nombres?.charAt(0)?.toUpperCase()}
                  </span>
                </div>
                <div>
                  <p className="font-bold text-slate-700">{datos.estudiante.apellidos}, {datos.estudiante.nombres}</p>
                  <p className="text-xs text-slate-400">
                    {datos.estudiante.cedula || '—'} · {datos.estudiante.codigo_estudiante || 'Sin código'}
                  </p>
                </div>
              </div>

              {datos.historial.length === 0 ? (
                <div className="bg-white border border-slate-200 rounded-2xl p-10 text-center text-slate-400 text-sm">
                  Este estudiante todavía no tiene resultados de promoción registrados.
                </div>
              ) : (
                <div className="bg-white border border-slate-200 rounded-2xl overflow-hidden">
                  <table className="w-full text-sm">
                    <thead>
                      <tr style={{ backgroundColor: PRIMARY }} className="text-white text-xs">
                        <th className="px-4 py-3 text-left">Año lectivo</th>
                        <th className="px-4 py-3 text-left">Grado</th>
                        <th className="px-4 py-3 text-center">Resultado</th>
                        <th className="px-4 py-3 text-center">Promedio anual</th>
                        <th className="px-4 py-3 text-left">Observaciones</th>
                        <th className="px-4 py-3 text-left">Registrado</th>
                      </tr>
                    </thead>
                    <tbody>
                      {datos.historial.map((h, i) => (
                        <tr key={h.id_historial} className={`border-t border-slate-100 ${i % 2 === 1 ? 'bg-slate-50/40' : ''}`}>
                          <td className="px-4 py-2.5 font-medium text-slate-700">{h.ano_lectivo || '—'}</td>
                          <td className="px-4 py-2.5 text-slate-600">{h.grado || '—'}</td>
                          <td className="px-4 py-2.5 text-center">
                            <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${RESULTADO_BADGE[h.resultado] || 'bg-slate-100 text-slate-600'}`}>
                              {h.resultado || '—'}
                            </span>
                          </td>
                          <td className="px-4 py-2.5 text-center font-mono text-slate-700">{h.promedio_anual ?? '—'}</td>
                          <td className="px-4 py-2.5 text-slate-500 text-xs max-w-56">{h.observaciones || '—'}</td>
                          <td className="px-4 py-2.5 text-slate-400 text-xs">
                            {h.registrado_por || '—'}
                            {h.fecha_registro && <><br />{new Date(h.fecha_registro).toLocaleDateString('es-EC')}</>}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </Layout>
  );
}
