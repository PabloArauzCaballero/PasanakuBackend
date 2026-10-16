import { test, expect, ingresarBackoffice } from './apoyo/escenario'
import { ClienteApi } from './apoyo/api'
import { capturarEstado } from './apoyo/capturas'
import { BASE_API, MODO } from './apoyo/entorno'

/**
 * F5 · RETIRO CON DOBLE APROBACIÓN (CU-11, `aprobarRetiro`, permiso RETIRO_APROBAR, ADR-049). Dos personas de tesorería, cada una en una
 * sesión independiente del backoffice.
 *
 * `VERIFIED_FUNCTIONAL_ONLY` y solo contra el DOBLE con estado: sembrar una orden «EN_REVISION» real exige un retiro por encima del
 * umbral con MFA y fondos (carril G). La bandeja `GET /billetera/retiros` ya existe (carril A · H2); la decisión se abre por enlace
 * profundo `operacion/retiros/:ordenId`, con la orden fijada por la ruta (carril B · L1). Lo que SÍ se
 * prueba acá es la pantalla y su contrato: entrada estricta (sin campos de más), la segregación (quien pidió no aprueba), la carrera
 * (el segundo recibe 409, nunca un doble efecto) y que el rechazo con motivo queda registrado.
 */
test.describe('F5 · retiro con doble aprobación (dos tesoreros)', () => {
  test.fixme(MODO === 'real', 'BLOQUEADO en modo real: no hay cómo sembrar una orden de retiro en revisión (sin alta de prueba en nucleo-financiero)')

  async function orden(solicitadaPor: string): Promise<string> {
    const r = await fetch(`${BASE_API}/__doble/retiro`, { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify({ solicitadaPor }) })
    expect(r.ok, 'el doble siembra una orden en revisión').toBe(true)
    return ((await r.json()) as { ordenId: string }).ordenId
  }
  const instantanea = async () => (await (await fetch(`${BASE_API}/__doble/instantanea`)).json()) as { retiros: { id: string; estado: string; aprobadaPor?: string; motivo?: string }[] }

  test('quien pidió el retiro no lo aprueba; otra persona lo autoriza una sola vez y la tercera ve que ya no está en revisión', async ({ b, mundo }) => {
    const t1 = mundo.tesoreria!
    const t2 = mundo.tesoreria2!
    const ordenId = await orden(t1.id!)

    // Quien pidió el retiro (t1) abre la pantalla, ve su alcance y el servidor le dice que no.
    await ingresarBackoffice(b.page, t1, `/operacion/retiros/${ordenId}`)
    await expect(b.page.getByRole('heading', { name: 'Aprobar o rechazar un retiro' })).toBeVisible()
    await expect(b.page.getByLabel(/Identificador de la orden de retiro/), 'la orden la fija la ruta').toHaveValue(ordenId)
    await expect(b.page.getByLabel(/Identificador de la orden de retiro/)).toBeDisabled()
    await capturarEstado(b.page, 'F5', 'b-retiro-formulario')

    const marca = b.obs.marca()
    await b.page.getByRole('button', { name: 'Registrar mi decisión' }).click()
    await expect(b.page.getByText('Elegí si autorizás o rechazás')).toBeVisible()
    expect(b.obs.peticiones.slice(marca).filter((p) => p.metodo === 'POST'), 'el formulario sin decisión no llega al servidor').toEqual([])
    await capturarEstado(b.page, 'F5', 'b-retiro-errores-por-campo', { viewports: ['movil', 'escritorio'] })

    await b.page.getByLabel('Autorizar el retiro', { exact: true }).check({ force: true })
    await b.page.getByRole('button', { name: 'Registrar mi decisión' }).click()
    await expect(b.page.getByText('No podés resolver un retiro que pediste vos')).toBeVisible()
    await expect(b.page.getByLabel(/Identificador de la orden de retiro/)).toHaveValue(ordenId)
    await capturarEstado(b.page, 'F5', 'b-retiro-autoaprobacion-rechazada')
    expect((await instantanea()).retiros.find((r) => r.id === ordenId)?.estado, 'la orden sigue en revisión').toBe('EN_REVISION')

    // La segunda persona (t2), en una sesión nueva, autoriza. Sin motivo: el cuerpo lleva solo `desenlace`.
    await b.page.reload()
    await ingresarBackoffice(b.page, t2, `/operacion/retiros/${ordenId}`)
    await b.page.getByLabel('Autorizar el retiro', { exact: true }).check({ force: true })
    const desde = b.obs.marca()
    await b.page.getByRole('button', { name: 'Registrar mi decisión' }).dblclick()
    await expect(b.page.getByText('la orden quedó autorizada')).toBeVisible()
    const envios = b.obs.peticiones.slice(desde).filter((p) => p.metodo === 'POST' && p.url.includes('/aprobacion'))
    expect(envios.length, 'el doble clic produce un solo envío').toBe(1)
    await capturarEstado(b.page, 'F5', 'b-retiro-autorizado')
    const final = (await instantanea()).retiros.find((r) => r.id === ordenId)
    expect(final?.estado).toBe('AUTORIZADA')
    expect(final?.aprobadaPor, 'aprobó otra persona, no quien lo pidió').not.toBe(t1.id)

    // La carrera perdida: t1 ya no puede (es quien pidió), pero una tercera persona con permiso ve 409 y NO hay segundo efecto.
    const otraOrden = await orden(mundo.operador.id!)
    const c2 = await ClienteApi.crear(t2)
    await c2.ingresar()
    expect((await c2.enviar('POST', `billetera/retiros/${otraOrden}/aprobacion`, { desenlace: 'AUTORIZADA' }, crypto.randomUUID())).estado).toBe(200)
    await c2.cerrar()
    await b.page.reload()
    await ingresarBackoffice(b.page, t1, `/operacion/retiros/${otraOrden}`)
    await b.page.getByLabel('Rechazar el retiro', { exact: true }).check({ force: true })
    await b.page.getByLabel(/Motivo/).fill('Titular no coincide con la cuenta de destino.')
    await b.page.getByRole('button', { name: 'Registrar mi decisión' }).click()
    await expect(b.page.getByText('otra persona la resolvió primero')).toBeVisible()
    await capturarEstado(b.page, 'F5', 'b-retiro-ya-resuelto')
    const estados = (await instantanea()).retiros
    expect(estados.find((r) => r.id === otraOrden)?.estado, 'la decisión perdida no cambió nada').toBe('AUTORIZADA')
  })

  test('rechazar con motivo libera la orden y queda registrado; un campo de más no llega al servidor', async ({ b, mundo }) => {
    const t1 = mundo.tesoreria!
    const ordenId = await orden(mundo.operador.id!)
    await ingresarBackoffice(b.page, t1, `/operacion/retiros/${ordenId}`)
    await b.page.getByLabel('Rechazar el retiro', { exact: true }).check({ force: true })
    await b.page.getByLabel(/Motivo/).fill('La cuenta de destino está en enfriamiento.')
    const desde = b.obs.marca()
    await b.page.getByRole('button', { name: 'Registrar mi decisión' }).click()
    await expect(b.page.getByText('la orden quedó rechazada')).toBeVisible()
    const envio = b.obs.peticiones.slice(desde).filter((p) => p.metodo === 'POST' && p.url.includes('/aprobacion'))
    expect(envio).toHaveLength(1)
    await capturarEstado(b.page, 'F5', 'b-retiro-rechazado')
    const registro = (await instantanea()).retiros.find((r) => r.id === ordenId)
    expect(registro?.estado).toBe('RECHAZADA')
    expect(registro?.motivo).toBe('La cuenta de destino está en enfriamiento.')
  })
})
