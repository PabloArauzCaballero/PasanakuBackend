import { expect, test } from '@playwright/test'

/**
 * F12.1 `web-verificacion.e2e.ts` — CU-61, `/publico/sorteos/:id`
 * (`apps/web/src/app/verificadores/verificador-de-sorteo.ts`). Corre contra
 * Prism (`yarn dev:mock`, puerto 4010) porque no hay backend real desplegado en este
 * sandbox — ver `planes/informes/carril-F12.md`. El id es un UUID cualquiera: Prism
 * devuelve el mismo ejemplo declarado en
 * `packages/simulado/ejemplos/transparencia/verificarSorteo.json` sin importar el
 * parámetro.
 *
 * HALLAZGO 1 (no es de este carril, se documenta y no se toca): el verificador de
 * cadena de transparencia (`apps/web/src/app/verificadores/verificador-de-cadena.ts:37`)
 * NO recomputa nada todavía — el propio comentario del archivo dice
 * "`serializarCanonico` y `hashDeBloque` aún no tienen una implementación de
 * referencia publicada por el backend". El recorrido `/publico/grupos/:codigo` de
 * `planes/15` §F12.1 ("web-verificacion.e2e.ts … recomputando en el cliente") no se
 * puede probar de verdad para la cadena hasta que ese carril (transparencia/backend)
 * publique la referencia.
 *
 * HALLAZGO 2 — bug real encontrado por este E2E, en `apps/web/src/app/paginas/publico-sorteos/sorteo-verificacion.ts`
 * y `apps/web/src/app/verificadores/verificador-de-sorteo.ts` (dueño: el carril que
 * escribió `@defer (hydrate on viewport)` ahí — no F12): al hidratar en el cliente,
 * Angular tira `ERROR NG0502` ("hydration mismatch" en `<section>`) y
 * `TypeError: Cannot read properties of null (reading 'nextSibling')`, visible en la
 * consola del navegador real (reproducido con Playwright contra el build de
 * producción, `yarn build` + `node dist/web/server/server.mjs`, y contra
 * `yarn dev:mock`). El bloque nunca sale del esqueleto de carga: el usuario ve
 * "Consultando el paquete del sorteo" para siempre y jamás el veredicto ni el
 * recómputo — la promesa central de CU-61 ("no hace falta creernos") no se cumple
 * en un navegador real. Esta prueba queda en rojo a propósito, como evidencia: no se
 * fuerza a verde ni se esconde (política del carril, `definicion-de-terminado`).
 */
test.describe('verificación pública recomputada en el cliente', () => {
  test('el sorteo público muestra el veredicto del servidor y lo recomputa en el navegador', async ({ page }) => {
    const erroresDeConsola: string[] = []
    page.on('console', (m) => {
      if (m.type() === 'error') erroresDeConsola.push(m.text())
    })

    const id = '11111111-1111-4111-8111-111111111111'
    const r = await page.goto(`/publico/sorteos/${id}`)
    expect(r?.status()).toBe(200)

    // `@defer (hydrate on viewport)`: el SSR manda el contenido completo, pero al
    // hidratar el cliente vuelve a mostrar el placeholder hasta que el bloque entra
    // en el viewport y se hidrata de verdad.
    await page.mouse.wheel(0, 200)
    await page.waitForTimeout(2000)

    // Ver HALLAZGO 2: hoy esto SÍ aparece, y es el bug.
    expect(erroresDeConsola.join('\n'), 'consola sin errores de hidratación (NG0502)').not.toMatch(/NG0502/)

    await expect(page.getByRole('heading', { name: 'Veredicto del servidor' })).toBeVisible({ timeout: 10_000 })
    await expect(page.getByRole('heading', { name: 'Recómputo en este navegador' })).toBeVisible()
    await expect(page.locator('.orden li').first()).toBeVisible({ timeout: 10_000 })
  })

  test('la página de verificación de certificado (CU-75) es noindex', async ({ page }) => {
    await page.goto('/verificar/un-codigo-cualquiera')
    const robots = await page.locator('meta[name="robots"]').last().getAttribute('content')
    expect(robots).toContain('noindex')
  })
})
