import { useState, useEffect } from "react";
import Layout from "../../components/Layout";
import {
  getMisAsignaciones,
  getActividades,
  getPeriodos,
  createActividad,
  deleteActividad,
} from "../../services/api";

const PRIMARY = "#243A76";

const TIPOS = ["LECCION_ORAL", "LECCION_ESCRITA", "TAREA", "TALLER", "CUADERNO", "TRABAJO_INDIVIDUAL", "EXPOSICION", "PROYECTO_INTERDISCIPLINARIO", "EXAMEN_TRIMESTRAL"];

// ── Cálculo de semanas dentro de un trimestre ────────────────
const MS_SEMANA = 7 * 24 * 3600 * 1000;
const parseFecha = (s) => {
  if (!s) return null;
  const [y, m, d] = s.split("-").map(Number);
  return new Date(y, m - 1, d);
};
const totalSemanas = (per) => {
  if (!per) return 0;
  const ini = parseFecha(per.fecha_inicio), fin = parseFecha(per.fecha_fin);
  if (!ini || !fin) return 0;
  return Math.max(1, Math.ceil((fin - ini) / MS_SEMANA));
};
const semanaDeFecha = (fechaStr, per) => {
  if (!per || !fechaStr) return 1;
  const diff = Math.floor((parseFecha(fechaStr) - parseFecha(per.fecha_inicio)) / MS_SEMANA);
  return Math.min(Math.max(diff + 1, 1), totalSemanas(per));
};
const rangoSemana = (per, n) => {
  const base = parseFecha(per.fecha_inicio).getTime();
  const ini = new Date(base + (n - 1) * MS_SEMANA);
  const fin = new Date(base + (n - 1) * MS_SEMANA + 6 * 24 * 3600 * 1000);
  return { ini, fin };
};
const fmtDia = (d) => d.toLocaleDateString("es-EC", { day: "2-digit", month: "2-digit", year: "numeric" });
const toInputDate = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;

const menuActividades = [
  {
    id: "lista",
    label: "Lista de Actividades",
    icon: (
      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 10h16M4 14h16M4 18h16" />
      </svg>
    ),
  },
  {
    id: "crear",
    label: "Crear Actividad",
    icon: (
      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
      </svg>
    ),
  },
];

const formVacio = {
  idAsignacion: "",
  idPeriodo: "",
  tipo: "TAREA",
  nombre: "",
  descripcion: "",
  fechaEntrega: "",
  ponderacion: "0",
  notaMaxima: "10",
  esSumativa: false,
};

