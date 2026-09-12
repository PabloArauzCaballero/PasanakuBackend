import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { axe } from 'vitest-axe'
import { beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { Sesion } from '../../../nucleo/sesion'
import { PantallaDePresupuesto } from './pantalla-de-presupuesto'

describe('PantallaDePresupuesto · accesibilidad', () => {
  beforeEach(() => {
    if (!Element.prototype.scrollTo) Element.prototype.scrollTo = () => {}
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: 'http://gw/api/v1' },
      ],
    })
  })

  it('sin violaciones serias, con el aviso de que el presupuesto mide y no bloquea', async () => {
    TestBed.inject(Sesion).abrir('t', ['CONTABILIDAD_ERP_PRESUPUESTO'], 'contabilidad')
    const fixture = TestBed.createComponent(PantallaDePresupuesto)
    fixture.detectChanges()
    TestBed.inject(HttpTestingController)
      .match(() => true)
      .forEach((p) => p.flush([]))
    await fixture.whenStable()
    expect(fixture.nativeElement.textContent).toContain('mide el gasto, no lo bloquea')
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })
})
