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

/** Accesibilidad bloqueante en la única isla del sitio: formulario con etiquetas, foco y resultado anunciado. */
describe('CalculadoraDePlazo · accesibilidad', () => {
  let http: HttpTestingController
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(withInterceptors([erroresInterceptor])), provideHttpClientTesting(), { provide: GATEWAY, useValue: 'http://gw/api/v1' }],
    })
    http = TestBed.inject(HttpTestingController)
  })

  it('el formulario vacío no tiene violaciones serias', async () => {
    const fixture = TestBed.createComponent(CalculadoraDePlazo)
    fixture.detectChanges()
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })

  it('el resultado tampoco', async () => {
    const fixture = TestBed.createComponent(CalculadoraDePlazo)
    fixture.detectChanges()
    const desde = fixture.nativeElement.querySelector('#desde') as HTMLInputElement
    desde.value = '2026-09-09'
    desde.dispatchEvent(new Event('input'))
    ;(fixture.nativeElement.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'))
    fixture.detectChanges()
    http.expectOne(() => true).flush(ejemploDe('grupos', 'calcularPlazoHabil', 'ok').cuerpo as object)
    await fixture.whenStable()
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })
})
