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
const capturas = process.argv[3] ?? './capturas'
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

// --- utilidades ---
const main = page.locator('main#contenido')
const filas = main.locator('tbody tr')
async function navegar(ruta) {
  if (await page.evaluate((r) => location.pathname === r, ruta)) return
  const titulo = await page.title()
  await page.evaluate((r) => { history.pushState({}, '', r); dispatchEvent(new PopStateEvent('popstate')) }, ruta)
  // El router pinta después de cambiar la URL: se espera a que cambie el título de la ruta, no solo `location`.
  await page.waitForFunction((t) => document.title !== t, titulo, { timeout: 15000 })
}
async function recargarVista(ruta) {
  await navegar('/tablero')
  await navegar(ruta)
}
async function esperarFilas(n) {
  await page.waitForFunction((n) => document.querySelectorAll('main#contenido tbody tr').length === n, n, { timeout: 15000 })
}
async function esperarPedido(pred, desde) {
  const t0 = Date.now()
  while (Date.now() - t0 < 15000) {
    const p = pedidos.slice(desde).find(pred)
    if (p) return p
    await page.waitForEvent('request', { timeout: 15000 }).catch(() => {})
  }
  throw new Error('no salió el pedido esperado')
}
async function bajar(texto) {
  const [d] = await Promise.all([page.waitForEvent('download'), main.getByRole('button', { name: texto }).click()])
  return { nombre: d.suggestedFilename(), contenido: readFileSync(await d.path(), 'utf8') }
}
function registrar(vista, nivel, paso, ok, detalle = '') {
  resultados.push({ vista, nivel, paso, veredicto: ok ? 'PASS' : 'FAIL', detalle })
  console.log(`${ok ? 'PASS' : 'FAIL'} · ${vista} · ${nivel} · ${paso}${detalle ? ' — ' + detalle : ''}`)
}
async function paso(vista, nivel, nombre, fn) {
  try {
    const d = await fn()
    registrar(vista, nivel, nombre, true, typeof d === 'string' ? d : '')
  } catch (e) {
    registrar(vista, nivel, nombre, false, String(e).slice(0, 200))
    await page.screenshot({ path: `${capturas}/FALLO-${vista}-${nombre}.png`.replace(/[^\w./-]+/g, '_') }).catch(() => {})
  }
}
async function capturar(vista) {
  for (const [ancho, alto] of [[1280, 900], [768, 1024], [375, 812]]) {
    await page.setViewportSize({ width: ancho, height: alto })
    await page.screenshot({ path: `${capturas}/${vista}-${ancho}-claro.png`, fullPage: true })
  }
  await page.setViewportSize({ width: 1280, height: 900 })
  await page.emulateMedia({ colorScheme: 'dark' })
  // Sin esto la captura sale a mitad de la transición de color (filas grises en oscuro).
  await page.waitForFunction(() => document.getAnimations().every((a) => a.playState !== 'running'), null, { timeout: 5000 }).catch(() => {})
  await page.screenshot({ path: `${capturas}/${vista}-1280-oscuro.png`, fullPage: true })
  await page.emulateMedia({ colorScheme: 'light' })
  const ancho = await page.evaluate(() => document.documentElement.scrollWidth)
  return ancho
}

