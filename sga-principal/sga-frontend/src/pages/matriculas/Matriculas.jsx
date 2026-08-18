import { useState, useEffect, useCallback } from "react";
import api from "../../config/axios";
import Layout from "../../components/Layout";

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

  const abrirVer = (m) => {
    setSelected(m);
    setNuevoEstado(m.estado);
    setModal("ver");
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.idEstudiante) { setError("Busca y selecciona un estudiante de la lista"); return; }
    if (!form.idGrado) { setError("Selecciona un grado"); return; }
    if (!form.idParalelo) { setError("Selecciona un paralelo"); return; }
    if (!form.idAnoLectivo) { setError("Selecciona un año lectivo"); return; }
    setSaving(true); setError("");
    try {
      await api.post("/api/matriculas", {
        idEstudiante: form.idEstudiante,
        idGrado: Number(form.idGrado),
        idParalelo: Number(form.idParalelo),
        idAnoLectivo: Number(form.idAnoLectivo),
        observaciones: form.observaciones || null,
      });
      setSuccess("Matrícula registrada correctamente.");
      setModal(null);
      cargar();
    } catch (err) {
      setError(err.response?.data?.message || err.response?.data?.error || "Error al registrar la matrícula");
    } finally {
      setSaving(false);
    }
  };

  const guardarEstado = async () => {
    if (!selected || nuevoEstado === selected.estado) { setModal(null); return; }
    setSaving(true); setError("");
    try {
      await api.patch(`/api/matriculas/${selected.idMatricula}/estado`, null, { params: { estado: nuevoEstado } });
      setSuccess("Estado de la matrícula actualizado.");
      setModal(null);
      cargar();
    } catch (err) {
      setError(err.response?.data?.message || "Error al cambiar el estado");
    } finally {
      setSaving(false);
    }
  };

  const handleSeccion = (id) => {
    setSeccion(id);
    if (id === "nueva") abrirNueva();
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

      {error && !modal && (
        <div className="mb-4 bg-red-50 border border-red-200 rounded-lg px-4 py-3 flex items-center justify-between">
          <span className="text-red-600 text-sm">{error}</span>
          <button onClick={() => setError("")} className="text-red-400 hover:text-red-600">✕</button>
        </div>
      )}
      {success && (
        <div className="mb-4 bg-green-50 border border-green-200 rounded-lg px-4 py-3 flex items-center justify-between">
          <span className="text-green-600 text-sm">{success}</span>
          <button onClick={() => setSuccess("")} className="text-green-400 hover:text-green-600">✕</button>
        </div>
      )}

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
                    <td className="px-4 py-2.5 text-slate-500 text-xs">{m.paralelo || "—"}</td>
                    <td className="px-4 py-2.5 text-slate-500 text-xs">{m.fechaRegistro || "—"}</td>
                    <td className="px-4 py-2.5 text-center">
                      <span className={`text-[10px] font-semibold px-2 py-0.5 rounded-full ${BADGE_ESTADO[m.estado] || "bg-slate-100 text-slate-500"}`}>
                        {m.estado}
                      </span>
                    </td>
                    <td className="px-4 py-2.5 text-center">
                      <button onClick={() => abrirVer(m)} title="Ver detalle"
                        className="p-1.5 rounded-lg text-slate-400 hover:text-blue-600 hover:bg-blue-50 transition">
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                        </svg>
                      </button>
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
        <div className="fixed inset-0 z-50 flex items-center justify-center px-4" style={modalBg} onClick={() => setModal(null)}>
          <div className="bg-slate-50 rounded-2xl shadow-2xl w-full max-w-lg overflow-hidden flex flex-col max-h-[90vh]" onClick={e => e.stopPropagation()}>
            <div style={{ background: `linear-gradient(135deg, ${PRIMARY} 0%, #3d5a9e 100%)` }} className="px-6 py-5 text-white flex items-center justify-between flex-shrink-0">
              <div className="flex items-center gap-4 min-w-0">
                <div className="w-14 h-14 rounded-2xl bg-white/20 backdrop-blur flex items-center justify-center font-bold text-lg flex-shrink-0 shadow-lg">
                  {(selected.estudianteNombres?.[0] || "") + (selected.estudianteApellidos?.[0] || "")}
                </div>
                <div className="min-w-0">
                  <h3 className="font-bold text-lg truncate">{selected.estudianteApellidos} {selected.estudianteNombres}</h3>
                  <div className="flex items-center gap-2 text-xs text-white/80 mt-0.5 flex-wrap">
                    <span className="font-semibold bg-white/15 px-2 py-0.5 rounded-md">{selected.grado}{selected.paralelo ? ` "${selected.paralelo}"` : ""}</span>
                    <span>·</span>
                    <span>{selected.anoLectivo}</span>
                  </div>
                </div>
              </div>
              <button onClick={() => setModal(null)} className="text-white/70 hover:text-white text-xl flex-shrink-0 ml-3 w-8 h-8 rounded-lg hover:bg-white/10 transition">✕</button>
            </div>

            <div className="p-6 overflow-y-auto space-y-4 flex-1">
              <Card title="Matrícula">
                <div className="grid grid-cols-2 gap-4">
                  <Field label="N° de orden" value={selected.numeroOrden} mono />
                  <Field label="Fecha de registro" value={selected.fechaRegistro} mono />
                  <Field label="Cédula" value={selected.estudianteCedula} mono />
                  <Field label="Código estudiante" value={selected.estudianteCodigo} mono />
                  <Field label="Registrado por" value={selected.registradoPor} />
                  <Field label="Estado actual" value={selected.estado} />
                </div>
              </Card>

              <Card title="Observaciones">
                <p className="text-sm text-slate-700 leading-relaxed">{selected.observaciones || "Sin observaciones."}</p>
              </Card>

              <Card title="Cambiar estado">
                <div className="flex items-center gap-3">
                  <select value={nuevoEstado} onChange={e => setNuevoEstado(e.target.value)}
                    className="flex-1 px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white">
                    {ESTADOS.map(es => <option key={es} value={es}>{es}</option>)}
                  </select>
                  <button onClick={guardarEstado} disabled={saving} style={{ backgroundColor: PRIMARY }}
                    className="px-4 py-2 rounded-lg text-sm text-white font-semibold hover:opacity-90 transition disabled:opacity-60">
                    {saving ? "Guardando..." : "Guardar"}
                  </button>
                </div>
              </Card>

              {error && <div className="bg-red-50 border border-red-200 text-red-600 text-xs px-3 py-2 rounded-lg">{error}</div>}
            </div>

            <div className="px-6 py-4 border-t border-slate-200 flex justify-end bg-white flex-shrink-0">
              <button onClick={() => setModal(null)} className="px-6 py-2 rounded-lg text-sm text-slate-600 border border-slate-200 hover:bg-slate-50 transition">
                Cerrar
              </button>
            </div>
          </div>
        </div>
      )}
    </Layout>
  );
}
