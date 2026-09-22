import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { ejemploDe } from '@aportaya/simulado'
import { axe } from 'vitest-axe'
import { beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../nucleo/gateway'
import { erroresInterceptor } from '../nucleo/errores.interceptor'
import { CalculadoraDePlazo } from './calculadora-de-plazo'

/** Los cuatro estados de la única lectura pública con contrato (CU-59), contra su ejemplo. */
describe('CalculadoraDePlazo', () => {
  let http: HttpTestingController
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(withInterceptors([erroresInterceptor])), provideHttpClientTesting(), { provide: GATEWAY, useValue: 'http://gw/api/v1' }],
    })
    http = TestBed.inject(HttpTestingController)
  })

  async function consultar() {
    const fixture = TestBed.createComponent(CalculadoraDePlazo)
    fixture.detectChanges()
    const desde = fixture.nativeElement.querySelector('#desde') as HTMLInputElement
    desde.value = '2026-09-09'
    desde.dispatchEvent(new Event('input'))
    ;(fixture.nativeElement.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'))
    fixture.detectChanges()
    return fixture
  }

  it('vacío antes de consultar: dice qué hacer', async () => {
    const fixture = TestBed.createComponent(CalculadoraDePlazo)
    fixture.detectChanges()
    expect(fixture.nativeElement.textContent).toContain('Elegí una fecha para calcular.')
    http.expectNone(() => true)
  })

  it('cargando y éxito: pide con desde, dias y alcance, y muestra la fecha límite del contrato', async () => {
    const fixture = await consultar()
    expect(fixture.nativeElement.querySelector('[role="status"]')?.getAttribute('aria-label')).toBe('Calculando el plazo')
    const peticion = http.expectOne((r) => r.url.endsWith('/grupos/calendario/calcular'))
    expect(peticion.request.params.get('alcance')).toBe('NACIONAL')
    expect(peticion.request.params.get('dias')).toBe('5')
    const ejemplo = ejemploDe('grupos', 'calcularPlazoHabil', 'ok')
    peticion.flush(ejemplo.cuerpo as object)
    await fixture.whenStable()
    expect(fixture.nativeElement.textContent).toContain(`Vence el ${(ejemplo.cuerpo as { fechaLimite: string }).fechaLimite}`)
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })

  it('error: mensaje humano y reintento', async () => {
    const fixture = await consultar()
    http.expectOne(() => true).flush({ codigo: 'AP-CU59-01', mensaje: 'x', trazaId: 't' }, { status: 422, statusText: 'Unprocessable' })
    await fixture.whenStable()
    expect(fixture.nativeElement.querySelectorAll('button')[1]?.textContent?.trim()).toBe('Volver a intentar')
  })

  it('sin red: no se queda cargando', async () => {
    const fixture = await consultar()
    http.expectOne(() => true).error(new ProgressEvent('error'), { status: 0 })
    await fixture.whenStable()
    expect(fixture.nativeElement.textContent).toContain('No hay conexión')
  })
})
