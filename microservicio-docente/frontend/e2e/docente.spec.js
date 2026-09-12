import { test, expect } from "@playwright/test";
import {
  abrirModulo,
  e2e,
  hasLoginConfiguration,
  loginDocente,
  seleccionarCurso,
} from "./helpers.js";

test.describe("Frontend Docente conectado al entorno real", () => {
  test.beforeEach(async ({ page }) => {
    test.skip(
      !hasLoginConfiguration,
      "Requiere las variables E2E de acceso al entorno real"
    );

    await loginDocente(page);
  });

  test("login docente autentica y muestra el dashboard", async ({ page }) => {
    await expect(
      page.getByRole("heading", { name: /bienvenido/i })
    ).toBeVisible();

    await expect(
      page.getByRole("button", { name: /calificaciones/i })
    ).toBeVisible();

    await expect(
      page.getByRole("button", { name: /asistencia/i })
    ).toBeVisible();
  });

  test("consulta de cursos navega mediante la interfaz real", async ({ page }) => {
    await abrirModulo(page, "Calificaciones");

    await expect(
      page.getByRole("heading", { name: "Calificaciones" })
    ).toBeVisible();

    await seleccionarCurso(page, "Calificar");
  });

  test("consulta de asistencia muestra la información del curso", async ({
    page,
  }) => {
    await abrirModulo(page, "Asistencia");

    await expect(
      page.getByRole("heading", { name: "Asistencia Docente" })
    ).toBeVisible();

    await seleccionarCurso(page, "Tomar asistencia");

    await expect(
      page.getByText(/Día:|Cargando estudiantes|No hay estudiantes/)
    ).toBeVisible();
  });

  test("registro de calificación restaura la nota original", async ({
    page,
  }) => {
    test.skip(
      !e2e.actividad,
      "Requiere E2E_ACTIVIDAD con una actividad que tenga notas restaurables"
    );

    await abrirModulo(page, "Calificaciones");
    await seleccionarCurso(page, "Calificar");

    const semana = page.getByRole("spinbutton");
    await expect(semana).toBeVisible();

    const maxSemanas =
      Number(await semana.getAttribute("max")) || 1;

    const actividad = page
      .getByRole("main")
      .getByRole("button")
      .filter({ hasText: e2e.actividad });

    let encontrada = false;

    for (let numero = 1; numero <= maxSemanas; numero += 1) {
      await semana.fill(String(numero));

      await expect(
        page.getByText(`Actividades de la semana ${numero}`, {
          exact: true,
        })
      ).toBeVisible();

      if ((await actividad.count()) > 0) {
        encontrada = true;
        break;
      }
    }

    expect(
      encontrada,
      `No se encontró la actividad "${e2e.actividad}" en las semanas del trimestre`
    ).toBeTruthy();

    await actividad.first().click();

    const nota = page.getByPlaceholder("—").first();

    await expect(nota).toBeVisible();

    const original = await nota.inputValue();

    test.skip(
      original === "",
      "La actividad no tiene una nota previa que pueda restaurarse con seguridad"
    );

    const maximo =
      Number(await nota.getAttribute("max")) || 10;

    const actual = Number(original);

    const temporal =
      actual >= 0.01
        ? actual - 0.01
        : Math.min(maximo, actual + 0.01);

    const guardarYEsperar = async () => {
      const respuesta = page.waitForResponse(
        (response) =>
          /\/calificaciones\//.test(response.url()) &&
          ["POST", "PATCH"].includes(
            response.request().method()
          ) &&
          response.ok()
      );

      await page
        .getByRole("button", { name: "Guardar notas" })
        .click();

      await respuesta;
    };

    try {
      await nota.fill(temporal.toFixed(2));

      await guardarYEsperar();

      await expect(
        page.getByText(/Se guardaron \d+ calificaciones/)
      ).toBeVisible();
    } finally {
      await nota.fill(original);

      await guardarYEsperar();

      await expect(
        page.getByText(/Se guardaron \d+ calificaciones/)
      ).toBeVisible();
    }
  });

  test("cierre de sesión vuelve al Login", async ({ page }) => {
    await page
      .getByRole("banner")
      .getByRole("button")
      .last()
      .click();

    await page
      .getByRole("button", { name: "Cerrar sesión" })
      .click();

    await expect(page).toHaveURL(
      new RegExp(
        `^${e2e.loginURL.replace(
          /[.*+?^${}()|[\]\\]/g,
          "\\$&"
        )}`
      )
    );
  });
});

test(
  "acceso sin autenticación a Asistencia es rechazado o redirigido al Login",
  async ({ page }) => {
    test.skip(
      !e2e.baseURL || !e2e.loginURL,
      "Requiere E2E_BASE_URL y E2E_LOGIN_URL"
    );

    await page.goto(
      new URL("/asistencia", e2e.baseURL).toString()
    );

    const loginOrigin = new URL(e2e.loginURL).origin;

    await expect
      .poll(() => new URL(page.url()).origin, {
        timeout: 15_000,
      })
      .toBe(loginOrigin);

    await expect(
      page.getByPlaceholder(/ingresa tu usuario/i)
    ).toBeVisible();
  }
);