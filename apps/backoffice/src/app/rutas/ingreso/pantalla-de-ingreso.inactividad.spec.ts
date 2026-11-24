import { provideHttpClient } from '@angular/common/http'
import { provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router'
import { describe, expect, it } from 'vitest'
import { GATEWAY } from '../../nucleo/gateway'
import { PantallaDeIngreso } from './pantalla-de-ingreso'

function montar(query: Record<string, string>): HTMLElement {
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideRouter([]),
      provideHttpClient(),
      provideHttpClientTesting(),
      { provide: GATEWAY, useValue: 'https://gateway.test/api/v1' },
      { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap(query) } } },
    ],
  })
  const fixture = TestBed.createComponent(PantallaDeIngreso)
  fixture.detectChanges()
  return fixture.nativeElement as HTMLElement
}

/** ADR-038 §1 · quien vuelve al ingreso porque su sesión se cerró sola sabe por qué (estado vacío que orienta, 95.2.2). */
describe('PantallaDeIngreso · cierre por inactividad', () => {
  it('explica el motivo cuando viene del cierre por inactividad', () => {
    const el = montar({ motivo: 'inactividad', volverA: '/operacion/retiros' })
    expect(el.textContent).toContain('pasaron 15 minutos sin actividad')
  })

  it('no dice nada de inactividad en un ingreso normal ni con un motivo desconocido', () => {
    expect(montar({}).textContent).not.toContain('sin actividad')
    TestBed.resetTestingModule()
    expect(montar({ motivo: '<script>' }).textContent).not.toContain('sin actividad')
  })
})
