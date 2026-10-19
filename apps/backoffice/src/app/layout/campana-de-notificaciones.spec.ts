import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter, Router } from '@angular/router'
import { beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../nucleo/gateway'
import { AvisosInternosServicio } from '../nucleo/avisos-internos'
import { CampanaDeNotificaciones } from './campana-de-notificaciones'
import type { AvisoInterno, PaginaDeAvisosInternos } from 'clientes/angular/notificaciones'

const GATEWAY_TEST = 'https://gateway.test/api/v1'

describe('CampanaDeNotificaciones', () => {
  let http: HttpTestingController
  let servicio: AvisosInternosServicio

  const aviso1: AvisoInterno = {
    id: '11111111-1111-4111-8111-111111111111',
    titulo: 'Alerta de prueba',
    mensaje: 'Mensaje descriptivo',
    tipo: 'ALERTA' as any,
    enlace: '/operacion/reclamos',
    creadoEn: '2026-10-19T10:00:00Z',
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: GATEWAY_TEST },
      ],
    })
    http = TestBed.inject(HttpTestingController)
    servicio = TestBed.inject(AvisosInternosServicio)
  })

  function montar() {
    const fixture = TestBed.createComponent(CampanaDeNotificaciones)
    fixture.detectChanges()
    return { fixture, el: fixture.nativeElement as HTMLElement }
  }

  it('no muestra insignia si no hay avisos no leídos', () => {
    const { el } = montar()
    expect(el.querySelector('.insignia')).toBeNull()
    expect(el.querySelector('.disparador')?.getAttribute('aria-label')).toBe('Avisos del sistema')
  })

  it('muestra insignia y abre panel al hacer clic en el disparador', async () => {
    const { fixture, el } = montar()

    // Simulamos carga con 1 no leído
    const pagina: PaginaDeAvisosInternos = {
      totalNoLeidos: 1,
      elementos: [aviso1],
    }
    servicio.cargar().subscribe()
    http.expectOne((r) => r.url.includes('/notificaciones/avisos-internos')).flush(pagina)
    await fixture.whenStable()
    fixture.detectChanges()

    const insignia = el.querySelector('.insignia')
    expect(insignia?.textContent?.trim()).toBe('1')
    expect(el.querySelector('.disparador')?.getAttribute('aria-label')).toBe('Avisos del sistema, 1 no leídos')

    // Clic para abrir
    const boton = el.querySelector('.disparador') as HTMLButtonElement
    boton.click()
    // Al abrir recarga
    http.expectOne((r) => r.url.includes('/notificaciones/avisos-internos')).flush(pagina)
    await fixture.whenStable()
    fixture.detectChanges()

    expect(el.querySelector('.panel')).not.toBeNull()
    expect(el.querySelector('.titulo-aviso')?.textContent?.trim()).toBe('Alerta de prueba')
  })

  it('permite marcar un aviso como leído', async () => {
    const { fixture, el } = montar()
    servicio.cargar().subscribe()
    http.expectOne((r) => r.url.includes('/notificaciones/avisos-internos')).flush({
      totalNoLeidos: 1,
      elementos: [aviso1],
    })
    await fixture.whenStable()
    fixture.detectChanges()

    // Abrir panel
    ;(el.querySelector('.disparador') as HTMLButtonElement).click()
    http.expectOne((r) => r.url.includes('/notificaciones/avisos-internos')).flush({
      totalNoLeidos: 1,
      elementos: [aviso1],
    })
    await fixture.whenStable()
    fixture.detectChanges()

    const btnLeido = el.querySelector('.accion-leido') as HTMLButtonElement
    expect(btnLeido).not.toBeNull()
    btnLeido.click()

    const req = http.expectOne(`${GATEWAY_TEST}/notificaciones/avisos-internos/${aviso1.id}/leido`)
    expect(req.request.method).toBe('POST')
    req.flush({ ...aviso1, leidoEn: '2026-10-19T10:05:00Z' })

    await fixture.whenStable()
    fixture.detectChanges()

    expect(servicio.noLeidos()).toBe(0)
  })

  it('navegar a un enlace cierra el panel y navega con el router', async () => {
    const router = TestBed.inject(Router)
    const destinos: string[] = []
    router.navigateByUrl = ((url: string) => { destinos.push(url); return Promise.resolve(true) }) as Router['navigateByUrl']

    const { fixture, el } = montar()
    servicio.cargar().subscribe()
    http.expectOne((r) => r.url.includes('/notificaciones/avisos-internos')).flush({
      totalNoLeidos: 1,
      elementos: [aviso1],
    })
    await fixture.whenStable()
    fixture.detectChanges()

    ;(el.querySelector('.disparador') as HTMLButtonElement).click()
    http.expectOne((r) => r.url.includes('/notificaciones/avisos-internos')).flush({
      totalNoLeidos: 1,
      elementos: [aviso1],
    })
    await fixture.whenStable()
    fixture.detectChanges()

    const btnEnlace = el.querySelector('.enlace-accion') as HTMLButtonElement
    btnEnlace.click()
    await fixture.whenStable()
    fixture.detectChanges()

    expect(destinos).toEqual(['/operacion/reclamos'])
    expect(el.querySelector('.panel')).toBeNull()
  })
})