export default function Actividades() {
  const [seccion, setSeccion] = useState("lista");
  const [asignaciones, setAsignaciones] = useState([]);
  const [periodos, setPeriodos] = useState([]);
  const [asignacionSel, setAsignacionSel] = useState("");
  const [periodoSel, setPeriodoSel] = useState("");
  const [semanaSel, setSemanaSel] = useState(1);
  const [actividades, setActividades] = useState([]);
  const [loading, setLoading] = useState(false);
  const [form, setForm] = useState(formVacio);
  const [guardando, setGuardando] = useState(false);
  const [mensaje, setMensaje] = useState(null);

  const periodoActual = periodos.find((p) => String(p.id_periodo) === String(periodoSel));
  const nSemanas = totalSemanas(periodoActual);

  useEffect(() => {
    cargarInicial();
  }, []);

  useEffect(() => {
    if (asignacionSel) cargarActividades(asignacionSel);
    else setActividades([]);
  }, [asignacionSel]);

  const cargarInicial = async () => {
    // Cada carga es independiente: si una falla, la otra igual se muestra.
    try {
      const asigRes = await getMisAsignaciones();
      setAsignaciones(asigRes.data || []);
      if (asigRes.data?.length > 0) setAsignacionSel(String(asigRes.data[0].idAsignacion));
    } catch (error) {
      console.error("Error cargando asignaciones:", error);
      if (error.response?.status === 401) {
        setMensaje({ tipo: "error", texto: "Tu sesión expiró. Vuelve a iniciar sesión." });
      }
    }
    try {
      const perRes = await getPeriodos();
      const pers = perRes.data || [];
      setPeriodos(pers);
      const activo = pers.find((p) => p.activo) || pers[0];
      if (activo) setPeriodoSel(String(activo.id_periodo));
    } catch (error) {
      console.error("Error cargando periodos:", error);
    }
  };

  const cargarActividades = async (idAsignacion) => {
    try {
      setLoading(true);
      const res = await getActividades(idAsignacion);
      // El gateway puede devolver un arreglo directo o {actividades:[...]}
      const data = Array.isArray(res.data) ? res.data : (res.data?.actividades || []);
      setActividades(data);
    } catch (error) {
      console.error("Error cargando actividades:", error);
      setActividades([]);
    } finally {
      setLoading(false);
    }
  };

  // Ponderación ya usada en el mismo curso + trimestre (para no pasar de 100%).
  const pondAcumulada = actividades
    .filter((a) => String(a.idAsignacion) === String(form.idAsignacion) && String(a.idPeriodo) === String(form.idPeriodo))
    .reduce((s, a) => s + (parseFloat(a.ponderacion) || 0), 0);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMensaje(null);

    const nuevaPond = parseFloat(form.ponderacion) || 0;
    if (String(form.idAsignacion) === asignacionSel && pondAcumulada + nuevaPond > 100) {
      setMensaje({ tipo: "error", texto: `La ponderación excede 100% en este trimestre (ya hay ${pondAcumulada}%, intentas sumar ${nuevaPond}%).` });
      return;
    }

    setGuardando(true);
    try {
      const payload = {
        asignacionId: parseInt(form.idAsignacion),
        periodoId: parseInt(form.idPeriodo),
        tipo: form.tipo,
        nombre: form.nombre,
        descripcion: form.descripcion,
        fechaEntrega: form.fechaEntrega,
        ponderacion: parseFloat(form.ponderacion),
        notaMaxima: parseFloat(form.notaMaxima),
        esSumativa: form.esSumativa,
      };
      await createActividad(payload);
      setMensaje({ tipo: "ok", texto: "Actividad creada correctamente." });
      setForm(formVacio);
      if (String(payload.idAsignacion) === asignacionSel) {
        cargarActividades(asignacionSel);
      }
      setSeccion("lista");
    } catch (error) {
      console.error("Error creando actividad:", error);
      setMensaje({ tipo: "error", texto: "No se pudo crear la actividad. Revise los datos." });
    } finally {
      setGuardando(false);
    }
  };

  const handleEliminar = async (id) => {
    if (!confirm("¿Eliminar esta actividad?")) return;
    try {
      await deleteActividad(id);
      cargarActividades(asignacionSel);
    } catch (error) {
      console.error("Error eliminando actividad:", error);
    }
  };

  const nombreAsignacion = (a) =>
    `${a.asignatura?.nombre || "Asignatura"} — ${a.grado?.nombre || ""}`;

  return (
    <Layout
      breadcrumb={["Inicio", "Actividades"]}
      sidebarTitle="ACTIVIDADES"
      menuItems={menuActividades}
      seccion={seccion}
      onSeccionChange={setSeccion}
    >
      <h1 className="text-2xl font-bold text-slate-800 mb-1">Actividades</h1>
      <p className="text-slate-500 mb-6">Gestione las actividades de sus cursos asignados.</p>

      {mensaje && (
        <div className={`mb-4 px-4 py-2 rounded-lg text-sm ${
          mensaje.tipo === "ok" ? "bg-green-50 text-green-700 border border-green-200" : "bg-red-50 text-red-700 border border-red-200"
        }`}>
          {mensaje.texto}
        </div>
      )}

      {/* SECCIÓN: LISTA — por semanas */}
      {seccion === "lista" && (
        <div>
          {/* Selectores de curso y trimestre */}
          <div className="bg-white p-4 rounded-xl border border-slate-200 mb-4 grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase tracking-wide">Curso</label>
              <select
                value={asignacionSel}
                onChange={(e) => setAsignacionSel(e.target.value)}
                className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50 focus:outline-none"
              >
                {asignaciones.length === 0 && <option value="">Sin asignaciones</option>}
                {asignaciones.map((a) => (
                  <option key={a.idAsignacion} value={a.idAsignacion}>{nombreAsignacion(a)}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase tracking-wide">Trimestre</label>
              <select
                value={periodoSel}
                onChange={(e) => { setPeriodoSel(e.target.value); setSemanaSel(1); }}
                className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50 focus:outline-none"
              >
                {periodos.length === 0 && <option value="">Sin trimestres</option>}
                {periodos.map((p) => (
                  <option key={p.id_periodo} value={p.id_periodo}>{p.nombre}</option>
                ))}
              </select>
            </div>
          </div>

          {/* Navegador de semanas (estilo plan de clases) */}
          {periodoActual && nSemanas > 0 && (
            <div className="bg-white rounded-xl border border-slate-200 mb-4 px-4 py-3 flex items-center justify-between">
              <button
                onClick={() => setSemanaSel((s) => Math.max(1, s - 1))}
                disabled={semanaSel <= 1}
                className="p-2 rounded-lg text-slate-500 hover:bg-slate-100 disabled:opacity-30"
                title="Semana anterior"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" /></svg>
              </button>
              <div className="text-center">
                <div className="flex items-center justify-center gap-2 text-sm text-slate-600">
                  <span className="font-semibold text-slate-700">Semana</span>
                  <input
                    type="number" min={1} max={nSemanas} value={semanaSel}
                    onChange={(e) => setSemanaSel(Math.min(nSemanas, Math.max(1, parseInt(e.target.value) || 1)))}
                    className="w-14 text-center border border-slate-200 rounded-lg py-1 text-sm bg-slate-50"
                  />
                  <span className="text-slate-400">de {nSemanas}</span>
                </div>
                <p className="text-xs text-slate-400 mt-1">
                  {(() => { const r = rangoSemana(periodoActual, semanaSel); return `${fmtDia(r.ini)} — ${fmtDia(r.fin)}`; })()}
                </p>
              </div>
              <button
                onClick={() => setSemanaSel((s) => Math.min(nSemanas, s + 1))}
                disabled={semanaSel >= nSemanas}
                className="p-2 rounded-lg text-slate-500 hover:bg-slate-100 disabled:opacity-30"
                title="Semana siguiente"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" /></svg>
              </button>
            </div>
          )}

          {/* Tarjeta de la semana (cabecera + actividades) */}
          <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
            <div style={{ backgroundColor: PRIMARY }} className="px-5 py-4 text-white flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="bg-white/15 rounded-lg px-3 py-1.5 text-center">
                  <div className="text-lg font-bold leading-none">{semanaSel}</div>
                  <div className="text-[10px] uppercase tracking-wide">Sem</div>
                </div>
                <div>
                  <h3 className="font-semibold text-sm">Actividades de la semana</h3>
                  <p className="text-white/70 text-xs">{periodoActual?.nombre || "—"}</p>
                </div>
              </div>
              <button
                onClick={() => {
                  const r = periodoActual ? rangoSemana(periodoActual, semanaSel) : null;
                  setForm({ ...formVacio, idAsignacion: asignacionSel, idPeriodo: periodoSel, fechaEntrega: r ? toInputDate(r.ini) : "" });
                  setSeccion("crear");
                }}
                className="bg-white/20 hover:bg-white/30 text-white text-xs font-medium px-3 py-1.5 rounded-lg"
              >
                + Actividad en esta semana
              </button>
            </div>

            {(() => {
              const actsSemana = actividades.filter(
                (a) => String(a.idPeriodo) === String(periodoSel) && semanaDeFecha(a.fechaEntrega, periodoActual) === semanaSel
              );
              if (loading) return <p className="text-center text-slate-400 py-10">Cargando actividades...</p>;
              if (!periodoActual) return <p className="text-center text-slate-400 py-10">Seleccione un trimestre.</p>;
              if (actsSemana.length === 0) return <p className="text-center text-slate-400 py-10">No hay actividades en la semana {semanaSel}.</p>;
              return (
                <div className="divide-y divide-slate-100">
                  {actsSemana.map((act) => (
                    <div key={act.idActividad} className="px-5 py-3 flex items-center justify-between hover:bg-slate-50">
                      <div className="min-w-0">
                        <div className="flex items-center gap-2">
                          <span className="text-sm font-medium text-slate-700">{act.nombre}</span>
                          <span className="bg-blue-50 text-blue-700 text-[10px] font-semibold px-1.5 py-0.5 rounded">{act.tipo}</span>
                          {act.esSumativa && <span className="bg-purple-50 text-purple-700 text-[10px] font-semibold px-1.5 py-0.5 rounded">SUMATIVA</span>}
                        </div>
                        <p className="text-xs text-slate-500">Entrega: {act.fechaEntrega} · Nota máx: {act.notaMaxima} · Pond: {act.ponderacion}%</p>
                      </div>
                      <button onClick={() => handleEliminar(act.idActividad)} className="text-red-500 hover:text-red-700 text-xs font-medium flex-shrink-0 ml-3">Eliminar</button>
                    </div>
                  ))}
                </div>
              );
            })()}
          </div>
        </div>
      )}

      {/* SECCIÓN: CREAR */}
      {seccion === "crear" && (
        <form onSubmit={handleSubmit} className="bg-white p-6 rounded-xl border border-slate-200 max-w-2xl space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Curso</label>
              <select
                required
                value={form.idAsignacion}
                onChange={(e) => setForm({ ...form, idAsignacion: e.target.value })}
                className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50"
              >
                <option value="">Seleccione...</option>
                {asignaciones.map((a) => (
                  <option key={a.idAsignacion} value={a.idAsignacion}>{nombreAsignacion(a)}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Trimestre</label>
              <select
                required
                value={form.idPeriodo}
                onChange={(e) => setForm({ ...form, idPeriodo: e.target.value })}
                className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50"
              >
                <option value="">Seleccione...</option>
                {periodos.map((p) => (
                  <option key={p.id_periodo} value={p.id_periodo}>{p.nombre}</option>
                ))}
              </select>
            </div>
          </div>

          {/* Semana dentro del trimestre → fija la fecha de entrega */}
          {(() => {
            const per = periodos.find((p) => String(p.id_periodo) === String(form.idPeriodo));
            const n = totalSemanas(per);
            if (!per || n === 0) return null;
            const semanaForm = semanaDeFecha(form.fechaEntrega, per);
            return (
              <div className="bg-blue-50/50 border border-blue-100 rounded-lg p-3">
                <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Semana del trimestre</label>
                <div className="flex items-center gap-3">
                  <select
                    value={form.fechaEntrega ? semanaForm : ""}
                    onChange={(e) => {
                      const r = rangoSemana(per, parseInt(e.target.value));
                      setForm({ ...form, fechaEntrega: toInputDate(r.ini) });
                    }}
                    className="border border-slate-200 rounded-lg px-3 py-2 text-sm bg-white"
                  >
                    <option value="">Elija semana...</option>
                    {Array.from({ length: n }, (_, i) => i + 1).map((w) => {
                      const r = rangoSemana(per, w);
                      return <option key={w} value={w}>Semana {w} ({fmtDia(r.ini)} — {fmtDia(r.fin)})</option>;
                    })}
                  </select>
                  <span className="text-xs text-slate-400">o fija la fecha exacta abajo</span>
                </div>
              </div>
            );
          })()}

          {/* Aviso de ponderación acumulada */}
          {form.idAsignacion && form.idPeriodo && (
            <div className={`text-xs px-3 py-2 rounded-lg border ${
              pondAcumulada + (parseFloat(form.ponderacion) || 0) > 100
                ? "bg-red-50 border-red-200 text-red-600"
                : "bg-slate-50 border-slate-200 text-slate-500"
            }`}>
              Ponderación del trimestre: <strong>{pondAcumulada}%</strong> usada + <strong>{parseFloat(form.ponderacion) || 0}%</strong> nueva = <strong>{pondAcumulada + (parseFloat(form.ponderacion) || 0)}%</strong> (máx. 100%)
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Nombre</label>
            <input
              required
              type="text"
              value={form.nombre}
              onChange={(e) => setForm({ ...form, nombre: e.target.value })}
              className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50"
              placeholder="Ej: Tarea 1 - Fracciones"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Descripción</label>
            <textarea
              value={form.descripcion}
              onChange={(e) => setForm({ ...form, descripcion: e.target.value })}
              className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50"
              rows={3}
            />
          </div>

          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Tipo</label>
              <select
                value={form.tipo}
                onChange={(e) => setForm({ ...form, tipo: e.target.value })}
                className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50"
              >
                {TIPOS.map((t) => <option key={t} value={t}>{t}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Entrega</label>
              <input
                required
                type="date"
                value={form.fechaEntrega}
                onChange={(e) => setForm({ ...form, fechaEntrega: e.target.value })}
                className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Nota Máx.</label>
              <input
                type="number"
                step="0.01"
                value={form.notaMaxima}
                onChange={(e) => setForm({ ...form, notaMaxima: e.target.value })}
                className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-500 mb-1 uppercase">Ponderación</label>
              <input
                type="number"
                step="0.01"
                value={form.ponderacion}
                onChange={(e) => setForm({ ...form, ponderacion: e.target.value })}
                className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm bg-slate-50"
              />
            </div>
          </div>

          <label className="flex items-center gap-2 text-sm text-slate-600">
            <input
              type="checkbox"
              checked={form.esSumativa}
              onChange={(e) => setForm({ ...form, esSumativa: e.target.checked })}
            />
            Es actividad sumativa
          </label>

          <div className="flex gap-3 pt-2">
            <button
              type="submit"
              disabled={guardando}
              style={{ backgroundColor: PRIMARY }}
              className="text-white px-5 py-2 rounded-lg text-sm font-medium hover:opacity-90 disabled:opacity-60"
            >
              {guardando ? "Guardando..." : "Crear Actividad"}
            </button>
            <button
              type="button"
              onClick={() => { setForm(formVacio); setSeccion("lista"); }}
              className="px-5 py-2 rounded-lg text-sm font-medium text-slate-500 hover:bg-slate-100"
            >
              Cancelar
            </button>
          </div>
        </form>
      )}
    </Layout>
  );
}
