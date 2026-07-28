import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import axios from "axios";

const API = "/api/soporte";
const PRIMARY = "#243A76";
const PRIMARY_LIGHT = "#2d4a96";

const prioridadBadge = (p) => {
    const map = {
        BAJO:    "bg-slate-100 text-slate-500",
        MEDIO:   "bg-blue-100 text-blue-600",
        ALTO:    "bg-orange-100 text-orange-600",
        CRITICO: "bg-red-100 text-red-600",
    };
    return map[p] || "bg-slate-100 text-slate-500";
};

const estadoBadge = (e) => {
    const map = {
        ABIERTO:    "bg-yellow-100 text-yellow-700",
        EN_PROCESO: "bg-blue-100 text-blue-700",
        RESUELTO:   "bg-green-100 text-green-700",
        CERRADO:    "bg-slate-100 text-slate-500",
    };
    return map[e] || "bg-slate-100 text-slate-500";
};

const categoriaBadge = (c) => {
    const map = {
        HARDWARE: "bg-purple-100 text-purple-600",
        SOFTWARE: "bg-indigo-100 text-indigo-600",
        RED:      "bg-cyan-100 text-cyan-600",
        CUENTA:   "bg-pink-100 text-pink-600",
        OTRO:     "bg-slate-100 text-slate-500",
    };
    return map[c] || "bg-slate-100 text-slate-500";
};

const formatFecha = (iso) => {
    if (!iso) return "M-bM-^@M-^T";
    return new Date(iso).toLocaleDateString("es-EC", {
        day: "2-digit", month: "short", year: "numeric",
    });
};