/** Los tres niveles de un listado por cursor. `antes` abre la pestaña, `accion` prueba la acción de fila. */
async function probarListado(v) {
  const { vista, ruta, recurso, buscar, local, servidor, antes, accion, exportar } = v
  modo[recurso] = 'ok'
  const inicio = pedidos.length
  await recargarVista(ruta)
  if (antes) await antes()
  await paso(vista, 'aceptado', 'lista 10 de 25 con limite=10', async () => {
    const p = await esperarPedido((x) => x.metodo === 'GET' && new URL(x.url, 'http://x').pathname === RECURSOS[recurso].ruta, inicio)
    await page.waitForFunction((r) => (document.querySelector('main#contenido tbody tr')?.textContent ?? '').length > 0 && document.querySelectorAll('main#contenido tbody tr').length === 10, null)
    if (!p.url.includes('limite=10')) throw new Error(p.url)
    return p.url
  })
  await paso(vista, 'aceptado', `buscar «${buscar.texto}»`, async () => {
    const n = pedidos.length
    await main.getByRole('searchbox').fill(buscar.texto)
    await esperarFilas(buscar.esperadas)
    if (pedidos.slice(n).some((x) => x.url.includes(RECURSOS[recurso].ruta))) throw new Error('la búsqueda pidió al servidor')
    await main.getByRole('button', { name: 'Limpiar filtros' }).first().click()
    await esperarFilas(10)
  })
  if (local) await paso(vista, 'aceptado', `filtro local «${local.etiqueta}»`, async () => {
    await main.getByLabel(local.etiqueta, { exact: true }).selectOption({ label: local.opcion })
    await page.waitForFunction(() => document.querySelectorAll('main#contenido tbody tr').length < 10)
    const n = await filas.count()
    await main.getByRole('button', { name: 'Limpiar filtros' }).first().click()
    await esperarFilas(10)
    return `${n} filas`
  })
  if (servidor) await paso(vista, 'aceptado', `filtro del servidor «${servidor.etiqueta}» → estados=${servidor.valor}`, async () => {
    const n = pedidos.length
    await main.getByLabel(servidor.etiqueta, { exact: true }).selectOption({ label: servidor.opcion })
    const p = await esperarPedido((x) => x.url.includes(RECURSOS[recurso].ruta) && x.url.includes(`estados=${servidor.valor}`), n)
    await main.getByRole('button', { name: 'Limpiar filtros' }).first().click()
    await esperarFilas(10)
    return p.url
  })
  await paso(vista, 'aceptado', 'filas por página 20 → limite=20', async () => {
    const n = pedidos.length
    await main.getByLabel('Filas por página').selectOption('20')
    await esperarPedido((x) => x.url.includes('limite=20'), n)
    await esperarFilas(20)
    await main.getByLabel('Filas por página').selectOption('10')
    await esperarFilas(10)
  })
  await paso(vista, 'aceptado', 'Siguiente pide con el cursor; «Ir a 3» muestra 5; Anterior vuelve', async () => {
    const n = pedidos.length
    await main.getByRole('button', { name: 'Siguiente' }).click()
    const p = await esperarPedido((x) => x.url.includes(RECURSOS[recurso].ruta) && /(antes|despues)DeId=/.test(x.url), n)
    await page.waitForFunction(() => /Página 2 de 3/.test(document.querySelector('main#contenido ap-paginacion')?.textContent ?? ''))
    await main.getByLabel('Ir a la página').fill('3')
    await main.getByLabel('Ir a la página').press('Enter')
    await esperarFilas(5)
    await main.getByRole('button', { name: 'Anterior' }).click()
    await esperarFilas(10)
    await page.waitForFunction(() => /Página 2 de 3/.test(document.querySelector('main#contenido ap-paginacion')?.textContent ?? ''))
    if (await main.getByRole('button', { name: 'Siguiente' }).isDisabled()) throw new Error('Siguiente apagado en la 2 de 3')
    await main.getByRole('button', { name: 'Anterior' }).click()
    await page.waitForFunction(() => /Página 1 de 3/.test(document.querySelector('main#contenido ap-paginacion')?.textContent ?? ''))
    return p.url
  })
  await paso(vista, 'aceptado', 'CSV y JSON de lo filtrado', async () => {
    await main.getByRole('searchbox').fill(buscar.texto)
    await esperarFilas(buscar.esperadas)
    const csv = await bajar('Descargar CSV')
    const json = await bajar('Descargar JSON')
    const lineas = csv.contenido.replace(/^﻿/, '').trim().split('\r\n')
    const datos = JSON.parse(json.contenido)
    if (lineas.length !== buscar.esperadas + 1 || datos.length !== buscar.esperadas) throw new Error(`${lineas.length} líneas, ${datos.length} objetos`)
    if (exportar && !csv.contenido.includes(exportar)) throw new Error(`el CSV no trae «${exportar}»`)
    if (csv.contenido.includes(",'-")) throw new Error('un importe negativo salió con apóstrofo')
    await main.getByRole('button', { name: 'Limpiar filtros' }).first().click()
    await esperarFilas(10)
    writeFileSync(`${capturas}/${vista}.csv`, csv.contenido)
    writeFileSync(`${capturas}/${vista}.json`, json.contenido)
    return `${csv.nombre} (${lineas.length} líneas: «${lineas[1]?.slice(0, 90)}») · ${json.nombre} (${datos.length})`
  })
  if (accion) await accion()
  await paso(vista, 'aceptado', 'capturas 1280/768/375 claro + 1280 oscuro, sin scroll horizontal', async () => {
    await page.evaluate(() => window.scrollTo(0, 0))
    const ancho = await capturar(vista)
    if (ancho > 1280) throw new Error(`scrollWidth ${ancho}`)
  })
  // límite
  await paso(vista, 'límite', 'búsqueda sin coincidencias → «Quitar filtros»', async () => {
    await main.getByRole('searchbox').fill('zzzz-no-existe')
    await main.getByRole('button', { name: 'Quitar filtros' }).click()
    await esperarFilas(10)
  })
  await paso(vista, 'límite', 'colección vacía → vacío orientado sin paginación', async () => {
    modo[recurso] = 'vacio'
    await recargarVista(ruta)
    if (antes) await antes()
    await main.locator('ap-estado-vacio').waitFor()
    if (await main.locator('ap-paginacion').count()) throw new Error('hay paginación')
    return (await main.locator('ap-estado-vacio').innerText()).replace(/\s+/g, ' ').slice(0, 80)
  })
  await paso(vista, 'límite', 'una sola fila → «Página 1 de 1»', async () => {
    modo[recurso] = 'una'
    await recargarVista(ruta)
    if (antes) await antes()
    await esperarFilas(1)
    const t = await main.locator('ap-paginacion').innerText()
    if (!t.includes('Página 1 de 1')) throw new Error(t)
  })
  // error
  for (const codigo of ['500', '403']) {
    await paso(vista, 'error', `${codigo} → mensaje accionable sin crudo; «Volver a intentar» repite`, async () => {
      modo[recurso] = codigo
      esperados++
      await recargarVista(ruta)
      if (antes) await antes()
      const alerta = main.locator('ap-estado-de-error')
      await alerta.waitFor()
      const texto = (await alerta.innerText()).replace(/\s+/g, ' ')
      if (/NullPointer|denegado interno/.test(texto)) throw new Error('error crudo: ' + texto)
      if (!texto.includes(`tz-franja-e-${codigo}`)) throw new Error('sin código de seguimiento: ' + texto)
      if (codigo === '403' && !texto.includes('No tenés acceso')) throw new Error(texto)
      modo[recurso] = 'ok'
      const n = pedidos.length
      await alerta.getByRole('button', { name: 'Volver a intentar' }).click()
      await esperarPedido((x) => x.url.includes(RECURSOS[recurso].ruta), n)
      await esperarFilas(10)
      return texto.slice(0, 110)
    })
  }
  modo[recurso] = 'ok'
}

