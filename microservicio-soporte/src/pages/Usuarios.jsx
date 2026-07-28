import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import axios from "axios";

const API_PRINCIPAL = "http://localhost:8080/api";
const PRIMARY = "#243A76";
const PRIMARY_LIGHT = "#2d4a96";

const ROLES_DISPONIBLES = [
    "DIRECTOR",
    "SECRETARIA",
    "DOCENTE",
    "SOPORTE_TECNICO",
    "ADMINISTRADOR",
];

const roleBadge = (rol) => {
    const map = {
        DIRECTOR:       "bg-purple-100 text-purple-700",
        SECRETARIA:     "bg-pink-100 text-pink-700",
        DOCENTE:        "bg-blue-100 text-blue-700",
        SOPORTE_TECNICO:"bg-cyan-100 text-cyan-700",
        ADMINISTRADOR:  "bg-orange-100 text-orange-700",
    };
    return map[rol] || "bg-slate-100 text-slate-500";
};

const estadoBadge = (activo) =>
    activo
        ? "bg-green-100 text-green-700"
        : "bg-red-100 text-red-600";

const EMPTY_FORM = {
    username: "",
    nombre: "",
    apellido: "",
    correo: "",
    password: "",
    roles: [],
    activo: true,
};

export default function Usuarios() {
    const navigate = useNavigate();
    const token    = localStorage.getItem("token");
    const roles    = JSON.parse(localStorage.getItem("roles") || "[]");
    const username = localStorage.getItem("username") || "";
    const headers  = { Authorization: `Bearer ${token}` };

    const puedeGestionar = roles.includes("SOPORTE_TECNICO") || roles.includes("ADMINISTRADOR") || roles.includes("DIRECTOR");

    const [usuarios,  setUsuarios]  = useState([]);
    const [loading,   setLoading]   = useState(true);
    const [busqueda,  setBusqueda]  = useState("");
    const [filtroRol, setFiltroRol] = useState("TODOS");
    const [error,     setError]     = useState("");
    const [success,   setSuccess]   = useState("");
    const [saving,    setSaving]    = useState(false);

    // Modal crear/editar
    const [showModal,    setShowModal]    = useState(false);
    const [editando,     setEditando]     = useState(null); // null = crear, objeto = editar
    const [form,         setForm]         = useState(EMPTY_FORM);

    // Modal reset contraseña
    const [showReset,    setShowReset]    = useState(null); // id del usuario
    const [resetLoading, setResetLoading] = useState(false);
    const [resetMsg,     setResetMsg]     = useState("");

    // Modal asignar roles
    const [showRoles,    setShowRoles]    = useState(null); // usuario completo
    const [rolesForm,    setRolesForm]    = useState([]);
    const [savingRoles,  setSavingRoles]  = useState(false);

    useEffect(() => {
        if (!token) { window.location.href = "http://localhost:5173/login"; return; }
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
            u.nombre?.toLowerCase().includes(q)   ||
            u.apellido?.toLowerCase().includes(q) ||
            u.correo?.toLowerCase().includes(q);
        return matchRol && matchQ;
    });

    // ── Crear / Editar ────────────────────────────────────────
    const abrirCrear = () => {
        setEditando(null);
        setForm(EMPTY_FORM);
        setError("");
        setShowModal(true);
    };

    const abrirEditar = (u) => {
        setEditando(u);
        setForm({
            username:  u.username  || "",
            nombre:    u.nombre    || "",
            apellido:  u.apellido  || "",
            correo:    u.correo    || "",
            password:  "",
            roles:     u.roles     || [],
            activo:    u.activo    ?? true,
        });
        setError("");
        setShowModal(true);
    };

    const handleGuardar = async (e) => {
        e.preventDefault();
        setSaving(true); setError("");
        try {
            const payload = { ...form };
            if (editando && !payload.password) delete payload.password;

            if (editando) {
                await axios.put(`${API_PRINCIPAL}/usuarios/${editando.id}`, payload, { headers });
                setSuccess("Usuario actualizado correctamente.");
            } else {
                await axios.post(`${API_PRINCIPAL}/usuarios`, payload, { headers });
                setSuccess("Usuario creado correctamente.");
            }
            setShowModal(false);
            cargar();
        } catch (err) {
            setError(err.response?.data?.message || "Error al guardar usuario");
        } finally { setSaving(false); }
    };

    // ── Activar / Desactivar ──────────────────────────────────
    const toggleEstado = async (u) => {
        const nuevoEstado = !u.activo;
        try {
            await axios.patch(
                `${API_PRINCIPAL}/usuarios/${u.id}/estado`,
                { activo: nuevoEstado },
                { headers }
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
            await axios.post(
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
    const abrirRoles = (u) => {
        setShowRoles(u);
        setRolesForm(u.roles || []);
        setError("");
    };

    const handleGuardarRoles = async () => {
        setSavingRoles(true); setError("");
        try {
            await axios.put(
                `${API_PRINCIPAL}/usuarios/${showRoles.id}`,
                { ...showRoles, roles: rolesForm },
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
        <div className="flex flex-col min-h-screen bg-slate-50">

            {/* TOPBAR */}
            <header style={{ backgroundColor: PRIMARY }} className="h-14 flex items-center justify-between px-4 shadow z-30 flex-shrink-0">
                <div className="flex items-center gap-3">
                    <div className="w-8 h-8 bg-white bg-opacity-20 rounded-full flex items-center justify-center">
                        <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
                        </svg>
                    </div>
                    <span className="text-white font-bold text-sm">SGA</span>
                    <span className="text-white text-opacity-60 text-sm hidden sm:inline">| Gestión de Usuarios</span>
                </div>
                <div className="flex items-center gap-2">
                    <button
                        onClick={() => navigate("/soporte")}
                        className="text-white text-opacity-70 hover:text-opacity-100 text-xs px-3 py-1.5 border border-white border-opacity-20 rounded-lg transition"
                    >
                        ← Soporte
                    </button>
                    <div style={{ backgroundColor: PRIMARY_LIGHT }} className="flex items-center gap-2 px-3 py-1.5 rounded-lg">
                        <div className="w-7 h-7 bg-white bg-opacity-20 rounded-full flex items-center justify-center text-xs font-bold text-white uppercase">
                            {username.charAt(0)}
                        </div>
                        <span className="text-white text-xs font-medium hidden sm:inline capitalize">{username}</span>
                    </div>
                </div>
            </header>

            {/* ALERTAS */}
            {success && (
                <div className="mx-4 mt-3 bg-green-50 border border-green-200 rounded-lg px-4 py-2.5 text-green-700 text-sm flex items-center gap-2">
                    <svg className="w-4 h-4 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                    </svg>
                    {success}
                </div>
            )}
            {error && !showModal && !showReset && !showRoles && (
                <div className="mx-4 mt-3 bg-red-50 border border-red-200 rounded-lg px-4 py-2.5 text-red-600 text-sm">
                    {error}
                </div>
            )}

            {/* MAIN */}
            <main className="flex-1 p-4 max-w-7xl mx-auto w-full">

                {/* CABECERA */}
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4">
                    <div>
                        <h1 className="text-lg font-bold text-slate-800">Usuarios del Sistema</h1>
                        <p className="text-xs text-slate-500">{usuarios.length} usuarios registrados</p>
                    </div>
                    <button
                        onClick={abrirCrear}
                        style={{ backgroundColor: PRIMARY }}
                        className="flex items-center gap-2 px-4 py-2 text-sm text-white rounded-lg hover:opacity-90 transition font-medium"
                    >
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                        </svg>
                        Nuevo usuario
                    </button>
                </div>

                {/* FILTROS */}
                <div className="flex flex-col sm:flex-row gap-2 mb-4">
                    <input
                        type="text"
                        placeholder="Buscar por nombre, usuario o correo..."
                        value={busqueda}
                        onChange={e => setBusqueda(e.target.value)}
                        className="flex-1 border border-slate-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-100 bg-white"
                    />
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
                                <tr className="border-b border-slate-100 bg-slate-50">
                                    <th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide">#</th>
                                    <th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide">Usuario</th>
                                    <th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide">Nombre</th>
                                    <th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide">Correo</th>
                                    <th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide">Roles</th>
                                    <th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide">Estado</th>
                                    <th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide">Acciones</th>
                                </tr>
                                </thead>
                                <tbody className="divide-y divide-slate-50">
                                {filtrados.map((u, i) => (
                                    <tr key={u.id} className="hover:bg-slate-50 transition">
                                        <td className="px-4 py-3 text-slate-400 text-xs">{i + 1}</td>
                                        <td className="px-4 py-3">
                                            <div className="flex items-center gap-2">
                                                <div
                                                    style={{ backgroundColor: PRIMARY }}
                                                    className="w-7 h-7 rounded-full flex items-center justify-center text-white text-xs font-bold uppercase flex-shrink-0"
                                                >
                                                    {u.username?.charAt(0)}
                                                </div>
                                                <span className="font-mono text-xs font-medium text-slate-700">{u.username}</span>
                                            </div>
                                        </td>
                                        <td className="px-4 py-3 text-xs text-slate-700">
                                            {u.nombre} {u.apellido}
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
                                            <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${estadoBadge(u.activo)}`}>
                                                {u.activo ? "Activo" : "Inactivo"}
                                            </span>
                                        </td>
                                        <td className="px-4 py-3">
                                            <div className="flex items-center gap-1">
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
                                                    onClick={() => { setShowReset(u.id); setResetMsg(""); }}
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
                                                    title={u.activo ? "Desactivar" : "Activar"}
                                                    className={`p-1.5 rounded-lg transition ${
                                                        u.activo
                                                            ? "text-slate-400 hover:text-red-600 hover:bg-red-50"
                                                            : "text-slate-400 hover:text-green-600 hover:bg-green-50"
                                                    }`}
                                                >
                                                    {u.activo ? (
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
            </main>

            {/* FOOTER */}
            <footer style={{ backgroundColor: PRIMARY }} className="text-white text-opacity-80 text-xs text-center py-2 flex-shrink-0">
                Sistema de Gestión Académica · Escuela Provincias Unidas © 2026
            </footer>

            {/* ── MODAL CREAR / EDITAR ─────────────────────────────── */}
            {showModal && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4" style={modalBg}>
                    <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden">
                        <div style={{ backgroundColor: PRIMARY }} className="px-6 py-4 flex items-center justify-between">
                            <h2 className="text-white font-semibold text-sm">
                                {editando ? "Editar usuario" : "Nuevo usuario"}
                            </h2>
                            <button onClick={() => setShowModal(false)} className="text-white hover:opacity-70 text-lg">✕</button>
                        </div>
                        <form onSubmit={handleGuardar} className="p-6 space-y-4">
                            {error && (
                                <div className="bg-red-50 border border-red-200 rounded-lg px-3 py-2 text-red-600 text-xs">{error}</div>
                            )}
                            <div className="grid grid-cols-2 gap-3">
                                <div>
                                    <label className="text-xs font-medium text-slate-600 block mb-1">Nombre *</label>
                                    <input
                                        type="text"
                                        value={form.nombre}
                                        onChange={e => setForm({ ...form, nombre: e.target.value })}
                                        className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-100"
                                        required
                                    />
                                </div>
                                <div>
                                    <label className="text-xs font-medium text-slate-600 block mb-1">Apellido *</label>
                                    <input
                                        type="text"
                                        value={form.apellido}
                                        onChange={e => setForm({ ...form, apellido: e.target.value })}
                                        className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-100"
                                        required
                                    />
                                </div>
                            </div>
                            <div>
                                <label className="text-xs font-medium text-slate-600 block mb-1">Usuario *</label>
                                <input
                                    type="text"
                                    value={form.username}
                                    onChange={e => setForm({ ...form, username: e.target.value })}
                                    disabled={!!editando}
                                    className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-100 disabled:bg-slate-50 disabled:text-slate-400"
                                    required
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
                            <div>
                                <label className="text-xs font-medium text-slate-600 block mb-1">
                                    {editando ? "Nueva contraseña (dejar en blanco para no cambiar)" : "Contraseña *"}
                                </label>
                                <input
                                    type="password"
                                    value={form.password}
                                    onChange={e => setForm({ ...form, password: e.target.value })}
                                    className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-100"
                                    required={!editando}
                                    minLength={8}
                                />
                            </div>
                            <div>
                                <label className="text-xs font-medium text-slate-600 block mb-2">Roles</label>
                                <div className="grid grid-cols-2 gap-2">
                                    {ROLES_DISPONIBLES.map(r => (
                                        <label key={r} className="flex items-center gap-2 text-xs text-slate-600 cursor-pointer">
                                            <input
                                                type="checkbox"
                                                checked={form.roles.includes(r)}
                                                onChange={() => {
                                                    const next = form.roles.includes(r)
                                                        ? form.roles.filter(x => x !== r)
                                                        : [...form.roles, r];
                                                    setForm({ ...form, roles: next });
                                                }}
                                                className="rounded"
                                            />
                                            <span className={`px-1.5 py-0.5 rounded-full font-medium ${roleBadge(r)}`}>
                                                {r.replace("_", " ")}
                                            </span>
                                        </label>
                                    ))}
                                </div>
                            </div>
                            <div className="flex items-center gap-2">
                                <input
                                    type="checkbox"
                                    id="activo"
                                    checked={form.activo}
                                    onChange={e => setForm({ ...form, activo: e.target.checked })}
                                    className="rounded"
                                />
                                <label htmlFor="activo" className="text-xs text-slate-600 cursor-pointer">Usuario activo</label>
                            </div>
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
                                    {saving ? "Guardando..." : (editando ? "Actualizar" : "Crear usuario")}
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
    );
}