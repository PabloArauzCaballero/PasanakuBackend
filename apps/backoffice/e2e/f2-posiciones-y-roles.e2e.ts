import { expect, test, type Page } from '@playwright/test'
import { readFileSync } from 'node:fs'
import path from 'node:path'
import { iniciarSesionDePrueba, navegarSinRecargar } from './apoyo/sesion-e2e'
import { capturar, todas } from './apoyo/capturas-f2'

/**
 * H12.S1.M2 (saldos y posiciones) y H12.S1.M1 (recorrido por roles) en el navegador.
 *
 * **Qué es esto — `VERIFIED_FUNCTIONAL_ONLY` contra Prism/doble, NO integración real:**
 *  - la sesión y el saldo con `comprometidoEnPozos` se sirven con `page.route`: el contrato publicado de
 *    `nucleo-financiero` NO trae ese campo (es el pedido del carril D, sin productor), así que la vista de posiciones
 *    separadas se prueba contra un DOBLE del contrato pedido; el caso «sin separar» usa el ejemplo real de Prism;
 *  - los permisos de cada rol salen del catálogo versionado (`sql/60_semillas/10-roles-y-permisos.sql`), pero el token es
 *    de prueba: no se prueba `identidad` ni que el servidor niegue lo que el cliente oculta.
 */
const RAIZ = path.resolve(__dirname, '../../..')
const SEMILLA = readFileSync(path.join(RAIZ, 'sql/60_semillas/10-roles-y-permisos.sql'), 'utf8')
const CONCEDIDOS = new Map<string, string[]>()
for (const m of SEMILLA.matchAll(/FROM rol WHERE codigo = '([A-Z_]+)'\)\s*,\s*\(SELECT id FROM permiso WHERE codigo = '([A-Z_]+)'\)/g)) {
  CONCEDIDOS.set(m[1]!, [...(CONCEDIDOS.get(m[1]!) ?? []), m[2]!])
}
/** El token de un rol: el código del rol más los permisos que el catálogo le concede. */
const permisosDe = (rol: string): string[] => [rol, ...(CONCEDIDOS.get(rol) ?? [])]

const CUENTA = '11111111-1111-4111-8111-111111111111'
const URL_SALDO = `**/api/v1/billetera/${CUENTA}/saldo`
const bs = (monto: string, moneda = 'BOB') => ({ monto, moneda })
const cuerpo = (disponible: string, retenido: string, comprometido?: string) => ({
  cuentaId: CUENTA,
  titularId: '22222222-2222-4222-8222-222222222222',
  disponible: bs(disponible),
  retenido: bs(retenido),
  ...(comprometido ? { comprometidoEnPozos: bs(comprometido) } : {}),
  alCorteDe: '2026-09-09T14:30:00Z',
})

async function verBilletera(page: Page): Promise<void> {
  await navegarSinRecargar(page, `/operacion/billetera/${CUENTA}`)
  await expect(page.getByRole('heading', { name: 'Billetera del titular' })).toBeVisible()
}

