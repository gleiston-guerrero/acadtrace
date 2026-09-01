import { useState, useEffect } from "react";
import api, { apiPrincipal } from "../utils/api";
import Layout from "../components/Layout";
import { useToast, useConfirm } from "../components/Toast";

const PRIMARY = "#243A76";
const modalBg = { backgroundColor: "rgba(36, 58, 118, 0.5)" };

const ic = (d) => <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d={d} /></svg>;

const menuItems = [
  { id: "catalogo", label: "Catálogo", icon: ic("M4 6h16M4 10h16M4 14h16M4 18h16") },
  { id: "malla", label: "Malla por grado", icon: ic("M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253") },
];

const formVacio = { nombre: "", codigo: "", descripcion: "" };

const CARD_PALETTES = [
  { id: "navy", header: "bg-[#2b3c66]" },
  { id: "slate", header: "bg-[#475569]" },
  { id: "indigo", header: "bg-[#3b4266]" },
  { id: "teal", header: "bg-[#33535e]" },
  { id: "olive", header: "bg-[#4a5840]" },
  { id: "zinc", header: "bg-[#52525b]" },
];

export default function Asignaturas() {
  const [seccion, setSeccion] = useState("catalogo");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Catálogo
  const [asignaturas, setAsignaturas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editando, setEditando] = useState(null);
  const [form, setForm] = useState(formVacio);
  const [saving, setSaving] = useState(false);

  // Malla
  const [grados, setGrados] = useState([]);
  const [anoActual, setAnoActual] = useState(null);
  const [gradoMalla, setGradoMalla] = useState("");
  const [gradoSeleccionado, setGradoSeleccionado] = useState(null);
  const [malla, setMalla] = useState(null);
  const [loadingMalla, setLoadingMalla] = useState(false);
  const [showMallaModal, setShowMallaModal] = useState(false);
  const [showAgregar, setShowAgregar] = useState(false);
  const [nuevaMalla, setNuevaMalla] = useState({ idAsignatura: "", horasSemana: "" });
  const [cardColors, setCardColors] = useState({});
  const [activeMenuId, setActiveMenuId] = useState(null);

  const [resumenGrados, setResumenGrados] = useState({});

  useEffect(() => { cargarAsignaturas(); cargarBase(); }, []);
  useEffect(() => { if (success) { const t = setTimeout(() => setSuccess(""), 3500); return () => clearTimeout(t); } }, [success]);
  useEffect(() => {
    if (seccion === "malla" && gradoMalla && anoActual) {
      cargarMalla(gradoMalla);
    }
    /* eslint-disable-next-line */
  }, [seccion, gradoMalla, anoActual]);

  const cargarAsignaturas = () => {
    setLoading(true);
    api.get(`/asignaturas`).then(r => setAsignaturas(r.data || [])).catch(() => setError("Error al cargar asignaturas")).finally(() => setLoading(false));
  };

  const cargarBase = () => {
    api.get(`/grados`).then(r => setGrados(r.data || [])).catch(() => {});
    apiPrincipal.get(`/anos-lectivos/actual`).then(r => {
      setAnoActual(r.data);
      const idAno = r.data?.idAnoLectivo || r.data?.id_ano_lectivo;
      if (idAno) {
        api.get(`/malla/resumen-grados`, { params: { idAnoLectivo: idAno } })
          .then(res => setResumenGrados(res.data || {}))
          .catch(() => {});
      }
    }).catch(() => {});
  };

  const [horasEditMap, setHorasEditMap] = useState({});

  const cargarMalla = (idTarget) => {
    const idGradoVal = idTarget || gradoMalla;
    const idAno = anoActual?.idAnoLectivo || anoActual?.id_ano_lectivo;
    if (!idGradoVal || !idAno) return;
    setLoadingMalla(true);
    api.get(`/malla/grado/${idGradoVal}`, { params: { idAnoLectivo: idAno } })
      .then(r => {
        setMalla(r.data);
        const map = {};
        (r.data?.materias || []).forEach(m => {
          map[m.idAsignatura] = m.horasSemana || 4;
        });
        setHorasEditMap(map);
      })
      .catch(() => setMalla({ totalHoras: 0, materias: [] }))
      .finally(() => setLoadingMalla(false));
  };

  const toast = useToast();
  const confirm = useConfirm();

  const guardarCambiosMalla = async () => {
    if (!gradoMalla || !anoActual) return;

    const total = Object.values(horasEditMap).reduce((a, b) => a + (parseInt(b) || 0), 0);
    if (total > 30) {
      toast.error("Exceso de carga horaria", `La suma total de la malla (${total}h) no puede superar el límite máximo de 30 horas semanales. Por favor ajusta las horas.`);
      return;
    }

    setSaving(true);
    setError("");
    try {
      const cambios = Object.entries(horasEditMap).map(([idAsig, h]) => ({
        idAsignatura: parseInt(idAsig),
        horasSemana: parseInt(h) || 1
      }));
      await api.post(`/malla/actualizar-horas-grado`, {
        idGrado: parseInt(gradoMalla),
        idAnoLectivo: anoActual.idAnoLectivo || anoActual.id_ano_lectivo,
        cambios
      });
      toast.success("Malla guardada", "¡Horas de la malla actualizadas y sincronizadas con éxito!");
      setSuccess("¡Horas de la malla actualizadas y guardadas con éxito!");
      cargarMalla(gradoMalla);
      cargarBase();
    } catch (err) {
      const errMsg = err.response?.data?.message || err.response?.data?.mensaje || "Error al guardar los cambios de horas.";
      setError(errMsg);
      toast.error("Error al guardar malla", errMsg);
    } finally {
      setSaving(false);
    }
  };

  const abrirModalMalla = (g) => {
    const idG = g.id_grado || g.idGrado;
    setGradoMalla(String(idG));
    setGradoSeleccionado(g);
    setShowMallaModal(true);
    cargarMalla(String(idG));
  };

  const abrirCrear = () => { setEditando(null); setForm(formVacio); setError(""); setShowModal(true); };
  const abrirEditar = (a) => { setEditando(a); setForm({ nombre: a.nombre, codigo: a.codigo, descripcion: a.descripcion || "", horasSemanales: a.horas_semana || a.horasSemanales || 4 }); setError(""); setShowModal(true); };

  const guardarAsignatura = async (e) => {
    e.preventDefault();
    setSaving(true); setError("");
    const id = editando ? (editando.id_asignatura || editando.idAsignatura) : null;
    try {
      if (editando) await api.put(`/asignaturas/${id}`, form);
      else await api.post(`/asignaturas`, form);
      toast.success("Asignatura guardada", editando ? "Asignatura actualizada correctamente." : "Nueva asignatura creada en el catálogo.");
      setSuccess(editando ? "Asignatura actualizada." : "Asignatura creada.");
      setShowModal(false); cargarAsignaturas();
    } catch (err) {
      const errMsg = err.response?.data?.message || "No se pudo guardar la asignatura.";
      setError(errMsg);
      toast.error("Error al guardar", errMsg);
    } finally { setSaving(false); }
  };

  const toggleEstado = async (a) => {
    const id = a.id_asignatura || a.idAsignatura;
    const isActivo = a.activa ?? a.activo ?? true;
    try {
      await api.patch(`/asignaturas/${id}/estado?activo=${!isActivo}`);
      toast.info("Estado actualizado", `Asignatura ${!isActivo ? "activada" : "desactivada"}.`);
      cargarAsignaturas();
    } catch {
      toast.error("Error", "No se pudo cambiar el estado.");
    }
  };

  const agregarAMalla = async (e) => {
    e.preventDefault();
    if (!nuevaMalla.idAsignatura || !nuevaMalla.horasSemana) return;
    setSaving(true); setError("");
    const idAno = anoActual?.idAnoLectivo || anoActual?.id_ano_lectivo;
    try {
      await api.post(`/malla`, {
        idGrado: parseInt(gradoMalla), idAnoLectivo: idAno,
        idAsignatura: parseInt(nuevaMalla.idAsignatura), horasSemana: parseInt(nuevaMalla.horasSemana),
      });
      toast.success("Materia agregada", "La materia se agregó a la malla del grado.");
      setShowAgregar(false); setNuevaMalla({ idAsignatura: "", horasSemana: "" }); cargarMalla();
    } catch (err) {
      const errMsg = err.response?.data?.message || "No se pudo agregar.";
      setError(errMsg);
      toast.error("Error al agregar", errMsg);
    } finally { setSaving(false); }
  };

  const cambiarHoras = async (idMalla, horas) => {
    try { await api.patch(`/malla/${idMalla}?horasSemana=${horas}`); cargarMalla(); }
    catch { toast.error("Error", "No se pudo actualizar las horas."); }
  };

  const quitarDeMalla = async (idMalla) => {
    if (!idMalla) {
      toast.warning("Materia de distributivo", "Esta materia proviene del distributivo docente. Para retirarla, debes hacerlo en el módulo de Asignaciones.");
      return;
    }
    const isOk = await confirm({
      title: "¿Quitar materia de la malla?",
      message: "Se eliminará esta asignatura de la plantilla del grado.",
      confirmText: "Sí, quitar",
      cancelText: "Cancelar",
      type: "danger"
    });
    if (!isOk) return;
    try {
      await api.delete(`/malla/${idMalla}`);
      toast.success("Materia retirada", "La asignatura se eliminó de la malla.");
      cargarMalla();
    } catch (err) {
      const errMsg = err.response?.data?.message || "No se pudo quitar la materia.";
      toast.error("Error al quitar", errMsg);
    }
  };

  const disponiblesParaMalla = asignaturas.filter(a => (a.activa ?? a.activo) && !malla?.materias?.some(m => (m.idAsignatura || m.id_asignatura) === (a.id_asignatura || a.idAsignatura)));

  return (
    <Layout breadcrumb={["Inicio", "Asignaturas"]} sidebarTitle="Asignaturas" menuItems={menuItems} seccion={seccion} onSeccionChange={setSeccion}>


      {/* CATÁLOGO */}
      {seccion === "catalogo" && (
        <div>
          <div className="flex items-center justify-between mb-5">
            <div>
              <h1 className="text-xl font-bold text-slate-700">Catálogo de asignaturas</h1>
              <p className="text-sm text-slate-400 mt-0.5">Materias disponibles en la institución</p>
            </div>
            <button onClick={abrirCrear} style={{ backgroundColor: PRIMARY }} className="flex items-center gap-2 text-white px-5 py-2.5 rounded-lg text-sm font-medium hover:opacity-90 transition shadow-sm">
              {ic("M12 4v16m8-8H4")} Nueva asignatura
            </button>
          </div>

          <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
            {loading ? <div className="p-12 text-center text-slate-400 text-sm">Cargando...</div>
             : asignaturas.length === 0 ? <div className="p-12 text-center text-slate-400 text-sm">No hay asignaturas registradas.</div>
             : (
              <table className="w-full text-sm">
                <thead><tr style={{ backgroundColor: "#f8f9fc" }} className="border-b border-slate-100">
                  <th className="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase">Asignatura</th>
                  <th className="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase">Código</th>
                  <th className="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase">Descripción</th>
                  <th className="text-center px-4 py-3 text-xs font-semibold text-slate-500 uppercase">Estado</th>
                  <th className="text-center px-4 py-3 text-xs font-semibold text-slate-500 uppercase">Acciones</th>
                </tr></thead>
                <tbody>
                  {asignaturas.map(a => (
                    <tr key={a.idAsignatura} className="border-b border-slate-50 hover:bg-slate-50">
                      <td className="px-4 py-3 font-medium text-slate-700">{a.nombre}</td>
                      <td className="px-4 py-3"><span className="font-mono text-xs px-2 py-0.5 rounded" style={{ backgroundColor: "#eef0f7", color: PRIMARY }}>{a.codigo}</span></td>
                      <td className="px-4 py-3 text-slate-500 text-xs max-w-xs truncate">{a.descripcion || "—"}</td>
                      <td className="px-4 py-3 text-center"><span className={`text-[11px] font-semibold px-2 py-0.5 rounded ${a.activo ? "bg-green-100 text-green-700" : "bg-slate-200 text-slate-500"}`}>{a.activo ? "Activa" : "Inactiva"}</span></td>
                      <td className="px-4 py-3">
                        <div className="flex items-center justify-center gap-1">
                          <button onClick={() => abrirEditar(a)} title="Editar" className="p-1.5 rounded-lg text-slate-400 hover:text-blue-600 hover:bg-blue-50">
                            {ic("M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z")}
                          </button>
                          <button onClick={() => toggleEstado(a)} title={a.activo ? "Desactivar" : "Activar"} className="p-1.5 rounded-lg text-slate-400 hover:text-amber-600 hover:bg-amber-50">
                            {ic(a.activo ? "M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" : "M5 13l4 4L19 7")}
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      )}

      {/* MALLA POR GRADO */}
      {seccion === "malla" && (
        <div className="space-y-6">
          <div>
            <h1 className="text-xl font-bold text-slate-700">Malla curricular por grado</h1>
            <p className="text-sm text-slate-400 mt-0.5">
              Haz clic en cualquier tarjeta de curso para consultar y administrar sus materias asignadas {anoActual && `— ${anoActual.nombre}`}
            </p>
          </div>

          {/* CONTENEDOR DE TARJETAS DE CURSOS / GRADOS */}
          <div className="bg-slate-50 border border-slate-200/90 rounded-2xl p-6 shadow-sm">
            <div className="flex items-center justify-between mb-5">
              <div>
                <h2 className="text-base font-bold text-slate-800">Grados Académicos</h2>
                <p className="text-xs text-slate-400 mt-0.5">Selecciona un curso para gestionar su malla curricular</p>
              </div>
              {grados.length > 0 && (
                <span className="text-xs font-semibold px-3 py-1 rounded-full bg-white border border-slate-200 text-slate-600 shadow-2xs">
                  {grados.length} Cursos disponibles
                </span>
              )}
            </div>

            {grados.length === 0 ? (
              <div className="p-8 text-center text-slate-400 text-sm">Cargando cursos...</div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-3 gap-5">
                {grados.map((g, idx) => {
                  const savedColor = cardColors[g.idGrado] ?? (localStorage.getItem(`malla_color_${g.idGrado}`) ? parseInt(localStorage.getItem(`malla_color_${g.idGrado}`), 10) : idx % CARD_PALETTES.length);
                  const currentPalette = CARD_PALETTES[savedColor % CARD_PALETTES.length];
                  const isMenuOpen = activeMenuId === g.idGrado;

                  const cambiarColorCard = (e) => {
                    e.stopPropagation();
                    const next = (savedColor + 1) % CARD_PALETTES.length;
                    setCardColors(prev => ({ ...prev, [g.idGrado]: next }));
                    localStorage.setItem(`malla_color_${g.idGrado}`, String(next));
                    setActiveMenuId(null);
                  };

                  return (
                    <button
                      key={g.idGrado}
                      type="button"
                      onClick={() => abrirModalMalla(g)}
                      className="bg-white border border-slate-200/90 rounded-2xl overflow-hidden shadow-sm hover:shadow-xl hover:border-blue-500 transition-all duration-300 transform hover:-translate-y-1 flex flex-col text-left group cursor-pointer relative"
                    >
                      {/* BANDA DE ENCABEZADO SUPERIOR CON COLOR Y BOTÓN 3 PUNTOS */}
                      <div className={`${currentPalette.header} p-5 text-white relative`}>
                        <div className="flex items-center justify-between gap-2 mb-3">
                          <span className="bg-white/20 backdrop-blur-xs text-white text-[10px] font-extrabold uppercase tracking-wider px-2.5 py-1 rounded-md">
                            GRADO ACADÉMICO
                          </span>

                          <div className="flex items-center gap-2">
                            <span className="bg-emerald-500/90 text-white text-[10px] font-extrabold uppercase px-2 py-0.5 rounded-full shadow-2xs">
                              ACTIVO
                            </span>

                            {/* BOTÓN 3 PUNTOS (•••) */}
                            <div className="relative">
                              <span
                                onClick={(e) => {
                                  e.stopPropagation();
                                  setActiveMenuId(isMenuOpen ? null : g.idGrado);
                                }}
                                className="w-7 h-7 rounded-lg bg-white/20 hover:bg-white/30 text-white flex items-center justify-center text-xs font-bold transition shadow-xs cursor-pointer inline-flex"
                                title="Opciones"
                              >
                                •••
                              </span>

                              {/* MENÚ DESPLEGABLE LIMPIO SIN EMOTICONES */}
                              {isMenuOpen && (
                                <div
                                  className="absolute right-0 top-7 w-40 bg-white rounded-lg shadow-xl border border-slate-200 py-1 z-30 text-slate-700"
                                  onClick={(e) => e.stopPropagation()}
                                >
                                  <button
                                    type="button"
                                    onClick={cambiarColorCard}
                                    className="w-full text-left px-3 py-1.5 text-xs hover:bg-slate-50 text-slate-700 transition"
                                  >
                                    Cambiar color
                                  </button>
                                  <button
                                    type="button"
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      setActiveMenuId(null);
                                      abrirModalMalla(g);
                                    }}
                                    className="w-full text-left px-3 py-1.5 text-xs hover:bg-slate-50 text-slate-700 transition"
                                  >
                                    Abrir materias
                                  </button>
                                </div>
                              )}
                            </div>
                          </div>
                        </div>

                        <h3 className="text-lg font-black uppercase tracking-wide leading-snug drop-shadow-xs line-clamp-2">
                          {g.nombre}
                        </h3>

                        <p className="text-xs text-blue-100/80 font-medium mt-1">
                          Escuela Provincias Unidas
                        </p>
                      </div>

                      {/* CUERPO INFERIOR CON RESUMEN Y ACCIONES */}
                      <div className="p-4 bg-white flex-1 flex flex-col justify-between space-y-3">
                        <div className="grid grid-cols-3 gap-2 py-1">
                          {(() => {
                            const infoG = resumenGrados[g.idGrado] || {};
                            const hrs = infoG.totalHoras || 0;
                            let styleHrs = "bg-slate-50 border-slate-100 text-slate-700";
                            if (hrs > 30) styleHrs = "bg-rose-50 border-rose-200 text-rose-700 font-extrabold";
                            else if (hrs === 30) styleHrs = "bg-emerald-50 border-emerald-200 text-emerald-700 font-extrabold";
                            else if (hrs > 0) styleHrs = "bg-amber-50 border-amber-200 text-amber-800 font-extrabold";

                            return (
                              <div className={`border rounded-xl p-2 text-center ${styleHrs}`}>
                                <span className="block text-[10px] font-bold uppercase tracking-tight opacity-75">MALLA</span>
                                <span className="block text-xs mt-0.5">{hrs > 0 ? `${hrs} / 30 HRS` : "0 / 30 HRS"}</span>
                              </div>
                            );
                          })()}
                          <div className="bg-slate-50 border border-slate-100 rounded-xl p-2 text-center">
                            <span className="block text-[10px] font-bold text-slate-400 uppercase tracking-tight">NIVEL</span>
                            <span className="block text-xs font-extrabold text-slate-700 mt-0.5">EGB</span>
                          </div>
                          <div className="bg-slate-50 border border-slate-100 rounded-xl p-2 text-center">
                            <span className="block text-[10px] font-bold text-slate-400 uppercase tracking-tight">ESTADO</span>
                            <span className="block text-xs font-extrabold text-emerald-600 mt-0.5">VIGENTE</span>
                          </div>
                        </div>

                        <div className="pt-2 border-t border-slate-100 flex items-center justify-between">
                          <span className="text-xs font-semibold text-slate-600 group-hover:text-[#243A76] transition-colors">
                            Ver materias y horas
                          </span>
                          <span className="w-6 h-6 rounded-full bg-slate-100 group-hover:bg-[#243A76] text-slate-500 group-hover:text-white flex items-center justify-center text-xs font-bold transition-all transform group-hover:translate-x-0.5">
                            →
                          </span>
                        </div>
                      </div>

                      {isMenuOpen && (
                        <div className="fixed inset-0 z-20 cursor-default" onClick={(e) => { e.stopPropagation(); setActiveMenuId(null); }} />
                      )}
                    </button>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      )}

      {/* MODAL DETALLE DE MALLA DEL GRADO SELECCIONADO */}
      {showMallaModal && gradoSeleccionado && (
        <div className="fixed inset-0 z-50 flex items-center justify-center px-4" style={modalBg}>
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-3xl overflow-hidden flex flex-col max-h-[85vh]">
            {/* Header del Modal */}
            <div style={{ backgroundColor: PRIMARY }} className="px-6 py-4 flex items-center justify-between flex-shrink-0">
              <div>
                <h2 className="text-white font-bold text-lg">{gradoSeleccionado.nombre}</h2>
                <p className="text-blue-200 text-xs">Malla curricular asignada {anoActual && `— ${anoActual.nombre}`}</p>
              </div>
              <button
                onClick={() => setShowMallaModal(false)}
                className="text-white text-opacity-70 hover:text-opacity-100 text-xl font-bold p-1"
              >
                ✕
              </button>
            </div>

            {/* Barra de Acciones del Modal */}
            <div className="px-6 py-3 bg-slate-50 border-b border-slate-200 flex flex-col sm:flex-row sm:items-center justify-between gap-3 flex-shrink-0">
              {(() => {
                const total = Object.values(horasEditMap).reduce((a, b) => a + (parseInt(b) || 0), 0);
                const dif = total - 30;
                if (total > 30) {
                  return (
                    <div className="bg-rose-50 border border-rose-200 text-rose-800 px-3.5 py-1.5 rounded-xl text-xs font-bold flex items-center gap-2 shadow-2xs">
                      <span>⚠️ EXCESO:</span>
                      <span><strong>{total} / 30 Horas semanales</strong> (Te estás pasando por <span className="bg-rose-200 text-rose-900 px-1.5 py-0.5 rounded font-black">+{dif}h</span>)</span>
                    </div>
                  );
                } else if (total < 30) {
                  return (
                    <div className="bg-amber-50 border border-amber-200 text-amber-900 px-3.5 py-1.5 rounded-xl text-xs font-bold flex items-center gap-2 shadow-2xs">
                      <span>ℹ️ MALLA INCOMPLETA:</span>
                      <span><strong>{total} / 30 Horas semanales</strong> (Faltan <span className="bg-amber-200 text-amber-900 px-1.5 py-0.5 rounded font-black">{Math.abs(dif)}h</span> para completar 30h)</span>
                    </div>
                  );
                } else {
                  return (
                    <div className="bg-emerald-50 border border-emerald-200 text-emerald-800 px-3.5 py-1.5 rounded-xl text-xs font-bold flex items-center gap-2 shadow-2xs">
                      <span>✅ MALLA COMPLETA:</span>
                      <span><strong>30 / 30 Horas semanales</strong> asignadas perfectamente</span>
                    </div>
                  );
                }
              })()}
              <div className="flex items-center gap-2 self-end sm:self-auto">
                <button
                  onClick={guardarCambiosMalla}
                  disabled={saving}
                  className="bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold px-4 py-2 rounded-xl transition shadow-sm flex items-center gap-1.5 disabled:opacity-50"
                >
                  {saving ? "Guardando..." : "💾 Guardar cambios"}
                </button>
                <button
                  onClick={() => { setShowAgregar(true); setNuevaMalla({ idAsignatura: "", horasSemana: "" }); setError(""); }}
                  style={{ backgroundColor: PRIMARY }}
                  className="text-white text-xs font-semibold px-4 py-2 rounded-xl hover:opacity-90 transition shadow-sm"
                >
                  + Agregar materia
                </button>
              </div>
            </div>

            {/* Contenido / Tabla del Modal */}
            <div className="p-6 overflow-y-auto flex-1">
              {loadingMalla ? (
                <div className="p-12 text-center text-slate-400 text-sm">Cargando información de materias...</div>
              ) : !malla || malla.materias.length === 0 ? (
                <div className="p-12 text-center text-slate-400 text-sm bg-slate-50 rounded-xl border border-dashed border-slate-200">
                  Este grado no tiene materias asignadas en su malla. Haz clic en <strong>+ Agregar materia</strong> para comenzar.
                </div>
              ) : (
                <div className="border border-slate-200 rounded-xl overflow-hidden">
                  <table className="w-full text-sm">
                    <thead>
                      <tr style={{ backgroundColor: "#f8f9fc" }} className="border-b border-slate-200">
                        <th className="text-left px-5 py-3 text-xs font-bold text-slate-500 uppercase tracking-wider">Asignatura</th>
                        <th className="text-center px-5 py-3 text-xs font-bold text-slate-500 uppercase tracking-wider">Horas / Semana</th>
                        <th className="text-center px-5 py-3 text-xs font-bold text-slate-500 uppercase tracking-wider">Acción</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {malla.materias.map((m) => (
                        <tr key={m.idMalla || m.idAsignatura} className="hover:bg-slate-50 transition-colors">
                          <td className="px-5 py-3.5 font-medium text-slate-700">
                            <div className="flex flex-col">
                              <div className="flex items-center gap-2 flex-wrap">
                                <span className="font-semibold text-slate-800">{m.asignatura}</span>
                                <span className="font-mono text-[10px] px-1.5 py-0.5 rounded bg-slate-100 text-slate-500 font-normal">
                                  {m.codigo}
                                </span>
                                {m.origen === "ASIGNACION" ? (
                                  <span className="text-[10px] font-bold px-2 py-0.5 rounded-md bg-indigo-50 text-indigo-700 border border-indigo-100 flex items-center gap-1">
                                    <span className="w-1.5 h-1.5 rounded-full bg-indigo-500"></span> De Asignaciones
                                  </span>
                                ) : (
                                  <span className="text-[10px] font-bold px-2 py-0.5 rounded-md bg-slate-100 text-slate-600 border border-slate-200">
                                    📙 Malla Base
                                  </span>
                                )}
                              </div>
                              {m.docentes && m.docentes.length > 0 ? (
                                <span className="text-xs text-slate-500 mt-1 flex items-center gap-1">
                                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 inline-block"></span>
                                  Docente: <strong className="text-slate-700 font-medium">{m.docentes.join(", ")}</strong>
                                </span>
                              ) : (
                                <span className="text-xs text-amber-600 mt-1 font-medium flex items-center gap-1">
                                  <span className="w-1.5 h-1.5 rounded-full bg-amber-500 inline-block"></span>
                                  Sin docente asignado en el distributivo
                                </span>
                              )}
                            </div>
                          </td>
                          <td className="px-5 py-3.5 text-center">
                            <input
                              type="number"
                              min={1}
                              max={40}
                              value={horasEditMap[m.idAsignatura] !== undefined ? horasEditMap[m.idAsignatura] : (m.horasSemana || 4)}
                              onChange={(e) => {
                                const val = parseInt(e.target.value) || 0;
                                setHorasEditMap({ ...horasEditMap, [m.idAsignatura]: val });
                              }}
                              className="w-20 border border-slate-200 rounded-lg px-2 py-1 text-sm text-center bg-white font-semibold text-slate-700 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                            />
                          </td>
                          <td className="px-5 py-3.5 text-center">
                            <button
                              onClick={() => quitarDeMalla(m.idMalla)}
                              className="px-3 py-1 rounded-lg text-xs font-medium text-red-600 hover:bg-red-50 border border-red-100 transition"
                              title="Quitar materia"
                            >
                              Quitar
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>

            {/* Footer del Modal */}
            <div className="px-6 py-3.5 bg-slate-50 border-t border-slate-200 flex items-center justify-between flex-shrink-0">
              <button
                onClick={() => setShowMallaModal(false)}
                className="px-5 py-2 border border-slate-200 rounded-xl text-sm font-medium text-slate-600 hover:bg-slate-100 transition"
              >
                Cerrar
              </button>
              <button
                onClick={guardarCambiosMalla}
                disabled={saving}
                className="px-6 py-2 rounded-xl text-sm font-bold text-white bg-emerald-600 hover:bg-emerald-700 shadow-md transition disabled:opacity-50 flex items-center gap-2"
              >
                {saving ? "Guardando..." : "💾 Guardar cambios de malla"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL CREAR/EDITAR ASIGNATURA */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center px-4" style={modalBg}>
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden">
            <div style={{ backgroundColor: PRIMARY }} className="px-6 py-4 flex items-center justify-between">
              <h2 className="text-white font-bold text-base">{editando ? "Editar asignatura" : "Nueva asignatura"}</h2>
              <button onClick={() => setShowModal(false)} className="text-white text-opacity-70 hover:text-opacity-100">✕</button>
            </div>
            <form onSubmit={guardarAsignatura} className="p-6 space-y-4">
              {error && <div className="bg-red-50 border border-red-200 rounded-lg px-3 py-2 text-red-600 text-xs">{error}</div>}
              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Nombre</label>
                <input required value={form.nombre} onChange={e => setForm({ ...form, nombre: e.target.value })} className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-slate-50" placeholder="Ej: Lengua y Literatura" />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Código</label>
                <input required value={form.codigo} onChange={e => setForm({ ...form, codigo: e.target.value.toUpperCase() })} className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-slate-50 font-mono" placeholder="Ej: LEN" maxLength={10} />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Descripción</label>
                <textarea value={form.descripcion} onChange={e => setForm({ ...form, descripcion: e.target.value })} rows={2} className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-slate-50" />
              </div>
              <div className="flex gap-3 pt-1">
                <button type="button" onClick={() => setShowModal(false)} className="flex-1 py-2 border border-slate-200 rounded-lg text-sm text-slate-600 hover:bg-slate-50">Cancelar</button>
                <button type="submit" disabled={saving} style={{ backgroundColor: PRIMARY }} className="flex-1 py-2 text-white rounded-lg text-sm font-medium hover:opacity-90 disabled:opacity-60">{saving ? "Guardando..." : "Guardar"}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL AGREGAR A MALLA */}
      {showAgregar && (
        <div className="fixed inset-0 z-50 flex items-center justify-center px-4" style={modalBg}>
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-sm overflow-hidden">
            <div style={{ backgroundColor: PRIMARY }} className="px-6 py-4 flex items-center justify-between">
              <h2 className="text-white font-bold text-base">Agregar materia a la malla</h2>
              <button onClick={() => setShowAgregar(false)} className="text-white text-opacity-70 hover:text-opacity-100">✕</button>
            </div>
            <form onSubmit={agregarAMalla} className="p-6 space-y-4">
              {error && <div className="bg-red-50 border border-red-200 rounded-lg px-3 py-2 text-red-600 text-xs">{error}</div>}
              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Asignatura</label>
                <select required value={nuevaMalla.idAsignatura} onChange={e => setNuevaMalla({ ...nuevaMalla, idAsignatura: e.target.value })} className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-slate-50">
                  <option value="">Selecciona...</option>
                  {disponiblesParaMalla.map(a => <option key={a.idAsignatura} value={a.idAsignatura}>{a.nombre}</option>)}
                </select>
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Horas / semana</label>
                <input required type="number" min={1} max={40} value={nuevaMalla.horasSemana} onChange={e => setNuevaMalla({ ...nuevaMalla, horasSemana: e.target.value })} className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-slate-50" placeholder="Ej: 6" />
              </div>
              <div className="flex gap-3 pt-1">
                <button type="button" onClick={() => setShowAgregar(false)} className="flex-1 py-2 border border-slate-200 rounded-lg text-sm text-slate-600 hover:bg-slate-50">Cancelar</button>
                <button type="submit" disabled={saving} style={{ backgroundColor: PRIMARY }} className="flex-1 py-2 text-white rounded-lg text-sm font-medium hover:opacity-90 disabled:opacity-60">{saving ? "Agregando..." : "Agregar"}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </Layout>
  );
}
