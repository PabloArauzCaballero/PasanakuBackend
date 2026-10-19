import { expect, test } from '@playwright/test'
import { iniciarSesionDePrueba, navegarSinRecargar } from './apoyo/sesion-e2e'

/**
 * Carril B (backoffice financiero, H1.S0.M5) · prueba visual real de una bandeja por lote en 3 viewports × 2 temas, contra el
 * mock de Prism (`@aportaya/simulado`). **Los datos son los ejemplos SINTÉTICOS del contrato** (`packages/simulado/ejemplos`), no
 * datos reales: esto prueba diseño, estados y consola, no el comportamiento del servicio. Capturas en
 * `.playwright/backoffice/visual/carril-b/` (gitignored).
 */
const VIEWPORTS = [
  { nombre: 'movil', width: 390, height: 844 },
  { nombre: 'tablet', width: 834, height: 1194 },
  { nombre: 'escritorio', width: 1440, height: 900 },
] as const
const TEMAS = ['light', 'dark'] as const

const PANTALLAS = [
  { nombre: 'tablero', ruta: '/tablero', permisos: ['RETIRO_APROBAR', 'RECLAMO_VER', 'CUMPLIMIENTO_ALERTAS'], titulo: /^Tablero$/ },
  { nombre: 'operacion-retiros', ruta: '/operacion/retiros', permisos: ['RETIRO_APROBAR'], titulo: /Retiros en revisión/ },
  { nombre: 'cumplimiento-alertas', ruta: '/cumplimiento/alertas', permisos: ['CUMPLIMIENTO_ALERTAS'], titulo: /Alertas de monitoreo/ },
  { nombre: 'contabilidad-periodo', ruta: '/contabilidad', permisos: ['CONTABILIDAD_ERP_REPORTES'], titulo: /Períodos contables|período/i },
  { nombre: 'configuracion-tarifario', ruta: '/configuracion/tarifario', permisos: ['TARIFARIO_PUBLICAR'], titulo: /Historial del tarifario/ },
] as const

for (const p of PANTALLAS) {
  for (const viewport of VIEWPORTS) {
    for (const tema of TEMAS) {
      test(`${p.nombre} — ${viewport.nombre} × ${tema}`, async ({ page }) => {
        const errores: string[] = []
        page.on('console', (m) => {
          if (m.type() === 'error') errores.push(m.text())
        })
        await page.setViewportSize({ width: viewport.width, height: viewport.height })
        await page.emulateMedia({ colorScheme: tema })
        await iniciarSesionDePrueba(page, p.permisos)
        await navegarSinRecargar(page, p.ruta)
        await expect(page.getByRole('heading', { level: 1, name: p.titulo })).toBeVisible()
        await page.waitForLoadState('networkidle')
        await page.screenshot({ path: `../../.playwright/backoffice/visual/carril-b/${p.nombre}.${viewport.nombre}.${tema}.png`, fullPage: true })
        expect(errores, 'consola sin errores').toEqual([])
      })
    }
  }
}
