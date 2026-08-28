import { useState, createContext, useContext, useCallback } from "react";

const css = `
  .toast-wrap {
    position: fixed; bottom: 30px; right: 20px;
    z-index: 100001; display: flex; flex-direction: column;
    gap: 10px; pointer-events: none;
  }
  .toast {
    display: flex; align-items: flex-start; gap: 12px;
    background: #fff; border-radius: 14px; padding: 14px 16px;
    box-shadow: 0 10px 35px rgba(13,34,68,0.18);
    min-width: 300px; max-width: 420px;
    pointer-events: all; border-left: 5px solid transparent;
    animation: tin 0.35s cubic-bezier(0.16,1,0.3,1);
    font-family: system-ui, -apple-system, sans-serif;
  }
  .toast.out { animation: tout 0.3s ease forwards; }
  @keyframes tin  { from { opacity:0; transform:translateX(50px) scale(0.9); } to { opacity:1; transform:translateX(0) scale(1); } }
  @keyframes tout { from { opacity:1; transform:translateX(0) scale(1); } to { opacity:0; transform:translateX(50px) scale(0.9); } }

  .toast.success { border-left-color: #16a34a; }
  .toast.error   { border-left-color: #dc2626; }
  .toast.warning { border-left-color: #d97706; }
  .toast.info    { border-left-color: #2563eb; }

  .toast-icon { flex-shrink: 0; margin-top: 1px; }
  .toast-body { flex: 1; }
  .toast-title { font-size: 14px; font-weight: 700; color: #0d2244; margin-bottom: 2px; }
  .toast-msg   { font-size: 12.5px; color: #475569; line-height: 1.4; }
  .toast-close {
    background: none; border: none; cursor: pointer;
    color: #a0aec0; padding: 2px; border-radius: 4px;
    display: flex; align-items: center; transition: color 0.15s;
    flex-shrink: 0; font-size: 14px; font-weight: bold;
  }
  .toast-close:hover { color: #1e293b; }
  .toast-bar {
    position: absolute; bottom: 0; left: 0;
    height: 4px; border-radius: 0 0 14px 14px;
    animation: bar linear forwards;
  }
  .toast { position: relative; overflow: hidden; }
  .toast.success .toast-bar { background: #16a34a; }
  .toast.error   .toast-bar { background: #dc2626; }
  .toast.warning .toast-bar { background: #d97706; }
  .toast.info    .toast-bar { background: #2563eb; }
  @keyframes bar { from { width:100%; } to { width:0%; } }

  /* MODAL CONFIRMACIÓN */
  .confirm-overlay {
    position: fixed; inset: 0; background: rgba(13,34,68,0.5);
    backdrop-filter: blur(3px);
    display: flex; align-items: center; justify-content: center;
    z-index: 100000; padding: 24px;
    animation: cfin 0.2s ease;
  }
  @keyframes cfin { from { opacity:0; } to { opacity:1; } }
  .confirm-modal {
    background: #fff; border-radius: 24px; width: 100%; max-width: 400px;
    padding: 28px; box-shadow: 0 25px 70px rgba(13,34,68,0.25);
    animation: min 0.3s cubic-bezier(0.16,1,0.3,1);
    font-family: system-ui, -apple-system, sans-serif;
    text-align: center; display: flex; flex-direction: column; align-items: center;
  }
  @keyframes min { from { opacity:0; transform:scale(0.88); } to { opacity:1; transform:scale(1); } }
  .confirm-icon { width: 56px; height: 56px; border-radius: 18px; display: flex; align-items: center; justify-content: center; margin-bottom: 16px; }
  .confirm-title { font-size: 19px; font-weight: 700; color: #0d2244; margin-bottom: 8px; }
  .confirm-msg   { font-size: 13.5px; color: #64748b; line-height: 1.5; margin-bottom: 24px; }
  .confirm-actions { display: flex; gap: 12px; width: 100%; justify-content: center; }
  .confirm-btn {
    flex: 1; padding: 11px 18px; border-radius: 12px; font-family: system-ui, -apple-system, sans-serif;
    font-size: 14px; font-weight: 600; cursor: pointer; border: none; transition: all 0.15s;
  }
  .confirm-btn-cancel { background: #f1f5f9; color: #475569; }
  .confirm-btn-cancel:hover { background: #e2e8f0; }
  .confirm-btn-danger { background: #dc2626; color: #fff; box-shadow: 0 4px 14px rgba(220,38,38,0.3); }
  .confirm-btn-danger:hover { background: #b91c1c; }
  .confirm-btn-primary { background: #243A76; color: #fff; box-shadow: 0 4px 14px rgba(36,58,118,0.3); }
  .confirm-btn-primary:hover { background: #1a2b58; }
`;