test.describe('[doble del contrato pedido] saldos y posiciones en el backoffice', () => {
  const consola: string[] = []
  test.beforeEach(({ page }) => {
    consola.length = 0
    page.on('console', (m) => m.type() === 'error' && consola.push(m.text()))
    page.on('pageerror', (e) => consola.push(`pageerror: ${e.message}`))
  })
  const sinErroresDeConsola = () => expect(consola.filter((c) => !/Failed to load resource/.test(c)), `consola: ${consola.join(' | ')}`).toEqual([])

  test('posiciones separadas: gastable, comprometido en pozos y retenido, con cifras que reconcilian', async ({ page }) => {
    await page.route(URL_SALDO, (r) => r.fulfill({ status: 200, json: cuerpo('1240.00', '60.00', '400.00') }))
    await iniciarSesionDePrueba(page, permisosDe('SOPORTE'), 'SOPORTE')
    await verBilletera(page)
    const filas = page.locator('.fila')
    await expect(filas).toHaveCount(4)
    await expect(filas.nth(0)).toContainText('Bs 840,00')
    await expect(filas.nth(0)).toContainText('Gastable')
    await expect(filas.nth(1)).toContainText('Bs 400,00')
    await expect(filas.nth(1)).toContainText('No gastable')
    await expect(filas.nth(2)).toContainText('Bs 60,00')
    await expect(filas.nth(3)).toContainText('Bs 1.300,00')
    await expect(page.getByText('Lo invertido', { exact: false })).toBeVisible()
    await todas(page, 'bo', 'billetera-posiciones', )
    sinErroresDeConsola()
  })

  test('sin separar: con el ejemplo real del contrato (sin comprometidoEnPozos) NO se afirma ninguna cifra gastable', async ({ page }) => {
    await iniciarSesionDePrueba(page, permisosDe('SOPORTE'), 'SOPORTE')
    await verBilletera(page)
    await expect(page.getByText('Disponible sin separar')).toBeVisible()
    await expect(page.getByText('Gastable por el titular ahora')).toHaveCount(0)
    await capturar(page, 'bo', 'billetera-sin-separar', 'movil', 'light')
    await capturar(page, 'bo', 'billetera-sin-separar', 'movil', 'dark')
    sinErroresDeConsola()
  })

  test('comprometido mayor que el disponible: se acota, lo gastable queda en cero y se avisa', async ({ page }) => {
    await page.route(URL_SALDO, (r) => r.fulfill({ status: 200, json: cuerpo('100.00', '0.00', '250.00') }))
    await iniciarSesionDePrueba(page, permisosDe('SOPORTE'), 'SOPORTE')
    await verBilletera(page)
    await expect(page.getByRole('status').filter({ hasText: 'revisar la conciliación' })).toBeVisible()
    await capturar(page, 'bo', 'billetera-excede', 'tablet', 'dark')
    await capturar(page, 'bo', 'billetera-excede', 'movil', 'light')
  })

  test('saldo incoherente (monedas distintas): no se muestra ninguna cifra', async ({ page }) => {
    await page.route(URL_SALDO, (r) => r.fulfill({ status: 200, json: { ...cuerpo('10.00', '1.00', '1.00'), retenido: bs('1.00', 'USD') } }))
    await iniciarSesionDePrueba(page, permisosDe('SOPORTE'), 'SOPORTE')
    await verBilletera(page)
    await expect(page.getByRole('alert').filter({ hasText: 'No mostramos este saldo' })).toBeVisible()
    await expect(page.locator('ap-monto')).toHaveCount(0)
    await capturar(page, 'bo', 'billetera-incoherente', 'movil', 'light')
  })

  test('cuenta en cero, error del servidor y cargando', async ({ page }) => {
    await page.route(URL_SALDO, (r) => r.fulfill({ status: 200, json: cuerpo('0.00', '0.00', '0.00') }))
    await iniciarSesionDePrueba(page, permisosDe('SOPORTE'), 'SOPORTE')
    await verBilletera(page)
    await expect(page.getByText('La cuenta está en cero')).toBeVisible()
    await capturar(page, 'bo', 'billetera-vacio', 'escritorio', 'light')

    await page.unroute(URL_SALDO)
    await page.route(URL_SALDO, (r) => r.fulfill({ status: 503, json: { codigo: 'AP-GW-503', trazaId: 'traza-f2-503' } }))
    await navegarSinRecargar(page, '/tablero')
    await verBilletera(page)
    await expect(page.getByText('Código de seguimiento: traza-f2-503')).toBeVisible()
    await capturar(page, 'bo', 'billetera-error', 'movil', 'dark')

    await page.unroute(URL_SALDO)
    await page.route(URL_SALDO, async (r) => {
      await new Promise((ok) => setTimeout(ok, 2500))
      await r.fulfill({ status: 200, json: cuerpo('1.00', '0.00', '0.00') })
    })
    await navegarSinRecargar(page, '/tablero')
    await navegarSinRecargar(page, `/operacion/billetera/${CUENTA}`)
    await expect(page.getByRole('status', { name: 'Cargando el saldo de la cuenta' })).toBeVisible()
    await capturar(page, 'bo', 'billetera-cargando', 'escritorio', 'dark')
  })
})

