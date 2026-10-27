// Recorrido en navegador de las bandejas por cursor de Operación (franja B). Aceptado contra el simulado OpenAPI
// (sin estado, sintético); límite y error con page.route (doble declarado). Uso: node recorrido-cursor.mjs [clave]
import { chromium, entrar, ir, afirmar, bajar, SALIDA } from './apoyo.mjs'

const SIM = 'http://127.0.0.1:4010/api/v1'
const VISTAS = {
  retiros: { ruta: '/operacion/retiros', titulo: 'Retiros en revisión', api: '/billetera/retiros', id: 'ordenRetiroId', prox: ['proximoFecha', 'proximoId'], param: 'despuesDeId',
    local: { etiqueta: 'Segundo factor', opcion: 'Con segundo factor' }, csv: 'Bs 4,44', largo: null,
    accion: async (page, fila) => { await page.getByRole('button', { name: `Revisar y decidir el retiro ${fila.ordenRetiroId}` }).click(); await page.waitForURL(`**/operacion/retiros/${fila.ordenRetiroId}`); return 'navega a /operacion/retiros/:ordenId' } },
  reclamos: { ruta: '/operacion/reclamos', titulo: 'Reclamos', api: '/reclamos', id: 'reclamoId', prox: ['proximoLimite', 'proximoId'], param: 'despuesDeId',
    servidor: { etiqueta: 'Estado', opcion: 'Cerrado', valor: 'CERRADO' }, local: { etiqueta: 'Canal', opcion: 'App' }, csv: '4,44 (moneda no registrada)', largo: 'producto',
    accion: async (page, fila) => {
      const [req] = await Promise.all([page.waitForRequest((r) => r.url().endsWith(`/reclamos/${fila.reclamoId}`)), page.getByRole('button', { name: `Responder el reclamo ${fila.codigo}` }).click()])
      await page.getByRole('dialog').waitFor()
      await page.getByRole('button', { name: 'Cancelar' }).click()
      return `abre el diálogo y pide ${req.method()} /reclamos/:id` } },
  custodia: { ruta: '/operacion/custodia', titulo: 'Conciliación de custodia', api: '/custodia/conciliaciones', id: 'conciliacionId', prox: ['proximoAntesDe', 'proximoAntesDeId'], param: 'antesDeId',
    local: { etiqueta: 'Estado', opcion: 'Cuadrada' }, csv: 'Bs 4,44', largo: null, tabla: 'Historial de conciliaciones' },
  cierres: { ruta: '/operacion/cierres-diarios', titulo: 'Cierres diarios', api: '/contabilidad/cierres-diarios', id: 'cierreId', prox: ['proximoAntesDe', 'proximoAntesDeId'], param: 'antesDeId',
    local: { etiqueta: 'Resultado', opcion: 'Cuadrado' }, csv: '4,44 (moneda no registrada)', largo: null, tabla: 'Historial de cierres' },
  reversos: { ruta: '/operacion/reversos', titulo: 'Reversos', api: '/contabilidad/reversos', id: 'reversoId', prox: ['proximoAntesDe', 'proximoAntesDeId'], param: 'antesDeId',
    servidor: { etiqueta: 'Estado', opcion: 'Rechazado', valor: 'RECHAZADO' }, local: { etiqueta: 'Tipo', opcion: 'Anulación' }, csv: '4,44 (moneda no registrada)', largo: 'motivo' },
  desembolsos: { ruta: '/operacion/desembolsos', titulo: 'Desembolsos', api: '/desembolsos', id: 'ordenId', prox: ['proximoAntesDe', 'proximoAntesDeId'], param: 'antesDeId',
    servidor: { etiqueta: 'Estado', opcion: 'Acreditada', valor: 'ACREDITADA' }, local: { etiqueta: 'Último resultado', opcion: 'Exitoso' }, csv: 'Bs 4,44', largo: 'ultimoCodigoError' },
}

const clave = process.argv[2]
const v = VISTAS[clave]
const esApi = (u) => { const url = new URL(u); return url.pathname === '/api/v1' + v.api && url.searchParams.get('limite') !== '1' }
const filas = (page) => page.locator('ap-tabla-de-datos tbody tr')
const titulo = v.tabla ?? v.titulo

const nav = await chromium.launch({ args: ['--disable-features=LocalNetworkAccessChecks'] })
const ses = await entrar(nav)
const { page } = ses
const red = []
page.on('request', (r) => esApi(r.url()) && red.push(new URL(r.url()).search))

