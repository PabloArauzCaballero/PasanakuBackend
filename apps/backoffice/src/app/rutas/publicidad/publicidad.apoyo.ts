import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection, type Type } from '@angular/core'
import { TestBed, type ComponentFixture } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { GATEWAY } from '../../nucleo/gateway'
import { erroresInterceptor } from '../../nucleo/errores.interceptor'
import { idempotenciaInterceptor } from '../../nucleo/idempotencia.interceptor'
import { trazaInterceptor } from '../../nucleo/traza.interceptor'
import { Sesion } from '../../nucleo/sesion'
import type { Anunciante } from './dominio/cu110-anunciantes'
import type { Campana } from './dominio/cu111-campanas'
import type { PiezaCreativa } from './dominio/cu112-moderacion'

/** Filas SINTÉTICAS con la forma exacta de `publicidad.yaml` (`AnuncianteEnLista`, `CampanaEnLista`, `PiezaEnLista`), para las pruebas. */
export const anunciante = (p: Partial<Anunciante> = {}): Anunciante => ({
  anuncianteId: 'a-1',
  tipo: 'SOCIO_COMERCIAL',
  razonSocialFacturacion: 'Anunciante SA',
  estado: 'ACTIVO',
  creadoEn: '2026-09-01T12:00:00Z',
  cuentaPublicitariaId: 'cp-1',
  estadoCuenta: 'ACTIVA',
  limiteGastoMensual: null,
  ...p,
})

export const campana = (p: Partial<Campana> = {}): Campana => ({
  campanaId: 'c-1',
  cuentaPublicitariaId: 'cp-1',
  anuncianteId: 'a-1',
  nombre: 'Lanzamiento',
  objetivo: 'VISIBILIDAD_MARCA',
  presupuestoTotal: { monto: '500.00', moneda: 'BOB' },
  presupuestoConsumido: { monto: '120.50', moneda: 'BOB' },
  fechaInicio: '2026-09-01T04:00:00Z',
  fechaFin: null,
  estado: 'EN_REVISION',
  aprobadaPor: null,
  ...p,
})

export const pieza = (p: Partial<PiezaCreativa> = {}): PiezaCreativa => ({
  piezaCreativaId: 'p-1',
  anuncianteId: 'a-1',
  titulo: 'Banner de aniversario',
  texto: null,
  urlRecurso: 'https://cdn.aportaya.test/p-1.png',
  tipoRecurso: 'IMAGEN',
  estadoModeracion: 'PENDIENTE',
  creadaEn: '2026-09-02T12:00:00Z',
  ...p,
})

/** Una página del contrato, sin próxima por omisión. */
export const pagina = <T>(items: T[], proximoFecha: string | null = null, proximoId: string | null = null) => ({ items, proximoFecha, proximoId })

/** Doble declarado del gateway para las pruebas de publicidad (regla 65): `HttpTestingController` con la forma del contrato. */
export const GW = 'http://gw/api/v1'

export function configurar(permisos: string[], extra: unknown[] = []): HttpTestingController {
  if (!Element.prototype.scrollTo) Element.prototype.scrollTo = () => {}
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideRouter([]),
      provideHttpClient(withInterceptors([trazaInterceptor, erroresInterceptor, idempotenciaInterceptor])),
      provideHttpClientTesting(),
      { provide: GATEWAY, useValue: GW },
      ...(extra as never[]),
    ],
  })
  TestBed.inject(Sesion).abrir('t', permisos, 'operador')
  return TestBed.inject(HttpTestingController)
}

/** Deja correr microtareas y efectos (zoneless) y vuelve a dibujar. */
export async function esperar(fixture: ComponentFixture<unknown>): Promise<void> {
  await new Promise((r) => setTimeout(r, 0))
  fixture.detectChanges()
  await new Promise((r) => setTimeout(r, 0))
  fixture.detectChanges()
}

export async function montar<T>(componente: Type<T>): Promise<ComponentFixture<T>> {
  const fixture = TestBed.createComponent(componente)
  fixture.detectChanges()
  await esperar(fixture)
  return fixture
}

export const texto = (f: ComponentFixture<unknown>) => (f.nativeElement as HTMLElement).textContent ?? ''

/** Un botón por su texto visible (o su nombre accesible), dentro del componente o del documento (diálogos). */
export function boton(raiz: ParentNode, nombre: string | RegExp): HTMLButtonElement | undefined {
  const coincide = (t: string | null | undefined) => (typeof nombre === 'string' ? t?.trim() === nombre : !!t && nombre.test(t.trim()))
  return [...raiz.querySelectorAll('button')].find((b) => coincide(b.textContent) || coincide(b.getAttribute('aria-label'))) as HTMLButtonElement | undefined
}

/** Escribe en el buscador del listado y espera su pausa de tipeo. */
export async function buscar(f: ComponentFixture<unknown>, valor: string): Promise<void> {
  const campo = (f.nativeElement as HTMLElement).querySelector('ap-barra-de-filtros input') as HTMLInputElement
  campo.value = valor
  campo.dispatchEvent(new Event('input'))
  await new Promise((r) => setTimeout(r, 400))
  await esperar(f)
}

/** Elige una opción del selector cuyo rótulo es `etiqueta`. */
export async function elegir(f: ComponentFixture<unknown>, etiqueta: string, valor: string): Promise<void> {
  const caja = [...(f.nativeElement as HTMLElement).querySelectorAll('ap-lista-de-opciones')].find((l) => l.textContent?.includes(etiqueta))
  const select = caja?.querySelector('select') as HTMLSelectElement
  select.value = valor
  select.dispatchEvent(new Event('change'))
  await esperar(f)
}
