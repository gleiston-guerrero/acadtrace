import { useState, useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import logo from "../assets/logo.png";
import { redirigirAMicroservicio, detectarHostVivo } from "../utils/handoff";
import { MICROSERVICIOS } from "../config/microservicios";

const PORTALES = {
  DIRECTOR: {
    label: "Administración Principal",
    desc: "Panel general y configuración",
    color: "bg-blue-50 text-blue-700 border-blue-200 hover:border-blue-500",
    badge: "bg-blue-100 text-blue-800",
    icon: (
      <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
      </svg>
    ),
  },
  SECRETARIA: {
    label: "Portal Secretaría",
    desc: "Matrículas, estudiantes, asignaturas y actas",
    color: "bg-indigo-50 text-indigo-700 border-indigo-200 hover:border-indigo-500",
    badge: "bg-indigo-100 text-indigo-800",
    icon: (
      <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
      </svg>
    ),
  },
  DOCENTE: {
    label: "Portal Docente",
    desc: "Actividades, asistencia y calificaciones",
    color: "bg-cyan-50 text-cyan-700 border-cyan-200 hover:border-cyan-500",
    badge: "bg-cyan-100 text-cyan-800",
    icon: (
      <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
      </svg>
    ),
  },
  SOPORTE_TECNICO: {
    label: "Portal Soporte",
    desc: "Incidencias y soporte técnico",
    color: "bg-emerald-50 text-emerald-700 border-emerald-200 hover:border-emerald-500",
    badge: "bg-emerald-100 text-emerald-800",
    icon: (
      <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M11.42 15.17L17.25 21A2.652 2.652 0 0021 17.25l-5.877-5.877M11.42 15.17l2.496-3.03c.317-.384.74-.626 1.208-.766M11.42 15.17l-4.655 5.653a2.548 2.548 0 11-3.586-3.586l6.837-5.63m5.108-.233c.55-.164 1.163-.188 1.743-.14a4.5 4.5 0 004.486-6.336l-3.276 3.277a3.004 3.004 0 01-2.25-2.25l3.276-3.276a4.5 4.5 0 00-6.336 4.486c.091 1.076-.071 2.264-.904 2.95l-.102.085" />
      </svg>
    ),
  },
};

const ORDEN = ["DIRECTOR", "SECRETARIA", "DOCENTE", "SOPORTE_TECNICO"];

export default function Portales() {
  const navigate = useNavigate();
  const location = useLocation();
  const [cargando, setCargando] = useState(null);

  // Obtener sesión del state o del localStorage
  const sesion = location.state || {
    token: localStorage.getItem("token"),
    username: localStorage.getItem("username") || "Usuario",
    roles: (() => {
      try {
        return JSON.parse(localStorage.getItem("roles") || "[]");
      } catch {
        return ["SECRETARIA"];
      }
    })(),
    idUsuario: localStorage.getItem("userId") || 1,
  };

  useEffect(() => {
    if (!sesion?.token && !localStorage.getItem("token")) {
      navigate("/login", { replace: true });
    }
  }, [sesion, navigate]);

  if (!sesion?.token && !localStorage.getItem("token")) return null;

  // Filtrar portales según roles del usuario o mostrar todos si tiene roles administrativos
  const userRoles = Array.isArray(sesion.roles) ? sesion.roles : [sesion.roles];
  let disponibles = ORDEN.filter((rol) => userRoles.includes(rol));

  // Si no tiene ninguno de la lista pero está autenticado, mostrar Secretaría
  if (disponibles.length === 0) {
    disponibles = ["SECRETARIA"];
  }

  const irAlPortal = async (rol) => {
    setCargando(rol);

    // Si elige Secretaría, entra localmente a este microservicio
    if (rol === "SECRETARIA") {
      localStorage.setItem("token", sesion.token);
      localStorage.setItem("username", sesion.username);
      localStorage.setItem("roles", JSON.stringify(userRoles));
      if (sesion.idUsuario) localStorage.setItem("userId", String(sesion.idUsuario));
      navigate("/dashboard");
      return;
    }

    // Si elige otro microservicio (DIRECTOR, DOCENTE, SOPORTE), redirige por handoff SSO
    const ok = await redirigirAMicroservicio(rol, sesion);
    if (!ok) setCargando(null);
  };

  const salir = async () => {
    localStorage.clear();
    const hostVivo = await detectarHostVivo(MICROSERVICIOS.DIRECTOR?.hosts || []);
    const fallbackHost = typeof window !== "undefined" ? `http://${window.location.hostname}:5173` : "http://localhost:5173";
    window.location.href = `${hostVivo || fallbackHost}/login`;
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-900 px-4 py-8 relative overflow-hidden">
      {/* Fondo decorativo */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute -top-32 -left-32 w-96 h-96 rounded-full bg-blue-600 opacity-20 blur-3xl" />
        <div className="absolute -bottom-32 -right-32 w-96 h-96 rounded-full bg-indigo-600 opacity-20 blur-3xl" />
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] rounded-full bg-[#243A76] opacity-15 blur-[120px]" />
      </div>

      <div className="w-full max-w-xl bg-white rounded-3xl shadow-2xl overflow-hidden relative z-10 border border-slate-100/10 animate-in fade-in zoom-in duration-200">
        {/* Header institucional */}
        <div className="bg-[#243A76] px-8 pt-8 pb-6 flex flex-col items-center text-center">
          <div className="w-20 h-20 rounded-full overflow-hidden border-4 border-white shadow-xl mb-3 bg-white p-0.5">
            <img src={logo} alt="Logo Provincias Unidas" className="w-full h-full object-cover" />
          </div>
          <h1 className="text-white text-lg font-bold tracking-wide leading-tight">
            Escuela de Educación Básica
          </h1>
          <p className="text-blue-200 text-sm font-extrabold tracking-widest uppercase mt-0.5">
            Provincias Unidas
          </p>
          <p className="text-blue-300 text-xs mt-1">Rcto. San Basilio</p>
        </div>

        {/* Selección de portales */}
        <div className="px-8 py-6">
          <div className="text-center mb-6">
            <h2 className="text-slate-800 text-base font-extrabold">
              Bienvenido(a), <span className="text-[#243A76]">{sesion.username}</span>
            </h2>
            <p className="text-slate-500 text-xs mt-1">
              Tienes acceso a múltiples módulos del sistema. Elige el portal al que deseas ingresar:
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
            {disponibles.map((rol) => {
              const portal = PORTALES[rol] || PORTALES.SECRETARIA;
              const isCurrent = rol === "SECRETARIA";

              return (
                <button
                  key={rol}
                  onClick={() => irAlPortal(rol)}
                  disabled={cargando !== null}
                  className={`flex items-start gap-3.5 p-4 rounded-2xl border text-left transition transform active:scale-98 cursor-pointer shadow-sm hover:shadow-md ${portal.color} disabled:opacity-60`}
                >
                  <div className="w-11 h-11 rounded-xl bg-white flex items-center justify-center shrink-0 shadow-sm border border-slate-200/60 mt-0.5">
                    {cargando === rol ? (
                      <svg className="w-5 h-5 animate-spin text-current" fill="none" viewBox="0 0 24 24">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
                      </svg>
                    ) : (
                      portal.icon
                    )}
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-1.5 justify-between">
                      <span className="block text-xs font-bold text-slate-800">{portal.label}</span>
                      {isCurrent && (
                        <span className="px-1.5 py-0.5 bg-[#243A76] text-white text-[9px] font-bold rounded-md uppercase tracking-wider">
                          Actual
                        </span>
                      )}
                    </div>
                    <span className="block text-[11px] text-slate-500 leading-snug mt-1">
                      {portal.desc}
                    </span>
                  </div>
                </button>
              );
            })}
          </div>

          <div className="text-center mt-6 pt-4 border-t border-slate-100 flex items-center justify-between text-xs text-slate-400">
            <span>SGA Sistema Distribuido</span>
            <button
              onClick={salir}
              className="text-rose-500 hover:text-rose-700 font-semibold transition cursor-pointer"
            >
              Cerrar sesión
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}