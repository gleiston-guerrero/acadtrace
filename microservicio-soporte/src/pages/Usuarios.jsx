import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import axios from "axios";
import Layout from "../components/Layout";

const host = typeof window !== "undefined" ? window.location.hostname : "localhost";
const API_PRINCIPAL = `http://${host}:8080/api`;
const IMG_BASE = `http://${host}:8080`;
const PRIMARY = "#243A76";

const ROLES_DISPONIBLES = [
    "DIRECTOR",
    "SECRETARIA",
    "DOCENTE",
    "SOPORTE_TECNICO",
    "ADMINISTRADOR",
];

// Roles reales que acepta el backend al crear (sga-principal: Set<Long> roles).
// Mismos 4 que usa sga-principal/sga-frontend/src/pages/usuarios/Usuarios.jsx
// (no incluye ADMINISTRADOR porque tampoco lo ofrece ahí).
const ROLES_CREAR = [
    { id: 1, nombre: "DIRECTOR" },
    { id: 2, nombre: "SECRETARIA" },
    { id: 3, nombre: "DOCENTE" },
    { id: 4, nombre: "SOPORTE_TECNICO" },
];
const ID_DOCENTE_CREAR = 3;

const FORM_CREAR_INICIAL = {
    cedula: "", nombres: "", apellidos: "", correo: "", roles: [],
    fechaNacimiento: "", genero: "", telefono: "", telefonoAlt: "",
    direccion: "", correoPersonal: "", tituloAcademico: "", especializacion: "", fotoUrl: "",
};

const roleBadge = () => "bg-blue-50 text-blue-600";

// GET /api/usuarios devuelve "estado" (boolean, activo/inactivo del login) y
// "primerIngreso" (boolean, DEFAULT true en la BD = todavia NO ha hecho su
// primer login). No existe ningun campo "activo" ni "nombre"/"apellido" en
// ese DTO (ver UsuarioResponseDTO.java) -- por eso "Pendiente" salia siempre:
// el codigo leia u.activo, que no existe, entonces era undefined -> falsy.
const estadoBadge = (estado) =>
    estado
        ? "bg-green-100 text-green-700"
        : "bg-red-100 text-red-600";

// Sidebar propio de esta página (reemplaza el genérico Panel/Tickets/Reportes/Usuarios)
const USUARIOS_MENU_ITEMS = [
    {
        id: "lista",
        label: "Lista de usuarios",
        icon: (
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
            </svg>
        ),
    },
    {
        id: "nuevo",
        label: "Nuevo usuario",
        icon: (
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
            </svg>
        ),
    },
];

