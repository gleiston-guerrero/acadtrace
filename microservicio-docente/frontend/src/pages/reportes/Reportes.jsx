import { useState, useEffect } from "react";
import Layout from "../../components/Layout";
import {
  getMisAsignaciones,
  getEstudiantesPorAsignacion,
  getPromedioFormativo,
  getPromedioFinal,
} from "../../services/api";

const PRIMARY = "#243A76";

const menuReportes = [
  {
    id: "concentrado",
    label: "Concentrado de Notas",
    icon: (
      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
      </svg>
    ),
  },
];

export default function Reportes() {
  const [asignaciones, setAsignaciones] = useState([]);
  const [asignacionSel, setAsignacionSel] = useState("");
  const [trimestre, setTrimestre] = useState(1);
  const [filas, setFilas] = useState([]);
  const [loading, setLoading] = useState(false);

  const username = localStorage.getItem("username") || "Docente";

  useEffect(() => {
    cargarInicial();
  }, []);

  useEffect(() => {
    if (asignacionSel) cargarConcentrado();
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

  const cargarConcentrado = async () => {
    setLoading(true);
    try {
      const estRes = await getEstudiantesPorAsignacion(asignacionSel);
      const estudiantes = estRes.data || [];
      const resultado = [];
      for (const est of estudiantes) {
        let formativo = 0, final = 0;
        try {
          const [f, fn] = await Promise.all([
            getPromedioFormativo(est.idMatricula, trimestre),
            getPromedioFinal(est.idMatricula, trimestre),
          ]);
          formativo = f.data ?? 0;
          final = fn.data ?? 0;
        } catch (e) {
          // sin notas
        }
        resultado.push({
          idMatricula: est.idMatricula,
          nombre: `${est.estudiante?.apellidos || ""} ${est.estudiante?.nombres || ""}`.trim(),
          cedula: est.estudiante?.cedula || "",
          formativo: Number(formativo),
          final: Number(final),
        });
      }
      setFilas(resultado);
    } catch (error) {
      console.error("Error cargando concentrado:", error);
      setFilas([]);
    } finally {
      setLoading(false);
    }
  };

  const asignacionActual = asignaciones.find((a) => String(a.idAsignacion) === asignacionSel);
  const nombreAsignacion = (a) =>
    a ? `${a.asignatura?.nombre || ""} — ${a.grado?.nombre || ""}` : "";

  return (
    <Layout
      breadcrumb={["Inicio", "Reportes"]}
      sidebarTitle="REPORTES"
      menuItems={menuReportes}
      seccion="concentrado"
    >
      <div className="flex items-center justify-between mb-1">
        <h1 className="text-2xl font-bold text-slate-800">Reportes</h1>
        <button
          onClick={() => window.print()}
          style={{ backgroundColor: PRIMARY }}
          className="text-white px-4 py-2 rounded-lg text-sm font-medium hover:opacity-90 print:hidden"
        >
          Imprimir / Guardar PDF
        </button>
      </div>
      <p className="text-slate-500 mb-6 print:hidden">Genere el concentrado de notas del trimestre.</p>

      {/* Filtros */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 mb-4 grid grid-cols-1 md:grid-cols-2 gap-4 print:hidden">
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

      {/* Documento del reporte */}
      <div className="bg-white rounded-xl border border-slate-200 p-6">
        <div className="text-center mb-4">
          <h2 className="font-bold text-slate-800">Concentrado de Notas</h2>
          <p className="text-sm text-slate-500">{nombreAsignacion(asignacionActual)}</p>
          <p className="text-xs text-slate-400">Trimestre {trimestre} · Docente: {username}</p>
        </div>

        {loading ? (
          <p className="text-center text-slate-400 py-10">Generando reporte...</p>
        ) : filas.length === 0 ? (
          <p className="text-center text-slate-400 py-10">No hay estudiantes en este curso.</p>
        ) : (
          <table className="w-full text-sm border border-slate-200">
            <thead>
              <tr className="bg-slate-50 text-slate-600 text-xs uppercase">
                <th className="text-left px-3 py-2 border border-slate-200">#</th>
                <th className="text-left px-3 py-2 border border-slate-200">Estudiante</th>
                <th className="text-left px-3 py-2 border border-slate-200">Cédula</th>
                <th className="text-center px-3 py-2 border border-slate-200">Formativo</th>
                <th className="text-center px-3 py-2 border border-slate-200">Final</th>
              </tr>
            </thead>
            <tbody>
              {filas.map((f, i) => (
                <tr key={f.idMatricula}>
                  <td className="px-3 py-2 border border-slate-200 text-slate-500">{i + 1}</td>
                  <td className="px-3 py-2 border border-slate-200 font-medium text-slate-700">{f.nombre}</td>
                  <td className="px-3 py-2 border border-slate-200 text-slate-500">{f.cedula}</td>
                  <td className="px-3 py-2 border border-slate-200 text-center">{f.formativo.toFixed(2)}</td>
                  <td className="px-3 py-2 border border-slate-200 text-center font-semibold">{f.final.toFixed(2)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </Layout>
  );
}
