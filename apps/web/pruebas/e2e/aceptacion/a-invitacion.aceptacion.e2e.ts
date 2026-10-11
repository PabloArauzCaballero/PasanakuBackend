import { expect, test } from '@playwright/test'
import { capturarEstado } from '../../../../backoffice/e2e/aceptacion/apoyo/capturas'
import { Observador } from '../../../../backoffice/e2e/aceptacion/apoyo/observador'

/**
 * Lado A (apps/web) de F1: la página de invitación en todos sus estados, con capturas por viewport y tema, axe, y la higiene del secreto.
 * La página no hace red (no tiene sesión de participantes): lo que se prueba es lo que SÍ es del sitio.
 */
const G = '5b0f6f0e-3c1a-4a52-9d0e-0f3b1c2a7d11'
const I = '8e3c2d1b-7a64-4b95-a1c2-3d4e5f60718a'
const TOKEN = 'd4'.repeat(32) // 64 hex sintético

test.describe('A · página de invitación (apps/web)', () => {
  test('estados: lista, código inválido, sin código y secreto en la consulta; el secreto no queda en ningún lado', async ({ page }) => {
    const obs = new Observador(page, 'A-web')

    await page.goto(`/invitacion/${G}/${I}#t=${TOKEN}`)
    await expect(page.getByRole('status').filter({ hasText: 'Tu invitación está lista' })).toBeVisible()
    expect(page.url()).not.toContain(TOKEN)
    expect(JSON.stringify(await page.evaluate(() => ({ ...localStorage, ...sessionStorage, c: document.cookie })))).not.toContain(TOKEN)
    expect(obs.conSecreto(TOKEN)).toEqual([])
    await capturarEstado(page, 'A', 'invitacion-lista')

    await page.goto('about:blank') // otra carga de documento: un cambio solo de #fragmento no recarga la página
    await page.goto(`/invitacion/${G}/${I}#t=esto-no-es-un-secreto`)
    await expect(page.getByRole('alert').filter({ hasText: 'no tiene un código de invitación válido' })).toBeVisible()
    await expect(page).toHaveURL(new RegExp(`/invitacion/${G}/${I}$`))
    await capturarEstado(page, 'A', 'invitacion-codigo-invalido')

    await page.goto('about:blank')
    await page.goto(`/invitacion/${G}/${I}`)
    await expect(page.getByRole('alert').filter({ hasText: 'no trae un código de invitación' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Ir al inicio' })).toBeVisible()
    await capturarEstado(page, 'A', 'invitacion-sin-codigo')

    await page.goto('about:blank')
    await page.goto(`/invitacion/${G}/${I}?token=${TOKEN}`)
    await expect(page.getByText('poco seguro')).toBeVisible()
    expect(page.url()).not.toContain(TOKEN)
    await capturarEstado(page, 'A', 'invitacion-secreto-en-la-consulta')

    // El enlace con ?token= es inseguro por construcción: la navegación inicial lleva el secreto en la URL (lo hizo quien abrió el enlace). Lo que
    // se vigila es que la propia página no lo reenvíe: ninguna OTRA petición lo lleva en la URL, y las que lo llevan en cabeceras (Referer) se anotan.
    const conSecreto = obs.conSecreto(TOKEN)
    expect(conSecreto.filter((p) => p.url.includes(TOKEN)).map((p) => p.url.replace(TOKEN, '<secreto>')), 'solo la navegación inicial lleva el secreto en la URL').toEqual([`${new URL(page.url()).origin}/invitacion/${G}/${I}?token=<secreto>`])
    const porReferer = conSecreto.filter((p) => !p.url.includes(TOKEN))
    if (porReferer.length > 0) test.info().annotations.push({ type: 'DEFECTO', description: `${porReferer.length} petición(es) de la página reenvían el secreto de ?token= en cabeceras (Referer): ${porReferer.slice(0, 3).map((p) => p.url).join(', ')}` })
    expect(obs.consolaInesperada(), `consola: ${obs.consola.join(' | ')}`).toEqual([])
    expect(obs.peticiones.filter((p) => /\/api\/v1\//.test(p.url)), 'la página no habla con la API').toEqual([])
  })
})
