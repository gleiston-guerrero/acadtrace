import { useState, useEffect, useCallback } from "react";
import api from "../../config/axios";
import Layout from "../../components/Layout";

const PRIMARY = "#243A76";

const ACCIONES = ["", "CREAR", "EDITAR", "ELIMINAR", "LOGIN", "LOGIN_FALLIDO", "LOGOUT",
  "CAMBIO_PASSWORD", "BLOQUEO", "DESBLOQUEO", "ROL_ASIGNADO", "LLAMADA_GRPC"];
const ORIGENES = ["", "PRINCIPAL", "SECRETARIA", "DOCENTE"];

const BADGE_ORIGEN = {
  PRINCIPAL: "bg-blue-50 text-blue-600",
  SECRETARIA: "bg-cyan-50 text-cyan-600",
  DOCENTE: "bg-purple-50 text-purple-600",
};

const SECCIONES = [
  { id: "todos", label: "Todos los eventos", categoria: null,
    icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 10h16M4 14h16M4 18h16" /></svg> },
  { id: "crud", label: "CRUD sensible", categoria: "CRUD",
    icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 13h6m-3-3v6m-9 1V7a2 2 0 012-2h6l2 2h6a2 2 0 012 2v8a2 2 0 01-2 2H5a2 2 0 01-2-2z" /></svg> },
  { id: "accesos", label: "Accesos", categoria: "ACCESOS",
    icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" /></svg> },
  { id: "config", label: "Config y roles", categoria: "CONFIG",
    icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065" /><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /></svg> },
  { id: "grpc", label: "Llamadas entre microservicios", categoria: "GRPC",
    icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.828 10.172a4 4 0 010 5.656l-4 4a4 4 0 01-5.656-5.656l1.5-1.5M10.172 13.828a4 4 0 010-5.656l4-4a4 4 0 015.656 5.656l-1.5 1.5" /></svg> },
];

function fmtFecha(f) {
  if (!f) return "—";
  return new Date(f).toLocaleString("es-EC", { dateStyle: "short", timeStyle: "medium" });
}

export default function Auditoria() {
  const [filas, setFilas] = useState([]);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [filtros, setFiltros] = useState({ schemaOrigen: "", accion: "", tablaAfectada: "", resultado: "", username: "" });
  const [seccion, setSeccion] = useState("todos");
  const [cadena, setCadena] = useState(null); // { traceId, eventos }
  const [cargandoCadena, setCargandoCadena] = useState(false);

  const cargar = useCallback(() => {
    setLoading(true);
    const categoria = SECCIONES.find(s => s.id === seccion)?.categoria;
    const params = { page, size: 20 };
    if (categoria) params.categoria = categoria;
    Object.entries(filtros).forEach(([k, v]) => { if (v) params[k] = v; });
    api.get("/api/auditoria", { params })
      .then(r => { setFilas(r.data.content); setTotalPages(r.data.totalPages); })
      .catch(() => setError("Error al cargar la auditoría"))
      .finally(() => setLoading(false));
  }, [page, filtros, seccion]);

  useEffect(() => { cargar(); }, [cargar]);

  const cambiarFiltro = (campo, valor) => {
    setPage(0);
    setFiltros(f => ({ ...f, [campo]: valor }));
  };

  const cambiarSeccion = (id) => {
    setPage(0);
    setSeccion(id);
  };

  const verCadena = (traceId) => {
    setCargandoCadena(true);
    setCadena({ traceId, eventos: [] });
    api.get(`/api/auditoria/trace/${traceId}`)
      .then(r => setCadena({ traceId, eventos: r.data }))
      .catch(() => setError("Error al cargar la cadena de eventos"))
      .finally(() => setCargandoCadena(false));
  };

  return (
    <Layout breadcrumb={["Inicio", "Auditoría"]} sidebarTitle="Auditoría"
      menuItems={SECCIONES} seccion={seccion} onSeccionChange={cambiarSeccion}>
      <div className="mb-4">
        <h1 className="text-lg font-bold text-slate-700">Auditoría del sistema</h1>
        <p className="text-xs text-slate-400">
          CRUD sensible, accesos, cambios de configuración y llamadas entre microservicios — cada fila esta firmada (HMAC) para detectar alteraciones.
        </p>
      </div>

      {error && (
        <div className="mb-4 bg-red-50 border border-red-200 rounded-lg px-4 py-3 flex items-center justify-between">
          <span className="text-red-600 text-sm">{error}</span>
          <button onClick={() => setError("")} className="text-red-400 hover:text-red-600">✕</button>
        </div>
      )}

      {/* Filtros */}
      <div className="bg-white border border-slate-200 rounded-xl p-3 mb-4 flex flex-wrap gap-2 items-center">
        <select value={filtros.schemaOrigen} onChange={e => cambiarFiltro("schemaOrigen", e.target.value)}
          className="px-3 py-1.5 text-xs border border-slate-200 rounded-lg bg-slate-50 focus:outline-none">
          {ORIGENES.map(o => <option key={o} value={o}>{o || "Todos los servicios"}</option>)}
        </select>
        <select value={filtros.accion} onChange={e => cambiarFiltro("accion", e.target.value)}
          className="px-3 py-1.5 text-xs border border-slate-200 rounded-lg bg-slate-50 focus:outline-none">
          {ACCIONES.map(a => <option key={a} value={a}>{a || "Todas las acciones"}</option>)}
        </select>
        <select value={filtros.resultado} onChange={e => cambiarFiltro("resultado", e.target.value)}
          className="px-3 py-1.5 text-xs border border-slate-200 rounded-lg bg-slate-50 focus:outline-none">
          <option value="">Éxito y fallo</option>
          <option value="EXITO">Solo éxito</option>
          <option value="FALLO">Solo fallo</option>
        </select>
        <input type="text" placeholder="Entidad (ej. estudiante)" value={filtros.tablaAfectada}
          onChange={e => cambiarFiltro("tablaAfectada", e.target.value)}
          className="px-3 py-1.5 text-xs border border-slate-200 rounded-lg bg-slate-50 focus:outline-none w-40" />
        <input type="text" placeholder="Usuario" value={filtros.username}
          onChange={e => cambiarFiltro("username", e.target.value)}
          className="px-3 py-1.5 text-xs border border-slate-200 rounded-lg bg-slate-50 focus:outline-none w-32" />
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
        {loading ? (
          <div className="p-12 text-center text-slate-400 text-sm">Cargando...</div>
        ) : filas.length === 0 ? (
          <div className="p-12 text-center text-slate-400 text-sm">Sin eventos que coincidan con el filtro</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr style={{ backgroundColor: PRIMARY }} className="text-white text-xs">
                  <th className="text-left px-4 py-3 font-semibold">Fecha</th>
                  <th className="text-left px-4 py-3 font-semibold">Origen</th>
                  <th className="text-left px-4 py-3 font-semibold">Usuario</th>
                  <th className="text-left px-4 py-3 font-semibold">Acción</th>
                  <th className="text-left px-4 py-3 font-semibold">Entidad</th>
                  <th className="text-left px-4 py-3 font-semibold">Descripción</th>
                  <th className="text-center px-4 py-3 font-semibold">Resultado</th>
                  <th className="text-center px-4 py-3 font-semibold">Integridad</th>
                  <th className="text-center px-4 py-3 font-semibold">Traza</th>
                </tr>
              </thead>
              <tbody>
                {filas.map((f, i) => (
                  <tr key={f.idAuditoria} className={`border-t border-slate-100 hover:bg-slate-50 transition ${i % 2 === 0 ? "" : "bg-slate-50/50"}`}>
                    <td className="px-4 py-2.5 text-slate-500 text-xs whitespace-nowrap">{fmtFecha(f.fecha)}</td>
                    <td className="px-4 py-2.5">
                      <span className={`text-[10px] font-semibold px-2 py-0.5 rounded-md ${BADGE_ORIGEN[f.schemaOrigen] || "bg-slate-100 text-slate-500"}`}>
                        {f.schemaOrigen}
                      </span>
                    </td>
                    <td className="px-4 py-2.5 text-slate-700 font-medium">{f.username || "—"}</td>
                    <td className="px-4 py-2.5 text-slate-600 font-mono text-xs">{f.accion}</td>
                    <td className="px-4 py-2.5 text-slate-500 text-xs">{f.tablaAfectada || "—"}{f.registroId ? ` #${f.registroId}` : ""}</td>
                    <td className="px-4 py-2.5 text-slate-500 text-xs max-w-xs truncate" title={f.descripcion}>{f.descripcion || "—"}</td>
                    <td className="px-4 py-2.5 text-center">
                      <span className={`text-[10px] font-semibold px-2 py-0.5 rounded-full ${f.resultado === "EXITO" ? "bg-green-100 text-green-700" : "bg-red-100 text-red-700"}`}>
                        {f.resultado}
                      </span>
                    </td>
                    <td className="px-4 py-2.5 text-center" title={f.hmacValido ? "Fila intacta (HMAC valido)" : "Posible alteracion: el HMAC no coincide"}>
                      {f.hmacValido ? <span className="text-green-500">✓</span> : <span className="text-red-500">⚠</span>}
                    </td>
                    <td className="px-4 py-2.5 text-center">
                      <button onClick={() => verCadena(f.traceId)} title="Ver cadena de eventos correlacionados"
                        className="p-1.5 rounded-lg text-slate-400 hover:text-blue-600 hover:bg-blue-50 transition">
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.828 10.172a4 4 0 010 5.656l-4 4a4 4 0 01-5.656-5.656l1.5-1.5M10.172 13.828a4 4 0 010-5.656l4-4a4 4 0 015.656 5.656l-1.5 1.5" />
                        </svg>
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {totalPages > 1 && (
          <div className="flex items-center justify-between px-4 py-3 border-t border-slate-100">
            <button disabled={page === 0} onClick={() => setPage(p => p - 1)}
              className="px-3 py-1.5 text-xs border border-slate-200 rounded-lg text-slate-600 hover:bg-slate-50 disabled:opacity-40 transition">Anterior</button>
            <span className="text-xs text-slate-400">Página {page + 1} de {totalPages}</span>
            <button disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}
              className="px-3 py-1.5 text-xs border border-slate-200 rounded-lg text-slate-600 hover:bg-slate-50 disabled:opacity-40 transition">Siguiente</button>
          </div>
        )}
      </div>

      {/* MODAL CADENA DE TRAZA */}
      {cadena && (
        <div className="fixed inset-0 z-50 flex items-center justify-center px-4" style={{ backgroundColor: "rgba(36, 58, 118, 0.5)" }} onClick={() => setCadena(null)}>
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl max-h-[80vh] overflow-hidden flex flex-col" onClick={e => e.stopPropagation()}>
            <div style={{ backgroundColor: PRIMARY }} className="px-6 py-4 flex items-center justify-between flex-shrink-0">
              <div>
                <h2 className="text-white font-bold text-base">Cadena de eventos correlacionados</h2>
                <p className="text-white/70 text-[11px] font-mono mt-0.5">trace_id: {cadena.traceId}</p>
              </div>
              <button onClick={() => setCadena(null)} className="text-white text-opacity-70 hover:text-opacity-100">✕</button>
            </div>
            <div className="p-6 overflow-y-auto space-y-3">
              {cargandoCadena ? (
                <p className="text-sm text-slate-400 text-center py-6">Cargando...</p>
              ) : cadena.eventos.length === 0 ? (
                <p className="text-sm text-slate-400 text-center py-6">No hay eventos para esta traza.</p>
              ) : (
                cadena.eventos.map((ev, i) => (
                  <div key={ev.idAuditoria} className="flex gap-3">
                    <div className="flex flex-col items-center flex-shrink-0">
                      <span className={`w-2.5 h-2.5 rounded-full ${ev.resultado === "EXITO" ? "bg-green-500" : "bg-red-500"}`} />
                      {i < cadena.eventos.length - 1 && <span className="w-px flex-1 bg-slate-200" />}
                    </div>
                    <div className="pb-3 min-w-0">
                      <p className="text-xs text-slate-400">{fmtFecha(ev.fecha)}</p>
                      <p className="text-sm font-medium text-slate-700">
                        <span className={`text-[10px] font-semibold px-1.5 py-0.5 rounded mr-1.5 ${BADGE_ORIGEN[ev.schemaOrigen] || "bg-slate-100 text-slate-500"}`}>
                          {ev.schemaOrigen}
                        </span>
                        {ev.accion} {ev.tablaAfectada ? `— ${ev.tablaAfectada}` : ""} {ev.username ? `(${ev.username})` : ""}
                      </p>
                      {ev.descripcion && <p className="text-xs text-slate-500 mt-0.5">{ev.descripcion}</p>}
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      )}
    </Layout>
  );
}
