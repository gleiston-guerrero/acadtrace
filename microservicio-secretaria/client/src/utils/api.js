import axios from 'axios';

// En producción todo está en el mismo servidor (puerto 3000).
// En desarrollo, Vite hace proxy de /api → :3000 automáticamente.
// Por eso usamos rutas relativas — funciona en ambos casos.

// Login contra sga-principal (Spring Boot en :8080)
export const apiPrincipal = axios.create({
  baseURL: import.meta.env.VITE_API_PRINCIPAL || 'http://localhost:8080/api',
});

// API del microservicio secretario (mismo servidor)
export const api = axios.create({
  baseURL: '/api/secretario',
});

// Inyectar token en todas las peticiones
[apiPrincipal, api].forEach((instance) => {
  instance.interceptors.request.use((config) => {
    const token = localStorage.getItem('token');
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
  });

  instance.interceptors.response.use(
    (r) => r,
    (err) => {
      if (err.response?.status === 401) {
        localStorage.clear();
        window.location.href = 'http://localhost:5173/login';
      }
      return Promise.reject(err);
    }
  );
});

export default api;
