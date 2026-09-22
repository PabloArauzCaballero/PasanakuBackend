import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { axe } from 'vitest-axe'
import { beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { Sesion } from '../../../nucleo/sesion'
import { PantallaDeCobros } from './pantalla-de-cobros'

describe('PantallaDeCobros · accesibilidad', () => {
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

  it('sin violaciones serias en la tabla de cuentas por cobrar', async () => {
    TestBed.inject(Sesion).abrir('t', ['CONTABILIDAD_ERP_COBRAR'], 'tesoreria')
    const fixture = TestBed.createComponent(PantallaDeCobros)
    fixture.detectChanges()
    TestBed.inject(HttpTestingController)
      .match(() => true)
      .forEach((p) => p.flush([]))
    await fixture.whenStable()
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })
})
