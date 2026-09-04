// Configuración de los microservicios-frontend para el handoff de sesión (SSO).
//
// Como Vite toma el siguiente puerto libre si el principal está ocupado
// (3000 → 3001 → 3002 ...), listamos VARIAS "puertas" por microservicio.
// Al iniciar sesión, el principal detecta automáticamente en cuál está vivo.
//
// Para agregar más puertos, solo añade más entradas al arreglo "hosts".
const host = typeof window !== "undefined" ? window.location.hostname : "localhost";

export const MICROSERVICIOS = {
  DOCENTE: {
    nombre: "Portal Docente",
    hosts: [
      `http://${host}:3000`,
      `http://${host}:3001`,
      `http://${host}:3002`,
      `http://${host}:3003`,
      `http://${host}:3004`,
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
      `http://${host}:5178`,
      `http://${host}:5177`,
      `http://${host}:6001`,
      `http://${host}:6002`,
    ],
  },
};
