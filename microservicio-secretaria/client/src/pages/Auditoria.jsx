import { useState, useEffect, useCallback } from 'react';
import Layout from '../components/Layout';
import { apiPrincipal } from '../utils/api';

const PRIMARY = '#243A76';

const ACCIONES = ["", "CREAR", "EDITAR", "ELIMINAR", "LOGIN", "LOGIN_FALLIDO", "LOGOUT",
  "CAMBIO_PASSWORD", "BLOQUEO", "DESBLOQUEO", "ROL_ASIGNADO", "LLAMADA_GRPC"];
const ORIGENES = ["", "PRINCIPAL", "SECRETARIA", "DOCENTE"];

const BADGE_ORIGEN = {
  PRINCIPAL: 'bg-blue-50 text-blue-600',
  SECRETARIA: 'bg-cyan-50 text-cyan-600',
  DOCENTE: 'bg-purple-50 text-purple-600',
};

function fmtFecha(f) {
  if (!f) return '—';
  return new Date(f).toLocaleString('es-EC', { dateStyle: 'short', timeStyle: 'medium' });
}

/**
 * Solo lectura: consulta directo a sga-principal (dueño de la tabla
 * centralizada sga_principal.auditoria) via apiPrincipal, igual que ya se
 * hace para /anos-lectivos/actual. La autorizacion real (solo DIRECTOR) la
 * aplica sga-principal en su SecurityConfig — este filtro de rol es solo
 * para no mostrar el módulo a quien no lo puede usar.
 */
export default function Auditoria() {
  const [filas, setFilas] = useState([]);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filtros, setFiltros] = useState({ schemaOrigen: '', accion: '', tablaAfectada: '', resultado: '', username: '' });

  const cargar = useCallback(() => {
    setLoading(true);
    const params = { page, size: 20 };
    Object.entries(filtros).forEach(([k, v]) => { if (v) params[k] = v; });
    apiPrincipal.get('/auditoria', { params })
      .then(r => { setFilas(r.data.content); setTotalPages(r.data.totalPages); })
      .catch(() => setError('Error al cargar la auditoría'))
      .finally(() => setLoading(false));
  }, [page, filtros]);

  useEffect(() => { cargar(); }, [cargar]);

  const cambiarFiltro = (campo, valor) => {
    setPage(0);
    setFiltros(f => ({ ...f, [campo]: valor }));
  };

  return (
    <Layout breadcrumb={['Inicio', 'Auditoría']}>
      <div className="mb-4">
        <h1 className="text-base font-bold text-slate-700">Auditoría del sistema</h1>
        <p className="text-xs text-slate-400">
          Vista de solo lectura — CRUD sensible, accesos y llamadas entre microservicios registrados en sga-principal.
        </p>
      </div>

      {error && (
        <div className="mb-4 bg-red-50 border border-red-200 rounded-xl px-4 py-3 flex items-center justify-between">
          <span className="text-red-600 text-sm">{error}</span>
          <button onClick={() => setError('')} className="text-red-400 hover:text-red-600">✕</button>
        </div>
      )}

      <div className="bg-white border border-slate-200 rounded-xl p-3 mb-4 flex flex-wrap gap-2 items-center">
        <select value={filtros.schemaOrigen} onChange={e => cambiarFiltro('schemaOrigen', e.target.value)}
          className="px-3 py-1.5 text-xs border border-slate-200 rounded-lg bg-slate-50 focus:outline-none">
          {ORIGENES.map(o => <option key={o} value={o}>{o || 'Todos los servicios'}</option>)}
        </select>
        <select value={filtros.accion} onChange={e => cambiarFiltro('accion', e.target.value)}
          className="px-3 py-1.5 text-xs border border-slate-200 rounded-lg bg-slate-50 focus:outline-none">
          {ACCIONES.map(a => <option key={a} value={a}>{a || 'Todas las acciones'}</option>)}
        </select>
        <select value={filtros.resultado} onChange={e => cambiarFiltro('resultado', e.target.value)}
          className="px-3 py-1.5 text-xs border border-slate-200 rounded-lg bg-slate-50 focus:outline-none">
          <option value="">Éxito y fallo</option>
          <option value="EXITO">Solo éxito</option>
          <option value="FALLO">Solo fallo</option>
        </select>
        <input type="text" placeholder="Entidad (ej. representante)" value={filtros.tablaAfectada}
          onChange={e => cambiarFiltro('tablaAfectada', e.target.value)}
          className="px-3 py-1.5 text-xs border border-slate-200 rounded-lg bg-slate-50 focus:outline-none w-44" />
        <input type="text" placeholder="Usuario" value={filtros.username}
          onChange={e => cambiarFiltro('username', e.target.value)}
          className="px-3 py-1.5 text-xs border border-slate-200 rounded-lg bg-slate-50 focus:outline-none w-32" />
      </div>

      <div className="bg-white border border-slate-200 rounded-2xl overflow-hidden">
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
                </tr>
              </thead>
              <tbody>
                {filas.map((f, i) => (
                  <tr key={f.idAuditoria} className={`border-t border-slate-100 hover:bg-slate-50 transition ${i % 2 === 1 ? 'bg-slate-50/40' : ''}`}>
                    <td className="px-4 py-2.5 text-slate-500 text-xs whitespace-nowrap">{fmtFecha(f.fecha)}</td>
                    <td className="px-4 py-2.5">
                      <span className={`text-[10px] font-semibold px-2 py-0.5 rounded-md ${BADGE_ORIGEN[f.schemaOrigen] || 'bg-slate-100 text-slate-500'}`}>
                        {f.schemaOrigen}
                      </span>
                    </td>
                    <td className="px-4 py-2.5 text-slate-700 font-medium">{f.username || '—'}</td>
                    <td className="px-4 py-2.5 text-slate-600 font-mono text-xs">{f.accion}</td>
                    <td className="px-4 py-2.5 text-slate-500 text-xs">{f.tablaAfectada || '—'}{f.registroId ? ` #${f.registroId}` : ''}</td>
                    <td className="px-4 py-2.5 text-slate-500 text-xs max-w-xs truncate" title={f.descripcion}>{f.descripcion || '—'}</td>
                    <td className="px-4 py-2.5 text-center">
                      <span className={`text-[10px] font-semibold px-2 py-0.5 rounded-full ${f.resultado === 'EXITO' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                        {f.resultado}
                      </span>
                    </td>
                    <td className="px-4 py-2.5 text-center" title={f.hmacValido ? 'Fila intacta (HMAC valido)' : 'Posible alteracion: el HMAC no coincide'}>
                      {f.hmacValido ? <span className="text-green-500">✓</span> : <span className="text-red-500">⚠</span>}
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
    </Layout>
  );
}
