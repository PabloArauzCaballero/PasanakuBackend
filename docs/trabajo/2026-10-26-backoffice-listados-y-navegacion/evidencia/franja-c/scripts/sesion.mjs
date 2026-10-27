// Doble de sesión copiado de e2e/apoyo/sonda-de-navegacion.mjs: intercepta el 2.º POST /sesiones y sirve un token con permisos.
import { chromium } from '/home/user/wt-c/node_modules/playwright/index.mjs'
import { readFileSync } from 'node:fs'
export const BASE = 'http://127.0.0.1:4303'
export const PERMISOS = readFileSync('/tmp/claude-0/-home-user/bfffe767-c9ff-52f5-8524-aca65b5504e0/scratchpad/permisos.txt', 'utf8').split(/\s+/).filter(Boolean)
function token(p, rol) {
  const b = Buffer.from(JSON.stringify({ permisos: p, rol, sub: 'sonda' })).toString('base64url')
  return `sonda.${b}.firma`
}
export async function abrir({ ancho = 1280, alto = 900, oscuro = false, permisos = PERMISOS, rol = 'OFICIAL_CUMPLIMIENTO' } = {}) {
  const navegador = await chromium.launch({ args: ['--disable-features=LocalNetworkAccessChecks'] })
  const contexto = await navegador.newContext({ viewport: { width: ancho, height: alto }, colorScheme: oscuro ? 'dark' : 'light', acceptDownloads: true })
  const page = await contexto.newPage()
  const errores = []
  page.on('console', (m) => m.type() === 'error' && errores.push({ url: page.url(), tipo: 'consola', texto: m.text().slice(0, 300) }))
  page.on('pageerror', (e) => errores.push({ url: page.url(), tipo: 'excepcion', texto: String(e).slice(0, 300) }))
  const red = []
  page.on('request', (r) => r.url().includes('/api/') && red.push(`${r.method()} ${r.url().replace(BASE, '')}`))
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
  return { navegador, page, errores, red }
}
/** Navega dentro de la SPA sin perder la sesión en memoria. */
export async function ir(page, ruta) {
  await page.evaluate((r) => { history.pushState({}, '', r); dispatchEvent(new PopStateEvent('popstate')) }, ruta)
  await page.waitForFunction((r) => location.pathname === r, ruta)
}
