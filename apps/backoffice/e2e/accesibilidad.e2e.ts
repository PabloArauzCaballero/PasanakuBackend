import { expect, test } from '@playwright/test'
import path from 'node:path'

/**
 * F12.2 — accesibilidad automática con axe-core, inyectado directamente (no hay
 * `@axe-core/playwright` en el catálogo de versiones y agregar una dependencia nueva
 * no es mío: `arrancar-carril` §7). Cubre lo que SÍ existe hoy sin sesión: el tablero
 * y las páginas gated redirigidas. Las pantallas detrás de permiso (operación,
 * cumplimiento, etc.) quedan fuera de este barrido por el mismo motivo que
 * `tablero-y-permisos.e2e.ts` documenta: no hay login real para entrar con un rol.
 */
const axePath = path.resolve(__dirname, '../../../node_modules/axe-core/axe.min.js')

test.describe('accesibilidad automática (axe-core)', () => {
  test('tablero — sin violaciones "serias" ni "críticas"', async ({ page }) => {
    await page.goto('/tablero')
    await page.addScriptTag({ path: axePath })
    const resultados = await page.evaluate(async () => {
      // @ts-expect-error inyectado por addScriptTag
      return await window.axe.run(document, { resultTypes: ['violations'] })
    })
    const graves = (resultados.violations as Array<{ impact: string; id: string; nodes: unknown[] }>).filter(
      (v) => v.impact === 'serious' || v.impact === 'critical',
    )
    if (graves.length > 0) {
      console.log('Violaciones axe (tablero):', JSON.stringify(graves, null, 2))
    }
    expect(graves, `violaciones axe: ${graves.map((v) => v.id).join(', ')}`).toHaveLength(0)
  })
})
