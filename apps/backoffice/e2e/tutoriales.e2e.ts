import { expect, test, type Page } from '@playwright/test'
import { sesionDeOperador } from './sesion-de-operador'

/**
 * Recorrido completo del motor de tutoriales, contra `ng serve` + el mock de Prism,
 * igual que el resto de `e2e/`.
 *
 * Entra con `sesionDeOperador` (H5.S1.M2, `./sesion-de-operador.ts`) por la pantalla
 * de ingreso real (CU-04) porque el token vive **solo en memoria**: no hay forma de
 * sembrar una sesión desde afuera, y eso es a propósito.
 *
 * Ese token de mentira no trae claims, así que la sesión queda **sin permisos de
 * sección**: se ven los cuatro tutoriales que no piden ninguno, que son justo los que
 * este recorrido necesita. El filtrado por permiso se prueba en
 * `src/app/nucleo/tutoriales/registro.spec.ts`.
 */

/**
 * Dentro del backoffice se navega **por el menú**, nunca con `page.goto`: una recarga
 * tira el token, que vive solo en memoria, y `requiereSesion` manda de vuelta al
 * ingreso. Es el comportamiento correcto, y la prueba tiene que respetarlo.
 */
async function irAAyuda(page: Page): Promise<void> {
  await page.locator('[data-tutorial-id="menu-ayuda"]').click()
  await expect(page).toHaveURL(/\/ayuda/)
}

test.describe('centro de tutoriales', () => {
  test('se llega desde el menú y lista los tutoriales con su avance', async ({ page }) => {
    await sesionDeOperador(page)
    await irAAyuda(page)
    await expect(page.locator('h1')).toHaveText('Centro de tutoriales')
    await expect(page.locator('[data-tutorial-id="ayuda-avance"]')).toContainText('tutoriales completados')
    await expect(page.locator('[data-tutorial-id="ayuda-lista"] ap-tarjeta-de-tutorial').first()).toBeVisible()
  })

  test('el buscador recorta la lista y el vacío explica por qué', async ({ page }) => {
    await sesionDeOperador(page)
    await irAAyuda(page)
    await page.locator('[data-tutorial-id="ayuda-buscador"] input').fill('zzzzzz')
    await expect(page.getByText('Ningún tutorial coincide')).toBeVisible()
  })

  test('comenzar un tutorial abre el globo sobre la pantalla real y el teclado lo recorre', async ({ page }) => {
    await sesionDeOperador(page)
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
    await sesionDeOperador(page)
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
    await sesionDeOperador(page)
    await expect(page.locator('[data-tutorial-id="lanzador-de-tutorial"]')).toContainText('Cómo funciona esta pantalla')
    await page.locator('[data-tutorial-id="lanzador-de-tutorial"] button').click()
    await expect(page.locator('ap-globo-de-tutorial')).toBeVisible()
  })

  test('lo dejado a medias se retoma donde quedó', async ({ page }) => {
    await sesionDeOperador(page)
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
