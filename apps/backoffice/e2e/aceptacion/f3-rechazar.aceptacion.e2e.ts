import { test, expect, ingresarBackoffice } from './apoyo/escenario'
import { ClienteApi } from './apoyo/api'
import { capturarEstado } from './apoyo/capturas'
import { PERMITIDAS_DEL_INGRESO } from './apoyo/observador'
import { abrirExpediente, llenarResolucion, prepararSolicitud, resolver, unica } from './apoyo/flujo'

/**
 * F3 · RECHAZAR. B rechaza (contradiciendo la propuesta, con motivo); no queda membresía; B no puede reabrirla sin una nueva revisión.
 *
 * BRECHA DE PRODUCTO verificada en el servicio `grupos` (no es del test): la persona rechazada NO tiene cómo enterarse del motivo.
 *  - `GET /grupos/solicitudes/{id}/decisiones` es «accesible al administrador del grupo y al backoffice» (CU68AceptarIngreso.historial).
 *  - Nada consume `grupos.admision_resuelta` (no hay notificación): buscado en servicios/<cada servicio>/src/main.
 *  - `GET /grupos/participaciones` solo lista participaciones vigentes: la rechazada no aparece.
 * El último test del archivo fija el comportamiento DESEADO («A ve el rechazo con el motivo») con `test.fail`: pasa mientras la brecha exista y
 * avisa (falla «inesperadamente») el día que se cierre, para que alguien lo convierta en prueba normal.
 */