const pestana = (nombre) => async () => { await main.getByRole('tab', { name: nombre }).click() }
const vigente = await page.evaluate(async () => (await fetch('/api/v1/erp/periodos/vigente')).json())

// --- Contabilidad ---
await probarListado({ vista: 'presupuesto', ruta: '/contabilidad/presupuesto', recurso: 'partidas', buscar: { texto: 'nandu', esperadas: 1 }, local: { etiqueta: 'Centro de costo', opcion: 'Marketing' }, servidor: { etiqueta: 'Estado del presupuesto', opcion: 'Aprobado', valor: 'APROBADO' }, exportar: 'BOB -359.90' })

await probarListado({
  vista: 'compras-ordenes', ruta: '/contabilidad/compras', recurso: 'ordenes', buscar: { texto: 'OC-0005', esperadas: 1 }, local: { etiqueta: 'Centro de costo', opcion: 'Sin centro de costo' }, servidor: { etiqueta: 'Estado de la orden', opcion: 'Borrador', valor: 'BORRADOR' }, exportar: 'BOB 805.00',
  accion: async () => {
    await paso('compras-ordenes', 'aceptado', '«Aprobar» OC-0005 → POST /aprobacion con Idempotency-Key y recarga', async () => {
      const n = pedidos.length
      await main.getByRole('button', { name: 'Aprobar la orden OC-0005' }).click()
      await page.getByRole('dialog').getByRole('button', { name: 'Aprobar la orden' }).click()
      const post = await esperarPedido((x) => x.metodo === 'POST' && x.url.endsWith(`/ordenes-de-compra/00000000-0000-4000-8000-000000000005/aprobacion`), n)
      if (!post.clave) throw new Error('sin Idempotency-Key')
      await main.getByText('quedó aprobada').waitFor()
      await esperarPedido((x) => x.metodo === 'GET' && x.url.includes('/ordenes-de-compra?'), n)
      return `${post.metodo} ${post.url} · Idempotency-Key ${post.clave.slice(0, 8)}…`
    })
    await paso('compras-ordenes', 'error', 'aprobación con 500 → error traducido, botón libre', async () => {
      esperados++
      await page.route('**/aprobacion', (r) => r.fulfill({ status: 500, contentType: 'application/json', body: JSON.stringify({ codigo: 'AP-GEN-500', mensaje: 'stack interno', trazaId: 'tz-aprob' }) }), { times: 1 })
      await main.getByRole('button', { name: 'Aprobar la orden OC-0005' }).click()
      await page.getByRole('dialog').getByRole('button', { name: 'Aprobar la orden' }).click()
      await main.locator('ap-error-de-envio').getByText(/tz-aprob|salió mal|Probá/).first().waitFor()
      if ((await main.innerText()).includes('stack interno')) throw new Error('crudo')
      if (await main.getByRole('button', { name: 'Aprobar la orden OC-0005' }).isDisabled()) throw new Error('trabado')
    })
  },
})

