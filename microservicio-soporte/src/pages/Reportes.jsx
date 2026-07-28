import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import axios from "axios";

const API = "/api/soporte";
const PRIMARY = "#243A76";
const PRIMARY_LIGHT = "#2d4a96";

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

const formatHoras = (h) => {
    if (h === null || h === undefined) return "—";
    if (h < 1) return `${Math.round(h * 60)} min`;
    if (h < 48) return `${h} h`;
    return `${(h / 24).toFixed(1)} días`;
};

const Barra = ({ label, valor, max, color, sufijo }) => {
    const pct = max > 0 ? Math.max((valor / max) * 100, 3) : 0;
    return (
        <div className="flex items-center gap-3">
            <span className="text-xs text-slate-600 w-28 truncate" title={label}>{label}</span>
            <div className="flex-1 h-3 bg-slate-100 rounded-full overflow-hidden">
                <div className={`h-full rounded-full ${color}`} style={{ width: `${pct}%` }} />
            </div>
            <span className="text-xs font-semibold text-slate-700 w-16 text-right">
                {valor}{sufijo || ""}
            </span>
        </div>
    );
};

export default function Reportes() {
    const [data, setData]       = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError]     = useState("");

    const navigate  = useNavigate();
    const token     = localStorage.getItem("token");
    const username  = localStorage.getItem("username") || "";
    const roles     = JSON.parse(localStorage.getItem("roles") || "[]");
    const esTecnico = roles.includes("SOPORTE_TECNICO") || roles.includes("ADMINISTRADOR");
    const headers   = { Authorization: `Bearer ${token}` };

    useEffect(() => {
        if (!token) { window.location.href = "http://localhost:5173/login"; return; }
        if (!esTecnico) { navigate("/soporte"); return; }
        axios.get(`${API}/tickets/reportes`, { headers })
            .then(r => setData(r.data))
            .catch(() => setError("No se pudieron cargar los reportes"))
            .finally(() => setLoading(false));
    }, []);

    const handleLogout = () => {
        localStorage.clear();
        window.location.href = "http://localhost:5173/login";
    };

    const maxCategoria = data ? Math.max(...data.porCategoria.map(c => c.total), 1) : 1;
    const maxTecnico   = data ? Math.max(...data.porTecnico.map(t => t.total), 1) : 1;

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
                    <span className="text-white text-opacity-60 text-sm hidden sm:inline">| Reportes de Soporte</span>
                </div>
                <div className="flex items-center gap-2">
                    <div style={{ backgroundColor: PRIMARY_LIGHT }} className="flex items-center gap-2 px-3 py-1.5 rounded-lg">
                        <div className="w-7 h-7 bg-white bg-opacity-20 rounded-full flex items-center justify-center text-xs font-bold text-white uppercase">
                            {username.charAt(0)}
                        </div>
                        <span className="text-white text-xs font-medium hidden sm:inline capitalize">{username}</span>
                    </div>
                    <button
                        onClick={() => navigate("/soporte")}
                        className="text-white text-opacity-70 hover:text-opacity-100 text-xs px-3 py-1.5 border border-white border-opacity-20 rounded-lg transition hidden sm:flex items-center gap-1.5"
                    >
                        <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 17l-5-5m0 0l5-5m-5 5h12" />
                        </svg>
                        Tickets
                    </button>
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
                    <span className="hover:underline cursor-pointer" onClick={() => navigate("/soporte")}>Soporte Técnico</span>
                    <span className="text-slate-300">/</span>
                    <span style={{ color: PRIMARY }} className="font-medium">Reportes</span>
                </nav>
            </div>

            {/* CONTENIDO */}
            <main className="flex-1 p-6">
                {error && (
                    <div className="mb-4 bg-red-50 border border-red-200 rounded-lg px-4 py-3 flex items-center justify-between">
                        <span className="text-red-600 text-sm">{error}</span>
                        <button onClick={() => setError("")} className="text-red-400 hover:text-red-600">✕</button>
                    </div>
                )}

                <div className="mb-5">
                    <h1 className="text-lg font-bold text-slate-700">Reportes</h1>
                    <p className="text-xs text-slate-400">Tickets por categoría, por técnico y tiempos de resolución</p>
                </div>

                {loading ? (
                    <div className="bg-white rounded-xl border border-slate-200 p-12 text-center text-slate-400 text-sm">
                        Cargando reportes...
                    </div>
                ) : !data ? null : (
                    <div className="space-y-5">

                        {/* Tiempo promedio general */}
                        <div className="grid grid-cols-2 gap-3">
                            <div className="bg-white rounded-xl border border-slate-200 p-4">
                                <p className="text-xs text-slate-400 mb-1">Tiempo promedio de resolución</p>
                                <p className="text-2xl font-bold" style={{ color: PRIMARY }}>
                                    {formatHoras(data.tiempoPromedioGeneral.horasPromedio)}
                                </p>
                            </div>
                            <div className="bg-white rounded-xl border border-slate-200 p-4">
                                <p className="text-xs text-slate-400 mb-1">Tickets resueltos considerados</p>
                                <p className="text-2xl font-bold text-slate-700">
                                    {data.tiempoPromedioGeneral.ticketsResueltos}
                                </p>
                            </div>
                        </div>

                        <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">

                            {/* Por categoría */}
                            <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-5">
                                <h2 className="text-sm font-bold text-slate-700 mb-4">Tickets por categoría</h2>
                                {data.porCategoria.length === 0 ? (
                                    <p className="text-xs text-slate-400 italic">Sin datos aún.</p>
                                ) : (
                                    <div className="space-y-3">
                                        {data.porCategoria.map(c => (
                                            <Barra
                                                key={c.categoria}
                                                label={c.categoria}
                                                valor={c.total}
                                                max={maxCategoria}
                                                color="bg-indigo-500"
                                            />
                                        ))}
                                    </div>
                                )}
                                <div className="mt-4 pt-4 border-t border-slate-100 flex flex-wrap gap-2">
                                    {data.porCategoria.map(c => (
                                        <span key={c.categoria} className={`text-[11px] px-2 py-1 rounded-lg font-medium ${categoriaBadge(c.categoria)}`}>
                                            {c.categoria}: {c.resueltos}/{c.total} resueltos
                                        </span>
                                    ))}
                                </div>
                            </div>

                            {/* Por técnico */}
                            <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-5">
                                <h2 className="text-sm font-bold text-slate-700 mb-4">Tickets por técnico</h2>
                                {data.porTecnico.length === 0 ? (
                                    <p className="text-xs text-slate-400 italic">Aún no hay tickets asignados.</p>
                                ) : (
                                    <div className="space-y-3">
                                        {data.porTecnico.map(t => (
                                            <Barra
                                                key={t.tecnico}
                                                label={t.tecnico}
                                                valor={t.total}
                                                max={maxTecnico}
                                                color="bg-blue-500"
                                            />
                                        ))}
                                    </div>
                                )}
                                {data.porTecnico.length > 0 && (
                                    <div className="mt-4 pt-4 border-t border-slate-100 overflow-x-auto">
                                        <table className="w-full text-xs">
                                            <thead>
                                                <tr className="text-slate-400 text-left">
                                                    <th className="pb-1 font-medium">Técnico</th>
                                                    <th className="pb-1 font-medium text-right">Resueltos</th>
                                                    <th className="pb-1 font-medium text-right">Prom.</th>
                                                </tr>
                                            </thead>
                                            <tbody className="divide-y divide-slate-50">
                                                {data.porTecnico.map(t => (
                                                    <tr key={t.tecnico}>
                                                        <td className="py-1.5 text-slate-600">{t.tecnico}</td>
                                                        <td className="py-1.5 text-right text-slate-600">{t.resueltos}/{t.total}</td>
                                                        <td className="py-1.5 text-right font-medium text-slate-700">
                                                            {formatHoras(t.horasPromedio)}
                                                        </td>
                                                    </tr>
                                                ))}
                                            </tbody>
                                        </table>
                                    </div>
                                )}
                            </div>
                        </div>

                        {/* Tiempo promedio por categoría */}
                        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-5">
                            <h2 className="text-sm font-bold text-slate-700 mb-4">Tiempo promedio de resolución por categoría</h2>
                            {data.tiempoPromedioPorCategoria.length === 0 ? (
                                <p className="text-xs text-slate-400 italic">Sin datos aún.</p>
                            ) : (
                                <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
                                    {data.tiempoPromedioPorCategoria.map(c => (
                                        <div key={c.categoria} className="rounded-lg border border-slate-200 p-3 text-center">
                                            <span className={`text-[10px] px-2 py-0.5 rounded-full font-medium ${categoriaBadge(c.categoria)}`}>
                                                {c.categoria}
                                            </span>
                                            <p className="text-lg font-bold text-slate-700 mt-2">
                                                {formatHoras(c.horasPromedio)}
                                            </p>
                                            <p className="text-[11px] text-slate-400">{c.ticketsResueltos} resueltos</p>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    </div>
                )}
            </main>
        </div>
    );
}
