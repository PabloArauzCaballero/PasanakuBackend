import { expect, type Page } from '@playwright/test'
import { appendFileSync, mkdirSync, readFileSync } from 'node:fs'
import path from 'node:path'
import { CAPTURAS, EVIDENCIA, MODO, TEMAS, VIEWPORTS, type Tema, type Viewport } from './entorno'

/**
 * Capturas por viewport (móvil/tablet/escritorio) y tema (claro/oscuro) de UN estado, con:
 *  - desborde horizontal medido (hace fallar la prueba: es un defecto visible);
 *  - axe-core completo (WCAG 2.0/2.1/2.2 A y AA + mejores prácticas) registrado en `axe-<modo>.jsonl`. Las violaciones NO hacen fallar la
 *    prueba funcional (son defectos de `src/` que se reportan al agente de frontend); `AXE_ESTRICTO=1` las convierte en fallo.
 */
const AXE = readFileSync(require.resolve('axe-core/axe.min.js'), 'utf8')
mkdirSync(CAPTURAS, { recursive: true })
const INFORME_AXE = path.join(EVIDENCIA, `axe-${MODO}.jsonl`)
const INDICE = path.join(EVIDENCIA, `capturas-${MODO}.jsonl`)

type Nodo = { destino: string; resumen: string; html: string }
export type Violacion = { regla: string; impacto: string | null; ayuda: string; nodos: Nodo[] }

export async function auditarAxe(page: Page): Promise<Violacion[]> {
  await page.evaluate(AXE)
  return page.evaluate(async () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const r = await (window as any).axe.run(document, { runOnly: { type: 'tag', values: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa', 'best-practice'] } })
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    return r.violations.map((v: any) => ({
      regla: v.id,
      impacto: v.impact,
      ayuda: v.help,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      nodos: v.nodes.slice(0, 4).map((n: any) => ({ destino: String(n.target), resumen: String(n.failureSummary ?? '').split('\n').slice(0, 3).join(' | '), html: String(n.html).slice(0, 140) })),
    }))
  })
}

/** Una captura por viewport y tema del estado `nombre` del flujo `flujo`. Devuelve las violaciones de axe halladas. */
export async function capturarEstado(page: Page, flujo: string, nombre: string, opciones: { viewports?: Viewport[]; temas?: readonly Tema[]; ruta?: string } = {}): Promise<Violacion[]> {
  const todas: Violacion[] = []
  for (const vp of opciones.viewports ?? (Object.keys(VIEWPORTS) as Viewport[])) {
    for (const tema of opciones.temas ?? TEMAS) {
      await page.setViewportSize(VIEWPORTS[vp])
      await page.emulateMedia({ colorScheme: tema })
      await page.evaluate((t) => document.documentElement.setAttribute('data-theme', t), tema)
      await page.addStyleTag({ content: '*,*::before,*::after{transition:none !important;animation:none !important}' })
      await page.evaluate(() => document.fonts.ready)
      const archivo = `${flujo}-${nombre}-${vp}-${tema}.png`
      await page.screenshot({ path: path.join(CAPTURAS, archivo), fullPage: true })
      const desborde = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)
      const violaciones = await auditarAxe(page)
      appendFileSync(INDICE, JSON.stringify({ archivo, url: page.url().replace(/#.*$/, ''), desborde, axe: violaciones.length }) + '\n')
      if (violaciones.length > 0) appendFileSync(INFORME_AXE, JSON.stringify({ archivo, violaciones }) + '\n')
      todas.push(...violaciones)
      expect(desborde, `${flujo}/${nombre} ${vp} ${tema}: la página no desplaza en horizontal`).toBeLessThanOrEqual(0)
    }
  }
  if (process.env['AXE_ESTRICTO']) expect(todas.map((v) => v.regla), `axe en ${flujo}/${nombre}`).toEqual([])
  return todas
}
