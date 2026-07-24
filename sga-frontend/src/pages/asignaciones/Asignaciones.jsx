import { useState, useEffect } from "react";
import axios from "axios";
import Layout from "../../components/Layout";

const API = "http://localhost:8080/api";
const PRIMARY = "#243A76";

const menuItems = [
  { id: "lista", label: "Asignaciones", icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" /></svg> },
  { id: "nuevo", label: "Nueva asignación", icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg> },
];

const formVacio = {
  idDocente: "",
  idAsignatura: "",
  idGrado: "",
  idParalelo: "",
  idAnoLectivo: "",
  esTutor: false,
};

export default function Asignaciones() {
  const [seccion, setSeccion] = useState("lista");
  const [asignaciones, setAsignaciones] = useState([]);
  const [docentes, setDocentes] = useState([]);
  const [asignaturas, setAsignaturas] = useState([]);
  const [grados, setGrados] = useState([]);
  const [paralelos, setParalelos] = useState([]);
  const [anosLectivos, setAnosLectivos] = useState([]);
  const [form, setForm] = useState(formVacio);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const token = localStorage.getItem("token");
  const H = { Authorization: `Bearer ${token}` };

  const cargar = () => {
    setLoading(true);
    axios.get(`${API}/asignaciones`, { headers: H })
      .then(r => setAsignaciones(r.data))
      .catch(() => setError("Error al cargar asignaciones"))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    cargar();
    axios.get(`${API}/asignaciones/docentes`, { headers: H }).then(r => setDocentes(r.data)).catch(() => {});
    axios.get(`${API}/asignaturas`, { headers: H }).then(r => setAsignaturas(r.data)).catch(() => {});
    axios.get(`${API}/grados`, { headers: H }).then(r => setGrados(r.data)).catch(() => {});
    axios.get(`${API}/anos-lectivos`, { headers: H }).then(r => setAnosLectivos(r.data)).catch(() => {});
  }, []);

  useEffect(() => {
    if (success) { const t = setTimeout(() => setSuccess(""), 4000); return () => clearTimeout(t); }
  }, [success]);

  // Al cambiar el grado, cargar sus paralelos
  useEffect(() => {
    if (form.idGrado) {
      axios.get(`${API}/asignaciones/grado/${form.idGrado}/paralelos`, { headers: H })
        .then(r => setParalelos(r.data))
        .catch(() => setParalelos([]));
    } else {
      setParalelos([]);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [form.idGrado]);

  const handleSeccion = (id) => {
    setSeccion(id);
    setError("");
    if (id === "nuevo") setForm(formVacio);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError("");
    try {
      await axios.post(`${API}/asignaciones`, {
        idDocente: parseInt(form.idDocente),
        idAsignatura: parseInt(form.idAsignatura),
        idGrado: parseInt(form.idGrado),
        idParalelo: parseInt(form.idParalelo),
        idAnoLectivo: parseInt(form.idAnoLectivo),
        esTutor: form.esTutor,
      }, { headers: H });
      setSuccess("Asignación creada correctamente.");
      setForm(formVacio);
      setSeccion("lista");
      cargar();
    } catch (err) {
      setError(err.response?.data?.message || err.response?.data?.error || "No se pudo crear la asignación.");
    } finally {
      setSaving(false);
    }
  };

  const toggleEstado = async (a) => {
    try {
      await axios.patch(`${API}/asignaciones/${a.idAsignacion}/estado?activo=${!a.activo}`, {}, { headers: H });
      cargar();
    } catch {
      setError("No se pudo cambiar el estado.");
    }
  };

  return (
    <Layout
      breadcrumb={["Inicio", "Asignaciones"]}
      sidebarTitle="Asignaciones"
      menuItems={menuItems}
      seccion={seccion}
      onSeccionChange={handleSeccion}
    >
      {error && <div className="mb-4 bg-red-50 border border-red-200 rounded-xl px-4 py-3 flex justify-between"><span className="text-red-600 text-sm">{error}</span><button onClick={() => setError("")} className="text-red-400 ml-4">✕</button></div>}
      {success && <div className="mb-4 bg-green-50 border border-green-200 rounded-xl px-4 py-3"><span className="text-green-700 text-sm">{success}</span></div>}

      <h1 className="text-lg font-bold text-slate-700 mb-1">Asignaciones</h1>
      <p className="text-slate-400 text-xs mb-5">Asigne docentes a cursos, asignaturas y paralelos.</p>

      {/* LISTA */}
      {seccion === "lista" && (
        <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
          {loading ? (
            <p className="text-center text-slate-400 py-10">Cargando asignaciones...</p>
          ) : asignaciones.length === 0 ? (
            <p className="text-center text-slate-400 py-10">No hay asignaciones registradas.</p>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-slate-50 text-slate-500 text-xs uppercase tracking-wide">
                  <th className="text-left px-4 py-3">Docente</th>
                  <th className="text-left px-4 py-3">Asignatura</th>
                  <th className="text-left px-4 py-3">Grado</th>
                  <th className="text-center px-4 py-3">Paralelo</th>
                  <th className="text-center px-4 py-3">Tutor</th>
                  <th className="text-center px-4 py-3">Estado</th>
                </tr>
              </thead>
              <tbody>
                {asignaciones.map((a) => (
                  <tr key={a.idAsignacion} className="border-t border-slate-100 hover:bg-slate-50">
                    <td className="px-4 py-3 font-medium text-slate-700">{a.docente}</td>
                    <td className="px-4 py-3 text-slate-600">{a.asignatura}</td>
                    <td className="px-4 py-3 text-slate-600">{a.grado}</td>
                    <td className="px-4 py-3 text-center text-slate-600">{a.paralelo || "—"}</td>
                    <td className="px-4 py-3 text-center">
                      {a.esTutor ? <span className="bg-blue-50 text-blue-700 text-xs font-semibold px-2 py-1 rounded">Tutor</span> : <span className="text-slate-300">—</span>}
                    </td>
                    <td className="px-4 py-3 text-center">
                      <button
                        onClick={() => toggleEstado(a)}
                        className={`text-xs font-semibold px-2 py-1 rounded ${a.activo ? "bg-green-100 text-green-700" : "bg-slate-100 text-slate-500"}`}
                      >
                        {a.activo ? "Activa" : "Inactiva"}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {/* NUEVO */}
      {seccion === "nuevo" && (
        <form onSubmit={handleSubmit} className="bg-white p-6 rounded-xl border border-slate-200 max-w-2xl space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Docente</label>
            <select required value={form.idDocente} onChange={(e) => setForm({ ...form, idDocente: e.target.value })}
              className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50">
              <option value="">Seleccione...</option>
              {docentes.map((d) => <option key={d.idDocente} value={d.idDocente}>{d.nombre}</option>)}
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Asignatura</label>
            <select required value={form.idAsignatura} onChange={(e) => setForm({ ...form, idAsignatura: e.target.value })}
              className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50">
              <option value="">Seleccione...</option>
              {asignaturas.map((s) => <option key={s.idAsignatura} value={s.idAsignatura}>{s.nombre}</option>)}
            </select>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Grado</label>
              <select required value={form.idGrado} onChange={(e) => setForm({ ...form, idGrado: e.target.value, idParalelo: "" })}
                className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50">
                <option value="">Seleccione...</option>
                {grados.map((g) => <option key={g.idGrado} value={g.idGrado}>{g.nombre}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Paralelo</label>
              <select required value={form.idParalelo} onChange={(e) => setForm({ ...form, idParalelo: e.target.value })}
                disabled={!form.idGrado}
                className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50 disabled:opacity-50">
                <option value="">{form.idGrado ? "Seleccione..." : "Elija un grado primero"}</option>
                {paralelos.map((p) => <option key={p.idParalelo} value={p.idParalelo}>Paralelo {p.letra}</option>)}
              </select>
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Año lectivo</label>
            <select required value={form.idAnoLectivo} onChange={(e) => setForm({ ...form, idAnoLectivo: e.target.value })}
              className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50">
              <option value="">Seleccione...</option>
              {anosLectivos.map((y) => <option key={y.idAnoLectivo} value={y.idAnoLectivo}>{y.nombre}</option>)}
            </select>
          </div>

          <label className="flex items-center gap-2 text-sm text-slate-600">
            <input type="checkbox" checked={form.esTutor} onChange={(e) => setForm({ ...form, esTutor: e.target.checked })} />
            Es docente tutor del curso
          </label>

          <div className="flex gap-3 pt-2">
            <button type="submit" disabled={saving} style={{ backgroundColor: PRIMARY }}
              className="text-white px-5 py-2 rounded-lg text-sm font-medium hover:opacity-90 disabled:opacity-60">
              {saving ? "Guardando..." : "Crear Asignación"}
            </button>
            <button type="button" onClick={() => { setForm(formVacio); setSeccion("lista"); }}
              className="px-5 py-2 rounded-lg text-sm font-medium text-slate-500 hover:bg-slate-100">
              Cancelar
            </button>
          </div>
        </form>
      )}
    </Layout>
  );
}
