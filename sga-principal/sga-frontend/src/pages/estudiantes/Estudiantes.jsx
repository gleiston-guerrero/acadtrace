import { useState, useEffect } from "react";
import api from "../../config/axios";
import Layout from "../../components/Layout";
import ImportarEstudiantesModal from "./ImportarEstudiantesModal";

const PRIMARY = "#243A76";
const modalBg = { backgroundColor: "rgba(36, 58, 118, 0.5)" };
const PAGE_SIZE = 10;

const ORIGEN_CONFIG = {
  MANUAL: { label: "Manual", color: "bg-blue-100 text-blue-700", cardColor: "#3b82f6", icon: "M12 4v16m8-8H4" },
  EXCEL: { label: "Excel/CSV", color: "bg-lime-100 text-lime-700", cardColor: "#84cc16", icon: "M9 17v-2a4 4 0 014-4h4M7 7h10M7 11h4m-4 4h4" },
  CAS: { label: "Listado CAS", color: "bg-amber-100 text-amber-700", cardColor: "#f59e0b", icon: "M4 4h16v4H4zM4 12h16v4H4zM4 20h16" },
  PRUEBA: { label: "Prueba", color: "bg-purple-100 text-purple-700", cardColor: "#a855f7", icon: "M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" },
};
const ORIGENES_KPI = ["MANUAL", "EXCEL", "CAS", "PRUEBA"];