await probarListado({
  vista: 'compras-facturas', ruta: '/contabilidad/compras', recurso: 'facturas', antes: pestana('Facturas de proveedor'), buscar: { texto: 'F-0003', esperadas: 1 }, local: { etiqueta: 'Vencimiento', opcion: 'Vencida con saldo' }, servidor: { etiqueta: 'Estado de la factura', opcion: 'Aprobada', valor: 'APROBADA' }, exportar: 'BOB 800.00',
  accion: async () => {
    await paso('compras-facturas', 'aceptado', '«Registrar el pago» F-0001 → POST /pagos con monto exacto y recarga', async () => {
      const n = pedidos.length
      await main.getByRole('button', { name: 'Registrar el pago de la factura F-0001' }).click()
      const dialogo = page.getByRole('dialog')
      await dialogo.getByLabel('Monto a pagar').fill('100,50')
      await dialogo.getByRole('button', { name: /Registrar el pago de F-0001/ }).click()
      const post = await esperarPedido((x) => x.metodo === 'POST' && x.url.endsWith('/facturas-de-proveedor/00000000-0000-4000-8000-000000000001/pagos'), n)
      if (!post.clave || !post.cuerpo.includes('"monto":"100.50"')) throw new Error(post.cuerpo)
      await main.getByText('quedó registrado').waitFor()
      await page.waitForFunction(() => /Página 1 de 2/.test(document.querySelector('main#contenido ap-paginacion')?.textContent ?? ''))
      if (await main.getByRole('button', { name: 'Siguiente' }).isDisabled()) throw new Error('Siguiente apagado tras recargar')
      return `${post.url} · ${post.cuerpo}`
    })
  },
})

