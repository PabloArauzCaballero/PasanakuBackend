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
 * HALLAZGO 2 — RESUELTO el 2026-09-16. Al hidratar, Angular tiraba `NG0502` y la página
 * se quedaba para siempre en el esqueleto: nunca se veía el veredicto ni el recómputo, y
 * la promesa de CU-61 («no hace falta creernos») no se cumplía en un navegador real.
 * Dos causas, las dos arregladas:
 *   · el bloque usaba `@defer (hydrate on viewport)`, así que se hidrataba DESPUÉS de que
 *     Angular descarta la caché que transfiere el servidor: volvía a pedir los datos y el
 *     DOM dejaba de coincidir. Acá el verificador es el contenido de la página y está en
 *     la primera pantalla, así que va `hydrate on immediate`;
 *   · el recómputo corría también en el servidor, y al rehacerse en el navegador cambiaba
 *     el DOM. Ahora corre solo en el navegador, que además es lo que el caso de uso
 *     promete: el servidor ya dio su veredicto, el punto es comprobarlo por fuera.
 * Esta prueba ya NO está en rojo: si vuelve a estarlo, el bug volvió.
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
    //
    // H4.S2.M2 (PR13-Ci.Frontend): antes esto era `await page.waitForTimeout(2000)`
    // -- un reloj, no una condición (regla 80.5/H4.S2). Se espera la CONDICIÓN real
    // (el encabezado que solo aparece una vez hidratado el bloque `@defer`) antes de
    // mirar la consola: es más rápido cuando hidrata ligero y no flakea cuando
    // hidrata lento, que es justo lo que un número fijo no puede garantizar en las
    // dos direcciones a la vez.
    await page.mouse.wheel(0, 200)
    await expect(page.getByRole('heading', { name: 'Recómputo en este navegador' })).toBeVisible({
      timeout: 10_000,
    })

    // Ver HALLAZGO 2: hoy esto SÍ aparece, y es el bug.
    expect(erroresDeConsola.join('\n'), 'consola sin errores de hidratación (NG0502)').not.toMatch(/NG0502/)

    await expect(page.getByRole('heading', { name: 'Veredicto del servidor' })).toBeVisible({ timeout: 10_000 })
    await expect(page.locator('.orden li').first()).toBeVisible({ timeout: 10_000 })
  })

  test('la página de verificación de certificado (CU-75) es noindex', async ({ page }) => {
    await page.goto('/verificar/un-codigo-cualquiera')
    const robots = await page.locator('meta[name="robots"]').last().getAttribute('content')
    expect(robots).toContain('noindex')
  })
})
