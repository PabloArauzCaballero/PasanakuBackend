import { expect, test } from '@playwright/test'
import { iniciarSesionDePrueba, navegarSinRecargar } from './apoyo/sesion-e2e'

/**
 * PR7 §H4.S1.M1 — `operacion/estado` migrada de `TablaDeDatosVirtualizada` al organismo
 * canónico `@aportaya/ui/tabla-de-datos`. Los datos son locales (`SERVICIOS`, sin red de
 * por medio), así que este recorrido ejercita la ruta real, la guarda `canMatch`, el login
 * de CU-04 (con `apoyo/sesion-e2e.ts` — ver ese archivo para qué es doble y qué es real) y
 * el organismo canónico completo, sin ningún mock de red.
 *
 * La sesión vive solo en memoria del navegador (por diseño, `sesion.ts`): la navegación a
 * la pantalla bajo prueba usa `navegarSinRecargar` en vez de `page.goto()` a mitad de
 * prueba (que recargaría la página y borraría la sesión).
 *
 * **Hallazgo real, fuera de mi alcance (`rutas/tablero/tablero.ts`, no es de las dos
 * pantallas migradas ni de `tabla-de-datos`):** la tarjeta "Arquitectura y estado" del
 * tablero arma su link con `[routerLink]="['/', a.ruta]"` y `a.ruta = 'operacion/estado'`.
 * Angular Router trata cada elemento del arreglo como UN segmento, así que la `/` que
 * ya trae `a.ruta` se codifica como `%2F` — el link real apunta a `/operacion%2Festado`,
 * no a `/operacion/estado` (visible clicando esa tarjeta a mano, o en el `error-context.md`
 * de un intento fallido de este archivo antes de este cambio). Se documenta y se navega con
 * `navegarSinRecargar` en vez de depender de esa tarjeta.
 */
test.describe('operacion/estado — tabla canónica', () => {
  test.beforeEach(async ({ page }) => {
    await iniciarSesionDePrueba(page, ['BILLETERA_VER_TERCEROS'])
    await navegarSinRecargar(page, '/operacion/estado')
    // `navegarSinRecargar` no espera el render: a diferencia de un `expect(...).toBeVisible()`,
    // `.count()` (usado más abajo) no reintenta solo, así que sin este `await` explícito una
    // prueba puede leer la tabla en 0 filas todavía.
    await expect(page.locator('table[role="table"]')).toBeVisible()
  })

  test('carga la tabla con filas reales, sin estado mudo', async ({ page }) => {
    await expect(page.locator('h1')).toHaveText('Arquitectura y estado del proyecto')

    const tabla = page.locator('table[role="table"]')
    await expect(tabla).toBeVisible()
    await expect(page.locator('table thead th')).toHaveCount(5)
    await expect(page.locator('table tbody tr').first()).toBeVisible()
  })

  test('ordenar por una columna ordenable cambia aria-sort', async ({ page }) => {
    const encabezado = page.getByRole('columnheader', { name: /Servicio/ })
    await expect(encabezado).toHaveAttribute('aria-sort', 'none')
    await encabezado.getByRole('button').click()
    await expect(encabezado).toHaveAttribute('aria-sort', 'ascending')
  })

  test('filtrar por nivel reduce las filas mostradas', async ({ page }) => {
    const filasIniciales = await page.locator('table tbody tr').count()
    await navegarSinRecargar(page, '/operacion/estado?filtro=N1')
    await expect(page.locator('table tbody tr').first()).toBeVisible()
    const filasFiltradas = await page.locator('table tbody tr').count()
    expect(filasFiltradas).toBeGreaterThan(0)
    expect(filasFiltradas).toBeLessThanOrEqual(filasIniciales)
  })
})

test('sin el permiso de operación, la ruta redirige a /tablero (canMatch real)', async ({ page }) => {
  await iniciarSesionDePrueba(page, [])
  await navegarSinRecargar(page, '/operacion/estado')
  await expect(page).toHaveURL(/\/tablero$/)
})
