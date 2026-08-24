import { MICROSERVICIOS } from "../config/microservicios";

async function detectarHostVivo(hosts) {
  for (const host of hosts) {
    try {
      const ctrl = new AbortController();
      const timer = setTimeout(() => ctrl.abort(), 2000);
      await fetch(host, { mode: "no-cors", signal: ctrl.signal });
      clearTimeout(timer);
      return host;
    } catch (_) {
      // Intenta el siguiente
    }
  }
  return hosts[0] || null;
}

export async function redirigirAMicroservicio(rol, sesion) {
  const cfg = MICROSERVICIOS[rol];
  if (!cfg) return false;

  const host = await detectarHostVivo(cfg.hosts);
  if (!host) {
    alert(
      `No se encontró el ${cfg.nombre} activo.\n` +
      `Verifica que el servicio esté encendido e intenta de nuevo.`
    );
    return false;
  }

  const params = new URLSearchParams({
    token: sesion.token,
    username: sesion.username,
    roles: JSON.stringify(sesion.roles),
    primerIngreso: String(sesion.primerIngreso || false),
  });

  const idUsuario = Number(sesion.idUsuario);
  if (Number.isInteger(idUsuario) && idUsuario > 0) {
    params.set("idUsuario", String(idUsuario));
  }

  window.location.href = `${host}/#${params.toString()}`;
  return true;
}