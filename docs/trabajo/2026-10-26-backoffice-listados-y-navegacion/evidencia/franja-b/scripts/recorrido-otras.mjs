// Escaladas (sin contrato), billetera (CU-13) y arquitectura y estado: aceptado contra el simulado, límite/error con page.route.
import { chromium, entrar, ir, afirmar, bajar, SALIDA } from './apoyo.mjs'

const nav = await chromium.launch({ args: ['--disable-features=LocalNetworkAccessChecks'] })
const ses = await entrar(nav)
const { page } = ses
const api = []
page.on('request', (r) => r.url().includes('/api/v1/') && api.push(`${r.method()} ${new URL(r.url()).pathname}`))
const ID = '9f1c2b7e-3a54-4d0e-8a61-5b7d2c9e4f10'

// ── Solicitudes escaladas ─────────────────────────────────────────────────────────────────────────────────────
api.length = 0
await ir(page, '/operacion/solicitudes-escaladas')
await page.getByText('Todavía no hay una cola de solicitudes escaladas.').waitFor()
afirmar(!api.some((a) => a.includes('solicitudes-escaladas')), `escaladas: no pide GET /grupos/solicitudes-escaladas (peticiones: ${JSON.stringify(api)})`)
await page.getByRole('button', { name: 'Abrir en Admisiones' }).click()
await page.getByText('Ese identificador no tiene el formato de una solicitud de ingreso.').waitFor()
afirmar(page.url().endsWith('/operacion/solicitudes-escaladas'), 'escaladas: error · identificador vacío marcado en el campo, no navega')
await page.getByLabel('Identificador de la solicitud').fill(ID.toUpperCase())
await page.getByRole('button', { name: 'Abrir en Admisiones' }).click()
await page.waitForURL(`**/admisiones/${ID}`)
afirmar(true, 'escaladas: aceptado · «Abrir en Admisiones» navega a /admisiones/:solicitudId')
const sinAdm = await entrar(nav, { permisos: ['SOLICITUD_INGRESO_VER', 'ver:operacion'] })
await ir(sinAdm.page, '/operacion/solicitudes-escaladas')
await sinAdm.page.getByText('Resolver solicitudes de ingreso es de quien tiene acceso a Admisiones.').waitFor()
afirmar(!(await sinAdm.page.getByRole('button', { name: 'Abrir en Admisiones' }).count()), 'escaladas: límite · sin acceso a Admisiones no se ofrece el botón y se explica')
await sinAdm.contexto.close()

