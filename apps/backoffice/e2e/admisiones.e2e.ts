import { expect, test, type Page, type Route } from '@playwright/test'
import { readFileSync } from 'node:fs'
import path from 'node:path'

/**
 * H12.S1.M1/M3/M5 · admisión humana en el backoffice.
 *
 * **Qué es esto:** un E2E de UI contra el servidor de desarrollo con la RED SIMULADA — la sesión y
 * las tres rutas de admisión se responden con `page.route`, con los cuerpos de
 * `packages/simulado/ejemplos/grupos/*.json`, y el resto va a Prism (`yarn dev:mock`, 4010).
 * **No prueba integración con `grupos`**: prueba la pantalla, el cliente y su recuperación ante
 * cortes de red, que Prism (sin estado, sin fallos) no sabe producir.
 *
 * Capturas: `CAPTURAS_DIR` (por defecto, la carpeta de evidencia del carril E).
 */
const SOLICITUD = '9f1c2b7e-3a54-4d0e-8a61-5b7d2c9e4f10'
const CAPTURAS =
  process.env['CAPTURAS_DIR'] ?? path.resolve(__dirname, '../../../docs/trabajo/2026-10-08-endurecimiento-integral/evidencia/carril-E/capturas')

type Decision = { id: string; fase: string; decision: string; propuestaId?: string; revision: number; motivo: string; actorId: string }
const EJEMPLO = JSON.parse(readFileSync(path.resolve(__dirname, '../../../packages/simulado/ejemplos/grupos/historialAdmision.json'), 'utf8')) as {
  escenarios: { ok: { cuerpo: Decision[] } }
}
const [PROPUESTA, RESOLUCION] = EJEMPLO.escenarios.ok.cuerpo as [Decision, Decision]

const VIEWPORTS = {
  movil: { width: 360, height: 740 },
  tablet: { width: 768, height: 1024 },
  escritorio: { width: 1280, height: 900 },
} as const
const TEMAS = ['light', 'dark'] as const

function jwt(permisos: string[], sub = 'bo-operador-1'): string {
  const cuerpo = Buffer.from(JSON.stringify({ sub, rol: 'BACKOFFICE', permisos })).toString('base64url')
  return `eyJhbGciOiJub25lIn0.${cuerpo}.firma-de-prueba`
}

/** Lo que el «servidor» simulado recuerda entre peticiones y recargas de la página. */
type Mundo = { registradas: Set<string>; decisiones: Decision[]; resoluciones: { clave: string | null; cuerpo: unknown }[]; fallarResolucion: 'no' | 'sin-llegar' | 'llega-pero-sin-respuesta'; demoraMs: number }

async function simular(page: Page, mundo: Mundo, permisos: string[]): Promise<void> {
  await page.route('**/api/v1/sesiones', (r: Route) => r.fulfill({ status: 200, json: { tokenAcceso: jwt(permisos), requiereFactorAdicional: false } }))
  await page.route(`**/api/v1/grupos/solicitudes/${SOLICITUD}/decisiones`, async (r: Route) => {
    if (mundo.demoraMs > 0) await new Promise((ok) => setTimeout(ok, mundo.demoraMs))
    await r.fulfill({ status: 200, json: mundo.decisiones })
  })
  await page.route(`**/api/v1/grupos/solicitudes/${SOLICITUD}/resoluciones`, async (r: Route) => {
    const clave = r.request().headers()['idempotency-key'] ?? null
    mundo.resoluciones.push({ clave, cuerpo: r.request().postDataJSON() })
    const yaRegistrada = clave !== null && mundo.registradas.has(clave)
    const cuerpo = r.request().postDataJSON() as { motivo: string; decision: string }
    const nueva: Decision = { ...RESOLUCION, motivo: cuerpo.motivo, decision: cuerpo.decision }
    if (mundo.fallarResolucion === 'sin-llegar') return r.abort('connectionreset')
    // Idempotencia del lado «servidor»: la misma clave devuelve la misma decisión, no crea otra.
    if (!yaRegistrada) {
      mundo.decisiones = [...mundo.decisiones.filter((d) => d.fase !== 'RESOLUCION'), nueva]
      if (clave !== null) mundo.registradas.add(clave)
    }
    if (mundo.fallarResolucion === 'llega-pero-sin-respuesta') return r.abort('connectionreset')
    return r.fulfill({ status: 200, json: nueva })
  })
}

