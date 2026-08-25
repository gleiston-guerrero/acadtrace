import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import logo from "../../assets/logo.png";
import { modulos } from "../../config/modulos";
import { getAnoLectivoActual, PRINCIPAL_LOGIN_URL } from "../../services/api";

const PRIMARY = "#243A76";
const PRIMARY_LIGHT = "#2d4a96";

export default function PanelPrincipal() {
    const [showPeriodo, setShowPeriodo] = useState(false);
    const [showUserMenu, setShowUserMenu] = useState(false);
    const [busqueda, setBusqueda] = useState("");
    const [breadcrumb, setBreadcrumb] = useState(["Inicio"]);
    const navigate = useNavigate();

    const username = localStorage.getItem("username") || "Docente";
    const roles = JSON.parse(localStorage.getItem("roles") || "[]");
    const [anoActual, setAnoActual] = useState(null);
    const [anoEstado, setAnoEstado] = useState("cargando");
    const [banner1, setBanner1] = useState(localStorage.getItem("sga_banner_1") || null);
    const [banner2, setBanner2] = useState(localStorage.getItem("sga_banner_2") || null);
    const [modalImagen, setModalImagen] = useState(null);

    useEffect(() => {
        getAnoLectivoActual().then((r) => { setAnoActual(r.data || null); setAnoEstado(r.data ? "dato" : "no-disponible"); })
            .catch(() => setAnoEstado("error"));

        // Cargar banners institucionales compartidos desde backend central
        const host = typeof window !== "undefined" ? window.location.hostname : "localhost";
        fetch(`http://${host}:8080/api/uploads/banners`)
            .then(r => r.json())
            .then(data => {
                if (data?.banner1) {
                    setBanner1(data.banner1);
                    localStorage.setItem("sga_banner_1", data.banner1);
                }
                if (data?.banner2) {
                    setBanner2(data.banner2);
                    localStorage.setItem("sga_banner_2", data.banner2);
                }
            })
            .catch(() => {});
    }, []);

    const handleModulo = (m) => {
        setBreadcrumb(["Inicio", m.label]);
        navigate(`/${m.id}`);
    };

    const modulosFiltrados = modulos.filter(m =>
        m.label.toLowerCase().includes(busqueda.toLowerCase()) ||
        m.desc.toLowerCase().includes(busqueda.toLowerCase())
    );

    const handleLogout = () => {
        localStorage.clear();
        window.location.href = PRINCIPAL_LOGIN_URL;
    };

    return (
        <div className="flex flex-col min-h-screen bg-slate-50">

            {/* TOP BAR */}
            <header style={{ backgroundColor: PRIMARY }} className="text-white h-14 flex items-center justify-between px-4 shadow z-30 flex-shrink-0">
                <div className="flex items-center gap-3">
                    <img src={logo} alt="Logo" className="w-8 h-8 rounded-full object-cover border-2 border-white border-opacity-40" />
                    <span className="font-bold text-sm">SGA</span>
                    <span className="text-white text-opacity-70 text-sm hidden sm:inline">| Portal Docente</span>
                </div>

                <div className="flex items-center gap-2">
                    {/* Período */}
                    <div className="relative">
                        <button
                            onClick={() => { setShowPeriodo(!showPeriodo); setShowUserMenu(false); }}
                            style={{ backgroundColor: PRIMARY_LIGHT }}
                            className="flex items-center gap-2 hover:opacity-90 px-3 py-1.5 rounded-lg text-xs font-medium transition"
                        >
                            <svg className="w-4 h-4 text-white opacity-80" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                            </svg>
                            <span className="hidden sm:inline">{anoEstado === "cargando" ? "Cargando año..." : anoActual?.nombre || (anoEstado === "error" ? "Año no disponible" : "Sin año lectivo")}</span>
                            <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                            </svg>
                        </button>
                        {showPeriodo && (
                            <div className="absolute right-0 top-11 w-64 bg-white rounded-xl shadow-2xl border border-slate-200 z-50 overflow-hidden">
                                <div style={{ backgroundColor: PRIMARY }} className="px-4 py-3 flex items-center gap-2">
                                    <svg className="w-4 h-4 text-white opacity-80" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                                    </svg>
                                    <span className="text-white text-sm font-semibold">Año Lectivo</span>
                                </div>
                                <div className="p-3">
                                    <div style={{ color: PRIMARY }} className="flex items-center gap-2 px-3 py-2 rounded-lg bg-blue-50 font-semibold text-sm">
                                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                        </svg>
                                        {anoActual?.nombre || "No disponible"}{anoActual ? " (Actual)" : ""}
                                    </div>
                                    <p className="text-xs text-slate-400 text-center mt-2 px-2">
                                        Gestiona los años lectivos desde el módulo correspondiente
                                    </p>
                                </div>
                            </div>
                        )}
                    </div>

                    {/* Notificaciones */}
                    <button className="relative p-2 rounded-lg hover:bg-white hover:bg-opacity-20 transition">
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
                        </svg>
                    </button>

                    {/* Usuario */}
                    <div className="relative">
                        <button
                            onClick={() => { setShowUserMenu(!showUserMenu); setShowPeriodo(false); }}
                            style={{ backgroundColor: PRIMARY_LIGHT }}
                            className="flex items-center gap-2 hover:opacity-90 px-3 py-1.5 rounded-lg transition"
                        >
                            <div className="w-7 h-7 bg-white bg-opacity-20 rounded-full flex items-center justify-center text-xs font-bold uppercase border border-white border-opacity-30">
                                {username.charAt(0)}
                            </div>
                            <span className="hidden sm:inline text-xs font-medium capitalize">{username}</span>
                            <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                            </svg>
                        </button>
                        {showUserMenu && (
                            <div className="absolute right-0 top-11 w-52 bg-white rounded-xl shadow-2xl border border-slate-200 z-50 overflow-hidden">
                                <div style={{ backgroundColor: PRIMARY }} className="px-4 py-3">
                                    <p className="text-white text-sm font-semibold capitalize">{username}</p>
                                    <p className="text-white text-opacity-60 text-xs">{roles[0] || "DOCENTE"}</p>
                                </div>
                                <div className="p-2">
                                    <button className="w-full text-left px-3 py-2 rounded-lg text-sm text-slate-600 hover:bg-slate-50 flex items-center gap-2 transition">
                                        <svg className="w-4 h-4 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" /></svg>
                                        Mi perfil
                                    </button>
                                    <hr className="my-1 border-slate-100" />
                                    <button
                                        onClick={handleLogout}
                                        className="w-full text-left px-3 py-2 rounded-lg text-sm text-red-600 hover:bg-red-50 flex items-center gap-2 transition font-medium"
                                    >
                                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" /></svg>
                                        Cerrar sesión
                                    </button>
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            </header>

            {/* BREADCRUMB + BUSCADOR */}
            <div className="bg-white border-b border-slate-200 px-6 py-2 flex items-center justify-between">
                <nav className="text-xs text-slate-500 flex items-center gap-1">
                    {breadcrumb.map((item, i) => (
                        <span key={i} className="flex items-center gap-1">
              {i > 0 && <span className="text-slate-300">/</span>}
                            <span
                                style={i === breadcrumb.length - 1 ? { color: PRIMARY } : {}}
                                className={i === breadcrumb.length - 1 ? "font-medium" : "hover:underline cursor-pointer"}
                                onClick={() => i === 0 && setBreadcrumb(["Inicio"])}
                            >
                {item}
              </span>
            </span>
                    ))}
                </nav>
                <div className="relative">
                    <input
                        type="text"
                        placeholder="Buscar..."
                        value={busqueda}
                        onChange={e => setBusqueda(e.target.value)}
                        className="pl-3 pr-8 py-1.5 text-xs border border-slate-200 rounded-lg focus:outline-none bg-slate-50 w-44"
                    />
                    <svg className="w-4 h-4 text-slate-400 absolute right-2 top-1/2 -translate-y-1/2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                    </svg>
                </div>
            </div>

            {/* BODY */}
            <div className="flex flex-1 overflow-hidden" style={{ paddingBottom: "2.5rem" }}>

                {/* PANEL IZQUIERDO — BANNERS Y AVISOS INSTITUCIONALES COMPARTIDOS */}
                <aside className="w-80 flex-shrink-0 bg-white border-r border-slate-200 overflow-y-auto p-4 hidden lg:flex flex-col gap-4">
                    {/* Afiche 1: Avisos y Comunicados Oficiales */}
                    <div className="relative group rounded-2xl overflow-hidden shadow-xs border border-slate-200 bg-white transition hover:shadow-md cursor-pointer">
                        {banner1 ? (
                            <div className="relative w-full">
                                <img
                                    src={banner1}
                                    alt="Avisos y Comunicados"
                                    onClick={() => setModalImagen({ src: banner1, title: "Aviso Importante — Docentes" })}
                                    className="w-full h-auto object-contain block rounded-2xl"
                                />
                                <div className="absolute inset-0 bg-slate-950/40 opacity-0 group-hover:opacity-100 transition-all flex items-center justify-center backdrop-blur-xs rounded-2xl">
                                    <button
                                        onClick={() => setModalImagen({ src: banner1, title: "Aviso Importante — Docentes" })}
                                        className="px-4 py-2 bg-white text-slate-800 rounded-xl text-xs font-bold shadow-lg hover:bg-slate-50 transition flex items-center gap-1.5">
                                        🔍 Ver Afiche Completo
                                    </button>
                                </div>
                            </div>
                        ) : (
                            <div
                                onClick={() => setModalImagen({ src: null, title: "Período Lectivo 2026-2027", desc: "Sistema de matrículas y registro de calificaciones activo." })}
                                className="bg-gradient-to-br from-[#1a2d5f] via-[#243A76] to-[#1e3a8a] p-5 text-white flex flex-col justify-between min-h-[220px]"
                            >
                                <div>
                                    <div className="flex items-center justify-between mb-2">
                                        <span className="text-[10px] font-black uppercase tracking-wider bg-white/20 px-2.5 py-0.5 rounded-full text-blue-100 backdrop-blur-xs">
                                            📢 AVISO OFICIAL
                                        </span>
                                        <span className="text-[10px] text-blue-200 font-bold">2026 - 2027</span>
                                    </div>
                                    <h4 className="font-bold text-base leading-snug text-white mt-1">
                                        Período Lectivo 2026-2027
                                    </h4>
                                    <p className="text-xs text-blue-100/90 mt-2 leading-relaxed font-sans">
                                        Sistema de matrículas y registro de calificaciones 70/30 activo en toda la institución.
                                    </p>
                                </div>
                                <div className="pt-3 border-t border-white/10 flex items-center justify-between text-xs text-blue-200">
                                    <span className="font-medium text-[11px]">Escuela Provincias Unidas</span>
                                    <span className="text-[10px] bg-white/10 px-2 py-0.5 rounded text-blue-100">Oficial</span>
                                </div>
                            </div>
                        )}
                    </div>

                    {/* Afiche 2: Calendario y Eventos Académicos */}
                    <div className="relative group rounded-2xl overflow-hidden shadow-xs border border-slate-200 bg-white transition hover:shadow-md cursor-pointer">
                        {banner2 ? (
                            <div className="relative w-full">
                                <img
                                    src={banner2}
                                    alt="Calendario y Eventos"
                                    onClick={() => setModalImagen({ src: banner2, title: "Calendario Académico — Eventos" })}
                                    className="w-full h-auto object-contain block rounded-2xl"
                                />
                                <div className="absolute inset-0 bg-slate-950/40 opacity-0 group-hover:opacity-100 transition-all flex items-center justify-center backdrop-blur-xs rounded-2xl">
                                    <button
                                        onClick={() => setModalImagen({ src: banner2, title: "Calendario Académico — Eventos" })}
                                        className="px-4 py-2 bg-white text-slate-800 rounded-xl text-xs font-bold shadow-lg hover:bg-slate-50 transition flex items-center gap-1.5">
                                        🔍 Ver Afiche Completo
                                    </button>
                                </div>
                            </div>
                        ) : (
                            <div
                                onClick={() => setModalImagen({ src: null, title: "Calendario Académico", desc: "Cronograma de exámenes y asentamiento de notas." })}
                                className="bg-gradient-to-br from-[#0f766e] via-[#115e59] to-[#134e4a] p-5 text-white flex flex-col justify-between min-h-[220px]"
                            >
                                <div>
                                    <div className="flex items-center justify-between mb-2">
                                        <span className="text-[10px] font-black uppercase tracking-wider bg-white/20 px-2.5 py-0.5 rounded-full text-teal-100 backdrop-blur-xs">
                                            🗓️ CALENDARIO
                                        </span>
                                        <span className="text-[10px] text-teal-200 font-bold">Trimestre 1</span>
                                    </div>
                                    <h4 className="font-bold text-base leading-snug text-white mt-1">
                                        Asentamiento de Notas
                                    </h4>
                                    <p className="text-xs text-teal-100/90 mt-2 leading-relaxed font-sans">
                                        Registro de aportes formativos (70%) y examen sumativo (30%) por docentes titulares.
                                    </p>
                                </div>
                                <div className="pt-3 border-t border-white/10 flex items-center justify-between text-xs text-teal-200">
                                    <span className="font-medium text-[11px]">Tutoría con IA Activa</span>
                                    <span className="text-[10px] bg-white/10 px-2 py-0.5 rounded text-teal-100">Oficial</span>
                                </div>
                            </div>
                        )}
                    </div>
                </aside>

                {/* MAIN */}
                <main className="flex-1 overflow-y-auto p-6">
                    <div className="mb-4">
                        <h1 className="text-lg font-bold text-slate-700">
                            Bienvenido, <span style={{ color: PRIMARY }} className="capitalize">{username}</span>
                        </h1>
                        <p className="text-slate-400 text-xs mt-0.5">
                            Año lectivo: <span style={{ color: PRIMARY }} className="font-semibold">{anoActual?.nombre || (anoEstado === "cargando" ? "Cargando..." : "N/D")}</span>
                        </p>
                    </div>

                    <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4">
                        {modulosFiltrados.map((m) => (
                            <button
                                key={m.id}
                                onClick={() => handleModulo(m)}
                                className="bg-white border border-slate-200 rounded-xl p-5 flex flex-col items-center gap-3 hover:shadow-md transition-all group text-center"
                                onMouseEnter={e => e.currentTarget.style.borderColor = PRIMARY}
                                onMouseLeave={e => e.currentTarget.style.borderColor = ''}
                            >
                                <div className={`${m.color} p-3 rounded-xl ${m.iconColor}`}>
                                    {m.icon}
                                </div>
                                <div>
                                    <p className="text-sm font-semibold text-slate-700 group-hover:text-[#243A76] transition">{m.label}</p>
                                    <p className="text-xs text-slate-400 mt-0.5 leading-tight">{m.desc}</p>
                                </div>
                            </button>
                        ))}
                        {modulosFiltrados.length === 0 && (
                            <div className="col-span-4 text-center py-10 text-slate-400 text-sm">
                                No se encontraron módulos con "{busqueda}"
                            </div>
                        )}
                    </div>
                </main>
            </div>

            {/* FOOTER */}
            <footer style={{ backgroundColor: PRIMARY }} className="fixed bottom-0 left-0 right-0 text-white text-opacity-80 text-xs text-center py-2 z-40 flex-shrink-0">
                Sistema de Gestión Académica — Escuela Provincias Unidas © 2026
            </footer>

            {/* MODAL VISOR LIGHTBOX DE AFICHE */}
            {modalImagen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-fade-in" onClick={() => setModalImagen(null)}>
                    <div className="relative bg-white rounded-3xl overflow-hidden shadow-2xl max-w-2xl w-full max-h-[90vh] flex flex-col border border-slate-200" onClick={e => e.stopPropagation()}>
                        <div style={{ backgroundColor: PRIMARY }} className="px-5 py-3.5 text-white flex items-center justify-between">
                            <div className="flex items-center gap-2">
                                <span className="text-base">📢</span>
                                <h3 className="font-bold text-sm">{modalImagen.title}</h3>
                            </div>
                            <button onClick={() => setModalImagen(null)} className="p-1 rounded-lg text-white/80 hover:text-white hover:bg-white/10 transition">
                                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                </svg>
                            </button>
                        </div>

                        <div className="p-4 flex items-center justify-center bg-slate-50 overflow-y-auto max-h-[calc(90vh-60px)]">
                            {modalImagen.src ? (
                                <img src={modalImagen.src} alt={modalImagen.title} className="max-w-full h-auto rounded-2xl shadow-sm object-contain" />
                            ) : (
                                <div className="p-8 text-center text-slate-600">
                                    <p className="font-bold text-base">{modalImagen.title}</p>
                                    <p className="text-sm mt-2">{modalImagen.desc}</p>
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            )}

            {/* Overlay */}
            {(showPeriodo || showUserMenu) && (
                <div className="fixed inset-0 z-20" onClick={() => { setShowPeriodo(false); setShowUserMenu(false); }} />
            )}
        </div>
    );
}
