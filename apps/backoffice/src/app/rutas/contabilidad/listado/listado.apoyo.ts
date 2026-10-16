import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection, type Type } from '@angular/core'
import { TestBed, type ComponentFixture } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { GATEWAY } from '../../../nucleo/gateway'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import { trazaInterceptor } from '../../../nucleo/traza.interceptor'
import { Sesion } from '../../../nucleo/sesion'

/**
 * Montaje compartido de las pruebas de bandejas por cursor (ERP y publicidad) contra `HttpTestingController`: doble
 * declarado del gateway (regla 65), con la forma de página del contrato. Los datos los pone cada prueba y son sintéticos.
 */
export const GW = 'http://gw/api/v1'

export type Respuesta = { cuerpo: unknown; status?: number }

export function configurarListado(permisos: string[]): HttpTestingController {
  if (!Element.prototype.scrollTo) Element.prototype.scrollTo = () => {}
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideRouter([]),
      provideHttpClient(withInterceptors([trazaInterceptor, erroresInterceptor])),
      provideHttpClientTesting(),
      { provide: GATEWAY, useValue: GW },
    ],
  })
  TestBed.inject(Sesion).abrir('t', permisos, 'operador')
  return TestBed.inject(HttpTestingController)
}

export async function esperar(fixture: ComponentFixture<unknown>): Promise<void> {
  await new Promise((r) => setTimeout(r, 0))
  fixture.detectChanges()
}

/** Monta la pantalla, responde la primera página pedida a `url` y deja que la tabla la dibuje. */
export async function montarListado<T>(
  http: HttpTestingController,
  componente: Type<T>,
  url: string,
  respuesta: Respuesta,
  entradas: Record<string, unknown> = {},
): Promise<ComponentFixture<T>> {
  const fixture = TestBed.createComponent(componente)
  for (const [k, v] of Object.entries(entradas)) fixture.componentRef.setInput(k, v)
  fixture.detectChanges()
  await esperar(fixture)
  http.expectOne(url).flush(respuesta.cuerpo as object, respuesta.status ? { status: respuesta.status, statusText: 'x' } : {})
  await esperar(fixture)
  await esperar(fixture)
  return fixture
}

export function textoDe(fixture: ComponentFixture<unknown>): string {
  return (fixture.nativeElement as HTMLElement).textContent ?? ''
}
