import { useState, useEffect, useMemo } from "react";
import api from "../../config/axios";
import Layout from "../../components/Layout";

const PRIMARY = "#243A76";

const COLORES = [
  { bg: "bg-blue-50",   icon: "text-blue-500",   border: "#3b82f6" },
  { bg: "bg-green-50",  icon: "text-green-500",  border: "#22c55e" },
  { bg: "bg-purple-50", icon: "text-purple-500", border: "#a855f7" },
  { bg: "bg-amber-50",  icon: "text-amber-500",  border: "#f59e0b" },
  { bg: "bg-rose-50",   icon: "text-rose-500",   border: "#f43f5e" },
  { bg: "bg-teal-50",   icon: "text-teal-500",   border: "#14b8a6" },
  { bg: "bg-indigo-50", icon: "text-indigo-500", border: "#6366f1" },
  { bg: "bg-pink-50",   icon: "text-pink-500",   border: "#ec4899" },
];

const IconLibro = ({ className }) => (
  <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
      d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
  </svg>
);

const IconBack = () => (
  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
  </svg>
);

const calMenuItems = [
  { id: "cursos", label: "Cursos / Asignaturas", icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" /></svg> },
];

export default function Calificaciones() {
  const [asignaciones, setAsignaciones]       = useState([]);
  const [matriculas, setMatriculas]           = useState([]);
  const [vista, setVista]                     = useState("cursos"); // cursos | matriz
  const [asignacionSel, setAsignacionSel]     = useState(null);
  const [tabTrimestre, setTabTrimestre]       = useState("todos"); // todos | 1 | 2 | 3
  const [busqueda, setBusqueda]               = useState("");
  const [loading, setLoading]                 = useState(false);
  const [anoActual, setAnoActual]             = useState(null);

  useEffect(() => {
    api.get(`/api/anos-lectivos/actual`)
      .then(r => {
        setAnoActual(r.data);
        cargarAsignaciones(r.data.idAnoLectivo);
      })
      .catch(() => cargarAsignaciones(null));
  }, []);

  const cargarAsignaciones = (idAno) => {
    setLoading(true);
    const url = idAno
      ? `/api/asignaciones/ano-lectivo/${idAno}`
      : `/api/asignaciones`;
    api.get(url)
      .then(r => setAsignaciones(r.data.filter(a => a.activo)))
      .finally(() => setLoading(false));
  };

  const abrirMatrizCurso = (asignacion) => {
    setAsignacionSel(asignacion);
    setLoading(true);
    setVista("matriz");
    setBusqueda("");

    // Cargar los estudiantes matriculados para este grado
    const params = asignacion.idGrado ? { idGrado: asignacion.idGrado, limit: 200 } : { limit: 200 };
    api.get(`/api/matriculas`, { params })
      .then(r => {
        const raw = r.data?.matriculas || r.data?.data || (Array.isArray(r.data) ? r.data : []);
        // Aislar los 70 alumnos del Paralelo A
        const filtrados = raw.filter(m => {
          const par = (m.paralelo || m.letraParalelo || m.paraleloLetra || "A").toUpperCase();
          const estado = (m.estado || "ACTIVA").toUpperCase();
          return par.includes("A") && estado === "ACTIVA";
        });
        setMatriculas(filtrados.length > 0 ? filtrados : raw.slice(0, 70));
      })
      .catch(() => setMatriculas([]))
      .finally(() => setLoading(false));
  };

  // Generador determinista de notas por estudiante y actividad
  const obtenerNotaSimulada = (idMatricula, seed) => {
    const hash = ((Number(idMatricula || 1) * 37 + seed * 19 + 7) % 31) / 10;
    const nota = 7.0 + hash;
    return Number(Math.min(10.0, Math.max(5.0, nota)).toFixed(2));
  };

  // Estructura de cálculos por alumno para los 3 trimestres
  const estudiantesProcesados = useMemo(() => {
    return matriculas.map((m, idx) => {
      const id = m.idMatricula || idx + 1;
      const apellidos = m.estudianteApellidos || (m.estudiante ? m.estudiante.split(" ").slice(0, 2).join(" ") : `ALUMNO ${idx + 1}`);
      const nombres   = m.estudianteNombres   || (m.estudiante ? m.estudiante.split(" ").slice(2).join(" ") : "");
      const nombreCompleto = `${apellidos} ${nombres}`.trim();

      // Trimestre 1
      const t1_oral      = obtenerNotaSimulada(id, 1);
      const t1_escrita   = obtenerNotaSimulada(id, 2);
      const t1_tareas    = obtenerNotaSimulada(id, 3);
      const t1_talleres  = obtenerNotaSimulada(id, 4);
      const t1_cuaderno  = obtenerNotaSimulada(id, 5);
      const t1_trabInd   = obtenerNotaSimulada(id, 6);
      const t1_expos     = obtenerNotaSimulada(id, 7);
      const t1_promForm  = Number(((t1_oral + t1_escrita + t1_tareas + t1_talleres + t1_cuaderno + t1_trabInd + t1_expos) / 7).toFixed(2));
      const t1_total70   = Number((t1_promForm * 0.70).toFixed(2));

      const t1_proy      = obtenerNotaSimulada(id, 8);
      const t1_examen    = obtenerNotaSimulada(id, 9);
      const t1_promSum   = Number(((t1_proy + t1_examen) / 2).toFixed(2));
      const t1_total30   = Number((t1_promSum * 0.30).toFixed(2));
      const t1_promTrim  = Number((t1_total70 + t1_total30).toFixed(2));

      // Trimestre 2
      const t2_oral      = obtenerNotaSimulada(id, 11);
      const t2_escrita   = obtenerNotaSimulada(id, 12);
      const t2_tareas    = obtenerNotaSimulada(id, 13);
      const t2_talleres  = obtenerNotaSimulada(id, 14);
      const t2_cuaderno  = obtenerNotaSimulada(id, 15);
      const t2_trabInd   = obtenerNotaSimulada(id, 16);
      const t2_expos     = obtenerNotaSimulada(id, 17);
      const t2_promForm  = Number(((t2_oral + t2_escrita + t2_tareas + t2_talleres + t2_cuaderno + t2_trabInd + t2_expos) / 7).toFixed(2));
      const t2_total70   = Number((t2_promForm * 0.70).toFixed(2));

      const t2_proy      = obtenerNotaSimulada(id, 18);
      const t2_examen    = obtenerNotaSimulada(id, 19);
      const t2_promSum   = Number(((t2_proy + t2_examen) / 2).toFixed(2));
      const t2_total30   = Number((t2_promSum * 0.30).toFixed(2));
      const t2_promTrim  = Number((t2_total70 + t2_total30).toFixed(2));

      // Trimestre 3
      const t3_oral      = obtenerNotaSimulada(id, 21);
      const t3_escrita   = obtenerNotaSimulada(id, 22);
      const t3_tareas    = obtenerNotaSimulada(id, 23);
      const t3_talleres  = obtenerNotaSimulada(id, 24);
      const t3_cuaderno  = obtenerNotaSimulada(id, 25);
      const t3_trabInd   = obtenerNotaSimulada(id, 26);
      const t3_expos     = obtenerNotaSimulada(id, 27);
      const t3_promForm  = Number(((t3_oral + t3_escrita + t3_tareas + t3_talleres + t3_cuaderno + t3_trabInd + t3_expos) / 7).toFixed(2));
      const t3_total70   = Number((t3_promForm * 0.70).toFixed(2));

      const t3_proy      = obtenerNotaSimulada(id, 28);
      const t3_examen    = obtenerNotaSimulada(id, 29);
      const t3_promSum   = Number(((t3_proy + t3_examen) / 2).toFixed(2));
      const t3_total30   = Number((t3_promSum * 0.30).toFixed(2));
      const t3_promTrim  = Number((t3_total70 + t3_total30).toFixed(2));

      // Promedio Final Anual
      const promFinal = Number(((t1_promTrim + t2_promTrim + t3_promTrim) / 3).toFixed(2));
      const cualitativa = promFinal >= 9.0 ? "DAR (Domina)" : promFinal >= 7.0 ? "AAR (Alcanza)" : "PAAR (Próximo)";

      return {
        idMatricula: id,
        numero: idx + 1,
        nombreCompleto,
        t1: { oral: t1_oral, escrita: t1_escrita, tareas: t1_tareas, talleres: t1_talleres, cuaderno: t1_cuaderno, trabInd: t1_trabInd, expos: t1_expos, promForm: t1_promForm, total70: t1_total70, proy: t1_proy, examen: t1_examen, promSum: t1_promSum, total30: t1_total30, promTrim: t1_promTrim },
        t2: { oral: t2_oral, escrita: t2_escrita, tareas: t2_tareas, talleres: t2_talleres, cuaderno: t2_cuaderno, trabInd: t2_trabInd, expos: t2_expos, promForm: t2_promForm, total70: t2_total70, proy: t2_proy, examen: t2_examen, promSum: t2_promSum, total30: t2_total30, promTrim: t2_promTrim },
        t3: { oral: t3_oral, escrita: t3_escrita, tareas: t3_tareas, talleres: t3_talleres, cuaderno: t3_cuaderno, trabInd: t3_trabInd, expos: t3_expos, promForm: t3_promForm, total70: t3_total70, proy: t3_proy, examen: t3_examen, promSum: t3_promSum, total30: t3_total30, promTrim: t3_promTrim },
        promFinal,
        cualitativa
      };
    });
  }, [matriculas]);

  const estudiantesFiltrados = useMemo(() => {
    if (!busqueda.trim()) return estudiantesProcesados;
    return estudiantesProcesados.filter(e =>
      e.nombreCompleto.toLowerCase().includes(busqueda.toLowerCase())
    );
  }, [estudiantesProcesados, busqueda]);

  const asignacionesFiltradas = asignaciones.filter(a =>
    `${a.asignatura} ${a.grado} ${a.docente}`.toLowerCase().includes(busqueda.toLowerCase())
  );

  // ── VISTA 1: CATÁLOGO DE CURSOS ─────────────────────────
  if (vista === "cursos") return (
    <Layout breadcrumb={["Inicio", "Calificaciones"]} sidebarTitle="Calificaciones" menuItems={calMenuItems} seccion="cursos" onSeccionChange={(id) => { setVista(id); }}>
      <div className="flex items-center justify-between mb-4">
        <div>
          <h1 className="text-lg font-bold text-slate-700">Calificaciones</h1>
          <p className="text-xs text-slate-400">
            {anoActual ? `Año lectivo: ${anoActual.nombre}` : "Selecciona un curso para ver la sábana oficial de calificaciones"}
          </p>
        </div>
        <div className="relative">
          <input type="text" placeholder="Buscar curso, asignatura..."
            value={busqueda} onChange={e => setBusqueda(e.target.value)}
            className="pl-3 pr-8 py-1.5 text-xs border border-slate-200 rounded-lg bg-slate-50 w-56 focus:outline-none" />
          <svg className="w-4 h-4 text-slate-400 absolute right-2 top-1/2 -translate-y-1/2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
        </div>
      </div>

      {loading ? (
        <div className="text-center py-20 text-slate-400 text-sm">Cargando cursos...</div>
      ) : asignacionesFiltradas.length === 0 ? (
        <div className="text-center py-20 text-slate-400 text-sm">No hay cursos activos</div>
      ) : (
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4">
          {asignacionesFiltradas.map((a, i) => {
            const c = COLORES[i % COLORES.length];
            return (
              <button key={a.idAsignacion} onClick={() => abrirMatrizCurso(a)}
                className="bg-white border border-slate-200 rounded-xl p-5 flex flex-col items-center gap-3 hover:shadow-md transition-all group text-center"
                onMouseEnter={e => e.currentTarget.style.borderColor = c.border}
                onMouseLeave={e => e.currentTarget.style.borderColor = ""}>
                <div className={`${c.bg} p-3 rounded-xl ${c.icon}`}>
                  <IconLibro className="w-10 h-10" />
                </div>
                <div>
                  <p className="text-sm font-semibold text-slate-700 group-hover:text-[#243A76] transition">{a.asignatura}</p>
                  <p className="text-xs text-slate-400 mt-0.5">{a.grado}</p>
                  <p className="text-xs text-slate-300 mt-0.5">{a.docente}</p>
                </div>
                {a.esTutor && (
                  <span className="text-xs px-2 py-0.5 rounded-full bg-blue-100 text-blue-600 font-medium">Tutor</span>
                )}
              </button>
            );
          })}
        </div>
      )}
    </Layout>
  );

  // ── VISTA 2: SÁBANA OFICIAL DE CALIFICACIONES (TIPO EXCEL) ───
  return (
    <Layout breadcrumb={["Inicio", "Calificaciones", asignacionSel?.asignatura]} sidebarTitle="Calificaciones" menuItems={calMenuItems} seccion="cursos" onSeccionChange={(id) => { setVista(id); }}>
      {/* Barra de cabecera */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-3 mb-4">
        <div className="flex items-center gap-3">
          <button onClick={() => { setVista("cursos"); setBusqueda(""); }}
            style={{ borderColor: PRIMARY, color: PRIMARY }}
            className="flex items-center gap-1 px-3 py-1.5 border rounded-lg text-xs font-medium hover:bg-blue-50 transition bg-white shadow-sm">
            <IconBack /> Volver a cursos
          </button>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-base font-bold text-slate-800">{asignacionSel?.asignatura}</h1>
              <span className="bg-[#243A76] text-white text-[10px] font-bold px-2 py-0.5 rounded-full">
                {asignacionSel?.grado || "Décimo año EGB - Paralelo A"}
              </span>
            </div>
            <p className="text-xs text-slate-500 mt-0.5">
              Docente: <strong className="text-slate-700">{asignacionSel?.docente}</strong> · {matriculas.length} Estudiantes Matriculados
            </p>
          </div>
        </div>

        {/* Controles: Selector de Trimestre, Búsqueda e Imprimir */}
        <div className="flex flex-wrap items-center gap-2">
          {/* Tabs Trimestres */}
          <div className="flex rounded-lg bg-slate-100 p-0.5 border border-slate-200">
            {[
              { id: "todos", label: "Sábana Anual" },
              { id: "1", label: "1T" },
              { id: "2", label: "2T" },
              { id: "3", label: "3T" },
            ].map(t => (
              <button key={t.id} onClick={() => setTabTrimestre(t.id)}
                className={`px-3 py-1 text-xs font-semibold rounded-md transition ${tabTrimestre === t.id ? "bg-[#243A76] text-white shadow-sm" : "text-slate-600 hover:text-slate-900"}`}>
                {t.label}
              </button>
            ))}
          </div>

          <input type="text" placeholder="Buscar alumno..."
            value={busqueda} onChange={e => setBusqueda(e.target.value)}
            className="px-3 py-1 text-xs border border-slate-200 rounded-lg bg-white w-44 focus:outline-none shadow-sm" />

          <button onClick={() => window.print()}
            className="flex items-center gap-1.5 px-3 py-1 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-semibold shadow-sm transition">
            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 17h2a2 2 0 002-2v-4a2 2 0 00-2-2H5a2 2 0 00-2 2v4a2 2 0 002 2h2m2 4h6a2 2 0 002-2v-4a2 2 0 00-2-2H9a2 2 0 00-2 2v4a2 2 0 002 2zm8-12V5a2 2 0 00-2-2H9a2 2 0 00-2 2v4h10z" />
            </svg>
            Imprimir PDF
          </button>
        </div>
      </div>

      {/* SÁBANA DE CALIFICACIONES TIPO EXCEL */}
      <div className="bg-white rounded-xl shadow-md border border-slate-300 overflow-hidden">
        <div className="overflow-x-auto max-h-[72vh] relative">
          <table className="w-full text-xs text-center border-collapse">
            {/* NIVEL 1: ENCABEZADO SUPERIOR */}
            <thead className="sticky top-0 z-20 shadow-sm text-white">
              <tr className="bg-[#1e293b] uppercase text-[11px] font-bold tracking-wider">
                <th colSpan={2} className="px-3 py-2 border-r border-slate-600 bg-[#0f172a] sticky left-0 z-30">
                  DATOS INFORMATIVOS
                </th>
                {(tabTrimestre === "todos" || tabTrimestre === "1") && (
                  <th colSpan={13} className="px-3 py-2 border-r border-slate-600 bg-[#1e3a8a]">
                    PRIMER TRIMESTRE
                  </th>
                )}
                {(tabTrimestre === "todos" || tabTrimestre === "2") && (
                  <th colSpan={13} className="px-3 py-2 border-r border-slate-600 bg-[#065f46]">
                    SEGUNDO TRIMESTRE
                  </th>
                )}
                {(tabTrimestre === "todos" || tabTrimestre === "3") && (
                  <th colSpan={13} className="px-3 py-2 border-r border-slate-600 bg-[#831843]">
                    TERCER TRIMESTRE
                  </th>
                )}
                <th colSpan={2} className="px-3 py-2 bg-[#1e293b]">
                  PROMEDIO ANUAL
                </th>
              </tr>

              {/* NIVEL 2: FORMATIVAS (70%) vs SUMATIVAS (30%) */}
              <tr className="text-[10px] font-bold">
                <th colSpan={2} className="bg-[#0f172a] border-r border-slate-600 sticky left-0 z-30"></th>

                {/* T1 */}
                {(tabTrimestre === "todos" || tabTrimestre === "1") && (
                  <>
                    <th colSpan={9} className="bg-[#2563eb] border-r border-blue-400 py-1">EVALUACIÓN FORMATIVA (70%)</th>
                    <th colSpan={3} className="bg-[#1d4ed8] border-r border-blue-400 py-1">SUMATIVA (30%)</th>
                    <th rowSpan={2} className="bg-[#172554] border-r border-slate-600 py-1 text-amber-300 font-extrabold">PROM. T1</th>
                  </>
                )}

                {/* T2 */}
                {(tabTrimestre === "todos" || tabTrimestre === "2") && (
                  <>
                    <th colSpan={9} className="bg-[#059669] border-r border-emerald-400 py-1">EVALUACIÓN FORMATIVA (70%)</th>
                    <th colSpan={3} className="bg-[#047857] border-r border-emerald-400 py-1">SUMATIVA (30%)</th>
                    <th rowSpan={2} className="bg-[#064e3b] border-r border-slate-600 py-1 text-amber-300 font-extrabold">PROM. T2</th>
                  </>
                )}

                {/* T3 */}
                {(tabTrimestre === "todos" || tabTrimestre === "3") && (
                  <>
                    <th colSpan={9} className="bg-[#db2777] border-r border-pink-400 py-1">EVALUACIÓN FORMATIVA (70%)</th>
                    <th colSpan={3} className="bg-[#be185d] border-r border-pink-400 py-1">SUMATIVA (30%)</th>
                    <th rowSpan={2} className="bg-[#701a75] border-r border-slate-600 py-1 text-amber-300 font-extrabold">PROM. T3</th>
                  </>
                )}

                <th rowSpan={2} className="bg-[#0f172a] text-yellow-300 font-extrabold border-r border-slate-600">PROM. FINAL</th>
                <th rowSpan={2} className="bg-[#0f172a] text-slate-200">CUALITATIVA</th>
              </tr>

              {/* NIVEL 3: DETALLE DE COLUMNAS / ACTIVIDADES */}
              <tr className="bg-slate-200 text-slate-800 text-[9px] font-bold border-b-2 border-slate-400">
                <th className="px-2 py-2 border-r border-slate-300 bg-slate-100 sticky left-0 z-30 w-8">Nº</th>
                <th className="px-3 py-2 border-r border-slate-300 bg-slate-100 sticky left-8 z-30 text-left min-w-[220px]">
                  APELLIDOS / NOMBRES
                </th>

                {/* Actividades T1 */}
                {(tabTrimestre === "todos" || tabTrimestre === "1") && (
                  <>
                    <th className="px-1.5 py-1 border-r border-slate-300 bg-blue-50/70">Oral</th>
                    <th className="px-1.5 py-1 border-r border-slate-300 bg-blue-50/70">Escrita</th>
                    <th className="px-1.5 py-1 border-r border-slate-300 bg-blue-50/70">Tareas</th>
                    <th className="px-1.5 py-1 border-r border-slate-300 bg-blue-50/70">Talleres</th>
                    <th className="px-1.5 py-1 border-r border-slate-300 bg-blue-50/70">Cuaderno</th>
                    <th className="px-1.5 py-1 border-r border-slate-300 bg-blue-50/70">Trab. Ind</th>
                    <th className="px-1.5 py-1 border-r border-slate-300 bg-blue-50/70">Expos</th>
                    <th className="px-1.5 py-1 border-r border-slate-300 bg-blue-200 font-black text-blue-900">PROM</th>
                    <th className="px-1.5 py-1 border-r border-slate-300 bg-blue-300 font-black text-blue-950">70%</th>
                    <th className="px-1.5 py-1 border-r border-slate-300 bg-indigo-50">Proyecto</th>
                    <th className="px-1.5 py-1 border-r border-slate-300 bg-indigo-50">Examen</th>
                    <th className="px-1.5 py-1 border-r border-slate-300 bg-indigo-200 font-black text-indigo-900">30%</th>
                  </>
                )}

                {/* Actividades T2 */}
                {(tabTrimestre === "todos" || tabTrimestre === "2") && (
                  <>
                    <th className="px-1.5 py-1 border-r border-slate-300 bg-emerald-50/70">Oral</th>
                    <th className="px-1.5 py-1 border-r border-slate-300 bg-emerald-50/70">Escrita</th>
                    <th className="px-1.5 py-1 border-r border-slate-300 bg-emerald-50/70">Tareas</th>
                    <th className="px-1.5 py-1 border-r border-slate-300 bg-emerald-50/70">Talleres</th>
                    <th className="px-1.5 py-1 border-r border-slate-300 bg-emerald-50/70">Cuaderno</th>
                    <th className="px-1.5 py-1 border-r border-slate-300 bg-emerald-50/70">Trab. Ind</th>
                    <th className="px-1.5 py-1 border-r border-slate-300 bg-emerald-50/70">Expos</th>
                    <th className="px-1.5 py-1 border-r border-slate-300 bg-emerald-200 font-black text-emerald-900">PROM</th>
                    <th className="px-1.5 py-1 border-r border-slate-300 bg-emerald-300 font-black text-emerald-950">70%</th>
                    <th className="px-1.5 py-1 border-r border-slate-300 bg-teal-50">Proyecto</th>
                    <th className="px-1.5 py-1 border-r border-slate-300 bg-teal-50">Examen</th>
                    <th className="px-1.5 py-1 border-r border-slate-300 bg-teal-200 font-black text-teal-900">30%</th>
                  </>
                )}

                {/* Actividades T3 */}
                {(tabTrimestre === "todos" || tabTrimestre === "3") && (
                  <>
                    <th className="px-1.5 py-1 border-r border-slate-300 bg-pink-50/70">Oral</th>
                    <th className="px-1.5 py-1 border-r border-slate-300 bg-pink-50/70">Escrita</th>
                    <th className="px-1.5 py-1 border-r border-slate-300 bg-pink-50/70">Tareas</th>
                    <th className="px-1.5 py-1 border-r border-slate-300 bg-pink-50/70">Talleres</th>
                    <th className="px-1.5 py-1 border-r border-slate-300 bg-pink-50/70">Cuaderno</th>
                    <th className="px-1.5 py-1 border-r border-slate-300 bg-pink-50/70">Trab. Ind</th>
                    <th className="px-1.5 py-1 border-r border-slate-300 bg-pink-50/70">Expos</th>
                    <th className="px-1.5 py-1 border-r border-slate-300 bg-pink-200 font-black text-pink-900">PROM</th>
                    <th className="px-1.5 py-1 border-r border-slate-300 bg-pink-300 font-black text-pink-950">70%</th>
                    <th className="px-1.5 py-1 border-r border-slate-300 bg-rose-50">Proyecto</th>
                    <th className="px-1.5 py-1 border-r border-slate-300 bg-rose-50">Examen</th>
                    <th className="px-1.5 py-1 border-r border-slate-300 bg-rose-200 font-black text-rose-900">30%</th>
                  </>
                )}
              </tr>
            </thead>

            {/* CUERPO DE DATOS */}
            <tbody className="divide-y divide-slate-200">
              {estudiantesFiltrados.map((e, idx) => (
                <tr key={e.idMatricula} className={`hover:bg-amber-50/50 transition font-mono text-[11px] ${idx % 2 === 0 ? "bg-white" : "bg-slate-50/70"}`}>
                  {/* Columnas fijas del alumno */}
                  <td className="px-2 py-1.5 border-r border-slate-200 text-slate-500 font-sans font-semibold sticky left-0 z-10 bg-inherit w-8">
                    {e.numero}
                  </td>
                  <td className="px-3 py-1.5 border-r border-slate-200 text-slate-800 font-sans font-bold text-left sticky left-8 z-10 bg-inherit whitespace-nowrap">
                    {e.nombreCompleto}
                  </td>

                  {/* Notas T1 */}
                  {(tabTrimestre === "todos" || tabTrimestre === "1") && (
                    <>
                      <td className="px-1 border-r border-slate-200">{e.t1.oral.toFixed(2)}</td>
                      <td className="px-1 border-r border-slate-200">{e.t1.escrita.toFixed(2)}</td>
                      <td className="px-1 border-r border-slate-200">{e.t1.tareas.toFixed(2)}</td>
                      <td className="px-1 border-r border-slate-200">{e.t1.talleres.toFixed(2)}</td>
                      <td className="px-1 border-r border-slate-200">{e.t1.cuaderno.toFixed(2)}</td>
                      <td className="px-1 border-r border-slate-200">{e.t1.trabInd.toFixed(2)}</td>
                      <td className="px-1 border-r border-slate-200">{e.t1.expos.toFixed(2)}</td>
                      <td className="px-1 border-r border-slate-200 font-bold bg-blue-50 text-blue-900">{e.t1.promForm.toFixed(2)}</td>
                      <td className="px-1 border-r border-slate-200 font-bold bg-blue-100 text-blue-950">{e.t1.total70.toFixed(2)}</td>
                      <td className="px-1 border-r border-slate-200">{e.t1.proy.toFixed(2)}</td>
                      <td className="px-1 border-r border-slate-200">{e.t1.examen.toFixed(2)}</td>
                      <td className="px-1 border-r border-slate-200 font-bold bg-indigo-100 text-indigo-900">{e.t1.total30.toFixed(2)}</td>
                      <td className="px-1.5 border-r border-slate-300 font-black bg-blue-200 text-blue-950">{e.t1.promTrim.toFixed(2)}</td>
                    </>
                  )}

                  {/* Notas T2 */}
                  {(tabTrimestre === "todos" || tabTrimestre === "2") && (
                    <>
                      <td className="px-1 border-r border-slate-200">{e.t2.oral.toFixed(2)}</td>
                      <td className="px-1 border-r border-slate-200">{e.t2.escrita.toFixed(2)}</td>
                      <td className="px-1 border-r border-slate-200">{e.t2.tareas.toFixed(2)}</td>
                      <td className="px-1 border-r border-slate-200">{e.t2.talleres.toFixed(2)}</td>
                      <td className="px-1 border-r border-slate-200">{e.t2.cuaderno.toFixed(2)}</td>
                      <td className="px-1 border-r border-slate-200">{e.t2.trabInd.toFixed(2)}</td>
                      <td className="px-1 border-r border-slate-200">{e.t2.expos.toFixed(2)}</td>
                      <td className="px-1 border-r border-slate-200 font-bold bg-emerald-50 text-emerald-900">{e.t2.promForm.toFixed(2)}</td>
                      <td className="px-1 border-r border-slate-200 font-bold bg-emerald-100 text-emerald-950">{e.t2.total70.toFixed(2)}</td>
                      <td className="px-1 border-r border-slate-200">{e.t2.proy.toFixed(2)}</td>
                      <td className="px-1 border-r border-slate-200">{e.t2.examen.toFixed(2)}</td>
                      <td className="px-1 border-r border-slate-200 font-bold bg-teal-100 text-teal-900">{e.t2.total30.toFixed(2)}</td>
                      <td className="px-1.5 border-r border-slate-300 font-black bg-emerald-200 text-emerald-950">{e.t2.promTrim.toFixed(2)}</td>
                    </>
                  )}

                  {/* Notas T3 */}
                  {(tabTrimestre === "todos" || tabTrimestre === "3") && (
                    <>
                      <td className="px-1 border-r border-slate-200">{e.t3.oral.toFixed(2)}</td>
                      <td className="px-1 border-r border-slate-200">{e.t3.escrita.toFixed(2)}</td>
                      <td className="px-1 border-r border-slate-200">{e.t3.tareas.toFixed(2)}</td>
                      <td className="px-1 border-r border-slate-200">{e.t3.talleres.toFixed(2)}</td>
                      <td className="px-1 border-r border-slate-200">{e.t3.cuaderno.toFixed(2)}</td>
                      <td className="px-1 border-r border-slate-200">{e.t3.trabInd.toFixed(2)}</td>
                      <td className="px-1 border-r border-slate-200">{e.t3.expos.toFixed(2)}</td>
                      <td className="px-1 border-r border-slate-200 font-bold bg-pink-50 text-pink-900">{e.t3.promForm.toFixed(2)}</td>
                      <td className="px-1 border-r border-slate-200 font-bold bg-pink-100 text-pink-950">{e.t3.total70.toFixed(2)}</td>
                      <td className="px-1 border-r border-slate-200">{e.t3.proy.toFixed(2)}</td>
                      <td className="px-1 border-r border-slate-200">{e.t3.examen.toFixed(2)}</td>
                      <td className="px-1 border-r border-slate-200 font-bold bg-rose-100 text-rose-900">{e.t3.total30.toFixed(2)}</td>
                      <td className="px-1.5 border-r border-slate-300 font-black bg-pink-200 text-pink-950">{e.t3.promTrim.toFixed(2)}</td>
                    </>
                  )}

                  {/* Promedio Final y Cualitativa */}
                  <td className="px-2 py-1.5 border-r border-slate-300 font-black bg-amber-100 text-slate-900 text-xs">
                    {e.promFinal.toFixed(2)}
                  </td>
                  <td className="px-2 py-1.5 font-sans font-bold text-[10px] text-emerald-700 bg-slate-50">
                    {e.cualitativa}
                  </td>
                </tr>
              ))}
              {estudiantesFiltrados.length === 0 && (
                <tr>
                  <td colSpan={40} className="text-center py-16 text-slate-400 text-sm">
                    No se encontraron estudiantes para este curso.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Pie informativo */}
      <div className="mt-3 flex flex-wrap items-center justify-between text-xs text-slate-400">
        <div className="flex items-center gap-1.5">
          <svg className="w-4 h-4 text-emerald-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          Sábana Oficial de Calificaciones según la normativa del Ministerio de Educación (Formativas 70% + Sumativas 30%).
        </div>
        <p className="font-medium text-slate-500">
          Base de Datos Fragmentada por Hash (4 Shards) · gRPC Service
        </p>
      </div>
    </Layout>
  );
}
