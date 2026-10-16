import { test, expect, ingresarBackoffice } from './apoyo/escenario'
import { ClienteApi } from './apoyo/api'
import { capturarEstado } from './apoyo/capturas'
import { PERMITIDAS_DEL_INGRESO } from './apoyo/observador'
import { abrirExpediente, llenarResolucion, prepararSolicitud, resolver, unica } from './apoyo/flujo'

/**
 * F2 · ACEPTAR. B (operador de backoffice, en SU navegador) abre la solicitud de A, la ve con la propuesta del administrador y la aprueba con motivo;
 * A (con SU sesión, independiente) ve el estado actualizado tras «recargar» (sesión nueva) y la membresía existe UNA sola vez aunque B haga doble
 * clic o reintente.
 *
 * Diferencias con lo que pedía el encargo, verificadas en el código y no supuestas:
 *  - No hay «bandeja» de admisiones en el backoffice: `entrada-de-admision.ts` dice «Todavía no hay un listado de solicitudes pendientes: cada una
 *    se abre por su identificador». B abre la solicitud por su identificador.
 *  - Aceptar no activa la membresía: crea un participante «pendiente de firma del reglamento» (textos.ts `reservaCupos`). Lo que se verifica es que
 *    existe UNA participación y su estado real, no que sea ACTIVA.
 *  - apps/web no tiene pantallas de participante: «A ve el estado» se verifica por la API de A (`GET /grupos/participaciones`), igual que la app móvil.
 */
