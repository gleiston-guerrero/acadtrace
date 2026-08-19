import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import * as XLSX from "xlsx";
import { jsPDF } from "jspdf";
import autoTable from "jspdf-autotable";
import Layout from "../../components/Layout";
import { getActividades, getAulaVirtualResumen, getEstudiantesPorAsignacion, getMisAsignaciones, getPeriodos, registrarAsistenciaGrupal } from "../../services/api";

const icon = "M4 6h16M4 12h16M4 18h16";
const Icono = () => <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeWidth="2" d={icon} /></svg>;
const hoy = () => new Date().toISOString().slice(0, 10);
const fmt = (f) => f ? new Date(`${String(f).slice(0, 10)}T00:00:00`).toLocaleDateString("es-EC") : "N/D";
const cursoNombre = (c) => `${c.asignatura?.nombre || "Asignatura"} · ${c.grado?.nombre || "Grado N/D"} ${c.paralelo?.letra || ""}`;
const errText = (e, fallback) => e.response?.data?.message || e.response?.data?.detail || fallback;

export default function AulaVirtual() {
  const [seccion, setSeccion] = useState("cursos");
  const [cursos, setCursos] = useState([]), [periodos, setPeriodos] = useState([]);
  const [resumenes, setResumenes] = useState({}), [actividades, setActividades] = useState({}), [estudiantes, setEstudiantes] = useState({});
  const [loading, setLoading] = useState(true), [error, setError] = useState(""), [avisos, setAvisos] = useState({});
  const [cursoAsis, setCursoAsis] = useState(""), [periodoAsis, setPeriodoAsis] = useState(""), [fecha, setFecha] = useState(hoy);
  const [estados, setEstados] = useState({}), [guardando, setGuardando] = useState(false), [mensaje, setMensaje] = useState("");
  const navigate = useNavigate();

  useEffect(() => { (async () => {
    try {
      const rc = await getMisAsignaciones(), lista = rc.data || []; setCursos(lista);
      if (lista[0]) setCursoAsis(String(lista[0].idAsignacion));
      const [rp, rr, ...otros] = await Promise.allSettled([getPeriodos(), getAulaVirtualResumen(lista.map((c) => c.idAsignacion)), ...lista.map((c) => getActividades(c.idAsignacion)), ...lista.map((c) => getEstudiantesPorAsignacion(c.idAsignacion))]);
      if (rp.status === "fulfilled") { const ps = rp.value.data || []; setPeriodos(ps); if (ps[0]) setPeriodoAsis(String(ps[0].id_periodo ?? ps[0].idPeriodo)); } else setAvisos((v) => ({...v, periodos:"No se pudo consultar el calendario."}));
      if (rr.status === "fulfilled") setResumenes(Object.fromEntries((rr.value.data?.cursos || []).map((x) => [x.id_asignacion, x]))); else setAvisos((v) => ({...v, resumen:"Promedios y asistencia no disponibles."}));
      const acts = {}, ests = {}; lista.forEach((c, i) => { if (otros[i]?.status === "fulfilled") acts[c.idAsignacion] = otros[i].value.data || []; if (otros[i + lista.length]?.status === "fulfilled") ests[c.idAsignacion] = otros[i + lista.length].value.data || []; });
      setActividades(acts); setEstudiantes(ests);
    } catch (e) { setError(errText(e, "No se pudieron cargar tus cursos.")); } finally { setLoading(false); }
  })(); }, []);
  useEffect(() => { setEstados(Object.fromEntries((estudiantes[cursoAsis] || []).map((m) => [m.idMatricula, "PRESENTE"]))); setMensaje(""); }, [cursoAsis, estudiantes]);

  const pendientes = useMemo(() => cursos.flatMap((c) => (actividades[c.idAsignacion] || []).filter((a) => String(a.fechaEntrega || a.fecha_entrega || "").slice(0,10) < hoy()).map((a) => ({...a, curso:cursoNombre(c), matriculados:estudiantes[c.idAsignacion]?.length}))), [cursos, actividades, estudiantes]);
  const consolidado = cursos.map((c) => ({ curso:cursoNombre(c), estudiantes:c.cantidadEstudiantes, promedio:resumenes[c.idAsignacion]?.promedio_curso }));
  const exportar = (tipo) => {
    const filas = consolidado.map((x) => ({Curso:x.curso, Estudiantes:x.estudiantes ?? "N/D", Promedio:x.promedio ?? "N/D"}));
    if (tipo === "xlsx") { const wb=XLSX.utils.book_new(); XLSX.utils.book_append_sheet(wb,XLSX.utils.json_to_sheet(filas),"Consolidado"); XLSX.writeFile(wb,"calificaciones-consolidadas.xlsx"); return; }
    const doc=new jsPDF({orientation:"landscape"}); doc.text("Calificaciones consolidadas",14,15); autoTable(doc,{head:[["Curso","Estudiantes","Promedio"]],body:filas.map(Object.values),startY:22}); doc.save("calificaciones-consolidadas.pdf");
  };
  const guardar = async () => {
    if (!cursoAsis || !periodoAsis) { setMensaje("Selecciona curso y período."); return; }
    setGuardando(true); setMensaje(""); try { const lista=estudiantes[cursoAsis]||[]; await registrarAsistenciaGrupal({idAsignacion:Number(cursoAsis),idPeriodo:Number(periodoAsis),fecha,asistencias:lista.map((m)=>({idMatricula:m.idMatricula,estado:estados[m.idMatricula]||"PRESENTE",justificacion:""}))}); setMensaje("Asistencia guardada correctamente."); } catch(e) { setMensaje(errText(e,"No se pudo guardar la asistencia.")); } finally { setGuardando(false); }
  };
  const labels={cursos:"Mis Cursos",calendario:"Calendario Académico",pendientes:"Actividades Pendientes",asistencia:"Asistencia Rápida",notas:"Calificaciones Consolidadas"};
  const menuItems=Object.entries(labels).map(([id,label])=>({id,label,icon:<Icono/>}));
  return <Layout breadcrumb={["Inicio","Aula Virtual"]} sidebarTitle="AULA VIRTUAL" menuItems={menuItems} seccion={seccion} onSeccionChange={setSeccion}>
    <h1 className="text-2xl font-bold text-slate-800">{labels[seccion]}</h1><p className="mb-5 mt-1 text-sm text-slate-500">Información académica real de tus asignaciones.</p>
    {loading && <div className="grid gap-4 md:grid-cols-3">{[1,2,3].map((x)=><div key={x} className="h-44 animate-pulse rounded-2xl bg-white"/>)}</div>}{!loading&&error&&<Aviso error>{error}</Aviso>}
    {!loading&&!error&&seccion==="cursos"&&(cursos.length?<div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{cursos.map((c)=><button key={c.idAsignacion} onClick={()=>navigate(`/aula-virtual/${c.idAsignacion}`)} className="rounded-2xl border bg-white p-5 text-left shadow-sm"><p className="text-lg font-bold text-[#243A76]">{c.asignatura?.nombre}</p><p className="mt-1 text-sm text-slate-500">{c.grado?.nombre} · Paralelo {c.paralelo?.letra||"N/D"}</p><p className="mt-5 text-sm font-semibold">{c.cantidadEstudiantes??"N/D"} estudiantes</p></button>)}</div>:<Aviso>No tienes cursos asignados.</Aviso>)}
    {!loading&&seccion==="calendario"&&<Panel><h2 className="font-bold">Períodos de evaluación</h2>{avisos.periodos&&<Aviso error>{avisos.periodos}</Aviso>}<div className="mt-4 grid gap-3 md:grid-cols-3">{periodos.map((p)=><div key={p.id_periodo??p.idPeriodo} className="rounded-xl border p-4"><b>{p.nombre}</b><p className="mt-2 text-sm text-slate-500">{fmt(p.fecha_inicio??p.fechaInicio)} — {fmt(p.fecha_fin??p.fechaFin)}</p></div>)}</div>{!avisos.periodos&&!periodos.length&&<p className="mt-4 text-sm text-slate-500">No hay períodos configurados.</p>}</Panel>}
    {!loading&&seccion==="pendientes"&&<Panel><p className="mb-4 text-sm">Actividades vencidas con estudiantes que podrían estar pendientes de calificación. No existe un contrato de Entrega; no se afirma que hayan entregado.</p>{pendientes.map((a)=><div key={a.idActividad??a.id_actividad} className="mb-2 rounded-xl border p-3"><b>{a.nombre}</b><p className="text-sm text-slate-500">{a.curso} · venció {fmt(a.fechaEntrega??a.fecha_entrega)} · {a.matriculados??"N/D"} matriculados</p></div>)}{!pendientes.length&&<p className="text-sm text-slate-500">No hay actividades vencidas identificables.</p>}</Panel>}
    {!loading&&seccion==="asistencia"&&<Panel><div className="grid gap-3 md:grid-cols-3"><Select label="Curso" value={cursoAsis} set={setCursoAsis} items={cursos.map((c)=>[c.idAsignacion,cursoNombre(c)])}/><Select label="Período" value={periodoAsis} set={setPeriodoAsis} items={periodos.map((p)=>[p.id_periodo??p.idPeriodo,p.nombre])}/><label className="text-sm">Fecha<input className="mt-1 w-full rounded-lg border p-2" type="date" value={fecha} onChange={(e)=>setFecha(e.target.value)}/></label></div><div className="mt-4 space-y-2">{(estudiantes[cursoAsis]||[]).map((m)=><div key={m.idMatricula} className="flex justify-between rounded-lg border p-2 text-sm"><span>{m.estudiante?.apellidos} {m.estudiante?.nombres}</span><select value={estados[m.idMatricula]||"PRESENTE"} onChange={(e)=>setEstados((v)=>({...v,[m.idMatricula]:e.target.value}))}>{[["PRESENTE","Presente"],["AUSENTE","Ausente"],["JUSTIFICADO","Justificado"],["ATRASO","Atrasado"]].map(([v,l])=><option key={v} value={v}>{l}</option>)}</select></div>)}</div><button disabled={guardando||!(estudiantes[cursoAsis]||[]).length} onClick={guardar} className="mt-4 rounded-lg bg-[#243A76] px-4 py-2 text-white disabled:opacity-50">{guardando?"Guardando...":"Guardar en grupo"}</button>{mensaje&&<p className="mt-3 text-sm">{mensaje}</p>}</Panel>}
    {!loading&&seccion==="notas"&&<Panel>{avisos.resumen&&<Aviso error>{avisos.resumen}</Aviso>}<div className="mb-4 flex gap-2"><button onClick={()=>exportar("xlsx")} className="rounded border px-3 py-2">Exportar Excel</button><button onClick={()=>exportar("pdf")} className="rounded border px-3 py-2">Exportar PDF</button></div><table className="w-full text-sm"><thead><tr className="border-b text-left"><th className="p-2">Curso</th><th>Estudiantes</th><th>Promedio real</th></tr></thead><tbody>{consolidado.map((x)=><tr className="border-b" key={x.curso}><td className="p-2">{x.curso}</td><td>{x.estudiantes??"N/D"}</td><td>{x.promedio==null?"N/D":Number(x.promedio).toFixed(2)}</td></tr>)}</tbody></table></Panel>}
  </Layout>;
}
function Panel({children}){return <section className="rounded-2xl border bg-white p-5 shadow-sm">{children}</section>}
function Aviso({children,error}){return <div className={`rounded-xl border p-4 text-sm ${error?"border-red-200 bg-red-50 text-red-700":"bg-white text-slate-500"}`}>{children}</div>}
function Select({label,value,set,items}){return <label className="text-sm">{label}<select className="mt-1 w-full rounded-lg border p-2" value={value} onChange={(e)=>set(e.target.value)}>{items.map(([v,l])=><option key={v} value={v}>{l}</option>)}</select></label>}
