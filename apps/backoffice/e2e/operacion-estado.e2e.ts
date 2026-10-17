import { expect, test, type Page } from '@playwright/test'
import { capturar, todas } from './apoyo/capturas-f2'
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
    await expect(page.locator('table thead th')).toHaveCount(6)
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

/**
 * 2026-10-17 · la salud ya no es la constante «arriba»: la pantalla consulta `GET /indicadores/operacion/servicios`.
 * **La respuesta de auditoria es un DOBLE declarado** (la pila real se ejercita aparte): esto prueba que la pantalla
 * pinta lo que contestó cada servicio, sus estados sin dato y su apariencia, NO la sonda real.
 */
async function conSalud(page: Page, respuesta: { estado?: number; cuerpo?: unknown; colgar?: boolean }, permisos: string[]) {
  await page.route(
    (url) => url.pathname.endsWith('/api/v1/indicadores/operacion/servicios'),
    async (route) => {
      if (respuesta.colgar) return
      await route.fulfill({ status: respuesta.estado ?? 200, contentType: 'application/json', body: JSON.stringify(respuesta.cuerpo ?? { codigo: 'AP-X', mensaje: 'detalle interno' }) })
    },
  )
  await iniciarSesionDePrueba(page, permisos)
  await navegarSinRecargar(page, '/operacion/estado')
}

const SALUD = {
  consultadoEn: '2026-10-17T15:04:05Z',
  servicios: [
    ...['identidad', 'nucleo-financiero', 'aportes', 'tarifas', 'grupos', 'entregas', 'garantia', 'notificaciones', 'organizador', 'cumplimiento', 'auditoria', 'transparencia'].map((servicio, i) => ({ servicio, estado: 'ARRIBA', latenciaMs: 8 + i })),
    { servicio: 'erp', estado: 'DEGRADADO', latenciaMs: 41 },
    { servicio: 'publicidad', estado: 'CAIDO' },
  ],
}

test.describe('operacion/estado — salud consultada', () => {
  const fila = (page: Page, servicio: string) => page.locator('table tbody tr').filter({ has: page.getByRole('cell', { name: servicio, exact: true }) })

  test('con datos: cada servicio dice lo que contestó, con la hora de la consulta', async ({ page }) => {
    await conSalud(page, { cuerpo: SALUD }, ['BILLETERA_VER_TERCEROS', 'TABLERO_SISTEMAS_VER'])
    await expect(page.getByText('Salud consultada a las 11:04:05')).toBeVisible()
    await expect(fila(page, 'identidad')).toContainText('arriba')
    await expect(fila(page, 'erp')).toContainText('con problemas')
    await expect(fila(page, 'publicidad')).toContainText('no responde')
    // Que esté en el DOM no alcanza: la columna se veía cortada fuera de la tabla. El chip tiene que verse.
    for (const vp of [{ width: 360, height: 740 }, { width: 1280, height: 900 }]) {
      await page.setViewportSize(vp)
      const chip = fila(page, 'identidad').getByText('arriba', { exact: true })
      // Solo en vertical (la fila puede quedar bajo el pliegue); nunca en horizontal, que es lo que se quiere medir.
      const caja = await chip.boundingBox()
      await page.evaluate((y) => window.scrollTo(0, Math.max(0, y - 200)), caja!.y + (await page.evaluate(() => window.scrollY)))
      await expect(chip).toBeInViewport()
    }
    await todas(page, 'estado', 'salud-datos')
  })

  test('sin permiso (403): lo dice y ninguna fila inventa «arriba»', async ({ page }) => {
    await conSalud(page, { estado: 403 }, ['BILLETERA_VER_TERCEROS'])
    await expect(page.getByText(/Tu rol no puede ver la salud/)).toBeVisible()
    await expect(page.locator('table tbody')).not.toContainText('arriba')
    await capturar(page, 'estado', 'salud-sin-permiso', 'movil', 'light')
    await capturar(page, 'estado', 'salud-sin-permiso', 'escritorio', 'dark')
  })

  test('error (500): «sin dato», sin el detalle interno, y se puede volver a consultar', async ({ page }) => {
    await conSalud(page, { estado: 500 }, ['BILLETERA_VER_TERCEROS', 'TABLERO_SISTEMAS_VER'])
    await expect(page.getByText(/No se pudo consultar la salud/)).toBeVisible()
    await expect(page.getByText('detalle interno')).toHaveCount(0)
    await expect(page.getByRole('button', { name: 'Volver a consultar' })).toBeVisible()
    await capturar(page, 'estado', 'salud-error', 'movil', 'light')
    await capturar(page, 'estado', 'salud-error', 'escritorio', 'dark')
  })

  test('consultando: lo dice mientras espera, sin «arriba»', async ({ page }) => {
    await conSalud(page, { colgar: true }, ['BILLETERA_VER_TERCEROS', 'TABLERO_SISTEMAS_VER'])
    await expect(page.getByText('Preguntando a cada servicio si está arriba…')).toBeVisible()
    await expect(page.locator('table tbody')).not.toContainText('arriba')
    await capturar(page, 'estado', 'salud-consultando', 'movil', 'light')
    await capturar(page, 'estado', 'salud-consultando', 'escritorio', 'dark')
  })
})
