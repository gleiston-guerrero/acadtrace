import React from "react";

export default function ConfirmModal({ config, onConfirm, onCancel }) {
  if (!config) return null;
  const { title, message, confirmText, cancelText, type } = config;

  let iconBg = "bg-rose-50 text-rose-600";
  let btnBg = "bg-rose-600 hover:bg-rose-700 text-white";
  let icon = (
    <svg className="w-8 h-8" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
    </svg>
  );

  if (type === "primary" || type === "info") {
    iconBg = "bg-blue-50 text-blue-600";
    btnBg = "bg-[#243A76] hover:opacity-90 text-white";
    icon = (
      <svg className="w-8 h-8" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
        <path strokeLinecap="round" strokeLinejoin="round" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
    );
  }

  return (
    <div className="fixed inset-0 z-[99999] flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs transition-opacity animate-fade-in">
      <div className="bg-white rounded-3xl p-6 shadow-2xl max-w-sm w-full text-center transform transition-all animate-scale-up border border-slate-100 flex flex-col items-center">
        {/* Icono central de advertencia en fondo suave */}
        <div className={`p-3.5 rounded-2xl mb-4 ${iconBg} inline-flex items-center justify-center`}>
          {icon}
        </div>

        {/* Título en negrita */}
        <h3 className="text-xl font-bold text-slate-800 tracking-tight leading-snug">
          {title}
        </h3>

        {/* Descripción corta */}
        {message && (
          <p className="text-sm text-slate-500 mt-2 mb-6 leading-relaxed px-2">
            {message}
          </p>
        )}

        {/* Botones de Acción */}
        <div className="flex gap-3 w-full justify-center pt-2">
          <button
            type="button"
            onClick={onCancel}
            className="flex-1 py-2.5 px-4 rounded-xl text-sm font-semibold text-slate-600 bg-slate-100 hover:bg-slate-200 transition"
          >
            {cancelText || "Cancelar"}
          </button>
          <button
            type="button"
            onClick={onConfirm}
            className={`flex-1 py-2.5 px-4 rounded-xl text-sm font-bold shadow-md transition ${btnBg}`}
          >
            {confirmText || "Sí, proceder"}
          </button>
        </div>
      </div>
    </div>
  );
}
