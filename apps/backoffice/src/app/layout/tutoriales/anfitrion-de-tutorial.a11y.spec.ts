import { Component, provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { ALMACEN_DE_PROGRESO } from '@aportaya/tutoriales/almacen'
import { AlmacenLocal } from '@aportaya/tutoriales/almacen-local'
import { BITACORA_DE_TUTORIALES, BitacoraEnMemoria } from '@aportaya/tutoriales/bitacora'
import { MotorDeTutoriales } from '@aportaya/tutoriales/motor'
import { ATRIBUTO } from '@aportaya/tutoriales/objetivo'
import { CARGADORES_DE_TUTORIALES, RUTAS_DEL_PRODUCTO, RegistroDeTutoriales } from '@aportaya/tutoriales/registro'
import { Sesion } from '../../nucleo/sesion'
import type { TutorialDefinicion } from '@aportaya/tutoriales/tipos'
import { AnfitrionDeTutorial } from '@aportaya/tutoriales/anfitrion-de-tutorial'
import { identidadDelOperador } from '../../rutas/ayuda/catalogo/proveer'

@Component({ template: 'tablero' })
class Tablero {}

const TUTORIAL: TutorialDefinicion = {
  id: 'a11y',
  version: '1',
  titulo: 'Recorrido',
  descripcion: 'd',
  categoria: 'Pruebas',
  ruta: '/tablero',
  dificultad: 'inicial',
  pasos: [
    { id: 'p1', titulo: 'Mirá el menú', descripcion: 'Desde acá se llega a todo.', objetivo: 'uno' },
    { id: 'p2', titulo: 'Y la tabla', descripcion: 'Cada fila es un expediente.', objetivo: 'dos' },
  ],
}

describe('AnfitrionDeTutorial · accesibilidad', () => {
  it('el globo abierto no tiene violaciones serias', async () => {
    document.body.innerHTML = ''
    for (const id of ['uno', 'dos']) {
      const div = document.createElement('div')
      div.setAttribute(ATRIBUTO, id)
      document.body.appendChild(div)
    }
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
      identidadDelOperador(),
        provideRouter([{ path: 'tablero', component: Tablero }]),
        { provide: CARGADORES_DE_TUTORIALES, useValue: () => Promise.resolve([TUTORIAL]), multi: true },
        { provide: RUTAS_DEL_PRODUCTO, useValue: ['/tablero'] },
        { provide: ALMACEN_DE_PROGRESO, useClass: AlmacenLocal },
        { provide: BITACORA_DE_TUTORIALES, useClass: BitacoraEnMemoria },
      ],
    })
    TestBed.inject(Sesion).abrir('token', [], 'rol', 'op-a11y')
    const fixture = TestBed.createComponent(AnfitrionDeTutorial)
    await TestBed.inject(RegistroDeTutoriales).cargar()
    await TestBed.inject(MotorDeTutoriales).iniciar('a11y')
    await fixture.whenStable()
    fixture.detectChanges()
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  }, PLAZO)
})

/** `axe` recorre el árbol entero: con el plazo de 5 s falla por reloj, no por accesibilidad. */
const PLAZO = 30_000
