import { useState, useEffect } from "react";
import Layout from "../../components/Layout";
import {
  getMisAsignaciones,
  getEstudiantesPorAsignacion,
  getPromedioFinal,
  getResumenAsistencia,
  getPeriodos,
  getSeguimientos,
  createSeguimiento,
  updateSeguimiento,
  deleteSeguimiento,
} from "../../services/api";

const NOTA_MINIMA = 7;
const ASISTENCIA_MINIMA = 80;

const menuSeguimiento = [
  {
    id: "rendimiento",
    label: "Rendimiento",
    icon: (
      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
      </svg>
    ),
  },
  {
    id: "alertas",
    label: "Alertas",
    icon: (
      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
      </svg>
    ),
  },
  {
    id: "observaciones",
    label: "Observaciones",
    icon: (
      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 20h9M12 4h9m-9 8h9M3 4h.01M3 12h.01M3 20h.01" />
      </svg>
    ),
  },
];

const observacionVacia = {
  id_matricula: "",
  categoria: "ACADEMICO",
  descripcion: "",
  acciones_tomadas: "",
  requiere_followup: false,
  fecha_evento: new Date().toISOString().slice(0, 10),
};

export default function Seguimiento() {
  const [seccion, setSeccion] = useState("rendimiento");
  const [asignaciones, setAsignaciones] = useState([]);
  const [asignacionSel, setAsignacionSel] = useState("");
  const [trimestre, setTrimestre] = useState(1);
  const [filas, setFilas] = useState([]);
  const [estudiantes, setEstudiantes] = useState([]);
  const [periodos, setPeriodos] = useState([]);
  const [periodoSeguimiento, setPeriodoSeguimiento] = useState("");
  const [observaciones, setObservaciones] = useState([]);
  const [formObservacion, setFormObservacion] = useState(observacionVacia);
  const [edicionId, setEdicionId] = useState(null);
  const [mensaje, setMensaje] = useState("");
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    cargarInicial();
  }, []);

  useEffect(() => {
    if (asignacionSel) {
      cargarSeguimiento();
      cargarObservaciones();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [asignacionSel, trimestre, periodoSeguimiento]);

  const cargarInicial = async () => {
    try {
      const [asignacionesRes, periodosRes] = await Promise.all([getMisAsignaciones(), getPeriodos()]);
      setAsignaciones(asignacionesRes.data || []);
      setPeriodos(periodosRes.data || []);
      if (asignacionesRes.data?.length > 0) setAsignacionSel(String(asignacionesRes.data[0].idAsignacion));
      const periodoActivo = (periodosRes.data || []).find((periodo) => periodo.activo) || periodosRes.data?.[0];
      if (periodoActivo) setPeriodoSeguimiento(String(periodoActivo.id_periodo));
    } catch (error) {
      console.error("Error cargando asignaciones:", error);
    }
  };

  const cargarSeguimiento = async () => {
    setLoading(true);
    try {
      const estRes = await getEstudiantesPorAsignacion(asignacionSel);
      const estudiantes = estRes.data || [];
      setEstudiantes(estudiantes);

      let resumenes = [];
      try {
        const resRes = await getResumenAsistencia(asignacionSel);
        resumenes = resRes.data?.resumenes || [];
      } catch {
        resumenes = [];
      }
      const resumenPorMatricula = {};
      resumenes.forEach((r) => { resumenPorMatricula[r.id_matricula] = r; });

      const resultado = [];
      for (const est of estudiantes) {
        let nota = 0;
        try {
          const f = await getPromedioFinal(est.idMatricula, trimestre);
          nota = f.data ?? 0;
        } catch {
          nota = 0;
        }
        const resumen = resumenPorMatricula[est.idMatricula];
        const asistencia = resumen ? Number(resumen.porcentaje_asistencia) : 100;
        resultado.push({
          idMatricula: est.idMatricula,
          nombre: `${est.estudiante?.apellidos || ""} ${est.estudiante?.nombres || ""}`.trim(),
          nota: Number(nota),
          asistencia,
          enRiesgo: Number(nota) < NOTA_MINIMA || asistencia < ASISTENCIA_MINIMA,
        });
      }
      setFilas(resultado);
    } catch (error) {
      console.error("Error cargando seguimiento:", error);
      setFilas([]);
    } finally {
      setLoading(false);
    }
  };

  const cargarObservaciones = async () => {
    const asignacionActual = asignaciones.find((item) => String(item.idAsignacion) === String(asignacionSel));
    if (!asignacionActual?.paralelo?.id) return;
    try {
      const params = { id_paralelo: asignacionActual.paralelo.id };
      if (periodoSeguimiento) params.id_periodo = periodoSeguimiento;
      const response = await getSeguimientos(params);
      setObservaciones(response.data || []);
    } catch (error) {
      console.error("Error cargando observaciones:", error);
      setMensaje("No se pudieron cargar las observaciones.");
    }
  };

  const guardarObservacion = async (event) => {
    event.preventDefault();
    if (!periodoSeguimiento) {
      setMensaje("Seleccione un período de evaluación.");
      return;
    }
    // Pendiente de SSO: SGA Principal debe entregar idUsuario en el handoff/JWT o un endpoint autenticado.
    const userId = Number(localStorage.getItem("userId"));
    if (!Number.isInteger(userId) || userId <= 0) {
      setMensaje("No se pudo identificar al usuario autenticado. Inicie sesión nuevamente.");
      return;
    }
    const payload = {
      ...formObservacion,
      id_matricula: Number(formObservacion.id_matricula),
      id_periodo: Number(periodoSeguimiento),
      registrado_por: userId,
    };
    try {
      if (edicionId) await updateSeguimiento(edicionId, payload);
      else await createSeguimiento(payload);
      setFormObservacion(observacionVacia);
      setEdicionId(null);
      setMensaje("Observación guardada.");
      await cargarObservaciones();
    } catch (error) {
      console.error("Error guardando observación:", error);
      setMensaje("No se pudo guardar la observación.");
    }
  };

  const editarObservacion = (observacion) => {
    setEdicionId(observacion.id_seguimiento);
    setFormObservacion({
      id_matricula: String(observacion.id_matricula),
      categoria: observacion.categoria,
      descripcion: observacion.descripcion,
      acciones_tomadas: observacion.acciones_tomadas || "",
      requiere_followup: observacion.requiere_followup,
      fecha_evento: observacion.fecha_evento,
    });
    setSeccion("observaciones");
  };

  const eliminarObservacion = async (idSeguimiento) => {
    if (!confirm("¿Eliminar esta observación?")) return;
    try {
      await deleteSeguimiento(idSeguimiento);
      await cargarObservaciones();
    } catch {
      setMensaje("No se pudo eliminar la observación.");
    }
  };

  const nombreAsignacion = (a) =>
    `${a.asignatura?.nombre || "Asignatura"} — ${a.grado?.nombre || ""}`;

  const visibles = seccion === "alertas" ? filas.filter((f) => f.enRiesgo) : filas;

  return (
    <Layout
      breadcrumb={["Inicio", "Seguimiento"]}
      sidebarTitle="SEGUIMIENTO"
      menuItems={menuSeguimiento}
      seccion={seccion}
      onSeccionChange={setSeccion}
    >
      <h1 className="text-2xl font-bold text-slate-800 mb-1">Seguimiento Académico</h1>
      <p className="text-slate-500 mb-6">
        {seccion === "observaciones"
          ? "Registre observaciones y derive los casos que requieren atención DECE."
          : seccion === "alertas"
          ? `Estudiantes con nota menor a ${NOTA_MINIMA} o asistencia menor a ${ASISTENCIA_MINIMA}%.`
          : "Monitoree el rendimiento y la asistencia de sus estudiantes."}
      </p>

      {mensaje && <p className="mb-4 text-sm text-[#243A76]">{mensaje}</p>}

      {/* Filtros */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 mb-4 grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase tracking-wide">Curso</label>
          <select
            value={asignacionSel}
            onChange={(e) => setAsignacionSel(e.target.value)}
            className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50 focus:outline-none"
          >
            {asignaciones.length === 0 && <option value="">Sin asignaciones</option>}
            {asignaciones.map((a) => (
              <option key={a.idAsignacion} value={a.idAsignacion}>{nombreAsignacion(a)}</option>
            ))}
          </select>
        </div>
        <div>
          <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase tracking-wide">Trimestre</label>
          <select
            value={trimestre}
            onChange={(e) => setTrimestre(parseInt(e.target.value))}
            className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50 focus:outline-none"
          >
            <option value={1}>Primer Trimestre</option>
            <option value={2}>Segundo Trimestre</option>
            <option value={3}>Tercer Trimestre</option>
          </select>
        </div>
      </div>

      {seccion === "observaciones" ? (
        <div className="grid gap-5 lg:grid-cols-[minmax(0,0.9fr)_minmax(0,1.1fr)]">
          <form onSubmit={guardarObservacion} className="bg-white rounded-xl border border-slate-200 p-5 space-y-3">
            <h2 className="font-semibold text-slate-800">{edicionId ? "Editar observación" : "Nueva observación"}</h2>
            <select required value={formObservacion.id_matricula} onChange={(event) => setFormObservacion({ ...formObservacion, id_matricula: event.target.value })} className="w-full border rounded-lg p-2 text-sm">
              <option value="">Seleccione estudiante</option>
              {estudiantes.map((estudiante) => <option key={estudiante.idMatricula} value={estudiante.idMatricula}>{estudiante.estudiante?.apellidos} {estudiante.estudiante?.nombres}</option>)}
            </select>
            <select value={periodoSeguimiento} onChange={(event) => setPeriodoSeguimiento(event.target.value)} className="w-full border rounded-lg p-2 text-sm">{periodos.map((periodo) => <option key={periodo.id_periodo} value={periodo.id_periodo}>{periodo.nombre}</option>)}</select>
            <select value={formObservacion.categoria} onChange={(event) => setFormObservacion({ ...formObservacion, categoria: event.target.value })} className="w-full border rounded-lg p-2 text-sm"><option>ACADEMICO</option><option>CONDUCTUAL</option><option>DECE</option><option>MEDICO</option><option>FAMILIAR</option><option>OTRO</option></select>
            <textarea required rows="4" value={formObservacion.descripcion} onChange={(event) => setFormObservacion({ ...formObservacion, descripcion: event.target.value })} placeholder="Observación" className="w-full border rounded-lg p-2 text-sm" />
            <textarea rows="3" value={formObservacion.acciones_tomadas} onChange={(event) => setFormObservacion({ ...formObservacion, acciones_tomadas: event.target.value })} placeholder="Acciones tomadas" className="w-full border rounded-lg p-2 text-sm" />
            <input type="date" value={formObservacion.fecha_evento} onChange={(event) => setFormObservacion({ ...formObservacion, fecha_evento: event.target.value })} className="w-full border rounded-lg p-2 text-sm" />
            <label className="flex gap-2 text-sm"><input type="checkbox" checked={formObservacion.requiere_followup} onChange={(event) => setFormObservacion({ ...formObservacion, requiere_followup: event.target.checked })} />Requiere atención DECE</label>
            <button className="w-full bg-[#243A76] text-white py-2 rounded-lg">{edicionId ? "Actualizar" : "Guardar observación"}</button>
          </form>
          <div className="bg-white rounded-xl border border-slate-200 divide-y">
            {observaciones.length === 0 && <p className="p-8 text-center text-slate-400">No hay observaciones para este paralelo.</p>}
            {observaciones.map((observacion) => {
              const estudiante = estudiantes.find((item) => Number(item.idMatricula) === Number(observacion.id_matricula));
              return <article key={observacion.id_seguimiento} className="p-5"><div className="flex justify-between gap-3"><div><h3 className="font-semibold text-slate-700">{estudiante ? `${estudiante.estudiante?.apellidos} ${estudiante.estudiante?.nombres}` : `Matrícula ${observacion.id_matricula}`}</h3><p className="text-xs text-slate-400">{observacion.categoria} · {observacion.fecha_evento}</p></div>{observacion.requiere_followup && <span className="text-xs bg-red-100 text-red-700 rounded px-2 py-1 h-fit">DECE</span>}</div><p className="text-sm text-slate-600 mt-3 whitespace-pre-wrap">{observacion.descripcion}</p>{observacion.acciones_tomadas && <p className="text-xs text-slate-500 mt-2">Acciones: {observacion.acciones_tomadas}</p>}<div className="flex gap-3 mt-3 text-sm"><button onClick={() => editarObservacion(observacion)} type="button" className="text-[#243A76]">Editar</button><button onClick={() => eliminarObservacion(observacion.id_seguimiento)} type="button" className="text-red-600">Eliminar</button></div></article>;
            })}
          </div>
        </div>
      ) : (
      <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
        {loading ? (
          <p className="text-center text-slate-400 py-10">Cargando seguimiento...</p>
        ) : visibles.length === 0 ? (
          <p className="text-center text-slate-400 py-10">
            {seccion === "alertas" ? "No hay estudiantes en riesgo." : "No hay datos para este curso."}
          </p>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-slate-50 text-slate-500 text-xs uppercase tracking-wide">
                <th className="text-left px-4 py-3">Estudiante</th>
                <th className="text-center px-4 py-3">Promedio</th>
                <th className="text-center px-4 py-3">Asistencia</th>
                <th className="text-center px-4 py-3">Estado</th>
              </tr>
            </thead>
            <tbody>
              {visibles.map((f) => (
                <tr key={f.idMatricula} className="border-t border-slate-100 hover:bg-slate-50">
                  <td className="px-4 py-3 font-medium text-slate-700">{f.nombre}</td>
                  <td className={`px-4 py-3 text-center font-semibold ${f.nota < NOTA_MINIMA ? "text-red-600" : "text-slate-600"}`}>
                    {f.nota.toFixed(2)}
                  </td>
                  <td className={`px-4 py-3 text-center ${f.asistencia < ASISTENCIA_MINIMA ? "text-red-600" : "text-slate-600"}`}>
                    {f.asistencia.toFixed(1)}%
                  </td>
                  <td className="px-4 py-3 text-center">
                    {f.enRiesgo ? (
                      <span className="bg-red-100 text-red-700 text-xs font-semibold px-2 py-1 rounded">En riesgo</span>
                    ) : (
                      <span className="bg-green-100 text-green-700 text-xs font-semibold px-2 py-1 rounded">Al día</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
      )}
    </Layout>
  );
}
