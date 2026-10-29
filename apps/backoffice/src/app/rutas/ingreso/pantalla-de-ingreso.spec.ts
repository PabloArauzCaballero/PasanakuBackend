import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed, type ComponentFixture } from '@angular/core/testing'
import { Router, provideRouter } from '@angular/router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { GATEWAY } from '../../nucleo/gateway'
import { erroresInterceptor } from '../../nucleo/errores.interceptor'
import { Sesion } from '../../nucleo/sesion'
import { sesionInterceptor } from '../../nucleo/sesion.interceptor'
import { PantallaDeIngreso } from './pantalla-de-ingreso'

const GW = 'https://gateway.test/api/v1'
const token = (permisos: string[]) => `x.${btoa(JSON.stringify({ permisos, rol: 'oficial', sub: 's' })).replace(/=+$/, '')}.f`

/** CU-04 · ingreso en dos pasos (credencial y código), contra el doble HTTP. Aceptado, límite y error. */
describe('PantallaDeIngreso', () => {
  let fixture: ComponentFixture<PantallaDeIngreso>
  let http: HttpTestingController
  let el: HTMLElement
  let navegar: ReturnType<typeof vi.fn>

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideHttpClient(withInterceptors([erroresInterceptor, sesionInterceptor])),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: GW },
      ],
    })
    http = TestBed.inject(HttpTestingController)
    navegar = vi.fn(() => Promise.resolve(true))
    TestBed.inject(Router).navigateByUrl = navegar as unknown as Router['navigateByUrl']
    fixture = TestBed.createComponent(PantallaDeIngreso)
    fixture.detectChanges()
    el = fixture.nativeElement as HTMLElement
  })

  const escribir = (selector: string, valor: string) => {
    const input = el.querySelector(selector) as HTMLInputElement
    input.value = valor
    input.dispatchEvent(new Event('input'))
    fixture.detectChanges()
  }
  const enviar = () => {
    el.querySelector('form')!.dispatchEvent(new Event('submit'))
    fixture.detectChanges()
  }
  const credencial = () => {
    escribir('input[autocomplete="username"]', '71234567')
    escribir('input[type="password"]', 'contrasena-e2e')
    enviar()
  }

  it('aceptado: credencial → pide código (AP-CU04-03) → código → abre la sesión y va al tablero', () => {
    credencial()
    http.expectOne(`${GW}/sesiones`).flush({ codigo: 'AP-CU04-03' }, { status: 401, statusText: 'Unauthorized' })
    fixture.detectChanges()
    expect(el.querySelector('h1')?.textContent).toContain('Confirmá que sos vos')
    escribir('input[autocomplete="one-time-code"]', '123456')
    enviar()
    const segundo = http.expectOne(`${GW}/sesiones`)
    expect(segundo.request.body.factor).toEqual({ tipo: 'TOTP', valor: '123456' })
    segundo.flush({ sesionId: 's', requiereFactorAdicional: false, tokenAcceso: token(['RECLAMO_VER']) })
    expect(TestBed.inject(Sesion).abierta()).toBe(true)
    expect(navegar).toHaveBeenCalledWith('/tablero')
  })

  it('límite: campos vacíos marcan cada campo con su error y no piden nada', () => {
    enviar()
    http.expectNone(`${GW}/sesiones`)
    const errores = Array.from(el.querySelectorAll('[aria-invalid="true"]'))
    expect(errores.length).toBe(2)
    expect(el.textContent).toContain('La contraseña tiene al menos 8 caracteres.')
    expect(el.querySelector('ap-alerta')).toBeNull()
  })

  it('límite: un código incompleto se marca en el campo y no se envía; sin doble envío mientras espera', () => {
    credencial()
    // Mientras el primer pedido no contesta, otro envío no sale.
    enviar()
    http.expectOne(`${GW}/sesiones`).flush({ codigo: 'AP-CU04-03' }, { status: 401, statusText: 'Unauthorized' })
    fixture.detectChanges()
    escribir('input[autocomplete="one-time-code"]', '123')
    enviar()
    http.expectNone(`${GW}/sesiones`)
    expect(el.querySelector('input[autocomplete="one-time-code"]')?.getAttribute('aria-invalid')).toBe('true')
    expect(el.textContent).toContain('El código tiene 6 dígitos')
  })

  it('error: credenciales inválidas (401 AP-CU04-01 y 401 sin código) dicen qué hacer, sin refresco de sesión de por medio', () => {
    credencial()
    http.expectOne(`${GW}/sesiones`).flush({ codigo: 'AP-CU04-01' }, { status: 401, statusText: 'Unauthorized' })
    fixture.detectChanges()
    expect(el.querySelector('ap-alerta')?.textContent).toContain('no coinciden')
    enviar()
    http.expectOne(`${GW}/sesiones`).flush(null, { status: 401, statusText: 'Unauthorized' })
    fixture.detectChanges()
    http.expectNone(`${GW}/sesion/refrescar`)
    expect(el.querySelector('ap-alerta')?.textContent).toContain('no coinciden')
    expect(el.querySelector('ap-alerta')?.textContent).not.toContain('venció')
  })

  it('error: 500 muestra un mensaje accionable, conserva lo escrito y el botón vuelve a estar disponible', () => {
    credencial()
    http.expectOne(`${GW}/sesiones`).flush({ codigo: 'X' }, { status: 500, statusText: 'Server Error' })
    fixture.detectChanges()
    expect(el.querySelector('ap-alerta')?.textContent).toContain('Probá de nuevo')
    expect((el.querySelector('input[autocomplete="username"]') as HTMLInputElement).value).toBe('71234567')
    expect(el.querySelector('button[type="submit"]')?.hasAttribute('disabled')).toBe(false)
  })
})
