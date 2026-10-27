// Mutaciones de Operación en navegador: éxito contra el simulado OpenAPI (SIN ESTADO: «verificada contra el doble»,
// nada persiste) y fallo 500 provocado con page.route (lo escrito se conserva, el botón no queda trabado).
import { chromium, entrar, ir, afirmar, SALIDA } from './apoyo.mjs'

const nav = await chromium.launch({ args: ['--disable-features=LocalNetworkAccessChecks'] })
const ses = await entrar(nav)
const { page } = ses
const post = (ruta) => (r) => r.method() === 'POST' && new URL(r.url()).pathname.startsWith('/api/v1' + ruta)
const CUENTA = '66d81a5c-e974-4f02-8705-39aa2504c995'

// ── Conciliación de custodia (CU-50) ──────────────────────────────────────────────────────────────────────────
await ir(page, '/operacion/custodia')
await page.getByRole('button', { name: 'Registrar conciliación' }).click()
async function llenarConciliacion() {
  await page.getByLabel('Identificador de la cuenta de custodia').fill(CUENTA)
  await page.getByLabel('Día que se concilia').fill('2026-10-20')
  await page.getByRole('radio', { name: 'BOB' }).check()
  await page.getByLabel('Saldo según el extracto bancario').fill('1.240,00')
  await page.getByLabel('Saldo en tránsito (ordenado y aún no impactado)').fill('0,00')
}
await page.getByRole('button', { name: 'Revisar y conciliar' }).click()
await page.getByText('Ese identificador no tiene el formato de una cuenta de custodia.').waitFor()
afirmar(true, 'custodia: validación en el campo antes de enviar (cuenta vacía)')
await llenarConciliacion()
await page.screenshot({ path: `${SALIDA}/custodia-formulario-1280-claro.png`, fullPage: true })
await page.route((u) => u.pathname === '/api/v1/custodia/conciliaciones', (route) => route.request().method() === 'POST' ? route.fulfill({ status: 500, json: { codigo: 'AP-GW-500', mensaje: 'detalle interno', trazaId: 'traza-c' } }) : route.fallback())
await page.getByRole('button', { name: 'Revisar y conciliar' }).click()
let [req] = await Promise.all([page.waitForRequest(post('/custodia/conciliaciones')), page.getByRole('button', { name: 'Registrar la conciliación' }).click()])
await page.getByText('No pudimos confirmar si la conciliación quedó registrada').waitFor()
afirmar((await page.getByLabel('Saldo según el extracto bancario').inputValue()) === '1.240,00' && await page.getByRole('button', { name: 'Revisar y conciliar' }).isEnabled(), 'custodia: 500 al registrar ⇒ aviso «sin confirmar», lo escrito se conserva y el botón queda usable')
afirmar(JSON.stringify(req.postDataJSON()) === JSON.stringify({ cuentaCustodiaId: CUENTA, fecha: '2026-10-20', saldoCustodia: { monto: '1240.00', moneda: 'BOB' }, saldoEnTransito: { monto: '0.00', moneda: 'BOB' } }) && !!req.headers()['idempotency-key'], 'custodia: el POST lleva exactamente el contrato (montos en cadena del contrato) y Idempotency-Key')
await page.unrouteAll()
const recarga = page.waitForRequest((r) => r.method() === 'GET' && new URL(r.url()).pathname === '/api/v1/custodia/conciliaciones')
await page.getByRole('button', { name: 'Revisar y conciliar' }).click()
;[req] = await Promise.all([page.waitForRequest(post('/custodia/conciliaciones')), page.getByRole('button', { name: 'Registrar la conciliación' }).click()])
const resp = await req.response()
await page.getByText('Conciliación registrada').waitFor()
await recarga
afirmar(resp.ok(), `custodia: POST /custodia/conciliaciones → ${resp.status()} (simulado sin estado) ⇒ recibo y el historial se vuelve a pedir [verificado contra el doble]`)
await page.screenshot({ path: `${SALIDA}/custodia-recibo-1280-claro.png`, fullPage: true })

// ── Cierre diario (CU-51) ─────────────────────────────────────────────────────────────────────────────────────
await ir(page, '/operacion/cierres-diarios')
await page.getByRole('button', { name: 'Cerrar un día' }).click()
await page.getByLabel('Día que se cierra').fill('2026-10-20')
await page.route((u) => u.pathname === '/api/v1/contabilidad/cierres-diarios', (route) => route.request().method() === 'POST' ? route.fulfill({ status: 500, json: { codigo: 'AP-GW-500', mensaje: 'detalle interno' } }) : route.fallback())
await page.getByRole('button', { name: 'Revisar y cerrar' }).click()
await Promise.all([page.waitForRequest(post('/contabilidad/cierres-diarios')), page.getByRole('button', { name: /^Cerrar el día/ }).click()])
await page.getByText('No pudimos confirmar si el día quedó cerrado').waitFor()
afirmar((await page.getByLabel('Día que se cierra').inputValue()) === '2026-10-20' && await page.getByRole('button', { name: 'Revisar y cerrar' }).isEnabled(), 'cierres: 500 al cerrar ⇒ aviso, el día elegido se conserva y el botón queda usable')
await page.unrouteAll()
await page.getByRole('button', { name: 'Revisar y cerrar' }).click()
;[req] = await Promise.all([page.waitForRequest(post('/contabilidad/cierres-diarios')), page.getByRole('button', { name: /^Cerrar el día/ }).click()])
afirmar(JSON.stringify(req.postDataJSON()) === '{"fecha":"2026-10-20"}', 'cierres: el POST manda solo { fecha } (las cifras las informa aportes)')
await page.locator('ap-comprobante-de-cierre').waitFor()
afirmar((await req.response()).ok(), 'cierres: POST → 2xx ⇒ comprobante del día [verificado contra el doble]')
await page.screenshot({ path: `${SALIDA}/cierres-comprobante-1280-claro.png`, fullPage: true })

