import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { ejemploDe } from '@aportaya/simulado'
import { erroresInterceptor } from '../src/app/nucleo/errores.interceptor'
import { GATEWAY } from '../src/app/nucleo/gateway'
import { idempotenciaInterceptor } from '../src/app/nucleo/idempotencia.interceptor'
import { Sesion } from '../src/app/nucleo/sesion'
import type { DecisionDeIngreso } from '../src/app/rutas/admisiones/dominio/cu68-admision'
import { PantallaDeAdmision } from '../src/app/rutas/admisiones/pantalla-de-admision'

/**
 * Andamiaje compartido de `pantalla-de-admision.spec.ts` y `pantalla-de-admision.decision-directa.spec.ts` (el barrido
 * de tamaño pone el tope en 200 líneas por archivo). Vive en `pruebas/` y no en `src/` porque importa `@aportaya/simulado`
 * (que lee del disco): `tsconfig.app.json` compila todo `src/` sin los tipos de Node y no puede verlo. **Prueba de UI contra HTTP simulado** (HttpTestingController):
 * ejercita la pantalla y su cliente, NO prueba integración con `grupos`. Los cuerpos de éxito salen de
 * `packages/simulado/ejemplos/grupos/*.json` (los mismos que sirve Prism).
 */
export const SOLICITUD = '9f1c2b7e-3a54-4d0e-8a61-5b7d2c9e4f10'
export const GW = 'http://gw/api/v1'
export const URL_HISTORIAL = `${GW}/grupos/solicitudes/${SOLICITUD}/decisiones`
export const URL_RESOLVER = `${GW}/grupos/solicitudes/${SOLICITUD}/resoluciones`
export const URL_DIRECTA = `${GW}/grupos/solicitudes/${SOLICITUD}/decision`
export const URL_BANDEJA = `${GW}/grupos/solicitudes`

export const ejemplo = (escenario: 'ok' | 'vacio') => ejemploDe('grupos', 'historialAdmision', escenario).cuerpo as DecisionDeIngreso[]
export const PROPUESTA = ejemplo('ok')[0] as DecisionDeIngreso
export const RESOLUCION = ejemplo('ok')[1] as DecisionDeIngreso

export const texto = (f: ComponentFixture<unknown>) => (f.nativeElement as HTMLElement).textContent ?? ''
export const boton = (f: ComponentFixture<unknown>, nombre: string) =>
  Array.from((f.nativeElement as HTMLElement).querySelectorAll('button')).find((b) => (b.textContent ?? '').includes(nombre))

/** Se llama en el `beforeEach` de cada spec: configura el módulo de prueba y devuelve lo que las pruebas usan. */
export function armarBanco() {
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideRouter([]),
      provideHttpClient(withInterceptors([idempotenciaInterceptor, erroresInterceptor])),
      provideHttpClientTesting(),
      { provide: GATEWAY, useValue: GW },
    ],
  })
  const http = TestBed.inject(HttpTestingController)
  const sesion = TestBed.inject(Sesion)

  function montar(permisos: string[] = ['ADMIN_PLATAFORMA'], sujeto = 'bo-1'): ComponentFixture<PantallaDeAdmision> {
    sesion.abrir('t', permisos, 'oficial', sujeto)
    const f = TestBed.createComponent(PantallaDeAdmision)
    f.componentRef.setInput('solicitudId', SOLICITUD)
    f.detectChanges()
    return f
  }
  /** Responde el historial y la bandeja (de donde sale el estado de la solicitud); sin fila, el estado es desconocido. */
  async function cargar(f: ComponentFixture<unknown>, cuerpo: unknown = [PROPUESTA], bandeja: object[] = []) {
    http.expectOne(URL_HISTORIAL).flush(cuerpo as object)
    http.match(URL_BANDEJA).forEach((r) => r.flush(bandeja))
    await f.whenStable()
    f.detectChanges()
  }
  /** `reload()` de un httpResource pide en el siguiente ciclo, no en el mismo tick. */
  async function recargar(f: ComponentFixture<unknown>, cuerpo: unknown) {
    f.detectChanges()
    TestBed.tick() // corre el efecto que dispara el pedido; whenStable() esperaría para siempre un pedido sin responder
    http.expectOne(URL_HISTORIAL).flush(cuerpo as object)
    await f.whenStable()
    f.detectChanges()
  }
  return { http, montar, cargar, recargar }
}
