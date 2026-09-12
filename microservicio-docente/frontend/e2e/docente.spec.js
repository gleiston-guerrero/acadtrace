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
    await abrirModulo(page, "Calificaciones");
    await seleccionarCurso(page, "Calificar");

    const semana = page.getByRole("spinbutton");

    await expect(semana).toBeVisible();
    await page.waitForLoadState('networkidle');

    const maxSemanas =
      Number(await semana.getAttribute("max")) || 1;

    let nota = null;
    let original = "";

    for (
      let numero = 1;
      numero <= maxSemanas && !nota;
      numero += 1
    ) {
      await semana.fill(String(numero));

      const encabezadoSemana = page.getByText(
        `Actividades de la semana ${numero}`,
        { exact: true }
      );

      await expect(encabezadoSemana).toBeVisible();

      const tarjetas = encabezadoSemana
        .locator("..")
        .getByRole("button");

      const totalTarjetas = await tarjetas.count();

      for (
        let indice = 0;
        indice < totalTarjetas && !nota;
        indice += 1
      ) {
        const tarjeta = tarjetas.nth(indice);

        const nombreActividad = (
          await tarjeta.locator("p").first().textContent()
        )?.trim();

        await tarjeta.click();

        if (nombreActividad) {
          await expect(
            page.getByRole("heading", {
              name: nombreActividad,
              exact: true,
            })
          ).toBeVisible();
        }

        const candidatas = page.getByPlaceholder("—");

        const sinEstudiantes = page
          .getByText(/No hay estudiantes/i)
          .first();

        await expect
          .poll(
            async () => {
              const total = await candidatas.count();

              for (let i = 0; i < total; i += 1) {
                if (await candidatas.nth(i).isVisible()) {
                  return true;
                }
              }

              return await sinEstudiantes.isVisible();
            },
            {
              timeout: 15_000,
              message:
                "La actividad no terminó de cargar sus estudiantes y calificaciones",
            }
          )
          .toBe(true);

        const totalNotas = await candidatas.count();

        for (
          let i = 0;
          i < totalNotas;
          i += 1
        ) {
          const candidata = candidatas.nth(i);

          if (!(await candidata.isVisible())) {
            continue;
          }

          const valor = await candidata.inputValue();

          nota = candidata;
          original = valor;
          break;
        }
      }
    }

    expect(
      nota,
      "No se encontró ninguna actividad con una nota previa restaurable"
    ).not.toBeNull();

    const maximo =
      Number(await nota.getAttribute("max")) || 10;

    const actual = Number(original);

    expect(
      Number.isFinite(actual),
      `La nota original no es numérica: "${original}"`
    ).toBeTruthy();

    const temporal =
      actual >= 0.01
        ? actual - 0.01
        : Math.min(
            maximo,
            actual + 0.01
          );

    const guardarYEsperar = async () => {
      const respuestaPromise = page.waitForResponse(
        (response) =>
          /\/calificaciones\//.test(response.url()) &&
          ["POST", "PATCH"].includes(
            response.request().method()
          ),
        {
          timeout: 15_000,
        }
      );

      await page
        .getByRole("button", {
          name: "Guardar notas",
        })
        .click();

      const respuesta = await respuestaPromise;

      expect(
        respuesta.ok(),
        `El guardado de calificaciones respondió HTTP ${respuesta.status()}`
      ).toBeTruthy();
    };

    try {
      await nota.fill(
        temporal.toFixed(2)
      );

      await guardarYEsperar();
      await expect(
        page.getByText(
          /Se guardaron \d+ calificaciones/
        )
      ).toBeVisible({ timeout: 15000 });
    } finally {
      await nota.fill(original);

      await guardarYEsperar();

      await expect(
        page.getByText(
          /Se guardaron \d+ calificaciones/
        )
      ).toBeVisible({ timeout: 15000 });
    }
  });

  test("cierre de sesión vuelve al Login", async ({ page }) => {
    await page
      .getByRole("banner")
      .getByRole("button")
      .last()
      .click();

    await page
      .getByRole("button", {
        name: "Cerrar sesión",
      })
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
      !e2e.baseURL ||
        !e2e.loginURL,
      "Requiere E2E_BASE_URL y E2E_LOGIN_URL"
    );

    await page.goto(
      new URL(
        "/asistencia",
        e2e.baseURL
      ).toString()
    );

    const loginOrigin =
      new URL(
        e2e.loginURL
      ).origin;

    await expect
      .poll(
        () =>
          new URL(
            page.url()
          ).origin,
        {
          timeout: 15_000,
        }
      )
      .toBe(loginOrigin);

    await expect(
      page.getByPlaceholder(
        /ingresa tu usuario/i
      )
    ).toBeVisible();
  }
);