async function ingresar(page: Page, yaEnElIngreso = false): Promise<void> {
  if (!yaEnElIngreso) await page.goto('/ingreso')
  await page.getByLabel(/Teléfono/).fill('71234567')
  await page.getByLabel(/Contraseña/).fill('contrasena-de-prueba')
  await page.getByRole('button', { name: 'Continuar' }).click()
  await expect(page.getByRole('heading', { name: 'Tablero' })).toBeVisible()
}

/** Navegación dentro de la app (sin recargar: la sesión vive solo en memoria). */
async function irA(page: Page, ruta: string): Promise<void> {
  await page.evaluate((r) => {
    window.history.pushState({}, '', r)
    window.dispatchEvent(new PopStateEvent('popstate'))
  }, ruta)
}

async function capturar(page: Page, nombre: string, viewport: keyof typeof VIEWPORTS, tema: (typeof TEMAS)[number]): Promise<void> {
  await page.setViewportSize(VIEWPORTS[viewport])
  await page.evaluate((t) => document.documentElement.setAttribute('data-theme', t), tema)
  await page.screenshot({ path: path.join(CAPTURAS, `bo-${nombre}-${viewport}-${tema}.png`), fullPage: true })
  const desborde = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)
  expect(desborde, `${nombre} ${viewport} ${tema}: la página no desplaza en horizontal`).toBeLessThanOrEqual(0)
}

const mundoNuevo = (decisiones: Decision[]): Mundo => ({ registradas: new Set(), decisiones, resoluciones: [], fallarResolucion: 'no', demoraMs: 0 })

