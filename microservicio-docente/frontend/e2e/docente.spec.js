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

        const notasTabla = page
      .getByRole("table")
      .getByRole("spinbutton");

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

    /*
     * Se obtiene una nota temporal que no coincida con ninguna
     * otra nota visible de la tabla. De esta forma podemos
     * identificar exactamente la petición HTTP correspondiente
     * a la calificación modificada por E10.
     */
    const valoresExistentes = new Set();
    const totalCampos = await notasTabla.count();

    for (let i = 0; i < totalCampos; i += 1) {
      const campo = notasTabla.nth(i);

      if (!(await campo.isVisible())) {
        continue;
      }

      const valor = (
        await campo.inputValue()
      ).trim();

      if (
        valor !== "" &&
        Number.isFinite(Number(valor))
      ) {
        valoresExistentes.add(
          Number(valor).toFixed(2)
        );
      }
    }

    let temporal = null;

    for (
      let centesimos = 1;
      centesimos <= 100 && temporal === null;
      centesimos += 1
    ) {
      for (const signo of [1, -1]) {
        const candidata = Number(
          (
            actual +
            signo * (centesimos / 100)
          ).toFixed(2)
        );

        if (
          candidata < minimo ||
          candidata > maximo
        ) {
          continue;
        }

        if (
          candidata === actual ||
          valoresExistentes.has(
            candidata.toFixed(2)
          )
        ) {
          continue;
        }

        temporal = candidata;
        break;
      }
    }

    expect(
      temporal,
      "No se encontró un valor temporal único dentro del rango permitido"
    ).not.toBeNull();

    const temporalTexto =
      temporal.toFixed(2);

    const botonGuardar = page.getByRole(
      "button",
      {
        name: /Guardar (notas|calificaciones)/i,
      }
    );

    await expect(botonGuardar).toBeVisible();
    await expect(botonGuardar).toBeEnabled();

    /*
     * Escribe únicamente sobre el campo seleccionado.
     * No se eliminan ni modifican las demás notas del formulario.
     */
    const prepararNota = async (valor) => {
      const objetivo =
        notasTabla.nth(indiceNota);

      await expect(objetivo).toBeVisible();
      await expect(objetivo).toBeEditable();

      await objetivo.fill(valor);

      await expect
        .poll(
          async () =>
            Number(
              await objetivo.inputValue()
            ),
          {
            timeout: 5_000,
            message:
              "El campo no recibió la calificación esperada",
          }
        )
        .toBeCloseTo(Number(valor), 2);

      await objetivo.blur();
    };

    let idCalificacionPersistida = null;

    /*
     * Guarda mediante la interfaz real y valida directamente
     * la petición y la respuesta del backend.
     *
     * No se depende de un mensaje temporal ni del tiempo que
     * tarde React en volver a dibujar la tabla.
     */
    const guardarYVerificar = async (
      valor,
      idEsperado = null
    ) => {
      await prepararNota(valor);

      const respuestaPromise =
        page.waitForResponse(
          (response) => {
            const request =
              response.request();

            if (
              request.method() !== "PATCH" ||
              !response
                .url()
                .includes("/calificaciones/")
            ) {
              return false;
            }

            if (
              idEsperado !== null &&
              !response
                .url()
                .includes(
                  `/calificaciones/${idEsperado}/`
                )
            ) {
              return false;
            }

            try {
              const payload =
                request.postDataJSON();

              return (
                Number(payload?.nota) ===
                Number(valor)
              );
            } catch {
              return false;
            }
          },
          {
            timeout: 30_000,
          }
        );

      await botonGuardar.click();

      const respuesta =
        await respuestaPromise;

      expect(
        respuesta.ok(),
        `La API respondió HTTP ${respuesta.status()} al guardar la calificación`
      ).toBeTruthy();

      expect(
        respuesta.request().method(),
        "Una calificación existente debe actualizarse mediante PATCH"
      ).toBe("PATCH");

      const cuerpo =
        await respuesta.json();

      expect(
        Number(cuerpo.nota),
        "El backend no devolvió la nota enviada"
      ).toBeCloseTo(Number(valor), 2);

      expect(
        cuerpo.id_calificacion,
        "El backend no devolvió el identificador de la calificación"
      ).toBeTruthy();

      if (idEsperado !== null) {
        expect(
          String(cuerpo.id_calificacion),
          "La restauración modificó una calificación distinta"
        ).toBe(String(idEsperado));
      }

      idCalificacionPersistida =
        cuerpo.id_calificacion;

      return cuerpo;
    };

    try {
      /*
       * Primera escritura:
       * cambia temporalmente una calificación real.
       */
      await guardarYVerificar(
        temporalTexto
      );
    } finally {
      /*
       * Segunda escritura:
       * restaura exactamente el mismo registro y su
       * valor original, incluso si una comprobación
       * posterior a la primera escritura falla.
       */
      await prepararNota(original);

      if (
        idCalificacionPersistida !== null
      ) {
        await guardarYVerificar(
          original,
          idCalificacionPersistida
        );
      } else {
        /*
         * Si no llegó a confirmarse ninguna escritura,
         * el campo vuelve localmente a su valor inicial.
         */
        await expect
          .poll(
            async () =>
              Number(
                await notasTabla
                  .nth(indiceNota)
                  .inputValue()
              ),
            {
              timeout: 5_000,
              message:
                "No se pudo recuperar la nota original en el formulario",
            }
          )
          .toBeCloseTo(actual, 2);
      }
    }

    expect(
      idCalificacionPersistida,
      "E10 no pudo verificar una escritura real de calificación"
    ).not.toBeNull();
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