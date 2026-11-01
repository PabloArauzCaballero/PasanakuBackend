// Apoyo del recorrido de la franja B: DOBLE DE SESIÓN copiado de e2e/apoyo/sonda-de-navegacion.mjs.
import { createRequire } from 'node:module'
import { readFileSync } from 'node:fs'
const require = createRequire('/home/user/wt-b/apps/backoffice/package.json')
export const { chromium } = require('playwright')

export const BASE = 'http://127.0.0.1:4302'
export const SALIDA = '/home/user/wt-b/docs/trabajo/2026-10-26-backoffice-listados-y-navegacion/evidencia/franja-b'
const TODOS = readFileSync('/tmp/claude-0/-home-user/bfffe767-c9ff-52f5-8524-aca65b5504e0/scratchpad/permisos.txt', 'utf8').split(/\s+/).filter(Boolean)

function token(p, rol) {
  const b = Buffer.from(JSON.stringify({ permisos: p, rol, sub: 'sonda-b' })).toString('base64url')
  return `sonda.${b}.firma`
}

/** Abre una página con sesión. `permisos` por omisión: todos. */
export async function entrar(navegador, { permisos = TODOS, rol = 'oficial', ancho = 1280, alto = 900, tema = 'light' } = {}) {
  const contexto = await navegador.newContext({ viewport: { width: ancho, height: alto }, colorScheme: tema, acceptDownloads: true })
  const page = await contexto.newPage()
  const errores = []
  const red = []
  page.on('console', (m) => m.type() === 'error' && errores.push(m.text().slice(0, 300)))
  page.on('pageerror', (e) => errores.push('excepcion: ' + String(e).slice(0, 300)))
  page.on('request', (r) => r.url().includes('/api/v1/') && red.push(`${r.method()} ${r.url().replace(BASE, '')}`))
  await page.route('**/ingreso', async (route) => {
    const r = await route.fetch()
    await route.fulfill({ response: r, body: (await r.text()).replace('</head>', '<meta name="aportaya-gateway" content="/api/v1"></head>') })
  }, { times: 1 })
  await page.route('**/api/v1/sesiones', async (route) => {
    if (!route.request().postDataJSON()?.factor) return route.continue()
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ sesionId: 's', expiraEn: new Date(Date.now() + 3.6e6).toISOString(), requiereFactorAdicional: false, dispositivoConfiable: true, tokenAcceso: token(permisos, rol) }) })
  })
  await page.goto(BASE + '/ingreso')
  await page.getByLabel('Correo o teléfono').fill('71234567')
  await page.getByLabel('Contraseña', { exact: true }).fill('contrasena-e2e')
  await page.getByRole('button', { name: 'Continuar' }).click()
  await page.getByLabel('Código de verificación').fill('123456')
  await page.waitForURL(/\/tablero$/, { timeout: 60000 })
  return { page, contexto, errores, red }
}

/** Navega sin recargar (la sesión vive en memoria). */
export async function ir(page, ruta) {
  await page.evaluate((r) => { history.pushState({}, '', r); dispatchEvent(new PopStateEvent('popstate')) }, ruta)
  await page.waitForURL((u) => u.pathname === ruta.split('?')[0])
}

export function afirmar(cond, msj) {
  if (!cond) throw new Error('FALLO: ' + msj)
  console.log('PASS ' + msj)
}

export async function bajar(page, nombreBoton) {
  const [d] = await Promise.all([page.waitForEvent('download'), page.getByRole('button', { name: nombreBoton }).first().click()])
  const ruta = await d.path()
  return { nombre: d.suggestedFilename(), texto: readFileSync(ruta, 'utf8') }
}
