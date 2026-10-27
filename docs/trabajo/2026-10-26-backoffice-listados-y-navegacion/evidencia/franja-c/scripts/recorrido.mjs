// Recorrido en navegador de la franja C (cumplimiento). Datos SINTÉTICOS servidos con page.route (doble declarado):
// el simulado devuelve una fila y siempre un cursor, así que para aceptado/límite/error se arma la respuesta acá.
// Las mutaciones se mandan al simulado (Prism, sin estado): «verificadas contra el doble», nunca «persistidas».
import { readFileSync, writeFileSync } from 'node:fs'
import { abrir, ir } from './sesion.mjs'

const salida = process.argv[2] ?? 'recorrido.json'
const resultados = []
const ok = (vista, nivel, caso, cond, detalle = '') => {
  resultados.push({ vista, nivel, caso, veredicto: cond ? 'PASS' : 'FAIL', detalle })
  console.log(`${cond ? 'PASS' : 'FAIL'} · ${vista} · ${nivel} · ${caso}${detalle ? ' · ' + detalle : ''}`)
}

const { navegador, page, errores } = await abrir()
const pedidos = []
page.on('request', (r) => r.url().includes('/api/v1/') && pedidos.push({ m: r.method(), u: decodeURIComponent(r.url().replace(/^.*\/api\/v1/, '')), cuerpo: r.postData() }))

/** Sirve la bandeja por cursor: `paginas` de filas; la 1.ª sin cursor, la siguiente con el par que devolvió la anterior. */
function bandeja(patron, { paginas, campoFecha, campoId, parametroId, estado = 200 }) {
  const control = { estado, paginas }
  page.route(patron, async (route) => {
    if (route.request().method() !== 'GET') return route.fallback()
    if (control.estado !== 200) return route.fulfill({ status: control.estado, json: { codigo: 'AP-X', mensaje: 'crudo del servidor', trazaId: 'traza-e2e-' + control.estado } })
    const u = new URL(route.request().url())
    const n = u.searchParams.has(parametroId) ? Number(u.searchParams.get(parametroId).split('-').pop()) : 0
    const items = control.paginas[n] ?? []
    const hay = n + 1 < control.paginas.length
    await route.fulfill({ json: { items, [campoFecha]: hay ? '2026-01-01T00:00:00Z' : null, [campoId]: hay ? `cursor-${n + 1}` : null } })
  })
  return control
}

async function esperarTexto(texto, timeout = 15000) {
  await page.locator('main#contenido').getByText(texto, { exact: false }).first().waitFor({ timeout })
}
async function descargar(boton) {
  const [d] = await Promise.all([page.waitForEvent('download'), page.getByRole('button', { name: boton }).click()])
  const ruta = await d.path()
  return { nombre: d.suggestedFilename(), contenido: readFileSync(ruta, 'utf8') }
}
const ultimo = (re) => [...pedidos].reverse().find((p) => re.test(p.u))
const capturas = async (nombre) => {
  const dir = '/home/user/wt-c/docs/trabajo/2026-10-26-backoffice-listados-y-navegacion/evidencia/franja-c'
  await page.mouse.move(0, 0)
  for (const [ancho, oscuro] of [[1280, false], [768, false], [375, false], [1280, true]]) {
    await page.setViewportSize({ width: ancho, height: 900 })
    await page.emulateMedia({ colorScheme: oscuro ? 'dark' : 'light' })
    await page.evaluate(() => new Promise((r) => requestAnimationFrame(() => requestAnimationFrame(r))))
    await page.screenshot({ path: `${dir}/${nombre}-${ancho}${oscuro ? '-oscuro' : ''}.png`, fullPage: true })
  }
  await page.setViewportSize({ width: 1280, height: 900 })
  await page.emulateMedia({ colorScheme: 'light' })
}

