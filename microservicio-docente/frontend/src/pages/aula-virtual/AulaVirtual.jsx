import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Layout from "../../components/Layout";
import { getAulaVirtualResumen, getMisAsignaciones } from "../../services/api";

const ACCENTS = ["#243A76", "#0f766e", "#7c3aed", "#b45309", "#be123c", "#0369a1"];

const formatDate = (value) => value
  ? new Date(`${value}T00:00:00`).toLocaleDateString("es-EC", { day: "2-digit", month: "short", year: "numeric" })
  : "Sin fechas registradas";

const nombreCurso = (asignacion) => asignacion?.asignatura?.nombre || "Asignatura";

const nombreGrado = (asignacion) => asignacion?.grado?.nombre || "Grado sin registrar";

const valorHorario = (asignacion, sigla) => {
  const buscar = (objeto) => Object.entries(objeto || {}).find(([clave, valor]) => (
    clave.toUpperCase() === sigla && valor !== null && valor !== undefined && valor !== ""
  ))?.[1];
  return buscar(asignacion) ?? buscar(asignacion?.asignatura);
};

function Icono({ tipo }) {
  const paths = {
    anuncios: "M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9",
    asistencia: "M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z",
    calificaciones: "M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253",
    materiales: "M12 4v16m8-8H4m16 0a8 8 0 11-16 0 8 8 0 0116 0",
  };
  return <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d={paths[tipo]} /></svg>;
}

const ICONO_MENU = {
  cursos: "M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253",
  calendario: "M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z",
  pendientes: "M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4",
  asistencia: "M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z",
  notas: "M9 17v-2a2 2 0 012-2h2a2 2 0 012 2v2m-6 0h6m-6 0H5a2 2 0 01-2-2V5a2 2 0 012-2h14a2 2 0 012 2v10a2 2 0 01-2 2h-4",
};

const IconoSvg = ({ path }) => (
  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d={path} />
  </svg>
);

