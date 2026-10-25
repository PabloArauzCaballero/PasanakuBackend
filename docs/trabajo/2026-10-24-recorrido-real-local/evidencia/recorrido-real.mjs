// Recorrido del backoffice contra el backend REAL levantado en el contenedor.
// Uso: node recorrido-real.mjs <archivo-de-rutas.json> <carpeta-de-salida> [ancho] [alto]
// Las credenciales salen de variables de entorno (CLAVE_DEV); no se escriben en ningun archivo.
import { createRequire } from 'node:module'
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs'
const require = createRequire(import.meta.url)
const { chromium } = require('playwright')

const [, , archivoRutas, salida, ancho = '1440', alto = '900'] = process.argv
const BASE = process.env.BASE_URL ?? 'http://127.0.0.1:4200'
const rutas = JSON.parse(readFileSync(archivoRutas, 'utf8'))
mkdirSync(salida, { recursive: true })

const navegador = await chromium.launch()
const contexto = await navegador.newContext({ viewport: { width: +ancho, height: +alto } })
const page = await contexto.newPage()

let peticiones = []
let consola = []
page.on('response', (r) => {
  const u = new URL(r.url())
  if (u.pathname.startsWith('/api/v1')) peticiones.push({ metodo: r.request().method(), ruta: u.pathname + (u.search ? '?…' : ''), estado: r.status() })
})
page.on('console', (m) => { if (m.type() === 'error') consola.push(m.text().slice(0, 200)) })
page.on('pageerror', (e) => consola.push('pageerror: ' + String(e.message).slice(0, 200)))

// El HTML de desarrollo no trae la meta del gateway; es la misma configuracion que inyectan los e2e.
await page.route('**/ingreso', async (route) => {
  const r = await route.fetch()
  await route.fulfill({ response: r, body: (await r.text()).replace('</head>', '<meta name="aportaya-gateway" content="/api/v1"></head>') })
}, { times: 1 })

await page.goto(BASE + '/ingreso')
await page.getByLabel('Correo o teléfono').fill(process.env.TELEFONO ?? '71000091')
await page.getByLabel('Contraseña', { exact: true }).fill(process.env.CLAVE_DEV)
await page.getByRole('button', { name: 'Continuar' }).click()
await page.getByLabel('Código de verificación').fill('000000')
await page.waitForURL(/\/tablero$/, { timeout: 30000 })
const login = peticiones.filter((p) => p.ruta.startsWith('/api/v1/sesion'))
console.log('LOGIN OK ->', page.url(), JSON.stringify(login))

async function esperarQuieto(ms = 900, tope = 20000) {
  const t0 = Date.now(); let ultimo = peticiones.length; let desde = Date.now()
  while (Date.now() - t0 < tope) {
    await page.waitForTimeout(150)
    if (peticiones.length !== ultimo) { ultimo = peticiones.length; desde = Date.now() }
    if (Date.now() - desde > ms) break
  }
}

const resultados = []
for (const ruta of rutas) {
  peticiones = []; consola = []
  await page.evaluate((r) => { history.pushState({}, '', r); window.dispatchEvent(new PopStateEvent('popstate')) }, ruta)
  await esperarQuieto()
  const info = await page.evaluate(() => {
    const main = document.querySelector('main') ?? document.body
    const txt = (main.innerText || '').replace(/\s+/g, ' ').trim()
    return {
      url: location.pathname,
      titulo: (document.querySelector('h1')?.textContent ?? '').trim().slice(0, 80),
      carga: !!document.querySelector('[aria-busy="true"], .esqueleto, [class*="skeleton"]'),
      error: /No pudimos|Volver a intentar|Algo salió mal|no se pudo/i.test(txt),
      vacio: /sin resultados|no hay |todavía no|aún no|vacío/i.test(txt),
      filas: document.querySelectorAll('tbody tr, [role="row"]').length,
      texto: txt.slice(0, 160),
      desborda: document.documentElement.scrollWidth > document.documentElement.clientWidth + 1,
    }
  })
  const nombre = ruta.replace(/^\//, '').replace(/[^a-z0-9]+/gi, '-').replace(/-$/, '') || 'raiz'
  await page.screenshot({ path: `${salida}/${nombre}.png` })
  const malas = peticiones.filter((p) => p.estado >= 400)
  resultados.push({ ruta, ...info, redirigida: info.url !== ruta, peticiones: peticiones.length, malas, consola: [...new Set(consola)] })
  console.log(`${ruta} -> ${info.url} | ${peticiones.length} pet. | malas: ${malas.map((m) => m.metodo + ' ' + m.ruta + ' ' + m.estado).join('; ') || 'ninguna'} | consola: ${consola.length}`)
}
writeFileSync(`${salida}/resultados.json`, JSON.stringify(resultados, null, 2))
await navegador.close()
