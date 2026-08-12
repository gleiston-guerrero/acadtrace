import axios from "axios";

// ─────────────────────────────────────────────────────────────
// El frontend SOLO habla con el SGA Principal (Java 8080) vía REST.
// El SGA Principal internamente usa gRPC hacia el microservicio
// docente (Django) para actividades, asistencia y calificaciones.
// (Patrón API Gateway — el frontend nunca habla gRPC ni toca Django.)
// ─────────────────────────────────────────────────────────────
const API = "http://localhost:8080/api";

// Datos de referencia (períodos) que aún solo expone Django por REST.
const API_DOCENTE_REST = "http://localhost:8081/api/docente";

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
  axios.get(`${API_DOCENTE_REST}/calificaciones/`, { params: { id_actividad: idActividad } });

// ─── PERÍODOS DE EVALUACIÓN (aún solo REST en Django) ────────
// TODO: exponer como endpoint en el gateway Java cuando exista.
export const getPeriodos = () =>
  axios.get(`${API_DOCENTE_REST}/periodos-evaluacion/`);

export const getAnuncios = (idAsignacion) =>
  axios.get(`${API_DOCENTE_REST}/anuncios/`, { params: { id_asignacion: idAsignacion } });

export const createAnuncio = (data) =>
  axios.post(`${API_DOCENTE_REST}/anuncios/`, data);

export const deleteAnuncio = (idAnuncio) =>
  axios.delete(`${API_DOCENTE_REST}/anuncios/${idAnuncio}/`);

export const getMateriales = (idAsignacion) =>
  axios.get(`${API_DOCENTE_REST}/materiales/`, { params: { id_asignacion: idAsignacion } });

export const createMaterial = (data) =>
  axios.post(`${API_DOCENTE_REST}/materiales/`, data);

export const updateMaterial = (idMaterial, data) =>
  axios.patch(`${API_DOCENTE_REST}/materiales/${idMaterial}/`, data);

export const deleteMaterial = (idMaterial) =>
  axios.delete(`${API_DOCENTE_REST}/materiales/${idMaterial}/`);

export const getSeguimientos = (params) =>
  axios.get(`${API_DOCENTE_REST}/seguimiento/`, { params });

export const createSeguimiento = (data) =>
  axios.post(`${API_DOCENTE_REST}/seguimiento/`, data);

export const updateSeguimiento = (idSeguimiento, data) =>
  axios.patch(`${API_DOCENTE_REST}/seguimiento/${idSeguimiento}/`, data);

export const deleteSeguimiento = (idSeguimiento) =>
  axios.delete(`${API_DOCENTE_REST}/seguimiento/${idSeguimiento}/`);
