import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { ejemploDe } from '@aportaya/simulado'
import { axe } from 'vitest-axe'
import { beforeEach, describe, expect, it } from 'vitest'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import { GATEWAY } from '../../../nucleo/gateway'
import { idempotenciaInterceptor, olvidarIntentos } from '../../../nucleo/idempotencia.interceptor'
import { Sesion } from '../../../nucleo/sesion'
import { PantallaDeAprobacionDeRetiro } from './pantalla-de-aprobacion-de-retiro'

const ORDEN = '2e4c38bf-2985-4b9d-87c9-332257d1445c'
const URL = `http://gw/api/v1/billetera/retiros/${ORDEN}/aprobacion`

describe('PantallaDeAprobacionDeRetiro · accesibilidad (axe, [UI contra HTTP simulado])', () => {
  let http: HttpTestingController
  beforeEach(() => {
    olvidarIntentos()
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(withInterceptors([idempotenciaInterceptor, erroresInterceptor])),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: 'http://gw/api/v1' },
      ],
    })
    http = TestBed.inject(HttpTestingController)
    TestBed.inject(Sesion).abrir('t', ['RETIRO_APROBAR'], 'TESORERIA', 'tes-1')
  })

  async function abrirYEnviar(respuesta: (p: ReturnType<HttpTestingController['expectOne']>) => void) {
    const fixture = TestBed.createComponent(PantallaDeAprobacionDeRetiro)
    fixture.detectChanges()
    fixture.componentInstance['ordenId'].set(ORDEN)
    fixture.componentInstance['desenlace'].set('RECHAZADA')
    fixture.componentInstance['motivo'].set('Titular no coincide')
    fixture.detectChanges()
    fixture.componentInstance['enviar']()
    fixture.detectChanges()
    respuesta(http.expectOne(URL))
    await fixture.whenStable()
    fixture.detectChanges()
    return fixture
  }

  it('sin violaciones en el formulario inicial', async () => {
    const fixture = TestBed.createComponent(PantallaDeAprobacionDeRetiro)
    fixture.detectChanges()
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })

  it('sin violaciones con los errores de campo a la vista', async () => {
    const fixture = TestBed.createComponent(PantallaDeAprobacionDeRetiro)
    fixture.detectChanges()
    fixture.componentInstance['enviar']()
    fixture.detectChanges()
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })

  it('sin violaciones con la decisión registrada', async () => {
    const f = await abrirYEnviar((p) => p.flush(ejemploDe('nucleo-financiero', 'aprobarRetiro', 'ok').cuerpo as object))
    expect(await axe(f.nativeElement)).toHaveNoViolations()
  })

  it('sin violaciones con el rechazo del servidor (autoaprobación)', async () => {
    const f = await abrirYEnviar((p) => p.flush({ codigo: 'AP-CU11-10', mensaje: '', trazaId: 't' }, { status: 422, statusText: 'x' }))
    expect(await axe(f.nativeElement)).toHaveNoViolations()
  })

  it('sin violaciones con el envío sin confirmar', async () => {
    const f = await abrirYEnviar((p) => p.error(new ProgressEvent('error'), { status: 0 }))
    expect(await axe(f.nativeElement)).toHaveNoViolations()
  })
})