export default function AulaVirtual() {
  const [asignaciones, setAsignaciones] = useState([]);
  const [resumenes, setResumenes] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [seccion, setSeccion] = useState("cursos");
  const navigate = useNavigate();
  const username = localStorage.getItem("username") || "Docente";

  const menuItems = [
    { id: "cursos",      label: "Mis cursos",              icon: <IconoSvg path={ICONO_MENU.cursos} /> },
    { id: "calendario",  label: "Calendario académico",    icon: <IconoSvg path={ICONO_MENU.calendario} /> },
    { id: "pendientes",  label: "Actividades pendientes",  icon: <IconoSvg path={ICONO_MENU.pendientes} /> },
    { id: "asistencia",  label: "Asistencia rápida",       icon: <IconoSvg path={ICONO_MENU.asistencia} /> },
    { id: "notas",       label: "Calificaciones",          icon: <IconoSvg path={ICONO_MENU.notas} /> },
  ];

  useEffect(() => {
    const cargarCursos = async () => {
      setLoading(true);
      setError("");
      try {
        const asignacionesResponse = await getMisAsignaciones();
        const cursos = asignacionesResponse.data || [];
        setAsignaciones(cursos);
        if (!cursos.length) return;
        try {
          const resumenResponse = await getAulaVirtualResumen(cursos.map((curso) => curso.idAsignacion));
          const porAsignacion = (resumenResponse.data?.cursos || []).reduce((acumulado, curso) => {
            acumulado[curso.id_asignacion] = curso;
            return acumulado;
          }, {});
          setResumenes(porAsignacion);
        } catch (resumenErr) {
          // Si el microservicio secundario falla en resumen, no bloquea la vista de asignaciones
        }
      } catch (requestError) {
        setError(requestError.response?.status === 401
          ? "Tu sesión expiró. Inicia sesión nuevamente."
          : "No se pudieron cargar los cursos asignados.");
      } finally {
        setLoading(false);
      }
    };
    cargarCursos();
  }, []);

  const titulos = {
    cursos:      { titulo: "Aula Virtual",              subtitulo: "Selecciona uno de tus cursos para revisar su agenda académica." },
    calendario:  { titulo: "Calendario académico",      subtitulo: "Vista global de los trimestres y fechas clave del año lectivo." },
    pendientes:  { titulo: "Actividades pendientes",    subtitulo: "Tareas y exámenes por calificar, filtrables por curso y estudiante." },
    asistencia:  { titulo: "Asistencia rápida",         subtitulo: "Registra la asistencia del día sin entrar a cada curso." },
    notas:       { titulo: "Calificaciones",            subtitulo: "Consolidado de promedios por curso y estudiante." },
  };
  const cabecera = titulos[seccion] || titulos.cursos;

  return (
    <Layout
      breadcrumb={["Inicio", "Aula Virtual"]}
      sidebarTitle="AULA VIRTUAL"
      menuItems={menuItems}
      seccion={seccion}
      onSeccionChange={setSeccion}
    >
      <section className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.18em] text-[#2d4a96]">Portal docente</p>
          <h1 className="mt-1 text-2xl font-bold text-slate-800">{cabecera.titulo}</h1>
          <p className="mt-1 text-sm text-slate-500">{cabecera.subtitulo}</p>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white px-4 py-3 text-right shadow-sm">
          <p className="text-xs text-slate-400">Docente titular</p>
          <p className="text-sm font-semibold capitalize text-slate-700">{username}</p>
        </div>
      </section>

      {seccion !== "cursos" && (
        <div className="rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center shadow-sm">
          <p className="text-sm font-semibold text-slate-500">Próximamente</p>
          <p className="mt-1 text-xs text-slate-400">Esta sección se habilitará en la siguiente iteración del aula virtual.</p>
        </div>
      )}

      {seccion === "cursos" && (
      <>


      {loading && (
        <div className="grid grid-cols-1 gap-5 md:grid-cols-2 xl:grid-cols-3">
          {[0, 1, 2].map((item) => <div key={item} className="h-80 animate-pulse rounded-2xl border border-slate-200 bg-white" />)}
        </div>
      )}

      {!loading && error && (
        <div className="rounded-2xl border border-red-200 bg-red-50 p-6 text-sm text-red-700">
          <p className="font-semibold">No fue posible cargar el Aula Virtual.</p>
          <p className="mt-1">{error}</p>
        </div>
      )}

      {!loading && !error && asignaciones.length === 0 && (
        <div className="rounded-2xl border border-dashed border-slate-300 bg-white p-12 text-center shadow-sm">
          <svg className="mx-auto h-10 w-10 text-slate-300" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18.477 16.5 18c-1.746 0-3.332.477-4.5 1.253" /></svg>
          <h2 className="mt-3 font-semibold text-slate-700">No tienes cursos asignados</h2>
          <p className="mt-1 text-sm text-slate-400">Cuando el SGA Principal registre una asignación para tu usuario, aparecerá aquí.</p>
        </div>
      )}

      {!loading && !error && asignaciones.length > 0 && (
        <div className="grid grid-cols-1 gap-5 md:grid-cols-2 xl:grid-cols-3">
          {asignaciones.map((asignacion, index) => {
            const resumen = resumenes[asignacion.idAsignacion] || {};
            const paralelo = asignacion.paralelo?.letra || "—";
            const horas = ["HAD", "HPS", "HAS"].map((sigla) => ({ sigla, valor: valorHorario(asignacion, sigla) }))
              .filter((item) => item.valor !== undefined);
            const color = ACCENTS[index % ACCENTS.length];
            return (
              <article
                key={asignacion.idAsignacion}
                className="group overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm transition hover:-translate-y-0.5 hover:shadow-lg"
              >
                <button
                  type="button"
                  onClick={() => navigate(`/aula-virtual/${asignacion.idAsignacion}`)}
                  className="w-full text-left"
                >
                  <div className="p-5 text-white" style={{ backgroundColor: color }}>
                    <div className="flex items-start justify-between gap-3">
                      <span className="rounded-full bg-white/20 px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider">Paralelo {paralelo}</span>
                      <span className="rounded-full bg-emerald-500/90 px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider">Activo</span>
                    </div>
                    <h2 className="mt-5 text-xl font-bold leading-tight">{nombreCurso(asignacion)}</h2>
                    <p className="mt-1 text-sm text-white/80">{nombreGrado(asignacion)} · Paralelo {paralelo}</p>
                  </div>
                  <div className="space-y-4 p-5">
                    <div className="grid grid-cols-2 gap-3">
                      <div className="rounded-xl bg-slate-50 p-3">
                        <p className="text-[10px] font-bold uppercase tracking-wide text-slate-400">Promedio</p>
                        <p className="mt-1 text-lg font-bold text-slate-700">{resumen.promedio_curso == null ? "—" : Number(resumen.promedio_curso).toFixed(2)}</p>
                        <p className="text-[10px] text-slate-400">Notas registradas</p>
                      </div>
                      <div className="rounded-xl bg-slate-50 p-3">
                        <p className="text-[10px] font-bold uppercase tracking-wide text-slate-400">Asistencia</p>
                        <p className="mt-1 text-lg font-bold text-slate-700">{resumen.porcentaje_asistencia == null ? "—" : `${Number(resumen.porcentaje_asistencia).toFixed(1)}%`}</p>
                        <p className="text-[10px] text-slate-400">P + atrasos / total</p>
                      </div>
                    </div>
                    <div className="flex items-center justify-between gap-3 text-xs">
                      <span className={`rounded-full px-2.5 py-1 font-semibold ${resumen.indicador_minimos === "DATOS_DISPONIBLES" ? "bg-emerald-50 text-emerald-700" : "bg-amber-50 text-amber-700"}`}>
                        {resumen.indicador_minimos === "DATOS_DISPONIBLES" ? "Mínimos: datos disponibles" : "Mínimos: faltan registros"}
                      </span>
                      <span className="text-right text-slate-400">{asignacion.cantidadEstudiantes ?? "—"} estudiantes</span>
                    </div>
                    <div className="border-t border-slate-100 pt-3 text-xs text-slate-500">
                      <p><span className="font-semibold text-slate-600">Docente titular:</span> {username}</p>
                      <p className="mt-1">{formatDate(resumen.fecha_inicio)} — {formatDate(resumen.fecha_fin)}</p>
                      {horas.length > 0 && <div className="mt-3 flex flex-wrap gap-1.5">{horas.map((hora) => <span key={hora.sigla} className="rounded-md bg-slate-100 px-2 py-1 text-[10px] font-bold text-slate-600">{hora.sigla} {hora.valor}</span>)}</div>}
                    </div>
                  </div>
                </button>
                <div className="grid grid-cols-4 border-t border-slate-100 bg-slate-50/70 text-slate-500">
                  {[
                    ["anuncios", "Anuncios", "/anuncios"],
                    ["asistencia", "Asistencia", "/asistencia"],
                    ["calificaciones", "Notas", "/calificaciones"],
                    ["materiales", "Material", "/material"],
                  ].map(([tipo, texto, ruta]) => (
                    <button key={tipo} type="button" onClick={() => navigate(ruta)} className="flex min-h-14 flex-col items-center justify-center gap-1 border-r border-slate-100 text-[10px] hover:bg-white hover:text-[#243A76] last:border-r-0">
                      <Icono tipo={tipo} />
                      {texto}
                    </button>
                  ))}
                </div>
              </article>
            );
          })}
        </div>
      )}
      </>
      )}
    </Layout>
  );
}
