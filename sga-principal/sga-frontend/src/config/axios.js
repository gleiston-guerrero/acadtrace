import axios from "axios";

const host = typeof window !== "undefined" ? window.location.hostname : "localhost";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || `http://${host}:8080`,
});

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
    const isLoginRequest = error.config?.url?.includes("/auth/login");
    if (error.response && (error.response.status === 401 || error.response.status === 403) && !isLoginRequest) {
      // Clear localStorage and redirect to login if unauthorized
      localStorage.clear();
      window.location.href = "/login";
    }
    return Promise.reject(error);
  }
);

export default api;

