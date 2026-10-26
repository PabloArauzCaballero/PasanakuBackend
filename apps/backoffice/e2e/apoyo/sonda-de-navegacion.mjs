// Sonda de navegación (DOBLE DE SESIÓN, igual que `sesion-e2e.ts`): entra con todos los permisos, recorre cada entrada
// del menú y cada pestaña de sección midiendo cuánto tarda en pintar la vista, y junta errores de consola y respuestas
// 4xx/5xx. Uso: `ROL=PLATAFORMA PERMISOS_ARCHIVO=permisos.txt node e2e/apoyo/sonda-de-navegacion.mjs [baseURL] [salida.json]`.
// No es una prueba: es un diagnóstico. Un clic cuyo destino ya es la URL actual no se mide (no es una navegación);
// uno que termina fuera de su `href` es un **rebote** y se lista aparte. Recorre también las subpestañas de grupo.
import { chromium } from 'playwright'
import { readFileSync, writeFileSync } from 'node:fs'

const base = process.argv[2] ?? 'http://127.0.0.1:4300'
const salida = process.argv[3] ?? 'sonda.json'
const permisos = (process.env.PERMISOS_ARCHIVO ? readFileSync(process.env.PERMISOS_ARCHIVO, 'utf8') : '').split(/\s+/).filter(Boolean)

const rol = process.env.ROL ?? 'oficial'
function token(p) {
  const b = Buffer.from(JSON.stringify({ permisos: p, rol, sub: 'sonda' })).toString('base64url')
  return `sonda.${b}.firma`
}

const navegador = await chromium.launch({ args: ['--disable-features=LocalNetworkAccessChecks'] })
const page = await navegador.newPage({ viewport: { width: 1280, height: 900 } })
const errores = []
page.on('console', (m) => m.type() === 'error' && errores.push({ url: page.url(), tipo: 'consola', texto: m.text().slice(0, 300) }))
page.on('pageerror', (e) => errores.push({ url: page.url(), tipo: 'excepcion', texto: String(e).slice(0, 300) }))
page.on('response', (r) => r.status() >= 400 && r.url().includes('/api/') && errores.push({ url: page.url(), tipo: 'red', texto: `${r.status()} ${r.request().method()} ${r.url().replace(base, '')}` }))

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

const medidas = []
const rebotes = []
const sinRuta = (u) => u.replace(base, '').split(/[?#]/)[0]
async function medir(nombre, href, accion) {
  const antes = page.url()
  if (sinRuta(antes) === href) return
  const t0 = Date.now()
  await accion()
  try {
    await page.waitForFunction((a) => location.href !== a, antes, { timeout: 8000 })
  } catch {}
  // "Pintó": el outlet tiene algo distinto de un esqueleto o "Cargando".
  try {
    await page.waitForFunction(() => {
      const m = document.querySelector('main#contenido')
      if (!m) return false
      const t = m.innerText
      return t.length > 40 && !/Cargando…?\s*$/.test(t.trim())
    }, null, { timeout: 8000 })
  } catch {}
  const url = sinRuta(page.url())
  const rebote = !(url === href || url.startsWith(href + '/'))
  medidas.push({ nombre, href, url, ms: Date.now() - t0, rebote })
  if (rebote) rebotes.push({ nombre, href, url })
}
const enlacesDe = (sel) => page.locator(sel).evaluateAll((as) => as.map((a) => ({ texto: a.textContent.trim(), href: a.getAttribute('href') })))

const enlaces = await enlacesDe('ap-menu-lateral a')
for (const e of enlaces) {
  await medir('menu:' + e.texto, e.href, () => page.locator(`ap-menu-lateral a[href="${e.href}"]`).first().click())
  const pestanas = await enlacesDe('ap-pestanas-de-seccion nav:not(.sub) a')
  for (const p of pestanas) {
    await medir(`pestana:${e.texto}/${p.texto}`, p.href, () => page.locator(`ap-pestanas-de-seccion nav:not(.sub) a[href="${p.href}"]`).first().click())
    if (medidas.at(-1)?.href === p.href) medidas.at(-1).botones = await page.locator('main#contenido button:visible').count()
    for (const h of await enlacesDe('ap-pestanas-de-seccion nav.sub a')) {
      await medir(`subpestana:${e.texto}/${p.texto}/${h.texto}`, h.href, () => page.locator(`ap-pestanas-de-seccion nav.sub a[href="${h.href}"]`).first().click())
    }
  }
}
writeFileSync(salida, JSON.stringify({ rol, entradas: enlaces, medidas, rebotes, errores }, null, 1))
console.log(JSON.stringify({ rol, vistas: medidas.length, rebotes: rebotes.length, lentas: medidas.filter((m) => m.ms > 1500).length, errores: errores.length }))
await navegador.close()
