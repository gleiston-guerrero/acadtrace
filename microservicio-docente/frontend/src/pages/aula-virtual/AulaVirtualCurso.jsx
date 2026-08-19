import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import * as XLSX from "xlsx";
import { jsPDF } from "jspdf";
import autoTable from "jspdf-autotable";
import Layout from "../../components/Layout";
import {
  getAulaVirtualSemanas,
  getMisAsignaciones,
  getEstudiantesPorAsignacion,
  getAsistenciaPorAsignacion,
  registrarAsistenciaGrupal,
  getActividades,
  createActividad,
  updateActividad,
  deleteActividad,
  getCalificacionesPorActividad,
  guardarCalificacion,
  getMateriales,
  createMaterial,
  getAnuncios,
  createAnuncio,
} from "../../services/api";

const TIPOS_ACTIVIDAD = [
  { valor: "LECCION_ORAL",              label: "Lección oral",              categoria: "FORMATIVA" },
  { valor: "LECCION_ESCRITA",           label: "Lección escrita",           categoria: "FORMATIVA" },
  { valor: "TAREA",                     label: "Tarea",                     categoria: "FORMATIVA" },
  { valor: "TALLER",                    label: "Taller",                    categoria: "FORMATIVA" },
  { valor: "CUADERNO",                  label: "Cuaderno",                  categoria: "FORMATIVA" },
  { valor: "TRABAJO_INDIVIDUAL",        label: "Trabajo individual",        categoria: "FORMATIVA" },
  { valor: "EXPOSICION",                label: "Exposición",                categoria: "FORMATIVA" },
  { valor: "PROYECTO_INTERDISCIPLINARIO", label: "Proyecto interdisciplinario", categoria: "SUMATIVA" },
  { valor: "EXAMEN_TRIMESTRAL",         label: "Examen trimestral",         categoria: "SUMATIVA" },
];
const esSumativaPorTipo = (tipo) => TIPOS_ACTIVIDAD.find((t) => t.valor === tipo)?.categoria === "SUMATIVA";
const labelTipo = (tipo) => TIPOS_ACTIVIDAD.find((t) => t.valor === tipo)?.label || tipo;
const ACT_FORM_INICIAL = {
  tipo: "TAREA",
  nombre: "",
  descripcion: "",
  fechaEntrega: new Date().toISOString().slice(0, 10),
  ponderacion: 10,
  notaMaxima: 10,
};

const PRIMARY = "#243A76";

const ICONO = {
  resumen:      "M4 6h16M4 10h16M4 14h10M4 18h10",
  asistencia:   "M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z",
  actividades:  "M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2",
  calificaciones: "M9 17v-2a2 2 0 012-2h2a2 2 0 012 2v2m-6 0h6m-6 0H5a2 2 0 01-2-2V5a2 2 0 012-2h14a2 2 0 012 2v10a2 2 0 01-2 2h-4",
  materiales:   "M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253",
  anuncios:     "M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9",
  estudiantes:  "M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z",
};
const IconoSvg = ({ path }) => (
  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d={path} />
  </svg>
);

const formatDate = (value) => value
  ? new Date(`${String(value).slice(0, 10)}T00:00:00`).toLocaleDateString("es-EC", { day: "2-digit", month: "short" })
  : "Sin fecha";

const resumenSemana = (semana) => [
  ["Actividades", semana.actividades, "text-indigo-700", "bg-indigo-50"],
  ["Asistencias", semana.asistencias, "text-emerald-700", "bg-emerald-50"],
  ["Materiales", semana.materiales, "text-cyan-700", "bg-cyan-50"],
  ["Anuncios", semana.anuncios, "text-amber-700", "bg-amber-50"],
  ["Calificaciones", semana.calificaciones, "text-violet-700", "bg-violet-50"],
];

const etiquetaTipo = (tipo) => String(tipo || "Actividad").replaceAll("_", " ");

function ItemSemana({ etiqueta, items, color, fondo }) {
  if (!items.length) return null;
  return (
    <section className={`rounded-xl ${fondo} p-3`}>
      <div className="mb-2 flex items-center justify-between gap-2">
        <h4 className={`text-xs font-bold uppercase tracking-wide ${color}`}>{etiqueta}</h4>
        <span className={`rounded-full bg-white px-2 py-0.5 text-[10px] font-bold ${color}`}>{items.length}</span>
      </div>
      <ul className="space-y-1.5">
        {items.slice(0, 6).map((item) => (
          <li key={`${etiqueta}-${item.id_actividad || item.id_asistencia || item.id_material || item.id_anuncio || item.id_calificacion}`} className="rounded-lg bg-white/80 px-2.5 py-2 text-xs text-slate-600">
            <p className="font-medium text-slate-700">{item.nombre || item.titulo || item.actividad || item.estado}</p>
            <p className="mt-0.5 text-[10px] text-slate-400">{formatDate(item.fecha_entrega || item.fecha || item.fecha_registro)}{item.nota != null ? ` · Nota ${item.nota}` : ""}</p>
          </li>
        ))}
        {items.length > 6 && <li className="px-1 text-[10px] text-slate-400">+ {items.length - 6} registros</li>}
      </ul>
    </section>
  );
}

