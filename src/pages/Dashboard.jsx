import { useState, useEffect, useCallback } from "react";
import { redirectToLogin } from "../utils/auth";
import { useNavigate } from "react-router-dom";
import axios from "axios";

const PRIMARY       = "#243A76";
const PRIMARY_LIGHT = "#2d4a96";

const SERVICIOS = [
    { nombre: "sga-principal",  url: "http://localhost:8080/actuator/health",     puerto: 8080, descripcion: "Sistema Principal" },
    { nombre: "sga-docente",    url: "http://localhost:8081/api/docente/",        puerto: 8081, descripcion: "Microservicio Docente (Django)" },
    { nombre: "sga-soporte",    url: "http://localhost:8082/actuator/health",     puerto: 8082, descripcion: "Soporte Técnico" },
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
    const username  = localStorage.getItem("username") || "";
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
        if (!token) { redirectToLogin(); return; }
        if (!esAdmin) { navigate("/soporte"); return; }
        verificarSalud();
        cargarAuditoria();
    }, []);

    // ── Salud de microservicios ───────────────────────────────
    const verificarSalud = useCallback(async () => {
        setLoadingSalud(true);
        const resultados = {};
        await Promise.all(
            SERVICIOS.map(async (s) => {
                const inicio = Date.now();
                try {
                    const r = await axios.get(s.url, { timeout: 4000, headers });
                    resultados[s.nombre] = {
                        estado: r.data?.status === "UP" ? "UP" : "DEGRADED",
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
        <div className="flex flex-col min-h-screen bg-slate-50">

            {/* TOPBAR */}
            <header style={{ backgroundColor: PRIMARY }} className="h-14 flex items-center justify-between px-4 shadow z-30 flex-shrink-0">
                <div className="flex items-center gap-3">
                    <div className="w-8 h-8 bg-white bg-opacity-20 rounded-full flex items-center justify-center">
                        <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                        </svg>
                    </div>
                    <span className="text-white font-bold text-sm">SGA</span>
                    <span className="text-white text-opacity-60 text-sm hidden sm:inline">| Monitoreo del Sistema</span>
                </div>
                <div className="flex items-center gap-2">
                    <button
                        onClick={() => navigate("/soporte")}
                        className="text-white text-opacity-70 hover:text-opacity-100 text-xs px-3 py-1.5 border border-white border-opacity-20 rounded-lg transition"
                    >
                        ← Soporte
                    </button>
                    <button
                        onClick={() => navigate("/usuarios")}
                        className="text-white text-opacity-70 hover:text-opacity-100 text-xs px-3 py-1.5 border border-white border-opacity-20 rounded-lg transition"
                    >
                        Usuarios
                    </button>
                    <div style={{ backgroundColor: PRIMARY_LIGHT }} className="flex items-center gap-2 px-3 py-1.5 rounded-lg">
                        <div className="w-7 h-7 bg-white bg-opacity-20 rounded-full flex items-center justify-center text-xs font-bold text-white uppercase">
                            {username.charAt(0)}
                        </div>
                        <span className="text-white text-xs font-medium hidden sm:inline capitalize">{username}</span>
                    </div>
                </div>
            </header>

            <main className="flex-1 p-4 max-w-7xl mx-auto w-full space-y-6">

                {/* ── SECCIÓN: ESTADO SERVICIOS ─────────────────────── */}
                <section>
                    <div className="flex items-center justify-between mb-3">
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
                            className="flex items-center gap-1.5 text-xs px-3 py-1.5 border border-slate-200 rounded-lg bg-white hover:bg-slate-50 transition text-slate-600 disabled:opacity-50"
                        >
                            <svg className={`w-3.5 h-3.5 ${loadingSalud ? "animate-spin" : ""}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                            </svg>
                            {loadingSalud ? "Verificando..." : "Actualizar"}
                        </button>
                    </div>

                    {/* Resumen */}
                    <div className="grid grid-cols-3 gap-3 mb-4">
                        <div className="bg-white rounded-xl border border-slate-200 p-4 text-center">
                            <p className="text-2xl font-bold text-slate-800">{totalServicios}</p>
                            <p className="text-xs text-slate-500 mt-0.5">Total servicios</p>
                        </div>
                        <div className="bg-white rounded-xl border border-green-200 p-4 text-center">
                            <p className="text-2xl font-bold text-green-600">{serviciosUP}</p>
                            <p className="text-xs text-slate-500 mt-0.5">Activos</p>
                        </div>
                        <div className="bg-white rounded-xl border border-red-200 p-4 text-center">
                            <p className="text-2xl font-bold text-red-600">{serviciosDOWN}</p>
                            <p className="text-xs text-slate-500 mt-0.5">Caídos</p>
                        </div>
                    </div>

                    {/* Tarjetas de servicios */}
                    <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                        {SERVICIOS.map(s => {
                            const info   = salud[s.nombre];
                            const estado = info?.estado || (loadingSalud ? "LOADING" : "UNKNOWN");
                            const c      = estadoColor(estado);

                            return (
                                <div key={s.nombre} className="bg-white rounded-xl border border-slate-200 p-4">
                                    <div className="flex items-start justify-between mb-3">
                                        <div>
                                            <p className="font-semibold text-slate-800 text-sm">{s.descripcion}</p>
                                            <p className="text-xs text-slate-400 font-mono">:{s.puerto}</p>
                                        </div>
                                        {loadingSalud ? (
                                            <div className="w-2.5 h-2.5 bg-slate-300 rounded-full animate-pulse mt-1" />
                                        ) : (
                                            <div className={`w-2.5 h-2.5 rounded-full mt-1 ${c.dot}`} />
                                        )}
                                    </div>

                                    <div className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold ${c.bg} ${c.text}`}>
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

                                    {/* Componentes internos si los hay */}
                                    {info?.detalle?.components && (
                                        <div className="mt-3 space-y-1">
                                            {Object.entries(info.detalle.components).map(([comp, val]) => (
                                                <div key={comp} className="flex items-center justify-between text-xs text-slate-500">
                                                    <span className="capitalize">{comp}</span>
                                                    <span className={val.status === "UP" ? "text-green-600" : "text-red-600"}>
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
                            className="flex items-center gap-1.5 text-xs px-3 py-1.5 border border-slate-200 rounded-lg bg-white hover:bg-slate-50 transition text-slate-600 disabled:opacity-50"
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
                            className="flex-1 border border-slate-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-100 bg-white"
                        />
                        <select
                            value={filtroAccion}
                            onChange={e => setFiltroAccion(e.target.value)}
                            className="border border-slate-200 rounded-lg px-3 py-2 text-sm focus:outline-none bg-white"
                        >
                            {accionesUnicas.map(a => (
                                <option key={a} value={a}>{a}</option>
                            ))}
                        </select>
                    </div>

                    {loadingAudit ? (
                        <div className="flex items-center justify-center h-32 text-slate-400 text-sm">
                            <svg className="w-5 h-5 mr-2 animate-spin" fill="none" viewBox="0 0 24 24">
                                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
                            </svg>
                            Cargando auditoría...
                        </div>
                    ) : auditoria.length === 0 ? (
                        <div className="bg-white rounded-xl border border-slate-200 p-8 text-center text-slate-400 text-sm">
                            <svg className="w-10 h-10 mx-auto mb-2 text-slate-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                            </svg>
                            No hay registros de auditoría disponibles.<br />
                            <span className="text-xs">Verifica que el endpoint <code className="bg-slate-100 px-1 rounded">/api/auditoria</code> esté disponible en sga-principal.</span>
                        </div>
                    ) : (
                        <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
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
            </main>

            {/* FOOTER */}
            <footer style={{ backgroundColor: PRIMARY }} className="text-white text-opacity-80 text-xs text-center py-2 flex-shrink-0">
                Sistema de Gestión Académica · Escuela Provincias Unidas © 2026
            </footer>
        </div>
    );
}