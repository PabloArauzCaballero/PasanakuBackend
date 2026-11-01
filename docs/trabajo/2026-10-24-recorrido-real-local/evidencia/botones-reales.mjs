// Pulsa los controles no destructivos de cada pantalla del backoffice contra el backend REAL.
// Uso: node botones-reales.mjs <rutas.json> <salida.json> [ancho] [alto]
// No pulsa lo que ejecuta una accion irreversible o de confirmacion (ver SALTAR); esas se prueban
// aparte, una por una, con su verificacion UI -> peticion -> respuesta -> persistencia -> recarga -> UI.
import { createRequire } from 'node:module'
import { readFileSync, writeFileSync } from 'node:fs'
const { chromium } = createRequire(import.meta.url)('playwright')
const [, , archivoRutas, salida, ancho = '1440', alto = '900'] = process.argv
const BASE = process.env.BASE_URL ?? 'http://localhost:4200'
const rutas = JSON.parse(readFileSync(archivoRutas, 'utf8'))
const SALTAR = /cerrar el per[ií]odo|cerrar sesi[oó]n|^salir$|eliminar|borrar|anular|revocar|bloquear|desbloquear|aprobar|rechazar|confirmar|guardar|^crear|enviar|reversar|ejecutar|abrir ejercicio|descartar|publicar|activar|desactivar|retirar|pagar|liquidar|registrar|emitir|importar/i

const navegador = await chromium.launch()
const page = await (await navegador.newContext({ viewport: { width: +ancho, height: +alto } })).newPage()
let peticiones = [], consola = []
page.on('response', (r) => { const u = new URL(r.url()); if (u.pathname.startsWith('/api/v1')) peticiones.push({ m: r.request().method(), r: u.pathname.replace('/api/v1', ''), e: r.status() }) })
page.on('console', (m) => { if (m.type() === 'error') consola.push(m.text().slice(0, 160)) })
page.on('pageerror', (e) => consola.push('pageerror: ' + String(e.message).slice(0, 160)))
await page.route('**/ingreso', async (route) => { const r = await route.fetch(); await route.fulfill({ response: r, body: (await r.text()).replace('</head>', '<meta name="aportaya-gateway" content="/api/v1"></head>') }) }, { times: 1 })
await page.goto(BASE + '/ingreso')
await page.getByLabel('Correo o teléfono').fill(process.env.TELEFONO ?? '71000091')
await page.getByLabel('Contraseña', { exact: true }).fill(process.env.CLAVE_DEV)
await page.getByRole('button', { name: 'Continuar' }).click()
await page.getByLabel('Código de verificación').fill('000000')
await page.waitForURL(/\/tablero$/, { timeout: 30000 })

const quieto = async (ms = 700, tope = 8000) => { const t0 = Date.now(); let n = peticiones.length, d = Date.now(); while (Date.now() - t0 < tope) { await page.waitForTimeout(120); if (peticiones.length !== n) { n = peticiones.length; d = Date.now() } if (Date.now() - d > ms) break } }
const ir = async (r) => { await page.evaluate((x) => { history.pushState({}, '', x); window.dispatchEvent(new PopStateEvent('popstate')) }, r); await quieto() }
const sel = 'main button:visible, main a:visible, main [role="tab"]:visible, main [role="switch"]:visible, main select:visible, main summary:visible'
const informe = []
for (const ruta of rutas) {
  await ir(ruta)
  const final = new URL(page.url()).pathname
  const fila = { ruta, final, controles: 0, pulsados: 0, saltados: [], hallazgos: [] }
  if (final !== ruta && !['/operacion', '/cumplimiento', '/sistemas', '/publicidad'].includes(ruta)) { fila.redirigida = true; informe.push(fila); continue }
  const total = await page.locator(sel).count(); fila.controles = total
  for (let i = 0; i < Math.min(total, 40); i++) {
    const el = page.locator(sel).nth(i)
    let nombre = ''
    try { nombre = ((await el.getAttribute('aria-label')) || (await el.innerText({ timeout: 1500 })) || (await el.getAttribute('title')) || '').replace(/\s+/g, ' ').trim().slice(0, 60) } catch { continue }
    const tag = await el.evaluate((n) => n.tagName).catch(() => '')
    if (SALTAR.test(nombre)) { fila.saltados.push(nombre); continue }
    if (await el.isDisabled().catch(() => false)) { (fila.deshabilitados ??= []).push(nombre); continue } // deshabilitado a proposito (p. ej. paginador con una sola pagina): no es un fallo
    peticiones = []; consola = []
    try {
      if (tag === 'SELECT') { const op = await el.locator('option').count(); if (op > 1) await el.selectOption({ index: 1 }, { timeout: 2000 }) }
      else await el.click({ timeout: 2500, trial: false })
    } catch (e) { fila.hallazgos.push({ control: nombre, problema: 'no se pudo pulsar: ' + String(e.message).split('\n')[0].slice(0, 90) }); continue }
    fila.pulsados++; await quieto()
    const malas = peticiones.filter((p) => p.e >= 400)
    const dialogo = await page.locator('[role="dialog"]:visible').count()
    if (malas.length || consola.length) fila.hallazgos.push({ control: nombre, malas: malas.map((p) => `${p.m} ${p.r} ${p.e}`), consola: [...new Set(consola)].slice(0, 2) })
    if (dialogo) { await page.keyboard.press('Escape'); await page.waitForTimeout(250) }
    if (new URL(page.url()).pathname !== final) await ir(ruta)
  }
  informe.push(fila)
  console.log(`${ruta}: ${fila.pulsados}/${fila.controles} pulsados, ${fila.saltados.length} saltados, ${fila.hallazgos.length} hallazgos`)
}
writeFileSync(salida, JSON.stringify(informe, null, 2))
await navegador.close()
