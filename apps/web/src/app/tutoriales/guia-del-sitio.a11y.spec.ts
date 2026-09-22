import { Component, provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { ALMACEN_DE_PROGRESO } from '@aportaya/tutoriales/almacen'
import { AlmacenLocal } from '@aportaya/tutoriales/almacen-local'
import { BITACORA_DE_TUTORIALES, BitacoraEnMemoria } from '@aportaya/tutoriales/bitacora'
import { IDENTIDAD_DE_TUTORIALES, visitanteAnonimo } from '@aportaya/tutoriales/identidad'
import { MotorDeTutoriales } from '@aportaya/tutoriales/motor'
import { CARGADORES_DE_TUTORIALES, RegistroDeTutoriales, RUTAS_DEL_PRODUCTO } from '@aportaya/tutoriales/registro'
import { AnfitrionDeTutorial } from '@aportaya/tutoriales/anfitrion-de-tutorial'
import { PantallaCentroDeAyuda } from '@aportaya/tutoriales/centro/pantalla-centro-de-ayuda'
import { CATALOGO_DEL_SITIO } from './catalogo/catalogo'
import { RUTAS_DEL_SITIO } from './proveer'

@Component({ template: 'inicio' })
class Inicio {}

/** `axe` recorre el árbol entero: con el plazo de 5 s falla por reloj, no por accesibilidad. */
const PLAZO = 30_000

function preparar(): void {
  TestBed.resetTestingModule()
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideRouter([{ path: '', component: Inicio }]),
      { provide: CARGADORES_DE_TUTORIALES, useValue: () => Promise.resolve(CATALOGO_DEL_SITIO), multi: true },
      { provide: RUTAS_DEL_PRODUCTO, useValue: RUTAS_DEL_SITIO },
      { provide: ALMACEN_DE_PROGRESO, useClass: AlmacenLocal },
      { provide: BITACORA_DE_TUTORIALES, useClass: BitacoraEnMemoria },
      { provide: IDENTIDAD_DE_TUTORIALES, useValue: visitanteAnonimo },
    ],
  })
}

describe('la guía del sitio · accesibilidad', () => {
  it('el listado no tiene violaciones, con el catálogo real', async () => {
    preparar()
    const fixture = TestBed.createComponent(PantallaCentroDeAyuda)
    await TestBed.inject(RegistroDeTutoriales).cargar()
    await fixture.whenStable()
    fixture.detectChanges()
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  }, PLAZO)

  it('el globo abierto sobre una página del sitio tampoco', async () => {
    document.body.innerHTML = ''
    const ancla = document.createElement('h1')
    ancla.setAttribute('data-tutorial-id', 'sitio-que-hace')
    ancla.textContent = 'El pasanaku, en tu bolsillo'
    document.body.appendChild(ancla)

    preparar()
    const fixture = TestBed.createComponent(AnfitrionDeTutorial)
    await TestBed.inject(RegistroDeTutoriales).cargar()
    await TestBed.inject(MotorDeTutoriales).iniciar('sitio-que-es')
    await fixture.whenStable()
    fixture.detectChanges()
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  }, PLAZO)
})
