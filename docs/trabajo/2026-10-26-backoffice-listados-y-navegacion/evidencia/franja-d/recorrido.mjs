// Recorrido en navegador de la franja D (Sistemas). DOBLE DE SESIÓN como `e2e/apoyo/sonda-de-navegacion.mjs`, con rol
// PLATAFORMA. Modo «contrato»: el simulado OpenAPI en 4010 (sin estado) + colecciones grandes servidas con page.route
// (declaradas como doble). Modo «demo»: <meta name="aportaya-demo" content="true"> → proveedor-fuentes.demo.ts.
// Uso: node recorrido.mjs  →  resultados.json + capturas en la carpeta de evidencia.
import { createRequire } from 'node:module'
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'
const { chromium } = createRequire('/home/user/wt-d/apps/backoffice/package.json')('playwright')

const BASE = 'http://127.0.0.1:4304'
const EVID = '/home/user/wt-d/docs/trabajo/2026-10-26-backoffice-listados-y-navegacion/evidencia/franja-d'
const CAPT = `${EVID}/capturas`
mkdirSync(CAPT, { recursive: true })
const permisos = readFileSync('/tmp/claude-0/-home-user/bfffe767-c9ff-52f5-8524-aca65b5504e0/scratchpad/permisos.txt', 'utf8').split(/\s+/).filter(Boolean)
const soloCapturas = process.argv.includes('--capturas')
const soloPruebas = process.argv.includes('--pruebas')

process.on('unhandledRejection', (e) => console.log('· promesa huérfana de un caso que ya falló:', String(e?.message ?? e).split('\n')[0].slice(0, 120)))
const resultados = []
const red = []
const consola = []
function registrar(vista, nivel, caso, ok, detalle = '') {
  resultados.push({ vista, nivel, caso, veredicto: ok ? 'PASS' : 'FAIL', detalle })
  console.log(`${ok ? 'PASS' : 'FAIL'} · ${vista} · ${nivel} · ${caso}${detalle ? ' · ' + detalle : ''}`)
}
const solo = process.argv.find((a) => a.startsWith('--solo='))?.slice(7)
async function caso(vista, nivel, nombre, fn) {
  if (solo && !vista.includes(solo)) return
  try {
    const d = await fn()
    registrar(vista, nivel, nombre, true, typeof d === 'string' ? d : '')
  } catch (e) {
    registrar(vista, nivel, nombre, false, String(e?.message ?? e).split('\n')[0].slice(0, 240))
    if (solo && globalThis.paginaActual) await globalThis.paginaActual.screenshot({ path: `/tmp/claude-0/-home-user/bfffe767-c9ff-52f5-8524-aca65b5504e0/scratchpad/franja-d/fallo-${resultados.length}.png` })
  }
}
function afirmar(cond, msg) {
  if (!cond) throw new Error(msg)
}

const uuid = (n) => `00000000-0000-4000-8000-${String(n).padStart(12, '0')}`
const usuarios = (n) =>
  Array.from({ length: n }, (_, i) => ({
    id: uuid(i + 1), usuarioId: uuid(500 + i), codigoPublico: `USR-${String(i + 1).padStart(3, '0')}`,
    nombres: i === 4 ? 'José' : i === 7 ? 'Maximiliana Valentina Esperanza de los Ángeles' : `Nombre${i + 1}`, apellidos: 'Prueba',
    correo: `op${i + 1}@aportaya.test`, telefono: '+59170000000', departamento: i % 2 ? 'RIESGOS' : 'TECNOLOGIA',
    cargo: i === 7 ? 'Coordinadora regional de cumplimiento normativo y prevención' : 'Analista', estado: i % 5 === 0 ? 'SUSPENDIDO' : 'ACTIVO',
    debeCambiarContrasena: false, tieneSegundoFactor: i % 3 !== 0, ...(i === 1 ? { bloqueadoHasta: '2026-10-27T10:00:00Z' } : {}), roles: ['RISK_ANALYST', ...(i % 4 ? [] : ['AUDITOR'])],
  }))
const roles = (n) => Array.from({ length: n }, (_, i) => ({ id: uuid(900 + i), codigo: i === 3 ? 'TREASURY_LEAD' : `ROL_${i + 1}`, nombre: `Rol ${i + 1}`, ambito: 'INTERNO', departamento: i % 5 === 0 ? 'AUDITORIA' : 'OPERACIONES', descripcion: 'Rol sintético', esSistema: i % 2 === 0, permisos: ['ALERTAS_RIESGO_LEER', 'AUDITORIA_LEER'] }))
const permisosSint = (n) => Array.from({ length: n }, (_, i) => ({ id: uuid(700 + i), codigo: `PERMISO_${i + 1}`, descripcion: 'Permiso sintético', recurso: i % 2 ? 'grupos' : 'entregas', accion: 'LEER', nivelRiesgo: ['BAJO', 'MEDIO', 'ALTO', 'CRITICO'][i % 4], exigeMotivo: i % 3 === 0, requiereMfa: i % 5 === 0 }))
const catalogos = (n) => Array.from({ length: n }, (_, i) => ({ codigo: i === 0 ? 'UMBRALES_UIF' : `CATALOGO_${i + 1}`, nombre: i === 0 ? 'Umbrales UIF' : `Catálogo ${i + 1}`, dueno: i % 5 === 0 ? 'Tesorería' : 'Cumplimiento', version: '1.0.0', totalElementos: 4, elementosActivos: 3, vigenteDesde: '2026-01-01T00:00:00Z', estado: i % 2 ? 'EN_REVISION' : 'VIGENTE', descripcion: 'Catálogo sintético de prueba' }))
const elementos = (n) => Array.from({ length: n }, (_, i) => ({ id: `elem-${i + 1}`, catalogoCodigo: 'UMBRALES_UIF', codigo: `ELEM_${i + 1}`, nombre: `Elemento ${i + 1}`, descripcion: 'Elemento sintético', activo: i % 4 !== 0, orden: i + 1, actualizadoEn: '2026-10-18T10:00:00Z' }))
const eventos = (desde, n) => Array.from({ length: n }, (_, i) => ({ id: uuid(3000 + desde + i), secuencia: 900 - desde - i, entidad: i % 2 ? 'USUARIO' : 'GRUPO', entidadId: uuid(1), accion: i === 0 ? 'CAMBIO_PERMISOS' : 'ACTUALIZACION', actorRol: 'ADMIN_PLATAFORMA', origen: 'PANEL_ADMIN', correlationId: uuid(2), hashRegistro: `e3b0c44298fc1c149afbf4c8996fb924${desde + i}`, fechaHora: '2026-10-25T10:00:00Z', motivo: 'Cambio sintético' }))

