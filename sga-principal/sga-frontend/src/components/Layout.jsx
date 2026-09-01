import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useConfirm } from "../context/ConfirmContext";
import { useI18n } from "../context/I18nContext";
import ThemeToggle from "./ThemeToggle";
import LanguageSelector from "./LanguageSelector";
import api from "../config/axios";
import logo from "../assets/logo.png";

const PRIMARY = "#243A76";
const PRIMARY_LIGHT = "#2d4a96";

export default function Layout({ children, breadcrumb = ["Inicio"], sidebarTitle, menuItems = [], seccion, onSeccionChange }) {
  const [showPeriodo, setShowPeriodo] = useState(false);
  const [showUserMenu, setShowUserMenu] = useState(false);
  const [showNotifs, setShowNotifs] = useState(false);
  const [notifs, setNotifs] = useState([]);
  const [noLeidas, setNoLeidas] = useState(0);
  const [anoActual, setAnoActual] = useState(null);
  const navigate = useNavigate();
  const { t } = useI18n();

  const username = localStorage.getItem("username") || "Usuario";
  const roles = JSON.parse(localStorage.getItem("roles") || "[]");

  useEffect(() => {
    api.get(`/api/anos-lectivos/actual`)
      .then(r => setAnoActual(r.data))
      .catch(() => {});
  }, []);

  const cargarNotificaciones = () => {
    api.get("/api/notificaciones/mias").then(r => {
      setNotifs(r.data?.notificaciones || []);
      setNoLeidas(r.data?.noLeidas || 0);
    }).catch(() => {});
  };

  useEffect(() => {
    cargarNotificaciones();
    const interval = setInterval(cargarNotificaciones, 30000);
    return () => clearInterval(interval);
  }, []);

  const abrirNotificacion = (n) => {
    api.post(`/api/notificaciones/marcar-leida/${n.idNotificacion}`).catch(() => {});
    setNotifs(prev => prev.map(x => x.idNotificacion === n.idNotificacion ? { ...x, leida: true } : x));
    setNoLeidas(prev => Math.max(0, prev - (n.leida ? 0 : 1)));
    setShowNotifs(false);
    if (n.urlDestino) navigate(n.urlDestino);
  };

  const marcarTodasLeidas = () => {
    api.post("/api/notificaciones/marcar-todas-leidas").catch(() => {});
    setNotifs(prev => prev.map(x => ({ ...x, leida: true })));
    setNoLeidas(0);
  };

  const confirm = useConfirm();

  const handleLogout = async () => {
    const isOk = await confirm({
      title: "¿Cerrar sesión?",
      message: "Tu sesión actual se cerrará y tendrás que volver a ingresar.",
      confirmText: "Sí, cerrar sesión",
      cancelText: "Cancelar",
      type: "danger"
    });
    if (isOk) {
      localStorage.clear();
      navigate("/login");
    }
  };

  const hasSidebar = menuItems.length > 0;

  return (
    <div className="flex flex-col min-h-screen bg-slate-50 dark:bg-slate-900 text-slate-800 dark:text-slate-100 transition-colors duration-200">

      {/* TOP BAR — FIJO */}
      <header style={{ backgroundColor: PRIMARY }} className="fixed top-0 left-0 right-0 text-white h-14 flex items-center justify-between px-4 shadow z-40 flex-shrink-0">
        <div className="flex items-center gap-3 cursor-pointer" onClick={() => navigate("/dashboard")}>
          <img src={logo} alt="Logo" className="w-8 h-8 rounded-full object-cover border-2 border-white border-opacity-40" />
          <span className="font-bold text-sm tracking-tight">SGA</span>
          <span className="text-white text-opacity-60 text-sm hidden sm:inline">| {t("app.title")}</span>
        </div>

        <div className="flex items-center gap-2">
          {/* Selector de Idioma (i18n) */}
          <LanguageSelector />

          {/* Selector de Tema (Claro / Oscuro) */}
          <ThemeToggle />

          {/* Período */}
          <div className="relative">
            <button
              onClick={() => { setShowPeriodo(!showPeriodo); setShowUserMenu(false); setShowNotifs(false); }}
              style={{ backgroundColor: PRIMARY_LIGHT }}
              className="flex items-center gap-2 hover:opacity-90 px-3 py-1.5 rounded-lg text-xs font-medium transition"
            >
              <svg className="w-4 h-4 opacity-80" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
              </svg>
              <span className="hidden sm:inline">{anoActual?.nombre || t("nav.no_period")}</span>
              <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
              </svg>
            </button>
            {showPeriodo && (
              <div className="absolute right-0 top-11 w-64 bg-white dark:bg-slate-800 rounded-xl shadow-2xl border border-slate-200 dark:border-slate-700 z-50 overflow-hidden text-slate-800 dark:text-slate-100">
                <div style={{ backgroundColor: PRIMARY }} className="px-4 py-3 flex items-center gap-2 text-white">
                  <svg className="w-4 h-4 opacity-80" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                  </svg>
                  <span className="text-white text-sm font-semibold">{t("nav.current_period")}</span>
                </div>
                <div className="p-3">
                  {anoActual ? (
                    <div style={{ color: PRIMARY }} className="flex items-center gap-2 px-3 py-2 rounded-lg bg-blue-50 dark:bg-blue-900/30 dark:text-blue-300 font-semibold text-sm">
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                      </svg>
                      {anoActual.nombre} (Actual)
                    </div>
                  ) : (
                    <p className="text-xs text-slate-400 text-center px-2">{t("nav.no_period")}</p>
                  )}
                </div>
              </div>
            )}
          </div>

          {/* Notificaciones */}
          <div className="relative">
            <button
              onClick={() => { setShowNotifs(!showNotifs); setShowPeriodo(false); setShowUserMenu(false); }}
              className="relative p-2 rounded-lg hover:bg-white/10 transition"
              title={t("nav.notifications")}
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
              </svg>
              {noLeidas > 0 && (
                <span className="absolute top-0 right-0 min-w-[16px] h-4 px-1 flex items-center justify-center rounded-full bg-red-500 text-white text-[10px] font-bold leading-none">
                  {noLeidas > 9 ? "9+" : noLeidas}
                </span>
              )}
            </button>
            {showNotifs && (
              <div className="absolute right-0 top-11 w-80 bg-white dark:bg-slate-800 rounded-xl shadow-2xl border border-slate-200 dark:border-slate-700 z-50 overflow-hidden text-slate-800 dark:text-slate-100">
                <div style={{ backgroundColor: PRIMARY }} className="px-4 py-3 flex items-center justify-between text-white">
                  <p className="text-white text-sm font-semibold">{t("nav.notifications")}</p>
                  {noLeidas > 0 && (
                    <button onClick={marcarTodasLeidas} className="text-white text-opacity-70 hover:text-opacity-100 text-xs">{t("nav.mark_all_read")}</button>
                  )}
                </div>
                <div className="max-h-80 overflow-y-auto">
                  {notifs.length === 0 && (
                    <p className="text-xs text-slate-400 text-center py-6">{t("nav.no_notifications")}</p>
                  )}
                  {notifs.map(n => (
                    <button
                      key={n.idNotificacion}
                      onClick={() => abrirNotificacion(n)}
                      className={`w-full text-left px-4 py-3 border-b border-slate-100 dark:border-slate-700 last:border-0 hover:bg-slate-50 dark:hover:bg-slate-700/50 transition ${n.leida ? "" : "bg-blue-50/50 dark:bg-blue-950/30"}`}
                    >
                      <p className={`text-xs ${n.leida ? "text-slate-600 dark:text-slate-400" : "text-slate-800 dark:text-slate-200 font-semibold"}`}>{n.titulo}</p>
                      {n.mensaje && <p className="text-xs text-slate-400 mt-0.5 line-clamp-2">{n.mensaje}</p>}
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* Usuario Menu */}
          <div className="relative">
            <button
              onClick={() => { setShowUserMenu(!showUserMenu); setShowPeriodo(false); setShowNotifs(false); }}
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
              <div className="absolute right-0 top-11 w-56 bg-white dark:bg-slate-800 rounded-xl shadow-2xl border border-slate-200 dark:border-slate-700 z-50 overflow-hidden text-slate-800 dark:text-slate-100">
                <div style={{ backgroundColor: PRIMARY }} className="px-4 py-3 text-white">
                  <p className="text-white text-sm font-semibold capitalize">{username}</p>
                  <p className="text-white text-opacity-60 text-xs">{roles.join(", ") || "SIN ROL"}</p>
                </div>
                <div className="p-2 space-y-1">
                  <button
                    onClick={() => { setShowUserMenu(false); navigate("/settings"); }}
                    className="w-full text-left px-3 py-2 rounded-lg text-xs font-medium text-slate-700 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-700 flex items-center gap-2 transition"
                  >
                    <svg className="w-4 h-4 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                    </svg>
                    {t("nav.settings")}
                  </button>
                  <button
                    onClick={() => { setShowUserMenu(false); navigate("/about"); }}
                    className="w-full text-left px-3 py-2 rounded-lg text-xs font-medium text-slate-700 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-700 flex items-center gap-2 transition"
                  >
                    <svg className="w-4 h-4 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    {t("nav.about")}
                  </button>
                  <button
                    onClick={() => { setShowUserMenu(false); navigate("/cambiar-password"); }}
                    className="w-full text-left px-3 py-2 rounded-lg text-xs font-medium text-slate-700 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-700 flex items-center gap-2 transition"
                  >
                    <svg className="w-4 h-4 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z" />
                    </svg>
                    {t("nav.change_password")}
                  </button>
                  <hr className="my-1 border-slate-100 dark:border-slate-700" />
                  <button
                    onClick={handleLogout}
                    className="w-full text-left px-3 py-2 rounded-lg text-xs text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/30 flex items-center gap-2 transition font-semibold"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                    </svg>
                    {t("nav.logout")}
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </header>

      {/* Spacer del header fijo */}
      <div className="h-14 flex-shrink-0" />

      {/* BREADCRUMB */}
      <div className="bg-white dark:bg-slate-800 border-b border-slate-200 dark:border-slate-700 px-6 py-2 flex items-center justify-between">
        <nav className="text-xs text-slate-500 dark:text-slate-400 flex items-center gap-1">
          {breadcrumb.map((item, i) => (
            <span key={i} className="flex items-center gap-1">
              {i > 0 && <span className="text-slate-300 dark:text-slate-600">/</span>}
              <span
                style={i === breadcrumb.length - 1 ? { color: PRIMARY } : {}}
                className={i === breadcrumb.length - 1 ? "font-semibold dark:text-blue-400" : "hover:underline cursor-pointer"}
                onClick={() => i === 0 && navigate("/dashboard")}
              >
                {item}
              </span>
            </span>
          ))}
        </nav>
      </div>

      {/* CONTENIDO CON O SIN SIDEBAR */}
      <div className="flex flex-1">
        {hasSidebar && (
          <aside className="w-56 bg-white dark:bg-slate-800 border-r border-slate-200 dark:border-slate-700 flex-shrink-0 p-3 hidden md:block">
            {sidebarTitle && (
              <p className="text-[11px] font-bold uppercase tracking-wider text-slate-400 dark:text-slate-500 px-3 mb-2">
                {sidebarTitle}
              </p>
            )}
            <nav className="space-y-1">
              {menuItems.map(item => (
                <button
                  key={item.id}
                  onClick={() => onSeccionChange && onSeccionChange(item.id)}
                  className={`w-full text-left px-3 py-2 rounded-lg text-xs font-medium flex items-center gap-2 transition ${
                    seccion === item.id
                      ? "bg-blue-50 dark:bg-blue-900/40 text-blue-700 dark:text-blue-300 font-bold"
                      : "text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700"
                  }`}
                >
                  {item.icon}
                  {item.label}
                </button>
              ))}
            </nav>
          </aside>
        )}

        <main className="flex-1 p-6 overflow-y-auto">
          {children}
        </main>
      </div>
    </div>
  );
}
