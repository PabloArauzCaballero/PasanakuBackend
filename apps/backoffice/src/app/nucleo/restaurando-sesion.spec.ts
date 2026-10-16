import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideLocationMocks } from '@angular/common/testing'
import { Component, provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
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
})
