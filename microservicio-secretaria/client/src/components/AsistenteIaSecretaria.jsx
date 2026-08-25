import { useState } from "react";
import api from "../utils/api";

export default function AsistenteIaSecretaria() {
  const [open, setOpen] = useState(false);
  const [tab, setTab] = useState("chat"); // 'chat' | 'diagnostico' | 'citacion'

  // Estado del Chat
  const [pregunta, setPregunta] = useState("");
  const [mensajes, setMensajes] = useState([
    {
      tipo: "ia",
      texto: "¡Hola! Soy tu Asistente IA de Secretaría. Puedo ayudarte con normativas LOEI, redacción de citaciones a representantes o análisis predictivo de rendimiento estudiantil.",
    },
  ]);
  const [loadingChat, setLoadingChat] = useState(false);

  // Estado del Diagnóstico
  const [diagForm, setDiagForm] = useState({
    estudiante: "Carlos Andrés Mendoza",
    materia: "Matemática",
    grado: "8vo Grado EGB",
    trimestre: 1,
    promedio: 6.45,
    porcentajeAsistencia: 82.5,
  });
  const [resultadoDiag, setResultadoDiag] = useState(null);
  const [loadingDiag, setLoadingDiag] = useState(false);

  // Estado de Citación
  const [citForm, setCitForm] = useState({
    estudiante: "Carlos Andrés Mendoza",
    representante: "Sra. María Mendoza",
    motivo: "Promedio menor a 7.00 en Matemática y asistencia irregular",
    fechaCitacion: "2026-09-01",
    horaCitacion: "08:30 AM",
  });
  const [docCitacion, setDocCitacion] = useState("");
  const [loadingCit, setLoadingCit] = useState(false);

  // Enviar mensaje al chat IA
  const handleSendChat = async (e) => {
    e?.preventDefault();
    if (!pregunta.trim() || loadingChat) return;

    const userMsg = pregunta;
    setMensajes((prev) => [...prev, { tipo: "user", texto: userMsg }]);
    setPregunta("");
    setLoadingChat(true);

    try {
      const res = await api.post("/ia/asistente", { pregunta: userMsg });
      setMensajes((prev) => [
        ...prev,
        { tipo: "ia", texto: res.data.respuesta || "Consulta procesada con éxito." },
      ]);
    } catch (err) {
      setMensajes((prev) => [
        ...prev,
        {
          tipo: "ia",
          texto: "Hubo un inconveniente al consultar con el motor de IA. Por favor, intenta de nuevo.",
        },
      ]);
    } finally {
      setLoadingChat(false);
    }
  };

  // Ejecutar diagnóstico pedagógico
  const handleDiagnosticar = async (e) => {
    e.preventDefault();
    setLoadingDiag(true);
    try {
      const res = await api.post("/ia/diagnostico-estudiante", {
        ...diagForm,
        promedio: parseFloat(diagForm.promedio),
        porcentajeAsistencia: parseFloat(diagForm.porcentajeAsistencia),
        idMatricula: 1,
      });
      setResultadoDiag(res.data);
    } catch (err) {
      alert("Error al generar diagnóstico: " + (err.response?.data?.error || err.message));
    } finally {
      setLoadingDiag(false);
    }
  };

  // Generar citación
  const handleGenerarCitacion = async (e) => {
    e.preventDefault();
    setLoadingCit(true);
    try {
      const res = await api.post("/ia/generar-citacion", {
        ...citForm,
        idMatricula: 1,
      });
      setDocCitacion(res.data.documentoCitacion);
    } catch (err) {
      alert("Error al redactar citación: " + (err.response?.data?.error || err.message));
    } finally {
      setLoadingCit(false);
    }
  };

  return (
    <>
      {/* Botón flotante animado */}
      <button
        onClick={() => setOpen(!open)}
        className="fixed bottom-6 right-6 z-50 flex items-center gap-2.5 px-4 py-3 bg-[#243A76] hover:bg-[#1b2b58] text-white rounded-full shadow-2xl shadow-blue-900/50 border border-white/20 transition-all transform hover:scale-105 active:scale-95 cursor-pointer"
        title="Asistente IA de Secretaría"
      >
        <span className="flex h-3 w-3 relative">
          <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-cyan-400 opacity-75"></span>
          <span className="relative inline-flex rounded-full h-3 w-3 bg-cyan-500"></span>
        </span>
        <span className="text-base">🤖</span>
        <span className="text-xs font-bold tracking-wide">Asistente IA</span>
      </button>

      {/* Ventana Modal Flotante */}
      {open && (
        <div className="fixed bottom-20 right-6 z-50 w-[440px] max-w-[92vw] h-[580px] max-h-[85vh] bg-white rounded-3xl shadow-2xl border border-slate-200 flex flex-col overflow-hidden animate-in fade-in slide-in-from-bottom-5 duration-200">
          {/* Header */}
          <div className="bg-[#243A76] px-5 py-4 text-white flex items-center justify-between shrink-0">
            <div className="flex items-center gap-2.5">
              <div className="w-8 h-8 rounded-xl bg-white/10 flex items-center justify-center text-lg border border-white/15">
                ✨
              </div>
              <div>
                <h3 className="text-sm font-bold leading-tight">Asistente IA Secretaría</h3>
                <p className="text-[10px] text-blue-200">Google Gemini & Normativa LOEI</p>
              </div>
            </div>
            <button
              onClick={() => setOpen(false)}
              className="text-white/60 hover:text-white text-lg font-bold p-1 transition cursor-pointer"
            >
              ✕
            </button>
          </div>

          {/* Navegación por pestañas */}
          <div className="flex border-b border-slate-200 bg-slate-50 text-xs font-semibold shrink-0">
            <button
              onClick={() => setTab("chat")}
              className={`flex-1 py-2.5 text-center transition cursor-pointer ${
                tab === "chat"
                  ? "border-b-2 border-[#243A76] text-[#243A76] bg-white font-bold"
                  : "text-slate-500 hover:text-slate-800"
              }`}
            >
              💬 Consultas
            </button>
            <button
              onClick={() => setTab("diagnostico")}
              className={`flex-1 py-2.5 text-center transition cursor-pointer ${
                tab === "diagnostico"
                  ? "border-b-2 border-[#243A76] text-[#243A76] bg-white font-bold"
                  : "text-slate-500 hover:text-slate-800"
              }`}
            >
              📊 Diagnóstico IA
            </button>
            <button
              onClick={() => setTab("citacion")}
              className={`flex-1 py-2.5 text-center transition cursor-pointer ${
                tab === "citacion"
                  ? "border-b-2 border-[#243A76] text-[#243A76] bg-white font-bold"
                  : "text-slate-500 hover:text-slate-800"
              }`}
            >
              ✉️ Citaciones
            </button>
          </div>

          {/* Contenido según pestaña */}
          <div className="flex-1 overflow-y-auto p-4 text-xs">
            {/* Pestaña 1: Chatbot */}
            {tab === "chat" && (
              <div className="flex flex-col h-full justify-between gap-3">
                <div className="flex-1 overflow-y-auto space-y-3 pr-1">
                  {mensajes.map((m, idx) => (
                    <div
                      key={idx}
                      className={`flex ${m.tipo === "user" ? "justify-end" : "justify-start"}`}
                    >
                      <div
                        className={`max-w-[85%] rounded-2xl px-3.5 py-2.5 text-xs leading-relaxed ${
                          m.tipo === "user"
                            ? "bg-[#243A76] text-white rounded-br-none"
                            : "bg-slate-100 text-slate-800 border border-slate-200/80 rounded-bl-none"
                        }`}
                      >
                        {m.texto}
                      </div>
                    </div>
                  ))}
                  {loadingChat && (
                    <div className="flex justify-start">
                      <div className="bg-slate-100 border border-slate-200 rounded-2xl px-3.5 py-2 text-slate-500 text-xs flex items-center gap-2">
                        <svg className="animate-spin w-3.5 h-3.5 text-[#243A76]" fill="none" viewBox="0 0 24 24">
                          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
                        </svg>
                        <span>Generando respuesta...</span>
                      </div>
                    </div>
                  )}
                </div>

                <form onSubmit={handleSendChat} className="flex gap-2 shrink-0 pt-2 border-t border-slate-100">
                  <input
                    type="text"
                    value={pregunta}
                    onChange={(e) => setPregunta(e.target.value)}
                    placeholder="Haz una pregunta sobre normativas, actas o matrículas..."
                    className="flex-1 px-3.5 py-2 border border-slate-200 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-[#243A76]"
                  />
                  <button
                    type="submit"
                    disabled={loadingChat || !pregunta.trim()}
                    className="px-3.5 py-2 bg-[#243A76] hover:bg-[#1b2b58] text-white rounded-xl font-bold transition disabled:opacity-50 cursor-pointer"
                  >
                    ➤
                  </button>
                </form>
              </div>
            )}

            {/* Pestaña 2: Diagnóstico Pedagógico */}
            {tab === "diagnostico" && (
              <div className="space-y-3">
                <form onSubmit={handleDiagnosticar} className="space-y-2.5 bg-slate-50 p-3 rounded-2xl border border-slate-200">
                  <div className="grid grid-cols-2 gap-2">
                    <div>
                      <label className="block text-[10px] font-bold text-slate-500 uppercase">Estudiante</label>
                      <input
                        type="text"
                        value={diagForm.estudiante}
                        onChange={(e) => setDiagForm({ ...diagForm, estudiante: e.target.value })}
                        required
                        className="w-full px-2 py-1.5 border border-slate-200 rounded-lg text-xs bg-white"
                      />
                    </div>
                    <div>
                      <label className="block text-[10px] font-bold text-slate-500 uppercase">Materia</label>
                      <input
                        type="text"
                        value={diagForm.materia}
                        onChange={(e) => setDiagForm({ ...diagForm, materia: e.target.value })}
                        required
                        className="w-full px-2 py-1.5 border border-slate-200 rounded-lg text-xs bg-white"
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-2">
                    <div>
                      <label className="block text-[10px] font-bold text-slate-500 uppercase">Promedio (/10)</label>
                      <input
                        type="number"
                        step="0.01"
                        min="0"
                        max="10"
                        value={diagForm.promedio}
                        onChange={(e) => setDiagForm({ ...diagForm, promedio: e.target.value })}
                        required
                        className="w-full px-2 py-1.5 border border-slate-200 rounded-lg text-xs bg-white"
                      />
                    </div>
                    <div>
                      <label className="block text-[10px] font-bold text-slate-500 uppercase">Asistencia (%)</label>
                      <input
                        type="number"
                        step="0.1"
                        min="0"
                        max="100"
                        value={diagForm.porcentajeAsistencia}
                        onChange={(e) => setDiagForm({ ...diagForm, porcentajeAsistencia: e.target.value })}
                        required
                        className="w-full px-2 py-1.5 border border-slate-200 rounded-lg text-xs bg-white"
                      />
                    </div>
                  </div>

                  <button
                    type="submit"
                    disabled={loadingDiag}
                    className="w-full py-2 bg-[#243A76] hover:bg-[#1b2b58] text-white font-bold rounded-xl transition text-xs flex items-center justify-center gap-1.5 cursor-pointer disabled:opacity-60"
                  >
                    {loadingDiag ? "Analizando con IA..." : "🔍 Evaluar Riesgo y Rendimiento"}
                  </button>
                </form>

                {resultadoDiag && (
                  <div className="p-3.5 bg-white border border-slate-200 rounded-2xl space-y-2.5 shadow-sm animate-in fade-in">
                    <div className="flex items-center justify-between">
                      <span className="font-bold text-slate-800">{resultadoDiag.estudiante}</span>
                      <span
                        className={`px-2 py-0.5 rounded-full text-[10px] font-extrabold uppercase ${
                          resultadoDiag.nivelRiesgo === "ALTO"
                            ? "bg-rose-100 text-rose-700"
                            : resultadoDiag.nivelRiesgo === "MEDIO"
                            ? "bg-amber-100 text-amber-700"
                            : "bg-emerald-100 text-emerald-700"
                        }`}
                      >
                        Riesgo {resultadoDiag.nivelRiesgo}
                      </span>
                    </div>

                    <div className="p-2 bg-slate-50 rounded-xl text-[11px] space-y-1">
                      <p><strong className="text-slate-600">Escala:</strong> {resultadoDiag.escalaCualitativa}</p>
                      <p><strong className="text-slate-600">Recomendación:</strong> {resultadoDiag.recomendacionPedagogica}</p>
                    </div>

                    {resultadoDiag.alertaRepresentante && (
                      <div className="p-2 bg-rose-50 border border-rose-200 rounded-xl text-rose-800 text-[11px] flex items-center gap-2">
                        <span>⚠️</span>
                        <span>Requiere citación formal a representante legal.</span>
                      </div>
                    )}
                  </div>
                )}
              </div>
            )}

            {/* Pestaña 3: Generador de Citaciones */}
            {tab === "citacion" && (
              <div className="space-y-3">
                <form onSubmit={handleGenerarCitacion} className="space-y-2 bg-slate-50 p-3 rounded-2xl border border-slate-200">
                  <div className="grid grid-cols-2 gap-2">
                    <div>
                      <label className="block text-[10px] font-bold text-slate-500 uppercase">Estudiante</label>
                      <input
                        type="text"
                        value={citForm.estudiante}
                        onChange={(e) => setCitForm({ ...citForm, estudiante: e.target.value })}
                        required
                        className="w-full px-2 py-1.5 border border-slate-200 rounded-lg text-xs bg-white"
                      />
                    </div>
                    <div>
                      <label className="block text-[10px] font-bold text-slate-500 uppercase">Representante</label>
                      <input
                        type="text"
                        value={citForm.representante}
                        onChange={(e) => setCitForm({ ...citForm, representante: e.target.value })}
                        required
                        className="w-full px-2 py-1.5 border border-slate-200 rounded-lg text-xs bg-white"
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-[10px] font-bold text-slate-500 uppercase">Motivo</label>
                    <input
                      type="text"
                      value={citForm.motivo}
                      onChange={(e) => setCitForm({ ...citForm, motivo: e.target.value })}
                      required
                      className="w-full px-2 py-1.5 border border-slate-200 rounded-lg text-xs bg-white"
                    />
                  </div>

                  <div className="grid grid-cols-2 gap-2">
                    <div>
                      <label className="block text-[10px] font-bold text-slate-500 uppercase">Fecha Convocatoria</label>
                      <input
                        type="date"
                        value={citForm.fechaCitacion}
                        onChange={(e) => setCitForm({ ...citForm, fechaCitacion: e.target.value })}
                        required
                        className="w-full px-2 py-1.5 border border-slate-200 rounded-lg text-xs bg-white"
                      />
                    </div>
                    <div>
                      <label className="block text-[10px] font-bold text-slate-500 uppercase">Hora</label>
                      <input
                        type="text"
                        value={citForm.horaCitacion}
                        onChange={(e) => setCitForm({ ...citForm, horaCitacion: e.target.value })}
                        required
                        className="w-full px-2 py-1.5 border border-slate-200 rounded-lg text-xs bg-white"
                      />
                    </div>
                  </div>

                  <button
                    type="submit"
                    disabled={loadingCit}
                    className="w-full py-2 bg-[#243A76] hover:bg-[#1b2b58] text-white font-bold rounded-xl transition text-xs flex items-center justify-center gap-1.5 cursor-pointer disabled:opacity-60"
                  >
                    {loadingCit ? "Redactando..." : "✍️ Redactar Citación Oficial"}
                  </button>
                </form>

                {docCitacion && (
                  <div className="space-y-2">
                    <textarea
                      readOnly
                      rows={7}
                      value={docCitacion}
                      className="w-full p-2.5 font-mono text-[10px] bg-slate-900 text-slate-100 rounded-xl border border-slate-800 leading-relaxed"
                    />
                    <button
                      type="button"
                      onClick={() => {
                        navigator.clipboard.writeText(docCitacion);
                        alert("Texto de citación copiado al portapapeles.");
                      }}
                      className="w-full py-1.5 bg-slate-200 hover:bg-slate-300 text-slate-800 font-bold rounded-lg text-[11px] transition cursor-pointer"
                    >
                      📋 Copiar Documento
                    </button>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      )}
    </>
  );
}