const iso = (d) => `2026-10-${String(d).padStart(2, '0')}T14:00:00Z`
const BANDEJAS = [
  {
    vista: 'alertas', ruta: '/cumplimiento/alertas', patron: '**/api/v1/cumplimiento/alertas?*', forma: { campoFecha: 'proximoFecha', campoId: 'proximoId', parametroId: 'despuesDeId' },
    fila: (i) => ({ alertaId: `a-${i}`, reglaCodigo: `R-${i}`, tipologia: i === 2 ? 'Fraccionamiento con un nombre de tipología muy largo que no debería romper la grilla a 375 px' : 'Persona expuesta', severidad: i % 2 ? 'ALTA' : 'CRITICA', estado: 'ABIERTA', detectadaEn: iso(i), casoId: null }),
    filtro: ['Estado', 'ESCALADA', /estados=ESCALADA/], busqueda: ['R-3', 1], accion: { nombre: 'Abrir caso con la alerta R-1', dialogo: 'Abrir un caso de investigación' }, vacio: 'No hay alertas en estos estados.',
  },
  {
    vista: 'casos', ruta: '/cumplimiento/casos', patron: '**/api/v1/cumplimiento/casos?*', forma: { campoFecha: 'proximoPlazo', campoId: 'proximoId', parametroId: 'despuesDeId' },
    fila: (i) => ({ casoId: `c-${i}`, codigo: `LFT-${i}`, estado: 'EN_ANALISIS', prioridad: 'ALTA', plazoLimite: iso(i), vencido: i === 1, decision: null }),
    filtro: ['Estado', 'CERRADO', /estados=CERRADO/], busqueda: ['LFT-3', 1], accion: { nombre: 'Decidir el caso LFT-1', dialogo: 'Decidir el caso LFT-1' }, vacio: 'No hay casos en estos estados.',
  },
  {
    vista: 'reportes-uif', ruta: '/cumplimiento/uif/reportes', patron: '**/api/v1/uif/reportes?*', forma: { campoFecha: 'proximoAntesDe', campoId: 'proximoAntesDeId', parametroId: 'antesDeId' },
    fila: (i) => ({ reporteId: `r-${i}`, catalogoReporteId: 'c', periodo: `2026-0${i}`, fechaCorte: '2026-09-30', fechaLimite: '2026-10-10', estado: i === 1 ? 'GENERADO' : 'ENVIADO', cantidadRegistros: i * 3, reporteEnCero: false, montoTotal: `${i}000.00` }),
    filtro: ['Estado', 'ENVIADO', /estados=ENVIADO/], busqueda: ['2026-03', 1], accion: { nombre: 'Aprobar y registrar envío del período 2026-01', dialogo: 'Aprobar y registrar el envío de 2026-01' }, vacio: 'No hay reportes en estos estados.',
  },
  {
    vista: 'requerimientos', ruta: '/cumplimiento/uif/requerimientos', patron: '**/api/v1/uif/requerimientos?*', forma: { campoFecha: 'proximoAntesDe', campoId: 'proximoAntesDeId', parametroId: 'antesDeId' },
    fila: (i) => ({ requerimientoId: `q-${i}`, autoridad: 'FISCALIA', numeroOficio: `OF-${i}`, fechaRecepcion: iso(i), plazoRespuesta: iso(i + 5), estado: 'RECIBIDO', conBloqueoDeSaldo: i === 1 }),
    filtro: ['Estado', 'VENCIDO', /estados=VENCIDO/], busqueda: ['OF-3', 1], accion: { nombre: 'Responder el oficio OF-1', dialogo: 'OF-1' }, vacio: 'No hay requerimientos en estos estados.',
  },
  {
    vista: 'continuidad', ruta: '/cumplimiento/continuidad', patron: '**/api/v1/cumplimiento/continuidad/pruebas?*', forma: { campoFecha: 'proximoAntesDe', campoId: 'proximoAntesDeId', parametroId: 'antesDeId' },
    fila: (i) => ({ pruebaId: `p-${i}`, fecha: '2026-09-01', tipo: 'PARCIAL', rtoMinutos: i * 10, rpoMinutos: i, resultado: 'EXITOSA', actaComiteId: null }),
    busqueda: ['30', 1], vacio: 'Todavía no hay pruebas de continuidad registradas.', pantalla: { boton: 'Registrar una prueba', dialogo: 'Registrar una prueba de continuidad' },
  },
  {
    vista: 'actas', ruta: '/cumplimiento/actas', patron: '**/api/v1/cumplimiento/comites/actas?*', forma: { campoFecha: 'proximoAntesDe', campoId: 'proximoAntesDeId', parametroId: 'antesDeId' },
    fila: (i) => ({ actaId: `ac-${i}`, comiteGobiernoId: 'g', numero: `ACTA-${i}`, fecha: '2026-09-01', cumpleQuorum: i !== 2, hashDocumento: 'f'.repeat(64) }),
    busqueda: ['ACTA-3', 1], vacio: 'Todavía no hay actas de comité registradas.',
  },
  {
    vista: 'eventos', ruta: '/cumplimiento/riesgos/eventos', patron: '**/api/v1/cumplimiento/riesgos/eventos?*', forma: { campoFecha: 'proximoAntesDe', campoId: 'proximoAntesDeId', parametroId: 'antesDeId' },
    fila: (i) => ({ eventoId: `e-${i}`, codigo: `EV-${i}`, categoria: 'FRAUDE_EXTERNO', factor: 'f', lineaNegocio: 'l', fechaOcurrencia: iso(i), fechaDeteccion: iso(i), perdidaBruta: { monto: `${i}00.00`, moneda: 'BOB' }, recuperacion: { monto: '0.00', moneda: 'BOB' }, perdidaNeta: { monto: `${i}00.00`, moneda: 'BOB' }, estado: 'REGISTRADO', reportadoCentral: false }),
    filtro: ['Estado', 'CERRADO', /estados=CERRADO/], busqueda: ['EV-3', 1], vacio: 'No hay eventos de riesgo en estos estados.',
  },
  {
    vista: 'incidentes', ruta: '/cumplimiento/seguridad/incidentes', patron: '**/api/v1/cumplimiento/seguridad/incidentes?*', forma: { campoFecha: 'proximoAntesDe', campoId: 'proximoAntesDeId', parametroId: 'antesDeId' },
    fila: (i) => ({ incidenteId: `i-${i}`, codigo: `INC-${i}`, tipo: 'ACCESO_NO_AUTORIZADO', severidad: 'ALTA', detectadoEn: iso(i), plazoReporte: iso(i + 1), reportadoAlOrganismoEn: null, estado: 'DETECTADO' }),
    filtro: ['Estado', 'CERRADO', /estados=CERRADO/], busqueda: ['INC-3', 1], vacio: 'No hay incidentes en estos estados.',
  },
]

