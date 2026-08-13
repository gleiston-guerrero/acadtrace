import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import axios from "axios";
import Layout from "../components/Layout";

const API = "/api/soporte";
const PRIMARY = "#243A76";

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

    const maxCategoria = data ? Math.max(...data.porCategoria.map(c => c.total), 1) : 1;
    const maxTecnico   = data ? Math.max(...data.porTecnico.map(t => t.total), 1) : 1;

    return (
        <Layout breadcrumb={["Inicio", "Reportes"]}>
            <div className="space-y-6">
                {error && (
                    <div className="bg-red-50 border border-red-200 rounded-xl px-4 py-3 flex items-center justify-between shadow-sm">
                        <span className="text-red-600 text-sm">{error}</span>
                        <button onClick={() => setError("")} className="text-red-400 hover:text-red-600">✕</button>
                    </div>
                )}

                <div>
                    <h1 className="text-xl font-bold text-slate-800">Reportes de Soporte</h1>
                    <p className="text-xs text-slate-400 mt-0.5">Tickets por categoría, por técnico y tiempos de resolución</p>
                </div>

                {loading ? (
                    <div className="bg-white rounded-2xl border border-slate-200 p-12 text-center text-slate-400 text-sm shadow-sm">
                        Cargando reportes...
                    </div>
                ) : !data ? null : (
                    <div className="space-y-6">

                        {/* Tiempo promedio general */}
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                            <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm hover:shadow-md transition">
                                <p className="text-xs font-medium text-slate-400 mb-1">Tiempo promedio de resolución</p>
                                <p className="text-3xl font-extrabold" style={{ color: PRIMARY }}>
                                    {formatHoras(data.tiempoPromedioGeneral.horasPromedio)}
                                </p>
                            </div>
                            <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm hover:shadow-md transition">
                                <p className="text-xs font-medium text-slate-400 mb-1">Tickets resueltos considerados</p>
                                <p className="text-3xl font-extrabold text-slate-700">
                                    {data.tiempoPromedioGeneral.ticketsResueltos}
                                </p>
                            </div>
                        </div>

                        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">

                            {/* Por categoría */}
                            <div className="bg-white rounded-2xl shadow-sm hover:shadow-md transition border border-slate-200 p-6">
                                <h2 className="text-sm font-bold text-slate-800 mb-4">Tickets por categoría</h2>
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
                                <div className="mt-5 pt-4 border-t border-slate-100 flex flex-wrap gap-2">
                                    {data.porCategoria.map(c => (
                                        <span key={c.categoria} className={`text-[11px] px-2.5 py-1 rounded-lg font-medium ${categoriaBadge(c.categoria)}`}>
                                            {c.categoria}: {c.resueltos}/{c.total} resueltos
                                        </span>
                                    ))}
                                </div>
                            </div>

                            {/* Por técnico */}
                            <div className="bg-white rounded-2xl shadow-sm hover:shadow-md transition border border-slate-200 p-6">
                                <h2 className="text-sm font-bold text-slate-800 mb-4">Tickets por técnico</h2>
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
                                    <div className="mt-5 pt-4 border-t border-slate-100 overflow-x-auto">
                                        <table className="w-full text-xs">
                                            <thead>
                                                <tr className="text-slate-400 text-left border-b border-slate-100">
                                                    <th className="pb-2 font-medium">Técnico</th>
                                                    <th className="pb-2 font-medium text-right">Resueltos</th>
                                                    <th className="pb-2 font-medium text-right">Promedio</th>
                                                </tr>
                                            </thead>
                                            <tbody className="divide-y divide-slate-50">
                                                {data.porTecnico.map(t => (
                                                    <tr key={t.tecnico}>
                                                        <td className="py-2 text-slate-600 font-medium">{t.tecnico}</td>
                                                        <td className="py-2 text-right text-slate-600">{t.resueltos}/{t.total}</td>
                                                        <td className="py-2 text-right font-semibold text-slate-700">
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
                        <div className="bg-white rounded-2xl shadow-sm hover:shadow-md transition border border-slate-200 p-6">
                            <h2 className="text-sm font-bold text-slate-800 mb-4">Tiempo promedio de resolución por categoría</h2>
                            {data.tiempoPromedioPorCategoria.length === 0 ? (
                                <p className="text-xs text-slate-400 italic">Sin datos aún.</p>
                            ) : (
                                <div className="grid grid-cols-2 sm:grid-cols-5 gap-4">
                                    {data.tiempoPromedioPorCategoria.map(c => (
                                        <div key={c.categoria} className="rounded-xl border border-slate-200 p-4 text-center bg-slate-50/50">
                                            <span className={`text-[10px] px-2.5 py-0.5 rounded-full font-medium ${categoriaBadge(c.categoria)}`}>
                                                {c.categoria}
                                            </span>
                                            <p className="text-xl font-bold text-slate-800 mt-2">
                                                {formatHoras(c.horasPromedio)}
                                            </p>
                                            <p className="text-[11px] text-slate-400 mt-0.5">{c.ticketsResueltos} resueltos</p>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    </div>
                )}
            </div>
        </Layout>
    );
}