function token() {
  return `sonda.${Buffer.from(JSON.stringify({ permisos, rol: 'PLATAFORMA', sub: 'franja-d' })).toString('base64url')}.firma`
}

async function abrir(navegador, { demo = false, oscuro = false, ancho = 1280 } = {}) {
  const contexto = await navegador.newContext({ viewport: { width: ancho, height: 900 }, colorScheme: oscuro ? 'dark' : 'light', acceptDownloads: true })
  const page = await contexto.newPage()
  page.on('console', (m) => m.type() === 'error' && consola.push({ url: page.url().replace(BASE, ''), texto: m.text().slice(0, 200) }))
  page.on('pageerror', (e) => consola.push({ url: page.url().replace(BASE, ''), texto: 'pageerror: ' + String(e).slice(0, 200) }))
  page.on('response', (r) => r.url().includes('/api/v1/') && red.push({ m: r.request().method(), url: r.url().replace(BASE, ''), s: r.status(), vista: page.url().replace(BASE, '') }))
  await page.route('**/ingreso', async (route) => {
    const r = await route.fetch()
    const meta = '<meta name="aportaya-gateway" content="/api/v1">' + (demo ? '<meta name="aportaya-demo" content="true">' : '')
    await route.fulfill({ response: r, body: (await r.text()).replace('</head>', meta + '</head>') })
  }, { times: 1 })
  await page.route('**/api/v1/sesiones', async (route) => {
    if (!route.request().postDataJSON()?.factor) return route.continue()
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ sesionId: 's', expiraEn: new Date(Date.now() + 3.6e6).toISOString(), requiereFactorAdicional: false, dispositivoConfiable: true, tokenAcceso: token() }) })
  })
  await page.goto(BASE + '/ingreso')
  await page.getByLabel('Correo o teléfono').fill('71234567')
  await page.getByLabel('Contraseña', { exact: true }).fill('contrasena-e2e')
  await page.getByRole('button', { name: 'Continuar' }).click()
  await page.getByLabel('Código de verificación').fill('123456')
  await page.waitForURL(/\/tablero$/, { timeout: 60000 })
  globalThis.paginaActual = page
  return { contexto, page }
}

/** Navega dentro de la SPA (la sesión vive en memoria: nada de page.goto después de entrar). */
async function ir(page, ruta, titulo) {
  await page.evaluate((u) => {
    history.pushState({}, '', u)
    dispatchEvent(new PopStateEvent('popstate'))
  }, ruta)
  await page.waitForURL((u) => u.pathname === ruta.split('?')[0], { timeout: 30000 })
  if (titulo) await page.getByRole('heading', { level: 1, name: titulo }).waitFor({ timeout: 30000 })
}
const json = (route, cuerpo, status = 200) => route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(cuerpo) })
const resumen = (page, i = 0) => page.locator('ap-listado').nth(i).locator('ap-paginacion .resumen').innerText()
const filas = (page, i = 0) => page.locator('ap-listado').nth(i).locator('tbody tr').count()
async function esperarTexto(loc, re, ms = 15000) {
  await loc.filter({ hasText: re }).first().waitFor({ timeout: ms })
}
async function descargar(page, nombre, i = 0) {
  const [d] = await Promise.all([page.waitForEvent('download'), page.locator('ap-exportar-listado').nth(i).getByRole('button', { name: nombre }).click()])
  return { archivo: d.suggestedFilename(), texto: readFileSync(await d.path(), 'utf8') }
}
async function buscar(page, texto) {
  const caja = page.locator('ap-barra-de-filtros input[type=search]').first()
  await caja.fill(texto)
}
async function capturar(page, nombre) {
  await page.screenshot({ path: `${CAPT}/${nombre}.png`, fullPage: true })
}
/** Desborde horizontal del documento y, si lo hay, los elementos más profundos que se salen del ancho visible. */
async function medirDesborde(page, nombre) {
  const m = await page.evaluate(() => {
    const ancho = document.documentElement.clientWidth
    const fuera = [...document.querySelectorAll('main *')].filter((e) => e.getBoundingClientRect().right > ancho + 1 && ![...e.children].some((h) => h.getBoundingClientRect().right > ancho + 1))
    const nombreDe = (e) => e.tagName.toLowerCase() + (e.className && typeof e.className === 'string' ? '.' + e.className.trim().split(/\s+/).slice(0, 2).join('.') : '')
    return { exceso: document.documentElement.scrollWidth - ancho, culpables: [...new Set(fuera.map((e) => nombreDe(e.closest('[class]') ?? e) + ' < ' + nombreDe(e.parentElement)))].slice(0, 4) }
  })
  registrar(nombre, 'límite', 'sin desborde horizontal del documento', m.exceso <= 0, `exceso ${m.exceso}px${m.culpables.length ? ' · ' + m.culpables.join(' | ') : ''}`)
}

