import { useState, useEffect } from "react";
import axios from "axios";
import Layout from "../components/Layout";

const PRIMARY       = "#243A76";
const PRIMARY_LIGHT = "#2d4a96";

const SERVICIOS_BASE = [
    { nombre: "sga-principal",   ruta: "/actuator/health", puerto: 8080, descripcion: "Sistema Principal" },
    { nombre: "sga-docente",     ruta: "/health",          puerto: 8081, descripcion: "Microservicio Docente (Django)" },
    { nombre: "sga-secretaria",  ruta: "/health",          puerto: 8082, descripcion: "Secretaría Académica" },
    { nombre: "sga-soporte",     ruta: "/actuator/health", puerto: 8083, descripcion: "Soporte Técnico" },
];

const getServicios = () => {
    const host = typeof window !== "undefined" ? window.location.hostname : "localhost";
    return SERVICIOS_BASE.map(s => ({ ...s, url: `http://${host}:${s.puerto}${s.ruta}` }));
};

const getFuentesLogs = () => {
    const host = typeof window !== "undefined" ? window.location.hostname : "localhost";
    return [
        { nombre: "sga-soporte",    descripcion: "Soporte Técnico",              disponible: true,  url: `http://${host}:8083/api/soporte/logs?limite=30` },
        { nombre: "sga-principal",  descripcion: "Sistema Principal",            disponible: false, url: null },
        { nombre: "sga-docente",    descripcion: "Microservicio Docente",        disponible: false, url: null },
        { nombre: "sga-secretaria", descripcion: "Secretaría Académica",         disponible: false, url: null },
    ];
};

const nivelBadge = (nivel) => {
    if (nivel === "ERROR") return "bg-red-100 text-red-700";
    if (nivel === "WARN")  return "bg-yellow-100 text-yellow-700";
    return "bg-slate-100 text-slate-500";
};

const accionBadge = (accion) => {
    if (!accion) return "bg-slate-100 text-slate-500";
    const a = accion.toUpperCase();
    if (a.includes("CREAR") || a.includes("REGISTRAR") || a.includes("NUEVO")) return "bg-emerald-100 text-emerald-700";
    if (a.includes("EDITAR") || a.includes("ACTUALIZAR") || a.includes("CAMBIAR")) return "bg-blue-100 text-blue-700";
    if (a.includes("ELIMINAR") || a.includes("BAJA")) return "bg-red-100 text-red-700";
    if (a.includes("LOGIN") || a.includes("AUTENTICAR")) return "bg-purple-100 text-purple-700";
    return "bg-slate-100 text-slate-600";
};

