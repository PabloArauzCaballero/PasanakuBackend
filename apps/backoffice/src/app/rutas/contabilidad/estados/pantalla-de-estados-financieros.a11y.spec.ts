import { provideHttpClient } from '@angular/common/http'
import { provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { axe } from 'vitest-axe'
import { beforeEach, describe, expect, it } from 'vitest'
import { ejemploDe } from '@aportaya/simulado'
import { GATEWAY } from '../../../nucleo/gateway'
import type { EstadoFinancieroGenerado } from '../dominio/cu106-estados-financieros'
import { PantallaDeEstadosFinancieros } from './pantalla-de-estados-financieros'

const GENERADO = ejemploDe('erp', 'generarEstadoFinanciero', 'ok').cuerpo as EstadoFinancieroGenerado

describe('PantallaDeEstadosFinancieros · accesibilidad', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting(), { provide: GATEWAY, useValue: 'http://gw/api/v1' }],
    })
  })

  it('sin violaciones serias antes de generar, con el aviso de provisorio a la vista', async () => {
    const fixture = TestBed.createComponent(PantallaDeEstadosFinancieros)
    fixture.componentRef.setInput('periodoId', 'p-1')
    fixture.detectChanges()
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })

  it('sin violaciones serias con el estado ya generado y su hash a la vista', async () => {
    const fixture = TestBed.createComponent(PantallaDeEstadosFinancieros)
    fixture.componentRef.setInput('periodoId', 'p-1')
    fixture.componentRef.setInput('periodoCerrado', true)
    fixture.detectChanges()
    fixture.componentInstance['generado'].set(GENERADO)
    fixture.detectChanges()
    expect(fixture.nativeElement.textContent).toContain(GENERADO.hashContenido)
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })
})
