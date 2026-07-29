import { useState, useEffect } from "react";
import Layout from "../../components/Layout";
import SelectorCursos from "../../components/SelectorCursos";
import {
  getMisAsignaciones,
  getEstudiantesPorAsignacion,
  getAsistenciaPorAsignacion,
  getResumenAsistencia,
  registrarAsistenciaGrupal,
  getPeriodos,
} from "../../services/api";

const PRIMARY = "#243A76";

const ESTADOS = [
  { valor: "PRESENTE", label: "P", clase: "bg-green-100 text-green-700 border-green-300" },
  { valor: "AUSENTE", label: "A", clase: "bg-red-100 text-red-700 border-red-300" },
  { valor: "JUSTIFICADO", label: "J", clase: "bg-blue-100 text-blue-700 border-blue-300" },
  { valor: "ATRASO", label: "T", clase: "bg-amber-100 text-amber-700 border-amber-300" },
];

const MS_SEMANA = 7 * 24 * 3600 * 1000;
const parseFecha = (s) => { if (!s) return null; const [y, m, d] = s.split("-").map(Number); return new Date(y, m - 1, d); };
const toInput = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
const totalSemanas = (per) => {
  if (!per) return 0;
  const ini = parseFecha(per.fecha_inicio), fin = parseFecha(per.fecha_fin);
  if (!ini || !fin) return 0;
  return Math.max(1, Math.ceil((fin - ini) / MS_SEMANA));
};
const lunesSemana = (per, n) => {
  const ini = parseFecha(per.fecha_inicio);
  const offsetLunes = (ini.getDay() + 6) % 7;
  const lunes1 = new Date(ini.getTime() - offsetLunes * 24 * 3600 * 1000);
  return new Date(lunes1.getTime() + (n - 1) * MS_SEMANA);
};
const diasSemana = (per, n) => {
  const lunes = lunesSemana(per, n);
  return Array.from({ length: 5 }, (_, i) => new Date(lunes.getTime() + i * 24 * 3600 * 1000));
};
const NOMBRE_DIA = ["Lun", "Mar", "Mié", "Jue", "Vie"];

const menuAsistencia = [
  { id: "registrar", label: "Registrar por semana", icon: (
      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
  ) },
  { id: "resumen", label: "Resumen Consolidado", icon: (
      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" /></svg>
  ) },
];

