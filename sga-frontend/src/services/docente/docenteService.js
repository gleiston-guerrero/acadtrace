import axios from "axios";

const API_PRINCIPAL = "http://localhost:8080/api";

const getHeaders = () => {
    const token = localStorage.getItem("token");
    return { Authorization: `Bearer ${token}` };
};

export const getMisAsignaciones = async () => {
    const response = await axios.get(`${API_PRINCIPAL}/docentes/mis-asignaciones`, { headers: getHeaders() });
    return response.data;
};

export const getActividadesPorAsignacion = async (asignacionId) => {
    const response = await axios.get(`${API_PRINCIPAL}/docente/actividades/asignacion/${asignacionId}`, { headers: getHeaders() });
    return response.data;
};

export const getActividadById = async (id) => {
    const response = await axios.get(`${API_PRINCIPAL}/docente/actividades/${id}`, { headers: getHeaders() });
    return response.data;
};

export const createActividad = async (data) => {
    const response = await axios.post(`${API_PRINCIPAL}/docente/actividades`, data, { headers: getHeaders() });
    return response.data;
};

export const updateActividad = async (id, data) => {
    const response = await axios.put(`${API_PRINCIPAL}/docente/actividades/${id}`, data, { headers: getHeaders() });
    return response.data;
};

export const deleteActividad = async (id) => {
    const response = await axios.delete(`${API_PRINCIPAL}/docente/actividades/${id}`, { headers: getHeaders() });
    return response.data;
};
