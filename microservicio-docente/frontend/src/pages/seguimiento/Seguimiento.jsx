import { useState, useEffect } from "react";
import Layout from "../../components/Layout";
import {
  getMisAsignaciones,
  getEstudiantesPorAsignacion,
  getPromedioFinal,
  getResumenAsistencia,
} from "../../services/api";

const PRIMARY = "#243A76";
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
];

export default function Seguimiento() {
  const [seccion, setSeccion] = useState("rendimiento");
  const [asignaciones, setAsignaciones] = useState([]);
  const [asignacionSel, setAsignacionSel] = useState("");
  const [trimestre, setTrimestre] = useState(1);
  const [filas, setFilas] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    cargarInicial();
  }, []);

  useEffect(() => {
    if (asignacionSel) cargarSeguimiento();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [asignacionSel, trimestre]);

  const cargarInicial = async () => {
    try {
      const res = await getMisAsignaciones();
      setAsignaciones(res.data || []);
      if (res.data?.length > 0) setAsignacionSel(String(res.data[0].idAsignacion));
    } catch (error) {
      console.error("Error cargando asignaciones:", error);
    }
  };

  const cargarSeguimiento = async () => {
    setLoading(true);
    try {
      const estRes = await getEstudiantesPorAsignacion(asignacionSel);
      const estudiantes = estRes.data || [];

      let resumenes = [];
      try {
        const resRes = await getResumenAsistencia(asignacionSel);
        resumenes = resRes.data?.resumenes || [];
      } catch (e) {
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
        } catch (e) {
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
        {seccion === "alertas"
          ? `Estudiantes con nota menor a ${NOTA_MINIMA} o asistencia menor a ${ASISTENCIA_MINIMA}%.`
          : "Monitoree el rendimiento y la asistencia de sus estudiantes."}
      </p>

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
    </Layout>
  );
}
