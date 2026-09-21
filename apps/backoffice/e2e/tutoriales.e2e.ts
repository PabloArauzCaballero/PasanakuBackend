import { expect, test, type Page } from '@playwright/test'

/**
 * Recorrido completo del motor de tutoriales, contra `ng serve` + el mock de Prism
 * (`yarn dev:mock`), igual que el resto de `e2e/`.
 *
 * Entra por la pantalla de ingreso real (CU-04) porque el token vive **solo en
 * memoria**: no hay forma de sembrar una sesión desde afuera, y eso es a propósito.
 *
 * **Por qué la cabecera `Prefer`.** El escenario por omisión de
 * `packages/simulado/ejemplos/identidad/autenticar.json` (`ok`) devuelve siempre
 * `requiereFactorAdicional: true`, también cuando la petición ya trae el factor: contra
 * ese escenario la pantalla vuelve a pedir el código en bucle y **nunca se abre
 * sesión**. Prism elige escenario con `Prefer: example=<nombre>`, y `vacio` es el que
 * responde con la sesión ya abierta. No se cambia el ejemplo `ok` porque es el que usan
 * las pruebas de contrato de la app (`apps/movil/test/identidad/autenticar_test.dart`).
 *
 * Ese token de mentira no trae claims, así que la sesión queda **sin permisos de
 * sección**: se ven los cuatro tutoriales que no piden ninguno, que son justo los que
 * este recorrido necesita. El filtrado por permiso se prueba en
 * `src/app/nucleo/tutoriales/registro.spec.ts`.
 */
test.use({ extraHTTPHeaders: { Prefer: 'example=vacio' } })

/**
 * Dentro del backoffice se navega **por el menú**, nunca con `page.goto`: una recarga
 * tira el token, que vive solo en memoria, y `requiereSesion` manda de vuelta al
 * ingreso. Es el comportamiento correcto, y la prueba tiene que respetarlo.
 */
async function irAAyuda(page: Page): Promise<void> {
  await page.locator('[data-tutorial-id="menu-ayuda"]').click()
  await expect(page).toHaveURL(/\/ayuda/)
}

async function entrar(page: Page): Promise<void> {
  await page.goto('/ingreso')
  await page.getByRole('textbox', { name: 'Teléfono' }).fill('71234567')
  await page.getByRole('textbox', { name: 'Contraseña' }).fill('una-contrasena-larga')
  await page.getByRole('button', { name: 'Continuar' }).click()
  await expect(page).toHaveURL(/\/tablero$/)
}

test.describe('centro de tutoriales', () => {
  test('se llega desde el menú y lista los tutoriales con su avance', async ({ page }) => {
    await entrar(page)
    await irAAyuda(page)
    await expect(page.locator('h1')).toHaveText('Centro de tutoriales')
    await expect(page.locator('[data-tutorial-id="ayuda-avance"]')).toContainText('tutoriales completados')
    await expect(page.locator('[data-tutorial-id="ayuda-lista"] ap-tarjeta-de-tutorial').first()).toBeVisible()
  })

  test('el buscador recorta la lista y el vacío explica por qué', async ({ page }) => {
    await entrar(page)
    await irAAyuda(page)
    await page.locator('[data-tutorial-id="ayuda-buscador"] input').fill('zzzzzz')
    await expect(page.getByText('Ningún tutorial coincide')).toBeVisible()
  })

  test('comenzar un tutorial abre el globo sobre la pantalla real y el teclado lo recorre', async ({ page }) => {
    await entrar(page)
    await irAAyuda(page)
    await page.getByRole('button', { name: 'Comenzar' }).first().click()

    const globo = page.locator('ap-globo-de-tutorial')
    await expect(globo).toBeVisible()
    await expect(globo).toContainText('Paso 1 de')
    await expect(page.locator('ap-foco-de-tutorial')).toBeVisible()

    await page.keyboard.press('ArrowRight')
    await expect(globo).toContainText('Paso 2 de')
    await page.keyboard.press('ArrowLeft')
    await expect(globo).toContainText('Paso 1 de')
  })

  test('Escape en el primer paso cierra; a la mitad pregunta antes de abandonar', async ({ page }) => {
    await entrar(page)
    await irAAyuda(page)
    await page.getByRole('button', { name: 'Comenzar' }).first().click()
    await expect(page.locator('ap-globo-de-tutorial')).toContainText('Paso 1 de')
    await page.keyboard.press('Escape')
    await expect(page.locator('ap-globo-de-tutorial')).toHaveCount(0)

    // El tutorial llevó al tablero al arrancar (su primer paso vive ahí): para volver a
    // lanzarlo hay que volver al centro.
    await irAAyuda(page)
    await page.getByRole('button', { name: /Comenzar|Continuar|Repetir/ }).first().click()
    await expect(page.locator('ap-globo-de-tutorial')).toContainText('Paso 1 de')
    await page.keyboard.press('ArrowRight')
    await page.keyboard.press('Escape')
    await expect(page.getByText('¿Dejamos el tutorial acá?')).toBeVisible()
  })

  test('el tour contextual aparece en la cabecera de una pantalla con tutorial', async ({ page }) => {
    await entrar(page)
    await expect(page.locator('[data-tutorial-id="lanzador-de-tutorial"]')).toContainText('Cómo funciona esta pantalla')
    await page.locator('[data-tutorial-id="lanzador-de-tutorial"] button').click()
    await expect(page.locator('ap-globo-de-tutorial')).toBeVisible()
  })

  test('lo dejado a medias se retoma donde quedó', async ({ page }) => {
    await entrar(page)
    await irAAyuda(page)
    await page.getByRole('button', { name: 'Comenzar' }).first().click()
    // Se espera al globo antes de tocar el teclado: el anfitrión llega por `@defer` y
    // hasta que no está montado no hay quien escuche las flechas.
    await expect(page.locator('ap-globo-de-tutorial')).toContainText('Paso 1 de')
    await page.keyboard.press('ArrowRight')
    await expect(page.locator('ap-globo-de-tutorial')).toContainText('Paso 2 de')
    await page.keyboard.press('Escape')
    await page.getByRole('button', { name: 'Dejarlo por ahora' }).click()

    await page.getByRole('button', { name: 'Continuar' }).first().click()
    await expect(page.locator('ap-globo-de-tutorial')).toContainText('Paso 2 de')
  })
})
