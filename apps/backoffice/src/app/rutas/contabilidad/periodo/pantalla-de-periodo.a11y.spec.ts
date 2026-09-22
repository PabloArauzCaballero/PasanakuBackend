import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { axe } from 'vitest-axe'
import { beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { Sesion } from '../../../nucleo/sesion'
import type { PeriodoContable } from '../dominio/cu100-periodos'
import { PantallaDePeriodo } from './pantalla-de-periodo'

const EJERCICIO = '11111111-1111-4111-8111-111111111111'

const PERIODOS: PeriodoContable[] = [
  {
    periodoId: 'p-1',
    ejercicioFiscalId: EJERCICIO,
    anio: 2026,
    mes: 1,
    nombre: 'enero 2026',
    estado: 'CERRADO',
    cerradoEn: '2026-02-01T10:00:00Z',
    totalDebe: { monto: '12000.00', moneda: 'BOB' },
    totalHaber: { monto: '12000.00', moneda: 'BOB' },
  },
  {
    periodoId: 'p-2',
    ejercicioFiscalId: EJERCICIO,
    anio: 2026,
    mes: 2,
    nombre: 'febrero 2026',
    estado: 'ABIERTO',
    cerradoEn: null,
    totalDebe: { monto: '8400.00', moneda: 'BOB' },
    totalHaber: { monto: '8400.00', moneda: 'BOB' },
  },
]

describe('PantallaDePeriodo · accesibilidad', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting(), { provide: GATEWAY, useValue: 'http://gw/api/v1' }],
    })
  })

  it('sin violaciones serias con un mes cerrado y otro abierto a la vez', async () => {
    TestBed.inject(Sesion).abrir('t', ['CONTABILIDAD_ERP_CERRAR'], 'contabilidad')
    const fixture = TestBed.createComponent(PantallaDePeriodo)
    fixture.componentRef.setInput('ejercicioId', EJERCICIO)
    fixture.detectChanges()
    TestBed.inject(HttpTestingController).expectOne(`http://gw/api/v1/erp/ejercicios/${EJERCICIO}/periodos`).flush(PERIODOS)
    fixture.detectChanges()
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })
})
