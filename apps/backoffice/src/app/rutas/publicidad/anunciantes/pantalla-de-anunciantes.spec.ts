import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import { trazaInterceptor } from '../../../nucleo/traza.interceptor'
import { Sesion } from '../../../nucleo/sesion'
import { PantallaDeAnunciantes } from './pantalla-de-anunciantes'
import type { Anunciante } from '../dominio/cu110-anunciantes'

const URL = 'http://gw/api/v1/publicidad/anunciantes'

function anunciante(parcial: Partial<Anunciante>): Anunciante {
  return {
    anuncianteId: 'a-1',
    tipo: 'SOCIO_COMERCIAL',
    razonSocialFacturacion: 'Anunciante SA',
    moneda: 'BOB',
    limiteGastoMensual: null,
    estado: 'ACTIVA',
    ...parcial,
  } as Anunciante
}

/**
 * Migrada de `TablaDeDatosVirtualizada` al organismo canónico (PR7 §H4.S1.M2). Estas
 * pruebas cubren lo propio de esta pantalla: la guarda de permiso del botón de alta y,
 * sobre todo, que una respuesta HTTP atrasada nunca pisa una más nueva (PR7 §H2.S2.M3) —
 * la pieza del contrato de selección/estado más fácil de romper en un refactor así.
 */
describe('PantallaDeAnunciantes', () => {
  let http: HttpTestingController

  function montarProviders() {
    return [
      provideZonelessChangeDetection(),
      provideRouter([]),
      provideHttpClient(withInterceptors([trazaInterceptor, erroresInterceptor])),
      provideHttpClientTesting(),
      { provide: GATEWAY, useValue: 'http://gw/api/v1' },
    ]
  }

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: montarProviders() })
    http = TestBed.inject(HttpTestingController)
  })

  it('sin el permiso PUBLICIDAD_ANUNCIANTES, el botón de alta no aparece', async () => {
    TestBed.inject(Sesion).abrir('t', [], 'lector')
    const fixture = TestBed.createComponent(PantallaDeAnunciantes)
    fixture.detectChanges()
    http.expectOne(URL).flush([anunciante({})])
    await fixture.whenStable()
    expect(fixture.nativeElement.querySelector('header ap-boton')).toBeNull()
  })

  it('con el permiso, la lista cargada se muestra en la tabla canónica', async () => {
    TestBed.inject(Sesion).abrir('t', ['PUBLICIDAD_ANUNCIANTES'], 'oficial')
    const fixture = TestBed.createComponent(PantallaDeAnunciantes)
    fixture.detectChanges()
    http.expectOne(URL).flush([anunciante({ anuncianteId: 'a-1', razonSocialFacturacion: 'Primero SA' })])
    await fixture.whenStable()
    expect(fixture.nativeElement.textContent).toContain('Primero SA')
  })

  it('una respuesta atrasada de una página vieja no reemplaza a la vigente de una página más nueva (PR7 §H2.S2.M3)', async () => {
    TestBed.inject(Sesion).abrir('t', [], 'lector')
    const fixture = TestBed.createComponent(PantallaDeAnunciantes)
    fixture.detectChanges()

    // Petición #1 (página 1): queda pendiente, todavía no se resuelve.
    const primera = http.expectOne(URL)

    // Cambia de página antes de que la #1 responda: se emite la petición #2 (página 2).
    fixture.componentInstance['pagina'].set(2)
    await fixture.whenStable()
    const segunda = http.expectOne(URL)

    // La #2 (más nueva) responde primero.
    segunda.flush([anunciante({ anuncianteId: 'b-1', razonSocialFacturacion: 'Segundo SA' })])
    await fixture.whenStable()
    expect(fixture.nativeElement.textContent).toContain('Segundo SA')

    // La #1 (más vieja) responde tarde: no debe pisar el resultado de la #2 ya aplicado.
    primera.flush([anunciante({ anuncianteId: 'a-1', razonSocialFacturacion: 'Primero SA' })])
    await fixture.whenStable()
    expect(fixture.nativeElement.textContent).toContain('Segundo SA')
    expect(fixture.nativeElement.textContent).not.toContain('Primero SA')
  })

  it('un error 403 se traduce a "sin-permiso" y no deja la tabla en un estado mudo', async () => {
    TestBed.inject(Sesion).abrir('t', [], 'lector')
    const fixture = TestBed.createComponent(PantallaDeAnunciantes)
    fixture.detectChanges()
    http.expectOne(URL).flush({ codigo: undefined }, { status: 403, statusText: 'Forbidden' })
    await fixture.whenStable()
    expect(fixture.nativeElement.querySelector('[role="alert"]')?.textContent).toContain('No tenés acceso')
  })
})
