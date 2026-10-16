import { expect, test, type Page, type Route } from '@playwright/test'
import { iniciarSesionDePrueba, navegarSinRecargar } from './apoyo/sesion-e2e'
import { capturar } from './apoyo/capturas-f2'

/**
 * H12.S1.M3 (estados de red y recuperación) y H12.S1.M4 (teclado y foco) sobre el cobro de una cuenta por cobrar.
 *
 * **`VERIFIED_FUNCTIONAL_ONLY` contra un DOBLE:** el «servidor» es `page.route` (idempotente por `Idempotency-Key`, como pide el
 * contrato de `erp`); no hay `erp` real. Prueba el cliente —clave estable, aviso de «sin confirmar», datos conservados, sin
 * doble envío—, no que el servidor real no duplique.
 */
const CUENTA = {
  cuentaPorCobrarId: 'cxc-1',
  origenTipo: 'GRUPO',
  origenId: 'g-1',
  terceroRazonSocial: 'Tercero de prueba',
  fechaVencimiento: '2026-10-20',
  estado: 'PENDIENTE',
  monto: { monto: '500.00', moneda: 'BOB' },
  cobrado: { monto: '0.00', moneda: 'BOB' },
  saldoPendiente: { monto: '500.00', moneda: 'BOB' },
}
type Mundo = { llamadas: { clave: string | null; cuerpo: unknown }[]; registrados: Map<string, unknown>; modo: 'ok' | 'llega-sin-respuesta' | 'no-llega' | 'cuelga' }
const nuevo = (): Mundo => ({ llamadas: [], registrados: new Map(), modo: 'ok' })

async function simular(page: Page, mundo: Mundo): Promise<void> {
  await page.route('**/api/v1/erp/cuentas-por-cobrar', (r: Route) => r.fulfill({ status: 200, json: [CUENTA] }))
  await page.route('**/api/v1/erp/cuentas-por-cobrar/cxc-1/cobros', async (r: Route) => {
    const clave = r.request().headers()['idempotency-key'] ?? null
    mundo.llamadas.push({ clave, cuerpo: r.request().postDataJSON() })
    if (mundo.modo === 'no-llega') return r.abort('connectionreset')
    if (mundo.modo === 'cuelga') return // la petición queda abierta: nunca hay respuesta
    if (clave && !mundo.registrados.has(clave)) mundo.registrados.set(clave, r.request().postDataJSON())
    if (mundo.modo === 'llega-sin-respuesta') return r.abort('connectionreset')
    return r.fulfill({ status: 200, json: { ...CUENTA, estado: 'COBRADA' } })
  })
}

async function abrirElCobro(page: Page, mundo: Mundo): Promise<void> {
  await simular(page, mundo)
  await iniciarSesionDePrueba(page, ['CONTABILIDAD', 'CONTABILIDAD_ERP_COBRAR'], 'CONTABILIDAD')
  await navegarSinRecargar(page, '/contabilidad/cobros')
  await expect(page.getByRole('heading', { name: 'Cuentas por cobrar', level: 1 })).toBeVisible()
  await page.getByRole('button', { name: 'Registrar el cobro', exact: true }).click()
  await expect(page.getByRole('dialog')).toBeVisible()
}
const escribirMonto = (page: Page, monto: string) => page.getByLabel('Monto cobrado').fill(monto)
const confirmar = (page: Page) => page.getByRole('button', { name: 'Registrar el cobro de Tercero de prueba' }).click()

