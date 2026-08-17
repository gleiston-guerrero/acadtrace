import axios from "axios";

// Instancia de axios para microservicio-soporte.
// No se establece baseURL forzado a 8080 para permitir que las rutas relativas (/api/soporte/...)
// sean dirigidas por el proxy de Vite al backend de soporte (5178/8083),
// mientras que las peticiones al SGA Principal usan la URL explícita http://localhost:8080.
const api = axios.create();

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && (error.response.status === 401 || error.response.status === 403)) {
      localStorage.clear();
      const host = typeof window !== "undefined" ? window.location.hostname : "localhost";
      window.location.href = `http://${host}:5174/login`;
    }
    return Promise.reject(error);
  }
);

export default api;
