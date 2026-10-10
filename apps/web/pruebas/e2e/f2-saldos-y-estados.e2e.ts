import { expect, test, type Locator, type Page } from '@playwright/test'
import { appendFileSync, readFileSync } from 'node:fs'
import path from 'node:path'

/**
 * H12.S1.M2/M4/M5 en el sitio público, contra el servidor de DESARROLLO (sin backend: estas páginas no hacen red).
 * `VERIFIED_FUNCTIONAL_ONLY` en lo que no es visual; lo visual se mira en las capturas. El contraste se mide en el navegador con
 * axe-core (`CONTRASTE_INFORME=1` solo registra; sin eso, una violación falla).
 */
const CAPTURAS =
  process.env['CAPTURAS_DIR'] ?? path.resolve(__dirname, '../../../../docs/trabajo/2026-10-08-endurecimiento-integral/evidencia/carril-F2/capturas')
const VIEWPORTS = { movil: { width: 360, height: 740 }, tablet: { width: 768, height: 1024 }, escritorio: { width: 1280, height: 900 } } as const
const TEMAS = ['light', 'dark'] as const
const AXE = readFileSync(require.resolve('axe-core/axe.min.js'), 'utf8')

async function contraste(page: Page) {
  await page.evaluate(AXE)
  return page.evaluate(async () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const r = await (window as any).axe.run({ include: [document], exclude: [['ap-marca']] }, { runOnly: ['color-contrast'] }) // el logotipo está exento (WCAG 1.4.3)
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    return r.violations.flatMap((v: any) => v.nodes.map((n: any) => ({ destino: String(n.target), fg: n.any[0]?.data?.fgColor, bg: n.any[0]?.data?.bgColor, ratio: n.any[0]?.data?.contrastRatio, html: String(n.html).slice(0, 100) })))
  })
}

async function capturar(page: Page, nombre: string, vp: keyof typeof VIEWPORTS, tema: (typeof TEMAS)[number], elemento?: Locator): Promise<void> {
  await page.setViewportSize(VIEWPORTS[vp])
  await page.evaluate((t) => document.documentElement.setAttribute('data-theme', t), tema)
  // Sin transiciones: capturar con un color a mitad de camino entre un tema y el otro da texto «pálido» que no es del producto.
  await page.addStyleTag({ content: '*,*::before,*::after{transition:none !important}' })
  await page.evaluate(() => document.fonts.ready)
  const archivo = path.join(CAPTURAS, `web-${nombre}-${vp}-${tema}.png`)
  if (elemento) await elemento.screenshot({ path: archivo })
  else await page.screenshot({ path: archivo, fullPage: true })
  const desborde = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)
  expect(desborde, `${nombre} ${vp} ${tema}: sin desplazamiento horizontal`).toBeLessThanOrEqual(0)
  const malos = await contraste(page)
  if (process.env['CONTRASTE_INFORME']) {
    appendFileSync(path.join(CAPTURAS, '..', 'contraste.jsonl'), JSON.stringify({ captura: `web-${nombre}-${vp}-${tema}`, violaciones: malos }) + '\n')
    return
  }
  expect(malos, `contraste en ${nombre} ${vp} ${tema}: ${JSON.stringify(malos.slice(0, 3))}`).toEqual([])
}
const todas = async (page: Page, nombre: string, elemento?: () => Locator) => {
  for (const vp of Object.keys(VIEWPORTS) as (keyof typeof VIEWPORTS)[]) for (const tema of TEMAS) await capturar(page, nombre, vp, tema, elemento?.())
}

test.describe('sitio público · saldos y posiciones, estados y teclado', () => {
  const consola: string[] = []
  test.beforeEach(({ page }) => {
    consola.length = 0
    page.on('console', (m) => m.type() === 'error' && consola.push(m.text()))
    page.on('pageerror', (e) => consola.push(`pageerror: ${e.message}`))
  })

  test('preguntas frecuentes: explica qué es gastable, comprometido en pozos y retenido', async ({ page }) => {
    await page.goto('/preguntas')
    await expect(page.getByRole('heading', { name: '¿Todo lo que veo en mi billetera lo puedo gastar?' })).toBeVisible()
    await expect(page.getByText('Lo que invertís tampoco forma parte de ese saldo')).toBeVisible()
    await todas(page, 'preguntas-saldos')
    expect(consola, `consola: ${consola.join(' | ')}`).toEqual([])
  })

  test('catálogo: las posiciones de saldo y el error de envío se ven en claro y oscuro', async ({ page }) => {
    await page.goto('/catalogo')
    const posiciones = page.locator('ap-posiciones-de-saldo').first()
    await expect(posiciones).toContainText('Lo que podés gastar ahora')
    await expect(posiciones).toContainText('No gastable')
    await posiciones.scrollIntoViewIfNeeded()
    await todas(page, 'catalogo-posiciones', () => posiciones)
    const error = page.locator('ap-error-de-envio').first()
    await expect(error).toContainText('No pudimos confirmar si se registró')
    await todas(page, 'catalogo-error-de-envio', () => error)
    expect(consola, `consola: ${consola.join(' | ')}`).toEqual([])
  })

  test('teclado: el enlace de invitación se recorre con Tab, cada parada tiene foco visible y no hay trampas', async ({ page }) => {
    await page.goto('/invitacion/11111111-1111-4111-8111-111111111111/22222222-2222-4222-8222-222222222222#t=' + 'c3'.repeat(32))
    await expect(page.getByRole('status').filter({ hasText: 'Tu invitación está lista' })).toBeVisible()
    const paradas: string[] = []
    for (let i = 0; i < 40; i++) {
      await page.keyboard.press('Tab')
      const info = await page.evaluate(() => {
        const el = document.activeElement as HTMLElement | null
        if (!el || el === document.body) return null
        const estilo = getComputedStyle(el)
        const visible = (estilo.outlineStyle !== 'none' && parseFloat(estilo.outlineWidth) > 0) || estilo.boxShadow !== 'none'
        return { nombre: (el.getAttribute('aria-label') ?? el.textContent ?? '').trim().slice(0, 40), visible }
      })
      if (!info) break
      paradas.push(info.nombre)
      expect(info.visible, `foco visible en «${info.nombre}»`).toBe(true)
    }
    expect(paradas.length, 'el recorrido llega a elementos operables').toBeGreaterThan(5)
    expect(new Set(paradas).size, 'sin trampa de foco: el recorrido avanza').toBeGreaterThan(5)
  })

  test('sin código y código inválido: el aviso se anuncia como alerta y se puede volver al inicio con teclado', async ({ page }) => {
    await page.goto('/invitacion/g/i')
    const alerta = page.getByRole('alert').filter({ hasText: 'no trae un código de invitación' })
    await expect(alerta).toBeVisible()
    await todas(page, 'invitacion-sin-codigo')
    await page.getByRole('link', { name: 'Ir al inicio' }).focus()
    await page.keyboard.press('Enter')
    await expect(page).toHaveURL(/\/$/)
  })
})