// ------------------------------------------------------------------------------------------------------------------
async function pruebasContrato(navegador) {
  const { contexto, page } = await abrir(navegador)

  // ---- Usuarios internos -----------------------------------------------------------------------------------------
  await caso('Usuarios internos', 'aceptado', 'GET real al simulado 4010 pinta la fila del contrato', async () => {
    const resp = page.waitForResponse((r) => r.url().endsWith('/api/v1/usuarios-internos') && r.request().method() === 'GET')
    await ir(page, '/sistemas/usuarios-internos', 'Usuarios internos')
    const r = await resp
    afirmar(r.status() === 200, `status ${r.status()}`)
    await page.locator('ap-listado tbody tr').first().waitFor()
    afirmar((await filas(page)) === 1, 'esperaba 1 fila del simulado')
    return 'GET /api/v1/usuarios-internos → 200, 1 fila'
  })
  await page.route('**/api/v1/usuarios-internos', (route) => (route.request().method() === 'GET' ? json(route, usuarios(25)) : route.fallback()))
  await caso('Usuarios internos', 'aceptado', 'Actualizar repite el GET; 25 filas paginan a 10 (Página 1 de 3)', async () => {
    const resp = page.waitForRequest((r) => r.url().endsWith('/api/v1/usuarios-internos') && r.method() === 'GET')
    await page.getByRole('button', { name: 'Actualizar' }).click()
    await resp
    await esperarTexto(page.locator('ap-paginacion .resumen'), /Página 1 de 3/)
    afirmar((await filas(page)) === 10, 'no hay 10 filas')
    return await resumen(page)
  })
  await caso('Usuarios internos', 'aceptado', 'Siguiente, Ir a 3 (5 filas), Anterior y filas por página 20', async () => {
    await page.locator('ap-paginacion').getByRole('button', { name: 'Siguiente' }).click()
    await esperarTexto(page.locator('ap-paginacion .resumen'), /Página 2 de 3/)
    await page.getByLabel('Ir a la página').fill('3')
    await page.getByLabel('Ir a la página').press('Enter')
    await esperarTexto(page.locator('ap-paginacion .resumen'), /Página 3 de 3/)
    afirmar((await filas(page)) === 5, `filas ${await filas(page)}`)
    await page.locator('ap-paginacion').getByRole('button', { name: 'Anterior' }).click()
    await esperarTexto(page.locator('ap-paginacion .resumen'), /Página 2 de 3/)
    await page.locator('ap-paginacion select').selectOption('20')
    await esperarTexto(page.locator('ap-paginacion .resumen'), /Página 1 de 2/)
    afirmar((await filas(page)) === 20, 'no hay 20 filas')
    await page.locator('ap-paginacion select').selectOption('10')
    return 'p2 → Ir a 3 (5 filas) → p2 → 20/pág = 2 páginas'
  })
  await caso('Usuarios internos', 'aceptado', 'buscar «jose» (sin acento) encuentra 1 y el CSV baja solo esa fila', async () => {
    await buscar(page, 'jose')
    await esperarTexto(page.locator('ap-listado .resumen'), /^1 de 25 usuarios$/)
    const { archivo, texto } = await descargar(page, 'Descargar CSV')
    const lineas = texto.replace(/^﻿/, '').trim().split(/\r?\n/)
    afirmar(lineas.length === 2 && lineas[1].includes('José Prueba'), `CSV: ${lineas.length} líneas`)
    writeFileSync(`${EVID}/descarga-usuarios-busqueda.csv`, texto)
    return `${archivo}: ${lineas.length} líneas · encabezado «${lineas[0].slice(0, 60)}…»`
  })
  await caso('Usuarios internos', 'aceptado', 'filtro Estado=SUSPENDIDO deja 5 y el JSON baja esas 5', async () => {
    await page.locator('ap-barra-de-filtros').getByRole('button', { name: 'Limpiar filtros' }).click()
    await esperarTexto(page.locator('ap-listado .resumen'), /^25 usuarios$/)
    await page.locator('ap-barra-de-filtros').getByLabel('Estado', { exact: true }).selectOption('SUSPENDIDO')
    await esperarTexto(page.locator('ap-listado .resumen'), /^5 de 25 usuarios$/)
    const { archivo, texto } = await descargar(page, 'Descargar JSON')
    const datos = JSON.parse(texto)
    afirmar(datos.length === 5 && datos.every((d) => d.estado === 'Suspendido'), 'JSON no coincide con el filtro')
    writeFileSync(`${EVID}/descarga-usuarios-suspendidos.json`, texto)
    await page.locator('ap-barra-de-filtros').getByRole('button', { name: 'Limpiar filtros' }).click()
    return `${archivo}: ${datos.length} objetos, todos «Suspendido»`
  })
  await caso('Usuarios internos', 'límite', 'búsqueda sin coincidencias: vacío con «Quitar filtros»; CSV con solo encabezado', async () => {
    await buscar(page, 'zzz-nadie')
    await page.getByRole('button', { name: 'Quitar filtros' }).waitFor()
    const { texto } = await descargar(page, 'Descargar CSV')
    afirmar(texto.replace(/^﻿/, '').trim().split(/\r?\n/).length === 1, 'CSV con filas')
    await page.getByRole('button', { name: 'Quitar filtros' }).click()
    await esperarTexto(page.locator('ap-listado .resumen'), /^25 usuarios$/)
    return 'CSV: 1 línea (encabezado)'
  })
  await caso('Usuarios internos', 'aceptado', 'Editar → PATCH /usuarios-internos/{id} al simulado con departamento, cargo y motivo; cierra y recarga', async () => {
    await page.getByRole('button', { name: 'Editar Nombre2 Prueba' }).click()
    const d = page.getByRole('dialog', { name: 'Editar datos del usuario interno' })
    await d.waitFor()
    await d.getByLabel('Cargo').fill('Jefa de riesgos')
    await d.getByLabel('Motivo (queda en la auditoría)').fill('Ascenso aprobado por RRHH')
    const req = page.waitForRequest((r) => r.method() === 'PATCH' && r.url().endsWith(`/usuarios-internos/${uuid(2)}`))
    const resp = page.waitForResponse((r) => r.request().method() === 'PATCH' && r.url().endsWith(`/usuarios-internos/${uuid(2)}`))
    const recarga = page.waitForRequest((r) => r.method() === 'GET' && r.url().endsWith('/usuarios-internos'))
    await d.getByRole('button', { name: 'Guardar cambios' }).click()
    const cuerpo = (await req).postDataJSON()
    const r = await resp
    await recarga
    await d.waitFor({ state: 'hidden' })
    afirmar(cuerpo.cargo === 'Jefa de riesgos' && cuerpo.motivo === 'Ascenso aprobado por RRHH' && cuerpo.departamento === 'RIESGOS', JSON.stringify(cuerpo))
    afirmar(r.status() === 200, `status ${r.status()}`)
    return `PATCH ${JSON.stringify(cuerpo)} → ${r.status()} (doble sin estado: no persiste)`
  })
  await caso('Usuarios internos', 'aceptado', 'Roles → GET /roles y PUT /usuarios-internos/{id}/roles con la selección y el motivo', async () => {
    await page.getByRole('button', { name: 'Asignar roles a Nombre2 Prueba' }).click()
    const d = page.getByRole('dialog', { name: 'Modificar roles asignados (CU-08)' })
    await d.locator('ap-casilla').first().waitFor()
    await d.locator('ap-casilla').first().click()
    await d.getByLabel('Motivo formal del cambio de roles').fill('Rota a auditoría por seis meses')
    const req = page.waitForRequest((r) => r.method() === 'PUT' && r.url().endsWith(`/usuarios-internos/${uuid(2)}/roles`))
    const resp = page.waitForResponse((r) => r.request().method() === 'PUT' && r.url().endsWith('/roles'))
    await d.getByRole('button', { name: 'Confirmar roles' }).click()
    const cuerpo = (await req).postDataJSON()
    const r = await resp
    await d.waitFor({ state: 'hidden' })
    return `PUT ${JSON.stringify(cuerpo)} → ${r.status()}`
  })
  await caso('Usuarios internos', 'error', 'Suspender con 500: el diálogo queda, conserva el motivo, muestra el código de seguimiento y el botón queda libre; Escape devuelve el foco', async () => {
    await page.route(`**/api/v1/usuarios-internos/${uuid(3)}`, (route) => (route.request().method() === 'PATCH' ? json(route, { codigo: 'X', trazaId: 'traza-e2e-500' }, 500) : route.fallback()), { times: 1 })
    const abridor = page.getByRole('button', { name: 'Suspender a Nombre3 Prueba' })
    await abridor.click()
    const d = page.getByRole('dialog', { name: 'Suspender usuario interno' })
    await d.waitFor()
    await d.getByLabel('Motivo (queda en la auditoría)').fill('Baja por fin de contrato')
    await d.getByRole('button', { name: 'Suspender' }).click()
    await esperarTexto(d.locator('ap-error-de-envio'), /traza-e2e-500/)
    afirmar((await d.getByLabel('Motivo (queda en la auditoría)').inputValue()) === 'Baja por fin de contrato', 'perdió el motivo')
    afirmar(await d.getByRole('button', { name: 'Suspender' }).isEnabled(), 'botón trabado')
    await capturar(page, 'usuarios-error-suspender-1280')
    await page.keyboard.press('Escape')
    await d.waitFor({ state: 'hidden' })
    const foco = await page.evaluate(() => document.activeElement?.getAttribute('aria-label'))
    afirmar(foco === 'Suspender a Nombre3 Prueba', `foco en ${foco}`)
    return `error visible con traza; foco vuelve a «${foco}»`
  })
  await caso('Usuarios internos', 'aceptado', 'Desbloquear solo aparece en la fila bloqueada y manda POST /desbloquear', async () => {
    afirmar((await page.getByRole('button', { name: /^Desbloquear a / }).count()) === 1, 'Desbloquear en más de una fila')
    await page.getByRole('button', { name: 'Desbloquear a Nombre2 Prueba' }).click()
    const d = page.getByRole('dialog', { name: 'Desbloquear usuario interno' })
    await d.getByLabel('Motivo (queda en la auditoría)').fill('ok')
    afirmar(await d.getByRole('button', { name: 'Desbloquear' }).isDisabled(), 'motivo corto habilitó el botón')
    await d.getByLabel('Motivo (queda en la auditoría)').fill('Identidad verificada por teléfono')
    const req = page.waitForRequest((r) => r.method() === 'POST' && r.url().endsWith(`/usuarios-internos/${uuid(2)}/desbloquear`))
    const resp = page.waitForResponse((r) => r.request().method() === 'POST' && r.url().endsWith('/desbloquear'))
    await d.getByRole('button', { name: 'Desbloquear' }).click()
    await req
    const r = await resp
    await d.waitFor({ state: 'hidden' })
    return `POST /desbloquear → ${r.status()} · con «ok» el botón estaba deshabilitado`
  })
  await caso('Usuarios internos', 'aceptado', 'Nuevo usuario interno → POST con Idempotency-Key y rol inicial del catálogo', async () => {
    await page.getByRole('button', { name: 'Nuevo usuario interno' }).click()
    const d = page.getByRole('dialog', { name: 'Dar de alta un usuario interno' })
    await d.waitFor()
    await d.getByLabel('Nombres').fill('Ana')
    await d.getByLabel('Apellidos').fill('Prueba')
    await d.getByLabel('Correo institucional').fill('ana.prueba@aportaya.test')
    await d.getByLabel('Teléfono móvil').fill('+59170000001')
    await d.getByLabel('Cargo').fill('Analista')
    const req = page.waitForRequest((r) => r.method() === 'POST' && r.url().endsWith('/usuarios-internos'))
    const resp = page.waitForResponse((r) => r.request().method() === 'POST' && r.url().endsWith('/usuarios-internos'))
    await d.getByRole('button', { name: 'Crear usuario' }).click()
    const rq = await req
    const r = await resp
    await d.waitFor({ state: 'hidden' })
    const cuerpo = rq.postDataJSON()
    afirmar(!!rq.headers()['idempotency-key'], 'sin Idempotency-Key')
    return `POST rolCodigo=${cuerpo.rolCodigo} · Idempotency-Key presente → ${r.status()}`
  })
  await caso('Usuarios internos', 'aceptado', 'Ver navega a la ficha y la ficha pide GET /usuarios-internos/{id}', async () => {
    const resp = page.waitForResponse((r) => r.request().method() === 'GET' && r.url().endsWith(`/usuarios-internos/${uuid(4)}`))
    await page.getByRole('button', { name: 'Ver Nombre4 Prueba' }).click()
    await page.waitForURL(`**/sistemas/usuarios-internos/${uuid(4)}`)
    const r = await resp
    await page.getByRole('heading', { level: 1 }).waitFor()
    return `GET ficha → ${r.status()}`
  })
  await caso('Ficha de usuario interno', 'aceptado', 'Suspender/Reactivar cuenta abre diálogo (no prompt) y manda PATCH estado', async () => {
    let huboPrompt = false
    page.once('dialog', async (dlg) => {
      huboPrompt = true
      await dlg.dismiss()
    })
    const boton = page.getByRole('button', { name: /(Suspender|Reactivar) cuenta/ })
    await boton.click()
    const d = page.getByRole('dialog', { name: /(Suspender|Reactivar) usuario interno/ })
    await d.getByLabel('Motivo (queda en la auditoría)').fill('Licencia prolongada')
    // La ficha viene del simulado (su `id` es el del ejemplo del contrato, no el sintético): el PATCH va a ese id.
    const idFicha = await page.evaluate(() => location.pathname.split('/').pop())
    const req = page.waitForRequest((r) => r.method() === 'PATCH' && /\/usuarios-internos\/[0-9a-f-]{36}$/.test(r.url()))
    await d.getByRole('button', { name: /^(Suspender|Reactivar)$/ }).click()
    const cuerpo = (await req).postDataJSON()
    await d.waitFor({ state: 'hidden' })
    afirmar(!huboPrompt, 'apareció un prompt nativo')
    await capturar(page, 'ficha-usuario-1280')
    return `PATCH ${JSON.stringify(cuerpo)} (ruta de la ficha ${idFicha === undefined ? '' : 'abierta'})`
  })
  await page.unroute('**/api/v1/usuarios-internos')
  await caso('Usuarios internos', 'límite', 'colección vacía: vacío orientado con «Dar de alta»', async () => {
    await page.route('**/api/v1/usuarios-internos', (route) => (route.request().method() === 'GET' ? json(route, []) : route.fallback()), { times: 1 })
    await ir(page, '/sistemas/usuarios-internos', 'Usuarios internos')
    await page.getByText('No se encontraron usuarios internos registrados.').waitFor()
    await page.locator('ap-listado').getByRole('button', { name: 'Dar de alta' }).waitFor()
    return 'vacío + Dar de alta'
  })
  await caso('Usuarios internos', 'error', '403 al listar: mensaje traducido sin cuerpo crudo y «Volver a intentar» repite el GET', async () => {
    await page.route('**/api/v1/usuarios-internos', (route) => (route.request().method() === 'GET' ? json(route, { mensaje: 'AccessDeniedException crudo', trazaId: 'traza-403' }, 403) : route.fallback()), { times: 1 })
    await ir(page, '/sistemas/roles-internos', 'Roles internos')
    await ir(page, '/sistemas/usuarios-internos', 'Usuarios internos')
    await page.locator('ap-estado-de-error').waitFor()
    const txt = await page.locator('ap-estado-de-error').innerText()
    afirmar(!txt.includes('AccessDeniedException'), 'muestra el crudo')
    const otra = page.waitForResponse((r) => r.url().endsWith('/usuarios-internos') && r.request().method() === 'GET')
    await page.getByRole('button', { name: 'Volver a intentar' }).click()
    const r = await otra
    await page.locator('ap-listado tbody tr').first().waitFor()
    return `«${txt.replace(/\s+/g, ' ').slice(0, 90)}» · reintento → ${r.status()}`
  })

  // ---- Roles y permisos ------------------------------------------------------------------------------------------
  await page.route('**/api/v1/roles', (route) => json(route, roles(25)))
  await caso('Roles internos', 'aceptado', '25 roles: buscar por permiso, filtro Departamento, Ver permisos en cajón, CSV', async () => {
    await ir(page, '/sistemas/roles-internos', 'Roles internos')
    await esperarTexto(page.locator('ap-paginacion .resumen'), /Página 1 de 3/)
    await buscar(page, 'treasury')
    await esperarTexto(page.locator('ap-listado .resumen'), /^1 de 25 roles$/)
    await page.locator('ap-barra-de-filtros').getByRole('button', { name: 'Limpiar filtros' }).click()
    await page.locator('ap-barra-de-filtros').getByLabel('Departamento').selectOption('AUDITORIA')
    await esperarTexto(page.locator('ap-listado .resumen'), /^5 de 25 roles$/)
    const { texto } = await descargar(page, 'Descargar CSV')
    afirmar(texto.trim().split(/\r?\n/).length === 6, 'CSV no tiene 5 filas')
    await page.getByRole('button', { name: 'Ver permisos Rol 1', exact: true }).click()
    await page.getByRole('dialog', { name: 'Permisos de Rol 1' }).getByText('AUDITORIA_LEER').waitFor()
    afirmar((await page.locator('tbody').getByRole('button', { name: /^(Editar|Eliminar)/ }).count()) === 0, 'hay Editar/Eliminar')
    await page.keyboard.press('Escape')
    return 'buscar 1/25 · AUDITORIA 5/25 · CSV 5 filas · cajón con permisos · sin Editar/Eliminar'
  })
  await page.route('**/api/v1/permisos', (route) => json(route, permisosSint(25)))
  await caso('Permisos internos', 'aceptado', 'filtro Riesgo=CRITICO y JSON de lo filtrado', async () => {
    await ir(page, '/sistemas/permisos-internos', 'Permisos internos')
    await esperarTexto(page.locator('ap-paginacion .resumen'), /Página 1 de 3/)
    await page.locator('ap-barra-de-filtros').getByLabel('Riesgo').selectOption('CRITICO')
    await esperarTexto(page.locator('ap-listado .resumen'), /^6 de 25 permisos$/)
    const { texto } = await descargar(page, 'Descargar JSON')
    afirmar(JSON.parse(texto).every((p) => p.nivelRiesgo === 'CRITICO'), 'JSON no filtrado')
    return 'CRITICO 6/25 · JSON 6 objetos'
  })
  await page.unroute('**/api/v1/roles')
  await page.unroute('**/api/v1/permisos')

  // ---- Catálogos operativos --------------------------------------------------------------------------------------
  await page.route('**/api/v1/auditoria/catalogos', (route) => json(route, catalogos(25)))
  await page.route('**/api/v1/auditoria/catalogos/UMBRALES_UIF/elementos', (route) => json(route, elementos(14)))
  await caso('Catálogos operativos', 'aceptado', '25 catálogos, filtro Dueño, Ver elementos navega a la ficha', async () => {
    await ir(page, '/sistemas/catalogos-operativos', 'Catálogos operativos')
    await esperarTexto(page.locator('ap-paginacion .resumen'), /Página 1 de 3/)
    await page.locator('ap-barra-de-filtros').getByLabel('Dueño').selectOption('Tesorería')
    await esperarTexto(page.locator('ap-listado .resumen'), /^5 de 25 catálogos$/)
    const resp = page.waitForResponse((r) => r.url().endsWith('/auditoria/catalogos/UMBRALES_UIF'))
    await page.getByRole('button', { name: 'Ver elementos Umbrales UIF' }).click()
    await page.waitForURL('**/sistemas/catalogos-operativos/UMBRALES_UIF')
    const r = await resp
    return `ficha GET → ${r.status()} (simulado)`
  })
  await caso('Ficha de catálogo', 'aceptado', 'elementos paginan (14 → 2 páginas); Desactivar manda PATCH {activo:false, motivo}', async () => {
    await esperarTexto(page.locator('ap-paginacion .resumen'), /Página 1 de 2/)
    await page.getByRole('button', { name: 'Desactivar Elemento 2', exact: true }).click()
    const d = page.getByRole('dialog', { name: 'Cambiar estado del elemento' })
    await d.getByRole('textbox').fill('Resolución del comité de riesgos')
    const req = page.waitForRequest((r) => r.method() === 'PATCH' && r.url().endsWith('/auditoria/catalogos/UMBRALES_UIF/elementos/elem-2'))
    const resp = page.waitForResponse((r) => r.request().method() === 'PATCH' && r.url().includes('/elementos/'))
    await d.getByRole('button', { name: 'Desactivar elemento' }).click()
    const cuerpo = (await req).postDataJSON()
    const r = await resp
    await d.waitFor({ state: 'hidden' })
    afirmar(cuerpo.activo === false, JSON.stringify(cuerpo))
    await capturar(page, 'ficha-catalogo-1280')
    return `PATCH ${JSON.stringify(cuerpo)} → ${r.status()}`
  })
  await caso('Ficha de catálogo', 'error', 'PATCH con 403: conserva el motivo y muestra el error', async () => {
    await page.route('**/api/v1/auditoria/catalogos/UMBRALES_UIF/elementos/elem-1', (route) => json(route, { codigo: 'X', trazaId: 'traza-403-cat' }, 403), { times: 1 })
    await page.getByRole('button', { name: 'Activar Elemento 1', exact: true }).click()
    const d = page.getByRole('dialog', { name: 'Cambiar estado del elemento' })
    await d.getByRole('textbox').fill('Reactivación por norma nueva')
    await d.getByRole('button', { name: 'Activar elemento' }).click()
    await esperarTexto(d.locator('ap-error-de-envio'), /traza-403-cat/)
    afirmar((await d.getByRole('textbox').inputValue()) === 'Reactivación por norma nueva', 'perdió el motivo')
    await d.getByRole('button', { name: 'Cancelar' }).click()
    return 'error con traza, motivo intacto'
  })
  await page.unroute('**/api/v1/auditoria/catalogos')
  await page.unroute('**/api/v1/auditoria/catalogos/UMBRALES_UIF/elementos')

  // ---- Bitácora --------------------------------------------------------------------------------------------------
  await page.route('**/api/v1/auditoria/bitacora?*', (route) => {
    const q = new URL(route.request().url()).searchParams
    const desde = q.get('antesDeId') ? Number(q.get('antesDeId').split('-').pop()) : 0
    const n = desde >= 50 ? 10 : 25
    return json(route, { items: eventos(desde, n), totalAproximado: 60, ...(desde + n < 60 ? { proximoAntesDe: '2026-10-24T00:00:00Z', proximoAntesDeId: `c-${desde + n}` } : {}) })
  })
  await caso('Registro del sistema', 'aceptado', 'Siguiente manda antesDe/antesDeId; Ir a 3 recorre la 2 y llega a la 3 (10 eventos)', async () => {
    const r1 = page.waitForRequest((r) => r.url().includes('/auditoria/bitacora?'))
    await ir(page, '/sistemas/bitacora', 'Registro del sistema')
    const q1 = new URL((await r1).url()).searchParams
    afirmar(q1.get('limite') === '25' && !q1.has('antesDe'), q1.toString())
    await esperarTexto(page.locator('ap-paginacion .resumen'), /Página 1 de 3/)
    const r2 = page.waitForRequest((r) => r.url().includes('antesDeId=c-25'))
    await page.locator('ap-paginacion').getByRole('button', { name: 'Siguiente' }).click()
    await r2
    await esperarTexto(page.locator('ap-paginacion .resumen'), /Página 2 de 3/)
    await page.locator('ap-paginacion').getByRole('button', { name: 'Anterior' }).click()
    await esperarTexto(page.locator('ap-paginacion .resumen'), /Página 1 de 3/)
    const r3 = page.waitForRequest((r) => r.url().includes('antesDeId=c-50'))
    await page.getByLabel('Ir a la página').fill('3')
    await page.getByLabel('Ir a la página').press('Enter')
    await r3
    await esperarTexto(page.locator('ap-paginacion .resumen'), /Página 3 de 3/)
    afirmar((await page.locator('tbody tr').count()) === 10, 'la 3 no tiene 10')
    return `1ª: ${q1.toString()} · Siguiente: antesDeId=c-25 · Ir a 3: antesDeId=c-50`
  })
  await caso('Registro del sistema', 'aceptado', 'filtro Entidad viaja como entidad= y vuelve a la página 1; Ver detalle abre el cajón; sin Eliminar', async () => {
    const r = page.waitForRequest((r) => r.url().includes('entidad=USUARIO'))
    await page.locator('ap-barra-de-filtros').getByLabel('Entidad', { exact: true }).selectOption('USUARIO')
    const q = new URL((await r).url()).searchParams
    afirmar(!q.has('antesDeId'), 'no volvió a la 1')
    await esperarTexto(page.locator('ap-paginacion .resumen'), /Página 1 de/)
    await page.locator('tbody').getByRole('button', { name: /^Ver detalle/ }).first().click()
    await page.getByRole('dialog', { name: /Detalle del evento/ }).getByText('Hash del registro', { exact: true }).first().waitFor()
    afirmar((await page.locator('tbody').getByRole('button', { name: /^(Editar|Eliminar)/ }).count()) === 0, 'Editar/Eliminar en bitácora')
    await page.keyboard.press('Escape')
    const { texto } = await descargar(page, 'Descargar CSV')
    writeFileSync(`${EVID}/descarga-bitacora-pagina.csv`, texto)
    return `${q.toString()} · cajón ok · CSV ${texto.trim().split(/\r?\n/).length - 1} filas`
  })
  await caso('Registro del sistema', 'error', '500: error traducido y Volver a intentar repite el GET', async () => {
    await page.unroute('**/api/v1/auditoria/bitacora?*')
    await page.route('**/api/v1/auditoria/bitacora?*', (route) => json(route, { mensaje: 'org.postgresql crudo' }, 500), { times: 1 })
    await page.locator('ap-barra-de-filtros').getByLabel('Entidad', { exact: true }).selectOption('GRUPO')
    await page.locator('ap-estado-de-error').waitFor()
    afirmar(!(await page.locator('ap-estado-de-error').innerText()).includes('postgresql'), 'crudo')
    const r = page.waitForResponse((r) => r.url().includes('/auditoria/bitacora?'))
    await page.getByRole('button', { name: 'Volver a intentar' }).click()
    return `reintento → ${(await r).status()} (simulado)`
  })

  // ---- Vistas de puerto en modo «producción» (sin la etiqueta demo) --------------------------------------------
  await caso('Servicios y SLO', 'error', 'sin fuente real: error accionable «todavía no existe un origen de datos real» con Volver a intentar', async () => {
    await ir(page, '/sistemas/servicios', 'Estado de servicios')
    await page.locator('ap-estado-de-error').waitFor()
    const t = await page.locator('ap-estado-de-error').innerText()
    afirmar(/origen de datos real/.test(t), t)
    await page.getByRole('button', { name: 'Volver a intentar' }).click()
    await page.locator('ap-estado-de-error').waitFor()
    await capturar(page, 'servicios-sin-fuente-1280')
    return t.replace(/\s+/g, ' ').slice(0, 120)
  })
  await contexto.close()
}

