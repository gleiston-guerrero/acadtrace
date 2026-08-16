import { useEffect, useMemo, useState } from "react";
import api from "../../config/axios";
import Layout from "../../components/Layout";

const DIAS = [
  { num: 1, label: "Lunes" },
  { num: 2, label: "Martes" },
  { num: 3, label: "Miércoles" },
  { num: 4, label: "Jueves" },
  { num: 5, label: "Viernes" },
];
const PRIMARY = "#243A76";

// Paleta ciclica para diferenciar materias
const PALETA = [
  { bg: "bg-blue-50",    border: "border-blue-500",    text: "text-blue-700" },
  { bg: "bg-emerald-50", border: "border-emerald-500", text: "text-emerald-700" },
  { bg: "bg-amber-50",   border: "border-amber-500",   text: "text-amber-700" },
  { bg: "bg-rose-50",    border: "border-rose-500",    text: "text-rose-700" },
  { bg: "bg-violet-50",  border: "border-violet-500",  text: "text-violet-700" },
  { bg: "bg-cyan-50",    border: "border-cyan-500",    text: "text-cyan-700" },
  { bg: "bg-lime-50",    border: "border-lime-500",    text: "text-lime-700" },
  { bg: "bg-orange-50",  border: "border-orange-500",  text: "text-orange-700" },
];
const colorMateria = (id) => PALETA[(Number(id) || 0) % PALETA.length];

