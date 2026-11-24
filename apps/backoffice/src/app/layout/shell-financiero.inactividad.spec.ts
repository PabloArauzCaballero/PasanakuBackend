import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { Component, provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter, Router } from '@angular/router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { GATEWAY } from '../nucleo/gateway'
import { PLAZOS_DE_INACTIVIDAD } from '../nucleo/inactividad'
import { Sesion } from '../nucleo/sesion'
import { proveerTutoriales } from '../rutas/ayuda/catalogo/proveer'
import { ShellFinanciero } from './shell-financiero'

const GATEWAY_TEST = 'https://gateway.test/api/v1'

@Component({ selector: 'ap-vacio-de-prueba', template: '' })
class VacioDePrueba {}

describe('ShellFinanciero · cierre por inactividad (ADR-038 §1, P-8)', () => {
  let http: HttpTestingController

  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval', 'Date'] })
    vi.setSystemTime(new Date('2026-11-24T15:00:00Z'))
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([
          { path: 'ingreso', component: VacioDePrueba },
          { path: 'operacion/retiros', component: VacioDePrueba },
        ]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: GATEWAY_TEST },
        // Los mismos plazos en proporción, en segundos: 15 → 3 s, aviso 2 → 1 s.
        { provide: PLAZOS_DE_INACTIVIDAD, useValue: { limiteMs: 3_000, avisoAntesMs: 1_000, latidoMs: 600_000 } },
        ...proveerTutoriales(),
      ],
    })
    TestBed.inject(Sesion).abrir('token', ['TABLERO_VER'], 'ADMIN_PLATAFORMA', 'sujeto')
    http = TestBed.inject(HttpTestingController)
  })

  afterEach(() => vi.useRealTimers())

  it('avisa con un diálogo accesible y «Seguir trabajando» lo cierra sin salir', async () => {
    const fixture = TestBed.createComponent(ShellFinanciero)
    await fixture.whenStable()

    vi.advanceTimersByTime(2_000)
    await fixture.whenStable()
    const dialogo: HTMLDialogElement | null = fixture.nativeElement.querySelector('dialog')
    expect(dialogo?.textContent).toContain('Tu sesión está por cerrarse')
    expect(dialogo?.getAttribute('aria-labelledby')).toBeTruthy()

    const seguir = Array.from(dialogo!.querySelectorAll('button')).find((b) => b.textContent?.includes('Seguir trabajando'))
    seguir!.dispatchEvent(new Event('pointerdown', { bubbles: true }))
    seguir!.click()
    await fixture.whenStable()

    expect(fixture.nativeElement.querySelector('dialog')).toBeNull()
    expect(TestBed.inject(Sesion).abierta()).toBe(true)
    http.expectNone(`${GATEWAY_TEST}/sesion`)
  })

  it('a los 15 «minutos» revoca la sesión en el servidor y vuelve al ingreso con el motivo y la pantalla de origen', async () => {
    const router = TestBed.inject(Router)
    await router.navigateByUrl('/operacion/retiros')
    const fixture = TestBed.createComponent(ShellFinanciero)
    await fixture.whenStable()

    vi.advanceTimersByTime(3_000)
    await fixture.whenStable()

    const cierre = http.expectOne(`${GATEWAY_TEST}/sesion`)
    expect(cierre.request.method).toBe('DELETE')
    cierre.flush(null, { status: 204, statusText: 'No Content' })
    await fixture.whenStable()

    expect(TestBed.inject(Sesion).abierta()).toBe(false)
    expect(router.url).toBe('/ingreso?motivo=inactividad&volverA=%2Foperacion%2Fretiros')
  })
})