// ------------------------------------------------------------------------------------------------------------------
const PUERTOS = [
  ['/sistemas/servicios', 'Estado de servicios', 'servicios'],
  ['/sistemas/despliegues', 'Despliegues e interruptores', 'despliegues'],
  ['/sistemas/base-de-datos', 'Base y migraciones', 'base'],
  ['/sistemas/respaldos', 'Respaldos', 'respaldos'],
  ['/sistemas/proveedores', 'Proveedores', 'proveedores'],
  ['/sistemas/outbox', 'Outbox y descartados', 'outbox'],
  ['/sistemas/webhooks', 'Webhooks', 'webhooks'],
  ['/sistemas/accesos', 'Accesos', 'accesos'],
  ['/sistemas/incidentes', 'Incidentes', 'incidentes'],
]
async function pruebasDemo(navegador) {
  const { contexto, page } = await abrir(navegador, { demo: true })
  for (const [ruta, titulo] of PUERTOS) {
    await caso(titulo, 'aceptado', 'modo demo: banner de datos de ejemplo, cifras y listado con filas, CSV de lo visible', async () => {
      await ir(page, ruta, titulo)
      await page.locator('ap-listado tbody tr').first().waitFor()
      await page.locator('ap-banner-datos-de-ejemplo').filter({ hasText: /ejemplo/i }).waitFor()
      const n = await page.locator('ap-listado').first().locator('tbody tr').count()
      const { texto } = await descargar(page, 'Descargar CSV')
      const lineas = texto.replace(/^﻿/, '').trim().split(/\r?\n/).length - 1
      afirmar(lineas === n, `CSV ${lineas} vs tabla ${n}`)
      return `${n} filas · CSV ${lineas} filas · cifras: ${(await page.locator('ap-cifras-de-sistemas').innerText()).replace(/\s+/g, ' ').slice(0, 80)}`
    })
  }
  await caso('Despliegues e interruptores', 'aceptado', 'interruptor que toca dinero: no se activa directo; pedirlo abre la doble confirmación, el mismo correo no confirma', async () => {
    await ir(page, '/sistemas/despliegues', 'Despliegues e interruptores')
    const pedir = page.getByRole('button', { name: /\(toca dinero\)/ }).first()
    await pedir.click()
    const d = page.getByRole('dialog', { name: 'Este interruptor toca dinero' })
    await d.getByLabel('Correo de quien confirma').fill('operador.actual@aportaya.bo')
    afirmar(await d.getByRole('button', { name: 'Confirmar el cambio (segunda persona)' }).isDisabled(), 'mismo correo habilitó confirmar')
    await d.getByRole('button', { name: 'Cancelar' }).click()
    return 'confirmar deshabilitado con el mismo correo'
  })
  await caso('Respaldos', 'aceptado', 'filtro Restauración=Vencida deja solo los VENCIDA', async () => {
    await ir(page, '/sistemas/respaldos', 'Respaldos')
    await page.locator('ap-barra-de-filtros').getByLabel('Restauración').selectOption('vencida')
    const n = await page.locator('tbody tr').count()
    const vencidas = await page.locator('tbody tr', { hasText: 'VENCIDA' }).count()
    afirmar(n === vencidas, `${vencidas}/${n}`)
    return `${n} filas, todas VENCIDA`
  })
  await caso('Outbox y descartados', 'aceptado', 'dos listados con nombres distintos; descartados con su motivo; sin Editar/Eliminar', async () => {
    await ir(page, '/sistemas/outbox', 'Outbox y descartados')
    afirmar((await page.locator('ap-listado').count()) === 2, 'no hay dos listados')
    afirmar((await page.getByRole('button', { name: /^(Editar|Eliminar)/ }).count()) === 0, 'hay Editar/Eliminar')
    return (await page.locator('ap-listado').nth(1).locator('tbody').innerText()).replace(/\s+/g, ' ').slice(0, 100)
  })
  await contexto.close()
}