export default function Horarios() {
  const [seccion, setSeccion] = useState("asignar");
  const [periodos, setPeriodos] = useState([]);
  const [asignaciones, setAsignaciones] = useState([]);
  const [grados, setGrados] = useState([]);
  const [cursoSel, setCursoSel] = useState("");            // key: "grado-paralelo-ano"
  const [asignSel, setAsignSel] = useState("");           // materia seleccionada en "Asignar"
  const [docenteSel, setDocenteSel] = useState("");
  const [grillaCurso, setGrillaCurso] = useState({ slots: [] });
  const [grillaDoc, setGrillaDoc] = useState({ slots: [], totalHoras: 0 });
  const [msg, setMsg] = useState({ tipo: "", texto: "" });

  const menuItems = [
    { id: "asignar", label: "Asignar horas" },
    { id: "curso",   label: "Ver por curso" },
    { id: "docente", label: "Ver por docente" },
  ];

  // Constantes de negocio (según reglas de la Escuela Provincias Unidas):
  //   TOPE_DOCENTE = 25h presenciales normales, con margen de hasta 30h
  //   permitido cuando falta docente en otros centros. Se avisa pero no se bloquea.
  const TOPE_DOCENTE = 25;
  const TOPE_DOCENTE_MAX = 30;
  const TOPE_CURSO = 30;

  useEffect(() => {
    api.get("/api/horarios/periodos").then((r) => setPeriodos(r.data || []));
    api.get("/api/asignaciones").then((r) => setAsignaciones(r.data || [])).catch(() => {});
    api.get("/api/grados/activos").then((r) => setGrados(r.data || [])).catch(() => {});
  }, []);

  // Cursos = TODOS los (grado × paralelo activo), aunque no tengan asignaciones.
  // Se cruza con las asignaciones para saber qué materias tiene cada uno.
  const cursos = useMemo(() => {
    const asigsPorCurso = new Map();
    (asignaciones || []).forEach((a) => {
      if (!a.idGrado || !a.idParalelo) return;
      const key = `${a.idGrado}-${a.idParalelo}`;
      if (!asigsPorCurso.has(key)) asigsPorCurso.set(key, []);
      asigsPorCurso.get(key).push(a);
    });
    const arr = [];
    (grados || []).forEach((g) => {
      (g.paralelos || []).filter((p) => p.activo !== false).forEach((p) => {
        const key = `${g.idGrado}-${p.idParalelo}`;
        const materias = asigsPorCurso.get(key) || [];
        arr.push({
          key,
          idGrado: g.idGrado,
          idParalelo: p.idParalelo,
          grado: g.nombre,
          paralelo: p.letra,
          nivel: g.nivelEducativo || "",
          orden: g.orden || 0,
          anoLectivo: materias[0]?.anoLectivo || "",
          idAsignacionRef: materias[0]?.idAsignacion || null,
          materias,
        });
      });
    });
    return arr.sort((a, b) => (a.orden - b.orden) || a.paralelo.localeCompare(b.paralelo));
  }, [grados, asignaciones]);

  const docentes = useMemo(() => {
    const map = new Map();
    (asignaciones || []).forEach((a) => {
      if (!a.idDocente) return;
      map.set(a.idDocente, a.docente || `Docente ${a.idDocente}`);
    });
    return [...map.entries()].map(([id, nombre]) => ({ id, nombre }))
      .sort((a, b) => a.nombre.localeCompare(b.nombre));
  }, [asignaciones]);

  const cursoActivo = useMemo(() => cursos.find((c) => c.key === cursoSel), [cursos, cursoSel]);

  const cargarGrillaCurso = () => {
    if (!cursoActivo || !cursoActivo.idAsignacionRef) { setGrillaCurso({ slots: [] }); return; }
    api.get(`/api/horarios/curso/${cursoActivo.idAsignacionRef}/grilla`)
      .then((r) => setGrillaCurso(r.data || { slots: [] }))
      .catch(() => setGrillaCurso({ slots: [] }));
  };
  const cargarGrillaDocente = (id) => {
    api.get(`/api/horarios/docente/${id}/grilla`)
      .then((r) => setGrillaDoc(r.data || { slots: [], totalHoras: 0 }))
      .catch(() => setGrillaDoc({ slots: [], totalHoras: 0 }));
  };

  useEffect(() => { if (cursoActivo) cargarGrillaCurso(); }, [cursoSel]);
  useEffect(() => { if (docenteSel) cargarGrillaDocente(docenteSel); }, [docenteSel]);

  // Al cambiar curso, resetear materia seleccionada
  useEffect(() => { setAsignSel(""); }, [cursoSel]);

  const slotsMap = useMemo(() => {
    const m = {};
    (grillaCurso.slots || []).forEach((s) => { m[`${s.idPeriodo}-${s.diaSemana}`] = s; });
    return m;
  }, [grillaCurso]);
  const slotsDocMap = useMemo(() => {
    const m = {};
    (grillaDoc.slots || []).forEach((s) => { m[`${s.idPeriodo}-${s.diaSemana}`] = s; });
    return m;
  }, [grillaDoc]);

  // Contador de horas por materia dentro del curso activo
  const conteoMaterias = useMemo(() => {
    if (!cursoActivo) return {};
    const conteo = {};
    (grillaCurso.slots || []).forEach((s) => {
      conteo[s.idAsignacion] = (conteo[s.idAsignacion] || 0) + 1;
    });
    return conteo;
  }, [grillaCurso, cursoActivo]);

  const totalHorasCurso = (grillaCurso.slots || []).length;
  const totalHorasEsperadas = (cursoActivo?.materias || []).reduce((s, m) => s + (m.horasSemanales || 4), 0);

  // Horas totales asignadas por docente en TODO el sistema (para la alerta suave
  // cuando pase de 25). Se calcula por lote consultando cada docente una sola vez.
  const [horasPorDocente, setHorasPorDocente] = useState({});
  useEffect(() => {
    if (docentes.length === 0) return;
    Promise.all(docentes.map((d) =>
      api.get(`/api/horarios/docente/${d.id}/grilla`)
        .then((r) => [d.id, r.data?.totalHoras || 0])
        .catch(() => [d.id, 0])
    )).then((pares) => setHorasPorDocente(Object.fromEntries(pares)));
  }, [docentes, grillaCurso]); // recalcula tras guardar/eliminar

  const asignarSlot = (dia, idPeriodo) => {
    if (!asignSel) { setMsg({ tipo: "error", texto: "Selecciona una materia primero." }); return; }
    setMsg({ tipo: "", texto: "" });
    api.post("/api/horarios", { idAsignacion: Number(asignSel), dia, idPeriodo })
      .then(() => { setMsg({ tipo: "ok", texto: "Hora asignada." }); cargarGrillaCurso(); })
      .catch((err) => setMsg({ tipo: "error", texto: err.response?.data?.mensaje || "No se pudo asignar." }));
  };
  const eliminarSlot = (idHorario) => {
    if (!window.confirm("¿Eliminar este slot?")) return;
    api.delete(`/api/horarios/${idHorario}`)
      .then(() => { setMsg({ tipo: "ok", texto: "Slot eliminado." }); cargarGrillaCurso(); })
      .catch((err) => setMsg({ tipo: "error", texto: err.response?.data?.mensaje || "No se pudo eliminar." }));
  };

  const cursoLabel = (c) => `${c.grado} · Paralelo ${c.paralelo}${c.anoLectivo ? ` — ${c.anoLectivo}` : ""}`;

  return (
    <Layout
      breadcrumb={["Inicio", "Horarios"]}
      sidebarTitle="HORARIOS"
      menuItems={menuItems}
      seccion={seccion}
      onSeccionChange={setSeccion}
    >
      {/* Header con KPIs */}
      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">Horarios escolares</h1>
          <p className="mt-1 text-sm text-slate-500">
            07:30–12:30 · 30h presenciales/semana · las 10h de trabajo en casa no se cargan.
          </p>
        </div>
        {cursoActivo && (
          <div className="flex gap-2">
            <KPI label={`Asignadas / ${TOPE_CURSO}`} valor={totalHorasCurso} tone={totalHorasCurso > TOPE_CURSO ? "red" : totalHorasCurso === TOPE_CURSO ? "emerald" : "blue"} />
            <KPI label="Suma materias" valor={totalHorasEsperadas} tone="slate" />
            <KPI label="Materias" valor={cursoActivo.materias.length} tone="emerald" />
          </div>
        )}
      </div>

      {msg.texto && (
        <div className={`mb-4 rounded-xl px-4 py-2 text-sm ${msg.tipo === "ok" ? "bg-emerald-50 text-emerald-700 border border-emerald-200" : "bg-red-50 text-red-700 border border-red-200"}`}>
          {msg.texto}
        </div>
      )}

      {/* SELECTOR PILL — Curso agrupado por nivel educativo (patrón del módulo Grados) */}
      {(seccion === "curso" || seccion === "asignar") && (
        <div className="mb-4 space-y-2 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
          {cursos.length === 0 && <span className="text-xs text-slate-400">Cargando cursos...</span>}
          {Object.entries(cursos.reduce((acc, c) => {
            const nivel = c.nivel || "Sin nivel";
            (acc[nivel] = acc[nivel] || []).push(c);
            return acc;
          }, {})).map(([nivel, lista]) => (
            <div key={nivel} className="flex flex-wrap items-center gap-2">
              <span className="text-[10px] font-bold uppercase text-slate-500 min-w-[110px]">{nivel}:</span>
              {lista.map((c) => {
                const sinMaterias = c.materias.length === 0;
                return (
                  <button key={c.key} type="button" onClick={() => setCursoSel(c.key)}
                    className={`rounded-full px-3 py-1.5 text-xs font-semibold transition ${
                      cursoSel === c.key
                        ? "bg-[#243A76] text-white shadow-sm"
                        : sinMaterias
                          ? "bg-slate-50 text-slate-400 border border-dashed border-slate-300 hover:bg-slate-100"
                          : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                    }`}
                    title={sinMaterias ? "Sin materias asignadas todavía" : ""}>
                    {c.grado} · {c.paralelo}
                    {sinMaterias && <span className="ml-1 text-[9px]">·⚠</span>}
                  </button>
                );
              })}
            </div>
          ))}
        </div>
      )}

      {/* SELECTOR PILL — Docente */}
      {seccion === "docente" && (
        <div className="mb-4 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
          <div className="flex flex-wrap items-center gap-2">
            <span className="text-[10px] font-bold uppercase text-slate-500 mr-2">Docente:</span>
            {docentes.length === 0 && <span className="text-xs text-slate-400">Sin docentes registrados.</span>}
            {docentes.map((d) => (
              <button key={d.id} type="button" onClick={() => setDocenteSel(d.id)}
                className={`rounded-full px-3 py-1.5 text-xs font-semibold transition ${
                  String(docenteSel) === String(d.id)
                    ? "bg-[#243A76] text-white shadow-sm"
                    : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                }`}>
                {d.nombre}
              </button>
            ))}
          </div>
          {docenteSel && (() => {
            const h = grillaDoc.totalHoras || 0;
            const tone = h >= TOPE_DOCENTE_MAX ? "text-red-700" : h >= TOPE_DOCENTE ? "text-amber-700" : "text-emerald-700";
            return (
              <div className="mt-3 flex flex-wrap items-center gap-2 text-xs">
                <span className="text-slate-500">Horas asignadas:</span>
                <span className={`rounded-full px-2 py-0.5 font-bold ${tone} bg-white border border-current`}>
                  {h}h / {TOPE_DOCENTE}h base (máx {TOPE_DOCENTE_MAX}h)
                </span>
                {h >= TOPE_DOCENTE && h < TOPE_DOCENTE_MAX && (
                  <span className="text-amber-700">⚠ Ya superó las 25h. Puede asumir hasta 30h si hay tiempo disponible.</span>
                )}
                {h >= TOPE_DOCENTE_MAX && (
                  <span className="text-red-700">⛔ Alcanzó el máximo permitido (30h).</span>
                )}
              </div>
            );
          })()}
        </div>
      )}

      {/* VISTA POR CURSO */}
      {seccion === "curso" && cursoActivo && (
        <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          <div className="flex items-center justify-between border-b border-slate-100 px-5 py-3">
            <div>
              <p className="text-xs text-slate-400">Horario del curso</p>
              <p className="text-sm font-bold text-slate-800">{cursoLabel(cursoActivo)}</p>
            </div>
            <span className="rounded-full bg-blue-50 px-3 py-1 text-xs font-bold text-[#243A76]">
              {totalHorasCurso} / {totalHorasEsperadas} horas
            </span>
          </div>
          <div className="overflow-x-auto">
            <GrillaHorario periodos={periodos} slotsMap={slotsMap} modo="curso" onEliminar={eliminarSlot} />
          </div>
        </div>
      )}

      {/* VISTA POR DOCENTE */}
      {seccion === "docente" && docenteSel && (
        <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          <div className="overflow-x-auto">
            <GrillaHorario periodos={periodos} slotsMap={slotsDocMap} modo="docente" />
          </div>
        </div>
      )}

      {/* ASIGNAR HORAS */}
      {seccion === "asignar" && cursoActivo && cursoActivo.materias.length === 0 && (
        <div className="rounded-2xl border border-amber-200 bg-amber-50 p-6 text-center shadow-sm">
          <p className="text-sm font-bold text-amber-800">Este curso aún no tiene materias asignadas.</p>
          <p className="mt-1 text-xs text-amber-700">
            Ve al módulo <a href="/asignaciones" className="underline font-semibold">Asignaciones</a> para crear las materias del curso {cursoActivo.grado} · Paralelo {cursoActivo.paralelo}.
          </p>
        </div>
      )}

      {seccion === "asignar" && cursoActivo && cursoActivo.materias.length > 0 && (
        <div className="grid gap-4 lg:grid-cols-[320px_minmax(0,1fr)]">
          {/* Panel de materias del curso */}
          <aside className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm h-fit">
            <h3 className="text-sm font-bold text-slate-800">Materias del curso</h3>
            <p className="mt-1 text-xs text-slate-500">Elige una para asignarle horas en la grilla →</p>
            <div className="mt-3 space-y-2">
              {cursoActivo.materias.map((m) => {
                const asignadas = conteoMaterias[m.idAsignacion] || 0;
                const max = m.horasSemanales || 4;
                const color = colorMateria(m.idAsignatura);
                const completo = asignadas >= max;
                const activo = String(asignSel) === String(m.idAsignacion);
                const horasDocente = horasPorDocente[m.idDocente] || 0;
                const excedeSuave = horasDocente >= TOPE_DOCENTE;
                const excedeDuro  = horasDocente >= TOPE_DOCENTE_MAX;
                return (
                  <button key={m.idAsignacion} type="button" onClick={() => setAsignSel(String(m.idAsignacion))}
                    className={`w-full rounded-xl border-l-4 ${color.border} px-3 py-2 text-left text-xs transition ${
                      activo ? `${color.bg} shadow-md ring-2 ring-offset-1 ring-[#243A76]` : "bg-white hover:bg-slate-50"
                    }`}>
                    <div className="flex items-start justify-between gap-2">
                      <p className={`font-bold ${activo ? color.text : "text-slate-700"}`}>{m.asignatura}</p>
                      <span className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${
                        completo ? "bg-emerald-100 text-emerald-700" : `${color.bg} ${color.text}`
                      }`}>
                        {asignadas}/{max}
                      </span>
                    </div>
                    <p className="mt-1 text-[10px] text-slate-500">{m.docente}</p>
                    <div className="mt-1 flex flex-wrap items-center gap-1">
                      {completo && <span className="text-[10px] font-bold text-emerald-600">✓ Materia completa</span>}
                      <span className={`rounded-full px-1.5 py-0.5 text-[9px] font-bold ${
                        excedeDuro ? "bg-red-100 text-red-700" : excedeSuave ? "bg-amber-100 text-amber-700" : "bg-slate-100 text-slate-500"
                      }`}>
                        Docente: {horasDocente}h {excedeDuro ? "⛔" : excedeSuave ? "⚠" : ""}
                      </span>
                    </div>
                    {excedeSuave && !excedeDuro && (
                      <p className="mt-1 text-[10px] text-amber-700">
                        Este docente ya cubrió sus 25h. Puedes seguir asignando hasta 30h si dispone de tiempo.
                      </p>
                    )}
                    {excedeDuro && (
                      <p className="mt-1 text-[10px] text-red-700">
                        Docente al máximo (30h). Revisa disponibilidad antes de asignar más.
                      </p>
                    )}
                  </button>
                );
              })}
            </div>
          </aside>

          {/* Grilla clickeable */}
          <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
            <div className="flex items-center justify-between border-b border-slate-100 px-5 py-3">
              <div>
                <p className="text-xs text-slate-400">Asignando en</p>
                <p className="text-sm font-bold text-slate-800">{cursoLabel(cursoActivo)}</p>
              </div>
              <span className="text-[10px] text-slate-500">
                {asignSel ? "Clic en LIBRE para asignar" : "Selecciona una materia →"}
              </span>
            </div>
            <div className="overflow-x-auto">
              <GrillaAsignar periodos={periodos} slotsMap={slotsMap} idAsignacion={asignSel} onAsignar={asignarSlot} onEliminar={eliminarSlot} />
            </div>
          </div>
        </div>
      )}

      {/* Empty states */}
      {seccion === "curso" && !cursoActivo && <Placeholder mensaje="Elige un curso arriba para ver su horario." />}
      {seccion === "docente" && !docenteSel && <Placeholder mensaje="Elige un docente arriba para ver su horario personal." />}
      {seccion === "asignar" && !cursoActivo && <Placeholder mensaje="Elige un curso arriba para empezar a asignar horas." />}
    </Layout>
  );
}