// ── ACEPTADO (simulado) ───────────────────────────────────────────────────────────────────────────────────────
const primero = page.waitForRequest((r) => esApi(r.url()))
await ir(page, v.ruta)
const r1 = new URL((await primero).url())
afirmar(r1.searchParams.get('limite') === '10' && !r1.searchParams.has(v.param), `${clave}: primera página GET ${v.api}?limite=10 sin cursor`)
await page.getByRole('heading', { level: 1, name: v.titulo, exact: true }).waitFor()
await filas(page).first().waitFor()
const muestra = (await (await fetch(`${SIM}${v.api}?limite=10`)).json()).items[0]
afirmar((await filas(page).count()) >= 1, `${clave}: la tabla pinta la página del simulado`)
await page.screenshot({ path: `${SALIDA}/${clave}-1280-claro.png`, fullPage: true })

let req = page.waitForRequest((r) => esApi(r.url()))
await page.getByRole('button', { name: 'Siguiente' }).click()
let u = new URL((await req).url())
afirmar(u.searchParams.has(v.param), `${clave}: Siguiente pide con el cursor (${v.param}=${u.searchParams.get(v.param)?.slice(0, 8)}…)`)
await page.getByText('Página 2 de').waitFor()
req = page.waitForRequest((r) => esApi(r.url()))
await page.getByRole('button', { name: 'Anterior' }).click()
u = new URL((await req).url())
afirmar(!u.searchParams.has(v.param), `${clave}: Anterior vuelve a la página 1 sin cursor`)
await page.getByText('Página 1 de').waitFor()
req = page.waitForRequest((r) => esApi(r.url()))
await page.getByLabel('Filas por página').selectOption('20')
u = new URL((await req).url())
afirmar(u.searchParams.get('limite') === '20', `${clave}: Filas por página = 20 pide limite=20`)

if (v.servidor) {
  req = page.waitForRequest((r) => esApi(r.url()))
  await page.getByLabel(v.servidor.etiqueta, { exact: true }).selectOption({ label: v.servidor.opcion })
  u = new URL((await req).url())
  afirmar(u.searchParams.getAll('estados').join() === v.servidor.valor, `${clave}: filtro ${v.servidor.etiqueta}=${v.servidor.opcion} viaja como estados=${v.servidor.valor}`)
  await filas(page).first().waitFor()
}
const antes = red.length
await page.getByLabel(v.local.etiqueta, { exact: true }).selectOption({ label: v.local.opcion })
await filas(page).first().waitFor()
afirmar(red.length === antes, `${clave}: filtro local ${v.local.etiqueta}=${v.local.opcion} no vuelve a pedir y deja la fila`)
const buscador = page.getByRole('searchbox').or(page.getByLabel('Buscar', { exact: true })).first()
await buscador.fill(String(muestra[v.id]).slice(0, 8))
await page.waitForFunction(() => /de \d+ \S+ en la página/.test(document.body.innerText))
afirmar((await filas(page).count()) === 1, `${clave}: buscar por ${v.id} encuentra la fila`)
await buscador.fill('zz-no-existe-zz')
await page.getByText('Ningún registro de esta página coincide').waitFor()
const csvVacio = await bajar(page, 'Descargar CSV')
afirmar(csvVacio.texto.trim().split(/\r?\n/).length === 1, `${clave}: CSV sin coincidencias baja solo el encabezado (${csvVacio.nombre})`)
await page.getByRole('button', { name: 'Quitar filtros' }).click()
await filas(page).first().waitFor()
const csv = await bajar(page, 'Descargar CSV')
afirmar(csv.texto.includes(v.csv) && csv.texto.trim().split(/\r?\n/).length >= 2, `${clave}: CSV de lo filtrado con el importe como se ve («${v.csv}») · ${csv.nombre}`)
const json = await bajar(page, 'Descargar JSON')
const datos = JSON.parse(json.texto)
afirmar(Array.isArray(datos) && datos.length === (await filas(page).count()), `${clave}: JSON con ${datos.length} fila(s) filtrada(s) · ${json.nombre}`)
if (v.accion) afirmar(true, `${clave}: acción de fila — ${await v.accion(page, muestra)}`)