// ------------------------------------------------------------------------------------------------------------------
async function capturas(navegador) {
  const vistasContrato = [
    ['/sistemas/usuarios-internos', 'Usuarios internos', 'usuarios', { '**/api/v1/usuarios-internos': () => usuarios(25) }],
    ['/sistemas/roles-internos', 'Roles internos', 'roles', { '**/api/v1/roles': () => roles(25) }],
    ['/sistemas/permisos-internos', 'Permisos internos', 'permisos', { '**/api/v1/permisos': () => permisosSint(25) }],
    ['/sistemas/catalogos-operativos', 'Catálogos operativos', 'catalogos', { '**/api/v1/auditoria/catalogos': () => catalogos(25) }],
    ['/sistemas/catalogos-operativos/UMBRALES_UIF', 'nombre', 'catalogo-ficha', { '**/api/v1/auditoria/catalogos/UMBRALES_UIF/elementos': () => elementos(14) }],
    ['/sistemas/bitacora', 'Registro del sistema', 'bitacora', { '**/api/v1/auditoria/bitacora?*': () => ({ items: eventos(0, 25), totalAproximado: 60, proximoAntesDe: '2026-10-24T00:00:00Z', proximoAntesDeId: 'c-25' }) }],
    ['/sistemas/usuarios-internos/' + uuid(1), 'nombres apellidos', 'usuario-ficha', {}],
  ]
  const anchos = [[1280, false], [768, false], [375, false], [1280, true]]
  for (const [ancho, oscuro] of anchos) {
    const sufijo = `${ancho}${oscuro ? '-oscuro' : ''}`
    for (const demo of [false, true]) {
      const { contexto, page } = await abrir(navegador, { demo, oscuro, ancho })
      if (!demo) {
        for (const [ruta, titulo, nombre, rutas] of vistasContrato) {
          for (const [patron, datos] of Object.entries(rutas)) await page.route(patron, (route) => (route.request().method() === 'GET' ? json(route, datos()) : route.fallback()))
          await ir(page, ruta, titulo)
          await page.locator('tbody tr, ap-seccion-clave-valor').first().waitFor({ timeout: 30000 })
          await capturar(page, `${nombre}-${sufijo}`)
          await medirDesborde(page, `${nombre}-${sufijo}`)
          for (const patron of Object.keys(rutas)) await page.unroute(patron)
        }
      } else {
        for (const [ruta, titulo, nombre] of PUERTOS) {
          await ir(page, ruta, titulo)
          await page.locator('ap-listado tbody tr').first().waitFor({ timeout: 30000 })
          await capturar(page, `${nombre}-${sufijo}`)
          await medirDesborde(page, `${nombre}-${sufijo}`)
        }
      }
      await contexto.close()
    }
  }
}

const navegador = await chromium.launch({ args: ['--disable-features=LocalNetworkAccessChecks'] })
try {
  if (!soloCapturas) {
    await pruebasContrato(navegador)
    await pruebasDemo(navegador)
  }
  if (!soloPruebas) await capturas(navegador)
} finally {
  await navegador.close()
  const errores4xx5xx = red.filter((r) => r.s >= 400)
  writeFileSync(`${EVID}/recorrido-${soloCapturas ? 'capturas' : 'pruebas'}.json`, JSON.stringify({ resultados, consola, red_4xx_5xx: errores4xx5xx, peticiones: red.length }, null, 1))
  console.log(JSON.stringify({ casos: resultados.length, pass: resultados.filter((r) => r.veredicto === 'PASS').length, fail: resultados.filter((r) => r.veredicto === 'FAIL').length, consola: consola.length, red4xx5xx: errores4xx5xx.length }))
}
