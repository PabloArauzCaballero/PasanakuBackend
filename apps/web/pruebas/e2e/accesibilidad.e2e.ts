import { expect, test } from '@playwright/test'

// axe-core directo, inyectado en la página: ya es dependencia del monorepo y no hace
// falta sumar @axe-core/playwright para una sola llamada.
const AXE = require.resolve('axe-core/axe.min.js')

type Violacion = { id: string; impact: string | null; nodes: { target: string[] }[] }

/**
 * Accesibilidad de TODAS las páginas públicas, contra el build SSR real.
 *
 * Existe desde el rediseño del sitio con la estética de la landing: la suite unitaria de
 * accesibilidad cubría un solo archivo, y un rediseño completo sin esto es revisar a ojo
 * una vez y confiar para siempre. Falla con cualquier violación seria o crítica, y con
 * más o menos de un `h1`.
 */
const RUTAS = [
  '/', '/como-funciona', '/tarifas', '/transparencia', '/seguridad', '/preguntas', '/plazos',
  '/descargar', '/reclamos', '/privacidad', '/contrato-de-adhesion', '/legal/estado-regulatorio',
]

for (const ruta of RUTAS) {
  test(`${ruta} · un h1 y sin violaciones serias de accesibilidad`, async ({ page }) => {
    const r = await page.goto(ruta)
    expect(r?.status()).toBe(200)
    await expect(page.locator('h1')).toHaveCount(1)
    await page.addScriptTag({ path: AXE })
    const violations = await page.evaluate(async () => {
      const axe = (window as unknown as { axe: { run: (c: Document, o: object) => Promise<{ violations: Violacion[] }> } }).axe
      // El «Ya» naranja del logotipo queda fuera del contraste A PROPÓSITO y SOLO ese nodo:
      // WCAG 1.4.3 exime el texto que es parte de un logotipo o nombre de marca, y
      // oscurecerlo lo separaría del logo de la app. Todo otro texto se sigue midiendo.
      const contexto = { exclude: [['.marca .palabra .ya']] }
      return (await axe.run(contexto as unknown as Document, { runOnly: { type: 'tag', values: ['wcag2a', 'wcag2aa', 'wcag21aa'] } })).violations
    })
    const graves = violations.filter((v) => v.impact === 'serious' || v.impact === 'critical')
    expect(graves.map((v) => `${v.id}: ${v.nodes.length} nodo(s) · ${v.nodes[0]?.target.join(' ')}`)).toEqual([])
  })
}
