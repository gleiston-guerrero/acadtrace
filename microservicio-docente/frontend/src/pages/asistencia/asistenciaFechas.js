const MS_SEMANA = 7 * 24 * 3600 * 1000;

export const parseFecha = (s) => {
  if (!s) return null;
  const [y, m, d] = s.split("-").map(Number);
  return new Date(y, m - 1, d);
};

export const toInput = (d) =>
  `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;

export const totalSemanas = (per) => {
  if (!per) return 0;
  const ini = parseFecha(per.fecha_inicio), fin = parseFecha(per.fecha_fin);
  if (!ini || !fin) return 0;
  return Math.max(1, Math.ceil((fin - ini) / MS_SEMANA));
};

export const lunesSemana = (per, n) => {
  if (!per) return null;
  const ini = parseFecha(per.fecha_inicio);
  if (!ini) return null;
  const offsetLunes = (ini.getDay() + 6) % 7;
  const lunes1 = new Date(ini.getTime() - offsetLunes * 24 * 3600 * 1000);
  return new Date(lunes1.getTime() + (n - 1) * MS_SEMANA);
};

export const diasSemana = (per, n) => {
  const lunes = lunesSemana(per, n);
  if (!lunes) return [];
  return Array.from({ length: 5 }, (_, i) => new Date(lunes.getTime() + i * 24 * 3600 * 1000));
};