test.describe('F2 · aceptar una solicitud (dos usuarios)', () => {
  test('B aprueba con motivo, el estado persiste tras recargar y A ve su participación una sola vez', async ({ b, mundo, api }) => {
    const s = await prepararSolicitud(api, mundo, { decision: 'ACEPTAR', motivo: 'Referencias verificadas por el administrador.' })
    expect((await api.a.misParticipaciones()).cuerpo, 'antes de resolver, A todavía no es participante').toEqual([])

    // B: ingreso (teléfono + contraseña + segundo factor) y expediente.
    await ingresarBackoffice(b.page, mundo.operador)
    await expect(b.page.getByRole('navigation').getByRole('link', { name: /Solicitudes de ingreso|Admisiones/ }).first()).toBeVisible()
    await irAEntrada(b.page)
    await capturarEstado(b.page, 'F2', 'b-entrada-de-admision')
    await abrirExpediente(b.page, s.solicitudId)
    await expect(b.page.getByRole('heading', { name: 'Solicitud de ingreso a un grupo' })).toBeVisible()
    await expect(b.page.getByText('Propuesta del administrador')).toBeVisible()
    await expect(b.page.getByRole('heading', { name: 'Tu resolución' })).toBeVisible()
    await capturarEstado(b.page, 'F2', 'b-expediente-con-propuesta')

    // Error por campo: sin resultado ni motivo no se manda nada.
    const marca = b.obs.marca()
    await resolver(b.page).click()
    await expect(b.page.getByText('Escribí el motivo')).toBeVisible()
    await expect(b.page.getByText('Elegí si aceptás o rechazás')).toBeVisible()
    expect(b.obs.peticiones.slice(marca).filter((p) => p.metodo === 'POST'), 'el formulario vacío no llega al servidor').toEqual([])
    await capturarEstado(b.page, 'F2', 'b-resolver-errores-por-campo', { viewports: ['movil', 'escritorio'] })

    // Aprobar con motivo, con DOBLE CLIC: una sola petición de resolución.
    await llenarResolucion(b.page, 'Aceptar', 'Evidencia suficiente; hay cupos y el perfil es compatible.')
    const desde = b.obs.marca()
    await resolver(b.page).dblclick()
    await expect(b.page.getByText('Resolución registrada. La solicitud quedó cerrada.')).toBeVisible()
    const envios = b.obs.peticiones.slice(desde).filter((p) => p.metodo === 'POST' && p.url.includes('/resoluciones'))
    expect(envios.length, 'el doble clic produce un solo envío').toBe(1)
    await expect(resolver(b.page)).toHaveCount(0)
    await capturarEstado(b.page, 'F2', 'b-resolucion-confirmada')

    // Estado REAL persistido (no el de la pantalla): una sola resolución, con motivo y a nombre de B.
    const hist = (await api.b.historial(s.solicitudId)).cuerpo
    const resolucion = unica(hist.filter((d) => d.fase === 'RESOLUCION'), 'una sola resolución en el historial')
    expect(resolucion.decision).toBe('ACEPTAR')
    expect(resolucion.motivo).toBe('Evidencia suficiente; hay cupos y el perfil es compatible.')
    expect(resolucion.propuestaId).toBe(s.propuesta?.id)
    expect(resolucion.actorId).not.toBe(s.propuesta?.actorId)

    // «Recargar»: la sesión de B vive solo en memoria, así que recargar la pantalla exige volver a entrar; lo resuelto sigue resuelto.
    await b.page.reload()
    await expect(b.page).toHaveURL(/\/ingreso(\?|$)/)
    await ingresarBackoffice(b.page, mundo.operador, `/admisiones/${s.solicitudId}`)
    await expect(b.page.getByRole('heading', { name: 'Resolución de backoffice', level: 3 })).toBeVisible()
    await expect(b.page.getByText('La solicitud ya está resuelta')).toBeVisible()
    await expect(resolver(b.page)).toHaveCount(0)
    await capturarEstado(b.page, 'F2', 'b-expediente-ya-resuelto')

    // A, con sesión nueva (equivale a cerrar y reabrir la app): ve UNA participación en ese grupo.
    const aNueva = await ClienteApi.crear(mundo.participante)
    await aNueva.ingresar()
    const mias = (await aNueva.misParticipaciones()).cuerpo.filter((p) => p.grupoId === s.grupoId)
    const participacion = unica(mias, 'A tiene una sola participación en el grupo')
    expect(participacion.participanteId).toBe(resolucion.participanteId)
    test.info().annotations.push({ type: 'ESTADO-REAL', description: `participación de A tras aceptar: ${participacion.estado}` })
    await aNueva.cerrar()

    // Reintento de B con la MISMA clave: misma decisión, no una segunda membresía. Con OTRA clave: rechazo claro.
    const claveOriginal = (await porClave(b.obs, s.solicitudId)) ?? undefined
    if (claveOriginal) {
      const reintento = await api.b.resolver(s.solicitudId, 'ACEPTAR', 'Evidencia suficiente; hay cupos y el perfil es compatible.', 1, s.propuesta!.id, claveOriginal)
      expect(reintento.estado, 'mismo envío con la misma clave: se reconoce').toBe(200)
      expect(reintento.cuerpo.id).toBe(resolucion.id)
    }
    const otraClave = await api.b.resolver(s.solicitudId, 'ACEPTAR', 'Otro intento.', 1, s.propuesta!.id)
    expect(otraClave.estado).toBe(422)
    expect(otraClave.cuerpo.mensaje).toMatch(/ya fue resuelta|cambió/)
    expect((await api.a.misParticipaciones()).cuerpo.filter((p) => p.grupoId === s.grupoId), 'sigue habiendo una sola participación').toHaveLength(1)

    // Consola y red de B: solo el 401 del refresco de sesión que `prepararArranque` responde a propósito (no hay cookie), y el 422 del reintento
    // NO pasa por el navegador (se hizo por API).
    expect(b.obs.consolaInesperada(PERMITIDAS_DEL_INGRESO), `consola de B: ${b.obs.consola.join(' | ')}`).toEqual([])
    expect(b.obs.inesperadas(PERMITIDAS_DEL_INGRESO)).toEqual([])
  })
})

async function irAEntrada(page: import('@playwright/test').Page): Promise<void> {
  await page.getByRole('link', { name: /Solicitudes de ingreso/ }).first().click()
  await expect(page.getByRole('heading', { name: 'Abrir una solicitud' })).toBeVisible()
}

/** La clave de idempotencia con la que el navegador de B mandó la resolución de esta solicitud. */
async function porClave(obs: import('./apoyo/observador').Observador, solicitudId: string): Promise<string | null> {
  const p = obs.peticiones.find((x) => x.metodo === 'POST' && x.url.includes(`/grupos/solicitudes/${solicitudId}/resoluciones`))
  if (!p) return null
  return (JSON.parse(p.cabeceras) as Record<string, string>)['idempotency-key'] ?? null
}
