import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiPrincipal } from '../utils/api';
import { useConfirm } from './Toast';
import logo from '../assets/logo.png';
import AsistenteIaSecretaria from './AsistenteIaSecretaria';

const PRIMARY = '#243A76';
const PRIMARY_LIGHT = '#2d4a96';

export default function Layout({ children, breadcrumb = ['Inicio'], sidebarTitle, menuItems = [], seccion, onSeccionChange, headerRight }) {
  const [showPeriodo, setShowPeriodo] = useState(false);
  const [showUserMenu, setShowUserMenu] = useState(false);
  const [showNotifs, setShowNotifs] = useState(false);
  const [notifs, setNotifs] = useState([]);
  const [noLeidas, setNoLeidas] = useState(0);
  const [anoActual, setAnoActual] = useState(null);
  const navigate = useNavigate();
  const confirm = useConfirm();

  const username = localStorage.getItem('username') || 'Secretario';
  const roles = JSON.parse(localStorage.getItem('roles') || '[]');

  useEffect(() => {
    apiPrincipal.get('/anos-lectivos/actual').then(r => setAnoActual(r.data)).catch(() => {});
  }, []);

  const cargarNotificaciones = () => {
    apiPrincipal.get('/notificaciones/mias').then(r => {
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
    apiPrincipal.post(`/notificaciones/marcar-leida/${n.idNotificacion}`).catch(() => {});
    setNotifs(prev => prev.map(x => x.idNotificacion === n.idNotificacion ? { ...x, leida: true } : x));
    setNoLeidas(prev => Math.max(0, prev - (n.leida ? 0 : 1)));
    setShowNotifs(false);
    if (n.urlDestino) navigate(n.urlDestino);
  };

  const marcarTodasLeidas = () => {
    apiPrincipal.post('/notificaciones/marcar-todas-leidas').catch(() => {});
    setNotifs(prev => prev.map(x => ({ ...x, leida: true })));
    setNoLeidas(0);
  };

  const handleLogout = async () => {
    const ok = await confirm({
      title: '¿Cerrar sesión?',
      message: 'Vas a salir del portal de Secretaría. Cualquier cambio sin guardar se perderá.',
      confirmText: 'Sí, cerrar sesión',
      cancelText: 'Cancelar',
      type: 'danger',
    });
    if (!ok) return;
    localStorage.clear();
    const host = typeof window !== 'undefined' ? window.location.hostname : 'localhost';
    window.location.href = `http://${host}:5173/login`;
  };

  const hasSidebar = menuItems.length > 0;

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">

      {/* TOP BAR — FIJO */}
      <header style={{ backgroundColor: PRIMARY }} className="fixed top-0 left-0 right-0 text-white h-14 flex items-center justify-between px-4 shadow z-40 flex-shrink-0">
        <div className="flex items-center gap-3">
          <img src={logo} alt="Logo" className="w-8 h-8 rounded-full object-cover border-2 border-white border-opacity-40" />
          <span className="font-bold text-sm">SGA</span>
          <span className="text-white text-opacity-60 text-sm hidden sm:inline">| Secretaría</span>
        </div>

        <div className="flex items-center gap-2">
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
              <span className="hidden sm:inline">{anoActual?.nombre || 'Sin período'}</span>
              <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
              </svg>
            </button>
            {showPeriodo && (
              <div className="absolute right-0 top-11 w-64 bg-white rounded-xl shadow-2xl border border-slate-200 z-50 overflow-hidden">
                <div style={{ backgroundColor: PRIMARY }} className="px-4 py-3 flex items-center gap-2">
                  <svg className="w-4 h-4 opacity-80" fill="none" stroke="currentColor" viewBox="0 0 24 24">
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
                  <button
                    onClick={() => { setShowPeriodo(false); navigate('/anos-lectivos'); }}
                    className="w-full mt-2 py-1.5 px-3 text-xs font-semibold text-[#243A76] bg-blue-50 hover:bg-blue-100 rounded-lg text-center transition"
                  >
                    Administrar Años Lectivos →
                  </button>
                </div>
              </div>
            )}
          </div>

          {/* Notificaciones */}
          <div className="relative">
            <button
              onClick={() => { setShowNotifs(!showNotifs); setShowPeriodo(false); setShowUserMenu(false); }}
              className="relative p-2 rounded-lg hover:bg-white hover:bg-opacity-10 transition"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
              </svg>
              {noLeidas > 0 && (
                <span className="absolute top-0 right-0 min-w-[16px] h-4 px-1 flex items-center justify-center rounded-full bg-red-500 text-white text-[10px] font-bold leading-none">
                  {noLeidas > 9 ? '9+' : noLeidas}
                </span>
              )}
            </button>
            {showNotifs && (
              <div className="absolute right-0 top-11 w-80 bg-white rounded-xl shadow-2xl border border-slate-200 z-50 overflow-hidden">
                <div style={{ backgroundColor: PRIMARY }} className="px-4 py-3 flex items-center justify-between">
                  <p className="text-white text-sm font-semibold">Notificaciones</p>
                  {noLeidas > 0 && (
                    <button onClick={marcarTodasLeidas} className="text-white text-opacity-70 hover:text-opacity-100 text-xs">Marcar todas leídas</button>
                  )}
                </div>
                <div className="max-h-80 overflow-y-auto">
                  {notifs.length === 0 && (
                    <p className="text-xs text-slate-400 text-center py-6">Sin notificaciones</p>
                  )}
                  {notifs.map(n => (
                    <button
                      key={n.idNotificacion}
                      onClick={() => abrirNotificacion(n)}
                      className={`w-full text-left px-4 py-3 border-b border-slate-100 last:border-0 hover:bg-slate-50 transition ${n.leida ? '' : 'bg-blue-50/50'}`}
                    >
                      <p className={`text-xs ${n.leida ? 'text-slate-600' : 'text-slate-800 font-semibold'}`}>{n.titulo}</p>
                      {n.mensaje && <p className="text-xs text-slate-400 mt-0.5 line-clamp-2">{n.mensaje}</p>}
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* Usuario */}
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
              <div className="absolute right-0 top-11 w-52 bg-white rounded-xl shadow-2xl border border-slate-200 z-50 overflow-hidden">
                <div style={{ backgroundColor: PRIMARY }} className="px-4 py-3">
                  <p className="text-white text-sm font-semibold capitalize">{username}</p>
                  <p className="text-white text-opacity-60 text-xs">{roles.join(', ') || 'SECRETARIO'}</p>
                </div>
                <div className="p-2">
                  <button
                    onClick={() => navigate('/portales')}
                    className="w-full text-left px-3 py-2 rounded-lg text-sm text-slate-600 hover:bg-slate-50 flex items-center gap-2 transition"
                  >
                    <svg className="w-4 h-4 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z" />
                    </svg>
                    Cambiar de Portal
                  </button>
                  <button
                    onClick={() => navigate('/cambiar-password')}
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

      {/* BREADCRUMB */}
      <div className="bg-white border-b border-slate-200 px-6 py-2 flex items-center justify-between">
        <nav className="text-xs text-slate-500 flex items-center gap-1">
          {breadcrumb.map((item, i) => (
            <span key={i} className="flex items-center gap-1">
              {i > 0 && <span className="text-slate-300">/</span>}
              <span
                style={i === breadcrumb.length - 1 ? { color: PRIMARY } : {}}
                className={i === breadcrumb.length - 1 ? 'font-medium' : 'hover:underline cursor-pointer'}
                onClick={() => i === 0 && navigate('/dashboard')}
              >
                {item}
              </span>
            </span>
          ))}
        </nav>
        {headerRight}
      </div>

      {/* SIDEBAR + CONTENT */}
      <div className="flex flex-1 overflow-hidden">

        {/* SIDEBAR FIJO */}
        {hasSidebar && (
          <aside className="w-56 flex-shrink-0 hidden md:flex flex-col border-r border-slate-200 bg-white overflow-y-auto"
            style={{ height: 'calc(100vh - 14rem)' }}
          >
            <div className="p-3">
              <div className="bg-white rounded-xl border border-slate-200 overflow-hidden shadow-sm">
                {sidebarTitle && (
                  <p className="px-4 pt-3 pb-2 text-[10px] font-semibold uppercase tracking-widest text-slate-400">
                    {sidebarTitle}
                  </p>
                )}
                <nav className="py-1">
                  {menuItems.map(item => (
                    <button
                      key={item.id}
                      onClick={() => item.path ? navigate(item.path) : onSeccionChange?.(item.id)}
                      className={`w-full flex items-center gap-2.5 px-4 py-2.5 text-sm text-left transition ${
                        seccion === item.id
                          ? 'bg-blue-50 font-medium border-l-[3px]'
                          : 'border-l-[3px] border-l-transparent text-slate-500 hover:bg-slate-50'
                      }`}
                      style={seccion === item.id ? { color: PRIMARY, borderLeftColor: PRIMARY } : {}}
                    >
                      {item.icon}
                      {item.label}
                    </button>
                  ))}
                </nav>
                <hr className="border-slate-100" />
                <button
                  onClick={() => navigate('/dashboard')}
                  className="w-full flex items-center gap-2.5 px-4 py-2.5 text-sm text-left text-slate-400 hover:bg-slate-50 transition"
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
                  </svg>
                  Volver al inicio
                </button>
              </div>
            </div>
          </aside>
        )}

        {/* CONTENIDO scrollable */}
        <main className="flex-1 min-w-0 overflow-y-auto p-5" style={{ paddingBottom: '4rem' }}>
          {children}
        </main>
      </div>

      {/* Footer Fijo */}
      <footer style={{ backgroundColor: PRIMARY }} className="fixed bottom-0 left-0 right-0 text-white text-opacity-80 text-xs text-center py-2 z-40">
        Sistema de Gestión Académica — Escuela Provincias Unidas © 2026
      </footer>

      {/* Widget Asistente IA Secretaría */}
      <AsistenteIaSecretaria />

      {/* Overlay */}
      {(showPeriodo || showUserMenu || showNotifs) && (
        <div className="fixed inset-0 z-20" onClick={() => { setShowPeriodo(false); setShowUserMenu(false); setShowNotifs(false); }} />
      )}
    </div>
  );
}
