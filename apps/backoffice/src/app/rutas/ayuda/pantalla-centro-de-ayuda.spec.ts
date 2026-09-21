import { Component, provideZonelessChangeDetection } from '@angular/core'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { beforeEach, describe, expect, it } from 'vitest'
import { ALMACEN_DE_PROGRESO } from '@aportaya/tutoriales/almacen'
import { AlmacenLocal } from '@aportaya/tutoriales/almacen-local'
import { BITACORA_DE_TUTORIALES, BitacoraEnMemoria } from '@aportaya/tutoriales/bitacora'
import { MotorDeTutoriales } from '@aportaya/tutoriales/motor'
import { ProgresoDeTutoriales } from '@aportaya/tutoriales/progreso'
import { CARGADORES_DE_TUTORIALES, RUTAS_DEL_PRODUCTO, RegistroDeTutoriales } from '@aportaya/tutoriales/registro'
import { Sesion } from '../../nucleo/sesion'
import type { TutorialDefinicion } from '@aportaya/tutoriales/tipos'
import { PantallaCentroDeAyuda } from '@aportaya/tutoriales/centro/pantalla-centro-de-ayuda'
import { identidadDelOperador } from '../../rutas/ayuda/catalogo/proveer'

@Component({ template: 'tablero' })
class Tablero {}

const paso = (id: string) => ({ id, titulo: id, descripcion: 'd', objetivo: 'ayuda-lista' })

const CATALOGO: TutorialDefinicion[] = [
  { id: 'intro', version: '1', titulo: 'Qué es el backoffice', descripcion: 'Una vuelta general', categoria: 'Primeros pasos', ruta: '/tablero', minutos: 3, dificultad: 'inicial', obligatorio: true, pasos: [paso('a'), paso('b')] },
  { id: 'billetera', version: '1', titulo: 'Mirar una billetera', descripcion: 'Saldos de terceros', categoria: 'Operación', ruta: '/tablero', minutos: 4, dificultad: 'intermedio', permisos: ['ver:operacion'], pasos: [paso('a')] },
]

async function montar(permisos: readonly string[] = []): Promise<ComponentFixture<PantallaCentroDeAyuda>> {
  TestBed.resetTestingModule()
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      identidadDelOperador(),
      provideRouter([{ path: 'tablero', component: Tablero }]),
      { provide: CARGADORES_DE_TUTORIALES, useValue: () => Promise.resolve(CATALOGO), multi: true },
      { provide: RUTAS_DEL_PRODUCTO, useValue: ['/tablero'] },
      { provide: ALMACEN_DE_PROGRESO, useClass: AlmacenLocal },
      { provide: BITACORA_DE_TUTORIALES, useClass: BitacoraEnMemoria },
    ],
  })
  TestBed.inject(Sesion).abrir('token', permisos, 'rol', 'op-centro')
  const fixture = TestBed.createComponent(PantallaCentroDeAyuda)
  await TestBed.inject(RegistroDeTutoriales).cargar()
  await fixture.whenStable()
  fixture.detectChanges()
  return fixture
}

const textos = (fixture: ComponentFixture<unknown>): string => fixture.nativeElement.textContent ?? ''
const tarjetas = (fixture: ComponentFixture<unknown>): number =>
  (fixture.nativeElement as HTMLElement).querySelectorAll('[data-tutorial-id="ayuda-lista"] ap-tarjeta-de-tutorial').length

describe('PantallaCentroDeAyuda', () => {
  beforeEach(() => localStorage.clear())

  it('lista los tutoriales que el rol puede ver, y no los otros', async () => {
    const fixture = await montar([])
    expect(textos(fixture)).toContain('Qué es el backoffice')
    expect(textos(fixture)).not.toContain('Mirar una billetera')
  })

  it('con el permiso del módulo aparece su tutorial', async () => {
    const fixture = await montar(['ver:operacion'])
    expect(textos(fixture)).toContain('Mirar una billetera')
  })

  it('muestra el avance general sobre lo que el rol puede ver', async () => {
    const fixture = await montar([])
    expect(textos(fixture)).toContain('0 de 1 tutoriales completados')
  })

  it('marca lo obligatorio y lo recomienda primero', async () => {
    const fixture = await montar([])
    expect(textos(fixture)).toContain('Obligatorio')
    expect(textos(fixture)).toContain('Recomendado para vos')
  })

  it('sin filtro, el recomendado va arriba y NO se repite en la lista', async () => {
    const fixture = await montar(['ver:operacion'])
    expect(textos(fixture)).toContain('Recomendado para vos')
    // Dos tutoriales disponibles: uno arriba como recomendado, el otro en la lista.
    expect(tarjetas(fixture)).toBe(1)
  })

  it('el buscador recorta la lista y guarda la recomendación mientras se busca', async () => {
    const fixture = await montar(['ver:operacion'])
    fixture.componentInstance['texto'].set('billetera')
    await fixture.whenStable()
    fixture.detectChanges()
    expect(tarjetas(fixture)).toBe(1)
    expect(textos(fixture)).toContain('Mirar una billetera')
    expect(textos(fixture)).not.toContain('Recomendado para vos')
  })

  it('una búsqueda sin resultados explica por qué no hay nada', async () => {
    const fixture = await montar([])
    fixture.componentInstance['texto'].set('zzzz')
    await fixture.whenStable()
    fixture.detectChanges()
    expect(textos(fixture)).toContain('Ningún tutorial coincide')
  })

  it('comenzar enciende el motor sobre la interfaz real', async () => {
    const fixture = await montar([])
    const motor = TestBed.inject(MotorDeTutoriales)
    fixture.componentInstance['comenzar']({ tutorial: CATALOGO[0]!, estado: 'pendiente', fraccion: 0, continuable: false, progreso: undefined, requisitosPendientes: [] })
    await fixture.whenStable()
    expect(motor.activo()).toBe(true)
    expect(motor.tutorial()?.id).toBe('intro')
  })

  it('reiniciar borra el avance de ese tutorial', async () => {
    const fixture = await montar([])
    const progresos = TestBed.inject(ProgresoDeTutoriales)
    const motor = TestBed.inject(MotorDeTutoriales)
    await motor.iniciar('intro')
    expect(progresos.de('intro')).toBeDefined()
    fixture.componentInstance['reiniciar']({ tutorial: CATALOGO[0]!, estado: 'en-progreso', fraccion: 0, continuable: false, progreso: progresos.de('intro'), requisitosPendientes: [] })
    await fixture.whenStable()
    expect(progresos.de('intro')).toBeUndefined()
  })

  it('un catálogo sano no muestra la alerta de problemas', async () => {
    const fixture = await montar([])
    expect(textos(fixture)).not.toContain('Problemas en el catálogo')
  })
})
