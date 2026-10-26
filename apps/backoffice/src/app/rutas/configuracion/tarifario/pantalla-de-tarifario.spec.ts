import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { PantallaDeTarifario } from './pantalla-de-tarifario'

const GW = 'http://gw/api/v1'
const URL = `${GW}/tarifas/tarifarios`
// H5.S2.M1 · Sintético con la forma del contrato (doble de `HttpTestingController`, regla 65); sin vigenteHasta ni simulación a propósito.
const FILA = { tarifarioId: 't-1', codigo: 'TAR-EJEMPLO', version: 3, nombre: 'Ejemplo', estado: 'EN_PREAVISO', monedaBase: 'BOB', vigenteDesde: '2026-11-01T04:00:00Z', diasPreaviso: 30 }

async function montar() {
  TestBed.configureTestingModule({
    providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting(), provideRouter([]), { provide: GATEWAY, useValue: GW }],
  })
  const fixture = TestBed.createComponent(PantallaDeTarifario)
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

const botones = (raiz: HTMLElement) => Array.from(raiz.querySelectorAll('button')).map((b) => b.textContent?.trim() ?? '')

describe('PantallaDeTarifario · historial de solo lectura', () => {
  it('aceptado: pinta la versión con su estado, «no disponible» donde el contrato no trajo el campo, y pide 10 por página', async () => {
    const { fixture, http, raiz } = await montar()
    const req = http.expectOne((r) => r.url === URL)
    expect(req.request.params.get('limite')).toBe('10')
    req.flush({ items: [FILA] })
    await pintar(fixture)
    expect(raiz.textContent).toContain('TAR-EJEMPLO')
    expect(raiz.textContent).toContain('En preaviso')
    expect(raiz.textContent).toContain('no disponible')
    // Nada que publicar ni editar: `tarifas.yaml` no tiene editar ni borrar una versión.
    expect(raiz.querySelector('ap-acciones-de-fila')).toBeNull()
    expect(botones(raiz).some((b) => /Editar|Eliminar|Publicar/.test(b))).toBe(false)
    expect(botones(raiz)).toContain('Descargar CSV')
  })

  it('aceptado: el estado viaja como `estados`; la moneda y el buscador filtran la página cargada', async () => {
    const { fixture, http, raiz } = await montar()
    http.expectOne((r) => r.url === URL).flush({ items: [FILA, { ...FILA, tarifarioId: 't-2', codigo: 'TAR-USD', monedaBase: 'USD', estado: 'VIGENTE' }] })
    await pintar(fixture)
    const p = fixture.componentInstance as unknown as { listado: { filtrar(n: string, v: string): void; buscar(t: string): void; limpiar(): void } }
    p.listado.filtrar('moneda', 'USD')
    await pintar(fixture)
    expect(raiz.querySelectorAll('tbody tr')).toHaveLength(1)
    p.listado.limpiar()
    p.listado.buscar('tar-ejemplo')
    await pintar(fixture)
    expect(raiz.querySelectorAll('tbody tr')).toHaveLength(1)
    p.listado.filtrar('estado', 'VIGENTE')
    await pintar(fixture)
    const req = http.expectOne((r) => r.url === URL)
    expect(req.request.params.getAll('estados')).toEqual(['VIGENTE'])
    req.flush({ items: [] })
    http.verify()
  })

  it('límite: sin tarifarios, el vacío lo dice', async () => {
    const { fixture, http, raiz } = await montar()
    http.expectOne((r) => r.url === URL).flush({ items: [] })
    await pintar(fixture)
    expect(raiz.textContent).toContain('Todavía no hay tarifarios publicados.')
  })

  it('error: 403 → «No tenés acceso a esto.» con traza, sin tabla; reintentar vuelve a pedir', async () => {
    const { fixture, http, raiz } = await montar()
    http.expectOne((r) => r.url === URL).flush({ codigo: 'AP-SEG-01', trazaId: 'tr-7' }, { status: 403, statusText: 'Forbidden' })
    await pintar(fixture)
    expect(raiz.querySelector('[role="alert"]')?.textContent).toContain('No tenés acceso a esto.')
    expect(raiz.textContent).toContain('tr-7')
    expect(raiz.querySelector('tbody')).toBeNull()
    raiz.querySelector<HTMLButtonElement>('ap-estado-de-error button')!.click()
    await pintar(fixture)
    http.expectOne((r) => r.url === URL).flush({ items: [FILA] })
    await pintar(fixture)
    expect(raiz.textContent).toContain('TAR-EJEMPLO')
    http.verify()
  })
})
