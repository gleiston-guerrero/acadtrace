import { useEffect, useMemo, useState } from "react";
import Layout from "../../components/Layout";
import { createMaterial, getMateriales, getMisAsignaciones } from "../../services/api";

const emptyForm = { titulo: "", descripcion: "", tipo: "", url: "", archivo: null };

const fieldClass = "mt-1.5 w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm text-slate-700 outline-none transition placeholder:text-slate-400 focus:border-[#243A76] focus:bg-white focus:ring-2 focus:ring-[#243A76]/15";

const nombreAsignacion = (item) => `${item.asignatura?.nombre || "Asignatura"} — ${item.grado?.nombre || ""}`;

const formatDate = (value) => value
  ? new Date(value).toLocaleDateString("es-EC", { day: "2-digit", month: "short", year: "numeric" })
  : "Fecha no disponible";

const formatSize = (bytes) => {
  if (!bytes) return null;
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${Math.ceil(bytes / 1024)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
};

function Icon({ name, className = "w-5 h-5", style }) {
  const paths = {
    book: "M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18.477 16.5 18c-1.746 0-3.332.477-4.5 1.253",
    folder: "M3 7a2 2 0 012-2h4l2 2h8a2 2 0 012 2v8a2 2 0 01-2 2H5a2 2 0 01-2-2V7z",
    upload: "M7 16a4 4 0 01-.88-7.903A5.002 5.002 0 0115.9 6L16 6a5 5 0 011 9.9M12 12l-3 3m3-3l3 3m-3-3v9",
    link: "M13.828 10.172a4 4 0 010 5.656l-2.828 2.828a4 4 0 01-5.656-5.656l1.5-1.5m1.5-1.5a4 4 0 015.656 0l.172.172m-4.5 4.5l4.5-4.5",
    file: "M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8zM14 2v6h6M8 13h8m-8 4h8",
    pdf: "M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8zM14 2v6h6M8 13h2m2 0h2m-6 4h8",
    video: "M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14m0-4v4m0-4H5a2 2 0 00-2 2v6a2 2 0 002 2h10a2 2 0 002-2v-6a2 2 0 00-2-2z",
    check: "M5 13l4 4L19 7",
    external: "M14 3h7m0 0v7m0-7L10 14M19 13v6a2 2 0 01-2 2H5a2 2 0 01-2-2V7a2 2 0 012-2h6",
    chevron: "M19 9l-7 7-7-7",
  };
  return <svg className={className} style={style} fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d={paths[name]} /></svg>;
}

function ResourceIcon({ type, url }) {
  const normalizedType = String(type || "").toLowerCase();
  const normalizedUrl = String(url || "").toLowerCase();
  const icon = normalizedType.includes("pdf") || normalizedUrl.includes(".pdf") ? "pdf"
    : normalizedType.includes("video") ? "video"
      : normalizedType.includes("link") || /^https?:/.test(normalizedUrl) ? "link"
        : "file";
  const colors = {
    pdf: "bg-rose-50 text-rose-700",
    video: "bg-violet-50 text-violet-700",
    link: "bg-cyan-50 text-cyan-700",
    file: "bg-blue-50 text-[#243A76]",
  };
  return <span className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-xl ${colors[icon]}`}><Icon name={icon} /></span>;
}

export default function Material() {
  const [asignaciones, setAsignaciones] = useState([]);
  const [asignacion, setAsignacion] = useState("");
  const [materiales, setMateriales] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [loadingCursos, setLoadingCursos] = useState(true);
  const [loadingMateriales, setLoadingMateriales] = useState(false);
  const [subiendo, setSubiendo] = useState(false);
  const [error, setError] = useState("");
  const [exito, setExito] = useState("");

  const cursoSeleccionado = useMemo(
    () => asignaciones.find((item) => String(item.idAsignacion) === String(asignacion)),
    [asignaciones, asignacion],
  );

  const cargar = async (idAsignacion) => {
    setLoadingMateriales(true);
    try {
      const response = await getMateriales(idAsignacion);
      setMateriales(response.data || []);
    } finally {
      setLoadingMateriales(false);
    }
  };

  useEffect(() => {
    const cargarAsignaciones = async () => {
      setLoadingCursos(true);
      try {
        const response = await getMisAsignaciones();
        const data = response.data || [];
        setAsignaciones(data);
        if (data[0]) setAsignacion(String(data[0].idAsignacion));
      } catch {
        setError("No se pudieron cargar las asignaciones.");
      } finally {
        setLoadingCursos(false);
      }
    };
    cargarAsignaciones();
  }, []);

  useEffect(() => {
    if (!asignacion) {
      setMateriales([]);
      return;
    }
    setError("");
    cargar(asignacion).catch(() => setError("No se pudo cargar el material."));
  }, [asignacion]);

  const subir = async (event) => {
    event.preventDefault();
    const data = new FormData();
    data.append("id_asignacion", asignacion);
    data.append("titulo", form.titulo);
    data.append("descripcion", form.descripcion);
    data.append("tipo", form.tipo);
    if (form.archivo) data.append("archivo", form.archivo);
    if (form.url) data.append("url", form.url);

    setSubiendo(true);
    setError("");
    setExito("");
    try {
      await createMaterial(data);
      setForm(emptyForm);
      event.currentTarget.reset();
      await cargar(asignacion);
      setExito("Material publicado correctamente para este curso.");
    } catch {
      setError("Adjunta un archivo o proporciona un enlace válido.");
    } finally {
      setSubiendo(false);
    }
  };

  return (
    <Layout breadcrumb={["Inicio", "Material"]} sidebarTitle="AULA VIRTUAL">
      <div className="mx-auto w-full max-w-7xl pb-5">
        <section className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <div className="flex items-start gap-3">
            <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-[#243A76] text-white shadow-sm"><Icon name="book" className="h-6 w-6" /></span>
            <div>
              <p className="text-xs font-bold uppercase tracking-[0.18em] text-[#2d4a96]">Material de clase</p>
              <h1 className="mt-1 text-2xl font-bold text-slate-800">Material de clase</h1>
              <p className="mt-1 text-sm text-slate-500">Comparte recursos, documentos y enlaces con tus estudiantes.</p>
            </div>
          </div>
          {!loadingCursos && <span className="rounded-full border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-500 shadow-sm">{materiales.length} publicado{materiales.length === 1 ? "" : "s"}</span>}
        </section>

        {error && <div className="mb-5 flex items-start gap-3 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"><span className="mt-0.5 font-bold">!</span><p>{error}</p></div>}
        {exito && <div className="mb-5 flex items-start gap-3 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700"><Icon name="check" className="mt-0.5 h-4 w-4 shrink-0" /><p>{exito}</p></div>}

        <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <div className="flex items-start gap-3">
            <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-blue-50 text-[#243A76]"><Icon name="folder" /></span>
            <div>
              <h2 className="font-semibold text-slate-800">Selecciona un curso</h2>
              <p className="mt-0.5 text-sm text-slate-500">Elige la materia donde deseas publicar o consultar materiales.</p>
            </div>
          </div>
          <div className="mt-4 max-w-3xl">
            <div className="relative">
              <select value={asignacion} onChange={(event) => { setAsignacion(event.target.value); setExito(""); }} disabled={loadingCursos || asignaciones.length === 0} className={`${fieldClass} appearance-none pr-10 disabled:cursor-not-allowed disabled:bg-slate-100`}>
                {loadingCursos && <option value="">Cargando cursos...</option>}
                {!loadingCursos && asignaciones.length === 0 && <option value="">No hay cursos asignados</option>}
                {asignaciones.map((item) => <option key={item.idAsignacion} value={item.idAsignacion}>{nombreAsignacion(item)}</option>)}
              </select>
              <span className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-slate-400"><Icon name="chevron" className="h-4 w-4" /></span>
            </div>
            {cursoSeleccionado && <div className="mt-3 flex flex-wrap gap-2 text-xs"><span className="rounded-full bg-slate-100 px-3 py-1.5 font-semibold text-slate-600">{cursoSeleccionado.asignatura?.nombre || "Asignatura"}</span>{cursoSeleccionado.grado?.nombre && <span className="rounded-full bg-blue-50 px-3 py-1.5 font-semibold text-[#243A76]">{cursoSeleccionado.grado.nombre}</span>}{cursoSeleccionado.paralelo?.letra && <span className="rounded-full bg-emerald-50 px-3 py-1.5 font-semibold text-emerald-700">Paralelo {cursoSeleccionado.paralelo.letra}</span>}</div>}
          </div>
        </section>

        {!loadingCursos && asignaciones.length === 0 && (
          <section className="mx-auto mt-6 max-w-[400px] rounded-2xl border border-slate-200 bg-white p-8 text-center shadow-sm">
            <span className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-[#EEF3FF] text-[#243A76]" style={{ flex: "0 0 64px" }}>
              <Icon name="folder" className="h-8 w-8 flex-none" style={{ width: "32px", height: "32px" }} />
            </span>
            <h2 className="mt-4 text-base font-semibold text-slate-700">No tienes cursos asignados</h2>
            <p className="mx-auto mt-2 max-w-xs text-sm leading-6 text-slate-400">Cuando tengas materias asignadas, podrás publicar materiales aquí.</p>
          </section>
        )}

        {asignacion && <div className="mt-6 grid gap-6 xl:grid-cols-[minmax(0,0.9fr)_minmax(0,1.1fr)]">
          <form onSubmit={subir} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
            <div className="flex items-start gap-3 border-b border-slate-100 pb-5">
              <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-[#243A76] text-white"><Icon name="upload" /></span>
              <div><h2 className="font-semibold text-slate-800">Nuevo material</h2><p className="mt-0.5 text-sm text-slate-500">Sube un archivo o comparte un enlace con el curso.</p></div>
            </div>
            <div className="mt-5 grid gap-4 sm:grid-cols-2">
              <label className="block text-sm font-medium text-slate-600">Título<input required value={form.titulo} onChange={(event) => setForm({ ...form, titulo: event.target.value })} placeholder="Ej. Guía de la unidad 2" className={fieldClass} /></label>
              <label className="block text-sm font-medium text-slate-600">Tipo<input value={form.tipo} onChange={(event) => setForm({ ...form, tipo: event.target.value })} placeholder="PDF, video o enlace" className={fieldClass} /></label>
              <label className="block text-sm font-medium text-slate-600 sm:col-span-2">Descripción<textarea rows="3" value={form.descripcion} onChange={(event) => setForm({ ...form, descripcion: event.target.value })} placeholder="Explica brevemente cómo usar este recurso." className={`${fieldClass} resize-y`} /></label>
            </div>
            <div className="mt-5 grid gap-4 sm:grid-cols-2">
              <div className="rounded-2xl border border-dashed border-slate-300 bg-slate-50 p-4 transition hover:border-[#243A76]/50 hover:bg-blue-50/40">
                <div className="flex items-start gap-3"><span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-white text-[#243A76] shadow-sm"><Icon name="upload" className="h-4 w-4" /></span><div><p className="text-sm font-semibold text-slate-700">Archivo</p><p className="mt-0.5 text-xs text-slate-400">PDF u otro recurso permitido.</p></div></div>
                <label className="mt-4 flex cursor-pointer items-center justify-center rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-[#243A76] transition hover:border-[#243A76] hover:bg-blue-50"><Icon name="upload" className="mr-2 h-4 w-4" />Selecciona un archivo<input type="file" className="sr-only" onChange={(event) => setForm({ ...form, archivo: event.target.files[0] || null })} /></label>
                <p className="mt-2 truncate text-xs text-slate-500">{form.archivo ? form.archivo.name : "Ningún archivo seleccionado"}</p>
              </div>
              <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                <div className="flex items-start gap-3"><span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-white text-cyan-700 shadow-sm"><Icon name="link" className="h-4 w-4" /></span><div><p className="text-sm font-semibold text-slate-700">Enlace</p><p className="mt-0.5 text-xs text-slate-400">Comparte un recurso externo.</p></div></div>
                <label className="mt-4 block text-xs font-semibold uppercase tracking-wide text-slate-500">URL<input type="url" value={form.url} onChange={(event) => setForm({ ...form, url: event.target.value })} placeholder="https://..." className={fieldClass} /></label>
              </div>
            </div>
            <button disabled={!asignacion || subiendo} className="mt-5 flex w-full items-center justify-center rounded-xl bg-[#243A76] px-4 py-3 text-sm font-semibold text-white shadow-sm transition hover:bg-[#2d4a96] hover:shadow disabled:cursor-not-allowed disabled:bg-slate-300 disabled:text-slate-500">
              <Icon name="upload" className="mr-2 h-4 w-4" />{subiendo ? "Publicando material..." : "Subir material"}
            </button>
          </form>

          <section className="min-w-0 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
            <div className="flex items-start justify-between gap-3 border-b border-slate-100 pb-5"><div className="flex items-start gap-3"><span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-cyan-50 text-cyan-700"><Icon name="folder" /></span><div><h2 className="font-semibold text-slate-800">Materiales publicados</h2><p className="mt-0.5 text-sm text-slate-500">Recursos disponibles para este curso.</p></div></div><span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-bold text-slate-500">{materiales.length}</span></div>
            {loadingMateriales && <div className="mt-5 space-y-3">{[0, 1, 2].map((item) => <div key={item} className="h-24 animate-pulse rounded-2xl bg-slate-100" />)}</div>}
            {!loadingMateriales && materiales.length === 0 && <div className="mt-6 rounded-2xl border border-dashed border-slate-300 bg-slate-50 px-5 py-10 text-center"><span className="mx-auto flex h-11 w-11 items-center justify-center rounded-2xl bg-white text-slate-400 shadow-sm"><Icon name="folder" /></span><h3 className="mt-3 text-sm font-semibold text-slate-700">Aún no hay materiales publicados para este curso.</h3><p className="mt-1 text-xs text-slate-400">Publica un archivo o un enlace para que aparezca en esta sección.</p></div>}
            {!loadingMateriales && materiales.length > 0 && <div className="mt-5 grid gap-3 sm:grid-cols-2">{materiales.map((item) => <article key={item.id_material} className="flex min-w-0 flex-col rounded-2xl border border-slate-200 bg-slate-50/70 p-4 transition hover:border-[#243A76]/40 hover:bg-white hover:shadow-sm"><div className="flex min-w-0 items-start gap-3"><ResourceIcon type={item.tipo} url={item.url} /><div className="min-w-0"><h3 className="truncate font-semibold text-slate-700">{item.titulo || "Material"}</h3><p className="mt-1 line-clamp-2 text-sm text-slate-500">{item.descripcion || "Sin descripción."}</p></div></div><div className="mt-4 flex flex-wrap gap-1.5 text-[11px]"><span className="rounded-md bg-white px-2 py-1 font-semibold uppercase text-slate-500">{item.tipo || "Archivo"}</span>{formatSize(item.tamano_bytes) && <span className="rounded-md bg-white px-2 py-1 font-semibold text-slate-500">{formatSize(item.tamano_bytes)}</span>}<span className="rounded-md bg-white px-2 py-1 text-slate-400">{formatDate(item.fecha)}</span></div><a href={item.url} target="_blank" rel="noreferrer" className="mt-4 inline-flex items-center text-sm font-semibold text-[#243A76] hover:text-[#2d4a96]"><Icon name="external" className="mr-1.5 h-4 w-4" />{String(item.tipo || "").toLowerCase().includes("link") ? "Abrir enlace" : "Ver material"}</a></article>)}</div>}
          </section>
        </div>}
      </div>
    </Layout>
  );
}
