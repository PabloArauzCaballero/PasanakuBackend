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
// H5.S2.M3 · Doble de `HttpTestingController` (regla 65). Sintéticos con la forma de auditoria.yaml; ningún reporte ni ejecución real.
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

async function irA(fixture: Parameters<typeof pintar>[0], raiz: HTMLElement, pestana: string) {
  ;(Array.from(raiz.querySelectorAll('[role="tab"]')).find((b) => b.textContent?.includes(pestana)) as HTMLElement).click()
  await pintar(fixture)
}

describe('PantallaDeReportes · catálogo e historial de solo lectura, en pestañas', () => {
  it('aceptado: catálogo con sensibilidad; ejecución con el nombre del catálogo, cero filas real y sin quién la pidió', async () => {
    const { fixture, http, raiz } = await montar()
    http.expectOne((r) => r.url === CATALOGO).flush({ items: [DEFINICION] })
    http.expectOne((r) => r.url === HISTORIAL).flush({ items: [EJECUCION, HUERFANA] })
    await pintar(fixture)
    expect(raiz.textContent).toContain('Datos sensibles: pide justificación')
    await irA(fixture, raiz, 'Historial')
    const historial = raiz.querySelector('table')!.textContent ?? ''
    expect(historial).toContain('Reporte de ejemplo')
    expect(historial).toContain('d-fuera-de-pagina') // identificador, nunca un nombre supuesto
    expect(historial).toContain('Completada')
    expect(historial).toContain('no disponible') // finalizadaEn y hashResultado ausentes
    expect(historial).not.toContain('op-oculto')
    expect(raiz.textContent).toContain('El contrato no publica los parámetros ni el corte')
    expect(raiz.querySelector('ap-acciones-de-fila')).toBeNull()
  })

  it('aceptado: el buscador del historial encuentra por el nombre del reporte; sensibilidad filtra el catálogo', async () => {
    const { fixture, http, raiz } = await montar()
    http.expectOne((r) => r.url === CATALOGO).flush({ items: [DEFINICION, { ...DEFINICION, definicionId: 'd-2', nombre: 'Otro', contieneDatosSensibles: false }] })
    http.expectOne((r) => r.url === HISTORIAL).flush({ items: [EJECUCION, HUERFANA] })
    await pintar(fixture)
    const p = fixture.componentInstance as unknown as { catalogo: { filtrar(n: string, v: string): void }; historial: { buscar(t: string): void } }
    p.catalogo.filtrar('sensibilidad', 'NO')
    await pintar(fixture)
    expect(raiz.querySelectorAll('tbody tr')).toHaveLength(1)
    p.historial.buscar('reporte de ejemplo')
    await irA(fixture, raiz, 'Historial')
    expect(raiz.querySelectorAll('tbody tr')).toHaveLength(1)
  })

  it('límite: catálogo e historial vacíos lo dicen cada uno', async () => {
    const { fixture, http, raiz } = await montar()
    http.expectOne((r) => r.url === CATALOGO).flush({ items: [] })
    http.expectOne((r) => r.url === HISTORIAL).flush({ items: [] })
    await pintar(fixture)
    expect(raiz.textContent).toContain('No hay reportes activos en el catálogo.')
    await irA(fixture, raiz, 'Historial')
    expect(raiz.textContent).toContain('Todavía nadie ejecutó un reporte.')
  })

  it('error: 403 en el catálogo no tapa el historial; 200 fuera de contrato en el historial es error', async () => {
    const { fixture, http, raiz } = await montar()
    http.expectOne((r) => r.url === CATALOGO).flush({ codigo: 'AP-SEG-01' }, { status: 403, statusText: 'Forbidden' })
    http.expectOne((r) => r.url === HISTORIAL).flush({ ejecuciones: [] })
    await pintar(fixture)
    expect(raiz.querySelector('[role="alert"]')?.textContent).toContain('No tenés acceso a esto.')
    await irA(fixture, raiz, 'Historial')
    expect(raiz.querySelector('[role="alert"]')?.textContent).toContain('El servicio respondió algo que no esperábamos')
    http.verify()
  })
})
