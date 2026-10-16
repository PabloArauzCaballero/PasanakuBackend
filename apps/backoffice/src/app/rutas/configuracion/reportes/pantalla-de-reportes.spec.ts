import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { PantallaDeReportes } from './pantalla-de-reportes'

const GW = 'http://gw/api/v1'
const CATALOGO = `${GW}/reportes`
const HISTORIAL = `${GW}/reportes/ejecuciones`
// Sintéticos con la forma de auditoria.yaml; ningún reporte ni ejecución real.
const DEFINICION = { definicionId: 'd-1', tipo: 'EJEMPLO', nombre: 'Reporte de ejemplo', descripcion: 'Sintético', permisoRequerido: 'AUDITORIA_LEER', contieneDatosSensibles: true }
const EJECUCION = { ejecucionId: 'x-1', definicionId: 'd-1', solicitadoPor: 'op-oculto', estado: 'COMPLETADA', filasGeneradas: 0, duracionMs: 15, iniciadaEn: '2026-10-16T12:00:00Z' }
const HUERFANA = { ...EJECUCION, ejecucionId: 'x-2', definicionId: 'd-fuera-de-pagina', estado: 'FALLIDA' }

async function montar() {
  TestBed.configureTestingModule({
    providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting(), provideRouter([]), { provide: GATEWAY, useValue: GW }],
  })
  const fixture = TestBed.createComponent(PantallaDeReportes)
  fixture.detectChanges()
  await fixture.whenStable()
  return { fixture, http: TestBed.inject(HttpTestingController), raiz: fixture.nativeElement as HTMLElement }
}

async function pintar(fixture: { whenStable: () => Promise<unknown>; detectChanges: () => void }) {
  await new Promise<void>((r) => queueMicrotask(r))
  await Promise.resolve()
  await fixture.whenStable()
  fixture.detectChanges()
}

describe('PantallaDeReportes · catálogo e historial de solo lectura', () => {
  it('correcto: catálogo con sensibilidad; ejecución con el nombre del catálogo, cero filas real y sin quién la pidió', async () => {
    const { fixture, http, raiz } = await montar()
    http.expectOne((r) => r.url === CATALOGO).flush({ items: [DEFINICION] })
    http.expectOne((r) => r.url === HISTORIAL).flush({ items: [EJECUCION, HUERFANA] })
    await pintar(fixture)
    const historial = raiz.querySelectorAll('ap-tabla-de-datos-virtualizada')[1]!.textContent ?? ''
    expect(raiz.textContent).toContain('Datos sensibles: pide justificación')
    expect(historial).toContain('Reporte de ejemplo')
    expect(historial).toContain('d-fuera-de-pagina') // identificador, nunca un nombre supuesto
    expect(historial).toContain('Completada')
    expect(historial).toContain('no disponible') // finalizadaEn y hashResultado ausentes
    expect(historial).not.toContain('op-oculto')
    expect(raiz.textContent).toContain('El contrato no publica los parámetros ni el corte')
  })

  it('límite: catálogo e historial vacíos lo dicen cada uno', async () => {
    const { fixture, http, raiz } = await montar()
    http.expectOne((r) => r.url === CATALOGO).flush({ items: [] })
    http.expectOne((r) => r.url === HISTORIAL).flush({ items: [] })
    await pintar(fixture)
    expect(raiz.textContent).toContain('No hay reportes activos en el catálogo.')
    expect(raiz.textContent).toContain('Todavía nadie ejecutó un reporte.')
  })

  it('inválido: 403 en el catálogo no tapa el historial; 200 fuera de contrato en el historial es error', async () => {
    const { fixture, http, raiz } = await montar()
    http.expectOne((r) => r.url === CATALOGO).flush({ codigo: 'AP-SEG-01' }, { status: 403, statusText: 'Forbidden' })
    http.expectOne((r) => r.url === HISTORIAL).flush({ ejecuciones: [] })
    await pintar(fixture)
    const alertas = Array.from(raiz.querySelectorAll('[role="alert"]')).map((a) => a.textContent ?? '').join(' ')
    expect(alertas).toContain('No tenés acceso a esto.')
    expect(alertas).toContain('El servicio respondió algo que no esperábamos')
    http.verify()
  })
})
