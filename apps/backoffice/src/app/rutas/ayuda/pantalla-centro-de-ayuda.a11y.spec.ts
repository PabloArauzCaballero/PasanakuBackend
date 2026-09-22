import { Component, provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { ALMACEN_DE_PROGRESO } from '@aportaya/tutoriales/almacen'
import { AlmacenLocal } from '@aportaya/tutoriales/almacen-local'
import { BITACORA_DE_TUTORIALES, BitacoraEnMemoria } from '@aportaya/tutoriales/bitacora'
import { CARGADORES_DE_TUTORIALES, RUTAS_DEL_PRODUCTO, RegistroDeTutoriales } from '@aportaya/tutoriales/registro'
import { Sesion } from '../../nucleo/sesion'
import { CATALOGO_DE_TUTORIALES } from './catalogo/catalogo'
import { RUTAS_CONOCIDAS } from './catalogo/proveer'
import { PantallaCentroDeAyuda } from '@aportaya/tutoriales/centro/pantalla-centro-de-ayuda'
import { identidadDelOperador } from '../../rutas/ayuda/catalogo/proveer'

@Component({ template: 'tablero' })
class Tablero {}

describe('PantallaCentroDeAyuda · accesibilidad', () => {
  it('sin violaciones, con el catálogo real y un rol que ve varias secciones', async () => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
      identidadDelOperador(),
        provideRouter([{ path: 'tablero', component: Tablero }]),
        { provide: CARGADORES_DE_TUTORIALES, useValue: () => Promise.resolve(CATALOGO_DE_TUTORIALES), multi: true },
        { provide: RUTAS_DEL_PRODUCTO, useValue: RUTAS_CONOCIDAS },
        { provide: ALMACEN_DE_PROGRESO, useClass: AlmacenLocal },
        { provide: BITACORA_DE_TUTORIALES, useClass: BitacoraEnMemoria },
      ],
    })
    TestBed.inject(Sesion).abrir('token', ['ver:operacion', 'ver:cumplimiento'], 'oficial', 'op-a11y')
    const fixture = TestBed.createComponent(PantallaCentroDeAyuda)
    await TestBed.inject(RegistroDeTutoriales).cargar()
    await fixture.whenStable()
    fixture.detectChanges()
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  }, PLAZO)
})

/** `axe` recorre el árbol entero: con el plazo de 5 s falla por reloj, no por accesibilidad. */
const PLAZO = 30_000
