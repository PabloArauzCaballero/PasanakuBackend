import { expect, test } from '@playwright/test'
import { iniciarSesionDePrueba, navegarSinRecargar } from './apoyo/sesion-e2e'

/**
 * PR7 §H4.S1.M2 — `publicidad/anunciantes` migrada de `TablaDeDatosVirtualizada` al
 * organismo canónico. `cargarAnunciantes` (`rutas/publicidad/dominio/cu110-anunciantes.ts`)
 * documenta un supuesto declarado: pide `GET /publicidad/anunciantes`, que el contrato
 * real de `publicidad` TODAVÍA no publica (solo el `POST`) — fuera de mi alcance, no soy
 * dueño de ese contrato. Contra el mock real (Prism, sin interceptar nada), esa `GET`
 * devuelve 405 de verdad.
 *
 * La sesión vive solo en memoria (por diseño, `sesion.ts`): se navega con el link real del
 * tablero o, sin ese link, con `navegarSinRecargar` — nunca `page.goto()` a mitad de
 * prueba (recargaría la página y borraría la sesión).
 */
test.describe('publicidad/anunciantes — tabla canónica', () => {
  test.beforeEach(async ({ page }) => {
    await iniciarSesionDePrueba(page, ['PUBLICIDAD_ANUNCIANTES'])
  })

  test('comportamiento real de hoy: el 405 del contrato se traduce a un estado de error visible, tabla oculta (no un estado mudo)', async ({ page }) => {
    // `ul.accesos` (tarjetas del tablero) y no el `nav` persistente del shell: ambos
    // tienen un link "Publicidad".
    await page.locator('ul.accesos').getByRole('link', { name: /Publicidad/ }).click()
    await page.waitForURL(/\/publicidad\/anunciantes$/)
    await expect(page.locator('h1')).toHaveText('Anunciantes')

    const alerta = page.locator('[role="alert"]')
    await expect(alerta).toBeVisible()
    await expect(page.locator('table')).toHaveCount(0)
  })

  test.describe('con el GET de listado disponible (interceptado — el contrato real todavía no lo publica, ver JSDoc de este archivo)', () => {
    test('la lista cargada se muestra en la tabla canónica, con el botón de alta visible', async ({ page }) => {
      await page.route('**/api/v1/publicidad/anunciantes', async (route) => {
        if (route.request().method() !== 'GET') { await route.fallback(); return }
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([
            { anuncianteId: 'a-1', tipo: 'SOCIO_COMERCIAL', razonSocialFacturacion: 'Café del Valle SRL', moneda: 'BOB', limiteGastoMensual: '500.00', estado: 'ACTIVA' },
            { anuncianteId: 'a-2', tipo: 'ORGANIZADOR', razonSocialFacturacion: 'Cooperativa Andina', moneda: 'BOB', limiteGastoMensual: null, estado: 'SUSPENDIDA' },
          ]),
        })
      })

      // `ul.accesos` (tarjetas del tablero) y no el `nav` persistente del shell: ambos
    // tienen un link "Publicidad".
    await page.locator('ul.accesos').getByRole('link', { name: /Publicidad/ }).click()
      await page.waitForURL(/\/publicidad\/anunciantes$/)

      const tabla = page.locator('table[role="table"]')
      await expect(tabla).toBeVisible()
      await expect(page.locator('table tbody tr')).toHaveCount(2)
      await expect(page.getByText('Café del Valle SRL')).toBeVisible()
      await expect(page.getByRole('button', { name: 'Dar de alta anunciante' })).toBeVisible()
    })
  })
})

test('sin el permiso PUBLICIDAD_ANUNCIANTES, la ruta redirige a /tablero (canMatch real)', async ({ page }) => {
  await iniciarSesionDePrueba(page, [])
  await navegarSinRecargar(page, '/publicidad/anunciantes')
  await expect(page).toHaveURL(/\/tablero$/)
})
