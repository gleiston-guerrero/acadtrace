import { test, expect } from "@playwright/test";

import {
  abrirModulo,
  e2e,
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
    // El recorrido puede revisar varios trimestres, semanas y actividades.
    test.setTimeout(120_000);

    await abrirModulo(page, "Calificaciones");
    await seleccionarCurso(page, "Calificar");

    const semana = page.getByRole("spinbutton").first();
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

    expect(
      periodos.length,
      "No se encontraron trimestres disponibles para ejecutar E10"
    ).toBeGreaterThan(0);

    let nota = null;
    let original = "";
    let indiceNota = -1;

    for (const periodo of periodos) {
      if (nota) {
        break;
      }

      await trimestre.selectOption(periodo);
      await expect(trimestre).toHaveValue(periodo);

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
        // Si no contiene una nota reutilizable, el test continúa
        // automáticamente con las demás actividades.
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

          const nombreActividad = (
            await tarjeta.locator("p").first().innerText()
          ).trim();

          await tarjeta.click();

          // Esperamos que la actividad seleccionada sea realmente
          // la que se encuentra cargada en la zona de calificaciones.
          await expect(
            page.getByRole("heading", {
              name: nombreActividad,
              exact: true,
            })
          ).toBeVisible({ timeout: 15_000 });

          // Se buscan únicamente los campos numéricos de la tabla.
          // De esta forma no se confunde el selector de semana con
          // los campos correspondientes a las calificaciones.
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

            if (!(await candidata.isEditable())) {
              continue;
            }

            const valor = (
              await candidata.inputValue()
            ).trim();

            // E10 debe modificar una calificación que ya existe
            // para poder restaurar exactamente el estado inicial.
            if (valor === "") {
              continue;
            }

            if (!Number.isFinite(Number(valor))) {
              continue;
            }

            nota = candidata;
            original = valor;
            indiceNota = i;

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

    expect(
      indiceNota,
      "No se pudo identificar la posición de la calificación seleccionada"
    ).toBeGreaterThanOrEqual(0);

    const maxAtributo = await nota.getAttribute("max");
    const minAtributo = await nota.getAttribute("min");

    const maximo =
      maxAtributo === null
        ? 10
        : Number(maxAtributo);

    const minimo =
      minAtributo === null
        ? 0
        : Number(minAtributo);

    const actual = Number(original);

    expect(
      Number.isFinite(actual),
      `La nota original no es numérica: "${original}"`
    ).toBeTruthy();

    expect(
      Number.isFinite(maximo),
      `El máximo permitido no es numérico: "${maxAtributo}"`
    ).toBeTruthy();

    expect(
      Number.isFinite(minimo),
      `El mínimo permitido no es numérico: "${minAtributo}"`
    ).toBeTruthy();

    let temporal;

    if (actual + 0.01 <= maximo) {
      temporal = actual + 0.01;
    } else if (actual - 0.01 >= minimo) {
      temporal = actual - 0.01;
    } else {
      throw new Error(
        `No es posible modificar temporalmente la nota ${actual} ` +
          `dentro del rango ${minimo}-${maximo}`
      );
    }

    const temporalTexto = temporal.toFixed(2);

    const notasTabla = page
      .getByRole("table")
      .getByRole("spinbutton");

    const botonGuardar = page.getByRole("button", {
      name: /Guardar (notas|calificaciones)/i,
    });

    await expect(botonGuardar).toBeVisible();

    /**
     * El formulario productivo puede contener muchas notas ya registradas.
     * Para que E10 pruebe una única escritura real y sea determinista,
     * dejamos temporalmente vacíos los demás campos y conservamos solo
     * la calificación que se está verificando.
     *
     * Esto modifica únicamente el estado del formulario. Los demás valores
     * no se envían al backend y son recuperados nuevamente cuando la
     * aplicación vuelve a consultar las calificaciones.
     */
    const prepararUnicaNota = async (valor) => {
      const total = await notasTabla.count();

      expect(
        total,
        "La tabla perdió la calificación seleccionada"
      ).toBeGreaterThan(indiceNota);

      for (
        let i = 0;
        i < total;
        i += 1
      ) {
        if (i === indiceNota) {
          continue;
        }

        const campo = notasTabla.nth(i);

        if (!(await campo.isVisible())) {
          continue;
        }

        if (!(await campo.isEditable())) {
          continue;
        }

        const valorActual = (
          await campo.inputValue()
        ).trim();

        if (valorActual !== "") {
          await campo.fill("");
        }
      }

      const objetivo = notasTabla.nth(indiceNota);

      await expect(objetivo).toBeVisible();
      await expect(objetivo).toBeEditable();

      await objetivo.fill(valor);
    };

    /**
     * Ejecuta una escritura real mediante la interfaz y comprueba:
     *
     * 1. Que exista una petición POST o PATCH a la API real.
     * 2. Que el servidor responda satisfactoriamente.
     * 3. Que el frontend vuelva a consultar las calificaciones.
     * 4. Que el valor mostrado después de la recarga coincida con
     *    el valor persistido.
     *
     * La prueba no depende de mensajes visuales temporales.
     */
    const guardarYVerificar = async (valor) => {
      await prepararUnicaNota(valor);

      await expect(botonGuardar).toBeEnabled();

      const escrituraPromise = page.waitForResponse(
        (response) => {
          const metodo =
            response.request().method();

          return (
            response.url().includes("/calificaciones/") &&
            (metodo === "POST" || metodo === "PATCH")
          );
        },
        {
          timeout: 30_000,
        }
      );

      // La aplicación vuelve a consultar las notas después
      // de completar correctamente la escritura.
      const recargaPromise = page
        .waitForResponse(
          (response) =>
            response.url().includes("/calificaciones/") &&
            response.request().method() === "GET",
          {
            timeout: 30_000,
          }
        )
        .catch(() => null);

      await botonGuardar.click();

      const escritura = await escrituraPromise;

      expect(
        escritura.ok(),
        `La API respondió HTTP ${escritura.status()} ` +
          "al guardar la calificación"
      ).toBeTruthy();

      const recarga = await recargaPromise;

      expect(
        recarga,
        "La aplicación no volvió a consultar las calificaciones después del guardado"
      ).not.toBeNull();

      expect(
        recarga.ok(),
        `La API respondió HTTP ${recarga.status()} ` +
          "al recargar las calificaciones"
      ).toBeTruthy();

      // Se comprueba el valor que aparece después de la recarga
      // realizada por la aplicación, no solamente el valor escrito
      // previamente en el campo del navegador.
      await expect
        .poll(
          async () => {
            const campo =
              notasTabla.nth(indiceNota);

            if (!(await campo.isVisible())) {
              return Number.NaN;
            }

            const valorRecargado =
              await campo.inputValue();

            return Number(valorRecargado);
          },
          {
            timeout: 15_000,
            message:
              "La interfaz no reflejó la calificación persistida por el backend",
          }
        )
        .toBeCloseTo(Number(valor), 2);
    };

    try {
      // Primera escritura: modifica temporalmente una nota real.
      await guardarYVerificar(temporalTexto);
    } finally {
      // La restauración se intenta incluso si una comprobación posterior
      // a la primera escritura falla.
      await guardarYVerificar(original);
    }

    // Evidencia final de que E10 dejó el sistema exactamente
    // en el estado académico existente antes de comenzar la prueba.
    await expect
      .poll(
        async () => {
          const campo =
            notasTabla.nth(indiceNota);

          if (!(await campo.isVisible())) {
            return Number.NaN;
          }

          const valorRestaurado =
            await campo.inputValue();

          return Number(valorRestaurado);
        },
        {
          timeout: 15_000,
          message:
            "La calificación original no quedó restaurada",
        }
      )
      .toBeCloseTo(actual, 2);
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