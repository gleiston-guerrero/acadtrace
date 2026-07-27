import { useState, useEffect } from "react";
import api from "../../config/axios";

const PRIMARY = "#243A76";
const modalBg = { backgroundColor: "rgba(36, 58, 118, 0.5)" };

export default function ImportarEstudiantesModal({ onCancel, onSuccess }) {
  const [grados, setGrados] = useState([]);
  const [anoActual, setAnoActual] = useState(null);
  const [idGrado, setIdGrado] = useState("");
  const [idParalelo, setIdParalelo] = useState("");
  const [archivo, setArchivo] = useState(null);

  const [preview, setPreview] = useState(null);
  const [cargando, setCargando] = useState(false);
  const [confirmando, setConfirmando] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    api.get(`/api/grados`).then(r => setGrados(r.data)).catch(() => {});
    // Solo /api/anos-lectivos/actual es accesible para ROLE_SECRETARIA; la lista
    // completa (/api/anos-lectivos) está restringida a ROLE_DIRECTOR (ver SecurityConfig).
    api.get(`/api/anos-lectivos/actual`).then(r => setAnoActual(r.data)).catch(() => {});
  }, []);

  const gradoSel = grados.find(g => String(g.idGrado) === idGrado);
  const paralelos = gradoSel?.paralelos || [];

  const handleGrado = (valor) => {
    setIdGrado(valor);
    setIdParalelo("");
  };

  const handlePrevisualizar = async (e) => {
    e.preventDefault();
    if (!archivo) { setError("Selecciona un archivo .csv, .xlsx, .xls o .pdf"); return; }
    setError(""); setCargando(true);
    try {
      const formData = new FormData();
      formData.append("archivo", archivo);
      const r = await api.post(`/api/importacion-excel/parsear`, formData, {
        headers: { "Content-Type": "multipart/form-data" },
      });
      setPreview(r.data);
    } catch (err) {
      setError(err.response?.data?.message || "No se pudo procesar el archivo");
      setPreview(null);
    } finally {
      setCargando(false);
    }
  };

  const handleConfirmar = async () => {
    setConfirmando(true); setError("");
    try {
      const r = await api.post(`/api/importacion-excel/confirmar`, {
        idGrado: Number(idGrado),
        idParalelo: Number(idParalelo),
        idAnoLectivo: anoActual.idAnoLectivo,
        estudiantes: preview.estudiantes,
      });
      const resumen = r.data;
      onSuccess(
        `${resumen.creados} estudiante${resumen.creados !== 1 ? "s" : ""} nuevo${resumen.creados !== 1 ? "s" : ""}, ` +
        `${resumen.existentes} ya existían, ${resumen.matriculados} matrícula${resumen.matriculados !== 1 ? "s" : ""} creada${resumen.matriculados !== 1 ? "s" : ""}` +
        `${resumen.omitidos > 0 ? `, ${resumen.omitidos} fila${resumen.omitidos !== 1 ? "s" : ""} omitida${resumen.omitidos !== 1 ? "s" : ""} por error` : ""} (total ${resumen.total}).`
      );
    } catch (err) {
      setError(err.response?.data?.message || "Error al confirmar la importación");
    } finally {
      setConfirmando(false);
    }
  };

  const volverAlFormulario = () => {
    setPreview(null);
    setError("");
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center px-4" style={modalBg}>
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl overflow-hidden max-h-[92vh] flex flex-col">
        <div style={{ backgroundColor: PRIMARY }} className="px-6 py-4 flex items-center justify-between flex-shrink-0">
          <div>
            <h2 className="text-white font-bold text-base">Importar Estudiantes</h2>
            <p className="text-white text-opacity-70 text-xs mt-0.5">Desde Excel, CSV o PDF (listado CAS)</p>
          </div>
          <button onClick={onCancel} className="text-white text-opacity-70 hover:text-opacity-100">✕</button>
        </div>

        <div className="p-6 overflow-y-auto flex-1">
          {error && (
            <div className="mb-4 bg-red-50 border border-red-200 rounded-xl px-4 py-3 flex justify-between">
              <span className="text-red-600 text-sm">{error}</span>
              <button onClick={() => setError("")} className="text-red-400 ml-4">✕</button>
            </div>
          )}

          {!preview ? (
            <form onSubmit={handlePrevisualizar} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase tracking-wide">Archivo</label>
                <div className="border-2 border-dashed border-slate-200 rounded-xl p-5 text-center hover:border-blue-300 transition">
                  <input
                    type="file"
                    accept=".csv,.xlsx,.xls,.pdf"
                    onChange={e => setArchivo(e.target.files?.[0] || null)}
                    className="w-full text-sm text-slate-600 file:mr-3 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-medium file:text-white file:cursor-pointer file:bg-[#243A76]"
                  />
                  <p className="text-xs text-slate-400 mt-2">Columnas esperadas: CEDULA, NOMBRES, APELLIDOS, CORREO (opcional)</p>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase tracking-wide">Año lectivo</label>
                  <div className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-slate-100 text-slate-600">
                    {anoActual?.nombre || "Sin año lectivo activo"}
                  </div>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase tracking-wide">Grado</label>
                  <select value={idGrado} onChange={e => handleGrado(e.target.value)} required
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none bg-slate-50">
                    <option value="">Seleccionar</option>
                    {grados.map(g => <option key={g.idGrado} value={g.idGrado}>{g.nombre}</option>)}
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase tracking-wide">Paralelo</label>
                <select value={idParalelo} onChange={e => setIdParalelo(e.target.value)} disabled={!gradoSel} required
                  className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none bg-slate-50 disabled:opacity-50">
                  <option value="">Seleccionar</option>
                  {paralelos.map(p => <option key={p.idParalelo} value={p.idParalelo}>{p.letra}</option>)}
                </select>
              </div>

              <div className="flex gap-3 pt-2">
                <button type="button" onClick={onCancel} className="flex-1 py-2 border border-slate-200 rounded-lg text-sm text-slate-600 hover:bg-slate-50 transition">
                  Cancelar
                </button>
                <button type="submit" disabled={cargando} style={{ backgroundColor: PRIMARY }}
                  className="flex-1 py-2 text-white rounded-lg text-sm font-medium hover:opacity-90 transition disabled:opacity-60">
                  {cargando ? "Procesando..." : "Previsualizar"}
                </button>
              </div>
            </form>
          ) : (
            <div>
              <div className="flex items-center justify-between mb-3">
                <p className="text-sm text-slate-500">
                  {preview.totalFilas} fila{preview.totalFilas !== 1 ? "s" : ""} — <span className="text-green-600 font-medium">{preview.filasValidas} válida{preview.filasValidas !== 1 ? "s" : ""}</span>
                  {preview.filasConError > 0 && <> — <span className="text-red-600 font-medium">{preview.filasConError} con error</span></>}
                </p>
                <button onClick={volverAlFormulario} className="text-xs text-slate-400 hover:text-slate-600 underline">
                  Volver
                </button>
              </div>

              <div className="bg-white rounded-xl border border-slate-200 overflow-hidden mb-4">
                <div className="overflow-x-auto" style={{ maxHeight: "20rem", overflowY: "auto" }}>
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="border-b border-slate-100 sticky top-0" style={{ backgroundColor: "#f8f9fc" }}>
                        <th className="text-left px-3 py-2.5 text-xs font-semibold text-slate-500 uppercase tracking-wide">Cédula</th>
                        <th className="text-left px-3 py-2.5 text-xs font-semibold text-slate-500 uppercase tracking-wide">Apellidos</th>
                        <th className="text-left px-3 py-2.5 text-xs font-semibold text-slate-500 uppercase tracking-wide">Nombres</th>
                        <th className="text-center px-3 py-2.5 text-xs font-semibold text-slate-500 uppercase tracking-wide">Estado</th>
                      </tr>
                    </thead>
                    <tbody>
                      {preview.estudiantes.map((est, i) => (
                        <tr key={i} className="border-b border-slate-50">
                          <td className="px-3 py-2 text-slate-600">{est.cedula || "—"}</td>
                          <td className="px-3 py-2 text-slate-600">{est.apellidos || "—"}</td>
                          <td className="px-3 py-2 text-slate-600">{est.nombres || "—"}</td>
                          <td className="px-3 py-2 text-center">
                            {est.error ? (
                              <span className="text-[11px] font-semibold px-2 py-0.5 rounded-md bg-red-100 text-red-700" title={est.error}>Error</span>
                            ) : est.yaExiste ? (
                              <span className="text-[11px] font-semibold px-2 py-0.5 rounded-md bg-amber-100 text-amber-700">Ya existe</span>
                            ) : (
                              <span className="text-[11px] font-semibold px-2 py-0.5 rounded-md bg-green-100 text-green-700">Nuevo</span>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>

              <div className="flex gap-3">
                <button onClick={onCancel} className="flex-1 py-2 border border-slate-200 rounded-lg text-sm text-slate-600 hover:bg-slate-50 transition">
                  Cancelar
                </button>
                <button
                  onClick={handleConfirmar}
                  disabled={confirmando || !idGrado || !idParalelo || !anoActual || preview.filasValidas === 0}
                  style={{ backgroundColor: PRIMARY }}
                  className="flex-1 py-2 text-white rounded-lg text-sm font-medium hover:opacity-90 transition disabled:opacity-60"
                >
                  {confirmando ? "Confirmando..." : `Confirmar (${preview.filasValidas})`}
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