test.describe('[doble del servidor] cobro: éxito no confirmado, corte, sin conexión y plazo vencido', () => {
  const consola: string[] = []
  test.beforeEach(({ page }) => {
    consola.length = 0
    page.on('pageerror', (e) => consola.push(`pageerror: ${e.message}`))
  })

  test('respuesta perdida: avisa que no se pudo confirmar, conserva el monto, y el reintento reutiliza la clave y no duplica', async ({ page }) => {
    const mundo = nuevo()
    await abrirElCobro(page, mundo)
    await escribirMonto(page, '125,50')
    mundo.modo = 'llega-sin-respuesta'
    await confirmar(page)
    const aviso = page.getByRole('alert').filter({ hasText: 'No pudimos confirmar si se registró' })
    await expect(aviso).toBeVisible()
    await expect(page.getByLabel('Monto cobrado')).toHaveValue('125,50')
    await expect(page.getByRole('dialog')).toBeVisible()
    await capturar(page, 'bo', 'cobro-sin-confirmar', 'movil', 'light')
    await capturar(page, 'bo', 'cobro-sin-confirmar', 'escritorio', 'dark')

    mundo.modo = 'ok'
    await confirmar(page)
    await expect(page.getByRole('dialog')).toHaveCount(0)
    expect(mundo.llamadas).toHaveLength(2)
    expect(mundo.llamadas[0]?.clave).toMatch(/^[0-9a-f-]{36}$/)
    expect(mundo.llamadas[1]?.clave, 'el reintento reutiliza la clave').toBe(mundo.llamadas[0]?.clave)
    expect(mundo.registrados.size, 'el servidor registró UN cobro').toBe(1)
    expect(consola).toEqual([])
  })

  test('el botón no permite doble envío mientras la operación está en curso', async ({ page }) => {
    const mundo = nuevo()
    await abrirElCobro(page, mundo)
    await escribirMonto(page, '10,00')
    mundo.modo = 'cuelga'
    await confirmar(page)
    await expect.poll(() => mundo.llamadas.length).toBe(1)
    // Con la primera en curso el botón queda bloqueado: no hay forma de mandar otra (regla 95.3.4).
    await expect(page.getByRole('button', { name: 'Registrar el cobro de Tercero de prueba' })).toBeDisabled()
    expect(mundo.llamadas, 'con la primera en curso no sale otra').toHaveLength(1)
  })

  test('plazo vencido (30 s sin respuesta): «sin confirmar», no «falló»; el reintento reutiliza la clave', async ({ page }) => {
    await page.clock.install()
    const mundo = nuevo()
    await abrirElCobro(page, mundo)
    await escribirMonto(page, '40,00')
    mundo.modo = 'cuelga'
    await confirmar(page)
    await expect.poll(() => mundo.llamadas.length).toBe(1)
    await page.clock.fastForward(31_000)
    await expect(page.getByRole('alert').filter({ hasText: 'No pudimos confirmar si se registró' })).toBeVisible()
    await expect(page.locator('ap-error-de-envio')).toContainText('tardó demasiado')
    mundo.modo = 'ok'
    await confirmar(page)
    await expect(page.getByRole('dialog')).toHaveCount(0)
    expect(mundo.llamadas[1]?.clave).toBe(mundo.llamadas[0]?.clave)
  })

  test('sin conexión: el envío falla visible, lo escrito se conserva y al volver la red el reintento sale con la misma clave', async ({ page }) => {
    const mundo = nuevo()
    await abrirElCobro(page, mundo)
    await escribirMonto(page, '75,00')
    mundo.modo = 'llega-sin-respuesta'
    await page.context().setOffline(true)
    await confirmar(page)
    await expect(page.getByRole('alert').filter({ hasText: 'No pudimos confirmar si se registró' })).toBeVisible()
    await expect(page.getByLabel('Monto cobrado')).toHaveValue('75,00')
    await capturar(page, 'bo', 'cobro-sin-conexion', 'tablet', 'dark')
    await page.context().setOffline(false)
    mundo.modo = 'ok'
    await confirmar(page)
    await expect(page.getByRole('dialog')).toHaveCount(0)
    expect(mundo.registrados.size).toBe(1)
  })

  test('rechazo del servidor (422): dice que NO se completó, con el código de seguimiento, y no promete nada sobre repetir', async ({ page }) => {
    const mundo = nuevo()
    await abrirElCobro(page, mundo)
    await page.unroute('**/api/v1/erp/cuentas-por-cobrar/cxc-1/cobros')
    await page.route('**/api/v1/erp/cuentas-por-cobrar/cxc-1/cobros', (r) =>
      r.fulfill({ status: 422, json: { codigo: 'AP-CU104-02', mensaje: 'tecnico', trazaId: 'traza-f2-422' } }),
    )
    await escribirMonto(page, '90,00')
    await confirmar(page)
    const aviso = page.getByRole('alert').filter({ hasText: 'No se pudo completar' })
    await expect(aviso).toBeVisible()
    await expect(aviso).toContainText('Código de seguimiento: traza-f2-422')
    await expect(aviso).not.toContainText('tecnico')
    await expect(aviso).not.toContainText('no se duplica')
    await capturar(page, 'bo', 'cobro-rechazado', 'movil', 'dark')
  })
})