for (const b of BANDEJAS) {
  const v = b.vista
  // Tres páginas de 3 filas para el aceptado (con 10 por página → limite=10 en la consulta).
  const control = bandeja(b.patron, { ...b.forma, paginas: [[1, 2, 3].map(b.fila), [4, 5, 6].map(b.fila), [7].map(b.fila)] })
  pedidos.length = 0
  await ir(page, b.ruta)
  await page.getByRole('table').first().waitFor({ timeout: 30000 })
  ok(v, 'aceptado', 'lista con limite=25 y sin cursor', /limite=25/.test(ultimo(/\?limite/)?.u ?? '') && !/Id=/.test(ultimo(/\?limite/)?.u ?? ''), ultimo(/\?limite/)?.u)
  // Buscar (en la página cargada)
  await page.getByRole('searchbox').fill(b.busqueda[0])
  await page.waitForFunction((n) => document.querySelectorAll('main#contenido tbody tr').length === n, b.busqueda[1])
  ok(v, 'aceptado', `buscar «${b.busqueda[0]}» deja ${b.busqueda[1]} fila`, true)
  const csv = await descargar('Descargar CSV')
  ok(v, 'aceptado', 'CSV baja lo filtrado', csv.nombre.endsWith('.csv') && csv.contenido.trim().split(/\r\n/).length === 2 && csv.contenido.includes(b.busqueda[0]), `${csv.nombre} · ${csv.contenido.trim().split(/\r\n/).length} líneas`)
  const json = await descargar('Descargar JSON')
  ok(v, 'aceptado', 'JSON baja lo filtrado', JSON.parse(json.contenido).length === 1, json.nombre)
  // Límite: búsqueda sin coincidencias → «Quitar filtros»
  await page.getByRole('searchbox').fill('zzz-sin-coincidencias')
  await esperarTexto('Ningún registro coincide')
  const csvVacio = await descargar('Descargar CSV')
  ok(v, 'límite', 'sin coincidencias: vacío orientado y CSV solo con encabezado', csvVacio.contenido.trim().split(/\r\n/).length === 1)
  await page.getByRole('button', { name: 'Quitar filtros' }).click()
  await page.waitForFunction(() => document.querySelectorAll('main#contenido tbody tr').length === 3)
  ok(v, 'límite', '«Quitar filtros» vuelve a mostrar la página', true)
  // Paginación: Siguiente pide con cursor; Anterior vuelve; filas por página → limite=10; Ir a 3 recorre cursores.
  await page.getByRole('button', { name: 'Siguiente' }).click()
  await esperarTexto('en la página 2')
  ok(v, 'aceptado', 'Siguiente pide la página 2 con el cursor del servidor', new RegExp(b.forma.parametroId + '=.*-1').test(ultimo(/\?limite/).u), ultimo(/\?limite/).u)
  await page.getByRole('button', { name: 'Anterior' }).click()
  await esperarTexto('en la página 1')
  ok(v, 'aceptado', 'Anterior vuelve a la página 1', true)
  await page.getByLabel('Filas por página').selectOption('10')
  await page.waitForFunction(() => /en la página 1/.test(document.querySelector('main#contenido')?.innerText ?? ''))
  ok(v, 'aceptado', 'filas por página 10 → limite=10', /limite=10/.test(ultimo(/\?limite/).u), ultimo(/\?limite/).u)
  await page.getByRole('button', { name: 'Siguiente' }).click()
  await esperarTexto('en la página 2')
  await page.getByLabel('Ir a la página').fill('3')
  await page.getByLabel('Ir a la página').press('Enter')
  await page.waitForFunction(() => /en la página 3/.test(document.querySelector('main#contenido')?.innerText ?? ''), null, { timeout: 15000 })
  const ultimaParcial = await page.locator('main#contenido tbody tr').count()
  ok(v, 'límite', 'Ir a 3 → última página parcial (1 fila) y Siguiente deshabilitado', ultimaParcial === 1 && (await page.getByRole('button', { name: 'Siguiente' }).isDisabled()))
  // Filtro de servidor
  if (b.filtro) {
    await page.getByLabel(b.filtro[0], { exact: true }).selectOption(b.filtro[1])
    await page.waitForFunction(() => /en la página 1/.test(document.querySelector('main#contenido')?.innerText ?? ''))
    ok(v, 'aceptado', `filtro ${b.filtro[0]} viaja al servidor y vuelve a la página 1`, b.filtro[2].test(ultimo(/\?limite/).u) && !/Id=/.test(ultimo(/\?limite/).u), ultimo(/\?limite/).u)
    await page.getByRole('button', { name: 'Limpiar filtros' }).click()
    await page.waitForFunction(() => document.querySelectorAll('main#contenido tbody tr').length > 0)
  }
  // Acción de fila de dominio o acción de pantalla: abre su diálogo de verdad.
  const accion = b.accion ?? b.pantalla
  if (accion) {
    const boton = b.accion ? page.getByRole('button', { name: b.accion.nombre }) : page.getByRole('button', { name: b.pantalla.boton })
    await boton.click()
    await page.getByRole('dialog').getByText(accion.dialogo).first().waitFor({ timeout: 10000 })
    ok(v, 'aceptado', `«${b.accion?.nombre ?? b.pantalla.boton}» abre su diálogo`, true)
    await page.keyboard.press('Escape')
    await page.getByRole('dialog').waitFor({ state: 'hidden', timeout: 10000 }).catch(() => {})
  }
  ok(v, 'aceptado', 'sin botones Editar/Eliminar (el contrato no los tiene)', (await page.locator('main#contenido').getByRole('button', { name: /^(Editar|Eliminar)\b/ }).count()) === 0)
  await page.getByLabel('Filas por página').selectOption('25')
  await page.waitForFunction(() => document.querySelectorAll('main#contenido tbody tr').length > 0)
  await capturas(v)
  // Límite: colección vacía
  const reentrar = async () => { await ir(page, '/tablero'); await ir(page, b.ruta) }
  control.paginas = [[]]
  await reentrar()
  await esperarTexto(b.vacio)
  ok(v, 'límite', 'colección vacía: explica el porqué', true)
  if (v === 'alertas') await capturas(`${v}-vacio`)
  // Error: 500 con reintento; 403 sin acceso
  control.paginas = [[1].map(b.fila)]
  control.estado = 500
  await reentrar()
  await esperarTexto('traza-e2e-500')
  const texto500 = await page.locator('main#contenido').innerText()
  ok(v, 'error', '500: mensaje accionable con traza, sin el texto crudo', !texto500.includes('crudo del servidor'))
  if (v === 'alertas') await capturas(`${v}-error`)
  control.estado = 200
  const antes = pedidos.length
  await page.getByRole('button', { name: 'Volver a intentar' }).click()
  await page.getByRole('table').first().waitFor()
  ok(v, 'error', '«Volver a intentar» repite el pedido y vuelve a la lista', pedidos.length > antes)
  control.estado = 403
  await reentrar()
  await esperarTexto('No tenés acceso a esta bandeja.')
  ok(v, 'error', '403: sin acceso, sin tabla ni descarga', (await page.locator('main#contenido table').count()) === 0 && (await page.getByRole('button', { name: 'Descargar CSV' }).count()) === 0)
  await page.unroute(b.patron)
}

writeFileSync(salida, JSON.stringify({ resultados, errores }, null, 1))
console.log(JSON.stringify({ pass: resultados.filter((r) => r.veredicto === 'PASS').length, fail: resultados.filter((r) => r.veredicto === 'FAIL').length, errores: errores.length }))
await navegador.close()
