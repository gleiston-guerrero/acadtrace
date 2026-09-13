import { expect } from "@playwright/test";

export const e2e = {
  baseURL: process.env.E2E_BASE_URL,
  loginURL: process.env.E2E_LOGIN_URL,
  user: process.env.E2E_DOCENTE_USER,
  password: process.env.E2E_DOCENTE_PASSWORD,
  grado: process.env.E2E_GRADO,
  curso: process.env.E2E_CURSO,
  actividad: process.env.E2E_ACTIVIDAD,
};

export const hasLoginConfiguration = Boolean(
  e2e.baseURL && e2e.loginURL && e2e.user && e2e.password
);

export async function loginDocente(page) {
  await page.goto(e2e.loginURL);
  await page.getByPlaceholder(/ingresa tu usuario/i).fill(e2e.user);
  await page.getByPlaceholder(/ingresa tu contraseña/i).fill(e2e.password);
  await page.getByRole("button", { name: /ingresar/i }).click();
  await page.waitForURL((url) => url.origin === new URL(e2e.baseURL).origin, {
    timeout: 60_000,
  });
  await expect(page.getByRole("heading", { name: /bienvenido/i })).toBeVisible();
}

export async function abrirModulo(page, nombre) {
  await page.goto(e2e.baseURL);
  await page.getByRole("button", { name: new RegExp(nombre, "i") }).click();
}

export async function seleccionarCurso(page, accion) {
  await expect(page.getByRole("heading", { name: "Mis grados" })).toBeVisible();
  const grado = e2e.grado
    ? page.getByRole("button").filter({ hasText: e2e.grado }).first()
    : page.getByRole("button").filter({ hasText: "Abrir cursos" }).first();
  await grado.click();
  await expect(page.getByText("Elige el curso (materia y paralelo)")).toBeVisible();
  const main = page.getByRole("main");

  const curso = e2e.curso
    ? main.getByRole("button").filter({ hasText: e2e.curso }).first()
    : main.getByRole("button").filter({ hasText: accion }).first();

  await expect(curso).toBeVisible();
  await curso.click();

  await expect(page.getByText("Elige el curso (materia y paralelo)")).toBeHidden();
}
