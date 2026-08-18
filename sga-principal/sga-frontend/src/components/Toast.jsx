import React from "react";

export default function Toast({ toast, onClose }) {
  const { title, message, type } = toast;

  let accentClass = "border-emerald-500 text-emerald-600 bg-emerald-50";
  let icon = (
    <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
    </svg>
  );

  if (type === "error") {
    accentClass = "border-rose-500 text-rose-600 bg-rose-50";
    icon = (
      <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
        <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
      </svg>
    );
  } else if (type === "warning") {
    accentClass = "border-amber-500 text-amber-600 bg-amber-50";
    icon = (
      <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
        <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
      </svg>
    );
  } else if (type === "info") {
    accentClass = "border-blue-500 text-blue-600 bg-blue-50";
    icon = (
      <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
        <path strokeLinecap="round" strokeLinejoin="round" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
    );
  }

  return (
    <div className="bg-white rounded-2xl shadow-xl border border-slate-100 overflow-hidden transition-all transform animate-slide-up flex flex-col relative">
      <div className="p-4 flex items-start gap-3.5 pr-10">
        <div className={`p-2 rounded-full flex-shrink-0 ${accentClass}`}>
          {icon}
        </div>
        <div className="flex-1 min-w-0">
          <h4 className="font-bold text-slate-800 text-sm leading-snug">{title}</h4>
          {message && <p className="text-xs text-slate-500 mt-0.5 leading-relaxed break-words">{message}</p>}
        </div>
        <button
          onClick={onClose}
          className="absolute top-3.5 right-3.5 text-slate-400 hover:text-slate-600 transition p-1 text-sm font-semibold"
        >
          ✕
        </button>
      </div>
      <div className={`h-1.5 w-full ${type === "error" ? "bg-rose-500" : type === "warning" ? "bg-amber-500" : type === "info" ? "bg-blue-500" : "bg-emerald-500"}`} />
    </div>
  );
}