const ToastCtx = createContext(null);
const ConfirmCtx = createContext(null);

export function ToastProvider({ children }) {
  const [toasts, setToasts]   = useState([]);
  const [confirm, setConfirm] = useState(null);

  const addToast = useCallback(({ type = "info", title, message, duration = 4000 }) => {
    const id = Date.now() + Math.random().toString(36).substring(2, 7);
    setToasts(prev => [...prev, { id, type, title, message, duration, out: false }]);
    setTimeout(() => {
      setToasts(prev => prev.map(t => t.id === id ? { ...t, out: true } : t));
      setTimeout(() => setToasts(prev => prev.filter(t => t.id !== id)), 310);
    }, duration);
  }, []);

  const toast = {
    success: (title, message, duration) => addToast({ type: "success", title, message, duration }),
    error:   (title, message, duration) => addToast({ type: "error",   title, message, duration }),
    warning: (title, message, duration) => addToast({ type: "warning", title, message, duration }),
    info:    (title, message, duration) => addToast({ type: "info",    title, message, duration }),
  };

  const showConfirm = useCallback(({ title, message, type = "danger", confirmText = "Confirmar", cancelText = "Cancelar" }) => {
    return new Promise((resolve) => {
      setConfirm({ title, message, type, confirmText, cancelText, resolve });
    });
  }, []);

  const icons = {
    success: (
      <div className="w-7 h-7 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center font-bold">
        ✓
      </div>
    ),
    error: (
      <div className="w-7 h-7 rounded-full bg-rose-100 text-rose-600 flex items-center justify-center font-bold">
        ⚠️
      </div>
    ),
    warning: (
      <div className="w-7 h-7 rounded-full bg-amber-100 text-amber-600 flex items-center justify-center font-bold">
        ⚡
      </div>
    ),
    info: (
      <div className="w-7 h-7 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center font-bold">
        ℹ️
      </div>
    ),
  };

  return (
    <ToastCtx.Provider value={toast}>
      <ConfirmCtx.Provider value={showConfirm}>
        <style>{css}</style>
        {children}

        {/* TOASTS */}
        <div className="toast-wrap">
          {toasts.map(t => (
            <div key={t.id} className={`toast ${t.type}${t.out ? ' out' : ''}`}>
              <div className="toast-icon">{icons[t.type]}</div>
              <div className="toast-body">
                {t.title   && <div className="toast-title">{t.title}</div>}
                {t.message && <div className="toast-msg">{t.message}</div>}
              </div>
              <button className="toast-close"
                onClick={() => setToasts(prev => prev.filter(x => x.id !== t.id))}>
                ✕
              </button>
              <div className="toast-bar" style={{ animationDuration: `${t.duration || 4000}ms` }} />
            </div>
          ))}
        </div>

        {/* MODAL CONFIRMACIÓN */}
        {confirm && (
          <div className="confirm-overlay" onClick={() => { confirm.resolve(false); setConfirm(null); }}>
            <div className="confirm-modal" onClick={e => e.stopPropagation()}>
              <div className="confirm-icon" style={{ background: confirm.type === "danger" ? "#fff1f2" : "#eef3fb" }}>
                {confirm.type === "danger" ? (
                  <span className="text-2xl">⚠️</span>
                ) : (
                  <span className="text-2xl">ℹ️</span>
                )}
              </div>
              <div className="confirm-title">{confirm.title}</div>
              <div className="confirm-msg">{confirm.message}</div>
              <div className="confirm-actions">
                <button
                  className="confirm-btn confirm-btn-cancel"
                  onClick={() => { confirm.resolve(false); setConfirm(null); }}>
                  {confirm.cancelText || "Cancelar"}
                </button>
                <button
                  className={`confirm-btn ${confirm.type === "danger" ? "confirm-btn-danger" : "confirm-btn-primary"}`}
                  onClick={() => { confirm.resolve(true); setConfirm(null); }}>
                  {confirm.confirmText || "Sí, proceder"}
                </button>
              </div>
            </div>
          </div>
        )}
      </ConfirmCtx.Provider>
    </ToastCtx.Provider>
  );
}

export function useToast() {
  return useContext(ToastCtx);
}

export function useConfirm() {
  return useContext(ConfirmCtx);
}
