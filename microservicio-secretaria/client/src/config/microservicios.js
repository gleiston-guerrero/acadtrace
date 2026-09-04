const host = typeof window !== "undefined" ? window.location.hostname : "localhost";

export const MICROSERVICIOS = {
  DIRECTOR: {
    nombre: "Portal Principal (Administración)",
    hosts: [
      `http://${host}:5173`,
      `http://${host}:5174`,
    ],
  },
  DOCENTE: {
    nombre: "Portal Docente",
    hosts: [
      `http://${host}:3000`,
    ],
  },
  SECRETARIA: {
    nombre: "Portal Secretaría",
    hosts: [
      `http://${host}:5176`,
      `http://${host}:8082`,
    ],
  },
  SOPORTE_TECNICO: {
    nombre: "Portal Soporte",
    hosts: [
      `http://${host}:8083`,
    ],
  },
};