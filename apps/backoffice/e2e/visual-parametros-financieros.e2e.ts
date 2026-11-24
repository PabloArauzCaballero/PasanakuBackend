import { expect, test, type Page } from '@playwright/test'
import { iniciarSesionDePrueba, navegarSinRecargar } from './apoyo/sesion-e2e'

/**
 * CU-132 · visual de parámetros en 3 viewports × 2 temas.
 * Red simulada declarada: este E2E verifica el renderizado y el flujo del navegador; la escritura y su persistencia
 * se verifican por separado en las pruebas de la API y el repositorio de `tarifas`.
 */
const VIEWPORTS = [
  { nombre: 'movil', width: 390, height: 844 },
  { nombre: 'tablet', width: 834, height: 1194 },
  { nombre: 'escritorio', width: 1440, height: 900 },
] as const
const TEMAS = ['light', 'dark'] as const
const BASE = '**/tarifas/parametros'
const VERSION = { id: 'v-1', codigo: 'DIAS_DE_GRACIA', valor: '5.000000', moneda: null, nivel: null, vigenteDesde: '2026-10-01T00:00:00Z', estado: 'APROBADA' }
const PROPUESTA = { id: 'v-2', codigo: 'DIAS_DE_GRACIA', valor: '6.000000', moneda: null, nivel: null, vigenteDesde: '2026-12-01T00:00:00Z', estado: 'PROPUESTA' }
const PARAMETROS = [{ codigo: 'DIAS_DE_GRACIA', descripcion: 'Días de gracia', unidad: 'DIAS', porNivel: false, vigentes: [VERSION], propuestas: [PROPUESTA] }]
const HISTORIAL = [PROPUESTA, VERSION, { ...VERSION, id: 'v-0', valor: '4.000000', estado: 'RECHAZADA' }]

async function abrir(page: Page, catalogo: { status: number; json: unknown } = { status: 200, json: PARAMETROS }): Promise<void> {
  await page.route(BASE, (route) => route.fulfill(catalogo))
  await page.route(`${BASE}/DIAS_DE_GRACIA/versiones`, (route) => route.fulfill({ status: 200, json: HISTORIAL }))
  await iniciarSesionDePrueba(page, ['TARIFARIO_ADMINISTRAR'])
  await navegarSinRecargar(page, '/configuracion/parametros-financieros')
  await expect(page.getByRole('heading', { level: 1, name: 'Parámetros financieros' })).toBeVisible()
}

for (const viewport of VIEWPORTS) {
  for (const tema of TEMAS) {
    test(`parámetros CU-132 — datos ${viewport.nombre} × ${tema}`, async ({ page }) => {
      const errores: string[] = []
      page.on('console', (m) => { if (m.type() === 'error') errores.push(m.text()) })
      page.on('pageerror', (e) => errores.push(e.message))
      await page.setViewportSize({ width: viewport.width, height: viewport.height })
      await page.emulateMedia({ colorScheme: tema })
      await abrir(page)
      await expect(page.getByText('5.000000')).toBeVisible()
      await expect(page.getByText('6.000000')).toBeVisible()
      await page.getByRole('button', { name: 'Ver historial completo' }).click()
      await expect(page.getByText('Rechazada')).toBeVisible()
      await page.screenshot({ path: `../../.playwright/backoffice/visual/parametros/${viewport.nombre}.${tema}.png`, fullPage: true })
      const desborde = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)
      expect(desborde, `${viewport.nombre} ${tema}: sin desborde horizontal`).toBeLessThanOrEqual(0)
      expect(errores, 'consola sin errores').toEqual([])
    })
  }
}

test('parámetros CU-132 — el formulario asocia errores locales a sus campos', async ({ page }) => {
  await abrir(page)
  await page.getByRole('button', { name: 'Proponer una versión' }).click()
  await page.getByRole('button', { name: 'Registrar propuesta' }).click()
  await expect(page.getByLabel('Valor (decimal)')).toHaveAttribute('aria-invalid', 'true')
  await expect(page.getByLabel('Fuente de la decisión')).toHaveAttribute('aria-invalid', 'true')
  await expect(page.getByText('Ingresá un decimal no negativo, con hasta 10 dígitos y 6 decimales.')).toBeVisible()
})

const CAPTURAS = '../../.playwright/backoffice/visual/parametros'

test('parámetros CU-132 — vacío orienta (móvil claro)', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await abrir(page, { status: 200, json: [] })
  await expect(page.getByText('No hay parámetros disponibles.')).toBeVisible()
  await page.screenshot({ path: `${CAPTURAS}/estado-vacio.movil.light.png`, fullPage: true })
})

test('parámetros CU-132 — error accionable con reintento (escritorio oscuro)', async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 900 })
  await page.emulateMedia({ colorScheme: 'dark' })
  await abrir(page, { status: 503, json: { codigo: 'X', mensaje: 'detalle interno', trazaId: 'traza-e2e' } })
  await expect(page.getByRole('button', { name: 'Volver a intentar' })).toBeVisible()
  await expect(page.getByText('detalle interno')).toHaveCount(0)
  await page.screenshot({ path: `${CAPTURAS}/estado-error.escritorio.dark.png`, fullPage: true })
})

test('parámetros CU-132 — propuesta abierta con errores por campo (tablet claro)', async ({ page }) => {
  await page.setViewportSize({ width: 834, height: 1194 })
  await abrir(page)
  await page.getByRole('button', { name: 'Proponer una versión' }).click()
  await page.getByRole('button', { name: 'Registrar propuesta' }).click()
  await expect(page.getByLabel('Valor (decimal)')).toHaveAttribute('aria-invalid', 'true')
  await page.screenshot({ path: `${CAPTURAS}/propuesta-con-errores.tablet.light.png`, fullPage: true })
})