export default function Dashboard() {
    const token = localStorage.getItem("token") || localStorage.getItem("sga_soporte_token") || "";
    const username = localStorage.getItem("username") || "";
    
    let roles = [];
    try {
        const raw = localStorage.getItem("roles");
        if (raw) {
            roles = Array.isArray(JSON.parse(raw)) ? JSON.parse(raw) : [raw];
        }
    } catch {
        roles = [];
    }
    const esDirector = roles.includes("DIRECTOR") || roles.includes("ADMINISTRADOR");

    // ── Resumen de tickets ────────────────────────────────────
    const [tickets,       setTickets]       = useState([]);
    const [loadingData,   setLoadingData]   = useState(true);

    // ── Salud de microservicios ───────────────────────────────
    const [salud,         setSalud]         = useState({});
    const [loadingSalud,  setLoadingSalud]  = useState(true);

    // ── Auditoría (solo DIRECTOR) ─────────────────────────────
    const [auditoria,     setAuditoria]     = useState([]);
    const [loadingAudit,  setLoadingAudit]  = useState(false);

    // ── Logs de fallos (solo DIRECTOR) ────────────────────────
    const [fuenteLogsSel, setFuenteLogsSel] = useState("sga-soporte");
    const [logs,          setLogs]          = useState([]);
    const [loadingLogs,   setLoadingLogs]   = useState(false);

    const headers = token ? { Authorization: `Bearer ${token}` } : {};
    const host = typeof window !== "undefined" ? window.location.hostname : "localhost";

    // ── Carga de tickets ──────────────────────────────────────
    const cargarTickets = () => {
        setLoadingData(true);
        axios.get(`http://${host}:8083/api/soporte/tickets`, { headers })
            .then(r => setTickets(r.data))
            .catch(() => {
                axios.get("/api/soporte/tickets", { headers })
                    .then(r => setTickets(r.data))
                    .catch(() => setTickets([]))
                    .finally(() => setLoadingData(false));
            })
            .finally(() => setLoadingData(false));
    };

    // ── Chequeo de salud ──────────────────────────────────────
    const chequearSalud = async () => {
        setLoadingSalud(true);
        const resultados = {};
        const servicios = getServicios();
        await Promise.all(
            servicios.map(async (s) => {
                const inicio = Date.now();
                try {
                    const res = await fetch(s.url, { method: "GET" });
                    const ms  = Date.now() - inicio;
                    if (res.ok) {
                        resultados[s.nombre] = { ok: true, ms, status: res.status };
                    } else {
                        resultados[s.nombre] = { ok: false, ms, status: res.status, error: `HTTP ${res.status}` };
                    }
                } catch (err) {
                    const ms = Date.now() - inicio;
                    resultados[s.nombre] = { ok: false, ms, status: 0, error: err.message || "Sin conexión" };
                }
            })
        );
        setSalud(resultados);
        setLoadingSalud(false);
    };

    // ── Log de auditoría ──────────────────────────────────────
    const cargarAuditoria = () => {
        setLoadingAudit(true);
        const host = typeof window !== "undefined" ? window.location.hostname : "localhost";
        axios.get(`http://${host}:8080/api/auditoria`, { headers })
            .then(r => setAuditoria(r.data))
            .catch(() => setAuditoria([]))
            .finally(() => setLoadingAudit(false));
    };

    // ── Logs de fallos ────────────────────────────────────────
    const cargarLogs = (fuenteNombre) => {
        const fuentes = getFuentesLogs();
        const f = fuentes.find(x => x.nombre === fuenteNombre);
        if (!f || !f.disponible || !f.url) {
            setLogs([]);
            return;
        }
        setLoadingLogs(true);
        axios.get(f.url, { headers })
            .then(r => setLogs(r.data))
            .catch(() => setLogs([]))
            .finally(() => setLoadingLogs(false));
    };

    useEffect(() => {
        cargarTickets();
        chequearSalud();
        if (esDirector) {
            cargarAuditoria();
            cargarLogs(fuenteLogsSel);
        }
    }, []);

    const handleFuenteLogsChange = (e) => {
        const val = e.target.value;
        setFuenteLogsSel(val);
        cargarLogs(val);
    };

    // Métricas calculadas
    const totalTickets     = tickets.length;
    const abiertos         = tickets.filter(t => t.estado === "ABIERTO").length;
    const enProceso        = tickets.filter(t => t.estado === "EN_PROCESO").length;
    const resueltos        = tickets.filter(t => t.estado === "RESUELTO" || t.estado === "CERRADO").length;

    const ultimosAudit     = auditoria.slice(0, 10);
    const fuentesLogs      = getFuentesLogs();
    const fuenteActualObj  = fuentesLogs.find(x => x.nombre === fuenteLogsSel);

    return (
        <Layout breadcrumb={["Inicio", "Dashboard"]} sidebarTitle="Dashboard" seccion="dashboard">
            <div className="space-y-6">

                {/* ── Encabezado principal ────────────────────── */}
                <div className="flex items-center justify-between gap-4 flex-wrap">
                    <div>
                        <h1 className="text-xl font-bold text-slate-800">Panel de Control de Soporte</h1>
                        <p className="text-xs text-slate-400 mt-0.5">
                            Monitoreo de servicios, métricas de tickets {esDirector && "y trazabilidad de eventos"}
                        </p>
                    </div>
                    <div className="flex items-center gap-2">
                        <button
                            onClick={() => { cargarTickets(); chequearSalud(); if (esDirector) { cargarAuditoria(); cargarLogs(fuenteLogsSel); } }}
                            className="flex items-center gap-1.5 text-xs text-slate-600 hover:text-slate-900 bg-white border border-slate-200 px-3 py-1.5 rounded-lg shadow-xs hover:border-slate-300 transition"
                        >
                            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                            </svg>
                            Actualizar todo
                        </button>
                    </div>
                </div>

                {/* ── 1. Tarjetas de Salud de Microservicios ── */}
                <div>
                    <div className="flex items-center justify-between mb-3">
                        <h2 className="text-sm font-bold text-slate-700 uppercase tracking-wider flex items-center gap-2">
                            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
                            Estado de la Red de Microservicios
                        </h2>
                        {loadingSalud && <span className="text-xs text-slate-400">Verificando salud...</span>}
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
                        {getServicios().map(s => {
                            const info  = salud[s.nombre];
                            const ok    = info?.ok;
                            const ms    = info?.ms;
                            const error = info?.error;

                            return (
                                <div
                                    key={s.nombre}
                                    className={`bg-white border rounded-xl p-4 shadow-xs flex flex-col justify-between transition-all ${
                                        loadingSalud
                                            ? "border-slate-200"
                                            : ok
                                            ? "border-emerald-200 hover:border-emerald-300"
                                            : "border-red-200 hover:border-red-300 bg-red-50/20"
                                    }`}
                                >
                                    <div className="flex items-start justify-between">
                                        <div>
                                            <p className="text-xs font-bold text-slate-800">{s.nombre}</p>
                                            <p className="text-[10px] text-slate-400">{s.descripcion}</p>
                                        </div>
                                        <span
                                            className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                                                loadingSalud
                                                    ? "bg-slate-100 text-slate-400"
                                                    : ok
                                                    ? "bg-emerald-100 text-emerald-700"
                                                    : "bg-red-100 text-red-700"
                                            }`}
                                        >
                                            {loadingSalud ? "Cargando..." : ok ? "Online" : "Offline"}
                                        </span>
                                    </div>

                                    <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between text-[11px]">
                                        <span className="text-slate-400 font-mono">Puerto {s.puerto}</span>
                                        <span className={`font-mono font-semibold ${ok ? "text-emerald-600" : "text-red-500"}`}>
                                            {loadingSalud ? "..." : ok ? `${ms} ms` : error}
                                        </span>
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                </div>

                {/* ── 2. Métricas de Tickets de Soporte ────── */}
                <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
                    <div className="bg-white border border-slate-200 rounded-xl p-4 shadow-xs">
                        <p className="text-xs font-medium text-slate-400">Total de Tickets</p>
                        <p className="text-2xl font-bold text-slate-800 mt-1">{loadingData ? "..." : totalTickets}</p>
                    </div>

                    <div className="bg-white border border-amber-200 rounded-xl p-4 shadow-xs bg-amber-50/20">
                        <p className="text-xs font-medium text-amber-700">Abiertos</p>
                        <p className="text-2xl font-bold text-amber-800 mt-1">{loadingData ? "..." : abiertos}</p>
                    </div>

                    <div className="bg-white border border-blue-200 rounded-xl p-4 shadow-xs bg-blue-50/20">
                        <p className="text-xs font-medium text-blue-700">En Proceso</p>
                        <p className="text-2xl font-bold text-blue-800 mt-1">{loadingData ? "..." : enProceso}</p>
                    </div>

                    <div className="bg-white border border-emerald-200 rounded-xl p-4 shadow-xs bg-emerald-50/20">
                        <p className="text-xs font-medium text-emerald-700">Resueltos</p>
                        <p className="text-2xl font-bold text-emerald-800 mt-1">{loadingData ? "..." : resueltos}</p>
                    </div>
                </div>

                {/* ── 3. Secciones Exclusivas de DIRECTOR ──── */}
                {esDirector && (
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">

                        {/* ── 3A. Panel de Logs de Fallos (Exclusivo DIRECTOR) ── */}
                        <div className="bg-white border border-slate-200 rounded-xl shadow-xs p-4 flex flex-col justify-between">
                            <div>
                                <div className="flex items-center justify-between mb-3 flex-wrap gap-2">
                                    <div>
                                        <h2 className="text-sm font-bold text-slate-800">Logs de Fallos y Errores</h2>
                                        <p className="text-[11px] text-slate-400">Excepciones recientes registradas por el servicio</p>
                                    </div>
                                    <select
                                        value={fuenteLogsSel}
                                        onChange={handleFuenteLogsChange}
                                        className="text-xs border border-slate-200 rounded-lg px-2.5 py-1 text-slate-700 font-medium focus:outline-none focus:ring-1 focus:ring-blue-500"
                                    >
                                        {fuentesLogs.map(f => (
                                            <option key={f.nombre} value={f.nombre}>
                                                {f.nombre} {!f.disponible && "(Próximamente)"}
                                            </option>
                                        ))}
                                    </select>
                                </div>

                                {!fuenteActualObj?.disponible ? (
                                    <div className="py-12 text-center text-xs text-slate-400 bg-slate-50 rounded-lg border border-dashed border-slate-200">
                                        <p className="font-semibold text-slate-500">Monitoreo de logs en desarrollo</p>
                                        <p className="mt-1 text-[11px]">
                                            El endpoint <code className="bg-slate-200 text-slate-700 px-1 py-0.5 rounded">/api/soporte/logs</code> estará disponible en la próxima actualización de este microservicio.
                                        </p>
                                    </div>
                                ) : loadingLogs ? (
                                    <div className="py-12 text-center text-xs text-slate-400">Cargando eventos de log...</div>
                                ) : logs.length === 0 ? (
                                    <div className="py-12 text-center text-xs text-slate-400 bg-emerald-50/30 rounded-lg border border-emerald-100">
                                        <p className="font-semibold text-emerald-700">Sin errores recientes</p>
                                        <p className="mt-0.5 text-[11px] text-emerald-600">No se han registrado excepciones en este microservicio.</p>
                                    </div>
                                ) : (
                                    <div className="space-y-2 max-h-80 overflow-y-auto pr-1">
                                        {logs.map((l, i) => (
                                            <div key={l.id || i} className="p-2.5 bg-slate-50 rounded-lg border border-slate-100 text-xs flex flex-col gap-1">
                                                <div className="flex items-center justify-between">
                                                    <span className={`text-[10px] font-bold px-1.5 py-0.5 rounded ${nivelBadge(l.nivel)}`}>
                                                        {l.nivel || "INFO"}
                                                    </span>
                                                    <span className="text-[10px] font-mono text-slate-400">{l.timestamp || l.fecha}</span>
                                                </div>
                                                <p className="font-mono text-[11px] text-slate-800 break-words font-semibold">{l.mensaje}</p>
                                                {l.logger && <p className="text-[10px] text-slate-400 font-mono truncate">{l.logger}</p>}
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>

                            <div className="pt-3 mt-3 border-t border-slate-100 text-right">
                                <button
                                    onClick={() => cargarLogs(fuenteLogsSel)}
                                    disabled={!fuenteActualObj?.disponible || loadingLogs}
                                    className="text-xs text-blue-700 font-semibold hover:underline disabled:opacity-40 disabled:no-underline"
                                >
                                    Refrescar logs
                                </button>
                            </div>
                        </div>

                        {/* ── 3B. Registro de Auditoría de Eventos (Exclusivo DIRECTOR) ── */}
                        <div className="bg-white border border-slate-200 rounded-xl shadow-xs p-4 flex flex-col justify-between">
                            <div>
                                <div className="flex items-center justify-between mb-3">
                                    <div>
                                        <h2 className="text-sm font-bold text-slate-800">Trazabilidad de Eventos</h2>
                                        <p className="text-[11px] text-slate-400">Últimas acciones registradas en el clúster</p>
                                    </div>
                                    <span className="text-[10px] font-bold bg-blue-50 text-blue-700 border border-blue-100 px-2 py-0.5 rounded-full">
                                        DIRECTOR
                                    </span>
                                </div>

                                {loadingAudit ? (
                                    <div className="py-12 text-center text-xs text-slate-400">Cargando eventos de auditoría...</div>
                                ) : ultimosAudit.length === 0 ? (
                                    <div className="py-12 text-center text-xs text-slate-400 bg-slate-50 rounded-lg border border-dashed border-slate-200">
                                        No hay registros de auditoría disponibles.
                                    </div>
                                ) : (
                                    <div className="space-y-2 max-h-80 overflow-y-auto pr-1">
                                        {ultimosAudit.map((a) => (
                                            <div key={a.idAuditoria || a.id} className="p-2.5 bg-slate-50 rounded-lg border border-slate-100 text-xs flex items-center justify-between gap-2">
                                                <div className="space-y-0.5">
                                                    <div className="flex items-center gap-2">
                                                        <span className={`text-[9px] font-bold px-1.5 py-0.2 rounded ${accionBadge(a.accion)}`}>
                                                            {a.accion}
                                                        </span>
                                                        <span className="font-bold text-slate-800">{a.usuario || "Sistema"}</span>
                                                    </div>
                                                    <p className="text-[11px] text-slate-600 truncate max-w-xs">{a.descripcion || a.detalle || "—"}</p>
                                                </div>
                                                <span className="text-[10px] text-slate-400 font-mono whitespace-nowrap">{a.fecha || a.timestamp}</span>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>

                            <div className="pt-3 mt-3 border-t border-slate-100 text-right">
                                <button
                                    onClick={cargarAuditoria}
                                    disabled={loadingAudit}
                                    className="text-xs text-blue-700 font-semibold hover:underline"
                                >
                                    Refrescar auditoría
                                </button>
                            </div>
                        </div>

                    </div>
                )}

            </div>
        </Layout>
    );
}