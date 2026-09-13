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
    // El recorrido puede revisar varios cursos, trimestres, semanas y actividades.
    test.setTimeout(240_000);

    let nota = null;
    let original = "";
    let indiceNota = -1;
    let totalCursos = 0;

    /*
     * E10 no depende de que el primer curso tenga una nota previa.
     * Se recorren los cursos disponibles hasta encontrar una
     * calificacion existente que pueda modificarse y restaurarse.
     */
    const buscarNotaRestaurable = async (indiceCurso) => {
      await abrirModulo(page, "Calificaciones");

      totalCursos = await seleccionarCurso(
        page,
        "Calificar",
        indiceCurso
      );

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

      if (periodos.length === 0) {
        return;
      }

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

    };

    await buscarNotaRestaurable(0);

    for (
      let indiceCurso = 1;
      indiceCurso < totalCursos && !nota;
      indiceCurso += 1
    ) {
      await buscarNotaRestaurable(indiceCurso);
    }

    expect(
      nota,
      "No se encontro ninguna calificacion previa restaurable en los cursos disponibles"
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
     * Deja una sola calificación no vacía en el formulario.
     * El frontend productivo guarda todas las notas no vacías,
     * por lo que así E10 provoca exactamente una escritura real.
     *
     * Los demás campos se vacían solamente en el estado del
     * formulario; no se envían al backend y no se eliminan.
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
        const campo = notasTabla.nth(i);

        if (!(await campo.isVisible())) {
          continue;
        }

        if (!(await campo.isEditable())) {
          continue;
        }

        if (i === indiceNota) {
          await campo.fill(valor);
          continue;
        }

        const valorActual = (
          await campo.inputValue()
        ).trim();

        if (valorActual !== "") {
          await campo.fill("");
        }
      }

      const objetivo =
        notasTabla.nth(indiceNota);

      await expect(objetivo).toBeVisible();
      await expect(objetivo).toBeEditable();

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
    };

    /*
     * Obtiene el JSON enviado realmente al backend.
     */
    const obtenerPayload = (request) => {
      try {
        return request.postDataJSON();
      } catch {
        return null;
      }
    };

    /*
     * Obtiene los campos estables que identifican la
     * calificación, sin incluir la nota porque ese es
     * justamente el valor que E10 modifica y restaura.
     */
    const obtenerIdentidad = (payload) => {
      const entradas =
        Object.entries(payload || {}).filter(
          ([clave]) =>
            clave !== "nota" &&
            /matricula|actividad|trimestre|periodo/i.test(
              clave
            )
        );

      if (entradas.length > 0) {
        return Object.fromEntries(
          entradas
        );
      }

      return Object.fromEntries(
        Object.entries(payload || {}).filter(
          ([clave]) => clave !== "nota"
        )
      );
    };

    const mismaIdentidad = (
      payload,
      identidad
    ) =>
      Object.entries(identidad).every(
        ([clave, valor]) =>
          String(payload?.[clave]) ===
          String(valor)
      );

       let identidadCalificacion = null;

    /*
     * Guarda mediante la interfaz real y comprueba:
     *
     * 1. Que el navegador produzca una petición real.
     * 2. Que sea POST o PATCH sobre calificaciones.
     * 3. Que la petición contenga exactamente la nota esperada.
     * 4. Que el servidor responda con un código satisfactorio.
     * 5. Que al restaurar se utilice la misma calificación.
     *
     * No se depende de mensajes visuales temporales ni de una
     * recarga GET posterior para considerar válida la escritura.
     */
    const guardarYVerificar = async (
      valor,
      identidadEsperada = null,
      guardarIdentidad = false
    ) => {
      await prepararUnicaNota(valor);

      await expect(
        botonGuardar
      ).toBeEnabled();

      /*
       * Como prepararUnicaNota deja un único campo con valor,
       * handleGuardar producirá una sola escritura de calificación.
       */
      await expect
        .poll(
          async () => {
            const total = await notasTabla.count();
            let noVacias = 0;

            for (
              let i = 0;
              i < total;
              i += 1
            ) {
              const campo = notasTabla.nth(i);

              if (!(await campo.isVisible())) {
                continue;
              }

              if (!(await campo.isEditable())) {
                continue;
              }

              const contenido = (
                await campo.inputValue()
              ).trim();

              if (contenido !== "") {
                noVacias += 1;
              }
            }

            return noVacias;
          },
          {
            timeout: 5_000,
            message:
              "E10 debe enviar exactamente una calificación",
          }
        )
        .toBe(1);

      const escrituraPromise =
        page.waitForResponse(
          (response) => {
            const request =
              response.request();

            if (
              !["POST", "PATCH"].includes(
                request.method()
              )
            ) {
              return false;
            }

            if (
              !response
                .url()
                .includes("/calificaciones/")
            ) {
              return false;
            }

            const payload =
              obtenerPayload(request);

            if (!payload) {
              return false;
            }

            if (
              Number(payload.nota) !==
              Number(valor)
            ) {
              return false;
            }

            if (
              identidadEsperada &&
              !mismaIdentidad(
                payload,
                identidadEsperada
              )
            ) {
              return false;
            }

            return true;
          },
          {
            timeout: 30_000,
          }
        );

      await botonGuardar.click();

      const respuesta =
        await escrituraPromise;

      const request =
        respuesta.request();

      const payload =
        obtenerPayload(request);

      /*
       * La identidad se captura inmediatamente después de recibir
       * la escritura. Así, aunque una comprobación posterior falle,
       * el bloque finally puede restaurar el mismo dato.
       */
      const identidadActual =
        obtenerIdentidad(payload);

      if (
        guardarIdentidad &&
        Object.keys(
          identidadActual
        ).length > 0
      ) {
        identidadCalificacion =
          identidadActual;
      }

      expect(
        respuesta.ok(),
        `La API respondió HTTP ${respuesta.status()} al guardar la calificación`
      ).toBeTruthy();

      expect(
        ["POST", "PATCH"],
        "La escritura debe utilizar POST o PATCH"
      ).toContain(
        request.method()
      );

      expect(
        payload,
        "La petición de calificación no contiene un JSON válido"
      ).not.toBeNull();

      expect(
        Number(payload.nota),
        "La petición no contiene la nota esperada"
      ).toBeCloseTo(
        Number(valor),
        2
      );

      expect(
        Object.keys(
          identidadActual
        ).length,
        "No se pudo identificar la calificación enviada"
      ).toBeGreaterThan(0);

      if (identidadEsperada) {
        expect(
          mismaIdentidad(
            payload,
            identidadEsperada
          ),
          "La restauración no corresponde a la misma calificación"
        ).toBeTruthy();
      }

      /*
       * handleGuardar mantiene el botón deshabilitado mientras
       * termina la operación. Esperar a que vuelva a habilitarse
       * garantiza que el ciclo de guardado finalizó.
       */
      await expect(
        botonGuardar
      ).toBeEnabled({
        timeout: 15_000,
      });

      return identidadActual;
    };

    try {
      /*
       * Primera escritura:
       * modifica temporalmente una calificación real.
       */
      await guardarYVerificar(
        temporalTexto,
        null,
        true
      );
    } finally {
      /*
       * La restauración se intenta siempre.
       *
       * Si la primera escritura llegó correctamente al backend,
       * se exige además que la segunda escritura corresponda
       * exactamente a la misma calificación.
       */
      await guardarYVerificar(
        original,
        identidadCalificacion
      );
    }

    expect(
      identidadCalificacion,
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