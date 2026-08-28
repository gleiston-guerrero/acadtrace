import { useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../utils/api";
import logo from "../assets/logo.png";

export default function Login() {
  const [form, setForm] = useState({ username: "", password: "" });
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
    setError("");
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError("");

    try {
      const res = await api.post("/auth/login", form);
      const data = res.data;
      const rawRoles = data.roles || [];
      const roles = Array.isArray(rawRoles) ? rawRoles : [rawRoles];

      const sesion = {
        token: data.token,
        idUsuario: data.idUsuario,
        username: data.username || form.username,
        roles: roles,
        primerIngreso: data.primerIngreso || false,
      };

      localStorage.setItem("token", data.token);
      localStorage.setItem("username", sesion.username);
      localStorage.setItem("roles", JSON.stringify(roles));
      if (data.idUsuario) localStorage.setItem("userId", String(data.idUsuario));

      // Filtrar portales válidos del sistema
      const portalesDisponibles = ["DIRECTOR", "SECRETARIA", "DOCENTE", "SOPORTE_TECNICO"]
        .filter((rol) => roles.includes(rol));

      // Si tiene más de un portal, ir a la pantalla de selección de portales
      if (portalesDisponibles.length > 1) {
        navigate("/portales", { state: sesion });
        return;
      }

      // Si solo tiene un portal o es secretaria, ir al dashboard
      navigate("/dashboard");
    } catch (err) {
      console.error("Error en login:", err);
      if (err.response?.status === 401) {
        setError("Usuario o contraseña incorrectos");
      } else if (err.response?.status === 403) {
        setError("Tu usuario no cuenta con un rol con acceso asignado");
      } else if (err.response?.data?.error) {
        setError(err.response.data.error);
      } else if (err.response?.data?.message) {
        setError(err.response.data.message);
      } else {
        setError(`No se pudo conectar con el servidor: ${err.message || "Error desconocido"}`);
      }
    } finally {
      setLoading(false);
    }
  };

  const handleDevBypass = () => {
    const sesion = {
      token: "dev-token-secretaria-2026",
      username: "admin_general",
      roles: ["DIRECTOR", "SECRETARIA", "DOCENTE", "SOPORTE_TECNICO"],
      idUsuario: 1,
      primerIngreso: false,
    };
    localStorage.setItem("token", sesion.token);
    localStorage.setItem("username", sesion.username);
    localStorage.setItem("roles", JSON.stringify(sesion.roles));
    localStorage.setItem("userId", "1");
    navigate("/portales", { state: sesion });
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-900 px-4 py-8 relative overflow-hidden">
      {/* Fondo decorativo institucional con halos */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute -top-32 -left-32 w-96 h-96 rounded-full bg-blue-600 opacity-20 blur-3xl" />
        <div className="absolute -bottom-32 -right-32 w-96 h-96 rounded-full bg-indigo-600 opacity-20 blur-3xl" />
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] rounded-full bg-[#243A76] opacity-15 blur-[120px]" />
      </div>

      <div className="w-full max-w-sm bg-white rounded-3xl shadow-2xl overflow-hidden relative z-10 border border-slate-100/10">
        {/* Header institucional */}
        <div className="bg-[#243A76] px-8 pt-8 pb-6 flex flex-col items-center relative text-center">
          {/* Logo circular institucional */}
          <div className="w-24 h-24 rounded-full overflow-hidden border-4 border-white shadow-xl mb-3 bg-white p-0.5">
            <img
              src={logo}
              alt="Logo Provincias Unidas"
              className="w-full h-full object-cover"
            />
          </div>
          <h1 className="text-white text-lg font-bold tracking-wide leading-tight">
            Escuela de Educación Básica
          </h1>
          <p className="text-blue-200 text-sm font-extrabold tracking-widest uppercase mt-0.5">
            Provincias Unidas
          </p>
          <p className="text-blue-300 text-xs mt-1">Rcto. San Basilio</p>
          <span className="inline-block mt-3 px-3 py-1 bg-white/15 text-blue-100 text-[10px] font-bold uppercase tracking-wider rounded-full border border-white/20">
            Sistema de Gestión Académica
          </span>
        </div>

        {/* Formulario */}
        <div className="px-8 py-6">
          <h2 className="text-slate-700 text-sm font-semibold mb-5 text-center">
            Inicia sesión con tu cuenta
          </h2>

          <form onSubmit={handleSubmit} className="space-y-4">
            {/* Usuario */}
            <div>
              <label className="block text-[11px] font-bold text-slate-500 mb-1 uppercase tracking-wider">
                Usuario
              </label>
              <div className="relative">
                <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400">
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                          d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                  </svg>
                </span>
                <input
                  type="text"
                  name="username"
                  value={form.username}
                  onChange={handleChange}
                  placeholder="Ingresa tu usuario"
                  required
                  className="w-full pl-10 pr-4 py-2.5 border border-slate-200 rounded-xl text-sm text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-[#243A76] focus:border-transparent transition bg-slate-50"
                />
              </div>
            </div>

            {/* Contraseña */}
            <div>
              <label className="block text-[11px] font-bold text-slate-500 mb-1 uppercase tracking-wider">
                Contraseña
              </label>
              <div className="relative">
                <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400">
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                          d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                  </svg>
                </span>
                <input
                  type={showPassword ? "text" : "password"}
                  name="password"
                  value={form.password}
                  onChange={handleChange}
                  placeholder="Ingresa tu contraseña"
                  required
                  className="w-full pl-10 pr-10 py-2.5 border border-slate-200 rounded-xl text-sm text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-[#243A76] focus:border-transparent transition bg-slate-50"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-[#243A76] transition cursor-pointer"
                >
                  {showPassword ? (
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                            d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 4.411m0 0L21 21" />
                    </svg>
                  ) : (
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                            d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                            d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                    </svg>
                  )}
                </button>
              </div>
            </div>

            {/* Mensaje de Error */}
            {error && (
              <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-rose-700 text-xs flex items-center gap-2 animate-in fade-in duration-200">
                <svg className="w-4 h-4 shrink-0 text-rose-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                        d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <span>{error}</span>
              </div>
            )}

            {/* Botón de Ingreso */}
            <button
              type="submit"
              disabled={loading}
              className="w-full py-3 px-4 rounded-xl text-xs font-bold text-white bg-[#243A76] hover:bg-[#1b2b58] shadow-lg shadow-blue-900/30 transition transform active:scale-98 cursor-pointer flex items-center justify-center gap-2 disabled:opacity-60"
            >
              {loading ? (
                <>
                  <svg className="animate-spin w-4 h-4 text-white" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
                  </svg>
                  <span>Validando credenciales...</span>
                </>
              ) : (
                <span>Iniciar Sesión →</span>
              )}
            </button>
          </form>

          {/* Acceso Rápido con 4 Roles para desarrollo */}
          <div className="mt-6 pt-4 border-t border-slate-100 text-center">
            <button
              type="button"
              onClick={handleDevBypass}
              className="text-[11px] font-semibold text-[#243A76] hover:text-blue-900 transition cursor-pointer underline underline-offset-2"
            >
              Ingresar con los 4 Roles (Ver Selector de Portales)
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}