// ── LÍMITE (doble con page.route: 25 filas sintéticas paginadas por cursor) ──────────────────────────────────────
function sinteticas(n) {
  return Array.from({ length: n }, (_, i) => ({ ...muestra, [v.id]: `00000000-0000-4000-8000-${String(i + 1).padStart(12, '0')}`, ...(v.largo ? { [v.largo]: i === 0 ? 'Texto largo sintético para probar el corte de línea en móvil '.repeat(4) : muestra[v.largo] } : {}), ...(clave === 'reclamos' ? { codigo: `RC-${i + 1}` } : {}) }))
}
let datosDoble = sinteticas(25)
let falla = null
await page.route((url) => esApi(url.href), async (route) => {
  if (falla) return route.fulfill({ status: falla, json: { codigo: `AP-GW-${falla}`, mensaje: 'detalle interno', trazaId: `traza-${falla}` } })
  const q = new URL(route.request().url()).searchParams
  const limite = Number(q.get('limite'))
  const desde = q.get(v.param) ? datosDoble.findIndex((f) => f[v.id] === q.get(v.param)) + 1 : 0
  const items = datosDoble.slice(desde, desde + limite)
  const hay = desde + limite < datosDoble.length
  await route.fulfill({ json: { items, [v.prox[0]]: hay ? '2026-01-15T10:30:00Z' : null, [v.prox[1]]: hay ? items.at(-1)[v.id] : null } })
})
await ir(page, '/tablero')
await ir(page, v.ruta)
await filas(page).nth(9).waitFor()
afirmar((await filas(page).count()) === 10, `${clave}: límite · 25 filas, 10 por página ⇒ 10 en la página 1`)
await page.getByRole('button', { name: 'Siguiente' }).click()
await page.getByText('Página 2 de 3').waitFor()
await page.getByLabel('Ir a la página').fill('3')
await page.getByLabel('Ir a la página').press('Enter')
await page.getByText('Página 3 de 3').waitFor()
afirmar((await filas(page).count()) === 5 && await page.getByRole('button', { name: 'Siguiente' }).isDisabled(), `${clave}: límite · Ir a 3 ⇒ última página parcial (5 filas) y Siguiente deshabilitado`)
datosDoble = sinteticas(1)
await ir(page, '/tablero'); await ir(page, v.ruta)
await page.getByText('Página 1 de 1').waitFor()
afirmar((await filas(page).count()) === 1, `${clave}: límite · una sola fila ⇒ Página 1 de 1`)
if (v.largo) {
  await page.setViewportSize({ width: 375, height: 812 })
  await page.waitForFunction(() => document.documentElement.scrollWidth <= 375)
  afirmar(true, `${clave}: límite · texto largo a 375 px sin desborde horizontal de la página`)
  await page.screenshot({ path: `${SALIDA}/${clave}-375-texto-largo.png`, fullPage: true })
  await page.setViewportSize({ width: 1280, height: 900 })
}
datosDoble = []
await ir(page, '/tablero'); await ir(page, v.ruta)
await page.locator('ap-estado-vacio').waitFor()
afirmar(!(await page.locator('ap-tabla-de-datos').count()), `${clave}: límite · colección vacía ⇒ vacío explicado («${(await page.locator('ap-estado-vacio').innerText()).split('\n')[0]}»)`)

// ── ERROR (doble: 500 y 403) ─────────────────────────────────────────────────────────────────────────────────
datosDoble = sinteticas(3)
falla = 500
await ir(page, '/tablero'); await ir(page, v.ruta)
await page.getByText(`No se pudo cargar ${titulo.toLowerCase()}.`).waitFor()
const textoError = await page.locator('main').innerText()
afirmar(textoError.includes('traza-500') && !textoError.includes('detalle interno'), `${clave}: error · 500 ⇒ mensaje accionable con código de seguimiento, sin el crudo`)
await page.screenshot({ path: `${SALIDA}/${clave}-1280-error.png`, fullPage: true })
falla = null
req = page.waitForRequest((r) => esApi(r.url()))
await page.getByRole('button', { name: 'Volver a intentar' }).click()
await req
await filas(page).first().waitFor()
afirmar((await filas(page).count()) === 3, `${clave}: error · «Volver a intentar» repite la petición y se recupera`)
falla = 403
await ir(page, '/tablero'); await ir(page, v.ruta)
await page.getByText('No tenés acceso a esta bandeja.').waitFor()
afirmar(true, `${clave}: error · 403 ⇒ «No tenés acceso a esta bandeja.»`)
await page.unrouteAll()

// ── CAPTURAS (simulado) ──────────────────────────────────────────────────────────────────────────────────────
for (const [ancho, alto, tema] of [[768, 1024, 'light'], [375, 812, 'light'], [1280, 900, 'dark']]) {
  const s = await entrar(nav, { ancho, alto, tema })
  await ir(s.page, v.ruta)
  await s.page.getByRole('heading', { level: 1, name: v.titulo, exact: true }).waitFor()
  await s.page.locator('ap-tabla-de-datos').waitFor()
  const anchoDoc = await s.page.evaluate(() => document.documentElement.scrollWidth)
  afirmar(anchoDoc <= ancho, `${clave}: ${ancho}px ${tema} sin desborde horizontal (scrollWidth=${anchoDoc})`)
  await s.page.screenshot({ path: `${SALIDA}/${clave}-${ancho}-${tema === 'dark' ? 'oscuro' : 'claro'}.png`, fullPage: true })
  afirmar(s.errores.length === 0, `${clave}: ${ancho}px ${tema} consola sin errores`)
  await s.contexto.close()
}
afirmar(ses.errores.filter((e) => !/status of (403|500)/.test(e)).length === 0, `${clave}: consola sin errores (salvo los 403/500 provocados) ${JSON.stringify(ses.errores.filter((e) => !/status of (403|500)/.test(e)))}`)
await nav.close()
