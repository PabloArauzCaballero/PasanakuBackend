import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { Router, provideRouter } from '@angular/router'
import { RouterTestingHarness } from '@angular/router/testing'
import { Component } from '@angular/core'
import { describe, expect, it } from 'vitest'
import { BarraDeFiltros } from './barra-de-filtros'

@Component({
  selector: 'ap-anfitrion',
  imports: [BarraDeFiltros],
  template: `<ap-barra-de-filtros [definiciones]="[{ valor: 'a', texto: 'A' }, { valor: 'b', texto: 'B' }]" />`,
})
class Anfitrion {}

/**
 * El estado de los filtros vive en la `querystring`: elegir un chip navega con
 * `queryParamsHandling: 'merge'`, y leer la barra de vuelta lee esa misma URL — así
 * sobrevive a recargar la página y a pegar el enlace en otra pestaña con sesión.
 */
describe('BarraDeFiltros · estado en la URL', () => {
  it('elegir un filtro escribe ?filtro=… y resetea la página', async () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideRouter([{ path: '**', component: Anfitrion }])] })
    const arnes = await RouterTestingHarness.create('/?pagina=4')
    const router = TestBed.inject(Router)
    const instancia = arnes.routeNativeElement?.querySelector('ap-barra-de-filtros') as unknown as { cambiarElegidos: (v: string[]) => void } | null
    const componente = arnes.fixture.debugElement.query((n) => n.componentInstance instanceof BarraDeFiltros)?.componentInstance as BarraDeFiltros
    componente.cambiarElegidos(['a'])
    await arnes.fixture.whenStable()
    expect(router.url).toContain('filtro=a')
    expect(router.url).not.toContain('pagina=4')
    void instancia
  })

  it('la búsqueda escribe ?q= sin tocar el filtro ya elegido', async () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideRouter([{ path: '**', component: Anfitrion }])] })
    const arnes = await RouterTestingHarness.create('/?filtro=b')
    const router = TestBed.inject(Router)
    const componente = arnes.fixture.debugElement.query((n) => n.componentInstance instanceof BarraDeFiltros)?.componentInstance as BarraDeFiltros
    componente.cambiarBusqueda('mora')
    await arnes.fixture.whenStable()
    expect(router.url).toContain('q=mora')
    expect(router.url).toContain('filtro=b')
  })
})
