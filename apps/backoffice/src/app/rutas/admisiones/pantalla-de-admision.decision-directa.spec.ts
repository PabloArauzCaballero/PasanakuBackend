import { HttpTestingController } from '@angular/common/http/testing'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import { SOLICITUD, URL_BANDEJA, URL_DIRECTA, armarBanco, ejemplo, texto } from '../../../../pruebas/pantalla-de-admision.apoyo'

/**
 * La decisión directa de backoffice (CU-68, #235): una solicitud sin propuesta del administrador del grupo. Parte de
 * `pantalla-de-admision.spec.ts`, que se dividió para respetar el tope de 200 líneas por archivo.
 */
describe('PantallaDeAdmision · decisión directa de backoffice [UI contra HTTP simulado] H12.S1.M1', () => {
  let http: HttpTestingController
  let montar: ReturnType<typeof armarBanco>['montar']
  let cargar: ReturnType<typeof armarBanco>['cargar']

  beforeEach(() => {
    ;({ http, montar, cargar } = armarBanco())
  })

  /** Fila mínima de la bandeja (`GET /grupos/solicitudes`) con el estado de esta solicitud. Datos sintéticos. */
  const filaDeBandeja = (estado: string) => [
    { solicitudId: SOLICITUD, usuarioId: 'u-1', postulante: 'Postulante sintético', grupoId: 'g-1', grupoCodigo: 'GR-TEST', cuposSolicitados: 1, estado, fechaSolicitud: '2026-10-28T12:00:00Z' },
  ]
  function decidirDirecto(f: ComponentFixture<unknown>, decision: string, motivo: string) {
    const formulario = f.debugElement.query((d) => d.name === 'ap-formulario-de-decision-directa').componentInstance
    formulario['decision'].set(decision)
    formulario['motivo'].set(motivo)
    formulario['enviar']()
    f.detectChanges()
    return formulario
  }

  it('sin propuesta (correcto): backoffice decide directo, con clave de idempotencia, y ve la confirmación', async () => {
    const f = montar()
    await cargar(f, ejemplo('vacio'), filaDeBandeja('PENDIENTE'))
    expect(texto(f)).toContain('Podés aceptar o rechazar directamente')
    decidirDirecto(f, 'ACEPTAR', 'Perfil completo y cupo libre.')
    const pedido = http.expectOne(URL_DIRECTA)
    expect(pedido.request.method).toBe('POST')
    expect(pedido.request.body).toEqual({ decision: 'ACEPTAR', motivo: 'Perfil completo y cupo libre.' })
    expect(pedido.request.headers.get('Idempotency-Key')).toBeTruthy()
    pedido.flush({ solicitudId: SOLICITUD, estado: 'APROBADA', participanteId: 'p-1', cupoId: 'c-1' })
    f.detectChanges()
    TestBed.tick()
    http.match(URL_BANDEJA).forEach((r) => r.flush(filaDeBandeja('ACEPTADA')))
    await f.whenStable()
    f.detectChanges()
    expect(texto(f)).toContain('Solicitud aceptada')
  })

  it('sin propuesta (límite): motivo de 4 o de 301 caracteres no se manda', async () => {
    const f = montar()
    await cargar(f, ejemplo('vacio'))
    const formulario = decidirDirecto(f, 'RECHAZAR', 'abcd')
    expect(formulario['errorMotivo']()).toContain('al menos 5')
    decidirDirecto(f, 'RECHAZAR', 'x'.repeat(301))
    expect(formulario['errorMotivo']()).toContain('hasta 300')
    http.expectNone(URL_DIRECTA)
  })

  it('sin propuesta (inválido): si el servidor exige propuesta, explica que hay que esperarla y no ofrece decidir', async () => {
    const f = montar()
    await cargar(f, ejemplo('vacio'))
    decidirDirecto(f, 'ACEPTAR', 'Perfil completo.')
    http.expectOne(URL_DIRECTA).flush({ codigo: 'AP-CU68-15', mensaje: 'x', trazaId: 't' }, { status: 422, statusText: 'x' })
    await f.whenStable()
    f.detectChanges()
    expect(texto(f)).toContain('exige que el administrador del grupo proponga primero')
    expect(f.debugElement.query((d) => d.name === 'ap-formulario-de-decision-directa')).toBeNull()
  })

  it('sin propuesta y ya aceptada en la bandeja: no ofrece decidir otra vez', async () => {
    const f = montar()
    await cargar(f, ejemplo('vacio'), filaDeBandeja('ACEPTADA'))
    expect(texto(f)).toContain('ya fue aceptada')
    expect(f.debugElement.query((d) => d.name === 'ap-formulario-de-decision-directa')).toBeNull()
  })
})
