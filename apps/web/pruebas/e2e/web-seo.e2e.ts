import { expect, test } from '@playwright/test'

/**
 * F12.1 `web-seo.e2e.ts` — canonical, JSON-LD, sitemap, robots.txt y el espejo `.md`,
 * contra el build de SSR ya hecho (`yarn build`, igual que `paginas.spec.ts` y
 * `catalogo.spec.ts` de W1). No reemplaza esas pruebas: las complementa con lo que
 * F10/F11 (GEO) agregaron y que W1 no cubría todavía.
 */
test.describe('SEO y GEO del sitio público', () => {
  test('/ trae canonical y JSON-LD Organization válido', async ({ page }) => {
    await page.goto('/')
    const canonical = await page.locator('link#ap-canonical').getAttribute('href')
    // HALLAZGO menor (no bloqueante): el canonical de home es "https://aportaya.bo"
    // (sin barra final, `apps/web/src/app/seo/servicio-meta.ts`) mientras que
    // `sitemap.xml` lista la misma página como "https://aportaya.bo/" (con barra).
    // No es un bug de indexación — Google normaliza ambas — pero es una
    // inconsistencia de estilo entre dos generadores del mismo carril (F10/F11) que
    // vale la pena unificar si se toca ese archivo por otra razón.
    expect(canonical).toBe('https://aportaya.bo')

    const ldJson = await page.locator('script#ap-json-ld').first().textContent()
    expect(ldJson).toBeTruthy()
    const datos = JSON.parse(ldJson ?? '[]')
    expect(Array.isArray(datos) ? datos[0]['@context'] : datos['@context']).toBe('https://schema.org')
  })

  test('una página indexable declara robots "index, follow" explícito', async ({ page }) => {
    await page.goto('/tarifas')
    const robots = await page.locator('meta[name="robots"]').getAttribute('content')
    expect(robots).toBe('index, follow')
  })

  test('robots.txt distingue citación (permitida) de entrenamiento (denegado) — ADR-042', async ({ request }) => {
    const r = await request.get('/robots.txt')
    expect(r.status()).toBe(200)
    const cuerpo = await r.text()
    expect(cuerpo).toMatch(/User-agent: ClaudeBot[\s\S]*Allow: \//)
    expect(cuerpo).toMatch(/User-agent: GPTBot[\s\S]*Disallow: \//)
  })

  test('sitemap.xml no incluye rutas paramétricas ni /catalogo, y trae lastmod', async ({ request }) => {
    const r = await request.get('/sitemap.xml')
    expect(r.status()).toBe(200)
    const cuerpo = await r.text()
    expect(cuerpo).toContain('<lastmod>')
    expect(cuerpo).not.toContain('/catalogo')
    expect(cuerpo).not.toMatch(/\/verificar\//)
  })

  test('llms.txt y llms-full.txt responden', async ({ request }) => {
    for (const ruta of ['/llms.txt', '/llms-full.txt']) {
      const r = await request.get(ruta)
      expect(r.status(), ruta).toBe(200)
    }
  })

  test('el espejo Markdown existe para una página de contenido (contenido/paginas/*.md)', async ({ request }) => {
    // El espejo solo se genera para páginas cuyo contenido vive en
    // `apps/web/contenido/paginas/*.md` (F11). `tarifas` y `plazos` son
    // herramientas interactivas sin ese archivo fuente, así que no tienen espejo —
    // no es un hueco, es el diseño de `scripts/contenido.mjs`.
    const r = await request.get('/reclamos.md')
    expect(r.status()).toBe(200)
    expect(r.headers()['content-type'] ?? '').toContain('text/markdown')
  })
})
