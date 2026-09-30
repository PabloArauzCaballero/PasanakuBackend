import { expect, test } from '@playwright/test'

/**
 * PR11-Sesion.Frontend, H3.S4 — kill-test #2 del carril: "F5 sobre `/operacion/reclamos`
 * con cookie válida vuelve a esa ruta, no al login".
 *
 * `POST /sesion/refrescar` no está en el contrato real de `identidad` todavía (verificado
 * contra `servicios/identidad/.../openapi/identidad.yaml`: solo existe `/sesiones`) ni en
 * el mock de Prism generado desde ese contrato. Se intercepta con `page.route` — regla 65,
 * el doble del contrato que falta — devolviendo lo que `AuthBootstrap` espera de un `200`
 * real: `{ acceso, permisos, rol }`.
 */
const REFRESCO_OK = { acceso: 't-refrescado', permisos: ['ver:operacion'], rol: 'oficial' }

test.describe('restaurar sesión al recargar', () => {
  test('F5 sobre una ruta protegida con cookie válida vuelve a esa ruta, no al login', async ({ page }) => {
    await page.route('**/sesion/refrescar', (route) => route.fulfill({ status: 200, json: REFRESCO_OK }))

    await page.goto('/operacion')
    await expect(page).toHaveURL(/\/operacion/)

    await page.reload()
    await expect(page).toHaveURL(/\/operacion/)
    await expect(page).not.toHaveURL(/\/ingreso/)
  })

  test('cookie inválida (401 del refresco) termina en /ingreso con volverA', async ({ page }) => {
    await page.route('**/sesion/refrescar', (route) => route.fulfill({ status: 401, json: {} }))
    await page.goto('/operacion')
    await expect(page).toHaveURL(/\/ingreso\?volverA=%2Foperacion/)
  })

  test('identidad no disponible (500) muestra la pantalla de error con "Reintentar", no un login silencioso', async ({ page }) => {
    await page.route('**/sesion/refrescar', (route) => route.fulfill({ status: 500, json: {} }))
    await page.goto('/operacion')
    await expect(page).toHaveURL(/\/arranque/)
    await expect(page.getByRole('button', { name: 'Reintentar' })).toBeVisible()
  })

  test('el botón "Reintentar" vuelve a llamar al refresco y, si ahora funciona, entra', async ({ page }) => {
    let intentos = 0
    await page.route('**/sesion/refrescar', (route) => {
      intentos++
      if (intentos === 1) return route.fulfill({ status: 500, json: {} })
      return route.fulfill({ status: 200, json: REFRESCO_OK })
    })
    await page.goto('/operacion')
    await expect(page).toHaveURL(/\/arranque/)
    await page.getByRole('button', { name: 'Reintentar' }).click()
    await expect(page).toHaveURL(/\/operacion/)
    expect(intentos).toBe(2)
  })
})