await probarListado({
  vista: 'cobros', ruta: '/contabilidad/cobros', recurso: 'cuentas', buscar: { texto: 'nunoa', esperadas: 1 }, local: { etiqueta: 'Origen', opcion: 'Venta de servicio' }, servidor: { etiqueta: 'Estado de la cuenta', opcion: 'Incobrable', valor: 'INCOBRABLE' }, exportar: 'BOB 300.00',
  accion: async () => {
    await paso('cobros', 'error', 'cobro con 500 → conserva lo escrito y el botón no queda trabado', async () => {
      esperados++
      await page.route('**/cobros', (r) => (r.request().method() === 'POST' ? r.fulfill({ status: 500, contentType: 'application/json', body: '{"codigo":"AP-GEN-500","trazaId":"tz-cobro"}' }) : r.continue()), { times: 1 })
      await main.getByRole('button', { name: 'Registrar el cobro de Anunciante 1' }).click()
      const dialogo = page.getByRole('dialog')
      await dialogo.getByLabel('Monto cobrado').fill('50,00')
      const confirmar = dialogo.getByRole('button', { name: /Registrar el cobro de/ })
      await confirmar.click()
      await dialogo.locator('ap-error-de-envio').getByText(/./).first().waitFor()
      if ((await dialogo.getByLabel('Monto cobrado').inputValue()) !== '50,00') throw new Error('perdió lo escrito')
      if (await confirmar.isDisabled()) throw new Error('trabado')
    })
    await paso('cobros', 'aceptado', 'reintento del cobro → POST /cobros con monto 50.00 y recarga', async () => {
      const n = pedidos.length
      await page.getByRole('dialog').getByRole('button', { name: /Registrar el cobro de/ }).click()
      const post = await esperarPedido((x) => x.metodo === 'POST' && x.url.endsWith('/cobros'), n)
      if (!post.clave || !post.cuerpo.includes('"monto":"50.00"')) throw new Error(post.cuerpo)
      await main.getByText('quedó registrado').waitFor()
      return `${post.url} · ${post.cuerpo} · clave ${post.clave.slice(0, 8)}…`
    })
  },
})

await probarListado({
  vista: 'activos', ruta: `/contabilidad/activos/${vigente.periodoId}`, recurso: 'activos', buscar: { texto: 'AF-005', esperadas: 1 }, local: { etiqueta: 'Categoría', opcion: 'Vehículos' }, servidor: { etiqueta: 'Estado del activo', opcion: 'Dado de baja', valor: 'DADO_DE_BAJA' }, exportar: 'BOB 2700.00',
  accion: async () => {
    await paso('activos', 'aceptado', '«Depreciar» AF-001 → POST /depreciaciones con el período y recarga', async () => {
      const n = pedidos.length
      await main.getByRole('button', { name: 'Depreciar en el período el activo AF-001' }).click()
      const post = await esperarPedido((x) => x.metodo === 'POST' && x.url.endsWith('/activos/00000000-0000-4000-8000-000000000001/depreciaciones'), n)
      if (!post.clave || !post.cuerpo.includes(vigente.periodoId)) throw new Error(post.cuerpo)
      await main.getByText('ya tiene su cuota').waitFor()
      return `${post.url} · ${post.cuerpo}`
    })
  },
})

// Períodos (sin listado: tarjetas de meses del simulado) y estados financieros
await paso('periodo', 'aceptado', 'período de hoy, ejercicios, meses; sin «Asentar»; cerrar → POST /cierre', async () => {
  await recargarVista('/contabilidad')
  await main.getByText('Hoy estás en').waitFor()
  await main.locator('.mes').first().waitFor()
  if (await main.getByRole('button', { name: /Asentar/ }).count()) throw new Error('botón muerto')
  const n = pedidos.length
  await main.getByRole('button', { name: 'Cerrar el período' }).first().click()
  await page.getByRole('dialog').getByRole('button', { name: /^Cerrar el período / }).click()
  const post = await esperarPedido((x) => x.metodo === 'POST' && x.url.endsWith('/cierre'), n)
  if (!post.clave) throw new Error('sin clave')
  await page.evaluate(() => window.scrollTo(0, 0))
  await capturar('periodo')
  return `${post.url} · ${post.cuerpo}`
})
await paso('estados', 'error', 'Prism repite un hash vacío junto al documento → NO se entrega el archivo y se dice por qué', async () => {
  await recargarVista(`/contabilidad/estados/${vigente.periodoId}`)
  await main.getByRole('button', { name: 'Generar el estado financiero' }).click()
  await main.getByText('Estado financiero generado').waitFor()
  let bajo = false
  page.once('download', () => (bajo = true))
  await main.getByRole('button', { name: 'Descargar el documento' }).click()
  await main.getByText('no corresponde al estado financiero generado').waitFor()
  if (bajo) throw new Error('entregó un archivo que no corresponde')
})
await paso('estados', 'aceptado', 'generar → POST; descargar → GET documento (doble con el hash generado) y el navegador recibe el archivo', async () => {
  await recargarVista('/contabilidad')
  await navegar(`/contabilidad/estados/${vigente.periodoId}`)
  const n = pedidos.length
  const generado = page.waitForResponse((r) => r.url().endsWith('/estados-financieros') && r.request().method() === 'POST')
  await main.getByRole('button', { name: 'Generar el estado financiero' }).click()
  const g = await (await generado).json()
  const post = await esperarPedido((x) => x.metodo === 'POST' && x.url.endsWith('/estados-financieros'), n)
  await main.getByText('Estado financiero generado').waitFor()
  await page.route('**/documento', (r) => r.fulfill({ status: 200, contentType: 'application/pdf', headers: { 'X-Hash-Contenido': g.hashContenido, 'Access-Control-Expose-Headers': 'X-Hash-Contenido' }, body: '%PDF-sintetico' }), { times: 1 })
  const [d] = await Promise.all([page.waitForEvent('download', { timeout: 15000 }), main.getByRole('button', { name: 'Descargar el documento' }).click()])
  await main.getByRole('status').filter({ hasText: 'Documento descargado:' }).waitFor()
  const contenido = readFileSync(await d.path(), 'utf8')
  if (contenido !== '%PDF-sintetico') throw new Error(contenido)
  await page.evaluate(() => window.scrollTo(0, 0))
  await capturar('estados')
  return `${post.url} · archivo ${d.suggestedFilename()} (${contenido.length} bytes, hash ${g.hashContenido})`
})

