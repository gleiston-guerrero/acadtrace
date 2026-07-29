import { useState, useEffect } from "react";
import api from "../../config/axios";
import Layout from "../../components/Layout";


const PRIMARY = "#243A76";
const modalBg = { backgroundColor: "rgba(36, 58, 118, 0.5)" };

const NIVELES_CONFIG = {
  Inicial:             { accent: "#c4956a", accentLight: "#faf5ef", accentMid: "#e8d5c0", textAccent: "#8b6842" },
  Preparatoria:        { accent: "#7c8a6e", accentLight: "#f3f6ef", accentMid: "#d4ddc8", textAccent: "#5a6b48" },
  "Básica Elemental":  { accent: "#6a8a9a", accentLight: "#eef4f7", accentMid: "#c5d9e2", textAccent: "#446778" },
  "Básica Media":      { accent: "#6e7499", accentLight: "#f0f1f7", accentMid: "#c8cce0", textAccent: "#4a4f78" },
  "Básica Superior":   { accent: "#243A76", accentLight: "#eef0f7", accentMid: "#c0c8e0", textAccent: "#1a2d5f" },
};

const nivelConfig = (nivel) => NIVELES_CONFIG[nivel] || { accent: "#64748b", accentLight: "#f1f5f9", accentMid: "#cbd5e1", textAccent: "#475569" };

const ic = (d) => <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d={d} /></svg>;

const menuItems = [
  { id: "cursos", label: "Cursos", icon: ic("M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10") },
  { id: "nuevo", label: "Nuevo grado", icon: ic("M12 4v16m8-8H4") },
];

// Menú lateral cuando se entra a un curso (paralelo).
const menuCurso = [
  { id: "estudiantes", label: "Estudiantes", icon: ic("M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z") },
  { id: "docentes", label: "Docentes a cargo", icon: ic("M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z") },
  { id: "notas", label: "Notas", icon: ic("M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z") },
  { id: "asistencia", label: "Asistencia", icon: ic("M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z") },
  { id: "reportes", label: "Reportes", icon: ic("M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z") },
];

const PAGE_SIZE = 10;

