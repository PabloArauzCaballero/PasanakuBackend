// Recorrido en navegador de la franja E (contabilidad + configuración). DOBLE DE SESIÓN (como la sonda) y DOBLE DE LISTADOS
// (page.route con 25 filas sintéticas por cursor: Prism siempre da 1 fila y cursor infinito). Las mutaciones (POST) van al
// simulado real (Prism, sin estado): se verifica la request que sale y que la UI reacciona a su respuesta.
// Uso: node recorrido.mjs [base] [dirCapturas]
import { createRequire } from 'node:module'
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs'
import { RECURSOS, pagina } from './datos.mjs'

const require = createRequire('/home/user/wt-e/package.json')
const { chromium } = require('playwright')
const base = process.argv[2] ?? 'http://127.0.0.1:4305'
const capturas = './rapidas'
const rutas = process.argv.slice(3)
mkdirSync(capturas, { recursive: true })
const permisos = readFileSync('/tmp/claude-0/-home-user/bfffe767-c9ff-52f5-8524-aca65b5504e0/scratchpad/permisos.txt', 'utf8').split(/\s+/).filter(Boolean)
const token = (p) => `sonda.${Buffer.from(JSON.stringify({ permisos: p, rol: 'oficial', sub: 'franja-e' })).toString('base64url')}.firma`

const navegador = await chromium.launch({ args: ['--disable-features=LocalNetworkAccessChecks'] })
const contexto = await navegador.newContext({ viewport: { width: 1280, height: 900 }, acceptDownloads: true })
const page = await contexto.newPage()
const resultados = []
const errores = []
const pedidos = []
let esperados = 0 // 4xx/5xx que provoca el propio recorrido
page.on('console', (m) => m.type() === 'error' && errores.push({ url: page.url(), tipo: 'consola', texto: m.text().slice(0, 240) }))
page.on('pageerror', (e) => errores.push({ url: page.url(), tipo: 'excepcion', texto: String(e).slice(0, 240) }))
page.on('request', (r) => r.url().includes('/api/v1/') && pedidos.push({ metodo: r.method(), url: r.url().replace(base, ''), clave: r.headers()['idempotency-key'] ?? null, cuerpo: r.postData() }))
page.on('response', (r) => r.status() >= 400 && r.url().includes('/api/') && errores.push({ url: page.url(), tipo: 'red', texto: `${r.status()} ${r.request().method()} ${r.url().replace(base, '')}` }))

// --- doble de los listados ---
const modo = {}
for (const [nombre, rec] of Object.entries(RECURSOS)) {
  modo[nombre] = 'ok'
  await page.route((u) => u.pathname === rec.ruta, async (route) => {
    if (route.request().method() !== 'GET') return route.continue()
    const m = modo[nombre]
    const url = new URL(route.request().url())
    if (m === '500') return route.fulfill({ status: 500, contentType: 'application/json', body: JSON.stringify({ codigo: 'AP-GEN-500', mensaje: 'NullPointerException interno', trazaId: 'tz-franja-e-500' }) })
    if (m === '403') return route.fulfill({ status: 403, contentType: 'application/json', body: JSON.stringify({ codigo: 'AP-SEG-01', mensaje: 'denegado interno', trazaId: 'tz-franja-e-403' }) })
    const total = m === 'vacio' ? 0 : m === 'una' ? 1 : 25
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(pagina(rec, url, { total, campoEstado: rec.campoEstado ?? 'estado' })) })
  })
}

// --- ingreso (doble de sesión, como e2e/apoyo/sonda-de-navegacion.mjs) ---
await page.route('**/ingreso', async (route) => {
  const r = await route.fetch()
  await route.fulfill({ response: r, body: (await r.text()).replace('</head>', '<meta name="aportaya-gateway" content="/api/v1"></head>') })
}, { times: 1 })
await page.route('**/api/v1/sesiones', async (route) => {
  if (!route.request().postDataJSON()?.factor) return route.continue()
  await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ sesionId: 's', expiraEn: new Date(Date.now() + 3.6e6).toISOString(), requiereFactorAdicional: false, dispositivoConfiable: true, tokenAcceso: token(permisos) }) })
})
await page.goto(base + '/ingreso')
await page.getByLabel('Correo o teléfono').fill('71234567')
await page.getByLabel('Contraseña', { exact: true }).fill('contrasena-e2e')
await page.getByRole('button', { name: 'Continuar' }).click()
await page.getByLabel('Código de verificación').fill('123456').catch(async (e) => { await page.screenshot({ path: capturas + '/FALLO-ingreso.png' }); throw e })
await page.waitForURL(/\/tablero$/, { timeout: 60000 })


for (const r of rutas) {
  const [ruta, ancho, tema, pestana] = r.split('|')
  const titulo = await page.title()
  await page.evaluate((x) => { history.pushState({}, '', x); dispatchEvent(new PopStateEvent('popstate')) }, ruta)
  await page.waitForFunction((t) => document.title !== t, titulo).catch(() => {})
  if (pestana) await page.getByRole('tab', { name: pestana }).click()
  await page.setViewportSize({ width: Number(ancho), height: 900 })
  await page.emulateMedia({ colorScheme: tema === 'oscuro' ? 'dark' : 'light' })
  await page.waitForFunction(() => document.querySelectorAll('main#contenido tbody tr, main#contenido .mes, main#contenido .accesos a, main#contenido ap-estado-vacio').length > 0, null, { timeout: 15000 }).catch(() => {})
  await page.waitForFunction(() => document.getAnimations().every((a) => a.playState !== 'running'), null, { timeout: 5000 }).catch(() => {})
  const nombre = ruta.replace(/[^a-z]+/g, '-') + '-' + ancho + '-' + (tema || 'claro') + (pestana ? '-' + pestana.replace(/[^a-zA-Z]+/g, '') : '')
  await page.screenshot({ path: `${capturas}/${nombre}.png`, fullPage: true })
  console.log(nombre, await page.evaluate(() => document.documentElement.scrollWidth))
  await page.evaluate(() => { history.pushState({}, '', '/tablero'); dispatchEvent(new PopStateEvent('popstate')) })
  await page.waitForFunction(() => document.title.includes('Tablero') || document.title.includes('Inicio')).catch(() => {})
}
await navegador.close()
