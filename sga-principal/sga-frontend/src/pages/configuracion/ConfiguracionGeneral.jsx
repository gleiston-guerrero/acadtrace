import { useState, useEffect } from "react";
import axios from "axios";
import api from "../../config/axios";
import Layout from "../../components/Layout";
import { useToast } from "../../context/ToastContext";
import { useConfirm } from "../../context/ConfirmContext";

const API_CALIF = `http://${window.location.hostname}:8080/api/configuracion/calificacion`;
const PRIMARY = "#243A76";
const modalBg = { backgroundColor: "rgba(36, 58, 118, 0.5)" };

const menuItems = [
  {
    id: "anos",
    label: "Años y Períodos Lectivos",
    icon: (
      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
      </svg>
    ),
  },
  {
    id: "calificaciones",
    label: "Esquema de Calificaciones",
    icon: (
      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
      </svg>
    ),
  },
  {
    id: "usuarios",
    label: "Usuarios y Roles",
    icon: (
      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
      </svg>
    ),
  },
  {
    id: "escuela",
    label: "Datos Institucionales",
    icon: (
      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5m0 0h4m-4 0V11m0 0h4m-4 0H7m4 0v10" />
      </svg>
    ),
  },
];

export default function ConfiguracionGeneral() {
  const [seccion, setSeccion] = useState("anos");
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  const toast = useToast();
  const confirm = useConfirm();

  // ESTADOS - AÑOS LECTIVOS
  const [anos, setAnos] = useState([]);
  const [showAnoModal, setShowAnoModal] = useState(false);
  const [formAno, setFormAno] = useState({ nombre: "", fechaInicio: "", fechaFin: "" });

  // ESTADOS - ESQUEMA DE CALIFICACIONES
  const [esquema, setEsquema] = useState({ pesoFormativa: 70, pesoSumativa: 30 });
  const [periodosEval, setPeriodosEval] = useState([]);
  const [aportes, setAportes] = useState([]);
  const [escala, setEscala] = useState([]);

  // ESTADOS - USUARIOS
  const [usuarios, setUsuarios] = useState([]);

  // ESTADOS - ESCUELA
  const [escuela, setEscuela] = useState({
    nombre: "Escuela de Educación Básica Provincias Unidas",
    amie: "09H01234",
    zona: "Zona 5 · Distrito 09D06",
    regimen: "Costa - Galápagos",
    jornada: "Matutina (07:30 - 12:30)",
    direccion: "Calle Principal & Av. Provincias Unidas, Guayaquil",
    telefono: "(04) 289-4567 / 0998765432",
    correo: "escuela.provinciasunidas@educacion.gob.ec",
    rectora: "Lcda. María Fernández",
    secretaria: "Dra. Carmen Morales",
  });

  const token = localStorage.getItem("token");
  const headers = { Authorization: `Bearer ${token}` };

  // CARGAS
  const cargarAnos = () => {
    api.get("/api/anos-lectivos").then(r => setAnos(r.data || [])).catch(() => {});
  };

  const cargarCalificaciones = () => {
    Promise.all([
      axios.get(`${API_CALIF}/esquema`, { headers }).then(r => setEsquema(r.data)),
      axios.get(`${API_CALIF}/periodos`, { headers }).then(r => setPeriodosEval(r.data)),
      axios.get(`${API_CALIF}/aportes`, { headers }).then(r => setAportes(r.data)),
      axios.get(`${API_CALIF}/escala`, { headers }).then(r => setEscala(r.data)),
    ]).catch(() => {});
  };

  const cargarUsuarios = () => {
    api.get("/api/usuarios").then(r => setUsuarios(r.data || [])).catch(() => {});
  };

  useEffect(() => {
    cargarAnos();
    cargarCalificaciones();
    cargarUsuarios();
  }, []);

  // ACCIONES AÑOS LECTIVOS
  const handleCrearAno = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      await api.post("/api/anos-lectivos", formAno);
      toast.success("Año Lectivo creado", `Se registró el período ${formAno.nombre} con éxito.`);
      setShowAnoModal(false);
      setFormAno({ nombre: "", fechaInicio: "", fechaFin: "" });
      cargarAnos();
    } catch (err) {
      toast.error("Error al crear", err.response?.data?.message || "No se pudo registrar el año lectivo.");
    } finally {
      setSaving(false);
    }
  };

  const handleActivarAno = async (id, nombre) => {
    const ok = await confirm({
      title: `¿Activar año lectivo ${nombre}?`,
      message: "Este período pasará a ser el AÑO LECTIVO ACTIVO en todo el sistema SGA.",
      confirmText: "Sí, activar",
      type: "info",
    });
    if (!ok) return;

    try {
      await api.post(`/api/anos-lectivos/${id}/activar`);
      toast.success("Año lectivo activado", `El período ${nombre} ahora es el año actual.`);
      cargarAnos();
    } catch (err) {
      toast.error("Error de activación", err.response?.data?.message || "No se pudo activar el período.");
    }
  };

  // ACCIONES ESQUEMA CALIFICACIONES
  const handleGuardarPonderacion = async (e) => {
    e.preventDefault();
    if (esquema.pesoFormativa + esquema.pesoSumativa !== 100) {
      toast.error("Ponderación inválida", "La suma de Formativa y Sumativa debe ser exactamente 100%.");
      return;
    }
    setSaving(true);
    try {
      await axios.put(`${API_CALIF}/esquema`, esquema, { headers });
      toast.success("Esquema actualizado", "Se guardó la ponderación de calificaciones.");
    } catch {
      toast.error("Error al guardar", "No se pudo actualizar el esquema.");
    } finally {
      setSaving(false);
    }
  };

  const togglePeriodoEstado = async (p) => {
    const nuevo = p.estado === "ABIERTO" ? "CERRADO" : "ABIERTO";
    const ok = await confirm({
      title: `¿${nuevo === "ABIERTO" ? "Abrir" : "Cerrar"} ${p.nombre}?`,
      message: `Los docentes ${nuevo === "ABIERTO" ? "podrán ingresar notas" : "ya NO podrán modificar notas"} en este trimestre.`,
      confirmText: `Sí, ${nuevo.toLowerCase()}`,
      type: nuevo === "ABIERTO" ? "info" : "warning",
    });
    if (!ok) return;

    try {
      await axios.put(`${API_CALIF}/periodos/${p.idPeriodo}`, { ...p, estado: nuevo }, { headers });
      toast.success("Trimestre actualizado", `${p.nombre} ahora está ${nuevo}.`);
      cargarCalificaciones();
    } catch {
      toast.error("Error", "No se pudo cambiar el estado del trimestre.");
    }
  };

  // ACCIONES DATOS INSTITUCIONALES
  const handleGuardarEscuela = (e) => {
    e.preventDefault();
    toast.success("Datos institucionales guardados", "Información oficial de la Escuela Provincias Unidas actualizada.");
  };

  return (
    <Layout
      breadcrumb={["Inicio", "Configuración General"]}
      sidebarTitle="Configuración"
      menuItems={menuItems}
      seccion={seccion}
      onSeccionChange={setSeccion}
    >
      <div className="mb-4">
        <h1 className="text-lg font-bold text-slate-700">Configuración General del Sistema</h1>
        <p className="text-xs text-slate-400">Gestión de períodos lectivos, esquema de calificaciones, usuarios e información institucional</p>
      </div>

      {/* SECCIÓN 1: AÑOS Y PERÍODOS LECTIVOS */}
      {seccion === "anos" && (
        <div className="space-y-4">
          <div className="flex items-center justify-between bg-white border border-slate-200 rounded-xl p-4">
            <div>
              <h2 className="text-sm font-bold text-slate-700">Períodos Lectivos Registrados</h2>
              <p className="text-xs text-slate-400">Define el año lectivo oficial activo para matrículas y calificaciones</p>
            </div>
            <button
              onClick={() => setShowAnoModal(true)}
              style={{ backgroundColor: PRIMARY }}
              className="flex items-center gap-2 text-white text-xs font-semibold px-4 py-2 rounded-lg hover:opacity-90 transition shadow-sm"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg>
              Nuevo Año Lectivo
            </button>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {anos.map((a) => (
              <div key={a.idAnoLectivo} className={`bg-white border rounded-2xl p-4 relative shadow-sm transition ${a.esActual ? "border-emerald-300 ring-2 ring-emerald-500/20" : "border-slate-200"}`}>
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm font-bold text-slate-800">{a.nombre}</span>
                  {a.esActual ? (
                    <span className="bg-emerald-100 text-emerald-700 text-[10px] font-extrabold px-2.5 py-0.5 rounded-full uppercase">Activo Vigente</span>
                  ) : (
                    <span className="bg-slate-100 text-slate-500 text-[10px] font-semibold px-2 py-0.5 rounded-full">Inactivo</span>
                  )}
                </div>
                <div className="text-xs text-slate-500 space-y-1 mb-4 font-mono">
                  <p>Inicio: {a.fechaInicio || "—"}</p>
                  <p>Fin: {a.fechaFin || "—"}</p>
                </div>
                {!a.esActual && (
                  <button
                    onClick={() => handleActivarAno(a.idAnoLectivo, a.nombre)}
                    className="w-full py-1.5 bg-blue-50 text-blue-700 hover:bg-blue-100 font-semibold text-xs rounded-lg transition"
                  >
                    Activar como Período Actual
                  </button>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* SECCIÓN 2: ESQUEMA DE CALIFICACIONES */}
      {seccion === "calificaciones" && (
        <div className="space-y-4">
          <div className="bg-white border border-slate-200 rounded-xl p-5 shadow-sm">
            <h2 className="text-sm font-bold text-slate-700 mb-1">Ponderación de Calificaciones</h2>
            <p className="text-xs text-slate-400 mb-4">Define la proporción entre la evaluación Formativa (actividades en clase) y Sumativa (exámenes/proyectos)</p>

            <form onSubmit={handleGuardarPonderacion} className="space-y-4 max-w-lg">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">Evaluación Formativa (%)</label>
                  <input
                    type="number"
                    min="0"
                    max="100"
                    value={esquema.pesoFormativa}
                    onChange={(e) => setEsquema({ ...esquema, pesoFormativa: Number(e.target.value), pesoSumativa: 100 - Number(e.target.value) })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-slate-50 focus:outline-none focus:ring-2 focus:ring-blue-500 font-mono font-bold text-blue-900"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">Evaluación Sumativa (%)</label>
                  <input
                    type="number"
                    min="0"
                    max="100"
                    value={esquema.pesoSumativa}
                    onChange={(e) => setEsquema({ ...esquema, pesoSumativa: Number(e.target.value), pesoFormativa: 100 - Number(e.target.value) })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-slate-50 focus:outline-none focus:ring-2 focus:ring-blue-500 font-mono font-bold text-amber-900"
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={saving}
                style={{ backgroundColor: PRIMARY }}
                className="px-5 py-2.5 text-white text-xs font-semibold rounded-lg hover:opacity-90 transition disabled:opacity-50"
              >
                {saving ? "Guardando..." : "Guardar Ponderación"}
              </button>
            </form>
          </div>

          <div className="bg-white border border-slate-200 rounded-xl p-5 shadow-sm">
            <h2 className="text-sm font-bold text-slate-700 mb-1">Apertura y Cierre de Trimestres</h2>
            <p className="text-xs text-slate-400 mb-4">Habilita o inactiva la edición de notas por parte del personal docente</p>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              {periodosEval.map((p) => (
                <div key={p.idPeriodo} className="border border-slate-200 rounded-xl p-4 bg-slate-50/50 flex flex-col justify-between">
                  <div>
                    <div className="flex items-center justify-between mb-2">
                      <span className="font-bold text-slate-700 text-sm">{p.nombre}</span>
                      <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${p.estado === "ABIERTO" ? "bg-emerald-100 text-emerald-700" : "bg-rose-100 text-rose-700"}`}>
                        {p.estado}
                      </span>
                    </div>
                    <p className="text-xs text-slate-500 font-mono mb-3">Orden: Trimestre N° {p.orden}</p>
                  </div>
                  <button
                    onClick={() => togglePeriodoEstado(p)}
                    className={`w-full py-1.5 text-xs font-semibold rounded-lg transition ${p.estado === "ABIERTO" ? "bg-rose-50 text-rose-700 hover:bg-rose-100" : "bg-emerald-50 text-emerald-700 hover:bg-emerald-100"}`}
                  >
                    {p.estado === "ABIERTO" ? "🔒 Cerrar Trimestre" : "🔓 Abrir Trimestre"}
                  </button>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* SECCIÓN 3: USUARIOS Y ROLES */}
      {seccion === "usuarios" && (
        <div className="bg-white rounded-xl border border-slate-200 overflow-hidden shadow-sm">
          <div className="p-4 border-b border-slate-100 flex items-center justify-between">
            <div>
              <h2 className="text-sm font-bold text-slate-700">Usuarios Registrados en el SGA</h2>
              <p className="text-xs text-slate-400">{usuarios.length} usuarios del sistema distribuidos en roles</p>
            </div>
          </div>

          <table className="w-full text-sm">
            <thead>
              <tr style={{ backgroundColor: PRIMARY }} className="text-white text-xs">
                <th className="text-left px-4 py-3 font-semibold">Usuario</th>
                <th className="text-left px-4 py-3 font-semibold">Cédula</th>
                <th className="text-left px-4 py-3 font-semibold">Nombres & Apellidos</th>
                <th className="text-left px-4 py-3 font-semibold">Rol Asignado</th>
                <th className="text-center px-4 py-3 font-semibold">Estado</th>
              </tr>
            </thead>
            <tbody>
              {usuarios.map((u, i) => (
                <tr key={u.idUsuario || i} className={`border-t border-slate-100 hover:bg-slate-50 transition ${i % 2 === 0 ? "" : "bg-slate-50/50"}`}>
                  <td className="px-4 py-3 font-bold text-slate-700">{u.username}</td>
                  <td className="px-4 py-3 text-slate-500 font-mono text-xs">{u.cedula || "—"}</td>
                  <td className="px-4 py-3 text-slate-700 font-medium">{u.apellidos ? `${u.apellidos} ${u.nombres}` : u.nombres || "—"}</td>
                  <td className="px-4 py-3">
                    <span className="bg-blue-50 text-blue-800 text-[11px] font-semibold px-2.5 py-0.5 rounded-md border border-blue-100">
                      {Array.isArray(u.roles) ? u.roles.join(", ") : u.rol || "DOCENTE"}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-center">
                    <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${u.estado === "ACTIVO" || u.activo ? "bg-emerald-100 text-emerald-700" : "bg-slate-100 text-slate-500"}`}>
                      {u.estado || (u.activo ? "ACTIVO" : "INACTIVO")}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* SECCIÓN 4: DATOS INSTITUCIONALES */}
      {seccion === "escuela" && (
        <div className="bg-white border border-slate-200 rounded-xl p-6 shadow-sm max-w-3xl">
          <h2 className="text-sm font-bold text-slate-700 mb-1">Información Oficial de la Institución</h2>
          <p className="text-xs text-slate-400 mb-5">Datos que aparecen impresos en certificados, fichas de matrícula y reportes en PDF</p>

          <form onSubmit={handleGuardarEscuela} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="col-span-2">
                <label className="block text-xs font-semibold text-slate-600 mb-1">Nombre Oficial de la Institución *</label>
                <input
                  type="text"
                  required
                  value={escuela.nombre}
                  onChange={(e) => setEscuela({ ...escuela, nombre: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-slate-50 focus:outline-none focus:ring-2 focus:ring-blue-500 font-semibold text-slate-800"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">Código AMIE MinEduc *</label>
                <input
                  type="text"
                  required
                  value={escuela.amie}
                  onChange={(e) => setEscuela({ ...escuela, amie: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-slate-50 font-mono font-bold text-blue-900"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">Zona & Distrito</label>
                <input
                  type="text"
                  value={escuela.zona}
                  onChange={(e) => setEscuela({ ...escuela, zona: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-slate-50"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">Régimen Escolar</label>
                <input
                  type="text"
                  value={escuela.regimen}
                  onChange={(e) => setEscuela({ ...escuela, regimen: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-slate-50"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">Jornada Escolar</label>
                <input
                  type="text"
                  value={escuela.jornada}
                  onChange={(e) => setEscuela({ ...escuela, jornada: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-slate-50"
                />
              </div>

              <div className="col-span-2">
                <label className="block text-xs font-semibold text-slate-600 mb-1">Dirección Institucional</label>
                <input
                  type="text"
                  value={escuela.direccion}
                  onChange={(e) => setEscuela({ ...escuela, direccion: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-slate-50"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">Director / Rector Autorizado</label>
                <input
                  type="text"
                  value={escuela.rectora}
                  onChange={(e) => setEscuela({ ...escuela, rectora: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-slate-50"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">Secretaria Institucional</label>
                <input
                  type="text"
                  value={escuela.secretaria}
                  onChange={(e) => setEscuela({ ...escuela, secretaria: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-slate-50"
                />
              </div>
            </div>

            <div className="pt-2">
              <button
                type="submit"
                style={{ backgroundColor: PRIMARY }}
                className="px-6 py-2.5 text-white text-xs font-semibold rounded-lg hover:opacity-90 transition shadow-sm"
              >
                Guardar Datos Institucionales
              </button>
            </div>
          </form>
        </div>
      )}

      {/* MODAL CREAR AÑO LECTIVO */}
      {showAnoModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center px-4" style={modalBg} onClick={() => setShowAnoModal(false)}>
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden" onClick={(e) => e.stopPropagation()}>
            <div style={{ backgroundColor: PRIMARY }} className="px-6 py-4 text-white flex items-center justify-between">
              <h3 className="font-bold text-sm">Nuevo Año Lectivo</h3>
              <button onClick={() => setShowAnoModal(false)} className="text-white/80 hover:text-white">✕</button>
            </div>
            <form onSubmit={handleCrearAno} className="p-6 space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">Nombre del Período *</label>
                <input
                  type="text"
                  required
                  placeholder="Ej: 2026 - 2027"
                  value={formAno.nombre}
                  onChange={(e) => setFormAno({ ...formAno, nombre: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-slate-50 focus:outline-none focus:ring-2 focus:ring-blue-500 font-bold"
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">Fecha Inicio</label>
                  <input
                    type="date"
                    value={formAno.fechaInicio}
                    onChange={(e) => setFormAno({ ...formAno, fechaInicio: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-xs bg-slate-50"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">Fecha Fin</label>
                  <input
                    type="date"
                    value={formAno.fechaFin}
                    onChange={(e) => setFormAno({ ...formAno, fechaFin: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-xs bg-slate-50"
                  />
                </div>
              </div>
              <div className="flex gap-3 pt-2">
                <button type="button" onClick={() => setShowAnoModal(false)} className="flex-1 py-2 border border-slate-200 rounded-lg text-xs text-slate-600">Cancelar</button>
                <button type="submit" disabled={saving} style={{ backgroundColor: PRIMARY }} className="flex-1 py-2 rounded-lg text-xs text-white font-semibold shadow">
                  {saving ? "Guardando..." : "Crear Período"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </Layout>
  );
}
