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
    const trimestre = page.getByRole("combobox").first();

    await expect(semana).toBeVisible();
    await expect(trimestre).toBeVisible();

    const periodos = await trimestre
      .locator("option")
      .evaluateAll((options) =>
        options
          .map((option) => option.value)
          .filter((value) => value !== "")
      );

    let nota = null;
    let original = "";

    for (const periodo of periodos) {
      if (nota) {
        break;
      }

      await trimestre.selectOption(periodo);

      const maxSemanas =
        Number(await semana.getAttribute("max")) || 1;

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

        const orden = Array.from(
          { length: totalTarjetas },
          (_, indice) => indice
        );

        // Si E2E_ACTIVIDAD está configurada, se intenta primero.
        // Si no sirve, el test continúa con las demás actividades.
        if (e2e.actividad) {
          for (
            let indice = 0;
            indice < totalTarjetas;
            indice += 1
          ) {
            const texto = (
              await tarjetas.nth(indice).innerText()
            ).trim();

            if (texto.includes(e2e.actividad)) {
              const posicion = orden.indexOf(indice);

              if (posicion >= 0) {
                orden.splice(posicion, 1);
                orden.unshift(indice);
              }

              break;
            }
          }
        }

        for (const indice of orden) {
          const tarjeta = tarjetas.nth(indice);

          await tarjeta.click();

          // Solo inputs de notas de la tabla.
          // No incluye el selector numérico de semana.
          const candidatas = page
            .getByRole("table")
            .getByRole("spinbutton");

          const sinEstudiantes = page
            .getByText(/No hay estudiantes/i)
            .first();

          await expect
            .poll(
              async () => {
                if ((await candidatas.count()) > 0) {
                  return true;
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

            const valor = (
              await candidata.inputValue()
            ).trim();

            if (valor === "") {
              continue;
            }

            if (!Number.isFinite(Number(valor))) {
              continue;
            }

            nota = candidata;
            original = valor;
            break;
          }

          if (nota) {
            break;
          }
        }
      }
    }

    expect(
      nota,
      "No se encontró ninguna calificación previa restaurable en el curso seleccionado"
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