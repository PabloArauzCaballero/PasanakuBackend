# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: f3-rechazar.aceptacion.e2e.ts >> F3 · rechazar una solicitud (dos usuarios) >> DESEADO: A ve el rechazo con su motivo (hoy no existe cómo)
- Location: e2e\aceptacion\f3-rechazar.aceptacion.e2e.ts:95:7

# Error details

```
Error: expect(received).toBe(expected) // Object.is equality

Expected: 200
Received: 422
```

# Test source

```ts
  3   | import { capturarEstado } from './apoyo/capturas'
  4   | import { PERMITIDAS_DEL_INGRESO } from './apoyo/observador'
  5   | import { abrirExpediente, llenarResolucion, prepararSolicitud, resolver, unica } from './apoyo/flujo'
  6   | 
  7   | /**
  8   |  * F3 · RECHAZAR. B rechaza (contradiciendo la propuesta, con motivo); no queda membresía; B no puede reabrirla sin una nueva revisión.
  9   |  *
  10  |  * BRECHA DE PRODUCTO verificada en el servicio `grupos` (no es del test): la persona rechazada NO tiene cómo enterarse del motivo.
  11  |  *  - `GET /grupos/solicitudes/{id}/decisiones` es «accesible al administrador del grupo y al backoffice» (CU68AceptarIngreso.historial).
  12  |  *  - Nada consume `grupos.admision_resuelta` (no hay notificación): buscado en servicios/<cada servicio>/src/main.
  13  |  *  - `GET /grupos/participaciones` solo lista participaciones vigentes: la rechazada no aparece.
  14  |  * El último test del archivo fija el comportamiento DESEADO («A ve el rechazo con el motivo») con `test.fail`: pasa mientras la brecha exista y
  15  |  * avisa (falla «inesperadamente») el día que se cierre, para que alguien lo convierta en prueba normal.
  16  |  */
  17  | test.describe('F3 · rechazar una solicitud (dos usuarios)', () => {
  18  |   test('B rechaza con motivo; no queda membresía; no se puede reabrir sin una nueva revisión', async ({ b, mundo, api }) => {
  19  |     const s = await prepararSolicitud(api, mundo, { decision: 'ACEPTAR', motivo: 'Perfil compatible según el administrador.' })
  20  |     const motivo = 'La concentración de riesgo del grupo supera el límite; se rechaza pese a la propuesta.'
  21  | 
  22  |     await ingresarBackoffice(b.page, mundo.operador)
  23  |     await abrirExpediente(b.page, s.solicitudId)
  24  |     await expect(b.page.getByText('Propuesta del administrador')).toBeVisible()
  25  | 
  26  |     // Rechazar sin motivo: el error es del campo y no sale ninguna petición.
  27  |     await b.page.getByLabel('Rechazar', { exact: true }).check({ force: true })
  28  |     const marca = b.obs.marca()
  29  |     await resolver(b.page).click()
  30  |     await expect(b.page.getByText('Escribí el motivo')).toBeVisible()
  31  |     expect(b.obs.peticiones.slice(marca).filter((p) => p.metodo === 'POST')).toEqual([])
  32  |     await capturarEstado(b.page, 'F3', 'b-rechazar-sin-motivo')
  33  | 
  34  |     await llenarResolucion(b.page, 'Rechazar', motivo)
  35  |     await capturarEstado(b.page, 'F3', 'b-rechazar-formulario-lleno', { viewports: ['movil', 'escritorio'] })
  36  |     await resolver(b.page).click()
  37  |     await expect(b.page.getByText('Resolución registrada. La solicitud quedó cerrada.')).toBeVisible()
  38  |     await capturarEstado(b.page, 'F3', 'b-rechazo-confirmado')
  39  | 
  40  |     // Estado real persistido.
  41  |     const hist = (await api.b.historial(s.solicitudId)).cuerpo
  42  |     const r = unica(hist.filter((d) => d.fase === 'RESOLUCION'), 'una sola resolución')
  43  |     expect(r.decision).toBe('RECHAZAR')
  44  |     expect(r.motivo).toBe(motivo)
  45  |     expect(r.participanteId, 'rechazar no crea participante').toBeUndefined()
  46  |     expect((await api.a.misParticipaciones()).cuerpo.filter((p) => p.grupoId === s.grupoId), 'NO queda membresía').toEqual([])
  47  |     expect((await api.organizador.solicitudesPendientes(s.grupoId)).cuerpo, 'sale de la cola pendiente del organizador').toEqual([])
  48  | 
  49  |     // Tras «recargar» (sesión nueva de B) el expediente sigue cerrado y sin formulario.
  50  |     await b.page.reload()
  51  |     await ingresarBackoffice(b.page, mundo.operador, `/admisiones/${s.solicitudId}`)
  52  |     await expect(b.page.getByText('La solicitud ya está resuelta')).toBeVisible()
  53  |     await expect(resolver(b.page)).toHaveCount(0)
  54  |     await expect(b.page.getByText(motivo)).toBeVisible()
  55  |     await capturarEstado(b.page, 'F3', 'b-expediente-rechazado')
  56  | 
  57  |     // No se reabre: ni B resolviendo otra vez, ni el administrador proponiendo de nuevo, ni A reusando el enlace.
  58  |     const reabrir = await api.b.resolver(s.solicitudId, 'ACEPTAR', 'Me arrepentí.', 2, s.propuesta!.id)
  59  |     expect(reabrir.estado).toBe(422)
  60  |     expect(reabrir.cuerpo.mensaje).toMatch(/ya fue resuelta|cambió/)
  61  |     const repropuesta = await api.organizador.proponer(s.solicitudId, 'ACEPTAR', 'Otra vez.', 2)
  62  |     expect(repropuesta.estado).toBe(422)
  63  |     const reuso = await api.a.canjear(s.grupoId, s.invitacionId, s.token)
  64  |     expect(reuso.estado, 'el enlace ya se consumió: pedir de nuevo exige una invitación nueva').toBe(422)
  65  |     expect((await api.a.misParticipaciones()).cuerpo.filter((p) => p.grupoId === s.grupoId)).toEqual([])
  66  |     expect(unica((await api.b.historial(s.solicitudId)).cuerpo.filter((d) => d.fase === 'RESOLUCION'), 'sigue habiendo una sola resolución').decision).toBe('RECHAZAR')
  67  | 
  68  |     expect(b.obs.consolaInesperada(PERMITIDAS_DEL_INGRESO), `consola de B: ${b.obs.consola.join(' | ')}`).toEqual([])
  69  |     expect(b.obs.inesperadas(PERMITIDAS_DEL_INGRESO)).toEqual([])
  70  |   })
  71  | 
  72  |   test('el motivo de más de 1000 caracteres se rechaza en el campo y no se envía', async ({ b, mundo, api }) => {
  73  |     const s = await prepararSolicitud(api, mundo, { decision: 'RECHAZAR', motivo: 'Riesgo alto.' })
  74  |     await ingresarBackoffice(b.page, mundo.operador)
  75  |     await abrirExpediente(b.page, s.solicitudId)
  76  |     await llenarResolucion(b.page, 'Rechazar', 'x'.repeat(1001))
  77  |     const marca = b.obs.marca()
  78  |     await resolver(b.page).click()
  79  |     await expect(b.page.getByText('El motivo admite hasta 1000 caracteres.')).toBeVisible()
  80  |     expect(b.obs.peticiones.slice(marca).filter((p) => p.metodo === 'POST')).toEqual([])
  81  |     await capturarEstado(b.page, 'F3', 'b-motivo-demasiado-largo', { viewports: ['movil'], temas: ['light', 'dark'] })
  82  |     // Y el servidor, directamente, también lo rechaza (la validación no es solo del cliente).
  83  |     const directo = await api.b.resolver(s.solicitudId, 'RECHAZAR', 'x'.repeat(1001), 1, s.propuesta!.id)
  84  |     expect(directo.estado).toBe(422)
  85  |   })
  86  | 
  87  |   test('A no puede leer el expediente de su propia solicitud ni decidir sobre ella (separación de funciones)', async ({ mundo, api }) => {
  88  |     const s = await prepararSolicitud(api, mundo, { decision: 'ACEPTAR', motivo: 'Propuesta.' })
  89  |     expect((await api.a.historial(s.solicitudId)).estado, 'A no es administrador ni backoffice').toBeGreaterThanOrEqual(400)
  90  |     expect((await api.a.proponer(s.solicitudId, 'ACEPTAR', 'Me acepto yo.', 1)).estado, 'A no tiene GRUPO_ADMINISTRAR').toBe(403)
  91  |     expect((await api.a.resolver(s.solicitudId, 'ACEPTAR', 'Me acepto yo.', 1, s.propuesta!.id)).estado, 'A no tiene ADMIN_PLATAFORMA').toBe(403)
  92  |     expect((await api.a.misParticipaciones()).cuerpo.filter((p) => p.grupoId === s.grupoId), 'y no ganó nada').toEqual([])
  93  |   })
  94  | 
  95  |   test('DESEADO: A ve el rechazo con su motivo (hoy no existe cómo)', async ({ mundo, api }) => {
  96  |     test.fail(true, 'BRECHA: ningún endpoint ni notificación le entrega a la persona rechazada el resultado ni el motivo (servicios/grupos CU68AceptarIngreso.historial; sin consumidor de grupos.admision_resuelta).')
  97  |     const s = await prepararSolicitud(api, mundo, { decision: 'ACEPTAR', motivo: 'Propuesta.' })
  98  |     expect((await api.b.resolver(s.solicitudId, 'RECHAZAR', 'Motivo visible para la persona.', 1, s.propuesta!.id)).estado).toBe(200)
  99  |     const aNueva = await ClienteApi.crear(mundo.participante)
  100 |     await aNueva.ingresar()
  101 |     const lectura = await aNueva.historial(s.solicitudId)
  102 |     await aNueva.cerrar()
> 103 |     expect(lectura.estado).toBe(200)
      |                            ^ Error: expect(received).toBe(expected) // Object.is equality
  104 |     expect(lectura.texto).toContain('Motivo visible para la persona.')
  105 |   })
  106 | })
  107 | 
```