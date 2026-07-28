import { useState, useEffect } from "react";
import Layout from "../../components/Layout";
import SelectorCursos from "../../components/SelectorCursos";
import {
  getMisAsignaciones,
  getActividades,
  getEstudiantesPorAsignacion,
  registrarCalificacion,
  getCalificacionesPorActividad,
  getPeriodos,
} from "../../services/api";

const PRIMARY = "#243A76";

// ── Semanas dentro de un trimestre ───────────────────────────
const MS_SEMANA = 7 * 24 * 3600 * 1000;
const parseFecha = (s) => {
  if (!s) return null;
  const [y, m, d] = s.split("-").map(Number);
  return new Date(y, m - 1, d);
};
const totalSemanas = (per) => {
  if (!per) return 0;
  const ini = parseFecha(per.fecha_inicio), fin = parseFecha(per.fecha_fin);
  if (!ini || !fin) return 0;
  return Math.max(1, Math.ceil((fin - ini) / MS_SEMANA));
};
const semanaDeFecha = (fechaStr, per) => {
  if (!per || !fechaStr) return 1;
  const diff = Math.floor((parseFecha(fechaStr) - parseFecha(per.fecha_inicio)) / MS_SEMANA);
  return Math.min(Math.max(diff + 1, 1), totalSemanas(per));
};
const rangoSemana = (per, n) => {
  const base = parseFecha(per.fecha_inicio).getTime();
  return {
    ini: new Date(base + (n - 1) * MS_SEMANA),
    fin: new Date(base + (n - 1) * MS_SEMANA + 6 * 24 * 3600 * 1000),
  };
};
const fmtDia = (d) => d.toLocaleDateString("es-EC", { day: "2-digit", month: "2-digit", year: "numeric" });

const menuCalificaciones = [
  {
    id: "registrar",
    label: "Calificar por semana",
    icon: (
      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
      </svg>
    ),
  },
];