// ── Billetera ─────────────────────────────────────────────────────────────────────────────────────────────────
const saldo = (id) => (r) => new URL(r.url()).pathname === `/api/v1/billetera/${id}/saldo`
let req = page.waitForRequest(saldo(ID))
await ir(page, `/operacion/billetera/${ID}`)
await req
await page.getByText('Bs 1.240,00').first().waitFor()
afirmar(true, 'billetera: aceptado · GET /billetera/:cuenta/saldo pinta el saldo con su moneda')
req = page.waitForRequest(saldo(ID))
await page.getByRole('button', { name: 'Actualizar saldo' }).click()
await req
afirmar(true, 'billetera: aceptado · «Actualizar saldo» vuelve a pedir el saldo')
await page.getByLabel('Identificador de la cuenta de billetera').fill('cuenta-1')
await page.getByRole('button', { name: 'Consultar saldo' }).click()
await page.getByText('Ese identificador no tiene el formato de una cuenta de billetera.').waitFor()
afirmar((await page.getByLabel('Identificador de la cuenta de billetera').inputValue()) === 'cuenta-1', 'billetera: error · identificador mal formado marcado y conservado, sin petición')
const OTRA = '22222222-2222-4222-8222-222222222222'
await page.getByLabel('Identificador de la cuenta de billetera').fill(OTRA)
req = page.waitForRequest(saldo(OTRA))
await page.getByRole('button', { name: 'Consultar saldo' }).click()
await req
await page.waitForURL(`**/operacion/billetera/${OTRA}`)
afirmar(true, 'billetera: aceptado · «Consultar saldo» navega a la otra cuenta y pide su saldo')
await page.screenshot({ path: `${SALIDA}/billetera-1280-claro.png`, fullPage: true })
await page.route((u) => u.pathname.endsWith('/saldo'), (r) => r.fulfill({ json: { cuentaId: OTRA, disponible: { monto: '0.00', moneda: 'BOB' }, retenido: { monto: '0.00', moneda: 'BOB' }, alCorteDe: '2026-10-26T10:00:00Z' } }))
await page.getByRole('button', { name: 'Actualizar saldo' }).click()
await page.getByText('La cuenta está en cero.', { exact: false }).waitFor()
afirmar(true, 'billetera: límite · saldo en cero ⇒ lo dice y aclara que no es un error')
await page.unrouteAll()
let falla = 500
await page.route((u) => u.pathname.endsWith('/saldo'), (r) => r.fulfill({ status: falla, json: { codigo: 'AP-GW', mensaje: 'detalle interno', trazaId: 'traza-b' } }))
await page.getByRole('button', { name: 'Actualizar saldo' }).click()
await page.getByRole('button', { name: 'Volver a intentar' }).waitFor()
afirmar(!(await page.locator('main').innerText()).includes('detalle interno'), 'billetera: error · 500 ⇒ mensaje accionable sin el crudo')
await page.screenshot({ path: `${SALIDA}/billetera-1280-error.png`, fullPage: true })
falla = 403
req = page.waitForRequest(saldo(OTRA))
await page.getByRole('button', { name: 'Volver a intentar' }).click()
await req
await page.getByText('No tenés acceso a esto.').waitFor()
afirmar(true, 'billetera: error · «Volver a intentar» repite; 403 ⇒ «No tenés acceso a esto.»')
await page.unrouteAll()