test.describe('teclado y foco en los flujos críticos del backoffice', () => {
  test('cobro: se abre con Enter, el foco entra y queda atrapado en el diálogo, Escape lo cierra y el foco vuelve al botón', async ({ page }) => {
    const mundo = nuevo()
    await simular(page, mundo)
    await iniciarSesionDePrueba(page, ['CONTABILIDAD', 'CONTABILIDAD_ERP_COBRAR'], 'CONTABILIDAD')
    await navegarSinRecargar(page, '/contabilidad/cobros')
    const boton = page.getByRole('button', { name: 'Registrar el cobro', exact: true })
    await expect(boton).toBeVisible()
    await boton.focus()
    await page.keyboard.press('Enter')
    const dialogo = page.getByRole('dialog')
    await expect(dialogo).toBeVisible()
    // Dentro del diálogo, o en <body> (el modal nativo entrega el foco al navegador tras el último control): nunca en la página de atrás, que es inerte.
    const dentro = () => page.evaluate(() => !!document.activeElement?.closest('dialog'))
    const enLaPaginaDeAtras = () => page.evaluate(() => !!document.activeElement && document.activeElement !== document.body && !document.activeElement.closest('dialog'))
    expect(await dentro(), 'foco inicial dentro del diálogo').toBe(true)
    for (let i = 0; i < 9; i++) {
      await page.keyboard.press('Tab')
      expect(await enLaPaginaDeAtras(), `Tab ${i + 1}: el foco no llega a la página de atrás`).toBe(false)
    }
    await page.keyboard.press('Shift+Tab')
    expect(await dentro(), 'Shift+Tab desde fuera vuelve a entrar al diálogo').toBe(true)
    await page.keyboard.press('Escape')
    await expect(dialogo).toHaveCount(0)
    await expect(boton, 'el foco vuelve al elemento que abrió el diálogo').toBeFocused()
  })

  test('ingreso: se completa y envía solo con teclado, el error de credenciales queda asociado al campo y el foco no se pierde', async ({ page }) => {
    await page.route('**/ingreso', async (route) => {
      const r = await route.fetch()
      await route.fulfill({ response: r, body: (await r.text()).replace('</head>', '<meta name="aportaya-gateway" content="/api/v1"></head>') })
    }, { times: 1 })
    await page.goto('/ingreso')
    await page.getByLabel('Teléfono').focus()
    await page.keyboard.type('71234567')
    await page.keyboard.press('Tab')
    await expect(page.getByLabel('Contraseña', { exact: true })).toBeFocused()
    await page.keyboard.type('contrasena-e2e')
    await page.route('**/api/v1/sesiones', (r) => r.fulfill({ status: 401, json: { codigo: 'AP-CU04-01', mensaje: 'tecnico', trazaId: 'traza-f2-401' } }))
    await page.keyboard.press('Enter')
    await expect(page.getByText('El teléfono o la contraseña no coinciden.')).toBeVisible()
    const foco = await page.evaluate(() => document.activeElement?.tagName ?? 'NINGUNO')
    expect(foco, 'el foco no se pierde en <body> tras el error').not.toBe('BODY')
    await capturar(page, 'bo', 'ingreso-error', 'movil', 'light')
  })
})
