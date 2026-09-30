import { expect, test } from '@playwright/test'

/**
 * `/catalogo` es la evidencia visual de F1-W (plan 22 §7): se captura en claro y oscuro
 * a ancho de escritorio y de teléfono, no se indexa, y todo lo que se toca mide ≥ 44px.
 */
const CAPTURAS = 'capturas'

/**
 * H4.S2.M2 (PR13-Ci.Frontend): reemplaza los `page.waitForTimeout(300)` que había
 * acá -- un número fijo no es una condición, y ni garantiza que el repintado ya
 * terminó en un runner lento ni evita perder 300ms de nada en uno rápido. Dos
 * `requestAnimationFrame` encadenados SÍ son una condición: el navegador solo
 * ejecuta el segundo callback después de haber pintado el frame donde se aplicó el
 * primero, así que cuando esto resuelve, el cambio de tema (o de viewport) ya se
 * reflejó en pantalla.
 */
async function esperarRepintado(page: import('@playwright/test').Page): Promise<void> {
  await page.evaluate(
    () => new Promise<void>((resolver) => requestAnimationFrame(() => requestAnimationFrame(() => resolver()))),
  )
}

test.describe('catálogo de @aportaya/ui', () => {
  test('no se indexa y se prerenderiza con todas las secciones', async ({ page }) => {
    const respuesta = await page.goto('/catalogo')
    expect(respuesta?.status()).toBe(200)
    await expect(page.locator('meta[name="robots"]')).toHaveAttribute('content', /noindex/)
    for (const ancla of ['botones', 'campos', 'seleccion', 'indicadores', 'tarjetas', 'listas', 'avisos', 'negocio', 'organismos']) {
      await expect(page.locator(`#${ancla}`)).toBeVisible()
    }
  })

  for (const tema of ['light', 'dark'] as const) {
    test(`captura en tema ${tema}, escritorio y teléfono`, async ({ page }) => {
      await page.goto('/catalogo')
      await page.evaluate((t) => document.documentElement.setAttribute('data-theme', t), tema)
      await esperarRepintado(page)
      await page.screenshot({ path: `${CAPTURAS}/catalogo-${tema}-escritorio.png`, fullPage: true })
      await page.setViewportSize({ width: 400, height: 860 })
      await esperarRepintado(page)
      await page.screenshot({ path: `${CAPTURAS}/catalogo-${tema}-telefono.png`, fullPage: true })
      const anchoDelDocumento = await page.evaluate(() => document.documentElement.scrollWidth)
      expect(anchoDelDocumento, 'el cuerpo no desplaza en horizontal a 400px').toBeLessThanOrEqual(400)
    })
  }

  test('todo lo que se toca mide al menos 44 píxeles de alto', async ({ page }) => {
    await page.goto('/catalogo')
    const chicos = await page.evaluate(() => {
      const min = 44
      return [...document.querySelectorAll<HTMLElement>('button:not([disabled]), a[href], input:not([type="checkbox"]):not([type="radio"])')]
        .filter((e) => e.offsetParent !== null && e.getBoundingClientRect().height > 0)
        .filter((e) => Math.max(e.getBoundingClientRect().height, (e.closest('label, ap-boton, .caja') as HTMLElement | null)?.getBoundingClientRect().height ?? 0) < min)
        .map((e) => `${e.tagName.toLowerCase()} «${(e.getAttribute('aria-label') ?? e.textContent ?? '').trim().slice(0, 30)}»`)
    })
    expect(chicos).toEqual([])
  })
})
