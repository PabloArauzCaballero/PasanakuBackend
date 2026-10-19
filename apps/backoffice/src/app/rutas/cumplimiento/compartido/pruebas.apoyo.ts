import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting, type TestRequest } from '@angular/common/http/testing'
import { provideZonelessChangeDetection, type Type } from '@angular/core'
import { TestBed, type ComponentFixture } from '@angular/core/testing'
import { By } from '@angular/platform-browser'
import { provideRouter } from '@angular/router'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import { GATEWAY } from '../../../nucleo/gateway'
import { idempotenciaInterceptor, olvidarIntentos } from '../../../nucleo/idempotencia.interceptor'
import { Sesion } from '../../../nucleo/sesion'
import { TablaDeDatosVirtualizada } from '../../../nucleo/tabla/tabla-de-datos-virtualizada'
import { trazaInterceptor } from '../../../nucleo/traza.interceptor'

/**
 * Montaje compartido de las pruebas del lote L2: la cadena de red real de errores, traza e
 * idempotencia contra `HttpTestingController` (doble del servidor, declarado como tal: estas
 * pruebas verifican la interfaz contra el CONTRATO, no contra `cumplimiento` corriendo).
 */
export const GW = 'http://gw/api/v1'

export function montar<T>(componente: Type<T>, permisos: string[] = [], rol = 'OPERADOR', entradas: Record<string, unknown> = {}) {
  if (!Element.prototype.scrollTo) Element.prototype.scrollTo = () => {}
  olvidarIntentos()
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideRouter([]),
      provideHttpClient(withInterceptors([trazaInterceptor, erroresInterceptor, idempotenciaInterceptor])),
      provideHttpClientTesting(),
      { provide: GATEWAY, useValue: GW },
    ],
  })
  TestBed.inject(Sesion).abrir('token', permisos, rol)
  const fixture = TestBed.createComponent(componente)
  for (const [nombre, valor] of Object.entries(entradas)) fixture.componentRef.setInput(nombre, valor)
  fixture.detectChanges()
  return { fixture, http: TestBed.inject(HttpTestingController) }
}

/** Deja correr la cadena de `await` (cargador → cursor → HTTP) y vuelve a dibujar. */
export async function asentar(fixture: ComponentFixture<unknown>): Promise<void> {
  for (let i = 0; i < 3; i++) {
    await new Promise((r) => setTimeout(r, 0))
    fixture.detectChanges()
  }
}

export const textoDe = (fixture: ComponentFixture<unknown>): string => (fixture.nativeElement as HTMLElement).textContent ?? ''

/** Las filas que recibió la tabla (el viewport virtual de jsdom no tiene alto, así que no se miran en el DOM). */
export function filasDe<F>(fixture: ComponentFixture<unknown>): F[] {
  return (fixture.debugElement.query(By.directive(TablaDeDatosVirtualizada)).componentInstance as TablaDeDatosVirtualizada<F & object>).filas() as F[]
}

/** Responde un rechazo con el cuerpo de error del contrato (`Error`: codigo, mensaje, trazaId). */
export function rechazar(pedido: TestRequest, estado: number, codigo?: string): void {
  pedido.flush(codigo ? { codigo, mensaje: 'texto del servidor que no se muestra', trazaId: 'traza-123' } : {}, { status: estado, statusText: 'x' })
}

/** El botón de confirmar del `ap-dialogo` montado en el fixture. */
export const botonConfirmar = (fixture: ComponentFixture<unknown>): HTMLButtonElement =>
  [...(fixture.nativeElement as HTMLElement).querySelectorAll('dialog .acciones button')].at(-1) as HTMLButtonElement
