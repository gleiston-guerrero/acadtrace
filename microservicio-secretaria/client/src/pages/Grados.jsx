import { useState, useEffect } from 'react';
import api, { apiPrincipal } from '../utils/api';
import Layout from '../components/Layout';
import FichaEstudianteModal from './FichaEstudianteModal';

const PRIMARY = '#243A76';
const PRINCIPAL_ORIGIN = (apiPrincipal.defaults.baseURL || 'http://localhost:8080/api').replace(/\/api\/?$/, '');
const modalBg = { backgroundColor: 'rgba(36, 58, 118, 0.5)' };

const PAGE_SIZE = 10;

const CARD_PALETTES = [
  { id: 'navy',   header: 'bg-[#2b3c66]' },
  { id: 'slate',  header: 'bg-[#475569]' },
  { id: 'indigo', header: 'bg-[#3b4266]' },
  { id: 'teal',   header: 'bg-[#33535e]' },
  { id: 'olive',  header: 'bg-[#4a5840]' },
  { id: 'zinc',   header: 'bg-[#52525b]' },
];

const ORDEN_NIVELES = [
  'Educación Inicial y Preparatoria',
  'Básica Elemental (2do - 4to EGB)',
  'Básica Media (5to - 7mo EGB)',
  'Básica Superior (8vo - 10mo EGB)',
  'Bachillerato General Unificado (BGU)',
];

const normalizarNivel = (nivel, nombreGrado = '') => {
  const n = (nivel || '').toUpperCase().trim();
  const nom = (nombreGrado || '').toUpperCase().trim();
  if (n.includes('INICIAL') || nom.includes('INICIAL') || n.includes('PREPARATORIA') || nom.includes('PREPARATORIA') || n.includes('1ER') || nom.includes('PRIMER')) {
    return 'Educación Inicial y Preparatoria';
  }
  if (n.includes('ELEMENTAL') || nom.includes('2DO') || nom.includes('3RO') || nom.includes('4TO') || nom.includes('SEGUNDO') || nom.includes('TERCERO') || nom.includes('CUARTO')) {
    return 'Básica Elemental (2do - 4to EGB)';
  }
  if (n.includes('MEDIA') || nom.includes('5TO') || nom.includes('6TO') || nom.includes('7MO') || nom.includes('QUINTO') || nom.includes('SEXTO') || nom.includes('SÉPTIMO')) {
    return 'Básica Media (5to - 7mo EGB)';
  }
  if (n.includes('SUPERIOR') || nom.includes('8VO') || nom.includes('9NO') || nom.includes('10MO') || nom.includes('OCTAVO') || nom.includes('NOVENO') || nom.includes('DÉCIMO')) {
    return 'Básica Superior (8vo - 10mo EGB)';
  }
  if (n.includes('BACHILLERATO') || n.includes('BGU') || nom.includes('BACHILLERATO') || nom.includes('BGU')) {
    return 'Bachillerato General Unificado (BGU)';
  }
  return nivel || 'General';
};

const normalizarGrado = (g) => ({
  idGrado: g.id_grado ?? g.idGrado,
  id_grado: g.id_grado ?? g.idGrado,
  nombre: g.nombre,
  orden: g.orden,
  nivelEducativo: g.nivel_educativo ?? g.nivelEducativo ?? '',
  nivel_educativo: g.nivel_educativo ?? g.nivelEducativo ?? '',
  idNivel: g.id_nivel ?? g.idNivel,
  id_nivel: g.id_nivel ?? g.idNivel,
  capacidadMax: g.capacidad_max ?? g.capacidadMax ?? 35,
  capacidad_max: g.capacidad_max ?? g.capacidadMax ?? 35,
  tipoEscala: g.tipo_escala ?? g.tipoEscala ?? 'CUANTITATIVA',
  tipo_escala: g.tipo_escala ?? g.tipoEscala ?? 'CUANTITATIVA',
  activo: g.activo ?? true,
  paralelos: (g.paralelos || []).map(p => ({
    idParalelo: p.id_paralelo ?? p.idParalelo,
    id_paralelo: p.id_paralelo ?? p.idParalelo,
    letra: p.letra,
    activo: p.activo ?? true,
    totalEstudiantes: p.total_estudiantes ?? p.totalEstudiantes ?? 0,
    total_estudiantes: p.total_estudiantes ?? p.totalEstudiantes ?? 0,
  }))
});

