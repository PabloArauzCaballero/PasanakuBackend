// Medición de latencia del menú (DOBLE DE SESIÓN, igual que `sonda-de-navegacion.mjs`): entra, estrangula la red y la
// CPU por CDP y mide clic → vista pintada en cada entrada del menú lateral. No es una prueba: es un diagnóstico para
// comparar antes y después de un cambio en el shell.
//
// Uso: `ROL=PLATAFORMA PERMISOS_ARCHIVO=permisos.txt node e2e/apoyo/medir-latencia-del-menu.mjs [baseURL] [salida.json]`
// Variables: LATENCIA_MS (150), CPU (4), REPOSO_MS (1500: cuánto tiempo sin pedidos de red se considera «reposo»
// antes de empezar a medir; simula a quien mira el tablero antes de hacer clic). SIN_REPOSO=1 hace clic apenas pinta
// cada vista, sin esperar a que la red quede quieta: el peor caso para una precarga.
import { chromium } from 'playwright'
import { readFileSync, writeFileSync } from 'node:fs'

const base = process.argv[2] ?? 'http://127.0.0.1:4300'
const salida = process.argv[3] ?? 'latencia.json'
const rol = process.env.ROL ?? 'PLATAFORMA'
const latencia = Number(process.env.LATENCIA_MS ?? 150)
const cpu = Number(process.env.CPU ?? 4)
const reposo = Number(process.env.REPOSO_MS ?? 1500)
const permisos = (process.env.PERMISOS_ARCHIVO ? readFileSync(process.env.PERMISOS_ARCHIVO, 'utf8') : '').split(/\s+/).filter(Boolean)

function token(p) {
  const b = Buffer.from(JSON.stringify({ permisos: p, rol, sub: 'sonda' })).toString('base64url')
  return `sonda.${b}.firma`
}

const navegador = await chromium.launch({ args: ['--disable-features=LocalNetworkAccessChecks'] })
const page = await navegador.newPage({ viewport: { width: 1280, height: 900 } })
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
await page.getByLabel('Código de verificación').fill('123456')
await page.waitForURL(/\/tablero$/, { timeout: 30000 })

const cdp = await page.context().newCDPSession(page)
await cdp.send('Network.enable')
await cdp.send('Network.emulateNetworkConditions', { offline: false, latency: latencia, downloadThroughput: (1.6 * 1024 * 1024) / 8, uploadThroughput: (750 * 1024) / 8 })
await cdp.send('Emulation.setCPUThrottlingRate', { rate: cpu })

/** Espera a que la red quede quieta `reposo` ms seguidos: una condición sobre la red, no una espera fija. */
async function esperarReposo() {
  if (process.env.SIN_REPOSO) return
  await page.waitForFunction(
    (ms) => {
      const w = window
      const n = performance.getEntriesByType('resource').length
      if (w.__n !== n) { w.__n = n; w.__t = performance.now(); return false }
      return performance.now() - w.__t >= ms
    },
    reposo,
    { timeout: 60000, polling: 100 },
  )
}

const chunksAntesDeMedir = await page.evaluate(() => performance.getEntriesByType('resource').filter((r) => r.name.endsWith('.js')).length)
await esperarReposo()
const chunksEnReposo = await page.evaluate(() => performance.getEntriesByType('resource').filter((r) => r.name.endsWith('.js')).length)

const medidas = []
const enlaces = await page.locator('ap-menu-lateral a').evaluateAll((as) => as.map((a) => ({ texto: a.textContent.trim(), href: a.getAttribute('href') })))
for (const e of enlaces) {
  const antes = page.url()
  const pedidos = await page.evaluate(() => performance.getEntriesByType('resource').length)
  const t0 = Date.now()
  await page.locator(`ap-menu-lateral a[href="${e.href}"]`).first().click()
  let ok = true
  try {
    if (!antes.endsWith(e.href)) await page.waitForFunction((a) => location.href !== a, antes, { timeout: 15000 })
    // «Pintó»: el outlet tiene texto que no es un esqueleto ni un «Cargando».
    await page.waitForFunction(() => {
      const m = document.querySelector('main#contenido')
      if (!m) return false
      const t = m.innerText
      return t.length > 40 && !/Cargando/.test(t)
    }, null, { timeout: 15000, polling: 16 })
  } catch {
    ok = false
  }
  const ms = Date.now() - t0
  const js = await page.evaluate((desde) => performance.getEntriesByType('resource').slice(desde).filter((r) => r.name.endsWith('.js')).length, pedidos)
  medidas.push({ entrada: e.texto, destino: e.href, url: page.url().replace(base, ''), ms, chunksPedidosAlNavegar: js, pinto: ok })
  await esperarReposo()
}
writeFileSync(salida, JSON.stringify({ rol, sinReposo: Boolean(process.env.SIN_REPOSO), latencia, cpu, chunksAntesDeMedir, chunksEnReposo, medidas }, null, 1))
console.log(JSON.stringify({ entradas: medidas.length, mediana: medidas.map((m) => m.ms).sort((a, b) => a - b)[Math.floor(medidas.length / 2)], max: Math.max(...medidas.map((m) => m.ms)) }))
await navegador.close()
