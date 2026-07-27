import { useState, useEffect } from "react";
import axios from "axios";
import Layout from "../../components/Layout";

const API = "http://localhost:8080/api";
const PRIMARY = "#243A76";

const menuItems = [
  { id: "lista", label: "Asignaciones", icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" /></svg> },
  { id: "docentes", label: "Docentes", icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" /></svg> },
  { id: "nuevo", label: "Nueva asignación", icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg> },
];

const Detalle = ({ label, value }) => (
  <div className="grid grid-cols-3 gap-2 text-sm py-1">
    <span className="text-slate-400 text-xs uppercase tracking-wide">{label}</span>
    <span className="col-span-2 text-slate-700">{value || <span className="text-slate-300">—</span>}</span>
  </div>
);

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
  const [asignaturas, setAsignaturas] = useState([]);
  const [grados, setGrados] = useState([]);
  const [paralelos, setParalelos] = useState([]);
  const [anosLectivos, setAnosLectivos] = useState([]);
  const [form, setForm] = useState(formVacio);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const [cedulaInput, setCedulaInput] = useState("");
  const [buscando, setBuscando] = useState(false);
  const [docenteSel, setDocenteSel] = useState(null);

  const [personasDocentes, setPersonasDocentes] = useState([]);
  const [loadingDocentes, setLoadingDocentes] = useState(false);
  const [perfilEdit, setPerfilEdit] = useState(null);
  const [nuevoDocente, setNuevoDocente] = useState(null);
  const [asignEdit, setAsignEdit] = useState(null);
  const [asignVer, setAsignVer] = useState(null);

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
    if (id === "nuevo") {
      setForm(formVacio);
      setCedulaInput("");
      setDocenteSel(null);
    }
    if (id === "docentes") cargarPersonasDocentes();
  };

  const cargarPersonasDocentes = () => {
    setLoadingDocentes(true);
    axios.get(`${API}/personas`, { headers: H })
      .then(r => setPersonasDocentes((r.data || []).filter(p => p.roles?.includes("DOCENTE"))))
      .catch(() => setError("No se pudo cargar la lista de docentes"))
      .finally(() => setLoadingDocentes(false));
  };

  const buscarDocentePorCedula = async () => {
    setDocenteSel(null);
    setError("");
    if (!/^\d{10}$/.test(cedulaInput)) {
      setError("La cédula debe tener 10 dígitos");
      return;
    }
    setBuscando(true);
    try {
      const { data } = await axios.get(`${API}/personas/buscar`, {
        headers: H, params: { cedula: cedulaInput },
      });
      if (!data.roles?.includes("DOCENTE")) {
        setError("El usuario existe pero no tiene rol DOCENTE. Asígnalo desde el módulo Usuarios.");
        return;
      }
      setDocenteSel(data);
      setForm(f => ({ ...f, idDocente: String(data.idPersona) }));
    } catch (err) {
      if (err.response?.status === 404) {
        setError("No hay un docente registrado con esa cédula. Créalo en el módulo Usuarios.");
      } else {
        setError("No se pudo buscar el docente.");
      }
    } finally {
      setBuscando(false);
    }
  };

  const subirFoto = async (file, setter) => {
    const fd = new FormData();
    fd.append("archivo", file);
    try {
      const { data } = await axios.post(`${API}/uploads/foto`, fd, {
        headers: { ...H, "Content-Type": "multipart/form-data" },
      });
      setter(data.url);
    } catch (err) {
      setError(err.response?.data?.message || "No se pudo subir la imagen.");
    }
  };

  const autoBuscarCedula = async (cedula) => {
    setNuevoDocente(nd => ({ ...nd, cedula, existente: null, buscando: true }));
    setError("");
    if (!/^\d{10}$/.test(cedula)) {
      setNuevoDocente(nd => ({ ...nd, existente: null, buscando: false }));
      return;
    }
    try {
      const { data } = await axios.get(`${API}/personas/buscar`, { headers: H, params: { cedula } });
      if (!data.roles?.includes("DOCENTE")) {
        setError("Ese usuario existe pero NO tiene rol DOCENTE. Ve a Usuarios y agrégaselo.");
        setNuevoDocente(nd => ({ ...nd, existente: "sin_rol", buscando: false }));
        return;
      }
      setNuevoDocente(nd => ({
        ...nd,
        buscando: false,
        existente: data,
        idPersona: data.idPersona,
        idUsuario: data.idUsuario,
        nombres: data.nombres || "",
        apellidos: data.apellidos || "",
        correo: data.correo || "",
        fechaNacimiento: data.fechaNacimiento || "",
        genero: data.genero || "",
        telefono: data.telefono || "",
        telefonoAlt: data.telefonoAlt || "",
        direccion: data.direccion || "",
        correoPersonal: data.correoPersonal || "",
        tituloAcademico: data.tituloAcademico || "",
        especializacion: data.especializacion || "",
        fotoUrl: data.fotoUrl || "",
      }));
    } catch (err) {
      if (err.response?.status === 404) {
        setNuevoDocente(nd => ({ ...nd, existente: "nuevo", buscando: false }));
      } else {
        setError("No se pudo consultar la cédula");
        setNuevoDocente(nd => ({ ...nd, buscando: false }));
      }
    }
  };

  const crearDocente = async (e) => {
    e.preventDefault();
    if (!/^\d{10}$/.test(nuevoDocente.cedula)) { setError("La cédula debe tener 10 dígitos"); return; }
    if (nuevoDocente.existente === "sin_rol") { setError("El usuario existe pero no tiene rol DOCENTE."); return; }
    setSaving(true); setError("");
    try {
      const perfil = {
        cedula: nuevoDocente.cedula,
        nombres: nuevoDocente.nombres,
        apellidos: nuevoDocente.apellidos,
        fechaNacimiento: nuevoDocente.fechaNacimiento || null,
        genero: nuevoDocente.genero || null,
        telefono: nuevoDocente.telefono || null,
        telefonoAlt: nuevoDocente.telefonoAlt || null,
        direccion: nuevoDocente.direccion || null,
        correoPersonal: nuevoDocente.correoPersonal || null,
        tituloAcademico: nuevoDocente.tituloAcademico || null,
        especializacion: nuevoDocente.especializacion || null,
        fotoUrl: nuevoDocente.fotoUrl || null,
      };

      if (nuevoDocente.existente && nuevoDocente.idPersona) {
        await axios.put(`${API}/personas/${nuevoDocente.idPersona}`, perfil, { headers: H });
        setSuccess("Perfil docente actualizado.");
      } else {
        const resp = await axios.post(`${API}/usuarios`, {
          nombres: nuevoDocente.nombres,
          apellidos: nuevoDocente.apellidos,
          correo: nuevoDocente.correo,
          roles: [3],
        }, { headers: H });
        const idUsuario = resp.data?.idUsuario;
        if (!idUsuario) throw new Error("No se creó el usuario");
        await axios.post(`${API}/personas`, { idUsuario, ...perfil }, { headers: H });
        setSuccess("Docente creado. Las credenciales se enviaron al correo.");
      }

      setNuevoDocente(null);
      cargarPersonasDocentes();
    } catch (err) {
      setError(err.response?.data?.message || "No se pudo guardar el docente.");
    } finally {
      setSaving(false);
    }
  };

  const guardarPerfil = async (e) => {
    e.preventDefault();
    setSaving(true); setError("");
    try {
      await axios.put(`${API}/personas/${perfilEdit.idPersona}`, {
        cedula: perfilEdit.cedula,
        nombres: perfilEdit.nombres,
        apellidos: perfilEdit.apellidos,
        telefono: perfilEdit.telefono || null,
        direccion: perfilEdit.direccion || null,
        correoPersonal: perfilEdit.correoPersonal || null,
        tituloAcademico: perfilEdit.tituloAcademico || null,
        especializacion: perfilEdit.especializacion || null,
        fotoUrl: perfilEdit.fotoUrl || null,
      }, { headers: H });
      setSuccess("Perfil actualizado.");
      setPerfilEdit(null);
      cargarPersonasDocentes();
    } catch (err) {
      setError(err.response?.data?.message || "No se pudo guardar el perfil.");
    } finally {
      setSaving(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    const faltantes = [];
    if (!form.idDocente) faltantes.push("docente (busca por cédula)");
    if (!form.idAsignatura) faltantes.push("asignatura");
    if (!form.idGrado) faltantes.push("grado");
    if (!form.idParalelo) faltantes.push("paralelo");
    if (!form.idAnoLectivo) faltantes.push("año lectivo");
    if (faltantes.length > 0) {
      setError("Falta seleccionar: " + faltantes.join(", ") + ".");
      return;
    }

    setSaving(true);
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

  const abrirEditar = (a) => {
    setError("");
    setAsignEdit({
      idAsignacion: a.idAsignacion,
      idDocente: String(a.idDocente || ""),
      docenteNombre: a.docente,
      cedulaDocente: a.cedulaDocente,
      idAsignatura: String(a.idAsignatura || ""),
      idGrado: String(a.idGrado || ""),
      idParalelo: String(a.idParalelo || ""),
      idAnoLectivo: String(a.idAnoLectivo || ""),
      esTutor: a.esTutor,
    });
    if (a.idGrado) {
      axios.get(`${API}/asignaciones/grado/${a.idGrado}/paralelos`, { headers: H })
        .then(r => setParalelos(r.data)).catch(() => setParalelos([]));
    }
  };

  const guardarEdicionAsign = async (e) => {
    e.preventDefault();
    setSaving(true); setError("");
    try {
      await axios.put(`${API}/asignaciones/${asignEdit.idAsignacion}`, {
        idDocente: parseInt(asignEdit.idDocente),
        idAsignatura: parseInt(asignEdit.idAsignatura),
        idGrado: parseInt(asignEdit.idGrado),
        idParalelo: parseInt(asignEdit.idParalelo),
        idAnoLectivo: parseInt(asignEdit.idAnoLectivo),
        esTutor: asignEdit.esTutor,
      }, { headers: H });
      setSuccess("Asignación actualizada.");
      setAsignEdit(null);
      cargar();
    } catch (err) {
      setError(err.response?.data?.message || "No se pudo actualizar la asignación.");
    } finally {
      setSaving(false);
    }
  };

  // Agrupa asignaciones por curso: grado + paralelo + año lectivo
  const cursos = asignaciones.reduce((acc, a) => {
    const clave = `${a.grado} "${a.paralelo || "—"}" · ${a.anoLectivo}`;
    (acc[clave] = acc[clave] || []).push(a);
    return acc;
  }, {});

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

      {/* LISTA — agrupada por curso en tarjetas */}
      {seccion === "lista" && (
        loading ? (
          <p className="text-center text-slate-400 py-10">Cargando asignaciones...</p>
        ) : asignaciones.length === 0 ? (
          <div className="bg-white rounded-xl border border-slate-200 p-10 text-center text-slate-400">
            No hay asignaciones registradas.
          </div>
        ) : (
          <div className="grid grid-cols-1 xl:grid-cols-2 gap-4">
            {Object.entries(cursos).map(([curso, items]) => {
              const tutor = items.find(i => i.esTutor);
              return (
                <div key={curso} className="bg-white rounded-xl border border-slate-200 overflow-hidden">
                  <div style={{ backgroundColor: PRIMARY }} className="px-4 py-3 text-white flex justify-between items-center">
                    <div>
                      <h3 className="font-semibold text-sm">{curso}</h3>
                      <p className="text-white/70 text-xs">{items.length} asignatura(s)</p>
                    </div>
                    {tutor
                      ? <span className="bg-white/20 text-white text-xs font-medium px-2 py-1 rounded">Tutor: {tutor.docente}</span>
                      : <span className="bg-amber-400/30 text-white text-xs font-medium px-2 py-1 rounded">Sin tutor</span>}
                  </div>
                  <div className="divide-y divide-slate-100">
                    {items.map((a) => (
                      <div key={a.idAsignacion} className="px-4 py-3 flex items-center justify-between hover:bg-slate-50">
                        <div className="min-w-0">
                          <p className="text-sm font-medium text-slate-700 truncate">{a.asignatura}</p>
                          <p className="text-xs text-slate-500 truncate">
                            {a.docente}
                            {a.esTutor && <span className="ml-2 bg-blue-50 text-blue-700 text-[10px] font-semibold px-1.5 py-0.5 rounded">TUTOR</span>}
                          </p>
                        </div>
                        <div className="flex items-center gap-1 flex-shrink-0">
                          <button onClick={() => setAsignVer(a)} title="Ver detalle"
                            className="p-1.5 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-100">
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" /></svg>
                          </button>
                          <button onClick={() => abrirEditar(a)} title="Editar"
                            className="p-1.5 rounded-lg text-slate-400 hover:text-blue-600 hover:bg-blue-50">
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" /></svg>
                          </button>
                          <button onClick={() => toggleEstado(a)}
                            className={`text-xs font-semibold px-2 py-1 rounded ${a.activo ? "bg-green-100 text-green-700" : "bg-slate-100 text-slate-500"}`}>
                            {a.activo ? "Activa" : "Inactiva"}
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              );
            })}
          </div>
        )
      )}

      {/* NUEVO */}
      {seccion === "nuevo" && (
        <form onSubmit={handleSubmit} className="bg-white p-6 rounded-xl border border-slate-200 max-w-2xl space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Docente — buscar por cédula</label>
            <div className="flex gap-2">
              <input
                type="text"
                inputMode="numeric"
                maxLength={10}
                value={cedulaInput}
                onChange={(e) => { setCedulaInput(e.target.value.replace(/\D/g, "")); setDocenteSel(null); setForm(f => ({ ...f, idDocente: "" })); }}
                placeholder="10 dígitos"
                className="flex-1 border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50"
              />
              <button
                type="button"
                onClick={buscarDocentePorCedula}
                disabled={buscando || cedulaInput.length !== 10}
                style={{ backgroundColor: PRIMARY }}
                className="text-white px-4 py-2 rounded-lg text-sm font-medium hover:opacity-90 disabled:opacity-50"
              >
                {buscando ? "Buscando..." : "Buscar"}
              </button>
            </div>
            {docenteSel && (
              <div className="mt-3 border border-blue-100 bg-blue-50 rounded-lg p-3 text-sm">
                <p className="font-semibold text-slate-700">{docenteSel.nombres} {docenteSel.apellidos}</p>
                <p className="text-slate-500 text-xs">Usuario: <span className="font-mono">{docenteSel.username}</span> · {docenteSel.correo}</p>
                <p className="text-slate-500 text-xs mt-1">Roles: {[...(docenteSel.roles || [])].join(", ")}</p>
              </div>
            )}
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

      {/* DOCENTES */}
      {seccion === "docentes" && !perfilEdit && (
        <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
          <div className="p-4 border-b border-slate-100 flex justify-between items-center">
            <div>
              <h2 className="text-slate-700 font-semibold text-sm">Docentes registrados</h2>
              <p className="text-slate-400 text-xs">Perfiles ligados a usuarios con rol DOCENTE.</p>
            </div>
            <div className="flex items-center gap-3">
              <span className="text-xs text-slate-400">{personasDocentes.length} docente(s)</span>
              <button
                onClick={() => setNuevoDocente({
                cedula: "", nombres: "", apellidos: "", correo: "",
                fechaNacimiento: "", genero: "", telefono: "", telefonoAlt: "",
                direccion: "", correoPersonal: "", tituloAcademico: "",
                especializacion: "", fotoUrl: "",
                existente: null, buscando: false, idPersona: null, idUsuario: null
              })}
                style={{ backgroundColor: PRIMARY }}
                className="text-white text-xs font-medium px-3 py-1.5 rounded-lg hover:opacity-90"
              >
                + Nuevo docente
              </button>
            </div>
          </div>
          {loadingDocentes ? (
            <p className="text-center text-slate-400 py-10 text-sm">Cargando...</p>
          ) : personasDocentes.length === 0 ? (
            <p className="text-center text-slate-400 py-10 text-sm">No hay docentes registrados. Créalos desde el módulo Usuarios.</p>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-slate-50 text-slate-500 text-xs uppercase tracking-wide">
                  <th className="text-left px-4 py-3">Cédula</th>
                  <th className="text-left px-4 py-3">Nombres</th>
                  <th className="text-left px-4 py-3">Usuario</th>
                  <th className="text-left px-4 py-3">Título</th>
                  <th className="text-center px-4 py-3">Perfil</th>
                </tr>
              </thead>
              <tbody>
                {personasDocentes.map((p) => (
                  <tr key={p.idPersona} className="border-t border-slate-100 hover:bg-slate-50">
                    <td className="px-4 py-3 font-mono text-slate-600">{p.cedula}</td>
                    <td className="px-4 py-3 text-slate-700 font-medium">{p.nombres} {p.apellidos}</td>
                    <td className="px-4 py-3 text-slate-500 text-xs">{p.username}<br /><span className="text-slate-400">{p.correo}</span></td>
                    <td className="px-4 py-3 text-slate-600 text-xs">{p.tituloAcademico || <span className="text-slate-300">—</span>}</td>
                    <td className="px-4 py-3 text-center">
                      <button
                        onClick={() => setPerfilEdit({ ...p })}
                        className="text-xs font-semibold px-3 py-1 rounded bg-blue-50 text-blue-700 hover:bg-blue-100"
                      >
                        Editar
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {nuevoDocente && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-3xl overflow-hidden">
            <div style={{ backgroundColor: PRIMARY }} className="px-6 py-4 flex justify-between items-center text-white">
              <div>
                <h3 className="font-semibold">Nuevo docente</h3>
                <p className="text-xs text-white/70">Se creará el usuario con rol DOCENTE y su perfil profesional.</p>
              </div>
              <button onClick={() => { setNuevoDocente(null); setError(""); }} className="text-white/70 hover:text-white">✕</button>
            </div>
            <form onSubmit={crearDocente} className="p-6 space-y-5 max-h-[75vh] overflow-y-auto">

              {/* FOTO */}
              <div className="flex items-center gap-4 pb-4 border-b border-slate-100">
                <div className="w-20 h-20 rounded-full bg-slate-100 border border-slate-200 flex items-center justify-center overflow-hidden flex-shrink-0">
                  {nuevoDocente.fotoUrl ? (
                    <img src={nuevoDocente.fotoUrl.startsWith("http") ? nuevoDocente.fotoUrl : `${API.replace("/api","")}${nuevoDocente.fotoUrl}`} alt="" className="w-full h-full object-cover" />
                  ) : (
                    <svg className="w-8 h-8 text-slate-300" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M15 12a3 3 0 11-6 0 3 3 0 016 0zM4 20a8 8 0 0116 0" /></svg>
                  )}
                </div>
                <div className="flex-1">
                  <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Foto de perfil</label>
                  <input type="file" accept="image/jpeg,image/png,image/webp"
                    onChange={(e) => { const f = e.target.files?.[0]; if (f) subirFoto(f, (url) => setNuevoDocente(nd => ({ ...nd, fotoUrl: url }))); }}
                    className="text-xs text-slate-500 file:mr-3 file:py-1.5 file:px-3 file:rounded-lg file:border-0 file:bg-slate-100 file:text-slate-700 file:cursor-pointer file:hover:bg-slate-200" />
                  <p className="text-xs text-slate-400 mt-1">JPG, PNG o WEBP · máximo 3 MB</p>
                </div>
              </div>

              {/* DATOS DE CUENTA — auto-llena desde el usuario */}
              <div>
                <p className="text-xs font-semibold text-slate-400 uppercase tracking-wide mb-2">Datos de cuenta</p>
                <div>
                  <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">
                    Cédula * <span className="text-slate-400 normal-case font-normal">— al escribir 10 dígitos se carga el usuario</span>
                  </label>
                  <input type="text" inputMode="numeric" maxLength={10} required
                    value={nuevoDocente.cedula}
                    onChange={(e) => autoBuscarCedula(e.target.value.replace(/\D/g, ""))}
                    placeholder="10 dígitos"
                    className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50 font-mono" />
                  {nuevoDocente.buscando && <p className="text-xs text-slate-400 mt-1">Buscando usuario...</p>}
                  {nuevoDocente.existente === "nuevo" && (
                    <p className="text-xs text-amber-600 bg-amber-50 border border-amber-100 rounded px-3 py-2 mt-2">
                      No hay un usuario con esa cédula. Se creará uno nuevo con rol DOCENTE.
                    </p>
                  )}
                  {nuevoDocente.existente && nuevoDocente.existente !== "nuevo" && nuevoDocente.existente !== "sin_rol" && (
                    <p className="text-xs text-green-700 bg-green-50 border border-green-100 rounded px-3 py-2 mt-2">
                      ✓ Usuario encontrado: <strong>{nuevoDocente.existente.username}</strong> — se completará su perfil profesional.
                    </p>
                  )}
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-3 mt-3">
                  <div>
                    <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Nombres *</label>
                    <input type="text" required value={nuevoDocente.nombres}
                      disabled={!!nuevoDocente.existente && nuevoDocente.existente !== "nuevo"}
                      onChange={(e) => setNuevoDocente({ ...nuevoDocente, nombres: e.target.value })}
                      className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50 disabled:bg-slate-100 disabled:text-slate-500" />
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Apellidos *</label>
                    <input type="text" required value={nuevoDocente.apellidos}
                      disabled={!!nuevoDocente.existente && nuevoDocente.existente !== "nuevo"}
                      onChange={(e) => setNuevoDocente({ ...nuevoDocente, apellidos: e.target.value })}
                      className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50 disabled:bg-slate-100 disabled:text-slate-500" />
                  </div>
                  <div className="md:col-span-2">
                    <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Correo institucional *</label>
                    <input type="email" required value={nuevoDocente.correo}
                      disabled={!!nuevoDocente.existente && nuevoDocente.existente !== "nuevo"}
                      onChange={(e) => setNuevoDocente({ ...nuevoDocente, correo: e.target.value })}
                      placeholder="docente@sga.com"
                      className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50 disabled:bg-slate-100 disabled:text-slate-500" />
                  </div>
                </div>
              </div>

              {/* DATOS PERSONALES */}
              <div>
                <p className="text-xs font-semibold text-slate-400 uppercase tracking-wide mb-2">Datos personales</p>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                  <div>
                    <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Fecha nacimiento</label>
                    <input type="date" value={nuevoDocente.fechaNacimiento}
                      onChange={(e) => setNuevoDocente({ ...nuevoDocente, fechaNacimiento: e.target.value })}
                      className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50" />
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Género</label>
                    <select value={nuevoDocente.genero}
                      onChange={(e) => setNuevoDocente({ ...nuevoDocente, genero: e.target.value })}
                      className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50">
                      <option value="">—</option>
                      <option value="MASCULINO">Masculino</option>
                      <option value="FEMENINO">Femenino</option>
                      <option value="OTRO">Otro</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Teléfono</label>
                    <input type="text" value={nuevoDocente.telefono}
                      onChange={(e) => setNuevoDocente({ ...nuevoDocente, telefono: e.target.value })}
                      className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50" />
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Teléfono alt.</label>
                    <input type="text" value={nuevoDocente.telefonoAlt}
                      onChange={(e) => setNuevoDocente({ ...nuevoDocente, telefonoAlt: e.target.value })}
                      className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50" />
                  </div>
                  <div className="md:col-span-2">
                    <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Correo personal (adicional)</label>
                    <input type="email" value={nuevoDocente.correoPersonal}
                      onChange={(e) => setNuevoDocente({ ...nuevoDocente, correoPersonal: e.target.value })}
                      className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50" />
                  </div>
                  <div className="md:col-span-3">
                    <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Dirección</label>
                    <input type="text" value={nuevoDocente.direccion}
                      onChange={(e) => setNuevoDocente({ ...nuevoDocente, direccion: e.target.value })}
                      className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50" />
                  </div>
                </div>
              </div>

              {/* DATOS PROFESIONALES */}
              <div>
                <p className="text-xs font-semibold text-slate-400 uppercase tracking-wide mb-2">Datos profesionales</p>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Título académico</label>
                    <input type="text" value={nuevoDocente.tituloAcademico}
                      onChange={(e) => setNuevoDocente({ ...nuevoDocente, tituloAcademico: e.target.value })}
                      placeholder="Lic. en Ciencias de la Educación"
                      className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50" />
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Especialización</label>
                    <input type="text" value={nuevoDocente.especializacion}
                      onChange={(e) => setNuevoDocente({ ...nuevoDocente, especializacion: e.target.value })}
                      placeholder="Matemáticas, Lengua, etc."
                      className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50" />
                  </div>
                </div>
              </div>

              {error && <div className="bg-red-50 border border-red-200 rounded-lg px-3 py-2 text-red-600 text-xs">{error}</div>}

              <div className="flex gap-3 pt-2 border-t border-slate-100">
                <button type="button" onClick={() => { setNuevoDocente(null); setError(""); }}
                  className="flex-1 py-2 border border-slate-200 rounded-lg text-sm text-slate-600 hover:bg-slate-50">
                  Cancelar
                </button>
                <button type="submit" disabled={saving || nuevoDocente.buscando || nuevoDocente.existente === "sin_rol"}
                  style={{ backgroundColor: PRIMARY }}
                  className="flex-1 py-2 text-white rounded-lg text-sm font-medium hover:opacity-90 disabled:opacity-60">
                  {saving
                    ? "Guardando..."
                    : nuevoDocente.existente && nuevoDocente.existente !== "nuevo"
                      ? "Guardar perfil"
                      : "Crear docente"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* EDITAR PERFIL DOCENTE */}
      {seccion === "docentes" && perfilEdit && (
        <form onSubmit={guardarPerfil} className="bg-white p-6 rounded-xl border border-slate-200 max-w-3xl space-y-4">
          <div className="flex items-center gap-4 pb-3 border-b border-slate-100">
            <div className="w-16 h-16 rounded-full bg-slate-100 border border-slate-200 flex items-center justify-center overflow-hidden">
              {perfilEdit.fotoUrl ? (
                <img src={perfilEdit.fotoUrl} alt="" className="w-full h-full object-cover" />
              ) : (
                <span className="text-slate-400 text-xl font-bold">{(perfilEdit.nombres?.[0] || "?").toUpperCase()}</span>
              )}
            </div>
            <div>
              <h2 className="text-slate-700 font-semibold">{perfilEdit.nombres} {perfilEdit.apellidos}</h2>
              <p className="text-xs text-slate-500">{perfilEdit.username} · {perfilEdit.correo}</p>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Cédula</label>
              <input type="text" value={perfilEdit.cedula || ""} disabled
                className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-100 font-mono text-slate-500" />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Teléfono</label>
              <input type="text" value={perfilEdit.telefono || ""} onChange={(e) => setPerfilEdit({ ...perfilEdit, telefono: e.target.value })}
                className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50" />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Dirección</label>
            <input type="text" value={perfilEdit.direccion || ""} onChange={(e) => setPerfilEdit({ ...perfilEdit, direccion: e.target.value })}
              className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50" />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Correo personal (adicional)</label>
            <input type="email" value={perfilEdit.correoPersonal || ""} onChange={(e) => setPerfilEdit({ ...perfilEdit, correoPersonal: e.target.value })}
              className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50" />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Título académico</label>
              <input type="text" value={perfilEdit.tituloAcademico || ""} onChange={(e) => setPerfilEdit({ ...perfilEdit, tituloAcademico: e.target.value })}
                placeholder="Ej: Lic. Ciencias de la Educación"
                className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50" />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Especialización</label>
              <input type="text" value={perfilEdit.especializacion || ""} onChange={(e) => setPerfilEdit({ ...perfilEdit, especializacion: e.target.value })}
                placeholder="Ej: Matemáticas"
                className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50" />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Foto</label>
            <input type="file" accept="image/jpeg,image/png,image/webp"
              onChange={(e) => { const f = e.target.files?.[0]; if (f) subirFoto(f, (url) => setPerfilEdit(p => ({ ...p, fotoUrl: url }))); }}
              className="text-xs text-slate-500 file:mr-3 file:py-1.5 file:px-3 file:rounded-lg file:border-0 file:bg-slate-100 file:text-slate-700 file:cursor-pointer" />
            <p className="text-xs text-slate-400 mt-1">JPG, PNG o WEBP · máximo 3 MB</p>
          </div>

          <div className="flex gap-3 pt-2">
            <button type="submit" disabled={saving} style={{ backgroundColor: PRIMARY }}
              className="text-white px-5 py-2 rounded-lg text-sm font-medium hover:opacity-90 disabled:opacity-60">
              {saving ? "Guardando..." : "Guardar cambios"}
            </button>
            <button type="button" onClick={() => setPerfilEdit(null)}
              className="px-5 py-2 rounded-lg text-sm font-medium text-slate-500 hover:bg-slate-100">
              Cancelar
            </button>
          </div>
        </form>
      )}

      {/* VER DETALLE ASIGNACIÓN */}
      {asignVer && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden">
            <div style={{ backgroundColor: PRIMARY }} className="px-6 py-4 flex justify-between items-center text-white">
              <h3 className="font-semibold">Detalle de la asignación</h3>
              <button onClick={() => setAsignVer(null)} className="text-white/70 hover:text-white">✕</button>
            </div>
            <div className="p-6 space-y-4">
              <div className="flex items-center gap-4 pb-3 border-b border-slate-100">
                <div className="w-14 h-14 rounded-full bg-slate-100 border border-slate-200 flex items-center justify-center overflow-hidden">
                  {asignVer.fotoDocente ? (
                    <img src={asignVer.fotoDocente.startsWith("http") ? asignVer.fotoDocente : `${API.replace("/api","")}${asignVer.fotoDocente}`} alt="" className="w-full h-full object-cover" />
                  ) : (
                    <span className="text-slate-400 text-lg font-bold">{asignVer.docente?.[0]?.toUpperCase()}</span>
                  )}
                </div>
                <div>
                  <p className="font-semibold text-slate-700">{asignVer.docente}</p>
                  <p className="text-xs text-slate-500 font-mono">{asignVer.cedulaDocente || "—"}</p>
                </div>
              </div>
              <Detalle label="Asignatura" value={asignVer.asignatura} />
              <Detalle label="Grado" value={asignVer.grado} />
              <Detalle label="Paralelo" value={asignVer.paralelo} />
              <Detalle label="Año lectivo" value={asignVer.anoLectivo} />
              <Detalle label="Tutor del curso" value={asignVer.esTutor ? "Sí" : "No"} />
              <Detalle label="Estado" value={asignVer.activo ? "Activa" : "Inactiva"} />
              <Detalle label="Título docente" value={asignVer.tituloDocente} />
              <Detalle label="Correo docente" value={asignVer.correoDocente} />
              <Detalle label="Asignado por" value={asignVer.asignadoPor} />
            </div>
            <div className="p-4 border-t border-slate-100 flex justify-end">
              <button onClick={() => setAsignVer(null)} className="px-5 py-2 rounded-lg text-sm font-medium text-slate-500 hover:bg-slate-100">Cerrar</button>
            </div>
          </div>
        </div>
      )}

      {/* EDITAR ASIGNACIÓN */}
      {asignEdit && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg overflow-hidden">
            <div style={{ backgroundColor: PRIMARY }} className="px-6 py-4 flex justify-between items-center text-white">
              <div>
                <h3 className="font-semibold">Editar asignación</h3>
                <p className="text-xs text-white/70">{asignEdit.docenteNombre} · {asignEdit.cedulaDocente}</p>
              </div>
              <button onClick={() => { setAsignEdit(null); setError(""); }} className="text-white/70 hover:text-white">✕</button>
            </div>
            <form onSubmit={guardarEdicionAsign} className="p-6 space-y-4">
              {error && <div className="bg-red-50 border border-red-200 rounded-lg px-3 py-2 text-red-600 text-xs">{error}</div>}
              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Asignatura</label>
                <select required value={asignEdit.idAsignatura} onChange={(e) => setAsignEdit({ ...asignEdit, idAsignatura: e.target.value })}
                  className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50">
                  <option value="">Seleccione...</option>
                  {asignaturas.map((s) => <option key={s.idAsignatura} value={s.idAsignatura}>{s.nombre}</option>)}
                </select>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Grado</label>
                  <select required value={asignEdit.idGrado}
                    onChange={(e) => {
                      const g = e.target.value;
                      setAsignEdit({ ...asignEdit, idGrado: g, idParalelo: "" });
                      if (g) axios.get(`${API}/asignaciones/grado/${g}/paralelos`, { headers: H }).then(r => setParalelos(r.data)).catch(() => setParalelos([]));
                    }}
                    className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50">
                    <option value="">Seleccione...</option>
                    {grados.map((g) => <option key={g.idGrado} value={g.idGrado}>{g.nombre}</option>)}
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Paralelo</label>
                  <select required value={asignEdit.idParalelo} onChange={(e) => setAsignEdit({ ...asignEdit, idParalelo: e.target.value })}
                    disabled={!asignEdit.idGrado}
                    className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50 disabled:opacity-50">
                    <option value="">{asignEdit.idGrado ? "Seleccione..." : "Elija un grado"}</option>
                    {paralelos.map((p) => <option key={p.idParalelo} value={p.idParalelo}>Paralelo {p.letra}</option>)}
                  </select>
                </div>
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Año lectivo</label>
                <select required value={asignEdit.idAnoLectivo} onChange={(e) => setAsignEdit({ ...asignEdit, idAnoLectivo: e.target.value })}
                  className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50">
                  <option value="">Seleccione...</option>
                  {anosLectivos.map((y) => <option key={y.idAnoLectivo} value={y.idAnoLectivo}>{y.nombre}</option>)}
                </select>
              </div>
              <label className="flex items-center gap-2 text-sm text-slate-600">
                <input type="checkbox" checked={asignEdit.esTutor} onChange={(e) => setAsignEdit({ ...asignEdit, esTutor: e.target.checked })} />
                Es docente tutor del curso
              </label>
              <div className="flex gap-3 pt-2">
                <button type="button" onClick={() => { setAsignEdit(null); setError(""); }}
                  className="flex-1 py-2 border border-slate-200 rounded-lg text-sm text-slate-600 hover:bg-slate-50">Cancelar</button>
                <button type="submit" disabled={saving} style={{ backgroundColor: PRIMARY }}
                  className="flex-1 py-2 text-white rounded-lg text-sm font-medium hover:opacity-90 disabled:opacity-60">
                  {saving ? "Guardando..." : "Guardar cambios"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </Layout>
  );
}
