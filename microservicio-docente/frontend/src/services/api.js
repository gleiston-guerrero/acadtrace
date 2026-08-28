import axios from "axios";

// ─────────────────────────────────────────────────────────────
// Bases públicas del despliegue. El Gateway cubre identidad, asignaciones,
// actividades y asistencia; Django REST conserva recursos aún no expuestos allí.
// ─────────────────────────────────────────────────────────────
const host = typeof window !== "undefined" ? window.location.hostname : "localhost";
export const API_GATEWAY_BASE = `http://${host}:8080/api`;
export const DOCENTE_API_BASE = `http://${host}:8081/api/docente`;
export const PRINCIPAL_LOGIN_URL = `http://${host}:5174/login`;
const API = API_GATEWAY_BASE;
const API_DOCENTE_REST = DOCENTE_API_BASE;

const authHeaders = () => {
  const token = localStorage.getItem("token");
  return { Authorization: `Bearer ${token}` };
};

// ─── ASIGNACIONES (backend principal) ────────────────────────
export const getMisAsignaciones = () =>
  axios.get(`${API}/docentes/mis-asignaciones`, { headers: authHeaders() });

export const getEstudiantesPorAsignacion = (asignacionId) =>
  axios.get(`${API}/docentes/asignaciones/${asignacionId}/estudiantes`, {
    headers: authHeaders(),
  });

export const getAnoLectivoActual = () =>
  axios.get(`${API}/anos-lectivos/actual`, { headers: authHeaders() });

// ─── ACTIVIDADES (Java → gRPC → Django) ──────────────────────
// Nota: el body viaja en camelCase porque Java usa ProtobufHttpMessageConverter.
export const getActividades = (idAsignacion, idPeriodo) => {
  const params = { idAsignacion };
  if (idPeriodo) params.idPeriodo = idPeriodo;
  return axios.get(`${API}/docente/actividades`, { params, headers: authHeaders() });
};

export const getActividad = (id) =>
  axios.get(`${API}/docente/actividades/${id}`, { headers: authHeaders() });

export const createActividad = (data) =>
  axios.post(`${API}/docente/actividades`, data, { headers: authHeaders() });

export const updateActividad = (id, data) =>
  axios.put(`${API}/docente/actividades/${id}`, data, { headers: authHeaders() });

export const deleteActividad = (id) =>
  axios.delete(`${API}/docente/actividades/${id}`, { headers: authHeaders() });

// ─── ASISTENCIAS (Java → gRPC → Django) ──────────────────────
export const getAsistenciaPorAsignacion = (asignacionId, fecha, idPeriodo = 0) => {
  let url = `${API}/docente/asistencias/asignacion/${asignacionId}?fecha=${fecha}`;
  if (idPeriodo > 0) url += `&idPeriodo=${idPeriodo}`;
  return axios.get(url, { headers: authHeaders() });
};

export const getResumenAsistencia = (asignacionId, idPeriodo = 0) => {
  let url = `${API}/docente/asistencias/asignacion/${asignacionId}/resumen`;
  if (idPeriodo > 0) url += `?idPeriodo=${idPeriodo}`;
  return axios.get(url, { headers: authHeaders() });
};

export const registrarAsistenciaGrupal = (data) =>
  axios.post(`${API}/docente/asistencias/masivo`, data, { headers: authHeaders() });

export const actualizarAsistencia = (id, data) =>
  axios.put(`${API}/docente/asistencias/${id}`, data, { headers: authHeaders() });

// ─── CALIFICACIONES (Java → gRPC → Django) ───────────────────
export const getCalificaciones = (idMatricula, trimestre) =>
  axios.get(`${API}/rpc/calificaciones/${idMatricula}/${trimestre}`, { headers: authHeaders() });

export const registrarCalificacion = (data) =>
  axios.post(`${API}/rpc/calificaciones/registrar`, data, { headers: authHeaders() });

export const getPromedioFormativo = (idMatricula, trimestre) =>
  axios.get(`${API}/rpc/calificaciones/promedio-formativo/${idMatricula}/${trimestre}`, { headers: authHeaders() });

export const getPromedioFinal = (idMatricula, trimestre) =>
  axios.get(`${API}/rpc/calificaciones/promedio-final/${idMatricula}/${trimestre}`, { headers: authHeaders() });

// Notas ya registradas de una actividad (para precargar la tabla de calificar).
export const getCalificacionesPorActividad = (idActividad) =>
  axios.get(`${API_DOCENTE_REST}/calificaciones/`, { params: { id_actividad: idActividad }, headers: authHeaders() });

export const guardarCalificacion = (data, idCalificacion) => idCalificacion
  ? axios.patch(`${API_DOCENTE_REST}/calificaciones/${idCalificacion}/`, data, { headers: authHeaders() })
  : axios.post(`${API_DOCENTE_REST}/calificaciones/`, data, { headers: authHeaders() });

// ─── PERÍODOS DE EVALUACIÓN (aún solo REST en Django) ────────
// TODO: exponer como endpoint en el gateway Java cuando exista.
export const getPeriodos = () =>
  axios.get(`${API}/docente/actividades/periodos`, { headers: authHeaders() });

export const getAulaVirtualResumen = (asignaciones) => {
  const params = new URLSearchParams();
  asignaciones.forEach((idAsignacion) => params.append("id_asignacion", idAsignacion));
  return axios.get(`${API_DOCENTE_REST}/aula-virtual/resumen/?${params.toString()}`, {
    headers: authHeaders(),
  });
};

export const getAulaVirtualSemanas = (idAsignacion) =>
  axios.get(`${API_DOCENTE_REST}/aula-virtual/${idAsignacion}/semanas/`, {
    headers: authHeaders(),
  });

export const getAnuncios = (idAsignacion) =>
  axios.get(`${API_DOCENTE_REST}/anuncios/`, { params: { id_asignacion: idAsignacion }, headers: authHeaders() });

export const createAnuncio = (data) =>
  axios.post(`${API_DOCENTE_REST}/anuncios/`, data, { headers: authHeaders() });

export const deleteAnuncio = (idAnuncio) =>
  axios.delete(`${API_DOCENTE_REST}/anuncios/${idAnuncio}/`, { headers: authHeaders() });

export const getMateriales = (idAsignacion) =>
  axios.get(`${API_DOCENTE_REST}/materiales/`, { params: { id_asignacion: idAsignacion }, headers: authHeaders() });

export const createMaterial = (data) =>
  axios.post(`${API_DOCENTE_REST}/materiales/`, data, { headers: authHeaders() });

export const updateMaterial = (idMaterial, data) =>
  axios.patch(`${API_DOCENTE_REST}/materiales/${idMaterial}/`, data, { headers: authHeaders() });

export const deleteMaterial = (idMaterial) =>
  axios.delete(`${API_DOCENTE_REST}/materiales/${idMaterial}/`, { headers: authHeaders() });

export const getSeguimientos = (params) =>
  axios.get(`${API_DOCENTE_REST}/seguimiento/`, { params });

export const createSeguimiento = (data) =>
  axios.post(`${API_DOCENTE_REST}/seguimiento/`, data);

export const updateSeguimiento = (idSeguimiento, data) =>
  axios.patch(`${API_DOCENTE_REST}/seguimiento/${idSeguimiento}/`, data);

export const deleteSeguimiento = (idSeguimiento) =>
  axios.delete(`${API_DOCENTE_REST}/seguimiento/${idSeguimiento}/`);
