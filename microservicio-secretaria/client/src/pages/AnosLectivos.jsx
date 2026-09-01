import { useState, useEffect } from "react";
import api from "../utils/api";
import Layout from "../components/Layout";
import { useToast, useConfirm } from "../components/Toast";

const PRIMARY = "#243A76";
const modalBg = { backgroundColor: "rgba(36, 58, 118, 0.5)" };

const ic = (d) => (
  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d={d} />
  </svg>
);

const menuItems = [
  { id: "lista", label: "Años Lectivos", icon: ic("M4 6h16M4 10h16M4 14h16M4 18h16") },
];

export default function AnosLectivos() {
  const toast = useToast();
  const confirm = useConfirm();

  const [anos, setAnos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busqueda, setBusqueda] = useState("");
  const [seccion, setSeccion] = useState("lista");

  // Modales
  const [showModal, setShowModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [saving, setSaving] = useState(false);
  const [anoEdit, setAnoEdit] = useState(null);

  // Periodos de evaluación modal
  const [showPeriodosModal, setShowPeriodosModal] = useState(false);
  const [anoPeriodosSel, setAnoPeriodosSel] = useState(null);
  const [periodosList, setPeriodosList] = useState([]);
  const [loadingPeriodos, setLoadingPeriodos] = useState(false);

  const [form, setForm] = useState({ nombre: "", fechaInicio: "", fechaFin: "" });

  const cargar = () => {
    setLoading(true);
    api.get("/anos-lectivos")
      .then(r => setAnos(r.data || []))
      .catch(() => toast.error("Error", "No se pudieron cargar los años lectivos"))
      .finally(() => setLoading(false));
  };

  useEffect(() => { cargar(); }, []);

  const filtrados = anos.filter(a =>
    (a.nombre || "").toLowerCase().includes(busqueda.toLowerCase())
  );

  const handleCrear = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      await api.post("/anos-lectivos", form);
      toast.success("Año lectivo creado", "El período escolar y sus trimestres se configuraron correctamente.");
      setShowModal(false);
      setForm({ nombre: "", fechaInicio: "", fechaFin: "" });
      cargar();
    } catch (e) {
      toast.error("Error al crear", e.response?.data?.message || "No se pudo registrar el año lectivo");
    } finally {
      setSaving(false);
    }
  };

  const handleEditar = async (e) => {
    e.preventDefault();
    setSaving(true);
    const id = anoEdit.id_ano_lectivo || anoEdit.idAnoLectivo;
    try {
      await api.put(`/anos-lectivos/${id}`, {
        nombre: anoEdit.nombre,
        fechaInicio: anoEdit.fecha_inicio || anoEdit.fechaInicio,
        fechaFin: anoEdit.fecha_fin || anoEdit.fechaFin,
      });
      toast.success("Actualizado", "Año lectivo modificado exitosamente.");
      setShowEditModal(false);
      cargar();
    } catch (e) {
      toast.error("Error al actualizar", e.response?.data?.message || "No se pudo actualizar el año lectivo");
    } finally {
      setSaving(false);
    }
  };

  const handleEstablecerActual = async (a) => {
    const id = a.id_ano_lectivo || a.idAnoLectivo;
    const ok = await confirm({
      title: "¿Establecer como Año Lectivo Activo?",
      message: `El año lectivo "${a.nombre}" será el período escolar vigente para todas las matrículas, asistencias y calificaciones.`,
      confirmText: "Sí, activar período",
      type: "info"
    });
    if (!ok) return;

    try {
      await api.patch(`/anos-lectivos/${id}/activar`);
      toast.success("Período activo actualizado", `"${a.nombre}" es ahora el año escolar en curso.`);
      cargar();
    } catch (e) {
      toast.error("Error", e.response?.data?.message || "No se pudo cambiar el año lectivo actual");
    }
  };

  const abrirModalPeriodos = async (a) => {
    const id = a.id_ano_lectivo || a.idAnoLectivo;
    setAnoPeriodosSel(a);
    setShowPeriodosModal(true);
    setLoadingPeriodos(true);
    try {
      const res = await api.get(`/anos-lectivos/${id}/periodos`);
      setPeriodosList(res.data || []);
    } catch {
      setPeriodosList([]);
    } finally {
      setLoadingPeriodos(false);
    }
  };

  const anoActivo = anos.find(a => (a.es_actual ?? a.esActual));

  return (
    <Layout
      breadcrumb={["Inicio", "Años Lectivos"]}
      sidebarTitle="Períodos"
      menuItems={menuItems}
      seccion={seccion}
      onSeccionChange={setSeccion}
      headerRight={
        <button
          onClick={() => {
            const currentYear = new Date().getFullYear();
            setForm({
              nombre: `${currentYear}-${currentYear + 1}`,
              fechaInicio: `${currentYear}-05-05`,
              fechaFin: `${currentYear + 1}-02-28`
            });
            setShowModal(true);
          }}
          style={{ backgroundColor: PRIMARY }}
          className="flex items-center gap-2 text-white px-4 py-2 rounded-xl text-xs font-semibold hover:opacity-90 shadow-sm transition"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
          </svg>
          Nuevo Año Lectivo
        </button>
      }
    >
      <div className="p-6 max-w-7xl mx-auto space-y-6">

        {/* Tarjetas de Estadísticas Superiores */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center flex-shrink-0">
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <div>
              <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wide">Año Lectivo Activo</p>
              <p className="text-base font-extrabold text-slate-800 mt-0.5">{anoActivo?.nombre || "Ninguno"}</p>
            </div>
          </div>

          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center flex-shrink-0">
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
              </svg>
            </div>
            <div>
              <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wide">Total Períodos</p>
              <p className="text-base font-extrabold text-slate-800 mt-0.5">{anos.length} Años Escolares</p>
            </div>
          </div>

          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center flex-shrink-0">
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
            </div>
            <div>
              <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wide">Matrículas en Activo</p>
              <p className="text-base font-extrabold text-slate-800 mt-0.5">{anoActivo?.total_matriculados || 0} Alumnos</p>
            </div>
          </div>
        </div>

        {/* Barra de Búsqueda */}
        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
          <div className="relative w-full sm:w-80">
            <svg className="w-4 h-4 text-slate-400 absolute left-3 top-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <input
              type="text"
              placeholder="Buscar año lectivo..."
              value={busqueda}
              onChange={(e) => setBusqueda(e.target.value)}
              className="w-full pl-9 pr-8 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20"
            />
          </div>
        </div>

        {/* Tabla de Años Lectivos */}
        <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
          {loading ? (
            <div className="p-16 text-center text-slate-400 text-sm">Cargando períodos académicos...</div>
          ) : filtrados.length === 0 ? (
            <div className="p-16 text-center text-slate-400 text-sm">No se encontraron años lectivos registrados.</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-xs">
                <thead>
                  <tr className="bg-slate-50 border-b border-slate-200 text-slate-600 uppercase font-bold text-[11px] tracking-wider">
                    <th className="text-left px-5 py-3.5">Período Lectivo</th>
                    <th className="text-left px-5 py-3.5">Fecha Inicio</th>
                    <th className="text-left px-5 py-3.5">Fecha Fin</th>
                    <th className="text-center px-5 py-3.5">Trimestres</th>
                    <th className="text-center px-5 py-3.5">Matrículas</th>
                    <th className="text-center px-5 py-3.5">Estado</th>
                    <th className="text-center px-5 py-3.5">Acciones</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {filtrados.map((a) => {
                    const isActual = a.es_actual ?? a.esActual;
                    const fInicio = a.fecha_inicio || a.fechaInicio || "—";
                    const fFin = a.fecha_fin || a.fechaFin || "—";
                    return (
                      <tr key={a.id_ano_lectivo || a.idAnoLectivo} className="hover:bg-slate-50 transition">
                        <td className="px-5 py-4 font-bold text-slate-800 text-sm">
                          {a.nombre}
                        </td>
                        <td className="px-5 py-4 text-slate-600 font-medium">{fInicio}</td>
                        <td className="px-5 py-4 text-slate-600 font-medium">{fFin}</td>
                        <td className="px-5 py-4 text-center">
                          <button
                            onClick={() => abrirModalPeriodos(a)}
                            className="bg-blue-50 text-blue-700 hover:bg-blue-100 font-bold px-2.5 py-1 rounded-lg transition"
                          >
                            📅 Ver Trimestres ({a.total_periodos || 3})
                          </button>
                        </td>
                        <td className="px-5 py-4 text-center font-bold text-slate-700">
                          {a.total_matriculados || 0}
                        </td>
                        <td className="px-5 py-4 text-center">
                          {isActual ? (
                            <span className="text-[10px] font-bold px-2.5 py-1 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">
                              ● EN CURSO
                            </span>
                          ) : (
                            <span className="text-[10px] font-bold px-2.5 py-1 rounded-full bg-slate-100 text-slate-500">
                              CERRADO
                            </span>
                          )}
                        </td>
                        <td className="px-5 py-4 text-center">
                          <div className="flex items-center justify-center gap-1.5">
                            <button
                              onClick={() => { setAnoEdit(a); setShowEditModal(true); }}
                              className="p-1.5 rounded-lg text-slate-400 hover:text-blue-600 hover:bg-blue-50 transition"
                              title="Editar fechas"
                            >
                              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                              </svg>
                            </button>
                            {!isActual && (
                              <button
                                onClick={() => handleEstablecerActual(a)}
                                className="px-2.5 py-1 rounded-lg text-xs font-semibold bg-emerald-50 text-emerald-700 hover:bg-emerald-100 transition"
                                title="Establecer como año actual"
                              >
                                Activar
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* MODAL CREAR AÑO LECTIVO */}
      {showModal && (
        <div style={modalBg} className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl w-full max-w-md shadow-2xl overflow-hidden animate-in fade-in zoom-in duration-200">
            <div style={{ backgroundColor: PRIMARY }} className="p-5 text-white flex items-center justify-between">
              <h3 className="font-bold text-sm">Nuevo Año Lectivo</h3>
              <button onClick={() => setShowModal(false)} className="text-white/80 hover:text-white text-sm font-bold">✕</button>
            </div>

            <form onSubmit={handleCrear} className="p-6 space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase tracking-wide mb-1">Nombre / Período *</label>
                <input
                  type="text"
                  required
                  placeholder="Ej: 2025-2026"
                  value={form.nombre}
                  onChange={(e) => setForm({ ...form, nombre: e.target.value })}
                  className="w-full text-xs px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 font-semibold"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase tracking-wide mb-1">Fecha Inicio *</label>
                  <input
                    type="date"
                    required
                    value={form.fechaInicio}
                    onChange={(e) => setForm({ ...form, fechaInicio: e.target.value })}
                    className="w-full text-xs px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase tracking-wide mb-1">Fecha Fin *</label>
                  <input
                    type="date"
                    required
                    value={form.fechaFin}
                    onChange={(e) => setForm({ ...form, fechaFin: e.target.value })}
                    className="w-full text-xs px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
                  />
                </div>
              </div>

              <p className="text-[11px] text-slate-400 italic">
                ℹ️ Al crear el año escolar, el sistema generará automáticamente los 3 trimestres de evaluación distribuidos en este rango.
              </p>

              <div className="pt-2 flex items-center justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl transition"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  style={{ backgroundColor: PRIMARY }}
                  className="px-5 py-2 text-xs font-semibold text-white rounded-xl hover:opacity-90 transition shadow"
                >
                  {saving ? "Guardando..." : "Crear Año Lectivo"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL EDITAR AÑO LECTIVO */}
      {showEditModal && anoEdit && (
        <div style={modalBg} className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl w-full max-w-md shadow-2xl overflow-hidden animate-in fade-in zoom-in duration-200">
            <div style={{ backgroundColor: PRIMARY }} className="p-5 text-white flex items-center justify-between">
              <h3 className="font-bold text-sm">Editar Año Lectivo</h3>
              <button onClick={() => setShowEditModal(false)} className="text-white/80 hover:text-white text-sm font-bold">✕</button>
            </div>

            <form onSubmit={handleEditar} className="p-6 space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase tracking-wide mb-1">Nombre / Período *</label>
                <input
                  type="text"
                  required
                  value={anoEdit.nombre}
                  onChange={(e) => setAnoEdit({ ...anoEdit, nombre: e.target.value })}
                  className="w-full text-xs px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-blue-500/20 font-semibold"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase tracking-wide mb-1">Fecha Inicio *</label>
                  <input
                    type="date"
                    required
                    value={anoEdit.fecha_inicio || anoEdit.fechaInicio}
                    onChange={(e) => setAnoEdit({ ...anoEdit, fecha_inicio: e.target.value, fechaInicio: e.target.value })}
                    className="w-full text-xs px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-blue-500/20"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase tracking-wide mb-1">Fecha Fin *</label>
                  <input
                    type="date"
                    required
                    value={anoEdit.fecha_fin || anoEdit.fechaFin}
                    onChange={(e) => setAnoEdit({ ...anoEdit, fecha_fin: e.target.value, fechaFin: e.target.value })}
                    className="w-full text-xs px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-blue-500/20"
                  />
                </div>
              </div>

              <div className="pt-2 flex items-center justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setShowEditModal(false)}
                  className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl transition"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  style={{ backgroundColor: PRIMARY }}
                  className="px-5 py-2 text-xs font-semibold text-white rounded-xl hover:opacity-90 transition shadow"
                >
                  {saving ? "Guardando..." : "Actualizar"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL VER / ADMINISTRAR TRIMESTRES DE EVALUACIÓN */}
      {showPeriodosModal && anoPeriodosSel && (
        <div style={modalBg} className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl w-full max-w-lg shadow-2xl overflow-hidden animate-in fade-in zoom-in duration-200">
            <div style={{ backgroundColor: PRIMARY }} className="p-5 text-white flex items-center justify-between">
              <div>
                <h3 className="font-bold text-sm">Trimestres de Evaluación</h3>
                <p className="text-[11px] text-blue-200 mt-0.5">{anoPeriodosSel.nombre}</p>
              </div>
              <button onClick={() => setShowPeriodosModal(false)} className="text-white/80 hover:text-white text-sm font-bold">✕</button>
            </div>

            <div className="p-6 space-y-4">
              {loadingPeriodos ? (
                <div className="py-8 text-center text-slate-400 text-xs">Cargando trimestres...</div>
              ) : periodosList.length === 0 ? (
                <div className="py-8 text-center text-slate-400 text-xs">No hay trimestres registrados para este período.</div>
              ) : (
                <div className="space-y-3">
                  {periodosList.map((p, idx) => (
                    <div key={p.id_periodo || idx} className="p-3.5 bg-slate-50 border border-slate-200 rounded-2xl flex items-center justify-between">
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-bold text-xs text-slate-800">{p.nombre}</span>
                          <span className="text-[9px] font-extrabold bg-blue-50 text-blue-700 px-2 py-0.5 rounded-md">
                            TRIMESTRE {idx + 1}
                          </span>
                        </div>
                        <p className="text-[11px] text-slate-500 mt-1">
                          🗓️ Desde: <strong>{p.fecha_inicio}</strong> · Hasta: <strong>{p.fecha_fin}</strong>
                        </p>
                      </div>
                      <span className="text-[10px] font-bold text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-full">
                        Activo
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="p-4 bg-slate-50 border-t border-slate-100 flex items-center justify-end">
              <button
                type="button"
                onClick={() => setShowPeriodosModal(false)}
                style={{ backgroundColor: PRIMARY }}
                className="px-5 py-2 text-xs font-semibold text-white rounded-xl hover:opacity-90 transition shadow"
              >
                Cerrar
              </button>
            </div>
          </div>
        </div>
      )}
    </Layout>
  );
}