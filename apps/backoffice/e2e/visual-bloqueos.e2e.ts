import { expect, test, type Page } from '@playwright/test'
import { iniciarSesionDePrueba, navegarSinRecargar } from './apoyo/sesion-e2e'

/**
 * CU-17 · prueba visual de la bandeja de bloqueos por orden de autoridad en 3 viewports × 2 temas y tres estados.
 * **Backend simulado, declarado**: el listado se responde con `page.route` usando datos SINTÉTICOS con la forma de
 * `PaginaDeBloqueos` (nucleo-financiero.yaml 1.4.0). Prueba diseño, estados y consola; no el comportamiento del servicio.
 * Capturas en `.playwright/backoffice/visual/bloqueos/` (gitignored).
 */
const VIEWPORTS = [
  { nombre: 'movil', width: 390, height: 844 },
  { nombre: 'tablet', width: 834, height: 1194 },
  { nombre: 'escritorio', width: 1440, height: 900 },
] as const
const TEMAS = ['light', 'dark'] as const

const BASE = { cuentaBilleteraId: '0b10c0e0-0000-4000-8000-0000000000c1', registradoPor: null, levantadoPor: null, venceEn: null, levantadoEn: null }
const PAGINA = {
  items: [
    { ...BASE, bloqueoId: '0b10c0e0-0000-4000-8000-000000000001', autoridad: 'JUZGADO', tipoOrden: 'EMBARGO', alcance: 'PARCIAL', estado: 'VIGENTE', montoBloqueado: { monto: '1240.50', moneda: 'BOB' }, recibidoEn: '2026-11-20T14:00:00Z', puedeLevantar: true },
    { ...BASE, bloqueoId: '0b10c0e0-0000-4000-8000-000000000002', autoridad: 'UIF', tipoOrden: 'CONGELAMIENTO', alcance: 'TOTAL', estado: 'VIGENTE', recibidoEn: '2026-11-19T10:30:00Z', venceEn: '2026-12-19T10:30:00Z', puedeLevantar: false },
    { ...BASE, bloqueoId: '0b10c0e0-0000-4000-8000-000000000003', autoridad: 'FISCALIA', tipoOrden: 'RETENCION', alcance: 'PARCIAL', estado: 'LEVANTADO', montoBloqueado: { monto: '350.00', moneda: 'BOB' }, recibidoEn: '2026-11-02T09:00:00Z', levantadoEn: '2026-11-10T16:00:00Z', puedeLevantar: false },
  ],
  proximoAntesDe: null,
  proximoAntesDeId: null,
}
const ESTADOS = [
  { nombre: 'datos', status: 200, cuerpo: PAGINA, espera: /Embargo/ },
  { nombre: 'vacio', status: 200, cuerpo: { items: [] }, espera: /No hay bloqueos en este estado/ },
  { nombre: 'error', status: 422, cuerpo: { codigo: 'AP-CU17-06', mensaje: 'x', trazaId: 'traza-visual' }, espera: /El filtro de la lista no es válido/ },
] as const

async function responder(page: Page, status: number, cuerpo: unknown): Promise<void> {
  await page.route(/\/billetera\/bloqueos(\?|$)/, (ruta) => ruta.fulfill({ status, contentType: 'application/json', body: JSON.stringify(cuerpo) }))
}

for (const estado of ESTADOS) {
  for (const viewport of VIEWPORTS) {
    for (const tema of TEMAS) {
      test(`bloqueos ${estado.nombre} — ${viewport.nombre} × ${tema}`, async ({ page }) => {
        const errores: string[] = []
        page.on('console', (m) => {
          if (m.type() === 'error' && !/422/.test(m.text())) errores.push(m.text())
        })
        await page.setViewportSize({ width: viewport.width, height: viewport.height })
        await page.emulateMedia({ colorScheme: tema })
        await responder(page, estado.status, estado.cuerpo)
        await iniciarSesionDePrueba(page, ['BLOQUEO_AUTORIDAD_LEVANTAR'])
        await navegarSinRecargar(page, '/cumplimiento/bloqueos')
        await expect(page.getByRole('heading', { level: 1, name: /Bloqueos de autoridad/ })).toBeVisible()
        await expect(page.getByText(estado.espera).first()).toBeVisible()
        await page.screenshot({ path: `../../.playwright/backoffice/visual/bloqueos/${estado.nombre}.${viewport.nombre}.${tema}.png`, fullPage: true })
        expect(errores, 'consola sin errores inesperados').toEqual([])
      })
    }
  }
}

test('bloqueos — diálogo «Levantar» abierto (escritorio × claro)', async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 900 })
  await responder(page, 200, PAGINA)
  await iniciarSesionDePrueba(page, ['BLOQUEO_AUTORIDAD_LEVANTAR'])
  await navegarSinRecargar(page, '/cumplimiento/bloqueos')
  await page.getByRole('button', { name: /^Levantar el bloqueo Embargo de Juzgado/ }).click()
  const dialogo = page.getByRole('dialog', { name: /Levantar el bloqueo: Embargo de Juzgado/ })
  await expect(dialogo).toBeVisible()
  await expect(dialogo.getByText(/distinto de quien lo registró/)).toBeVisible()
  await page.screenshot({ path: '../../.playwright/backoffice/visual/bloqueos/dialogo.escritorio.light.png', fullPage: true })
})
