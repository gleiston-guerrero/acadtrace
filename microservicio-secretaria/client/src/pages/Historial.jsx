import { useState, useEffect, useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import Layout from '../components/Layout';
import api, { apiPrincipal } from '../utils/api';
import { useI18n } from '../context/I18nContext';
import { useToast } from '../components/Toast';
import FichaEstudianteModal from './FichaEstudianteModal';

const PRIMARY = '#243A76';
const PRIMARY_LIGHT = '#2d4a96';
const PRINCIPAL_ORIGIN = (apiPrincipal.defaults.baseURL || 'http://localhost:8080/api').replace(/\/api\/?$/, '');

const RESULTADO_CONFIG = {
  PROMOVIDO: {
    bg: 'bg-emerald-100 dark:bg-emerald-950/60',
    text: 'text-emerald-800 dark:text-emerald-300',
    border: 'border-emerald-200 dark:border-emerald-800',
    dot: 'bg-emerald-500',
    label: 'Promovido',
  },
  NO_PROMOVIDO: {
    bg: 'bg-rose-100 dark:bg-rose-950/60',
    text: 'text-rose-800 dark:text-rose-300',
    border: 'border-rose-200 dark:border-rose-800',
    dot: 'bg-rose-500',
    label: 'No Promovido',
  },
  RETIRADO: {
    bg: 'bg-amber-100 dark:bg-amber-950/60',
    text: 'text-amber-800 dark:text-amber-300',
    border: 'border-amber-200 dark:border-amber-800',
    dot: 'bg-amber-500',
    label: 'Retirado',
  },
  REGULAR: {
    bg: 'bg-blue-100 dark:bg-blue-950/60',
    text: 'text-blue-800 dark:text-blue-300',
    border: 'border-blue-200 dark:border-blue-800',
    dot: 'bg-blue-500',
    label: 'En Curso',
  },
};

const getBadge = (resultado) => {
  const r = (resultado || '').toUpperCase();
  return RESULTADO_CONFIG[r] || {
    bg: 'bg-slate-100 dark:bg-slate-800',
    text: 'text-slate-700 dark:text-slate-300',
    border: 'border-slate-200 dark:border-slate-700',
    dot: 'bg-slate-400',
    label: resultado || 'Registrado',
  };
};

const getEscalaEcuatoriana = (promedio) => {
  const num = Number(promedio);
  if (isNaN(num)) return { escala: 'Sin calificación', color: 'text-slate-400' };
  if (num >= 9.0) return { escala: 'Domina los aprendizajes (DAR)', color: 'text-emerald-600 dark:text-emerald-400' };
  if (num >= 7.0) return { escala: 'Alcanza los aprendizajes (AAR)', color: 'text-blue-600 dark:text-blue-400' };
  if (num >= 4.01) return { escala: 'Próximo a alcanzar (PAR)', color: 'text-amber-600 dark:text-amber-400' };
  return { escala: 'No alcanza los aprendizajes (NAR)', color: 'text-rose-600 dark:text-rose-400' };
};

export default function Historial() {
  const { t } = useI18n();
  const toast = useToast();
  const navigate = useNavigate();

  // Menú y Secciones
  const [seccion, setSeccion] = useState('estudiante'); // 'estudiante' | 'nomina'
  const [vistaHistorial, setVistaHistorial] = useState('timeline'); // 'timeline' | 'tabla'

  // Búsqueda de estudiantes
  const [search, setSearch] = useState('');
  const [resultados, setResultados] = useState([]);
  const [buscando, setBuscando] = useState(false);
  const [estudianteSel, setEstudianteSel] = useState(null);

  // Datos del historial
  const [datosHistorial, setDatosHistorial] = useState(null);
  const [matriculasEstudiante, setMatriculasEstudiante] = useState([]);
  const [cargandoHistorial, setCargandoHistorial] = useState(false);

  // Nómina por año lectivo
  const [anos, setAnos] = useState([]);
  const [anoSel, setAnoSel] = useState('');
  const [grados, setGrados] = useState([]);
  const [gradoSel, setGradoSel] = useState('');
  const [paralelos, setParalelos] = useState([]);
  const [paraleloSel, setParaleloSel] = useState('');
  const [estadoSel, setEstadoSel] = useState('');
  const [busquedaNomina, setBusquedaNomina] = useState('');
  const [nomina, setNomina] = useState([]);
  const [cargandoNomina, setCargandoNomina] = useState(false);
  const [paginaNomina, setPaginaNomina] = useState(1);
  const limitePorPagina = 12;

  // Modales
  const [modalFicha, setModalFicha] = useState(false);
  const [estudianteFicha, setEstudianteFicha] = useState(null);
  const [modalPdf, setModalPdf] = useState(null); // { titulo, blobUrl }
  const [cargandoPdf, setCargandoPdf] = useState(false);

  // Cargar Años Lectivos y Grados al inicio
  useEffect(() => {
    apiPrincipal.get('/anos-lectivos')
      .then(r => {
        const lista = r.data || [];
        setAnos(lista);
        const actual = lista.find(a => a.esActual);
        if (actual) setAnoSel(String(actual.idAnoLectivo));
        else if (lista.length > 0) setAnoSel(String(lista[0].idAnoLectivo));
      })
      .catch(() => {});

    apiPrincipal.get('/grados')
      .then(r => setGrados(r.data || []))
      .catch(() => {});
  }, []);

  // Cargar paralelos cuando cambia el grado en el explorador
  useEffect(() => {
    if (!gradoSel) {
      setParalelos([]);
      setParaleloSel('');
      return;
    }
    api.get(`/matriculas/paralelos/${gradoSel}`)
      .then(r => setParalelos(r.data || []))
      .catch(() => setParalelos([]));
  }, [gradoSel]);

  // Búsqueda de estudiantes con debounce
  const buscarEstudiantes = useCallback(async () => {
    if (!search.trim()) {
      setResultados([]);
      return;
    }
    setBuscando(true);
    try {
      const res = await api.get('/estudiantes', { params: { q: search.trim(), page: 1, limit: 12 } });
      setResultados(res.data?.data || []);
    } catch (e) {
      console.error('Error buscando estudiantes:', e);
    } finally {
      setBuscando(false);
    }
  }, [search]);

  useEffect(() => {
    const timer = setTimeout(buscarEstudiantes, 300);
    return () => clearTimeout(timer);
  }, [buscarEstudiantes]);

  // Cargar historial completo y matrículas asociadas del estudiante
  const seleccionarEstudiante = async (est) => {
    setEstudianteSel(est);
    setDatosHistorial(null);
    setMatriculasEstudiante([]);
    setCargandoHistorial(true);
    setSearch('');
    setResultados([]);

    const id = est.id_estudiante || est.idEstudiante;

    try {
      const [resHist, resMat] = await Promise.all([
        api.get(`/historial/estudiante/${id}`).catch(() => ({ data: null })),
        api.get(`/matriculas/estudiante/${id}`).catch(() => ({ data: [] })),
      ]);

      if (resHist.data) {
        setDatosHistorial(resHist.data);
      } else {
        // Estructura mínima si no hay historial previo
        setDatosHistorial({
          estudiante: est,
          historial: [],
        });
      }

      setMatriculasEstudiante(Array.isArray(resMat.data) ? resMat.data : []);
    } catch (err) {
      toast.error('No se pudo cargar el historial completo del estudiante');
    } finally {
      setCargandoHistorial(false);
    }
  };

  // Cargar Nómina por Año Lectivo
  const cargarNomina = useCallback(async () => {
    if (!anoSel) return;
    setCargandoNomina(true);
    try {
      const params = {};
      if (gradoSel) params.id_grado = gradoSel;
      if (paraleloSel) params.id_paralelo = paraleloSel;
      if (estadoSel) params.estado = estadoSel;
      if (busquedaNomina.trim()) params.q = busquedaNomina.trim();

      const res = await api.get(`/historial/ano-lectivo/${anoSel}/nomina`, { params });
      setNomina(Array.isArray(res.data) ? res.data : []);
      setPaginaNomina(1);
    } catch (e) {
      console.error('Error al cargar nómina de historial:', e);
      setNomina([]);
    } finally {
      setCargandoNomina(false);
    }
  }, [anoSel, gradoSel, paraleloSel, estadoSel, busquedaNomina]);

  useEffect(() => {
    if (seccion === 'nomina') {
      cargarNomina();
    }
  }, [seccion, cargarNomina]);

  // Cálculos estadísticos del historial del estudiante seleccionado
  const metricasEstudiante = useMemo(() => {
    if (!datosHistorial || !datosHistorial.historial) {
      return { totalAnos: 0, promedioGlobal: 0, promovidos: 0, tasaPromocion: 0 };
    }
    const list = datosHistorial.historial;
    const totalAnos = list.length;
    const conNota = list.filter(h => h.promedio_anual != null && !isNaN(Number(h.promedio_anual)));
    const sumaNotas = conNota.reduce((acc, h) => acc + Number(h.promedio_anual), 0);
    const promedioGlobal = conNota.length > 0 ? (sumaNotas / conNota.length) : 0;
    const promovidos = list.filter(h => (h.resultado || '').toUpperCase() === 'PROMOVIDO').length;
    const tasaPromocion = totalAnos > 0 ? Math.round((promovidos / totalAnos) * 100) : 0;

    return {
      totalAnos,
      promedioGlobal: promedioGlobal ? promedioGlobal.toFixed(2) : '—',
      promovidos,
      tasaPromocion,
    };
  }, [datosHistorial]);

  // Abrir Ficha Estudiante Modal
  const abrirFichaEstudiante = async (idEstudiante) => {
    try {
      const res = await api.get(`/estudiantes/${idEstudiante}`);
      setEstudianteFicha(res.data || estudianteSel);
      setModalFicha(true);
    } catch {
      setEstudianteFicha(estudianteSel);
      setModalFicha(true);
    }
  };

  // Descargar o previsualizar PDF oficial
  const abrirReportePdf = async (urlEndpoint, titulo) => {
    setCargandoPdf(true);
    try {
      const res = await api.get(urlEndpoint, { responseType: 'blob' });
      const blob = new Blob([res.data], { type: 'application/pdf' });
      const blobUrl = URL.createObjectURL(blob);
      setModalPdf({ titulo, blobUrl });
    } catch (e) {
      toast.error('No se pudo generar el documento PDF.');
    } finally {
      setCargandoPdf(false);
    }
  };

  // Exportar nómina a CSV para Excel
  const exportarNominaCSV = () => {
    if (nomina.length === 0) {
      toast.warning('No hay datos disponibles para exportar');
      return;
    }
    const headers = ['Estudiante', 'Cédula', 'Código', 'Grado', 'Paralelo', 'Promedio Anual', 'Resultado', 'Observaciones', 'Registrado Por', 'Fecha Registro'];
    const rows = nomina.map(n => [
      `"${(n.estudiante || '').replace(/"/g, '""')}"`,
      `"${(n.cedula || '').replace(/"/g, '""')}"`,
      `"${(n.codigo_estudiante || '').replace(/"/g, '""')}"`,
      `"${(n.grado || '').replace(/"/g, '""')}"`,
      `"${(n.paralelo || '').replace(/"/g, '""')}"`,
      n.promedio_anual != null ? Number(n.promedio_anual).toFixed(2) : '',
      `"${n.resultado || 'PENDIENTE'}"`,
      `"${(n.observaciones || '').replace(/"/g, '""')}"`,
      `"${(n.registrado_por || '').replace(/"/g, '""')}"`,
      `"${n.fecha_registro ? n.fecha_registro.slice(0, 10) : ''}"`
    ]);

    const csvContent = '\uFEFF' + [headers.join(','), ...rows.map(r => r.join(','))].join('\r\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.setAttribute('href', url);
    const anoNombre = anos.find(a => String(a.idAnoLectivo) === String(anoSel))?.nombre || 'ano';
    link.setAttribute('download', `Historial_Academico_${anoNombre.replace(/\s+/g, '_')}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    toast.success('Nómina exportada correctamente en CSV');
  };

  // Paginación de Nómina
  const nominaFiltrada = nomina;
  const totalPaginasNomina = Math.max(1, Math.ceil(nominaFiltrada.length / limitePorPagina));
  const nominaPaginada = nominaFiltrada.slice((paginaNomina - 1) * limitePorPagina, paginaNomina * limitePorPagina);

  const menuItems = [
    {
      id: 'estudiante',
      label: t('historial.tab_estudiante'),
      icon: (
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
        </svg>
      ),
    },
    {
      id: 'nomina',
      label: t('historial.tab_ano'),
      icon: (
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
        </svg>
      ),
    },
    {
      id: 'promocion',
      label: 'Asentar Promociones',
      path: '/promocion',
      icon: (
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      ),
    },
  ];

  return (
    <Layout
      breadcrumb={[t('nav.home'), t('historial.title')]}
      sidebarTitle="Historial"
      menuItems={menuItems}
      seccion={seccion}
      onSeccionChange={setSeccion}
    >
      {/* ENCABEZADO DE PÁGINA */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <div>
          <h1 className="text-xl font-bold text-slate-700 dark:text-white">
            {t('historial.title')}
          </h1>
          <p className="text-xs text-slate-400 dark:text-slate-400 mt-0.5">
            {t('historial.subtitle')} — Escuela Provincias Unidas
          </p>
        </div>

        {seccion === 'estudiante' && estudianteSel && (
          <div className="flex items-center gap-2">
            <button
              onClick={() => window.print()}
              className="flex items-center gap-1.5 px-3 py-2 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 text-xs font-semibold rounded-xl shadow-xs transition"
              title="Imprimir Sábana de Historial"
            >
              <svg className="w-4 h-4 text-slate-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 17h2a2 2 0 002-2v-4a2 2 0 00-2-2H5a2 2 0 00-2 2v4a2 2 0 002 2h2m2 4h6a2 2 0 002-2v-4a2 2 0 00-2-2H9a2 2 0 00-2 2v4a2 2 0 002 2zm8-12V5a2 2 0 00-2-2H9a2 2 0 00-2 2v4h10z" />
              </svg>
              <span>{t('historial.btn_print')}</span>
            </button>
            <button
              onClick={() => abrirReportePdf(`/reportes/ficha-estudiante/${estudianteSel.id_estudiante || estudianteSel.idEstudiante}`, `Ficha_${estudianteSel.apellidos}`)}
              style={{ backgroundColor: PRIMARY }}
              className="flex items-center gap-1.5 px-3.5 py-2 text-white text-xs font-semibold rounded-xl shadow-xs hover:opacity-90 transition"
            >
              <svg className="w-4 h-4 opacity-90" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H8a2 2 0 01-2-2V5a2 2 0 012-2h6l6 6v11a2 2 0 01-2 2z" />
              </svg>
              <span>{t('historial.btn_pdf')}</span>
            </button>
          </div>
        )}

        {seccion === 'nomina' && (
          <button
            onClick={exportarNominaCSV}
            className="flex items-center gap-2 px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold rounded-xl shadow-xs transition"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
            </svg>
            <span>{t('historial.export_csv')}</span>
          </button>
        )}
      </div>

      {/* ── SECCIÓN 1: HISTORIAL POR ESTUDIANTE ── */}
      {seccion === 'estudiante' && (
        <div className="grid lg:grid-cols-[22rem_1fr] gap-5 items-start">
          {/* PANEL IZQUIERDO: BÚSQUEDA Y SELECCIÓN DE ALUMNO */}
          <div className="bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-2xl p-4 shadow-xs">
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                Buscar Estudiante
              </h3>
              {estudianteSel && (
                <button
                  onClick={() => { setEstudianteSel(null); setDatosHistorial(null); setSearch(''); }}
                  className="text-[11px] font-semibold text-blue-600 dark:text-blue-400 hover:underline"
                >
                  Limpiar selección
                </button>
              )}
            </div>

            {/* Input de búsqueda rápida */}
            <div className="relative mb-3">
              <svg className="w-4 h-4 text-slate-400 absolute left-3 top-2.5 pointer-events-none" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
              <input
                type="text"
                value={search}
                onChange={e => setSearch(e.target.value)}
                placeholder={t('historial.search_placeholder')}
                className="w-full pl-9 pr-4 py-2 text-xs bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 text-slate-800 dark:text-slate-100 placeholder-slate-400"
              />
            </div>

            {buscando ? (
              <div className="py-8 text-center text-xs text-slate-400 flex items-center justify-center gap-2">
                <svg className="w-4 h-4 animate-spin text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
                <span>Buscando registros...</span>
              </div>
            ) : resultados.length > 0 ? (
              <div className="space-y-1.5 max-h-[30rem] overflow-y-auto pr-1">
                <p className="text-[11px] text-slate-400 px-1 font-medium mb-1">
                  Resultados encontrados ({resultados.length})
                </p>
                {resultados.map(est => {
                  const id = est.id_estudiante || est.idEstudiante;
                  const isSelected = estudianteSel && (estudianteSel.id_estudiante === id || estudianteSel.idEstudiante === id);
                  return (
                    <button
                      key={id}
                      onClick={() => seleccionarEstudiante(est)}
                      className={`w-full text-left p-2.5 rounded-xl border transition flex items-center gap-3 cursor-pointer ${
                        isSelected
                          ? 'bg-blue-50 dark:bg-blue-900/30 border-blue-200 dark:border-blue-700 shadow-xs'
                          : 'bg-white dark:bg-slate-800 border-slate-100 dark:border-slate-700/60 hover:bg-slate-50 dark:hover:bg-slate-700/50'
                      }`}
                    >
                      <div
                        className="w-9 h-9 rounded-full flex items-center justify-center text-white text-xs font-bold flex-shrink-0"
                        style={{ backgroundColor: PRIMARY }}
                      >
                        {est.nombres?.[0]}{est.apellidos?.[0]}
                      </div>
                      <div className="min-w-0 flex-1">
                        <p className="text-xs font-bold text-slate-800 dark:text-slate-100 truncate">
                          {est.apellidos}, {est.nombres}
                        </p>
                        <p className="text-[10px] text-slate-400 font-mono">
                          {est.cedula ? `CI: ${est.cedula}` : 'Sin cédula'} · {est.codigo_estudiante || est.codigoEstudiante || 'S/C'}
                        </p>
                      </div>
                    </button>
                  );
                })}
              </div>
            ) : search.trim() ? (
              <div className="py-8 text-center text-xs text-slate-400 dark:text-slate-500">
                No se encontraron estudiantes con "{search}".
              </div>
            ) : (
              <div className="py-8 text-center text-xs text-slate-400 dark:text-slate-500">
                Escribe un nombre, apellido, cédula o código para consultar la trayectoria académica.
              </div>
            )}
          </div>

          {/* PANEL DERECHO: DETALLE DEL HISTORIAL */}
          <div>
            {!estudianteSel ? (
              <div className="bg-white dark:bg-slate-800 border border-dashed border-slate-300 dark:border-slate-700 rounded-2xl p-16 text-center shadow-xs">
                <div
                  className="w-16 h-16 rounded-2xl mx-auto mb-3 flex items-center justify-center"
                  style={{ backgroundColor: '#eef0f7', color: PRIMARY }}
                >
                  <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
                  </svg>
                </div>
                <h3 className="font-bold text-slate-700 dark:text-slate-200 text-sm">
                  {t('historial.select_student')}
                </h3>
                <p className="text-xs text-slate-400 dark:text-slate-400 mt-1 max-w-sm mx-auto">
                  Utiliza el buscador lateral para localizar la hoja de vida académica, notas anuales y certificados de cualquier alumno matriculado.
                </p>
              </div>
            ) : cargandoHistorial ? (
              <div className="bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-2xl p-16 text-center text-slate-400 text-sm shadow-xs flex flex-col items-center justify-center gap-3">
                <svg className="w-8 h-8 animate-spin text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
                <span>Cargando trayectoria escolar del estudiante...</span>
              </div>
            ) : (
              <div className="space-y-5">
                {/* FICHA RESUMEN DEL ALUMNO */}
                <div className="bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-2xl p-5 shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-4">
                  <div className="flex items-center gap-4">
                    <div
                      className="w-14 h-14 rounded-2xl flex items-center justify-center text-white text-xl font-bold flex-shrink-0 shadow-sm"
                      style={{ backgroundColor: PRIMARY }}
                    >
                      {estudianteSel.nombres?.[0]}{estudianteSel.apellidos?.[0]}
                    </div>
                    <div>
                      <h2 className="text-base font-bold text-slate-800 dark:text-white">
                        {estudianteSel.apellidos}, {estudianteSel.nombres}
                      </h2>
                      <div className="flex flex-wrap items-center gap-2 mt-1 text-xs text-slate-500 dark:text-slate-400">
                        <span className="font-mono bg-slate-100 dark:bg-slate-700/60 px-2 py-0.5 rounded text-slate-700 dark:text-slate-300">
                          CI: {estudianteSel.cedula || '—'}
                        </span>
                        <span className="font-mono bg-slate-100 dark:bg-slate-700/60 px-2 py-0.5 rounded text-slate-700 dark:text-slate-300">
                          Cód: {estudianteSel.codigo_estudiante || estudianteSel.codigoEstudiante || '—'}
                        </span>
                        {estudianteSel.representante && (
                          <span className="truncate max-w-[200px]" title={estudianteSel.representante}>
                            Rep: {estudianteSel.representante}
                          </span>
                        )}
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 flex-shrink-0">
                    <button
                      onClick={() => abrirFichaEstudiante(estudianteSel.id_estudiante || estudianteSel.idEstudiante)}
                      className="px-3 py-1.5 bg-blue-50 dark:bg-blue-900/30 text-[#243A76] dark:text-blue-300 hover:bg-blue-100 dark:hover:bg-blue-900/50 text-xs font-semibold rounded-xl transition"
                    >
                      {t('historial.btn_ficha')}
                    </button>
                    <div className="flex bg-slate-100 dark:bg-slate-700/50 p-1 rounded-xl gap-1 text-xs">
                      <button
                        onClick={() => setVistaHistorial('timeline')}
                        className={`px-2.5 py-1 rounded-lg font-medium transition ${
                          vistaHistorial === 'timeline'
                            ? 'bg-white dark:bg-slate-800 text-slate-800 dark:text-slate-100 shadow-xs'
                            : 'text-slate-500 dark:text-slate-400 hover:text-slate-800'
                        }`}
                        title={t('historial.view_timeline')}
                      >
                        Línea de tiempo
                      </button>
                      <button
                        onClick={() => setVistaHistorial('tabla')}
                        className={`px-2.5 py-1 rounded-lg font-medium transition ${
                          vistaHistorial === 'tabla'
                            ? 'bg-white dark:bg-slate-800 text-slate-800 dark:text-slate-100 shadow-xs'
                            : 'text-slate-500 dark:text-slate-400 hover:text-slate-800'
                        }`}
                        title={t('historial.view_table')}
                      >
                        Tabla
                      </button>
                    </div>
                  </div>
                </div>

                {/* TARJETAS MÉTRICAS GLOBALES */}
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                  <div className="bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl p-4 shadow-xs">
                    <span className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                      {t('historial.years_enrolled')}
                    </span>
                    <p className="text-2xl font-black text-slate-800 dark:text-white mt-1">
                      {metricasEstudiante.totalAnos}
                    </p>
                    <span className="text-[11px] text-slate-400">períodos registrados</span>
                  </div>

                  <div className="bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl p-4 shadow-xs">
                    <span className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                      {t('historial.overall_avg')}
                    </span>
                    <p className="text-2xl font-black text-[#243A76] dark:text-blue-400 mt-1">
                      {metricasEstudiante.promedioGlobal}
                      {metricasEstudiante.promedioGlobal !== '—' && <span className="text-xs font-normal text-slate-400"> /10</span>}
                    </p>
                    <span className="text-[11px] text-slate-400">promedio acumulado</span>
                  </div>

                  <div className="bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl p-4 shadow-xs">
                    <span className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                      {t('historial.promoted_years')}
                    </span>
                    <p className="text-2xl font-black text-emerald-600 dark:text-emerald-400 mt-1">
                      {metricasEstudiante.promovidos}
                    </p>
                    <span className="text-[11px] text-emerald-600 dark:text-emerald-400 font-semibold">
                      {metricasEstudiante.tasaPromocion}% efectividad
                    </span>
                  </div>

                  <div className="bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl p-4 shadow-xs">
                    <span className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                      {t('historial.academic_status')}
                    </span>
                    <div className="mt-1">
                      <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-bold bg-blue-100 dark:bg-blue-900/40 text-blue-800 dark:text-blue-300">
                        <span className="w-2 h-2 rounded-full bg-blue-500" />
                        Regular / Activo
                      </span>
                    </div>
                    <span className="text-[11px] text-slate-400">estado curricular</span>
                  </div>
                </div>

                {/* LISTADO DE AÑOS O TIMELINE */}
                {(!datosHistorial.historial || datosHistorial.historial.length === 0) ? (
                  <div className="bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-2xl p-12 text-center text-slate-400 dark:text-slate-500 text-xs shadow-xs">
                    {t('historial.no_history')}
                  </div>
                ) : vistaHistorial === 'timeline' ? (
                  /* VISTA TIMELINE / CRONOLOGÍA */
                  <div className="relative pl-6 space-y-4 before:content-[''] before:absolute before:left-2.5 before:top-3 before:bottom-3 before:w-0.5 before:bg-slate-200 dark:before:bg-slate-700">
                    {datosHistorial.historial.map((h, idx) => {
                      const badge = getBadge(h.resultado);
                      const escala = getEscalaEcuatoriana(h.promedio_anual);
                      // Buscar matrícula del mismo año lectivo si existe
                      const matAsociada = matriculasEstudiante.find(m => String(m.id_ano_lectivo) === String(h.id_ano_lectivo));

                      return (
                        <div key={h.id_historial || idx} className="relative group">
                          {/* Punto indicador del timeline */}
                          <div className={`absolute -left-6 top-4 w-3.5 h-3.5 rounded-full border-2 border-white dark:border-slate-900 ${badge.dot} shadow-xs z-10`} />

                          <div className="bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-2xl p-5 shadow-xs hover:border-slate-300 dark:hover:border-slate-600 transition">
                            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-slate-100 dark:border-slate-700">
                              <div>
                                <div className="flex items-center gap-2">
                                  <span className="text-xs font-bold px-2.5 py-0.5 rounded-lg bg-slate-100 dark:bg-slate-700 text-slate-700 dark:text-slate-200 font-mono">
                                    {h.ano_lectivo || 'Año Lectivo'}
                                  </span>
                                  <h3 className="text-sm font-bold text-slate-800 dark:text-white">
                                    {h.grado || 'Grado no especificado'}
                                  </h3>
                                </div>
                                <p className={`text-[11px] font-medium mt-1 ${escala.color}`}>
                                  {escala.escala}
                                </p>
                              </div>

                              <div className="flex items-center gap-3">
                                <div className="text-right">
                                  <span className="block text-[10px] uppercase font-bold text-slate-400">Promedio Anual</span>
                                  <span className="text-base font-black text-slate-800 dark:text-white">
                                    {h.promedio_anual != null ? Number(h.promedio_anual).toFixed(2) : '—'}
                                  </span>
                                </div>
                                <span className={`px-3 py-1 rounded-full text-xs font-bold border ${badge.bg} ${badge.text} ${badge.border}`}>
                                  {badge.label}
                                </span>
                              </div>
                            </div>

                            {/* Observaciones y metadatos */}
                            <div className="pt-3 text-xs flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                              <p className="text-slate-600 dark:text-slate-300 italic">
                                {h.observaciones ? `“${h.observaciones}”` : 'Sin observaciones registradas'}
                              </p>

                              <div className="flex items-center gap-2 flex-shrink-0">
                                {matAsociada?.id_matricula && (
                                  <>
                                    <button
                                      onClick={() => abrirReportePdf(`/reportes/certificado-matricula/${matAsociada.id_matricula}`, `Certificado_${matAsociada.id_matricula}`)}
                                      className="px-2.5 py-1 text-[11px] font-semibold bg-slate-100 dark:bg-slate-700 hover:bg-slate-200 dark:hover:bg-slate-600 text-slate-700 dark:text-slate-200 rounded-lg transition"
                                      title={t('historial.certificate')}
                                    >
                                      Certificado Matrícula
                                    </button>
                                    <button
                                      onClick={() => abrirReportePdf(`/reportes/libreta/${matAsociada.id_matricula}`, `Libreta_${matAsociada.id_matricula}`)}
                                      className="px-2.5 py-1 text-[11px] font-semibold bg-blue-50 dark:bg-blue-900/40 hover:bg-blue-100 dark:hover:bg-blue-900/60 text-[#243A76] dark:text-blue-300 rounded-lg transition"
                                      title={t('historial.report_card')}
                                    >
                                      Libreta PDF
                                    </button>
                                  </>
                                )}
                              </div>
                            </div>

                            <div className="mt-2 pt-2 border-t border-slate-50 dark:border-slate-700/50 flex items-center justify-between text-[10px] text-slate-400">
                              <span>Asentado por: <strong className="text-slate-600 dark:text-slate-300">{h.registrado_por || 'Sistema'}</strong></span>
                              <span>Fecha: {h.fecha_registro ? new Date(h.fecha_registro).toLocaleDateString('es-EC') : '—'}</span>
                            </div>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                ) : (
                  /* VISTA TABLA DETALLADA */
                  <div className="bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-2xl overflow-hidden shadow-xs">
                    <table className="w-full text-sm">
                      <thead>
                        <tr className="border-b border-slate-100 dark:border-slate-700" style={{ backgroundColor: '#f8f9fc' }}>
                          <th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">{t('historial.year')}</th>
                          <th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">{t('historial.grade')}</th>
                          <th className="px-4 py-3 text-center text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">{t('historial.avg')}</th>
                          <th className="px-4 py-3 text-center text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">{t('historial.result')}</th>
                          <th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">{t('historial.observations')}</th>
                          <th className="px-4 py-3 text-center text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Documentos</th>
                        </tr>
                      </thead>
                      <tbody>
                        {datosHistorial.historial.map((h, i) => {
                          const badge = getBadge(h.resultado);
                          const matAsociada = matriculasEstudiante.find(m => String(m.id_ano_lectivo) === String(h.id_ano_lectivo));

                          return (
                            <tr key={h.id_historial || i} className="border-b border-slate-50 dark:border-slate-700/50 hover:bg-slate-50 dark:hover:bg-slate-700/30 transition">
                              <td className="px-4 py-3 font-semibold text-slate-700 dark:text-slate-200 font-mono text-xs">{h.ano_lectivo || '—'}</td>
                              <td className="px-4 py-3 text-slate-600 dark:text-slate-300 font-medium">{h.grado || '—'}</td>
                              <td className="px-4 py-3 text-center font-mono font-bold text-slate-800 dark:text-white">
                                {h.promedio_anual != null ? Number(h.promedio_anual).toFixed(2) : '—'}
                              </td>
                              <td className="px-4 py-3 text-center">
                                <span className={`inline-flex px-2.5 py-0.5 rounded-full text-xs font-bold border ${badge.bg} ${badge.text} ${badge.border}`}>
                                  {badge.label}
                                </span>
                              </td>
                              <td className="px-4 py-3 text-slate-500 dark:text-slate-400 text-xs max-w-xs truncate" title={h.observaciones}>
                                {h.observaciones || '—'}
                              </td>
                              <td className="px-4 py-3 text-center">
                                {matAsociada?.id_matricula ? (
                                  <div className="flex items-center justify-center gap-1">
                                    <button
                                      onClick={() => abrirReportePdf(`/reportes/libreta/${matAsociada.id_matricula}`, `Libreta_${matAsociada.id_matricula}`)}
                                      className="p-1.5 rounded-lg text-slate-500 hover:text-blue-600 dark:hover:text-blue-400 transition"
                                      title="Ver Libreta PDF"
                                    >
                                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                                      </svg>
                                    </button>
                                    <button
                                      onClick={() => abrirReportePdf(`/reportes/certificado-matricula/${matAsociada.id_matricula}`, `Certificado_${matAsociada.id_matricula}`)}
                                      className="p-1.5 rounded-lg text-slate-500 hover:text-emerald-600 dark:hover:text-emerald-400 transition"
                                      title="Ver Certificado de Matrícula PDF"
                                    >
                                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H8a2 2 0 01-2-2V5a2 2 0 012-2h6l6 6v11a2 2 0 01-2 2z" />
                                      </svg>
                                    </button>
                                  </div>
                                ) : (
                                  <span className="text-xs text-slate-400">—</span>
                                )}
                              </td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      )}

      {/* ── SECCIÓN 2: CONSULTA POR AÑO LECTIVO Y GRADO ── */}
      {seccion === 'nomina' && (
        <div className="space-y-4">
          {/* BARRA DE FILTROS SUPERIOR */}
          <div className="bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-2xl p-4 shadow-xs">
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
              {/* Año lectivo */}
              <div>
                <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">
                  {t('historial.year')}
                </label>
                <select
                  value={anoSel}
                  onChange={e => setAnoSel(e.target.value)}
                  className="w-full px-3 py-2 text-xs bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-xl focus:outline-none text-slate-800 dark:text-slate-100"
                >
                  {anos.map(a => (
                    <option key={a.idAnoLectivo} value={a.idAnoLectivo}>
                      {a.nombre} {a.esActual ? '(Actual)' : ''}
                    </option>
                  ))}
                </select>
              </div>

              {/* Grado */}
              <div>
                <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">
                  Grado
                </label>
                <select
                  value={gradoSel}
                  onChange={e => setGradoSel(e.target.value)}
                  className="w-full px-3 py-2 text-xs bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-xl focus:outline-none text-slate-800 dark:text-slate-100"
                >
                  <option value="">Todos los grados</option>
                  {grados.map(g => (
                    <option key={g.idGrado} value={g.idGrado}>
                      {g.nombre}
                    </option>
                  ))}
                </select>
              </div>

              {/* Paralelo */}
              <div>
                <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">
                  Paralelo
                </label>
                <select
                  value={paraleloSel}
                  onChange={e => setParaleloSel(e.target.value)}
                  disabled={!gradoSel}
                  className="w-full px-3 py-2 text-xs bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-xl focus:outline-none text-slate-800 dark:text-slate-100 disabled:opacity-50"
                >
                  <option value="">Todos los paralelos</option>
                  {paralelos.map(p => (
                    <option key={p.id_paralelo} value={p.id_paralelo}>
                      Paralelo {p.letra}
                    </option>
                  ))}
                </select>
              </div>

              {/* Estado de Promoción */}
              <div>
                <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">
                  Estado
                </label>
                <select
                  value={estadoSel}
                  onChange={e => setEstadoSel(e.target.value)}
                  className="w-full px-3 py-2 text-xs bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-xl focus:outline-none text-slate-800 dark:text-slate-100"
                >
                  <option value="">Todos los estados</option>
                  <option value="PROMOVIDO">Promovido</option>
                  <option value="NO_PROMOVIDO">No Promovido</option>
                  <option value="RETIRADO">Retirado</option>
                  <option value="PENDIENTE">Pendiente / Sin asentar</option>
                </select>
              </div>

              {/* Búsqueda en nómina */}
              <div>
                <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">
                  Buscar
                </label>
                <input
                  type="text"
                  value={busquedaNomina}
                  onChange={e => setBusquedaNomina(e.target.value)}
                  placeholder="Estudiante o cédula..."
                  className="w-full px-3 py-2 text-xs bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-xl focus:outline-none text-slate-800 dark:text-slate-100 placeholder-slate-400"
                />
              </div>
            </div>
          </div>

          {/* TABLA DE NÓMINA GENERAL */}
          <div className="bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-2xl overflow-hidden shadow-xs">
            {cargandoNomina ? (
              <div className="p-12 text-center text-slate-400 text-sm">Cargando nómina de historial...</div>
            ) : nominaFiltrada.length === 0 ? (
              <div className="p-12 text-center text-slate-400 text-xs">
                No se encontraron registros de promoción con los filtros especificados.
              </div>
            ) : (
              <>
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-slate-100 dark:border-slate-700" style={{ backgroundColor: '#f8f9fc' }}>
                      <th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Estudiante</th>
                      <th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Cédula</th>
                      <th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Grado / Paralelo</th>
                      <th className="px-4 py-3 text-center text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Promedio</th>
                      <th className="px-4 py-3 text-center text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Resultado</th>
                      <th className="px-4 py-3 text-center text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Acciones</th>
                    </tr>
                  </thead>
                  <tbody>
                    {nominaPaginada.map((n, i) => {
                      const badge = getBadge(n.resultado);
                      return (
                        <tr key={n.id_matricula || i} className="border-b border-slate-50 dark:border-slate-700/50 hover:bg-slate-50 dark:hover:bg-slate-700/30 transition">
                          <td className="px-4 py-3 font-semibold text-slate-800 dark:text-slate-100">
                            {n.estudiante}
                          </td>
                          <td className="px-4 py-3 font-mono text-xs text-slate-600 dark:text-slate-400">
                            {n.cedula || '—'}
                          </td>
                          <td className="px-4 py-3 text-xs text-slate-600 dark:text-slate-300">
                            {n.grado} "{n.paralelo}"
                          </td>
                          <td className="px-4 py-3 text-center font-mono font-bold text-slate-800 dark:text-white">
                            {n.promedio_anual != null ? Number(n.promedio_anual).toFixed(2) : '—'}
                          </td>
                          <td className="px-4 py-3 text-center">
                            <span className={`inline-flex px-2.5 py-0.5 rounded-full text-xs font-bold border ${badge.bg} ${badge.text} ${badge.border}`}>
                              {badge.label}
                            </span>
                          </td>
                          <td className="px-4 py-3 text-center">
                            <button
                              onClick={() => {
                                setSeccion('estudiante');
                                seleccionarEstudiante({
                                  id_estudiante: n.id_estudiante,
                                  nombres: n.estudiante?.split(' ')[0] || n.estudiante,
                                  apellidos: n.estudiante?.split(' ').slice(1).join(' ') || '',
                                  cedula: n.cedula,
                                  codigo_estudiante: n.codigo_estudiante,
                                });
                              }}
                              className="px-2.5 py-1 text-xs font-semibold text-blue-600 dark:text-blue-400 hover:underline"
                            >
                              Ver Trayectoria →
                            </button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>

                {/* PAGINACIÓN DE NÓMINA */}
                {totalPaginasNomina > 1 && (
                  <div className="p-4 border-t border-slate-100 dark:border-slate-700 flex items-center justify-between text-xs text-slate-500 dark:text-slate-400">
                    <p>
                      Mostrando {(paginaNomina - 1) * limitePorPagina + 1}–{Math.min(paginaNomina * limitePorPagina, nominaFiltrada.length)} de {nominaFiltrada.length}
                    </p>
                    <div className="flex items-center gap-1">
                      <button
                        onClick={() => setPaginaNomina(p => Math.max(1, p - 1))}
                        disabled={paginaNomina === 1}
                        className="px-3 py-1.5 border border-slate-200 dark:border-slate-700 rounded-lg hover:bg-slate-50 dark:hover:bg-slate-700 disabled:opacity-40 transition"
                      >
                        Anterior
                      </button>
                      <span className="px-2 font-semibold text-slate-700 dark:text-slate-200">
                        {paginaNomina} / {totalPaginasNomina}
                      </span>
                      <button
                        onClick={() => setPaginaNomina(p => Math.min(totalPaginasNomina, p + 1))}
                        disabled={paginaNomina === totalPaginasNomina}
                        className="px-3 py-1.5 border border-slate-200 dark:border-slate-700 rounded-lg hover:bg-slate-50 dark:hover:bg-slate-700 disabled:opacity-40 transition"
                      >
                        Siguiente
                      </button>
                    </div>
                  </div>
                )}
              </>
            )}
          </div>
        </div>
      )}

      {/* ── MODAL VISOR PDF OFICIAL ── */}
      {modalPdf && (
        <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4 backdrop-blur-xs" onClick={() => setModalPdf(null)}>
          <div className="bg-white dark:bg-slate-900 rounded-2xl shadow-2xl w-full max-w-4xl h-[90vh] overflow-hidden flex flex-col" onClick={e => e.stopPropagation()}>
            <div style={{ backgroundColor: PRIMARY }} className="px-6 py-3.5 text-white flex items-center justify-between flex-shrink-0">
              <div className="flex items-center gap-2">
                <svg className="w-5 h-5 opacity-90" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                <h3 className="font-bold text-sm">{modalPdf.titulo}</h3>
              </div>
              <div className="flex items-center gap-3">
                <a
                  href={modalPdf.blobUrl}
                  download={`${modalPdf.titulo}.pdf`}
                  className="px-3 py-1.5 bg-white/20 hover:bg-white/30 text-white rounded-lg text-xs font-semibold transition"
                >
                  Descargar
                </a>
                <button onClick={() => setModalPdf(null)} className="text-white/80 hover:text-white text-lg">✕</button>
              </div>
            </div>
            <div className="flex-1 bg-slate-100 dark:bg-slate-800 p-2">
              <iframe src={modalPdf.blobUrl} title="Documento PDF" className="w-full h-full rounded-xl bg-white shadow-inner border border-slate-200 dark:border-slate-700" />
            </div>
          </div>
        </div>
      )}

      {/* ── MODAL FICHA COMPLETA DEL ESTUDIANTE ── */}
      {modalFicha && estudianteFicha && (
        <FichaEstudianteModal
          selected={estudianteFicha}
          principalOrigin={PRINCIPAL_ORIGIN}
          primary={PRIMARY}
          onClose={() => setModalFicha(false)}
        />
      )}
    </Layout>
  );
}
