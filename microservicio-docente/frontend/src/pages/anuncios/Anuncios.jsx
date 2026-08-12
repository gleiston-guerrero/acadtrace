import { useEffect, useState } from "react";
import Layout from "../../components/Layout";
import { createAnuncio, getAnuncios, getMisAsignaciones } from "../../services/api";

const emptyForm = { titulo: "", contenido: "", fijado: false };

export default function Anuncios() {
  const [asignaciones, setAsignaciones] = useState([]);
  const [asignacion, setAsignacion] = useState("");
  const [anuncios, setAnuncios] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [modal, setModal] = useState(false);
  const [error, setError] = useState("");

  const cargarAnuncios = async (idAsignacion) => {
    const response = await getAnuncios(idAsignacion);
    setAnuncios(response.data || []);
  };

  useEffect(() => {
    getMisAsignaciones().then((response) => {
      const data = response.data || [];
      setAsignaciones(data);
      if (data[0]) setAsignacion(String(data[0].idAsignacion));
    }).catch(() => setError("No se pudieron cargar las asignaciones."));
  }, []);

  useEffect(() => {
    if (asignacion) cargarAnuncios(asignacion).catch(() => setError("No se pudieron cargar los anuncios."));
  }, [asignacion]);

  const publicar = async (event) => {
    event.preventDefault();
    try {
      await createAnuncio({
        ...form,
        id_asignacion: Number(asignacion),
        autor_id: Number(localStorage.getItem("userId")) || null,
      });
      setForm(emptyForm);
      setModal(false);
      await cargarAnuncios(asignacion);
    } catch {
      setError("No se pudo publicar el anuncio.");
    }
  };

  const nombreAsignacion = (item) => `${item.asignatura?.nombre || "Asignatura"} — ${item.grado?.nombre || ""}`;

  return (
    <Layout breadcrumb={["Inicio", "Anuncios"]} sidebarTitle="AULA VIRTUAL">
      <div className="flex flex-wrap justify-between gap-4 mb-5">
        <div><h1 className="text-2xl font-bold text-slate-800">Anuncios</h1><p className="text-sm text-slate-500">Muro cronológico de cada curso.</p></div>
        <button onClick={() => setModal(true)} disabled={!asignacion} className="bg-[#243A76] text-white px-4 py-2 rounded-lg text-sm disabled:opacity-50">Publicar</button>
      </div>
      {error && <p className="text-sm text-red-600 mb-4">{error}</p>}
      <select value={asignacion} onChange={(event) => setAsignacion(event.target.value)} className="w-full max-w-xl mb-5 border rounded-lg p-2 bg-white">
        {asignaciones.map((item) => <option key={item.idAsignacion} value={item.idAsignacion}>{nombreAsignacion(item)}</option>)}
      </select>
      <div className="space-y-4 max-w-3xl">
        {anuncios.length === 0 && <p className="bg-white border rounded-xl p-8 text-center text-slate-400">No hay anuncios para este curso.</p>}
        {anuncios.map((item) => <article key={item.id_anuncio} className="bg-white border rounded-xl p-5 shadow-sm"><div className="flex justify-between gap-3"><h2 className="font-semibold text-slate-800">{item.titulo || "Sin título"}</h2>{item.fijado && <span className="text-xs bg-amber-100 text-amber-700 px-2 py-1 rounded">Fijado</span>}</div><p className="mt-3 text-sm whitespace-pre-wrap text-slate-600">{item.contenido}</p><time className="block mt-4 text-xs text-slate-400">{new Date(item.fecha).toLocaleString("es-EC")}</time></article>)}
      </div>
      {modal && <div className="fixed inset-0 z-50 bg-slate-900/40 flex items-center justify-center p-4"><form onSubmit={publicar} className="w-full max-w-lg bg-white rounded-xl p-6 shadow-xl"><div className="flex justify-between mb-4"><h2 className="font-bold">Publicar anuncio</h2><button type="button" onClick={() => setModal(false)}>×</button></div><input required value={form.titulo} onChange={(event) => setForm({ ...form, titulo: event.target.value })} placeholder="Título" className="w-full border rounded-lg p-2 mb-3" /><textarea required rows="6" value={form.contenido} onChange={(event) => setForm({ ...form, contenido: event.target.value })} placeholder="Contenido" className="w-full border rounded-lg p-2 mb-3" /><label className="flex gap-2 text-sm mb-5"><input type="checkbox" checked={form.fijado} onChange={(event) => setForm({ ...form, fijado: event.target.checked })} />Fijar anuncio</label><button className="w-full bg-[#243A76] text-white py-2 rounded-lg">Publicar</button></form></div>}
    </Layout>
  );
}
