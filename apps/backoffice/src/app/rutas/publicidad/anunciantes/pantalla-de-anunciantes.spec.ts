import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed, type ComponentFixture } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import { trazaInterceptor } from '../../../nucleo/traza.interceptor'
import { Sesion } from '../../../nucleo/sesion'
import { PantallaDeAnunciantes } from './pantalla-de-anunciantes'
import type { Anunciante } from '../dominio/cu110-anunciantes'

const URL = 'http://gw/api/v1/publicidad/anunciantes'

/**
 * Deja correr la cola de microtareas (el `await` encadenado de `cargar`, HTTP incluido) sin
 * usar `whenStable()`: con una petición deliberadamente sin resolver — como en la prueba de
 * la #H2.S2.M3 — `whenStable()` no resuelve nunca, porque `HttpClient` la mantiene como tarea
 * pendiente para la app zoneless hasta que se resuelve. Un `setTimeout` de macrotarea garantiza
 * que toda la cola de microtareas ya corrió antes de seguir.
 */
async function esperarMicrotareas(fixture: ComponentFixture<unknown>): Promise<void> {
  await new Promise((resolve) => setTimeout(resolve, 0))
  fixture.detectChanges()
}

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

    // Petición #1 (pedido inicial): queda pendiente, todavía no se resuelve.
    const primera = http.expectOne(URL)

    // Cambia el orden antes de que la #1 responda: se emite la petición #2 con un pedido
    // distinto. Se usa `orden` y no `pagina` porque `cargarAnunciantes` trae la lista COMPLETA
    // en cada petición y pagina en memoria (JSDoc de la pantalla): mockear una "página 2" con
    // una lista de un solo elemento cortaría vacío por aritmética de `slice`, no por el defecto
    // que esta prueba busca cubrir. No se usa `whenStable()` acá: mientras la #1 sigue sin
    // resolver, HttpClient la mantiene como tarea pendiente (para que la app zoneless no se dé
    // por estable con una petición en vuelo) y `whenStable()` nunca resolvería.
    // `esperarMicrotareas` avanza la cola sin depender de esa estabilidad global.
    fixture.componentInstance['orden'].set({ clave: 'razonSocialFacturacion', sentido: 'asc' })
    await esperarMicrotareas(fixture)
    const segunda = http.expectOne(URL)

    // La #2 (más nueva) responde primero.
    segunda.flush([anunciante({ anuncianteId: 'b-1', razonSocialFacturacion: 'Segundo SA' })])
    await esperarMicrotareas(fixture)
    expect(fixture.nativeElement.textContent).toContain('Segundo SA')

    // La #1 (más vieja) responde tarde: no debe pisar el resultado de la #2 ya aplicado.
    primera.flush([anunciante({ anuncianteId: 'a-1', razonSocialFacturacion: 'Primero SA' })])
    await esperarMicrotareas(fixture)
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
