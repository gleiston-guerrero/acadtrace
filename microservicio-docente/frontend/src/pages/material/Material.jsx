import { useEffect, useState } from "react";
import Layout from "../../components/Layout";
import { createMaterial, getMateriales, getMisAsignaciones } from "../../services/api";

const emptyForm = { titulo: "", descripcion: "", tipo: "", url: "", archivo: null };
const icono = (tipo = "") => tipo.toLowerCase().includes("pdf") ? "PDF" : tipo.toLowerCase().includes("video") ? "▶" : tipo.toLowerCase().includes("link") ? "↗" : "FILE";

export default function Material() {
  const [asignaciones, setAsignaciones] = useState([]);
  const [asignacion, setAsignacion] = useState("");
  const [materiales, setMateriales] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [error, setError] = useState("");

  const cargar = async (idAsignacion) => setMateriales((await getMateriales(idAsignacion)).data || []);
  useEffect(() => { getMisAsignaciones().then((response) => { const data = response.data || []; setAsignaciones(data); if (data[0]) setAsignacion(String(data[0].idAsignacion)); }).catch(() => setError("No se pudieron cargar las asignaciones.")); }, []);
  useEffect(() => { if (asignacion) cargar(asignacion).catch(() => setError("No se pudo cargar el material.")); }, [asignacion]);

  const subir = async (event) => {
    event.preventDefault();
    const data = new FormData();
    data.append("id_asignacion", asignacion);
    data.append("titulo", form.titulo);
    data.append("descripcion", form.descripcion);
    data.append("tipo", form.tipo);
    if (form.archivo) data.append("archivo", form.archivo);
    if (form.url) data.append("url", form.url);
    try { await createMaterial(data); setForm(emptyForm); event.target.reset(); await cargar(asignacion); } catch { setError("Adjunta un archivo o proporciona un enlace válido."); }
  };
  const nombreAsignacion = (item) => `${item.asignatura?.nombre || "Asignatura"} — ${item.grado?.nombre || ""}`;

  return (
    <Layout breadcrumb={["Inicio", "Material"]} sidebarTitle="AULA VIRTUAL">
      <h1 className="text-2xl font-bold text-slate-800">Material de clase</h1><p className="text-sm text-slate-500 mb-5">Archivos y enlaces persistentes por curso.</p>
      {error && <p className="text-sm text-red-600 mb-4">{error}</p>}
      <select value={asignacion} onChange={(event) => setAsignacion(event.target.value)} className="w-full max-w-xl mb-5 border rounded-lg p-2 bg-white">{asignaciones.map((item) => <option key={item.idAsignacion} value={item.idAsignacion}>{nombreAsignacion(item)}</option>)}</select>
      <form onSubmit={subir} className="grid gap-3 md:grid-cols-2 bg-white border rounded-xl p-5 mb-6"><input required placeholder="Título" value={form.titulo} onChange={(event) => setForm({ ...form, titulo: event.target.value })} className="border rounded-lg p-2" /><input placeholder="Tipo: PDF, video o enlace" value={form.tipo} onChange={(event) => setForm({ ...form, tipo: event.target.value })} className="border rounded-lg p-2" /><textarea placeholder="Descripción" value={form.descripcion} onChange={(event) => setForm({ ...form, descripcion: event.target.value })} className="border rounded-lg p-2 md:col-span-2" /><input type="file" onChange={(event) => setForm({ ...form, archivo: event.target.files[0] || null })} className="border rounded-lg p-2" /><input type="url" placeholder="o enlace https://..." value={form.url} onChange={(event) => setForm({ ...form, url: event.target.value })} className="border rounded-lg p-2" /><button disabled={!asignacion} className="md:col-span-2 bg-[#243A76] text-white py-2 rounded-lg disabled:opacity-50">Subir material</button></form>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">{materiales.map((item) => <article key={item.id_material} className="bg-white border rounded-xl p-5 flex flex-col gap-3"><div className="flex gap-3 items-center"><span className="text-xs font-bold bg-cyan-50 text-cyan-700 p-2 rounded">{icono(item.tipo)}</span><h2 className="font-semibold">{item.titulo || "Material"}</h2></div><p className="text-sm text-slate-600 flex-1">{item.descripcion || "Sin descripción."}</p><span className="text-xs text-slate-400">{item.tipo || "Archivo"} · {item.tamano_bytes ? `${Math.ceil(item.tamano_bytes / 1024)} KB` : "Enlace"}</span><time className="text-xs text-slate-400">{new Date(item.fecha).toLocaleDateString("es-EC")}</time><a href={item.url} target="_blank" rel="noreferrer" className="text-sm font-medium text-[#243A76]">Abrir material →</a></article>)}</div>
    </Layout>
  );
}
