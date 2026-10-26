import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { ApplicationRef, provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter, Router } from '@angular/router'
import { RouterTestingHarness } from '@angular/router/testing'
import { describe, expect, it, vi } from 'vitest'
import { Sesion } from '../../../nucleo/sesion'
import { GATEWAY } from '../../../nucleo/gateway'
import { CREDENCIAL, OBLIGACION, paginaDe, resumen, tabla } from './catalogo-sintetico'
import { PantallaCatalogo } from './pantalla-catalogo'

const BASE = 'http://gw/api/v1/indicadores/catalogo-de-datos'
const RESUMEN = `${BASE}/resumen`
const tablasCon = (consulta: string) => `${BASE}/tablas?${consulta}`

/** Datos → Catálogo de datos: los cuatro estados, la página pedida al servidor y el estado en la URL. */
describe('PantallaCatalogo · cargando, con datos, vacío de búsqueda y error', () => {
  async function abrir(url: string, permisos: string[] = []) {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: '**', component: PantallaCatalogo }]),
        { provide: GATEWAY, useValue: 'http://gw/api/v1' },
      ],
    })
    TestBed.inject(Sesion).abrir('t', permisos, 'operador')
    const arnes = await RouterTestingHarness.create(url)
    TestBed.inject(ApplicationRef).tick()
    return { arnes, http: TestBed.inject(HttpTestingController) }
  }

  async function texto(arnes: RouterTestingHarness) {
    await arnes.fixture.whenStable()
    arnes.fixture.detectChanges()
    return arnes.routeNativeElement?.textContent ?? ''
  }

  it('cargando: pide el resumen y UNA página (no el catálogo entero), y mientras tanto dice qué consulta', async () => {
    const { arnes, http } = await abrir('/')
    http.expectOne(RESUMEN).flush(resumen())
    const req = http.expectOne(tablasCon('pagina=1&limite=20'))
    expect(req.request.method).toBe('GET')
    expect(arnes.routeNativeElement?.querySelector('[role=status][aria-label="Consultando el catálogo"]')).not.toBeNull()
    http.expectNone(BASE)
    req.flush(paginaDe([OBLIGACION]))
  })

  it('correcto: cifras del resumen, cada fila con lo que es, su estado, revisión y sensibilidad, y el enlace por esquema y tabla', async () => {
    const { arnes, http } = await abrir('/')
    http.expectOne(RESUMEN).flush(resumen({ tablas: 391, tablasDeNegocio: 342, tablasDeInfraestructura: 49 }))
    http.expectOne(tablasCon('pagina=1&limite=20')).flush(paginaDe([OBLIGACION, CREDENCIAL], 1, 20, 391))
    const t = await texto(arnes)
    expect(t).toContain('342 de negocio · 49 de infraestructura')
    expect(t).toContain('Lo que un cupo debe en un período')
    expect(t).toContain('Activa')
    expect(t).toContain('Detectada')
    expect(t).toContain('Financiera')
    expect(t).toContain('Sin asignar')
    expect(arnes.routeNativeElement?.querySelector('.total')?.textContent?.trim()).toBe('391')
    const enlace = arnes.routeNativeElement?.querySelector<HTMLAnchorElement>('a[href="/tablero/catalogo-de-datos/aportes/obligacion_aporte"]')
    expect(enlace?.textContent?.trim()).toBe('obligacion_aporte')
  })

  it('límite: los filtros y la página de la URL viajan al servidor; la paginación dice dónde está', async () => {
    const { arnes, http } = await abrir('/?q=Per%C3%ADodo&esquema=erp&sensibilidad=PERSONAL&pagina=2&otro=x')
    http.expectOne(RESUMEN).flush(resumen())
    const extra = Array.from({ length: 5 }, (_, i) => tabla(`tabla_${20 + i}`, 'erp'))
    http.expectOne(tablasCon('q=Per%C3%ADodo&esquema=erp&sensibilidad=PERSONAL&pagina=2&limite=20')).flush(paginaDe(extra, 2, 20, 25))
    const t = await texto(arnes)
    expect(t).toContain('tabla_24')
    expect(t).toContain('Página 2 de 2')
  })

  it('vacío: una búsqueda sin coincidencias lo dice y orienta, no deja la tabla muda', async () => {
    const { arnes, http } = await abrir('/?q=zzz-inexistente')
    http.expectOne(RESUMEN).flush(resumen())
    http.expectOne(tablasCon('q=zzz-inexistente&pagina=1&limite=20')).flush(paginaDe([]))
    expect(await texto(arnes)).toContain('Ninguna tabla coincide con la búsqueda')
  })

  it('correcto: la pestaña Esquemas (en la URL) muestra cada esquema con su servicio y no pide tablas', async () => {
    const { arnes, http } = await abrir('/?vista=esquemas')
    http.expectOne(RESUMEN).flush(resumen())
    const t = await texto(arnes)
    http.expectNone((r) => r.url.startsWith(`${BASE}/tablas`))
    expect(t).toContain('Servicio aportes')
    expect(t).toContain('Compartido')
    expect(arnes.routeNativeElement?.querySelector('[aria-selected="true"]')?.textContent).toContain('Esquemas')
  })

  it('error: un 403 no muestra el error crudo y ofrece volver a intentar', async () => {
    const { arnes, http } = await abrir('/')
    http.expectOne(RESUMEN).flush({ codigo: 'AP-SEG-403', detalle: 'stack interno' }, { status: 403, statusText: 'Forbidden' })
    http.expectOne(tablasCon('pagina=1&limite=20')).flush({ codigo: 'AP-SEG-403', detalle: 'stack interno' }, { status: 403, statusText: 'Forbidden' })
    const t = await texto(arnes)
    expect(t).not.toContain('stack interno')
    expect(t).toContain('Volver a intentar')
  })

  it('correcto: la página filtrada se descarga (el botón dice cuántas filas) y cada fila ofrece «Ver ficha»; sin permiso no hay «Editar»', async () => {
    const { arnes, http } = await abrir('/tablero/catalogo-de-datos')
    http.expectOne(RESUMEN).flush(resumen())
    http.expectOne(tablasCon('pagina=1&limite=20')).flush(paginaDe([OBLIGACION, CREDENCIAL], 1, 20, 2))
    await texto(arnes)
    const raiz = arnes.routeNativeElement!
    expect(raiz.querySelector('ap-exportar-listado')?.textContent).toContain('2 filas')
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true)
    raiz.querySelector<HTMLButtonElement>('button[aria-label="Ver ficha aportes.obligacion_aporte"]')!.click()
    expect(navegar).toHaveBeenCalledWith(['/tablero/catalogo-de-datos', 'aportes', 'obligacion_aporte'])
    expect(raiz.querySelector('button[aria-label^="Editar"]')).toBeNull()
    expect(raiz.querySelector('button[aria-label^="Eliminar"]')).toBeNull()
  })

  it('correcto: con CATALOGO_DATOS_EDITAR, «Editar» lleva a la ficha de gobierno (CU-130)', async () => {
    const { arnes, http } = await abrir('/tablero/catalogo-de-datos', ['CATALOGO_DATOS_EDITAR'])
    http.expectOne(RESUMEN).flush(resumen())
    http.expectOne(tablasCon('pagina=1&limite=20')).flush(paginaDe([OBLIGACION], 1, 20, 1))
    await texto(arnes)
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true)
    arnes.routeNativeElement!.querySelector<HTMLButtonElement>('button[aria-label="Editar aportes.obligacion_aporte"]')!.click()
    expect(navegar).toHaveBeenCalledWith(['/tablero/catalogo-de-datos', 'aportes', 'obligacion_aporte', 'ficha'])
  })
})
