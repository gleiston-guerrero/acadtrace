import { useState, useEffect, useCallback } from 'react';
import Layout from '../components/Layout';
import api from '../utils/api';

const PRIMARY = '#243A76';

const menuItems = [
  { id: 'lista', label: 'Lista de representantes', icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 10h16M4 14h16M4 18h16" /></svg> },
  { id: 'nuevo', label: 'Nuevo representante', icon: <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg> },
];

const EMPTY = {
  cedula: '', nombres: '', apellidos: '', parentesco: '',
  telefono_principal: '', telefono_alt: '', correo: '', direccion: '',
};

function Field({ label, value, mono, truncate, full }) {
  return (
    <div className={`min-w-0 ${full ? 'col-span-2' : ''}`}>
      <p className="text-[10px] font-semibold text-slate-400 uppercase tracking-wider mb-1">{label}</p>
      <p className={`text-sm text-slate-700 ${mono ? 'font-mono' : ''} ${truncate ? 'truncate' : ''}`}>{value || '—'}</p>
    </div>
  );
}

function Card({ title, children }) {
  return (
    <div className="bg-white border border-slate-200 rounded-xl overflow-hidden">
      <div className="flex items-center gap-2 px-5 py-3 border-b border-slate-100 bg-slate-50/70">
        <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500">{title}</h4>
      </div>
      <div className="p-5">{children}</div>
    </div>
  );
}

export default function Representantes() {
  const [representantes, setRepresentantes] = useState([]);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');
  const [modal, setModal] = useState(null);
  const [selected, setSelected] = useState(null);
  const [editId, setEditId] = useState(null);
  const [form, setForm] = useState(EMPTY);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [estudiantesRep, setEstudiantesRep] = useState([]);
  const [loadingEstudiantes, setLoadingEstudiantes] = useState(false);

  const cargar = useCallback(async () => {
    setLoading(true);
    try {
      const res = await api.get('/representantes', { params: { q: search || undefined } });
      setRepresentantes(res.data || []);
    } catch (e) {
      console.error('Error cargando representantes:', e);
    } finally {
      setLoading(false);
    }
  }, [search]);

  useEffect(() => { cargar(); }, [cargar]);
  useEffect(() => {
    if (success) { const t = setTimeout(() => setSuccess(''), 4000); return () => clearTimeout(t); }
  }, [success]);

  const abrirNuevo = () => { setEditId(null); setForm(EMPTY); setError(''); setModal('form'); };
  const abrirEditar = (r) => {
    setEditId(r.id_representante);
    setForm({
      cedula: r.cedula || '', nombres: r.nombres || '', apellidos: r.apellidos || '',
      parentesco: r.parentesco || '', telefono_principal: r.telefono_principal || '',
      telefono_alt: r.telefono_alt || '', correo: r.correo || '', direccion: r.direccion || '',
    });
    setError(''); setModal('form');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true); setError('');
    try {
      if (editId) {
        await api.put(`/representantes/${editId}`, form);
        setSuccess('Representante actualizado');
      } else {
        await api.post('/representantes', form);
        setSuccess('Representante registrado');
      }
      setModal(null);
      cargar();
    } catch (err) {
      setError(err.response?.data?.error || err.response?.data?.detalles?.[0]?.mensaje || 'Error al guardar representante');
    } finally {
      setSaving(false);
    }
  };

  const handleSeccion = (id) => {
    if (id === 'nuevo') { abrirNuevo(); return; }
  };

  const verDetalle = async (r) => {
    setSelected(r);
    setModal('ver');
    setEstudiantesRep([]);
    setLoadingEstudiantes(true);
    try {
      const res = await api.get(`/representantes/${r.id_representante}/estudiantes`);
      setEstudiantesRep(res.data || []);
    } catch (e) {
      console.error('Error cargando estudiantes del representante:', e);
    } finally {
      setLoadingEstudiantes(false);
    }
  };

  return (
    <Layout breadcrumb={['Inicio', 'Representantes']} sidebarTitle="Representantes" menuItems={menuItems} seccion="lista" onSeccionChange={handleSeccion}>
      <div className="flex items-center justify-between mb-4">
        <div>
          <h1 className="text-base font-bold text-slate-700">Representantes</h1>
          <p className="text-xs text-slate-400">Padres, madres y tutores de los estudiantes</p>
        </div>
        <button onClick={abrirNuevo} style={{ backgroundColor: PRIMARY }}
          className="flex items-center gap-2 text-white text-sm px-4 py-2 rounded-lg hover:opacity-90 transition shadow-sm">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
          </svg>
          Nuevo representante
        </button>
      </div>

      {error && !modal && (
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
          <input value={search} onChange={e => setSearch(e.target.value)}
            placeholder="Buscar por nombre, apellido o cédula..."
            className="w-full pl-9 pr-4 py-2 text-sm border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 bg-slate-50" />
        </div>
      </div>

      {/* Tabla */}
      <div className="bg-white border border-slate-200 rounded-2xl overflow-hidden">
        <table className="w-full text-sm">
          <thead>
            <tr style={{ backgroundColor: PRIMARY }} className="text-white text-xs">
              <th className="px-4 py-3 text-left">Nombre</th>
              <th className="px-4 py-3 text-left">Cédula</th>
              <th className="px-4 py-3 text-left">Parentesco</th>
              <th className="px-4 py-3 text-left">Teléfono</th>
              <th className="px-4 py-3 text-left">Correo</th>
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
            ) : representantes.length === 0 ? (
              <tr><td colSpan={6} className="text-center py-12 text-slate-400 text-sm">
                {search ? `Sin resultados para "${search}"` : 'No hay representantes registrados'}
              </td></tr>
            ) : representantes.map((r, i) => (
              <tr key={r.id_representante}
                className={`border-t border-slate-100 ${i % 2 === 1 ? 'bg-slate-50/40' : ''} hover:bg-blue-50/30 transition`}>
                <td className="px-4 py-2.5 font-medium text-slate-700">{r.apellidos}, {r.nombres}</td>
                <td className="px-4 py-2.5 text-slate-500 text-xs">{r.cedula || '—'}</td>
                <td className="px-4 py-2.5 text-slate-500 text-xs">{r.parentesco || '—'}</td>
                <td className="px-4 py-2.5 text-slate-500 text-xs">{r.telefono_principal || '—'}</td>
                <td className="px-4 py-2.5 text-slate-500 text-xs">{r.correo || '—'}</td>
                <td className="px-4 py-2.5 text-center">
                  <div className="flex items-center justify-center gap-1">
                    <button onClick={() => verDetalle(r)} title="Ver detalle"
                      className="p-1.5 rounded-lg text-slate-400 hover:text-blue-600 hover:bg-blue-50 transition">
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                      </svg>
                    </button>
                    <button onClick={() => abrirEditar(r)} title="Editar"
                      className="p-1.5 rounded-lg text-slate-400 hover:text-amber-600 hover:bg-amber-50 transition">
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                      </svg>
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <div className="border-t border-slate-100 px-4 py-2.5 text-xs text-slate-400">
          {representantes.length} representante{representantes.length !== 1 ? 's' : ''}
        </div>
      </div>

      {/* MODAL CREAR / EDITAR */}
      {modal === 'form' && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg max-h-[90vh] overflow-y-auto">
            <div style={{ backgroundColor: PRIMARY }} className="px-6 py-4 flex items-center justify-between rounded-t-2xl sticky top-0">
              <h2 className="text-white font-semibold text-sm">{editId ? 'Editar representante' : 'Nuevo representante'}</h2>
              <button onClick={() => setModal(null)} className="text-white/70 hover:text-white">
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </div>
            <form onSubmit={handleSubmit} className="p-6 space-y-4">
              <div className="grid grid-cols-2 gap-3">
                {[
                  { label: 'Nombres *', name: 'nombres', required: true },
                  { label: 'Apellidos *', name: 'apellidos', required: true },
                  { label: 'Cédula', name: 'cedula' },
                  { label: 'Parentesco', name: 'parentesco', placeholder: 'Padre, madre, tutor...' },
                  { label: 'Teléfono principal *', name: 'telefono_principal', required: true },
                  { label: 'Teléfono alternativo', name: 'telefono_alt' },
                  { label: 'Correo', name: 'correo', type: 'email' },
                  { label: 'Dirección', name: 'direccion' },
                ].map(f => (
                  <div key={f.name} className={f.name === 'direccion' ? 'col-span-2' : ''}>
                    <label className="block text-xs font-semibold text-slate-500 mb-1">{f.label}</label>
                    <input type={f.type || 'text'} required={f.required} placeholder={f.placeholder}
                      value={form[f.name]} onChange={e => setForm({ ...form, [f.name]: e.target.value })}
                      className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-slate-50" />
                  </div>
                ))}
              </div>
              {error && <div className="bg-red-50 border border-red-200 text-red-600 text-xs px-3 py-2 rounded-lg">{error}</div>}
              <div className="flex gap-3 pt-2">
                <button type="button" onClick={() => setModal(null)}
                  className="flex-1 py-2.5 border border-slate-200 rounded-lg text-sm text-slate-600 hover:bg-slate-50 transition">Cancelar</button>
                <button type="submit" disabled={saving} style={{ backgroundColor: PRIMARY }}
                  className="flex-1 py-2.5 rounded-lg text-sm text-white font-semibold hover:opacity-90 transition disabled:opacity-60">
                  {saving ? 'Guardando...' : editId ? 'Guardar cambios' : 'Registrar representante'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL VER */}
      {modal === 'ver' && selected && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4" onClick={() => setModal(null)}>
          <div className="bg-slate-50 rounded-2xl shadow-2xl w-full max-w-lg overflow-hidden flex flex-col max-h-[90vh]" onClick={e => e.stopPropagation()}>
            <div style={{ background: `linear-gradient(135deg, ${PRIMARY} 0%, #3d5a9e 100%)` }} className="px-6 py-5 text-white flex items-center justify-between flex-shrink-0">
              <div className="flex items-center gap-4 min-w-0">
                <div className="w-14 h-14 rounded-2xl bg-white/20 backdrop-blur flex items-center justify-center font-bold text-lg flex-shrink-0 shadow-lg">
                  {(selected.nombres?.[0] || '') + (selected.apellidos?.[0] || '')}
                </div>
                <div className="min-w-0">
                  <h3 className="font-bold text-lg truncate">{selected.apellidos} {selected.nombres}</h3>
                  <div className="flex items-center gap-2 text-xs text-white/80 mt-0.5 flex-wrap">
                    <span className="font-semibold bg-white/15 px-2 py-0.5 rounded-md">{selected.parentesco || 'Representante'}</span>
                    <span>·</span>
                    <span>Cédula {selected.cedula || '—'}</span>
                  </div>
                </div>
              </div>
              <button onClick={() => setModal(null)} className="text-white/70 hover:text-white text-xl flex-shrink-0 ml-3 w-8 h-8 rounded-lg hover:bg-white/10 transition">✕</button>
            </div>

            <div className="p-6 overflow-y-auto space-y-4 flex-1">
              <Card title="Datos personales">
                <div className="grid grid-cols-2 gap-4">
                  <Field label="Cédula" value={selected.cedula} mono />
                  <Field label="Parentesco" value={selected.parentesco} />
                </div>
              </Card>

              <Card title="Contacto">
                <div className="grid grid-cols-2 gap-4">
                  <Field label="Teléfono principal" value={selected.telefono_principal} mono />
                  <Field label="Teléfono alterno" value={selected.telefono_alt} mono />
                  <Field label="Correo" value={selected.correo} truncate full />
                  <Field label="Dirección" value={selected.direccion} full />
                </div>
              </Card>

              <Card title={`Estudiantes a cargo${estudiantesRep.length ? ` (${estudiantesRep.length})` : ''}`}>
                {loadingEstudiantes ? (
                  <p className="text-xs text-slate-400">Cargando...</p>
                ) : estudiantesRep.length === 0 ? (
                  <p className="text-xs text-slate-400">No tiene estudiantes registrados a su cargo.</p>
                ) : (
                  <div className="space-y-2">
                    {estudiantesRep.map(est => (
                      <div key={est.id_estudiante}
                        className="flex items-center gap-3 px-3 py-2 rounded-lg bg-slate-50 border border-slate-100">
                        <div className="w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0"
                          style={{ backgroundColor: '#e8edf7', color: PRIMARY }}>
                          {(est.nombres?.[0] || '') + (est.apellidos?.[0] || '')}
                        </div>
                        <div className="min-w-0 flex-1">
                          <p className="text-sm font-medium text-slate-700 truncate">{est.apellidos} {est.nombres}</p>
                          <p className="text-[11px] text-slate-400 font-mono">{est.codigo_estudiante || est.cedula || '—'}</p>
                        </div>
                        <span className={`text-[10px] font-semibold px-2 py-0.5 rounded-md flex-shrink-0 ${
                          est.estado === 'ACTIVO' ? 'bg-green-50 text-green-600' : 'bg-slate-100 text-slate-500'
                        }`}>
                          {est.estado || '—'}
                        </span>
                      </div>
                    ))}
                  </div>
                )}
              </Card>
            </div>

            <div className="px-6 py-4 border-t border-slate-200 flex justify-end bg-white flex-shrink-0">
              <button onClick={() => setModal(null)} style={{ backgroundColor: PRIMARY }}
                className="px-6 py-2 rounded-lg text-sm text-white font-semibold hover:opacity-90 transition shadow-sm">
                Cerrar
              </button>
            </div>
          </div>
        </div>
      )}
    </Layout>
  );
}
