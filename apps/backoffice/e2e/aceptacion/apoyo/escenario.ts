import { test as base, expect, type BrowserContext, type Page } from '@playwright/test'
import { prepararArranque } from '../../apoyo/arranque-e2e'
import { ClienteApi, type Cuenta } from './api'
import { BASE_API, BASE_BO, BASE_WEB, MODO } from './entorno'
import { Observador } from './observador'
import { sembrarReal } from './siembra-real'

/** Todo lo que un escenario necesita: un grupo, su organizador y las cuentas de los dos protagonistas (+ intrusos para los límites). */
export type Mundo = {
  grupoId: string
  organizador: Cuenta
  /** Usuario A: la persona invitada (participante). */
  participante: Cuenta
  /** Otra persona que NO fue invitada. */
  participante2: Cuenta
  /** Usuario B: operador de backoffice con rol ADMIN_PLATAFORMA. */
  operador: Cuenta
  /** Un segundo operador de backoffice (para la carrera de dos pestañas de B). */
  operador2: Cuenta
  /** Operador sin permiso sobre admisiones. */
  sinPermiso: Cuenta
}

async function sembrar(): Promise<Mundo> {
  if (MODO === 'real') return sembrarReal()
  const r = await fetch(`${BASE_API}/__doble/sembrar`, { method: 'POST' })
  if (!r.ok) throw new Error(`El doble no respondió a la siembra (${r.status}). ¿Está levantado en ${BASE_API}?`)
  return (await r.json()) as Mundo
}

export type Actor = { contexto: BrowserContext; page: Page; obs: Observador }
type Fixtures = {
  mundo: Mundo
  /** A: contexto de navegador aislado contra el sitio web. */
  a: Actor
  /** B: otro contexto, aislado del de A, contra el backoffice. */
  b: Actor
  api: { organizador: ClienteApi; a: ClienteApi; a2: ClienteApi; b: ClienteApi; b2: ClienteApi; sinPermiso: ClienteApi }
}

/** Un solo worker, un solo Chromium (el `browser` del worker) y DOS `newContext` aislados: nunca dos navegadores. */
export const test = base.extend<Fixtures>({
  // eslint-disable-next-line no-empty-pattern
  mundo: async ({}, usar) => {
    await usar(await sembrar())
  },
  a: async ({ browser }, usar) => {
    const contexto = await browser.newContext({ baseURL: BASE_WEB, viewport: { width: 1280, height: 900 }, locale: 'es-BO' })
    const page = await contexto.newPage()
    await usar({ contexto, page, obs: new Observador(page, 'A-web') })
    await contexto.close()
  },
  b: async ({ browser }, usar) => {
    const contexto = await browser.newContext({ baseURL: BASE_BO, viewport: { width: 1280, height: 900 }, locale: 'es-BO' })
    const page = await contexto.newPage()
    await prepararArranque(page)
    await usar({ contexto, page, obs: new Observador(page, 'B-backoffice') })
    await contexto.close()
  },
  api: async ({ mundo }, usar) => {
    const todos = {
      organizador: await ClienteApi.crear(mundo.organizador),
      a: await ClienteApi.crear(mundo.participante),
      a2: await ClienteApi.crear(mundo.participante2),
      b: await ClienteApi.crear(mundo.operador),
      b2: await ClienteApi.crear(mundo.operador2),
      sinPermiso: await ClienteApi.crear(mundo.sinPermiso),
    }
    await Promise.all(Object.values(todos).map((c) => c.ingresar()))
    await usar(todos)
    await Promise.all(Object.values(todos).map((c) => c.cerrar()))
  },
})
export { expect }

// ── Acciones de UI del backoffice (B) ─────────────────────────────────────────────────────────────

/** Ingreso de un operador por la pantalla real: teléfono + contraseña y, si la API lo pide, el segundo factor. */
export async function ingresarBackoffice(page: Page, cuenta: Cuenta, destino = '/ingreso'): Promise<void> {
  await page.goto(destino.startsWith('/ingreso') ? destino : `/ingreso?volverA=${encodeURIComponent(destino)}`)
  await page.getByLabel(/Teléfono/).fill(cuenta.telefono.replace('+591', ''))
  await page.getByLabel(/Contraseña/).fill(cuenta.clave)
  await page.getByRole('button', { name: 'Continuar' }).click()
  if (cuenta.factor) {
    const codigo = typeof cuenta.factor === 'function' ? cuenta.factor() : cuenta.factor
    // `ap-campo-otp` envía sola al completar los 6 dígitos.
    await page.getByLabel('Código de verificación').fill(codigo)
  }
  await page.waitForURL(/\/(tablero|admisiones|operacion)/)
}

/** Navegación dentro de la app, sin recargar (la sesión del backoffice vive solo en memoria, a propósito). */
export async function irA(page: Page, ruta: string): Promise<void> {
  await page.evaluate((r) => {
    window.history.pushState({}, '', r)
    window.dispatchEvent(new PopStateEvent('popstate'))
  }, ruta)
}