// Sección de datos del docente (igual que sga-principal): sólo aparece si se
// marca el rol DOCENTE, porque el resto del sistema espera ese perfil.
function DocenteCampos({ data, onChange, subirFoto }) {
    const set = (k, v) => onChange({ ...data, [k]: v });
    const foto = data.fotoUrl
        ? (data.fotoUrl.startsWith("http") ? data.fotoUrl : `${IMG_BASE}${data.fotoUrl}`)
        : null;
    return (
        <div className="border-t border-slate-100 pt-4 space-y-4">
            <p className="text-xs font-semibold text-[#243A76] uppercase tracking-wide">
                Datos del docente <span className="text-slate-400 normal-case font-normal">— requeridos por el rol DOCENTE</span>
            </p>

            <div className="flex items-center gap-4">
                <div className="w-16 h-16 rounded-full bg-slate-100 border border-slate-200 flex items-center justify-center overflow-hidden flex-shrink-0">
                    {foto
                        ? <img src={foto} alt="" className="w-full h-full object-cover" />
                        : <svg className="w-7 h-7 text-slate-300" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M15 12a3 3 0 11-6 0 3 3 0 016 0zM4 20a8 8 0 0116 0" /></svg>}
                </div>
                <div>
                    <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Foto de perfil</label>
                    <input type="file" accept="image/jpeg,image/png,image/webp"
                        onChange={(e) => { const f = e.target.files?.[0]; if (f) subirFoto(f, (url) => set("fotoUrl", url)); }}
                        className="text-xs text-slate-500 file:mr-3 file:py-1.5 file:px-3 file:rounded-lg file:border-0 file:bg-slate-100 file:text-slate-700 file:cursor-pointer" />
                    <p className="text-xs text-slate-400 mt-1">JPG, PNG o WEBP · máx 3 MB</p>
                </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                <div>
                    <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Título académico *</label>
                    <input type="text" value={data.tituloAcademico || ""} onChange={(e) => set("tituloAcademico", e.target.value)}
                        placeholder="Lic. en Ciencias de la Educación"
                        className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-slate-50" />
                </div>
                <div>
                    <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Especialización</label>
                    <input type="text" value={data.especializacion || ""} onChange={(e) => set("especializacion", e.target.value)}
                        placeholder="Matemáticas, Lengua, etc."
                        className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-slate-50" />
                </div>
                <div>
                    <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Fecha de nacimiento</label>
                    <input type="date" value={data.fechaNacimiento || ""} onChange={(e) => set("fechaNacimiento", e.target.value)}
                        className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-slate-50" />
                </div>
                <div>
                    <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Género</label>
                    <select value={data.genero || ""} onChange={(e) => set("genero", e.target.value)}
                        className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-slate-50">
                        <option value="">—</option>
                        <option value="MASCULINO">Masculino</option>
                        <option value="FEMENINO">Femenino</option>
                        <option value="OTRO">Otro</option>
                    </select>
                </div>
                <div>
                    <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Teléfono</label>
                    <input type="text" value={data.telefono || ""} onChange={(e) => set("telefono", e.target.value)}
                        className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-slate-50" />
                </div>
                <div>
                    <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Teléfono alt.</label>
                    <input type="text" value={data.telefonoAlt || ""} onChange={(e) => set("telefonoAlt", e.target.value)}
                        className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-slate-50" />
                </div>
                <div className="md:col-span-2">
                    <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Correo personal (adicional)</label>
                    <input type="email" value={data.correoPersonal || ""} onChange={(e) => set("correoPersonal", e.target.value)}
                        className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-slate-50" />
                </div>
                <div className="md:col-span-2">
                    <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Dirección</label>
                    <input type="text" value={data.direccion || ""} onChange={(e) => set("direccion", e.target.value)}
                        className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-slate-50" />
                </div>
            </div>
        </div>
    );
}

const EMPTY_FORM = {
    correo: "",
    roles: [],
};

export default function Usuarios() {
    const navigate = useNavigate();
    const token    = localStorage.getItem("token");
    const roles    = JSON.parse(localStorage.getItem("roles") || "[]");
    const headers  = { Authorization: `Bearer ${token}` };

    const puedeGestionar = roles.includes("SOPORTE_TECNICO") || roles.includes("ADMINISTRADOR") || roles.includes("DIRECTOR");

    const [usuarios,  setUsuarios]  = useState([]);
    const [loading,   setLoading]   = useState(true);
    const [busqueda,  setBusqueda]  = useState("");
    const [filtroRol, setFiltroRol] = useState("TODOS");
    const [error,     setError]     = useState("");
    const [success,   setSuccess]   = useState("");
    const [saving,    setSaving]    = useState(false);

    // Modal editar (el de crear viejo ya no se usa para crear, ver showCrear)
    const [showModal,    setShowModal]    = useState(false);
    const [showDetalle,  setShowDetalle]  = useState(null); // usuario completo, para el modal de "ver"
    const [editando,     setEditando]     = useState(null); // null = crear, objeto = editar
    const [form,         setForm]         = useState(EMPTY_FORM);

    // Modal "Nuevo Usuario" (igual que sga-principal: cédula + nombres/apellidos
    // + correo + roles → usuario y contraseña se generan solos y se envían por correo)
    const [showCrear,  setShowCrear]  = useState(false);
    const [formCrear,  setFormCrear]  = useState(FORM_CREAR_INICIAL);
    const esDocenteCrear = formCrear.roles.includes(ID_DOCENTE_CREAR);

    // Modal reset contraseña
    const [showReset,    setShowReset]    = useState(null); // id del usuario
    const [resetLoading, setResetLoading] = useState(false);
    const [resetMsg,     setResetMsg]     = useState("");

    // Modal asignar roles
    const [showRoles,    setShowRoles]    = useState(null); // usuario completo
    const [rolesForm,    setRolesForm]    = useState([]);
    const [savingRoles,  setSavingRoles]  = useState(false);

    useEffect(() => {
        if (!token) {
            const host = typeof window !== "undefined" ? window.location.hostname : "localhost";
            window.location.href = `http://${host}:5174/login`;
            return;
        }
        if (!puedeGestionar) { navigate("/soporte"); return; }
        cargar();
    }, []);

    useEffect(() => {
        if (success) {
            const t = setTimeout(() => setSuccess(""), 4000);
            return () => clearTimeout(t);
        }
    }, [success]);

    const cargar = () => {
        setLoading(true);
        axios.get(`${API_PRINCIPAL}/usuarios`, { headers })
            .then(r => setUsuarios(r.data))
            .catch(() => setError("Error al cargar usuarios"))
            .finally(() => setLoading(false));
    };

    // ── Filtrado ──────────────────────────────────────────────
    const filtrados = usuarios.filter(u => {
        const matchRol = filtroRol === "TODOS" || (u.roles || []).includes(filtroRol);
        const q = busqueda.toLowerCase();
        const matchQ =
            u.username?.toLowerCase().includes(q) ||
            u.correo?.toLowerCase().includes(q);
        return matchRol && matchQ;
    });

    // ── Editar ────────────────────────────────────────────────
    // OJO: UsuarioUpdateDTO (backend) solo acepta { correo, roles: Set<Long> }.
    // No existen username/nombre/apellido/password/activo en ese endpoint --
    // este modal de "Editar" hoy solo puede cambiar el correo de forma
    // confiable. El campo roles tambien necesitaria mandarse como IDs
    // numericos (ver ROLES_CREAR), no como texto; eso queda pendiente, igual
    // que en "Asignar roles".
    const abrirEditar = (u) => {
        setEditando(u);
        setForm({ correo: u.correo || "" });
        setError("");
        setShowModal(true);
    };

    const handleGuardar = async (e) => {
        e.preventDefault();
        setSaving(true); setError("");
        try {
            await axios.put(`${API_PRINCIPAL}/usuarios/${editando.idUsuario}`, { correo: form.correo }, { headers });
            setSuccess("Correo actualizado correctamente.");
            setShowModal(false);
            cargar();
        } catch (err) {
            setError(err.response?.data?.message || "Error al guardar usuario");
        } finally { setSaving(false); }
    };

    // ── Nuevo usuario (real: cédula + nombres/apellidos + correo + roles,
    // usuario/contraseña autogenerados en el backend, igual que sga-principal) ──
    const abrirCrear = () => {
        setFormCrear(FORM_CREAR_INICIAL);
        setError("");
        setShowCrear(true);
    };

    const toggleRolCrear = (id) => {
        setFormCrear(f => ({
            ...f,
            roles: f.roles.includes(id) ? f.roles.filter(r => r !== id) : [...f.roles, id],
        }));
    };

    const subirFoto = async (file, setter) => {
        const fd = new FormData();
        fd.append("archivo", file);
        try {
            const { data } = await axios.post(`${API_PRINCIPAL}/uploads/foto`, fd, {
                headers: { ...headers, "Content-Type": "multipart/form-data" },
            });
            setter(data.url);
        } catch (err) {
            setError(err.response?.data?.message || "No se pudo subir la imagen.");
        }
    };

    const handleCrear = async (e) => {
        e.preventDefault();
        if (formCrear.roles.length === 0) { setError("Selecciona al menos un rol"); return; }
        if (!/^\d{10}$/.test(formCrear.cedula)) { setError("La cédula debe tener 10 dígitos"); return; }
        if (esDocenteCrear && !formCrear.tituloAcademico.trim()) {
            setError("Como el usuario tiene rol DOCENTE, indica al menos el título académico.");
            return;
        }
        setSaving(true); setError("");
        try {
            const { cedula, nombres, apellidos, correo } = formCrear;
            const resp = await axios.post(`${API_PRINCIPAL}/usuarios`, {
                nombres, apellidos, correo,
                roles: formCrear.roles.map(Number),
            }, { headers });
            const idUsuario = resp.data?.idUsuario;
            if (idUsuario) {
                const personaPayload = { idUsuario, cedula, nombres, apellidos };
                if (esDocenteCrear) {
                    Object.assign(personaPayload, {
                        fechaNacimiento: formCrear.fechaNacimiento || null,
                        genero: formCrear.genero || null,
                        telefono: formCrear.telefono || null,
                        telefonoAlt: formCrear.telefonoAlt || null,
                        direccion: formCrear.direccion || null,
                        correoPersonal: formCrear.correoPersonal || null,
                        tituloAcademico: formCrear.tituloAcademico || null,
                        especializacion: formCrear.especializacion || null,
                        fotoUrl: formCrear.fotoUrl || null,
                    });
                }
                try {
                    await axios.post(`${API_PRINCIPAL}/personas`, personaPayload, { headers });
                } catch (perr) {
                    const msg = perr.response?.data?.message || perr.message || "sin detalle";
                    setError(`Usuario creado, pero NO se guardó el perfil: ${msg}. Complétalo editando el usuario.`);
                    setShowCrear(false);
                    setFormCrear(FORM_CREAR_INICIAL);
                    cargar();
                    setSaving(false);
                    return;
                }
            }
            setSuccess("Usuario creado. Se enviaron las credenciales al correo.");
            setShowCrear(false);
            setFormCrear(FORM_CREAR_INICIAL);
            cargar();
        } catch (err) {
            setError(err.response?.data?.message || "Error al crear usuario");
        } finally {
            setSaving(false);
        }
    };

    // ── Activar / Desactivar ──────────────────────────────────
    // El endpoint real es PATCH /usuarios/{id}/estado?estado=... (query
    // param, no JSON body -- ver UsuarioController.cambiarEstado). Nota: el
    // service de sga-principal hoy SIEMPRE pone estado=true sin mirar el
    // parametro (bug del backend, fuera de microservicio-soporte), asi que
    // "Desactivar" no va a surtir efecto hasta que se corrija alla -- esto
    // ya manda la forma correcta de la petición para cuando se arregle.
    const toggleEstado = async (u) => {
        const nuevoEstado = !u.estado;
        try {
            await axios.patch(
                `${API_PRINCIPAL}/usuarios/${u.idUsuario}/estado`,
                null,
                { headers, params: { estado: nuevoEstado ? "ACTIVO" : "INACTIVO" } }
            );
            setSuccess(`Usuario ${nuevoEstado ? "activado" : "desactivado"}.`);
            cargar();
        } catch {
            setError("Error al cambiar estado del usuario");
        }
    };

    // ── Reset contraseña ──────────────────────────────────────
    const handleReset = async () => {
        setResetLoading(true); setResetMsg("");
        try {
            await axios.patch(
                `${API_PRINCIPAL}/usuarios/${showReset}/reset-password`,
                {},
                { headers }
            );
            setResetMsg("✓ Contraseña reseteada. Se envió al correo del usuario.");
        } catch {
            setResetMsg("Error al resetear la contraseña.");
        } finally { setResetLoading(false); }
    };

    // ── Asignar roles ─────────────────────────────────────────
    // OJO (mismo problema que "Editar"): el backend espera roles como
    // Set<Long> de IDs (ver ROLES_CREAR), pero este modal todavia arma
    // rolesForm con los NOMBRES de texto de ROLES_DISPONIBLES. Falta mapear
    // nombre -> id antes de enviarlo o el PUT va a fallar con 400. Lo dejo
    // señalado, no lo cambié en este arreglo.
    const abrirRoles = (u) => {
        setShowRoles(u);
        setRolesForm(u.roles || []);
        setError("");
    };

    const handleGuardarRoles = async () => {
        setSavingRoles(true); setError("");
        try {
            await axios.put(
                `${API_PRINCIPAL}/usuarios/${showRoles.idUsuario}`,
                { correo: showRoles.correo, roles: rolesForm },
                { headers }
            );
            setSuccess("Roles actualizados correctamente.");
            setShowRoles(null);
            cargar();
        } catch {
            setError("Error al guardar roles");
        } finally { setSavingRoles(false); }
    };

    const toggleRol = (rol) => {
        setRolesForm(prev =>
            prev.includes(rol) ? prev.filter(r => r !== rol) : [...prev, rol]
        );
    };

    const modalBg = { backgroundColor: "rgba(36,58,118,0.5)" };

    return (
        <Layout breadcrumb={["Inicio", "Usuarios"]} sidebarTitle="Usuarios" menuItems={USUARIOS_MENU_ITEMS} seccion="lista"
            onSeccionChange={(id) => { if (id === "nuevo") abrirCrear(); }}>
            <div className="space-y-4">
                {/* ALERTAS */}
                {success && (
                    <div className="bg-green-50 border border-green-200 rounded-xl px-4 py-2.5 text-green-700 text-sm flex items-center gap-2 shadow-sm">
                        <svg className="w-4 h-4 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                        </svg>
                        {success}
                    </div>
                )}
                {error && !showModal && !showCrear && !showReset && !showRoles && !showDetalle && (
                    <div className="bg-red-50 border border-red-200 rounded-xl px-4 py-2.5 text-red-600 text-sm shadow-sm">
                        {error}
                    </div>
                )}


                {/* CABECERA */}
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4">
                    <div>
                        <h1 className="text-lg font-bold text-slate-800">Usuarios</h1>
                        <p className="text-xs text-slate-500">{usuarios.length} usuarios encontrados</p>
                    </div>
                    <div className="flex items-center gap-2">
                        <input
                            type="text"
                            placeholder="Buscar usuario..."
                            value={busqueda}
                            onChange={e => setBusqueda(e.target.value)}
                            className="border border-slate-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-100 bg-white w-56"
                        />
                        <button
                            onClick={abrirCrear}
                            style={{ backgroundColor: PRIMARY }}
                            className="flex items-center gap-2 px-4 py-2 text-sm text-white rounded-lg hover:opacity-90 transition font-medium whitespace-nowrap"
                        >
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                            </svg>
                            Nuevo usuario
                        </button>
                    </div>
                </div>

                {/* FILTRO DE ROL */}
                <div className="flex flex-col sm:flex-row gap-2 mb-4">
                    <select
                        value={filtroRol}
                        onChange={e => setFiltroRol(e.target.value)}
                        className="border border-slate-200 rounded-lg px-3 py-2 text-sm focus:outline-none bg-white"
                    >
                        <option value="TODOS">Todos los roles</option>
                        {ROLES_DISPONIBLES.map(r => (
                            <option key={r} value={r}>{r.replace("_", " ")}</option>
                        ))}
                    </select>
                </div>

                {/* TABLA */}
                {loading ? (
                    <div className="flex items-center justify-center h-40 text-slate-400 text-sm">
                        <svg className="w-5 h-5 mr-2 animate-spin" fill="none" viewBox="0 0 24 24">
                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
                        </svg>
                        Cargando usuarios...
                    </div>
                ) : filtrados.length === 0 ? (
                    <div className="text-center py-16 text-slate-400 text-sm">
                        No se encontraron usuarios con esos criterios.
                    </div>
                ) : (
                    <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
                        <div className="overflow-x-auto">
                            <table className="w-full text-sm">
                                <thead>
                                <tr style={{ backgroundColor: PRIMARY }}>
                                    <th className="px-4 py-3 text-left text-xs font-semibold text-white uppercase tracking-wide">#</th>
                                    <th className="px-4 py-3 text-left text-xs font-semibold text-white uppercase tracking-wide">Usuario</th>
                                    <th className="px-4 py-3 text-left text-xs font-semibold text-white uppercase tracking-wide">Correo</th>
                                    <th className="px-4 py-3 text-left text-xs font-semibold text-white uppercase tracking-wide">Roles</th>
                                    <th className="px-4 py-3 text-left text-xs font-semibold text-white uppercase tracking-wide">Estado</th>
                                    <th className="px-4 py-3 text-left text-xs font-semibold text-white uppercase tracking-wide">Primer ingreso</th>
                                    <th className="px-4 py-3 text-left text-xs font-semibold text-white uppercase tracking-wide">Acciones</th>
                                </tr>
                                </thead>
                                <tbody className="divide-y divide-slate-50">
                                {filtrados.map((u, i) => (
                                    <tr key={u.idUsuario} className="hover:bg-slate-50 transition">
                                        <td className="px-4 py-3 text-slate-400 text-xs">{i + 1}</td>
                                        <td className="px-4 py-3">
                                            <div className="flex items-center gap-2">
                                                <div
                                                    style={{ backgroundColor: PRIMARY }}
                                                    className="w-7 h-7 rounded-full flex items-center justify-center text-white text-xs font-bold uppercase flex-shrink-0"
                                                >
                                                    {u.username?.charAt(0)}
                                                </div>
                                                <p className="font-mono text-xs font-medium text-slate-700">{u.username}</p>
                                            </div>
                                        </td>
                                        <td className="px-4 py-3 text-xs text-slate-500">{u.correo}</td>
                                        <td className="px-4 py-3">
                                            <div className="flex flex-wrap gap-1">
                                                {(u.roles || []).map(r => (
                                                    <span key={r} className={`text-xs px-1.5 py-0.5 rounded-full font-medium ${roleBadge(r)}`}>
                                                        {r.replace("_", " ")}
                                                    </span>
                                                ))}
                                            </div>
                                        </td>
                                        <td className="px-4 py-3">
                                            <span
                                                title={u.estado ? "Activo" : "Inactivo"}
                                                className={`inline-block w-2.5 h-2.5 rounded-full ${u.estado ? "bg-green-500" : "bg-red-400"}`}
                                            />
                                        </td>
                                        <td className="px-4 py-3">
                                            <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${!u.primerIngreso ? "bg-green-100 text-green-700" : "bg-amber-100 text-amber-700"}`}>
                                                {!u.primerIngreso ? "Completo" : "Pendiente"}
                                            </span>
                                        </td>
                                        <td className="px-4 py-3">
                                            <div className="flex items-center gap-1">
                                                {/* Ver detalle */}
                                                <button
                                                    onClick={() => setShowDetalle(u)}
                                                    title="Ver detalle"
                                                    className="p-1.5 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition"
                                                >
                                                    <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                                                    </svg>
                                                </button>
                                                {/* Editar */}
                                                <button
                                                    onClick={() => abrirEditar(u)}
                                                    title="Editar"
                                                    className="p-1.5 rounded-lg text-slate-400 hover:text-blue-600 hover:bg-blue-50 transition"
                                                >
                                                    <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                                                    </svg>
                                                </button>
                                                {/* Roles */}
                                                <button
                                                    onClick={() => abrirRoles(u)}
                                                    title="Asignar roles"
                                                    className="p-1.5 rounded-lg text-slate-400 hover:text-purple-600 hover:bg-purple-50 transition"
                                                >
                                                    <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                                                    </svg>
                                                </button>
                                                {/* Reset password */}
                                                <button
                                                    onClick={() => { setShowReset(u.idUsuario); setResetMsg(""); }}
                                                    title="Resetear contraseña"
                                                    className="p-1.5 rounded-lg text-slate-400 hover:text-orange-600 hover:bg-orange-50 transition"
                                                >
                                                    <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z" />
                                                    </svg>
                                                </button>
                                                {/* Activar/Desactivar */}
                                                <button
                                                    onClick={() => toggleEstado(u)}
                                                    title={u.estado ? "Desactivar" : "Activar"}
                                                    className={`p-1.5 rounded-lg transition ${
                                                        u.estado
                                                            ? "text-slate-400 hover:text-red-600 hover:bg-red-50"
                                                            : "text-slate-400 hover:text-green-600 hover:bg-green-50"
                                                    }`}
                                                >
                                                    {u.estado ? (
                                                        <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" />
                                                        </svg>
                                                    ) : (
                                                        <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                                        </svg>
                                                    )}
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    </div>
                )}


            {/* ── MODAL VER DETALLE ────────────────────────────────── */}
            {showDetalle && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4" style={modalBg}>
                    <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden max-h-[90vh] flex flex-col">
                        <div style={{ backgroundColor: PRIMARY }} className="px-6 py-4 flex items-start justify-between flex-shrink-0">
                            <div>
                                <h3 className="text-white font-bold">Detalle del usuario</h3>
                                <p className="text-white text-opacity-70 text-xs mt-0.5">Información completa (sin credenciales)</p>
                            </div>
                            <button onClick={() => setShowDetalle(null)} className="text-white text-opacity-80 hover:text-opacity-100">
                                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                </svg>
                            </button>
                        </div>

                        <div className="p-6 overflow-y-auto space-y-5">
                            <div className="flex items-center gap-3">
                                <div className="w-12 h-12 rounded-full bg-slate-100 text-slate-500 flex items-center justify-center text-lg font-bold uppercase flex-shrink-0">
                                    {showDetalle.username?.charAt(0)}
                                </div>
                                <div>
                                    <p className="font-bold text-slate-800">{showDetalle.username}</p>
                                    <p className="text-xs text-slate-400 font-mono">{showDetalle.username}</p>
                                </div>
                            </div>

                            <div className="border-t border-slate-100 pt-4 space-y-4">
                                <div>
                                    <p className="text-[10px] font-semibold uppercase tracking-wide text-slate-400">Correo institucional</p>
                                    <p className="text-sm text-slate-700 mt-0.5">{showDetalle.correo}</p>
                                </div>
                                <div>
                                    <p className="text-[10px] font-semibold uppercase tracking-wide text-slate-400">Estado</p>
                                    <span className={`inline-block mt-0.5 text-xs px-2 py-0.5 rounded-full font-medium ${estadoBadge(showDetalle.estado)}`}>
                                        {showDetalle.estado ? "Activo" : "Inactivo"}
                                    </span>
                                </div>
                                <div>
                                    <p className="text-[10px] font-semibold uppercase tracking-wide text-slate-400">Primer ingreso</p>
                                    <p className="text-sm text-slate-700 mt-0.5">{!showDetalle.primerIngreso ? "Completado" : "Pendiente"}</p>
                                </div>
                                <div>
                                    <p className="text-[10px] font-semibold uppercase tracking-wide text-slate-400">Intentos fallidos</p>
                                    <p className="text-sm text-slate-700 mt-0.5">{showDetalle.intentosFallidos ?? "—"}</p>
                                </div>
                                <div>
                                    <p className="text-[10px] font-semibold uppercase tracking-wide text-slate-400">Último acceso</p>
                                    <p className="text-sm text-slate-700 mt-0.5">{showDetalle.ultimoAcceso ? new Date(showDetalle.ultimoAcceso).toLocaleString("es-EC") : "—"}</p>
                                </div>
                                <div>
                                    <p className="text-[10px] font-semibold uppercase tracking-wide text-slate-400">Roles</p>
                                    <p className="text-sm text-slate-700 mt-0.5">{(showDetalle.roles || []).join(", ") || "—"}</p>
                                </div>
                            </div>

                            <div className="border-t border-slate-100 pt-4">
                                <p className="text-[10px] font-semibold uppercase tracking-wide text-slate-400 mb-2">Datos personales</p>
                                <div className="bg-amber-50 border border-amber-200 rounded-lg px-3 py-2.5 text-amber-700 text-xs">
                                    Este usuario no tiene datos personales registrados.
                                </div>
                            </div>
                        </div>

                        <div className="px-6 py-3 border-t border-slate-100 flex justify-end flex-shrink-0">
                            <button
                                onClick={() => setShowDetalle(null)}
                                className="px-4 py-2 text-sm text-slate-500 hover:text-slate-700 font-medium"
                            >
                                Cerrar
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* ── MODAL NUEVO USUARIO (real: cédula + nombres/apellidos + correo +
                 roles; usuario/contraseña se autogeneran y se mandan por correo,
                 igual que sga-principal) ─────────────────────────────────── */}
            {showCrear && (
                <div className="fixed inset-0 z-50 flex items-center justify-center px-4" style={modalBg}>
                    <div className={`bg-white rounded-2xl shadow-2xl w-full overflow-hidden ${esDocenteCrear ? "max-w-2xl" : "max-w-md"}`}>
                        <div style={{ backgroundColor: PRIMARY }} className="px-6 py-4 flex items-center justify-between">
                            <h2 className="text-white font-bold text-base">Nuevo Usuario</h2>
                            <button onClick={() => { setShowCrear(false); setError(""); }} className="text-white text-opacity-70 hover:text-opacity-100">✕</button>
                        </div>
                        <form onSubmit={handleCrear} className="p-6 space-y-4 max-h-[80vh] overflow-y-auto">
                            {error && <div className="bg-red-50 border border-red-200 rounded-lg px-3 py-2 text-red-600 text-xs">{error}</div>}

                            <div>
                                <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase tracking-wide">Cédula</label>
                                <input
                                    type="text"
                                    inputMode="numeric"
                                    maxLength={10}
                                    value={formCrear.cedula}
                                    onChange={e => setFormCrear({ ...formCrear, cedula: e.target.value.replace(/\D/g, "") })}
                                    placeholder="10 dígitos"
                                    required
                                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none bg-slate-50"
                                />
                            </div>

                            <div className="grid grid-cols-2 gap-3">
                                <div>
                                    <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase tracking-wide">Nombres</label>
                                    <input
                                        type="text"
                                        value={formCrear.nombres}
                                        onChange={e => setFormCrear({ ...formCrear, nombres: e.target.value })}
                                        placeholder="Ej: María José"
                                        required
                                        className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none bg-slate-50"
                                    />
                                </div>
                                <div>
                                    <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase tracking-wide">Apellidos</label>
                                    <input
                                        type="text"
                                        value={formCrear.apellidos}
                                        onChange={e => setFormCrear({ ...formCrear, apellidos: e.target.value })}
                                        placeholder="Ej: Rodríguez Pérez"
                                        required
                                        className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none bg-slate-50"
                                    />
                                </div>
                            </div>

                            <div>
                                <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase tracking-wide">Correo electrónico</label>
                                <input
                                    type="email"
                                    value={formCrear.correo}
                                    onChange={e => setFormCrear({ ...formCrear, correo: e.target.value })}
                                    placeholder="correo@ejemplo.com"
                                    required
                                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none bg-slate-50"
                                />
                                <p className="text-xs text-slate-400 mt-1">Las credenciales se enviarán a este correo.</p>
                            </div>

                            <div>
                                <label className="block text-xs font-semibold text-slate-500 mb-2 uppercase tracking-wide">Roles</label>
                                <div className="grid grid-cols-2 gap-2">
                                    {ROLES_CREAR.map(r => (
                                        <label key={r.id} className={`flex items-center gap-2 px-3 py-2 rounded-lg border cursor-pointer transition text-sm ${formCrear.roles.includes(r.id) ? "border-[#243A76] bg-blue-50 text-[#243A76] font-medium" : "border-slate-200 text-slate-600 hover:border-slate-300"}`}>
                                            <input type="checkbox" checked={formCrear.roles.includes(r.id)} onChange={() => toggleRolCrear(r.id)} className="hidden" />
                                            {formCrear.roles.includes(r.id) ? (
                                                <svg className="w-4 h-4 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
                                            ) : (
                                                <span className="w-4 h-4 rounded-full border-2 border-slate-300 flex-shrink-0" />
                                            )}
                                            {r.nombre.replace("_", " ")}
                                        </label>
                                    ))}
                                </div>
                            </div>

                            {esDocenteCrear && <DocenteCampos data={formCrear} onChange={setFormCrear} subirFoto={subirFoto} />}

                            <div className="bg-blue-50 border border-blue-100 rounded-lg px-3 py-2">
                                <p className="text-xs text-blue-600">
                                    <strong>Usuario generado automáticamente</strong> a partir del nombre y apellido.<br />
                                    La contraseña será aleatoria y se enviará al correo indicado.
                                </p>
                            </div>

                            <div className="flex gap-3 pt-2">
                                <button type="button" onClick={() => { setShowCrear(false); setError(""); }} className="flex-1 py-2 border border-slate-200 rounded-lg text-sm text-slate-600 hover:bg-slate-50 transition">
                                    Cancelar
                                </button>
                                <button type="submit" disabled={saving} style={{ backgroundColor: PRIMARY }} className="flex-1 py-2 text-white rounded-lg text-sm font-medium hover:opacity-90 transition disabled:opacity-60">
                                    {saving ? "Creando..." : "Crear usuario"}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* ── MODAL EDITAR (solo correo: ver nota en abrirEditar) ──── */}
            {showModal && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4" style={modalBg}>
                    <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden">
                        <div style={{ backgroundColor: PRIMARY }} className="px-6 py-4 flex items-center justify-between">
                            <h2 className="text-white font-semibold text-sm">Editar usuario</h2>
                            <button onClick={() => setShowModal(false)} className="text-white hover:opacity-70 text-lg">✕</button>
                        </div>
                        <form onSubmit={handleGuardar} className="p-6 space-y-4">
                            {error && (
                                <div className="bg-red-50 border border-red-200 rounded-lg px-3 py-2 text-red-600 text-xs">{error}</div>
                            )}
                            <div>
                                <label className="text-xs font-medium text-slate-600 block mb-1">Usuario</label>
                                <input
                                    type="text"
                                    value={editando?.username || ""}
                                    disabled
                                    className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50 text-slate-400"
                                />
                            </div>
                            <div>
                                <label className="text-xs font-medium text-slate-600 block mb-1">Correo electrónico *</label>
                                <input
                                    type="email"
                                    value={form.correo}
                                    onChange={e => setForm({ ...form, correo: e.target.value })}
                                    className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-100"
                                    required
                                />
                            </div>
                            <p className="text-xs text-slate-400">
                                Para cambiar los roles usa el botón de escudo (Asignar roles). Para resetear la
                                contraseña, usa el botón de llave.
                            </p>
                            <div className="flex justify-end gap-3 pt-2">
                                <button
                                    type="button"
                                    onClick={() => setShowModal(false)}
                                    className="px-4 py-2 text-sm text-slate-500 hover:text-slate-700 border border-slate-200 rounded-lg transition"
                                >
                                    Cancelar
                                </button>
                                <button
                                    type="submit"
                                    disabled={saving}
                                    style={{ backgroundColor: PRIMARY }}
                                    className="px-5 py-2 text-sm text-white rounded-lg hover:opacity-90 transition font-medium disabled:opacity-50"
                                >
                                    {saving ? "Guardando..." : "Actualizar"}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* ── MODAL RESET CONTRASEÑA ───────────────────────────── */}
            {showReset && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4" style={modalBg}>
                    <div className="bg-white rounded-2xl shadow-2xl w-full max-w-sm overflow-hidden">
                        <div style={{ backgroundColor: "#b45309" }} className="px-6 py-4 flex items-center justify-between">
                            <h2 className="text-white font-semibold text-sm">Resetear contraseña</h2>
                            <button onClick={() => setShowReset(null)} className="text-white hover:opacity-70 text-lg">✕</button>
                        </div>
                        <div className="p-6 space-y-4">
                            <p className="text-sm text-slate-600">
                                Se generará una contraseña temporal y se enviará al correo registrado del usuario.
                                El usuario deberá cambiarla en su próximo inicio de sesión.
                            </p>
                            {resetMsg && (
                                <div className={`rounded-lg px-3 py-2 text-xs ${
                                    resetMsg.startsWith("✓")
                                        ? "bg-green-50 border border-green-200 text-green-700"
                                        : "bg-red-50 border border-red-200 text-red-600"
                                }`}>
                                    {resetMsg}
                                </div>
                            )}
                            <div className="flex justify-end gap-3">
                                <button
                                    onClick={() => setShowReset(null)}
                                    className="px-4 py-2 text-sm text-slate-500 hover:text-slate-700 border border-slate-200 rounded-lg transition"
                                >
                                    Cancelar
                                </button>
                                <button
                                    onClick={handleReset}
                                    disabled={resetLoading || !!resetMsg.startsWith("✓")}
                                    className="px-5 py-2 text-sm text-white rounded-lg hover:opacity-90 transition font-medium disabled:opacity-50"
                                    style={{ backgroundColor: "#b45309" }}
                                >
                                    {resetLoading ? "Enviando..." : "Confirmar reset"}
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* ── MODAL ASIGNAR ROLES ──────────────────────────────── */}
            {showRoles && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4" style={modalBg}>
                    <div className="bg-white rounded-2xl shadow-2xl w-full max-w-sm overflow-hidden">
                        <div style={{ backgroundColor: "#6d28d9" }} className="px-6 py-4 flex items-center justify-between">
                            <div>
                                <h2 className="text-white font-semibold text-sm">Asignar roles</h2>
                                <p className="text-white text-opacity-70 text-xs">{showRoles.username}</p>
                            </div>
                            <button onClick={() => setShowRoles(null)} className="text-white hover:opacity-70 text-lg">✕</button>
                        </div>
                        <div className="p-6 space-y-4">
                            <p className="text-xs text-slate-500">Selecciona los roles que tendrá este usuario en el sistema.</p>
                            {error && (
                                <div className="bg-red-50 border border-red-200 rounded-lg px-3 py-2 text-red-600 text-xs">{error}</div>
                            )}
                            <div className="space-y-2">
                                {ROLES_DISPONIBLES.map(r => (
                                    <label key={r} className="flex items-center gap-3 p-2.5 rounded-lg border border-slate-100 hover:bg-slate-50 cursor-pointer transition">
                                        <input
                                            type="checkbox"
                                            checked={rolesForm.includes(r)}
                                            onChange={() => toggleRol(r)}
                                            className="rounded"
                                        />
                                        <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${roleBadge(r)}`}>
                                            {r.replace("_", " ")}
                                        </span>
                                    </label>
                                ))}
                            </div>
                            <div className="flex justify-end gap-3 pt-2">
                                <button
                                    onClick={() => setShowRoles(null)}
                                    className="px-4 py-2 text-sm text-slate-500 hover:text-slate-700 border border-slate-200 rounded-lg transition"
                                >
                                    Cancelar
                                </button>
                                <button
                                    onClick={handleGuardarRoles}
                                    disabled={savingRoles}
                                    className="px-5 py-2 text-sm text-white rounded-lg hover:opacity-90 transition font-medium disabled:opacity-50"
                                    style={{ backgroundColor: "#6d28d9" }}
                                >
                                    {savingRoles ? "Guardando..." : "Guardar roles"}
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
            </div>
        </Layout>
    );
}