import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { ApplicationRef, provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { catalogo } from './catalogo-sintetico'
import { FichaDeTabla } from './ficha-de-tabla'

const URL = 'http://gw/api/v1/indicadores/catalogo-de-datos'

/** La ficha de una tabla: negocio, sistema, columnas con su sensibilidad en palabras y relaciones navegables. */
describe('FichaDeTabla', () => {
  let http: HttpTestingController

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: GATEWAY, useValue: 'http://gw/api/v1' },
      ],
    })
    http = TestBed.inject(HttpTestingController)
  })

  async function abrir(tabla: string, respuesta: (r: ReturnType<HttpTestingController['expectOne']>) => void) {
    const fixture = TestBed.createComponent(FichaDeTabla)
    fixture.componentRef.setInput('tabla', tabla)
    fixture.detectChanges()
    TestBed.inject(ApplicationRef).tick()
    respuesta(http.expectOne(URL))
    await fixture.whenStable()
    fixture.detectChanges()
    return fixture.nativeElement as HTMLElement
  }

  it('correcto: las tres preguntas de negocio, la regla nombrada y la sensibilidad dicha en palabras', async () => {
    const el = await abrir('obligacion_aporte', (r) => r.flush(catalogo()))
    const t = el.textContent ?? ''
    expect(t).toContain('Es el eje del sistema financiero')
    expect(t).toContain('Sin ella no hay deuda formal')
    expect(t).toContain('ck_obligacion_saldo')
    expect(t).toContain('Dato financiero')
  })

  it('correcto: cada relación lleva a la ficha de la otra tabla, con su esquema', async () => {
    const el = await abrir('obligacion_aporte', (r) => r.flush(catalogo()))
    expect(el.querySelector('a[href="/tablero/catalogo-de-datos/grupo"]')?.textContent).toContain('grupos.grupo')
    expect(el.querySelector('a[href="/tablero/catalogo-de-datos/pago"]')?.textContent).toContain('aportes.pago')
  })

  it('límite: una tabla sin relaciones lo dice, en vez de dejar la lista vacía', async () => {
    const el = await abrir('credencial_acceso', (r) => r.flush(catalogo()))
    expect(el.querySelectorAll('.relaciones li')).toHaveLength(2)
    expect(el.textContent).toContain('Ninguna')
  })

  it('inválido: una tabla que no está en el catálogo lo dice y deja volver a la lista', async () => {
    const el = await abrir('tabla_que_no_existe', (r) => r.flush(catalogo()))
    expect(el.textContent).toContain('La tabla «tabla_que_no_existe» no está en el catálogo')
    expect(el.querySelector('a[href="/tablero/catalogo-de-datos"]')).not.toBeNull()
  })

  it('error: un 500 no muestra el error crudo y ofrece volver a intentar', async () => {
    const el = await abrir('obligacion_aporte', (r) => r.flush({ detalle: 'NullPointer en X' }, { status: 500, statusText: 'Error' }))
    expect(el.textContent).not.toContain('NullPointer')
    expect(el.textContent).toContain('Volver a intentar')
  })
})
