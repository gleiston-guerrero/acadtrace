import { MICROSERVICIOS } from "../config/microservicios";

// Detecta cuál host (puerto) del microservicio está activo, probándolos en orden.
// Usa fetch en modo "no-cors": si algo responde, el puerto está vivo.
async function detectarHostVivo(hosts) {
  for (const host of hosts) {
    try {
      const ctrl = new AbortController();
      const timer = setTimeout(() => ctrl.abort(), 900);
      await fetch(host, { mode: "no-cors", signal: ctrl.signal });
      clearTimeout(timer);
      return host; // respondió → está corriendo aquí
    } catch (_) {
      // no respondió, se prueba el siguiente puerto
    }
  }
  return null;
}

// Entrega la sesión al microservicio del rol indicado y redirige.
// El token viaja en el fragmento (#) del URL: no se envía a ningún servidor.
export async function redirigirAMicroservicio(rol, sesion) {
  const cfg = MICROSERVICIOS[rol];
  if (!cfg) return false;

  const host = await detectarHostVivo(cfg.hosts);
  if (!host) {
    alert(
      `No se encontró el ${cfg.nombre} activo.\n` +
      `Verifica que esté encendido (npm run dev) e intenta de nuevo.`
    );
    return false;
  }

  const params = new URLSearchParams({
    token: sesion.token,
    username: sesion.username,
    roles: JSON.stringify(sesion.roles),
    primerIngreso: String(sesion.primerIngreso),
  });

  window.location.href = `${host}/#${params.toString()}`;
  return true;
}
