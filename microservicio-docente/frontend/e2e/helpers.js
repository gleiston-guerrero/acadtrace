import { expect } from "@playwright/test";

const REQUIRED_E2E_ENV = [
  "E2E_BASE_URL",
  "E2E_LOGIN_URL",
  "E2E_DOCENTE_USER",
  "E2E_DOCENTE_PASSWORD",
  "E2E_ACTIVIDAD",
];

const faltantes = REQUIRED_E2E_ENV.filter(
  (nombre) => !process.env[nombre]?.trim()
);

if (faltantes.length > 0) {
  throw new Error(
    `Configuración E10 incompleta. Faltan variables obligatorias: ${faltantes.join(
      ", "
    )}`
  );
}

export const e2e = {
  baseURL: process.env.E2E_BASE_URL.trim(),
  loginURL: process.env.E2E_LOGIN_URL.trim(),
  user: process.env.E2E_DOCENTE_USER.trim(),
  password: process.env.E2E_DOCENTE_PASSWORD.trim(),
  actividad: process.env.E2E_ACTIVIDAD.trim(),

  // Preferencias opcionales.
  grado: process.env.E2E_GRADO?.trim() || null,
  curso: process.env.E2E_CURSO?.trim() || null,
};

export async function loginDocente(page) {
  await page.goto(e2e.loginURL, {
    waitUntil: "domcontentloaded",
  });

  await expect(
    page.getByPlaceholder(/ingresa tu usuario/i)
  ).toBeVisible({
    timeout: 15_000,
  });

  await page
    .getByPlaceholder(/ingresa tu usuario/i)
    .fill(e2e.user);

  await page
    .getByPlaceholder(/ingresa tu contraseña/i)
    .fill(e2e.password);

  const botonIngresar = page.getByRole("button", {
    name: /ingresar/i,
  });

  await expect(botonIngresar).toBeVisible();
  await expect(botonIngresar).toBeEnabled();

  await botonIngresar.click();

  await page.waitForURL(
    (url) =>
      url.origin === new URL(e2e.baseURL).origin,
    {
      timeout: 60_000,
      waitUntil: "domcontentloaded",
    }
  );

  await expect(
    page.getByRole("heading", {
      name: /bienvenido/i,
    })
  ).toBeVisible({
    timeout: 15_000,
  });
}

export async function abrirModulo(page, nombre) {
  await page.goto(e2e.baseURL, {
    waitUntil: "domcontentloaded",
  });

  const modulo = page.getByRole("button", {
    name: new RegExp(nombre, "i"),
  });

  await expect(modulo).toBeVisible({
    timeout: 15_000,
  });

  await expect(modulo).toBeEnabled();

  await modulo.click();
}

export async function seleccionarCurso(
  page,
  accion,
  indiceCurso = null
) {
  const encabezado = page.getByRole("heading", {
    name: "Mis grados",
  });

  await expect(encabezado).toBeVisible({
    timeout: 15_000,
  });

  let grado = null;

  /*
   * E2E_GRADO es únicamente una preferencia.
   * Si no existe, E10 utiliza un grado real disponible.
   */
  if (e2e.grado) {
    const gradoPreferido = page
      .getByRole("button")
      .filter({
        hasText: e2e.grado,
      })
      .first();

    if (
      (await gradoPreferido.count()) > 0 &&
      (await gradoPreferido.isVisible())
    ) {
      grado = gradoPreferido;
    }
  }

  /*
   * Busca primero controles cuyo contenido parezca
   * corresponder a un grado académico.
   */
  if (!grado) {
    const gradosAcademicos = page
      .getByRole("button")
      .filter({
        hasText:
          /EGB|BGU|bachillerato|año|ano|grado/i,
      });

    const totalAcademicos =
      await gradosAcademicos.count();

    for (
      let i = 0;
      i < totalAcademicos;
      i += 1
    ) {
      const candidato =
        gradosAcademicos.nth(i);

      if (await candidato.isVisible()) {
        grado = candidato;
        break;
      }
    }
  }

  /*
   * Fallback:
   * toma un botón visible situado después de "Mis grados",
   * descartando controles de navegación conocidos.
   */
  if (!grado) {
    const candidatos = encabezado.locator(
      'xpath=following::*[self::button or @role="button"]'
    );

    const totalCandidatos =
      await candidatos.count();

    for (
      let i = 0;
      i < totalCandidatos;
      i += 1
    ) {
      const candidato =
        candidatos.nth(i);

      if (!(await candidato.isVisible())) {
        continue;
      }

      const textoBoton = (
        await candidato.innerText()
      ).trim();

      if (
        /cerrar sesi[oó]n|calificaciones|asistencia|inicio/i.test(
          textoBoton
        )
      ) {
        continue;
      }

      grado = candidato;
      break;
    }
  }

  expect(
    grado,
    "No existe ningún grado real visible para el docente autenticado"
  ).not.toBeNull();

  await expect(grado).toBeVisible();
  await expect(grado).toBeEnabled();

  await grado.click();

    /*
   * SelectorCursos renderiza cada curso como un botón que contiene
   * exactamente la acción funcional indicada por la página:
   * "Calificar" o "Tomar asistencia".
   *
   * Usar un descendiente con texto exacto evita confundir
   * "Calificar" con el botón lateral "Calificaciones".
   */
  const cursos = page
    .getByRole("button")
    .filter({
      has: page.getByText(accion, {
        exact: true,
      }),
    });

  await expect(cursos.first()).toBeVisible({
    timeout: 15_000,
  });

  const totalCursos =
    await cursos.count();

  expect(
    totalCursos,
    `No se encontraron cursos disponibles con la acción "${accion}"`
  ).toBeGreaterThan(0);

  let curso;

  if (indiceCurso !== null) {
    expect(
      indiceCurso,
      `El índice de curso ${indiceCurso} está fuera del rango disponible`
    ).toBeLessThan(totalCursos);

    curso = cursos.nth(indiceCurso);
  } else {
    curso = cursos.first();

    /*
     * E2E_CURSO es únicamente una preferencia.
     * Si el curso configurado existe, se utiliza.
     * De lo contrario se mantiene el primer curso real disponible.
     */
    if (e2e.curso) {
      const cursoPreferido = cursos
        .filter({
          hasText: e2e.curso,
        })
        .first();

      if (
        (await cursoPreferido.count()) > 0 &&
        (await cursoPreferido.isVisible())
      ) {
        curso = cursoPreferido;
      }
    }
  }

  await expect(curso).toBeVisible();
  await expect(curso).toBeEnabled();

  /*
   * Layout coloca este overlay cuando queda abierto alguno de
   * los menús superiores. Si está presente, se cierra mediante
   * su comportamiento normal antes de pulsar la tarjeta del curso.
   *
   * No se utiliza force:true porque E10 debe interactuar como
   * un navegador real.
   */
  const overlay = page.locator(
    "div.fixed.inset-0.z-20"
  );

  if (
    (await overlay.count()) > 0 &&
    (await overlay.isVisible())
  ) {
    await overlay.click({
      position: {
        x: 5,
        y: 5,
      },
    });

    await expect(overlay).toBeHidden({
      timeout: 5_000,
    });
  }

  await curso.click();

  return totalCursos;
}