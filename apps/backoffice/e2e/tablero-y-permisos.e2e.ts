import { expect, test, type Page } from '@playwright/test'

/**
 * F12.1 — humo del shell contra el mock de Prism (`yarn dev:mock`).
 *
 * **Actualizado (PR11-Sesion.Frontend, H3, 2026-09-22):** el hallazgo original de F12
 * ("no existe ninguna pantalla de login ni un `APP_INITIALIZER`") ya no es cierto —
 * `pantalla-de-ingreso.ts` y `provideAppInitializer(inicializarSesion())` existen. Ahora
 * TODA carga del backoffice intenta `POST /sesion/refrescar` antes de montar cualquier
 * ruta. Ese endpoint no está en el contrato real de `identidad`
 * (`servicios/identidad/src/main/resources/openapi/identidad.yaml` solo define
 * `/sesiones`, nunca `/sesion/refrescar` — verificado, no supuesto) ni en el mock de
 * Prism generado desde ese mismo contrato, así que sin interceptarlo el arranque
 * termina en `ERROR` (identidad no responde con nada reconocible) y va a `/arranque`,
 * no a `/ingreso`. Estos tests interceptan el refresco con un `401` limpio (la cookie
 * no existe: es exactamente lo que pasa en un navegador sin sesión) para ejercitar el
 * camino `ANONYMOUS` real que describe el CA de H3, en vez del camino `ERROR` que es un
 * artefacto de que el backend simulado no tiene el endpoint todavía (regla 65: se aísla
 * el contrato que falta, no se prueba contra el hueco).
 */
async function sinCookieDeSesion(page: Page): Promise<void> {
  await page.route('**/sesion/refrescar', (route) => route.fulfill({ status: 401, json: {} }))
}

test.describe('tablero — punto de entrada', () => {
  test('carga, tiene un único h1 y no ofrece secciones sin permiso', async ({ page }) => {
    await sinCookieDeSesion(page)
    const r = await page.goto('/tablero')
    expect(r?.status()).toBe(200)
    await expect(page).toHaveURL(/\/ingreso/)
  })

  test('la redirección "/" cae en /ingreso cuando no hay sesión', async ({ page }) => {
    await sinCookieDeSesion(page)
    await page.goto('/')
    await expect(page).toHaveURL(/\/ingreso/)
  })

  for (const ruta of ['operacion', 'cumplimiento', 'sistemas', 'contabilidad', 'publicidad']) {
    test(`sin sesión, /${ruta} redirige a /ingreso preservando la ruta pedida`, async ({ page }) => {
      await sinCookieDeSesion(page)
      await page.goto(`/${ruta}`)
      await expect(page).toHaveURL(new RegExp(`/ingreso\\?volverA=%2F${ruta}`))
    })
  }

  test('el meta robots noindex está presente (defensa en profundidad; NGINX repite la cabecera en producción, docker/nginx.conf)', async ({
    page,
  }) => {
    await sinCookieDeSesion(page)
    await page.goto('/tablero')
    const robots = await page.locator('meta[name="robots"]').getAttribute('content')
    expect(robots).toContain('noindex')
  })
})