/** Qué ve cada rol del catálogo y a qué NO puede entrar aunque escriba la dirección. */
const SECCIONES = ['Operación', 'Cumplimiento', 'Admisiones', 'Sistemas', 'Contabilidad', 'Publicidad'] as const
const ROLES: { rol: string; ve: (typeof SECCIONES)[number][]; prohibida: string; billeteraDeTerceros: boolean }[] = [
  { rol: 'PARTICIPANTE', ve: [], prohibida: '/operacion/estado', billeteraDeTerceros: false },
  { rol: 'SOPORTE', ve: ['Operación'], prohibida: '/admisiones', billeteraDeTerceros: true },
  // F4: el catálogo sembrado le concede RECLAMO_VER y SOLICITUD_INGRESO_VER/RESOLVER, que piden operacion/reclamos y operacion/solicitudes-escaladas:
  // por eso ve «Operación». Contabilidad sigue ajena (no tiene ningún CONTABILIDAD_ERP_*).
  { rol: 'ADMIN_PLATAFORMA', ve: ['Operación', 'Cumplimiento', 'Admisiones', 'Sistemas'], prohibida: '/contabilidad', billeteraDeTerceros: false },
  { rol: 'CONTABILIDAD', ve: ['Operación', 'Contabilidad'], prohibida: '/cumplimiento', billeteraDeTerceros: true },
]

test.describe('[token de prueba, permisos del catálogo] recorrido por roles sin acciones ajenas', () => {
  for (const { rol, ve, prohibida, billeteraDeTerceros } of ROLES) {
    test(`${rol}: el menú ofrece solo ${ve.length ? ve.join(', ') : 'Inicio y Ayuda'}; lo ajeno no monta ni escribiendo la dirección`, async ({ page }) => {
      const consola: string[] = []
      page.on('pageerror', (e) => consola.push(e.message))
      await iniciarSesionDePrueba(page, permisosDe(rol), rol)
      const menu = page.getByRole('navigation', { name: 'Secciones del backoffice' })
      await expect(menu.getByRole('link', { name: 'Inicio', exact: true })).toBeVisible()
      await expect(menu.getByRole('link', { name: 'Ayuda' })).toBeVisible()
      for (const s of SECCIONES) await expect(menu.getByRole('link', { name: s, exact: true })).toHaveCount(ve.includes(s) ? 1 : 0)

      await navegarSinRecargar(page, prohibida)
      await expect(page).not.toHaveURL(new RegExp(`${prohibida.replace('/', '\\/')}$`))
      await expect(page.getByRole('button', { name: /Resolver solicitud|Cobrar|Responder/ })).toHaveCount(0)

      await page.route(URL_SALDO, (r) => r.fulfill({ status: 200, json: cuerpo('10.00', '0.00', '0.00') }))
      await navegarSinRecargar(page, `/operacion/billetera/${CUENTA}`)
      if (billeteraDeTerceros) await expect(page.getByRole('heading', { name: 'Billetera del titular' })).toBeVisible()
      else {
        await expect(page.getByRole('heading', { name: 'Billetera del titular' })).toHaveCount(0)
        await expect(page).not.toHaveURL(/\/billetera\//)
      }
      await capturar(page, 'bo', `rol-${rol.toLowerCase()}`, 'movil', 'light')
      expect(consola).toEqual([])
    })
  }

  test('una ruta ajena no deja ninguna acción ni ningún código interno de permiso a la vista: vuelve al tablero', async ({ page }) => {
    await iniciarSesionDePrueba(page, permisosDe('SOPORTE'), 'SOPORTE')
    await navegarSinRecargar(page, '/admisiones/9f1c2b7e-3a54-4d0e-8a61-5b7d2c9e4f10')
    await expect(page).toHaveURL(/\/tablero$/)
    await expect(page.getByRole('heading', { name: 'Tablero' })).toBeVisible()
    await expect(page.locator('body')).not.toContainText('ver:')
    await expect(page.getByRole('button', { name: 'Resolver solicitud' })).toHaveCount(0)
    await capturar(page, 'bo', 'ruta-ajena-vuelve-al-tablero', 'movil', 'dark')
  })
})