// --- Configuración ---
await paso('configuracion', 'aceptado', 'inicio con tres accesos con ícono que navegan', async () => {
  await recargarVista('/configuracion')
  const accesos = main.locator('.accesos a')
  if ((await accesos.count()) !== 3) throw new Error(String(await accesos.count()))
  await capturar('configuracion')
  await accesos.filter({ hasText: 'Tarifario' }).click()
  await page.waitForURL(/\/configuracion\/tarifario$/)
})
await probarListado({ vista: 'tarifario', ruta: '/configuracion/tarifario', recurso: 'tarifarios', buscar: { texto: 'tarifa general', esperadas: 1 }, local: { etiqueta: 'Moneda', opcion: 'USD' }, servidor: { etiqueta: 'Estado', opcion: 'Vigente', valor: 'VIGENTE' } })
await probarListado({ vista: 'mensajeria-plantillas', ruta: '/configuracion/mensajeria', recurso: 'plantillas', buscar: { texto: 'PLANTILLA_7', esperadas: 1 }, local: { etiqueta: 'Canal', opcion: 'SMS' }, servidor: { etiqueta: 'Aprobación', opcion: 'Rechazada', valor: 'RECHAZADA' } })
await probarListado({ vista: 'mensajeria-envios', ruta: '/configuracion/mensajeria', recurso: 'envios', antes: pestana('Envíos'), buscar: { texto: 'E-EJEMPLO', esperadas: 2 }, local: { etiqueta: 'Proveedor', opcion: 'PROVEEDOR_A' }, servidor: { etiqueta: 'Estado', opcion: 'Fallido', valor: 'FALLIDO' }, exportar: '0.0125 USD' })
await probarListado({ vista: 'reportes-catalogo', ruta: '/configuracion/reportes', recurso: 'definiciones', buscar: { texto: 'reporte 9', esperadas: 1 }, local: { etiqueta: 'Tipo', opcion: 'FINANCIERO' } })
await probarListado({ vista: 'reportes-ejecuciones', ruta: '/configuracion/reportes', recurso: 'ejecuciones', antes: pestana('Historial de ejecuciones'), buscar: { texto: 'reporte 2', esperadas: 2 }, servidor: { etiqueta: 'Estado', opcion: 'Fallida', valor: 'FALLIDA' } })

const red = errores.filter((e) => e.tipo === 'red')
const resumen = { pasos: resultados.length, fallos: resultados.filter((r) => r.veredicto === 'FAIL').length, erroresDeRed: red.length, provocados: esperados, consola: errores.filter((e) => e.tipo !== 'red') }
writeFileSync(`${capturas}/recorrido.json`, JSON.stringify({ resumen, resultados, errores, mutaciones: pedidos.filter((p) => p.metodo !== 'GET') }, null, 1))
console.log(JSON.stringify(resumen, null, 1))
await navegador.close()
