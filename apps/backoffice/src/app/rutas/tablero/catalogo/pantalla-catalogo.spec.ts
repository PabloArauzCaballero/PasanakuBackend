import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { ApplicationRef, provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { RouterTestingHarness } from '@angular/router/testing'
import { describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { catalogo } from './catalogo-sintetico'
import { PantallaCatalogo } from './pantalla-catalogo'

const URL = 'http://gw/api/v1/indicadores/catalogo-de-datos'

/** Sistemas → Catálogo de datos: los cuatro estados, y la búsqueda y la página en la URL. */
describe('PantallaCatalogo · cargando, con datos, vacío de búsqueda y error', () => {
  async function abrir(url: string) {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: '**', component: PantallaCatalogo }]),
        { provide: GATEWAY, useValue: 'http://gw/api/v1' },
      ],
    })
    const arnes = await RouterTestingHarness.create(url)
    TestBed.inject(ApplicationRef).tick()
    return { arnes, http: TestBed.inject(HttpTestingController) }
  }

  async function texto(arnes: RouterTestingHarness) {
    await arnes.fixture.whenStable()
    arnes.fixture.detectChanges()
    return arnes.routeNativeElement?.textContent ?? ''
  }

  it('cargando: una sola petición, y mientras tanto dice qué está consultando', async () => {
    const { arnes, http } = await abrir('/')
    const req = http.expectOne(URL)
    expect(req.request.method).toBe('GET')
    expect(arnes.routeNativeElement?.querySelector('[role=status][aria-label="Consultando el catálogo"]')).not.toBeNull()
    req.flush(catalogo())
  })

  it('correcto: lista las tablas con módulo, estado y revisión (activa y detectada sin ficha) y un enlace a la ficha', async () => {
    const { arnes, http } = await abrir('/')
    http.expectOne(URL).flush(catalogo())
    const t = await texto(arnes)
    expect(t).toContain('3 tablas')
    expect(t).toContain('03 · Aportes')
    expect(t).toContain('Activa')
    expect(t).toContain('Detectada')
    expect(t).toContain('Sin asignar')
    const enlace = arnes.routeNativeElement?.querySelector<HTMLAnchorElement>('a[href="/tablero/catalogo-de-datos/obligacion_aporte"]')
    expect(enlace?.textContent?.trim()).toBe('obligacion_aporte')
  })

  it('límite: la búsqueda de la URL filtra sin acentos; la página 2 trae las que sobran de veinte', async () => {
    const { arnes, http } = await abrir('/?q=Per%C3%ADodo')
    http.expectOne(URL).flush(catalogo())
    expect(await texto(arnes)).toContain('1 de 3 tablas')

    TestBed.resetTestingModule()
    const otra = await abrir('/?filtro=erp&pagina=2')
    otra.http.expectOne(URL).flush(catalogo(25))
    const t = await texto(otra.arnes)
    expect(t).toContain('25 de 28 tablas')
    expect(t).toContain('tabla_24')
    expect(t).not.toContain('tabla_00')
    expect(t).toContain('Página 2 de 2')
  })

  it('vacío: una búsqueda sin coincidencias lo dice y orienta, no deja la tabla muda', async () => {
    const { arnes, http } = await abrir('/?q=zzz-inexistente')
    http.expectOne(URL).flush(catalogo())
    expect(await texto(arnes)).toContain('Ninguna tabla coincide con la búsqueda')
  })

  it('error: un 403 no muestra el error crudo y ofrece volver a intentar', async () => {
    const { arnes, http } = await abrir('/')
    http.expectOne(URL).flush({ codigo: 'AP-SEG-403', detalle: 'stack interno' }, { status: 403, statusText: 'Forbidden' })
    const t = await texto(arnes)
    expect(t).not.toContain('stack interno')
    expect(t).toContain('Volver a intentar')
  })
})
