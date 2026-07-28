import { useState, useEffect } from 'react';
import Layout from '../components/Layout';
import api from '../utils/api';

const PRIMARY = '#243A76';

// Nuestro microservicio devuelve: id_usuario, username, correo, estado (bool),
// nombres, apellidos (del JOIN con personas), roles (array)
const EMPTY = {
  username: '', correo: '', nombres: '', apellidos: '',
  cedula: '', telefono: '', cargo: '', titulo_academico: '',
  especializacion: '', roles: [],
};

export default function Usuarios() {
  const [usuarios, setUsuarios] = useState([]);
  const [roles, setRoles] = useState([]);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');
  const [modal, setModal] = useState(null);
  const [selected, setSelected] = useState(null);
  const [form, setForm] = useState(EMPTY);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [tempPass, setTempPass] = useState('');

  const cargar = async () => {
    setLoading(true);
    try {
      const [u, r] = await Promise.all([
        api.get('/usuarios'),
        api.get('/usuarios/roles'),
      ]);
      setUsuarios(u.data || []);
      setRoles(r.data || []);
    } catch (e) {
      console.error('Error cargando usuarios:', e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { cargar(); }, []);

  const filtrados = usuarios.filter(u => {
    if (!search) return true;
    const q = search.toLowerCase();
    return (
      u.username?.toLowerCase().includes(q) ||
      u.nombres?.toLowerCase().includes(q) ||
      u.apellidos?.toLowerCase().includes(q) ||
      u.correo?.toLowerCase().includes(q)
    );
  });

  const toggleRol = (nombre) => {
    setForm(f => ({
      ...f,
      roles: f.roles.includes(nombre)
        ? f.roles.filter(r => r !== nombre)
        : [...f.roles, nombre],
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    try {
      const res = await api.post('/usuarios', form);
      setTempPass(res.data.temp_password || '');
      setModal('pass');
      cargar();
    } catch (err) {
      setError(err.response?.data?.error || err.response?.data?.detalles?.[0]?.mensaje || 'Error al crear usuario');
    } finally {
      setSaving(false);
    }
  };

  const resetPass = async (id) => {
    try {
      const res = await api.patch(`/usuarios/${id}/reset-password`);
      setTempPass(res.data.temp_password || '—');
      setModal('pass');
    } catch (e) {
      console.error('Error reseteando contraseña:', e);
    }
  };

  const cambiarEstado = async (id, estadoActual) => {
    try {
      await api.patch(`/usuarios/${id}/estado`, { estado: !estadoActual });
      cargar();
    } catch (e) {
      console.error('Error cambiando estado:', e);
    }
  };

  // nombre para mostrar - puede venir de personas (JOIN) o no existir
  const nombreCompleto = (u) => {
    if (u.nombres && u.apellidos) return `${u.apellidos}, ${u.nombres}`;
    if (u.nombres) return u.nombres;
    return u.username;
  };

  return (
    <Layout breadcrumb={['Inicio', 'Usuarios']}>
      <div className="flex items-center justify-between mb-4">
        <div>
          <h1 className="text-base font-bold text-slate-700">Usuarios y Docentes</h1>
          <p className="text-xs text-slate-400">Gestión de accesos al sistema</p>
        </div>
        <button onClick={() => { setForm(EMPTY); setError(''); setModal('crear'); }}
          style={{ backgroundColor: PRIMARY }}
          className="flex items-center gap-2 text-white text-sm px-4 py-2 rounded-lg hover:opacity-90 transition shadow-sm">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
          </svg>
          Nuevo usuario
        </button>
      </div>

      {/* Buscador */}
      <div className="bg-white border border-slate-200 rounded-xl p-3 mb-4">
        <div className="relative">
          <svg className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          <input value={search} onChange={e => setSearch(e.target.value)}
            placeholder="Buscar por nombre, usuario o correo..."
            className="w-full pl-9 pr-4 py-2 text-sm border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 bg-slate-50" />
        </div>
      </div>

      {/* Tabla */}
      <div className="bg-white border border-slate-200 rounded-xl overflow-hidden">
        <table className="w-full text-sm">
          <thead>
            <tr style={{ backgroundColor: PRIMARY }} className="text-white text-xs">
              <th className="px-4 py-3 text-left">Nombre / Usuario</th>
              <th className="px-4 py-3 text-left">Correo</th>
              <th className="px-4 py-3 text-left">Cargo</th>
              <th className="px-4 py-3 text-left">Roles</th>
              <th className="px-4 py-3 text-center">Estado</th>
              <th className="px-4 py-3 text-center">Acciones</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={6} className="text-center py-12">
                <div className="flex items-center justify-center gap-2 text-slate-400">
                  <svg className="w-5 h-5 animate-spin" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z"/>
                  </svg>
                  <span className="text-sm">Cargando...</span>
                </div>
              </td></tr>
            ) : filtrados.length === 0 ? (
              <tr><td colSpan={6} className="text-center py-12 text-slate-400 text-sm">
                {search ? `Sin resultados para "${search}"` : 'No hay usuarios registrados'}
              </td></tr>
            ) : filtrados.map((u, i) => (
              <tr key={u.id_usuario}
                className={`border-t border-slate-100 ${i % 2 === 1 ? 'bg-slate-50/40' : ''} hover:bg-blue-50/30 transition`}>
                <td className="px-4 py-2.5">
                  <p className="font-medium text-slate-700">{nombreCompleto(u)}</p>
                  <p className="text-xs text-slate-400 font-mono">@{u.username}</p>
                </td>
                <td className="px-4 py-2.5 text-slate-500 text-xs">{u.correo}</td>
                <td className="px-4 py-2.5 text-slate-500 text-xs">{u.cargo || '—'}</td>
                <td className="px-4 py-2.5">
                  <div className="flex flex-wrap gap-1">
                    {(u.roles || []).length === 0
                      ? <span className="text-xs text-slate-300">Sin rol</span>
                      : (u.roles || []).map(r => (
                        <span key={r} className="px-1.5 py-0.5 bg-blue-50 text-blue-700 rounded text-xs">
                          {r.replace('ROLE_', '')}
                        </span>
                      ))
                    }
                  </div>
                </td>
                <td className="px-4 py-2.5 text-center">
                  <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${u.estado ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-600'}`}>
                    {u.estado ? 'Activo' : 'Inactivo'}
                  </span>
                </td>
                <td className="px-4 py-2.5 text-center">
                  <div className="flex items-center justify-center gap-1">
                    <button onClick={() => { setSelected(u); setModal('ver'); }} title="Ver detalle"
                      className="p-1.5 rounded-lg text-slate-400 hover:text-blue-600 hover:bg-blue-50 transition">
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                      </svg>
                    </button>
                    <button onClick={() => resetPass(u.id_usuario)} title="Resetear contraseña"
                      className="p-1.5 rounded-lg text-slate-400 hover:text-amber-600 hover:bg-amber-50 transition">
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z" />
                      </svg>
                    </button>
                    <button onClick={() => cambiarEstado(u.id_usuario, u.estado)}
                      title={u.estado ? 'Desactivar' : 'Activar'}
                      className={`p-1.5 rounded-lg transition ${u.estado ? 'text-slate-400 hover:text-red-500 hover:bg-red-50' : 'text-slate-400 hover:text-green-600 hover:bg-green-50'}`}>
                      {u.estado
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
        <div className="border-t border-slate-100 px-4 py-2.5 text-xs text-slate-400">
          {filtrados.length} de {usuarios.length} usuarios
        </div>
      </div>

      {/* MODAL CREAR */}
      {modal === 'crear' && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg max-h-[90vh] overflow-y-auto">
            <div style={{ backgroundColor: PRIMARY }} className="px-6 py-4 flex items-center justify-between rounded-t-2xl sticky top-0">
              <h2 className="text-white font-semibold text-sm">Nuevo Usuario</h2>
              <button onClick={() => setModal(null)} className="text-white/70 hover:text-white">
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </div>
            <form onSubmit={handleSubmit} className="p-6 space-y-4">
              <div className="grid grid-cols-2 gap-3">
                {[
                  { label: 'Nombres *', name: 'nombres', required: true },
                  { label: 'Apellidos *', name: 'apellidos', required: true },
                  { label: 'Username *', name: 'username', required: true },
                  { label: 'Correo *', name: 'correo', type: 'email', required: true },
                  { label: 'Cédula', name: 'cedula' },
                  { label: 'Teléfono', name: 'telefono' },
                  { label: 'Cargo', name: 'cargo' },
                  { label: 'Título académico', name: 'titulo_academico' },
                ].map(f => (
                  <div key={f.name}>
                    <label className="block text-xs font-semibold text-slate-500 mb-1">{f.label}</label>
                    <input type={f.type || 'text'} required={f.required}
                      value={form[f.name]} onChange={e => setForm({ ...form, [f.name]: e.target.value })}
                      className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-slate-50" />
                  </div>
                ))}
              </div>
              {roles.length > 0 && (
                <div>
                  <label className="block text-xs font-semibold text-slate-500 mb-2">Roles</label>
                  <div className="flex flex-wrap gap-2">
                    {roles.map(r => (
                      <label key={r.id_rol} className="flex items-center gap-1.5 cursor-pointer bg-slate-50 border border-slate-200 rounded-lg px-3 py-1.5 hover:bg-blue-50 hover:border-blue-200 transition">
                        <input type="checkbox" checked={form.roles.includes(r.nombre)}
                          onChange={() => toggleRol(r.nombre)} className="rounded" />
                        <span className="text-xs text-slate-600">{r.nombre.replace('ROLE_', '')}</span>
                      </label>
                    ))}
                  </div>
                </div>
              )}
              {error && <div className="bg-red-50 border border-red-200 text-red-600 text-xs px-3 py-2 rounded-lg">{error}</div>}
              <p className="text-xs text-slate-400">La contraseña temporal se genera automáticamente y se mostrará al crear.</p>
              <div className="flex gap-3 pt-2">
                <button type="button" onClick={() => setModal(null)}
                  className="flex-1 py-2.5 border border-slate-200 rounded-lg text-sm text-slate-600 hover:bg-slate-50 transition">Cancelar</button>
                <button type="submit" disabled={saving} style={{ backgroundColor: PRIMARY }}
                  className="flex-1 py-2.5 rounded-lg text-sm text-white font-semibold hover:opacity-90 transition disabled:opacity-60">
                  {saving ? 'Creando...' : 'Crear usuario'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL CONTRASEÑA TEMPORAL */}
      {modal === 'pass' && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-sm text-center p-8">
            <div className="w-14 h-14 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <svg className="w-7 h-7 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <h3 className="font-bold text-slate-700 mb-1">Contraseña temporal</h3>
            <p className="text-xs text-slate-500 mb-4">Entrégala al usuario para su primer acceso:</p>
            <div className="bg-slate-100 rounded-xl px-5 py-4 font-mono text-xl font-bold text-slate-800 tracking-widest mb-4 select-all">
              {tempPass}
            </div>
            <p className="text-xs text-slate-400 mb-5">El usuario deberá cambiarla al ingresar por primera vez.</p>
            <button onClick={() => setModal(null)} style={{ backgroundColor: PRIMARY }}
              className="w-full py-2.5 rounded-lg text-sm text-white font-semibold hover:opacity-90 transition">
              Entendido
            </button>
          </div>
        </div>
      )}

      {/* MODAL VER */}
      {modal === 'ver' && selected && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md">
            <div style={{ backgroundColor: PRIMARY }} className="px-6 py-4 flex items-center justify-between rounded-t-2xl">
              <h2 className="text-white font-semibold text-sm">Detalle de usuario</h2>
              <button onClick={() => setModal(null)} className="text-white/70 hover:text-white">
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </div>
            <div className="p-6 space-y-3">
              <div className="text-center mb-4">
                <div className="w-14 h-14 rounded-full flex items-center justify-center mx-auto mb-2" style={{ backgroundColor: '#e8edf7' }}>
                  <span style={{ color: PRIMARY }} className="text-xl font-bold">
                    {(selected.nombres || selected.username)?.charAt(0)?.toUpperCase()}
                  </span>
                </div>
                <p className="font-bold text-slate-700">{nombreCompleto(selected)}</p>
                <p className="text-xs text-slate-400 mt-0.5">@{selected.username}</p>
              </div>
              {[
                ['Correo', selected.correo],
                ['Cédula', selected.cedula],
                ['Teléfono', selected.telefono],
                ['Cargo', selected.cargo],
                ['Título académico', selected.titulo_academico],
                ['Especialización', selected.especializacion],
                ['Último acceso', selected.ultimo_acceso ? new Date(selected.ultimo_acceso).toLocaleString('es-EC') : null],
              ].filter(([, v]) => v).map(([k, v]) => (
                <div key={k} className="flex justify-between py-1.5 border-b border-slate-100 last:border-0">
                  <span className="text-slate-400 text-xs">{k}</span>
                  <span className="text-slate-700 font-medium text-xs text-right max-w-48">{v}</span>
                </div>
              ))}
              {(selected.roles || []).length > 0 && (
                <div className="pt-1">
                  <p className="text-xs text-slate-400 mb-2">Roles asignados</p>
                  <div className="flex flex-wrap gap-1">
                    {(selected.roles || []).map(r => (
                      <span key={r} className="px-2 py-0.5 bg-blue-50 text-blue-700 rounded text-xs font-medium">
                        {r.replace('ROLE_', '')}
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </Layout>
  );
}
