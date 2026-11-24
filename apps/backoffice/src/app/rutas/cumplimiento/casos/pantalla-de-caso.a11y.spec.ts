import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { provideRouter } from '@angular/router'
import { GATEWAY } from '../../../nucleo/gateway'
import { TestBed } from '@angular/core/testing'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { ServicioBorrador } from '../../../nucleo/borrador'
import { PantallaDeCaso } from './pantalla-de-caso'

class ServicioBorradorEnMemoria {
  async guardar(): Promise<void> {}
  async leer(): Promise<undefined> {
    return undefined
  }
  async borrar(): Promise<void> {}
}

describe('PantallaDeCaso · accesibilidad', () => {
  it('sin violaciones serias, con la causal sin elegir y el botón deshabilitado', async () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideRouter([]), provideHttpClient(), provideHttpClientTesting(), { provide: GATEWAY, useValue: 'http://gw/api/v1' }, { provide: ServicioBorrador, useValue: new ServicioBorradorEnMemoria() }] })
    const fixture = TestBed.createComponent(PantallaDeCaso)
    fixture.componentRef.setInput('casoId', 'caso-a11y')
    fixture.detectChanges()
    await fixture.whenStable()
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })

  it('sin violaciones serias con la ficha cargada: alerta de origen y titular enmascarado', async () => {
    TestBed.resetTestingModule()
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideRouter([]), provideHttpClient(), provideHttpClientTesting(), { provide: GATEWAY, useValue: 'http://gw/api/v1' }, { provide: ServicioBorrador, useValue: new ServicioBorradorEnMemoria() }] })
    const fixture = TestBed.createComponent(PantallaDeCaso)
    fixture.componentRef.setInput('casoId', 'caso-a11y')
    fixture.detectChanges()
    TestBed.inject(HttpTestingController)
      .expectOne('http://gw/api/v1/cumplimiento/casos/caso-a11y')
      .flush({ casoId: 'caso-a11y', codigo: 'LFT-2026-0042', origen: 'ALERTA', estado: 'EN_ANALISIS', prioridad: 'ALTA', abiertoEn: '2026-01-15T10:30:00Z', plazoLimite: '2026-02-14T10:30:00Z', vencido: false, alertaDeOrigen: { alertaId: 'a1', reglaCodigo: 'RM-FRACCIONAMIENTO', tipologia: 'Fraccionamiento de operaciones', severidad: 'ALTA', detectadaEn: '2026-01-14T18:05:00Z' }, alertasEnlazadas: 3, titularEnmascarado: '****7c2e' })
    fixture.detectChanges()
    await fixture.whenStable()
    expect(fixture.nativeElement.textContent).toContain('****7c2e')
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })
})
