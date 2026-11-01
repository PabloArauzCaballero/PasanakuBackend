// Recorrido 2 de la franja C: mutaciones (contra el SIMULADO, sin estado → «verificadas contra el doble»), una mutación
// que falla (500 simulado con page.route) y las vistas que no son bandejas por cursor: expedientes, caso y habilitación.
// Datos SINTÉTICOS.
import { readFileSync, writeFileSync } from 'node:fs'
import { abrir, ir } from './sesion.mjs'

const resultados = []
const ok = (vista, nivel, caso, cond, detalle = '') => {
  resultados.push({ vista, nivel, caso, veredicto: cond ? 'PASS' : 'FAIL', detalle })
  console.log(`${cond ? 'PASS' : 'FAIL'} · ${vista} · ${nivel} · ${caso}${detalle ? ' · ' + detalle : ''}`)
}
const { navegador, page, errores } = await abrir()
const dir = '/home/user/wt-c/docs/trabajo/2026-10-26-backoffice-listados-y-navegacion/evidencia/franja-c'
const capturas = async (nombre) => {
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
const respuestaDe = (metodo, re) => page.waitForResponse((r) => r.request().method() === metodo && re.test(r.url()), { timeout: 20000 })
const UUID = '7c1d9e40-0000-4000-8000-0000000000aa'

// ── Casos: decidir contra el simulado; después la misma decisión con 500 conserva lo escrito y no traba el botón.
await page.route('**/api/v1/cumplimiento/casos?*', (r) => r.fulfill({ json: { items: [{ casoId: '5a1e0c22-0000-4000-8000-000000000001', codigo: 'LFT-1', estado: 'EN_ANALISIS', prioridad: 'ALTA', plazoLimite: '2026-10-31T12:00:00Z', vencido: false, decision: null }], proximoPlazo: null, proximoId: null } }))
await ir(page, '/cumplimiento/casos')
await page.getByRole('button', { name: 'Decidir el caso LFT-1' }).click()
const dialogo = page.getByRole('dialog')
await dialogo.getByRole('radio', { name: 'Descartar' }).check()
const conclusion = 'Las operaciones tienen respaldo documental suficiente y coherente.'
await dialogo.getByLabel('Conclusión').fill(conclusion)
let resp = respuestaDe('POST', /\/cumplimiento\/casos\/.*\/decision$/)
await dialogo.getByRole('button', { name: 'Registrar la decisión' }).click()
let r = await resp
ok('casos', 'aceptado', 'Decidir → POST /cumplimiento/casos/{id}/decision con Idempotency-Key (contra el doble)', r.status() < 300 && !!r.request().headers()['idempotency-key'], `${r.status()} ${r.request().postData()}`)
await page.locator('main#contenido').getByText('Decisión registrada').waitFor()
ok('casos', 'aceptado', 'recibo visible tras el 2xx y la bandeja se recarga', true)
await page.route('**/api/v1/cumplimiento/casos/*/decision', (x) => x.fulfill({ status: 500, json: { codigo: 'X', mensaje: 'crudo', trazaId: 'traza-mut-500' } }))
await page.getByRole('button', { name: 'Decidir el caso LFT-1' }).click()
await dialogo.getByRole('radio', { name: 'Descartar' }).check()
await dialogo.getByLabel('Conclusión').fill(conclusion)
resp = respuestaDe('POST', /\/decision$/)
await dialogo.getByRole('button', { name: 'Registrar la decisión' }).click()
await resp
await dialogo.getByText(/traza-mut-500|no pasó|No se pudo|Algo salió mal/).first().waitFor({ timeout: 10000 })
ok('casos', 'error', 'decisión con 500: el diálogo sigue abierto, conserva lo escrito y el botón no queda trabado',
  (await dialogo.getByLabel('Conclusión').inputValue()) === conclusion && (await dialogo.getByRole('button', { name: 'Registrar la decisión' }).isEnabled()))
await capturas('casos-decision-error')
await page.keyboard.press('Escape')
await page.unroute('**/api/v1/cumplimiento/casos/*/decision')
// «Ver» lleva a la ficha del caso; su «Confirmar decisión» ya no es un botón muerto.
await page.getByRole('button', { name: 'Ver caso LFT-1' }).click()
await page.waitForURL(/\/cumplimiento\/casos\/5a1e0c22/)
ok('casos', 'aceptado', '«Ver» navega a la ficha del caso', true)
await page.getByRole('radio').first().check()
await page.getByLabel('Narrativa').fill('Cinco depósitos bajo el umbral el mismo día.')
await page.getByRole('button', { name: 'Confirmar decisión' }).click()
await dialogo.getByText('Decidir el caso').first().waitFor()
const prellenada = await dialogo.getByLabel('Conclusión').inputValue()
ok('caso', 'aceptado', '«Confirmar decisión» abre la decisión real con la conclusión prellenada', prellenada.startsWith('Causal: Fraccionamiento de operaciones.'), prellenada)
await dialogo.getByRole('radio', { name: 'Monitoreo reforzado' }).check()
resp = respuestaDe('POST', /\/cumplimiento\/casos\/5a1e0c22.*\/decision$/)
await dialogo.getByRole('button', { name: 'Registrar la decisión' }).click()
r = await resp
ok('caso', 'aceptado', 'POST decidirCasoLft desde la ficha (contra el doble)', r.status() < 300, String(r.status()))
await page.locator('main#contenido').getByText('Decisión registrada').waitFor()
await capturas('caso')
await page.unroute('**/api/v1/cumplimiento/casos?*')

// ── Alertas: abrir caso con una alerta (simulado).
await page.route('**/api/v1/cumplimiento/alertas?*', (x) => x.fulfill({ json: { items: [{ alertaId: '0b9b2a61-0000-4000-8000-000000000001', reglaCodigo: 'R-1', tipologia: 'Fraccionamiento', severidad: 'ALTA', estado: 'ABIERTA', detectadaEn: '2026-10-10T14:00:00Z', casoId: null }], proximoFecha: null, proximoId: null } }))
await ir(page, '/cumplimiento/alertas')
await page.getByRole('button', { name: 'Abrir caso con la alerta R-1' }).click()
await dialogo.getByLabel('Identificador del analista asignado').fill(UUID)
await dialogo.getByLabel('Resumen del caso').fill('Fraccionamiento en tres días seguidos.')
resp = respuestaDe('POST', /\/cumplimiento\/casos$/)
await dialogo.getByRole('button', { name: 'Abrir el caso' }).click()
r = await resp
ok('alertas', 'aceptado', 'Abrir caso → POST /cumplimiento/casos con la alerta de la fila (contra el doble)', r.status() < 300 && r.request().postData().includes('0b9b2a61-0000-4000-8000-000000000001'), `${r.status()} ${r.request().postData()}`)
await page.locator('main#contenido').getByText('Caso abierto').waitFor()
await page.unroute('**/api/v1/cumplimiento/alertas?*')

// ── Reportes: preparar (simulado).
await ir(page, '/cumplimiento/uif/reportes')
await page.getByRole('button', { name: 'Preparar un reporte' }).click()
await dialogo.getByLabel('Período (año-mes)').fill('2026-09')
await dialogo.getByLabel('Código del catálogo de reportes').fill('UIF-ROG')
await dialogo.getByLabel('Formulario').selectOption('ROG-01')
resp = respuestaDe('POST', /\/uif\/reportes$/)
await dialogo.getByRole('button', { name: 'Preparar el reporte' }).click()
r = await resp
ok('reportes-uif', 'aceptado', 'Preparar → POST /uif/reportes (contra el doble)', r.status() < 300, `${r.status()} ${r.request().postData()}`)
await page.locator('main#contenido').getByText(/Reporte preparado|ya estaba preparado/).waitFor()

// ── Requerimientos: responder (simulado; la fila del simulado viene en RECIBIDO).
await page.route('**/api/v1/uif/requerimientos?*', (x) => x.fulfill({ json: { items: [{ requerimientoId: '6b0c1d00-0000-4000-8000-000000000001', autoridad: 'FISCALIA', numeroOficio: 'OF-1', fechaRecepcion: '2026-10-01T13:00:00Z', plazoRespuesta: '2026-10-15T13:00:00Z', estado: 'RECIBIDO', conBloqueoDeSaldo: false }] } }))
await ir(page, '/cumplimiento/uif/requerimientos')
await page.getByRole('button', { name: 'Responder el oficio OF-1' }).click()
await dialogo.getByLabel('Enlace a la respuesta entregada').fill('https://docs.ejemplo.test/respuesta-of-1.pdf')
resp = respuestaDe('POST', /\/respuesta$/)
await dialogo.getByRole('button', { name: 'Registrar la respuesta' }).click()
r = await resp
ok('requerimientos', 'aceptado', 'Responder → POST /uif/requerimientos/{id}/respuesta (contra el doble)', r.status() < 300, `${r.status()} ${r.request().postData()}`)
await page.unroute('**/api/v1/uif/requerimientos?*')

// ── Expedientes de identidad: 25 sintéticos, búsqueda multicampo, filtros, paginación, descarga, vacío y error.
const base = (i) => ({ verificacionId: `00000000-0000-4000-8000-0000000000${String(i).padStart(2, '0')}`, usuarioId: `u-${i}`, nombreCompleto: i === 3 ? 'José Ñandú de la Cruz Quispe Mamani con un apellido larguísimo' : `Persona Sintética ${i}`, telefono: `+5917000${String(i).padStart(4, '0')}`, correo: i % 4 ? `p${i}@ejemplo.test` : undefined, documento: `CI ${1000000 + i} SC`, estado: 'EN_REVISION', iniciadaEn: '2026-10-10T10:00:00Z', documentoVigente: i % 5 !== 0, fechaExpiracionDocumento: '2030-01-01', fotos: [] })
const exp = { estado: 200, filas: Array.from({ length: 25 }, (_, i) => base(i + 1)) }
await page.route('**/api/v1/identidad/verificaciones*', (x) => {
  if (x.request().method() !== 'GET' || /\/fotos\//.test(x.request().url())) return x.fallback()
  return exp.estado === 200 ? x.fulfill({ json: exp.filas }) : x.fulfill({ status: exp.estado, json: { codigo: 'X', mensaje: 'crudo', trazaId: 'traza-exp' } })
})
await ir(page, '/cumplimiento/verificaciones')
await page.locator('ap-tarjeta-de-expediente').first().waitFor({ timeout: 30000 })
ok('verificaciones', 'aceptado', '25 expedientes, 10 por página → Página 1 de 3', (await page.locator('ap-tarjeta-de-expediente').count()) === 10 && (await page.getByText('Página 1 de 3').count()) === 1)
for (const [t, n] of [['jose', 1], ['70000007', 1], ['p6@ejemplo', 1], ['1000012', 1]]) {
  await page.getByRole('searchbox').fill(t)
  await page.waitForFunction((n) => document.querySelectorAll('ap-tarjeta-de-expediente').length === n, n)
  ok('verificaciones', 'aceptado', `buscar «${t}» encuentra ${n}`, true)
}
await page.getByRole('searchbox').fill('')
await page.getByLabel('Documento', { exact: true }).selectOption('Documento vencido')
await page.waitForFunction(() => document.querySelectorAll('ap-tarjeta-de-expediente').length === 5)
ok('verificaciones', 'aceptado', 'filtro «Documento vencido» deja 5', true)
const [d] = await Promise.all([page.waitForEvent('download'), page.getByRole('button', { name: 'Descargar CSV' }).click()])
const csv = readFileSync(await d.path(), 'utf8')
ok('verificaciones', 'aceptado', 'CSV de lo filtrado (5 filas + encabezado), sin ids internos', csv.trim().split(/\r\n/).length === 6 && !csv.includes('u-5') && !csv.includes('00000000-0000'), d.suggestedFilename())
await page.getByRole('button', { name: 'Limpiar filtros' }).click()
await page.getByLabel('Ir a la página').fill('3')
await page.getByLabel('Ir a la página').press('Enter')
await page.waitForFunction(() => document.querySelectorAll('ap-tarjeta-de-expediente').length === 5)
ok('verificaciones', 'límite', 'Ir a 3 → última página con 5', true)
await page.getByLabel('Filas por página').selectOption('5')
await page.waitForFunction(() => document.querySelectorAll('ap-tarjeta-de-expediente').length === 5 && /Página 1 de 5/.test(document.body.innerText))
ok('verificaciones', 'aceptado', 'filas por página 5 → Página 1 de 5', true)
resp = respuestaDe('GET', /\/identidad\/verificaciones\?estado=APROBADA/)
await page.getByLabel('Estado', { exact: true }).selectOption('APROBADA')
r = await resp
ok('verificaciones', 'aceptado', 'estado viaja al servidor (?estado=APROBADA)', r.status() === 200)
await page.getByLabel('Estado', { exact: true }).selectOption('')
await page.locator('ap-tarjeta-de-expediente').first().waitFor()
await page.getByLabel('Filas por página').selectOption('10')
// Decisión: rechazar con motivo (simulado).
await page.getByLabel('Motivo').first().fill('La foto del documento está borrosa.')
resp = respuestaDe('POST', /\/identidad\/verificaciones\/.*\/decision$/)
await page.locator('ap-tarjeta-de-expediente').first().getByRole('button', { name: 'Rechazar' }).click()
r = await resp
ok('verificaciones', 'aceptado', 'Rechazar → POST /identidad/verificaciones/{id}/decision (contra el doble)', r.status() < 300 && r.request().postData().includes('RECHAZAR'), `${r.status()} ${r.request().postData()}`)
await capturas('verificaciones')
await page.getByRole('searchbox').fill('zzz')
await page.getByText('No hay expedientes que coincidan con la búsqueda.').waitFor()
ok('verificaciones', 'límite', 'sin coincidencias: vacío orientado con «Quitar filtros»', (await page.getByRole('button', { name: 'Quitar filtros' }).count()) === 1)
await page.getByRole('button', { name: 'Quitar filtros' }).click()
exp.filas = []
await ir(page, '/tablero'); await ir(page, '/cumplimiento/verificaciones')
await page.getByText('No hay expedientes esperando').waitFor({ timeout: 20000 })
ok('verificaciones', 'límite', 'cola vacía: explica que no hay nada esperando', true)
exp.estado = 500
await ir(page, '/tablero'); await ir(page, '/cumplimiento/verificaciones')
await page.getByText('traza-exp').waitFor({ timeout: 20000 })
ok('verificaciones', 'error', '500: error con traza y sin texto crudo', !(await page.locator('main#contenido').innerText()).includes('crudo'))
exp.estado = 200; exp.filas = [base(1)]
await page.getByRole('button', { name: 'Volver a intentar' }).click()
await page.locator('ap-tarjeta-de-expediente').first().waitFor()
ok('verificaciones', 'error', '«Volver a intentar» repite y vuelve a la cola', true)

// ── Habilitación de organizador (simulado), y CU-97 / acta / reclamo con su indisponibilidad honesta.
resp = respuestaDe('GET', /\/organizadores\/.*\/habilitacion$/)
await ir(page, `/cumplimiento/organizadores/${UUID}/habilitacion`)
r = await resp
ok('habilitacion', 'aceptado', 'GET /organizadores/{id}/habilitacion', r.status() === 200, String(r.status()))
await page.locator('main#contenido h1').getByText('Habilitación').first().waitFor().catch(() => {})
await capturas('habilitacion')
await ir(page, '/cumplimiento/alertas/x')
await page.getByText('todavía no está disponible').waitFor()
await capturas('indisponible')

writeFileSync(process.argv[2] ?? 'recorrido-2.json', JSON.stringify({ resultados, errores }, null, 1))
console.log(JSON.stringify({ pass: resultados.filter((x) => x.veredicto === 'PASS').length, fail: resultados.filter((x) => x.veredicto === 'FAIL').length, errores: errores.length }))
await navegador.close()
