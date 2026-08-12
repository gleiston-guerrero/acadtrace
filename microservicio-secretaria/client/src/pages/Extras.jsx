import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Layout from '../components/Layout';
import { apiPrincipal } from '../utils/api';

const PRIMARY = '#243A76';

export function CambiarPassword() {
  const [form, setForm] = useState({ actual: '', nueva: '', confirmar: '' });
  const [loading, setLoading] = useState(false);
  const [msg, setMsg] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (form.nueva.length < 6) { setError('La nueva contraseña debe tener mínimo 6 caracteres'); return; }
    if (form.nueva !== form.confirmar) { setError('Las contraseñas no coinciden'); return; }
    setLoading(true); setError(''); setMsg('');
    try {
      // Endpoint: PATCH /api/auth/cambiar-password en sga-principal
      // Campo correcto del DTO: passwordNuevo (no passwordNueva)
      await apiPrincipal.patch('/auth/cambiar-password', {
        passwordActual: form.actual,
        passwordNuevo: form.nueva,
      });
      setMsg('¡Contraseña actualizada! Redirigiendo...');
      setForm({ actual: '', nueva: '', confirmar: '' });
      localStorage.setItem('primerIngreso', 'false');
      setTimeout(() => navigate('/dashboard'), 1500);
    } catch (err) {
      const mensaje = err.response?.data?.message || err.response?.data?.error || 'Contraseña actual incorrecta';
      setError(mensaje);
    } finally { setLoading(false); }
  };

  return (
    <Layout breadcrumb={['Inicio', 'Cambiar contraseña']}>
      <div className="max-w-md mx-auto">
        <div className="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
          <div style={{ backgroundColor: PRIMARY }} className="px-6 py-5">
            <h1 className="text-white font-bold text-sm">Cambiar contraseña</h1>
            <p className="text-white/60 text-xs mt-0.5">Mantén tu cuenta segura</p>
          </div>
          <form onSubmit={handleSubmit} className="p-6 space-y-4">
            {['actual', 'nueva', 'confirmar'].map((k, i) => (
              <div key={k}>
                <label className="block text-xs font-semibold text-slate-500 mb-1">
                  {['Contraseña actual', 'Nueva contraseña', 'Confirmar nueva contraseña'][i]}
                </label>
                <input type="password" required value={form[k]} onChange={e => setForm({ ...form, [k]: e.target.value })}
                  className="w-full px-3 py-2.5 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-slate-50" />
              </div>
            ))}
            {error && <div className="bg-red-50 border border-red-200 rounded-lg px-3 py-2 text-red-600 text-xs">{error}</div>}
            {msg && <div className="bg-green-50 border border-green-200 rounded-lg px-3 py-2 text-green-600 text-xs">{msg}</div>}
            <button type="submit" disabled={loading} style={{ backgroundColor: PRIMARY }}
              className="w-full py-2.5 rounded-lg text-sm text-white font-semibold hover:opacity-90 transition disabled:opacity-60">
              {loading ? 'Actualizando...' : 'Actualizar contraseña'}
            </button>
          </form>
        </div>
      </div>
    </Layout>
  );
}
