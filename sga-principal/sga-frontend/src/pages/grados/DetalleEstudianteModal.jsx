import React, { useState } from "react";

const PRIMARY = "#243A76";
const modalBg = { backgroundColor: "rgba(36, 58, 118, 0.55)" };

function Field({ label, value, mono, truncate, full }) {
  return (
    <div className={`min-w-0 ${full ? "col-span-2 md:col-span-3" : ""}`}>
      <p className="text-[10px] font-semibold text-slate-400 uppercase tracking-wider mb-1">{label}</p>
      <p className={`text-sm text-slate-700 ${mono ? "font-mono" : ""} ${truncate ? "truncate" : ""}`}>{value || "—"}</p>
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

export default function DetalleEstudianteModal({ detalleEst, onClose }) {
  const [tab, setTab] = useState("estudiante");
  const rep = detalleEst.representanteDetalle;
  const iniciales = `${detalleEst.nombres?.[0] || ""}${detalleEst.apellidos?.[0] || ""}`.toUpperCase();
  const fechaNac = detalleEst.fechaNacimiento ? new Date(detalleEst.fechaNacimiento).toLocaleDateString("es-EC") : null;

  const TABS = [
    { id: "estudiante",    label: "Estudiante",       icon: "M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" },
    { id: "familiares",    label: "Datos familiares", icon: "M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z" },
    { id: "representante", label: "Representante",    icon: "M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" },
  ];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4" style={modalBg} onClick={onClose}>
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
            const active = tab === t.id;
            return (
              <button
                key={t.id}
                type="button"
                onClick={() => setTab(t.id)}
                className={`flex items-center gap-2 px-4 py-3 text-sm font-medium border-b-2 -mb-px transition ${active ? "border-current" : "border-transparent text-slate-400 hover:text-slate-600"}`}
                style={active ? { color: PRIMARY, borderColor: PRIMARY } : {}}
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
          {tab === "estudiante" && (
            <>
              <Card title="Identificación">
                <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                  <Field label="Cédula" value={detalleEst.cedula} mono />
                  <Field label="Código estudiante" value={detalleEst.codigoEstudiante} mono />
                  <Field label="Género" value={detalleEst.genero} />
                  <Field label="Fecha de nacimiento" value={fechaNac} />
                  <Field label="Lugar de nacimiento" value={detalleEst.lugarNacimiento} />
                  <Field label="Nacionalidad" value={detalleEst.nacionalidad} />
                </div>
              </Card>

              <Card title="Contacto">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <Field label="Teléfono" value={detalleEst.telefono} mono />
                  <Field label="Teléfono alterno" value={detalleEst.telefonoAlt} mono />
                  <Field label="Correo" value={detalleEst.correo} truncate full />
                  <Field label="Dirección" value={detalleEst.direccion} full />
                </div>
              </Card>

              <Card title="Matrícula">
                <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                  <Field label="Estado" value={detalleEst.estado} />
                  <Field label="Número de orden" value={detalleEst.numeroOrden} />
                  <Field label="ID Matrícula" value={detalleEst.idMatricula} mono />
                </div>
              </Card>
            </>
          )}

          {tab === "familiares" && (
            <>
              <Card title="Entorno familiar">
                <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                  <Field label="Etnia" value={detalleEst.etnia} />
                  <Field label="Vive con" value={detalleEst.viveCon} />
                  <Field label="N.º hermanos" value={detalleEst.numerosHermanos} />
                  <Field label="Beneficio social" value={detalleEst.beneficioSocial ? "Sí" : "No"} />
                </div>
              </Card>

              <Card title="Discapacidad">
                {detalleEst.discapacidad ? (
                  <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                    <Field label="Tipo" value={detalleEst.tipoDiscapacidad} />
                    <Field label="Porcentaje" value={detalleEst.porcentajeDisc != null ? `${detalleEst.porcentajeDisc}%` : null} />
                    <Field label="Carnet CONADIS" value={detalleEst.carnetConadis} mono />
                  </div>
                ) : (
                  <p className="text-sm text-slate-400 text-center py-2">Sin discapacidad registrada.</p>
                )}
              </Card>
            </>
          )}

          {tab === "representante" && (
            rep ? (
              <>
                <Card title="Datos personales">
                  <div className="flex items-center gap-4 mb-5 pb-5 border-b border-slate-100">
                    <div className="w-14 h-14 rounded-full flex items-center justify-center text-white font-bold flex-shrink-0 text-lg" style={{ backgroundColor: PRIMARY }}>
                      {(rep.nombres?.[0] || "") + (rep.apellidos?.[0] || "")}
                    </div>
                    <div className="min-w-0">
                      <p className="font-bold text-slate-700 text-base truncate">{rep.nombres} {rep.apellidos}</p>
                      <p className="text-xs text-slate-500 mt-0.5">
                        {rep.parentesco || "Sin parentesco"} · Cédula {rep.cedula || "—"}
                        {rep.conviveConEstudiante && (
                          <span className="ml-2 inline-flex items-center gap-1 text-emerald-700 bg-emerald-50 border border-emerald-200 px-1.5 py-0.5 rounded text-[10px] font-semibold">
                            ✓ Convive con el estudiante
                          </span>
                        )}
                      </p>
                    </div>
                  </div>
                  <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                    <Field label="Cédula"             value={rep.cedula} mono />
                    <Field label="Fecha nacimiento"   value={rep.fechaNacimiento} />
                    <Field label="Género"             value={rep.genero} />
                    <Field label="Estado civil"       value={rep.estadoCivil} />
                    <Field label="Nacionalidad"       value={rep.nacionalidad} />
                    <Field label="Nivel instrucción"  value={rep.nivelInstruccion} />
                  </div>
                </Card>

                <Card title="Contacto">
                  <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                    <Field label="Teléfono principal" value={rep.telefonoPrincipal} mono />
                    <Field label="Teléfono alterno"   value={rep.telefonoAlt} mono />
                    <Field label="Correo"             value={rep.correo} truncate />
                    <Field label="Dirección"          value={rep.direccion} full />
                  </div>
                </Card>

                <Card title="Datos laborales">
                  <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                    <Field label="Ocupación"          value={rep.ocupacion} />
                    <Field label="Cargo"              value={rep.cargo} />
                    <Field label="Lugar de trabajo"   value={rep.lugarTrabajo} truncate />
                    <Field label="Teléfono trabajo"   value={rep.telefonoTrabajo} mono />
                    <Field label="Ingreso mensual"    value={rep.ingresoMensual ? `$ ${Number(rep.ingresoMensual).toFixed(2)}` : null} />
                  </div>
                </Card>

                <Card title="Contacto de emergencia">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <Field label="Nombre"   value={rep.contactoEmergenciaNombre} />
                    <Field label="Teléfono" value={rep.contactoEmergenciaTelefono} mono />
                  </div>
                </Card>

                {rep.observaciones && (
                  <Card title="Observaciones">
                    <p className="text-sm text-slate-700 leading-relaxed">{rep.observaciones}</p>
                  </Card>
                )}
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