export default function Calificaciones() {
  const [asignaciones, setAsignaciones] = useState([]);
  const [periodos, setPeriodos] = useState([]);
  const [gradoSel, setGradoSel] = useState(null);
  const [asignacionSel, setAsignacionSel] = useState("");
  const [periodoSel, setPeriodoSel] = useState("");
  const [semanaSel, setSemanaSel] = useState(1);
  const [actividades, setActividades] = useState([]);
  const [actividadSel, setActividadSel] = useState(null);
  const [estudiantes, setEstudiantes] = useState([]);
  const [notas, setNotas] = useState({});
  const [loading, setLoading] = useState(false);
  const [cargandoNotas, setCargandoNotas] = useState(false);
  const [guardando, setGuardando] = useState(false);
  const [mensaje, setMensaje] = useState(null);

  const periodoActual = periodos.find((p) => String(p.id_periodo) === String(periodoSel));
  const nSemanas = totalSemanas(periodoActual);
  const asignacionActual = asignaciones.find((a) => String(a.idAsignacion) === String(asignacionSel));

  useEffect(() => { cargarInicial(); }, []);

  useEffect(() => {
    if (asignacionSel) {
      cargarActividades(asignacionSel);
      cargarEstudiantes(asignacionSel);
    }
    setActividadSel(null);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [asignacionSel]);

  useEffect(() => {
    if (actividadSel) cargarNotasActividad(actividadSel.idActividad);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [actividadSel]);

  const cargarInicial = async () => {
    try {
      const res = await getMisAsignaciones();
      setAsignaciones(res.data || []);
    } catch (e) {
      if (e.response?.status === 401) setMensaje({ tipo: "error", texto: "Tu sesión expiró. Vuelve a iniciar sesión." });
    }
    try {
      const per = await getPeriodos();
      const pers = per.data || [];
      setPeriodos(pers);
      const activo = pers.find((p) => p.activo) || pers[0];
      if (activo) setPeriodoSel(String(activo.id_periodo));
    } catch (e) { console.error("periodos", e); }
  };

  const cargarActividades = async (idAsignacion) => {
    try {
      const res = await getActividades(idAsignacion);
      setActividades(Array.isArray(res.data) ? res.data : (res.data?.actividades || []));
    } catch { setActividades([]); }
  };

  const cargarEstudiantes = async (idAsignacion) => {
    try {
      setLoading(true);
      const res = await getEstudiantesPorAsignacion(idAsignacion);
      setEstudiantes(res.data || []);
    } catch { setEstudiantes([]); }
    finally { setLoading(false); }
  };

  const cargarNotasActividad = async (idActividad) => {
    setCargandoNotas(true);
    try {
      const res = await getCalificacionesPorActividad(idActividad);
      const mapa = {};
      (res.data || []).forEach((c) => { mapa[c.id_matricula] = String(c.nota); });
      setNotas(mapa);
    } catch (e) { console.error("notas", e); setNotas({}); }
    finally { setCargandoNotas(false); }
  };

  const handleNota = (idMatricula, valor) => setNotas((n) => ({ ...n, [idMatricula]: valor }));

  const handleGuardar = async () => {
    if (!actividadSel) return;
    setGuardando(true); setMensaje(null);
    try {
      const pendientes = estudiantes.filter((e) => notas[e.idMatricula] !== undefined && notas[e.idMatricula] !== "");
      for (const est of pendientes) {
        await registrarCalificacion({
          idMatricula: est.idMatricula,
          idActividad: actividadSel.idActividad,
          nota: parseFloat(notas[est.idMatricula]),
          trimestre: parseInt(periodoSel),
        });
      }
      setMensaje({ tipo: "ok", texto: `Se guardaron ${pendientes.length} calificaciones.` });
      cargarNotasActividad(actividadSel.idActividad);
    } catch {
      setMensaje({ tipo: "error", texto: "No se pudieron guardar las calificaciones." });
    } finally { setGuardando(false); }
  };

  const nombreAsignacion = (a) => `${a.asignatura?.nombre || "Asignatura"} — ${a.grado?.nombre || ""}`;
  const nombreEstudiante = (e) => `${e.estudiante?.apellidos || ""} ${e.estudiante?.nombres || ""}`.trim() || "Estudiante";

  const actsSemana = actividades.filter(
    (a) => String(a.idPeriodo) === String(periodoSel) && semanaDeFecha(a.fechaEntrega, periodoActual) === semanaSel
  );
  const notasLlenas = estudiantes.filter((e) => notas[e.idMatricula] !== undefined && notas[e.idMatricula] !== "").length;

  return (
    <Layout
      breadcrumb={["Inicio", "Calificaciones"]}
      sidebarTitle="CALIFICACIONES"
      menuItems={menuCalificaciones}
      seccion="registrar"
      onSeccionChange={() => {}}
    >
      <h1 className="text-2xl font-bold text-slate-800 mb-1">Calificaciones</h1>
      <p className="text-slate-500 mb-5">Elige la semana, la actividad y registra las notas de tus estudiantes.</p>

      {mensaje && (
        <div className={`mb-4 px-4 py-2 rounded-lg text-sm ${mensaje.tipo === "ok" ? "bg-green-50 text-green-700 border border-green-200" : "bg-red-50 text-red-700 border border-red-200"}`}>
          {mensaje.texto}
        </div>
      )}

      {/* ===== SELECTOR DE CURSO POR TARJETAS (mismo diseño del módulo Grados) ===== */}
      {!asignacionSel && (
        <SelectorCursos
          asignaciones={asignaciones}
          gradoSel={gradoSel}
          setGradoSel={setGradoSel}
          onSeleccionar={setAsignacionSel}
          accion="Calificar"
        />
      )}

      {asignacionSel && (
      <>
      {/* Cabecera del curso + trimestre */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 mb-4 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className="w-11 h-11 rounded-xl flex items-center justify-center text-white font-bold flex-shrink-0" style={{ backgroundColor: PRIMARY }}>
            {asignacionActual?.paralelo?.letra || asignacionActual?.grado?.nombre?.[0] || "C"}
          </div>
          <div>
            <p className="text-sm font-bold text-slate-800">{asignacionActual?.asignatura?.nombre}</p>
            <p className="text-xs text-slate-500">{asignacionActual?.grado?.nombre} · Paralelo "{asignacionActual?.paralelo?.letra}"</p>
          </div>
        </div>
        <div className="flex items-end gap-3">
          <div>
            <label className="block text-[10px] font-semibold text-slate-400 mb-1 uppercase tracking-wide">Trimestre</label>
            <select value={periodoSel} onChange={(e) => { setPeriodoSel(e.target.value); setSemanaSel(1); setActividadSel(null); }}
              className="border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50">
              {periodos.length === 0 && <option value="">Sin trimestres</option>}
              {periodos.map((p) => <option key={p.id_periodo} value={p.id_periodo}>{p.nombre}</option>)}
            </select>
          </div>
          <button onClick={() => setAsignacionSel("")}
            className="border border-slate-200 rounded-lg px-3 py-2 text-sm text-slate-600 hover:bg-slate-50">
            Cambiar curso
          </button>
        </div>
      </div>

      {/* Navegador de semanas */}
      {periodoActual && nSemanas > 0 && (
        <div className="bg-white rounded-xl border border-slate-200 mb-4 px-4 py-3 flex items-center justify-between">
          <button onClick={() => { setSemanaSel((s) => Math.max(1, s - 1)); setActividadSel(null); }} disabled={semanaSel <= 1}
            className="p-2 rounded-lg text-slate-500 hover:bg-slate-100 disabled:opacity-30">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" /></svg>
          </button>
          <div className="text-center">
            <div className="flex items-center justify-center gap-2 text-sm">
              <span className="font-semibold text-slate-700">Semana</span>
              <input type="number" min={1} max={nSemanas} value={semanaSel}
                onChange={(e) => { setSemanaSel(Math.min(nSemanas, Math.max(1, parseInt(e.target.value) || 1))); setActividadSel(null); }}
                className="w-14 text-center border border-slate-200 rounded-lg py-1 text-sm bg-slate-50" />
              <span className="text-slate-400">de {nSemanas}</span>
            </div>
            <p className="text-xs text-slate-400 mt-1">
              {(() => { const r = rangoSemana(periodoActual, semanaSel); return `${fmtDia(r.ini)} — ${fmtDia(r.fin)}`; })()}
            </p>
          </div>
          <button onClick={() => { setSemanaSel((s) => Math.min(nSemanas, s + 1)); setActividadSel(null); }} disabled={semanaSel >= nSemanas}
            className="p-2 rounded-lg text-slate-500 hover:bg-slate-100 disabled:opacity-30">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" /></svg>
          </button>
        </div>
      )}

      {/* Actividades de la semana (tarjetas seleccionables) */}
      <div className="mb-4">
        <p className="text-xs font-semibold text-slate-400 uppercase tracking-wide mb-2">Actividades de la semana {semanaSel}</p>
        {actsSemana.length === 0 ? (
          <div className="bg-white rounded-xl border border-dashed border-slate-200 p-6 text-center text-slate-400 text-sm">
            No hay actividades en esta semana. Créalas en el módulo Actividades.
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
            {actsSemana.map((act) => {
              const activo = actividadSel?.idActividad === act.idActividad;
              return (
                <button key={act.idActividad} onClick={() => setActividadSel(act)}
                  className={`text-left p-4 rounded-xl border transition ${activo ? "border-transparent shadow-md text-white" : "border-slate-200 bg-white hover:border-blue-300"}`}
                  style={activo ? { backgroundColor: PRIMARY } : {}}>
                  <div className="flex items-center gap-2 mb-1">
                    <span className={`text-[10px] font-semibold px-1.5 py-0.5 rounded ${activo ? "bg-white/20" : "bg-blue-50 text-blue-700"}`}>{act.tipo}</span>
                    {act.esSumativa && <span className={`text-[10px] font-semibold px-1.5 py-0.5 rounded ${activo ? "bg-white/20" : "bg-purple-50 text-purple-700"}`}>SUMATIVA</span>}
                  </div>
                  <p className={`text-sm font-semibold ${activo ? "text-white" : "text-slate-700"}`}>{act.nombre}</p>
                  <p className={`text-xs mt-1 ${activo ? "text-white/70" : "text-slate-400"}`}>Entrega {act.fechaEntrega} · máx {act.notaMaxima} · {act.ponderacion}%</p>
                </button>
              );
            })}
          </div>
        )}
      </div>

      {/* Detalle de la actividad + lista de estudiantes para calificar */}
      {actividadSel && (
        <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
          <div style={{ backgroundColor: PRIMARY }} className="px-5 py-4 text-white">
            <div className="flex items-start justify-between gap-4">
              <div className="min-w-0">
                <h3 className="font-semibold">{actividadSel.nombre}</h3>
                <p className="text-white/70 text-xs mt-0.5">
                  {actividadSel.tipo} · Nota máx {actividadSel.notaMaxima} · Ponderación {actividadSel.ponderacion}% · Entrega {actividadSel.fechaEntrega}
                </p>
                {actividadSel.descripcion && <p className="text-white/80 text-sm mt-2">{actividadSel.descripcion}</p>}
              </div>
              <div className="text-right flex-shrink-0">
                <div className="text-2xl font-bold leading-none">{notasLlenas}/{estudiantes.length}</div>
                <div className="text-[10px] uppercase text-white/70">calificados</div>
              </div>
            </div>
            {/* Evidencia / material — placeholder (sin microservicio de estudiantes) */}
            <div className="mt-3 flex items-center gap-2 text-xs text-white/70 bg-white/10 rounded-lg px-3 py-2">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15.172 7l-6.586 6.586a2 2 0 102.828 2.828l6.414-6.586a4 4 0 00-5.656-5.656l-6.415 6.585a6 6 0 108.486 8.486L20.5 13" /></svg>
              Evidencia / material de la actividad — disponible cuando exista el microservicio de estudiantes.
            </div>
          </div>

          {cargandoNotas || loading ? (
            <p className="text-center text-slate-400 py-10 text-sm">Cargando estudiantes y notas...</p>
          ) : estudiantes.length === 0 ? (
            <p className="text-center text-slate-400 py-10 text-sm">No hay estudiantes en este curso.</p>
          ) : (
            <>
              <div className="max-h-[55vh] overflow-y-auto">
                <table className="w-full text-sm">
                  <thead className="sticky top-0">
                    <tr className="bg-slate-50 text-slate-500 text-xs uppercase tracking-wide">
                      <th className="text-left px-5 py-3 w-10">#</th>
                      <th className="text-left px-5 py-3">Estudiante</th>
                      <th className="text-left px-5 py-3">Cédula</th>
                      <th className="text-center px-5 py-3 w-32">Nota (0 – {actividadSel.notaMaxima})</th>
                    </tr>
                  </thead>
                  <tbody>
                    {estudiantes.map((est, i) => {
                      const val = notas[est.idMatricula] ?? "";
                      return (
                        <tr key={est.idMatricula} className="border-t border-slate-100 hover:bg-slate-50">
                          <td className="px-5 py-2.5 text-slate-400">{i + 1}</td>
                          <td className="px-5 py-2.5 font-medium text-slate-700">{nombreEstudiante(est)}</td>
                          <td className="px-5 py-2.5 text-slate-500">{est.estudiante?.cedula || "—"}</td>
                          <td className="px-5 py-2.5 text-center">
                            <input type="number" step="0.01" min="0" max={actividadSel.notaMaxima}
                              value={val}
                              onChange={(e) => handleNota(est.idMatricula, e.target.value)}
                              className={`w-24 border rounded-lg px-2 py-1.5 text-sm text-center ${val !== "" ? "border-blue-300 bg-blue-50/40 font-semibold text-slate-700" : "border-slate-200 bg-slate-50"}`}
                              placeholder="—" />
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
              <div className="p-4 flex items-center justify-between border-t border-slate-100">
                <span className="text-xs text-slate-400">{notasLlenas} de {estudiantes.length} con nota</span>
                <button onClick={handleGuardar} disabled={guardando}
                  style={{ backgroundColor: PRIMARY }}
                  className="text-white px-5 py-2 rounded-lg text-sm font-medium hover:opacity-90 disabled:opacity-60">
                  {guardando ? "Guardando..." : "Guardar notas"}
                </button>
              </div>
            </>
          )}
        </div>
      )}
      </>
      )}
    </Layout>
  );
}
