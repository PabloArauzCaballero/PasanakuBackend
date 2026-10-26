import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection, type Type } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { GATEWAY } from '../../nucleo/gateway'
import { Sesion } from '../../nucleo/sesion'
import { PantallaDeConfiguracion } from './pantalla-de-configuracion'
import { PantallaDeMensajeria } from './mensajeria/pantalla-de-mensajeria'
import { PantallaDeReportes } from './reportes/pantalla-de-reportes'
import { PantallaDeTarifario } from './tarifario/pantalla-de-tarifario'

/**
 * H5.S2 · Accesibilidad de las vistas de Configuración con el listado nuevo (barra de filtros, exportar, tabla,
 * paginación), con una fila sintética por listado (doble de `HttpTestingController`, regla 65).
 */
const FILAS: Record<string, unknown> = {
  '/tarifas/tarifarios': { tarifarioId: 't-1', codigo: 'TAR-1', version: 1, nombre: 'Tarifa', estado: 'VIGENTE', monedaBase: 'BOB', vigenteDesde: '2026-11-01T04:00:00Z', diasPreaviso: 30 },
  '/notificaciones/plantillas': { plantillaId: 'p-1', codigo: 'P_1', evento: 'E_1', canal: 'SMS', categoriaProveedor: 'UTILITY', estadoAprobacion: 'APROBADA', activa: true, versionId: 'v-1', version: 1, idioma: 'es', vigenteDesde: '2026-09-01T04:00:00Z' },
  '/reportes': { definicionId: 'd-1', tipo: 'X', nombre: 'Reporte', descripcion: 'Sintético', permisoRequerido: 'AUDITORIA_LEER', contieneDatosSensibles: false },
  '/reportes/ejecuciones': { ejecucionId: 'x-1', definicionId: 'd-1', solicitadoPor: 'o', estado: 'COMPLETADA', filasGeneradas: 1, duracionMs: 1, iniciadaEn: '2026-10-16T12:00:00Z' },
}

async function sinViolaciones(componente: Type<unknown>) {
  if (!Element.prototype.scrollTo) Element.prototype.scrollTo = () => {}
  TestBed.configureTestingModule({
    providers: [provideZonelessChangeDetection(), provideRouter([]), provideHttpClient(), provideHttpClientTesting(), { provide: GATEWAY, useValue: 'http://gw/api/v1' }],
  })
  TestBed.inject(Sesion).abrir('t', ['AUDITORIA_LEER', 'TARIFARIO_PUBLICAR'], 'auditoria')
  const fixture = TestBed.createComponent(componente)
  fixture.detectChanges()
  await new Promise((r) => setTimeout(r, 0))
  for (const p of TestBed.inject(HttpTestingController).match(() => true)) {
    const fila = FILAS[new URL(p.request.url).pathname.replace('/api/v1', '')]
    p.flush({ items: fila ? [fila] : [] })
  }
  for (let i = 0; i < 3; i++) {
    await new Promise((r) => setTimeout(r, 0))
    fixture.detectChanges()
  }
  expect(fixture.nativeElement.querySelector('tbody, .accesos')).not.toBeNull()
  expect(await axe(fixture.nativeElement)).toHaveNoViolations()
}

describe('Configuración · accesibilidad', () => {
  it('inicio con accesos con ícono', () => sinViolaciones(PantallaDeConfiguracion))
  it('tarifario con su listado', () => sinViolaciones(PantallaDeTarifario))
  it('mensajería con pestañas y listado', () => sinViolaciones(PantallaDeMensajeria))
  it('reportes con pestañas y listado', () => sinViolaciones(PantallaDeReportes))
})
