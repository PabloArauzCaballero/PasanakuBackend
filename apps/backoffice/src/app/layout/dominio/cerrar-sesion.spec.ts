import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { Router, provideRouter } from '@angular/router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { GATEWAY } from '../../nucleo/gateway'
import { INTERCEPTORES_DE_RED } from '../../nucleo/interceptores'
import { Sesion } from '../../nucleo/sesion'
import { proveerTutoriales } from '../../rutas/ayuda/catalogo/proveer'
import { ShellFinanciero } from '../shell-financiero'

const GW = 'https://gateway.test/api/v1'

/**
 * «Salir» revoca de verdad: con el token de acceso vencido, el `DELETE /sesion` en 401 pide `/sesion/refrescar` y
 * reintenta el `DELETE` con el token nuevo (ese es el que revoca la familia de refresh). Y si el refresco falla, igual
 * se cierra la sesión local y se vuelve al ingreso. Con los interceptores reales de la app.
 */
describe('Salir · cerrar la sesión con el token vencido', () => {
  let http: HttpTestingController
  let sesion: Sesion
  let destinos: string[]

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideHttpClient(withInterceptors(INTERCEPTORES_DE_RED)),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: GW },
        ...proveerTutoriales(),
      ],
    })
    http = TestBed.inject(HttpTestingController)
    sesion = TestBed.inject(Sesion)
    sesion.abrir('token-vencido', ['RECLAMO_VER'], 'SOPORTE')
    destinos = []
    TestBed.inject(Router).navigateByUrl = vi.fn((url: string) => (destinos.push(url), Promise.resolve(true))) as unknown as Router['navigateByUrl']
  })

  function salir(): void {
    const fixture = TestBed.createComponent(ShellFinanciero)
    fixture.detectChanges()
    ;(Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('button')).find((b) => b.textContent?.includes('Salir')) as HTMLButtonElement).click()
  }

  it('correcto: DELETE 401 → refresca → reintenta el DELETE con el token nuevo → sesión cerrada e /ingreso', () => {
    salir()
    const primero = http.expectOne({ method: 'DELETE', url: `${GW}/sesion` })
    expect(primero.request.headers.get('Authorization')).toBe('Bearer token-vencido')
    primero.flush({ codigo: 'AP-SES-01' }, { status: 401, statusText: 'Unauthorized' })
    http.expectOne({ method: 'POST', url: `${GW}/sesion/refrescar` }).flush({ acceso: 'token-nuevo' })
    const reintento = http.expectOne({ method: 'DELETE', url: `${GW}/sesion` })
    expect(reintento.request.headers.get('Authorization')).toBe('Bearer token-nuevo')
    reintento.flush(null, { status: 204, statusText: 'No Content' })
    expect(sesion.abierta()).toBe(false)
    expect(destinos).toEqual(['/ingreso'])
  })

  it('error: si el refresco también falla, igual se cierra la sesión local y se vuelve a /ingreso', () => {
    salir()
    http.expectOne({ method: 'DELETE', url: `${GW}/sesion` }).flush({}, { status: 401, statusText: 'Unauthorized' })
    http.expectOne({ method: 'POST', url: `${GW}/sesion/refrescar` }).flush({}, { status: 401, statusText: 'Unauthorized' })
    http.expectNone({ method: 'DELETE', url: `${GW}/sesion` })
    expect(sesion.abierta()).toBe(false)
    expect(destinos).toEqual(['/ingreso'])
  })
})