// ── Arquitectura y estado ─────────────────────────────────────────────────────────────────────────────────────
const SALUD = { consultadoEn: '2026-10-17T15:04:05Z', servicios: [{ servicio: 'identidad', estado: 'ARRIBA', latenciaMs: 12 }, { servicio: 'erp', estado: 'DEGRADADO', latenciaMs: 80 }, { servicio: 'publicidad', estado: 'CAIDO' }] }
await page.route((u) => u.pathname === '/api/v1/indicadores/operacion/servicios', (r) => r.fulfill({ json: SALUD }))
await ir(page, '/operacion/estado')
await page.getByText('Salud consultada a las 11:04:05').waitFor()
const filas = page.locator('ap-tabla-de-datos tbody tr')
afirmar((await filas.count()) === 14, 'estado: aceptado · 14 servicios en una página (20 por página)')
await page.getByLabel('Nivel', { exact: true }).selectOption({ label: 'N1 · crítico' })
await page.waitForURL(/filtro=N1/)
afirmar((await filas.count()) === 4, 'estado: aceptado · Nivel N1 deja 4 servicios y escribe ?filtro=N1 en la URL')
const csv = await bajar(page, 'Descargar CSV')
afirmar(csv.texto.trim().split(/\r?\n/).length === 5 && csv.texto.includes('identidad'), `estado: CSV de lo filtrado (4 filas + encabezado) · ${csv.nombre}`)
await page.getByLabel('Nivel', { exact: true }).selectOption({ label: 'Todos' })
await page.getByLabel('Salud', { exact: true }).selectOption({ label: 'no responde' })
await page.waitForFunction(() => document.querySelectorAll('ap-tabla-de-datos tbody tr').length === 1)
const json = JSON.parse((await bajar(page, 'Descargar JSON')).texto)
afirmar(json.length === 1 && json[0].servicio === 'publicidad', 'estado: filtro Salud = no responde ⇒ solo publicidad, y el JSON baja esa fila')
await page.getByLabel('Salud', { exact: true }).selectOption({ label: 'Todos' })
await page.getByLabel('Buscar', { exact: true }).fill('zzz')
await page.getByRole('button', { name: 'Quitar filtros' }).click()
await page.waitForFunction(() => document.querySelectorAll('ap-tabla-de-datos tbody tr').length === 14)
afirmar(true, 'estado: límite · búsqueda sin coincidencias ⇒ «Quitar filtros» vuelve a los 14')
await page.getByLabel('Buscar', { exact: true }).fill('libro contable')
await page.waitForFunction(() => document.querySelectorAll('ap-tabla-de-datos tbody tr').length === 1)
afirmar(true, 'estado: aceptado · buscar por el motivo del nivel encuentra nucleo-financiero')
await page.getByLabel('Buscar', { exact: true }).fill('')
await page.screenshot({ path: `${SALIDA}/estado-1280-claro.png`, fullPage: true })
await page.unrouteAll()
falla = 500
await page.route((u) => u.pathname === '/api/v1/indicadores/operacion/servicios', (r) => r.fulfill({ status: falla, json: { codigo: 'AP', mensaje: 'detalle interno' } }))
req = page.waitForRequest((r) => r.url().includes('/indicadores/operacion/servicios'))
await page.getByRole('button', { name: 'Volver a consultar' }).click()
await req
await page.getByText(/No se pudo consultar la salud/).waitFor()
afirmar(!(await page.locator('main').innerText()).includes('detalle interno') && (await page.getByText('Sin dato de salud').count()) > 0, 'estado: error · 500 ⇒ «No se pudo consultar la salud», tarjetas «Sin dato de salud», sin el crudo')
await page.screenshot({ path: `${SALIDA}/estado-1280-error.png`, fullPage: true })
falla = 403
await page.getByRole('button', { name: 'Volver a consultar' }).click()
await page.getByText(/Tu rol no puede ver la salud/).waitFor()
afirmar(true, 'estado: error · «Volver a consultar» repite; 403 ⇒ lo dice y la tabla queda con la arquitectura')
await page.unrouteAll()

// ── Capturas por ancho y tema (simulado) ──────────────────────────────────────────────────────────────────────
for (const [ruta, nombre, h1] of [['/operacion/solicitudes-escaladas', 'escaladas', 'Solicitudes de ingreso escaladas'], [`/operacion/billetera/${ID}`, 'billetera', 'Billetera del titular'], ['/operacion/estado', 'estado', 'Arquitectura y estado del proyecto']]) {
  for (const [ancho, alto, tema] of [[1280, 900, 'light'], [768, 1024, 'light'], [375, 812, 'light'], [1280, 900, 'dark']]) {
    if (nombre !== 'escaladas' && ancho === 1280 && tema === 'light') continue
    const s = await entrar(nav, { ancho, alto, tema })
    await ir(s.page, ruta)
    await s.page.getByRole('heading', { level: 1, name: h1, exact: true }).waitFor()
    await s.page.waitForFunction(() => !document.querySelector('ap-esqueleto, [aria-busy="true"]'))
    const w = await s.page.evaluate(() => document.documentElement.scrollWidth)
    afirmar(w <= ancho && s.errores.length === 0, `${nombre}: ${ancho}px ${tema} sin desborde (scrollWidth=${w}) y consola limpia`)
    await s.page.screenshot({ path: `${SALIDA}/${nombre}-${ancho}-${tema === 'dark' ? 'oscuro' : 'claro'}.png`, fullPage: true })
    await s.contexto.close()
  }
}
afirmar(ses.errores.filter((e) => !/status of (403|500)/.test(e)).length === 0, `otras: consola sin errores salvo los provocados ${JSON.stringify(ses.errores.filter((e) => !/status of (403|500)/.test(e)))}`)
await nav.close()
