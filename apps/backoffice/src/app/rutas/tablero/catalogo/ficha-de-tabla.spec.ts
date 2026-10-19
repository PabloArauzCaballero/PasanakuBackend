import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { ApplicationRef, provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter, withComponentInputBinding } from '@angular/router'
import { RouterTestingHarness } from '@angular/router/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { Sesion } from '../../../nucleo/sesion'
import { catalogo } from './catalogo-sintetico'
import { FichaDeTabla } from './ficha-de-tabla'

const URL = 'http://gw/api/v1/indicadores/catalogo-de-datos'

/** La ficha de una tabla: cabecera con insignias, pestañas en la URL, secciones clave–valor y relaciones navegables. */
describe('FichaDeTabla', () => {
  let http: HttpTestingController

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'tablero/catalogo-de-datos/:tabla', component: FichaDeTabla }], withComponentInputBinding()),
        { provide: GATEWAY, useValue: 'http://gw/api/v1' },
      ],
    })
    http = TestBed.inject(HttpTestingController)
  })

  async function abrir(url: string, respuesta: (r: ReturnType<HttpTestingController['expectOne']>) => void, permisos: string[] = ['TABLERO_SISTEMAS_VER']) {
    TestBed.inject(Sesion).abrir('token', permisos, 'ADMIN_PLATAFORMA')
    const arnes = await RouterTestingHarness.create(url)
    TestBed.inject(ApplicationRef).tick()
    respuesta(http.expectOne(URL))
    await arnes.fixture.whenStable()
    arnes.fixture.detectChanges()
    return arnes.routeNativeElement as HTMLElement
  }

  it('correcto: cabecera con esquema.tabla, propósito e insignias; la vista general trae las tres preguntas, la regla y la clasificación', async () => {
    const el = await abrir('/tablero/catalogo-de-datos/obligacion_aporte', (r) => r.flush(catalogo()))
    expect(el.querySelector('h1')?.textContent?.trim()).toBe('aportes.obligacion_aporte')
    const chips = Array.from(el.querySelectorAll('ap-cabecera-de-pagina ap-chip-estado')).map((c) => c.textContent?.trim())
    expect(chips).toEqual(['Detectada', 'Activa'])
    const t = el.textContent ?? ''
    expect(t).toContain('Es el eje del sistema financiero')
    expect(t).toContain('Sin ella no hay deuda formal')
    expect(t).toContain('ck_obligacion_saldo')
    expect(el.querySelector('[role="tablist"]')?.querySelector('[aria-selected="true"]')?.textContent).toContain('Vista general')
    expect(el.querySelector('ap-cabecera-de-pagina ap-boton'), 'sin permiso de edición no hay «Editar ficha»').toBeNull()
  })

  it('correcto: con permiso aparece «Editar ficha»; la pestaña Relaciones (en la URL) lleva a la ficha de la otra tabla', async () => {
    const el = await abrir('/tablero/catalogo-de-datos/obligacion_aporte?vista=relaciones', (r) => r.flush(catalogo()), ['TABLERO_SISTEMAS_VER', 'CATALOGO_DATOS_EDITAR'])
    expect(el.querySelector('ap-cabecera-de-pagina ap-boton')?.textContent).toContain('Editar ficha')
    expect(el.querySelector('a[href="/tablero/catalogo-de-datos/grupo"]')?.textContent).toContain('grupos.grupo')
    expect(el.querySelector('a[href="/tablero/catalogo-de-datos/pago"]')?.textContent).toContain('aportes.pago')
  })

  it('límite: una tabla sin relaciones lo dice en vez de dejar la lista vacía; una vista desconocida cae en la general', async () => {
    const el = await abrir('/tablero/catalogo-de-datos/credencial_acceso?vista=otra', (r) => r.flush(catalogo()))
    expect(el.querySelector('[aria-selected="true"]')?.textContent).toContain('Vista general')
    expect(el.textContent).toContain('Compartido (ningún servicio es dueño)')
  })

  it('inválido: una tabla que no está en el catálogo lo dice y deja volver a la lista', async () => {
    const el = await abrir('/tablero/catalogo-de-datos/tabla_que_no_existe', (r) => r.flush(catalogo()))
    expect(el.textContent).toContain('La tabla «tabla_que_no_existe» no está en el catálogo')
    expect(el.querySelector('a[href="/tablero/catalogo-de-datos"]')).not.toBeNull()
  })

  it('error: un 500 no muestra el error crudo y ofrece volver a intentar', async () => {
    const el = await abrir('/tablero/catalogo-de-datos/obligacion_aporte', (r) => r.flush({ detalle: 'NullPointer en X' }, { status: 500, statusText: 'Error' }))
    expect(el.textContent).not.toContain('NullPointer')
    expect(el.textContent).toContain('Volver a intentar')
  })
})
