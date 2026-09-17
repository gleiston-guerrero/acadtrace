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
  /*
   * SelectorCursos tiene dos niveles reales:
   *
   * 1. Tarjetas de grado, identificadas por el badge exacto "GRADO".
   * 2. Tarjetas de curso, identificadas por el footer exacto recibido
   *    en "accion": "Calificar" o "Tomar asistencia".
   *
   * No se usan XPath, índices globales ni textos parciales de la
   * navegación lateral.
   */

  const cerrarOverlay = async () => {
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
  };

  /*
   * Esperamos que el selector real de grados haya terminado
   * de cargar.
   */
  await expect(
    page.getByRole("heading", {
      name: "Mis grados",
      exact: true,
    })
  ).toBeVisible({
    timeout: 15_000,
  });

  /*
   * Las tarjetas de grado contienen el badge exacto "GRADO".
   * Esto excluye botones del sidebar y otros controles.
   */
  const grados = page
    .getByRole("button")
    .filter({
      has: page.getByText("GRADO", {
        exact: true,
      }),
    });

  await expect(grados.first()).toBeVisible({
    timeout: 15_000,
  });

  const totalGrados = await grados.count();

  expect(
    totalGrados,
    "El docente autenticado no tiene grados disponibles"
  ).toBeGreaterThan(0);

  let grado = grados.first();

  /*
   * E2E_GRADO es solamente una preferencia.
   * Si no existe, se utiliza el primer grado real disponible.
   */
  if (e2e.grado) {
    const gradoPreferido = grados
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

  await expect(grado).toBeVisible();
  await expect(grado).toBeEnabled();

  await cerrarOverlay();

  await grado.click();

  /*
   * Tras elegir un grado, SelectorCursos cambia al segundo nivel.
   * La presencia del botón para volver confirma que esa transición
   * ocurrió realmente.
   */
  await expect(
    page.getByTitle("Volver a grados")
  ).toBeVisible({
    timeout: 15_000,
  });

  /*
   * Cada tarjeta de curso contiene como footer exactamente:
   *
   * "Calificar"
   * o
   * "Tomar asistencia"
   *
   * El texto debe ser exacto para no confundir "Calificar"
   * con el botón lateral "Calificaciones".
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

  const totalCursos = await cursos.count();

  expect(
    totalCursos,
    `No se encontraron cursos disponibles para "${accion}"`
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
     * E2E_CURSO también es únicamente una preferencia.
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

  await cerrarOverlay();

  await curso.click();

  return totalCursos;
}