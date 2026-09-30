import { expect, test } from '@playwright/test'

/**
 * PR11-Sesion.Frontend, H3.S3.M6 — prueba visual de `RestaurandoSesion`: sus dos estados
 * (`RESTORING` y `ERROR`) en 3 viewports × 2 temas = 12 capturas. `RESTORING` es
 * transitorio (se resuelve apenas responde el refresco), así que la petición se retiene
 * a propósito para poder capturarlo montado.
 */
const VIEWPORTS = [
  { nombre: 'movil', width: 390, height: 844 },
  { nombre: 'tablet', width: 834, height: 1194 },
  { nombre: 'escritorio', width: 1440, height: 900 },
] as const

test.describe('RestaurandoSesion — comparación visual (3 viewports × 2 temas)', () => {
  for (const vp of VIEWPORTS) {
    for (const tema of ['claro', 'oscuro'] as const) {
      test(`${vp.nombre} · ${tema} · RESTORING`, async ({ page }) => {
        await page.setViewportSize({ width: vp.width, height: vp.height })
        await page.emulateMedia({ colorScheme: tema === 'oscuro' ? 'dark' : 'light' })
        // `RESTORING` durante el arranque (primer `provideAppInitializer`) es invisible:
        // Angular no renderiza NADA mientras el inicializador está pendiente — bloquea el
        // propio bootstrap, no solo las rutas guardadas. La única `RESTORING` que se puede
        // fotografiar es la del REINTENTO: primero falla rápido (queda en ERROR, ya
        // montado), y el segundo refresco (el del botón) sí se retiene con la app ya
        // renderizada.
        let refrescos = 0
        let liberarSegundo: (() => void) | undefined
        const segundoDetenido = new Promise<void>((r) => (liberarSegundo = r))
        await page.route('**/sesion/refrescar', async (route) => {
          refrescos++
          if (refrescos === 1) return route.fulfill({ status: 500, json: {} })
          await segundoDetenido
          await route.fulfill({ status: 200, json: { acceso: 't', permisos: [], rol: 'oficial' } })
        })
        await page.goto('/tablero')
        await expect(page.getByRole('button', { name: 'Reintentar' })).toBeVisible()
        await page.getByRole('button', { name: 'Reintentar' }).click()
        await expect(page.getByText('Restaurando tu sesión…')).toBeVisible()
        await page.screenshot({ path: `../../evidencia/visual-restaurando-${vp.nombre}-${tema}.png` })
        liberarSegundo?.()
      })

      test(`${vp.nombre} · ${tema} · ERROR con "Reintentar"`, async ({ page }) => {
        await page.setViewportSize({ width: vp.width, height: vp.height })
        await page.emulateMedia({ colorScheme: tema === 'oscuro' ? 'dark' : 'light' })
        await page.route('**/sesion/refrescar', (route) => route.fulfill({ status: 500, json: {} }))
        await page.goto('/tablero')
        await expect(page.getByRole('button', { name: 'Reintentar' })).toBeVisible()
        await page.screenshot({ path: `../../evidencia/visual-error-restauracion-${vp.nombre}-${tema}.png` })
      })
    }
  }
})
