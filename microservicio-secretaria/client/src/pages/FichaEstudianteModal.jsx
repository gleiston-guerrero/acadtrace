import React, { useState } from 'react';

function Field({ label, value, mono, truncate, full }) {
  return (
    <div className={`min-w-0 ${full ? 'col-span-2 md:col-span-3' : ''}`}>
      <p className="text-[10px] font-semibold text-slate-400 uppercase tracking-wider mb-1">{label}</p>
      <p className={`text-sm text-slate-700 ${mono ? 'font-mono' : ''} ${truncate ? 'truncate' : ''}`}>{value || '—'}</p>
    </div>
  );
}

function Card({ title, badge, children }) {
  return (
    <div className="bg-white border border-slate-200 rounded-xl overflow-hidden">
      <div className="flex items-center gap-2 px-5 py-3 border-b border-slate-100 bg-slate-50/70">
        <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500">{title}</h4>
        {badge}
      </div>
      <div className="p-5">{children}</div>
    </div>
  );
}

export default function FichaEstudianteModal({ selected, principalOrigin, primary, onClose }) {
  const [tab, setTab] = useState('estudiante');

  const TABS = [
    { id: 'estudiante',    label: 'Estudiante',    icon: 'M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z' },
    { id: 'familiares',    label: 'Datos familiares', icon: 'M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z' },
    { id: 'representante', label: 'Representante', icon: 'M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z' },
  ];

  const iniciales = `${selected.nombres?.[0] || ''}${selected.apellidos?.[0] || ''}`.toUpperCase();
  const fechaNac = selected.fecha_nacimiento ? new Date(selected.fecha_nacimiento).toLocaleDateString('es-EC') : null;
  const tieneRep = !!(selected.rep_nombres || selected.id_representante);

  return (
    <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="bg-slate-50 rounded-2xl shadow-2xl w-full max-w-3xl overflow-hidden flex flex-col max-h-[90vh]" onClick={e => e.stopPropagation()}>
        <div style={{ background: `linear-gradient(135deg, ${primary} 0%, #3d5a9e 100%)` }} className="px-6 py-5 text-white flex items-center justify-between flex-shrink-0">
          <div className="flex items-center gap-4 min-w-0">
            {selected.foto_url ? (
              <img src={`${principalOrigin}${selected.foto_url}`} alt="" className="w-14 h-14 rounded-2xl object-cover shadow-lg flex-shrink-0" />
            ) : (
              <div className="w-14 h-14 rounded-2xl bg-white/20 backdrop-blur flex items-center justify-center font-bold text-lg flex-shrink-0 shadow-lg">
                {iniciales}
              </div>
            )}
            <div className="min-w-0">
              <h3 className="font-bold text-lg truncate">{selected.apellidos} {selected.nombres}</h3>
              <div className="flex items-center gap-2 text-xs text-white/80 mt-0.5 flex-wrap">
                <span className="font-mono bg-white/15 px-2 py-0.5 rounded">{selected.codigo_estudiante || 'sin código'}</span>
                <span>·</span>
                <span>Cédula {selected.cedula || '—'}</span>
                {selected.estado && (<><span>·</span>
                  <span className={`px-2 py-0.5 rounded-md font-semibold ${selected.estado === 'ACTIVO' ? 'bg-emerald-400/90 text-emerald-950' : 'bg-slate-300/90 text-slate-700'}`}>
                    {selected.estado}
                  </span>
                </>)}
              </div>
            </div>
          </div>
          <button onClick={onClose} className="text-white/70 hover:text-white text-xl flex-shrink-0 ml-3 w-8 h-8 rounded-lg hover:bg-white/10 transition">✕</button>
        </div>

        <div className="flex border-b border-slate-200 px-6 bg-white flex-shrink-0">
          {TABS.map(t => {
            const active = tab === t.id;
            return (
              <button
                key={t.id}
                type="button"
                onClick={() => setTab(t.id)}
                className={`flex items-center gap-2 px-4 py-3 text-sm font-medium border-b-2 -mb-px transition ${active ? 'border-current' : 'border-transparent text-slate-400 hover:text-slate-600'}`}
                style={active ? { color: primary, borderColor: primary } : {}}
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d={t.icon} />
                </svg>
                {t.label}
              </button>
            );
          })}
        </div>

        <div className="p-6 overflow-y-auto space-y-4 flex-1">
          {tab === 'estudiante' && (
            <>
              <Card title="Identificación">
                <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                  <Field label="Cédula" value={selected.cedula} mono />
                  <Field label="Código estudiante" value={selected.codigo_estudiante} mono />
                  <Field label="Género" value={selected.genero} />
                  <Field label="Fecha de nacimiento" value={fechaNac} />
                  <Field label="Lugar de nacimiento" value={selected.lugar_nacimiento} />
                  <Field label="Nacionalidad" value={selected.nacionalidad} />
                </div>
              </Card>

              <Card title="Contacto">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <Field label="Teléfono" value={selected.telefono} mono />
                  <Field label="Teléfono alterno" value={selected.telefono_alt} mono />
                  <Field label="Correo" value={selected.correo} truncate full />
                  <Field label="Dirección" value={selected.direccion} full />
                </div>
              </Card>
            </>
          )}

          {tab === 'familiares' && (
            <>
              <Card title="Entorno familiar">
                <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                  <Field label="Etnia" value={selected.etnia} />
                  <Field label="Vive con" value={selected.vive_con} />
                  <Field label="N.º hermanos" value={selected.numeros_hermanos} />
                  <Field label="Beneficio social" value={selected.beneficio_social ? 'Sí' : 'No'} />
                </div>
              </Card>

              <Card title="Discapacidad">
                {selected.discapacidad ? (
                  <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                    <Field label="Tipo" value={selected.tipo_discapacidad} />
                    <Field label="Porcentaje" value={selected.porcentaje_disc != null ? `${selected.porcentaje_disc}%` : null} />
                    <Field label="Carnet CONADIS" value={selected.carnet_conadis} mono />
                  </div>
                ) : (
                  <p className="text-sm text-slate-400 text-center py-2">Sin discapacidad registrada.</p>
                )}
              </Card>
            </>
          )}

          {tab === 'representante' && (
            tieneRep ? (
              <>
                <Card title="Datos personales">
                  <div className="flex items-center gap-4 mb-5 pb-5 border-b border-slate-100">
                    <div className="w-14 h-14 rounded-full flex items-center justify-center text-white font-bold flex-shrink-0 text-lg" style={{ backgroundColor: primary }}>
                      {(selected.rep_nombres?.[0] || '') + (selected.rep_apellidos?.[0] || '')}
                    </div>
                    <div className="min-w-0">
                      <p className="font-bold text-slate-700 text-base truncate">{selected.rep_nombres} {selected.rep_apellidos}</p>
                      <p className="text-xs text-slate-500 mt-0.5">
                        {selected.parentesco || 'Sin parentesco'} · Cédula {selected.rep_cedula || '—'}
                        {selected.rep_convive_con_estudiante && (
                          <span className="ml-2 inline-flex items-center gap-1 text-emerald-700 bg-emerald-50 border border-emerald-200 px-1.5 py-0.5 rounded text-[10px] font-semibold">
                            ✓ Convive con el estudiante
                          </span>
                        )}
                      </p>
                    </div>
                  </div>
                  <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                    <Field label="Cédula"            value={selected.rep_cedula} mono />
                    <Field label="Fecha nacimiento"  value={selected.rep_fecha_nacimiento} />
                    <Field label="Género"            value={selected.rep_genero} />
                    <Field label="Estado civil"      value={selected.rep_estado_civil} />
                    <Field label="Nacionalidad"      value={selected.rep_nacionalidad} />
                    <Field label="Nivel instrucción" value={selected.rep_nivel_instruccion} />
                  </div>
                </Card>

                <Card title="Contacto">
                  <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                    <Field label="Teléfono principal" value={selected.rep_telefono} mono />
                    <Field label="Teléfono alterno"   value={selected.rep_telefono_alt} mono />
                    <Field label="Correo"             value={selected.rep_correo} truncate />
                    <Field label="Dirección"          value={selected.rep_direccion} full />
                  </div>
                </Card>

                <Card title="Datos laborales">
                  <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                    <Field label="Ocupación"         value={selected.rep_ocupacion} />
                    <Field label="Cargo"             value={selected.rep_cargo} />
                    <Field label="Lugar de trabajo"  value={selected.rep_lugar_trabajo} truncate />
                    <Field label="Teléfono trabajo"  value={selected.rep_telefono_trabajo} mono />
                    <Field label="Ingreso mensual"   value={selected.rep_ingreso_mensual ? `$ ${Number(selected.rep_ingreso_mensual).toFixed(2)}` : null} />
                  </div>
                </Card>

                <Card title="Contacto de emergencia">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <Field label="Nombre"   value={selected.rep_contacto_emergencia_nombre} />
                    <Field label="Teléfono" value={selected.rep_contacto_emergencia_telefono} mono />
                  </div>
                </Card>

                {selected.rep_observaciones && (
                  <Card title="Observaciones">
                    <p className="text-sm text-slate-700 leading-relaxed">{selected.rep_observaciones}</p>
                  </Card>
                )}
              </>
            ) : (
              <div className="bg-white border border-slate-200 rounded-xl p-12 text-center">
                <svg className="w-12 h-12 mx-auto mb-3 text-slate-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
                <p className="text-sm text-slate-500 font-medium">Sin representante asignado</p>
                <p className="text-xs text-slate-400 mt-1">Edita al estudiante para agregar un representante.</p>
              </div>
            )
          )}
        </div>

        <div className="px-6 py-4 border-t border-slate-200 flex justify-end bg-white flex-shrink-0">
          <button onClick={onClose} style={{ backgroundColor: primary }}
            className="px-6 py-2 rounded-lg text-sm text-white font-semibold hover:opacity-90 transition shadow-sm">
            Cerrar
          </button>
        </div>
      </div>
    </div>
  );
}
