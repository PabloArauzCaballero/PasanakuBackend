import { ClienteApi } from './apoyo/api'
import { test, expect, ingresarBackoffice, irA } from './apoyo/escenario'
import { capturarEstado } from './apoyo/capturas'
import { PERMITIDAS_DEL_INGRESO } from './apoyo/observador'
import { abrirExpediente, llenarResolucion, prepararSolicitud, resolver, unica } from './apoyo/flujo'

const RESOLUCIONES = /\/grupos\/solicitudes\/[^/]+\/resoluciones$/

/** F4 · LÍMITES: permisos, separación de funciones, carrera de dos pestañas de B y respuesta perdida con reintento de la misma clave. */
test.describe('F4 · límites de la admisión', () => {
  test('un operador sin permiso sobre admisiones no ve la sección ni puede abrirla; el servidor también lo rechaza (403)', async ({ b, mundo, api }) => {
    const s = await prepararSolicitud(api, mundo, { decision: 'ACEPTAR', motivo: 'Propuesta.' })
    // Por la UI: entra directo al expediente (como un enlace pegado) y el guardián lo devuelve al tablero.
    await ingresarBackoffice(b.page, mundo.sinPermiso, `/admisiones/${s.solicitudId}`)
    await expect(b.page).toHaveURL(/\/tablero$/)
    await expect(b.page.getByRole('navigation').getByRole('link', { name: /Admisiones|Solicitudes de ingreso/ })).toHaveCount(0)
    await expect(resolver(b.page)).toHaveCount(0)
    await capturarEstado(b.page, 'F4', 'b-sin-permiso-vuelve-al-tablero')
    // Aunque forzara la petición: el servidor decide (el rol oculta, no protege).
    const forzada = await api.sinPermiso.resolver(s.solicitudId, 'ACEPTAR', 'Sin permiso.', 1, s.propuesta!.id)
    expect(forzada.estado).toBe(403)
    expect((await api.sinPermiso.historial(s.solicitudId)).estado, 'tampoco lee el expediente').toBeGreaterThanOrEqual(400)
    // Sin token: 401, no 403 (son situaciones distintas).
    const anonimo = await ClienteApi.crear(mundo.sinPermiso)
    expect((await anonimo.resolver(s.solicitudId, 'ACEPTAR', 'Anónimo.', 1, s.propuesta!.id)).estado).toBe(401)
    await anonimo.cerrar()
    expect((await api.b.historial(s.solicitudId)).cuerpo.filter((d) => d.fase === 'RESOLUCION'), 'nada se resolvió').toEqual([])
    expect(b.obs.consolaInesperada(PERMITIDAS_DEL_INGRESO), `consola de B: ${b.obs.consola.join(' | ')}`).toEqual([])
  })

  test('nadie resuelve su propia postulación ni se la propone: A (postulante) recibe 403 en ambas rutas', async ({ mundo, api }) => {
    const s = await prepararSolicitud(api, mundo, { decision: 'ACEPTAR', motivo: 'Propuesta.' })
    expect((await api.a.proponer(s.solicitudId, 'ACEPTAR', 'Me propongo.', 1)).estado).toBe(403)
    expect((await api.a.resolver(s.solicitudId, 'ACEPTAR', 'Me resuelvo.', 1, s.propuesta!.id)).estado).toBe(403)
    // El organizador tampoco resuelve: la resolución es de backoffice (otro responsable humano).
    expect((await api.organizador.resolver(s.solicitudId, 'ACEPTAR', 'Resuelve el organizador.', 1, s.propuesta!.id)).estado).toBe(403)
    expect((await api.b.historial(s.solicitudId)).cuerpo.filter((d) => d.fase === 'RESOLUCION')).toEqual([])
    test.info().annotations.push({
      type: 'NO-CUBIERTO',
      description: 'La regla «Nadie puede decidir su propia admisión» (CU68AceptarIngreso.decidir) exige una cuenta con el rol de B Y que sea la postulante; con tokens de un solo rol no es alcanzable por el contrato: queda cubierta solo por la prueba unitaria del servicio.',
    })
  })

  test('doble resolución concurrente: dos pestañas de B con decisiones opuestas dejan UNA sola decisión', async ({ b, mundo, api }) => {
    const s = await prepararSolicitud(api, mundo, { decision: 'ACEPTAR', motivo: 'Propuesta.' })
    await ingresarBackoffice(b.page, mundo.operador)
    await abrirExpediente(b.page, s.solicitudId)
    // Segunda pestaña del MISMO contexto de B (la sesión vive en memoria: entra por su cuenta).
    const otra = await b.contexto.newPage()
    await otra.route('**/*', async (ruta) => {
      if (ruta.request().resourceType() !== 'document') return ruta.fallback()
      const r = await ruta.fetch()
      return ruta.fulfill({ response: r, body: (await r.text()).replace('</head>', '<meta name="aportaya-gateway" content="/api/v1"></head>') })
    })
    await otra.route('**/sesion/refrescar', (r) => r.fulfill({ status: 401, json: {} }))
    await ingresarBackoffice(otra, mundo.operador, `/admisiones/${s.solicitudId}`)
    await expect(otra.getByRole('heading', { name: 'Tu resolución' })).toBeVisible()
    await llenarResolucion(b.page, 'Aceptar', 'Pestaña 1: aceptar.')
    await llenarResolucion(otra, 'Rechazar', 'Pestaña 2: rechazar.')
    await capturarEstado(b.page, 'F4', 'b-pestana-1-antes-de-la-carrera', { viewports: ['escritorio'], temas: ['light'] })

    await Promise.all([resolver(b.page).click(), resolver(otra).click()])
    const hist = (await api.b.historial(s.solicitudId)).cuerpo
    const finales = hist.filter((d) => d.fase === 'RESOLUCION')
    const ganadora = unica(finales, 'una sola resolución pese a las dos pestañas')
    // La pestaña perdedora lo dice con claridad y no deja la solicitud a medias.
    const confirmaciones = [b.page, otra].map((p) => p.getByText('Resolución registrada. La solicitud quedó cerrada.'))
    const cuantas = async () => (await Promise.all(confirmaciones.map((c) => c.isVisible()))).filter(Boolean).length
    await expect.poll(cuantas, { message: 'exactamente una pestaña confirma' }).toBe(1)
    const visibles = await Promise.all(confirmaciones.map((c) => c.isVisible()))
    const perdedora = visibles[0] ? otra : b.page
    await expect(perdedora.getByText(/Otra persona ya resolvió|La solicitud cambió|ya está resuelta/).first()).toBeVisible()
    await capturarEstado(perdedora, 'F4', 'b-pestana-perdedora-otra-persona-resolvio')
    const participaciones = (await api.a.misParticipaciones()).cuerpo.filter((p) => p.grupoId === s.grupoId)
    expect(participaciones, 'la membresía corresponde a la decisión ganadora y no se duplicó').toHaveLength(ganadora.decision === 'ACEPTAR' ? 1 : 0)
    await otra.close()
  })

  test('respuesta perdida: B no la reenvía a ciegas, verifica, y el reintento con la MISMA clave no duplica', async ({ b, mundo, api }) => {
    const s = await prepararSolicitud(api, mundo, { decision: 'ACEPTAR', motivo: 'Propuesta.' })
    await ingresarBackoffice(b.page, mundo.operador)
    await abrirExpediente(b.page, s.solicitudId)
    await llenarResolucion(b.page, 'Rechazar', 'Se rechaza: no cumple la concentración de riesgo.')

    // Falla de red inyectada en el NAVEGADOR (no se simula el servidor): la petición SÍ llega y el servidor la procesa, pero la respuesta se corta.
    let perdida = true
    await b.page.route(RESOLUCIONES, async (ruta) => {
      if (!perdida) return ruta.continue()
      perdida = false
      await ruta.fetch() // llega al servidor real y se procesa
      return ruta.abort('connectionreset')
    })
    const desde = b.obs.marca()
    await resolver(b.page).click()
    await expect(b.page.getByText('No pudimos confirmar si tu resolución quedó registrada')).toBeVisible()
    await expect(b.page.getByLabel(/Motivo de la resolución/)).toHaveValue('Se rechaza: no cumple la concentración de riesgo.')
    await capturarEstado(b.page, 'F4', 'b-respuesta-perdida-sin-confirmar')

    // Verificar consulta el historial: la resolución está y NO se reenvía nada.
    await b.page.getByRole('button', { name: 'Verificar si se registró' }).click()
    await expect(b.page.getByText('Resolución registrada. La solicitud quedó cerrada.')).toBeVisible()
    expect(b.obs.peticiones.slice(desde).filter((p) => p.metodo === 'POST' && RESOLUCIONES.test(p.url)), 'un solo envío de resolución').toHaveLength(1)
    unica((await api.b.historial(s.solicitudId)).cuerpo.filter((d) => d.fase === 'RESOLUCION'), 'una sola resolución persistida')

    // Y a nivel de API: reenviar con la misma clave devuelve LA MISMA decisión; con otra, se rechaza.
    const envio = b.obs.peticiones.find((p) => p.metodo === 'POST' && RESOLUCIONES.test(p.url))
    const clave = (JSON.parse(envio?.cabeceras ?? '{}') as Record<string, string>)['idempotency-key']
    expect(clave, 'el navegador mandó Idempotency-Key').toMatch(/^[0-9a-f-]{36}$/)
    const original = unica((await api.b.historial(s.solicitudId)).cuerpo.filter((d) => d.fase === 'RESOLUCION'), 'resolución')
    const igual = await api.b.resolver(s.solicitudId, 'RECHAZAR', 'Se rechaza: no cumple la concentración de riesgo.', 1, s.propuesta!.id, clave)
    expect(igual.estado).toBe(200)
    expect(igual.cuerpo.id).toBe(original.id)
    // La misma clave con OTRO contenido es un error de uso, no se aplica.
    const otraCosa = await api.b.resolver(s.solicitudId, 'ACEPTAR', 'Distinto contenido.', 1, s.propuesta!.id, clave)
    expect(otraCosa.estado).toBe(422)
    expect((await api.a.misParticipaciones()).cuerpo.filter((p) => p.grupoId === s.grupoId)).toEqual([])
    // Consola: el aborto intencional deja un `net::ERR_*` / «Failed to load resource» esperado SOLO para esa petición.
    expect(b.obs.consola.filter((c) => !/Failed to load resource|ERR_CONNECTION_RESET|status of 401/.test(c)), `consola: ${b.obs.consola.join(' | ')}`).toEqual([])
  })

  test('el envío que nunca llegó: verificar no la encuentra y el reintento reutiliza la MISMA clave', async ({ b, mundo, api }) => {
    const s = await prepararSolicitud(api, mundo, { decision: 'ACEPTAR', motivo: 'Propuesta.' })
    await ingresarBackoffice(b.page, mundo.operador)
    await abrirExpediente(b.page, s.solicitudId)
    await llenarResolucion(b.page, 'Aceptar', 'Referencias verificadas por el equipo.')
    let corta = true
    await b.page.route(RESOLUCIONES, (ruta) => (corta ? ruta.abort('connectionreset') : ruta.continue()))
    await resolver(b.page).click()
    await expect(b.page.getByText('No pudimos confirmar si tu resolución quedó registrada')).toBeVisible()
    await b.page.getByRole('button', { name: 'Verificar si se registró' }).click()
    await expect(b.page.getByText('La resolución no se registró')).toBeVisible()
    expect((await api.b.historial(s.solicitudId)).cuerpo.filter((d) => d.fase === 'RESOLUCION'), 'nada llegó al servidor').toEqual([])
    await capturarEstado(b.page, 'F4', 'b-envio-no-registrado')
    corta = false
    await b.page.getByRole('button', { name: 'Reintentar el envío' }).click()
    await expect(b.page.getByText('Resolución registrada. La solicitud quedó cerrada.')).toBeVisible()
    const envios = b.obs.peticiones.filter((p) => p.metodo === 'POST' && RESOLUCIONES.test(p.url))
    expect(envios).toHaveLength(2)
    const claves = envios.map((p) => (JSON.parse(p.cabeceras) as Record<string, string>)['idempotency-key'])
    expect(claves[0], 'el reintento reutiliza la clave de idempotencia').toBe(claves[1])
    unica((await api.b.historial(s.solicitudId)).cuerpo.filter((d) => d.fase === 'RESOLUCION'), 'una sola resolución')
    expect((await api.a.misParticipaciones()).cuerpo.filter((p) => p.grupoId === s.grupoId)).toHaveLength(1)
  })

  test('estados de la pantalla: cargando, vacío (sin propuesta) y error del servidor', async ({ b, mundo, api }) => {
    const s = await prepararSolicitud(api, mundo, null) // el administrador todavía no propuso
    await ingresarBackoffice(b.page, mundo.operador)
    // Cargando: se retiene la respuesta del historial hasta capturar.
    let soltar!: () => void
    const retenida = new Promise<void>((ok) => (soltar = ok))
    await b.page.route(/\/grupos\/solicitudes\/[^/]+\/decisiones$/, async (ruta) => {
      await retenida
      await ruta.continue()
    })
    await abrirExpedienteSinEsperar(b.page, s.solicitudId)
    await expect(b.page.getByRole('status', { name: 'Cargando el expediente de la solicitud' })).toBeVisible()
    await capturarEstado(b.page, 'F4', 'b-expediente-cargando')
    soltar()
    await expect(b.page.getByText('todavía no propuso una decisión').first()).toBeVisible()
    await capturarEstado(b.page, 'F4', 'b-expediente-vacio-sin-propuesta')
    // Error: una solicitud que no existe → el servidor contesta 422 y la pantalla lo dice sin volcar el error crudo.
    await b.page.unroute(/\/grupos\/solicitudes\/[^/]+\/decisiones$/)
    const inexistente = '00000000-0000-4000-8000-000000000000'
    await abrirExpediente(b.page, inexistente)
    await expect(b.page.getByRole('alert')).toBeVisible()
    await capturarEstado(b.page, 'F4', 'b-expediente-error')
    expect(b.obs.consolaInesperada([...PERMITIDAS_DEL_INGRESO, { url: /\/decisiones$/, estados: [422], razon: 'la solicitud inexistente se contesta 422 y la pantalla lo muestra' }]), `consola: ${b.obs.consola.join(' | ')}`).toEqual([])
  })
})

async function abrirExpedienteSinEsperar(page: import('@playwright/test').Page, id: string): Promise<void> {
  await irA(page, `/admisiones/${id}`)
}
