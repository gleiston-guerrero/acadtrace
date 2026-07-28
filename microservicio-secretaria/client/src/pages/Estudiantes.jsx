import { useState, useEffect, useCallback } from 'react';
import Layout from '../components/Layout';
import api, { apiPrincipal } from '../utils/api';

const PRIMARY = '#243A76';
const PRINCIPAL_ORIGIN = (apiPrincipal.defaults.baseURL || 'http://localhost:8080/api').replace(/\/api\/?$/, '');

const EMPTY = {
  cedula: '', nombres: '', apellidos: '', fecha_nacimiento: '',
  genero: '', correo: '', telefono: '', direccion: '',
  discapacidad: false, tipo_discapacidad: '', porcentaje_disc: '',
  nacionalidad: 'Ecuatoriana', etnia: '', lugar_nacimiento: '', vive_con: '',
  numeros_hermanos: '', beneficio_social: false, carnet_conadis: '', foto_url: '',
  id_representante: null,
};
const REPRESENTANTE_VACIO = {
  cedula: '', nombres: '', apellidos: '', parentesco: '',
  telefono_principal: '', telefono_alt: '', correo: '', direccion: '',
};

const menuItems = [
  { id: 'lista', label: 'Lista de estudiantes', icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 10h16M4 14h16M4 18h16" /></svg> },
  { id: 'nuevo', label: 'Nuevo estudiante', icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg> },
  { id: 'importar', label: 'Importar estudiantes', icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M12 12v9m0-9l-3 3m3-3l3 3" /></svg> },
];

export default function Estudiantes() {
  const [estudiantes, setEstudiantes] = useState([]);
  const [meta, setMeta] = useState({});
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(false);
  const [seccion, setSeccion] = useState('lista');
  const [modal, setModal] = useState(null);
  const [showImportModal, setShowImportModal] = useState(false);
  const [selected, setSelected] = useState(null);
  const [editId, setEditId] = useState(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [fotoAmpliada, setFotoAmpliada] = useState(null);

  const cargar = useCallback(async () => {
    setLoading(true);
    try {
      const res = await api.get('/estudiantes', { params: { q: search || undefined, page, limit: 15 } });
      setEstudiantes(res.data.data || []);
      setMeta(res.data.meta || {});
    } catch (e) {
      console.error('Error cargando estudiantes:', e);
    } finally {
      setLoading(false);
    }
  }, [search, page]);

  useEffect(() => { cargar(); }, [cargar]);
  useEffect(() => {
    if (success) { const t = setTimeout(() => setSuccess(''), 4000); return () => clearTimeout(t); }
  }, [success]);

  const handleSeccion = (id) => {
    if (id === 'nuevo') { setEditId(null); setModal('form'); return; }
    if (id === 'importar') { setShowImportModal(true); return; }
    setSeccion(id);
  };

  const abrirVer = async (id) => {
    try {
      const res = await api.get(`/estudiantes/${id}`);
      setSelected(res.data);
      setModal('ver');
    } catch (e) {
      console.error('Error cargando detalle:', e);
    }
  };

  const cambiarEstado = async (id, estadoActual) => {
    try {
      await api.patch(`/estudiantes/${id}/estado`, { estado: !estadoActual });
      cargar();
    } catch (e) {
      console.error('Error cambiando estado:', e);
    }
  };

  const esActivo = (e) => e.estado === true || e.estado === 'true';

  return (
    <Layout breadcrumb={['Inicio', 'Estudiantes']} sidebarTitle="Estudiantes" menuItems={menuItems} seccion={seccion} onSeccionChange={handleSeccion}>
      <div className="flex items-center justify-between mb-4">
        <div>
          <h1 className="text-base font-bold text-slate-700">Estudiantes</h1>
          <p className="text-xs text-slate-400">Registro y gestión de estudiantes</p>
        </div>
        <div className="flex items-center gap-2">
          <button onClick={() => setShowImportModal(true)}
            className="flex items-center gap-2 border border-slate-200 text-slate-600 text-sm px-4 py-2 rounded-lg hover:bg-slate-50 transition">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M12 12v9m0-9l-3 3m3-3l3 3" /></svg>
            Importar estudiantes
          </button>
          <button onClick={() => { setEditId(null); setModal('form'); }} style={{ backgroundColor: PRIMARY }}
            className="flex items-center gap-2 text-white text-sm px-4 py-2 rounded-lg hover:opacity-90 transition shadow-sm">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
            </svg>
            Nuevo estudiante
          </button>
        </div>
      </div>

      {error && (
        <div className="mb-4 bg-red-50 border border-red-200 rounded-xl px-4 py-3 flex justify-between">
          <span className="text-red-600 text-sm">{error}</span>
          <button onClick={() => setError('')} className="text-red-400 ml-4">✕</button>
        </div>
      )}
      {success && (
        <div className="mb-4 bg-green-50 border border-green-200 rounded-xl px-4 py-3 flex justify-between">
          <span className="text-green-600 text-sm">{success}</span>
          <button onClick={() => setSuccess('')} className="text-green-400 ml-4">✕</button>
        </div>
      )}

      {/* Buscador */}
      <div className="bg-white border border-slate-200 rounded-xl p-3 mb-4">
        <div className="relative">
          <svg className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          <input value={search} onChange={e => { setSearch(e.target.value); setPage(1); }}
            placeholder="Buscar por nombre, apellido o cédula..."
            className="w-full pl-9 pr-4 py-2 text-sm border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 bg-slate-50" />
        </div>
      </div>

      {/* Tabla */}
      <div className="bg-white border border-slate-200 rounded-xl overflow-hidden">
        <table className="w-full text-sm">
          <thead>
            <tr style={{ backgroundColor: PRIMARY }} className="text-white text-xs">
              <th className="px-4 py-3 text-left">Estudiante</th>
              <th className="px-4 py-3 text-left">Código</th>
              <th className="px-4 py-3 text-left">Cédula</th>
              <th className="px-4 py-3 text-left">Teléfono</th>
              <th className="px-4 py-3 text-center">Estado</th>
              <th className="px-4 py-3 text-center">Acciones</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={6} className="text-center py-12">
                <div className="flex items-center justify-center gap-2 text-slate-400">
                  <svg className="w-5 h-5 animate-spin" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
                  </svg>
                  <span className="text-sm">Cargando...</span>
                </div>
              </td></tr>
            ) : estudiantes.length === 0 ? (
              <tr><td colSpan={6} className="text-center py-12 text-slate-400 text-sm">
                {search ? `No se encontraron resultados para "${search}"` : 'No hay estudiantes registrados'}
              </td></tr>
            ) : estudiantes.map((e, i) => (
              <tr key={e.id_estudiante}
                className={`border-t border-slate-100 ${i % 2 === 1 ? 'bg-slate-50/40' : ''} hover:bg-blue-50/30 transition`}>
                <td className="px-4 py-2.5">
                  <div className="flex items-center gap-3">
                    {e.foto_url ? (
                      <button type="button" onClick={() => setFotoAmpliada({ url: e.foto_url, nombre: `${e.nombres} ${e.apellidos}` })}
                        className="relative w-8 h-8 rounded-full flex-shrink-0 group" title="Ver foto">
                        <img src={`${PRINCIPAL_ORIGIN}${e.foto_url}`} alt="" className="w-8 h-8 rounded-full object-cover border border-slate-200" />
                        <span className="absolute inset-0 rounded-full bg-black/0 group-hover:bg-black/30 transition flex items-center justify-center">
                          <svg className="w-3 h-3 text-white opacity-0 group-hover:opacity-100 transition" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                          </svg>
                        </span>
                      </button>
                    ) : (
                      <div className="w-8 h-8 rounded-full flex items-center justify-center text-white text-xs font-bold flex-shrink-0" style={{ backgroundColor: PRIMARY }}>
                        {e.nombres?.[0]}{e.apellidos?.[0]}
                      </div>
                    )}
                    <span className="font-medium text-slate-700">{e.apellidos}, {e.nombres}</span>
                  </div>
                </td>
                <td className="px-4 py-2.5 text-xs text-slate-400 font-mono">{e.codigo_estudiante || '—'}</td>
                <td className="px-4 py-2.5 text-slate-500">{e.cedula || '—'}</td>
                <td className="px-4 py-2.5 text-slate-500">{e.telefono || '—'}</td>
                <td className="px-4 py-2.5 text-center">
                  <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${esActivo(e) ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-600'}`}>
                    {esActivo(e) ? 'Activo' : 'Inactivo'}
                  </span>
                </td>
                <td className="px-4 py-2.5 text-center">
                  <div className="flex items-center justify-center gap-1">
                    <button onClick={() => abrirVer(e.id_estudiante)} title="Ver detalle"
                      className="p-1.5 rounded-lg text-slate-400 hover:text-blue-600 hover:bg-blue-50 transition">
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                      </svg>
                    </button>
                    <button onClick={() => { setEditId(e.id_estudiante); setModal('form'); }} title="Editar"
                      className="p-1.5 rounded-lg text-slate-400 hover:text-amber-600 hover:bg-amber-50 transition">
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                      </svg>
                    </button>
                    <button onClick={() => cambiarEstado(e.id_estudiante, esActivo(e))}
                      title={esActivo(e) ? 'Desactivar' : 'Activar'}
                      className={`p-1.5 rounded-lg transition ${esActivo(e) ? 'text-slate-400 hover:text-red-500 hover:bg-red-50' : 'text-slate-400 hover:text-green-600 hover:bg-green-50'}`}>
                      {esActivo(e)
                        ? <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
                        : <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
                      }
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {meta.pages > 1 && (
          <div className="border-t border-slate-100 px-4 py-3 flex items-center justify-between text-xs text-slate-500">
            <span>{estudiantes.length} de {meta.total} estudiantes</span>
            <div className="flex gap-1">
              {Array.from({ length: meta.pages }, (_, i) => i + 1).map(p => (
                <button key={p} onClick={() => setPage(p)}
                  style={p === page ? { backgroundColor: PRIMARY, color: 'white' } : {}}
                  className={`w-7 h-7 rounded-lg text-xs font-medium transition ${p !== page ? 'hover:bg-slate-100' : ''}`}>
                  {p}
                </button>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* MODAL VER */}
      {modal === 'ver' && selected && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg max-h-[90vh] overflow-y-auto">
            <div style={{ backgroundColor: PRIMARY }} className="px-6 py-4 flex items-center justify-between rounded-t-2xl">
              <h2 className="text-white font-semibold text-sm">Ficha del Estudiante</h2>
              <button onClick={() => setModal(null)} className="text-white/70 hover:text-white">
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </div>
            <div className="p-6 space-y-3">
              <div className="text-center mb-5">
                {selected.foto_url ? (
                  <img src={`${PRINCIPAL_ORIGIN}${selected.foto_url}`} alt="" className="w-16 h-16 rounded-full object-cover mx-auto mb-2 border border-slate-200" />
                ) : (
                  <div className="w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-2" style={{ backgroundColor: '#e8edf7' }}>
                    <span style={{ color: PRIMARY }} className="text-2xl font-bold">{selected.nombres?.charAt(0)}</span>
                  </div>
                )}
                <p className="font-bold text-slate-700 text-base">{selected.nombres} {selected.apellidos}</p>
                <p className="text-xs text-slate-400 font-mono mt-0.5">{selected.codigo_estudiante || 'Sin código'}</p>
              </div>
              {[
                ['Cédula', selected.cedula],
                ['Fecha de nacimiento', selected.fecha_nacimiento ? new Date(selected.fecha_nacimiento).toLocaleDateString('es-EC') : null],
                ['Género', selected.genero],
                ['Teléfono', selected.telefono],
                ['Correo', selected.correo],
                ['Dirección', selected.direccion],
                ['Nacionalidad', selected.nacionalidad],
                ['Etnia', selected.etnia],
                ['Lugar de nacimiento', selected.lugar_nacimiento],
                ['Vive con', selected.vive_con],
              ].filter(([, v]) => v).map(([k, v]) => (
                <div key={k} className="flex justify-between text-sm py-1.5 border-b border-slate-100 last:border-0">
                  <span className="text-slate-400 text-xs">{k}</span>
                  <span className="text-slate-700 font-medium text-xs text-right max-w-48">{v}</span>
                </div>
              ))}
              {selected.rep_nombres && (
                <div className="bg-slate-50 rounded-xl p-4 mt-3">
                  <p className="text-xs font-bold text-slate-400 mb-2 uppercase tracking-wide">Representante</p>
                  <p className="text-sm font-medium text-slate-700">{selected.rep_nombres} {selected.rep_apellidos}</p>
                  <p className="text-xs text-slate-500 mt-0.5">{selected.parentesco} · {selected.rep_telefono}</p>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* MODAL NUEVO/EDITAR */}
      {modal === 'form' && (
        <EstudianteFormModal
          estudianteId={editId}
          onCancel={() => setModal(null)}
          onSuccess={(msg) => { setModal(null); setSeccion('lista'); setSuccess(msg); cargar(); }}
        />
      )}

      {/* MODAL IMPORTAR */}
      {showImportModal && (
        <ImportarEstudiantesModal
          onCancel={() => setShowImportModal(false)}
          onSuccess={(msg) => { setShowImportModal(false); setSeccion('lista'); setSuccess(msg); cargar(); }}
        />
      )}

      {/* VISUALIZADOR DE FOTO */}
      {fotoAmpliada && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center px-4 bg-black/50" onClick={() => setFotoAmpliada(null)}>
          <div className="bg-white rounded-2xl shadow-2xl overflow-hidden max-w-sm w-full" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between px-4 py-3 border-b border-slate-100">
              <p className="text-sm font-semibold text-slate-700 truncate">{fotoAmpliada.nombre}</p>
              <button onClick={() => setFotoAmpliada(null)} className="text-slate-400 hover:text-slate-600 flex-shrink-0 ml-3">✕</button>
            </div>
            <div className="p-4 flex items-center justify-center bg-slate-50">
              <img src={`${PRINCIPAL_ORIGIN}${fotoAmpliada.url}`} alt={fotoAmpliada.nombre} className="max-w-full max-h-[70vh] rounded-xl object-contain shadow-sm" />
            </div>
          </div>
        </div>
      )}
    </Layout>
  );
}

/* ───────────────────────── Modal Nuevo/Editar Estudiante ───────────────────────── */

const TABS = [
  { id: 'estudiante', label: 'Estudiante' },
  { id: 'familiar', label: 'Datos familiares' },
  { id: 'representante', label: 'Representante' },
];

function FotoUpload({ fotoUrl, nombres, apellidos, onUploaded }) {
  const [preview, setPreview] = useState(null);
  const [subiendo, setSubiendo] = useState(false);
  const [error, setError] = useState('');

  const handleFile = async (e) => {
    const archivo = e.target.files?.[0];
    if (!archivo) return;
    setError('');
    setPreview(URL.createObjectURL(archivo));
    setSubiendo(true);
    try {
      const formData = new FormData();
      formData.append('archivo', archivo);
      const r = await apiPrincipal.post('/uploads/foto', formData, { headers: { 'Content-Type': 'multipart/form-data' } });
      onUploaded(r.data.url);
    } catch (err) {
      setError(err.response?.data?.message || 'No se pudo subir la foto');
      setPreview(null);
    } finally {
      setSubiendo(false);
    }
  };

  const src = preview || (fotoUrl ? `${PRINCIPAL_ORIGIN}${fotoUrl}` : null);

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
        <label style={{ backgroundColor: PRIMARY }}
          className="absolute bottom-0 right-0 w-7 h-7 rounded-full flex items-center justify-center text-white cursor-pointer shadow-md hover:opacity-90 transition" title="Subir foto">
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

function SectionTitle({ children }) {
  return (
    <div className="flex items-center gap-2 mb-3 first:mt-0 mt-5">
      <h3 className="text-xs font-bold uppercase tracking-wider text-slate-500">{children}</h3>
      <div className="flex-1 h-px bg-slate-100" />
    </div>
  );
}

function EstudianteFormModal({ estudianteId, onCancel, onSuccess }) {
  const esEdicion = !!estudianteId;
  const [seccion, setSeccion] = useState('estudiante');
  const [form, setForm] = useState(EMPTY);
  const [representante, setRepresentante] = useState(REPRESENTANTE_VACIO);
  const [repBusqueda, setRepBusqueda] = useState('');
  const [repResultados, setRepResultados] = useState([]);
  const [repSeleccionado, setRepSeleccionado] = useState(null);
  const [repModoNuevo, setRepModoNuevo] = useState(false);
  const [cargando, setCargando] = useState(esEdicion);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!esEdicion) return;
    api.get(`/estudiantes/${estudianteId}`)
      .then(r => {
        const d = r.data;
        setForm({
          cedula: d.cedula || '', nombres: d.nombres || '', apellidos: d.apellidos || '',
          fecha_nacimiento: d.fecha_nacimiento ? d.fecha_nacimiento.split('T')[0] : '',
          genero: d.genero || '', correo: d.correo || '', telefono: d.telefono || '',
          direccion: d.direccion || '', discapacidad: d.discapacidad || false,
          tipo_discapacidad: d.tipo_discapacidad || '', porcentaje_disc: d.porcentaje_disc || '',
          nacionalidad: d.nacionalidad || 'Ecuatoriana', etnia: d.etnia || '',
          lugar_nacimiento: d.lugar_nacimiento || '', vive_con: d.vive_con || '',
          numeros_hermanos: d.numeros_hermanos ?? '', beneficio_social: d.beneficio_social || false,
          carnet_conadis: d.carnet_conadis || '', foto_url: d.foto_url || '',
          id_representante: d.id_representante || null,
        });
        if (d.rep_nombres) {
          setRepSeleccionado({ id_representante: d.id_representante, nombres: d.rep_nombres, apellidos: d.rep_apellidos, telefono_principal: d.rep_telefono, parentesco: d.parentesco });
        }
      })
      .catch(() => setError('No se pudo cargar el estudiante'))
      .finally(() => setCargando(false));
  }, [estudianteId, esEdicion]);

  useEffect(() => {
    if (seccion !== 'representante' || repModoNuevo) return;
    const t = setTimeout(() => {
      api.get('/representantes', { params: { q: repBusqueda || undefined } })
        .then(r => setRepResultados(r.data || []))
        .catch(() => setRepResultados([]));
    }, 300);
    return () => clearTimeout(t);
  }, [repBusqueda, seccion, repModoNuevo]);

  const set = (campo) => (e) => {
    const val = e.target.type === 'checkbox' ? e.target.checked : e.target.value;
    setForm({ ...form, [campo]: val });
  };
  const setRep = (campo) => (e) => setRepresentante({ ...representante, [campo]: e.target.value });

  const seleccionarRepresentante = (r) => {
    setRepSeleccionado(r);
    setForm({ ...form, id_representante: r.id_representante });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true); setError('');
    try {
      let idRepresentante = form.id_representante;

      if (repModoNuevo && (representante.nombres || representante.apellidos)) {
        const nuevo = await api.post('/representantes', representante);
        idRepresentante = nuevo.data.id_representante;
      }

      const payload = {
        ...form,
        porcentaje_disc: form.porcentaje_disc === '' ? null : Number(form.porcentaje_disc),
        numeros_hermanos: form.numeros_hermanos === '' ? null : Number(form.numeros_hermanos),
        id_representante: idRepresentante,
      };

      if (esEdicion) {
        await api.put(`/estudiantes/${estudianteId}`, payload);
        onSuccess(`Estudiante ${form.nombres} ${form.apellidos} actualizado correctamente`);
      } else {
        await api.post('/estudiantes', payload);
        onSuccess(`Estudiante ${form.nombres} ${form.apellidos} registrado correctamente`);
      }
    } catch (err) {
      setError(err.response?.data?.error || err.response?.data?.detalles?.[0]?.mensaje || 'Error al guardar');
    } finally {
      setSaving(false);
    }
  };

  const inputCls = 'w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-slate-50';
  const labelCls = 'block text-xs font-semibold text-slate-500 mb-1 uppercase tracking-wide';

  return (
    <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl max-h-[92vh] overflow-hidden flex flex-col">
        <div style={{ backgroundColor: PRIMARY }} className="px-6 py-4 flex items-center justify-between flex-shrink-0">
          <h2 className="text-white font-semibold text-sm">{esEdicion ? 'Editar Estudiante' : 'Nuevo Estudiante'}</h2>
          <button onClick={onCancel} className="text-white/70 hover:text-white">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
          </button>
        </div>

        {cargando ? (
          <div className="p-16 text-center text-slate-400 text-sm">
            <svg className="w-6 h-6 animate-spin mx-auto mb-2 text-slate-300" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
            </svg>
            Cargando estudiante...
          </div>
        ) : (
          <>
            <div className="flex border-b border-slate-100 px-6 flex-shrink-0">
              {TABS.map(t => (
                <button key={t.id} type="button" onClick={() => setSeccion(t.id)}
                  className={`px-4 py-3 text-sm font-medium border-b-2 -mb-px transition ${seccion === t.id ? 'border-current' : 'border-transparent text-slate-400 hover:text-slate-600'}`}
                  style={seccion === t.id ? { color: PRIMARY, borderColor: PRIMARY } : {}}>
                  {t.label}
                </button>
              ))}
            </div>

            <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto flex flex-col">
              <div className="p-6 flex-1">
                {error && <div className="mb-4 bg-red-50 border border-red-200 rounded-lg px-3 py-2 text-red-600 text-xs">{error}</div>}

                {seccion === 'estudiante' && (
                  <div>
                    <FotoUpload fotoUrl={form.foto_url} nombres={form.nombres} apellidos={form.apellidos} onUploaded={(url) => setForm({ ...form, foto_url: url })} />
                    <SectionTitle>Identificación</SectionTitle>
                    <div className="grid grid-cols-2 gap-3">
                      <div className="col-span-2">
                        <label className={labelCls}>Cédula</label>
                        <input value={form.cedula} onChange={set('cedula')} required maxLength={10} className={inputCls} />
                      </div>
                      <div>
                        <label className={labelCls}>Nombres</label>
                        <input value={form.nombres} onChange={set('nombres')} required className={inputCls} />
                      </div>
                      <div>
                        <label className={labelCls}>Apellidos</label>
                        <input value={form.apellidos} onChange={set('apellidos')} required className={inputCls} />
                      </div>
                      <div>
                        <label className={labelCls}>Género</label>
                        <select value={form.genero} onChange={set('genero')} className={inputCls}>
                          <option value="">Seleccionar</option>
                          <option value="MASCULINO">Masculino</option>
                          <option value="FEMENINO">Femenino</option>
                          <option value="OTRO">Otro</option>
                        </select>
                      </div>
                      <div>
                        <label className={labelCls}>Fecha de nacimiento</label>
                        <input type="date" value={form.fecha_nacimiento} onChange={set('fecha_nacimiento')} className={inputCls} />
                      </div>
                    </div>

                    <SectionTitle>Contacto</SectionTitle>
                    <div className="grid grid-cols-2 gap-3">
                      <div>
                        <label className={labelCls}>Correo</label>
                        <input type="email" value={form.correo} onChange={set('correo')} className={inputCls} />
                      </div>
                      <div>
                        <label className={labelCls}>Teléfono</label>
                        <input value={form.telefono} onChange={set('telefono')} className={inputCls} />
                      </div>
                      <div className="col-span-2">
                        <label className={labelCls}>Dirección</label>
                        <textarea value={form.direccion} onChange={set('direccion')} rows={2} className={inputCls} />
                      </div>
                    </div>
                  </div>
                )}

                {seccion === 'familiar' && (
                  <div>
                    <SectionTitle>Origen y entorno</SectionTitle>
                    <div className="grid grid-cols-2 gap-3">
                      <div>
                        <label className={labelCls}>Nacionalidad</label>
                        <input value={form.nacionalidad} onChange={set('nacionalidad')} className={inputCls} />
                      </div>
                      <div>
                        <label className={labelCls}>Etnia</label>
                        <input value={form.etnia} onChange={set('etnia')} className={inputCls} />
                      </div>
                      <div>
                        <label className={labelCls}>Lugar de nacimiento</label>
                        <input value={form.lugar_nacimiento} onChange={set('lugar_nacimiento')} className={inputCls} />
                      </div>
                      <div>
                        <label className={labelCls}>Vive con</label>
                        <input value={form.vive_con} onChange={set('vive_con')} placeholder="Ej: Ambos padres" className={inputCls} />
                      </div>
                      <div>
                        <label className={labelCls}>N.º de hermanos</label>
                        <input type="number" min="0" value={form.numeros_hermanos} onChange={set('numeros_hermanos')} className={inputCls} />
                      </div>
                      <div className="col-span-2 flex items-center gap-2 pt-1">
                        <input type="checkbox" checked={form.beneficio_social} onChange={set('beneficio_social')} className="rounded" />
                        <span className="text-sm text-slate-600">Recibe beneficio social</span>
                      </div>
                    </div>

                    <SectionTitle>Discapacidad</SectionTitle>
                    <div className="grid grid-cols-2 gap-3">
                      <div className="col-span-2 flex items-center gap-2">
                        <input type="checkbox" checked={form.discapacidad} onChange={set('discapacidad')} className="rounded" />
                        <span className="text-sm text-slate-600">Tiene alguna discapacidad</span>
                      </div>
                      {form.discapacidad && (
                        <>
                          <div>
                            <label className={labelCls}>Tipo de discapacidad</label>
                            <input value={form.tipo_discapacidad} onChange={set('tipo_discapacidad')} className={inputCls} />
                          </div>
                          <div>
                            <label className={labelCls}>Porcentaje (%)</label>
                            <input type="number" min="0" max="100" value={form.porcentaje_disc} onChange={set('porcentaje_disc')} className={inputCls} />
                          </div>
                          <div className="col-span-2">
                            <label className={labelCls}>Carnet CONADIS</label>
                            <input value={form.carnet_conadis} onChange={set('carnet_conadis')} className={inputCls} />
                          </div>
                        </>
                      )}
                    </div>
                  </div>
                )}

                {seccion === 'representante' && (
                  <div>
                    <SectionTitle>Representante legal</SectionTitle>

                    {repSeleccionado && !repModoNuevo && (
                      <div className="bg-blue-50 border border-blue-200 rounded-xl p-4 mb-3 flex items-center justify-between">
                        <div>
                          <p className="text-sm font-semibold text-slate-700">{repSeleccionado.nombres} {repSeleccionado.apellidos}</p>
                          <p className="text-xs text-slate-500">{repSeleccionado.parentesco} · {repSeleccionado.telefono_principal}</p>
                        </div>
                        <button type="button" onClick={() => { setRepSeleccionado(null); setForm({ ...form, id_representante: null }); }}
                          className="text-xs text-red-500 hover:underline">Quitar</button>
                      </div>
                    )}

                    {!repSeleccionado && !repModoNuevo && (
                      <div>
                        <input value={repBusqueda} onChange={e => setRepBusqueda(e.target.value)}
                          placeholder="Buscar representante existente por nombre o cédula..." className={inputCls} />
                        <div className="mt-2 max-h-40 overflow-y-auto border border-slate-100 rounded-lg divide-y divide-slate-50">
                          {repResultados.map(r => (
                            <button type="button" key={r.id_representante} onClick={() => seleccionarRepresentante(r)}
                              className="w-full text-left px-3 py-2 text-sm hover:bg-slate-50 transition">
                              <span className="font-medium text-slate-700">{r.nombres} {r.apellidos}</span>
                              <span className="text-xs text-slate-400 ml-2">{r.parentesco} · {r.telefono_principal}</span>
                            </button>
                          ))}
                          {repResultados.length === 0 && (
                            <p className="px-3 py-2 text-xs text-slate-400">Sin resultados. Puedes crear uno nuevo.</p>
                          )}
                        </div>
                        <button type="button" onClick={() => setRepModoNuevo(true)}
                          className="mt-2 text-xs font-medium underline" style={{ color: PRIMARY }}>
                          + Registrar un representante nuevo
                        </button>
                      </div>
                    )}

                    {repModoNuevo && (
                      <div>
                        <div className="flex justify-end mb-2">
                          <button type="button" onClick={() => setRepModoNuevo(false)} className="text-xs text-slate-400 hover:underline">
                            Cancelar y buscar existente
                          </button>
                        </div>
                        <div className="grid grid-cols-2 gap-3">
                          <div>
                            <label className={labelCls}>Cédula</label>
                            <input value={representante.cedula} onChange={setRep('cedula')} className={inputCls} />
                          </div>
                          <div>
                            <label className={labelCls}>Parentesco</label>
                            <input value={representante.parentesco} onChange={setRep('parentesco')} placeholder="Ej: Madre" className={inputCls} />
                          </div>
                          <div>
                            <label className={labelCls}>Nombres</label>
                            <input value={representante.nombres} onChange={setRep('nombres')} required className={inputCls} />
                          </div>
                          <div>
                            <label className={labelCls}>Apellidos</label>
                            <input value={representante.apellidos} onChange={setRep('apellidos')} required className={inputCls} />
                          </div>
                          <div>
                            <label className={labelCls}>Teléfono principal</label>
                            <input value={representante.telefono_principal} onChange={setRep('telefono_principal')} required className={inputCls} />
                          </div>
                          <div>
                            <label className={labelCls}>Teléfono alterno</label>
                            <input value={representante.telefono_alt} onChange={setRep('telefono_alt')} className={inputCls} />
                          </div>
                          <div>
                            <label className={labelCls}>Correo</label>
                            <input value={representante.correo} onChange={setRep('correo')} className={inputCls} />
                          </div>
                          <div>
                            <label className={labelCls}>Dirección</label>
                            <input value={representante.direccion} onChange={setRep('direccion')} className={inputCls} />
                          </div>
                        </div>
                      </div>
                    )}
                  </div>
                )}
              </div>

              <div className="flex gap-3 p-6 pt-4 border-t border-slate-100 flex-shrink-0">
                <button type="button" onClick={onCancel} className="flex-1 py-2.5 border border-slate-200 rounded-lg text-sm text-slate-600 hover:bg-slate-50 transition">
                  Cancelar
                </button>
                <button type="submit" disabled={saving} style={{ backgroundColor: PRIMARY }}
                  className="flex-1 py-2.5 rounded-lg text-sm text-white font-semibold hover:opacity-90 transition disabled:opacity-60">
                  {saving ? 'Guardando...' : esEdicion ? 'Guardar cambios' : 'Registrar estudiante'}
                </button>
              </div>
            </form>
          </>
        )}
      </div>
    </div>
  );
}

/* ───────────────────────── Modal Importar Estudiantes ───────────────────────── */

function ImportarEstudiantesModal({ onCancel, onSuccess }) {
  const [archivo, setArchivo] = useState(null);
  const [preview, setPreview] = useState(null);
  const [cargando, setCargando] = useState(false);
  const [confirmando, setConfirmando] = useState(false);
  const [error, setError] = useState('');

  const [anoActual, setAnoActual] = useState(null);
  const [grados, setGrados] = useState([]);
  const [paralelos, setParalelos] = useState([]);
  const [idGrado, setIdGrado] = useState('');
  const [idParalelo, setIdParalelo] = useState('');
  const idAno = anoActual?.idAnoLectivo || '';

  useEffect(() => {
    // El listado completo de anos lectivos (GET /anos-lectivos) es solo para DIRECTOR;
    // Secretaria solo puede consultar el actual, que es el unico relevante para matricular al importar.
    apiPrincipal.get('/anos-lectivos/actual').then(r => setAnoActual(r.data)).catch(() => {});
    apiPrincipal.get('/grados').then(r => setGrados(r.data || [])).catch(() => {});
  }, []);

  useEffect(() => {
    setIdParalelo('');
    setParalelos([]);
    if (!idGrado) return;
    api.get(`/matriculas/paralelos/${idGrado}`).then(r => setParalelos(r.data || [])).catch(() => {});
  }, [idGrado]);

  const handlePrevisualizar = async (e) => {
    e.preventDefault();
    if (!archivo) { setError('Selecciona un archivo .csv, .xlsx, .xls o .pdf'); return; }
    setError(''); setCargando(true);
    try {
      const formData = new FormData();
      formData.append('archivo', archivo);
      const r = await api.post('/importacion-estudiantes/parsear', formData, { headers: { 'Content-Type': 'multipart/form-data' } });
      setPreview(r.data);
    } catch (err) {
      setError(err.response?.data?.error || 'No se pudo procesar el archivo');
      setPreview(null);
    } finally {
      setCargando(false);
    }
  };

  const handleConfirmar = async () => {
    setConfirmando(true); setError('');
    try {
      const r = await api.post('/importacion-estudiantes/confirmar', {
        estudiantes: preview.estudiantes,
        id_grado: idGrado || null,
        id_paralelo: idParalelo || null,
        id_ano_lectivo: idAno || null,
      });
      const res = r.data;
      const matriculaTxt = idGrado && idParalelo ? `, ${res.matriculados} matriculado${res.matriculados !== 1 ? 's' : ''}` : '';
      onSuccess(`${res.creados} estudiante${res.creados !== 1 ? 's' : ''} nuevo${res.creados !== 1 ? 's' : ''}, ${res.existentes} ya existían${matriculaTxt}${res.omitidos > 0 ? `, ${res.omitidos} omitido${res.omitidos !== 1 ? 's' : ''} por error` : ''} (total ${res.total}).`);
    } catch (err) {
      setError(err.response?.data?.error || 'Error al confirmar la importación');
    } finally {
      setConfirmando(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl max-h-[92vh] overflow-hidden flex flex-col">
        <div style={{ backgroundColor: PRIMARY }} className="px-6 py-4 flex items-center justify-between flex-shrink-0">
          <div>
            <h2 className="text-white font-semibold text-sm">Importar Estudiantes</h2>
            <p className="text-white/70 text-xs mt-0.5">Desde Excel, CSV o PDF (listado CAS)</p>
          </div>
          <button onClick={onCancel} className="text-white/70 hover:text-white">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
          </button>
        </div>

        <div className="p-6 overflow-y-auto flex-1">
          {error && (
            <div className="mb-4 bg-red-50 border border-red-200 rounded-xl px-4 py-3 flex justify-between">
              <span className="text-red-600 text-sm">{error}</span>
              <button onClick={() => setError('')} className="text-red-400 ml-4">✕</button>
            </div>
          )}

          {!preview ? (
            <form onSubmit={handlePrevisualizar} className="space-y-4">
              <div className="border-2 border-dashed border-slate-200 rounded-xl p-5 text-center hover:border-blue-300 transition">
                <input type="file" accept=".csv,.xlsx,.xls,.pdf" onChange={e => setArchivo(e.target.files?.[0] || null)}
                  className="w-full text-sm text-slate-600 file:mr-3 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-medium file:text-white file:cursor-pointer"
                  style={{ '--tw-file-bg': PRIMARY }} />
                <p className="text-xs text-slate-400 mt-2">Columnas esperadas: CEDULA, NOMBRES, APELLIDOS, CORREO (opcional)</p>
              </div>

              <div>
                <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-2">Matricular al importar (opcional)</p>
                <div className="grid grid-cols-3 gap-3">
                  <div>
                    <label className="block text-xs text-slate-400 mb-1">Año lectivo</label>
                    <div className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-slate-100 text-slate-500">
                      {anoActual ? anoActual.nombre : 'Cargando...'}
                    </div>
                  </div>
                  <div>
                    <label className="block text-xs text-slate-400 mb-1">Grado</label>
                    <select value={idGrado} onChange={e => setIdGrado(e.target.value)} disabled={!idAno}
                      className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-slate-50 disabled:opacity-50">
                      <option value="">— Seleccionar —</option>
                      {grados.map(g => <option key={g.idGrado} value={g.idGrado}>{g.nombre}</option>)}
                    </select>
                  </div>
                  <div>
                    <label className="block text-xs text-slate-400 mb-1">Paralelo</label>
                    <select value={idParalelo} onChange={e => setIdParalelo(e.target.value)} disabled={!idGrado || paralelos.length === 0}
                      className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-slate-50 disabled:opacity-50">
                      <option value="">{idGrado && paralelos.length === 0 ? 'Cargando...' : '— Seleccionar —'}</option>
                      {paralelos.map(p => <option key={p.id_paralelo} value={p.id_paralelo}>{p.letra}</option>)}
                    </select>
                  </div>
                </div>
                <p className="text-xs text-slate-400 mt-1.5">Si dejas esto vacío, los estudiantes se crean sin matrícula (puedes matricularlos luego desde el módulo de Matrículas).</p>
              </div>

              <div className="flex gap-3 pt-2">
                <button type="button" onClick={onCancel} className="flex-1 py-2 border border-slate-200 rounded-lg text-sm text-slate-600 hover:bg-slate-50 transition">
                  Cancelar
                </button>
                <button type="submit" disabled={cargando} style={{ backgroundColor: PRIMARY }}
                  className="flex-1 py-2 text-white rounded-lg text-sm font-medium hover:opacity-90 transition disabled:opacity-60">
                  {cargando ? 'Procesando...' : 'Previsualizar'}
                </button>
              </div>
            </form>
          ) : (
            <div>
              <div className="flex items-center justify-between mb-3">
                <p className="text-sm text-slate-500">
                  {preview.totalFilas} fila{preview.totalFilas !== 1 ? 's' : ''} — <span className="text-green-600 font-medium">{preview.filasValidas} válida{preview.filasValidas !== 1 ? 's' : ''}</span>
                  {preview.filasConError > 0 && <> — <span className="text-red-600 font-medium">{preview.filasConError} con error</span></>}
                </p>
                <button onClick={() => setPreview(null)} className="text-xs text-slate-400 hover:text-slate-600 underline">Volver</button>
              </div>

              <div className="bg-white rounded-xl border border-slate-200 overflow-hidden mb-4">
                <div className="overflow-x-auto" style={{ maxHeight: '20rem', overflowY: 'auto' }}>
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="border-b border-slate-100 sticky top-0" style={{ backgroundColor: '#f8f9fc' }}>
                        <th className="text-left px-3 py-2.5 text-xs font-semibold text-slate-500 uppercase tracking-wide">Cédula</th>
                        <th className="text-left px-3 py-2.5 text-xs font-semibold text-slate-500 uppercase tracking-wide">Apellidos</th>
                        <th className="text-left px-3 py-2.5 text-xs font-semibold text-slate-500 uppercase tracking-wide">Nombres</th>
                        <th className="text-center px-3 py-2.5 text-xs font-semibold text-slate-500 uppercase tracking-wide">Estado</th>
                      </tr>
                    </thead>
                    <tbody>
                      {preview.estudiantes.map((est, i) => (
                        <tr key={i} className="border-b border-slate-50">
                          <td className="px-3 py-2 text-slate-600">{est.cedula || '—'}</td>
                          <td className="px-3 py-2 text-slate-600">{est.apellidos || '—'}</td>
                          <td className="px-3 py-2 text-slate-600">{est.nombres || '—'}</td>
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
                <button onClick={handleConfirmar} disabled={confirmando || preview.filasValidas === 0}
                  style={{ backgroundColor: PRIMARY }}
                  className="flex-1 py-2 text-white rounded-lg text-sm font-medium hover:opacity-90 transition disabled:opacity-60">
                  {confirmando ? 'Confirmando...' : `Confirmar (${preview.filasValidas})`}
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