export default function Grados() {
  const [grados, setGrados] = useState([]);
  const [loading, setLoading] = useState(true);
  const [seccion, setSeccion] = useState('cursos');
  const [gradoSel, setGradoSel] = useState(null);
  const [paraleloSel, setParaleloSel] = useState(null);
  const [showCrearParalelo, setShowCrearParalelo] = useState(false);
  const [nuevaLetra, setNuevaLetra] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [gradoEditar, setGradoEditar] = useState(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [saving, setSaving] = useState(false);

  // Vista de estudiantes en paralelo
  const [estudiantesParalelo, setEstudiantesParalelo] = useState([]);
  const [loadingEstudiantes, setLoadingEstudiantes] = useState(false);
  const [anoLectivoActual, setAnoLectivoActual] = useState(null);
  const [busqueda, setBusqueda] = useState('');
  const [pagina, setPagina] = useState(1);
  const [estudianteDetalle, setEstudianteDetalle] = useState(null);

  const cargar = () => {
    setLoading(true);
    api.get('/grados')
      .then(r => {
        const raw = r.data || [];
        const normalizados = raw.map(normalizarGrado);
        setGrados(normalizados);
        if (gradoSel) {
          const actualizado = normalizados.find(g => g.idGrado === gradoSel.idGrado);
          if (actualizado) setGradoSel(actualizado);
        }
      })
      .catch(() => setError('Error al cargar grados'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    cargar();
    apiPrincipal.get('/anos-lectivos/actual')
      .then(r => setAnoLectivoActual(r.data))
      .catch(() => {});
  }, []);

  useEffect(() => {
    if (success) {
      const t = setTimeout(() => setSuccess(''), 4000);
      return () => clearTimeout(t);
    }
  }, [success]);

  // Agrupamiento por nivel normalizado y orden pedagógico
  const nivelesAgrupados = {};
  grados.forEach(g => {
    const nivel = normalizarNivel(g.nivelEducativo, g.nombre);
    if (!nivelesAgrupados[nivel]) nivelesAgrupados[nivel] = [];
    nivelesAgrupados[nivel].push(g);
  });

  const nivelesOrdenados = Object.entries(nivelesAgrupados).sort(([a], [b]) => {
    const idxA = ORDEN_NIVELES.indexOf(a);
    const idxB = ORDEN_NIVELES.indexOf(b);
    if (idxA !== -1 && idxB !== -1) return idxA - idxB;
    if (idxA !== -1) return -1;
    if (idxB !== -1) return 1;
    return a.localeCompare(b);
  });

  const niveles = (() => {
    const seen = new Set();
    const nivs = [];
    grados.forEach(g => {
      if (g.idNivel && !seen.has(g.idNivel)) {
        seen.add(g.idNivel);
        nivs.push({ id: g.idNivel, nombre: g.nivelEducativo });
      }
    });
    return nivs;
  })();

  const crearParalelo = async () => {
    if (!nuevaLetra.trim()) return;
    setSaving(true);
    try {
      await api.post(`/grados/${gradoSel.idGrado}/paralelos`, {}, { params: { letra: nuevaLetra.trim().toUpperCase() } });
      setSuccess(`Paralelo ${nuevaLetra.toUpperCase()} creado`);
      setShowCrearParalelo(false);
      setNuevaLetra('');
      cargar();
    } catch (e) {
      setError(e.response?.data?.error || 'Error al crear paralelo');
    } finally {
      setSaving(false);
    }
  };

  const cambiarEstadoGrado = async (grado) => {
    try {
      await api.patch(`/grados/${grado.idGrado}/estado`, {}, { params: { activo: !grado.activo } });
      cargar();
    } catch {
      setError('Error al cambiar el estado del grado');
    }
  };

  const cambiarEstadoParalelo = async (paralelo) => {
    try {
      await api.patch(`/grados/paralelos/${paralelo.idParalelo}/estado`, {}, { params: { activo: !paralelo.activo } });
      cargar();
    } catch {
      setError('Error al cambiar el estado del paralelo');
    }
  };

  const verEstudiantes = async (paralelo, grado) => {
    setParaleloSel({ ...paralelo, gradoNombre: grado.nombre, nivelEducativo: grado.nivelEducativo, idGrado: grado.idGrado });
    setLoadingEstudiantes(true);
    setBusqueda('');
    setPagina(1);

    const idG = grado.idGrado;
    const idP = paralelo.idParalelo;

    try {
      let anoId = anoLectivoActual?.idAnoLectivo || anoLectivoActual?.id_ano_lectivo;
      if (!anoId) {
        const rAno = await apiPrincipal.get('/anos-lectivos/actual').catch(() => null);
        if (rAno?.data) {
          setAnoLectivoActual(rAno.data);
          anoId = rAno.data.idAnoLectivo || rAno.data.id_ano_lectivo;
        }
      }

      let alumnos = [];
      if (anoId) {
        try {
          const res = await apiPrincipal.get('/estudiantes/por-grado', {
            params: { idGrado: idG, idAnoLectivo: anoId, idParalelo: idP }
          });
          if (Array.isArray(res.data) && res.data.length > 0) {
            alumnos = res.data.map(e => ({
              idEstudiante: e.idEstudiante || e.id_estudiante,
              nombres: e.nombres || '',
              apellidos: e.apellidos || '',
              cedula: e.cedula || '',
              codigoEstudiante: e.codigoEstudiante || e.codigo_estudiante || '',
              representante: e.representante || '—',
              estado: e.estado || 'ACTIVO',
              correo: e.correo || '',
            }));
          }
        } catch {
          // Fallback a matriculas de la base local si por-grado no responde
        }
      }

      if (alumnos.length === 0 && anoId) {
        try {
          const rMat = await api.get(`/matriculas/ano-lectivo/${anoId}`, { params: { limit: 100 } });
          const items = rMat.data?.data || rMat.data?.items || rMat.data || [];
          if (Array.isArray(items)) {
            alumnos = items
              .filter(m => (m.id_grado === idG || m.idGrado === idG) && (m.id_paralelo === idP || m.idParalelo === idP))
              .map(m => ({
                idEstudiante: m.id_estudiante || m.idEstudiante,
                nombres: m.estudiante_nombres || m.nombres || '',
                apellidos: m.estudiante_apellidos || m.apellidos || '',
                cedula: m.estudiante_cedula || m.cedula || '',
                codigoEstudiante: m.codigo_estudiante || m.codigoEstudiante || '',
                representante: m.representante_nombres ? `${m.representante_nombres} ${m.representante_apellidos || ''}` : (m.representante || '—'),
                estado: m.estado || 'ACTIVO',
                correo: m.correo || '',
              }));
          }
        } catch {
          // fallback
        }
      }

      setEstudiantesParalelo(alumnos);
    } catch (e) {
      console.error('Error al cargar estudiantes del curso:', e);
      setEstudiantesParalelo([]);
    } finally {
      setLoadingEstudiantes(false);
    }
  };

  const abrirDetalleEstudiante = async (est) => {
    const id = est.idEstudiante || est.id_estudiante;
    try {
      const res = await api.get(`/estudiantes/${id}`);
      setEstudianteDetalle(res.data || est);
    } catch {
      setEstudianteDetalle(est);
    }
  };

  const filtrados = estudiantesParalelo
    .filter(e => `${e.nombres} ${e.apellidos} ${e.cedula || ''} ${e.codigoEstudiante || ''}`.toLowerCase().includes(busqueda.toLowerCase()))
    .sort((a, b) => `${a.apellidos} ${a.nombres}`.localeCompare(`${b.apellidos} ${b.nombres}`));
  const totalPaginas = Math.max(1, Math.ceil(filtrados.length / PAGE_SIZE));
  const paginaReal = Math.min(pagina, totalPaginas);
  const paginados = filtrados.slice((paginaReal - 1) * PAGE_SIZE, paginaReal * PAGE_SIZE);

  const menuItems = [
    { id: 'cursos', label: 'Cursos', icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" /></svg> },
    { id: 'nuevo', label: 'Nuevo grado', icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg> },
  ];

  const handleSeccion = (id) => {
    setSeccion(id);
    if (id === 'nuevo') {
      setGradoEditar(null);
      setShowModal(true);
      setError('');
    }
    if (id === 'cursos') {
      setGradoSel(null);
      setParaleloSel(null);
    }
  };

  return (
    <Layout
      breadcrumb={
        paraleloSel
          ? ['Inicio', 'Grados', gradoSel?.nombre || '', `Paralelo ${paraleloSel.letra}`]
          : gradoSel
            ? ['Inicio', 'Grados', gradoSel.nombre]
            : ['Inicio', 'Grados']
      }
      sidebarTitle={paraleloSel ? `${paraleloSel.gradoNombre} "${paraleloSel.letra}"` : 'Grados'}
      menuItems={menuItems}
      seccion={seccion}
      onSeccionChange={handleSeccion}
    >
      {error && (
        <div className="mb-4 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-800 rounded-xl px-4 py-3 flex justify-between">
          <span className="text-red-600 dark:text-red-300 text-sm">{error}</span>
          <button onClick={() => setError('')} className="text-red-400 hover:text-red-600 ml-4">✕</button>
        </div>
      )}
      {success && (
        <div className="mb-4 bg-green-50 dark:bg-green-950/40 border border-green-200 dark:border-green-800 rounded-xl px-4 py-3 flex justify-between">
          <span className="text-green-600 dark:text-green-300 text-sm">{success}</span>
          <button onClick={() => setSuccess('')} className="text-green-400 hover:text-green-600 ml-4">✕</button>
        </div>
      )}

      {loading ? (
        <div className="p-12 text-center text-slate-400 dark:text-slate-500 text-sm">Cargando grados...</div>
      ) : paraleloSel ? (
        /* ── VISTA ESTUDIANTES DEL PARALELO ── */
        <div>
          <button
            onClick={() => setParaleloSel(null)}
            className="flex items-center gap-1 text-sm text-slate-500 dark:text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 mb-4 transition"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
            </svg>
            Volver a paralelos de {gradoSel?.nombre}
          </button>

          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-5">
            <div>
              <h1 className="text-xl font-bold text-slate-700 dark:text-white">
                {paraleloSel.gradoNombre} "{paraleloSel.letra}"
              </h1>
              <p className="text-sm text-slate-400 dark:text-slate-400 mt-0.5">
                {paraleloSel.nivelEducativo} — {estudiantesParalelo.length} estudiante(s) matriculado(s)
              </p>
            </div>

            <div className="w-full sm:w-72">
              <div className="relative">
                <input
                  type="text"
                  value={busqueda}
                  onChange={e => { setBusqueda(e.target.value); setPagina(1); }}
                  placeholder="Buscar estudiante o cédula..."
                  className="w-full pl-9 pr-3 py-2 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl text-xs focus:outline-none text-slate-700 dark:text-slate-200 placeholder-slate-400"
                />
                <svg className="w-4 h-4 absolute left-3 top-2.5 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
              </div>
            </div>
          </div>

          {loadingEstudiantes ? (
            <div className="p-12 text-center text-slate-400 dark:text-slate-500 text-sm bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700">
              Cargando estudiantes del curso...
            </div>
          ) : filtrados.length === 0 ? (
            <div className="p-12 text-center text-slate-400 dark:text-slate-500 text-sm bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700">
              {busqueda ? 'No se encontraron estudiantes con esa búsqueda.' : 'No hay estudiantes matriculados en este paralelo.'}
            </div>
          ) : (
            <>
              <div className="bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700 overflow-hidden shadow-xs">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-slate-100 dark:border-slate-700" style={{ backgroundColor: '#f8f9fc' }}>
                      <th className="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">#</th>
                      <th className="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Estudiante</th>
                      <th className="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Cédula</th>
                      <th className="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Código</th>
                      <th className="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Representante</th>
                      <th className="text-center px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Estado</th>
                      <th className="text-center px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Ver</th>
                    </tr>
                  </thead>
                  <tbody>
                    {paginados.map((e, i) => (
                      <tr key={e.idEstudiante || i} className="border-b border-slate-50 dark:border-slate-700/50 hover:bg-slate-50 dark:hover:bg-slate-700/30 transition">
                        <td className="px-4 py-3 text-slate-400 text-xs">{(paginaReal - 1) * PAGE_SIZE + i + 1}</td>
                        <td className="px-4 py-3">
                          <div className="flex items-center gap-3">
                            <div className="w-8 h-8 rounded-full flex items-center justify-center text-white text-xs font-bold flex-shrink-0" style={{ backgroundColor: PRIMARY }}>
                              {e.nombres?.[0]}{e.apellidos?.[0]}
                            </div>
                            <div>
                              <p className="font-medium text-slate-700 dark:text-slate-200">{e.apellidos} {e.nombres}</p>
                              {e.correo && <p className="text-xs text-slate-400">{e.correo}</p>}
                            </div>
                          </div>
                        </td>
                        <td className="px-4 py-3 text-slate-600 dark:text-slate-400">{e.cedula || '—'}</td>
                        <td className="px-4 py-3 text-slate-600 dark:text-slate-400 font-mono text-xs">{e.codigoEstudiante || '—'}</td>
                        <td className="px-4 py-3 text-slate-600 dark:text-slate-400">{e.representante || '—'}</td>
                        <td className="px-4 py-3 text-center">
                          <span className={`text-[11px] font-semibold px-2 py-0.5 rounded-md ${
                            (e.estado === 'ACTIVO' || e.estado === 'ACTIVA')
                              ? 'bg-green-100 dark:bg-green-900/40 text-green-700 dark:text-green-300'
                              : 'bg-slate-200 dark:bg-slate-700 text-slate-500 dark:text-slate-300'
                          }`}>
                            {e.estado}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-center">
                          <button
                            onClick={() => abrirDetalleEstudiante(e)}
                            title="Ver ficha completa"
                            className="p-1.5 rounded-lg text-slate-400 hover:text-white transition"
                            onMouseOver={ev => ev.currentTarget.style.backgroundColor = PRIMARY}
                            onMouseOut={ev => ev.currentTarget.style.backgroundColor = 'transparent'}
                          >
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                            </svg>
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {totalPaginas > 1 && (
                <div className="flex items-center justify-between mt-4">
                  <p className="text-xs text-slate-400 dark:text-slate-500">
                    Mostrando {(paginaReal - 1) * PAGE_SIZE + 1}–{Math.min(paginaReal * PAGE_SIZE, filtrados.length)} de {filtrados.length}
                  </p>
                  <div className="flex items-center gap-1">
                    <button
                      onClick={() => setPagina(p => Math.max(1, p - 1))}
                      disabled={paginaReal === 1}
                      className="px-3 py-1.5 text-xs border border-slate-200 dark:border-slate-700 rounded-lg hover:bg-slate-50 dark:hover:bg-slate-700 disabled:opacity-40 transition text-slate-700 dark:text-slate-200"
                    >
                      Anterior
                    </button>
                    {Array.from({ length: totalPaginas }, (_, i) => i + 1)
                      .filter(p => p === 1 || p === totalPaginas || Math.abs(p - paginaReal) <= 1)
                      .map((p, idx, arr) => (
                        <span key={p}>
                          {idx > 0 && arr[idx - 1] < p - 1 && <span className="px-1 text-slate-300 dark:text-slate-600">...</span>}
                          <button
                            onClick={() => setPagina(p)}
                            className={`px-3 py-1.5 text-xs rounded-lg transition ${
                              p === paginaReal ? 'text-white' : 'border border-slate-200 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200'
                            }`}
                            style={p === paginaReal ? { backgroundColor: PRIMARY } : {}}
                          >
                            {p}
                          </button>
                        </span>
                      ))}
                    <button
                      onClick={() => setPagina(p => Math.min(totalPaginas, p + 1))}
                      disabled={paginaReal === totalPaginas}
                      className="px-3 py-1.5 text-xs border border-slate-200 dark:border-slate-700 rounded-lg hover:bg-slate-50 dark:hover:bg-slate-700 disabled:opacity-40 transition text-slate-700 dark:text-slate-200"
                    >
                      Siguiente
                    </button>
                  </div>
                </div>
              )}
            </>
          )}

          {/* Modal detalle estudiante */}
          {estudianteDetalle && (
            <FichaEstudianteModal
              selected={estudianteDetalle}
              principalOrigin={PRINCIPAL_ORIGIN}
              primary={PRIMARY}
              onClose={() => setEstudianteDetalle(null)}
            />
          )}
        </div>
      ) : !gradoSel ? (
        /* ── VISTA PRINCIPAL: Tarjetas grandes agrupadas por nivel — UNIFORME CON SGA PRINCIPAL ── */
        <div>
          <div className="flex items-center justify-between mb-6">
            <div>
              <h1 className="text-xl font-bold text-slate-700 dark:text-white">Grados y Cursos</h1>
              <p className="text-sm text-slate-400 dark:text-slate-400 mt-0.5">
                {grados.length} grados configurados — Escuela "Provincias Unidas"
              </p>
            </div>
          </div>

          {grados.length === 0 ? (
            <div className="p-12 text-center text-slate-400 dark:text-slate-500 text-sm bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700">
              No hay grados configurados todavía.
            </div>
          ) : (
            nivelesOrdenados.map(([nivel, gradosNivel]) => (
              <div key={nivel} className="mb-6">
                <div className="flex items-center gap-2 mb-3">
                  <div className="w-1.5 h-5 rounded-full" style={{ backgroundColor: PRIMARY }} />
                  <h2 className="text-sm font-bold uppercase tracking-wide" style={{ color: PRIMARY }}>
                    {nivel}
                  </h2>
                  <span className="text-xs text-slate-400 dark:text-slate-500 ml-1">
                    ({gradosNivel.length} grado{gradosNivel.length !== 1 ? 's' : ''})
                  </span>
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                  {gradosNivel.map(g => (
                    <GradoCard
                      key={g.idGrado}
                      grado={g}
                      onClick={() => setGradoSel(g)}
                      onEditar={() => { setGradoEditar(g); setShowModal(true); }}
                      onToggleEstado={() => cambiarEstadoGrado(g)}
                    />
                  ))}
                </div>
              </div>
            ))
          )}
        </div>
      ) : (
        /* ── VISTA DETALLE: Paralelos del grado seleccionado — UNIFORME CON SGA PRINCIPAL ── */
        <div>
          <button
            onClick={() => setGradoSel(null)}
            className="flex items-center gap-1 text-sm text-slate-500 dark:text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 mb-4 transition"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
            </svg>
            Volver a grados
          </button>

          <div className="flex items-center justify-between mb-5">
            <div>
              <h1 className="text-xl font-bold text-slate-700 dark:text-white">{gradoSel.nombre}</h1>
              <p className="text-sm text-slate-400 dark:text-slate-400 mt-0.5">
                {gradoSel.nivelEducativo} — {gradoSel.tipoEscala === 'CUALITATIVA' ? 'Escala cualitativa' : 'Escala cuantitativa'} — Capacidad máx: {gradoSel.capacidadMax || 35} alumnos
              </p>
            </div>
            <button
              onClick={() => { setShowCrearParalelo(true); setNuevaLetra(''); setError(''); }}
              style={{ backgroundColor: PRIMARY }}
              className="flex items-center gap-2 text-white px-5 py-2.5 rounded-lg text-sm font-medium hover:opacity-90 transition shadow-sm"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
              </svg>
              Nuevo paralelo
            </button>
          </div>

          {(!gradoSel.paralelos || gradoSel.paralelos.length === 0) ? (
            <div className="p-12 text-center text-slate-400 dark:text-slate-500 text-sm bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700">
              No hay paralelos configurados para este grado.
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
              {gradoSel.paralelos.map(p => (
                <ParaleloCard
                  key={p.idParalelo}
                  paralelo={p}
                  grado={gradoSel}
                  onToggleEstado={() => cambiarEstadoParalelo(p)}
                  onVerEstudiantes={() => verEstudiantes(p, gradoSel)}
                />
              ))}
            </div>
          )}
        </div>
      )}

      {/* MODAL CREAR PARALELO */}
      {showCrearParalelo && (
        <div className="fixed inset-0 z-50 flex items-center justify-center px-4" style={modalBg}>
          <div className="bg-white dark:bg-slate-800 rounded-2xl shadow-2xl w-full max-w-sm overflow-hidden text-slate-800 dark:text-slate-100">
            <div style={{ backgroundColor: PRIMARY }} className="px-6 py-4 flex items-center justify-between">
              <h2 className="text-white font-bold text-base">Nuevo Paralelo — {gradoSel.nombre}</h2>
              <button onClick={() => setShowCrearParalelo(false)} className="text-white text-opacity-70 hover:text-opacity-100">✕</button>
            </div>
            <div className="p-6">
              {error && <div className="mb-3 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-800 rounded-lg px-3 py-2 text-red-600 dark:text-red-300 text-xs">{error}</div>}
              <label className="block text-xs font-semibold text-slate-500 dark:text-slate-400 mb-1 uppercase tracking-wide">Letra del paralelo</label>
              <input
                type="text"
                maxLength={1}
                value={nuevaLetra}
                onChange={e => setNuevaLetra(e.target.value.toUpperCase())}
                placeholder="Ej: D"
                className="w-full px-3 py-2 border border-slate-200 dark:border-slate-700 rounded-lg text-sm focus:outline-none bg-slate-50 dark:bg-slate-900 text-center text-2xl font-bold uppercase"
                style={{ color: PRIMARY }}
              />
              <div className="flex gap-3 mt-5">
                <button
                  onClick={() => setShowCrearParalelo(false)}
                  className="flex-1 py-2 border border-slate-200 dark:border-slate-700 rounded-lg text-sm text-slate-600 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-700 transition"
                >
                  Cancelar
                </button>
                <button
                  onClick={crearParalelo}
                  disabled={!nuevaLetra.trim() || saving}
                  style={{ backgroundColor: PRIMARY }}
                  className="flex-1 py-2 text-white rounded-lg text-sm font-medium hover:opacity-90 transition disabled:opacity-60"
                >
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
          <div className="bg-white dark:bg-slate-800 rounded-2xl shadow-2xl w-full max-w-md overflow-hidden text-slate-800 dark:text-slate-100">
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

/* ── TARJETA DE GRADO — DISEÑO DOS TONOS UNIFORME CON SGA PRINCIPAL ── */
function GradoCard({ grado, onClick, onEditar, onToggleEstado }) {
  const totalParalelos = grado.paralelos?.length || 0;
  const paralelosActivos = grado.paralelos?.filter(p => p.activo).length || 0;

  const storageKey = `grado_color_${grado.idGrado}`;
  const [colorIdx, setColorIdx] = useState(() => {
    const saved = localStorage.getItem(storageKey);
    return saved !== null ? parseInt(saved, 10) : (grado.idGrado % CARD_PALETTES.length);
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
    <div
      onClick={onClick}
      className="bg-white dark:bg-slate-800 border border-slate-200/90 dark:border-slate-700 rounded-2xl overflow-hidden shadow-sm hover:shadow-md hover:border-slate-400 transition-all duration-200 flex flex-col text-left group cursor-pointer relative"
    >
      {/* BANDA DE ENCABEZADO CON TONO SUAVE */}
      <div className={`${currentPalette.header} p-4 text-white relative`}>
        <div className="flex items-center justify-between gap-2 mb-2">
          <span className="bg-white/20 text-white text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded">
            {grado.nivelEducativo || 'EGB'}
          </span>

          <div className="flex items-center gap-2">
            <span className={`text-[10px] font-bold uppercase px-2 py-0.5 rounded ${grado.activo ? 'bg-emerald-600 text-white' : 'bg-slate-500 text-white'}`}>
              {grado.activo ? 'ACTIVO' : 'INACTIVO'}
            </span>

            {/* BOTÓN 3 PUNTOS (•••) */}
            <div className="relative">
              <button
                type="button"
                onClick={e => { e.stopPropagation(); setShowMenu(!showMenu); }}
                className="w-6 h-6 rounded bg-white/20 hover:bg-white/30 text-white flex items-center justify-center text-xs font-bold transition cursor-pointer"
                title="Opciones"
              >
                •••
              </button>

              {/* MENÚ DESPLEGABLE */}
              {showMenu && (
                <div
                  className="absolute right-0 top-7 w-40 bg-white dark:bg-slate-800 rounded-lg shadow-xl border border-slate-200 dark:border-slate-700 py-1 z-30 text-slate-700 dark:text-slate-200"
                  onClick={e => e.stopPropagation()}
                >
                  <button
                    type="button"
                    onClick={cambiarColor}
                    className="w-full text-left px-3 py-1.5 text-xs hover:bg-slate-50 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 transition"
                  >
                    Cambiar color
                  </button>
                  {onEditar && (
                    <button
                      type="button"
                      onClick={e => { e.stopPropagation(); setShowMenu(false); onEditar(); }}
                      className="w-full text-left px-3 py-1.5 text-xs hover:bg-slate-50 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 transition"
                    >
                      Editar grado
                    </button>
                  )}
                  {onToggleEstado && (
                    <button
                      type="button"
                      onClick={e => { e.stopPropagation(); setShowMenu(false); onToggleEstado(); }}
                      className="w-full text-left px-3 py-1.5 text-xs hover:bg-slate-50 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 transition"
                    >
                      {grado.activo ? 'Desactivar' : 'Activar'}
                    </button>
                  )}
                  <button
                    type="button"
                    onClick={e => { e.stopPropagation(); setShowMenu(false); onClick(); }}
                    className="w-full text-left px-3 py-1.5 text-xs hover:bg-slate-50 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 transition"
                  >
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

      {/* CUERPO INFERIOR CON 3 MÉTRICAS */}
      <div className="p-4 bg-white dark:bg-slate-800 flex-1 flex flex-col justify-between space-y-3">
        <div className="grid grid-cols-3 gap-2">
          <div className="bg-slate-50 dark:bg-slate-700/50 border border-slate-100 dark:border-slate-700 rounded-lg p-2 text-center">
            <span className="block text-[10px] font-semibold text-slate-400 uppercase">PARALELOS</span>
            <span className="block text-xs font-bold text-slate-700 dark:text-slate-200 mt-0.5">{totalParalelos}</span>
          </div>
          <div className="bg-slate-50 dark:bg-slate-700/50 border border-slate-100 dark:border-slate-700 rounded-lg p-2 text-center">
            <span className="block text-[10px] font-semibold text-slate-400 uppercase">ACTIVOS</span>
            <span className="block text-xs font-bold text-slate-700 dark:text-slate-200 mt-0.5">{paralelosActivos}</span>
          </div>
          <div className="bg-slate-50 dark:bg-slate-700/50 border border-slate-100 dark:border-slate-700 rounded-lg p-2 text-center">
            <span className="block text-[10px] font-semibold text-slate-400 uppercase">ESTADO</span>
            <span className="block text-xs font-bold text-emerald-600 dark:text-emerald-400 mt-0.5">{grado.activo ? 'Vigente' : 'Inactivo'}</span>
          </div>
        </div>

        <div className="pt-2 border-t border-slate-100 dark:border-slate-700 flex items-center justify-between">
          <span className="text-xs font-medium text-slate-600 dark:text-slate-300 group-hover:text-[#243A76] dark:group-hover:text-blue-400 transition-colors">
            Abrir paralelos
          </span>
          <span className="text-xs font-bold text-slate-400 dark:text-slate-500 group-hover:text-[#243A76] dark:group-hover:text-blue-400 transition-colors">
            →
          </span>
        </div>
      </div>

      {showMenu && (
        <div className="fixed inset-0 z-20 cursor-default" onClick={e => { e.stopPropagation(); setShowMenu(false); }} />
      )}
    </div>
  );
}

/* ── TARJETA DE PARALELO — DISEÑO DOS TONOS UNIFORME CON SGA PRINCIPAL ── */
function ParaleloCard({ paralelo, grado, onToggleEstado, onVerEstudiantes }) {
  const storageKey = `paralelo_color_${paralelo.idParalelo}`;
  const [colorIdx, setColorIdx] = useState(() => {
    const saved = localStorage.getItem(storageKey);
    return saved !== null ? parseInt(saved, 10) : (paralelo.idParalelo % CARD_PALETTES.length);
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
    <div className="bg-white dark:bg-slate-800 border border-slate-200/90 dark:border-slate-700 rounded-2xl overflow-hidden shadow-sm hover:shadow-md hover:border-slate-400 transition-all duration-200 flex flex-col text-left group relative">
      {/* CABECERA CON COLOR */}
      <div className={`${currentPalette.header} p-4 text-white relative`}>
        <div className="flex items-center justify-between gap-2 mb-2">
          <div className="flex items-center gap-2">
            <span className="bg-white/20 text-white text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded">
              {grado.nivelEducativo || 'EGB'}
            </span>
            <span className={`text-[10px] font-bold uppercase px-2 py-0.5 rounded ${paralelo.activo ? 'bg-emerald-600 text-white' : 'bg-slate-500 text-white'}`}>
              {paralelo.activo ? 'ACTIVO' : 'INACTIVO'}
            </span>
          </div>

          <div className="relative">
            <button
              type="button"
              onClick={e => { e.stopPropagation(); setShowMenu(!showMenu); }}
              className="w-6 h-6 rounded bg-white/20 hover:bg-white/30 text-white flex items-center justify-center text-xs font-bold transition cursor-pointer"
              title="Opciones"
            >
              •••
            </button>

            {showMenu && (
              <div
                className="absolute right-0 top-7 w-40 bg-white dark:bg-slate-800 rounded-lg shadow-xl border border-slate-200 dark:border-slate-700 py-1 z-30 text-slate-700 dark:text-slate-200"
                onClick={e => e.stopPropagation()}
              >
                <button
                  type="button"
                  onClick={cambiarColor}
                  className="w-full text-left px-3 py-1.5 text-xs hover:bg-slate-50 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 transition"
                >
                  Cambiar color
                </button>
                {onToggleEstado && (
                  <button
                    type="button"
                    onClick={e => { e.stopPropagation(); setShowMenu(false); onToggleEstado(); }}
                    className="w-full text-left px-3 py-1.5 text-xs hover:bg-slate-50 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 transition"
                  >
                    {paralelo.activo ? 'Desactivar' : 'Activar'}
                  </button>
                )}
                <button
                  type="button"
                  onClick={e => { e.stopPropagation(); setShowMenu(false); onVerEstudiantes(); }}
                  className="w-full text-left px-3 py-1.5 text-xs hover:bg-slate-50 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 transition"
                >
                  Ver estudiantes
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
            <h3 className="text-base font-bold uppercase tracking-wide leading-tight">
              {grado.nombre} "{paralelo.letra}"
            </h3>
            <p className="text-[11px] text-slate-200 font-medium mt-0.5">Escuela Provincias Unidas</p>
          </div>
        </div>
      </div>

      {/* CUERPO INFERIOR CON ESTADÍSTICAS */}
      <div className="p-4 bg-white dark:bg-slate-800 flex-1 flex flex-col justify-between space-y-3">
        <div className="grid grid-cols-3 gap-2">
          <div className="bg-slate-50 dark:bg-slate-700/50 border border-slate-100 dark:border-slate-700 rounded-lg p-2 text-center">
            <span className="block text-[10px] font-semibold text-slate-400 uppercase">ALUMNOS</span>
            <span className="block text-xs font-bold text-slate-700 dark:text-slate-200 mt-0.5">{paralelo.totalEstudiantes ?? 0}</span>
          </div>
          <div className="bg-slate-50 dark:bg-slate-700/50 border border-slate-100 dark:border-slate-700 rounded-lg p-2 text-center">
            <span className="block text-[10px] font-semibold text-slate-400 uppercase">CAPACIDAD</span>
            <span className="block text-xs font-bold text-slate-700 dark:text-slate-200 mt-0.5">{grado.capacidadMax || 35}</span>
          </div>
          <div className="bg-slate-50 dark:bg-slate-700/50 border border-slate-100 dark:border-slate-700 rounded-lg p-2 text-center">
            <span className="block text-[10px] font-semibold text-slate-400 uppercase">ESCALA</span>
            <span className="block text-xs font-bold text-slate-700 dark:text-slate-200 mt-0.5">{grado.tipoEscala === 'CUALITATIVA' ? 'C' : 'N'}</span>
          </div>
        </div>

        <div className="pt-2 border-t border-slate-100 dark:border-slate-700 flex items-center justify-between">
          <button
            type="button"
            onClick={onVerEstudiantes}
            className="text-xs font-medium text-slate-600 dark:text-slate-300 group-hover:text-[#243A76] dark:group-hover:text-blue-400 transition-colors"
          >
            Ver detalle y estudiantes
          </button>
          <button
            type="button"
            onClick={onVerEstudiantes}
            className="text-xs font-bold text-slate-400 dark:text-slate-500 group-hover:text-[#243A76] dark:group-hover:text-blue-400 transition-colors"
          >
            →
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
  const [capacidad, setCapacidad] = useState(String(grado?.capacidadMax || grado?.capacidad_max || 35));
  const [idNivel, setIdNivel] = useState(grado?.idNivel || grado?.id_nivel || '');
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
        await api.put(`/grados/${grado.idGrado}`, payload);
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
        <label className="block text-xs font-semibold text-slate-500 dark:text-slate-400 mb-1 uppercase tracking-wide">Nombre del grado</label>
        <input
          type="text"
          value={nombre}
          onChange={e => setNombre(e.target.value)}
          required
          placeholder="Ej: Quinto año EGB"
          className="w-full px-3 py-2 border border-slate-200 dark:border-slate-700 rounded-lg text-sm focus:outline-none bg-slate-50 dark:bg-slate-900 text-slate-800 dark:text-slate-100"
        />
      </div>
      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="block text-xs font-semibold text-slate-500 dark:text-slate-400 mb-1 uppercase tracking-wide">Orden</label>
          <input
            type="number"
            value={orden}
            onChange={e => setOrden(e.target.value)}
            required
            placeholder="1"
            className="w-full px-3 py-2 border border-slate-200 dark:border-slate-700 rounded-lg text-sm focus:outline-none bg-slate-50 dark:bg-slate-900 text-slate-800 dark:text-slate-100"
          />
        </div>
        <div>
          <label className="block text-xs font-semibold text-slate-500 dark:text-slate-400 mb-1 uppercase tracking-wide">Capacidad máx.</label>
          <input
            type="number"
            value={capacidad}
            onChange={e => setCapacidad(e.target.value)}
            placeholder="35"
            className="w-full px-3 py-2 border border-slate-200 dark:border-slate-700 rounded-lg text-sm focus:outline-none bg-slate-50 dark:bg-slate-900 text-slate-800 dark:text-slate-100"
          />
        </div>
      </div>
      <div>
        <label className="block text-xs font-semibold text-slate-500 dark:text-slate-400 mb-1 uppercase tracking-wide">Nivel educativo</label>
        <select
          value={idNivel}
          onChange={e => setIdNivel(e.target.value)}
          className="w-full px-3 py-2 border border-slate-200 dark:border-slate-700 rounded-lg text-sm focus:outline-none bg-slate-50 dark:bg-slate-900 text-slate-800 dark:text-slate-100"
        >
          <option value="">Seleccionar nivel</option>
          {niveles.map(n => <option key={n.id} value={n.id}>{n.nombre}</option>)}
        </select>
        <p className="text-xs text-slate-400 dark:text-slate-500 mt-1">Los niveles disponibles se toman de los grados ya existentes.</p>
      </div>
      <div className="flex gap-3 pt-2">
        <button
          type="button"
          onClick={onCancel}
          className="flex-1 py-2 border border-slate-200 dark:border-slate-700 rounded-lg text-sm text-slate-600 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-700 transition"
        >
          Cancelar
        </button>
        <button
          type="submit"
          disabled={saving}
          style={{ backgroundColor: PRIMARY }}
          className="flex-1 py-2 text-white rounded-lg text-sm font-medium hover:opacity-90 transition disabled:opacity-60"
        >
          {saving ? 'Guardando...' : esEdicion ? 'Guardar cambios' : 'Crear grado'}
        </button>
      </div>
    </form>
  );
}
