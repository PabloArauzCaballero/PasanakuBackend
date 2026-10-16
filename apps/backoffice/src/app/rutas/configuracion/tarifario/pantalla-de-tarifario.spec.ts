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
// Sintético con la forma del contrato; sin vigenteHasta ni simulación a propósito.
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

describe('PantallaDeTarifario · historial de solo lectura', () => {
  it('correcto: pinta la versión con su estado y dice «no disponible» donde el contrato no trajo el campo', async () => {
    const { fixture, http, raiz } = await montar()
    http.expectOne((r) => r.url === URL).flush({ items: [FILA] })
    await pintar(fixture)
    expect(raiz.textContent).toContain('TAR-EJEMPLO')
    expect(raiz.textContent).toContain('En preaviso')
    expect(raiz.textContent).toContain('no disponible')
    expect(raiz.querySelector('ap-boton')).toBeNull() // nada que publicar ni editar
  })

  it('límite: sin tarifarios, el vacío lo dice', async () => {
    const { fixture, http, raiz } = await montar()
    http.expectOne((r) => r.url === URL).flush({ items: [] })
    await pintar(fixture)
    expect(raiz.textContent).toContain('Todavía no hay tarifarios publicados.')
  })

  it('inválido: 403 → «No tenés acceso a esto.» con traza, sin tabla; reintentar vuelve a pedir', async () => {
    const { fixture, http, raiz } = await montar()
    http.expectOne((r) => r.url === URL).flush({ codigo: 'AP-SEG-01', trazaId: 'tr-7' }, { status: 403, statusText: 'Forbidden' })
    await pintar(fixture)
    expect(raiz.querySelector('[role="alert"]')?.textContent).toContain('No tenés acceso a esto.')
    expect(raiz.textContent).toContain('tr-7')
    expect(raiz.querySelector('ap-tabla-de-datos-virtualizada')).toBeNull()
    raiz.querySelector<HTMLButtonElement>('ap-aviso-de-listado ap-boton button')!.click()
    await pintar(fixture)
    http.expectOne((r) => r.url === URL).flush({ items: [FILA] })
    await pintar(fixture)
    expect(raiz.textContent).toContain('TAR-EJEMPLO')
    http.verify()
  })
})