const menuItems = [
  { id: "lista", label: "Lista de estudiantes", icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 10h16M4 14h16M4 18h16" /></svg> },
  { id: "nuevo", label: "Nuevo estudiante", icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg> },
  { id: "importar", label: "Importar estudiantes", icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M12 12v9m0-9l-3 3m3-3l3 3" /></svg> },
];

export default function Estudiantes() {
  const [estudiantes, setEstudiantes] = useState([]);
  const [cargandoLista, setCargandoLista] = useState(true);
  const [busqueda, setBusqueda] = useState("");
  const [pagina, setPagina] = useState(1);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [seccion, setSeccion] = useState("lista");
  const [showModal, setShowModal] = useState(false);
  const [editId, setEditId] = useState(null);
  const [showImportModal, setShowImportModal] = useState(false);
  const [fotoAmpliada, setFotoAmpliada] = useState(null);
  const [origenFiltro, setOrigenFiltro] = useState(null);

  const handleSeccion = (id) => {
    if (id === "nuevo") { setShowModal(true); return; }
    if (id === "importar") { setShowImportModal(true); return; }
    setSeccion(id);
  };

  const cargar = (q) => {
    setCargandoLista(true);
    api.get(`/api/estudiantes`, { params: q ? { q } : {} })
      .then(r => setEstudiantes(Array.isArray(r.data) ? r.data : []))
      .catch(() => setError("No se pudo cargar la lista de estudiantes"))
      .finally(() => setCargandoLista(false));
  };

  useEffect(() => {
    setPagina(1);
    const t = setTimeout(() => cargar(busqueda), 300);
    return () => clearTimeout(t);
  }, [busqueda]);

  useEffect(() => {
    if (success) { const t = setTimeout(() => setSuccess(""), 4000); return () => clearTimeout(t); }
  }, [success]);

  const conteosOrigen = estudiantes.reduce((acc, e) => {
    const k = e.origenListado || "SIN_ORIGEN";
    acc[k] = (acc[k] || 0) + 1;
    return acc;
  }, {});

  const filtrados = origenFiltro
    ? estudiantes.filter(e => (e.origenListado || "SIN_ORIGEN") === origenFiltro)
    : estudiantes;

  const totalPaginas = Math.max(1, Math.ceil(filtrados.length / PAGE_SIZE));
  const paginaReal = Math.min(pagina, totalPaginas);
  const paginados = filtrados.slice((paginaReal - 1) * PAGE_SIZE, paginaReal * PAGE_SIZE);

  const toggleOrigen = (origen) => {
    setPagina(1);
    setOrigenFiltro(prev => prev === origen ? null : origen);
  };

  return (
    <Layout
      breadcrumb={["Inicio", "Estudiantes"]}
      sidebarTitle="Estudiantes"
      menuItems={menuItems}
      seccion={seccion}
      onSeccionChange={handleSeccion}
    >
      <div className="flex items-center justify-between mb-5">
        <div>
          <h1 className="text-lg font-bold text-slate-700">Estudiantes</h1>
          <p className="text-sm text-slate-400 mt-0.5">
            {estudiantes.length} estudiante{estudiantes.length !== 1 ? "s" : ""} registrado{estudiantes.length !== 1 ? "s" : ""} en el núcleo
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={() => setShowModal(true)}
            className="flex items-center gap-2 border border-slate-200 text-slate-600 px-4 py-2 rounded-lg text-sm font-medium hover:bg-slate-50 transition"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
            </svg>
            Nuevo estudiante
          </button>
          <button
            onClick={() => setShowImportModal(true)}
            style={{ backgroundColor: PRIMARY }}
            className="flex items-center gap-2 text-white px-4 py-2 rounded-lg text-sm font-medium hover:opacity-90 transition shadow-sm"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M12 12v9m0-9l-3 3m3-3l3 3" />
            </svg>
            Importar estudiantes
          </button>
        </div>
      </div>

      {error && (
        <div className="mb-4 bg-red-50 border border-red-200 rounded-xl px-4 py-3 flex justify-between">
          <span className="text-red-600 text-sm">{error}</span>
          <button onClick={() => setError("")} className="text-red-400 ml-4">✕</button>
        </div>
      )}
      {success && (
        <div className="mb-4 bg-green-50 border border-green-200 rounded-xl px-4 py-3 flex justify-between">
          <span className="text-green-600 text-sm">{success}</span>
          <button onClick={() => setSuccess("")} className="text-green-400 ml-4">✕</button>
        </div>
      )}

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3 mb-5">
        <button
          type="button"
          onClick={() => { setOrigenFiltro(null); setPagina(1); }}
          className={`text-left bg-white rounded-2xl border p-4 shadow-sm transition hover:shadow-md ${origenFiltro === null ? "ring-2 ring-offset-1" : "border-slate-200"}`}
          style={origenFiltro === null ? { "--tw-ring-color": PRIMARY, borderColor: PRIMARY } : {}}
        >
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wide">Todos</span>
            <div className="w-8 h-8 rounded-lg flex items-center justify-center text-white" style={{ backgroundColor: PRIMARY }}>
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
            </div>
          </div>
          <p className="text-2xl font-bold text-slate-700">{estudiantes.length}</p>
          <p className="text-xs text-slate-400 mt-0.5">Registrados en total</p>
        </button>

        {ORIGENES_KPI.map(origen => {
          const cfg = ORIGEN_CONFIG[origen];
          const count = conteosOrigen[origen] || 0;
          const active = origenFiltro === origen;
          const pct = estudiantes.length ? Math.round((count / estudiantes.length) * 100) : 0;
          return (
            <button
              key={origen}
              type="button"
              onClick={() => toggleOrigen(origen)}
              className={`text-left bg-white rounded-2xl border p-4 shadow-sm transition hover:shadow-md ${active ? "ring-2 ring-offset-1" : "border-slate-200"}`}
              style={active ? { "--tw-ring-color": cfg.cardColor, borderColor: cfg.cardColor } : {}}
            >
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs font-semibold text-slate-500 uppercase tracking-wide">{cfg.label}</span>
                <div className="w-8 h-8 rounded-lg flex items-center justify-center text-white" style={{ backgroundColor: cfg.cardColor }}>
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d={cfg.icon} />
                  </svg>
                </div>
              </div>
              <div className="flex items-baseline gap-2">
                <p className="text-2xl font-bold text-slate-700">{count}</p>
                <p className="text-xs text-slate-400">{pct}%</p>
              </div>
              <div className="mt-2 h-1.5 bg-slate-100 rounded-full overflow-hidden">
                <div className="h-full rounded-full transition-all" style={{ width: `${pct}%`, backgroundColor: cfg.cardColor }} />
              </div>
            </button>
          );
        })}
      </div>

      {origenFiltro && (
        <div className="mb-3 flex items-center gap-2 text-xs">
          <span className="text-slate-500">Filtrado por origen:</span>
          <span className={`font-semibold px-2 py-0.5 rounded-md ${ORIGEN_CONFIG[origenFiltro]?.color || "bg-slate-100 text-slate-600"}`}>
            {ORIGEN_CONFIG[origenFiltro]?.label || origenFiltro}
          </span>
          <button onClick={() => setOrigenFiltro(null)} className="text-slate-400 hover:text-slate-600 underline">Quitar filtro</button>
        </div>
      )}

      <div className="mb-4">
        <div className="relative max-w-md">
          <input
            type="text"
            placeholder="Buscar por nombre o apellido..."
            value={busqueda}
            onChange={e => setBusqueda(e.target.value)}
            className="w-full pl-9 pr-4 py-2.5 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 bg-white shadow-sm"
            style={{ "--tw-ring-color": "#c7d2ea" }}
          />
          <svg className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
        </div>
      </div>

      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        {cargandoLista ? (
          <div className="p-16 text-center text-slate-400 text-sm">
            <svg className="w-6 h-6 animate-spin mx-auto mb-2 text-slate-300" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
            </svg>
            Cargando estudiantes...
          </div>
        ) : filtrados.length === 0 ? (
          <div className="p-16 text-center text-slate-400 text-sm">
            <svg className="w-10 h-10 mx-auto mb-3 text-slate-200" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            {origenFiltro
              ? `No hay estudiantes con origen "${ORIGEN_CONFIG[origenFiltro]?.label || origenFiltro}".`
              : busqueda
              ? "No se encontraron estudiantes con esa búsqueda."
              : 'Aún no hay estudiantes registrados. Usa "Nuevo estudiante" o "Importar estudiantes".'}
          </div>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-slate-100" style={{ backgroundColor: "#f8f9fc" }}>
                    <th className="text-left px-5 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Estudiante</th>
                    <th className="text-left px-5 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Cédula</th>
                    <th className="text-left px-5 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Código</th>
                    <th className="text-left px-5 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Representante</th>
                    <th className="text-center px-5 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Origen</th>
                    <th className="text-center px-5 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Estado</th>
                    <th className="text-center px-5 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide"></th>
                  </tr>
                </thead>
                <tbody>
                  {paginados.map(e => {
                    const origen = ORIGEN_CONFIG[e.origenListado] || { label: e.origenListado || "—", color: "bg-slate-100 text-slate-500" };
                    return (
                      <tr key={e.idEstudiante} className="border-b border-slate-50 hover:bg-slate-50/80 transition">
                        <td className="px-5 py-3.5">
                          <div className="flex items-center gap-3">
                            {e.fotoUrl ? (
                              <button
                                type="button"
                                onClick={() => setFotoAmpliada({ url: e.fotoUrl, nombre: `${e.nombres} ${e.apellidos}` })}
                                className="relative w-9 h-9 rounded-full flex-shrink-0 group"
                                title="Ver foto"
                              >
                                <img src={`${api.defaults.baseURL}${e.fotoUrl}`} alt="" className="w-9 h-9 rounded-full object-cover shadow-sm border border-slate-200" />
                                <span className="absolute inset-0 rounded-full bg-black/0 group-hover:bg-black/30 transition flex items-center justify-center">
                                  <svg className="w-3.5 h-3.5 text-white opacity-0 group-hover:opacity-100 transition" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                                  </svg>
                                </span>
                              </button>
                            ) : (
                              <div className="w-9 h-9 rounded-full flex items-center justify-center text-white text-xs font-bold flex-shrink-0 shadow-sm" style={{ backgroundColor: PRIMARY }}>
                                {e.nombres?.[0]}{e.apellidos?.[0]}
                              </div>
                            )}
                            <div className="min-w-0">
                              <p className="font-semibold text-slate-700 leading-tight">{e.apellidos} {e.nombres}</p>
                              {e.correo && <p className="text-xs text-slate-400 truncate">{e.correo}</p>}
                            </div>
                          </div>
                        </td>
                        <td className="px-5 py-3.5 text-slate-600">{e.cedula || "—"}</td>
                        <td className="px-5 py-3.5 text-slate-600 font-mono text-xs">{e.codigoEstudiante || "—"}</td>
                        <td className="px-5 py-3.5 text-slate-600">{e.representante || "—"}</td>
                        <td className="px-5 py-3.5 text-center">
                          <span className={`text-[11px] font-semibold px-2 py-0.5 rounded-md ${origen.color}`}>
                            {origen.label}
                          </span>
                        </td>
                        <td className="px-5 py-3.5 text-center">
                          <span className={`text-[11px] font-semibold px-2 py-0.5 rounded-md ${e.estado === "ACTIVA" || e.estado === "ACTIVO" ? "bg-green-100 text-green-700" : "bg-slate-200 text-slate-500"}`}>
                            {e.estado}
                          </span>
                        </td>
                        <td className="px-5 py-3.5 text-center">
                          <button
                            onClick={() => setEditId(e.idEstudiante)}
                            className="p-1.5 rounded-lg text-slate-400 hover:text-blue-600 hover:bg-blue-50 transition"
                            title="Editar"
                          >
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                            </svg>
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            {totalPaginas > 1 && (
              <div className="flex items-center justify-between px-5 py-3 border-t border-slate-100">
                <p className="text-xs text-slate-400">
                  Mostrando {(paginaReal - 1) * PAGE_SIZE + 1}–{Math.min(paginaReal * PAGE_SIZE, filtrados.length)} de {filtrados.length}
                </p>
                <div className="flex items-center gap-1">
                  <button onClick={() => setPagina(p => Math.max(1, p - 1))} disabled={paginaReal === 1}
                    className="px-3 py-1.5 text-xs border border-slate-200 rounded-lg hover:bg-slate-50 disabled:opacity-40 transition">
                    Anterior
                  </button>
                  {Array.from({ length: totalPaginas }, (_, i) => i + 1)
                    .filter(p => p === 1 || p === totalPaginas || Math.abs(p - paginaReal) <= 1)
                    .map((p, idx, arr) => (
                      <span key={p}>
                        {idx > 0 && arr[idx - 1] < p - 1 && <span className="px-1 text-slate-300">...</span>}
                        <button onClick={() => setPagina(p)}
                          className={`px-3 py-1.5 text-xs rounded-lg transition ${p === paginaReal ? "text-white" : "border border-slate-200 hover:bg-slate-50"}`}
                          style={p === paginaReal ? { backgroundColor: PRIMARY } : {}}>
                          {p}
                        </button>
                      </span>
                    ))}
                  <button onClick={() => setPagina(p => Math.min(totalPaginas, p + 1))} disabled={paginaReal === totalPaginas}
                    className="px-3 py-1.5 text-xs border border-slate-200 rounded-lg hover:bg-slate-50 disabled:opacity-40 transition">
                    Siguiente
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </div>

      <p className="text-xs text-slate-400 mt-4">
        Esta lista viene del registro de respaldo en el núcleo (sga_principal), alimentado por "Nuevo estudiante" e "Importar estudiantes".
      </p>

      {showModal && (
        <NuevoEstudianteModal
          onCancel={() => setShowModal(false)}
          onSuccess={(msg) => { setShowModal(false); setSuccess(msg); cargar(busqueda); }}
        />
      )}

      {editId && (
        <NuevoEstudianteModal
          estudianteId={editId}
          onCancel={() => setEditId(null)}
          onSuccess={(msg) => { setEditId(null); setSuccess(msg); cargar(busqueda); }}
        />
      )}

      {showImportModal && (
        <ImportarEstudiantesModal
          onCancel={() => setShowImportModal(false)}
          onSuccess={(msg) => { setShowImportModal(false); setSuccess(msg); cargar(busqueda); }}
        />
      )}

      {fotoAmpliada && (
        <FotoAmpliadaModal
          url={fotoAmpliada.url}
          nombre={fotoAmpliada.nombre}
          onClose={() => setFotoAmpliada(null)}
        />
      )}
    </Layout>
  );
}

function FotoAmpliadaModal({ url, nombre, onClose }) {
  return (
    <div className="fixed inset-0 z-[60] flex items-center justify-center px-4" style={modalBg} onClick={onClose}>
      <div className="bg-white rounded-2xl shadow-2xl overflow-hidden max-w-sm w-full" onClick={e => e.stopPropagation()}>
        <div className="flex items-center justify-between px-4 py-3 border-b border-slate-100">
          <p className="text-sm font-semibold text-slate-700 truncate">{nombre}</p>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-600 flex-shrink-0 ml-3">✕</button>
        </div>
        <div className="p-4 flex items-center justify-center bg-slate-50">
          <img src={`${api.defaults.baseURL}${url}`} alt={nombre} className="max-w-full max-h-[70vh] rounded-xl object-contain shadow-sm" />
        </div>
      </div>
    </div>
  );
}

const TABS = [
  {
    id: "estudiante", label: "Estudiante", icon: (
      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" /></svg>
    )
  },
  {
    id: "familiar", label: "Datos familiares", icon: (
      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" /></svg>
    )
  },
  {
    id: "representante", label: "Representante", icon: (
      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>
    )
  },
];

function SectionTitle({ icon, children }) {
  return (
    <div className="flex items-center gap-2 mb-3 first:mt-0 mt-5">
      <span className="text-slate-400">{icon}</span>
      <h3 className="text-xs font-bold uppercase tracking-wider text-slate-500">{children}</h3>
      <div className="flex-1 h-px bg-slate-100" />
    </div>
  );
}

function FotoUpload({ fotoUrl, onUploaded, nombres, apellidos }) {
  const [preview, setPreview] = useState(null);
  const [subiendo, setSubiendo] = useState(false);
  const [error, setError] = useState("");

  const handleFile = async (e) => {
    const archivo = e.target.files?.[0];
    if (!archivo) return;
    setError("");
    setPreview(URL.createObjectURL(archivo));
    setSubiendo(true);
    try {
      const formData = new FormData();
      formData.append("archivo", archivo);
      const r = await api.post(`/api/uploads/foto`, formData, {
        headers: { "Content-Type": "multipart/form-data" },
      });
      onUploaded(r.data.url);
    } catch (err) {
      setError(err.response?.data?.message || "No se pudo subir la foto");
      setPreview(null);
    } finally {
      setSubiendo(false);
    }
  };

  const src = preview || (fotoUrl ? `${api.defaults.baseURL}${fotoUrl}` : null);

  return (
    <div className="flex items-center gap-4">
      <div className="relative w-20 h-20 flex-shrink-0">
        <div className="w-20 h-20 rounded-full overflow-hidden bg-slate-100 border-2 border-slate-200 flex items-center justify-center">
          {src ? (
            <img src={src} alt="" className="w-full h-full object-cover" />
          ) : (nombres || apellidos) ? (
            <div className="w-full h-full flex items-center justify-center text-white text-xl font-bold" style={{ backgroundColor: PRIMARY }}>
              {nombres?.trim()?.[0]?.toUpperCase()}{apellidos?.trim()?.[0]?.toUpperCase()}
            </div>
          ) : (
            <svg className="w-8 h-8 text-slate-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
            </svg>
          )}
          {subiendo && (
            <div className="absolute inset-0 bg-black/40 flex items-center justify-center">
              <svg className="w-5 h-5 animate-spin text-white" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
              </svg>
            </div>
          )}
        </div>
        <label
          style={{ backgroundColor: PRIMARY }}
          className="absolute bottom-0 right-0 w-7 h-7 rounded-full flex items-center justify-center text-white cursor-pointer shadow-md hover:opacity-90 transition"
          title="Subir foto"
        >
          <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" />
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 13a3 3 0 11-6 0 3 3 0 016 0z" />
          </svg>
          <input type="file" accept="image/jpeg,image/png,image/webp" className="hidden" onChange={handleFile} />
        </label>
      </div>
      <div>
        <p className="text-sm font-medium text-slate-600">Foto del estudiante</p>
        <p className="text-xs text-slate-400">JPG, PNG o WEBP — máx. 3 MB</p>
        {error && <p className="text-xs text-red-500 mt-0.5">{error}</p>}
      </div>
    </div>
  );
}

const FORM_VACIO = {
  cedula: "", nombres: "", apellidos: "", correo: "",
  telefono: "", telefonoAlt: "", fechaNacimiento: "", genero: "",
  direccion: "", nacionalidad: "Ecuatoriana", etnia: "", lugarNacimiento: "",
  viveCon: "", numerosHermanos: "", beneficioSocial: false,
  discapacidad: false, tipoDiscapacidad: "", porcentajeDisc: "", carnetConadis: "",
  fotoUrl: "",
};
const REPRESENTANTE_VACIO = {
  cedula: "", nombres: "", apellidos: "", parentesco: "",
  telefonoPrincipal: "", telefonoAlt: "", correo: "", direccion: "",
};

function NuevoEstudianteModal({ estudianteId, onCancel, onSuccess }) {
  const esEdicion = !!estudianteId;
  const [seccion, setSeccion] = useState("estudiante");
  const [form, setForm] = useState(FORM_VACIO);
  const [representante, setRepresentante] = useState(REPRESENTANTE_VACIO);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const [cargando, setCargando] = useState(esEdicion);

  useEffect(() => {
    if (!esEdicion) return;
    api.get(`/api/estudiantes/${estudianteId}`)
      .then(r => {
        const d = r.data;
        setForm({
          cedula: d.cedula || "", nombres: d.nombres || "", apellidos: d.apellidos || "",
          correo: d.correo || "", telefono: d.telefono || "", telefonoAlt: d.telefonoAlt || "",
          fechaNacimiento: d.fechaNacimiento || "", genero: d.genero || "", direccion: d.direccion || "",
          nacionalidad: d.nacionalidad || "Ecuatoriana", etnia: d.etnia || "", lugarNacimiento: d.lugarNacimiento || "",
          viveCon: d.viveCon || "", numerosHermanos: d.numerosHermanos ?? "", beneficioSocial: !!d.beneficioSocial,
          discapacidad: !!d.discapacidad, tipoDiscapacidad: d.tipoDiscapacidad || "",
          porcentajeDisc: d.porcentajeDisc ?? "", carnetConadis: d.carnetConadis || "",
          fotoUrl: d.fotoUrl || "",
        });
        if (d.representante) {
          setRepresentante({
            cedula: d.representante.cedula || "", nombres: d.representante.nombres || "",
            apellidos: d.representante.apellidos || "", parentesco: d.representante.parentesco || "",
            telefonoPrincipal: d.representante.telefonoPrincipal || "", telefonoAlt: d.representante.telefonoAlt || "",
            correo: d.representante.correo || "", direccion: d.representante.direccion || "",
          });
        }
      })
      .catch(() => setError("No se pudo cargar el estudiante"))
      .finally(() => setCargando(false));
  }, [estudianteId]);

  const set = (campo) => (e) => {
    const val = e.target.type === "checkbox" ? e.target.checked : e.target.value;
    setForm({ ...form, [campo]: val });
  };
  const setRep = (campo) => (e) => setRepresentante({ ...representante, [campo]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true); setError("");
    const payload = {
      ...form,
      fechaNacimiento: form.fechaNacimiento || null,
      numerosHermanos: form.numerosHermanos ? Number(form.numerosHermanos) : null,
      porcentajeDisc: form.porcentajeDisc ? Number(form.porcentajeDisc) : null,
      representante,
    };
    try {
      if (esEdicion) {
        await api.put(`/api/estudiantes/${estudianteId}`, payload);
        onSuccess(`Estudiante ${form.nombres} ${form.apellidos} actualizado correctamente`);
      } else {
        await api.post(`/api/estudiantes`, payload);
        onSuccess(`Estudiante ${form.nombres} ${form.apellidos} registrado correctamente`);
      }
    } catch (err) {
      setError(err.response?.data?.message || "Error al guardar el estudiante");
    } finally {
      setSaving(false);
    }
  };

  const inputCls = "w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 bg-slate-50 transition";
  const labelCls = "block text-xs font-semibold text-slate-500 mb-1 uppercase tracking-wide";
  const ringStyle = { "--tw-ring-color": "#c7d2ea" };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center px-4" style={modalBg}>
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl overflow-hidden max-h-[92vh] flex flex-col">
        <div style={{ backgroundColor: PRIMARY }} className="px-6 py-4 flex items-center justify-between flex-shrink-0">
          <div>
            <h2 className="text-white font-bold text-base">{esEdicion ? "Editar Estudiante" : "Nuevo Estudiante"}</h2>
            <p className="text-white text-opacity-70 text-xs mt-0.5">Registro completo en el núcleo</p>
          </div>
          <button onClick={onCancel} className="text-white text-opacity-70 hover:text-opacity-100">✕</button>
        </div>

        {cargando && (
          <div className="p-16 text-center text-slate-400 text-sm">
            <svg className="w-6 h-6 animate-spin mx-auto mb-2 text-slate-300" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
            </svg>
            Cargando estudiante...
          </div>
        )}

        {!cargando && (
        <>
        <div className="flex border-b border-slate-100 px-6 flex-shrink-0">
          {TABS.map(t => (
            <button
              key={t.id}
              type="button"
              onClick={() => setSeccion(t.id)}
              className={`flex items-center gap-1.5 px-4 py-3 text-sm font-medium border-b-2 -mb-px transition ${
                seccion === t.id ? "border-current" : "border-transparent text-slate-400 hover:text-slate-600"
              }`}
              style={seccion === t.id ? { color: PRIMARY, borderColor: PRIMARY } : {}}
            >
              {t.icon}
              {t.label}
            </button>
          ))}
        </div>

        <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto flex flex-col">
          <div className="p-6 flex-1">
            {error && <div className="mb-4 bg-red-50 border border-red-200 rounded-lg px-3 py-2 text-red-600 text-xs">{error}</div>}

            {seccion === "estudiante" && (
              <div>
                <FotoUpload fotoUrl={form.fotoUrl} nombres={form.nombres} apellidos={form.apellidos} onUploaded={(url) => setForm({ ...form, fotoUrl: url })} />

                <SectionTitle icon={
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>
                }>
                  Identificación
                </SectionTitle>
                <div className="grid grid-cols-2 gap-3">
                  <div className="col-span-2">
                    <label className={labelCls}>Cédula</label>
                    <input type="text" value={form.cedula} onChange={set("cedula")} required maxLength={10} placeholder="10 dígitos" className={inputCls} style={ringStyle} />
                  </div>
                  <div>
                    <label className={labelCls}>Nombres</label>
                    <input type="text" value={form.nombres} onChange={set("nombres")} required className={inputCls} style={ringStyle} />
                  </div>
                  <div>
                    <label className={labelCls}>Apellidos</label>
                    <input type="text" value={form.apellidos} onChange={set("apellidos")} required className={inputCls} style={ringStyle} />
                  </div>
                  <div>
                    <label className={labelCls}>Género</label>
                    <select value={form.genero} onChange={set("genero")} className={inputCls} style={ringStyle}>
                      <option value="">Seleccionar</option>
                      <option value="M">Masculino</option>
                      <option value="F">Femenino</option>
                    </select>
                  </div>
                  <div>
                    <label className={labelCls}>Fecha de nacimiento</label>
                    <input type="date" value={form.fechaNacimiento} onChange={set("fechaNacimiento")} className={inputCls} style={ringStyle} />
                  </div>
                </div>

                <SectionTitle icon={
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" /></svg>
                }>
                  Contacto
                </SectionTitle>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className={labelCls}>Correo (opcional)</label>
                    <input type="email" value={form.correo} onChange={set("correo")} className={inputCls} style={ringStyle} />
                  </div>
                  <div>
                    <label className={labelCls}>Teléfono</label>
                    <input type="text" value={form.telefono} onChange={set("telefono")} className={inputCls} style={ringStyle} />
                  </div>
                  <div>
                    <label className={labelCls}>Teléfono alterno</label>
                    <input type="text" value={form.telefonoAlt} onChange={set("telefonoAlt")} className={inputCls} style={ringStyle} />
                  </div>
                  <div>
                    <label className={labelCls}>Lugar de nacimiento</label>
                    <input type="text" value={form.lugarNacimiento} onChange={set("lugarNacimiento")} className={inputCls} style={ringStyle} />
                  </div>
                  <div className="col-span-2">
                    <label className={labelCls}>Dirección</label>
                    <textarea value={form.direccion} onChange={set("direccion")} rows={2} className={inputCls} style={ringStyle} />
                  </div>
                </div>
              </div>
            )}

            {seccion === "familiar" && (
              <div>
                <SectionTitle icon={
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
                }>
                  Origen y entorno
                </SectionTitle>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className={labelCls}>Nacionalidad</label>
                    <input type="text" value={form.nacionalidad} onChange={set("nacionalidad")} className={inputCls} style={ringStyle} />
                  </div>
                  <div>
                    <label className={labelCls}>Etnia</label>
                    <input type="text" value={form.etnia} onChange={set("etnia")} className={inputCls} style={ringStyle} />
                  </div>
                  <div>
                    <label className={labelCls}>Vive con</label>
                    <input type="text" value={form.viveCon} onChange={set("viveCon")} placeholder="Ej: Ambos padres" className={inputCls} style={ringStyle} />
                  </div>
                  <div>
                    <label className={labelCls}>N.º de hermanos</label>
                    <input type="number" min="0" value={form.numerosHermanos} onChange={set("numerosHermanos")} className={inputCls} style={ringStyle} />
                  </div>
                  <div className="col-span-2 flex items-center gap-2 pt-1">
                    <input type="checkbox" id="beneficioSocial" checked={form.beneficioSocial} onChange={set("beneficioSocial")} className="w-4 h-4" />
                    <label htmlFor="beneficioSocial" className="text-sm text-slate-600">Recibe beneficio social</label>
                  </div>
                </div>

                <SectionTitle icon={
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" /></svg>
                }>
                  Discapacidad
                </SectionTitle>
                <div className="grid grid-cols-2 gap-3">
                  <div className="col-span-2 flex items-center gap-2">
                    <input type="checkbox" id="discapacidad" checked={form.discapacidad} onChange={set("discapacidad")} className="w-4 h-4" />
                    <label htmlFor="discapacidad" className="text-sm text-slate-600">Tiene alguna discapacidad</label>
                  </div>
                  {form.discapacidad && (
                    <>
                      <div>
                        <label className={labelCls}>Tipo de discapacidad</label>
                        <input type="text" value={form.tipoDiscapacidad} onChange={set("tipoDiscapacidad")} className={inputCls} style={ringStyle} />
                      </div>
                      <div>
                        <label className={labelCls}>Porcentaje (%)</label>
                        <input type="number" min="0" max="100" value={form.porcentajeDisc} onChange={set("porcentajeDisc")} className={inputCls} style={ringStyle} />
                      </div>
                      <div className="col-span-2">
                        <label className={labelCls}>Carnet CONADIS</label>
                        <input type="text" value={form.carnetConadis} onChange={set("carnetConadis")} className={inputCls} style={ringStyle} />
                      </div>
                    </>
                  )}
                </div>
              </div>
            )}

            {seccion === "representante" && (
              <div>
                <SectionTitle icon={
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" /></svg>
                }>
                  Representante legal
                </SectionTitle>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className={labelCls}>Cédula</label>
                    <input type="text" value={representante.cedula} onChange={setRep("cedula")} placeholder="Si ya existe, se reutiliza" className={inputCls} style={ringStyle} />
                  </div>
                  <div>
                    <label className={labelCls}>Parentesco</label>
                    <input type="text" value={representante.parentesco} onChange={setRep("parentesco")} placeholder="Ej: Madre" className={inputCls} style={ringStyle} />
                  </div>
                  <div>
                    <label className={labelCls}>Nombres</label>
                    <input type="text" value={representante.nombres} onChange={setRep("nombres")} required className={inputCls} style={ringStyle} />
                  </div>
                  <div>
                    <label className={labelCls}>Apellidos</label>
                    <input type="text" value={representante.apellidos} onChange={setRep("apellidos")} required className={inputCls} style={ringStyle} />
                  </div>
                  <div>
                    <label className={labelCls}>Teléfono principal</label>
                    <input type="text" value={representante.telefonoPrincipal} onChange={setRep("telefonoPrincipal")} required className={inputCls} style={ringStyle} />
                  </div>
                  <div>
                    <label className={labelCls}>Teléfono alterno</label>
                    <input type="text" value={representante.telefonoAlt} onChange={setRep("telefonoAlt")} className={inputCls} style={ringStyle} />
                  </div>
                  <div>
                    <label className={labelCls}>Correo</label>
                    <input type="email" value={representante.correo} onChange={setRep("correo")} className={inputCls} style={ringStyle} />
                  </div>
                  <div>
                    <label className={labelCls}>Dirección</label>
                    <input type="text" value={representante.direccion} onChange={setRep("direccion")} className={inputCls} style={ringStyle} />
                  </div>
                </div>
              </div>
            )}
          </div>

          <div className="flex gap-3 p-6 pt-4 border-t border-slate-100 flex-shrink-0">
            <button type="button" onClick={onCancel} className="flex-1 py-2 border border-slate-200 rounded-lg text-sm text-slate-600 hover:bg-slate-50 transition">
              Cancelar
            </button>
            <button type="submit" disabled={saving} style={{ backgroundColor: PRIMARY }}
              className="flex-1 py-2 text-white rounded-lg text-sm font-medium hover:opacity-90 transition disabled:opacity-60">
              {saving ? "Guardando..." : esEdicion ? "Guardar cambios" : "Registrar estudiante"}
            </button>
          </div>
        </form>
        </>
        )}
      </div>
    </div>
  );
}