export default function Soporte() {
    const [tickets, setTickets]           = useState([]);
    const [estadisticas, setEstadisticas] = useState(null);
    const [loading, setLoading]           = useState(true);
    const [busqueda, setBusqueda]         = useState("");
    const [filtroEstado, setFiltroEstado] = useState("TODOS");
    const [showModal, setShowModal]       = useState(false);
    const [showDetalle, setShowDetalle]   = useState(null);
    const [saving, setSaving]             = useState(false);
    const [error, setError]               = useState("");
    const [success, setSuccess]           = useState("");
    const [comentarios, setComentarios]   = useState([]);
    const [nuevoComentario, setNuevoComentario] = useState("");
    const [notaInterna, setNotaInterna]   = useState(false);
    const [enviandoCom, setEnviandoCom]   = useState(false);

    const [form, setForm] = useState({
        titulo: "", descripcion: "", prioridad: "MEDIO", categoria: "OTRO",
    });
    const [formUpdate, setFormUpdate] = useState({
        estado: "", asignadoA: "", solucionAplicada: "",
    });

    const navigate  = useNavigate();
    const token     = localStorage.getItem("token");
    const username  = localStorage.getItem("username") || "";
    const roles     = JSON.parse(localStorage.getItem("roles") || "[]");
    const esTecnico = roles.includes("SOPORTE_TECNICO") || roles.includes("ADMINISTRADOR");
    const headers   = { Authorization: `Bearer ${token}` };

    const cargar = () => {
        setLoading(true);
        const endpoint = esTecnico ? `${API}/tickets` : `${API}/tickets/mis-tickets`;
        axios.get(endpoint, { headers })
            .then(r => setTickets(r.data))
            .catch(() => setError("Error al cargar tickets"))
            .finally(() => setLoading(false));
    };

    const cargarEstadisticas = () => {
        if (!esTecnico) return;
        axios.get(`${API}/tickets/estadisticas`, { headers })
            .then(r => setEstadisticas(r.data))
            .catch(() => {});
    };

    useEffect(() => {
        if (!token) { window.location.href = "http://localhost:5173/login"; return; }
        cargar();
        cargarEstadisticas();
    }, []);

    useEffect(() => {
        if (success) {
            const t = setTimeout(() => setSuccess(""), 4000);
            return () => clearTimeout(t);
        }
    }, [success]);

    const filtrados = tickets.filter(t => {
        const matchEstado = filtroEstado === "TODOS" || t.estado === filtroEstado;
        const matchBusq =
            t.titulo?.toLowerCase().includes(busqueda.toLowerCase()) ||
            t.numeroTicket?.toLowerCase().includes(busqueda.toLowerCase()) ||
            t.creadoPor?.toLowerCase().includes(busqueda.toLowerCase());
        return matchEstado && matchBusq;
    });

    const handleCrear = async (e) => {
        e.preventDefault();
        setSaving(true); setError("");
        try {
            await axios.post(`${API}/tickets`, form, { headers });
            setSuccess("Ticket creado correctamente.");
            setShowModal(false);
            setForm({ titulo: "", descripcion: "", prioridad: "MEDIO", categoria: "OTRO" });
            cargar(); cargarEstadisticas();
        } catch (err) {
            setError(err.response?.data?.message || "Error al crear ticket");
        } finally { setSaving(false); }
    };

    const handleActualizar = async (e) => {
        e.preventDefault();
        setSaving(true); setError("");
        try {
            await axios.put(`${API}/tickets/${showDetalle.idTicket}`, formUpdate, { headers });
            setSuccess("Ticket actualizado correctamente.");
            cargar(); cargarEstadisticas();
            const r = await axios.get(`${API}/tickets/${showDetalle.idTicket}`, { headers });
            setShowDetalle(r.data);
        } catch (err) {
            setError(err.response?.data?.message || "Error al actualizar");
        } finally { setSaving(false); }
    };

    const abrirDetalle = async (ticket) => {
        setShowDetalle(ticket);
        setFormUpdate({
            estado: ticket.estado,
            asignadoA: ticket.asignadoA || "",
            solucionAplicada: ticket.solucionAplicada || "",
        });
        try {
            const r = await axios.get(`${API}/tickets/${ticket.idTicket}/comentarios`, { headers });
            setComentarios(r.data);
        } catch { setComentarios([]); }
    };

    const enviarComentario = async () => {
        if (!nuevoComentario.trim()) return;
        setEnviandoCom(true);
        try {
            await axios.post(
                `${API}/tickets/${showDetalle.idTicket}/comentarios`,
                { contenido: nuevoComentario, notaInterna },
                { headers }
            );
            setNuevoComentario(""); setNotaInterna(false);
            const r = await axios.get(`${API}/tickets/${showDetalle.idTicket}/comentarios`, { headers });
            setComentarios(r.data);
        } catch { setError("Error al enviar comentario"); }
        finally { setEnviandoCom(false); }
    };

    const handleLogout = () => {
        localStorage.clear();
        window.location.href = "http://localhost:5173/login";
    };

    const modalBg = { backgroundColor: "rgba(36,58,118,0.5)" };

    return (
        <div className="flex flex-col min-h-screen bg-slate-50">

            {/* TOPBAR */}
            <header style={{ backgroundColor: PRIMARY }} className="h-14 flex items-center justify-between px-4 shadow z-30 flex-shrink-0">
                <div className="flex items-center gap-3">
                    <div className="w-8 h-8 bg-white bg-opacity-20 rounded-full flex items-center justify-center">
                        <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M18.364 5.636l-3.536 3.536m0 5.656l3.536 3.536M9.172 9.172L5.636 5.636m3.536 9.192l-3.536 3.536M21 12a9 9 0 11-18 0 9 9 0 0118 0zm-5 0a4 4 0 11-8 0 4 4 0 018 0z" />
                        </svg>
                    </div>
                    <span className="text-white font-bold text-sm">SGA</span>
                    <span className="text-white text-opacity-60 text-sm hidden sm:inline">| Soporte TM-CM-)cnico</span>
                </div>
                <div className="flex items-center gap-2">
                    <div style={{ backgroundColor: PRIMARY_LIGHT }} className="flex items-center gap-2 px-3 py-1.5 rounded-lg">
                        <div className="w-7 h-7 bg-white bg-opacity-20 rounded-full flex items-center justify-center text-xs font-bold text-white uppercase">
                            {username.charAt(0)}
                        </div>
                        <span className="text-white text-xs font-medium hidden sm:inline capitalize">{username}</span>
                    </div>
                    {esTecnico && (
                        <>
                            <button
                                onClick={() => navigate("/usuarios")}
                                className="text-white text-opacity-70 hover:text-opacity-100 text-xs px-3 py-1.5 border border-white border-opacity-20 rounded-lg transition hidden sm:flex items-center gap-1.5"
                            >
                                <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
                                </svg>
                                Usuarios
                            </button>
                            <button
                                onClick={() => navigate("/dashboard")}
                                className="text-white text-opacity-70 hover:text-opacity-100 text-xs px-3 py-1.5 border border-white border-opacity-20 rounded-lg transition hidden sm:flex items-center gap-1.5"
                            >
                                <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                                </svg>
                                Monitor
                            </button>
                        </>
                    )}
                    <button
                        onClick={handleLogout}
                        className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs text-white hover:bg-white hover:bg-opacity-10 transition"
                    >
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                        </svg>
                        Salir
                    </button>
                </div>
            </header>

            {/* BREADCRUMB */}
            <div className="bg-white border-b border-slate-200 px-6 py-2">
                <nav className="text-xs text-slate-500 flex items-center gap-1">
                    <span className="hover:underline cursor-pointer">Inicio</span>
                    <span className="text-slate-300">/</span>
                    <span style={{ color: PRIMARY }} className="font-medium">Soporte TM-CM-)cnico</span>
                </nav>
            </div>

            {/* CONTENIDO */}
            <main className="flex-1 p-6">

                {/* Alertas */}
                {error && (
                    <div className="mb-4 bg-red-50 border border-red-200 rounded-lg px-4 py-3 flex items-center justify-between">
                        <span className="text-red-600 text-sm">{error}</span>
                        <button onClick={() => setError("")} className="text-red-400 hover:text-red-600">M-bM-^\M-^U</button>
                    </div>
                )}
                {success && (
                    <div className="mb-4 bg-green-50 border border-green-200 rounded-lg px-4 py-3 flex items-center justify-between">
                        <span className="text-green-600 text-sm">{success}</span>
                        <button onClick={() => setSuccess("")} className="text-green-400 hover:text-green-600">M-bM-^\M-^U</button>
                    </div>
                )}

                {/* EstadM-CM--sticas */}
                {esTecnico && estadisticas && (
                    <div className="grid grid-cols-2 sm:grid-cols-5 gap-3 mb-5">
                        {[
                            { label: "Total",      value: estadisticas.total,     color: "bg-slate-50 border-slate-200",   text: "text-slate-700" },
                            { label: "Abiertos",   value: estadisticas.abiertos,  color: "bg-yellow-50 border-yellow-200", text: "text-yellow-700" },
                            { label: "En proceso", value: estadisticas.enProceso, color: "bg-blue-50 border-blue-200",     text: "text-blue-700" },
                            { label: "Resueltos",  value: estadisticas.resueltos, color: "bg-green-50 border-green-200",   text: "text-green-700" },
                            { label: "Cerrados",   value: estadisticas.cerrados,  color: "bg-slate-50 border-slate-200",   text: "text-slate-500" },
                        ].map((s, i) => (
                            <div key={i} className={`rounded-xl border p-3 text-center ${s.color}`}>
                                <p className={`text-2xl font-bold ${s.text}`}>{s.value}</p>
                                <p className="text-xs text-slate-500 mt-0.5">{s.label}</p>
                            </div>
                        ))}
                    </div>
                )}

                {/* Header */}
                <div className="flex items-center justify-between mb-4">
                    <div>
                        <h1 className="text-lg font-bold text-slate-700">Tickets de Soporte</h1>
                        <p className="text-xs text-slate-400">
                            {filtrados.length} ticket{filtrados.length !== 1 ? "s" : ""} encontrado{filtrados.length !== 1 ? "s" : ""}
                        </p>
                    </div>
                    <div className="flex items-center gap-2 flex-wrap justify-end">
                        <select
                            value={filtroEstado}
                            onChange={e => setFiltroEstado(e.target.value)}
                            className="text-xs border border-slate-200 rounded-lg px-2 py-1.5 bg-slate-50 focus:outline-none"
                        >
                            <option value="TODOS">Todos los estados</option>
                            <option value="ABIERTO">Abierto</option>
                            <option value="EN_PROCESO">En proceso</option>
                            <option value="RESUELTO">Resuelto</option>
                            <option value="CERRADO">Cerrado</option>
                        </select>
                        <div className="relative">
                            <input
                                type="text"
                                placeholder="Buscar ticket..."
                                value={busqueda}
                                onChange={e => setBusqueda(e.target.value)}
                                className="pl-3 pr-8 py-1.5 text-xs border border-slate-200 rounded-lg focus:outline-none bg-slate-50 w-44"
                            />
                            <svg className="w-4 h-4 text-slate-400 absolute right-2 top-1/2 -translate-y-1/2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                            </svg>
                        </div>
                        <button
                            onClick={() => { setShowModal(true); setError(""); }}
                            style={{ backgroundColor: PRIMARY }}
                            className="flex items-center gap-2 text-white px-4 py-2 rounded-lg text-sm font-medium hover:opacity-90 transition shadow-sm"
                        >
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                            </svg>
                            Nuevo ticket
                        </button>
                    </div>
                </div>

                {/* Tabla */}
                <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
                    {loading ? (
                        <div className="p-12 text-center text-slate-400 text-sm">Cargando tickets...</div>
                    ) : filtrados.length === 0 ? (
                        <div className="p-12 text-center text-slate-400 text-sm">No se encontraron tickets</div>
                    ) : (
                        <div className="overflow-x-auto">
                            <table className="w-full text-sm">
                                <thead>
                                <tr style={{ backgroundColor: PRIMARY }} className="text-white text-xs">
                                    <th className="text-left px-4 py-3 font-semibold">#</th>
                                    <th className="text-left px-4 py-3 font-semibold">NM-BM-0 Ticket</th>
                                    <th className="text-left px-4 py-3 font-semibold">TM-CM--tulo</th>
                                    <th className="text-left px-4 py-3 font-semibold">CategorM-CM--a</th>
                                    <th className="text-left px-4 py-3 font-semibold">Prioridad</th>
                                    <th className="text-left px-4 py-3 font-semibold">Estado</th>
                                    {esTecnico && <th className="text-left px-4 py-3 font-semibold">Creado por</th>}
                                    <th className="text-left px-4 py-3 font-semibold">Asignado a</th>
                                    <th className="text-left px-4 py-3 font-semibold">Fecha</th>
                                    <th className="text-left px-4 py-3 font-semibold">Acciones</th>
                                </tr>
                                </thead>
                                <tbody className="divide-y divide-slate-100">
                                {filtrados.map((t, i) => (
                                    <tr key={t.idTicket} className="hover:bg-slate-50 transition">
                                        <td className="px-4 py-3 text-slate-400 text-xs">{i + 1}</td>
                                        <td className="px-4 py-3">
                        <span className="font-mono text-xs font-semibold" style={{ color: PRIMARY }}>
                          {t.numeroTicket}
                        </span>
                                        </td>
                                        <td className="px-4 py-3 max-w-xs">
                                            <p className="truncate text-xs font-medium text-slate-700">{t.titulo}</p>
                                        </td>
                                        <td className="px-4 py-3">
                        <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${categoriaBadge(t.categoria)}`}>
                          {t.categoria}
                        </span>
                                        </td>
                                        <td className="px-4 py-3">
                        <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${prioridadBadge(t.prioridad)}`}>
                          {t.prioridad}
                        </span>
                                        </td>
                                        <td className="px-4 py-3">
                        <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${estadoBadge(t.estado)}`}>
                          {t.estado.replace("_", " ")}
                        </span>
                                        </td>
                                        {esTecnico && (
                                            <td className="px-4 py-3 text-xs text-slate-600">{t.creadoPor}</td>
                                        )}
                                        <td className="px-4 py-3 text-xs text-slate-500">{t.asignadoA || "M-bM-^@M-^T"}</td>
                                        <td className="px-4 py-3 text-xs text-slate-400">{formatFecha(t.fechaCreacion)}</td>
                                        <td className="px-4 py-3">
                                            <button
                                                onClick={() => abrirDetalle(t)}
                                                style={{ color: PRIMARY }}
                                                className="text-xs font-medium hover:underline flex items-center gap-1"
                                            >
                                                <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                                                </svg>
                                                Ver
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>
            </main>

            {/* FOOTER */}
            <footer style={{ backgroundColor: PRIMARY }} className="text-white text-opacity-80 text-xs text-center py-2 flex-shrink-0">
                Sistema de GestiM-CM-3n AcadM-CM-)mica M-bM-^@M-^T Escuela Provincias Unidas M-BM-) 2026
            </footer>

            {/* MODAL NUEVO TICKET */}
            {showModal && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4" style={modalBg}>
                    <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg overflow-hidden">
                        <div style={{ backgroundColor: PRIMARY }} className="px-6 py-4 flex items-center justify-between">
                            <h2 className="text-white font-semibold text-sm">Nuevo Ticket de Soporte</h2>
                            <button onClick={() => setShowModal(false)} className="text-white hover:opacity-70 text-lg">M-bM-^\M-^U</button>
                        </div>
                        <form onSubmit={handleCrear} className="p-6 space-y-4">
                            {error && (
                                <div className="bg-red-50 border border-red-200 rounded-lg px-3 py-2 text-red-600 text-xs">{error}</div>
                            )}
                            <div>
                                <label className="text-xs font-medium text-slate-600 block mb-1">TM-CM--tulo *</label>
                                <input
                                    type="text"
                                    value={form.titulo}
                                    onChange={e => setForm({ ...form, titulo: e.target.value })}
                                    placeholder="Describe brevemente el problema"
                                    className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-100"
                                    required maxLength={150}
                                />
                            </div>
                            <div>
                                <label className="text-xs font-medium text-slate-600 block mb-1">DescripciM-CM-3n *</label>
                                <textarea
                                    value={form.descripcion}
                                    onChange={e => setForm({ ...form, descripcion: e.target.value })}
                                    placeholder="Detalla el problema con el mayor contexto posible..."
                                    className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-100 resize-none h-24"
                                    required
                                />
                            </div>
                            <div className="grid grid-cols-2 gap-3">
                                <div>
                                    <label className="text-xs font-medium text-slate-600 block mb-1">CategorM-CM--a</label>
                                    <select
                                        value={form.categoria}
                                        onChange={e => setForm({ ...form, categoria: e.target.value })}
                                        className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm focus:outline-none"
                                    >
                                        <option value="HARDWARE">Hardware</option>
                                        <option value="SOFTWARE">Software</option>
                                        <option value="RED">Red / Conectividad</option>
                                        <option value="CUENTA">Cuenta / Acceso</option>
                                        <option value="OTRO">Otro</option>
                                    </select>
                                </div>
                                <div>
                                    <label className="text-xs font-medium text-slate-600 block mb-1">Prioridad</label>
                                    <select
                                        value={form.prioridad}
                                        onChange={e => setForm({ ...form, prioridad: e.target.value })}
                                        className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm focus:outline-none"
                                    >
                                        <option value="BAJO">Bajo</option>
                                        <option value="MEDIO">Medio</option>
                                        <option value="ALTO">Alto</option>
                                        <option value="CRITICO">CrM-CM--tico</option>
                                    </select>
                                </div>
                            </div>
                            <div className="flex justify-end gap-3 pt-2">
                                <button type="button" onClick={() => setShowModal(false)}
                                        className="px-4 py-2 text-sm text-slate-500 hover:text-slate-700 border border-slate-200 rounded-lg transition">
                                    Cancelar
                                </button>
                                <button type="submit" disabled={saving}
                                        style={{ backgroundColor: PRIMARY }}
                                        className="px-5 py-2 text-sm text-white rounded-lg hover:opacity-90 transition font-medium disabled:opacity-50">
                                    {saving ? "Guardando..." : "Crear ticket"}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* MODAL DETALLE */}
            {showDetalle && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4" style={modalBg}>
                    <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl overflow-hidden max-h-screen flex flex-col">
                        <div style={{ backgroundColor: PRIMARY }} className="px-6 py-4 flex items-center justify-between flex-shrink-0">
                            <div>
                                <p className="text-white text-opacity-70 text-xs font-mono">{showDetalle.numeroTicket}</p>
                                <h2 className="text-white font-semibold text-sm">{showDetalle.titulo}</h2>
                            </div>
                            <button onClick={() => setShowDetalle(null)} className="text-white hover:opacity-70 text-lg">M-bM-^\M-^U</button>
                        </div>
                        <div className="overflow-y-auto flex-1 p-6 space-y-5">
                            <div className="flex flex-wrap gap-2">
                <span className={`text-xs px-2 py-1 rounded-full font-medium ${estadoBadge(showDetalle.estado)}`}>
                  {showDetalle.estado.replace("_", " ")}
                </span>
                                <span className={`text-xs px-2 py-1 rounded-full font-medium ${prioridadBadge(showDetalle.prioridad)}`}>
                  {showDetalle.prioridad}
                </span>
                                <span className={`text-xs px-2 py-1 rounded-full font-medium ${categoriaBadge(showDetalle.categoria)}`}>
                  {showDetalle.categoria}
                </span>
                            </div>
                            <div className="grid grid-cols-2 gap-3 text-xs">
                                <div className="bg-slate-50 rounded-lg p-3">
                                    <p className="text-slate-400 mb-0.5">Creado por</p>
                                    <p className="font-medium text-slate-700">{showDetalle.creadoPor}</p>
                                </div>
                                <div className="bg-slate-50 rounded-lg p-3">
                                    <p className="text-slate-400 mb-0.5">Asignado a</p>
                                    <p className="font-medium text-slate-700">{showDetalle.asignadoA || "Sin asignar"}</p>
                                </div>
                                <div className="bg-slate-50 rounded-lg p-3">
                                    <p className="text-slate-400 mb-0.5">Fecha creaciM-CM-3n</p>
                                    <p className="font-medium text-slate-700">{formatFecha(showDetalle.fechaCreacion)}</p>
                                </div>
                                <div className="bg-slate-50 rounded-lg p-3">
                                    <p className="text-slate-400 mb-0.5">Fecha resoluciM-CM-3n</p>
                                    <p className="font-medium text-slate-700">{formatFecha(showDetalle.fechaResolucion)}</p>
                                </div>
                            </div>
                            <div>
                                <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1">DescripciM-CM-3n</p>
                                <p className="text-sm text-slate-700 bg-slate-50 rounded-lg p-3 leading-relaxed">{showDetalle.descripcion}</p>
                            </div>
                            {showDetalle.solucionAplicada && (
                                <div>
                                    <p className="text-xs font-semibold text-green-600 uppercase tracking-wide mb-1">SoluciM-CM-3n aplicada</p>
                                    <p className="text-sm text-slate-700 bg-green-50 border border-green-100 rounded-lg p-3 leading-relaxed">{showDetalle.solucionAplicada}</p>
                                </div>
                            )}
                            {esTecnico && (
                                <form onSubmit={handleActualizar} className="border border-slate-200 rounded-xl p-4 space-y-3">
                                    <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide">GestiM-CM-3n del ticket</p>
                                    <div className="grid grid-cols-2 gap-3">
                                        <div>
                                            <label className="text-xs text-slate-500 block mb-1">Estado</label>
                                            <select
                                                value={formUpdate.estado}
                                                onChange={e => setFormUpdate({ ...formUpdate, estado: e.target.value })}
                                                className="w-full border border-slate-200 rounded-lg px-2 py-1.5 text-xs focus:outline-none"
                                            >
                                                <option value="ABIERTO">Abierto</option>
                                                <option value="EN_PROCESO">En proceso</option>
                                                <option value="RESUELTO">Resuelto</option>
                                                <option value="CERRADO">Cerrado</option>
                                            </select>
                                        </div>
                                        <div>
                                            <label className="text-xs text-slate-500 block mb-1">Asignar a</label>
                                            <input
                                                type="text"
                                                value={formUpdate.asignadoA}
                                                onChange={e => setFormUpdate({ ...formUpdate, asignadoA: e.target.value })}
                                                placeholder="username del tM-CM-)cnico"
                                                className="w-full border border-slate-200 rounded-lg px-2 py-1.5 text-xs focus:outline-none"
                                            />
                                        </div>
                                    </div>
                                    <div>
                                        <label className="text-xs text-slate-500 block mb-1">SoluciM-CM-3n aplicada</label>
                                        <textarea
                                            value={formUpdate.solucionAplicada}
                                            onChange={e => setFormUpdate({ ...formUpdate, solucionAplicada: e.target.value })}
                                            placeholder="Describe la soluciM-CM-3n..."
                                            className="w-full border border-slate-200 rounded-lg px-2 py-1.5 text-xs focus:outline-none resize-none h-16"
                                        />
                                    </div>
                                    <div className="flex justify-end">
                                        <button type="submit" disabled={saving}
                                                style={{ backgroundColor: PRIMARY }}
                                                className="px-4 py-1.5 text-xs text-white rounded-lg hover:opacity-90 transition font-medium disabled:opacity-50">
                                            {saving ? "Guardando..." : "Guardar cambios"}
                                        </button>
                                    </div>
                                </form>
                            )}
                            <div>
                                <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-2">
                                    Comentarios ({comentarios.length})
                                </p>
                                {comentarios.length === 0 ? (
                                    <p className="text-xs text-slate-400 italic">Sin comentarios aM-CM-:n.</p>
                                ) : (
                                    <div className="space-y-2 max-h-40 overflow-y-auto">
                                        {comentarios.map(c => (
                                            <div key={c.idComentario}
                                                 className={`rounded-lg p-3 text-xs ${c.notaInterna ? "bg-amber-50 border border-amber-100" : "bg-slate-50 border border-slate-100"}`}>
                                                <div className="flex items-center justify-between mb-1">
                                                    <span className="font-semibold text-slate-700">{c.autor}</span>
                                                    <div className="flex items-center gap-2">
                                                        {c.notaInterna && (
                                                            <span className="text-amber-600 bg-amber-100 px-1.5 py-0.5 rounded-full">Nota interna</span>
                                                        )}
                                                        <span className="text-slate-400">{formatFecha(c.fechaCreacion)}</span>
                                                    </div>
                                                </div>
                                                <p className="text-slate-600 leading-relaxed">{c.contenido}</p>
                                            </div>
                                        ))}
                                    </div>
                                )}
                                {showDetalle.estado !== "CERRADO" && (
                                    <div className="mt-3 space-y-2">
                    <textarea
                        value={nuevoComentario}
                        onChange={e => setNuevoComentario(e.target.value)}
                        placeholder="Escribe un comentario..."
                        className="w-full border border-slate-200 rounded-lg px-3 py-2 text-xs focus:outline-none resize-none h-16"
                    />
                                        <div className="flex items-center justify-between">
                                            {esTecnico && (
                                                <label className="flex items-center gap-1.5 text-xs text-slate-500 cursor-pointer">
                                                    <input
                                                        type="checkbox"
                                                        checked={notaInterna}
                                                        onChange={e => setNotaInterna(e.target.checked)}
                                                        className="rounded"
                                                    />
                                                    Nota interna
                                                </label>
                                            )}
                                            <button
                                                onClick={enviarComentario}
                                                disabled={enviandoCom || !nuevoComentario.trim()}
                                                style={{ backgroundColor: PRIMARY }}
                                                className="ml-auto px-3 py-1.5 text-xs text-white rounded-lg hover:opacity-90 transition disabled:opacity-40"
                                            >
                                                {enviandoCom ? "Enviando..." : "Comentar"}
                                            </button>
                                        </div>
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}