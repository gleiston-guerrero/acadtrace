import { MICROSERVICIOS } from "../config/microservicios";

async function detectarHostVivo(hosts) {
  const currentOrigin = typeof window !== "undefined" ? window.location.origin : "";
  // Excluir el origen actual para evitar bucles hacia sí mismo
  const targetHosts = hosts.filter((h) => h !== currentOrigin);

  for (const host of targetHosts) {
    try {
      const ctrl = new AbortController();
      const timer = setTimeout(() => ctrl.abort(), 2000);
      await fetch(host, { mode: "no-cors", signal: ctrl.signal });
      clearTimeout(timer);
      return host;
    } catch (_) {
      // Intenta el siguiente puerto
    }
  }
  return targetHosts[0] || null;
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
    token: sesion.token || localStorage.getItem("token") || "",
    username: sesion.username || localStorage.getItem("username") || "",
    roles: JSON.stringify(sesion.roles || []),
    primerIngreso: String(sesion.primerIngreso || false),
  });

  const idUsuario = Number(sesion.idUsuario || localStorage.getItem("userId"));
  if (Number.isInteger(idUsuario) && idUsuario > 0) {
    params.set("idUsuario", String(idUsuario));
  }

  window.location.href = `${host}/#${params.toString()}`;
  return true;
}