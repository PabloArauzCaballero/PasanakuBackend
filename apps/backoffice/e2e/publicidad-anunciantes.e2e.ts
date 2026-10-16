import { expect, test } from '@playwright/test'
import { iniciarSesionDePrueba, navegarSinRecargar } from './apoyo/sesion-e2e'
import { PAGINA_SINTETICA } from './apoyo/anunciantes-sinteticos'

/**
 * PR7 §H4.S1.M2 — `publicidad/anunciantes` migrada de `TablaDeDatosVirtualizada` al
 * organismo canónico. `cargarAnunciantes` (`rutas/publicidad/dominio/cu110-anunciantes.ts`)
 * consume `GET /publicidad/anunciantes` (CU-110, publicado por carril A · H7): una página `{ items, proximoFecha, proximoId }`.
 * Contra el mock real (Prism, sin interceptar nada) se ve el ejemplo SINTÉTICO del contrato; el estado de error se ejercita
 * interceptando una falla del servidor (antes lo daba el 405 de un contrato que todavía no publicaba el listado).
 *
 * La sesión vive solo en memoria (por diseño, `sesion.ts`): se navega con el link real del
 * tablero o, sin ese link, con `navegarSinRecargar` — nunca `page.goto()` a mitad de
 * prueba (recargaría la página y borraría la sesión).
 */
test.describe('publicidad/anunciantes — tabla canónica', () => {
  test.beforeEach(async ({ page }) => {
    await iniciarSesionDePrueba(page, ['PUBLICIDAD_ANUNCIANTES'])
  })

  test('contra el contrato real (Prism): la página del listado se muestra en la tabla canónica, sin estado de error', async ({ page }) => {
    // `ul.accesos` (tarjetas del tablero) y no el `nav` persistente del shell: ambos
    // tienen un link "Publicidad".
    await page.locator('ul.accesos').getByRole('link', { name: /Publicidad/ }).click()
    await page.waitForURL(/\/publicidad\/anunciantes$/)
    await expect(page.locator('h1')).toHaveText('Anunciantes')
    await expect(page.locator('table[role="table"]')).toBeVisible()
    await expect(page.locator('[role="alert"]')).toHaveCount(0)
  })

  test('falla del servidor: se traduce a un estado de error visible, tabla oculta (no un estado mudo)', async ({ page }) => {
    await page.route((u) => u.pathname.endsWith('/api/v1/publicidad/anunciantes'), (route) =>
      route.request().method() === 'GET' ? route.fulfill({ status: 503, json: { codigo: 'AP-SISTEMA-01', mensaje: 'no disponible' } }) : route.fallback(),
    )
    await page.locator('ul.accesos').getByRole('link', { name: /Publicidad/ }).click()
    await page.waitForURL(/\/publicidad\/anunciantes$/)
    await expect(page.locator('h1')).toHaveText('Anunciantes')
    await expect(page.locator('[role="alert"]')).toBeVisible()
    await expect(page.locator('table')).toHaveCount(0)
  })

  test.describe('con una página de dos anunciantes (interceptado, forma `PaginaDeAnunciantes` del contrato)', () => {
    test('la lista cargada se muestra en la tabla canónica, con el botón de alta visible', async ({ page }) => {
      await page.route((u) => u.pathname.endsWith('/api/v1/publicidad/anunciantes'), async (route) => {
        if (route.request().method() !== 'GET') { await route.fallback(); return }
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(PAGINA_SINTETICA),
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