// ── Respuesta a un reclamo (CU-52) ────────────────────────────────────────────────────────────────────────────
await ir(page, '/operacion/reclamos')
await page.getByRole('button', { name: /^Responder el reclamo/ }).first().click()
const dialogo = page.getByRole('dialog')
await dialogo.waitFor()
await dialogo.getByRole('radio', { name: 'Desfavorable' }).check()
const TEXTO = 'No corresponde: la comisión figura en el tarifario vigente.'
await dialogo.getByLabel('Respuesta para la persona').fill(TEXTO)
await page.route((u) => /\/api\/v1\/reclamos\/[^/]+\/respuesta$/.test(u.pathname), (route) => route.fulfill({ status: 500, json: { codigo: 'AP-GW-500', mensaje: 'detalle interno' } }))
await Promise.all([page.waitForRequest(post('/reclamos/')), dialogo.getByRole('button', { name: /^Responder y cerrar/ }).click()])
await dialogo.getByText('No pudimos confirmar si la respuesta quedó registrada').waitFor()
afirmar((await dialogo.getByLabel('Respuesta para la persona').inputValue()) === TEXTO && await dialogo.getByRole('button', { name: /^Responder y cerrar/ }).isEnabled(), 'reclamos: 500 al responder ⇒ aviso, la respuesta escrita se conserva y el botón queda usable')
await page.screenshot({ path: `${SALIDA}/reclamos-respuesta-error-1280-claro.png`, fullPage: true })
await page.unrouteAll()
;[req] = await Promise.all([page.waitForRequest(post('/reclamos/')), dialogo.getByRole('button', { name: /^Responder y cerrar/ }).click()])
await page.getByText(/^Respuesta registrada: el reclamo/).waitFor()
afirmar(JSON.stringify(req.postDataJSON()) === JSON.stringify({ resultado: 'DESFAVORABLE', respuesta: TEXTO }), 'reclamos: POST /reclamos/:id/respuesta con el cuerpo del contrato ⇒ «Respuesta registrada» [verificado contra el doble]')

// ── Aprobación de un retiro (CU-11) ───────────────────────────────────────────────────────────────────────────
await ir(page, '/operacion/retiros')
await page.getByRole('button', { name: /^Revisar y decidir el retiro/ }).first().click()
await page.getByRole('heading', { level: 1 }).filter({ hasText: /retiro/i }).waitFor()
await page.getByRole('radio', { name: 'Rechazar el retiro' }).check()
await page.getByLabel('Motivo (opcional)').fill('Cuenta destino sin verificar.')
await page.route((u) => /\/aprobacion$/.test(u.pathname), (route) => route.fulfill({ status: 500, json: { codigo: 'AP-GW-500', mensaje: 'detalle interno' } }))
await Promise.all([page.waitForRequest(post('/billetera/retiros/')), page.getByRole('button', { name: 'Registrar mi decisión' }).click()])
await page.getByText('No pudimos confirmar si tu decisión quedó registrada.').waitFor()
afirmar((await page.getByLabel('Motivo (opcional)').inputValue()) === 'Cuenta destino sin verificar.', 'retiros: 500 al decidir ⇒ aviso y el motivo se conserva')
await page.screenshot({ path: `${SALIDA}/retiros-aprobacion-error-1280-claro.png`, fullPage: true })
await page.unrouteAll()
const reintento = page.getByRole('button', { name: /Reintentar el envío|Registrar mi decisión/ }).first()
;[req] = await Promise.all([page.waitForRequest(post('/billetera/retiros/')), reintento.click()])
afirmar((await req.response()).ok() && req.postDataJSON().desenlace === 'RECHAZADA', 'retiros: POST /billetera/retiros/:id/aprobacion → 2xx con desenlace RECHAZADA [verificado contra el doble]')
await page.getByText('Registrado:', { exact: false }).first().waitFor()
await page.getByRole('button', { name: 'Volver a la bandeja de retiros' }).click()
await page.waitForURL('**/operacion/retiros')
afirmar(true, 'retiros: «Volver a la bandeja de retiros» navega a /operacion/retiros')

afirmar(ses.errores.filter((e) => !/status of 500/.test(e)).length === 0, `formularios: consola sin errores salvo los 500 provocados ${JSON.stringify(ses.errores.filter((e) => !/status of 500/.test(e)))}`)
await nav.close()
