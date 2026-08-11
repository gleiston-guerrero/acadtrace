import { useState, useEffect, useCallback } from 'react';
import Layout from '../components/Layout';
import api from '../utils/api';

const TIPO_COLOR = {
  FERIADO: 'bg-red-100 text-red-700',
  PERIODO_EVALUACION: 'bg-amber-100 text-amber-700',
  ACTIVIDAD: 'bg-blue-100 text-blue-700',
  ANO_LECTIVO: 'bg-slate-100 text-slate-600',
  REUNION: 'bg-purple-100 text-purple-700',
  EVALUACION: 'bg-amber-100 text-amber-700',
  CIVICO: 'bg-teal-100 text-teal-700',
};

const TIPO_LABEL = {
  FERIADO: 'Feriado',
  PERIODO_EVALUACION: 'Periodo de evaluación',
  ACTIVIDAD: 'Actividad / entrega',
  ANO_LECTIVO: 'Año lectivo',
  REUNION: 'Reunión',
  EVALUACION: 'Evaluación',
  CIVICO: 'Cívico',
};

const DIAS = ['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'];

function hoyYYYYMM() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
}

export default function Calendario() {
  const [mes, setMes] = useState(hoyYYYYMM());
  const [eventos, setEventos] = useState([]);
  const [loading, setLoading] = useState(false);

  const cargar = useCallback(() => {
    setLoading(true);
    api.get('/calendario', { params: { mes } })
      .then(r => setEventos(r.data || []))
      .catch(() => setEventos([]))
      .finally(() => setLoading(false));
  }, [mes]);

  useEffect(() => { cargar(); }, [cargar]);

  const [anio, mesNum] = mes.split('-').map(Number);
  const diasEnMes = new Date(anio, mesNum, 0).getDate();
  const offset = new Date(anio, mesNum - 1, 1).getDay();

  const eventosPorDia = {};
  eventos.forEach(e => {
    const f = String(e.fecha_inicio || '').slice(0, 10);
    if (!f) return;
    (eventosPorDia[f] = eventosPorDia[f] || []).push(e);
  });

  const celdas = [];
  for (let i = 0; i < offset; i++) celdas.push(null);
  for (let d = 1; d <= diasEnMes; d++) celdas.push(d);

  return (
    <Layout breadcrumb={['Inicio', 'Calendario']}>
      <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
        <div>
          <h1 className="text-base font-bold text-slate-700">Calendario Académico</h1>
          <p className="text-xs text-slate-400">Eventos, periodos de evaluación, actividades y feriados</p>
        </div>
        <input
          type="month" value={mes} onChange={e => setMes(e.target.value)}
          className="px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white"
        />
      </div>

      <div className="flex flex-wrap gap-2 mb-4">
        {Object.entries(TIPO_LABEL).map(([tipo, label]) => (
          <span key={tipo} className={`text-[10px] px-2 py-1 rounded-full font-medium ${TIPO_COLOR[tipo]}`}>{label}</span>
        ))}
      </div>

      <div className="bg-white border border-slate-200 rounded-xl overflow-hidden">
        <div className="grid grid-cols-7 bg-slate-50 border-b border-slate-200">
          {DIAS.map(d => (
            <div key={d} className="px-2 py-2 text-center text-xs font-semibold text-slate-500">{d}</div>
          ))}
        </div>
        <div className="grid grid-cols-7">
          {celdas.map((d, i) => {
            const fecha = d ? `${mes}-${String(d).padStart(2, '0')}` : null;
            const delDia = fecha ? (eventosPorDia[fecha] || []) : [];
            return (
              <div key={i} className={`min-h-[90px] border-b border-r border-slate-100 p-1.5 ${d ? '' : 'bg-slate-50/50'}`}>
                {d && <p className="text-xs font-semibold text-slate-600 mb-1">{d}</p>}
                <div className="space-y-1">
                  {delDia.slice(0, 3).map((e, idx) => (
                    <p
                      key={idx}
                      title={e.titulo}
                      className={`text-[10px] px-1.5 py-0.5 rounded truncate ${TIPO_COLOR[e.tipo] || 'bg-slate-100 text-slate-600'}`}
                    >
                      {e.titulo}
                    </p>
                  ))}
                  {delDia.length > 3 && <p className="text-[10px] text-slate-400">+{delDia.length - 3} más</p>}
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {loading && <p className="text-xs text-slate-400 mt-2">Cargando...</p>}
      {!loading && eventos.length === 0 && <p className="text-xs text-slate-400 mt-2">Sin eventos este mes.</p>}
    </Layout>
  );
}