test.describe('F3 · rechazar una solicitud (dos usuarios)', () => {
  test('B rechaza con motivo; no queda membresía; no se puede reabrir sin una nueva revisión', async ({ b, mundo, api }) => {
    const s = await prepararSolicitud(api, mundo, { decision: 'ACEPTAR', motivo: 'Perfil compatible según el administrador.' })
    const motivo = 'La concentración de riesgo del grupo supera el límite; se rechaza pese a la propuesta.'

    await ingresarBackoffice(b.page, mundo.operador)
    await abrirExpediente(b.page, s.solicitudId)
    await expect(b.page.getByText('Propuesta del administrador')).toBeVisible()

    // Rechazar sin motivo: el error es del campo y no sale ninguna petición.
    await b.page.getByLabel('Rechazar', { exact: true }).check({ force: true })
    const marca = b.obs.marca()
    await resolver(b.page).click()
    await expect(b.page.getByText('Escribí el motivo')).toBeVisible()
    expect(b.obs.peticiones.slice(marca).filter((p) => p.metodo === 'POST')).toEqual([])
    await capturarEstado(b.page, 'F3', 'b-rechazar-sin-motivo')

    await llenarResolucion(b.page, 'Rechazar', motivo)
    await capturarEstado(b.page, 'F3', 'b-rechazar-formulario-lleno', { viewports: ['movil', 'escritorio'] })
    await resolver(b.page).click()
    await expect(b.page.getByText('Resolución registrada. La solicitud quedó cerrada.')).toBeVisible()
    await capturarEstado(b.page, 'F3', 'b-rechazo-confirmado')

    // Estado real persistido.
    const hist = (await api.b.historial(s.solicitudId)).cuerpo
    const r = unica(hist.filter((d) => d.fase === 'RESOLUCION'), 'una sola resolución')
    expect(r.decision).toBe('RECHAZAR')
    expect(r.motivo).toBe(motivo)
    expect(r.participanteId, 'rechazar no crea participante').toBeUndefined()
    expect((await api.a.misParticipaciones()).cuerpo.filter((p) => p.grupoId === s.grupoId), 'NO queda membresía').toEqual([])
    expect((await api.organizador.solicitudesPendientes(s.grupoId)).cuerpo, 'sale de la cola pendiente del organizador').toEqual([])

    // Tras «recargar» (sesión nueva de B) el expediente sigue cerrado y sin formulario.
    await b.page.reload()
    await ingresarBackoffice(b.page, mundo.operador, `/admisiones/${s.solicitudId}`)
    await expect(b.page.getByText('La solicitud ya está resuelta')).toBeVisible()
    await expect(resolver(b.page)).toHaveCount(0)
    await expect(b.page.getByText(motivo)).toBeVisible()
    await capturarEstado(b.page, 'F3', 'b-expediente-rechazado')

    // No se reabre: ni B resolviendo otra vez, ni el administrador proponiendo de nuevo, ni A reusando el enlace.
    const reabrir = await api.b.resolver(s.solicitudId, 'ACEPTAR', 'Me arrepentí.', 2, s.propuesta!.id)
    expect(reabrir.estado).toBe(422)
    expect(reabrir.cuerpo.mensaje).toMatch(/ya fue resuelta|cambió/)
    const repropuesta = await api.organizador.proponer(s.solicitudId, 'ACEPTAR', 'Otra vez.', 2)
    expect(repropuesta.estado).toBe(422)
    const reuso = await api.a.canjear(s.grupoId, s.invitacionId, s.token)
    expect(reuso.estado, 'el enlace ya se consumió: pedir de nuevo exige una invitación nueva').toBe(422)
    expect((await api.a.misParticipaciones()).cuerpo.filter((p) => p.grupoId === s.grupoId)).toEqual([])
    expect(unica((await api.b.historial(s.solicitudId)).cuerpo.filter((d) => d.fase === 'RESOLUCION'), 'sigue habiendo una sola resolución').decision).toBe('RECHAZAR')

    expect(b.obs.consolaInesperada(PERMITIDAS_DEL_INGRESO), `consola de B: ${b.obs.consola.join(' | ')}`).toEqual([])
    expect(b.obs.inesperadas(PERMITIDAS_DEL_INGRESO)).toEqual([])
  })

  test('el motivo de más de 1000 caracteres se rechaza en el campo y no se envía', async ({ b, mundo, api }) => {
    const s = await prepararSolicitud(api, mundo, { decision: 'RECHAZAR', motivo: 'Riesgo alto.' })
    await ingresarBackoffice(b.page, mundo.operador)
    await abrirExpediente(b.page, s.solicitudId)
    await llenarResolucion(b.page, 'Rechazar', 'x'.repeat(1001))
    const marca = b.obs.marca()
    await resolver(b.page).click()
    await expect(b.page.getByText('El motivo admite hasta 1000 caracteres.')).toBeVisible()
    expect(b.obs.peticiones.slice(marca).filter((p) => p.metodo === 'POST')).toEqual([])
    await capturarEstado(b.page, 'F3', 'b-motivo-demasiado-largo', { viewports: ['movil'], temas: ['light', 'dark'] })
    // Y el servidor, directamente, también lo rechaza (la validación no es solo del cliente).
    const directo = await api.b.resolver(s.solicitudId, 'RECHAZAR', 'x'.repeat(1001), 1, s.propuesta!.id)
    expect(directo.estado).toBe(422)
  })

  test('A no puede leer el expediente de su propia solicitud ni decidir sobre ella (separación de funciones)', async ({ mundo, api }) => {
    const s = await prepararSolicitud(api, mundo, { decision: 'ACEPTAR', motivo: 'Propuesta.' })
    expect((await api.a.historial(s.solicitudId)).estado, 'A no es administrador ni backoffice').toBeGreaterThanOrEqual(400)
    expect((await api.a.proponer(s.solicitudId, 'ACEPTAR', 'Me acepto yo.', 1)).estado, 'A no tiene GRUPO_ADMINISTRAR').toBe(403)
    expect((await api.a.resolver(s.solicitudId, 'ACEPTAR', 'Me acepto yo.', 1, s.propuesta!.id)).estado, 'A no tiene ADMIN_PLATAFORMA').toBe(403)
    expect((await api.a.misParticipaciones()).cuerpo.filter((p) => p.grupoId === s.grupoId), 'y no ganó nada').toEqual([])
  })

  test('DESEADO: A ve el rechazo con su motivo (hoy no existe cómo)', async ({ mundo, api }) => {
    test.fail(true, 'BRECHA: ningún endpoint ni notificación le entrega a la persona rechazada el resultado ni el motivo (servicios/grupos CU68AceptarIngreso.historial; sin consumidor de grupos.admision_resuelta).')
    const s = await prepararSolicitud(api, mundo, { decision: 'ACEPTAR', motivo: 'Propuesta.' })
    expect((await api.b.resolver(s.solicitudId, 'RECHAZAR', 'Motivo visible para la persona.', 1, s.propuesta!.id)).estado).toBe(200)
    const aNueva = await ClienteApi.crear(mundo.participante)
    await aNueva.ingresar()
    const lectura = await aNueva.historial(s.solicitudId)
    await aNueva.cerrar()
    expect(lectura.estado).toBe(200)
    expect(lectura.texto).toContain('Motivo visible para la persona.')
  })
})
