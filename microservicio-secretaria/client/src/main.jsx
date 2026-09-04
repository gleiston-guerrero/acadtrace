import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'

// ── Captura de sesión (SSO handoff) ──────────────────────────
// El SGA Principal redirige aquí con el token en el fragmento (#) del URL.
// Lo leemos, lo guardamos en NUESTRO propio localStorage y limpiamos el URL.
function capturarSesionSSO() {
  const hash = window.location.hash?.startsWith("#") ? window.location.hash.substring(1) : "";
  if (!hash) return;
  const p = new URLSearchParams(hash);
  const token = p.get("token");
  if (!token) return;

  localStorage.setItem("token", token);
  localStorage.setItem("username", p.get("username") || "");
  localStorage.setItem("roles", p.get("roles") || "[]");
  localStorage.setItem("primerIngreso", p.get("primerIngreso") || "false");
  if (p.get("idUsuario")) {
    localStorage.setItem("userId", p.get("idUsuario"));
  }

  // Quita el token del URL para no dejarlo expuesto.
  window.history.replaceState({}, document.title, window.location.pathname);
}
capturarSesionSSO();

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