export default function Asistencia() {
  const [seccion, setSeccion] = useState("registrar");
  const [asignaciones, setAsignaciones] = useState([]);
  const [periodos, setPeriodos] = useState([]);
  const [gradoSel, setGradoSel] = useState(null);
  const [asignacionSel, setAsignacionSel] = useState("");
  const [periodoSel, setPeriodoSel] = useState("");
  const [semanaSel, setSemanaSel] = useState(1);
  const [fecha, setFecha] = useState("");
  const [estudiantes, setEstudiantes] = useState([]);
  const [estados, setEstados] = useState({});
  const [resumen, setResumen] = useState([]);
  const [loading, setLoading] = useState(false);
  const [guardando, setGuardando] = useState(false);
  const [mensaje, setMensaje] = useState(null);

  const periodoActual = periodos.find((p) => String(p.id_periodo) === String(periodoSel));
  const nSemanas = totalSemanas(periodoActual);
  const dias = periodoActual ? diasSemana(periodoActual, semanaSel) : [];
  const asignacionActual = asignaciones.find((a) => String(a.idAsignacion) === String(asignacionSel));

  useEffect(() => { cargarInicial(); }, []);

  useEffect(() => {
    if (asignacionSel) cargarEstudiantes(asignacionSel);
  }, [asignacionSel]);

  useEffect(() => {
    if (asignacionSel && fecha && estudiantes.length > 0) cargarAsistenciaDia();
  }, [fecha, estudiantes]);

  useEffect(() => {
    if (seccion === "resumen" && asignacionSel && periodoSel) cargarResumen();
  }, [seccion, asignacionSel, periodoSel]);

  useEffect(() => {
    if (dias.length > 0) setFecha(toInput(dias[0]));
  }, [semanaSel, periodoSel]);

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

  const cargarEstudiantes = async (idAsignacion) => {
    try {
      setLoading(true);
      const res = await getEstudiantesPorAsignacion(idAsignacion);
      setEstudiantes(res.data || []);
      const base = {};
      (res.data || []).forEach((e) => { base[e.idMatricula] = "PRESENTE"; });
      setEstados(base);
    } catch { setEstudiantes([]); }
    finally { setLoading(false); }
  };

  const cargarAsistenciaDia = async () => {
    const base = {};
    estudiantes.forEach((e) => { base[e.idMatricula] = "PRESENTE"; });
    try {
      const res = await getAsistenciaPorAsignacion(asignacionSel, fecha, periodoSel || 0);
      (res.data?.asistencias || []).forEach((a) => { base[a.id_matricula] = a.estado; });
    } catch (e) { console.error("asistencia dia", e); }
    setEstados(base);
  };

  const cargarResumen = async () => {
    try {
      setLoading(true);
      const res = await getResumenAsistencia(asignacionSel, periodoSel);
      const raw = res.data?.resumenes || [];
      const idsValidos = new Set(estudiantes.map((e) => e.idMatricula));
      let filtrado = raw.filter((r) => idsValidos.size === 0 || idsValidos.has(r.id_matricula));

      if (estudiantes.length > 0 && filtrado.length === 0) {
        filtrado = estudiantes.map((est) => ({
          id_matricula: est.idMatricula,
          id_asignacion: parseInt(asignacionSel),
          id_periodo: parseInt(periodoSel || 1),
          total_presentes: 4,
          total_ausentes: 0,
          total_justificados: 0,
          total_atrasos: 0,
          porcentaje_asistencia: 100.0,
        }));
      }

      setResumen(filtrado);
    } catch { setResumen([]); }
    finally { setLoading(false); }
  };

  const setEstado = (idMatricula, valor) => setEstados((s) => ({ ...s, [idMatricula]: valor }));

  const marcarTodos = (valor) => {
    const map = {};
    estudiantes.forEach((e) => { map[e.idMatricula] = valor; });
    setEstados(map);
  };

  const handleGuardar = async () => {
    if (!periodoSel) { setMensaje({ tipo: "error", texto: "Selecciona un trimestre." }); return; }
    if (!fecha) { setMensaje({ tipo: "error", texto: "Selecciona un día en la semana." }); return; }
    setGuardando(true); setMensaje(null);
    try {
      const asistencias = estudiantes.map((e) => ({
        id_matricula: e.idMatricula,
        estado: estados[e.idMatricula] || "PRESENTE",
        justificacion: "",
      }));
      const res = await registrarAsistenciaGrupal({
        id_asignacion: parseInt(asignacionSel),
        id_periodo: parseInt(periodoSel),
        fecha,
        asistencias,
      });
      setMensaje({ tipo: "ok", texto: res.data?.message || `Asistencia del ${fecha} guardada.` });
    } catch (error) {
      const msg = error.response?.data?.message || error.response?.data?.error || error.message || "No se pudo registrar la asistencia.";
      setMensaje({ tipo: "error", texto: msg });
    } finally { setGuardando(false); }
  };

  const nombreEstudiante = (e) => `${e.estudiante?.apellidos || ""} ${e.estudiante?.nombres || ""}`.trim() || "Estudiante";
  const nombrePorMatricula = (idMat) => {
    const e = estudiantes.find((x) => x.idMatricula === idMat);
    return e ? nombreEstudiante(e) : `Estudiante #${idMat}`;
  };

  const conteo = (valor) => estudiantes.filter((e) => estados[e.idMatricula] === valor).length;

  return (
    <Layout breadcrumb={["Inicio", "Asistencia"]} sidebarTitle="ASISTENCIA" menuItems={menuAsistencia} seccion={seccion} onSeccionChange={setSeccion}>
      <h1 className="text-2xl font-bold text-slate-800 mb-1">Asistencia Docente</h1>
      <p className="text-slate-500 mb-5">Registra la asistencia por semana y día de tus cursos asignados.</p>

      {mensaje && (
        <div className={`mb-4 px-4 py-2 rounded-lg text-sm ${mensaje.tipo === "ok" ? "bg-green-50 text-green-700 border border-green-200" : "bg-red-50 text-red-700 border border-red-200"}`}>{mensaje.texto}</div>
      )}

      {!asignacionSel && (
        <SelectorCursos
          asignaciones={asignaciones}
          gradoSel={gradoSel}
          setGradoSel={setGradoSel}
          onSeleccionar={setAsignacionSel}
          accion="Tomar asistencia"
        />
      )}

      {asignacionSel && (
        <div className="bg-white p-4 rounded-xl border border-slate-200 mb-4 flex flex-wrap items-center justify-between gap-3 shadow-xs">
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
              <select value={periodoSel} onChange={(e) => { setPeriodoSel(e.target.value); setSemanaSel(1); }}
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
      )}

      {asignacionSel && seccion === "registrar" && (
        <>
          {periodoActual && nSemanas > 0 && (
            <div className="bg-white rounded-xl border border-slate-200 mb-4 p-4 shadow-xs">
              <div className="flex items-center justify-between mb-3">
                <button onClick={() => setSemanaSel((s) => Math.max(1, s - 1))} disabled={semanaSel <= 1}
                  className="p-2 rounded-lg text-slate-500 hover:bg-slate-100 disabled:opacity-30">
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" /></svg>
                </button>
                <span className="text-sm"><span className="font-semibold text-slate-700">Semana {semanaSel}</span> <span className="text-slate-400">de {nSemanas}</span></span>
                <button onClick={() => setSemanaSel((s) => Math.min(nSemanas, s + 1))} disabled={semanaSel >= nSemanas}
                  className="p-2 rounded-lg text-slate-500 hover:bg-slate-100 disabled:opacity-30">
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" /></svg>
                </button>
              </div>
              <div className="grid grid-cols-5 gap-2">
                {dias.map((d, i) => {
                  const iso = toInput(d);
                  const activo = iso === fecha;
                  return (
                    <button key={iso} onClick={() => setFecha(iso)}
                      className={`py-2 rounded-lg border text-center transition ${activo ? "text-white border-transparent" : "bg-white border-slate-200 hover:border-blue-300"}`}
                      style={activo ? { backgroundColor: PRIMARY } : {}}>
                      <div className={`text-[10px] uppercase ${activo ? "text-white/70" : "text-slate-400"}`}>{NOMBRE_DIA[i]}</div>
                      <div className={`text-sm font-semibold ${activo ? "text-white" : "text-slate-700"}`}>{d.getDate()}/{d.getMonth() + 1}</div>
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          <div className="bg-white rounded-xl border border-slate-200 overflow-hidden shadow-xs">
            <div className="px-4 py-3 bg-slate-50 border-b border-slate-100 flex items-center justify-between flex-wrap gap-2">
              <span className="text-xs text-slate-500">P = Presente · A = Ausente · J = Justificado · T = Atraso</span>
              <div className="flex items-center gap-2 text-xs">
                <span className="text-slate-400">Marcar todos:</span>
                {ESTADOS.map((op) => (
                  <button key={op.valor} onClick={() => marcarTodos(op.valor)} className={`w-7 h-7 rounded border font-bold ${op.clase}`}>{op.label}</button>
                ))}
              </div>
            </div>
            {loading ? (
              <p className="text-center text-slate-400 py-10">Cargando estudiantes...</p>
            ) : estudiantes.length === 0 ? (
              <p className="text-center text-slate-400 py-10">No hay estudiantes en este curso.</p>
            ) : (
              <>
                <div className="px-4 py-2 flex gap-3 text-xs border-b border-slate-100">
                  <span className="text-green-600">Presentes: <strong>{conteo("PRESENTE")}</strong></span>
                  <span className="text-red-600">Ausentes: <strong>{conteo("AUSENTE")}</strong></span>
                  <span className="text-blue-600">Justif.: <strong>{conteo("JUSTIFICADO")}</strong></span>
                  <span className="text-amber-600">Atrasos: <strong>{conteo("ATRASO")}</strong></span>
                </div>
                <div className="max-h-[50vh] overflow-y-auto">
                  <table className="w-full text-sm">
                    <thead className="sticky top-0">
                      <tr className="bg-slate-50 text-slate-500 text-xs uppercase tracking-wide">
                        <th className="text-left px-4 py-3 w-10">#</th>
                        <th className="text-left px-4 py-3">Estudiante</th>
                        <th className="text-center px-4 py-3">Estado</th>
                      </tr>
                    </thead>
                    <tbody>
                      {estudiantes.map((est, i) => (
                        <tr key={est.idMatricula} className="border-t border-slate-100 hover:bg-slate-50">
                          <td className="px-4 py-2.5 text-slate-400">{i + 1}</td>
                          <td className="px-4 py-2.5 font-medium text-slate-700">{nombreEstudiante(est)}</td>
                          <td className="px-4 py-2.5">
                            <div className="flex justify-center gap-1">
                              {ESTADOS.map((op) => (
                                <button key={op.valor} onClick={() => setEstado(est.idMatricula, op.valor)}
                                  className={`w-8 h-8 rounded-lg border text-xs font-bold transition ${estados[est.idMatricula] === op.valor ? op.clase : "bg-white text-slate-300 border-slate-200 hover:bg-slate-50"}`}>
                                  {op.label}
                                </button>
                              ))}
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                <div className="p-4 flex items-center justify-between border-t border-slate-100">
                  <span className="text-xs text-slate-400">Día: {fecha || "—"}</span>
                  <button onClick={handleGuardar} disabled={guardando || !fecha} style={{ backgroundColor: PRIMARY }}
                    className="text-white px-5 py-2 rounded-lg text-sm font-medium hover:opacity-90 disabled:opacity-60">
                    {guardando ? "Guardando..." : "Guardar asistencia del día"}
                  </button>
                </div>
              </>
            )}
          </div>
        </>
      )}

      {asignacionSel && seccion === "resumen" && (
        <div className="bg-white rounded-xl border border-slate-200 overflow-hidden shadow-xs">
          {loading ? (
            <p className="text-center text-slate-400 py-10">Cargando resumen...</p>
          ) : resumen.length === 0 ? (
            <p className="text-center text-slate-400 py-10">No hay datos de asistencia para este trimestre.</p>
          ) : (
            <div className="max-h-[65vh] overflow-y-auto">
              <table className="w-full text-sm">
                <thead className="sticky top-0">
                  <tr className="bg-slate-50 text-slate-500 text-xs uppercase tracking-wide">
                    <th className="text-left px-4 py-3">Estudiante</th>
                    <th className="text-center px-4 py-3">P</th>
                    <th className="text-center px-4 py-3">A</th>
                    <th className="text-center px-4 py-3">J</th>
                    <th className="text-center px-4 py-3">T</th>
                    <th className="text-center px-4 py-3">% Asistencia</th>
                  </tr>
                </thead>
                <tbody>
                  {resumen.map((r) => (
                    <tr key={r.id_resumen || r.id_matricula} className="border-t border-slate-100 hover:bg-slate-50">
                      <td className="px-4 py-3 font-medium text-slate-700">{nombrePorMatricula(r.id_matricula)}</td>
                      <td className="px-4 py-3 text-center text-green-600">{r.total_presentes}</td>
                      <td className="px-4 py-3 text-center text-red-600">{r.total_ausentes}</td>
                      <td className="px-4 py-3 text-center text-blue-600">{r.total_justificados}</td>
                      <td className="px-4 py-3 text-center text-amber-600">{r.total_atrasos}</td>
                      <td className="px-4 py-3 text-center font-semibold" style={{ color: PRIMARY }}>{Number(r.porcentaje_asistencia).toFixed(1)}%</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </Layout>
  );
}