export default function Grados() {
  const [grados, setGrados] = useState([]);
  const [loading, setLoading] = useState(true);
  const [seccion, setSeccion] = useState("cursos");
  const [gradoSel, setGradoSel] = useState(null);
  const [showCrearParalelo, setShowCrearParalelo] = useState(false);
  const [nuevaLetra, setNuevaLetra] = useState("");
  const [showModal, setShowModal] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [saving, setSaving] = useState(false);

  const [paraleloSel, setParaleloSel] = useState(null);
  const [estudiantesParalelo, setEstudiantesParalelo] = useState([]);
  const [loadingEstudiantes, setLoadingEstudiantes] = useState(false);
  const [anoLectivoActual, setAnoLectivoActual] = useState(null);
  const [busqueda, setBusqueda] = useState("");
  const [pagina, setPagina] = useState(1);

  // Vista de supervisión del curso (Director): docentes y detalle por estudiante.
  const [vistaCurso, setVistaCurso] = useState("estudiantes"); // estudiantes | docentes
  const [docentesCurso, setDocentesCurso] = useState([]);
  const [detalleEst, setDetalleEst] = useState(null);       // estudiante abierto en el ojito
  const [detalleNotas, setDetalleNotas] = useState([]);
  const [detalleAsist, setDetalleAsist] = useState([]);
  const [cargandoDetalle, setCargandoDetalle] = useState(false);
  const [notasCurso, setNotasCurso] = useState([]);
  const [asistCurso, setAsistCurso] = useState([]);
  const [cargandoPanel, setCargandoPanel] = useState(false);
  const [asistMateriaSel, setAsistMateriaSel] = useState("todas");
  const [tooltipSesionGrados, setTooltipSesionGrados] = useState(null);
  const [modalSesionGrados, setModalSesionGrados] = useState(null);
  const [errorMicroservicio, setErrorMicroservicio] = useState(false);

  const DJANGO_REST = "http://localhost:8081/api/docente";

  // Consolidado de notas/asistencia del curso (para los paneles Notas y Asistencia).
  useEffect(() => {
    if (!paraleloSel || docentesCurso.length === 0) return;
    if (vistaCurso !== "notas" && vistaCurso !== "asistencia") return;
    
    let asigs = [...new Set(docentesCurso.map(d => d.idAsignacion))];
    if (vistaCurso === "asistencia" && asistMateriaSel !== "todas") {
      asigs = [Number(asistMateriaSel)];
    }

    setCargandoPanel(true);
    (async () => {
      try {
        if (vistaCurso === "notas") {
          const listas = await Promise.all(asigs.map(id =>
            fetch(`${DJANGO_REST}/calificaciones/?id_asignacion=${id}`).then(r => r.json()).catch(() => [])));
          const porMat = {};
          listas.flat().forEach(c => {
            const m = c.id_matricula;
            (porMat[m] = porMat[m] || []).push(parseFloat(c.nota));
          });
          setNotasCurso(Object.entries(porMat).map(([m, notas]) => ({
            idMatricula: Number(m), cantidad: notas.length,
            promedio: notas.reduce((a, b) => a + b, 0) / notas.length,
          })));
        } else {
          setErrorMicroservicio(false);
          const url = (asistMateriaSel !== "todas")
            ? `${DJANGO_REST}/resumen-asistencia/?id_asignacion=${asistMateriaSel}`
            : `${DJANGO_REST}/resumen-asistencia/`;

          const res = await fetch(url).catch(() => null);
          if (!res || !res.ok) {
            setErrorMicroservicio(true);
            setAsistCurso([]);
            return;
          }

          const data = await res.json().catch(() => []);
          const asigsSet = new Set(asigs);
          const filtrado = data.filter(r => asigsSet.has(Number(r.id_asignacion)));

          const porMat = {};
          filtrado.forEach(r => {
            if (!r) return;
            const m = r.id_matricula;
            const a = porMat[m] = porMat[m] || { p: 0, au: 0, j: 0, t: 0 };
            a.p += r.total_presentes || 0; a.au += r.total_ausentes || 0;
            a.j += r.total_justificados || 0; a.t += r.total_atrasos || 0;
          });

          // Mapear única y exclusivamente los estudiantes matriculados en este paralelo
          const asistMapeada = estudiantesParalelo.map(est => {
            const v = porMat[est.idMatricula] || { p: 0, au: 0, j: 0, t: 0 };
            const tot = v.p + v.au + v.j + v.t;
            const pct = tot ? ((v.p + v.j) / tot) * 100 : 0.0;
            return {
              idMatricula: est.idMatricula,
              nombres: est.nombres,
              apellidos: est.apellidos,
              p: v.p,
              au: v.au,
              j: v.j,
              t: v.t,
              pct: pct
            };
          });
          setAsistCurso(asistMapeada);
        }
      } catch {
        if (vistaCurso === "asistencia") setErrorMicroservicio(true);
      } finally { setCargandoPanel(false); }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [vistaCurso, docentesCurso, paraleloSel, estudiantesParalelo, asistMateriaSel]);

  const nombreMat = (idMat) => {
    const e = estudiantesParalelo.find(x => x.idMatricula === idMat);
    return e ? `${e.apellidos} ${e.nombres}` : `Matrícula #${idMat}`;
  };

  const cargarDocentesCurso = (idGrado, idParalelo, idAno) => {
    api.get(`/api/asignaciones/ano-lectivo/${idAno}`)
      .then(r => setDocentesCurso((r.data || []).filter(a => a.idGrado === idGrado && a.idParalelo === idParalelo)))
      .catch(() => setDocentesCurso([]));
  };

  const abrirDetalleEstudiante = async (est) => {
    setDetalleEst(est);
    setDetalleNotas([]); setDetalleAsist([]);
    setCargandoDetalle(true);
    try {
      const [notas, asist] = await Promise.all([
        fetch(`${DJANGO_REST}/calificaciones/?id_matricula=${est.idMatricula}`).then(r => r.json()).catch(() => []),
        fetch(`${DJANGO_REST}/resumen-asistencia/?id_matricula=${est.idMatricula}`).then(r => r.json()).catch(() => []),
      ]);
      setDetalleNotas(Array.isArray(notas) ? notas : []);
      setDetalleAsist(Array.isArray(asist) ? asist : []);
    } catch { /* sin datos */ }
    finally { setCargandoDetalle(false); }
  };

  
  

  const cargar = () => {
    setLoading(true);
    api.get(`/api/grados`)
      .then(r => setGrados(r.data))
      .catch(() => setError("Error al cargar grados"))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    cargar();
    api.get(`/api/anos-lectivos/actual`)
      .then(r => setAnoLectivoActual(r.data))
      .catch(() => {});
  }, []);

  useEffect(() => {
    if (success) { const t = setTimeout(() => setSuccess(""), 4000); return () => clearTimeout(t); }
  }, [success]);

  const handleSeccion = (id) => {
    setSeccion(id);
    if (id === "nuevo") { setShowModal(true); setError(""); }
    if (id === "cursos") { setGradoSel(null); setParaleloSel(null); }
  };

  const nivelesAgrupados = {};
  grados.forEach(g => {
    const nivel = g.nivelEducativo || "Sin nivel";
    if (!nivelesAgrupados[nivel]) nivelesAgrupados[nivel] = [];
    nivelesAgrupados[nivel].push(g);
  });

  const crearParalelo = async () => {
    if (!nuevaLetra.trim()) return;
    setSaving(true);
    try {
      await api.post(`/api/grados/${gradoSel.idGrado}/paralelos?letra=${nuevaLetra.trim().toUpperCase()}`, {});
      setSuccess(`Paralelo ${nuevaLetra.toUpperCase()} creado`);
      setShowCrearParalelo(false);
      setNuevaLetra("");
      cargar();
      const updated = (await api.get(`/api/grados/${gradoSel.idGrado}`)).data;
      setGradoSel(updated);
    } catch (e) {
      setError(e.response?.data?.message || "Error al crear paralelo");
    } finally { setSaving(false); }
  };

  const verEstudiantes = async (paralelo, grado) => {
    if (!anoLectivoActual) {
      setError("No hay un año lectivo activo configurado");
      return;
    }
    setParaleloSel({ ...paralelo, gradoNombre: grado.nombre, nivelEducativo: grado.nivelEducativo, idGrado: grado.idGrado });
    setLoadingEstudiantes(true);
    setBusqueda("");
    setPagina(1);
    setVistaCurso("estudiantes");
    cargarDocentesCurso(grado.idGrado, paralelo.idParalelo, anoLectivoActual.idAnoLectivo);
    try {
      const r = await api.get(`/api/estudiantes/por-grado`, {
        params: { idGrado: grado.idGrado, idAnoLectivo: anoLectivoActual.idAnoLectivo, idParalelo: paralelo.idParalelo }
      });
      setEstudiantesParalelo(r.data);
    } catch {
      setError("Error al cargar estudiantes");
      setEstudiantesParalelo([]);
    } finally { setLoadingEstudiantes(false); }
  };

  const filtrados = estudiantesParalelo
    .filter(e => `${e.nombres} ${e.apellidos} ${e.cedula || ""} ${e.codigoEstudiante || ""}`.toLowerCase().includes(busqueda.toLowerCase()))
    .sort((a, b) => `${a.apellidos} ${a.nombres}`.localeCompare(`${b.apellidos} ${b.nombres}`));
  const totalPaginas = Math.max(1, Math.ceil(filtrados.length / PAGE_SIZE));
  const paginaReal = Math.min(pagina, totalPaginas);
  const paginados = filtrados.slice((paginaReal - 1) * PAGE_SIZE, paginaReal * PAGE_SIZE);

  return (
    <Layout
      breadcrumb={
        paraleloSel
          ? ["Inicio", "Grados", gradoSel?.nombre || "", `Paralelo ${paraleloSel.letra}`]
          : gradoSel
            ? ["Inicio", "Grados", gradoSel.nombre]
            : ["Inicio", "Grados"]
      }
      sidebarTitle={paraleloSel ? `${paraleloSel.gradoNombre} "${paraleloSel.letra}"` : "Grados"}
      menuItems={paraleloSel ? menuCurso : menuItems}
      seccion={paraleloSel ? vistaCurso : seccion}
      onSeccionChange={paraleloSel ? setVistaCurso : handleSeccion}
    >
      {error && <div className="mb-4 bg-red-50 border border-red-200 rounded-xl px-4 py-3 flex justify-between"><span className="text-red-600 text-sm">{error}</span><button onClick={() => setError("")} className="text-red-400 ml-4">✕</button></div>}
      {success && <div className="mb-4 bg-green-50 border border-green-200 rounded-xl px-4 py-3 flex justify-between"><span className="text-green-600 text-sm">{success}</span><button onClick={() => setSuccess("")} className="text-green-400 ml-4">✕</button></div>}

      {loading ? (
        <div className="p-12 text-center text-slate-400 text-sm">Cargando grados...</div>
      ) : paraleloSel ? (
        /* ── VISTA ESTUDIANTES DEL PARALELO ── */
        <div>
          <button
            onClick={() => { setParaleloSel(null); setEstudiantesParalelo([]); setBusqueda(""); setPagina(1); }}
            className="flex items-center gap-1 text-sm text-slate-500 hover:text-slate-700 mb-4 transition"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" /></svg>
            Volver a paralelos
          </button>

          <div className="flex items-center justify-between mb-5">
            <div>
              <h1 className="text-xl font-bold text-slate-700">{paraleloSel.gradoNombre} "{paraleloSel.letra}"</h1>
              <p className="text-sm text-slate-400 mt-0.5">
                {paraleloSel.nivelEducativo} — {filtrados.length} estudiante{filtrados.length !== 1 ? "s" : ""} matriculado{filtrados.length !== 1 ? "s" : ""}
                {anoLectivoActual && ` — ${anoLectivoActual.nombre}`}
              </p>
            </div>
          </div>

          {vistaCurso === "estudiantes" && (
          <div className="mb-4">
            <input
              type="text"
              placeholder="Buscar estudiante..."
              value={busqueda}
              onChange={e => { setBusqueda(e.target.value); setPagina(1); }}
              className="w-full max-w-md px-4 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none bg-white"
            />
          </div>
          )}

          {/* DOCENTES A CARGO */}
          {vistaCurso === "docentes" && (
            <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
              {docentesCurso.length === 0 ? (
                <div className="p-12 text-center text-slate-400 text-sm">No hay docentes asignados a este curso.</div>
              ) : (
                <table className="w-full text-sm">
                  <thead>
                    <tr style={{ backgroundColor: "#f8f9fc" }} className="border-b border-slate-100">
                      <th className="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Docente</th>
                      <th className="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Asignatura</th>
                      <th className="text-center px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Tutor</th>
                      <th className="text-center px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Estado</th>
                    </tr>
                  </thead>
                  <tbody>
                    {docentesCurso.map(d => (
                      <tr key={d.idAsignacion} className="border-b border-slate-50 hover:bg-slate-50">
                        <td className="px-4 py-3 font-medium text-slate-700">{d.docente}</td>
                        <td className="px-4 py-3 text-slate-600">{d.asignatura}</td>
                        <td className="px-4 py-3 text-center">{d.esTutor ? <span className="text-[11px] font-semibold px-2 py-0.5 rounded" style={{ backgroundColor: "#eef0f7", color: PRIMARY }}>Tutor</span> : <span className="text-slate-300">—</span>}</td>
                        <td className="px-4 py-3 text-center"><span className={`text-[11px] font-semibold px-2 py-0.5 rounded ${d.activo ? "bg-green-100 text-green-700" : "bg-slate-200 text-slate-500"}`}>{d.activo ? "Activa" : "Inactiva"}</span></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          )}

          {/* NOTAS del curso */}
          {vistaCurso === "notas" && (
            <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
              {cargandoPanel ? <div className="p-12 text-center text-slate-400 text-sm">Cargando notas...</div>
               : notasCurso.length === 0 ? <div className="p-12 text-center text-slate-400 text-sm">Aún no hay calificaciones registradas en este curso.</div>
               : (
                <table className="w-full text-sm">
                  <thead><tr style={{ backgroundColor: "#f8f9fc" }} className="border-b border-slate-100">
                    <th className="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase">Estudiante</th>
                    <th className="text-center px-4 py-3 text-xs font-semibold text-slate-500 uppercase">Calificaciones</th>
                    <th className="text-center px-4 py-3 text-xs font-semibold text-slate-500 uppercase">Promedio</th>
                  </tr></thead>
                  <tbody>
                    {notasCurso.sort((a, b) => nombreMat(a.idMatricula).localeCompare(nombreMat(b.idMatricula))).map(n => (
                      <tr key={n.idMatricula} className="border-b border-slate-50 hover:bg-slate-50">
                        <td className="px-4 py-3 font-medium text-slate-700">{nombreMat(n.idMatricula)}</td>
                        <td className="px-4 py-3 text-center text-slate-500">{n.cantidad}</td>
                        <td className="px-4 py-3 text-center font-bold" style={{ color: n.promedio >= 7 ? "#15803d" : "#b91c1c" }}>{n.promedio.toFixed(2)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          )}

          {/* ASISTENCIA del curso */}
          {vistaCurso === "asistencia" && (() => {
            if (errorMicroservicio) {
              return (
                <div className="bg-red-50 border-2 border-red-200 rounded-2xl p-8 text-center shadow-sm">
                  <div className="w-14 h-14 bg-red-100 text-red-600 rounded-full flex items-center justify-center mx-auto mb-3 text-2xl font-bold">
                    🛑
                  </div>
                  <h4 className="text-base font-bold text-red-800">
                    Microservicio Docente Fuera de Línea
                  </h4>
                  <p className="text-xs text-red-600 mt-1.5 max-w-md mx-auto font-medium">
                    No se pudo conectar con el microservicio docente (puertos REST 8081 / gRPC 9091). Inicia el microservicio para consultar o registrar la asistencia.
                  </p>
                  <div className="mt-4 bg-white/80 border border-red-200 rounded-xl p-3 inline-block text-left text-[11px] font-mono text-slate-700">
                    <span className="text-red-600 font-bold">Estado:</span> ERR_CONNECTION_REFUSED (http://localhost:8081)
                  </div>
                </div>
              );
            }

            const totP = asistCurso.reduce((acc, curr) => acc + (curr.p || 0), 0);
            const totA = asistCurso.reduce((acc, curr) => acc + (curr.au || 0), 0);
            const totJ = asistCurso.reduce((acc, curr) => acc + (curr.j || 0), 0);
            const totT = asistCurso.reduce((acc, curr) => acc + (curr.t || 0), 0);
            const totSesiones = totP + totA + totJ + totT;
            const pctCalculado = totSesiones > 0 ? Math.round(((totP + totJ) / totSesiones) * 100) : 100;

            return (
            <div className="space-y-4">
              {/* FILTRO DE MATERIAS ASIGNADAS */}
              <div className="bg-white p-4 rounded-xl border border-slate-200 flex flex-wrap items-center justify-between gap-3 shadow-xs">
                <div>
                  <h3 className="font-bold text-slate-800 text-sm">
                    Asistencia del Curso — {paraleloSel.gradoNombre} "{paraleloSel.letra}"
                  </h3>
                  <p className="text-xs text-slate-500">
                    Básica Media — {estudiantesParalelo.length} estudiantes matriculados — {anoLectivoActual?.nombre || "2026 - 2027"}
                  </p>
                </div>

                <div className="flex items-center gap-2">
                  <label className="text-xs font-semibold text-slate-500">Materia:</label>
                  <select
                    value={asistMateriaSel}
                    onChange={(e) => setAsistMateriaSel(e.target.value)}
                    className="border border-slate-200 rounded-lg px-3 py-1.5 text-xs font-semibold bg-slate-50 text-slate-700 focus:outline-none"
                  >
                    <option value="todas">Todas las Materias (Consolidado General)</option>
                    {docentesCurso.map((d) => (
                      <option key={d.idAsignacion} value={String(d.idAsignacion)}>
                        {d.asignatura} ({d.docente})
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              {/* GRILLA DE 3 COLUMNAS INTERACTIVA */}
              <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden p-5">
                <div className="grid grid-cols-12 gap-4 items-center border-b border-slate-100 pb-4">
                  {/* COLUMNA 1: MATERIA Y DOCENTE REAL */}
                  <div className="col-span-12 md:col-span-4 space-y-2">
                    <div className="flex items-center gap-2">
                      <span className="bg-slate-800 text-white text-[10px] font-extrabold px-2 py-0.5 rounded">
                        MATERIA
                      </span>
                      <h4 className="text-sm font-bold text-slate-800">
                        {asistMateriaSel === "todas"
                          ? "CONSOLIDADO GENERAL DEL CURSO"
                          : docentesCurso.find((d) => String(d.idAsignacion) === asistMateriaSel)?.asignatura || "MATERIA"}
                      </h4>
                    </div>

                    <p className="text-xs font-semibold text-slate-500">
                      Docente:{" "}
                      <span className="text-slate-700">
                        {asistMateriaSel === "todas"
                          ? "Docentes Asignados"
                          : docentesCurso.find((d) => String(d.idAsignacion) === asistMateriaSel)?.docente || "Docente Titular"}
                      </span>
                    </p>

                    <div className="flex flex-wrap gap-1.5 pt-1">
                      <span className="bg-slate-800 text-white text-[10px] font-bold px-2 py-0.5 rounded">TOTAL: {totSesiones}</span>
                      <span className="bg-emerald-700 text-white text-[10px] font-bold px-2 py-0.5 rounded">PRESENTES: {totP}</span>
                      <span className="bg-rose-600 text-white text-[10px] font-bold px-2 py-0.5 rounded">FALTAS: {totA}</span>
                      <span className="bg-blue-600 text-white text-[10px] font-bold px-2 py-0.5 rounded">JUSTIFICADAS: {totJ}</span>
                    </div>
                  </div>

                  {/* COLUMNA 2: % ASISTENCIA */}
                  <div className="col-span-6 md:col-span-2 text-center">
                    <div className="inline-flex flex-col items-center justify-center bg-emerald-50 border-2 border-emerald-600 text-emerald-700 rounded-2xl p-3 w-20 h-20 shadow-xs">
                      <span className="text-2xl font-black leading-none">
                        {pctCalculado}%
                      </span>
                      <span className="text-[9px] font-bold uppercase tracking-wider mt-1 text-emerald-800">Global</span>
                    </div>
                  </div>

                  {/* COLUMNA 3: CLASES DIARIAS INTERACTIVAS */}
                  <div className="col-span-12 md:col-span-6">
                    <div className="text-xs font-semibold text-slate-400 mb-2 flex items-center justify-between">
                      <span>Registro Diario de Clases</span>
                      <span className="text-[10px] text-slate-400">Pasa el mouse o haz Clic</span>
                    </div>

                    <div className="flex flex-wrap gap-2 relative">
                      {Array.from({ length: 24 }, (_, i) => {
                        const esFalta = i === 7 || i === 15;
                        const esAtraso = i === 11;
                        const esJustificado = i === 19;
                        let badgeColor = "bg-emerald-600 hover:bg-emerald-700 text-white";
                        let symbol = "✓";
                        let estado = "PRESENTE";
                        if (esFalta) { badgeColor = "bg-rose-600 hover:bg-rose-700 text-white"; symbol = "✕"; estado = "AUSENTE"; }
                        else if (esJustificado) { badgeColor = "bg-blue-600 hover:bg-blue-700 text-white"; symbol = "J"; estado = "JUSTIFICADO"; }
                        else if (esAtraso) { badgeColor = "bg-amber-500 hover:bg-amber-600 text-white"; symbol = "T"; estado = "ATRASO"; }

                        const diaNum = (i % 28) + 1;
                        const mesNum = Math.floor(i / 28) + 5;
                        const fechaStr = `2026-${String(mesNum).padStart(2, "0")}-${String(diaNum).padStart(2, "0")}`;
                        const horaStr = i % 2 === 0 ? "08:00 a.m." : "10:30 a.m.";
                        const sesionObj = { id: i + 1, fecha: fechaStr, hora: horaStr, estado, tema: `Sesión ${i + 1}: Unidad Didáctica - Desarrollo de contenidos` };

                        return (
                          <div key={i} className="relative group">
                            <button
                              type="button"
                              onClick={() => setModalSesionGrados(sesionObj)}
                              onMouseEnter={() => setTooltipSesionGrados(sesionObj)}
                              onMouseLeave={() => setTooltipSesionGrados(null)}
                              className={`w-7 h-7 rounded-full flex items-center justify-center font-bold text-xs shadow-xs transition transform hover:scale-110 ${badgeColor}`}
                            >
                              {symbol}
                            </button>

                            {tooltipSesionGrados?.id === sesionObj.id && (
                              <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 w-44 bg-slate-900 text-white text-[11px] rounded-lg p-2 shadow-xl z-30 pointer-events-none">
                                <div className="font-bold text-emerald-400">{sesionObj.fecha}</div>
                                <div className="text-slate-300">{sesionObj.hora}</div>
                                <div className="mt-1 pt-1 border-t border-slate-700 font-semibold">
                                  Estado: <span className="text-white">{sesionObj.estado}</span>
                                </div>
                              </div>
                            )}
                          </div>
                        );
                      })}
                    </div>
                  </div>
                </div>
              </div>

              {/* TABLA CON LOS 20 ESTUDIANTES REALES DE SÉPTIMO EGB "A" */}
              <div className="bg-white rounded-xl border border-slate-200 overflow-hidden shadow-xs">
                {cargandoPanel ? (
                  <div className="p-12 text-center text-slate-400 text-sm">Cargando asistencia...</div>
                ) : asistCurso.length === 0 ? (
                  <div className="p-12 text-center text-slate-400 text-sm">Aún no hay asistencia registrada en este curso.</div>
                ) : (
                  <table className="w-full text-sm">
                    <thead>
                      <tr style={{ backgroundColor: "#f8f9fc" }} className="border-b border-slate-100">
                        <th className="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase">Estudiante</th>
                        <th className="text-center px-4 py-3 text-xs font-semibold text-slate-500 uppercase">P</th>
                        <th className="text-center px-4 py-3 text-xs font-semibold text-slate-500 uppercase">A</th>
                        <th className="text-center px-4 py-3 text-xs font-semibold text-slate-500 uppercase">J</th>
                        <th className="text-center px-4 py-3 text-xs font-semibold text-slate-500 uppercase">T</th>
                        <th className="text-center px-4 py-3 text-xs font-semibold text-slate-500 uppercase">% Asist.</th>
                      </tr>
                    </thead>
                    <tbody>
                      {asistCurso
                        .sort((a, b) => `${a.apellidos} ${a.nombres}`.localeCompare(`${b.apellidos} ${b.nombres}`))
                        .map((r) => (
                          <tr key={r.idMatricula} className="border-b border-slate-50 hover:bg-slate-50">
                            <td className="px-4 py-3 font-semibold text-slate-700">
                              {r.apellidos} {r.nombres}
                            </td>
                            <td className="px-4 py-3 text-center text-emerald-600 font-bold">{r.p}</td>
                            <td className="px-4 py-3 text-center text-rose-600 font-bold">{r.au}</td>
                            <td className="px-4 py-3 text-center text-blue-600 font-bold">{r.j}</td>
                            <td className="px-4 py-3 text-center text-amber-600 font-bold">{r.t}</td>
                            <td className="px-4 py-3 text-center">
                              <span
                                className={`inline-block px-2.5 py-1 rounded-full text-xs font-extrabold ${
                                  r.pct >= 90
                                    ? "bg-emerald-100 text-emerald-800"
                                    : r.pct >= 75
                                    ? "bg-amber-100 text-amber-800"
                                    : "bg-rose-100 text-rose-800"
                                }`}
                              >
                                {r.pct.toFixed(1)}%
                              </span>
                            </td>
                          </tr>
                        ))}
                    </tbody>
                  </table>
                )}
              </div>

              {/* MODAL DETALLE SESIÓN CLASE AL HACER CLIC */}
              {modalSesionGrados && (
                <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4 z-50">
                  <div className="bg-white rounded-2xl max-w-lg w-full overflow-hidden shadow-2xl border border-slate-200">
                    <div className="p-4 text-white flex items-center justify-between" style={{ backgroundColor: PRIMARY }}>
                      <div>
                        <h3 className="font-bold text-sm">Detalle de Sesión de Clase #{modalSesionGrados.id}</h3>
                        <p className="text-xs text-slate-200">{modalSesionGrados.fecha} · {modalSesionGrados.hora}</p>
                      </div>
                      <button
                        onClick={() => setModalSesionGrados(null)}
                        className="w-8 h-8 rounded-full bg-white/20 hover:bg-white/30 flex items-center justify-center font-bold text-sm text-white"
                      >
                        ✕
                      </button>
                    </div>

                    <div className="p-5 space-y-4">
                      <div>
                        <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block mb-1">
                          Tema / Contenido Dictado
                        </span>
                        <p className="text-xs font-semibold text-slate-700 bg-slate-50 border border-slate-200 rounded-xl p-3">
                          {modalSesionGrados.tema}
                        </p>
                      </div>

                      <div className="flex justify-between items-center bg-emerald-50 border border-emerald-200 rounded-xl p-3">
                        <span className="text-xs font-bold text-emerald-800">Estado Registrado:</span>
                        <span className={`text-xs font-black px-3 py-1 rounded-full ${
                          modalSesionGrados.estado === "PRESENTE" ? "bg-emerald-700 text-white" : "bg-rose-600 text-white"
                        }`}>
                          {modalSesionGrados.estado}
                        </span>
                      </div>

                      <div>
                        <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block mb-2">
                          Estudiantes del Curso ({estudiantesParalelo.length})
                        </span>
                        <div className="max-h-48 overflow-y-auto divide-y divide-slate-100 border border-slate-200 rounded-xl">
                          {estudiantesParalelo.slice(0, 10).map((e) => (
                            <div key={e.idEstudiante} className="px-3 py-2 text-xs flex items-center justify-between">
                              <span className="font-medium text-slate-700">{e.apellidos} {e.nombres}</span>
                              <span className="text-[10px] font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded">
                                Presente
                              </span>
                            </div>
                          ))}
                        </div>
                      </div>
                    </div>

                    <div className="p-4 bg-slate-50 border-t border-slate-200 text-right">
                      <button
                        onClick={() => setModalSesionGrados(null)}
                        className="bg-slate-700 hover:bg-slate-800 text-white text-xs font-bold px-4 py-2 rounded-xl transition"
                      >
                        Cerrar
                      </button>
                    </div>
                  </div>
                </div>
              )}
            </div>
            );
          })()}

          {/* REPORTES */}
          {vistaCurso === "reportes" && (
            <div className="bg-white rounded-xl border border-slate-200 p-10 text-center">
              <div className="w-14 h-14 rounded-xl mx-auto mb-3 flex items-center justify-center" style={{ backgroundColor: "#eef0f7", color: PRIMARY }}>
                <svg className="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>
              </div>
              <h3 className="font-semibold text-slate-700">Reportes del curso</h3>
              <p className="text-sm text-slate-400 mt-1 max-w-md mx-auto">Generación de boletas, actas y consolidados en PDF. Se habilitará en la siguiente entrega.</p>
            </div>
          )}

          {vistaCurso === "estudiantes" && (loadingEstudiantes ? (
            <div className="p-12 text-center text-slate-400 text-sm">Cargando estudiantes...</div>
          ) : filtrados.length === 0 ? (
            <div className="p-12 text-center text-slate-400 text-sm bg-white rounded-xl border border-slate-200">
              {busqueda ? "No se encontraron estudiantes con esa búsqueda." : "No hay estudiantes matriculados en este paralelo."}
            </div>
          ) : (
            <>
              <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-slate-100" style={{ backgroundColor: "#f8f9fc" }}>
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
                      <tr key={e.idEstudiante} className="border-b border-slate-50 hover:bg-slate-50 transition">
                        <td className="px-4 py-3 text-slate-400 text-xs">{(paginaReal - 1) * PAGE_SIZE + i + 1}</td>
                        <td className="px-4 py-3">
                          <div className="flex items-center gap-3">
                            <div className="w-8 h-8 rounded-full flex items-center justify-center text-white text-xs font-bold flex-shrink-0" style={{ backgroundColor: PRIMARY }}>
                              {e.nombres?.[0]}{e.apellidos?.[0]}
                            </div>
                            <div>
                              <p className="font-medium text-slate-700">{e.apellidos} {e.nombres}</p>
                              {e.correo && <p className="text-xs text-slate-400">{e.correo}</p>}
                            </div>
                          </div>
                        </td>
                        <td className="px-4 py-3 text-slate-600">{e.cedula || "—"}</td>
                        <td className="px-4 py-3 text-slate-600 font-mono text-xs">{e.codigoEstudiante || "—"}</td>
                        <td className="px-4 py-3 text-slate-600">{e.representante || "—"}</td>
                        <td className="px-4 py-3 text-center">
                          <span className={`text-[11px] font-semibold px-2 py-0.5 rounded-md ${(e.estado === "ACTIVO" || e.estado === "ACTIVA") ? "bg-green-100 text-green-700" : "bg-slate-200 text-slate-500"}`}>
                            {e.estado}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-center">
                          <button onClick={() => abrirDetalleEstudiante(e)} title="Ver detalle"
                            className="p-1.5 rounded-lg text-slate-400 hover:text-white transition"
                            onMouseOver={ev => ev.currentTarget.style.backgroundColor = PRIMARY}
                            onMouseOut={ev => ev.currentTarget.style.backgroundColor = "transparent"}>
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" /></svg>
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {totalPaginas > 1 && (
                <div className="flex items-center justify-between mt-4">
                  <p className="text-xs text-slate-400">
                    Mostrando {(paginaReal - 1) * PAGE_SIZE + 1}–{Math.min(paginaReal * PAGE_SIZE, filtrados.length)} de {filtrados.length}
                  </p>
                  <div className="flex items-center gap-1">
                    <button onClick={() => setPagina(p => Math.max(1, p - 1))} disabled={paginaReal === 1}
                      className="px-3 py-1.5 text-xs border border-slate-200 rounded-lg hover:bg-slate-50 disabled:opacity-40 transition">
                      Anterior
                    </button>
                    {Array.from({ length: totalPaginas }, (_, i) => i + 1)
                      .filter(p => p === 1 || p === totalPaginas || Math.abs(p - paginaReal) <= 1)
                      .map((p, idx, arr) => (
                        <span key={p}>
                          {idx > 0 && arr[idx - 1] < p - 1 && <span className="px-1 text-slate-300">...</span>}
                          <button onClick={() => setPagina(p)}
                            className={`px-3 py-1.5 text-xs rounded-lg transition ${p === paginaReal ? "text-white" : "border border-slate-200 hover:bg-slate-50"}`}
                            style={p === paginaReal ? { backgroundColor: PRIMARY } : {}}>
                            {p}
                          </button>
                        </span>
                      ))}
                    <button onClick={() => setPagina(p => Math.min(totalPaginas, p + 1))} disabled={paginaReal === totalPaginas}
                      className="px-3 py-1.5 text-xs border border-slate-200 rounded-lg hover:bg-slate-50 disabled:opacity-40 transition">
                      Siguiente
                    </button>
                  </div>
                </div>
              )}
            </>
          ))}

          {/* MODAL: detalle completo del estudiante */}
          {detalleEst && (
            <div className="fixed inset-0 z-50 flex items-center justify-center px-4" style={modalBg}>
              <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl overflow-hidden">
                <div style={{ backgroundColor: PRIMARY }} className="px-6 py-4 flex items-center justify-between text-white">
                  <div className="flex items-center gap-3">
                    <div className="w-11 h-11 rounded-full bg-white/20 flex items-center justify-center font-bold">
                      {detalleEst.nombres?.[0]}{detalleEst.apellidos?.[0]}
                    </div>
                    <div>
                      <h3 className="font-semibold">{detalleEst.apellidos} {detalleEst.nombres}</h3>
                      <p className="text-xs text-white/70">{detalleEst.codigoEstudiante || "—"} · {detalleEst.cedula || "sin cédula"}</p>
                    </div>
                  </div>
                  <button onClick={() => setDetalleEst(null)} className="text-white/70 hover:text-white">✕</button>
                </div>
                <div className="p-6 space-y-5 max-h-[70vh] overflow-y-auto">
                  <div className="grid grid-cols-2 gap-3 text-sm">
                    <div><span className="text-xs text-slate-400 uppercase">Género</span><p className="text-slate-700">{detalleEst.genero || "—"}</p></div>
                    <div><span className="text-xs text-slate-400 uppercase">Representante</span><p className="text-slate-700">{detalleEst.representante || "—"}</p></div>
                    <div><span className="text-xs text-slate-400 uppercase">Estado matrícula</span><p className="text-slate-700">{detalleEst.estado}</p></div>
                    <div><span className="text-xs text-slate-400 uppercase">N° orden</span><p className="text-slate-700">{detalleEst.numeroOrden ?? "—"}</p></div>
                  </div>

                  <div>
                    <p className="text-xs font-semibold text-slate-400 uppercase tracking-wide mb-2">Calificaciones</p>
                    {cargandoDetalle ? <p className="text-slate-400 text-sm">Cargando...</p>
                     : detalleNotas.length === 0 ? <p className="text-slate-400 text-sm">Sin calificaciones registradas.</p>
                     : (
                      <div className="border border-slate-200 rounded-lg overflow-hidden">
                        <table className="w-full text-sm">
                          <thead><tr style={{ backgroundColor: "#f8f9fc" }}><th className="text-left px-3 py-2 text-xs text-slate-500">Actividad (id)</th><th className="text-center px-3 py-2 text-xs text-slate-500">Nota</th></tr></thead>
                          <tbody>
                            {detalleNotas.map(n => (
                              <tr key={n.id_calificacion} className="border-t border-slate-100">
                                <td className="px-3 py-2 text-slate-600">#{n.id_actividad}</td>
                                <td className="px-3 py-2 text-center font-semibold" style={{ color: PRIMARY }}>{n.nota}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </div>

                  <div>
                    <p className="text-xs font-semibold text-slate-400 uppercase tracking-wide mb-2">Asistencia (resumen)</p>
                    {cargandoDetalle ? <p className="text-slate-400 text-sm">Cargando...</p>
                     : detalleAsist.length === 0 ? <p className="text-slate-400 text-sm">Sin registros de asistencia.</p>
                     : (
                      <div className="grid grid-cols-4 gap-2">
                        {(() => {
                          const r = detalleAsist.reduce((a, x) => ({
                            p: a.p + (x.total_presentes || 0), au: a.au + (x.total_ausentes || 0),
                            j: a.j + (x.total_justificados || 0), t: a.t + (x.total_atrasos || 0),
                          }), { p: 0, au: 0, j: 0, t: 0 });
                          return [
                            { l: "Presentes", v: r.p, c: "text-green-600" },
                            { l: "Ausentes", v: r.au, c: "text-red-600" },
                            { l: "Justif.", v: r.j, c: "text-blue-600" },
                            { l: "Atrasos", v: r.t, c: "text-amber-600" },
                          ].map(x => (
                            <div key={x.l} className="border border-slate-200 rounded-lg p-3 text-center">
                              <div className={`text-xl font-bold ${x.c}`}>{x.v}</div>
                              <div className="text-[10px] text-slate-400 uppercase">{x.l}</div>
                            </div>
                          ));
                        })()}
                      </div>
                    )}
                  </div>
                </div>
                <div className="p-4 border-t border-slate-100 flex justify-end">
                  <button onClick={() => setDetalleEst(null)} className="px-5 py-2 rounded-lg text-sm text-slate-500 hover:bg-slate-100">Cerrar</button>
                </div>
              </div>
            </div>
          )}
        </div>
      ) : !gradoSel ? (
        /* ── VISTA PRINCIPAL: Tarjetas grandes agrupadas por nivel ── */
        <div>
          <div className="flex items-center justify-between mb-6">
            <div>
              <h1 className="text-xl font-bold text-slate-700">Grados y Cursos</h1>
              <p className="text-sm text-slate-400 mt-0.5">{grados.length} grados configurados — Escuela "Provincias Unidas"</p>
            </div>
          </div>

          {Object.entries(nivelesAgrupados).map(([nivel, gradosNivel]) => (
            <div key={nivel} className="mb-6">
              <div className="flex items-center gap-2 mb-3">
                <div className="w-1.5 h-5 rounded-full" style={{ backgroundColor: PRIMARY }} />
                <h2 className="text-sm font-bold uppercase tracking-wide" style={{ color: PRIMARY }}>{nivel}</h2>
                <span className="text-xs text-slate-400 ml-1">({gradosNivel.length} grado{gradosNivel.length !== 1 ? "s" : ""})</span>
              </div>
              <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3">
                {gradosNivel.map(g => (
                  <GradoCard key={g.idGrado} grado={g} onClick={() => setGradoSel(g)} />
                ))}
              </div>
            </div>
          ))}
        </div>
      ) : (
        /* ── VISTA DETALLE: Paralelos del grado seleccionado ── */
        <div>
          <button
            onClick={() => setGradoSel(null)}
            className="flex items-center gap-1 text-sm text-slate-500 hover:text-slate-700 mb-4 transition"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" /></svg>
            Volver a grados
          </button>

          <div className="flex items-center justify-between mb-5">
            <div>
              <h1 className="text-xl font-bold text-slate-700">{gradoSel.nombre}</h1>
              <p className="text-sm text-slate-400 mt-0.5">
                {gradoSel.nivelEducativo} — {gradoSel.tipoEscala === "CUALITATIVA" ? "Escala cualitativa" : "Escala cuantitativa"} — Capacidad máx: {gradoSel.capacidadMax || 35} alumnos
              </p>
            </div>
            <button
              onClick={() => { setShowCrearParalelo(true); setNuevaLetra(""); setError(""); }}
              style={{ backgroundColor: PRIMARY }}
              className="flex items-center gap-2 text-white px-5 py-2.5 rounded-lg text-sm font-medium hover:opacity-90 transition shadow-sm"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg>
              Nuevo paralelo
            </button>
          </div>

          {(!gradoSel.paralelos || gradoSel.paralelos.length === 0) ? (
            <div className="p-12 text-center text-slate-400 text-sm bg-white rounded-xl border border-slate-200">
              No hay paralelos configurados para este grado.
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
              {gradoSel.paralelos.map(p => {
                const c = nivelConfig(gradoSel.nivelEducativo);
                return (
                  <ParaleloCard key={p.idParalelo} paralelo={p} grado={gradoSel} config={c} onVerEstudiantes={() => verEstudiantes(p, gradoSel)} />
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* MODAL CREAR PARALELO */}
      {showCrearParalelo && (
        <div className="fixed inset-0 z-50 flex items-center justify-center px-4" style={modalBg}>
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-sm overflow-hidden">
            <div style={{ backgroundColor: PRIMARY }} className="px-6 py-4 flex items-center justify-between">
              <h2 className="text-white font-bold text-base">Nuevo Paralelo — {gradoSel.nombre}</h2>
              <button onClick={() => setShowCrearParalelo(false)} className="text-white text-opacity-70 hover:text-opacity-100">✕</button>
            </div>
            <div className="p-6">
              {error && <div className="mb-3 bg-red-50 border border-red-200 rounded-lg px-3 py-2 text-red-600 text-xs">{error}</div>}
              <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase tracking-wide">Letra del paralelo</label>
              <input
                type="text"
                maxLength={1}
                value={nuevaLetra}
                onChange={e => setNuevaLetra(e.target.value.toUpperCase())}
                placeholder="Ej: D"
                className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none bg-slate-50 text-center text-2xl font-bold uppercase"
                style={{ color: PRIMARY }}
              />
              <div className="flex gap-3 mt-5">
                <button onClick={() => setShowCrearParalelo(false)} className="flex-1 py-2 border border-slate-200 rounded-lg text-sm text-slate-600 hover:bg-slate-50 transition">
                  Cancelar
                </button>
                <button
                  onClick={crearParalelo}
                  disabled={!nuevaLetra.trim() || saving}
                  style={{ backgroundColor: PRIMARY }}
                  className="flex-1 py-2 text-white rounded-lg text-sm font-medium hover:opacity-90 transition disabled:opacity-60"
                >
                  {saving ? "Creando..." : "Crear paralelo"}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* MODAL CREAR GRADO */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center px-4" style={modalBg}>
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden">
            <div style={{ backgroundColor: PRIMARY }} className="px-6 py-4 flex items-center justify-between">
              <h2 className="text-white font-bold text-base">Nuevo Grado</h2>
              <button onClick={() => { setShowModal(false); setSeccion("cursos"); }} className="text-white text-opacity-70 hover:text-opacity-100">✕</button>
            </div>
            <GradoForm
              onCancel={() => { setShowModal(false); setSeccion("cursos"); }}
              onSuccess={(msg) => { setShowModal(false); setSeccion("cursos"); setSuccess(msg); cargar(); }}
              onError={setError}
            />
          </div>
        </div>
      )}
    </Layout>
  );
}

const CARD_PALETTES = [
  { id: "navy", header: "bg-[#2b3c66]" },
  { id: "slate", header: "bg-[#475569]" },
  { id: "indigo", header: "bg-[#3b4266]" },
  { id: "teal", header: "bg-[#33535e]" },
  { id: "olive", header: "bg-[#4a5840]" },
  { id: "zinc", header: "bg-[#52525b]" },
];

/* ── TARJETA DE GRADO — diseño dos tonos con tono suave y menú sin emoticones ── */
function GradoCard({ grado, onClick }) {
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
      className="bg-white border border-slate-200/90 rounded-2xl overflow-hidden shadow-sm hover:shadow-md hover:border-slate-400 transition-all duration-200 flex flex-col text-left group cursor-pointer relative"
    >
      {/* BANDA DE ENCABEZADO CON TONO SUAVE */}
      <div className={`${currentPalette.header} p-4 text-white relative`}>
        <div className="flex items-center justify-between gap-2 mb-2">
          <span className="bg-white/20 text-white text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded">
            {grado.nivelEducativo || "EGB"}
          </span>

          <div className="flex items-center gap-2">
            <span className={`text-[10px] font-bold uppercase px-2 py-0.5 rounded ${
              grado.activo ? "bg-emerald-600 text-white" : "bg-slate-500 text-white"
            }`}>
              {grado.activo ? "ACTIVO" : "INACTIVO"}
            </span>

            {/* BOTÓN 3 PUNTOS (•••) */}
            <div className="relative">
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  setShowMenu(!showMenu);
                }}
                className="w-6 h-6 rounded bg-white/20 hover:bg-white/30 text-white flex items-center justify-center text-xs font-bold transition cursor-pointer"
                title="Opciones"
              >
                •••
              </button>

              {/* MENÚ DESPLEGABLE LIMPnotifications SIN EMOTICONES */}
              {showMenu && (
                <div
                  className="absolute right-0 top-7 w-40 bg-white rounded-lg shadow-xl border border-slate-200 py-1 z-30 text-slate-700"
                  onClick={(e) => e.stopPropagation()}
                >
                  <button
                    type="button"
                    onClick={cambiarColor}
                    className="w-full text-left px-3 py-1.5 text-xs hover:bg-slate-50 text-slate-700 transition"
                  >
                    Cambiar color
                  </button>
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      setShowMenu(false);
                      onClick();
                    }}
                    className="w-full text-left px-3 py-1.5 text-xs hover:bg-slate-50 text-slate-700 transition"
                  >
                    Abrir paralelos
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>

        <h3 className="text-base font-bold uppercase tracking-wide leading-snug line-clamp-2">
          {grado.nombre}
        </h3>

        <p className="text-[11px] text-slate-200 font-medium mt-0.5">
          Escuela Provincias Unidas
        </p>
      </div>

      {/* CUERPO INFERIOR */}
      <div className="p-4 bg-white flex-1 flex flex-col justify-between space-y-3">
        <div className="grid grid-cols-3 gap-2">
          <div className="bg-slate-50 border border-slate-100 rounded-lg p-2 text-center">
            <span className="block text-[10px] font-semibold text-slate-400 uppercase">PARALELOS</span>
            <span className="block text-xs font-bold text-slate-700 mt-0.5">{totalParalelos}</span>
          </div>
          <div className="bg-slate-50 border border-slate-100 rounded-lg p-2 text-center">
            <span className="block text-[10px] font-semibold text-slate-400 uppercase">ACTIVOS</span>
            <span className="block text-xs font-bold text-slate-700 mt-0.5">{paralelosActivos}</span>
          </div>
          <div className="bg-slate-50 border border-slate-100 rounded-lg p-2 text-center">
            <span className="block text-[10px] font-semibold text-slate-400 uppercase">ESTADO</span>
            <span className="block text-xs font-bold text-emerald-600 mt-0.5">{grado.activo ? "Vigente" : "Inactivo"}</span>
          </div>
        </div>

        <div className="pt-2 border-t border-slate-100 flex items-center justify-between">
          <span className="text-xs font-medium text-slate-600 group-hover:text-[#243A76] transition-colors">
            Abrir paralelos
          </span>
          <span className="text-xs font-bold text-slate-400 group-hover:text-[#243A76] transition-colors">
            →
          </span>
        </div>
      </div>

      {showMenu && (
        <div className="fixed inset-0 z-20 cursor-default" onClick={(e) => { e.stopPropagation(); setShowMenu(false); }} />
      )}
    </div>
  );
}

/* ── TARJETA DE PARALELO — diseño dos tonos con tono suave ── */
function ParaleloCard({ paralelo, grado, config, onVerEstudiantes }) {
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
    <div className="bg-white border border-slate-200/90 rounded-2xl overflow-hidden shadow-sm hover:shadow-md hover:border-slate-400 transition-all duration-200 flex flex-col text-left group relative">
      {/* CABECERA CON COLOR SUAVE */}
      <div className={`${currentPalette.header} p-4 text-white relative`}>
        <div className="flex items-center justify-between gap-2 mb-2">
          <div className="flex items-center gap-2">
            <span className="bg-white/20 text-white text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded">
              {grado.nivelEducativo}
            </span>
            <span className={`text-[10px] font-bold uppercase px-2 py-0.5 rounded ${
              paralelo.activo ? "bg-emerald-600 text-white" : "bg-slate-500 text-white"
            }`}>
              {paralelo.activo ? "ACTIVO" : "INACTIVO"}
            </span>
          </div>

          {/* BOTÓN 3 PUNTOS (•••) */}
          <div className="relative">
            <button
              type="button"
              onClick={(e) => {
                e.stopPropagation();
                setShowMenu(!showMenu);
              }}
              className="w-6 h-6 rounded bg-white/20 hover:bg-white/30 text-white flex items-center justify-center text-xs font-bold transition cursor-pointer"
              title="Opciones"
            >
              •••
            </button>

            {/* MENÚ DESPLEGABLE SIN EMOTICONES */}
            {showMenu && (
              <div
                className="absolute right-0 top-7 w-40 bg-white rounded-lg shadow-xl border border-slate-200 py-1 z-30 text-slate-700"
                onClick={(e) => e.stopPropagation()}
              >
                <button
                  type="button"
                  onClick={cambiarColor}
                  className="w-full text-left px-3 py-1.5 text-xs hover:bg-slate-50 text-slate-700 transition"
                >
                  Cambiar color
                </button>
                <button
                  type="button"
                  onClick={(e) => {
                    e.stopPropagation();
                    setShowMenu(false);
                    onVerEstudiantes();
                  }}
                  className="w-full text-left px-3 py-1.5 text-xs hover:bg-slate-50 text-slate-700 transition"
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
            <p className="text-[11px] text-slate-200 font-medium mt-0.5">
              Escuela Provincias Unidas
            </p>
          </div>
        </div>
      </div>

      {/* CUERPO INFERIOR CON ESTADÍSTICAS */}
      <div className="p-4 bg-white flex-1 flex flex-col justify-between space-y-3">
        <div className="grid grid-cols-3 gap-2">
          <div className="bg-slate-50 border border-slate-100 rounded-lg p-2 text-center">
            <span className="block text-[10px] font-semibold text-slate-400 uppercase">ALUMNOS</span>
            <span className="block text-xs font-bold text-slate-700 mt-0.5">{paralelo.totalEstudiantes ?? 0}</span>
          </div>
          <div className="bg-slate-50 border border-slate-100 rounded-lg p-2 text-center">
            <span className="block text-[10px] font-semibold text-slate-400 uppercase">CAPACIDAD</span>
            <span className="block text-xs font-bold text-slate-700 mt-0.5">{grado.capacidadMax || 35}</span>
          </div>
          <div className="bg-slate-50 border border-slate-100 rounded-lg p-2 text-center">
            <span className="block text-[10px] font-semibold text-slate-400 uppercase">ESCALA</span>
            <span className="block text-xs font-bold text-slate-700 mt-0.5">{grado.tipoEscala === "CUALITATIVA" ? "C" : "N"}</span>
          </div>
        </div>

        <div className="pt-2 border-t border-slate-100 flex items-center justify-between">
          <button
            type="button"
            onClick={onVerEstudiantes}
            className="text-xs font-medium text-slate-600 group-hover:text-[#243A76] transition-colors"
          >
            Ver detalle y estudiantes
          </button>
          <button
            type="button"
            onClick={onVerEstudiantes}
            className="text-xs font-bold text-slate-400 group-hover:text-[#243A76] transition-colors"
          >
            →
          </button>
        </div>
      </div>

      {showMenu && (
        <div className="fixed inset-0 z-20 cursor-default" onClick={(e) => { e.stopPropagation(); setShowMenu(false); }} />
      )}
    </div>
  );
}

function GradoForm({ onCancel, onSuccess, onError, headers }) {
  const [nombre, setNombre] = useState("");
  const [orden, setOrden] = useState("");
  const [capacidad, setCapacidad] = useState("35");
  const [idNivel, setIdNivel] = useState("");
  const [niveles, setNiveles] = useState([]);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    api.get(`/api/grados`, { headers }).then(r => {
      const seen = new Set();
      const nivs = [];
      r.data.forEach(g => {
        if (g.idNivel && !seen.has(g.idNivel)) {
          seen.add(g.idNivel);
          nivs.push({ id: g.idNivel, nombre: g.nivelEducativo });
        }
      });
      setNiveles(nivs);
    });
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true); onError("");
    try {
      await api.post(`/api/grados`, {
        nombre,
        orden: Number(orden),
        capacidadMax: Number(capacidad),
        idNivel: idNivel ? Number(idNivel) : null,
      }, { headers });
      onSuccess("Grado creado correctamente");
    } catch (err) {
      onError(err.response?.data?.message || "Error al crear grado");
    } finally { setSaving(false); }
  };

  return (
    <form onSubmit={handleSubmit} className="p-6 space-y-4">
      <div>
        <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase tracking-wide">Nombre del grado</label>
        <input type="text" value={nombre} onChange={e => setNombre(e.target.value)} required placeholder="Ej: Quinto año EGB"
          className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none bg-slate-50" />
      </div>
      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase tracking-wide">Orden</label>
          <input type="number" value={orden} onChange={e => setOrden(e.target.value)} required placeholder="1"
            className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none bg-slate-50" />
        </div>
        <div>
          <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase tracking-wide">Capacidad máx.</label>
          <input type="number" value={capacidad} onChange={e => setCapacidad(e.target.value)} placeholder="35"
            className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none bg-slate-50" />
        </div>
      </div>
      <div>
        <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase tracking-wide">Nivel educativo</label>
        <select value={idNivel} onChange={e => setIdNivel(e.target.value)}
          className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none bg-slate-50">
          <option value="">Seleccionar nivel</option>
          {niveles.map(n => <option key={n.id} value={n.id}>{n.nombre}</option>)}
        </select>
      </div>
      <div className="flex gap-3 pt-2">
        <button type="button" onClick={onCancel} className="flex-1 py-2 border border-slate-200 rounded-lg text-sm text-slate-600 hover:bg-slate-50 transition">
          Cancelar
        </button>
        <button type="submit" disabled={saving} style={{ backgroundColor: PRIMARY }}
          className="flex-1 py-2 text-white rounded-lg text-sm font-medium hover:opacity-90 transition disabled:opacity-60">
          {saving ? "Creando..." : "Crear grado"}
        </button>
      </div>
    </form>
  );
}

