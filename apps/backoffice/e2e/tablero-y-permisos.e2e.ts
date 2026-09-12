import { expect, test } from '@playwright/test'

/**
 * F12.1 — humo del shell contra el mock de Prism (`yarn dev:mock`).
 *
 * HALLAZGO (carril F12, ver `planes/informes/carril-F12.md`): no existe ninguna
 * pantalla de inicio de sesión en `apps/backoffice` — `Sesion.abrir()`
 * (`apps/backoffice/src/app/nucleo/sesion.ts`) solo se invoca desde specs unitarios y
 * desde el reintento de refresco en `nucleo/sesion.interceptor.ts:35`, nunca desde un
 * componente de login ni desde un `APP_INITIALIZER` en `app.config.ts`. Por lo tanto,
 * en una carga real del backoffice `Sesion.permisos()` empieza vacío y **todas** las
 * rutas con `canMatch: [requierePermiso(...)]` (`operacion`, `cumplimiento`, `sistemas`,
 * `contabilidad`, `publicidad` — ver `app.routes.ts`) redirigen silenciosamente a
 * `/tablero`. Los recorridos `backoffice-cobranza.e2e.ts`, `backoffice-cumplimiento.e2e.ts`
 * y `backoffice-doble-control.e2e.ts` que pide `planes/15` §F12.1 ("Login por rol")
 * no se pueden escribir contra código que no existe: se documentan acá como
 * bloqueados, no se inventa una pantalla de login (fuera de mi alcance: no soy dueño
 * de pantallas de negocio, solo de `e2e/`).
 */
test.describe('tablero — punto de entrada', () => {
  test('carga, tiene un único h1 y no ofrece secciones sin permiso', async ({ page }) => {
    const r = await page.goto('/tablero')
    expect(r?.status()).toBe(200)
    await expect(page).toHaveTitle(/Tablero/)
    await expect(page.locator('h1')).toHaveText('Tablero')
    // Sin sesión abierta, ningún acceso a dominio protegido debe listarse.
    await expect(page.locator('ul.accesos li')).toHaveCount(0)
  })

  test('la redirección "/" cae en /tablero', async ({ page }) => {
    await page.goto('/')
    await expect(page).toHaveURL(/\/tablero$/)
  })

  for (const ruta of ['operacion', 'cumplimiento', 'sistemas', 'contabilidad', 'publicidad']) {
    test(`sin sesión, /${ruta} redirige a /tablero (canMatch, "el rol oculta, no protege")`, async ({ page }) => {
      await page.goto(`/${ruta}`)
      await expect(page).toHaveURL(/\/tablero$/)
    })
  }

  test('el meta robots noindex está presente (defensa en profundidad; NGINX repite la cabecera en producción, docker/nginx.conf)', async ({ page }) => {
    await page.goto('/tablero')
    const robots = await page.locator('meta[name="robots"]').getAttribute('content')
    expect(robots).toContain('noindex')
  })
})
