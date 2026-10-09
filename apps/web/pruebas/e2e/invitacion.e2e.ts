import { expect, test, type Page } from '@playwright/test'
import path from 'node:path'

/**
 * H12.S1.M4/M5 · enlace de invitación en el sitio público: el secreto sale de la URL.
 *
 * No toca el backend: la página no hace red. Se verifica lo que SÍ es del sitio: que el secreto
 * (fragmento `#t=`) no queda en la barra de direcciones, en el historial, en almacenamiento
 * accesible por script ni en ninguna petición, y que la página se ve bien en 3 viewports y 2 temas.
 */
const TOKEN = 'c3'.repeat(32) // 64 hex, sintético
const CAPTURAS =
  process.env['CAPTURAS_DIR'] ?? path.resolve(__dirname, '../../../../docs/trabajo/2026-10-08-endurecimiento-integral/evidencia/carril-E/capturas')
const VIEWPORTS = { movil: { width: 360, height: 740 }, tablet: { width: 768, height: 1024 }, escritorio: { width: 1280, height: 900 } } as const

async function capturar(page: Page, nombre: string, vp: keyof typeof VIEWPORTS, tema: 'light' | 'dark'): Promise<void> {
  await page.setViewportSize(VIEWPORTS[vp])
  await page.evaluate((t) => document.documentElement.setAttribute('data-theme', t), tema)
  await page.screenshot({ path: path.join(CAPTURAS, `web-${nombre}-${vp}-${tema}.png`), fullPage: true })
  const desborde = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)
  expect(desborde, `${nombre} ${vp} ${tema}: sin desplazamiento horizontal`).toBeLessThanOrEqual(0)
}

test.describe('invitación por enlace (sitio público)', () => {
  const consola: string[] = []
  const peticiones: string[] = []
  test.beforeEach(({ page }) => {
    consola.length = 0
    peticiones.length = 0
    page.on('console', (m) => m.type() === 'error' && consola.push(m.text()))
    page.on('pageerror', (e) => consola.push(`pageerror: ${e.message}`))
    page.on('request', (r) => peticiones.push(`${r.url()} ${r.postData() ?? ''} ${JSON.stringify(r.headers())}`))
  })

  test('con secreto en el fragmento: se retira de la URL, no queda en storage ni en peticiones, y no promete membresía', async ({ page }) => {
    await page.goto(`/invitacion/11111111-1111-4111-8111-111111111111/22222222-2222-4222-8222-222222222222#t=${TOKEN}`)
    await expect(page.getByRole('status').filter({ hasText: 'Tu invitación está lista' })).toBeVisible()
    await expect(page).toHaveURL(/\/invitacion\/[0-9a-f-]+\/[0-9a-f-]+$/)
    expect(page.url()).not.toContain(TOKEN)
    await expect(page.getByText('no entrás automáticamente')).toBeVisible()

    const almacenado = await page.evaluate(() => JSON.stringify({ local: { ...localStorage }, sesion: { ...sessionStorage }, cookies: document.cookie }))
    expect(almacenado).not.toContain(TOKEN)
    expect(peticiones.join('\n')).not.toContain(TOKEN)
    await expect(page.locator('body')).not.toContainText(TOKEN)

    for (const vp of Object.keys(VIEWPORTS) as (keyof typeof VIEWPORTS)[]) for (const tema of ['light', 'dark'] as const) await capturar(page, 'invitacion-lista', vp, tema)
    expect(consola, `consola: ${consola.join(' | ')}`).toEqual([])
  })

  test('el historial no guarda el secreto: «atrás» no lo devuelve', async ({ page }) => {
    await page.goto('about:blank')
    await page.goto(`/invitacion/g/i#t=${TOKEN}`)
    await expect(page.getByRole('status').filter({ hasText: 'Tu invitación está lista' })).toBeVisible()
    // Una sola entrada de historial para la invitación: atrás vuelve a la página anterior y adelante
    // regresa a la invitación SIN el secreto (se reemplazó la entrada, no se agregó otra).
    await page.goBack()
    await expect(page).toHaveURL('about:blank')
    await page.goForward()
    await expect(page).toHaveURL(/\/invitacion\/g\/i$/)
    expect(page.url()).not.toContain(TOKEN)
  })

  test('código inválido: alerta clara, nada guardado, secreto fuera de la URL', async ({ page }) => {
    await page.goto('/invitacion/g/i#t=esto-no-es-un-secreto')
    await expect(page.getByRole('alert').filter({ hasText: 'no tiene un código de invitación válido' })).toBeVisible()
    await expect(page).toHaveURL(/\/invitacion\/g\/i$/)
    await capturar(page, 'invitacion-invalida', 'movil', 'light')
  })

  test('sin código: alerta clara con salida al inicio', async ({ page }) => {
    await page.goto('/invitacion/g/i')
    await expect(page.getByRole('alert').filter({ hasText: 'no trae un código de invitación' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Ir al inicio' })).toBeVisible()
    await capturar(page, 'invitacion-sin-codigo', 'tablet', 'dark')
  })

  test('el secreto en la consulta (?token=) también se retira y se avisa', async ({ page }) => {
    await page.goto(`/invitacion/g/i?token=${TOKEN}`)
    await expect(page.getByText('poco seguro')).toBeVisible()
    await expect(page).toHaveURL(/\/invitacion\/g\/i$/)
    expect(page.url()).not.toContain(TOKEN)
  })
})
