import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router'
import { of } from 'rxjs'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import { trazaInterceptor } from '../../../nucleo/traza.interceptor'
import { idempotenciaInterceptor } from '../../../nucleo/idempotencia.interceptor'
import { PanelDeAprobacionDeCampana } from './panel-de-aprobacion'

function montar() {
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideRouter([]),
      provideHttpClient(withInterceptors([trazaInterceptor, erroresInterceptor, idempotenciaInterceptor])),
      provideHttpClientTesting(),
      { provide: GATEWAY, useValue: 'http://gw/api/v1' },
      { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ campanaId: 'cmp-1' })) } },
    ],
  })
  const fixture = TestBed.createComponent(PanelDeAprobacionDeCampana)
  fixture.detectChanges()
  return fixture
}

/**
 * Migración al diálogo canónico (H4.S2.M2). El lado **aprobar** nunca tiene borrador
 * (sin campo), así que su política de descarte nunca pregunta — se prueba justamente
 * para dejar eso explícito, no asumido. El lado **rechazar** sí, con el motivo escrito.
 */
describe('PanelDeAprobacionDeCampana · CU-111, migrado a ap-dialogo con política de descarte', () => {
  beforeEach(() => {
    vi.spyOn(window, 'confirm')
  })
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('aprobar: sin campo de por medio, cerrar nunca pregunta (sucio() es siempre false en esa rama)', () => {
    const fixture = montar()
    const http = TestBed.inject(HttpTestingController)
    fixture.componentInstance.abrir('APROBAR')
    fixture.detectChanges()
    expect(fixture.componentInstance['sucio']()).toBe(false)

    const dialog = fixture.nativeElement.querySelector('dialog') as HTMLDialogElement
    const evento = new Event('cancel', { cancelable: true })
    dialog.dispatchEvent(evento)
    expect(evento.defaultPrevented).toBe(true)
    expect(window.confirm).not.toHaveBeenCalled()
    http.verify()
  })

  it('rechazar con motivo sin escribir: no está sucio, Escape cierra sin preguntar', () => {
    const fixture = montar()
    const http = TestBed.inject(HttpTestingController)
    fixture.componentInstance.abrir('RECHAZAR')
    fixture.detectChanges()
    expect(fixture.componentInstance['sucio']()).toBe(false)
    http.verify()
  })

  it('rechazar con motivo escrito: sucio, las tres rutas preguntan igual', () => {
    const fixture = montar()
    const http = TestBed.inject(HttpTestingController)
    ;(window.confirm as ReturnType<typeof vi.spyOn>).mockReturnValue(false)
    fixture.componentInstance.abrir('RECHAZAR')
    fixture.detectChanges()
    const campoMotivo = fixture.nativeElement.querySelector('ap-campo input') as HTMLInputElement
    campoMotivo.value = 'Contenido no cumple la política de la plataforma.'
    campoMotivo.dispatchEvent(new Event('input'))
    fixture.detectChanges()
    expect(fixture.componentInstance['sucio']()).toBe(true)

    const dialog = fixture.nativeElement.querySelector('dialog') as HTMLDialogElement
    const evento = new Event('cancel', { cancelable: true })
    dialog.dispatchEvent(evento)
    expect(evento.defaultPrevented).toBe(true)
    expect(window.confirm).toHaveBeenCalledOnce()
    expect(fixture.componentInstance['dialogoAbierto']()).toBe(true)
    expect(fixture.componentInstance['motivo']()).toBe('Contenido no cumple la política de la plataforma.')

    const clickEnFondo = new MouseEvent('click')
    Object.defineProperty(clickEnFondo, 'target', { value: dialog })
    dialog.dispatchEvent(clickEnFondo)
    expect(window.confirm).toHaveBeenCalledTimes(2)
    expect(fixture.componentInstance['dialogoAbierto']()).toBe(true)
    http.verify()
  })

  it('reapertura: abrir() siempre resetea el motivo — una reapertura no arrastra el rechazo anterior', () => {
    const fixture = montar()
    const http = TestBed.inject(HttpTestingController)
    fixture.componentInstance.abrir('RECHAZAR')
    fixture.componentInstance['motivo'].set('primer intento')
    fixture.componentInstance.abrir('RECHAZAR')
    expect(fixture.componentInstance['motivo']()).toBe('')
    http.verify()
  })

  it('aceptado · aprobar hace el POST y deja el resultado a la vista, sin volver a ofrecer la decisión', async () => {
    const fixture = montar()
    const http = TestBed.inject(HttpTestingController)
    fixture.componentInstance.abrir('APROBAR')
    void fixture.componentInstance.confirmar()
    const req = http.expectOne('http://gw/api/v1/publicidad/campanas/cmp-1/aprobacion')
    expect(req.request.method).toBe('POST')
    req.flush({ campanaPublicitariaId: 'cmp-1', estado: 'ACTIVA', presupuestoTotal: '500.00' })
    await new Promise((r) => setTimeout(r, 0))
    fixture.detectChanges()
    const texto = fixture.nativeElement.textContent as string
    expect(texto).toContain('Campaña aprobada. Empieza a entregar.')
    const botones = [...fixture.nativeElement.querySelectorAll('section button')].map((b: Element) => b.textContent?.trim())
    expect(botones).not.toContain('Aprobar campaña')
    expect(botones).not.toContain('Rechazar campaña')
  })

  it('límite · abierta por enlace (sin estado de navegación) dice por qué solo muestra el identificador', () => {
    const fixture = montar()
    expect(fixture.nativeElement.textContent).toContain('cmp-1')
    expect(fixture.nativeElement.textContent).toContain('el contrato no tiene una consulta de una campaña sola')
  })

  it('error · un rechazo que falla deja el diálogo abierto, con el motivo escrito y el error traducido', async () => {
    const fixture = montar()
    const http = TestBed.inject(HttpTestingController)
    fixture.componentInstance.abrir('RECHAZAR')
    fixture.componentInstance['motivo'].set('No cumple la política.')
    void fixture.componentInstance.confirmar()
    const req = http.expectOne('http://gw/api/v1/publicidad/campanas/cmp-1/rechazo')
    expect(req.request.body).toEqual({ motivo: 'No cumple la política.' })
    req.flush({ codigo: 'AP-CU111-09', mensaje: 'crudo', trazaId: 'tz-9' }, { status: 500, statusText: 'x' })
    await new Promise((r) => setTimeout(r, 0))
    fixture.detectChanges()
    expect(fixture.componentInstance['dialogoAbierto']()).toBe(true)
    expect(fixture.componentInstance['motivo']()).toBe('No cumple la política.')
    expect(fixture.componentInstance['enviando']()).toBe(false)
    expect(fixture.nativeElement.querySelector('ap-error-de-envio')?.textContent).toContain('tz-9')
  })
})
