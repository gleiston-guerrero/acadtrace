import { useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import axios from "axios";
import Layout from "../components/Layout";

const PRIMARY       = "#243A76";
const PRIMARY_LIGHT = "#2d4a96";

const SERVICIOS = [
    { nombre: "sga-principal",  url: "http://localhost:8080/actuator/health",     puerto: 8080, descripcion: "Sistema Principal" },
    { nombre: "sga-docente",    url: "http://localhost:8081/health",              puerto: 8081, descripcion: "Microservicio Docente (Django)" },
    { nombre: "sga-soporte",    url: "http://localhost:8083/actuator/health",     puerto: 8083, descripcion: "Soporte Técnico" },
];

const accionBadge = (accion) => {
    if (!accion) return "bg-slate-100 text-slate-500";
    const a = accion.toUpperCase();
    if (a.includes("CREATE") || a.includes("CREAR"))  return "bg-green-100 text-green-700";
    if (a.includes("UPDATE") || a.includes("EDITAR") || a.includes("ACTUALIZAR")) return "bg-blue-100 text-blue-700";
    if (a.includes("DELETE") || a.includes("ELIMINAR")) return "bg-red-100 text-red-700";
    if (a.includes("LOGIN"))  return "bg-purple-100 text-purple-700";
    if (a.includes("LOGOUT")) return "bg-slate-100 text-slate-500";
    if (a.includes("RESET"))  return "bg-orange-100 text-orange-700";
    return "bg-slate-100 text-slate-500";
};

const formatFecha = (iso) => {
    if (!iso) return "—";
    return new Date(iso).toLocaleString("es-EC", {
        day: "2-digit", month: "short", year: "numeric",
        hour: "2-digit", minute: "2-digit",
    });
};

export default function Dashboard() {
    const navigate  = useNavigate();
    const token     = localStorage.getItem("token");
    const roles     = JSON.parse(localStorage.getItem("roles") || "[]");
    const headers   = { Authorization: `Bearer ${token}` };

    const esAdmin = roles.includes("SOPORTE_TECNICO") || roles.includes("ADMINISTRADOR");

    const [salud,        setSalud]        = useState({});
    const [loadingSalud, setLoadingSalud] = useState(true);
    const [lastCheck,    setLastCheck]    = useState(null);

    const [auditoria,    setAuditoria]    = useState([]);
    const [loadingAudit, setLoadingAudit] = useState(true);
    const [filtroBusq,   setFiltroBusq]   = useState("");
    const [filtroAccion, setFiltroAccion] = useState("TODAS");

    useEffect(() => {
        if (!token) { window.location.href = "http://localhost:5173/login"; return; }
        if (!esAdmin) { navigate("/soporte"); return; }
        verificarSalud();
        cargarAuditoria();
    }, []);

    // ── Salud de microservicios (SIN Authorization header) ───
    const verificarSalud = useCallback(async () => {
        setLoadingSalud(true);
        const resultados = {};
        await Promise.all(
            SERVICIOS.map(async (s) => {
                const inicio = Date.now();
                try {
                    // Los healthchecks son públicos, NO enviamos Authorization header para evitar fallos de preflight CORS
                    const r = await axios.get(s.url, { timeout: 4000 });
                    const isUp = r.data?.status === "UP" || r.data?.status === "active" || r.status === 200;
                    resultados[s.nombre] = {
                        estado: isUp ? "UP" : "DEGRADED",
                        latencia: Date.now() - inicio,
                        detalle: r.data,
                    };
                } catch {
                    resultados[s.nombre] = {
                        estado: "DOWN",
                        latencia: null,
                        detalle: null,
                    };
                }
            })
        );
        setSalud(resultados);
        setLastCheck(new Date());
        setLoadingSalud(false);
    }, []);

    // Auto-refresh cada 30 segundos
    useEffect(() => {
        const interval = setInterval(verificarSalud, 30000);
        return () => clearInterval(interval);
    }, [verificarSalud]);

    // ── Log de auditoría ──────────────────────────────────────
    const cargarAuditoria = () => {
        setLoadingAudit(true);
        axios.get("http://localhost:8080/api/auditoria", { headers })
            .then(r => setAuditoria(r.data))
            .catch(() => setAuditoria([]))
            .finally(() => setLoadingAudit(false));
    };

    const accionesUnicas = ["TODAS", ...new Set(auditoria.map(a => a.accion).filter(Boolean))];

    const auditoriaFiltrada = auditoria.filter(a => {
        const matchAccion = filtroAccion === "TODAS" || a.accion === filtroAccion;
        const q = filtroBusq.toLowerCase();
        const matchQ =
            a.usuario?.toLowerCase().includes(q) ||
            a.accion?.toLowerCase().includes(q)  ||
            a.detalle?.toLowerCase().includes(q) ||
            a.ip?.toLowerCase().includes(q);
        return matchAccion && matchQ;
    });

    // ── Resumen salud ─────────────────────────────────────────
    const totalServicios = SERVICIOS.length;
    const serviciosUP    = Object.values(salud).filter(s => s.estado === "UP").length;
    const serviciosDOWN  = Object.values(salud).filter(s => s.estado === "DOWN").length;

    const estadoColor = (estado) => {
        if (estado === "UP")      return { bg: "bg-green-100",  text: "text-green-700",  dot: "bg-green-500" };
        if (estado === "DEGRADED") return { bg: "bg-yellow-100", text: "text-yellow-700", dot: "bg-yellow-500" };
        if (estado === "DOWN")     return { bg: "bg-red-100",    text: "text-red-700",    dot: "bg-red-500" };
        return { bg: "bg-slate-100", text: "text-slate-500", dot: "bg-slate-400" };
    };

    return (
        <Layout breadcrumb={["Inicio", "Monitoreo del sistema"]}>
            <div className="space-y-6">

                {/* ── SECCIÓN: ACCESOS RÁPIDOS ──────────────────────── */}
                <section>
                    <div className="mb-4">
                        <h2 className="text-base font-bold text-slate-800">Accesos rápidos</h2>
                        <p className="text-xs text-slate-400">Ir directo a una sección del módulo</p>
                    </div>
                    <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4">
                        {[
                            {
                                label: "Tickets",
                                desc: "Gestión de tickets de soporte",
                                color: "bg-amber-50",
                                iconColor: "text-amber-500",
                                path: "/soporte",
                                icon: (
                                    <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 5v2m0 4v2m0 4v2M5 5a2 2 0 00-2 2v3a2 2 0 002 2h14a2 2 0 002-2V7a2 2 0 00-2-2H5zM5 14a2 2 0 00-2 2v3a2 2 0 002 2h14a2 2 0 002-2v-3a2 2 0 00-2-2H5z" />
                                    </svg>
                                ),
                            },
                            {
                                label: "Reportes",
                                desc: "Historial y estadísticas",
                                color: "bg-rose-50",
                                iconColor: "text-rose-500",
                                path: "/reportes",
                                icon: (
                                    <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                                    </svg>
                                ),
                            },
                            {
                                label: "Usuarios",
                                desc: "Técnicos y usuarios del sistema",
                                color: "bg-purple-50",
                                iconColor: "text-purple-500",
                                path: "/usuarios",
                                icon: (
                                    <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
                                    </svg>
                                ),
                            },
                        ].map((m) => (
                            <button
                                key={m.label}
                                onClick={() => navigate(m.path)}
                                className="bg-white border border-slate-200 rounded-xl p-5 flex flex-col items-center gap-3 hover:shadow-md transition-all group text-center"
                                onMouseEnter={(e) => (e.currentTarget.style.borderColor = PRIMARY)}
                                onMouseLeave={(e) => (e.currentTarget.style.borderColor = "")}
                            >
                                <div className={`${m.color} ${m.iconColor} p-3 rounded-xl`}>
                                    {m.icon}
                                </div>
                                <div>
                                    <p className="text-sm font-semibold text-slate-700 group-hover:text-[#243A76] transition">{m.label}</p>
                                    <p className="text-xs text-slate-400 mt-0.5 leading-tight">{m.desc}</p>
                                </div>
                            </button>
                        ))}
                    </div>
                </section>

                {/* ── SECCIÓN: ESTADO SERVICIOS ─────────────────────── */}
                <section>
                    <div className="flex items-center justify-between mb-4">
                        <div>
                            <h2 className="text-base font-bold text-slate-800">Estado de microservicios</h2>
                            <p className="text-xs text-slate-400">
                                {lastCheck
                                    ? `Última verificación: ${lastCheck.toLocaleTimeString("es-EC")}`
                                    : "Verificando..."}
                            </p>
                        </div>
                        <button
                            onClick={verificarSalud}
                            disabled={loadingSalud}
                            className="flex items-center gap-1.5 text-xs px-3 py-1.5 border border-slate-200 rounded-xl bg-white hover:bg-slate-50 transition text-slate-600 disabled:opacity-50 shadow-sm"
                        >
                            <svg className={`w-3.5 h-3.5 ${loadingSalud ? "animate-spin" : ""}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                            </svg>
                            {loadingSalud ? "Verificando..." : "Actualizar"}
                        </button>
                    </div>

                    {/* Resumen Cards (rounded-2xl, sombra suave, ícono a la izquierda + número grande + label debajo) */}
                    <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
                        <div className="bg-white border border-slate-200 rounded-2xl p-5 flex items-center gap-4 shadow-sm hover:shadow-md transition">
                            <div className="w-12 h-12 rounded-xl bg-blue-50 text-[#243A76] flex items-center justify-center flex-shrink-0">
                                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M5 12h14M12 5l7 7-7 7" />
                                </svg>
                            </div>
                            <div>
                                <p className="text-2xl font-extrabold text-slate-800">{totalServicios}</p>
                                <p className="text-xs font-medium text-slate-400 mt-0.5">Total de Servicios</p>
                            </div>
                        </div>

                        <div className="bg-white border border-slate-200 rounded-2xl p-5 flex items-center gap-4 shadow-sm hover:shadow-md transition">
                            <div className="w-12 h-12 rounded-xl bg-green-50 text-green-600 flex items-center justify-center flex-shrink-0">
                                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                                </svg>
                            </div>
                            <div>
                                <p className="text-2xl font-extrabold text-green-600">{serviciosUP}</p>
                                <p className="text-xs font-medium text-slate-400 mt-0.5">Servicios Activos</p>
                            </div>
                        </div>

                        <div className="bg-white border border-slate-200 rounded-2xl p-5 flex items-center gap-4 shadow-sm hover:shadow-md transition">
                            <div className="w-12 h-12 rounded-xl bg-red-50 text-red-600 flex items-center justify-center flex-shrink-0">
                                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                                </svg>
                            </div>
                            <div>
                                <p className="text-2xl font-extrabold text-red-600">{serviciosDOWN}</p>
                                <p className="text-xs font-medium text-slate-400 mt-0.5">Servicios Caídos</p>
                            </div>
                        </div>
                    </div>

                    {/* Tarjetas de microservicios (rounded-2xl) */}
                    <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                        {SERVICIOS.map(s => {
                            const info   = salud[s.nombre];
                            const estado = info?.estado || (loadingSalud ? "LOADING" : "UNKNOWN");
                            const c      = estadoColor(estado);

                            return (
                                <div key={s.nombre} className="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm hover:shadow-md transition">
                                    <div className="flex items-start justify-between mb-3">
                                        <div>
                                            <p className="font-bold text-slate-800 text-sm">{s.descripcion}</p>
                                            <p className="text-xs text-slate-400 font-mono">Puerto :{s.puerto}</p>
                                        </div>
                                        {loadingSalud ? (
                                            <div className="w-3 h-3 bg-slate-300 rounded-full animate-pulse mt-1" />
                                        ) : (
                                            <div className={`w-3 h-3 rounded-full mt-1 ${c.dot}`} />
                                        )}
                                    </div>

                                    <div className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold ${c.bg} ${c.text}`}>
                                        {loadingSalud ? (
                                            <span>Verificando…</span>
                                        ) : (
                                            <>
                                                <span>{estado}</span>
                                                {info?.latencia != null && (
                                                    <span className="opacity-60">· {info.latencia}ms</span>
                                                )}
                                            </>
                                        )}
                                    </div>

                                    {/* Componentes internos Spring Boot Actuator */}
                                    {info?.detalle?.components && (
                                        <div className="mt-4 pt-3 border-t border-slate-100 space-y-1.5">
                                            {Object.entries(info.detalle.components).map(([comp, val]) => (
                                                <div key={comp} className="flex items-center justify-between text-xs text-slate-500">
                                                    <span className="capitalize">{comp}</span>
                                                    <span className={`font-semibold ${val.status === "UP" ? "text-green-600" : "text-red-600"}`}>
                                                        {val.status}
                                                    </span>
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                </div>
                            );
                        })}
                    </div>
                </section>

                {/* ── SECCIÓN: LOG DE AUDITORÍA ─────────────────────── */}
                <section>
                    <div className="flex items-center justify-between mb-3">
                        <div>
                            <h2 className="text-base font-bold text-slate-800">Log de auditoría</h2>
                            <p className="text-xs text-slate-400">Acciones críticas del sistema</p>
                        </div>
                        <button
                            onClick={cargarAuditoria}
                            disabled={loadingAudit}
                            className="flex items-center gap-1.5 text-xs px-3 py-1.5 border border-slate-200 rounded-xl bg-white hover:bg-slate-50 transition text-slate-600 disabled:opacity-50 shadow-sm"
                        >
                            <svg className={`w-3.5 h-3.5 ${loadingAudit ? "animate-spin" : ""}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                            </svg>
                            {loadingAudit ? "Cargando..." : "Actualizar"}
                        </button>
                    </div>

                    {/* Filtros auditoría */}
                    <div className="flex gap-2 mb-3">
                        <input
                            type="text"
                            placeholder="Buscar por usuario, acción, IP..."
                            value={filtroBusq}
                            onChange={e => setFiltroBusq(e.target.value)}
                            className="flex-1 border border-slate-200 rounded-xl px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-100 bg-white"
                        />
                        <select
                            value={filtroAccion}
                            onChange={e => setFiltroAccion(e.target.value)}
                            className="border border-slate-200 rounded-xl px-3 py-2 text-sm focus:outline-none bg-white text-slate-700"
                        >
                            {accionesUnicas.map(a => (
                                <option key={a} value={a}>{a}</option>
                            ))}
                        </select>
                    </div>

                    {loadingAudit ? (
                        <div className="flex items-center justify-center h-32 text-slate-400 text-sm bg-white rounded-2xl border border-slate-200">
                            <svg className="w-5 h-5 mr-2 animate-spin text-[#243A76]" fill="none" viewBox="0 0 24 24">
                                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
                            </svg>
                            Cargando auditoría...
                        </div>
                    ) : auditoria.length === 0 ? (
                        <div className="bg-white rounded-2xl border border-slate-200 p-8 text-center text-slate-400 text-sm shadow-sm">
                            <svg className="w-10 h-10 mx-auto mb-2 text-slate-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                            </svg>
                            No hay registros de auditoría disponibles.<br />
                            <span className="text-xs">Verifica que el endpoint <code className="bg-slate-100 px-1 rounded">/api/auditoria</code> esté disponible en sga-principal.</span>
                        </div>
                    ) : (
                        <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-sm">
                            <div className="overflow-x-auto">
                                <table className="w-full text-sm">
                                    <thead>
                                    <tr className="border-b border-slate-100 bg-slate-50">
                                        <th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide">Fecha</th>
                                        <th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide">Usuario</th>
                                        <th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide">Acción</th>
                                        <th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide">Detalle</th>
                                        <th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide">IP</th>
                                    </tr>
                                    </thead>
                                    <tbody className="divide-y divide-slate-50">
                                    {auditoriaFiltrada.map((a, i) => (
                                        <tr key={a.id || i} className="hover:bg-slate-50 transition">
                                            <td className="px-4 py-3 text-xs text-slate-400 whitespace-nowrap">
                                                {formatFecha(a.fechaCreacion || a.fecha || a.timestamp)}
                                            </td>
                                            <td className="px-4 py-3">
                                                <span className="text-xs font-mono font-medium text-slate-700">{a.usuario || a.username || "—"}</span>
                                            </td>
                                            <td className="px-4 py-3">
                                                <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${accionBadge(a.accion)}`}>
                                                    {a.accion || "—"}
                                                </span>
                                            </td>
                                            <td className="px-4 py-3 text-xs text-slate-600 max-w-xs truncate">
                                                {a.detalle || a.descripcion || a.recurso || "—"}
                                            </td>
                                            <td className="px-4 py-3 text-xs text-slate-400 font-mono">
                                                {a.ip || a.ipOrigen || "—"}
                                            </td>
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            </div>
                            {auditoriaFiltrada.length === 0 && (
                                <div className="text-center py-8 text-slate-400 text-xs">
                                    No hay registros que coincidan con el filtro.
                                </div>
                            )}
                        </div>
                    )}
                </section>
            </div>
        </Layout>
    );
}