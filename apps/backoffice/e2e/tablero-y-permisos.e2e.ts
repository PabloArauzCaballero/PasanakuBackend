import { expect, test } from '@playwright/test'

/**
 * F12.1 — humo del shell contra el mock de Prism (`yarn dev:mock`).
 *
 * La pantalla `/ingreso` existe ahora. `requiereSesion` envía allí a quien llega
 * sin token, incluso si entra por `/` o `/tablero`. Este recorrido comprueba que
 * ninguna ruta protegida se muestra antes de autenticarse.
 */
test.describe('tablero — punto de entrada', () => {
  test('sin sesión, tablero muestra el ingreso y no ofrece secciones', async ({ page }) => {
    const r = await page.goto('/tablero')
    expect(r?.status()).toBe(200)
    await expect(page).toHaveURL(/\/ingreso$/)
    await expect(page).toHaveTitle(/Ingresar/)
    await expect(page.locator('h1')).toHaveText('Ingresá con tu cuenta de operador')
    // Sin sesión abierta, ningún acceso a dominio protegido debe listarse.
    await expect(page.locator('ul.accesos li')).toHaveCount(0)
  })

  test('sin sesión, la redirección "/" termina en /ingreso', async ({ page }) => {
    await page.goto('/')
    await expect(page).toHaveURL(/\/ingreso$/)
  })

  for (const ruta of ['operacion', 'cumplimiento', 'sistemas', 'contabilidad', 'publicidad']) {
    test(`sin sesión, /${ruta} redirige a /ingreso`, async ({ page }) => {
      await page.goto(`/${ruta}`)
      await expect(page).toHaveURL(/\/ingreso$/)
    })
  }

  test('el meta robots noindex está presente (defensa en profundidad; NGINX repite la cabecera en producción, docker/nginx.conf)', async ({ page }) => {
    await page.goto('/tablero')
    const robots = await page.locator('meta[name="robots"]').getAttribute('content')
    expect(robots).toContain('noindex')
  })
})