test.describe('[UI con red simulada] admisión humana en backoffice', () => {
  const consola: string[] = []
  test.beforeEach(({ page }) => {
    consola.length = 0
    page.on('console', (m) => {
      if (m.type() === 'error') consola.push(m.text())
    })
    page.on('pageerror', (e) => consola.push(`pageerror: ${e.message}`))
  })

  // Causa demostrada de la primera carga lenta: el servidor de desarrollo (Vite) optimiza dependencias
  // la primera vez que un navegador pide la app (ver ng-bo.log: «optimizer bundling dependencies»).
  // Esta prueba paga ese costo una vez, con su propio plazo, y de paso verifica que el ingreso carga.
  test('arranque en frío: el ingreso carga', async ({ page }) => {
    test.setTimeout(150_000)
    await page.goto('/ingreso', { timeout: 120_000 })
    await expect(page.getByRole('heading', { name: /Ingresá con tu cuenta de operador/ })).toBeVisible({ timeout: 60_000 })
  })

  test('BO con permiso: ve la propuesta y la evidencia, resuelve, y la resolución persiste tras recargar', async ({ page }) => {
    const mundo = mundoNuevo([PROPUESTA])
    await simular(page, mundo, ['ADMIN_PLATAFORMA', 'VERIFICACION_RESOLVER'])
    await ingresar(page)
    await irA(page, `/admisiones/${SOLICITUD}`)

    await expect(page.getByRole('heading', { name: 'Solicitud de ingreso a un grupo' })).toBeVisible()
    await expect(page.getByRole('heading', { name: 'Evidencia del algoritmo' })).toBeVisible()
    await expect(page.getByText('Es una recomendación')).toBeVisible()
    for (const vp of Object.keys(VIEWPORTS) as (keyof typeof VIEWPORTS)[]) for (const tema of TEMAS) await capturar(page, 'propuesta-y-formulario', vp, tema)

    // Sin motivo ni resultado: error por campo, ninguna petición.
    await page.getByRole('button', { name: 'Resolver solicitud' }).click()
    await expect(page.getByText('Escribí el motivo')).toBeVisible()
    await expect(page.getByText('Elegí si aceptás o rechazás')).toBeVisible()
    expect(mundo.resoluciones).toHaveLength(0)
    await capturar(page, 'errores-por-campo', 'movil', 'light')

    await page.getByLabel('Aceptar').check({ force: true })
    await page.getByLabel(/Motivo de la resolución/).fill('Evidencia suficiente; se reservan los cupos.')
    await page.getByRole('button', { name: 'Resolver solicitud' }).click()
    await expect(page.getByText('Resolución registrada. La solicitud quedó cerrada.')).toBeVisible()
    expect(mundo.resoluciones).toHaveLength(1)
    expect(mundo.resoluciones[0]?.clave).toMatch(/^[0-9a-f-]{36}$/)
    await expect(page.getByRole('button', { name: 'Resolver solicitud' })).toHaveCount(0)
    await capturar(page, 'resolucion-confirmada', 'escritorio', 'dark')

    // Persistencia: recargar pierde la sesión (solo en memoria, a propósito); al volver a entrar, la resolución sigue.
    await page.reload()
    await expect(page).toHaveURL(/\/ingreso$/)
    await ingresar(page, true)
    await irA(page, `/admisiones/${SOLICITUD}`)
    await expect(page.getByRole('heading', { name: 'Resolución de backoffice', level: 3 })).toBeVisible()
    await expect(page.getByText('La solicitud ya está resuelta')).toBeVisible()
    expect(consola, `consola: ${consola.join(' | ')}`).toEqual([])
  })

  test('rol sin el permiso de la plataforma: la sección no aparece y la ruta no monta (sin acciones ajenas)', async ({ page }) => {
    await simular(page, mundoNuevo([PROPUESTA]), ['VERIFICACION_RESOLVER'])
    await ingresar(page)
    await expect(page.getByRole('link', { name: 'Solicitudes de ingreso' })).toHaveCount(0)
    await expect(page.getByRole('navigation').getByRole('link', { name: 'Admisiones' })).toHaveCount(0)
    await irA(page, `/admisiones/${SOLICITUD}`)
    await expect(page).toHaveURL(/\/sin-permiso/)
    await expect(page.getByRole('button', { name: 'Resolver solicitud' })).toHaveCount(0)
    await capturar(page, 'sin-permiso', 'movil', 'dark')
    expect(consola, `consola: ${consola.join(' | ')}`).toEqual([])
  })

  test('sin sesión no se entra a la solicitud', async ({ page }) => {
    await page.goto(`/admisiones/${SOLICITUD}`)
    await expect(page).toHaveURL(/\/ingreso$/)
  })

  test('estados: cargando, vacío y error del servidor', async ({ page }) => {
    const mundo = mundoNuevo([])
    mundo.demoraMs = 1500
    await simular(page, mundo, ['ADMIN_PLATAFORMA'])
    await ingresar(page)
    await irA(page, `/admisiones/${SOLICITUD}`)
    await expect(page.getByRole('status', { name: 'Cargando el expediente de la solicitud' })).toBeVisible()
    await capturar(page, 'cargando', 'escritorio', 'light')
    await expect(page.getByText('todavía no propuso una decisión')).toBeVisible()
    await capturar(page, 'vacio', 'movil', 'light')

    await page.unroute(`**/api/v1/grupos/solicitudes/${SOLICITUD}/decisiones`)
    await page.route(`**/api/v1/grupos/solicitudes/${SOLICITUD}/decisiones`, (r) =>
      r.fulfill({ status: 422, json: { codigo: 'AP-CU68-06', mensaje: 'El expediente de decisión requiere permisos de revisión.', trazaId: 'traza-e2e-1' } }),
    )
    await irA(page, '/admisiones')
    await expect(page.getByRole('heading', { name: 'Abrir una solicitud' })).toBeVisible()
    await irA(page, `/admisiones/${SOLICITUD}`)
    await expect(page.getByRole('alert').getByText(/Actualizá el expediente/)).toBeVisible()
    await expect(page.getByText('Código de seguimiento: traza-e2e-1')).toBeVisible()
    await capturar(page, 'error', 'tablet', 'dark')
    // Los 422 y el aborto intencional de red generan «Failed to load resource» en consola: se esperan aquí.
    expect(consola.filter((c) => !/Failed to load resource/.test(c)), `consola: ${consola.join(' | ')}`).toEqual([])
  })

  test('respuesta perdida: queda sin confirmar, verifica, reintenta con la MISMA clave y no duplica', async ({ page }) => {
    const mundo = mundoNuevo([PROPUESTA])
    await simular(page, mundo, ['ADMIN_PLATAFORMA'])
    await ingresar(page)
    await irA(page, `/admisiones/${SOLICITUD}`)
    await expect(page.getByRole('heading', { name: 'Tu resolución' })).toBeVisible()
    await page.getByLabel('Rechazar').check({ force: true })
    await page.getByLabel(/Motivo de la resolución/).fill('La concentración de riesgo supera el límite del grupo.')

    // 1) El envío llega al servidor y se registra, pero la respuesta se pierde.
    mundo.fallarResolucion = 'llega-pero-sin-respuesta'
    await page.getByRole('button', { name: 'Resolver solicitud' }).click()
    await expect(page.getByText('No pudimos confirmar si tu resolución quedó registrada')).toBeVisible()
    await expect(page.getByLabel(/Motivo de la resolución/)).toHaveValue('La concentración de riesgo supera el límite del grupo.')
    await capturar(page, 'sin-confirmar', 'tablet', 'light')
    await capturar(page, 'sin-confirmar', 'movil', 'dark')

    // 2) Verificar consulta el historial: encuentra la resolución y NO se reenvía nada.
    await page.getByRole('button', { name: 'Verificar si se registró' }).click()
    await expect(page.getByText('Resolución registrada. La solicitud quedó cerrada.')).toBeVisible()
    expect(mundo.resoluciones).toHaveLength(1)
    expect(consola.filter((c) => !/Failed to load resource|ERR_/.test(c)), `consola: ${consola.join(' | ')}`).toEqual([])
  })

  test('envío que nunca llegó: verificar no la encuentra y el reintento reutiliza la misma clave', async ({ page }) => {
    const mundo = mundoNuevo([PROPUESTA])
    await simular(page, mundo, ['ADMIN_PLATAFORMA'])
    await ingresar(page)
    await irA(page, `/admisiones/${SOLICITUD}`)
    await page.getByLabel('Aceptar').check({ force: true })
    await page.getByLabel(/Motivo de la resolución/).fill('Referencias verificadas por el equipo.')

    mundo.fallarResolucion = 'sin-llegar'
    await page.getByRole('button', { name: 'Resolver solicitud' }).click()
    await expect(page.getByText('No pudimos confirmar si tu resolución quedó registrada')).toBeVisible()
    await page.getByRole('button', { name: 'Verificar si se registró' }).click()
    await expect(page.getByText('La resolución no se registró')).toBeVisible()
    await capturar(page, 'no-registrada', 'escritorio', 'light')

    mundo.fallarResolucion = 'no'
    await page.getByRole('button', { name: 'Reintentar el envío' }).click()
    await expect(page.getByText('Resolución registrada. La solicitud quedó cerrada.')).toBeVisible()
    const claves = mundo.resoluciones.map((x) => x.clave)
    expect(claves).toHaveLength(2)
    expect(claves[0], 'el reintento reutiliza la clave de idempotencia').toBe(claves[1])
    expect(mundo.decisiones.filter((d) => d.fase === 'RESOLUCION')).toHaveLength(1)
  })

  test('puerta de entrada: pide el identificador y rechaza uno mal formado', async ({ page }) => {
    await simular(page, mundoNuevo([PROPUESTA]), ['ADMIN_PLATAFORMA'])
    await ingresar(page)
    await page.getByRole('link', { name: /Solicitudes de ingreso/ }).click()
    await expect(page.getByRole('heading', { name: 'Abrir una solicitud' })).toBeVisible()
    await page.getByLabel(/Identificador de la solicitud/).fill('no-es-un-uuid')
    await page.getByRole('button', { name: 'Abrir solicitud' }).click()
    await expect(page.getByText('Ese identificador no tiene el formato de una solicitud.')).toBeVisible()
    await capturar(page, 'entrada-error', 'movil', 'light')
    await page.getByLabel(/Identificador de la solicitud/).fill(SOLICITUD)
    await page.getByRole('button', { name: 'Abrir solicitud' }).click()
    await expect(page).toHaveURL(new RegExp(`/admisiones/${SOLICITUD}$`))
    expect(consola.filter((c) => !/Failed to load resource/.test(c)), `consola: ${consola.join(' | ')}`).toEqual([])
  })
})
