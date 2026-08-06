import React from "react";

const PRIMARY = "#243A76";
const modalBg = { backgroundColor: "rgba(36, 58, 118, 0.55)" };

function InfoField({ label, value, mono, truncate }) {
  return (
    <div className="min-w-0">
      <p className="text-[10px] font-semibold text-slate-400 uppercase tracking-wider mb-1">{label}</p>
      <p className={`text-sm text-slate-700 ${mono ? "font-mono" : ""} ${truncate ? "truncate" : ""}`}>{value || "—"}</p>
    </div>
  );
}

function SectionCard({ title, badge, children }) {
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

export default function DetalleEstudianteModal({ detalleEst, detalleTab, setDetalleTab, detalleNotas, detalleAsist, cargandoDetalle, onClose }) {
  const rep = detalleEst.representanteDetalle;
  const iniciales = `${detalleEst.nombres?.[0] || ""}${detalleEst.apellidos?.[0] || ""}`.toUpperCase();

  const TABS = [
    { id: "estudiante",    label: "Estudiante",    icon: "M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" },
    { id: "representante", label: "Representante", icon: "M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" },
    { id: "academico",     label: "Académico",     icon: "M12 14l9-5-9-5-9 5 9 5zm0 0l6.16-3.422a12.083 12.083 0 01.665 6.479A11.952 11.952 0 0012 20.055a11.952 11.952 0 00-6.824-2.998 12.078 12.078 0 01.665-6.479L12 14z" },
  ];

  const totalAsist = detalleAsist.reduce((a, x) => ({
    p: a.p + (x.total_presentes || 0), au: a.au + (x.total_ausentes || 0),
    j: a.j + (x.total_justificados || 0), t: a.t + (x.total_atrasos || 0),
  }), { p: 0, au: 0, j: 0, t: 0 });
  const totalDias = totalAsist.p + totalAsist.au + totalAsist.j + totalAsist.t;
  const pctAsist = totalDias > 0 ? Math.round((totalAsist.p / totalDias) * 100) : 0;

  const promedioNota = detalleNotas.length > 0
    ? (detalleNotas.reduce((s, n) => s + Number(n.nota || 0), 0) / detalleNotas.length).toFixed(2)
    : null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center px-4 py-6" style={modalBg} onClick={onClose}>
      <div className="bg-slate-50 rounded-2xl shadow-2xl w-full max-w-3xl overflow-hidden flex flex-col max-h-[90vh]" onClick={e => e.stopPropagation()}>
        <div style={{ background: `linear-gradient(135deg, ${PRIMARY} 0%, #3d5a9e 100%)` }} className="px-6 py-5 text-white flex items-center justify-between flex-shrink-0">
          <div className="flex items-center gap-4 min-w-0">
            <div className="w-14 h-14 rounded-2xl bg-white/20 backdrop-blur flex items-center justify-center font-bold text-lg flex-shrink-0 shadow-lg">
              {iniciales}
            </div>
            <div className="min-w-0">
              <h3 className="font-bold text-lg truncate">{detalleEst.apellidos} {detalleEst.nombres}</h3>
              <div className="flex items-center gap-2 text-xs text-white/80 mt-0.5 flex-wrap">
                <span className="font-mono bg-white/15 px-2 py-0.5 rounded">{detalleEst.codigoEstudiante || "sin código"}</span>
                <span>·</span>
                <span>Cédula {detalleEst.cedula || "—"}</span>
                <span>·</span>
                <span className={`px-2 py-0.5 rounded-md font-semibold ${detalleEst.estado === "ACTIVA" ? "bg-emerald-400/90 text-emerald-950" : "bg-slate-300/90 text-slate-700"}`}>
                  {detalleEst.estado}
                </span>
              </div>
            </div>
          </div>
          <button onClick={onClose} className="text-white/70 hover:text-white text-xl flex-shrink-0 ml-3 w-8 h-8 rounded-lg hover:bg-white/10 transition">✕</button>
        </div>

        <div className="flex border-b border-slate-200 px-6 bg-white flex-shrink-0">
          {TABS.map(t => {
            const active = detalleTab === t.id;
            return (
              <button
                key={t.id}
                type="button"
                onClick={() => setDetalleTab(t.id)}
                className={`flex items-center gap-2 px-4 py-3 text-sm font-medium border-b-2 -mb-px transition ${active ? "border-current" : "border-transparent text-slate-400 hover:text-slate-600"}`}
                style={active ? { color: PRIMARY, borderColor: PRIMARY } : {}}
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d={t.icon} />
                </svg>
                {t.label}
                {t.id === "representante" && rep && (
                  <span className="text-[9px] bg-emerald-100 text-emerald-700 px-1.5 py-0.5 rounded font-mono">gRPC</span>
                )}
              </button>
            );
          })}
        </div>

        <div className="p-6 overflow-y-auto space-y-4 flex-1">
          {detalleTab === "estudiante" && (
            <>
              <SectionCard title="Identificación">
                <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                  <InfoField label="Cédula" value={detalleEst.cedula} mono />
                  <InfoField label="Código estudiante" value={detalleEst.codigoEstudiante} mono />
                  <InfoField label="Género" value={detalleEst.genero} />
                </div>
              </SectionCard>

              <SectionCard title="Contacto">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <InfoField label="Teléfono" value={detalleEst.telefono} mono />
                  <InfoField label="Correo" value={detalleEst.correo} truncate />
                </div>
              </SectionCard>

              <SectionCard title="Matrícula">
                <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                  <InfoField label="Estado" value={detalleEst.estado} />
                  <InfoField label="Número de orden" value={detalleEst.numeroOrden} />
                  <InfoField label="ID Matrícula" value={detalleEst.idMatricula} mono />
                </div>
              </SectionCard>
            </>
          )}

          {detalleTab === "representante" && (
            rep ? (
              <>
                <SectionCard
                  title="Representante legal"
                  badge={<span className="text-[10px] bg-emerald-100 text-emerald-700 px-2 py-0.5 rounded font-mono font-bold ml-1">gRPC · sga-principal</span>}
                >
                  <div className="flex items-center gap-4 mb-5 pb-5 border-b border-slate-100">
                    <div className="w-12 h-12 rounded-full flex items-center justify-center text-white font-bold flex-shrink-0" style={{ backgroundColor: PRIMARY }}>
                      {(rep.nombres?.[0] || "") + (rep.apellidos?.[0] || "")}
                    </div>
                    <div className="min-w-0">
                      <p className="font-bold text-slate-700 text-base truncate">{rep.nombres} {rep.apellidos}</p>
                      <p className="text-xs text-slate-500">{rep.parentesco || "Sin parentesco"} · Cédula {rep.cedula || "—"}</p>
                    </div>
                  </div>
                  <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                    <InfoField label="Teléfono principal" value={rep.telefonoPrincipal} mono />
                    <InfoField label="Teléfono alterno"   value={rep.telefonoAlt} mono />
                    <InfoField label="Correo"             value={rep.correo} truncate />
                    <div className="col-span-2 md:col-span-3">
                      <InfoField label="Dirección" value={rep.direccion} />
                    </div>
                  </div>
                </SectionCard>

                <div className="text-[11px] text-slate-400 text-center">
                  <span className="inline-flex items-center gap-1.5">
                    <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 inline-block"></span>
                    Servido por <code className="text-slate-500">PrincipalService.ObtenerRepresentante</code> (gRPC :9092)
                  </span>
                </div>
              </>
            ) : (
              <div className="bg-white border border-slate-200 rounded-xl p-12 text-center">
                <svg className="w-12 h-12 mx-auto mb-3 text-slate-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
                <p className="text-sm text-slate-500 font-medium">Sin representante asignado</p>
                <p className="text-xs text-slate-400 mt-1">El estudiante aún no tiene un representante registrado.</p>
              </div>
            )
          )}

          {detalleTab === "academico" && (
            <>
              <SectionCard title="Resumen de asistencia">
                {cargandoDetalle ? (
                  <p className="text-slate-400 text-sm">Cargando...</p>
                ) : detalleAsist.length === 0 ? (
                  <p className="text-slate-400 text-sm text-center py-4">Sin registros de asistencia.</p>
                ) : (
                  <>
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-4">
                      {[
                        { l: "Presentes",   v: totalAsist.p,  color: "text-emerald-600", bg: "bg-emerald-50", border: "border-emerald-200" },
                        { l: "Ausentes",    v: totalAsist.au, color: "text-red-600",     bg: "bg-red-50",     border: "border-red-200" },
                        { l: "Atrasos",     v: totalAsist.t,  color: "text-amber-600",   bg: "bg-amber-50",   border: "border-amber-200" },
                        { l: "Justificados",v: totalAsist.j,  color: "text-blue-600",    bg: "bg-blue-50",    border: "border-blue-200" },
                      ].map(x => (
                        <div key={x.l} className={`border ${x.border} ${x.bg} rounded-xl p-3 text-center`}>
                          <div className={`text-2xl font-bold ${x.color}`}>{x.v}</div>
                          <div className="text-[10px] text-slate-500 uppercase font-semibold tracking-wide mt-0.5">{x.l}</div>
                        </div>
                      ))}
                    </div>
                    <div>
                      <div className="flex items-center justify-between text-xs mb-1.5">
                        <span className="text-slate-500 font-medium">% Asistencia</span>
                        <span className="font-bold" style={{ color: PRIMARY }}>{pctAsist}%</span>
                      </div>
                      <div className="h-2 bg-slate-100 rounded-full overflow-hidden">
                        <div className="h-full rounded-full transition-all" style={{ width: `${pctAsist}%`, backgroundColor: pctAsist >= 90 ? "#10b981" : pctAsist >= 75 ? "#f59e0b" : "#ef4444" }} />
                      </div>
                    </div>
                  </>
                )}
              </SectionCard>

              <SectionCard
                title="Calificaciones"
                badge={promedioNota && (
                  <span className="text-[10px] bg-slate-100 text-slate-600 px-2 py-0.5 rounded ml-1 font-semibold">
                    Promedio: <span style={{ color: PRIMARY }}>{promedioNota}</span>
                  </span>
                )}
              >
                {cargandoDetalle ? (
                  <p className="text-slate-400 text-sm">Cargando...</p>
                ) : detalleNotas.length === 0 ? (
                  <p className="text-slate-400 text-sm text-center py-4">Sin calificaciones registradas.</p>
                ) : (
                  <div className="border border-slate-100 rounded-lg overflow-hidden max-h-64 overflow-y-auto">
                    <table className="w-full text-sm">
                      <thead className="sticky top-0" style={{ backgroundColor: "#f8f9fc" }}>
                        <tr>
                          <th className="text-left px-3 py-2 text-xs font-semibold text-slate-500 uppercase tracking-wide">Actividad</th>
                          <th className="text-center px-3 py-2 text-xs font-semibold text-slate-500 uppercase tracking-wide">Nota</th>
                        </tr>
                      </thead>
                      <tbody>
                        {detalleNotas.map(n => {
                          const nota = Number(n.nota || 0);
                          const color = nota >= 7 ? "text-emerald-600" : nota >= 5 ? "text-amber-600" : "text-red-600";
                          return (
                            <tr key={n.id_calificacion} className="border-t border-slate-100 hover:bg-slate-50/60">
                              <td className="px-3 py-2 text-slate-600 font-mono text-xs">#{n.id_actividad}</td>
                              <td className={`px-3 py-2 text-center font-bold ${color}`}>{nota.toFixed(2)}</td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  </div>
                )}
              </SectionCard>
            </>
          )}
        </div>

        <div className="px-6 py-4 border-t border-slate-200 flex justify-end bg-white flex-shrink-0">
          <button onClick={onClose} style={{ backgroundColor: PRIMARY }}
            className="px-6 py-2 rounded-lg text-sm text-white font-semibold hover:opacity-90 transition shadow-sm">
            Cerrar
          </button>
        </div>
      </div>
    </div>
  );
}
