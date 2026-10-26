import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { ApplicationRef, provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter, Router } from '@angular/router'
import { RouterTestingHarness } from '@angular/router/testing'
import { describe, expect, it, vi } from 'vitest'
import { erroresInterceptor } from '../../nucleo/errores.interceptor'
import { GATEWAY } from '../../nucleo/gateway'
import type { SolicitudDeIngreso } from './dominio/cu68-admision'
import { EntradaDeAdmision } from './entrada-de-admision'

const GRUPO = '7a1c2b7e-3a54-4d0e-8a61-5b7d2c9e4f10'
const URL = `http://gw/api/v1/grupos/${GRUPO}/solicitudes`
/** SINTÉTICAS, con la forma de `SolicitudDeIngreso` de grupos.yaml. */
const solicitud = (i: number, extra: Partial<SolicitudDeIngreso> = {}): SolicitudDeIngreso => ({
  solicitudId: `00000000-0000-4000-8000-${String(i).padStart(12, '0')}`,
  usuarioId: `10000000-0000-4000-8000-${String(i).padStart(12, '0')}`,
  cuposSolicitados: 1 + (i % 2),
  mensaje: `Quiero entrar al grupo ${i}`,
  puntaje: '70',
  nombreVisible: `Persona ${i} S.`,
  porEnlace: i % 3 === 0,
  estado: 'PENDIENTE',
  fechaSolicitud: '2026-10-20T14:00:00Z',
  ...extra,
})

async function abrir(url: string) {
  if (!Element.prototype.scrollTo) Element.prototype.scrollTo = () => {}
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideHttpClient(withInterceptors([erroresInterceptor])),
      provideHttpClientTesting(),
      provideRouter([{ path: '**', component: EntradaDeAdmision }]),
      { provide: GATEWAY, useValue: 'http://gw/api/v1' },
    ],
  })
  const arnes = await RouterTestingHarness.create(url)
  TestBed.inject(ApplicationRef).tick()
  return { arnes, http: TestBed.inject(HttpTestingController), raiz: () => arnes.routeNativeElement! }
}

async function dibujar(arnes: RouterTestingHarness) {
  await arnes.fixture.whenStable()
  arnes.fixture.detectChanges()
}

/** CU-68 · La entrada: pendientes de un grupo (con «Resolver») o abrir una solicitud por identificador. */
describe('EntradaDeAdmision · CU-68', () => {
  it('aceptado · con ?grupo= pide sus pendientes, pagina de a 10, busca y «Resolver» abre el expediente', async () => {
    const { arnes, http, raiz } = await abrir(`/admisiones?grupo=${GRUPO}`)
    http.expectOne(URL).flush(Array.from({ length: 12 }, (_, i) => solicitud(i + 1)))
    await dibujar(arnes)
    expect(raiz().textContent).toContain('Página 1 de 2')
    expect(raiz().textContent).not.toContain('10000000-0000') // el usuarioId no se muestra
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true)
    raiz().querySelector<HTMLButtonElement>('button[aria-label="Resolver Persona 1 S."]')!.click()
    expect(navegar).toHaveBeenCalledWith(['/admisiones', solicitud(1).solicitudId])
    expect(raiz().querySelector('button[aria-label^="Eliminar"]')).toBeNull()
  })

  it('aceptado · elegir un grupo lo deja en la URL; abrir por identificador navega a la solicitud', async () => {
    const { arnes, raiz } = await abrir('/admisiones')
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true)
    const campo = raiz().querySelector<HTMLInputElement>('ap-campo input')!
    campo.value = GRUPO.toUpperCase()
    campo.dispatchEvent(new Event('input'))
    raiz().querySelector<HTMLFormElement>('form')!.dispatchEvent(new Event('submit'))
    expect(navegar).toHaveBeenCalledWith([], expect.objectContaining({ queryParams: { grupo: GRUPO } }))
    await dibujar(arnes)
  })

  it('límite · sin grupo no pide nada y orienta; grupo sin pendientes lo dice; identificador inválido marca el campo', async () => {
    const { arnes, http, raiz } = await abrir('/admisiones')
    expect(http.match(() => true)).toHaveLength(0)
    expect(raiz().textContent).toContain('Elegí un grupo para ver sus solicitudes pendientes')
    const campo = raiz().querySelector<HTMLInputElement>('ap-campo input')!
    campo.value = 'no-es-un-grupo'
    campo.dispatchEvent(new Event('input'))
    raiz().querySelector<HTMLFormElement>('form')!.dispatchEvent(new Event('submit'))
    await dibujar(arnes)
    expect(raiz().textContent).toContain('Ese identificador no tiene el formato de un grupo.')
    await arnes.navigateByUrl(`/admisiones?grupo=${GRUPO}`)
    TestBed.inject(ApplicationRef).tick()
    http.expectOne(URL).flush([])
    await dibujar(arnes)
    expect(raiz().textContent).toContain('Este grupo no tiene solicitudes pendientes.')
  })

  it('error · 403 (la cola es del organizador) se dice traducido y «Volver a intentar» repite el pedido', async () => {
    const { arnes, http, raiz } = await abrir(`/admisiones?grupo=${GRUPO}`)
    http.expectOne(URL).flush({ codigo: 'AP-SEG-403', mensaje: 'crudo', trazaId: 'tz-a' }, { status: 403, statusText: 'x' })
    await dibujar(arnes)
    const alerta = raiz().querySelector('[role="alert"]')!
    expect(alerta.textContent).toContain('tz-a')
    expect(alerta.textContent).not.toContain('crudo')
    ;[...raiz().querySelectorAll('button')].find((b) => b.textContent?.includes('Volver a intentar'))!.click()
    TestBed.inject(ApplicationRef).tick()
    expect(http.match(URL)).toHaveLength(1)
  })
})
