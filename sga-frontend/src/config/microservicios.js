// Configuración de los microservicios-frontend para el handoff de sesión (SSO).
//
// Como Vite toma el siguiente puerto libre si el principal está ocupado
// (3000 → 3001 → 3002 ...), listamos VARIAS "puertas" por microservicio.
// Al iniciar sesión, el principal detecta automáticamente en cuál está vivo.
//
// Para agregar más puertos, solo añade más entradas al arreglo "hosts".
export const MICROSERVICIOS = {
  DOCENTE: {
    nombre: "Portal Docente",
    hosts: [
      "http://localhost:3000",
      "http://localhost:3001",
      "http://localhost:3002",
      "http://localhost:3003",
      "http://localhost:3004",
    ],
  },
  SECRETARIA: {
    nombre: "Portal Secretaría",
    hosts: [
      "http://localhost:5174",
      "http://localhost:4000",
      "http://localhost:4001",
      "http://localhost:4002",
      "http://localhost:4003",
      "http://localhost:4004",
    ],
  },
  SOPORTE_TECNICO: {
    nombre: "Portal Soporte",
    hosts: [
      "http://localhost:5177",
      "http://localhost:6001",
      "http://localhost:6002",
      "http://localhost:6003",
      "http://localhost:6004",
    ],
  },
};
