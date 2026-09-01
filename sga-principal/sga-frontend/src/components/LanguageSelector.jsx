import { useI18n } from "../context/I18nContext";

export default function LanguageSelector({ className = "" }) {
  const { lang, setLang } = useI18n();

  return (
    <div className={`flex items-center rounded-lg bg-white/10 p-0.5 border border-white/20 text-xs ${className}`}>
      <button
        onClick={() => setLang("es")}
        className={`px-2 py-1 rounded font-medium transition-all ${
          lang === "es"
            ? "bg-white text-slate-900 shadow-sm"
            : "text-white/80 hover:text-white"
        }`}
        title="Español"
      >
        🇪🇸 ES
      </button>
      <button
        onClick={() => setLang("en")}
        className={`px-2 py-1 rounded font-medium transition-all ${
          lang === "en"
            ? "bg-white text-slate-900 shadow-sm"
            : "text-white/80 hover:text-white"
        }`}
        title="English"
      >
        🇺🇸 EN
      </button>
    </div>
  );
}
