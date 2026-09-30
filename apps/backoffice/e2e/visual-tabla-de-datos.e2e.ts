import { expect, test } from '@playwright/test'
import { iniciarSesionDePrueba, navegarSinRecargar } from './apoyo/sesion-e2e'

/**
 * Evidencia visual real (PNG de Playwright, no una maqueta) de las dos pantallas
 * migradas al organismo canónico, en 3 anchos de viewport × 2 esquemas de color. No hay
 * pipeline de diff visual en el repo (ni antes de esta migración) ni una corrida
 * "anterior" para comparar en pixel — es la primera captura real de este organismo en
 * estos dos consumidores, y sienta la base para una comparación futura. Se guardan en
 * `.playwright/backoffice/visual/` (gitignored, como el resto de `.playwright/`).
 *
 * `operacion/estado` navega con `navegarSinRecargar` y no con la tarjeta del tablero:
 * ver el hallazgo documentado en `operacion-estado.e2e.ts` (`[routerLink]="['/', a.ruta]"`
 * codifica la `/` de `a.ruta` como `%2F`, un bug real y preexistente de `tablero.ts`, fuera
 * de mi alcance).
 */
const VIEWPORTS = [
  { nombre: 'movil', width: 390, height: 844 },
  { nombre: 'tablet', width: 834, height: 1194 },
  { nombre: 'escritorio', width: 1440, height: 900 },
] as const
const TEMAS = ['light', 'dark'] as const

for (const viewport of VIEWPORTS) {
  for (const tema of TEMAS) {
    test(`operacion/estado — ${viewport.nombre} × ${tema}`, async ({ page }) => {
      await page.setViewportSize({ width: viewport.width, height: viewport.height })
      await page.emulateMedia({ colorScheme: tema })
      await iniciarSesionDePrueba(page, ['BILLETERA_VER_TERCEROS'])
      await navegarSinRecargar(page, '/operacion/estado')
      await expect(page.locator('table[role="table"]')).toBeVisible()
      await page.screenshot({ path: `../../.playwright/backoffice/visual/operacion-estado.${viewport.nombre}.${tema}.png`, fullPage: true })
    })

    test(`publicidad/anunciantes — ${viewport.nombre} × ${tema} (error real del contrato)`, async ({ page }) => {
      await page.setViewportSize({ width: viewport.width, height: viewport.height })
      await page.emulateMedia({ colorScheme: tema })
      await iniciarSesionDePrueba(page, ['PUBLICIDAD_ANUNCIANTES'])
      await page.locator('ul.accesos').getByRole('link', { name: /Publicidad/ }).click()
      await page.waitForURL(/\/publicidad\/anunciantes$/)
      await expect(page.locator('[role="alert"]')).toBeVisible()
      await page.screenshot({ path: `../../.playwright/backoffice/visual/publicidad-anunciantes-error.${viewport.nombre}.${tema}.png`, fullPage: true })
    })
  }
}

test('publicidad/anunciantes — escritorio × light, con datos (interceptado — ver JSDoc de publicidad-anunciantes.e2e.ts)', async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 900 })
  await page.emulateMedia({ colorScheme: 'light' })
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
  await iniciarSesionDePrueba(page, ['PUBLICIDAD_ANUNCIANTES'])
  await page.locator('ul.accesos').getByRole('link', { name: /Publicidad/ }).click()
  await page.waitForURL(/\/publicidad\/anunciantes$/)
  await expect(page.locator('table[role="table"]')).toBeVisible()
  await page.screenshot({ path: '../../.playwright/backoffice/visual/publicidad-anunciantes-con-datos.escritorio.light.png', fullPage: true })
})
