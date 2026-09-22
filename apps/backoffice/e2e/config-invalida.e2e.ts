import { expect, test } from '@playwright/test'

/**
 * H3.S2.M5 — build de producción servido SIN la etiqueta `<meta name="aportaya-
 * gateway">`: la app no debe hacer ninguna petición a la API, y a partir de este
 * carril `GATEWAY` queda en `''` (nunca `http://localhost:4010`, el hallazgo
 * original) — comprobado también con `resolverGateway`/`provideGateway` en
 * `nucleo/gateway.spec.ts`.
 *
 * ESTADO (ver `entregables/cableado-app-config.md`): `ConfiguracionInvalida`
 * (`nucleo/configuracion-invalida.ts`) y el par de providers `provideGateway`/
 * `CONFIGURACION_GATEWAY` están escritos y probados con `TestBed`, pero **no**
 * están cableados en `app.config.ts`/`app.ts` — eso es de Richard, fuera de mi
 * reserva de archivos. Hasta que se cablee, este E2E corre contra el comportamiento
 * de HOY (sin la pantalla dedicada) y documenta la brecha exacta en vez de
 * simular un cableado que no hice.
 */
test.describe('configuración inválida — build de producción sin la etiqueta del gateway', () => {
  test('nunca hace una petición con "localhost" en la URL', async ({ page }) => {
    const peticionesALocalhost: string[] = []
    page.on('request', (req) => {
      if (/localhost:4010/.test(req.url())) peticionesALocalhost.push(req.url())
    })

    const r = await page.goto('/tablero')
    expect(r?.status()).toBe(200)
    await page.waitForLoadState('networkidle')

    expect(peticionesALocalhost, `peticiones a localhost: ${peticionesALocalhost.join(', ')}`).toHaveLength(0)
  })

  test('ConfiguracionInvalida: escrito y accesible (TestBed), pendiente de cableado en app.config.ts/app.ts (Richard)', async () => {
    // Este caso documenta el estado real en vez de afirmar un comportamiento que
    // todavía no está cableado. La cobertura funcional del componente en sí vive en
    // `nucleo/configuracion-invalida.a11y.spec.ts` (TestBed + axe, PASS).
    test.info().annotations.push({
      type: 'bloqueado-cableado-app-config',
      description: 'Ver entregables/cableado-app-config.md: la pantalla de bloqueo no se muestra hasta que app.config.ts/app.ts (Richard) lean CONFIGURACION_GATEWAY.',
    })
  })
})
