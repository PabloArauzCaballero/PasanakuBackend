import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting, type TestRequest } from '@angular/common/http/testing'
import { provideZonelessChangeDetection, type Type } from '@angular/core'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import { GATEWAY } from '../../../nucleo/gateway'
import { idempotenciaInterceptor, olvidarIntentos } from '../../../nucleo/idempotencia.interceptor'
import { Sesion } from '../../../nucleo/sesion'
import { TablaDeDatosVirtualizada } from '../../../nucleo/tabla/tabla-de-datos-virtualizada'

/** Montaje compartido de las pruebas de las bandejas de operación (UI contra HTTP simulado). Solo lo importan specs. */
export const GW = 'http://gw/api/v1'

export function configurar(): HttpTestingController {
  olvidarIntentos()
  if (!Element.prototype.scrollTo) Element.prototype.scrollTo = () => {}
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideRouter([]),
      provideHttpClient(withInterceptors([idempotenciaInterceptor, erroresInterceptor])),
      provideHttpClientTesting(),
      { provide: GATEWAY, useValue: GW },
    ],
  })
  return TestBed.inject(HttpTestingController)
}

export async function montar<C>(componente: Type<C>, permisos: string[]): Promise<ComponentFixture<C>> {
  TestBed.inject(Sesion).abrir('t', permisos, 'r')
  const f = TestBed.createComponent(componente)
  f.detectChanges()
  await f.whenStable()
  return f
}

/** Responde y deja que la promesa del cargador (cursor → tabla → estado) termine antes de mirar la pantalla. */
export async function responder(f: ComponentFixture<unknown>, p: TestRequest, cuerpo: object, estado = 200): Promise<void> {
  p.flush(cuerpo, { status: estado, statusText: 'x' })
  await new Promise((r) => setTimeout(r, 0))
  await f.whenStable()
  f.detectChanges()
}

export const texto = (f: ComponentFixture<unknown>): string => (f.nativeElement as HTMLElement).textContent ?? ''

export function tablaDe<T extends object>(f: ComponentFixture<unknown>): TablaDeDatosVirtualizada<T> | null {
  return (f.debugElement.query((d) => d.componentInstance instanceof TablaDeDatosVirtualizada)?.componentInstance as TablaDeDatosVirtualizada<T>) ?? null
}
