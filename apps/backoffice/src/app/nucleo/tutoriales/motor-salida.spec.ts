import { TestBed } from '@angular/core/testing'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { MotorDeTutoriales } from '@aportaya/tutoriales/motor'
import { CATALOGO_DE_PRUEBA, proveedoresDeTutoriales, sembrarObjetivo } from './preparar-motor'
import { ProgresoDeTutoriales } from '@aportaya/tutoriales/progreso'
import { RegistroDeTutoriales } from '@aportaya/tutoriales/registro'
import { Sesion } from '../sesion'

/**
 * Lo que pasa cuando el recorrido NO sale derecho: un objetivo que no aparece, uno que
 * tarda, y las formas de irse. El recorrido feliz vive en `motor.spec.ts`.
 */
async function montar(): Promise<{ motor: MotorDeTutoriales; progresos: ProgresoDeTutoriales }> {
  TestBed.resetTestingModule()
  TestBed.configureTestingModule({ providers: proveedoresDeTutoriales(CATALOGO_DE_PRUEBA) })
  TestBed.inject(Sesion).abrir('token', [], 'rol', 'op-salida')
  await TestBed.inject(RegistroDeTutoriales).cargar()
  return { motor: TestBed.inject(MotorDeTutoriales), progresos: TestBed.inject(ProgresoDeTutoriales) }
}

describe('MotorDeTutoriales · cuando algo falla o alguien se va', () => {
  beforeEach(() => {
    localStorage.clear()
    document.body.innerHTML = ''
    for (const id of ['uno', 'dos', 'tres']) sembrarObjetivo(id)
  })
  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('un objetivo que no aparece deja el problema declarado y NO traba el tutorial', async () => {
    const { motor } = await montar()
    await motor.iniciar('fantasma')
    expect(motor.activo()).toBe(true)
    expect(motor.problema()?.motivo).toBe('sin-objetivo')
    expect(motor.elemento()).toBeNull()
  })

  it('espera a un elemento que llega después de una petición', async () => {
    const { motor } = await montar()
    setTimeout(() => sembrarObjetivo('llega-despues'), 10)
    await motor.iniciar('tardio')
    expect(motor.elemento()).not.toBeNull()
    expect(motor.problema()).toBeNull()
  })

  it('reintentar vuelve a buscar el objetivo del paso actual', async () => {
    const { motor } = await montar()
    await motor.iniciar('fantasma')
    expect(motor.problema()).not.toBeNull()
    sembrarObjetivo('no-existe-en-ningun-lado')
    await motor.reintentar()
    expect(motor.problema()).toBeNull()
    expect(motor.elemento()).not.toBeNull()
  })

  it('salir en el primer paso no pregunta; a la mitad sí', async () => {
    const { motor } = await montar()
    await motor.iniciar('tres')
    motor.pedirSalida()
    expect(motor.activo()).toBe(false)

    await motor.iniciar('tres')
    await motor.avanzar()
    motor.pedirSalida()
    expect(motor.confirmandoSalida()).toBe(true)
    expect(motor.activo()).toBe(true)
  })

  it('seguir en el tutorial cancela la salida', async () => {
    const { motor } = await montar()
    await motor.iniciar('tres')
    await motor.avanzar()
    motor.pedirSalida()
    motor.seguirEnElTutorial()
    expect(motor.confirmandoSalida()).toBe(false)
    expect(motor.activo()).toBe(true)
  })

  it('omitir guarda el paso donde se fue, para poder volver', async () => {
    const { motor, progresos } = await montar()
    await motor.iniciar('tres')
    await motor.avanzar()
    motor.omitir()
    expect(motor.activo()).toBe(false)
    expect(progresos.de('tres')).toMatchObject({ estado: 'omitido', indice: 1, pasoId: 'p2' })
  })

  it('un tutorial omitido se puede repetir desde cero', async () => {
    const { motor, progresos } = await montar()
    await motor.iniciar('tres')
    await motor.avanzar()
    motor.omitir()
    await motor.iniciar('tres', progresos.de('tres')?.repeticiones ?? 0)
    expect(motor.indice()).toBe(0)
    expect(motor.activo()).toBe(true)
  })
})
