import { useState, useEffect, useCallback } from "react";
import Layout from "../../components/Layout";
import api from "../../config/axios";

const PRIMARY = "#243A76";
const INSTITUTIONAL_GREEN = "#2E7D32";

const menuItems = [
  {
    id: "materia",
    label: "Grilla por Materia",
    icon: (
      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 17v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
      </svg>
    ),
  },
  {
    id: "estudiantes",
    label: "Asistencia por Estudiante",
    icon: (
      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
      </svg>
    ),
  },
];

export default function ConsultaAsistencias() {
  const [seccion, setSeccion] = useState("materia");
  const [grados, setGrados] = useState([]);
  const [gradoSel, setGradoSel] = useState("");
  const [todasMatriculas, setTodasMatriculas] = useState([]);
  const [todasAsignaciones, setTodasAsignaciones] = useState([]);
  
  const [estudiantesMatriculados, setEstudiantesMatriculados] = useState([]);
  const [estudianteSel, setEstudianteSel] = useState(null);
  const [materiasDelGrado, setMateriasDelGrado] = useState([]);
  const [asistenciasGrpc, setAsistenciasGrpc] = useState({});
  const [loading, setLoading] = useState(true);
  const [modalSesion, setModalSesion] = useState(null);

  // 1. CARGAR DATOS REALES DE LA BASE DE DATOS Y AUTOMÁTICAMENTE SELECCIONAR EL PRIMER GRADO CON ALUMNOS
  useEffect(() => {
    setLoading(true);
    Promise.all([
      api.get("/api/grados").catch(() => ({ data: [] })),
      api.get("/api/matriculas", { params: { limit: 300 } }).catch(() => ({ data: { items: [] } })),
      api.get("/api/asignaciones").catch(() => ({ data: [] })),
    ])
      .then(([resGrados, resMatriculas, resAsignaciones]) => {
        const listaGrados = resGrados.data || [];
        const listaMatriculas = resMatriculas.data?.items || resMatriculas.data || [];
        const listaAsignaciones = resAsignaciones.data || [];

        setGrados(listaGrados);
        setTodasMatriculas(listaMatriculas);
        setTodasAsignaciones(listaAsignaciones);

        // Seleccionar automáticamente el primer grado que posea alumnos matriculados en la BD
        const gradoConAlumnos = listaGrados.find((g) =>
          listaMatriculas.some((m) => String(m.idGrado || m.grado?.idGrado) === String(g.idGrado))
        );

        if (gradoConAlumnos) {
          setGradoSel(String(gradoConAlumnos.idGrado));
        } else if (listaGrados.length > 0) {
          setGradoSel(String(listaGrados[0].idGrado));
        }
      })
      .finally(() => setLoading(false));
  }, []);

  // 2. FILTRAR ALUMNOS REALES Y DOCENTES ASIGNADOS REALES DE LA BASE DE DATOS PARA EL GRADO ELEGIDO
  const actualizarGrado = useCallback(() => {
    if (!gradoSel) return;

    // Obtener los estudiantes matriculados reales directamente del backend para este grado
    api.get("/api/matriculas", { params: { idGrado: gradoSel, limit: 500 } })
      .then((r) => {
        const items = r.data?.items || r.data || [];
        setEstudiantesMatriculados(items);
        setEstudianteSel(items.length > 0 ? items[0] : null);
      })
      .catch(() => {
        const filtradosMat = todasMatriculas.filter(
          (m) => String(m.idGrado || m.grado?.idGrado) === String(gradoSel)
        );
        setEstudiantesMatriculados(filtradosMat);
        setEstudianteSel(filtradosMat.length > 0 ? filtradosMat[0] : null);
      });

    // Asignaciones reales de docentes a materias en este grado de la BD
    const asigGrado = todasAsignaciones.filter(
      (a) => String(a.idGrado || a.grado?.idGrado) === String(gradoSel)
    );

    const gradoObj = grados.find((g) => String(g.idGrado) === String(gradoSel)) || { nombre: "Curso" };

    if (asigGrado.length > 0) {
      const mapeadas = asigGrado.map((a, idx) => ({
        idAsignacion: a.idAsignacion || idx + 1,
        materiaNombre: a.asignatura || a.nombreAsignatura || a.asignatura?.nombre || `Asignatura #${idx + 1}`,
        codigoMateria: `EGB-${101 + idx}`,
        docenteNombre: a.docente || (a.docenteObj ? `${a.docenteObj.apellidos} ${a.docenteObj.nombres}` : "Docente Sin Asignar (Pendiente)"),
        gradoNombre: gradoObj.nombre,
      }));
      setMateriasDelGrado(mapeadas);
    } else {
      setMateriasDelGrado([]);
    }
  }, [gradoSel, todasMatriculas, todasAsignaciones, grados]);

  useEffect(() => {
    actualizarGrado();
  }, [actualizarGrado]);

  // 3. CONSULTA gRPC DE ASISTENCIAS POR CADA ASIGNACIÓN REAL
  useEffect(() => {
    if (materiasDelGrado.length === 0) {
      setAsistenciasGrpc({});
      return;
    }

    materiasDelGrado.forEach((mat) => {
      api.get(`/api/docente/asistencias/asignacion/${mat.idAsignacion}`)
        .then((r) => {
          if (r.data?.asistencias && Array.isArray(r.data.asistencias)) {
            setAsistenciasGrpc((prev) => ({ ...prev, [mat.idAsignacion]: r.data.asistencias }));
          } else {
            setAsistenciasGrpc((prev) => ({ ...prev, [mat.idAsignacion]: [] }));
          }
        })
        .catch(() => {
          setAsistenciasGrpc((prev) => ({ ...prev, [mat.idAsignacion]: [] }));
        });
    });
  }, [materiasDelGrado]);

  const gradoActualObj = grados.find((g) => String(g.idGrado) === String(gradoSel)) || { nombre: "Curso" };

  const getNombreAlumno = (m) => {
    if (!m) return "Estudiante";
    if (m.estudiante) {
      return `${m.estudiante.apellidos || ""} ${m.estudiante.nombres || ""}`.trim();
    }
    if (m.apellidos || m.nombres) {
      return `${m.apellidos || ""} ${m.nombres || ""}`.trim();
    }
    return m.nombresCompletos || `Estudiante #${m.idMatricula || m.idEstudiante}`;
  };

  const getCedulaAlumno = (m) => {
    if (!m) return "—";
    if (m.estudiante) return m.estudiante.cedula || "S/C";
    return m.cedula || "—";
  };

  return (
    <Layout
      breadcrumb={["Inicio", "Consulta de Asistencias"]}
      sidebarTitle="Asistencias"
      menuItems={menuItems}
      seccion={seccion}
      onSeccionChange={setSeccion}
    >
      {/* BARRA DE SELECCIÓN DE CURSO (MUESTRA EL NÚMERO REAL DE MATRICULADOS EN BD) */}
      <div className="bg-white border border-slate-200 rounded-2xl p-4 mb-5 shadow-sm">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div className="flex flex-wrap items-center gap-4">
            <div>
              <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">
                Curso / Grado Escolar *
              </label>
              <select
                value={gradoSel}
                onChange={(e) => setGradoSel(e.target.value)}
                className="border border-slate-200 rounded-lg px-3 py-2 text-xs font-bold text-slate-800 bg-slate-50 focus:outline-none focus:ring-2 focus:ring-blue-500 min-w-[260px]"
              >
                {grados.map((g) => {
                  const cant = todasMatriculas.filter((m) => String(m.idGrado || m.grado?.idGrado) === String(g.idGrado)).length;
                  return (
                    <option key={g.idGrado} value={g.idGrado}>
                      {g.nombre} — Paralelo A ({cant} alumnos en BD)
                    </option>
                  );
                })}
              </select>
            </div>

            <div>
              <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">
                Período Lectivo Oficial
              </label>
              <span className="inline-block border border-slate-200 bg-slate-100 rounded-lg px-3 py-2 text-xs font-bold text-slate-700 font-mono">
                AÑO LECTIVO VIGENTE 2026 - 2027
              </span>
            </div>
          </div>

          <div className="text-right">
            <span className="text-xs font-extrabold text-[#243A76] block">
              {gradoActualObj.nombre} · Paralelo A
            </span>
            <span className="text-[11px] font-bold text-emerald-700 bg-emerald-50 px-2.5 py-0.5 rounded-full inline-block mt-0.5">
              {estudiantesMatriculados.length} Estudiantes Matriculados
            </span>
          </div>
        </div>
      </div>

      {/* VISTA 1: GRILLA GENERAL POR MATERIA */}
      {seccion === "materia" && (
        <div className="bg-white border border-slate-200 rounded-xl overflow-hidden shadow-sm">
          {/* CABECERA */}
          <div style={{ backgroundColor: PRIMARY }} className="text-white px-5 py-3 text-xs font-bold uppercase tracking-wider grid grid-cols-12 gap-3 items-center">
            <div className="col-span-12 md:col-span-4">MATERIA / DOCENTE ASIGNADO EN BD</div>
            <div className="col-span-6 md:col-span-2 text-center">% ASISTENCIA</div>
            <div className="col-span-6 md:col-span-6 text-right">REGISTRO DIARIO DE CLASES (gRPC)</div>
          </div>

          {/* FILAS DE MATERIAS DE LA BASE DE DATOS */}
          <div className="divide-y divide-slate-100">
            {materiasDelGrado.length === 0 ? (
              <div className="p-12 text-center text-slate-500 text-xs font-medium">
                No existen asignaciones de docentes ni materias configuradas para este curso en la base de datos.
              </div>
            ) : (
              materiasDelGrado.map((mat) => {
                const asistList = asistenciasGrpc[mat.idAsignacion] || [];
                const totalSes = asistList.length;
                const ausentes = asistList.filter((a) => a.estado === "AUSENTE" || a.estado === "FALTA").length;
                const presentes = totalSes - ausentes;
                const porcentaje = totalSes > 0 ? Math.round((presentes / totalSes) * 100) : 0;

                return (
                  <div key={mat.idAsignacion} className="p-4 grid grid-cols-12 gap-4 items-center hover:bg-slate-50/80 transition">
                    {/* MATERIA & DOCENTE REAL */}
                    <div className="col-span-12 md:col-span-4 space-y-1.5">
                      <h3 className="text-xs font-bold text-slate-800 leading-snug">
                        {mat.materiaNombre} - [{mat.codigoMateria}] - A - {mat.gradoNombre.toUpperCase()}
                      </h3>
                      <p className="text-[11px] text-slate-600 font-semibold flex items-center gap-1">
                        <svg className="w-3.5 h-3.5 text-blue-800" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" /></svg>
                        {mat.docenteNombre}
                      </p>

                      <div className="flex items-center gap-1.5 pt-1">
                        <span className="bg-slate-100 border border-slate-200 text-slate-700 text-[10px] font-bold px-2 py-0.5 rounded">
                          TOTAL: {totalSes}
                        </span>
                        <span className="bg-emerald-50 border border-emerald-200 text-emerald-700 text-[10px] font-bold px-2 py-0.5 rounded">
                          PRESENTES: {presentes}
                        </span>
                        <span className="bg-rose-50 border border-rose-200 text-rose-700 text-[10px] font-bold px-2 py-0.5 rounded">
                          FALTAS: {ausentes}
                        </span>
                      </div>
                    </div>

                    {/* PORCENTAJE */}
                    <div className="col-span-6 md:col-span-2 text-center flex flex-col items-center justify-center">
                      <span className="text-xl font-black text-slate-800 leading-none mb-1">
                        {totalSes > 0 ? `${porcentaje}%` : "—"}
                      </span>
                      <div className="w-14 h-1.5 bg-slate-200 rounded-full overflow-hidden">
                        <div
                          style={{ width: `${porcentaje}%`, backgroundColor: porcentaje >= 85 ? INSTITUTIONAL_GREEN : (porcentaje >= 80 ? "#D97706" : "#DC2626") }}
                          className="h-full rounded-full"
                        />
                      </div>
                    </div>

                    {/* GRILLA DE SESIONES */}
                    <div className="col-span-12 md:col-span-6">
                      {asistList.length === 0 ? (
                        <span className="text-[11px] text-slate-400 font-medium italic">
                          Sin registros de asistencia sincronizados en gRPC
                        </span>
                      ) : (
                        <div className="flex flex-wrap gap-1 items-center max-h-24 overflow-y-auto p-1">
                          {asistList.map((s, idx) => {
                            const esFalta = s.estado === "AUSENTE" || s.estado === "FALTA";
                            const esJust = s.estado === "JUSTIFICADO";
                            return (
                              <button
                                key={s.id_asistencia || idx}
                                type="button"
                                onClick={() => setModalSesion({ materia: mat.materiaNombre, docente: mat.docenteNombre, sesion: s, num: idx + 1 })}
                                className={`w-5 h-5 rounded-full flex items-center justify-center text-[10px] font-bold text-white transition transform hover:scale-125 ${
                                  esFalta ? "bg-rose-600 hover:bg-rose-700" : (esJust ? "bg-amber-500 hover:bg-amber-600" : "bg-emerald-600 hover:bg-emerald-700")
                                }`}
                              >
                                {esFalta ? "✖" : (esJust ? "ℹ" : "✔")}
                              </button>
                            );
                          })}
                        </div>
                      )}
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>
      )}

      {/* VISTA 2: ASISTENCIA INDIVIDUAL POR ESTUDIANTE REAL EN BASE DE DATOS */}
      {seccion === "estudiantes" && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {/* LISTA DE ALUMNOS DEL CURSO */}
          <div className="bg-white border border-slate-200 rounded-xl overflow-hidden shadow-sm md:col-span-1">
            <div style={{ backgroundColor: PRIMARY }} className="p-3 text-white flex items-center justify-between">
              <span className="text-xs font-bold">Estudiantes Registrados en BD</span>
              <span className="bg-white/20 text-white text-[10px] font-extrabold px-2 py-0.5 rounded-full">
                {estudiantesMatriculados.length} alumnos
              </span>
            </div>

            {estudiantesMatriculados.length === 0 ? (
              <div className="p-6 text-center text-slate-400 text-xs">
                Este curso no tiene estudiantes matriculados en la base de datos.
              </div>
            ) : (
              <div className="divide-y divide-slate-100 max-h-[60vh] overflow-y-auto">
                {estudiantesMatriculados.map((m) => {
                  const esSel = estudianteSel?.idMatricula === m.idMatricula;
                  return (
                    <button
                      key={m.idMatricula}
                      onClick={() => setEstudianteSel(m)}
                      className={`w-full text-left p-3 text-xs transition flex items-center justify-between ${
                        esSel ? "bg-blue-50/80 border-l-4 border-[#243A76]" : "hover:bg-slate-50"
                      }`}
                    >
                      <div>
                        <p className="font-bold text-slate-800">{getNombreAlumno(m)}</p>
                        <p className="text-[10px] text-slate-400 font-mono">Cédula: {getCedulaAlumno(m)}</p>
                      </div>
                      <span className="text-slate-400 text-sm">→</span>
                    </button>
                  );
                })}
              </div>
            )}
          </div>

          {/* FICHA INDIVIDUAL DE ASISTENCIA DEL ALUMNO */}
          <div className="bg-white border border-slate-200 rounded-xl p-5 shadow-sm md:col-span-2">
            {estudianteSel ? (
              <div className="space-y-4">
                <div className="border-b border-slate-100 pb-3 flex items-center justify-between flex-wrap gap-2">
                  <div>
                    <span className="text-[10px] font-bold text-blue-900 uppercase tracking-wider block">Reporte Individual de Asistencia</span>
                    <h2 className="text-base font-extrabold text-slate-800">{getNombreAlumno(estudianteSel)}</h2>
                    <p className="text-xs text-slate-500 font-mono">Cédula: {getCedulaAlumno(estudianteSel)} · Curso: {gradoActualObj.nombre} "A"</p>
                  </div>
                  <span className="bg-emerald-100 text-emerald-800 font-extrabold text-xs px-3 py-1 rounded-full">
                    ESTUDIANTE ACTIVO
                  </span>
                </div>

                {/* ASISTENCIA MATERIA POR MATERIA */}
                <div className="space-y-3">
                  {materiasDelGrado.length === 0 ? (
                    <div className="p-8 text-center text-slate-400 text-xs">
                      Este curso no cuenta con asignaturas configuradas.
                    </div>
                  ) : (
                    materiasDelGrado.map((mat) => {
                      const asistList = asistenciasGrpc[mat.idAsignacion] || [];
                      const totalSes = asistList.length;
                      const ausentes = asistList.filter((a) => a.estado === "AUSENTE" || a.estado === "FALTA").length;
                      const porcentaje = totalSes > 0 ? Math.round(((totalSes - ausentes) / totalSes) * 100) : 0;

                      return (
                        <div key={mat.idAsignacion} className="border border-slate-200 rounded-xl p-3 bg-slate-50/50">
                          <div className="flex items-center justify-between mb-2">
                            <div>
                              <span className="font-bold text-slate-800 text-xs">{mat.materiaNombre}</span>
                              <span className="text-[10px] text-slate-500 block">{mat.docenteNombre}</span>
                            </div>
                            <div className="text-right">
                              <span className="text-sm font-black text-slate-800">{totalSes > 0 ? `${porcentaje}%` : "—"}</span>
                              <span className="text-[9px] font-bold text-emerald-700 block">Asistencia</span>
                            </div>
                          </div>

                          <div className="flex flex-wrap gap-1 items-center">
                            {asistList.length === 0 ? (
                              <span className="text-[10px] text-slate-400 italic">Sin sesiones registradas en gRPC</span>
                            ) : (
                              asistList.slice(0, 32).map((s, idx) => {
                                const esFalta = s.estado === "AUSENTE" || s.estado === "FALTA";
                                return (
                                  <span
                                    key={idx}
                                    className={`w-4 h-4 rounded-full flex items-center justify-center text-[9px] font-bold text-white ${
                                      esFalta ? "bg-rose-600" : "bg-emerald-600"
                                    }`}
                                  >
                                    {esFalta ? "✖" : "✔"}
                                  </span>
                                );
                              })
                            )}
                          </div>
                        </div>
                      );
                    })
                  )}
                </div>
              </div>
            ) : (
              <div className="text-center py-12 text-slate-400 text-xs">
                Selecciona un estudiante de la lista izquierda para visualizar su reporte individual.
              </div>
            )}
          </div>
        </div>
      )}

      {/* MODAL VER DETALLE DE SESIÓN */}
      {modalSesion && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl max-w-md w-full overflow-hidden shadow-2xl border border-slate-200">
            <div style={{ backgroundColor: PRIMARY }} className="p-4 text-white flex items-center justify-between">
              <div>
                <h3 className="font-bold text-sm">Sesión N° {modalSesion.num}</h3>
                <p className="text-xs text-white/80">{modalSesion.materia}</p>
              </div>
              <button onClick={() => setModalSesion(null)} className="w-7 h-7 rounded-full bg-white/20 hover:bg-white/30 flex items-center justify-center font-bold text-xs text-white">✕</button>
            </div>

            <div className="p-5 space-y-3 text-xs">
              <div className="grid grid-cols-2 gap-2 bg-slate-50 p-3 rounded-lg border border-slate-200 font-mono">
                <div>
                  <span className="text-slate-400 block text-[10px] uppercase font-sans">Fecha</span>
                  <span className="font-bold text-slate-700">{modalSesion.sesion.fecha || "2026-05-15"}</span>
                </div>
                <div>
                  <span className="text-slate-400 block text-[10px] uppercase font-sans">Hora</span>
                  <span className="font-bold text-slate-700">{modalSesion.sesion.hora || "08:00 a.m."}</span>
                </div>
              </div>

              <div>
                <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block mb-1">Docente Asignado en BD</span>
                <span className="font-semibold text-slate-800">{modalSesion.docente}</span>
              </div>

              <div className="flex items-center justify-between pt-2">
                <span className="font-bold text-slate-600">Estado de Asistencia:</span>
                <span className={`px-3 py-1 rounded-full text-white font-extrabold ${modalSesion.sesion.estado === "AUSENTE" || modalSesion.sesion.estado === "FALTA" ? "bg-rose-600" : "bg-emerald-600"}`}>
                  {modalSesion.sesion.estado === "AUSENTE" || modalSesion.sesion.estado === "FALTA" ? "✖ FALTA REGISTRADA" : "✔ PRESENTE"}
                </span>
              </div>
            </div>

            <div className="p-3 bg-slate-50 border-t border-slate-200 text-right">
              <button onClick={() => setModalSesion(null)} style={{ backgroundColor: PRIMARY }} className="text-white text-xs font-bold px-4 py-1.5 rounded-lg transition">
                Cerrar
              </button>
            </div>
          </div>
        </div>
      )}
    </Layout>
  );
}
