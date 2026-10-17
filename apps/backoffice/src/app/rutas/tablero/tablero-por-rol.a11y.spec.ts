import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { ApplicationRef, provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { IndicadorFamiliaEnum, SalidaTableroDimensionEnum } from 'clientes/angular/auditoria'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { GATEWAY } from '../../nucleo/gateway'
import { TableroPorRol } from './tablero-por-rol'

describe('TableroPorRol · accesibilidad', () => {
  for (const tipo of ['financiero', 'comercial', 'sistemas'] as const) {
    it(`${tipo}: sin violaciones serias, con valor, suprimido y sin datos a la vez`, async () => {
      TestBed.configureTestingModule({
        providers: [
          provideZonelessChangeDetection(),
          provideHttpClient(),
          provideHttpClientTesting(),
          { provide: GATEWAY, useValue: 'http://gw/api/v1' },
        ],
      })
      const http = TestBed.inject(HttpTestingController)
      const fixture = TestBed.createComponent(TableroPorRol)
      fixture.componentRef.setInput('tipo', tipo)
      fixture.detectChanges()
      TestBed.inject(ApplicationRef).tick()
      http.expectOne((r) => r.url === 'http://gw/api/v1/indicadores').flush({
        periodo: '2026-09',
        provisorio: true,
        dimension: SalidaTableroDimensionEnum.Global,
        indicadores: [
          { codigo: 'SALDO_CARGADO', nombre: 'Saldo cargado', valor: '1500.00', unidad: 'BOB', familia: IndicadorFamiliaEnum.Finanzas, duenoFamilia: 'Tesorería', definicionVersion: 'v1', suprimidoPorPrivacidad: false, casos: 6, calculadoEn: '2026-10-01T06:15:00Z' },
          { codigo: 'TASA_MORA', nombre: 'Tasa de mora', unidad: 'PORCENTAJE', familia: IndicadorFamiliaEnum.Riesgo, duenoFamilia: 'Riesgos', definicionVersion: 'v1', suprimidoPorPrivacidad: true, minimoCasos: 20, calculadoEn: '2026-10-01T06:15:00Z' },
          { codigo: 'USUARIOS_NUEVOS', nombre: 'Usuarios nuevos', valor: '40', unidad: 'CONTEO', familia: IndicadorFamiliaEnum.Negocio, duenoFamilia: 'Comercial', definicionVersion: 'v1', suprimidoPorPrivacidad: false, casos: 40, calculadoEn: '2026-10-01T06:15:00Z' },
        ],
      })
      if (tipo === 'sistemas') {
        http.expectOne((r) => r.url === 'http://gw/api/v1/indicadores/operacion/endpoints').flush({
          desde: '2026-10-16T04:00:00Z',
          hasta: '2026-10-16T18:00:00Z',
          fuenteDisponible: true,
          endpoints: [{ servicio: 'grupos', metodo: 'GET', ruta: '/grupos/{id}', solicitudes: 300, errores4xx: 2, errores5xx: 1, p95Ms: '120.50' }],
        })
      }
      await fixture.whenStable()
      fixture.detectChanges()
      expect(await axe(fixture.nativeElement)).toHaveNoViolations()
    })
  }
})
