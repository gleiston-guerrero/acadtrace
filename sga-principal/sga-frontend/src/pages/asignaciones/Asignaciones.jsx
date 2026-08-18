import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import axios from "axios";
import Layout from "../../components/Layout";

const API = `http://${window.location.hostname}:8080/api`;
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

const CARD_PALETTES = [
  { id: "navy", header: "bg-[#2b3c66]" },
  { id: "slate", header: "bg-[#475569]" },
  { id: "indigo", header: "bg-[#3b4266]" },
  { id: "teal", header: "bg-[#33535e]" },
  { id: "olive", header: "bg-[#4a5840]" },
  { id: "zinc", header: "bg-[#52525b]" },
];

export default function Asignaciones() {
  const navigate = useNavigate();
  const [seccion, setSeccion] = useState("lista");
  const [asignaciones, setAsignaciones] = useState([]);
  const [asignaturas, setAsignaturas] = useState([]);
  const [grados, setGrados] = useState([]);
  const [paralelos, setParalelos] = useState([]);
  const [anosLectivos, setAnosLectivos] = useState([]);
  const [materiasMalla, setMateriasMalla] = useState([]);
  const [loadingMaterias, setLoadingMaterias] = useState(false);
  const [materiasMallaEdit, setMateriasMallaEdit] = useState([]);
  const [form, setForm] = useState(formVacio);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const [cedulaInput, setCedulaInput] = useState("");
  const [buscando, setBuscando] = useState(false);
  const [docenteSel, setDocenteSel] = useState(null);
  const [modoBusqueda, setModoBusqueda] = useState("cedula"); // "cedula" | "nombre"
  const [nombreQuery, setNombreQuery] = useState("");

  const [filtroAsignatura, setFiltroAsignatura] = useState("");
  const [personasDocentes, setPersonasDocentes] = useState([]);
  const [loadingDocentes, setLoadingDocentes] = useState(false);

  const ALIASES = {
    lenguaje: "lengua",
    literatura: "lengua",
    mate: "matemática",
    matematica: "matemática",
    matematicas: "matemática",
    ciencias: "ciencias",
    naturales: "ciencias",
    cn: "ciencias",
    sociales: "estudios",
    estudios: "estudios",
    "ee.ss": "estudios",
    fisica: "física",
    deporte: "física",
    ef: "física",
    eca: "cultural",
    arte: "cultural",
    cultural: "cultural",
    ingles: "inglés",
    english: "inglés",
    lectura: "lectura",
    animacion: "lectura",
    tutor: "acompañamiento",
    "acompañamiento": "acompañamiento",
    curriculo: "currículo",
    integrado: "currículo",
  };

  const opcionesAsignaturas = (() => {
    const mapa = new Map();
    (materiasMalla || []).forEach(m => {
      if (m.idAsignatura) mapa.set(Number(m.idAsignatura), m.asignatura || m.nombre);
    });
    (asignaturas || []).forEach(a => {
      if (!mapa.has(Number(a.idAsignatura))) {
        mapa.set(Number(a.idAsignatura), a.nombre);
      }
    });
    let list = Array.from(mapa.entries()).map(([id, nombre]) => ({ idAsignatura: id, nombre }));
    if (filtroAsignatura.trim()) {
      const qRaw = filtroAsignatura.toLowerCase().trim();
      const qAlias = ALIASES[qRaw] || qRaw;
      list = list.filter(item => {
        const nom = item.nombre.toLowerCase();
        return nom.includes(qRaw) || nom.includes(qAlias);
      });
    }
    return list;
  })();
  const [asignEdit, setAsignEdit] = useState(null);
  const [asignVer, setAsignVer] = useState(null);
  const [docenteVer, setDocenteVer] = useState(null);

  const [cardColors, setCardColors] = useState({});
  const [activeMenuKey, setActiveMenuKey] = useState(null);

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
    cargarPersonasDocentes();
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

  // Cargar solo las materias que ya están en la malla del grado + año lectivo
  useEffect(() => {
    if (form.idGrado && form.idAnoLectivo) {
      setLoadingMaterias(true);
      axios.get(`${API}/malla/grado/${form.idGrado}`, { headers: H, params: { idAnoLectivo: form.idAnoLectivo } })
        .then(r => setMateriasMalla(r.data?.materias || []))
        .catch(() => setMateriasMalla([]))
        .finally(() => setLoadingMaterias(false));
    } else {
      setMateriasMalla([]);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [form.idGrado, form.idAnoLectivo]);

  const cargarMateriasMallaEdit = (idGrado, idAnoLectivo) => {
    if (!idGrado || !idAnoLectivo) { setMateriasMallaEdit([]); return; }
    axios.get(`${API}/malla/grado/${idGrado}`, { headers: H, params: { idAnoLectivo } })
      .then(r => setMateriasMallaEdit(r.data?.materias || []))
      .catch(() => setMateriasMallaEdit([]));
  };

  const handleSeccion = (id) => {
    setSeccion(id);
    setError("");
    if (id === "nuevo") {
      setForm(formVacio);
      setCedulaInput("");
      setDocenteSel(null);
      setNombreQuery("");
      setModoBusqueda("cedula");
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

  const seleccionarDocentePorNombre = (p) => {
    setDocenteSel(p);
    setForm(f => ({ ...f, idDocente: String(p.idPersona) }));
    setError("");
  };

  const docentesFiltrados = nombreQuery.trim().length < 2
    ? []
    : personasDocentes.filter(p =>
        `${p.nombres} ${p.apellidos} ${p.cedula}`.toLowerCase().includes(nombreQuery.trim().toLowerCase())
      ).slice(0, 8);

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
    cargarMateriasMallaEdit(a.idGrado, a.idAnoLectivo);
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
            {Object.entries(cursos).map(([curso, items], idx) => {
              const tutor = items.find(i => i.esTutor);
              const savedColor = cardColors[curso] ?? (localStorage.getItem(`asig_color_${curso}`) ? parseInt(localStorage.getItem(`asig_color_${curso}`), 10) : idx % CARD_PALETTES.length);
              const currentPalette = CARD_PALETTES[savedColor % CARD_PALETTES.length];
              const isMenuOpen = activeMenuKey === curso;

              const cambiarColorCard = () => {
                const next = (savedColor + 1) % CARD_PALETTES.length;
                setCardColors(prev => ({ ...prev, [curso]: next }));
                localStorage.setItem(`asig_color_${curso}`, String(next));
                setActiveMenuKey(null);
              };

              return (
                <div key={curso} className="bg-white rounded-2xl border border-slate-200/90 overflow-hidden shadow-sm hover:shadow-md transition-all duration-200">
                  {/* BANDA DE ENCABEZADO CON TONO SUAVE Y 3 PUNTOS */}
                  <div className={`${currentPalette.header} px-4 py-3 text-white flex justify-between items-center relative`}>
                    <div>
                      <h3 className="font-bold text-sm leading-snug">{curso}</h3>
                      <p className="text-white/80 text-[11px] font-medium mt-0.5">{items.length} asignatura(s)</p>
                    </div>

                    <div className="flex items-center gap-2">
                      {tutor
                        ? <span className="bg-white/20 text-white text-xs font-semibold px-2.5 py-1 rounded-md">Tutor: {tutor.docente}</span>
                        : <span className="bg-amber-500/80 text-white text-xs font-semibold px-2.5 py-1 rounded-md">Sin tutor</span>}

                      {/* BOTÓN 3 PUNTOS (•••) */}
                      <div className="relative">
                        <button
                          type="button"
                          onClick={() => setActiveMenuKey(isMenuOpen ? null : curso)}
                          className="w-6 h-6 rounded bg-white/20 hover:bg-white/30 text-white flex items-center justify-center text-xs font-bold transition cursor-pointer"
                          title="Opciones"
                        >
                          •••
                        </button>

                        {/* MENÚ DESPLEGABLE LIMPIO SIN EMOTICONES */}
                        {isMenuOpen && (
                          <div className="absolute right-0 top-7 w-40 bg-white rounded-lg shadow-xl border border-slate-200 py-1 z-30 text-slate-700">
                            <button
                              type="button"
                              onClick={cambiarColorCard}
                              className="w-full text-left px-3 py-1.5 text-xs hover:bg-slate-50 text-slate-700 transition"
                            >
                              Cambiar color
                            </button>
                          </div>
                        )}
                      </div>
                    </div>
                  </div>

                  {/* LISTA DE ASIGNATURAS DEL CURSO */}
                  <div className="divide-y divide-slate-100">
                    {items.map((a) => (
                      <div key={a.idAsignacion} className="px-4 py-3 flex items-center justify-between hover:bg-slate-50 transition">
                        <div className="min-w-0">
                          <p className="text-sm font-semibold text-slate-700 truncate">{a.asignatura}</p>
                          <p className="text-xs text-slate-500 truncate">
                            {a.docente}
                            {a.esTutor && <span className="ml-2 bg-blue-50 text-blue-700 text-[10px] font-bold px-1.5 py-0.5 rounded">TUTOR</span>}
                          </p>
                        </div>
                        <div className="flex items-center gap-1.5 flex-shrink-0">
                          <button onClick={() => setAsignVer(a)} title="Ver detalle"
                            className="p-1.5 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition">
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" /></svg>
                          </button>
                          <button onClick={() => abrirEditar(a)} title="Editar"
                            className="p-1.5 rounded-lg text-slate-400 hover:text-blue-600 hover:bg-blue-50 transition">
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" /></svg>
                          </button>
                          <button onClick={() => toggleEstado(a)}
                            className={`text-xs font-semibold px-2 py-1 rounded transition ${a.activo ? "bg-green-100 text-green-700" : "bg-slate-100 text-slate-500"}`}>
                            {a.activo ? "Activa" : "Inactiva"}
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>

                  {isMenuOpen && (
                    <div className="fixed inset-0 z-20 cursor-default" onClick={() => setActiveMenuKey(null)} />
                  )}
                </div>
              );
            })}
          </div>
        )
      )}

      {/* NUEVO */}
      {seccion === "nuevo" && (
        <form onSubmit={handleSubmit} className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
          {/* Encabezado */}
          <div style={{ backgroundColor: PRIMARY }} className="px-6 py-5 text-white">
            <h2 className="font-bold text-lg leading-tight">Nueva asignación</h2>
            <p className="text-white/70 text-xs mt-0.5">Vincula un docente con una materia de la malla del curso.</p>
          </div>

          <div className="p-6 lg:p-8 grid grid-cols-1 lg:grid-cols-3 gap-8">
            {/* COLUMNA IZQUIERDA — DOCENTE */}
            <div className="lg:col-span-1">
              <div className="flex items-center gap-2 mb-3">
                <span className="w-6 h-6 rounded-full text-white text-xs font-bold flex items-center justify-center" style={{ backgroundColor: PRIMARY }}>1</span>
                <h3 className="text-sm font-bold text-slate-700">Docente</h3>
              </div>
              {/* Conmutador cédula / nombre */}
              <div className="inline-flex rounded-lg border border-slate-200 p-0.5 mb-2 bg-slate-50">
                <button type="button" onClick={() => { setModoBusqueda("cedula"); setDocenteSel(null); setForm(f => ({ ...f, idDocente: "" })); }}
                  className={`px-3 py-1 rounded-md text-xs font-medium transition ${modoBusqueda === "cedula" ? "bg-white shadow-sm text-slate-700" : "text-slate-400"}`}>
                  Por cédula
                </button>
                <button type="button" onClick={() => { setModoBusqueda("nombre"); setDocenteSel(null); setForm(f => ({ ...f, idDocente: "" })); }}
                  className={`px-3 py-1 rounded-md text-xs font-medium transition ${modoBusqueda === "nombre" ? "bg-white shadow-sm text-slate-700" : "text-slate-400"}`}>
                  Por nombre
                </button>
              </div>

              {modoBusqueda === "cedula" ? (
                <>
                  <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Buscar por cédula</label>
                  <div className="flex gap-2">
                    <input
                      type="text"
                      inputMode="numeric"
                      maxLength={10}
                      value={cedulaInput}
                      onChange={(e) => { setCedulaInput(e.target.value.replace(/\D/g, "")); setDocenteSel(null); setForm(f => ({ ...f, idDocente: "" })); }}
                      placeholder="10 dígitos"
                      className="flex-1 min-w-0 border border-slate-200 rounded-lg px-3 py-2.5 text-sm bg-slate-50 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                    />
                    <button
                      type="button"
                      onClick={buscarDocentePorCedula}
                      disabled={buscando || cedulaInput.length !== 10}
                      style={{ backgroundColor: PRIMARY }}
                      className="text-white px-4 py-2.5 rounded-lg text-sm font-medium hover:opacity-90 disabled:opacity-50 flex-shrink-0"
                    >
                      {buscando ? "..." : "Buscar"}
                    </button>
                  </div>
                </>
              ) : (
                <div>
                  <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Buscar por nombre</label>
                  <input
                    type="text"
                    value={nombreQuery}
                    onChange={(e) => { setNombreQuery(e.target.value); setDocenteSel(null); setForm(f => ({ ...f, idDocente: "" })); }}
                    placeholder="Escribe nombre, apellido o cédula..."
                    className="w-full border border-slate-200 rounded-lg px-3 py-2.5 text-sm bg-slate-50 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                  />
                  {nombreQuery.trim().length >= 2 && !docenteSel && (
                    <div className="mt-2 border border-slate-200 rounded-lg divide-y divide-slate-100 max-h-52 overflow-y-auto">
                      {docentesFiltrados.length === 0 ? (
                        <p className="px-3 py-3 text-xs text-slate-400">Sin coincidencias entre los docentes registrados.</p>
                      ) : docentesFiltrados.map((p) => (
                        <button key={p.idPersona} type="button" onClick={() => seleccionarDocentePorNombre(p)}
                          className="w-full text-left px-3 py-2 hover:bg-slate-50 transition">
                          <p className="text-sm font-medium text-slate-700">{p.nombres} {p.apellidos}</p>
                          <p className="text-xs text-slate-400 font-mono">{p.cedula || "sin cédula"}</p>
                        </button>
                      ))}
                    </div>
                  )}
                </div>
              )}
              {docenteSel ? (
                <div className="mt-4 border border-blue-100 bg-blue-50 rounded-xl p-4">
                  <div className="flex items-center gap-3">
                    <div className="w-11 h-11 rounded-full flex items-center justify-center text-white font-bold flex-shrink-0" style={{ backgroundColor: PRIMARY }}>
                      {(docenteSel.nombres?.[0] || "?").toUpperCase()}
                    </div>
                    <div className="min-w-0">
                      <p className="font-semibold text-slate-700 truncate">{docenteSel.nombres} {docenteSel.apellidos}</p>
                      <p className="text-slate-500 text-xs truncate">{docenteSel.correo}</p>
                    </div>
                  </div>
                  <p className="text-slate-500 text-xs mt-3 pt-3 border-t border-blue-100">
                    Usuario: <span className="font-mono text-slate-600">{docenteSel.username}</span>
                  </p>
                </div>
              ) : (
                <div className="mt-4 border border-dashed border-slate-200 rounded-xl p-6 text-center">
                  <p className="text-xs text-slate-400">Ingresa la cédula y presiona <strong>Buscar</strong> para seleccionar al docente.</p>
                </div>
              )}
            </div>

            {/* COLUMNA DERECHA — DETALLE ACADÉMICO */}
            <div className="lg:col-span-2 lg:border-l lg:border-slate-100 lg:pl-8">
              <div className="flex items-center gap-2 mb-4">
                <span className="w-6 h-6 rounded-full text-white text-xs font-bold flex items-center justify-center" style={{ backgroundColor: PRIMARY }}>2</span>
                <h3 className="text-sm font-bold text-slate-700">Curso y materia</h3>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                <div>
                  <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Grado</label>
                  <select required value={form.idGrado} onChange={(e) => setForm({ ...form, idGrado: e.target.value, idParalelo: "", idAsignatura: "" })}
                    className="w-full border border-slate-200 rounded-lg px-3 py-2.5 text-sm bg-slate-50 focus:ring-2 focus:ring-blue-500 focus:outline-none">
                    <option value="">Seleccione...</option>
                    {grados.map((g) => <option key={g.idGrado} value={g.idGrado}>{g.nombre}</option>)}
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Paralelo</label>
                  <select required value={form.idParalelo} onChange={(e) => setForm({ ...form, idParalelo: e.target.value })}
                    disabled={!form.idGrado}
                    className="w-full border border-slate-200 rounded-lg px-3 py-2.5 text-sm bg-slate-50 disabled:opacity-50 focus:ring-2 focus:ring-blue-500 focus:outline-none">
                    <option value="">{form.idGrado ? "Seleccione..." : "Elija un grado primero"}</option>
                    {paralelos.map((p) => <option key={p.idParalelo} value={p.idParalelo}>Paralelo {p.letra}</option>)}
                  </select>
                </div>
                <div className="md:col-span-2">
                  <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Año lectivo</label>
                  <select required value={form.idAnoLectivo} onChange={(e) => setForm({ ...form, idAnoLectivo: e.target.value, idAsignatura: "" })}
                    className="w-full border border-slate-200 rounded-lg px-3 py-2.5 text-sm bg-slate-50 focus:ring-2 focus:ring-blue-500 focus:outline-none">
                    <option value="">Seleccione...</option>
                    {anosLectivos.map((y) => <option key={y.idAnoLectivo} value={y.idAnoLectivo}>{y.nombre}</option>)}
                  </select>
                </div>
                <div className="md:col-span-2">
                  <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">
                    Asignatura <span className="text-slate-400 normal-case font-normal">— seleccione o busque directamente</span>
                  </label>
                  <div className="mb-2">
                    <input
                      type="text"
                      value={filtroAsignatura}
                      onChange={(e) => setFiltroAsignatura(e.target.value)}
                      placeholder="🔍 Buscar materia por nombre (Lengua, Matemática, etc.)..."
                      className="w-full border border-slate-200 rounded-lg px-3 py-2 text-xs bg-white focus:ring-2 focus:ring-blue-500 focus:outline-none"
                    />
                  </div>
                  <select required value={form.idAsignatura} onChange={(e) => setForm({ ...form, idAsignatura: e.target.value })}
                    className="w-full border border-slate-200 rounded-lg px-3 py-2.5 text-sm bg-slate-50 focus:ring-2 focus:ring-blue-500 focus:outline-none">
                    <option value="">Seleccione la materia...</option>
                    {opcionesAsignaturas.map((m) => (
                      <option key={m.idAsignatura} value={m.idAsignatura}>{m.nombre}</option>
                    ))}
                  </select>
                </div>
              </div>

              <label className="flex items-center gap-2 text-sm text-slate-600 mt-5 select-none cursor-pointer">
                <input type="checkbox" checked={form.esTutor} onChange={(e) => setForm({ ...form, esTutor: e.target.checked })} className="w-4 h-4" />
                Es docente tutor del curso
              </label>
            </div>
          </div>

          {/* Pie de acciones */}
          <div className="px-6 lg:px-8 py-4 bg-slate-50 border-t border-slate-100 flex gap-3 justify-end">
            <button type="button" onClick={() => { setForm(formVacio); setCedulaInput(""); setDocenteSel(null); setSeccion("lista"); }}
              className="px-5 py-2.5 rounded-lg text-sm font-medium text-slate-500 hover:bg-slate-100 transition">
              Cancelar
            </button>
            <button type="submit" disabled={saving} style={{ backgroundColor: PRIMARY }}
              className="text-white px-6 py-2.5 rounded-lg text-sm font-medium hover:opacity-90 disabled:opacity-60 shadow-sm transition">
              {saving ? "Guardando..." : "Crear Asignación"}
            </button>
          </div>
        </form>
      )}

      {/* DOCENTES — solo consulta. La gestión vive en el módulo Usuarios. */}
      {seccion === "docentes" && (
        <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
          <div className="p-4 border-b border-slate-100 flex justify-between items-center gap-3 flex-wrap">
            <div>
              <h2 className="text-slate-700 font-semibold text-sm">Docentes registrados</h2>
              <p className="text-slate-400 text-xs">Consulta de perfiles con rol DOCENTE. Para crear, editar o desactivar, usa el módulo Usuarios.</p>
            </div>
            <div className="flex items-center gap-3">
              <span className="text-xs text-slate-400">{personasDocentes.length} docente(s)</span>
              <button
                onClick={() => navigate("/usuarios")}
                style={{ backgroundColor: PRIMARY }}
                className="text-white text-xs font-medium px-3 py-1.5 rounded-lg hover:opacity-90"
              >
                Gestionar en Usuarios →
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
                    <td className="px-4 py-3 font-mono text-slate-600">{p.cedula || "—"}</td>
                    <td className="px-4 py-3 text-slate-700 font-medium">{p.nombres} {p.apellidos}</td>
                    <td className="px-4 py-3 text-slate-500 text-xs">{p.username}<br /><span className="text-slate-400">{p.correo}</span></td>
                    <td className="px-4 py-3 text-slate-600 text-xs">{p.tituloAcademico || <span className="text-slate-300">—</span>}</td>
                    <td className="px-4 py-3 text-center">
                      <button
                        onClick={() => setDocenteVer(p)}
                        className="text-xs font-semibold px-3 py-1 rounded bg-slate-100 text-slate-600 hover:bg-slate-200"
                      >
                        Ver
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {docenteVer && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg overflow-hidden">
            <div style={{ backgroundColor: PRIMARY }} className="px-6 py-4 flex justify-between items-center text-white">
              <h3 className="font-semibold">Perfil del docente</h3>
              <button onClick={() => setDocenteVer(null)} className="text-white/70 hover:text-white">✕</button>
            </div>
            <div className="p-6 space-y-4 max-h-[75vh] overflow-y-auto">
              <div className="flex items-center gap-4 pb-3 border-b border-slate-100">
                <div className="w-16 h-16 rounded-full bg-slate-100 border border-slate-200 flex items-center justify-center overflow-hidden flex-shrink-0">
                  {docenteVer.fotoUrl
                    ? <img src={docenteVer.fotoUrl.startsWith("http") ? docenteVer.fotoUrl : `${API.replace("/api","")}${docenteVer.fotoUrl}`} alt="" className="w-full h-full object-cover" />
                    : <span className="text-slate-400 text-xl font-bold">{(docenteVer.nombres?.[0] || "?").toUpperCase()}</span>}
                </div>
                <div className="min-w-0">
                  <p className="font-semibold text-slate-700 truncate">{docenteVer.nombres} {docenteVer.apellidos}</p>
                  <p className="text-xs text-slate-500 truncate">{docenteVer.username} · {docenteVer.correo}</p>
                </div>
              </div>
              <Detalle label="Cédula" value={docenteVer.cedula} />
              <Detalle label="Título académico" value={docenteVer.tituloAcademico} />
              <Detalle label="Especialización" value={docenteVer.especializacion} />
              <Detalle label="Teléfono" value={docenteVer.telefono} />
              <Detalle label="Dirección" value={docenteVer.direccion} />
              <Detalle label="Correo personal" value={docenteVer.correoPersonal} />
            </div>
            <div className="p-4 border-t border-slate-100 flex justify-end gap-3">
              <button onClick={() => { setDocenteVer(null); navigate("/usuarios"); }}
                className="px-4 py-2 rounded-lg text-sm font-medium text-white" style={{ backgroundColor: PRIMARY }}>
                Editar en Usuarios
              </button>
              <button onClick={() => setDocenteVer(null)} className="px-5 py-2 rounded-lg text-sm font-medium text-slate-500 hover:bg-slate-100">Cerrar</button>
            </div>
          </div>
        </div>
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
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Grado</label>
                  <select required value={asignEdit.idGrado}
                    onChange={(e) => {
                      const g = e.target.value;
                      setAsignEdit({ ...asignEdit, idGrado: g, idParalelo: "", idAsignatura: "" });
                      if (g) axios.get(`${API}/asignaciones/grado/${g}/paralelos`, { headers: H }).then(r => setParalelos(r.data)).catch(() => setParalelos([]));
                      cargarMateriasMallaEdit(g, asignEdit.idAnoLectivo);
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
                <select required value={asignEdit.idAnoLectivo}
                  onChange={(e) => {
                    const y = e.target.value;
                    setAsignEdit({ ...asignEdit, idAnoLectivo: y, idAsignatura: "" });
                    cargarMateriasMallaEdit(asignEdit.idGrado, y);
                  }}
                  className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50">
                  <option value="">Seleccione...</option>
                  {anosLectivos.map((y) => <option key={y.idAnoLectivo} value={y.idAnoLectivo}>{y.nombre}</option>)}
                </select>
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Asignatura</label>
                <select required value={asignEdit.idAsignatura} onChange={(e) => setAsignEdit({ ...asignEdit, idAsignatura: e.target.value })}
                  disabled={!asignEdit.idGrado || !asignEdit.idAnoLectivo}
                  className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50 disabled:opacity-50">
                  <option value="">
                    {!asignEdit.idGrado || !asignEdit.idAnoLectivo
                      ? "Elija grado y año lectivo"
                      : materiasMallaEdit.length === 0
                        ? "El grado no tiene materias en su malla"
                        : "Seleccione..."}
                  </option>
                  {materiasMallaEdit.map((m) => <option key={m.idAsignatura} value={m.idAsignatura}>{m.asignatura}</option>)}
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
