import { useState, useEffect } from "react";
import axios from "axios";
import Layout from "../../components/Layout";

const API = "http://localhost:8080/api/configuracion/calificacion";
const PRIMARY = "#243A76";
const modalBg = { backgroundColor: "rgba(36, 58, 118, 0.5)" };

const menuItems = [
  { id: "ponderacion", label: "Ponderación", icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 6l3 1m0 0l-3 9a5.002 5.002 0 006.001 0M6 7l3 9M6 7l6-2m6 2l3-1m-3 1l-3 9a5.002 5.002 0 006.001 0M18 7l3 9m-3-9l-6-2m0-2v2m0 16V5m0 16H9m3 0h3" /></svg> },
  { id: "trimestres", label: "Trimestres", icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" /></svg> },
  { id: "aportes", label: "Tipos de aporte", icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" /></svg> },
  { id: "escala", label: "Escala cualitativa", icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 3.055A9.001 9.001 0 1020.945 13H11V3.055z" /></svg> },
];

export default function ConfiguracionCalificacion() {
  const [seccion, setSeccion] = useState("ponderacion");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const [esquema, setEsquema] = useState({ pesoFormativa: 70, pesoSumativa: 30 });
  const [periodos, setPeriodos] = useState([]);
  const [aportes, setAportes] = useState([]);
  const [escala, setEscala] = useState([]);
  const [niveles, setNiveles] = useState([]);
  const [nivelSel, setNivelSel] = useState("");

  const [periodoEdit, setPeriodoEdit] = useState(null);
  const [aporteEdit, setAporteEdit] = useState(null);
  const [showAporteModal, setShowAporteModal] = useState(false);
  const [formAporte, setFormAporte] = useState({ nombre: "", tipoEvaluacion: "FORMATIVA", orden: 0 });

  const token = localStorage.getItem("token");
  const headers = { Authorization: `Bearer ${token}` };

  const cargar = () => {
    setLoading(true);
    Promise.all([
      axios.get(`${API}/esquema`, { headers }).then(r => setEsquema(r.data)),
      axios.get(`${API}/periodos`, { headers }).then(r => setPeriodos(r.data)),
      axios.get(`${API}/aportes`, { headers }).then(r => setAportes(r.data)),
      axios.get(`${API}/escala`, { headers }).then(r => {
        setEscala(r.data);
        const nivs = [...new Map(r.data.map(e => [e.idNivel, e.nivel])).entries()];
        setNiveles(nivs);
        if (nivs.length && !nivelSel) setNivelSel(String(nivs[0][0]));
      }),
    ])
      .catch(() => setError("Error al cargar la configuración"))
      .finally(() => setLoading(false));
  };

  useEffect(() => { cargar(); }, []);

  useEffect(() => {
    if (success) { const t = setTimeout(() => setSuccess(""), 4000); return () => clearTimeout(t); }
  }, [success]);

  // ---- ponderación ----
  const guardarEsquema = async (e) => {
    e.preventDefault();
    setSaving(true); setError("");
    try {
      const r = await axios.put(`${API}/esquema`, {
        pesoFormativa: Number(esquema.pesoFormativa),
        pesoSumativa: Number(esquema.pesoSumativa),
      }, { headers });
      setEsquema(r.data);
      setSuccess("Ponderación guardada correctamente.");
    } catch (err) {
      setError(err.response?.data?.message || "Los pesos deben sumar 100");
    } finally { setSaving(false); }
  };

  // ---- trimestres ----
  const guardarPeriodo = async (e) => {
    e.preventDefault();
    setSaving(true); setError("");
    try {
      await axios.put(`${API}/periodos/${periodoEdit.idPeriodo}`, periodoEdit, { headers });
      setPeriodoEdit(null);
      setSuccess("Trimestre actualizado.");
      cargar();
    } catch (err) {
      setError(err.response?.data?.message || "Error al actualizar el trimestre");
    } finally { setSaving(false); }
  };

  // ---- aportes ----
  const guardarAporte = async (e) => {
    e.preventDefault();
    setSaving(true); setError("");
    try {
      if (aporteEdit) await axios.put(`${API}/aportes/${aporteEdit.idTipoAporte}`, formAporte, { headers });
      else await axios.post(`${API}/aportes`, formAporte, { headers });
      setShowAporteModal(false); setAporteEdit(null);
      setFormAporte({ nombre: "", tipoEvaluacion: "FORMATIVA", orden: 0 });
      setSuccess("Tipo de aporte guardado.");
      cargar();
    } catch (err) {
      setError(err.response?.data?.message || "Error al guardar el aporte");
    } finally { setSaving(false); }
  };

  const eliminarAporte = async (id) => {
    try {
      await axios.delete(`${API}/aportes/${id}`, { headers });
      setSuccess("Tipo de aporte eliminado.");
      cargar();
    } catch { setError("Error al eliminar"); }
  };

  const abrirAporte = (a) => {
    setAporteEdit(a);
    setFormAporte(a
      ? { nombre: a.nombre, tipoEvaluacion: a.tipoEvaluacion, orden: a.orden }
      : { nombre: "", tipoEvaluacion: "FORMATIVA", orden: 0 });
    setShowAporteModal(true);
  };

  const escalaFiltrada = escala.filter(e => !nivelSel || String(e.idNivel) === String(nivelSel));
  const formativos = aportes.filter(a => a.tipoEvaluacion === "FORMATIVA");
  const sumativos = aportes.filter(a => a.tipoEvaluacion === "SUMATIVA");

  return (
    <Layout
      breadcrumb={["Inicio", "Configuración", "Esquema de calificación"]}
      sidebarTitle="Calificación"
      menuItems={menuItems}
      seccion={seccion}
      onSeccionChange={setSeccion}
    >
      <div className="p-6">
        <div className="mb-5">
          <h1 className="text-xl font-bold" style={{ color: PRIMARY }}>Esquema de calificación</h1>
          <p className="text-sm text-slate-500">
            Define cómo se califica en el año lectivo activo. El portal docente consume esta
            configuración para calcular los promedios.
          </p>
        </div>

        {error && (
          <div className="mb-4 bg-red-50 border border-red-200 rounded-lg px-4 py-2 text-sm text-red-600">{error}</div>
        )}
        {success && (
          <div className="mb-4 bg-green-50 border border-green-200 rounded-lg px-4 py-2 text-sm text-green-700">{success}</div>
        )}

        {loading ? (
          <div className="text-slate-500 text-sm">Cargando configuración...</div>
        ) : (
          <>
            {/* ------------------------------ PONDERACIÓN ------------------------------ */}
            {seccion === "ponderacion" && (
              <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6 max-w-xl">
                <h2 className="font-semibold text-slate-700 mb-1">Ponderación del promedio trimestral</h2>
                <p className="text-xs text-slate-500 mb-5">
                  Promedio trimestral = Formativa × peso + Sumativa × peso. Ambos deben sumar 100.
                </p>
                <form onSubmit={guardarEsquema} className="space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Evaluación formativa (%)</label>
                      <input type="number" step="0.01" min="0" max="100" value={esquema.pesoFormativa}
                        onChange={e => setEsquema({ ...esquema, pesoFormativa: e.target.value })}
                        className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-slate-50 focus:ring-2 focus:ring-blue-500 focus:outline-none" />
                      <p className="text-xs text-slate-400 mt-1">Promedio de los aportes (tareas, lecciones...)</p>
                    </div>
                    <div>
                      <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Evaluación sumativa (%)</label>
                      <input type="number" step="0.01" min="0" max="100" value={esquema.pesoSumativa}
                        onChange={e => setEsquema({ ...esquema, pesoSumativa: e.target.value })}
                        className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-slate-50 focus:ring-2 focus:ring-blue-500 focus:outline-none" />
                      <p className="text-xs text-slate-400 mt-1">Proyecto interdisciplinario y examen</p>
                    </div>
                  </div>
                  <div className="text-sm">
                    Suma:{" "}
                    <span className={Number(esquema.pesoFormativa) + Number(esquema.pesoSumativa) === 100
                      ? "text-green-600 font-semibold" : "text-red-600 font-semibold"}>
                      {Number(esquema.pesoFormativa) + Number(esquema.pesoSumativa)}%
                    </span>
                  </div>
                  <button type="submit" disabled={saving}
                    style={{ backgroundColor: PRIMARY }}
                    className="text-white text-sm font-semibold px-5 py-2 rounded-lg disabled:opacity-60">
                    {saving ? "Guardando..." : "Guardar ponderación"}
                  </button>
                </form>
              </div>
            )}

            {/* ------------------------------- TRIMESTRES ------------------------------ */}
            {seccion === "trimestres" && (
              <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
                <table className="w-full text-sm">
                  <thead className="bg-slate-50 text-slate-500 text-xs uppercase">
                    <tr>
                      <th className="text-left px-4 py-3">Trimestre</th>
                      <th className="text-left px-4 py-3">Desde</th>
                      <th className="text-left px-4 py-3">Hasta</th>
                      <th className="text-left px-4 py-3">Estado</th>
                      <th className="px-4 py-3"></th>
                    </tr>
                  </thead>
                  <tbody>
                    {periodos.map(p => (
                      <tr key={p.idPeriodo} className="border-t border-slate-100">
                        <td className="px-4 py-3 font-medium text-slate-700">{p.nombre}</td>
                        <td className="px-4 py-3 text-slate-600">{p.fechaInicio}</td>
                        <td className="px-4 py-3 text-slate-600">{p.fechaFin}</td>
                        <td className="px-4 py-3">
                          <span className={`px-2 py-0.5 rounded-full text-xs ${p.activo ? "bg-green-50 text-green-600" : "bg-slate-100 text-slate-500"}`}>
                            {p.activo ? "Activo" : "Inactivo"}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-right">
                          <button onClick={() => setPeriodoEdit(p)} className="text-blue-600 hover:underline text-xs">Editar</button>
                        </td>
                      </tr>
                    ))}
                    {periodos.length === 0 && (
                      <tr><td colSpan={5} className="px-4 py-6 text-center text-slate-400">Sin trimestres configurados</td></tr>
                    )}
                  </tbody>
                </table>
              </div>
            )}

            {/* --------------------------- TIPOS DE APORTE ---------------------------- */}
            {seccion === "aportes" && (
              <div className="space-y-5">
                <div className="flex justify-end">
                  <button onClick={() => abrirAporte(null)} style={{ backgroundColor: PRIMARY }}
                    className="text-white text-sm font-semibold px-4 py-2 rounded-lg">+ Nuevo aporte</button>
                </div>
                {[["Evaluación formativa", formativos], ["Evaluación sumativa", sumativos]].map(([titulo, lista]) => (
                  <div key={titulo} className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
                    <div className="px-4 py-3 bg-slate-50 text-sm font-semibold text-slate-700">{titulo}</div>
                    <table className="w-full text-sm">
                      <tbody>
                        {lista.map(a => (
                          <tr key={a.idTipoAporte} className="border-t border-slate-100">
                            <td className="px-4 py-2.5 w-10 text-slate-400">{a.orden}</td>
                            <td className="px-4 py-2.5 text-slate-700">{a.nombre}</td>
                            <td className="px-4 py-2.5 text-right space-x-3">
                              <button onClick={() => abrirAporte(a)} className="text-blue-600 hover:underline text-xs">Editar</button>
                              <button onClick={() => eliminarAporte(a.idTipoAporte)} className="text-red-500 hover:underline text-xs">Eliminar</button>
                            </td>
                          </tr>
                        ))}
                        {lista.length === 0 && (
                          <tr><td colSpan={3} className="px-4 py-5 text-center text-slate-400">Sin aportes</td></tr>
                        )}
                      </tbody>
                    </table>
                  </div>
                ))}
              </div>
            )}

            {/* ----------------------------- ESCALA CUALITATIVA ----------------------- */}
            {seccion === "escala" && (
              <div className="space-y-4">
                <div className="flex items-center gap-3">
                  <label className="text-xs font-semibold text-slate-500 uppercase">Nivel educativo</label>
                  <select value={nivelSel} onChange={e => setNivelSel(e.target.value)}
                    className="px-3 py-2 border border-slate-200 rounded-lg text-sm bg-white">
                    {niveles.map(([id, nombre]) => <option key={id} value={id}>{nombre}</option>)}
                  </select>
                </div>
                <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
                  <table className="w-full text-sm">
                    <thead className="bg-slate-50 text-slate-500 text-xs uppercase">
                      <tr>
                        <th className="text-left px-4 py-3">Equivalente</th>
                        <th className="text-left px-4 py-3">Desde</th>
                        <th className="text-left px-4 py-3">Hasta</th>
                        <th className="text-left px-4 py-3">Descripción</th>
                      </tr>
                    </thead>
                    <tbody>
                      {escalaFiltrada.map(e => (
                        <tr key={e.idEscala} className="border-t border-slate-100">
                          <td className="px-4 py-2.5">
                            <span className="px-2 py-0.5 rounded font-bold text-xs" style={{ backgroundColor: "#EEF2FF", color: PRIMARY }}>
                              {e.equivalenteCualitativo}
                            </span>
                          </td>
                          <td className="px-4 py-2.5 text-slate-600">{e.notaMinima}</td>
                          <td className="px-4 py-2.5 text-slate-600">{e.notaMaxima}</td>
                          <td className="px-4 py-2.5 text-slate-600">{e.descripcion}</td>
                        </tr>
                      ))}
                      {escalaFiltrada.length === 0 && (
                        <tr><td colSpan={4} className="px-4 py-6 text-center text-slate-400">Sin escala configurada</td></tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </>
        )}
      </div>

      {/* MODAL editar trimestre */}
      {periodoEdit && (
        <div className="fixed inset-0 flex items-center justify-center z-50 px-4" style={modalBg}>
          <div className="bg-white rounded-xl shadow-2xl w-full max-w-md p-6">
            <h3 className="font-semibold text-slate-700 mb-4">Editar {periodoEdit.nombre}</h3>
            <form onSubmit={guardarPeriodo} className="space-y-3">
              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Nombre</label>
                <input value={periodoEdit.nombre} onChange={e => setPeriodoEdit({ ...periodoEdit, nombre: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-slate-50" required />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Desde</label>
                  <input type="date" value={periodoEdit.fechaInicio}
                    onChange={e => setPeriodoEdit({ ...periodoEdit, fechaInicio: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-slate-50" required />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Hasta</label>
                  <input type="date" value={periodoEdit.fechaFin}
                    onChange={e => setPeriodoEdit({ ...periodoEdit, fechaFin: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-slate-50" required />
                </div>
              </div>
              <div className="flex justify-end gap-2 pt-2">
                <button type="button" onClick={() => setPeriodoEdit(null)}
                  className="px-4 py-2 text-sm text-slate-500 hover:text-slate-700">Cancelar</button>
                <button type="submit" disabled={saving} style={{ backgroundColor: PRIMARY }}
                  className="text-white text-sm font-semibold px-4 py-2 rounded-lg disabled:opacity-60">
                  {saving ? "Guardando..." : "Guardar"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL aporte */}
      {showAporteModal && (
        <div className="fixed inset-0 flex items-center justify-center z-50 px-4" style={modalBg}>
          <div className="bg-white rounded-xl shadow-2xl w-full max-w-md p-6">
            <h3 className="font-semibold text-slate-700 mb-4">{aporteEdit ? "Editar" : "Nuevo"} tipo de aporte</h3>
            <form onSubmit={guardarAporte} className="space-y-3">
              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Nombre</label>
                <input value={formAporte.nombre} onChange={e => setFormAporte({ ...formAporte, nombre: e.target.value })}
                  placeholder="Ej. Lección Oral"
                  className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-slate-50" required />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Evaluación</label>
                  <select value={formAporte.tipoEvaluacion}
                    onChange={e => setFormAporte({ ...formAporte, tipoEvaluacion: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-white">
                    <option value="FORMATIVA">Formativa</option>
                    <option value="SUMATIVA">Sumativa</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Orden</label>
                  <input type="number" min="0" value={formAporte.orden}
                    onChange={e => setFormAporte({ ...formAporte, orden: Number(e.target.value) })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-slate-50" />
                </div>
              </div>
              <div className="flex justify-end gap-2 pt-2">
                <button type="button" onClick={() => { setShowAporteModal(false); setAporteEdit(null); }}
                  className="px-4 py-2 text-sm text-slate-500 hover:text-slate-700">Cancelar</button>
                <button type="submit" disabled={saving} style={{ backgroundColor: PRIMARY }}
                  className="text-white text-sm font-semibold px-4 py-2 rounded-lg disabled:opacity-60">
                  {saving ? "Guardando..." : "Guardar"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </Layout>
  );
}
