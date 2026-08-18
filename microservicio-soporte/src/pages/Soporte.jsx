import { useState, useEffect, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import axios from "axios";
import Layout from "../components/Layout";

const TICKETS_MENU_ITEMS = [
    {
        id: "kanban",
        label: "Tablero Kanban",
        color: "bg-amber-50",
        iconColor: "text-amber-500",
        icon: (
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 17V7m6 10V7M5 5h14a2 2 0 012 2v10a2 2 0 01-2 2H5a2 2 0 01-2-2V7a2 2 0 012-2z" />
            </svg>
        ),
    },
    {
        id: "dashboard",
        label: "Vista resumen",
        color: "bg-blue-50",
        iconColor: "text-blue-500",
        icon: (
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z" />
            </svg>
        ),
    },
    {
        id: "nuevo",
        label: "Nuevo ticket",
        color: "bg-rose-50",
        iconColor: "text-rose-500",
        icon: (
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
            </svg>
        ),
    },
];

const API = "/api/soporte";
const PRIMARY = "#243A76";
const PRIMARY_LIGHT = "#2d4a96";

/* ────────────────────────────────────────────────────────────
   Columnas del Kanban. El backend solo soporta los 4 estados
   ABIERTO / EN_PROCESO / RESUELTO / CERRADO (CHECK constraint en
   sga_soporte.tickets). La columna "En Revisión" se mapea a
   RESUELTO: un ticket resuelto que aún no se confirma como cerrado.
   ──────────────────────────────────────────────────────────── */
const COLUMNAS = [
    { estado: "ABIERTO",    titulo: "Abierto",      dot: "bg-yellow-400", head: "bg-yellow-50 text-yellow-700 border-yellow-200" },
    { estado: "EN_PROCESO", titulo: "En Proceso",   dot: "bg-blue-400",   head: "bg-blue-50 text-blue-700 border-blue-200" },
    { estado: "RESUELTO",   titulo: "En Revisión",  dot: "bg-green-400",  head: "bg-green-50 text-green-700 border-green-200" },
    { estado: "CERRADO",    titulo: "Cerrado",      dot: "bg-slate-400",  head: "bg-slate-100 text-slate-500 border-slate-200" },
];

const PRIORIDAD_ORDEN = { CRITICO: 0, ALTO: 1, MEDIO: 2, BAJO: 3 };

const prioridadBadge = (p) => {
    const map = {
        BAJO:    "bg-slate-100 text-slate-500",
        MEDIO:   "bg-blue-100 text-blue-600",
        ALTO:    "bg-orange-100 text-orange-600",
        CRITICO: "bg-red-100 text-red-600",
    };
    return map[p] || "bg-slate-100 text-slate-500";
};

const prioridadBarra = (p) => {
    const map = { BAJO: "#94a3b8", MEDIO: "#3b82f6", ALTO: "#f97316", CRITICO: "#ef4444" };
    return map[p] || "#94a3b8";
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

const CATEGORIA_COLOR = {
    HARDWARE: "#a855f7",
    SOFTWARE: "#6366f1",
    RED:      "#06b6d4",
    CUENTA:   "#ec4899",
    OTRO:     "#94a3b8",
};

const formatFecha = (iso) => {
    if (!iso) return "—";
    return new Date(iso).toLocaleDateString("es-EC", {
        day: "2-digit", month: "short", year: "numeric",
    });
};

const formatFechaHora = (iso) => {
    if (!iso) return "—";
    return new Date(iso).toLocaleString("es-EC", {
        day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit",
    });
};

const tiempoTranscurrido = (iso) => {
    if (!iso) return "";
    const diffMs = Date.now() - new Date(iso).getTime();
    const mins = Math.floor(diffMs / 60000);
    if (mins < 1) return "ahora";
    if (mins < 60) return `hace ${mins} min`;
    const horas = Math.floor(mins / 60);
    if (horas < 24) return `hace ${horas} h`;
    const dias = Math.floor(horas / 24);
    if (dias < 30) return `hace ${dias} d`;
    return `hace ${Math.floor(dias / 30)} mes${dias >= 60 ? "es" : ""}`;
};

const esHoy = (iso) => {
    if (!iso) return false;
    const d = new Date(iso);
    const hoy = new Date();
    return d.getFullYear() === hoy.getFullYear() &&
        d.getMonth() === hoy.getMonth() &&
        d.getDate() === hoy.getDate();
};

/* ── Mini gráfico de línea (SVG, sin dependencias) ─────────── */
function LineChart({ data, height = 140 }) {
    const width = 100; // usa viewBox porcentual, escala con el contenedor
    const max = Math.max(1, ...data.map(d => d.count));
    const pts = data.map((d, i) => {
        const x = (i / Math.max(1, data.length - 1)) * width;
        const y = height - (d.count / max) * (height - 20) - 5;
        return `${x},${y}`;
    });
    const path = "M" + pts.join(" L");
    const areaPath = `${path} L${width},${height} L0,${height} Z`;

    return (
        <svg viewBox={`0 0 ${width} ${height}`} preserveAspectRatio="none" className="w-full" style={{ height }}>
            <defs>
                <linearGradient id="lineFill" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor={PRIMARY} stopOpacity="0.25" />
                    <stop offset="100%" stopColor={PRIMARY} stopOpacity="0" />
                </linearGradient>
            </defs>
            <path d={areaPath} fill="url(#lineFill)" stroke="none" />
            <path d={path} fill="none" stroke={PRIMARY} strokeWidth="1" vectorEffect="non-scaling-stroke" />
            {data.map((d, i) => {
                const x = (i / Math.max(1, data.length - 1)) * width;
                const y = height - (d.count / max) * (height - 20) - 5;
                return d.count > 0 ? (
                    <circle key={i} cx={x} cy={y} r="1.4" fill={PRIMARY} vectorEffect="non-scaling-stroke" />
                ) : null;
            })}
        </svg>
    );
}

/* ── Mini donut (SVG, sin dependencias) ────────────────────── */
function DonutChart({ data, size = 160 }) {
    const total = data.reduce((s, d) => s + d.value, 0) || 1;
    const r = 40, c = 2 * Math.PI * r;
    const segmentos = [];
    {
        let acc = 0;
        for (const d of data) {
            const dash = (d.value / total) * c;
            segmentos.push({ ...d, dash, offset: c - acc });
            acc += dash;
        }
    }
    return (
        <div className="flex items-center gap-5">
            <svg width={size} height={size} viewBox="0 0 100 100" className="-rotate-90 flex-shrink-0">
                <circle cx="50" cy="50" r={r} fill="none" stroke="#f1f5f9" strokeWidth="14" />
                {segmentos.map((d, i) => {
                    const { dash, offset } = d;
                    return (
                        <circle
                            key={i} cx="50" cy="50" r={r} fill="none"
                            stroke={d.color} strokeWidth="14"
                            strokeDasharray={`${dash} ${c - dash}`}
                            strokeDashoffset={offset}
                        />
                    );
                })}
            </svg>
            <div className="space-y-1.5 text-xs">
                {data.map((d, i) => (
                    <div key={i} className="flex items-center gap-2">
                        <span className="w-2.5 h-2.5 rounded-full flex-shrink-0" style={{ backgroundColor: d.color }} />
                        <span className="text-slate-600">{d.label}</span>
                        <span className="text-slate-400">({d.value})</span>
                    </div>
                ))}
            </div>
        </div>
    );
}

export default function Soporte() {
    const [tickets, setTickets]           = useState([]);
    const [tecnicos, setTecnicos]         = useState([]);
    const [loading, setLoading]           = useState(true);
    const [busqueda, setBusqueda]         = useState("");
    const [filtroTecnico, setFiltroTecnico]     = useState("TODOS");
    const [filtroPrioridad, setFiltroPrioridad] = useState("TODOS");
    const [filtroCategoria, setFiltroCategoria] = useState("TODOS");
    const [fechaDesde, setFechaDesde]     = useState("");
    const [fechaHasta, setFechaHasta]     = useState("");
    const [vista, setVista]               = useState("kanban"); // kanban | dashboard
    const [showModal, setShowModal]       = useState(false);
    const [showDetalle, setShowDetalle]   = useState(null);
    const [saving, setSaving]             = useState(false);
    const [error, setError]               = useState("");
    const [success, setSuccess]           = useState("");
    const [comentarios, setComentarios]   = useState([]);
    const [nuevoComentario, setNuevoComentario] = useState("");
    const [notaInterna, setNotaInterna]   = useState(false);
    const [enviandoCom, setEnviandoCom]   = useState(false);
    const [adjunto, setAdjunto]           = useState(null);
    const [dragOverCol, setDragOverCol]   = useState(null);
    const [draggingId, setDraggingId]     = useState(null);

    // acciones rápidas del detalle
    const [asignarSel, setAsignarSel]     = useState("");
    const [guardandoAsignar, setGuardandoAsignar] = useState(false);
    const [prioridadSel, setPrioridadSel] = useState("");
    const [motivoEscalar, setMotivoEscalar] = useState("");
    const [guardandoPrioridad, setGuardandoPrioridad] = useState(false);
    const [cerrando, setCerrando]         = useState(false);

    const [form, setForm] = useState({
        titulo: "", descripcion: "", prioridad: "MEDIO", categoria: "OTRO",
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

    const cargarTecnicos = () => {
        if (!esTecnico) return;
        axios.get(`${API}/tecnicos-list`, { headers })
            .then(r => setTecnicos(r.data))
            .catch(() => setTecnicos([]));
    };

    useEffect(() => {
        if (!token) {
            const host = typeof window !== "undefined" ? window.location.hostname : "localhost";
            window.location.href = `http://${host}:5174/login`;
            return;
        }
        cargar();
        cargarTecnicos();
    }, []);

    useEffect(() => {
        if (success) {
            const t = setTimeout(() => setSuccess(""), 4000);
            return () => clearTimeout(t);
        }
    }, [success]);

    /* ── Filtros ────────────────────────────────────────────── */
    const filtrados = useMemo(() => {
        return tickets.filter(t => {
            const matchBusq =
                t.titulo?.toLowerCase().includes(busqueda.toLowerCase()) ||
                t.numeroTicket?.toLowerCase().includes(busqueda.toLowerCase()) ||
                t.creadoPor?.toLowerCase().includes(busqueda.toLowerCase());
            const matchTecnico   = filtroTecnico === "TODOS" || t.asignadoA === filtroTecnico;
            const matchPrioridad = filtroPrioridad === "TODOS" || t.prioridad === filtroPrioridad;
            const matchCategoria = filtroCategoria === "TODOS" || t.categoria === filtroCategoria;
            let matchFecha = true;
            if (fechaDesde) matchFecha = matchFecha && new Date(t.fechaCreacion) >= new Date(fechaDesde);
            if (fechaHasta) matchFecha = matchFecha && new Date(t.fechaCreacion) <= new Date(fechaHasta + "T23:59:59");
            return matchBusq && matchTecnico && matchPrioridad && matchCategoria && matchFecha;
        });
    }, [tickets, busqueda, filtroTecnico, filtroPrioridad, filtroCategoria, fechaDesde, fechaHasta]);

    const porColumna = (estado) =>
        filtrados
            .filter(t => t.estado === estado)
            .sort((a, b) => (PRIORIDAD_ORDEN[a.prioridad] ?? 9) - (PRIORIDAD_ORDEN[b.prioridad] ?? 9));

    const limpiarFiltros = () => {
        setBusqueda(""); setFiltroTecnico("TODOS"); setFiltroPrioridad("TODOS");
        setFiltroCategoria("TODOS"); setFechaDesde(""); setFechaHasta("");
    };
    const hayFiltrosActivos = busqueda || filtroTecnico !== "TODOS" || filtroPrioridad !== "TODOS" ||
        filtroCategoria !== "TODOS" || fechaDesde || fechaHasta;

    /* ── Drag & drop ────────────────────────────────────────── */
    const cambiarEstado = async (ticket, nuevoEstado) => {
        if (ticket.estado === nuevoEstado) return;
        const anterior = tickets;
        setTickets(prev => prev.map(t => t.idTicket === ticket.idTicket ? { ...t, estado: nuevoEstado } : t));
        try {
            await axios.put(`${API}/tickets/${ticket.idTicket}`, { estado: nuevoEstado }, { headers });
            setSuccess(`Ticket ${ticket.numeroTicket} movido a ${COLUMNAS.find(c => c.estado === nuevoEstado)?.titulo}.`);
        } catch (err) {
            setTickets(anterior);
            setError(err.response?.data?.message || "No se pudo cambiar el estado del ticket");
        }
    };

    const onDrop = (e, estadoDestino) => {
        e.preventDefault();
        setDragOverCol(null);
        const id = e.dataTransfer.getData("text/plain");
        const ticket = tickets.find(t => String(t.idTicket) === id);
        if (ticket && esTecnico) cambiarEstado(ticket, estadoDestino);
        setDraggingId(null);
    };

    /* ── CRUD ticket ────────────────────────────────────────── */
    const handleCrear = async (e) => {
        e.preventDefault();
        setSaving(true); setError("");
        try {
            await axios.post(`${API}/tickets`, form, { headers });
            setSuccess("Ticket creado correctamente.");
            setShowModal(false);
            setForm({ titulo: "", descripcion: "", prioridad: "MEDIO", categoria: "OTRO" });
            cargar();
        } catch (err) {
            setError(err.response?.data?.message || "Error al crear ticket");
        } finally { setSaving(false); }
    };

    const abrirDetalle = async (ticket) => {
        setShowDetalle(ticket);
        setAsignarSel(ticket.asignadoA || "");
        setPrioridadSel(ticket.prioridad);
        setMotivoEscalar("");
        setAdjunto(null);
        try {
            const r = await axios.get(`${API}/tickets/${ticket.idTicket}/comentarios`, { headers });
            setComentarios(r.data);
        } catch { setComentarios([]); }
    };

    const refrescarDetalle = async (id) => {
        const r = await axios.get(`${API}/tickets/${id}`, { headers });
        setShowDetalle(r.data);
        cargar();
        return r.data;
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
            setNuevoComentario(""); setNotaInterna(false); setAdjunto(null);
            const r = await axios.get(`${API}/tickets/${showDetalle.idTicket}/comentarios`, { headers });
            setComentarios(r.data);
        } catch { setError("Error al enviar comentario"); }
        finally { setEnviandoCom(false); }
    };

    const handleAsignar = async () => {
        if (!asignarSel) return;
        setGuardandoAsignar(true); setError("");
        try {
            await axios.put(`${API}/tickets/${showDetalle.idTicket}`,
                { estado: showDetalle.estado, asignadoA: asignarSel }, { headers });
            setSuccess("Técnico asignado.");
            await refrescarDetalle(showDetalle.idTicket);
        } catch (err) {
            setError(err.response?.data?.message || "No se pudo asignar el técnico");
        } finally { setGuardandoAsignar(false); }
    };

    const handleCambiarPrioridad = async () => {
        if (!motivoEscalar.trim()) { setError("El motivo es obligatorio para cambiar la prioridad."); return; }
        setGuardandoPrioridad(true); setError("");
        try {
            await axios.post(`${API}/tickets/${showDetalle.idTicket}/escalar`,
                { nuevaPrioridad: prioridadSel, motivo: motivoEscalar }, { headers });
            setSuccess("Prioridad actualizada.");
            setMotivoEscalar("");
            await refrescarDetalle(showDetalle.idTicket);
            const rc = await axios.get(`${API}/tickets/${showDetalle.idTicket}/comentarios`, { headers });
            setComentarios(rc.data);
        } catch (err) {
            setError(err.response?.data?.message || "No se pudo cambiar la prioridad");
        } finally { setGuardandoPrioridad(false); }
    };

    const handleCerrarTicket = async () => {
        if (!window.confirm(`¿Cerrar el ticket ${showDetalle.numeroTicket}? Esta acción no se puede deshacer desde aquí.`)) return;
        setCerrando(true); setError("");
        try {
            await axios.put(`${API}/tickets/${showDetalle.idTicket}`, { estado: "CERRADO" }, { headers });
            setSuccess("Ticket cerrado.");
            await refrescarDetalle(showDetalle.idTicket);
        } catch (err) {
            setError(err.response?.data?.message || "No se pudo cerrar el ticket");
        } finally { setCerrando(false); }
    };

    const handleLogout = () => {
        localStorage.clear();
        const host = typeof window !== "undefined" ? window.location.hostname : "localhost";
        window.location.href = `http://${host}:5174/login`;
    };

    /* ── Datos para el dashboard (calculados en el cliente) ──── */
    const dashboardData = useMemo(() => {
        const total = tickets.length;
        const abiertosHoy = tickets.filter(t => t.estado === "ABIERTO" && esHoy(t.fechaCreacion)).length;

        const resueltosConFecha = tickets.filter(t => t.fechaResolucion);
        const promedioHoras = resueltosConFecha.length
            ? (resueltosConFecha.reduce((sum, t) => {
                const h = (new Date(t.fechaResolucion) - new Date(t.fechaCreacion)) / 3_600_000;
                return sum + h;
            }, 0) / resueltosConFecha.length)
            : 0;

        const porTecnicoMap = {};
        tickets.filter(t => t.estado === "CERRADO" && t.asignadoA).forEach(t => {
            porTecnicoMap[t.asignadoA] = (porTecnicoMap[t.asignadoA] || 0) + 1;
        });
        const topTecnicos = Object.entries(porTecnicoMap)
            .map(([tecnico, cerrados]) => ({ tecnico, cerrados }))
            .sort((a, b) => b.cerrados - a.cerrados)
            .slice(0, 5);

        // últimos 30 días
        const dias = [];
        for (let i = 29; i >= 0; i--) {
            const d = new Date();
            d.setHours(0, 0, 0, 0);
            d.setDate(d.getDate() - i);
            dias.push(d);
        }
        const porDia = dias.map(d => {
            const count = tickets.filter(t => {
                const f = new Date(t.fechaCreacion);
                return f.getFullYear() === d.getFullYear() && f.getMonth() === d.getMonth() && f.getDate() === d.getDate();
            }).length;
            return { fecha: d, count };
        });

        const porCategoriaMap = {};
        tickets.forEach(t => { porCategoriaMap[t.categoria] = (porCategoriaMap[t.categoria] || 0) + 1; });
        const porCategoria = Object.entries(porCategoriaMap)
            .map(([label, value]) => ({ label, value, color: CATEGORIA_COLOR[label] || "#94a3b8" }))
            .sort((a, b) => b.value - a.value);

        return { total, abiertosHoy, promedioHoras, topTecnicos, porDia, porCategoria };
    }, [tickets]);

    const modalBg = { backgroundColor: "rgba(36,58,118,0.5)" };

    return (
        <Layout breadcrumb={["Inicio", "Tickets"]} sidebarTitle="Tickets" menuItems={TICKETS_MENU_ITEMS} seccion={vista}
            onSeccionChange={(id) => {
                if (id === "nuevo") { setShowModal(true); setError(""); }
                else { setVista(id); }
            }}>
            <div className="space-y-4">
                <div className="flex items-center justify-between flex-wrap gap-2 mb-2">
                    <div>
                        <h1 className="text-xl font-bold text-slate-800">Módulo de Tickets</h1>
                        <p className="text-xs text-slate-400">Gestión de atención e incidencias técnicas</p>
                    </div>
                    {esTecnico && (
                        <div className="flex gap-1 bg-slate-200/60 rounded-xl p-1">
                            <button
                                onClick={() => setVista("kanban")}
                                className={`text-xs px-3.5 py-1.5 rounded-lg font-medium transition ${vista === "kanban" ? "bg-white shadow-sm font-semibold text-[#243A76]" : "text-slate-600 hover:text-slate-900"}`}
                            >
                                Tablero
                            </button>
                            <button
                                onClick={() => setVista("dashboard")}
                                className={`text-xs px-3.5 py-1.5 rounded-lg font-medium transition ${vista === "dashboard" ? "bg-white shadow-sm font-semibold text-[#243A76]" : "text-slate-600 hover:text-slate-900"}`}
                            >
                                Estadísticas
                            </button>
                        </div>
                    )}
                </div>


                {/* Alertas */}
                {error && (
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

                {vista === "dashboard" && esTecnico ? (
                    /* ══════════════ DASHBOARD ══════════════ */
                    <div className="space-y-5">
                        <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                            <div className="rounded-xl border border-slate-200 bg-white p-4">
                                <p className="text-2xl font-bold text-slate-700">{dashboardData.total}</p>
                                <p className="text-xs text-slate-400 mt-0.5">Total de tickets</p>
                            </div>
                            <div className="rounded-xl border border-yellow-200 bg-yellow-50 p-4">
                                <p className="text-2xl font-bold text-yellow-700">{dashboardData.abiertosHoy}</p>
                                <p className="text-xs text-slate-500 mt-0.5">Abiertos hoy</p>
                            </div>
                            <div className="rounded-xl border border-blue-200 bg-blue-50 p-4">
                                <p className="text-2xl font-bold text-blue-700">
                                    {dashboardData.promedioHoras ? dashboardData.promedioHoras.toFixed(1) : "0"}
                                    <span className="text-sm font-medium"> h</span>
                                </p>
                                <p className="text-xs text-slate-500 mt-0.5">Promedio de resolución</p>
                            </div>
                        </div>

                        <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
                            <div className="lg:col-span-2 bg-white rounded-xl border border-slate-200 p-4">
                                <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-3">
                                    Tickets creados — últimos 30 días
                                </p>
                                <LineChart data={dashboardData.porDia} />
                                <div className="flex justify-between text-[10px] text-slate-400 mt-1">
                                    <span>{formatFecha(dashboardData.porDia[0]?.fecha)}</span>
                                    <span>{formatFecha(dashboardData.porDia[dashboardData.porDia.length - 1]?.fecha)}</span>
                                </div>
                            </div>

                            <div className="bg-white rounded-xl border border-slate-200 p-4">
                                <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-3">
                                    Distribución por categoría
                                </p>
                                {dashboardData.porCategoria.length > 0 ? (
                                    <DonutChart data={dashboardData.porCategoria} />
                                ) : (
                                    <p className="text-xs text-slate-400">Sin datos aún.</p>
                                )}
                            </div>
                        </div>

                        <div className="bg-white rounded-xl border border-slate-200 p-4">
                            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-3">
                                Top 5 técnicos por tickets cerrados
                            </p>
                            {dashboardData.topTecnicos.length === 0 ? (
                                <p className="text-xs text-slate-400">Aún no hay tickets cerrados.</p>
                            ) : (
                                <div className="space-y-2">
                                    {dashboardData.topTecnicos.map((t, i) => {
                                        const max = dashboardData.topTecnicos[0].cerrados || 1;
                                        return (
                                            <div key={t.tecnico} className="flex items-center gap-3">
                                                <span className="text-xs text-slate-400 w-4">{i + 1}</span>
                                                <span className="text-xs font-medium text-slate-700 w-32 truncate">{t.tecnico}</span>
                                                <div className="flex-1 bg-slate-100 rounded-full h-2.5 overflow-hidden">
                                                    <div
                                                        className="h-full rounded-full"
                                                        style={{ width: `${(t.cerrados / max) * 100}%`, backgroundColor: PRIMARY }}
                                                    />
                                                </div>
                                                <span className="text-xs font-semibold text-slate-600 w-6 text-right">{t.cerrados}</span>
                                            </div>
                                        );
                                    })}
                                </div>
                            )}
                        </div>
                    </div>
                ) : (
                    /* ══════════════ TABLERO KANBAN ══════════════ */
                    <div>
                        {/* Header + filtros */}
                        <div className="flex items-center justify-between mb-3 flex-wrap gap-2">
                            <div>
                                <h1 className="text-lg font-bold text-slate-700">Tickets de Soporte</h1>
                                <p className="text-xs text-slate-400">
                                    {filtrados.length} ticket{filtrados.length !== 1 ? "s" : ""} encontrado{filtrados.length !== 1 ? "s" : ""}
                                </p>
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

                        <div className="flex items-center gap-2 flex-wrap mb-4 bg-white border border-slate-200 rounded-lg p-2.5">
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
                            {esTecnico && (
                                <select
                                    value={filtroTecnico}
                                    onChange={e => setFiltroTecnico(e.target.value)}
                                    className="text-xs border border-slate-200 rounded-lg px-2 py-1.5 bg-slate-50 focus:outline-none"
                                >
                                    <option value="TODOS">Todos los técnicos</option>
                                    {tecnicos.map(t => (
                                        <option key={t.username} value={t.username}>{t.nombreCompleto || t.username}</option>
                                    ))}
                                </select>
                            )}
                            <select
                                value={filtroPrioridad}
                                onChange={e => setFiltroPrioridad(e.target.value)}
                                className="text-xs border border-slate-200 rounded-lg px-2 py-1.5 bg-slate-50 focus:outline-none"
                            >
                                <option value="TODOS">Toda prioridad</option>
                                <option value="BAJO">Bajo</option>
                                <option value="MEDIO">Medio</option>
                                <option value="ALTO">Alto</option>
                                <option value="CRITICO">Crítico</option>
                            </select>
                            <select
                                value={filtroCategoria}
                                onChange={e => setFiltroCategoria(e.target.value)}
                                className="text-xs border border-slate-200 rounded-lg px-2 py-1.5 bg-slate-50 focus:outline-none"
                            >
                                <option value="TODOS">Toda categoría</option>
                                <option value="HARDWARE">Hardware</option>
                                <option value="SOFTWARE">Software</option>
                                <option value="RED">Red</option>
                                <option value="CUENTA">Cuenta</option>
                                <option value="OTRO">Otro</option>
                            </select>
                            <div className="flex items-center gap-1">
                                <input type="date" value={fechaDesde} onChange={e => setFechaDesde(e.target.value)}
                                    className="text-xs border border-slate-200 rounded-lg px-2 py-1.5 bg-slate-50 focus:outline-none" />
                                <span className="text-xs text-slate-400">a</span>
                                <input type="date" value={fechaHasta} onChange={e => setFechaHasta(e.target.value)}
                                    className="text-xs border border-slate-200 rounded-lg px-2 py-1.5 bg-slate-50 focus:outline-none" />
                            </div>
                            {hayFiltrosActivos && (
                                <button onClick={limpiarFiltros} className="text-xs text-slate-400 hover:text-slate-600 underline ml-auto">
                                    Limpiar filtros
                                </button>
                            )}
                        </div>

                        {/* Kanban */}
                        {loading ? (
                            <div className="p-12 text-center text-slate-400 text-sm">Cargando tickets...</div>
                        ) : (
                            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4">
                                {COLUMNAS.map(col => {
                                    const items = porColumna(col.estado);
                                    return (
                                        <div
                                            key={col.estado}
                                            onDragOver={(e) => { e.preventDefault(); if (esTecnico) setDragOverCol(col.estado); }}
                                            onDragLeave={() => setDragOverCol(null)}
                                            onDrop={(e) => onDrop(e, col.estado)}
                                            className={`rounded-xl border-2 border-dashed transition ${dragOverCol === col.estado ? "border-blue-300 bg-blue-50" : "border-transparent"}`}
                                        >
                                            <div className={`flex items-center justify-between rounded-t-xl border px-3 py-2 ${col.head}`}>
                                                <div className="flex items-center gap-2">
                                                    <span className={`w-2 h-2 rounded-full ${col.dot}`} />
                                                    <span className="text-xs font-semibold">{col.titulo}</span>
                                                </div>
                                                <span className="text-xs font-bold bg-white bg-opacity-70 rounded-full px-2">{items.length}</span>
                                            </div>
                                            <div className="bg-slate-100 rounded-b-xl p-2 space-y-2 min-h-[120px] max-h-[65vh] overflow-y-auto">
                                                {items.length === 0 ? (
                                                    <p className="text-[11px] text-slate-400 text-center py-6">Sin tickets</p>
                                                ) : items.map(t => (
                                                    <div
                                                        key={t.idTicket}
                                                        draggable={esTecnico}
                                                        onDragStart={(e) => { e.dataTransfer.setData("text/plain", String(t.idTicket)); setDraggingId(t.idTicket); }}
                                                        onDragEnd={() => setDraggingId(null)}
                                                        onClick={() => abrirDetalle(t)}
                                                        className={`bg-white rounded-lg shadow-sm border border-slate-200 p-3 cursor-pointer hover:shadow-md transition relative overflow-hidden ${draggingId === t.idTicket ? "opacity-40" : ""}`}
                                                    >
                                                        <span className="absolute left-0 top-0 bottom-0 w-1" style={{ backgroundColor: prioridadBarra(t.prioridad) }} />
                                                        <div className="pl-2">
                                                            <div className="flex items-center justify-between mb-1">
                                                                <span className="font-mono text-[10px] font-semibold" style={{ color: PRIMARY }}>{t.numeroTicket}</span>
                                                                <span className={`text-[10px] px-1.5 py-0.5 rounded-full font-medium ${prioridadBadge(t.prioridad)}`}>{t.prioridad}</span>
                                                            </div>
                                                            <p className="text-xs font-medium text-slate-700 leading-snug line-clamp-2 mb-1.5">{t.titulo}</p>
                                                            <div className="flex items-center justify-between text-[10px] text-slate-400">
                                                                <span className="flex items-center gap-1 truncate">
                                                                    <svg className="w-3 h-3 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                                                                    </svg>
                                                                    {t.creadoPor}
                                                                </span>
                                                                <span>{tiempoTranscurrido(t.fechaCreacion)}</span>
                                                            </div>
                                                            <span className={`inline-block mt-1.5 text-[10px] px-1.5 py-0.5 rounded-full font-medium ${categoriaBadge(t.categoria)}`}>
                                                                {t.categoria}
                                                            </span>
                                                        </div>
                                                    </div>
                                                ))}
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>
                        )}
                    </div>
                )}


            {/* MODAL NUEVO TICKET */}
            {showModal && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4" style={modalBg}>
                    <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg overflow-hidden">
                        <div style={{ backgroundColor: PRIMARY }} className="px-6 py-4 flex items-center justify-between">
                            <h2 className="text-white font-semibold text-sm">Nuevo Ticket de Soporte</h2>
                            <button onClick={() => setShowModal(false)} className="text-white hover:opacity-70 text-lg">✕</button>
                        </div>
                        <form onSubmit={handleCrear} className="p-6 space-y-4">
                            {error && (
                                <div className="bg-red-50 border border-red-200 rounded-lg px-3 py-2 text-red-600 text-xs">{error}</div>
                            )}
                            <div>
                                <label className="text-xs font-medium text-slate-600 block mb-1">Título *</label>
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
                                <label className="text-xs font-medium text-slate-600 block mb-1">Descripción *</label>
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
                                    <label className="text-xs font-medium text-slate-600 block mb-1">Categoría</label>
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
                                        <option value="CRITICO">Crítico</option>
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
                    <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl overflow-hidden max-h-[92vh] flex flex-col">
                        <div style={{ backgroundColor: PRIMARY }} className="px-6 py-4 flex items-center justify-between flex-shrink-0">
                            <div>
                                <p className="text-white text-opacity-70 text-xs font-mono">{showDetalle.numeroTicket}</p>
                                <h2 className="text-white font-semibold text-sm">{showDetalle.titulo}</h2>
                            </div>
                            <button onClick={() => setShowDetalle(null)} className="text-white hover:opacity-70 text-lg">✕</button>
                        </div>
                        <div className="overflow-y-auto flex-1 p-6 space-y-5">

                            <div className="flex flex-wrap gap-2">
                                <span className={`text-xs px-2 py-1 rounded-full font-medium ${prioridadBadge(showDetalle.prioridad)}`}>
                                    {showDetalle.prioridad}
                                </span>
                                <span className={`text-xs px-2 py-1 rounded-full font-medium ${categoriaBadge(showDetalle.categoria)}`}>
                                    {showDetalle.categoria}
                                </span>
                                <span className="text-xs px-2 py-1 rounded-full font-medium bg-slate-100 text-slate-600">
                                    {COLUMNAS.find(c => c.estado === showDetalle.estado)?.titulo || showDetalle.estado}
                                </span>
                            </div>

                            <div className="grid grid-cols-2 gap-3 text-xs">
                                <div className="bg-slate-50 rounded-lg p-3">
                                    <p className="text-slate-400 mb-0.5">Solicitante</p>
                                    <p className="font-medium text-slate-700">{showDetalle.creadoPor}</p>
                                </div>
                                <div className="bg-slate-50 rounded-lg p-3">
                                    <p className="text-slate-400 mb-0.5">Asignado a</p>
                                    <p className="font-medium text-slate-700">{showDetalle.asignadoA || "Sin asignar"}</p>
                                </div>
                                <div className="bg-slate-50 rounded-lg p-3">
                                    <p className="text-slate-400 mb-0.5">Creado</p>
                                    <p className="font-medium text-slate-700">{formatFecha(showDetalle.fechaCreacion)} · {tiempoTranscurrido(showDetalle.fechaCreacion)}</p>
                                </div>
                                <div className="bg-slate-50 rounded-lg p-3">
                                    <p className="text-slate-400 mb-0.5">Resolución</p>
                                    <p className="font-medium text-slate-700">{formatFecha(showDetalle.fechaResolucion)}</p>
                                </div>
                            </div>

                            <div>
                                <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1">Descripción</p>
                                <p className="text-sm text-slate-700 bg-slate-50 rounded-lg p-3 leading-relaxed">{showDetalle.descripcion}</p>
                            </div>

                            {showDetalle.solucionAplicada && (
                                <div>
                                    <p className="text-xs font-semibold text-green-600 uppercase tracking-wide mb-1">Solución aplicada</p>
                                    <p className="text-sm text-slate-700 bg-green-50 border border-green-100 rounded-lg p-3 leading-relaxed">{showDetalle.solucionAplicada}</p>
                                </div>
                            )}

                            {/* Acciones rápidas — solo técnicos */}
                            {esTecnico && showDetalle.estado !== "CERRADO" && (
                                <div className="border border-slate-200 rounded-xl p-4 space-y-4">
                                    <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide">Acciones</p>

                                    <div className="flex items-end gap-2 flex-wrap">
                                        <div className="flex-1 min-w-[160px]">
                                            <label className="text-[11px] text-slate-500 block mb-1">Asignar técnico</label>
                                            <select
                                                value={asignarSel}
                                                onChange={e => setAsignarSel(e.target.value)}
                                                className="w-full border border-slate-200 rounded-lg px-2 py-1.5 text-xs focus:outline-none"
                                            >
                                                <option value="">Selecciona un técnico…</option>
                                                {tecnicos.map(t => (
                                                    <option key={t.username} value={t.username}>{t.nombreCompleto || t.username}</option>
                                                ))}
                                            </select>
                                        </div>
                                        <button
                                            onClick={handleAsignar}
                                            disabled={guardandoAsignar || !asignarSel || asignarSel === showDetalle.asignadoA}
                                            style={{ backgroundColor: PRIMARY }}
                                            className="px-3 py-1.5 text-xs text-white rounded-lg hover:opacity-90 transition disabled:opacity-40"
                                        >
                                            {guardandoAsignar ? "Asignando..." : "Asignar"}
                                        </button>
                                    </div>

                                    <div className="flex items-end gap-2 flex-wrap">
                                        <div className="w-28">
                                            <label className="text-[11px] text-slate-500 block mb-1">Prioridad</label>
                                            <select
                                                value={prioridadSel}
                                                onChange={e => setPrioridadSel(e.target.value)}
                                                className="w-full border border-slate-200 rounded-lg px-2 py-1.5 text-xs focus:outline-none"
                                            >
                                                <option value="BAJO">Bajo</option>
                                                <option value="MEDIO">Medio</option>
                                                <option value="ALTO">Alto</option>
                                                <option value="CRITICO">Crítico</option>
                                            </select>
                                        </div>
                                        <div className="flex-1 min-w-[160px]">
                                            <label className="text-[11px] text-slate-500 block mb-1">Motivo del cambio *</label>
                                            <input
                                                type="text"
                                                value={motivoEscalar}
                                                onChange={e => setMotivoEscalar(e.target.value)}
                                                placeholder="Requerido para escalar/cambiar prioridad"
                                                className="w-full border border-slate-200 rounded-lg px-2 py-1.5 text-xs focus:outline-none"
                                            />
                                        </div>
                                        <button
                                            onClick={handleCambiarPrioridad}
                                            disabled={guardandoPrioridad || !motivoEscalar.trim() || prioridadSel === showDetalle.prioridad}
                                            style={{ backgroundColor: PRIMARY }}
                                            className="px-3 py-1.5 text-xs text-white rounded-lg hover:opacity-90 transition disabled:opacity-40"
                                        >
                                            {guardandoPrioridad ? "Guardando..." : "Cambiar prioridad"}
                                        </button>
                                    </div>

                                    <div className="flex justify-end pt-1 border-t border-slate-100">
                                        <button
                                            onClick={handleCerrarTicket}
                                            disabled={cerrando}
                                            className="px-3 py-1.5 text-xs text-red-600 border border-red-200 rounded-lg hover:bg-red-50 transition disabled:opacity-40"
                                        >
                                            {cerrando ? "Cerrando..." : "Cerrar ticket"}
                                        </button>
                                    </div>
                                </div>
                            )}

                            {/* Timeline de comentarios como chat */}
                            <div>
                                <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-2">
                                    Conversación ({comentarios.length})
                                </p>
                                {comentarios.length === 0 ? (
                                    <p className="text-xs text-slate-400 italic">Sin comentarios aún.</p>
                                ) : (
                                    <div className="space-y-2 max-h-64 overflow-y-auto pr-1">
                                        {comentarios.map(c => {
                                            const propio = c.autor === username;
                                            return (
                                                <div key={c.idComentario} className={`flex ${propio ? "justify-end" : "justify-start"}`}>
                                                    <div className={`max-w-[80%] rounded-2xl px-3 py-2 text-xs ${
                                                        c.notaInterna
                                                            ? "bg-amber-50 border border-amber-200"
                                                            : propio
                                                                ? "text-white"
                                                                : "bg-slate-100"
                                                    }`}
                                                        style={propio && !c.notaInterna ? { backgroundColor: PRIMARY } : {}}
                                                    >
                                                        <div className="flex items-center gap-2 mb-0.5">
                                                            <span className={`font-semibold ${propio && !c.notaInterna ? "text-white text-opacity-90" : "text-slate-600"}`}>
                                                                {c.autor}
                                                            </span>
                                                            {c.notaInterna && (
                                                                <span className="text-amber-600 bg-amber-100 px-1.5 py-0.5 rounded-full text-[10px]">Nota interna</span>
                                                            )}
                                                        </div>
                                                        <p className={propio && !c.notaInterna ? "text-white" : "text-slate-700"}>{c.contenido}</p>
                                                        <p className={`text-[10px] mt-0.5 text-right ${propio && !c.notaInterna ? "text-white text-opacity-60" : "text-slate-400"}`}>
                                                            {formatFechaHora(c.fechaCreacion)}
                                                        </p>
                                                    </div>
                                                </div>
                                            );
                                        })}
                                    </div>
                                )}

                                {showDetalle.estado !== "CERRADO" && (
                                    <div className="mt-3 space-y-2">
                                        <textarea
                                            value={nuevoComentario}
                                            onChange={e => setNuevoComentario(e.target.value)}
                                            placeholder="Escribe una respuesta..."
                                            className="w-full border border-slate-200 rounded-lg px-3 py-2 text-xs focus:outline-none resize-none h-16"
                                        />
                                        <div className="flex items-center justify-between flex-wrap gap-2">
                                            <div className="flex items-center gap-3">
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
                                                <label className="flex items-center gap-1.5 text-xs text-slate-400 cursor-pointer" title="El backend aún no tiene endpoint de subida de archivos: el adjunto no se envía todavía.">
                                                    <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15.172 7l-6.586 6.586a2 2 0 102.828 2.828l6.414-6.586a4 4 0 00-5.656-5.656l-6.415 6.585a6 6 0 108.486 8.486L20.5 13" />
                                                    </svg>
                                                    {adjunto ? adjunto.name : "Adjuntar (opcional)"}
                                                    <input type="file" className="hidden" onChange={e => setAdjunto(e.target.files?.[0] || null)} />
                                                </label>
                                            </div>
                                            <button
                                                onClick={enviarComentario}
                                                disabled={enviandoCom || !nuevoComentario.trim()}
                                                style={{ backgroundColor: PRIMARY }}
                                                className="px-3 py-1.5 text-xs text-white rounded-lg hover:opacity-90 transition disabled:opacity-40"
                                            >
                                                {enviandoCom ? "Enviando..." : "Responder"}
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
        </Layout>
    );
}