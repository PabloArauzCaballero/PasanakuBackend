import { Component, provideZonelessChangeDetection } from '@angular/core'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { beforeEach, describe, expect, it } from 'vitest'
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
  id: 'tres',
  version: '1',
  titulo: 'Tres pasos',
  descripcion: 'd',
  categoria: 'Pruebas',
  ruta: '/tablero',
  dificultad: 'inicial',
  pasos: [
    { id: 'p1', titulo: 'Primero esto', descripcion: 'Mirá el menú', objetivo: 'uno' },
    { id: 'p2', titulo: 'Después esto', descripcion: 'Mirá la tabla', objetivo: 'dos' },
    { id: 'p3', titulo: 'Y listo', descripcion: 'Terminaste', objetivo: 'tres' },
  ],
}

const sembrar = (id: string): HTMLElement => {
  const div = document.createElement('div')
  div.setAttribute(ATRIBUTO, id)
  document.body.appendChild(div)
  return div
}

async function montar(): Promise<{ fixture: ComponentFixture<AnfitrionDeTutorial>; motor: MotorDeTutoriales }> {
  TestBed.resetTestingModule()
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
  TestBed.inject(Sesion).abrir('token', [], 'rol', 'op-anfitrion')
  const fixture = TestBed.createComponent(AnfitrionDeTutorial)
  await TestBed.inject(RegistroDeTutoriales).cargar()
  await fixture.whenStable()
  fixture.detectChanges()
  return { fixture, motor: TestBed.inject(MotorDeTutoriales) }
}

const tecla = (key: string): void => {
  document.dispatchEvent(new KeyboardEvent('keydown', { key, bubbles: true }))
}
const texto = (fixture: ComponentFixture<unknown>): string => (fixture.nativeElement as HTMLElement).textContent ?? ''

describe('AnfitrionDeTutorial', () => {
  beforeEach(() => {
    localStorage.clear()
    document.body.innerHTML = ''
    for (const id of ['uno', 'dos', 'tres']) sembrar(id)
  })

  it('con el tutorial apagado no dibuja nada', async () => {
    const { fixture } = await montar()
    expect((fixture.nativeElement as HTMLElement).querySelector('ap-globo-de-tutorial')).toBeNull()
  })

  it('al iniciar aparece el globo con el paso y su cuenta', async () => {
    const { fixture, motor } = await montar()
    await motor.iniciar('tres')
    await fixture.whenStable()
    fixture.detectChanges()
    expect(texto(fixture)).toContain('Primero esto')
    expect(texto(fixture)).toContain('Paso 1 de 3')
  })

  it('el globo se anuncia como diálogo, con título y texto asociados', async () => {
    const { fixture, motor } = await montar()
    await motor.iniciar('tres')
    await fixture.whenStable()
    fixture.detectChanges()
    const globo = (fixture.nativeElement as HTMLElement).querySelector('ap-globo-de-tutorial')!
    expect(globo.getAttribute('role')).toBe('dialog')
    expect(globo.getAttribute('aria-labelledby')).toBeTruthy()
    expect(globo.getAttribute('aria-describedby')).toBeTruthy()
  })

  it('la flecha derecha avanza y la izquierda retrocede', async () => {
    const { fixture, motor } = await montar()
    await motor.iniciar('tres')
    tecla('ArrowRight')
    await fixture.whenStable()
    expect(motor.indice()).toBe(1)
    tecla('ArrowLeft')
    await fixture.whenStable()
    expect(motor.indice()).toBe(0)
  })

  it('Escape en el primer paso cierra sin preguntar', async () => {
    const { fixture, motor } = await montar()
    await motor.iniciar('tres')
    tecla('Escape')
    await fixture.whenStable()
    expect(motor.activo()).toBe(false)
  })

  it('Escape a la mitad pregunta antes de abandonar', async () => {
    const { fixture, motor } = await montar()
    await motor.iniciar('tres')
    await motor.avanzar()
    tecla('Escape')
    await fixture.whenStable()
    fixture.detectChanges()
    expect(motor.confirmandoSalida()).toBe(true)
    expect(motor.activo()).toBe(true)
    expect(texto(fixture)).toContain('¿Dejamos el tutorial acá?')
  })

  it('escribiendo en un campo, el teclado es del campo y no del tutorial', async () => {
    const { fixture, motor } = await montar()
    await motor.iniciar('tres')
    const campo = document.createElement('input')
    document.body.appendChild(campo)
    campo.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight', bubbles: true }))
    await fixture.whenStable()
    expect(motor.indice()).toBe(0)
  })

  it('el foco vuelve a donde estaba al terminar el tutorial', async () => {
    const { fixture, motor } = await montar()
    const boton = document.createElement('button')
    document.body.appendChild(boton)
    boton.focus()
    await motor.iniciar('tres')
    await fixture.whenStable()
    motor.omitir()
    await fixture.whenStable()
    fixture.detectChanges()
    expect(document.activeElement).toBe(boton)
  })

  it('en el último paso el botón principal dice Terminar', async () => {
    const { fixture, motor } = await montar()
    await motor.iniciar('tres')
    await motor.avanzar()
    await motor.avanzar()
    await fixture.whenStable()
    fixture.detectChanges()
    expect(texto(fixture)).toContain('Terminar')
  })
})
