import { useState, useEffect, useMemo } from 'react';
import Layout from '../components/Layout';
import api, { apiPrincipal } from '../utils/api';
import { useToast, useConfirm } from '../components/Toast';

const PRIMARY = '#243A76';
const PRIMARY_LIGHT = '#2d4a96';

const menuItems = [
  { id: 'nomina', label: 'Nómina y Calificación', icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" /></svg> },
  { id: 'resumen', label: 'Resumen y Estadísticas', icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" /></svg> },
  { id: 'historial', label: 'Historial por Estudiante', icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" /></svg> },
];

export default function Promocion() {
  const [seccion, setSeccion] = useState('nomina');
  const [anos, setAnos] = useState([]);
  const [anoSel, setAnoSel] = useState('');
  const [grados, setGrados] = useState([]);
  const [paralelos, setParalelos] = useState([]);
  const [gradoSel, setGradoSel] = useState('');
  const [paraleloSel, setParaleloSel] = useState('');
  const [estadoSel, setEstadoSel] = useState('');
  const [busqueda, setBusqueda] = useState('');

  // Datos
  const [resumen, setResumen] = useState([]);
  const [nomina, setNomina] = useState([]);
  const [loading, setLoading] = useState(false);

  // Paginación
  const [paginaActual, setPaginaActual] = useState(1);
  const [limitePorPagina, setLimitePorPagina] = useState(25);

  // Selección múltiple para acciones en lote
  const [seleccionados, setSeleccionados] = useState([]);

  // Modales
  const [modalIndividual, setModalIndividual] = useState(false);
  const [modalMasivo, setModalMasivo] = useState(false);
  const [estudianteModal, setEstudianteModal] = useState(null);
  const [form, setForm] = useState({ id_matricula: '', resultado: 'PROMOVIDO', promedio_anual: '', observaciones: '' });
  const [formMasivo, setFormMasivo] = useState({ resultado: 'PROMOVIDO', promedio_anual: '', observaciones: '' });
  const [saving, setSaving] = useState(false);
  const [errorModal, setErrorModal] = useState('');

  // Historial individual
  const [busquedaEstudiante, setBusquedaEstudiante] = useState('');
  const [estudiantesLista, setEstudiantesLista] = useState([]);
  const [historialDetalle, setHistorialDetalle] = useState(null);
  const [loadingHistorial, setLoadingHistorial] = useState(false);

  const toast = useToast();
  const confirm = useConfirm();

  // Reset de página al cambiar filtros
  useEffect(() => {
    setPaginaActual(1);
  }, [busqueda, gradoSel, paraleloSel, estadoSel, anoSel]);

  // Exportar a Excel (CSV)
  const exportarCSV = () => {
    if (nomina.length === 0) {
      toast.warning('No hay datos para exportar');
      return;
    }
    const headers = ['Estudiante', 'Código', 'Cédula', 'Grado', 'Paralelo', 'Promedio Anual', 'Estado Promoción', 'Observaciones', 'Registrado Por', 'Fecha Registro'];
    const rows = nomina.map(n => [
      `"${(n.estudiante || '').replace(/"/g, '""')}"`,
      `"${(n.codigo_estudiante || '').replace(/"/g, '""')}"`,
      `"${(n.cedula || '').replace(/"/g, '""')}"`,
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
    const anoActual = anos.find(a => String(a.idAnoLectivo) === String(anoSel))?.nombre || 'ano';
    link.setAttribute('download', `Nomina_Promocion_${anoActual.replace(/\s+/g, '_')}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    toast.success('Archivo CSV descargado correctamente');
  };

  // Cargar catálogos iniciales
  useEffect(() => {
    apiPrincipal.get('/anos-lectivos').then(r => {
      setAnos(r.data || []);
      const actual = (r.data || []).find(a => a.esActual);
      if (actual) setAnoSel(String(actual.idAnoLectivo));
      else if (r.data?.length > 0) setAnoSel(String(r.data[0].idAnoLectivo));
    }).catch(() => {});

    apiPrincipal.get('/grados').then(r => setGrados(r.data || [])).catch(() => {});
  }, []);

  // Cargar paralelos cuando cambia el grado
  useEffect(() => {
    if (!gradoSel) {
      setParalelos([]);
      setParaleloSel('');
      return;
    }
    api.get(`/matriculas/paralelos/${gradoSel}`).then(r => setParalelos(r.data || [])).catch(() => {});
  }, [gradoSel]);

  // Cargar datos según año y filtros
  const cargarDatos = () => {
    if (!anoSel) return;
    setLoading(true);

    const params = {};
    if (gradoSel) params.id_grado = gradoSel;
    if (paraleloSel) params.id_paralelo = paraleloSel;
    if (estadoSel) params.estado = estadoSel;
    if (busqueda) params.q = busqueda;

    Promise.all([
      api.get(`/historial/ano-lectivo/${anoSel}/resumen`),
      api.get(`/historial/ano-lectivo/${anoSel}/nomina`, { params }),
    ]).then(([resResumen, resNomina]) => {
      setResumen(resResumen.data || []);
      setNomina(resNomina.data || []);
      setSeleccionados([]);
    }).catch(err => {
      console.error(err);
      toast.error('Error al cargar nómina de promoción');
    }).finally(() => setLoading(false));
  };

  useEffect(() => {
    cargarDatos();
  }, [anoSel, gradoSel, paraleloSel, estadoSel]);

  // Búsqueda con debounce
  useEffect(() => {
    const timer = setTimeout(() => {
      cargarDatos();
    }, 350);
    return () => clearTimeout(timer);
  }, [busqueda]);

  // Cargar lista de estudiantes para búsqueda de historial
  useEffect(() => {
    if (seccion === 'historial') {
      api.get('/estudiantes').then(r => setEstudiantesLista(r.data || [])).catch(() => {});
    }
  }, [seccion]);

  // Métricas calculadas
  const stats = useMemo(() => {
    const total = nomina.length;
    const promovidos = nomina.filter(n => n.resultado === 'PROMOVIDO').length;
    const noPromovidos = nomina.filter(n => n.resultado === 'NO_PROMOVIDO' || n.resultado === 'REPROBADO').length;
    const retirados = nomina.filter(n => n.resultado === 'RETIRADO').length;
    const pendientes = nomina.filter(n => !n.id_historial).length;

    const notasValidas = nomina.filter(n => n.promedio_anual != null).map(n => Number(n.promedio_anual));
    const promedioGen = notasValidas.length > 0
      ? (notasValidas.reduce((a, b) => a + b, 0) / notasValidas.length).toFixed(2)
      : '0.00';

    return { total, promovidos, noPromovidos, retirados, pendientes, promedioGen };
  }, [nomina]);

  // Paginación calculada
  const totalPaginas = useMemo(() => {
    if (limitePorPagina === 'todos' || !limitePorPagina) return 1;
    return Math.max(1, Math.ceil(nomina.length / Number(limitePorPagina)));
  }, [nomina.length, limitePorPagina]);

  const nominaPaginada = useMemo(() => {
    if (limitePorPagina === 'todos') return nomina;
    const lim = Number(limitePorPagina) || 25;
    const inicio = (paginaActual - 1) * lim;
    return nomina.slice(inicio, inicio + lim);
  }, [nomina, paginaActual, limitePorPagina]);

  // Handlers para selección múltiple
  const toggleSeleccion = (idMatricula) => {
    setSeleccionados(prev =>
      prev.includes(idMatricula) ? prev.filter(x => x !== idMatricula) : [...prev, idMatricula]
    );
  };

  const toggleSeleccionarTodos = () => {
    if (seleccionados.length === nomina.length) {
      setSeleccionados([]);
    } else {
      setSeleccionados(nomina.map(n => n.id_matricula));
    }
  };

  // Abrir modal individual
  const abrirModalIndividual = (item) => {
    setEstudianteModal(item);
    setForm({
      id_matricula: String(item.id_matricula),
      resultado: item.resultado || 'PROMOVIDO',
      promedio_anual: item.promedio_anual != null ? String(item.promedio_anual) : '',
      observaciones: item.observaciones || '',
    });
    setErrorModal('');
    setModalIndividual(true);
  };

  // Guardar individual
  const handleGuardarIndividual = async (e) => {
    e.preventDefault();
    setSaving(true);
    setErrorModal('');
    try {
      const payload = {
        id_matricula: Number(form.id_matricula),
        resultado: form.resultado,
        promedio_anual: form.promedio_anual ? Number(form.promedio_anual) : null,
        observaciones: form.observaciones ? form.observaciones.trim() : null,
      };
      await api.post('/historial', payload);
      toast.success('Resultado de promoción guardado correctamente');
      setModalIndividual(false);
      cargarDatos();
    } catch (err) {
      setErrorModal(err.response?.data?.error || err.response?.data?.message || 'Error al registrar promoción');
    } finally {
      setSaving(false);
    }
  };

  // Guardar masivo
  const handleGuardarMasivo = async (e) => {
    e.preventDefault();
    if (seleccionados.length === 0) return;
    setSaving(true);
    try {
      const dtos = seleccionados.map(idMat => ({
        id_matricula: Number(idMat),
        resultado: formMasivo.resultado,
        promedio_anual: formMasivo.promedio_anual ? Number(formMasivo.promedio_anual) : null,
        observaciones: formMasivo.observaciones ? formMasivo.observaciones.trim() : 'Promoción procesada en lote',
      }));

      await api.post('/historial/masivo', dtos);
      toast.success(`Se procesaron ${seleccionados.length} estudiantes exitosamente`);
      setModalMasivo(false);
      setSeleccionados([]);
      cargarDatos();
    } catch (err) {
      toast.error(err.response?.data?.error || 'Error al procesar promoción masiva');
    } finally {
      setSaving(false);
    }
  };

  // Eliminar / Revertir promoción
  const handleEliminarPromocion = async (item) => {
    if (!item.id_historial) return;
    const ok = await confirm({
      title: '¿Revertir promoción?',
      message: `Se eliminará el resultado académico de "${item.estudiante}". Su matrícula volverá a estado MATRICULADO.`,
      confirmText: 'Sí, revertir',
      cancelText: 'Cancelar',
      type: 'warning',
    });
    if (!ok) return;

    try {
      await api.delete(`/historial/${item.id_historial}`);
      toast.success('Promoción revertida correctamente');
      cargarDatos();
    } catch (err) {
      toast.error('Error al revertir promoción');
    }
  };

  // Consultar historial por estudiante
  const verHistorialEstudiante = async (idEstudiante) => {
    setLoadingHistorial(true);
    try {
      const res = await api.get(`/historial/estudiante/${idEstudiante}`);
      setHistorialDetalle(res.data);
    } catch (err) {
      toast.error('No se pudo cargar el historial del estudiante');
    } finally {
      setLoadingHistorial(false);
    }
  };

  const estudiantesFiltrados = useMemo(() => {
    if (!busquedaEstudiante) return estudiantesLista.slice(0, 15);
    const q = busquedaEstudiante.toLowerCase();
    return estudiantesLista.filter(e =>
      (e.nombres && e.nombres.toLowerCase().includes(q)) ||
      (e.apellidos && e.apellidos.toLowerCase().includes(q)) ||
      (e.cedula && e.cedula.includes(q)) ||
      (e.codigo_estudiante && e.codigo_estudiante.toLowerCase().includes(q))
    ).slice(0, 20);
  }, [estudiantesLista, busquedaEstudiante]);

  return (
    <Layout
      breadcrumb={['Inicio', 'Promoción']}
      sidebarTitle="Promoción"
      menuItems={menuItems}
      seccion={seccion}
      onSeccionChange={setSeccion}
    >
      {/* HEADER PRINCIPAL */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <div>
          <h1 className="text-xl font-bold text-slate-800 flex items-center gap-2">
            <span>🎓</span> Promoción y Cierre Académico
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Registro oficial de resultados (Promovido, No Promovido, Retirado) y expedientes anuales
          </p>
        </div>

        {/* SELECTOR DE AÑO LECTIVO */}
        <div className="flex items-center gap-2">
          <label className="text-xs font-semibold text-slate-600 hidden sm:inline">Período:</label>
          <select
            value={anoSel}
            onChange={e => setAnoSel(e.target.value)}
            className="px-3 py-2 bg-white border border-slate-300 rounded-xl text-xs font-bold text-[#243A76] shadow-xs focus:ring-2 focus:ring-blue-500 focus:outline-none"
          >
            {anos.map(a => (
              <option key={a.idAnoLectivo} value={a.idAnoLectivo}>
                {a.nombre} {a.esActual ? '(Actual)' : ''}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* KPI METRICS */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3 mb-6">
        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-xs flex flex-col justify-between">
          <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">Total Nómina</span>
          <div className="flex items-baseline justify-between mt-2">
            <span className="text-2xl font-extrabold text-slate-800">{stats.total}</span>
            <span className="text-xs text-slate-400 font-medium">100%</span>
          </div>
        </div>

        <div className="bg-emerald-50/70 p-4 rounded-2xl border border-emerald-200 shadow-xs flex flex-col justify-between">
          <span className="text-[11px] font-bold text-emerald-700 uppercase tracking-wider">Promovidos</span>
          <div className="flex items-baseline justify-between mt-2">
            <span className="text-2xl font-extrabold text-emerald-700">{stats.promovidos}</span>
            <span className="text-xs text-emerald-600 font-bold">
              {stats.total > 0 ? ((stats.promovidos / stats.total) * 100).toFixed(0) : 0}%
            </span>
          </div>
        </div>

        <div className="bg-rose-50/70 p-4 rounded-2xl border border-rose-200 shadow-xs flex flex-col justify-between">
          <span className="text-[11px] font-bold text-rose-700 uppercase tracking-wider">No Promovidos</span>
          <div className="flex items-baseline justify-between mt-2">
            <span className="text-2xl font-extrabold text-rose-700">{stats.noPromovidos}</span>
            <span className="text-xs text-rose-600 font-bold">
              {stats.total > 0 ? ((stats.noPromovidos / stats.total) * 100).toFixed(0) : 0}%
            </span>
          </div>
        </div>

        <div className="bg-slate-100 p-4 rounded-2xl border border-slate-200 shadow-xs flex flex-col justify-between">
          <span className="text-[11px] font-bold text-slate-600 uppercase tracking-wider">Retirados</span>
          <div className="flex items-baseline justify-between mt-2">
            <span className="text-2xl font-extrabold text-slate-700">{stats.retirados}</span>
            <span className="text-xs text-slate-500 font-bold">
              {stats.total > 0 ? ((stats.retirados / stats.total) * 100).toFixed(0) : 0}%
            </span>
          </div>
        </div>

        <div className="bg-amber-50/80 p-4 rounded-2xl border border-amber-200 shadow-xs flex flex-col justify-between">
          <span className="text-[11px] font-bold text-amber-700 uppercase tracking-wider">Pendientes</span>
          <div className="flex items-baseline justify-between mt-2">
            <span className="text-2xl font-extrabold text-amber-700">{stats.pendientes}</span>
            <span className="text-xs text-amber-600 font-bold">
              {stats.total > 0 ? ((stats.pendientes / stats.total) * 100).toFixed(0) : 0}%
            </span>
          </div>
        </div>

        <div className="bg-blue-50/70 p-4 rounded-2xl border border-blue-200 shadow-xs flex flex-col justify-between">
          <span className="text-[11px] font-bold text-blue-700 uppercase tracking-wider">Prom. General</span>
          <div className="flex items-baseline justify-between mt-2">
            <span className="text-2xl font-extrabold text-[#243A76]">{stats.promedioGen}</span>
            <span className="text-xs text-blue-600 font-bold">/ 10</span>
          </div>
        </div>
      </div>

      {/* VISTA 1: RESUMEN Y ESTADÍSTICAS POR GRADO */}
      {seccion === 'resumen' && (
        <div className="space-y-6 animate-in fade-in duration-200">
          <div className="bg-white border border-slate-200 rounded-3xl overflow-hidden shadow-xs">
            <div style={{ backgroundColor: PRIMARY }} className="px-6 py-4 flex items-center justify-between text-white">
              <div className="flex items-center gap-2">
                <span className="text-lg">📊</span>
                <h3 className="font-bold text-sm">Resumen Consolidado de Promoción por Grado</h3>
              </div>
              <span className="text-xs text-blue-100 bg-white/10 px-3 py-1 rounded-full">
                {resumen.length} Grados evaluados
              </span>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="bg-slate-50 text-slate-600 border-b border-slate-200 text-xs uppercase tracking-wider">
                    <th className="px-6 py-3.5 text-left font-bold">Grado</th>
                    <th className="px-4 py-3.5 text-center font-bold text-emerald-700">Promovidos</th>
                    <th className="px-4 py-3.5 text-center font-bold text-rose-700">No Promovidos</th>
                    <th className="px-4 py-3.5 text-center font-bold text-slate-600">Retirados</th>
                    <th className="px-4 py-3.5 text-center font-bold text-blue-700">Total Evaluados</th>
                    <th className="px-4 py-3.5 text-center font-bold text-slate-700">Promedio General</th>
                    <th className="px-6 py-3.5 text-center font-bold">Tasa Aprobación</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {resumen.map((r, i) => {
                    const totalG = Number(r.total_registrados || 0);
                    const promG = Number(r.promovidos || 0);
                    const tasa = totalG > 0 ? ((promG / totalG) * 100).toFixed(1) : '0.0';

                    return (
                      <tr key={i} className={`hover:bg-blue-50/30 transition ${i % 2 === 1 ? 'bg-slate-50/40' : ''}`}>
                        <td className="px-6 py-4 font-bold text-slate-800 flex items-center gap-2">
                          <span className="w-2 h-2 rounded-full bg-blue-600"></span>
                          {r.grado}
                        </td>
                        <td className="px-4 py-4 text-center font-extrabold text-emerald-600">{r.promovidos}</td>
                        <td className="px-4 py-4 text-center font-extrabold text-rose-600">{r.no_promovidos}</td>
                        <td className="px-4 py-4 text-center font-medium text-slate-500">{r.retirados}</td>
                        <td className="px-4 py-4 text-center font-bold text-blue-900 bg-blue-50/30">{r.total_registrados}</td>
                        <td className="px-4 py-4 text-center font-mono font-bold text-slate-700">
                          {r.promedio_general ? Number(r.promedio_general).toFixed(2) : '—'}
                        </td>
                        <td className="px-6 py-4 text-center">
                          <div className="flex items-center justify-center gap-2">
                            <div className="w-24 bg-slate-200 rounded-full h-2 overflow-hidden">
                              <div
                                className="bg-emerald-500 h-2 rounded-full transition-all duration-500"
                                style={{ width: `${Math.min(100, Number(tasa))}%` }}
                              />
                            </div>
                            <span className="text-xs font-bold text-slate-700 w-10 text-right">{tasa}%</span>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                  {resumen.length === 0 && (
                    <tr>
                      <td colSpan={7} className="px-6 py-12 text-center text-slate-400">
                        No hay registros de promoción en este período lectivo.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* VISTA 2: NÓMINA Y CALIFICACIÓN DE PROMOCIÓN */}
      {seccion === 'nomina' && (
        <div className="space-y-4 animate-in fade-in duration-200">
          {/* BARRA DE FILTROS Y ACCIONES */}
          <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-xs flex flex-wrap items-center justify-between gap-3">
            <div className="flex flex-wrap items-center gap-2 flex-1 min-w-[280px]">
              {/* Buscador */}
              <div className="relative flex-1 min-w-[200px]">
                <input
                  type="text"
                  placeholder="Buscar por estudiante, cédula o código..."
                  value={busqueda}
                  onChange={e => setBusqueda(e.target.value)}
                  className="w-full pl-9 pr-4 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none bg-slate-50"
                />
                <svg className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
              </div>

              {/* Filtro Grado */}
              <select
                value={gradoSel}
                onChange={e => { setGradoSel(e.target.value); setParaleloSel(''); }}
                className="px-3 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none bg-slate-50 font-medium text-slate-700"
              >
                <option value="">— Todos los grados —</option>
                {grados.map(g => (
                  <option key={g.idGrado} value={g.idGrado}>{g.nombre}</option>
                ))}
              </select>

              {/* Filtro Paralelo */}
              {paralelos.length > 0 && (
                <select
                  value={paraleloSel}
                  onChange={e => setParaleloSel(e.target.value)}
                  className="px-3 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none bg-slate-50 font-medium text-slate-700"
                >
                  <option value="">— Paralelo —</option>
                  {paralelos.map(p => (
                    <option key={p.id_paralelo || p.id} value={p.id_paralelo || p.id}>Paralelo {p.letra}</option>
                  ))}
                </select>
              )}

              {/* Filtro Estado */}
              <select
                value={estadoSel}
                onChange={e => setEstadoSel(e.target.value)}
                className="px-3 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none bg-slate-50 font-medium text-slate-700"
              >
                <option value="">— Todos los estados —</option>
                <option value="PENDIENTE">⏳ Pendientes de evaluación</option>
                <option value="PROMOVIDO">✅ Promovidos</option>
                <option value="NO_PROMOVIDO">❌ No Promovidos</option>
                <option value="RETIRADO">⚪ Retirados</option>
              </select>
            </div>

            {/* BOTONES DE ACCIÓN */}
            <div className="flex items-center gap-2">
              <button
                onClick={exportarCSV}
                className="flex items-center gap-1.5 px-3.5 py-2 bg-slate-800 text-white rounded-xl text-xs font-bold shadow-xs hover:bg-slate-700 transition"
                title="Descargar nómina en formato CSV para Excel"
              >
                <svg className="w-4 h-4 text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                <span>Exportar CSV</span>
              </button>

              {seleccionados.length > 0 && (
                <button
                  onClick={() => setModalMasivo(true)}
                  className="flex items-center gap-1.5 px-3 py-2 bg-emerald-600 text-white rounded-xl text-xs font-bold shadow-sm hover:bg-emerald-700 transition animate-in zoom-in-95"
                >
                  <span>⚡</span> Promoción Masiva ({seleccionados.length})
                </button>
              )}

              <button
                onClick={cargarDatos}
                className="p-2 border border-slate-200 rounded-xl text-slate-600 hover:bg-slate-100 transition"
                title="Actualizar datos"
              >
                <svg className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
              </button>
            </div>
          </div>

          {/* TABLA DE NÓMINA */}
          <div className="bg-white border border-slate-200 rounded-3xl overflow-hidden shadow-xs">
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="bg-slate-50 text-slate-600 border-b border-slate-200 text-xs uppercase tracking-wider">
                    <th className="px-4 py-3.5 text-center w-10">
                      <input
                        type="checkbox"
                        checked={nomina.length > 0 && seleccionados.length === nomina.length}
                        onChange={toggleSeleccionarTodos}
                        className="rounded border-slate-300 text-blue-600 focus:ring-blue-500 w-4 h-4 cursor-pointer"
                      />
                    </th>
                    <th className="px-4 py-3.5 text-left font-bold">Estudiante</th>
                    <th className="px-4 py-3.5 text-left font-bold">Cédula</th>
                    <th className="px-4 py-3.5 text-left font-bold">Grado y Paralelo</th>
                    <th className="px-4 py-3.5 text-center font-bold">Promedio Anual</th>
                    <th className="px-4 py-3.5 text-center font-bold">Estado Promoción</th>
                    <th className="px-4 py-3.5 text-left font-bold">Observaciones</th>
                    <th className="px-4 py-3.5 text-center font-bold w-28">Acciones</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {nominaPaginada.map((item, i) => {
                    const isSelected = seleccionados.includes(item.id_matricula);
                    const tienePromocion = Boolean(item.id_historial);

                    return (
                      <tr
                        key={item.id_matricula}
                        className={`transition hover:bg-blue-50/30 ${isSelected ? 'bg-blue-50/50' : i % 2 === 1 ? 'bg-slate-50/30' : ''}`}
                      >
                        <td className="px-4 py-3.5 text-center">
                          <input
                            type="checkbox"
                            checked={isSelected}
                            onChange={() => toggleSeleccion(item.id_matricula)}
                            className="rounded border-slate-300 text-blue-600 focus:ring-blue-500 w-4 h-4 cursor-pointer"
                          />
                        </td>
                        <td className="px-4 py-3.5 font-bold text-slate-800">
                          <div className="flex flex-col">
                            <span>{item.estudiante}</span>
                            {item.codigo_estudiante && (
                              <span className="text-[10px] text-slate-400 font-mono font-normal">Cód: {item.codigo_estudiante}</span>
                            )}
                          </div>
                        </td>
                        <td className="px-4 py-3.5 text-slate-600 font-mono text-xs">{item.cedula || '—'}</td>
                        <td className="px-4 py-3.5 text-slate-700">
                          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-slate-100 text-slate-700">
                            {item.grado || '—'} {item.paralelo ? `"${item.paralelo}"` : ''}
                          </span>
                        </td>
                        <td className="px-4 py-3.5 text-center font-mono font-bold text-slate-800">
                          {item.promedio_anual != null ? (
                            <span className={`px-2 py-0.5 rounded-md ${Number(item.promedio_anual) >= 7 ? 'bg-emerald-50 text-emerald-700' : 'bg-rose-50 text-rose-700'}`}>
                              {Number(item.promedio_anual).toFixed(2)}
                            </span>
                          ) : '—'}
                        </td>
                        <td className="px-4 py-3.5 text-center">
                          {item.resultado === 'PROMOVIDO' && (
                            <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold bg-emerald-100 text-emerald-800">
                              ✅ PROMOVIDO
                            </span>
                          )}
                          {(item.resultado === 'NO_PROMOVIDO' || item.resultado === 'REPROBADO') && (
                            <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold bg-rose-100 text-rose-800">
                              ❌ NO PROMOVIDO
                            </span>
                          )}
                          {item.resultado === 'RETIRADO' && (
                            <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold bg-slate-200 text-slate-700">
                              ⚪ RETIRADO
                            </span>
                          )}
                          {!item.resultado && (
                            <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-amber-100 text-amber-800">
                              ⏳ PENDIENTE
                            </span>
                          )}
                        </td>
                        <td className="px-4 py-3.5 text-slate-500 text-xs max-w-xs truncate">
                          {item.observaciones || <span className="text-slate-300 italic">Sin observaciones</span>}
                        </td>
                        <td className="px-4 py-3.5 text-center">
                          <div className="flex items-center justify-center gap-1">
                            <button
                              onClick={() => abrirModalIndividual(item)}
                              style={{ backgroundColor: tienePromocion ? '#e0e7ff' : PRIMARY }}
                              className={`px-2.5 py-1 rounded-lg text-xs font-bold transition flex items-center gap-1 ${tienePromocion ? 'text-indigo-800 hover:bg-indigo-200' : 'text-white hover:opacity-90'}`}
                              title={tienePromocion ? 'Editar resultado' : 'Calificar promoción'}
                            >
                              {tienePromocion ? '✏️ Editar' : '📝 Calificar'}
                            </button>

                            {tienePromocion && (
                              <button
                                onClick={() => handleEliminarPromocion(item)}
                                className="p-1 text-slate-400 hover:text-rose-600 rounded-lg hover:bg-rose-50 transition"
                                title="Revertir promoción"
                              >
                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                                </svg>
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                  {nomina.length === 0 && !loading && (
                    <tr>
                      <td colSpan={8} className="px-6 py-12 text-center text-slate-400">
                        No se encontraron estudiantes para los filtros seleccionados.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>

            {/* BARRA DE PAGINACIÓN */}
            {nomina.length > 0 && (
              <div className="px-6 py-4 bg-slate-50 border-t border-slate-200 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-slate-600">
                <div className="flex items-center gap-2">
                  <span>
                    Mostrando{' '}
                    <strong>
                      {limitePorPagina === 'todos'
                        ? 1
                        : Math.min(nomina.length, (paginaActual - 1) * Number(limitePorPagina) + 1)}
                    </strong>{' '}
                    a{' '}
                    <strong>
                      {limitePorPagina === 'todos'
                        ? nomina.length
                        : Math.min(nomina.length, paginaActual * Number(limitePorPagina))}
                    </strong>{' '}
                    de <strong>{nomina.length}</strong> estudiantes
                  </span>
                </div>

                <div className="flex items-center gap-3">
                  <div className="flex items-center gap-1.5">
                    <span className="text-slate-400">Por página:</span>
                    <select
                      value={limitePorPagina}
                      onChange={e => {
                        setLimitePorPagina(e.target.value === 'todos' ? 'todos' : Number(e.target.value));
                        setPaginaActual(1);
                      }}
                      className="px-2 py-1 border border-slate-200 rounded-lg bg-white font-medium text-slate-700 focus:outline-none"
                    >
                      <option value={25}>25</option>
                      <option value={50}>50</option>
                      <option value={100}>100</option>
                      <option value="todos">Todos ({nomina.length})</option>
                    </select>
                  </div>

                  {limitePorPagina !== 'todos' && totalPaginas > 1 && (
                    <div className="flex items-center gap-1">
                      <button
                        onClick={() => setPaginaActual(1)}
                        disabled={paginaActual === 1}
                        className="px-2.5 py-1 border border-slate-200 rounded-lg bg-white hover:bg-slate-100 disabled:opacity-40 disabled:cursor-not-allowed font-bold"
                        title="Primera página"
                      >
                        «
                      </button>
                      <button
                        onClick={() => setPaginaActual(prev => Math.max(1, prev - 1))}
                        disabled={paginaActual === 1}
                        className="px-2.5 py-1 border border-slate-200 rounded-lg bg-white hover:bg-slate-100 disabled:opacity-40 disabled:cursor-not-allowed font-medium"
                      >
                        ‹ Anterior
                      </button>
                      <span className="px-3 py-1 font-bold text-slate-800">
                        {paginaActual} / {totalPaginas}
                      </span>
                      <button
                        onClick={() => setPaginaActual(prev => Math.min(totalPaginas, prev + 1))}
                        disabled={paginaActual === totalPaginas}
                        className="px-2.5 py-1 border border-slate-200 rounded-lg bg-white hover:bg-slate-100 disabled:opacity-40 disabled:cursor-not-allowed font-medium"
                      >
                        Siguiente ›
                      </button>
                      <button
                        onClick={() => setPaginaActual(totalPaginas)}
                        disabled={paginaActual === totalPaginas}
                        className="px-2.5 py-1 border border-slate-200 rounded-lg bg-white hover:bg-slate-100 disabled:opacity-40 disabled:cursor-not-allowed font-bold"
                        title="Última página"
                      >
                        »
                      </button>
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* VISTA 3: HISTORIAL POR ESTUDIANTE */}
      {seccion === 'historial' && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 animate-in fade-in duration-200">
          {/* Panel Izquierdo: Selección de Estudiante */}
          <div className="bg-white p-4 rounded-3xl border border-slate-200 shadow-xs space-y-4">
            <div>
              <h3 className="font-bold text-sm text-slate-800">Buscar Estudiante</h3>
              <p className="text-xs text-slate-400">Selecciona un estudiante para ver su expediente histórico</p>
            </div>

            <input
              type="text"
              placeholder="Buscar por nombre o cédula..."
              value={busquedaEstudiante}
              onChange={e => setBusquedaEstudiante(e.target.value)}
              className="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none bg-slate-50"
            />

            <div className="divide-y divide-slate-100 max-h-[460px] overflow-y-auto pr-1">
              {estudiantesFiltrados.map(e => (
                <button
                  key={e.id_estudiante}
                  onClick={() => verHistorialEstudiante(e.id_estudiante)}
                  className="w-full text-left p-3 hover:bg-blue-50 rounded-xl transition flex flex-col gap-0.5 group"
                >
                  <span className="font-bold text-xs text-slate-700 group-hover:text-[#243A76] transition">
                    {e.nombres} {e.apellidos}
                  </span>
                  <div className="flex items-center gap-2 text-[11px] text-slate-400">
                    <span>CI: {e.cedula || '—'}</span>
                    {e.codigo_estudiante && <span>• Cód: {e.codigo_estudiante}</span>}
                  </div>
                </button>
              ))}
            </div>
          </div>

          {/* Panel Derecho: Trayectoria Histórica */}
          <div className="md:col-span-2 bg-white p-6 rounded-3xl border border-slate-200 shadow-xs">
            {loadingHistorial && (
              <div className="flex items-center justify-center py-20 text-slate-400">
                <svg className="w-6 h-6 animate-spin mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
                Cargando historial académico...
              </div>
            )}

            {!loadingHistorial && historialDetalle && (
              <div className="space-y-6">
                {/* Cabecera del Estudiante */}
                <div className="flex items-center justify-between pb-4 border-b border-slate-100">
                  <div className="flex items-center gap-3">
                    <div className="w-12 h-12 rounded-2xl bg-blue-100 text-[#243A76] flex items-center justify-center font-black text-lg">
                      {historialDetalle.estudiante?.nombres?.charAt(0) || 'E'}
                    </div>
                    <div>
                      <h2 className="font-bold text-base text-slate-800">
                        {historialDetalle.estudiante?.nombres} {historialDetalle.estudiante?.apellidos}
                      </h2>
                      <p className="text-xs text-slate-500 font-mono">
                        Cédula: {historialDetalle.estudiante?.cedula || '—'} | Cód: {historialDetalle.estudiante?.codigo_estudiante || '—'}
                      </p>
                    </div>
                  </div>
                </div>

                {/* Timeline del Historial */}
                <div className="space-y-4">
                  <h3 className="font-bold text-xs text-slate-400 uppercase tracking-wider">Historial de Períodos y Promociones</h3>

                  {historialDetalle.historial?.map((h, idx) => (
                    <div key={idx} className="p-4 rounded-2xl bg-slate-50 border border-slate-200 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <span className="font-bold text-sm text-slate-800">{h.ano_lectivo || 'Año lectivo'}</span>
                          <span className="text-xs px-2 py-0.5 rounded-md bg-blue-100 text-blue-800 font-semibold">{h.grado || 'Grado'}</span>
                        </div>
                        <p className="text-xs text-slate-500">
                          {h.observaciones || <span className="italic text-slate-400">Sin observaciones</span>}
                        </p>
                        {h.registrado_por && (
                          <p className="text-[10px] text-slate-400">Registrado por: {h.registrado_por}</p>
                        )}
                      </div>

                      <div className="flex items-center gap-4">
                        <div className="text-right">
                          <span className="text-[10px] text-slate-400 block uppercase font-bold">Promedio Final</span>
                          <span className="font-mono font-black text-base text-slate-800">
                            {h.promedio_anual != null ? Number(h.promedio_anual).toFixed(2) : '—'}
                          </span>
                        </div>

                        {h.resultado === 'PROMOVIDO' && (
                          <span className="px-3 py-1.5 rounded-xl text-xs font-black bg-emerald-100 text-emerald-800">
                            PROMOVIDO
                          </span>
                        )}
                        {(h.resultado === 'NO_PROMOVIDO' || h.resultado === 'REPROBADO') && (
                          <span className="px-3 py-1.5 rounded-xl text-xs font-black bg-rose-100 text-rose-800">
                            NO PROMOVIDO
                          </span>
                        )}
                        {h.resultado === 'RETIRADO' && (
                          <span className="px-3 py-1.5 rounded-xl text-xs font-black bg-slate-200 text-slate-700">
                            RETIRADO
                          </span>
                        )}
                      </div>
                    </div>
                  ))}

                  {(!historialDetalle.historial || historialDetalle.historial.length === 0) && (
                    <p className="text-sm text-slate-400 text-center py-8">
                      Este estudiante no tiene registros de promociones en años anteriores.
                    </p>
                  )}
                </div>
              </div>
            )}

            {!loadingHistorial && !historialDetalle && (
              <div className="py-24 text-center text-slate-400">
                <span className="text-3xl block mb-2">🔍</span>
                Selecciona un estudiante de la lista para consultar su trayectoria académica.
              </div>
            )}
          </div>
        </div>
      )}

      {/* MODAL REGISTRAR / EDITAR RESULTADO INDIVIDUAL */}
      {modalIndividual && (
        <div className="fixed inset-0 bg-slate-950/70 z-50 flex items-center justify-center p-4 backdrop-blur-xs animate-in fade-in duration-150" onClick={() => setModalIndividual(false)}>
          <div className="bg-white rounded-3xl shadow-2xl max-w-md w-full overflow-hidden border border-slate-100" onClick={e => e.stopPropagation()}>
            <div style={{ backgroundColor: PRIMARY }} className="px-6 py-4 text-white flex items-center justify-between">
              <div>
                <h3 className="font-bold text-sm">Calificar Promoción</h3>
                <p className="text-xs text-blue-100 mt-0.5">{estudianteModal?.estudiante}</p>
              </div>
              <button onClick={() => setModalIndividual(false)} className="p-1 rounded-lg text-white/80 hover:text-white hover:bg-white/10 transition">✕</button>
            </div>

            <form onSubmit={handleGuardarIndividual} className="p-6 space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1.5">Resultado Oficial *</label>
                <div className="grid grid-cols-3 gap-2">
                  {[
                    { id: 'PROMOVIDO', label: 'Promovido', color: 'border-emerald-500 bg-emerald-50 text-emerald-800' },
                    { id: 'NO_PROMOVIDO', label: 'No promovido', color: 'border-rose-500 bg-rose-50 text-rose-800' },
                    { id: 'RETIRADO', label: 'Retirado', color: 'border-slate-400 bg-slate-100 text-slate-700' },
                  ].map(opc => (
                    <button
                      type="button"
                      key={opc.id}
                      onClick={() => setForm({ ...form, resultado: opc.id })}
                      className={`py-2 px-1 text-xs font-bold rounded-xl border-2 transition text-center ${form.resultado === opc.id ? opc.color : 'border-slate-200 text-slate-500 hover:bg-slate-50'}`}
                    >
                      {opc.label}
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Promedio Anual (0 - 10)</label>
                <input
                  type="number"
                  min="0"
                  max="10"
                  step="0.01"
                  placeholder="Ej. 8.75"
                  value={form.promedio_anual}
                  onChange={e => setForm({ ...form, promedio_anual: e.target.value })}
                  className="w-full px-3.5 py-2.5 border border-slate-200 rounded-xl text-sm font-mono font-bold focus:ring-2 focus:ring-blue-500 focus:outline-none bg-slate-50"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Observaciones / Acta</label>
                <textarea
                  rows={3}
                  placeholder="Observaciones de la junta académica o resolución..."
                  value={form.observaciones}
                  onChange={e => setForm({ ...form, observaciones: e.target.value })}
                  className="w-full px-3.5 py-2.5 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none bg-slate-50 resize-none"
                />
              </div>

              {errorModal && (
                <div className="p-3 bg-rose-50 border border-rose-200 text-rose-700 text-xs rounded-xl">
                  {errorModal}
                </div>
              )}

              <div className="flex gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setModalIndividual(false)}
                  className="flex-1 py-2.5 border border-slate-200 rounded-xl text-xs font-bold text-slate-600 hover:bg-slate-50 transition"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  style={{ backgroundColor: PRIMARY }}
                  className="flex-1 py-2.5 rounded-xl text-xs font-bold text-white shadow-md hover:opacity-90 transition disabled:opacity-60"
                >
                  {saving ? 'Guardando...' : 'Guardar Resultado'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL PROMOCIÓN MASIVA */}
      {modalMasivo && (
        <div className="fixed inset-0 bg-slate-950/70 z-50 flex items-center justify-center p-4 backdrop-blur-xs animate-in fade-in duration-150" onClick={() => setModalMasivo(false)}>
          <div className="bg-white rounded-3xl shadow-2xl max-w-md w-full overflow-hidden border border-slate-100" onClick={e => e.stopPropagation()}>
            <div className="bg-emerald-700 px-6 py-4 text-white flex items-center justify-between">
              <div>
                <h3 className="font-bold text-sm flex items-center gap-2">⚡ Promoción en Lote</h3>
                <p className="text-xs text-emerald-100 mt-0.5">{seleccionados.length} estudiantes seleccionados</p>
              </div>
              <button onClick={() => setModalMasivo(false)} className="p-1 rounded-lg text-white/80 hover:text-white hover:bg-white/10 transition">✕</button>
            </div>

            <form onSubmit={handleGuardarMasivo} className="p-6 space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1.5">Resultado Masivo *</label>
                <div className="grid grid-cols-3 gap-2">
                  {[
                    { id: 'PROMOVIDO', label: 'Promover', color: 'border-emerald-500 bg-emerald-50 text-emerald-800' },
                    { id: 'NO_PROMOVIDO', label: 'No Promover', color: 'border-rose-500 bg-rose-50 text-rose-800' },
                    { id: 'RETIRADO', label: 'Retirar', color: 'border-slate-400 bg-slate-100 text-slate-700' },
                  ].map(opc => (
                    <button
                      type="button"
                      key={opc.id}
                      onClick={() => setFormMasivo({ ...formMasivo, resultado: opc.id })}
                      className={`py-2 px-1 text-xs font-bold rounded-xl border-2 transition text-center ${formMasivo.resultado === opc.id ? opc.color : 'border-slate-200 text-slate-500 hover:bg-slate-50'}`}
                    >
                      {opc.label}
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Promedio Anual (Opcional)</label>
                <input
                  type="number"
                  min="0"
                  max="10"
                  step="0.01"
                  placeholder="Dejar en blanco si varía por estudiante"
                  value={formMasivo.promedio_anual}
                  onChange={e => setFormMasivo({ ...formMasivo, promedio_anual: e.target.value })}
                  className="w-full px-3.5 py-2.5 border border-slate-200 rounded-xl text-sm font-mono font-bold focus:ring-2 focus:ring-blue-500 focus:outline-none bg-slate-50"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Observación General</label>
                <textarea
                  rows={2}
                  placeholder="Resolución de Junta General de Profesores..."
                  value={formMasivo.observaciones}
                  onChange={e => setFormMasivo({ ...formMasivo, observaciones: e.target.value })}
                  className="w-full px-3.5 py-2.5 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none bg-slate-50 resize-none"
                />
              </div>

              <div className="flex gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setModalMasivo(false)}
                  className="flex-1 py-2.5 border border-slate-200 rounded-xl text-xs font-bold text-slate-600 hover:bg-slate-50 transition"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="flex-1 py-2.5 rounded-xl text-xs font-bold text-white bg-emerald-600 shadow-md hover:bg-emerald-700 transition disabled:opacity-60"
                >
                  {saving ? 'Procesando...' : `Aplicar a ${seleccionados.length} Alumnos`}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </Layout>
  );
}
