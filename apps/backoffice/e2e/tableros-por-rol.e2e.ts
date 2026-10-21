import { expect, test, type Page } from '@playwright/test'
import { capturar, todas } from './apoyo/capturas-f2'
import { iniciarSesionDePrueba, navegarSinRecargar } from './apoyo/sesion-e2e'
import { CATALOGO, COMERCIAL, ENDPOINTS, FINANCIERO, SISTEMAS, VACIO, servir, servirCatalogo, type Respuesta } from './apoyo/tableros-sinteticos'

/**
 * Tableros por rol (H9) · recorrido y prueba visual de los tres tableros y del catálogo de datos: guardas `canMatch`
 * reales, login real de CU-04 contra Prism (`apoyo/sesion-e2e.ts`) y la pantalla real. **Las respuestas de auditoria
 * son un DOBLE declarado** (`apoyo/tableros-sinteticos.ts`): esto prueba la pantalla y su apariencia, NO la
 * integración con el backend real, que sigue pendiente de la pila (ver REPORTE).
 *
 * Capturas: los tres viewports y los dos temas con datos; «vacío», «error» y «cargando» en móvil claro y escritorio
 * oscuro. Cada captura además falla si la página desplaza en horizontal o si axe mide un contraste insuficiente.
 */
const P = 'h9'

async function abrir(page: Page, permiso: string, ruta: string, respuestas: Partial<Record<Parameters<typeof servir>[1], Respuesta>>) {
  for (const [r, resp] of Object.entries(respuestas)) await servir(page, r as Parameters<typeof servir>[1], resp!)
  await iniciarSesionDePrueba(page, [permiso])
  await navegarSinRecargar(page, ruta)
}

const TABLEROS = [
  { tipo: 'financiero', permiso: 'TABLERO_FINANCIERO_VER', titulo: 'Tablero financiero', datos: FINANCIERO, cifra: '184.500,00' },
  { tipo: 'comercial', permiso: 'TABLERO_COMERCIAL_VER', titulo: 'Tablero comercial', datos: COMERCIAL, cifra: '3.180' },
  { tipo: 'sistemas', permiso: 'TABLERO_SISTEMAS_VER', titulo: 'Tablero de sistemas', datos: SISTEMAS, cifra: '187,5 ms' },
] as const

for (const t of TABLEROS) {
  test.describe(`tablero ${t.tipo}`, () => {
    const extra = t.tipo === 'sistemas' ? { '/indicadores/operacion/endpoints': { cuerpo: ENDPOINTS } } : {}

    test('con datos: las cifras del período, lo suprimido y lo que no llegó se distinguen', async ({ page }) => {
      await abrir(page, t.permiso, `/tablero/${t.tipo}`, { '/indicadores': { cuerpo: t.datos }, ...extra })
      await expect(page.getByRole('heading', { level: 1, name: t.titulo })).toBeVisible()
      await expect(page.getByText(t.cifra).first()).toBeVisible()
      await todas(page, P, `${t.tipo}-datos`)
    })

    test('vacío: el período sin indicadores orienta, no muestra ceros', async ({ page }) => {
      await abrir(page, t.permiso, `/tablero/${t.tipo}`, { '/indicadores': { cuerpo: VACIO }, ...extra })
      await expect(page.getByText(/Todavía no hay indicadores de este período/)).toBeVisible()
      await capturar(page, P, `${t.tipo}-vacio`, 'movil', 'light')
      await capturar(page, P, `${t.tipo}-vacio`, 'escritorio', 'dark')
    })

    test('error: un 500 no muestra el detalle interno y ofrece volver a intentar', async ({ page }) => {
      await abrir(page, t.permiso, `/tablero/${t.tipo}`, { '/indicadores': { estado: 500 }, ...extra })
      await expect(page.getByRole('button', { name: 'Volver a intentar' }).first()).toBeVisible()
      await expect(page.getByText('detalle interno')).toHaveCount(0)
      await capturar(page, P, `${t.tipo}-error`, 'movil', 'light')
      await capturar(page, P, `${t.tipo}-error`, 'escritorio', 'dark')
    })

    test('cargando: dice qué consulta mientras espera', async ({ page }) => {
      await abrir(page, t.permiso, `/tablero/${t.tipo}`, { '/indicadores': { colgar: true }, ...extra })
      await expect(page.locator('[role=status][aria-label="Consultando los indicadores"]').first()).toBeVisible()
      await capturar(page, P, `${t.tipo}-cargando`, 'movil', 'light')
      await capturar(page, P, `${t.tipo}-cargando`, 'escritorio', 'dark')
    })
  })
}

test.describe('catálogo de datos', () => {
  test('lista con datos y ficha de una tabla, con su sensibilidad y relaciones', async ({ page }) => {
    await servirCatalogo(page, CATALOGO)
    await abrir(page, 'TABLERO_SISTEMAS_VER', '/tablero/catalogo-de-datos', {})
    await expect(page.getByRole('heading', { level: 1, name: 'Catálogo de datos' })).toBeVisible()
    await expect(page.locator('ap-tabla-de-datos .total')).toHaveText(String(CATALOGO.tablas.length))
    await todas(page, P, 'catalogo-datos')

    await page.getByRole('searchbox').fill('obligación aporte')
    await expect(page.getByRole('link', { name: 'obligacion_aporte', exact: true })).toBeVisible()
    await page.getByRole('link', { name: 'obligacion_aporte', exact: true }).click()
    await expect(page.getByRole('heading', { level: 1 })).toContainText('obligacion_aporte')
    await expect(page.getByText(/Sí · \d+ columnas?/).first()).toBeVisible()
    await todas(page, P, 'catalogo-ficha')
  })

  test('vacío de búsqueda y error del catálogo', async ({ page }) => {
    await servirCatalogo(page, CATALOGO)
    await abrir(page, 'TABLERO_SISTEMAS_VER', '/tablero/catalogo-de-datos?q=zzz-inexistente', {})
    await expect(page.getByText(/Ninguna tabla coincide con la búsqueda/).first()).toBeVisible()
    await capturar(page, P, 'catalogo-vacio', 'movil', 'light')
    await capturar(page, P, 'catalogo-vacio', 'escritorio', 'dark')
  })

  test('error: un 403 del catálogo no muestra el detalle interno', async ({ page }) => {
    await servirCatalogo(page, CATALOGO, { estado: 403 })
    await abrir(page, 'TABLERO_SISTEMAS_VER', '/tablero/catalogo-de-datos', {})
    await expect(page.getByRole('button', { name: 'Volver a intentar' }).first()).toBeVisible()
    await expect(page.getByText('detalle interno')).toHaveCount(0)
    await capturar(page, P, 'catalogo-error', 'movil', 'light')
    await capturar(page, P, 'catalogo-error', 'escritorio', 'dark')
  })
})

test('acceso: la tarjeta del tablero financiero lleva a su ruta real (no a %2F)', async ({ page }) => {
  await abrir(page, 'TABLERO_FINANCIERO_VER', '/tablero', { '/indicadores': { cuerpo: FINANCIERO } })
  await page.locator('[data-tutorial-id="tablero-tablero/financiero"]').click()
  await expect(page).toHaveURL(/\/tablero\/financiero$/)
  await expect(page.getByRole('heading', { level: 1, name: 'Tablero financiero' })).toBeVisible()
})
