import { HttpTestingController } from '@angular/common/http/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import { PROPUESTA, RESOLUCION, URL_BANDEJA, URL_HISTORIAL, URL_RESOLVER, armarBanco, boton, texto } from '../../../../pruebas/pantalla-de-admision.apoyo'

/**
 * **Prueba de UI contra HTTP simulado** (HttpTestingController): ejercita la pantalla y su cliente, NO prueba integración
 * con `grupos`. El envío y la recuperación de red están en `formulario-de-resolucion.spec.ts`; la decisión directa de
 * backoffice (sin propuesta), en `pantalla-de-admision.decision-directa.spec.ts`.
 */
describe('PantallaDeAdmision · [UI contra HTTP simulado] H12.S1.M1', () => {
  let http: HttpTestingController
  let montar: ReturnType<typeof armarBanco>['montar']
  let cargar: ReturnType<typeof armarBanco>['cargar']
  let recargar: ReturnType<typeof armarBanco>['recargar']

  beforeEach(() => {
    ;({ http, montar, cargar, recargar } = armarBanco())
  })

  it('cargando: avisa que está trayendo el expediente, sin formulario', () => {
    const f = montar()
    expect((f.nativeElement as HTMLElement).querySelector('[role="status"]')?.getAttribute('aria-label')).toContain('Cargando')
    expect(boton(f, 'Resolver solicitud')).toBeUndefined()
    http.expectOne(URL_HISTORIAL).flush([])
  })

  it('con datos: la evidencia del algoritmo es una recomendación, y se ve la propuesta del administrador con el formulario', async () => {
    const f = montar()
    await cargar(f)
    expect(texto(f)).toContain('Evidencia del algoritmo')
    expect(texto(f)).toContain('Es una recomendación')
    expect(texto(f)).toContain('Propuesta del administrador')
    expect(texto(f)).toContain(PROPUESTA.motivo)
    expect(boton(f, 'Resolver solicitud')).toBeDefined()
  })

  it('error: mensaje en español accionable, sin el código ni el texto crudo del backend', async () => {
    const f = montar()
    http.expectOne(URL_HISTORIAL).flush({ codigo: 'AP-CU68-06', mensaje: 'crudo del backend', trazaId: 'traza-1' }, { status: 422, statusText: 'x' })
    http.match(URL_BANDEJA).forEach((r) => r.flush([]))
    await f.whenStable()
    f.detectChanges()
    expect(texto(f)).toContain('Actualizá el expediente')
    expect(texto(f)).not.toContain('crudo del backend')
    expect(boton(f, 'Volver a intentar')).toBeDefined()
  })

  it('sin el permiso de resolver ve el expediente pero ninguna acción de resolución (roles sin acciones ajenas)', async () => {
    const f = montar(['CUMPLIMIENTO_CASOS'])
    await cargar(f)
    expect(texto(f)).toContain(PROPUESTA.motivo)
    expect(texto(f)).toContain('Podés ver el expediente')
    expect(boton(f, 'Resolver solicitud')).toBeUndefined()
    expect((f.nativeElement as HTMLElement).querySelector('input[type="radio"]')).toBeNull()
  })

  it('si la propuesta la hizo la misma persona, no se le ofrece resolver', async () => {
    const f = montar(['ADMIN_PLATAFORMA'], PROPUESTA.actorId)
    await cargar(f)
    expect(texto(f)).toContain('La resolución la tiene que dar otra persona')
    expect(boton(f, 'Resolver solicitud')).toBeUndefined()
  })

  it('una solicitud ya resuelta se muestra cerrada, sin formulario, y avisa que la persona todavía no es integrante', async () => {
    const f = montar()
    await cargar(f, [PROPUESTA, RESOLUCION])
    expect(texto(f)).toContain('Resolución de backoffice')
    expect(texto(f)).toContain('La solicitud ya está resuelta')
    expect(texto(f)).toContain('todavía no es integrante')
    expect(boton(f, 'Resolver solicitud')).toBeUndefined()
  })

  it('al resolver, vuelve a pedir el historial y muestra la solicitud cerrada con la confirmación', async () => {
    const f = montar()
    await cargar(f)
    const formulario = f.debugElement.query((d) => d.name === 'ap-formulario-de-resolucion').componentInstance
    formulario['decision'].set('ACEPTAR')
    formulario['motivo'].set('Evidencia suficiente.')
    formulario['enviar']()
    http.expectOne(URL_RESOLVER).flush(RESOLUCION)
    await recargar(f, [PROPUESTA, RESOLUCION])
    expect(texto(f)).toContain('Resolución registrada. La solicitud quedó cerrada.')
    expect(boton(f, 'Resolver solicitud')).toBeUndefined()
  })

  it('si el servidor dice que la solicitud cambió, vuelve a pedir el historial y conserva lo escrito', async () => {
    const f = montar()
    await cargar(f)
    const formulario = f.debugElement.query((d) => d.name === 'ap-formulario-de-resolucion').componentInstance
    formulario['decision'].set('RECHAZAR')
    formulario['motivo'].set('Conservar este texto')
    formulario['enviar']()
    http.expectOne(URL_RESOLVER).flush({ codigo: 'AP-CU68-06', mensaje: 'x', trazaId: 't' }, { status: 422, statusText: 'x' })
    await recargar(f, [PROPUESTA, { ...PROPUESTA, id: 'nueva-propuesta', revision: PROPUESTA.revision + 1 }])
    expect(texto(f)).toContain('Actualizamos el expediente')
    expect(formulario['motivo']()).toBe('Conservar este texto')
  })
})
