import { test, expect } from './apoyo/escenario'
import { capturarEstado } from './apoyo/capturas'
import { unica } from './apoyo/flujo'

/**
 * F1 · INVITACIÓN: quien administra el grupo emite una invitación por enlace; A abre el enlace en el sitio web (el secreto sale de la URL y no
 * queda en storage ni en peticiones posteriores), pide ingreso, y el enlace se consume UNA sola vez.
 *
 * Qué hace cada actor (y por qué así):
 *  - Organizador (API): emitir la invitación es del ORGANIZADOR del grupo (`@Permiso("GRUPO_ADMINISTRAR")`), no de backoffice, y no hay pantalla
 *    en apps/web ni en apps/backoffice para hacerlo (es de la app móvil) → se hace por la API, con su propia sesión.
 *  - A (navegador, apps/web): abre el enlace. El sitio NO tiene sesión de participantes (invitacion.ts): «sigue en la app». Por eso la solicitud de
 *    ingreso (`canje`) la hace el cliente HTTP de A, con la sesión de A, que es lo que hace la app móvil (que no se puede correr en esta máquina).
 */
test.describe('F1 · invitación por enlace de un solo uso', () => {
  test('el secreto sale de la URL y de toda petición; A pide ingreso; el enlace se consume una sola vez', async ({ a, mundo, api }) => {
    // 1) El organizador emite la invitación para el teléfono de A.
    const inv = await api.organizador.invitar(mundo.grupoId, mundo.participante.telefono)
    expect(inv.estado, `invitar: ${inv.texto.slice(0, 200)}`).toBe(201)
    const { invitacionId, token } = inv.cuerpo
    expect(token).toMatch(/^[0-9a-f]{64}$/)

    // 2) A abre el enlace en el sitio web (contexto de navegador propio).
    const antes = a.obs.marca()
    await a.page.goto(`/invitacion/${mundo.grupoId}/${invitacionId}#t=${token}`)
    await expect(a.page.getByRole('status').filter({ hasText: 'Tu invitación está lista' })).toBeVisible()
    await expect(a.page).toHaveURL(new RegExp(`/invitacion/${mundo.grupoId}/${invitacionId}$`))
    expect(a.page.url(), 'el secreto no queda en la barra de direcciones').not.toContain(token)
    await expect(a.page.getByText('no entrás automáticamente')).toBeVisible()

    const almacenado = await a.page.evaluate(() => JSON.stringify({ local: { ...localStorage }, sesion: { ...sessionStorage }, cookies: document.cookie }))
    expect(almacenado, 'ni en localStorage, ni en sessionStorage, ni en cookies').not.toContain(token)
    await expect(a.page.locator('body')).not.toContainText(token)
    expect(a.obs.conSecreto(token, antes), 'ninguna petición posterior lleva el secreto (url, cuerpo ni cabeceras)').toEqual([])
    expect(await a.contexto.cookies(), 'el sitio no escribe cookies con el secreto').not.toContainEqual(expect.objectContaining({ value: expect.stringContaining(token) }))
    await capturarEstado(a.page, 'F1', 'a-invitacion-lista')

    // 3) Recargar NO devuelve el secreto: ya no está en ninguna parte (se consumió de la URL y vive solo en memoria).
    await a.page.reload()
    await expect(a.page.getByRole('alert').filter({ hasText: 'no trae un código de invitación' })).toBeVisible()
    await capturarEstado(a.page, 'F1', 'a-recarga-sin-codigo')

    // 4) A pide ingreso con su propia sesión. Estado persistido: la solicitud queda PENDIENTE en la cola del organizador (no hay membresía todavía).
    const canje = await api.a.canjear(mundo.grupoId, invitacionId, token)
    expect(canje.estado, `canjear: ${canje.texto.slice(0, 200)}`).toBe(200)
    expect(canje.cuerpo.estado).toBe('PENDIENTE_BACKOFFICE')
    const cola = await api.organizador.solicitudesPendientes(mundo.grupoId)
    expect(cola.estado).toBe(200)
    const pendiente = unica(cola.cuerpo, 'una sola solicitud pendiente en el grupo')
    expect(pendiente.solicitudId).toBe(canje.cuerpo.solicitudId)
    expect((await api.a.misParticipaciones()).cuerpo, 'pedir ingreso NO activa la membresía').toEqual([])

    // 5) Segundo uso del enlace con OTRA clave: falla con un mensaje claro y no crea otra solicitud.
    const segundo = await api.a.canjear(mundo.grupoId, invitacionId, token)
    expect(segundo.estado).toBe(422)
    expect(segundo.cuerpo.codigo).toBe('AP-CU69-05')
    expect(segundo.cuerpo.mensaje).toMatch(/ya no es v[aá]lida/)
    // …y otra persona que consiguiera el enlace tampoco lo usa (la invitación es para el teléfono de A).
    const ajeno = await api.a2.canjear(mundo.grupoId, invitacionId, token)
    expect(ajeno.estado).toBe(422)
    expect(ajeno.cuerpo.codigo).toBe('AP-CU69-05')
    const despues = await api.organizador.solicitudesPendientes(mundo.grupoId)
    expect(despues.cuerpo, 'sigue habiendo una sola solicitud, la de A').toHaveLength(1)

    expect(a.obs.consolaInesperada(), `consola de A: ${a.obs.consola.join(' | ')}`).toEqual([])
    expect(a.obs.inesperadas(), 'red de A').toEqual([])
  })

  test('el reintento con la MISMA clave devuelve la misma solicitud (la respuesta se perdió, no se duplica)', async ({ mundo, api }) => {
    const inv = await api.organizador.invitar(mundo.grupoId, mundo.participante.telefono)
    expect(inv.estado).toBe(201)
    const clave = crypto.randomUUID()
    const uno = await api.a.canjear(mundo.grupoId, inv.cuerpo.invitacionId, inv.cuerpo.token, clave)
    const dos = await api.a.canjear(mundo.grupoId, inv.cuerpo.invitacionId, inv.cuerpo.token, clave)
    expect([uno.estado, dos.estado]).toEqual([200, 200])
    expect(dos.cuerpo.solicitudId, 'misma solicitud').toBe(uno.cuerpo.solicitudId)
    unica((await api.organizador.solicitudesPendientes(mundo.grupoId)).cuerpo, 'una sola solicitud')
  })

  test('rechazos del canje: sin sesión, secreto mal formado, secreto de otra invitación y clave ausente', async ({ mundo, api }) => {
    const inv = await api.organizador.invitar(mundo.grupoId, mundo.participante.telefono)
    const { invitacionId, token } = inv.cuerpo
    // Secreto mal formado → 400/422 y nada se consume.
    expect((await api.a.canjear(mundo.grupoId, invitacionId, 'no-es-hex')).estado).toBeGreaterThanOrEqual(400)
    // Secreto de otro enlace.
    expect((await api.a.canjear(mundo.grupoId, invitacionId, 'ab'.repeat(32))).estado).toBe(422)
    // Sin Idempotency-Key.
    const sinClave = await api.a.enviar('POST', `grupos/${mundo.grupoId}/invitaciones/${invitacionId}/canje`, { token, cuposSolicitados: 1 })
    expect(sinClave.estado).toBeGreaterThanOrEqual(400)
    // Nada de eso consumió el enlace: el uso legítimo todavía funciona.
    expect((await api.a.canjear(mundo.grupoId, invitacionId, token)).estado).toBe(200)
  })

  test('segundo uso en la web: el enlace consumido se sigue viendo «listo» pero la pantalla declara que no puede verificar la vigencia', async ({ a, mundo, api }) => {
    const inv = await api.organizador.invitar(mundo.grupoId, mundo.participante.telefono)
    const { invitacionId, token } = inv.cuerpo
    expect((await api.a.canjear(mundo.grupoId, invitacionId, token)).estado).toBe(200)
    await a.page.goto(`/invitacion/${mundo.grupoId}/${invitacionId}#t=${token}`)
    // El sitio es estático y sin sesión: no puede saber que el enlace ya se usó. El rechazo con mensaje claro llega recién en la app.
    await expect(a.page.getByRole('status').filter({ hasText: 'Tu invitación está lista' })).toBeVisible()
    // Límite declarado en pantalla (F4): la web lo dice en vez de aparentar que verificó.
    await expect(a.page.getByRole('note').filter({ hasText: 'no podemos saber si el enlace sigue vigente' })).toBeVisible()
    test.info().annotations.push({ type: 'LIMITE', description: 'apps/web/src/app/paginas/invitacion/invitacion.ts: ni consultarInvitacionPorEnlace ni validarTokenDeInvitacion responden sin la sesión del teléfono invitado; la pantalla declara que no puede verificar la vigencia.' })
    await capturarEstado(a.page, 'F1', 'a-enlace-ya-consumido-se-ve-listo', { viewports: ['movil'], temas: ['light'] })
  })
})
