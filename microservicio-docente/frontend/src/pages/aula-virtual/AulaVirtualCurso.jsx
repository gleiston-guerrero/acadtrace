import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import Layout from "../../components/Layout";
import { getAulaVirtualSemanas, getMisAsignaciones } from "../../services/api";

const PRIMARY = "#243A76";

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
        const agendaResponse = await getAulaVirtualSemanas(idAsignacion);
        const data = agendaResponse.data || { trimestres: [], pendientes: [] };
        setAgenda(data);
        if (data.trimestres?.[0]) {
          setTrimestreActivo(String(data.trimestres[0].id_periodo));
          setSemanaAbierta(`${data.trimestres[0].id_periodo}-1`);
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

  const trimestre = useMemo(
    () => agenda.trimestres.find((item) => String(item.id_periodo) === trimestreActivo),
    [agenda.trimestres, trimestreActivo],
  );
  const anuncios = useMemo(() => agenda.trimestres.flatMap((item) => item.semanas.flatMap((semana) => semana.anuncios))
    .sort((a, b) => new Date(b.fecha) - new Date(a.fecha)).slice(0, 5), [agenda.trimestres]);

  const menuCurso = [
    { id: "calendario", label: "Calendario", action: () => document.getElementById("calendario")?.scrollIntoView({ behavior: "smooth" }) },
    { id: "materias", label: "Mis Materias", action: () => navigate("/aula-virtual") },
    { id: "examenes", label: "Horarios de Examen", disabled: true },
    { id: "tutorias", label: "Tutorías", disabled: true },
    { id: "calificaciones", label: "Mis Calificaciones", action: () => navigate("/calificaciones") },
    { id: "asistencias", label: "Asistencias", action: () => navigate("/asistencia") },
  ];

  return (
    <Layout breadcrumb={["Inicio", "Aula Virtual", curso?.asignatura?.nombre || "Curso"]} sidebarTitle="CURSO">
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

          <div className="grid gap-5 xl:grid-cols-[210px_minmax(0,1fr)_300px]">
            <aside className="rounded-2xl border border-slate-200 bg-white p-3 shadow-sm">
              <p className="px-2 pb-2 text-[10px] font-bold uppercase tracking-[0.16em] text-slate-400">Navegación del curso</p>
              <nav className="space-y-1">
                {menuCurso.map((item) => (
                  <button key={item.id} type="button" disabled={item.disabled} onClick={item.action}
                    className={`flex w-full items-center gap-2 rounded-xl px-3 py-2.5 text-left text-sm transition ${item.disabled ? "cursor-not-allowed text-slate-300" : "text-slate-600 hover:bg-blue-50 hover:text-[#243A76]"}`}>
                    <span className="h-1.5 w-1.5 rounded-full" style={{ backgroundColor: item.disabled ? "#cbd5e1" : PRIMARY }} />
                    <span>{item.label}</span>
                    {item.disabled && <span className="ml-auto text-[9px] uppercase">Próximamente</span>}
                  </button>
                ))}
              </nav>
            </aside>

            <main id="calendario" className="min-w-0 space-y-4">
              <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <h2 className="font-bold text-slate-800">Calendario académico</h2>
                    <p className="mt-1 text-sm text-slate-500">Semanas calculadas dinámicamente de lunes a viernes para cada trimestre.</p>
                  </div>
                  <span className="rounded-full bg-blue-50 px-3 py-1 text-xs font-semibold text-[#243A76]">{agenda.trimestres.length} trimestre(s)</span>
                </div>
                {agenda.trimestres.length === 0 ? (
                  <p className="py-10 text-center text-sm text-slate-400">No hay períodos trimestrales activos para mostrar.</p>
                ) : (
                  <div className="mt-5 flex flex-wrap gap-2 border-b border-slate-100 pb-4">
                    {agenda.trimestres.map((item) => (
                      <button key={item.id_periodo} type="button" onClick={() => { setTrimestreActivo(String(item.id_periodo)); setSemanaAbierta(`${item.id_periodo}-1`); }}
                        className={`rounded-xl px-3 py-2 text-xs font-semibold transition ${String(item.id_periodo) === trimestreActivo ? "bg-[#243A76] text-white" : "bg-slate-100 text-slate-500 hover:bg-slate-200"}`}>
                        {item.trimestre}
                      </button>
                    ))}
                  </div>
                )}
                {trimestre && <p className="mt-4 text-xs text-slate-400">{formatDate(trimestre.fecha_inicio)} — {formatDate(trimestre.fecha_fin)}</p>}
              </div>

              {trimestre?.semanas.map((semana) => {
                const clave = `${trimestre.id_periodo}-${semana.numero}`;
                const abierta = semanaAbierta === clave;
                const total = resumenSemana(semana).reduce((suma, [, items]) => suma + items.length, 0);
                return (
                  <section key={clave} className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
                    <button type="button" onClick={() => setSemanaAbierta(abierta ? null : clave)} className="flex w-full items-center justify-between gap-3 p-4 text-left hover:bg-slate-50">
                      <div><p className="font-semibold text-slate-700">Semana {semana.numero}</p><p className="mt-0.5 text-xs text-slate-400">{formatDate(semana.fecha_inicio)} — {formatDate(semana.fecha_fin)}</p></div>
                      <div className="flex items-center gap-3"><span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-500">{total} registros</span><span className="text-slate-400">{abierta ? "−" : "+"}</span></div>
                    </button>
                    {abierta && <div className="grid gap-3 border-t border-slate-100 p-4 sm:grid-cols-2">{resumenSemana(semana).map(([etiqueta, items, color, fondo]) => <ItemSemana key={etiqueta} etiqueta={etiqueta} items={items} color={color} fondo={fondo} />)}{total === 0 && <p className="sm:col-span-2 rounded-xl bg-slate-50 py-5 text-center text-sm text-slate-400">No hay registros académicos en esta semana.</p>}</div>}
                  </section>
                );
              })}
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
