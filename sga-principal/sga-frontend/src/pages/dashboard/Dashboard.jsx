import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import api from "../../config/axios";
import logo from "../../assets/logo.png";
import { modulos } from "../../config/modulos";
import { redirigirAMicroservicio } from "../../utils/handoff";

const PRIMARY = "#243A76";
const PRIMARY_DARK = "#1a2d5f";
const PRIMARY_LIGHT = "#2d4a96";

export default function Dashboard() {
    const [showPeriodo, setShowPeriodo] = useState(false);
    const [showUserMenu, setShowUserMenu] = useState(false);
    const [anoActual, setAnoActual] = useState(null);
    const [busqueda, setBusqueda] = useState("");
    const [breadcrumb, setBreadcrumb] = useState(["Inicio"]);
    const [banner1, setBanner1] = useState(localStorage.getItem("sga_banner_1") || null);
    const [banner2, setBanner2] = useState(localStorage.getItem("sga_banner_2") || null);
    const [modalImagen, setModalImagen] = useState(null);
    const username = localStorage.getItem("username") || "Director";
    const roles = JSON.parse(localStorage.getItem("roles") || "[]");
    const esAdmin = roles.length === 0 || roles.some(r => {
        const ro = (r.nombre || r.name || r || "").toString().toUpperCase();
        return ro.includes("ADMIN") || ro.includes("DIRECTOR") || ro.includes("SECRETAR") || ro.includes("RECTOR");
    });
    const token = localStorage.getItem("token");
    const idUsuario = localStorage.getItem("userId");
    const navigate = useNavigate();

    const handleUploadBanner = (num, e) => {
        const file = e.target.files?.[0];
        if (!file) return;
        const reader = new FileReader();
        reader.onload = (ev) => {
            const base64 = ev.target.result;
            if (num === 1) {
                setBanner1(base64);
                localStorage.setItem("sga_banner_1", base64);
            } else {
                setBanner2(base64);
                localStorage.setItem("sga_banner_2", base64);
            }
            // Sincronizar con el backend central de sga-principal para que todos los microservicios lo vean
            api.post(`/api/uploads/banner/${num}`, { data: base64 })
               .catch(err => console.error("Error al sincronizar banner con servidor:", err));
        };
        reader.readAsDataURL(file);
    };

    const handleEliminarBanner = (num, e) => {
        e.stopPropagation();
        if (num === 1) {
            setBanner1(null);
            localStorage.removeItem("sga_banner_1");
        } else {
            setBanner2(null);
            localStorage.removeItem("sga_banner_2");
        }
        api.delete(`/api/uploads/banner/${num}`).catch(() => {});
    };

    useEffect(() => {
        api.get(`/api/anos-lectivos/actual`)
            .then(r => setAnoActual(r.data))
            .catch(() => {});

        // Cargar banners institucionales compartidos desde backend
        api.get(`/api/uploads/banners`)
            .then(r => {
                if (r.data?.banner1) {
                    setBanner1(r.data.banner1);
                    localStorage.setItem("sga_banner_1", r.data.banner1);
                }
                if (r.data?.banner2) {
                    setBanner2(r.data.banner2);
                    localStorage.setItem("sga_banner_2", r.data.banner2);
                }
            })
            .catch(() => {});
    }, []);

    const handleModulo = (m) => {
        // Modulos marcados con handoff no tienen pagina local (su dominio vive
        // por completo en otro microservicio); en vez de navegar a una ruta local
        // se entrega la sesion por el mismo mecanismo de SSO que usa Portales.jsx.
        if (m.handoff) {
            redirigirAMicroservicio(m.handoff, {
                token,
                idUsuario,
                username,
                roles,
                primerIngreso: localStorage.getItem("primerIngreso") === "true",
            });
            return;
        }
        setBreadcrumb(["Inicio", m.label]);
        navigate(`/${m.id}`);
    };

    // El dashboard es el portal del DIRECTOR: muestra todos los módulos
    // administrativos. Los docentes no llegan aquí (se entregan a su
    // microservicio), así que no se filtra por DOCENTE.
    const modulosFiltrados = modulos.filter(m =>
        m.label.toLowerCase().includes(busqueda.toLowerCase()) ||
        m.desc.toLowerCase().includes(busqueda.toLowerCase())
    );

    const handleLogout = () => {
        localStorage.clear();
        navigate("/login");
    };

    return (
        <div className="flex flex-col min-h-screen bg-slate-50">

            {/* TOP BAR - FIJO */}
            <header style={{ backgroundColor: PRIMARY }} className="fixed top-0 left-0 right-0 text-white h-14 flex items-center justify-between px-4 shadow z-40 flex-shrink-0">
                <div className="flex items-center gap-3">
                    <img src={logo} alt="Logo" className="w-8 h-8 rounded-full object-cover border-2 border-white border-opacity-40" />
                    <span className="font-bold text-sm">SGA</span>
                    <span className="text-white text-opacity-70 text-sm hidden sm:inline">| Sistema de Gestión Académica</span>
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
                            <span className="hidden sm:inline">{anoActual?.nombre || "Sin período"}</span>
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
                                    {anoActual ? (
                                        <div style={{ color: PRIMARY }} className="flex items-center gap-2 px-3 py-2 rounded-lg bg-blue-50 font-semibold text-sm">
                                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                            </svg>
                                            {anoActual.nombre} (Actual)
                                        </div>
                                    ) : (
                                        <p className="text-xs text-slate-400 text-center px-2">No hay año lectivo activo</p>
                                    )}
                                    <p className="text-xs text-slate-400 text-center mt-2 px-2">
                                        Gestiona los años lectivos desde el módulo correspondiente
                                    </p>
                                </div>
                            </div>
                        )}
                    </div>

                    {/* Notificaciones */}
                    <button style={{ '--hover-bg': PRIMARY_LIGHT }} className="relative p-2 rounded-lg hover:bg-white hover:bg-opacity-20 transition">
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
                                    <p className="text-white text-opacity-60 text-xs">{roles[0] || "ADMINISTRADOR"}</p>
                                </div>
                                <div className="p-2">
                                    <button className="w-full text-left px-3 py-2 rounded-lg text-sm text-slate-600 hover:bg-slate-50 flex items-center gap-2 transition">
                                        <svg className="w-4 h-4 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" /></svg>
                                        Mi perfil
                                    </button>
                                    <button
                                        onClick={() => navigate("/cambiar-password")}
                                        className="w-full text-left px-3 py-2 rounded-lg text-sm text-slate-600 hover:bg-slate-50 flex items-center gap-2 transition"
                                    >
                                        <svg className="w-4 h-4 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z" /></svg>
                                        Cambiar contraseña
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

            {/* Spacer del header fijo */}
            <div className="h-14 flex-shrink-0" />

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
                        style={{ '--tw-ring-color': PRIMARY }}
                    />
                    <svg className="w-4 h-4 text-slate-400 absolute right-2 top-1/2 -translate-y-1/2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                    </svg>
                </div>
            </div>

            {/* BODY */}
            <div className="flex flex-1 overflow-hidden" style={{ paddingBottom: "2.5rem" }}>

                {/* PANEL IZQUIERDO — AFICHES Y BANNERS INFORMATIVOS (ESTILO SGA UTEQ) */}
                <aside className="w-80 flex-shrink-0 bg-white border-r border-slate-200 overflow-y-auto p-4 hidden lg:flex flex-col gap-4">
                    {/* Afiche 1: Avisos y Comunicados Oficiales */}
                    <div className="relative group rounded-2xl overflow-hidden shadow-xs border border-slate-200 bg-white transition hover:shadow-md cursor-pointer">
                        {banner1 ? (
                            <div className="relative w-full">
                                <img
                                    src={banner1}
                                    alt="Avisos y Comunicados"
                                    onClick={() => setModalImagen({ src: banner1, title: "Aviso Importante — Escuela Provincias Unidas" })}
                                    className="w-full h-auto object-contain block rounded-2xl"
                                />
                                <div className="absolute inset-0 bg-slate-950/40 opacity-0 group-hover:opacity-100 transition-all flex items-center justify-center gap-2 backdrop-blur-xs rounded-2xl">
                                    <button
                                        onClick={() => setModalImagen({ src: banner1, title: "Aviso Importante — Escuela Provincias Unidas" })}
                                        className="px-3 py-1.5 bg-white text-slate-800 rounded-xl text-xs font-bold shadow-lg hover:bg-slate-50 transition flex items-center gap-1">
                                        🔍 Ver
                                    </button>
                                    {esAdmin && (
                                        <>
                                            <label className="cursor-pointer px-3 py-1.5 bg-blue-600 text-white rounded-xl text-xs font-bold shadow-lg hover:bg-blue-700 transition flex items-center gap-1">
                                                📷 Cambiar
                                                <input type="file" accept="image/*" className="hidden" onChange={e => handleUploadBanner(1, e)} />
                                            </label>
                                            <button
                                                onClick={(e) => handleEliminarBanner(1, e)}
                                                className="px-2.5 py-1.5 bg-rose-600 text-white rounded-xl text-xs font-bold shadow-lg hover:bg-rose-700 transition">
                                                ✕
                                            </button>
                                        </>
                                    )}
                                </div>
                            </div>
                        ) : (
                            <div
                                onClick={() => !esAdmin && setModalImagen({ src: null, title: "Período Lectivo 2026-2027", desc: "Sistema de matrículas y calificaciones activo." })}
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
                                    {esAdmin && (
                                        <label className="cursor-pointer font-bold text-white bg-white/20 hover:bg-white/30 px-3 py-1 rounded-lg text-xs transition flex items-center gap-1">
                                            📷 Subir afiche
                                            <input type="file" accept="image/*" className="hidden" onChange={e => handleUploadBanner(1, e)} />
                                        </label>
                                    )}
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
                                    onClick={() => setModalImagen({ src: banner2, title: "Calendario Académico — Escuela Provincias Unidas" })}
                                    className="w-full h-auto object-contain block rounded-2xl"
                                />
                                <div className="absolute inset-0 bg-slate-950/40 opacity-0 group-hover:opacity-100 transition-all flex items-center justify-center gap-2 backdrop-blur-xs rounded-2xl">
                                    <button
                                        onClick={() => setModalImagen({ src: banner2, title: "Calendario Académico — Escuela Provincias Unidas" })}
                                        className="px-3 py-1.5 bg-white text-slate-800 rounded-xl text-xs font-bold shadow-lg hover:bg-slate-50 transition flex items-center gap-1">
                                        🔍 Ver
                                    </button>
                                    {esAdmin && (
                                        <>
                                            <label className="cursor-pointer px-3 py-1.5 bg-teal-600 text-white rounded-xl text-xs font-bold shadow-lg hover:bg-teal-700 transition flex items-center gap-1">
                                                📷 Cambiar
                                                <input type="file" accept="image/*" className="hidden" onChange={e => handleUploadBanner(2, e)} />
                                            </label>
                                            <button
                                                onClick={(e) => handleEliminarBanner(2, e)}
                                                className="px-2.5 py-1.5 bg-rose-600 text-white rounded-xl text-xs font-bold shadow-lg hover:bg-rose-700 transition">
                                                ✕
                                            </button>
                                        </>
                                    )}
                                </div>
                            </div>
                        ) : (
                            <div
                                onClick={() => !esAdmin && setModalImagen({ src: null, title: "Calendario Académico", desc: "Cronograma de exámenes y asentamiento de notas." })}
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
                                    {esAdmin && (
                                        <label className="cursor-pointer font-bold text-white bg-white/20 hover:bg-white/30 px-3 py-1 rounded-lg text-xs transition flex items-center gap-1">
                                            📷 Subir afiche
                                            <input type="file" accept="image/*" className="hidden" onChange={e => handleUploadBanner(2, e)} />
                                        </label>
                                    )}
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
                        {anoActual && (
                            <p className="text-slate-400 text-xs mt-0.5">
                                Año lectivo activo: <span style={{ color: PRIMARY }} className="font-semibold">{anoActual.nombre}</span>
                            </p>
                        )}
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

            {/* FOOTER - FIJO */}
            <footer style={{ backgroundColor: PRIMARY }} className="fixed bottom-0 left-0 right-0 text-white text-opacity-80 text-xs text-center py-2 z-40 flex-shrink-0">
                Sistema de Gestión Académica — Escuela Provincias Unidas © 2026
            </footer>

            {/* MODAL VISOR LIGHTBOX DE AFICHE */}
            {modalImagen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-fade-in" onClick={() => setModalImagen(null)}>
                    <div className="relative bg-white rounded-3xl overflow-hidden shadow-2xl max-w-2xl w-full max-h-[90vh] flex flex-col border border-slate-200" onClick={e => e.stopPropagation()}>
                        {/* Cabecera del modal */}
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

                        {/* Contenido de la imagen completa */}
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
