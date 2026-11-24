import { expect, test } from '@playwright/test'

/**
 * GU-15 / ADR-038 §1 (P-8) — cierre por 15 minutos de inactividad, con el reloj de Playwright (`page.clock`): sin
 * esperar de verdad, a los 13 minutos aparece el aviso y a los 15 la sesión se cierra en el servidor (`DELETE /sesion`)
 * y el ingreso explica el motivo. 3 viewports × 2 temas; las capturas quedan en la evidencia del trabajo.
 *
 * El refresco y el cierre se responden acá (el simulado no guarda sesiones): lo que se prueba es el cliente.
 */
const VIEWPORTS = [
  { nombre: 'movil', width: 390, height: 844 },
  { nombre: 'tablet', width: 834, height: 1194 },
  { nombre: 'escritorio', width: 1440, height: 900 },
] as const

const EVIDENCIA = '../../docs/trabajo/2026-11-24-gu-e/evidencia'
/** Las animaciones corren con el tiempo real del compositor, no con `page.clock`: se espera a que terminen. */
const sinAnimaciones = (page: import('@playwright/test').Page) => page.evaluate(() => Promise.all(document.getAnimations().map((a) => a.finished)))

const TOKEN = `e2e.${Buffer.from(JSON.stringify({ permisos: [], rol: 'oficial', sub: 'e2e' })).toString('base64url')}.firma`

test.describe('Cierre por inactividad — aviso e ingreso (3 viewports × 2 temas)', () => {
  for (const vp of VIEWPORTS) {
    for (const tema of ['claro', 'oscuro'] as const) {
      test(`${vp.nombre} · ${tema}`, async ({ page }) => {
        await page.setViewportSize({ width: vp.width, height: vp.height })
        await page.emulateMedia({ colorScheme: tema === 'oscuro' ? 'dark' : 'light' })
        await page.clock.install({ time: new Date('2026-11-24T15:00:00Z') })
        await page.route('**/sesion/refrescar', (route) => route.fulfill({ status: 200, json: { acceso: TOKEN, permisos: [], rol: 'oficial' } }))
        let cierres = 0
        await page.route('**/sesion', (route) => {
          if (route.request().method() !== 'DELETE') return route.continue()
          cierres++
          return route.fulfill({ status: 204 })
        })

        await page.goto('/tablero')
        await expect(page.locator('#contenido')).toBeVisible()

        await page.clock.fastForward('13:00')
        await page.clock.runFor(1_000)
        const aviso = page.getByRole('dialog', { name: 'Tu sesión está por cerrarse' })
        await expect(aviso).toBeVisible()
        await expect(aviso.getByRole('button', { name: 'Seguir trabajando' })).toBeVisible()
        await sinAnimaciones(page)
        await page.screenshot({ path: `${EVIDENCIA}/visual-inactividad-aviso-${vp.nombre}-${tema}.png` })

        await page.clock.fastForward('02:00')
        await page.clock.runFor(1_000)
        await expect(page).toHaveURL(/\/ingreso\?motivo=inactividad&volverA=%2Ftablero/)
        await expect(page.getByText('pasaron 15 minutos sin actividad')).toBeVisible()
        expect(cierres).toBe(1)
        await sinAnimaciones(page)
        await page.screenshot({ path: `${EVIDENCIA}/visual-inactividad-ingreso-${vp.nombre}-${tema}.png` })
      })
    }
  }
})
