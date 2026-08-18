import { useState, useEffect } from "react";
import axios from "axios";
import api from "../../config/axios";
import Layout from "../../components/Layout";
import { useToast } from "../../context/ToastContext";
import { useConfirm } from "../../context/ConfirmContext";
import Usuarios from "../usuarios/Usuarios";
import AnosLectivos from "../anos-lectivos/AnosLectivos";

const API_CALIF = `http://${window.location.hostname}:8080/api/configuracion/calificacion`;
const PRIMARY = "#243A76";

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
    id: "usuarios",
    label: "Usuarios y Roles del Sistema",
    icon: (
      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
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
    id: "cursos",
    label: "Baja de Cursos y Grados",
    icon: (
      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
      </svg>
    ),
  },
  {
    id: "baja-estudiantes",
    label: "Baja de Estudiantes",
    icon: (
      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7a4 4 0 11-8 0 4 4 0 018 0zM9 14a6 6 0 00-6 6v1h12v-1a6 6 0 00-6-6zM21 12h-6" />
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
  const [saving, setSaving] = useState(false);

  const toast = useToast();
  const confirm = useConfirm();

  // ESQUEMA CALIFICACIONES
  const [esquema, setEsquema] = useState({ pesoFormativa: 70, pesoSumativa: 30 });
  const [periodosEval, setPeriodosEval] = useState([]);

  // GRADOS Y ESTUDIANTES PARA BAJAS
  const [grados, setGrados] = useState([]);
  const [estudiantes, setEstudiantes] = useState([]);
  const [busquedaEstudiante, setBusquedaEstudiante] = useState("");

  // ESCUELA
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

  const cargarGrados = () => {
    api.get("/api/grados").then(r => setGrados(r.data || [])).catch(() => {});
  };

  const cargarEstudiantes = () => {
    api.get("/api/estudiantes").then(r => setEstudiantes(r.data || [])).catch(() => {});
  };

  const cargarCalificaciones = () => {
    Promise.all([
      axios.get(`${API_CALIF}/esquema`, { headers }).then(r => setEsquema(r.data)),
      axios.get(`${API_CALIF}/periodos`, { headers }).then(r => setPeriodosEval(r.data)),
    ]).catch(() => {});
  };

  useEffect(() => {
    cargarCalificaciones();
    cargarGrados();
    cargarEstudiantes();
  }, []);

  // BAJA / ACTIVAR CURSO
  const handleToggleEstadoGrado = async (g) => {
    const nuevoEstado = !g.activo;
    const ok = await confirm({
      title: `¿${nuevoEstado ? "Activar" : "Dar de baja"} curso ${g.nombre}?`,
      message: nuevoEstado
        ? `El curso ${g.nombre} pasará a estar activo para nuevas matrículas.`
        : `El curso ${g.nombre} se dará de baja en el sistema.`,
      confirmText: nuevoEstado ? "Sí, activar" : "Sí, dar de baja",
      type: nuevoEstado ? "info" : "danger",
    });
    if (!ok) return;

    try {
      await api.patch(`/api/grados/${g.idGrado}/estado`, null, { params: { activo: nuevoEstado } });
      toast.success("Estado de curso actualizado", `${g.nombre} fue ${nuevoEstado ? "activado" : "dado de baja"}.`);
      cargarGrados();
    } catch {
      toast.error("Error", "No se pudo actualizar el estado del curso.");
    }
  };

  // BAJA / RETIRO DE ESTUDIANTE
  const handleToggleEstadoEstudiante = async (est) => {
    const estaActivo = est.activo !== false;
    const ok = await confirm({
      title: `¿${estaActivo ? "Dar de baja" : "Reactivar"} a ${est.apellidos} ${est.nombres}?`,
      message: estaActivo
        ? `El estudiante quedará inactivo en el sistema.`
        : `El estudiante volverá a estar activo en el sistema.`,
      confirmText: estaActivo ? "Sí, dar de baja" : "Sí, reactivar",
      type: estaActivo ? "danger" : "info",
    });
    if (!ok) return;

    try {
      await api.patch(`/api/estudiantes/${est.idEstudiante}/estado`, null, { params: { activo: !estaActivo } });
      toast.success("Estudiante actualizado", `El estudiante fue ${estaActivo ? "dado de baja" : "reactivado"}.`);
      cargarEstudiantes();
    } catch {
      toast.error("Error", "No se pudo cambiar el estado del estudiante.");
    }
  };

  // ESQUEMA CALIFICACIONES
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

  const estudiantesFiltrados = estudiantes.filter(e =>
    `${e.apellidos} ${e.nombres} ${e.cedula || ""}`.toLowerCase().includes(busquedaEstudiante.toLowerCase())
  );

  return (
    <Layout
      breadcrumb={["Inicio", "Configuración General"]}
      sidebarTitle="Configuración"
      menuItems={menuItems}
      seccion={seccion}
      onSeccionChange={setSeccion}
    >
      {/* 1. AÑOS Y PERÍODOS LECTIVOS */}
      {seccion === "anos" && <AnosLectivos embed={true} />}

      {/* 2. USUARIOS Y ROLES */}
      {seccion === "usuarios" && <Usuarios embed={true} />}

      {/* 3. ESQUEMA DE CALIFICACIONES */}
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

      {/* 4. BAJA DE CURSOS Y GRADOS */}
      {seccion === "cursos" && (
        <div className="bg-white border border-slate-200 rounded-xl overflow-hidden shadow-sm">
          <div className="p-4 border-b border-slate-100">
            <h2 className="text-sm font-bold text-slate-700">Gestión y Baja de Cursos de la Institución</h2>
            <p className="text-xs text-slate-400">Permite dar de baja o activar cursos para la inscripción de matrículas</p>
          </div>

          <div className="p-4 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {grados.map((g) => (
              <div key={g.idGrado} className="border border-slate-200 rounded-2xl p-4 bg-slate-50/50 flex flex-col justify-between">
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <span className="font-bold text-slate-800 text-sm">{g.nombre}</span>
                    <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${g.activo !== false ? "bg-emerald-100 text-emerald-700" : "bg-rose-100 text-rose-700"}`}>
                      {g.activo !== false ? "ACTIVO" : "DADO DE BAJA"}
                    </span>
                  </div>
                  <p className="text-xs text-slate-500 mb-3">Paralelo Asignado: <span className="font-bold text-blue-800">Paralelo A</span></p>
                </div>

                <button
                  onClick={() => handleToggleEstadoGrado(g)}
                  className={`w-full py-2 text-xs font-semibold rounded-xl transition ${g.activo !== false ? "bg-rose-50 text-rose-700 border border-rose-200 hover:bg-rose-100" : "bg-emerald-50 text-emerald-700 border border-emerald-200 hover:bg-emerald-100"}`}
                >
                  {g.activo !== false ? "🚫 Dar de baja curso" : "✅ Activar curso"}
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* 5. BAJA DE ESTUDIANTES */}
      {seccion === "baja-estudiantes" && (
        <div className="bg-white border border-slate-200 rounded-xl overflow-hidden shadow-sm">
          <div className="p-4 border-b border-slate-100 flex items-center justify-between flex-wrap gap-3">
            <div>
              <h2 className="text-sm font-bold text-slate-700">Control y Baja de Estudiantes</h2>
              <p className="text-xs text-slate-400">Desactiva o retira estudiantes del sistema institucional</p>
            </div>
            <input
              type="text"
              value={busquedaEstudiante}
              onChange={e => setBusquedaEstudiante(e.target.value)}
              placeholder="Buscar por cédula o nombre..."
              className="px-3 py-1.5 border border-slate-200 rounded-lg text-xs bg-slate-50 focus:outline-none focus:ring-2 focus:ring-blue-500 w-64"
            />
          </div>

          <table className="w-full text-sm">
            <thead>
              <tr style={{ backgroundColor: PRIMARY }} className="text-white text-xs">
                <th className="text-left px-4 py-3 font-semibold">Cédula</th>
                <th className="text-left px-4 py-3 font-semibold">Estudiante</th>
                <th className="text-center px-4 py-3 font-semibold">Estado</th>
                <th className="text-center px-4 py-3 font-semibold">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {estudiantesFiltrados.slice(0, 15).map((est, i) => (
                <tr key={est.idEstudiante} className={`border-t border-slate-100 hover:bg-slate-50 transition ${i % 2 === 0 ? "" : "bg-slate-50/50"}`}>
                  <td className="px-4 py-3 font-mono text-xs text-slate-500">{est.cedula || "—"}</td>
                  <td className="px-4 py-3 font-medium text-slate-700">{est.apellidos} {est.nombres}</td>
                  <td className="px-4 py-3 text-center">
                    <span className={`text-[10px] font-bold px-2.5 py-0.5 rounded-full ${est.activo !== false ? "bg-emerald-100 text-emerald-700" : "bg-rose-100 text-rose-700"}`}>
                      {est.activo !== false ? "ACTIVO" : "INACTIVO / BAJA"}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-center">
                    <button
                      onClick={() => handleToggleEstadoEstudiante(est)}
                      className={`px-3 py-1 text-xs font-semibold rounded-lg transition ${est.activo !== false ? "bg-rose-50 text-rose-700 hover:bg-rose-100 border border-rose-200" : "bg-emerald-50 text-emerald-700 hover:bg-emerald-100 border border-emerald-200"}`}
                    >
                      {est.activo !== false ? "Dar de baja" : "Reactivar"}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* 6. DATOS INSTITUCIONALES */}
      {seccion === "escuela" && (
        <div className="bg-white border border-slate-200 rounded-xl p-6 shadow-sm max-w-3xl">
          <h2 className="text-sm font-bold text-slate-700 mb-1">Información Oficial de la Institución</h2>
          <p className="text-xs text-slate-400 mb-5">Datos que aparecen impresos en certificados, fichas de matrícula y reportes en PDF</p>

          <form onSubmit={(e) => { e.preventDefault(); toast.success("Datos institucionales guardados", "Información oficial actualizada."); }} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="col-span-2">
                <label className="block text-xs font-semibold text-slate-600 mb-1">Nombre Oficial de la Institución *</label>
                <input
                  type="text"
                  required
                  value={escuela.nombre}
                  onChange={(e) => setEscuela({ ...escuela, nombre: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-slate-50 font-semibold text-slate-800"
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

              <div className="col-span-2">
                <label className="block text-xs font-semibold text-slate-600 mb-1">Dirección Institucional</label>
                <input
                  type="text"
                  value={escuela.direccion}
                  onChange={(e) => setEscuela({ ...escuela, direccion: e.target.value })}
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
    </Layout>
  );
}
