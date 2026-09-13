import test from "node:test";
import assert from "node:assert/strict";

import { diasSemana, lunesSemana, toInput, totalSemanas } from "./asistenciaFechas.js";

test("un período con fechas produce los cinco días laborables de la semana", () => {
  const periodo = { fecha_inicio: "2026-09-15", fecha_fin: "2026-12-15" };

  assert.equal(totalSemanas(periodo) > 0, true);
  assert.equal(toInput(lunesSemana(periodo, 1)), "2026-09-14");
  assert.deepEqual(diasSemana(periodo, 1).map(toInput), [
    "2026-09-14", "2026-09-15", "2026-09-16", "2026-09-17", "2026-09-18",
  ]);
});

test("un período sin fechas no rompe el cálculo", () => {
  const periodo = { id_periodo: 1, nombre: "Primer trimestre", activo: true };

  assert.equal(totalSemanas(periodo), 0);
  assert.equal(lunesSemana(periodo, 1), null);
  assert.deepEqual(diasSemana(periodo, 1), []);
});
