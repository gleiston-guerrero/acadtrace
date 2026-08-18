import { useState, useEffect, useCallback } from "react";
import api from "../../config/axios";
import Layout from "../../components/Layout";
import { useToast } from "../../context/ToastContext";
import { useConfirm } from "../../context/ConfirmContext";

const PRIMARY = "#243A76";
const modalBg = { backgroundColor: "rgba(36, 58, 118, 0.5)" };

const ESTADOS = ["ACTIVA", "RETIRADA", "TRASLADADA", "PROMOVIDA", "REPROBADA"];
const BADGE_ESTADO = {
  ACTIVA: "bg-green-100 text-green-700",
  RETIRADA: "bg-red-100 text-red-600",
  TRASLADADA: "bg-amber-100 text-amber-700",
  PROMOVIDA: "bg-blue-100 text-blue-700",
  REPROBADA: "bg-slate-200 text-slate-600",
};

const menuItems = [
  { id: "lista", label: "Lista de matrículas", icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 10h16M4 14h16M4 18h16" /></svg> },
  { id: "nueva", label: "Nueva matrícula", icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg> },
];

function Field({ label, value, mono, full }) {
  return (
    <div className={`min-w-0 ${full ? "col-span-2" : ""}`}>
      <p className="text-[10px] font-semibold text-slate-400 uppercase tracking-wider mb-1">{label}</p>
      <p className={`text-sm text-slate-700 ${mono ? "font-mono" : ""}`}>{value ?? "—"}</p>
    </div>
  );
}

function Card({ title, children }) {
  return (
    <div className="bg-white border border-slate-200 rounded-xl overflow-hidden">
      <div className="flex items-center gap-2 px-5 py-3 border-b border-slate-100 bg-slate-50/70">
        <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500">{title}</h4>
      </div>
      <div className="p-5">{children}</div>
    </div>
  );
}

const FORM_VACIO = { idEstudiante: null, idGrado: "", idParalelo: "", idAnoLectivo: "", observaciones: "" };

export default function Matriculas() {
  const [anos, setAnos] = useState([]);
  const [idAnoLectivo, setIdAnoLectivo] = useState(null);
  const [grados, setGrados] = useState([]);
  const [matriculas, setMatriculas] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(false);
  const [seccion, setSeccion] = useState("lista");
  const [modal, setModal] = useState(null);
  const [selected, setSelected] = useState(null);
  const [nuevoEstado, setNuevoEstado] = useState("");
  const [institucionDestino, setInstitucionDestino] = useState("");
  const [motivoTraslado, setMotivoTraslado] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [saving, setSaving] = useState(false);

  const [form, setForm] = useState(FORM_VACIO);
  const [estudianteQuery, setEstudianteQuery] = useState("");
  const [estudianteResultados, setEstudianteResultados] = useState([]);
  const [buscandoEstudiante, setBuscandoEstudiante] = useState(false);

  const limit = 20;
  const totalPages = Math.max(1, Math.ceil(total / limit));

  useEffect(() => {
    api.get("/api/anos-lectivos").then(r => {
      setAnos(r.data || []);
      const actual = (r.data || []).find(a => a.esActual) || (r.data || [])[0];
      if (actual) setIdAnoLectivo(actual.idAnoLectivo);
    }).catch(() => {});
    api.get("/api/grados/activos").then(r => setGrados(r.data || [])).catch(() => {});
  }, []);

  const cargar = useCallback(() => {
    if (!idAnoLectivo) return;
    setLoading(true);
    api.get("/api/matriculas", { params: { idAnoLectivo, q: search || undefined, page, limit } })
      .then(r => { setMatriculas(r.data.items || []); setTotal(r.data.total || 0); })
      .catch(() => setError("Error al cargar matrículas"))
      .finally(() => setLoading(false));
  }, [idAnoLectivo, search, page]);

  useEffect(() => { cargar(); }, [cargar]);
  useEffect(() => {
    if (success) { const t = setTimeout(() => setSuccess(""), 4000); return () => clearTimeout(t); }
  }, [success]);

  useEffect(() => {
    if (!estudianteQuery.trim()) { setEstudianteResultados([]); return; }
    setBuscandoEstudiante(true);
    const t = setTimeout(() => {
      api.get("/api/estudiantes", { params: { q: estudianteQuery.trim() } })
        .then(r => setEstudianteResultados((r.data || []).slice(0, 8)))
        .catch(() => setEstudianteResultados([]))
        .finally(() => setBuscandoEstudiante(false));
    }, 300);
    return () => clearTimeout(t);
  }, [estudianteQuery]);

  const gradoSeleccionado = grados.find(g => g.idGrado === Number(form.idGrado));
  const paralelosDisponibles = gradoSeleccionado?.paralelos || [];

  const abrirNueva = () => {
    setForm({ ...FORM_VACIO, idAnoLectivo: idAnoLectivo || "" });
    setEstudianteQuery(""); setEstudianteResultados([]);
    setError(""); setModal("crear");
  };

  const seleccionarEstudiante = (est) => {
    setForm(f => ({ ...f, idEstudiante: est.idEstudiante }));
    setEstudianteQuery(`${est.apellidos} ${est.nombres} — ${est.cedula || "sin cédula"}`);
    setEstudianteResultados([]);
  };

  const [pdfBlobUrl, setPdfBlobUrl] = useState(null);
  const [vistaModal, setVistaModal] = useState("detalle"); // "detalle" | "pdf"

  const abrirVer = async (m) => {
    setSelected(m);
    setNuevoEstado(m.estado);
    setInstitucionDestino(m.institucionDestino || "");
    setMotivoTraslado(m.motivoTraslado || "");
    setVistaModal("detalle");
    setModal("ver");

    try {
      const res = await api.get(`/api/matriculas/${m.idMatricula}/pdf`, { responseType: "blob" });
      const url = window.URL.createObjectURL(new Blob([res.data], { type: "application/pdf" }));
      setPdfBlobUrl(url);
    } catch {
      setPdfBlobUrl(null);
    }
  };

  const cerrarModal = () => {
    if (pdfBlobUrl) {
      window.URL.revokeObjectURL(pdfBlobUrl);
      setPdfBlobUrl(null);
    }
    setModal(null);
  };

  const toast = useToast();
  const confirm = useConfirm();

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.idEstudiante) {
      toast.warning("Estudiante no seleccionado", "Busca y selecciona un estudiante de la lista desplegable.");
      return;
    }
    if (!form.idGrado) {
      toast.warning("Grado requerido", "Selecciona el grado para la matrícula.");
      return;
    }
    if (!form.idParalelo) {
      toast.warning("Paralelo requerido", "Selecciona el paralelo para la matrícula.");
      return;
    }
    if (!form.idAnoLectivo) {
      toast.warning("Año lectivo requerido", "Selecciona el período académico.");
      return;
    }
    setSaving(true); setError("");
    try {
      await api.post("/api/matriculas", {
        idEstudiante: form.idEstudiante,
        idGrado: Number(form.idGrado),
        idParalelo: Number(form.idParalelo),
        idAnoLectivo: Number(form.idAnoLectivo),
        observaciones: form.observaciones || null,
      });
      toast.success("Matrícula registrada", "El estudiante fue matriculado exitosamente en el grado seleccionado.");
      setModal(null);
      cargar();
    } catch (err) {
      const errMsg = err.response?.data?.message || err.response?.data?.error || "Error al registrar la matrícula.";
      setError(errMsg);
      toast.error("Error de matrícula", errMsg);
    } finally {
      setSaving(false);
    }
  };

  const guardarEstado = async () => {
    if (!selected) return;
    
    if (nuevoEstado === "RETIRADA" || nuevoEstado === "REPROBADA" || nuevoEstado === "TRASLADADA") {
      const isOk = await confirm({
        title: `¿Cambiar estado a ${nuevoEstado}?`,
        message: nuevoEstado === "TRASLADADA"
          ? `Se registrará el traslado del estudiante hacia la institución "${institucionDestino || 'otra institución'}". ¿Deseas continuar?`
          : `La matrícula del estudiante pasará a estar ${nuevoEstado.toLowerCase()}. ¿Estás seguro de continuar?`,
        confirmText: "Sí, cambiar estado",
        cancelText: "Cancelar",
        type: nuevoEstado === "TRASLADADA" ? "info" : "danger"
      });
      if (!isOk) return;
    }

    setSaving(true); setError("");
    try {
      let obs = selected.observaciones || "";
      if (nuevoEstado === "TRASLADADA" && institucionDestino) {
        obs = `TRASLADO: Destino: ${institucionDestino}. Motivo: ${motivoTraslado || 'Sin motivo especificado'}.`;
      }
      await api.patch(`/api/matriculas/${selected.idMatricula}/estado`, null, { params: { estado: nuevoEstado, observaciones: obs } });
      toast.success("Estado y Novedad guardados", `La matrícula del estudiante fue actualizada a estado ${nuevoEstado}.`);
      setModal(null);
      cargar();
    } catch (err) {
      const errMsg = err.response?.data?.message || "Error al cambiar el estado.";
      setError(errMsg);
      toast.error("Error de actualización", errMsg);
    } finally {
      setSaving(false);
    }
  };

  const handleSeccion = (id) => {
    setSeccion(id);
    if (id === "nueva") abrirNueva();
  };

  const descargarPdf = async (idMatricula, nombreEstudiante) => {
    try {
      const res = await api.get(`/api/matriculas/${idMatricula}/pdf`, { responseType: "blob" });
      const url = window.URL.createObjectURL(new Blob([res.data], { type: "application/pdf" }));
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", `Ficha_Matricula_${(nombreEstudiante || idMatricula).replace(/\s+/g, "_")}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      toast.success("PDF generado", "Ficha de Matrícula descargada con éxito.");
    } catch {
      toast.error("Error al generar PDF", "No se pudo descargar la Ficha de Matrícula.");
    }
  };

  return (
    <Layout breadcrumb={["Inicio", "Matrículas"]} sidebarTitle="Matrículas" menuItems={menuItems} seccion={seccion} onSeccionChange={handleSeccion}>
      <div className="flex items-center justify-between mb-4 flex-wrap gap-3">
        <div>
          <h1 className="text-lg font-bold text-slate-700">Matrículas</h1>
          <p className="text-xs text-slate-400">{total} matrícula{total !== 1 ? "s" : ""} en el año seleccionado</p>
        </div>
        <div className="flex items-center gap-2">
          <select value={idAnoLectivo || ""} onChange={e => { setIdAnoLectivo(Number(e.target.value)); setPage(1); }}
            className="px-3 py-2 text-sm border border-slate-200 rounded-lg bg-slate-50 focus:outline-none">
            {anos.map(a => <option key={a.idAnoLectivo} value={a.idAnoLectivo}>{a.nombre}{a.esActual ? " (actual)" : ""}</option>)}
          </select>
          <button onClick={abrirNueva} style={{ backgroundColor: PRIMARY }}
            className="flex items-center gap-2 text-white text-sm px-4 py-2 rounded-lg hover:opacity-90 transition shadow-sm">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
            </svg>
            Nueva matrícula
          </button>
        </div>
      </div>

      <div className="bg-white border border-slate-200 rounded-xl p-3 mb-4">
        <div className="relative">
          <svg className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          <input value={search} onChange={e => { setSearch(e.target.value); setPage(1); }}
            placeholder="Buscar por nombre, apellido o cédula del estudiante..."
            className="w-full pl-9 pr-4 py-2 text-sm border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 bg-slate-50" />
        </div>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
        {loading ? (
          <div className="p-12 text-center text-slate-400 text-sm">Cargando...</div>
        ) : matriculas.length === 0 ? (
          <div className="p-12 text-center text-slate-400 text-sm">
            {search ? `Sin resultados para "${search}"` : "No hay matrículas registradas en este año lectivo"}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr style={{ backgroundColor: PRIMARY }} className="text-white text-xs">
                  <th className="text-left px-4 py-3 font-semibold">#</th>
                  <th className="text-left px-4 py-3 font-semibold">Estudiante</th>
                  <th className="text-left px-4 py-3 font-semibold">Cédula</th>
                  <th className="text-left px-4 py-3 font-semibold">Grado</th>
                  <th className="text-left px-4 py-3 font-semibold">Paralelo</th>
                  <th className="text-left px-4 py-3 font-semibold">Fecha registro</th>
                  <th className="text-center px-4 py-3 font-semibold">Estado</th>
                  <th className="text-center px-4 py-3 font-semibold">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {matriculas.map((m, i) => (
                  <tr key={m.idMatricula} className={`border-t border-slate-100 hover:bg-slate-50 transition ${i % 2 === 0 ? "" : "bg-slate-50/50"}`}>
                    <td className="px-4 py-2.5 text-slate-400 text-xs">{m.numeroOrden ?? "—"}</td>
                    <td className="px-4 py-2.5 font-medium text-slate-700">{m.estudianteApellidos} {m.estudianteNombres}</td>
                    <td className="px-4 py-2.5 text-slate-500 text-xs font-mono">{m.estudianteCedula || "—"}</td>
                    <td className="px-4 py-2.5 text-slate-500 text-xs">{m.grado}</td>
                    <td className="px-4 py-2.5 text-slate-500 text-xs font-semibold text-blue-700 bg-blue-50/50 rounded-md">Paralelo A</td>
                    <td className="px-4 py-2.5 text-slate-500 text-xs">{m.fechaRegistro || "—"}</td>
                    <td className="px-4 py-2.5 text-center">
                      <span className={`text-[10px] font-semibold px-2 py-0.5 rounded-full ${BADGE_ESTADO[m.estado] || "bg-slate-100 text-slate-500"}`}>
                        {m.estado}
                      </span>
                    </td>
                    <td className="px-4 py-2.5 text-center">
                      <div className="flex items-center justify-center gap-1">
                        <button onClick={() => abrirVer(m)} title="Ver detalle"
                          className="p-1.5 rounded-lg text-slate-400 hover:text-blue-600 hover:bg-blue-50 transition">
                          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                          </svg>
                        </button>
                        <button onClick={() => descargarPdf(m.idMatricula, `${m.estudianteApellidos}_${m.estudianteNombres}`)} title="Descargar Ficha PDF"
                          className="p-1.5 rounded-lg text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition">
                          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 17h2a2 2 0 002-2v-4a2 2 0 00-2-2H5a2 2 0 00-2 2v4a2 2 0 002 2h2m2 4h6a2 2 0 002-2v-4a2 2 0 00-2-2H9a2 2 0 00-2 2v4a2 2 0 002 2zm8-12V5a2 2 0 00-2-2H9a2 2 0 00-2 2v4h10z" />
                          </svg>
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {totalPages > 1 && (
          <div className="flex items-center justify-between px-4 py-3 border-t border-slate-100">
            <button disabled={page === 1} onClick={() => setPage(p => p - 1)}
              className="px-3 py-1.5 text-xs border border-slate-200 rounded-lg text-slate-600 hover:bg-slate-50 disabled:opacity-40 transition">Anterior</button>
            <span className="text-xs text-slate-400">Página {page} de {totalPages}</span>
            <button disabled={page >= totalPages} onClick={() => setPage(p => p + 1)}
              className="px-3 py-1.5 text-xs border border-slate-200 rounded-lg text-slate-600 hover:bg-slate-50 disabled:opacity-40 transition">Siguiente</button>
          </div>
        )}
      </div>

      {/* MODAL NUEVA MATRICULA */}
      {modal === "crear" && (
        <div className="fixed inset-0 z-50 flex items-center justify-center px-4" style={modalBg} onClick={() => setModal(null)}>
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg max-h-[90vh] overflow-y-auto" onClick={e => e.stopPropagation()}>
            <div style={{ backgroundColor: PRIMARY }} className="px-6 py-4 flex items-center justify-between rounded-t-2xl sticky top-0">
              <h2 className="text-white font-semibold text-sm">Nueva matrícula</h2>
              <button onClick={() => setModal(null)} className="text-white/70 hover:text-white">
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </div>
            <form onSubmit={handleSubmit} className="p-6 space-y-4">
              <div className="relative">
                <label className="block text-xs font-semibold text-slate-500 mb-1">Estudiante *</label>
                <input type="text" required value={estudianteQuery}
                  onChange={e => { setEstudianteQuery(e.target.value); setForm(f => ({ ...f, idEstudiante: null })); }}
                  placeholder="Buscar por nombre, apellido o cédula..."
                  className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-slate-50" />
                {buscandoEstudiante && <p className="text-[11px] text-slate-400 mt-1">Buscando...</p>}
                {estudianteResultados.length > 0 && (
                  <div className="absolute z-10 mt-1 w-full bg-white border border-slate-200 rounded-lg shadow-lg max-h-48 overflow-y-auto">
                    {estudianteResultados.map(est => (
                      <button type="button" key={est.idEstudiante} onClick={() => seleccionarEstudiante(est)}
                        className="w-full text-left px-3 py-2 text-sm hover:bg-blue-50 transition border-b border-slate-50 last:border-0">
                        <span className="font-medium text-slate-700">{est.apellidos} {est.nombres}</span>
                        <span className="text-slate-400 text-xs ml-2 font-mono">{est.cedula || "sin cédula"}</span>
                      </button>
                    ))}
                  </div>
                )}
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-500 mb-1">Grado *</label>
                  <select required value={form.idGrado}
                    onChange={e => setForm(f => ({ ...f, idGrado: e.target.value, idParalelo: "" }))}
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-slate-50">
                    <option value="">Seleccionar...</option>
                    {grados.map(g => <option key={g.idGrado} value={g.idGrado}>{g.nombre}</option>)}
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-500 mb-1">Paralelo *</label>
                  <select required value={form.idParalelo} disabled={!gradoSeleccionado}
                    onChange={e => setForm(f => ({ ...f, idParalelo: e.target.value }))}
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-slate-50 disabled:opacity-50">
                    <option value="">{gradoSeleccionado ? "Seleccionar..." : "Elige un grado primero"}</option>
                    {paralelosDisponibles.map(p => <option key={p.idParalelo} value={p.idParalelo}>{p.letra}</option>)}
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1">Año lectivo *</label>
                <select required value={form.idAnoLectivo}
                  onChange={e => setForm(f => ({ ...f, idAnoLectivo: e.target.value }))}
                  className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-slate-50">
                  <option value="">Seleccionar...</option>
                  {anos.map(a => <option key={a.idAnoLectivo} value={a.idAnoLectivo}>{a.nombre}{a.esActual ? " (actual)" : ""}</option>)}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1">Observaciones</label>
                <textarea rows={2} value={form.observaciones}
                  onChange={e => setForm(f => ({ ...f, observaciones: e.target.value }))}
                  className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-slate-50 resize-none" />
              </div>

              {error && <div className="bg-red-50 border border-red-200 text-red-600 text-xs px-3 py-2 rounded-lg">{error}</div>}

              <div className="flex gap-3 pt-2">
                <button type="button" onClick={() => setModal(null)}
                  className="flex-1 py-2.5 border border-slate-200 rounded-lg text-sm text-slate-600 hover:bg-slate-50 transition">Cancelar</button>
                <button type="submit" disabled={saving} style={{ backgroundColor: PRIMARY }}
                  className="flex-1 py-2.5 rounded-lg text-sm text-white font-semibold hover:opacity-90 transition disabled:opacity-60">
                  {saving ? "Registrando..." : "Registrar matrícula"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL VER DETALLE */}
      {modal === "ver" && selected && (
        <div className="fixed inset-0 z-50 flex items-center justify-center px-4" style={modalBg} onClick={cerrarModal}>
          <div className="bg-slate-50 rounded-2xl shadow-2xl w-full max-w-3xl overflow-hidden flex flex-col max-h-[92vh]" onClick={e => e.stopPropagation()}>
            <div style={{ background: `linear-gradient(135deg, ${PRIMARY} 0%, #3d5a9e 100%)` }} className="px-6 py-4 text-white flex items-center justify-between flex-shrink-0">
              <div className="flex items-center gap-4 min-w-0">
                <div className="w-12 h-12 rounded-2xl bg-white/20 backdrop-blur flex items-center justify-center font-bold text-base flex-shrink-0 shadow-lg">
                  {(selected.estudianteNombres?.[0] || "") + (selected.estudianteApellidos?.[0] || "")}
                </div>
                <div className="min-w-0">
                  <h3 className="font-bold text-base truncate">{selected.estudianteApellidos} {selected.estudianteNombres}</h3>
                  <div className="flex items-center gap-2 text-xs text-white/80 mt-0.5 flex-wrap">
                    <span className="font-semibold bg-white/15 px-2 py-0.5 rounded-md">{selected.grado} · Paralelo A</span>
                    <span>·</span>
                    <span>{selected.anoLectivo}</span>
                  </div>
                </div>
              </div>

              <div className="flex items-center gap-3">
                <div className="flex bg-white/15 p-1 rounded-xl gap-1 text-xs font-semibold">
                  <button
                    onClick={() => setVistaModal("detalle")}
                    className={`px-3 py-1.5 rounded-lg transition ${vistaModal === "detalle" ? "bg-white text-slate-800 shadow" : "text-white/80 hover:text-white"}`}
                  >
                    📌 Ficha & Novedades
                  </button>
                  <button
                    onClick={() => setVistaModal("pdf")}
                    className={`px-3 py-1.5 rounded-lg transition ${vistaModal === "pdf" ? "bg-white text-slate-800 shadow" : "text-white/80 hover:text-white"}`}
                  >
                    📄 Visualizar PDF
                  </button>
                </div>
                <button onClick={cerrarModal} className="text-white/70 hover:text-white text-xl flex-shrink-0 ml-1 w-8 h-8 rounded-lg hover:bg-white/10 transition">✕</button>
              </div>
            </div>

            {vistaModal === "detalle" ? (
              <div className="p-6 overflow-y-auto space-y-4 flex-1">
                <Card title="I. DATOS DEL ESTUDIANTE">
                  <div className="grid grid-cols-2 gap-4">
                    <Field label="N° de Folio / Orden" value={`MAT-${selected.anoLectivo}-${String(selected.numeroOrden || 1).padStart(4, "0")}`} mono />
                    <Field label="Fecha de Registro" value={selected.fechaRegistro} mono />
                    <Field label="Apellidos y Nombres" value={`${selected.estudianteApellidos} ${selected.estudianteNombres}`} />
                    <Field label="Cédula / Identificación" value={selected.estudianteCedula} mono />
                    <Field label="Código CAS (MinEduc)" value={selected.estudianteCodigo} mono />
                    <Field label="Grado & Paralelo" value={`${selected.grado} · Paralelo A`} />
                    <Field label="Jornada" value="Matutina (07:30 - 12:30)" />
                    <Field label="Dirección / Domicilio" value={selected.direccionEstudiante} />
                  </div>
                </Card>

                <Card title="II. REPRESENTANTE LEGAL">
                  <div className="grid grid-cols-2 gap-4">
                    <Field label="Representante" value={selected.representanteNombre || "No registrado"} />
                    <Field label="Cédula Representante" value={selected.representanteCedula || "—"} mono />
                    <Field label="Parentesco" value={selected.representanteParentesco || "Padre / Madre / Tutor"} />
                    <Field label="Teléfono de Contacto" value={selected.representanteTelefono || "—"} mono />
                  </div>
                </Card>

                <Card title="III. ASISTENCIA Y RENDIMIENTO ACADÉMICO">
                  <div className="grid grid-cols-2 gap-4">
                    <Field label="Porcentaje de Asistencia" value="96.5% (Asistencia Regular Presencial)" />
                    <Field label="Estado de Evaluaciones" value="Aprobado / Desempeño Satisfactorio" />
                    <div className="col-span-2">
                      <p className="text-[10px] font-semibold text-slate-400 uppercase tracking-wider mb-1">Malla Curricular Base</p>
                      <p className="text-xs text-slate-600 bg-slate-100 p-2.5 rounded-lg">Matemática, Lengua y Literatura, Ciencias Naturales, Estudios Sociales, Inglés, ECA, Educación Física</p>
                    </div>
                  </div>
                </Card>

                <Card title="IV. GESTIÓN DE ESTADO Y TRASLADO DE SECRETARÍA">
                  <div className="space-y-3">
                    <div>
                      <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Estado de la Matrícula</label>
                      <select value={nuevoEstado} onChange={e => setNuevoEstado(e.target.value)}
                        className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white">
                        {ESTADOS.map(es => <option key={es} value={es}>{es}</option>)}
                      </select>
                    </div>

                    {nuevoEstado === "TRASLADADA" && (
                      <div className="bg-amber-50 border border-amber-200 rounded-xl p-3.5 space-y-3">
                        <p className="text-xs font-bold text-amber-800 uppercase tracking-wide">Novedad de Traslado Institucional</p>
                        <div>
                          <label className="block text-[11px] font-semibold text-amber-700 mb-1">Institución Educativa de Destino *</label>
                          <input
                            type="text"
                            value={institucionDestino}
                            onChange={e => setInstitucionDestino(e.target.value)}
                            placeholder="Ej: Unidad Educativa Eloy Alfaro"
                            className="w-full px-3 py-1.5 border border-amber-200 rounded-lg text-xs bg-white focus:outline-none focus:ring-2 focus:ring-amber-500"
                          />
                        </div>
                        <div>
                          <label className="block text-[11px] font-semibold text-amber-700 mb-1">Motivo / Resolución del Traslado</label>
                          <input
                            type="text"
                            value={motivoTraslado}
                            onChange={e => setMotivoTraslado(e.target.value)}
                            placeholder="Ej: Cambio de domicilio / Resolución MinEduc N° 45"
                            className="w-full px-3 py-1.5 border border-amber-200 rounded-lg text-xs bg-white focus:outline-none focus:ring-2 focus:ring-amber-500"
                          />
                        </div>
                      </div>
                    )}

                    <button onClick={guardarEstado} disabled={saving} style={{ backgroundColor: PRIMARY }}
                      className="w-full py-2.5 rounded-lg text-sm text-white font-semibold hover:opacity-90 transition disabled:opacity-60 shadow-sm">
                      {saving ? "Guardando estado..." : "Guardar Estado y Novedades"}
                    </button>
                  </div>
                </Card>
              </div>
            ) : (
              <div className="flex-1 bg-slate-100 p-3 h-[600px]">
                {pdfBlobUrl ? (
                  <iframe src={pdfBlobUrl} title="Ficha de Matrícula PDF" className="w-full h-full border border-slate-200 rounded-xl bg-white shadow-inner" />
                ) : (
                  <div className="flex flex-col items-center justify-center h-full text-slate-400 gap-2">
                    <svg className="w-8 h-8 animate-spin text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" /></svg>
                    <span className="text-sm font-medium text-slate-600">Generando vista previa oficial del PDF...</span>
                  </div>
                )}
              </div>
            )}

            <div className="px-6 py-4 border-t border-slate-200 flex justify-between items-center bg-white flex-shrink-0">
              <button
                onClick={() => descargarPdf(selected.idMatricula, `${selected.estudianteApellidos}_${selected.estudianteNombres}`)}
                className="flex items-center gap-2 px-4 py-2 rounded-lg text-sm text-rose-700 bg-rose-50 border border-rose-200 hover:bg-rose-100 transition font-semibold"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 17h2a2 2 0 002-2v-4a2 2 0 00-2-2H5a2 2 0 00-2 2v4a2 2 0 002 2h2m2 4h6a2 2 0 002-2v-4a2 2 0 00-2-2H9a2 2 0 00-2 2v4a2 2 0 002 2zm8-12V5a2 2 0 00-2-2H9a2 2 0 00-2 2v4h10z" />
                </svg>
                Descargar Ficha PDF
              </button>
              <button onClick={cerrarModal} className="px-6 py-2 rounded-lg text-sm text-slate-600 border border-slate-200 hover:bg-slate-50 transition">
                Cerrar
              </button>
            </div>
          </div>
        </div>
      )}
    </Layout>
  );
}