export default function AulaVirtualCurso() {
  const { idAsignacion } = useParams();
  const navigate = useNavigate();
  const [curso, setCurso] = useState(null);
  const [agenda, setAgenda] = useState({ trimestres: [], pendientes: [] });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [trimestreActivo, setTrimestreActivo] = useState("");
  const [semanaAbierta, setSemanaAbierta] = useState(null);
  const [seccion, setSeccion] = useState("resumen");
  const [numeroSemana, setNumeroSemana] = useState(1);
  const [bloquesAbiertos, setBloquesAbiertos] = useState({});
  const toggleBloque = (etiqueta) => setBloquesAbiertos((prev) => ({ ...prev, [etiqueta]: !prev[etiqueta] }));

  // Estado de la sección "Tomar asistencia"
  const [estudiantesCurso, setEstudiantesCurso] = useState([]);
  const [asistenciaFecha, setAsistenciaFecha] = useState(() => new Date().toISOString().slice(0, 10));
  const [asistenciaEstado, setAsistenciaEstado] = useState({}); // { idMatricula: "PRESENTE" }
  const [asistenciaJustif, setAsistenciaJustif] = useState({}); // { idMatricula: "texto..." }
  const [asistenciaCargando, setAsistenciaCargando] = useState(false);
  const [asistenciaGuardando, setAsistenciaGuardando] = useState(false);
  const [asistenciaMsg, setAsistenciaMsg] = useState({ tipo: "", texto: "" });
  const [asistenciaYaExiste, setAsistenciaYaExiste] = useState(false);

  // Estado de la sección "Actividades"
  const [actividades, setActividades] = useState([]);
  const [actividadesCargando, setActividadesCargando] = useState(false);
  const [actividadesMsg, setActividadesMsg] = useState({ tipo: "", texto: "" });
  const [modalActividad, setModalActividad] = useState({ abierto: false, editando: null, form: ACT_FORM_INICIAL, guardando: false });
  const [notas, setNotas] = useState({});
  const [recursos, setRecursos] = useState({ materiales: [], anuncios: [] });
  const [seccionMsg, setSeccionMsg] = useState("");

  // Trimestre activo (movido arriba para evitar TDZ en los useEffect que lo referencian)
  const trimestre = useMemo(
    () => agenda.trimestres.find((item) => String(item.id_periodo) === trimestreActivo),
    [agenda.trimestres, trimestreActivo],
  );
  const anuncios = useMemo(() => agenda.trimestres.flatMap((item) => item.semanas.flatMap((semana) => semana.anuncios))
    .sort((a, b) => new Date(b.fecha) - new Date(a.fecha)).slice(0, 5), [agenda.trimestres]);

  const cargarActividades = () => {
    if (!trimestre) return;
    setActividadesCargando(true);
    getActividades(Number(idAsignacion), Number(trimestre.id_periodo))
      .then((r) => setActividades(r.data || []))
      .catch(() => setActividadesMsg({ tipo: "error", texto: "No se pudieron cargar las actividades." }))
      .finally(() => setActividadesCargando(false));
  };

  useEffect(() => {
    if (seccion !== "actividades" || !trimestre) return;
    cargarActividades();
  }, [seccion, trimestre?.id_periodo, idAsignacion]);

  useEffect(() => {
    if (!["calificaciones", "materiales", "anuncios", "estudiantes"].includes(seccion)) return;
    if (!estudiantesCurso.length) getEstudiantesPorAsignacion(idAsignacion).then((r) => setEstudiantesCurso(r.data || [])).catch(() => setSeccionMsg("No se pudieron cargar los estudiantes."));
    if (seccion === "calificaciones" && trimestre) {
      getActividades(Number(idAsignacion), Number(trimestre.id_periodo)).then(async (r) => {
        const acts = r.data || []; setActividades(acts);
        const resultados = await Promise.allSettled(acts.map((a) => getCalificacionesPorActividad(a.idActividad)));
        const mapa = {}; resultados.forEach((res) => { if (res.status === "fulfilled") (res.value.data || []).forEach((n) => { mapa[`${n.id_matricula}-${n.id_actividad}`] = n; }); }); setNotas(mapa);
      }).catch(() => setSeccionMsg("No se pudo cargar la matriz de calificaciones."));
    }
    if (seccion === "materiales") getMateriales(idAsignacion).then((r) => setRecursos((v) => ({...v, materiales:r.data || []}))).catch(() => setSeccionMsg("No se pudieron cargar los materiales."));
    if (seccion === "anuncios") getAnuncios(idAsignacion).then((r) => setRecursos((v) => ({...v, anuncios:r.data || []}))).catch(() => setSeccionMsg("No se pudieron cargar los anuncios."));
  }, [seccion, trimestre?.id_periodo, idAsignacion]);

  const abrirModalActividad = (act = null) => {
    setActividadesMsg({ tipo: "", texto: "" });
    if (act) {
      // El backend a veces devuelve "None" como string literal si el campo no tiene fecha.
      // Solo aceptamos strings que empiecen con dígito (formato YYYY-MM-DD).
      const rawFecha = String(act.fechaEntrega || "");
      const fechaValida = /^\d{4}-\d{2}-\d{2}/.test(rawFecha) ? rawFecha.slice(0, 10) : ACT_FORM_INICIAL.fechaEntrega;
      setModalActividad({
        abierto: true,
        editando: act,
        form: {
          tipo: act.tipo,
          nombre: act.nombre || "",
          descripcion: act.descripcion && act.descripcion !== "None" ? act.descripcion : "",
          fechaEntrega: fechaValida,
          ponderacion: Number(act.ponderacion || 0),
          notaMaxima: Number(act.notaMaxima || 10),
        },
        guardando: false,
      });
    } else {
      setModalActividad({ abierto: true, editando: null, form: ACT_FORM_INICIAL, guardando: false });
    }
  };
  const cerrarModalActividad = () => setModalActividad((m) => ({ ...m, abierto: false }));
  const setFormActividad = (patch) => setModalActividad((m) => ({ ...m, form: { ...m.form, ...patch } }));

  const guardarActividad = async () => {
    const { form, editando } = modalActividad;
    if (!form.nombre.trim()) { setActividadesMsg({ tipo: "error", texto: "El nombre es obligatorio." }); return; }
    if (!form.fechaEntrega || !/^\d{4}-\d{2}-\d{2}$/.test(form.fechaEntrega)) {
      setActividadesMsg({ tipo: "error", texto: "La fecha de entrega debe tener formato YYYY-MM-DD." });
      return;
    }
    if (!trimestre)           { setActividadesMsg({ tipo: "error", texto: "Selecciona un trimestre primero." }); return; }
    const nuevaEsSumativa = esSumativaPorTipo(form.tipo);
    const totalCategoria = actividades
      .filter((a) => esSumativaPorTipo(a.tipo) === nuevaEsSumativa && (!editando || a.idActividad !== editando.idActividad))
      .reduce((total, a) => total + Number(a.ponderacion || 0), 0);
    const limite = nuevaEsSumativa ? 30 : 70;
    if (totalCategoria + Number(form.ponderacion || 0) > limite) {
      setActividadesMsg({ tipo: "error", texto: `La ponderación total ${nuevaEsSumativa ? "sumativa" : "formativa"} no puede superar ${limite}%.` });
      return;
    }
    const payload = {
      asignacionId: Number(idAsignacion),
      periodoId: Number(trimestre.id_periodo),
      tipo: form.tipo,
      nombre: form.nombre.trim(),
      descripcion: form.descripcion.trim(),
      fechaEntrega: form.fechaEntrega,
      ponderacion: Number(form.ponderacion) || 0,
      notaMaxima: Number(form.notaMaxima) || 10,
      esSumativa: nuevaEsSumativa,
    };
    setModalActividad((m) => ({ ...m, guardando: true }));
    setActividadesMsg({ tipo: "", texto: "" });
    try {
      if (editando) {
        await updateActividad(editando.idActividad, payload);
        setActividadesMsg({ tipo: "ok", texto: `Actividad "${payload.nombre}" actualizada.` });
      } else {
        await createActividad(payload);
        setActividadesMsg({ tipo: "ok", texto: `Actividad "${payload.nombre}" creada.` });
      }
      cerrarModalActividad();
      cargarActividades();
    } catch (err) {
      const msg = err.response?.data?.mensaje || err.response?.data?.message || "No se pudo guardar la actividad.";
      setActividadesMsg({ tipo: "error", texto: `[${err.response?.status || "?"}] ${msg}` });
    } finally {
      setModalActividad((m) => ({ ...m, guardando: false }));
    }
  };

  const eliminarActividadCurso = async (act) => {
    if (!window.confirm(`¿Eliminar la actividad "${act.nombre}"? Esta acción no se puede deshacer.`)) return;
    setActividadesMsg({ tipo: "", texto: "" });
    try {
      await deleteActividad(act.idActividad);
      setActividadesMsg({ tipo: "ok", texto: `Actividad "${act.nombre}" eliminada.` });
      cargarActividades();
    } catch (err) {
      const msg = err.response?.data?.mensaje || err.response?.data?.message || "No se pudo eliminar la actividad.";
      setActividadesMsg({ tipo: "error", texto: `[${err.response?.status || "?"}] ${msg}` });
    }
  };

  const conteoPorCategoria = useMemo(() => {
    const total = { FORMATIVA: 0, SUMATIVA: 0, pesoFormativa: 0, pesoSumativa: 0 };
    actividades.forEach((a) => {
      const cat = esSumativaPorTipo(a.tipo) ? "SUMATIVA" : "FORMATIVA";
      total[cat] += 1;
      total[cat === "FORMATIVA" ? "pesoFormativa" : "pesoSumativa"] += Number(a.ponderacion || 0);
    });
    return total;
  }, [actividades]);

  const ESTADOS_ASISTENCIA = [
    { valor: "PRESENTE",    color: "bg-emerald-100 text-emerald-700" },
    { valor: "AUSENTE",     color: "bg-red-100 text-red-700" },
    { valor: "JUSTIFICADO", color: "bg-amber-100 text-amber-700" },
    { valor: "ATRASO",     color: "bg-orange-100 text-orange-700" },
  ];

  useEffect(() => {
    if (seccion !== "asistencia") return;
    if (estudiantesCurso.length > 0) return;
    setAsistenciaCargando(true);
    getEstudiantesPorAsignacion(idAsignacion)
      .then((r) => setEstudiantesCurso(r.data || []))
      .catch(() => setAsistenciaMsg({ tipo: "error", texto: "No se pudieron cargar los estudiantes del curso." }))
      .finally(() => setAsistenciaCargando(false));
  }, [seccion, idAsignacion]);

  useEffect(() => {
    if (seccion !== "asistencia" || !asistenciaFecha) return;
    setAsistenciaEstado({});
    setAsistenciaJustif({});
    setAsistenciaYaExiste(false);
    setAsistenciaMsg({ tipo: "", texto: "" });
    getAsistenciaPorAsignacion(idAsignacion, asistenciaFecha, Number(trimestre?.id_periodo || 0))
      .then((r) => {
        const lista = r.data?.asistencias || r.data || [];
        const map = {};
        const justif = {};
        lista.forEach((a) => {
          map[a.idMatricula ?? a.id_matricula] = a.estado;
          if (a.justificacion) justif[a.idMatricula ?? a.id_matricula] = a.justificacion;
        });
        setAsistenciaEstado(map);
        setAsistenciaJustif(justif);
        setAsistenciaYaExiste(lista.length > 0);
      })
      .catch(() => { /* no pasa nada, empieza vacío */ });
  }, [seccion, asistenciaFecha, idAsignacion, trimestre?.id_periodo]);

  // Lista de estudiantes ordenada alfabeticamente por apellidos + nombres
  const estudiantesOrdenados = useMemo(() => {
    const orden = [...estudiantesCurso];
    orden.sort((a, b) => {
      const na = `${a.estudiante?.apellidos || ""} ${a.estudiante?.nombres || ""}`.trim().toLowerCase();
      const nb = `${b.estudiante?.apellidos || ""} ${b.estudiante?.nombres || ""}`.trim().toLowerCase();
      return na.localeCompare(nb, "es");
    });
    return orden;
  }, [estudiantesCurso]);

  const marcarTodos = (valor) => {
    const nuevo = {};
    estudiantesCurso.forEach((m) => { nuevo[m.idMatricula] = valor; });
    setAsistenciaEstado(nuevo);
  };

  const guardarAsistencia = async () => {
    if (!trimestre) { setAsistenciaMsg({ tipo: "error", texto: "Selecciona un trimestre primero." }); return; }
    const asistencias = estudiantesCurso
      .filter((m) => asistenciaEstado[m.idMatricula])
      .map((m) => ({
        idMatricula: m.idMatricula,
        estado: asistenciaEstado[m.idMatricula],
        justificacion: asistenciaJustif[m.idMatricula] || "",
      }));
    if (asistencias.length === 0) { setAsistenciaMsg({ tipo: "error", texto: "Marca al menos un estudiante antes de guardar." }); return; }
    if (asistenciaYaExiste) {
      const ok = window.confirm(
        `Ya existe asistencia registrada para el ${asistenciaFecha}. ` +
        `Se va a sobreescribir con los cambios actuales (por ejemplo, cambiar de AUSENTE a ATRASO o agregar justificación).\n\n¿Deseas continuar?`
      );
      if (!ok) return;
    }
    setAsistenciaGuardando(true);
    setAsistenciaMsg({ tipo: "", texto: "" });
    try {
      const payload = {
        idAsignacion: Number(idAsignacion),
        idPeriodo: Number(trimestre.id_periodo),
        fecha: asistenciaFecha,
        asistencias,
      };
      await registrarAsistenciaGrupal(payload);
      setAsistenciaMsg({ tipo: "ok", texto: `Asistencia guardada (${asistencias.length} estudiante(s)).` });
      setAsistenciaYaExiste(true);
    } catch (err) {
      console.error("[asistencia] error del backend:", err.response?.status, err.response?.data);
      const backendMsg = err.response?.data?.message || err.response?.data?.mensaje || JSON.stringify(err.response?.data);
      setAsistenciaMsg({ tipo: "error", texto: `[${err.response?.status || "?"}] ${backendMsg || "No se pudo guardar."}` });
    } finally {
      setAsistenciaGuardando(false);
    }
  };

  const contadorAsistencia = ESTADOS_ASISTENCIA.map((e) => ({
    ...e,
    total: Object.values(asistenciaEstado).filter((v) => v === e.valor).length,
  }));

  const guardarNotaCelda = async (matricula, actividad, valor) => {
    if (valor === "") return;
    const nota = Number(valor), maxima = Number(actividad.notaMaxima || 10);
    if (!Number.isFinite(nota) || nota < 0 || nota > maxima) { setSeccionMsg(`La nota debe estar entre 0 y ${maxima}.`); return; }
    const clave = `${matricula.idMatricula}-${actividad.idActividad}`, existente = notas[clave];
    try {
      const r = await guardarCalificacion({ id_matricula: matricula.idMatricula, id_actividad: actividad.idActividad, nota }, existente?.id_calificacion);
      setNotas((v) => ({ ...v, [clave]: r.data })); setSeccionMsg("Nota guardada.");
    } catch (e) { setSeccionMsg(e.response?.data?.nota?.[0] || "No se pudo guardar la nota."); }
  };
  const exportarMatriz = (tipo) => {
    const filas = estudiantesOrdenados.map((m) => ({ Estudiante:`${m.estudiante?.apellidos || ""} ${m.estudiante?.nombres || ""}`.trim(), ...Object.fromEntries(actividades.map((a) => [a.nombre, notas[`${m.idMatricula}-${a.idActividad}`]?.nota ?? "N/D"])) }));
    if (tipo === "xlsx") { const wb=XLSX.utils.book_new(); XLSX.utils.book_append_sheet(wb,XLSX.utils.json_to_sheet(filas),"Calificaciones"); XLSX.writeFile(wb,`calificaciones-${idAsignacion}.xlsx`); return; }
    const doc=new jsPDF({orientation:"landscape"}); doc.text("Matriz de calificaciones",14,15); autoTable(doc,{head:[["Estudiante",...actividades.map((a)=>a.nombre)]],body:filas.map(Object.values),startY:22}); doc.save(`calificaciones-${idAsignacion}.pdf`);
  };
  const publicarMaterial = async (event) => {
    event.preventDefault(); const form=new FormData(event.currentTarget); form.set("id_asignacion",idAsignacion);
    try { await createMaterial(form); const r=await getMateriales(idAsignacion); setRecursos((v)=>({...v,materiales:r.data||[]})); event.currentTarget.reset(); setSeccionMsg("Material publicado."); } catch(e) { setSeccionMsg(e.response?.data?.archivo?.[0] || "No se pudo publicar el material."); }
  };
  const publicarAnuncio = async (event) => {
    event.preventDefault(); const data=Object.fromEntries(new FormData(event.currentTarget)); data.id_asignacion=Number(idAsignacion);
    try { await createAnuncio(data); const r=await getAnuncios(idAsignacion); setRecursos((v)=>({...v,anuncios:r.data||[]})); event.currentTarget.reset(); setSeccionMsg("Anuncio publicado."); } catch(_) { setSeccionMsg("No se pudo publicar el anuncio."); }
  };

  const menuItems = [
    { id: "resumen",        label: "Resumen del curso",  icon: <IconoSvg path={ICONO.resumen} /> },
    { id: "asistencia",     label: "Tomar asistencia",   icon: <IconoSvg path={ICONO.asistencia} /> },
    { id: "actividades",    label: "Actividades",        icon: <IconoSvg path={ICONO.actividades} /> },
    { id: "calificaciones", label: "Calificaciones",     icon: <IconoSvg path={ICONO.calificaciones} /> },
    { id: "materiales",     label: "Materiales",         icon: <IconoSvg path={ICONO.materiales} /> },
    { id: "anuncios",       label: "Anuncios",           icon: <IconoSvg path={ICONO.anuncios} /> },
    { id: "estudiantes",    label: "Estudiantes",        icon: <IconoSvg path={ICONO.estudiantes} /> },
  ];

  useEffect(() => {
    const cargarCurso = async () => {
      setLoading(true);
      setError("");
      try {
        const asignacionesResponse = await getMisAsignaciones();
        const asignacion = (asignacionesResponse.data || []).find((item) => String(item.idAsignacion) === String(idAsignacion));
        if (!asignacion) {
          setError("El curso solicitado no está disponible entre tus asignaciones.");
          return;
        }
        setCurso(asignacion);
        try {
          const agendaResponse = await getAulaVirtualSemanas(idAsignacion);
          const data = agendaResponse.data || { trimestres: [], pendientes: [] };
          setAgenda(data);
          if (data.trimestres?.[0]) {
            setTrimestreActivo(String(data.trimestres[0].id_periodo));
            setSemanaAbierta(`${data.trimestres[0].id_periodo}-1`);
            setNumeroSemana(1);
          }
        } catch (_) {
          setAgenda({ trimestres: [], pendientes: [] });
        }
      } catch (requestError) {
        setError(requestError.response?.status === 401
          ? "Tu sesión expiró. Inicia sesión nuevamente."
          : "No se pudo cargar la información del curso.");
      } finally {
        setLoading(false);
      }
    };
    cargarCurso();
  }, [idAsignacion]);

  return (
    <Layout
      breadcrumb={["Inicio", "Aula Virtual", curso?.asignatura?.nombre || "Curso"]}
      sidebarTitle="CURSO"
      menuItems={menuItems}
      seccion={seccion}
      onSeccionChange={setSeccion}
    >
      {loading && <div className="h-96 animate-pulse rounded-2xl border border-slate-200 bg-white" />}
      {!loading && error && <div className="rounded-2xl border border-red-200 bg-red-50 p-6 text-sm text-red-700">{error}</div>}
      {!loading && !error && curso && (
        <div className="space-y-5">
          <header className="overflow-hidden rounded-2xl bg-[#243A76] p-6 text-white shadow-sm">
            <button type="button" onClick={() => navigate("/aula-virtual")} className="mb-5 text-xs font-semibold text-white/75 hover:text-white">← Volver a mis cursos</button>
            <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
              <div>
                <p className="text-xs font-bold uppercase tracking-[0.18em] text-white/60">Aula Virtual</p>
                <h1 className="mt-2 text-2xl font-bold">{curso.asignatura?.nombre || "Asignatura"}</h1>
                <p className="mt-1 text-sm text-white/75">{curso.grado?.nombre || "Grado"} · Paralelo {curso.paralelo?.letra || "—"}</p>
              </div>
              <div className="rounded-xl bg-white/10 px-4 py-3 text-sm">
                <p className="text-xs text-white/60">Estudiantes</p>
                <p className="font-semibold">{curso.cantidadEstudiantes ?? "Sin dato"}</p>
              </div>
            </div>
          </header>

          <div className="grid gap-5 xl:grid-cols-[minmax(0,1fr)_300px]">
            <main id="calendario" className="min-w-0 space-y-4">
            {seccionMsg && ["calificaciones","materiales","anuncios","estudiantes"].includes(seccion) && <div className="rounded-xl border bg-white p-3 text-sm text-slate-600">{seccionMsg}</div>}
            {seccion === "calificaciones" && <div className="overflow-x-auto rounded-2xl border bg-white p-5 shadow-sm"><div className="mb-4 flex items-center justify-between"><h2 className="font-bold">Matriz de calificaciones</h2><div className="flex gap-2"><button onClick={()=>exportarMatriz("xlsx")} className="rounded border px-3 py-1 text-sm">Excel</button><button onClick={()=>exportarMatriz("pdf")} className="rounded border px-3 py-1 text-sm">PDF</button></div></div><table className="min-w-full text-sm"><thead><tr className="border-b"><th className="p-2 text-left">Estudiante</th>{actividades.map((a)=><th key={a.idActividad} className="min-w-28 p-2 text-center"><span title={`Máximo ${a.notaMaxima}`}>{a.nombre}</span></th>)}</tr></thead><tbody>{estudiantesOrdenados.map((m)=><tr key={m.idMatricula} className="border-b"><td className="p-2">{m.estudiante?.apellidos} {m.estudiante?.nombres}</td>{actividades.map((a)=>{const n=notas[`${m.idMatricula}-${a.idActividad}`];return <td key={a.idActividad} className="p-1"><input aria-label={`Nota ${a.nombre}`} type="number" min="0" max={a.notaMaxima||10} step="0.01" defaultValue={n?.nota??""} onBlur={(e)=>guardarNotaCelda(m,a,e.target.value)} className="w-24 rounded border p-1 text-center"/></td>})}</tr>)}</tbody></table>{!actividades.length&&<p className="py-6 text-center text-sm text-slate-400">No hay actividades en este período.</p>}</div>}
            {seccion === "materiales" && <div className="space-y-4"><form onSubmit={publicarMaterial} className="grid gap-3 rounded-2xl border bg-white p-5 md:grid-cols-2"><h2 className="font-bold md:col-span-2">Publicar material</h2><input required name="titulo" placeholder="Título" className="rounded border p-2"/><input name="url" type="url" placeholder="https://... (opcional)" className="rounded border p-2"/><input name="archivo" type="file" className="rounded border p-2"/><textarea name="descripcion" placeholder="Descripción" className="rounded border p-2"/><button className="rounded bg-[#243A76] p-2 text-white">Publicar</button></form><div className="grid gap-3 md:grid-cols-2">{recursos.materiales.map((m)=><a key={m.id_material} href={m.url} target="_blank" rel="noreferrer" className="rounded-xl border bg-white p-4"><b>{m.titulo}</b><p className="text-sm text-slate-500">{m.descripcion||m.tipo||"Material"}</p></a>)}</div></div>}
            {seccion === "anuncios" && <div className="space-y-4"><form onSubmit={publicarAnuncio} className="grid gap-3 rounded-2xl border bg-white p-5"><h2 className="font-bold">Publicar anuncio</h2><input required name="titulo" placeholder="Título" className="rounded border p-2"/><textarea required name="contenido" placeholder="Contenido" className="rounded border p-2"/><button className="w-fit rounded bg-[#243A76] px-4 py-2 text-white">Publicar</button></form>{recursos.anuncios.map((a)=><article key={a.id_anuncio} className="rounded-xl border bg-white p-4"><b>{a.titulo}</b><p className="mt-1 text-sm text-slate-600">{a.contenido}</p><time className="text-xs text-slate-400">{formatDate(a.fecha)}</time></article>)}</div>}
            {seccion === "estudiantes" && <div className="grid gap-3 md:grid-cols-2">{estudiantesOrdenados.map((m)=>{const e=m.estudiante||{},ini=`${e.nombres?.[0]||""}${e.apellidos?.[0]||""}`;return <article key={m.idMatricula} className="flex gap-3 rounded-2xl border bg-white p-4"><div className="flex h-11 w-11 items-center justify-center rounded-full bg-blue-100 font-bold text-[#243A76]">{ini||"?"}</div><div><b>{e.apellidos} {e.nombres}</b><p className="text-sm text-slate-500">Matrícula: {m.idMatricula}</p><p className="text-sm text-slate-500">Cédula: {e.cedula||"N/D"} · Estado: {m.estado||"N/D"}</p></div></article>})}</div>}

            {seccion === "actividades" && (
              <div className="space-y-4">
                {/* Cabecera con explicación del esquema 70/30 */}
                <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                  <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
                    <div>
                      <h2 className="font-bold text-slate-800">Actividades del curso</h2>
                      <p className="mt-1 text-sm text-slate-500">
                        Trimestre: <span className="font-semibold">{trimestre?.trimestre || "—"}</span>.
                        Esquema de calificación: <span className="font-semibold">70% formativa + 30% sumativa</span>.
                      </p>
                    </div>
                    <button type="button" onClick={() => abrirModalActividad(null)}
                      className="rounded-xl bg-[#243A76] px-4 py-2 text-sm font-bold text-white shadow-sm hover:bg-[#2d4a96]">
                      + Nueva actividad
                    </button>
                  </div>
                  <div className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
                    <div className="rounded-lg bg-indigo-50 px-3 py-2">
                      <p className="text-[10px] font-bold uppercase text-indigo-700">Formativas</p>
                      <p className="text-lg font-bold text-indigo-700">{conteoPorCategoria.FORMATIVA}</p>
                      <p className="text-[10px] text-indigo-600">Peso total: {conteoPorCategoria.pesoFormativa}</p>
                    </div>
                    <div className="rounded-lg bg-violet-50 px-3 py-2">
                      <p className="text-[10px] font-bold uppercase text-violet-700">Sumativas</p>
                      <p className="text-lg font-bold text-violet-700">{conteoPorCategoria.SUMATIVA}</p>
                      <p className="text-[10px] text-violet-600">Peso total: {conteoPorCategoria.pesoSumativa}</p>
                    </div>
                    <div className="rounded-lg bg-slate-100 px-3 py-2 col-span-2">
                      <p className="text-[10px] font-bold uppercase text-slate-500">Total actividades</p>
                      <p className="text-lg font-bold text-slate-700">{actividades.length}</p>
                      <p className="text-[10px] text-slate-500">Sumativas típicas por trimestre: 2 (proyecto + examen).</p>
                    </div>
                  </div>
                  {actividadesMsg.texto && (
                    <div className={`mt-4 rounded-lg px-4 py-2 text-sm ${actividadesMsg.tipo === "ok" ? "bg-emerald-50 text-emerald-700" : "bg-red-50 text-red-700"}`}>
                      {actividadesMsg.texto}
                    </div>
                  )}
                </div>

                {/* Tabla de actividades */}
                <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
                  {actividadesCargando && <p className="py-10 text-center text-sm text-slate-400">Cargando actividades...</p>}
                  {!actividadesCargando && actividades.length === 0 && (
                    <p className="py-10 text-center text-sm text-slate-400">No hay actividades para este trimestre. Crea la primera con "+ Nueva actividad".</p>
                  )}
                  {!actividadesCargando && actividades.length > 0 && (
                    <div className="max-h-[520px] overflow-y-auto">
                      <table className="w-full text-sm">
                        <thead className="sticky top-0 z-10 bg-slate-50 text-[10px] uppercase tracking-wide text-slate-500 shadow-sm">
                          <tr>
                            <th className="px-4 py-3 text-left font-bold">#</th>
                            <th className="px-4 py-3 text-left font-bold">Tipo</th>
                            <th className="px-4 py-3 text-left font-bold">Nombre</th>
                            <th className="px-4 py-3 text-left font-bold">Fecha entrega</th>
                            <th className="px-4 py-3 text-center font-bold">Ponderación</th>
                            <th className="px-4 py-3 text-center font-bold">Nota máx.</th>
                            <th className="px-4 py-3 text-center font-bold">Categoría</th>
                            <th className="px-4 py-3 text-right font-bold">Acciones</th>
                          </tr>
                        </thead>
                        <tbody>
                          {actividades.map((a, index) => {
                            const sumativa = esSumativaPorTipo(a.tipo);
                            return (
                              <tr key={a.idActividad} className="border-t border-slate-100 hover:bg-slate-50">
                                <td className="px-4 py-2 text-xs text-slate-400">{index + 1}</td>
                                <td className="px-4 py-2 text-xs text-slate-600">{labelTipo(a.tipo)}</td>
                                <td className="px-4 py-2">
                                  <p className="font-medium text-slate-700">{a.nombre}</p>
                                  {a.descripcion && <p className="text-[10px] text-slate-400 line-clamp-1">{a.descripcion}</p>}
                                </td>
                                <td className="px-4 py-2 text-xs text-slate-500">{formatDate(a.fechaEntrega)}</td>
                                <td className="px-4 py-2 text-center text-xs text-slate-700">{a.ponderacion}</td>
                                <td className="px-4 py-2 text-center text-xs text-slate-700">{a.notaMaxima}</td>
                                <td className="px-4 py-2 text-center">
                                  <span className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${sumativa ? "bg-violet-100 text-violet-700" : "bg-indigo-100 text-indigo-700"}`}>
                                    {sumativa ? "SUMATIVA" : "FORMATIVA"}
                                  </span>
                                </td>
                                <td className="px-4 py-2">
                                  <div className="flex justify-end gap-2">
                                    <button type="button" onClick={() => abrirModalActividad(a)}
                                      className="rounded-lg border border-slate-200 px-2 py-1 text-[10px] font-semibold text-slate-600 hover:bg-slate-100">Editar</button>
                                    <button type="button" onClick={() => eliminarActividadCurso(a)}
                                      className="rounded-lg border border-red-200 px-2 py-1 text-[10px] font-semibold text-red-600 hover:bg-red-50">Eliminar</button>
                                  </div>
                                </td>
                              </tr>
                            );
                          })}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* Modal de crear/editar actividad */}
            {modalActividad.abierto && (
              <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" onClick={cerrarModalActividad}>
                <div className="w-full max-w-lg rounded-2xl bg-white shadow-xl" onClick={(e) => e.stopPropagation()}>
                  <div className="border-b border-slate-100 px-5 py-4">
                    <h3 className="font-bold text-slate-800">
                      {modalActividad.editando ? "Editar actividad" : "Nueva actividad"}
                    </h3>
                    <p className="mt-1 text-xs text-slate-500">
                      La categoría (formativa/sumativa) se asigna automáticamente según el tipo.
                    </p>
                  </div>
                  <div className="space-y-3 p-5">
                    <div>
                      <label className="text-[10px] font-bold uppercase text-slate-500">Tipo de actividad</label>
                      <select value={modalActividad.form.tipo}
                        onChange={(e) => setFormActividad({ tipo: e.target.value })}
                        className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700">
                        <optgroup label="Formativas (70%)">
                          {TIPOS_ACTIVIDAD.filter((t) => t.categoria === "FORMATIVA").map((t) => (
                            <option key={t.valor} value={t.valor}>{t.label}</option>
                          ))}
                        </optgroup>
                        <optgroup label="Sumativas (30%)">
                          {TIPOS_ACTIVIDAD.filter((t) => t.categoria === "SUMATIVA").map((t) => (
                            <option key={t.valor} value={t.valor}>{t.label}</option>
                          ))}
                        </optgroup>
                      </select>
                      <p className="mt-1 text-[10px] text-slate-400">
                        Esta actividad se contará como <span className="font-semibold">{esSumativaPorTipo(modalActividad.form.tipo) ? "SUMATIVA" : "FORMATIVA"}</span>.
                      </p>
                    </div>
                    <div>
                      <label className="text-[10px] font-bold uppercase text-slate-500">Nombre *</label>
                      <input type="text" value={modalActividad.form.nombre}
                        onChange={(e) => setFormActividad({ nombre: e.target.value })}
                        placeholder="Ej: Tarea 1 - Ecuaciones lineales"
                        className="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm" />
                    </div>
                    <div>
                      <label className="text-[10px] font-bold uppercase text-slate-500">Descripción</label>
                      <textarea rows={3} value={modalActividad.form.descripcion}
                        onChange={(e) => setFormActividad({ descripcion: e.target.value })}
                        placeholder="Instrucciones, criterios, recursos..."
                        className="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm" />
                    </div>
                    <div className="grid grid-cols-3 gap-3">
                      <div>
                        <label className="text-[10px] font-bold uppercase text-slate-500">Fecha entrega *</label>
                        <input type="date" value={modalActividad.form.fechaEntrega}
                          onChange={(e) => setFormActividad({ fechaEntrega: e.target.value })}
                          className="mt-1 w-full rounded-lg border border-slate-200 px-2 py-2 text-sm" />
                      </div>
                      <div>
                        <label className="text-[10px] font-bold uppercase text-slate-500">Ponderación</label>
                        <input type="number" step="0.01" min="0" value={modalActividad.form.ponderacion}
                          onChange={(e) => setFormActividad({ ponderacion: e.target.value })}
                          className="mt-1 w-full rounded-lg border border-slate-200 px-2 py-2 text-sm" />
                      </div>
                      <div>
                        <label className="text-[10px] font-bold uppercase text-slate-500">Nota máx.</label>
                        <input type="number" step="0.01" min="0" value={modalActividad.form.notaMaxima}
                          onChange={(e) => setFormActividad({ notaMaxima: e.target.value })}
                          className="mt-1 w-full rounded-lg border border-slate-200 px-2 py-2 text-sm" />
                      </div>
                    </div>
                  </div>
                  <div className="flex justify-end gap-2 border-t border-slate-100 px-5 py-3">
                    <button type="button" onClick={cerrarModalActividad}
                      className="rounded-lg border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-600 hover:bg-slate-50">
                      Cancelar
                    </button>
                    <button type="button" onClick={guardarActividad} disabled={modalActividad.guardando}
                      className="rounded-lg bg-[#243A76] px-4 py-2 text-sm font-bold text-white hover:bg-[#2d4a96] disabled:opacity-60">
                      {modalActividad.guardando ? "Guardando..." : (modalActividad.editando ? "Actualizar" : "Crear")}
                    </button>
                  </div>
                </div>
              </div>
            )}

            {seccion === "asistencia" && (
              <div className="space-y-4">
                {/* Barra superior */}
                <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                  <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
                    <div>
                      <h2 className="font-bold text-slate-800">Tomar asistencia</h2>
                      <p className="mt-1 text-sm text-slate-500">
                        {estudiantesCurso.length} estudiante(s) matriculado(s) en este curso.
                        Marca el estado de cada uno y presiona <span className="font-semibold">Guardar</span>.
                      </p>
                    </div>
                    <div className="flex flex-wrap items-end gap-3">
                      <div>
                        <label className="block text-[10px] font-bold uppercase tracking-wide text-slate-400">Fecha</label>
                        <input type="date" value={asistenciaFecha}
                          onChange={(e) => setAsistenciaFecha(e.target.value)}
                          className="mt-1 rounded-lg border border-slate-300 px-3 py-1.5 text-sm text-slate-700" />
                      </div>
                      <div>
                        <label className="block text-[10px] font-bold uppercase tracking-wide text-slate-400">Marcar todos</label>
                        <div className="mt-1 flex gap-1">
                          {ESTADOS_ASISTENCIA.map((e) => (
                            <button key={e.valor} type="button" onClick={() => marcarTodos(e.valor)}
                              className={`rounded-lg px-2 py-1.5 text-[10px] font-bold uppercase ${e.color} hover:opacity-80`}
                              title={`Marcar todos como ${e.valor}`}>
                              {e.valor.slice(0, 3)}
                            </button>
                          ))}
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Contadores */}
                  <div className="mt-4 grid grid-cols-2 gap-2 sm:grid-cols-4">
                    {contadorAsistencia.map((c) => (
                      <div key={c.valor} className={`rounded-lg px-3 py-2 text-center ${c.color}`}>
                        <p className="text-[10px] font-bold uppercase">{c.valor}</p>
                        <p className="text-lg font-bold">{c.total}</p>
                      </div>
                    ))}
                  </div>

                  {asistenciaYaExiste && !asistenciaMsg.texto && (
                    <div className="mt-4 rounded-lg border border-amber-200 bg-amber-50 px-4 py-2 text-sm text-amber-800">
                      <span className="font-semibold">Ya existe asistencia registrada</span> para el {asistenciaFecha}.
                      Se cargaron los estados anteriores. Si modificas y guardas, se sobreescribirán.
                    </div>
                  )}
                  {asistenciaMsg.texto && (
                    <div className={`mt-4 rounded-lg px-4 py-2 text-sm ${asistenciaMsg.tipo === "ok" ? "bg-emerald-50 text-emerald-700" : "bg-red-50 text-red-700"}`}>
                      {asistenciaMsg.texto}
                    </div>
                  )}
                </div>

                {/* Tabla de estudiantes */}
                <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
                  {asistenciaCargando && <p className="py-10 text-center text-sm text-slate-400">Cargando estudiantes...</p>}
                  {!asistenciaCargando && estudiantesOrdenados.length === 0 && (
                    <p className="py-10 text-center text-sm text-slate-400">No hay estudiantes matriculados en este curso.</p>
                  )}
                  {!asistenciaCargando && estudiantesOrdenados.length > 0 && (
                    <div className="max-h-[520px] overflow-y-auto">
                    <table className="w-full text-sm">
                      <thead className="sticky top-0 z-10 bg-slate-50 text-[10px] uppercase tracking-wide text-slate-500 shadow-sm">
                        <tr>
                          <th className="px-4 py-3 text-left font-bold">#</th>
                          <th className="px-4 py-3 text-left font-bold">Estudiante</th>
                          <th className="px-4 py-3 text-left font-bold">Cédula</th>
                          <th className="px-4 py-3 text-left font-bold">Estado</th>
                          <th className="px-4 py-3 text-left font-bold">Justificación</th>
                        </tr>
                      </thead>
                      <tbody>
                        {estudiantesOrdenados.map((m, index) => {
                          const est = m.estudiante || {};
                          const estadoActual = asistenciaEstado[m.idMatricula] || "";
                          return (
                            <tr key={m.idMatricula} className="border-t border-slate-100 hover:bg-slate-50">
                              <td className="px-4 py-2 text-xs text-slate-400">{index + 1}</td>
                              <td className="px-4 py-2">
                                <p className="font-medium text-slate-700">{est.apellidos || ""} {est.nombres || ""}</p>
                                <p className="text-[10px] text-slate-400">Matrícula #{m.idMatricula}</p>
                              </td>
                              <td className="px-4 py-2 text-slate-500">{est.cedula || "—"}</td>
                              <td className="px-4 py-2">
                                <select value={estadoActual}
                                  onChange={(e) => setAsistenciaEstado((prev) => ({ ...prev, [m.idMatricula]: e.target.value }))}
                                  className={`rounded-lg border border-slate-200 bg-white px-2 py-1 text-xs font-semibold ${
                                    estadoActual ? "text-slate-700" : "text-slate-400"
                                  }`}>
                                  <option value="">— Sin marcar —</option>
                                  {ESTADOS_ASISTENCIA.map((e) => (
                                    <option key={e.valor} value={e.valor}>{e.valor}</option>
                                  ))}
                                </select>
                              </td>
                              <td className="px-4 py-2">
                                <input type="text" placeholder="—"
                                  value={asistenciaJustif[m.idMatricula] || ""}
                                  onChange={(e) => setAsistenciaJustif((prev) => ({ ...prev, [m.idMatricula]: e.target.value }))}
                                  disabled={estadoActual !== "JUSTIFICADO" && estadoActual !== "AUSENTE"}
                                  className="w-full rounded-lg border border-slate-200 px-2 py-1 text-xs text-slate-600 disabled:bg-slate-50 disabled:text-slate-300" />
                              </td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                    </div>
                  )}
                </div>

                {/* Botón de guardar */}
                <div className="flex justify-end">
                  <button type="button" onClick={guardarAsistencia} disabled={asistenciaGuardando || estudiantesCurso.length === 0}
                    className="rounded-xl bg-[#243A76] px-6 py-2.5 text-sm font-bold text-white shadow-sm hover:bg-[#2d4a96] disabled:opacity-60">
                    {asistenciaGuardando ? "Guardando..." : "Guardar asistencia del día"}
                  </button>
                </div>
              </div>
            )}
            {seccion === "resumen" && (() => {
              const totalSemanas = trimestre?.semanas?.length || 0;
              const semanaActual = trimestre?.semanas?.[numeroSemana - 1];
              const totalRegistros = semanaActual ? resumenSemana(semanaActual).reduce((s, [, items]) => s + items.length, 0) : 0;
              return (
              <>
              {/* Selector de trimestre */}
              <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <h2 className="font-bold text-slate-800">Plan de clases</h2>
                    <p className="mt-1 text-sm text-slate-500">Navega por trimestre y semana. Toda la agenda del curso se muestra aquí.</p>
                  </div>
                  <span className="rounded-full bg-blue-50 px-3 py-1 text-xs font-semibold text-[#243A76]">{agenda.trimestres.length} trimestre(s)</span>
                </div>
                {agenda.trimestres.length > 0 && (
                  <div className="mt-4 flex flex-wrap gap-2">
                    {agenda.trimestres.map((item) => (
                      <button key={item.id_periodo} type="button"
                        onClick={() => { setTrimestreActivo(String(item.id_periodo)); setNumeroSemana(1); }}
                        className={`rounded-xl px-3 py-2 text-xs font-semibold transition ${String(item.id_periodo) === trimestreActivo ? "bg-[#243A76] text-white" : "bg-slate-100 text-slate-500 hover:bg-slate-200"}`}>
                        {item.trimestre}
                      </button>
                    ))}
                  </div>
                )}
                {trimestre && <p className="mt-3 text-xs text-slate-400">{formatDate(trimestre.fecha_inicio)} — {formatDate(trimestre.fecha_fin)}</p>}
              </div>

              {/* Barra de paginación de semanas */}
              {totalSemanas > 0 && (
                <div className="flex items-center justify-center gap-3 rounded-2xl border border-slate-200 bg-white px-4 py-3 shadow-sm">
                  <button type="button" onClick={() => setNumeroSemana((n) => Math.max(1, n - 1))} disabled={numeroSemana <= 1}
                    className="rounded-lg border border-slate-200 px-3 py-1.5 text-sm font-semibold text-slate-500 disabled:opacity-40 hover:bg-slate-50">← Anterior</button>
                  <div className="flex items-center gap-2 text-sm text-slate-600">
                    <span>Semana</span>
                    <input type="number" min={1} max={totalSemanas} value={numeroSemana}
                      onChange={(e) => setNumeroSemana(Math.min(totalSemanas, Math.max(1, Number(e.target.value) || 1)))}
                      className="w-16 rounded-lg border border-slate-200 px-2 py-1 text-center text-sm font-semibold text-[#243A76]" />
                    <span>de {totalSemanas}</span>
                  </div>
                  <button type="button" onClick={() => setNumeroSemana((n) => Math.min(totalSemanas, n + 1))} disabled={numeroSemana >= totalSemanas}
                    className="rounded-lg border border-slate-200 px-3 py-1.5 text-sm font-semibold text-slate-500 disabled:opacity-40 hover:bg-slate-50">Siguiente →</button>
                </div>
              )}

              {/* Contenido de la semana seleccionada */}
              {semanaActual && (
                <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
                  <header className="flex items-center justify-between gap-4 border-b border-slate-100 bg-slate-50/60 px-5 py-4">
                    <div className="flex items-center gap-3">
                      <div className="flex h-12 w-12 flex-col items-center justify-center rounded-xl bg-[#243A76] text-white">
                        <span className="text-lg font-bold leading-none">{semanaActual.numero}</span>
                        <span className="text-[9px] uppercase tracking-wider opacity-80">Sem</span>
                      </div>
                      <div>
                        <p className="text-sm font-bold uppercase tracking-wide text-slate-700">Semana {semanaActual.numero} — {trimestre.trimestre}</p>
                        <p className="mt-0.5 text-xs text-slate-500">{formatDate(semanaActual.fecha_inicio)} — {formatDate(semanaActual.fecha_fin)}</p>
                      </div>
                    </div>
                    <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-500">{totalRegistros} registros</span>
                  </header>

                  <div className="space-y-3 p-5">
                    {resumenSemana(semanaActual).map(([etiqueta, items, color, fondo]) => {
                      const abierto = !!bloquesAbiertos[etiqueta];
                      const esAsistencia = etiqueta === "Asistencias";
                      return (
                        <div key={etiqueta} className={`rounded-xl ${fondo} overflow-hidden`}>
                          {/* Encabezado clickable */}
                          <button type="button" onClick={() => toggleBloque(etiqueta)}
                            className="flex w-full items-center justify-between gap-2 px-4 py-3 text-left hover:bg-white/40 transition">
                            <div className="flex items-center gap-2">
                              <span className={`text-xs font-bold uppercase tracking-wide ${color}`}>{etiqueta}</span>
                              <span className={`rounded-full bg-white px-2 py-0.5 text-[10px] font-bold ${color}`}>{items.length}</span>
                            </div>
                            <div className="flex items-center gap-2">
                              <span
                                role="button"
                                tabIndex={0}
                                onClick={(e) => { e.stopPropagation(); setSeccion(etiqueta.toLowerCase()); }}
                                onKeyDown={(e) => { if (e.key === "Enter") { e.stopPropagation(); setSeccion(etiqueta.toLowerCase()); } }}
                                className={`rounded-lg bg-white/90 px-2.5 py-1 text-[10px] font-bold uppercase ${color} hover:bg-white cursor-pointer`}
                              >+ Agregar</span>
                              <svg className={`w-4 h-4 ${color} transition-transform ${abierto ? "rotate-180" : ""}`}
                                fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                              </svg>
                            </div>
                          </button>

                          {/* Contenido expandido */}
                          {abierto && (
                            <div className="border-t border-white/60 bg-white/40 px-4 py-4">
                              {items.length === 0 && (
                                <p className="rounded-lg bg-white/60 py-4 text-center text-xs text-slate-500">
                                  Sin registros para esta semana. Usa "+ Agregar" para crear uno nuevo.
                                </p>
                              )}

                              {items.length > 0 && esAsistencia && (
                                <div className="overflow-hidden rounded-lg border border-white bg-white">
                                  <table className="w-full text-xs">
                                    <thead className={`${color} text-[10px] uppercase tracking-wide`}>
                                      <tr className="border-b border-slate-100">
                                        <th className="px-3 py-2 text-left font-bold">Matrícula</th>
                                        <th className="px-3 py-2 text-left font-bold">Fecha</th>
                                        <th className="px-3 py-2 text-left font-bold">Estado</th>
                                        <th className="px-3 py-2 text-left font-bold">Justificación</th>
                                      </tr>
                                    </thead>
                                    <tbody>
                                      {items.slice(0, 20).map((item) => (
                                        <tr key={`asi-${item.id_asistencia}`} className="border-b border-slate-50 last:border-b-0 hover:bg-slate-50">
                                          <td className="px-3 py-2 font-medium text-slate-700">#{item.id_matricula ?? "—"}</td>
                                          <td className="px-3 py-2 text-slate-500">{formatDate(item.fecha)}</td>
                                          <td className="px-3 py-2">
                                            <span className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${
                                              item.estado === "PRESENTE" ? "bg-emerald-100 text-emerald-700" :
                                              item.estado === "AUSENTE" ? "bg-red-100 text-red-700" :
                                              item.estado === "JUSTIFICADO" ? "bg-amber-100 text-amber-700" :
                                              item.estado === "ATRASO" ? "bg-orange-100 text-orange-700" :
                                              "bg-slate-100 text-slate-500"
                                            }`}>{item.estado || "—"}</span>
                                          </td>
                                          <td className="px-3 py-2 text-slate-500">{item.justificacion || "—"}</td>
                                        </tr>
                                      ))}
                                      {items.length > 20 && (
                                        <tr><td colSpan={4} className="px-3 py-2 text-center text-[10px] text-slate-400">+ {items.length - 20} registros más</td></tr>
                                      )}
                                    </tbody>
                                  </table>
                                </div>
                              )}

                              {items.length > 0 && !esAsistencia && (
                                <ul className="grid gap-2 sm:grid-cols-2">
                                  {items.slice(0, 8).map((item) => (
                                    <li key={`${etiqueta}-${item.id_actividad || item.id_material || item.id_anuncio || item.id_calificacion}`}
                                        className="rounded-lg bg-white px-3 py-2 text-xs text-slate-600">
                                      <p className="font-medium text-slate-700">{item.nombre || item.titulo || item.actividad || item.estado}</p>
                                      <p className="mt-0.5 text-[10px] text-slate-400">{formatDate(item.fecha_entrega || item.fecha || item.fecha_registro)}{item.nota != null ? ` · Nota ${item.nota}` : ""}</p>
                                    </li>
                                  ))}
                                  {items.length > 8 && <li className="rounded-lg bg-white/70 px-3 py-2 text-center text-[10px] text-slate-400">+ {items.length - 8} registros</li>}
                                </ul>
                              )}
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>
                </section>
              )}
              </>
              );
            })()}
            </main>

            <aside className="space-y-5">
              <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
                <div className="flex items-center justify-between"><h2 className="font-bold text-slate-800">Pendientes</h2><span className="rounded-full bg-rose-50 px-2 py-1 text-[10px] font-bold text-rose-700">{agenda.pendientes.length}</span></div>
                <div className="mt-4 space-y-3">
                  {agenda.pendientes.length === 0 && <p className="text-sm text-slate-400">No hay actividades próximas ni anuncios registrados.</p>}
                  {agenda.pendientes.map((item, index) => <div key={`${item.tipo}-${item.titulo}-${index}`} className="rounded-xl bg-slate-50 p-3"><p className="text-[10px] font-bold uppercase tracking-wide text-[#2d4a96]">{etiquetaTipo(item.tipo)}</p><p className="mt-1 text-sm font-semibold text-slate-700">{item.titulo}</p><p className="mt-1 text-xs text-slate-400">{item.dias_restantes == null ? `Publicado ${formatDate(item.fecha)}` : item.dias_restantes === 0 ? "Finaliza hoy" : item.dias_restantes === 1 ? "Finaliza mañana" : `Finaliza en ${item.dias_restantes} días`}</p></div>)}
                </div>
              </section>
              <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
                <div className="flex items-center justify-between"><h2 className="font-bold text-slate-800">Anuncios</h2><button type="button" onClick={() => navigate("/anuncios")} className="text-xs font-semibold text-[#243A76]">Ver todos</button></div>
                <div className="mt-4 space-y-3">{anuncios.length === 0 && <p className="text-sm text-slate-400">Aún no hay anuncios para este curso.</p>}{anuncios.map((anuncio) => <article key={anuncio.id_anuncio} className="border-l-2 border-[#243A76] pl-3"><p className="text-sm font-semibold text-slate-700">{anuncio.titulo || "Anuncio"}</p><p className="mt-1 line-clamp-2 text-xs text-slate-500">{anuncio.contenido}</p><time className="mt-1 block text-[10px] text-slate-400">{formatDate(anuncio.fecha)}</time></article>)}</div>
              </section>
            </aside>
          </div>
        </div>
      )}
    </Layout>
  );
}
