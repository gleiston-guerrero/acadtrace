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
    waitUntil: "domcontentloaded",
  });
  await expect(page.getByRole("heading", { name: /bienvenido/i })).toBeVisible();
}

export async function abrirModulo(page, nombre) {
  await page.goto(e2e.baseURL);
  await page.getByRole("button", { name: new RegExp(nombre, "i") }).click();
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
   * E2E_GRADO es una preferencia.
   * Si el dato configurado ya no existe en produccion,
   * se selecciona un grado real disponible en la interfaz.
   */
  if (e2e.grado) {
    const gradoPreferido = page
      .getByRole("button")
      .filter({ hasText: e2e.grado })
      .first();

    if (
      (await gradoPreferido.count()) > 0 &&
      (await gradoPreferido.isVisible())
    ) {
      grado = gradoPreferido;
    }
  }

  /*
   * Segunda opcion: nombres academicos habituales.
   */
  if (!grado) {
    const gradosAcademicos = page
      .getByRole("button")
      .filter({
        hasText: /EGB|BGU|bachillerato|a?o|grado/i,
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
   * Ultimo fallback:
   * toma el primer control interactivo visible que aparece
   * despues del encabezado "Mis grados".
   *
   * Esto evita depender de clases CSS, <main> o textos
   * auxiliares como "Abrir cursos".
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
        /cerrar sesi[o?]n|calificaciones|asistencia|inicio/i.test(
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
    "No existe ningun grado real visible para el docente autenticado"
  ).not.toBeNull();

  await expect(grado).toBeVisible();
  await grado.click();

  const selectorCursos = page.getByText(
    "Elige el curso (materia y paralelo)"
  );

  await expect(selectorCursos).toBeVisible({
    timeout: 15_000,
  });

  /*
   * La accion identifica de forma estable los cursos:
   * "Calificar" o "Tomar asistencia".
   * No se depende de un contenedor <main>.
   */
  const cursos = page
    .getByRole("button")
    .filter({ hasText: accion });

  const totalCursos = await cursos.count();

  expect(
    totalCursos,
    `No se encontraron cursos con la accion "${accion}"`
  ).toBeGreaterThan(0);

  let curso;

  if (indiceCurso !== null) {
    expect(
      indiceCurso,
      `El indice de curso ${indiceCurso} esta fuera del rango disponible`
    ).toBeLessThan(totalCursos);

    curso = cursos.nth(indiceCurso);
  } else {
    curso = cursos.first();

    /*
     * E2E_CURSO tambien es solamente una preferencia.
     */
    if (e2e.curso) {
      const cursoPreferido = cursos
        .filter({ hasText: e2e.curso })
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
  await curso.click();

  await expect(selectorCursos).toBeHidden({
    timeout: 15_000,
  });

  return totalCursos;
}
