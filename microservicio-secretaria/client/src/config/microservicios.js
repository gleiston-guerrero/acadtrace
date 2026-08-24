const host = typeof window !== "undefined" ? window.location.hostname : "localhost";

export const MICROSERVICIOS = {
  DIRECTOR: {
    nombre: "Portal Principal (Administración)",
    hosts: [
      `http://${host}:5173`,
      `http://${host}:5174`,
      `http://${host}:8080`,
    ],
  },
  DOCENTE: {
    nombre: "Portal Docente",
    hosts: [
      `http://${host}:3000`,
      `http://${host}:3001`,
      `http://${host}:3002`,
    ],
  },
  SECRETARIA: {
    nombre: "Portal Secretaría",
    hosts: [
      `http://${host}:5174`,
      `http://${host}:8082`,
      `http://${host}:5175`,
    ],
  },
  SOPORTE_TECNICO: {
    nombre: "Portal Soporte",
    hosts: [
      `http://${host}:8083`,
      `http://${host}:6001`,
      `http://${host}:5178`,
    ],
  },
};