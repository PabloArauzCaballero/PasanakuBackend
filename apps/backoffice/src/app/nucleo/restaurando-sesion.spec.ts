import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideLocationMocks } from '@angular/common/testing'
import { Component, provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter, Router } from '@angular/router'
import { describe, expect, it } from 'vitest'
import { GATEWAY } from './gateway'
import { RestaurandoSesion } from './restaurando-sesion'
import { Sesion } from './sesion'

const GATEWAY_TEST = 'https://gateway.test/api/v1'

@Component({ selector: 'ap-vacio-de-prueba', template: '' })
class VacioDePrueba {}

function configurar() {
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideHttpClient(),
      provideHttpClientTesting(),
      provideRouter([
        { path: 'tablero', component: VacioDePrueba },
        { path: 'ingreso', component: VacioDePrueba },
      ]),
      provideLocationMocks(),
      { provide: GATEWAY, useValue: GATEWAY_TEST },
    ],
  })
}

describe('RestaurandoSesion', () => {
  it('mientras restaura, no muestra el botón de reintentar', () => {
    configurar()
    const sesion = TestBed.inject(Sesion)
    sesion.restaurando()
    const fixture = TestBed.createComponent(RestaurandoSesion)
    fixture.detectChanges()
    const texto: string = fixture.nativeElement.textContent
    expect(texto).toContain('Restaurando')
    expect(fixture.nativeElement.querySelector('button')).toBeNull()
  })

  it('en ERROR, muestra el mensaje accionable con "Reintentar"', () => {
    configurar()
    const sesion = TestBed.inject(Sesion)
    sesion.restaurando()
    sesion.fallo()
    const fixture = TestBed.createComponent(RestaurandoSesion)
    fixture.detectChanges()
    const texto: string = fixture.nativeElement.textContent
    expect(texto).toContain('Reintentar')
    expect(fixture.nativeElement.querySelector('[role="alert"]')).not.toBeNull()
  })

  it('el click en "Reintentar" dispara un nuevo POST /sesion/refrescar', async () => {
    configurar()
    const sesion = TestBed.inject(Sesion)
    sesion.restaurando()
    sesion.fallo()
    const fixture = TestBed.createComponent(RestaurandoSesion)
    fixture.detectChanges()
    const backend = TestBed.inject(HttpTestingController)

    const boton = fixture.nativeElement.querySelector('button') as HTMLButtonElement
    boton.click()

    const peticion = backend.expectOne(`${GATEWAY_TEST}/sesion/refrescar`)
    peticion.flush({ acceso: 'nuevo', permisos: [], rol: 'oficial' })
    await fixture.whenStable()

    expect(sesion.estado()).toBe('AUTHENTICATED')
    backend.verify()
  })

  // 2026-10-19: abriendo /arranque directo, el arranque ya resolvió la sesión y la
  // pantalla quedaba en «Restaurando tu sesión…» para siempre.
  it('sin sesión (401 del arranque) sale sola a /ingreso', async () => {
    configurar()
    const sesion = TestBed.inject(Sesion)
    sesion.restaurando()
    sesion.anonima()
    const router = TestBed.inject(Router)
    await router.navigateByUrl('/tablero')
    const fixture = TestBed.createComponent(RestaurandoSesion)
    fixture.detectChanges()
    await fixture.whenStable()
    expect(router.url).toBe('/ingreso')
  })

  it('con sesión ya abierta sale sola al tablero', async () => {
    configurar()
    const sesion = TestBed.inject(Sesion)
    sesion.abrir('token', [], 'oficial')
    const router = TestBed.inject(Router)
    await router.navigateByUrl('/ingreso')
    const fixture = TestBed.createComponent(RestaurandoSesion)
    fixture.detectChanges()
    await fixture.whenStable()
    expect(router.url).toBe('/tablero')
  })

  it('cuando la restauración termina mientras se ve, sale sola', async () => {
    configurar()
    const sesion = TestBed.inject(Sesion)
    sesion.restaurando()
    const router = TestBed.inject(Router)
    await router.navigateByUrl('/tablero')
    const fixture = TestBed.createComponent(RestaurandoSesion)
    fixture.detectChanges()
    expect(fixture.nativeElement.textContent).toContain('Restaurando')
    sesion.anonima()
    TestBed.tick()
    await fixture.whenStable()
    expect(router.url).toBe('/ingreso')
  })
})
