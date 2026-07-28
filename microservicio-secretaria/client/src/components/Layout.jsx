import { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { apiPrincipal } from '../utils/api';
import logo from '../assets/logo.png';

const PRIMARY = '#1e3a8a';
const PRIMARY_LIGHT = '#2d4a96';

export default function Layout({ children, breadcrumb = ['Inicio'], sidebarTitle, menuItems = [], seccion, onSeccionChange }) {
  const [showUserMenu, setShowUserMenu] = useState(false);
  const [anoActual, setAnoActual] = useState(null);
  const navigate = useNavigate();
  const location = useLocation();

  const username = localStorage.getItem('username') || 'Secretario';
  const roles = JSON.parse(localStorage.getItem('roles') || '[]');

  useEffect(() => {
    apiPrincipal.get('/anos-lectivos/actual').then(r => setAnoActual(r.data)).catch(() => {});
  }, []);

  // El login vive en el SGA Principal: cerrar sesión vuelve allá, no a una ruta local.
  const handleLogout = () => { localStorage.clear(); window.location.href = 'http://localhost:5173/login'; };

  return (
    <div className="flex flex-col min-h-screen bg-slate-50">

      {/* TOP BAR */}
      <header style={{ backgroundColor: PRIMARY }} className="h-14 flex items-center justify-between px-4 shadow z-30 flex-shrink-0 text-white">
        <div className="flex items-center gap-3">
          <img src={logo} alt="Logo" className="w-8 h-8 rounded-full object-cover border-2 border-white/40" />
          <div>
            <span className="font-bold text-sm">SGA</span>
            <span className="text-white/60 text-sm hidden sm:inline"> | Secretaría</span>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {anoActual && (
            <div style={{ backgroundColor: PRIMARY_LIGHT }} className="hidden sm:flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium">
              <svg className="w-3.5 h-3.5 opacity-70" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" /></svg>
              {anoActual.nombre}
            </div>
          )}

          <div className="relative">
            <button
              onClick={() => setShowUserMenu(!showUserMenu)}
              style={{ backgroundColor: PRIMARY_LIGHT }}
              className="flex items-center gap-2 hover:opacity-90 px-3 py-1.5 rounded-lg transition"
            >
              <div className="w-7 h-7 bg-white/20 rounded-full flex items-center justify-center text-xs font-bold uppercase border border-white/30">
                {username.charAt(0)}
              </div>
              <span className="hidden sm:inline text-xs font-medium capitalize">{username}</span>
              <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" /></svg>
            </button>
            {showUserMenu && (
              <div className="absolute right-0 top-11 w-52 bg-white rounded-xl shadow-2xl border border-slate-200 z-50 overflow-hidden">
                <div style={{ backgroundColor: PRIMARY }} className="px-4 py-3">
                  <p className="text-white text-sm font-semibold capitalize">{username}</p>
                  <p className="text-white/60 text-xs">{roles.join(', ') || 'SECRETARIO'}</p>
                </div>
                <div className="p-2">
                  <button onClick={() => navigate('/cambiar-password')} className="w-full text-left px-3 py-2 rounded-lg text-sm text-slate-600 hover:bg-slate-50 flex items-center gap-2 transition">
                    <svg className="w-4 h-4 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z" /></svg>
                    Cambiar contraseña
                  </button>
                  <hr className="my-1 border-slate-100" />
                  <button onClick={handleLogout} className="w-full text-left px-3 py-2 rounded-lg text-sm text-red-600 hover:bg-red-50 flex items-center gap-2 transition font-medium">
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" /></svg>
                    Cerrar sesión
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </header>

      <div className="flex flex-1 overflow-hidden">
        {/* SIDEBAR (opcional) */}
        {menuItems.length > 0 && (
          <aside className="w-56 flex-shrink-0 hidden md:flex flex-col border-r border-slate-200 bg-white overflow-y-auto">
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
                      onClick={() => onSeccionChange?.(item.id)}
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

        {/* MAIN */}
        <div className="flex-1 flex flex-col overflow-hidden">
          {/* Breadcrumb */}
          <div className="bg-white border-b border-slate-200 px-5 py-2 flex-shrink-0">
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
          </div>
          <main className="flex-1 overflow-y-auto p-5">{children}</main>
        </div>
      </div>

      <footer style={{ backgroundColor: PRIMARY }} className="text-white/70 text-xs text-center py-2 flex-shrink-0">
        Sistema de Gestión Académica — Escuela Provincias Unidas © 2026
      </footer>

      {showUserMenu && <div className="fixed inset-0 z-20" onClick={() => setShowUserMenu(false)} />}
    </div>
  );
}