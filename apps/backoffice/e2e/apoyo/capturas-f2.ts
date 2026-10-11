import { expect, type Page } from '@playwright/test'
import { readFileSync, writeFileSync, appendFileSync } from 'node:fs'
import path from 'node:path'

/**
 * Apoyo de las pruebas visuales de H12 (carril F2): capturas por viewport y tema, desborde horizontal y contraste medido
 * en el navegador con axe-core (jsdom no calcula contraste). `CONTRASTE_INFORME=1` solo registra las violaciones en
 * `contraste.jsonl` (para hallarlas); sin esa variable, una violación hace fallar la captura.
 */
export const CAPTURAS =
  process.env['CAPTURAS_DIR'] ?? path.resolve(__dirname, '../../../../docs/trabajo/2026-10-08-endurecimiento-integral/evidencia/carril-F2/capturas')

export const VIEWPORTS = {
  movil: { width: 360, height: 740 },
  tablet: { width: 768, height: 1024 },
  escritorio: { width: 1280, height: 900 },
} as const
export type Viewport = keyof typeof VIEWPORTS
export const TEMAS = ['light', 'dark'] as const
export type Tema = (typeof TEMAS)[number]

const AXE = readFileSync(require.resolve('axe-core/axe.min.js'), 'utf8')

type Hallazgo = { destino: string; fg: string; bg: string; ratio: number; esperado: number; html: string }

export async function contraste(page: Page): Promise<Hallazgo[]> {
  await page.evaluate(AXE)
  return page.evaluate(async () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const resultado = await (window as any).axe.run(document, { runOnly: ['color-contrast'] })
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    return resultado.violations.flatMap((v: any) =>
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      v.nodes.map((n: any) => ({
        destino: String(n.target),
        fg: n.any[0]?.data?.fgColor,
        bg: n.any[0]?.data?.bgColor,
        ratio: n.any[0]?.data?.contrastRatio,
        esperado: n.any[0]?.data?.expectedContrastRatio,
        html: String(n.html).slice(0, 110),
      })),
    )
  })
}

export async function capturar(page: Page, prefijo: string, nombre: string, viewport: Viewport, tema: Tema): Promise<void> {
  await page.setViewportSize(VIEWPORTS[viewport])
  await page.evaluate((t) => document.documentElement.setAttribute('data-theme', t), tema)
  // Sin transiciones: capturar con un color a mitad de camino entre un tema y el otro da texto «pálido» que no es del producto.
  await page.addStyleTag({ content: '*,*::before,*::after{transition:none !important}' })
  await page.evaluate(() => document.fonts.ready)
  await page.screenshot({ path: path.join(CAPTURAS, `${prefijo}-${nombre}-${viewport}-${tema}.png`), fullPage: true })
  const desborde = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)
  expect(desborde, `${nombre} ${viewport} ${tema}: la página no desplaza en horizontal`).toBeLessThanOrEqual(0)
  const malos = await contraste(page)
  const registro = { captura: `${prefijo}-${nombre}-${viewport}-${tema}`, violaciones: malos }
  if (process.env['CONTRASTE_INFORME']) {
    appendFileSync(path.join(CAPTURAS, '..', 'contraste.jsonl'), JSON.stringify(registro) + '\n')
    return
  }
  expect(malos, `contraste en ${registro.captura}: ${JSON.stringify(malos.slice(0, 3))}`).toEqual([])
}

export async function todas(page: Page, prefijo: string, nombre: string): Promise<void> {
  for (const vp of Object.keys(VIEWPORTS) as Viewport[]) for (const tema of TEMAS) await capturar(page, prefijo, nombre, vp, tema)
}

/** Para dejar constancia de lo que se midió aunque no falle. */
export function nota(archivo: string, texto: string): void {
  writeFileSync(path.join(CAPTURAS, '..', archivo), texto)
}