function KPI({ label, valor, tone }) {
  const tones = {
    blue:    "bg-blue-50 text-blue-700",
    emerald: "bg-emerald-50 text-emerald-700",
    amber:   "bg-amber-50 text-amber-700",
    red:     "bg-red-50 text-red-700",
    slate:   "bg-slate-100 text-slate-600",
  };
  return (
    <div className={`rounded-xl px-3 py-2 ${tones[tone] || tones.slate}`}>
      <p className="text-[10px] font-bold uppercase leading-none">{label}</p>
      <p className="mt-1 text-lg font-bold leading-none">{valor}</p>
    </div>
  );
}

function Placeholder({ mensaje }) {
  return (
    <div className="rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center shadow-sm">
      <p className="text-sm text-slate-400">{mensaje}</p>
    </div>
  );
}

function GrillaHorario({ periodos, slotsMap, modo, onEliminar }) {
  return (
    <table className="w-full text-sm">
      <thead className="bg-slate-50 text-[10px] uppercase tracking-wide text-slate-500">
        <tr>
          <th className="px-3 py-3 text-left font-bold w-32">Período</th>
          {DIAS.map((d) => <th key={d.num} className="px-3 py-3 text-center font-bold min-w-[140px]">{d.label}</th>)}
        </tr>
      </thead>
      <tbody>
        {periodos.map((p) => (
          <tr key={p.idPeriodo} className="border-t border-slate-100">
            <td className="px-3 py-2 bg-slate-50 align-top">
              <p className="text-xs font-bold text-slate-700">{p.nombre}</p>
              <p className="text-[10px] text-slate-500">{String(p.horaInicio).slice(0, 5)} – {String(p.horaFin).slice(0, 5)}</p>
            </td>
            {DIAS.map((d) => {
              const s = slotsMap[`${p.idPeriodo}-${d.num}`];
              if (!s) return <td key={d.num} className="px-2 py-2 text-center align-middle"><span className="text-xs text-slate-300">—</span></td>;
              const color = colorMateria(s.asignatura);
              return (
                <td key={d.num} className="px-2 py-2 align-middle">
                  <div className={`group rounded-lg border-l-4 ${color.border} ${color.bg} px-2 py-2 text-left`}>
                    <p className={`text-xs font-bold ${color.text} truncate`}>{s.asignatura}</p>
                    <p className="mt-0.5 text-[10px] text-slate-600 truncate">
                      {modo === "docente" ? `${s.grado} · ${s.paralelo}` : s.docente}
                    </p>
                    {onEliminar && (
                      <button onClick={() => onEliminar(s.idHorario)}
                        className="mt-1 hidden rounded bg-red-500 px-2 py-0.5 text-[9px] font-bold text-white hover:bg-red-600 group-hover:inline-block">
                        Eliminar
                      </button>
                    )}
                  </div>
                </td>
              );
            })}
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function GrillaAsignar({ periodos, slotsMap, idAsignacion, onAsignar, onEliminar }) {
  return (
    <table className="w-full text-sm">
      <thead className="bg-slate-50 text-[10px] uppercase tracking-wide text-slate-500">
        <tr>
          <th className="px-3 py-3 text-left font-bold w-32">Período</th>
          {DIAS.map((d) => <th key={d.num} className="px-3 py-3 text-center font-bold min-w-[130px]">{d.label}</th>)}
        </tr>
      </thead>
      <tbody>
        {periodos.map((p) => (
          <tr key={p.idPeriodo} className="border-t border-slate-100">
            <td className="px-3 py-2 bg-slate-50 align-top">
              <p className="text-xs font-bold text-slate-700">{p.nombre}</p>
              <p className="text-[10px] text-slate-500">{String(p.horaInicio).slice(0, 5)} – {String(p.horaFin).slice(0, 5)}</p>
            </td>
            {DIAS.map((d) => {
              const s = slotsMap[`${p.idPeriodo}-${d.num}`];
              const ocupadoPorMi = s && String(s.idAsignacion) === String(idAsignacion);
              if (s) {
                const color = colorMateria(s.asignatura);
                return (
                  <td key={d.num} className="px-2 py-2 align-middle">
                    <div className={`group rounded-lg border-l-4 ${color.border} ${ocupadoPorMi ? color.bg : "bg-slate-50 opacity-60"} px-2 py-1.5 text-left`}>
                      <p className={`text-[11px] font-bold ${ocupadoPorMi ? color.text : "text-slate-500"} truncate`}>{s.asignatura}</p>
                      <p className="text-[9px] text-slate-500 truncate">{s.docente}</p>
                      {ocupadoPorMi && onEliminar && (
                        <button onClick={() => onEliminar(s.idHorario)}
                          className="mt-1 hidden rounded bg-red-500 px-1.5 py-0.5 text-[9px] font-bold text-white group-hover:inline-block">
                          Quitar
                        </button>
                      )}
                    </div>
                  </td>
                );
              }
              return (
                <td key={d.num} className="px-2 py-2 text-center align-middle">
                  <button onClick={() => onAsignar(d.num, p.idPeriodo)}
                    disabled={!idAsignacion}
                    className={`w-full rounded-lg border-2 border-dashed py-2 text-[10px] font-bold transition ${
                      idAsignacion
                        ? "border-emerald-300 bg-emerald-50 text-emerald-700 hover:border-emerald-500 hover:bg-emerald-100"
                        : "border-slate-200 text-slate-300 cursor-not-allowed"
                    }`}>
                    LIBRE
                  </button>
                </td>
              );
            })}
          </tr>
        ))}
      </tbody>
    </table>
  );
}
