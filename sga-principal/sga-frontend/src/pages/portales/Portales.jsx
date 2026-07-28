import { useState, useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import logo from "../../assets/logo.png";
import { redirigirAMicroservicio } from "../../utils/handoff";

// Portales disponibles según el rol del usuario. DIRECTOR permanece en este mismo
// origen (SGA Principal); el resto se entrega a su microservicio por SSO.
const PORTALES = {
    DIRECTOR: {
        label: "Administración",
        desc: "Panel principal y configuración",
        color: "bg-blue-50",
        iconColor: "text-blue-600",
        tipo: "local",
        icon: (
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
        ),
    },
    SECRETARIA: {
        label: "Portal Secretaría",
        desc: "Matrículas, estudiantes y reportes",
        color: "bg-purple-50",
        iconColor: "text-purple-600",
        tipo: "handoff",
        destino: "SECRETARIA",
        icon: (
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
        ),
    },
    DOCENTE: {
        label: "Portal Docente",
        desc: "Actividades, asistencia y calificaciones",
        color: "bg-cyan-50",
        iconColor: "text-cyan-600",
        tipo: "handoff",
        destino: "DOCENTE",
        icon: (
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
            </svg>
        ),
    },
    SOPORTE_TECNICO: {
        label: "Portal Soporte",
        desc: "Soporte técnico del sistema",
        color: "bg-emerald-50",
        iconColor: "text-emerald-600",
        tipo: "handoff",
        destino: "SOPORTE_TECNICO",
        icon: (
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M11.42 15.17L17.25 21A2.652 2.652 0 0021 17.25l-5.877-5.877M11.42 15.17l2.496-3.03c.317-.384.74-.626 1.208-.766M11.42 15.17l-4.655 5.653a2.548 2.548 0 11-3.586-3.586l6.837-5.63m5.108-.233c.55-.164 1.163-.188 1.743-.14a4.5 4.5 0 004.486-6.336l-3.276 3.277a3.004 3.004 0 01-2.25-2.25l3.276-3.276a4.5 4.5 0 00-6.336 4.486c.091 1.076-.071 2.264-.904 2.95l-.102.085" />
            </svg>
        ),
    },
};

// Orden de aparición cuando el usuario tiene varios roles.
const ORDEN = ["DIRECTOR", "SECRETARIA", "DOCENTE", "SOPORTE_TECNICO"];

export default function Portales() {
    const navigate = useNavigate();
    const location = useLocation();
    const sesion = location.state;
    const [cargando, setCargando] = useState(null);

    // Sin sesión en el estado de navegación = acceso directo a la URL → al login.
    useEffect(() => {
        if (!sesion?.token) navigate("/login", { replace: true });
    }, [sesion, navigate]);

    if (!sesion?.token) return null;

    const disponibles = ORDEN.filter((rol) => sesion.roles?.includes(rol));

    const irAlPortal = async (rol) => {
        const portal = PORTALES[rol];
        setCargando(rol);

        if (portal.tipo === "local") {
            localStorage.setItem("token", sesion.token);
            localStorage.setItem("username", sesion.username);
            localStorage.setItem("roles", JSON.stringify(sesion.roles));
            localStorage.setItem("primerIngreso", String(sesion.primerIngreso));
            navigate("/dashboard");
            return;
        }

        const ok = await redirigirAMicroservicio(portal.destino, sesion);
        if (!ok) setCargando(null);
    };

    const salir = () => navigate("/login", { replace: true });

    return (
        <div className="min-h-screen flex items-center justify-center bg-slate-100 px-4">
            {/* Fondo decorativo */}
            <div className="absolute inset-0 overflow-hidden pointer-events-none">
                <div className="absolute -top-32 -left-32 w-96 h-96 rounded-full bg-blue-900 opacity-10" />
                <div className="absolute -bottom-32 -right-32 w-96 h-96 rounded-full bg-blue-700 opacity-10" />
            </div>

            <div className="w-full max-w-xl bg-white rounded-2xl shadow-2xl overflow-hidden relative z-10">
                {/* Header azul */}
                <div className="bg-blue-900 px-8 pt-8 pb-6 flex flex-col items-center">
                    <div className="w-20 h-20 rounded-full overflow-hidden border-4 border-white shadow-lg mb-3">
                        <img src={logo} alt="Logo Provincias Unidas" className="w-full h-full object-cover" />
                    </div>
                    <h1 className="text-white text-lg font-bold tracking-wide text-center leading-tight">
                        Escuela de Educación Básica
                    </h1>
                    <p className="text-blue-200 text-sm font-semibold tracking-widest uppercase mt-1">
                        Provincias Unidas
                    </p>
                </div>

                {/* Selección de portal */}
                <div className="px-8 py-6">
                    <h2 className="text-slate-700 text-base font-semibold text-center">
                        Hola, {sesion.username}
                    </h2>
                    <p className="text-slate-500 text-sm text-center mb-5">
                        Tienes acceso a varios portales. Elige a cuál deseas ingresar.
                    </p>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                        {disponibles.map((rol) => {
                            const portal = PORTALES[rol];
                            return (
                                <button
                                    key={rol}
                                    onClick={() => irAlPortal(rol)}
                                    disabled={cargando !== null}
                                    className="flex items-center gap-3 p-4 rounded-xl border border-slate-200 hover:border-blue-400 hover:shadow-md transition text-left disabled:opacity-60 disabled:cursor-not-allowed"
                                >
                                    <span className={`w-12 h-12 rounded-lg flex items-center justify-center flex-shrink-0 ${portal.color} ${portal.iconColor}`}>
                                        {cargando === rol ? (
                                            <svg className="w-5 h-5 animate-spin" fill="none" viewBox="0 0 24 24">
                                                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                                                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
                                            </svg>
                                        ) : portal.icon}
                                    </span>
                                    <span className="min-w-0">
                                        <span className="block text-sm font-semibold text-slate-700">{portal.label}</span>
                                        <span className="block text-xs text-slate-500 leading-snug">{portal.desc}</span>
                                    </span>
                                </button>
                            );
                        })}
                    </div>

                    <div className="text-center mt-6">
                        <button onClick={salir} className="text-xs text-slate-400 hover:text-blue-600 transition">
                            Cerrar sesión
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}
