import { TestBed } from '@angular/core/testing'
import { Router } from '@angular/router'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { BITACORA_DE_TUTORIALES, BitacoraEnMemoria } from '@aportaya/tutoriales/bitacora'
import { MotorDeTutoriales } from '@aportaya/tutoriales/motor'
import { ATRIBUTO } from '@aportaya/tutoriales/objetivo'
import { CATALOGO_DE_PRUEBA, proveedoresDeTutoriales, sembrarObjetivo } from './preparar-motor'
import { ProgresoDeTutoriales } from '@aportaya/tutoriales/progreso'
import { RegistroDeTutoriales } from '@aportaya/tutoriales/registro'
import { Sesion } from '../sesion'

/** El recorrido cuando todo sale bien. Lo que falla, en `motor-salida.spec.ts`. */
async function montar(): Promise<{ motor: MotorDeTutoriales; progresos: ProgresoDeTutoriales; router: Router }> {
  TestBed.resetTestingModule()
  TestBed.configureTestingModule({ providers: proveedoresDeTutoriales(CATALOGO_DE_PRUEBA) })
  TestBed.inject(Sesion).abrir('token', [], 'rol', 'op-motor')
  await TestBed.inject(RegistroDeTutoriales).cargar()
  return { motor: TestBed.inject(MotorDeTutoriales), progresos: TestBed.inject(ProgresoDeTutoriales), router: TestBed.inject(Router) }
}

describe('MotorDeTutoriales · el recorrido', () => {
  beforeEach(() => {
    localStorage.clear()
    document.body.innerHTML = ''
    for (const id of ['uno', 'dos', 'tres']) sembrarObjetivo(id)
  })
  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('iniciar deja el primer paso activo y con su elemento resaltado', async () => {
    const { motor } = await montar()
    await motor.iniciar('tres')
    expect(motor.activo()).toBe(true)
    expect(motor.paso()?.id).toBe('p1')
    expect(motor.elemento()?.getAttribute(ATRIBUTO)).toBe('uno')
  })

  it('un tutorial que no existe no arranca nada', async () => {
    const { motor } = await montar()
    await motor.iniciar('no-esta')
    expect(motor.activo()).toBe(false)
  })

  it('avanza y retrocede, y no retrocede más allá del primero', async () => {
    const { motor } = await montar()
    await motor.iniciar('tres')
    await motor.avanzar()
    expect(motor.indice()).toBe(1)
    await motor.retroceder()
    await motor.retroceder()
    expect(motor.indice()).toBe(0)
  })

  it('al terminar el último paso, el tutorial queda completado y se apaga', async () => {
    const { motor, progresos } = await montar()
    await motor.iniciar('tres')
    await motor.avanzar()
    await motor.avanzar()
    expect(motor.esUltimo()).toBe(true)
    await motor.avanzar()
    expect(motor.activo()).toBe(false)
    expect(progresos.de('tres')).toMatchObject({ estado: 'completado', repeticiones: 1 })
  })

  it('guarda el paso en el que va, para poder retomarlo', async () => {
    const { motor, progresos } = await montar()
    await motor.iniciar('tres')
    await motor.avanzar()
    expect(progresos.de('tres')).toMatchObject({ estado: 'en-progreso', indice: 1, pasoId: 'p2' })
  })

  it('reanuda donde quedó', async () => {
    const { motor, progresos } = await montar()
    await motor.iniciar('tres')
    await motor.avanzar()
    await motor.continuar('tres', progresos.de('tres')!)
    expect(motor.indice()).toBe(1)
  })

  it('un índice guardado fuera de rango no rompe: vuelve al último paso posible', async () => {
    const { motor, progresos } = await montar()
    await motor.iniciar('tres')
    await motor.continuar('tres', { ...progresos.de('tres')!, indice: 99 })
    expect(motor.indice()).toBe(2)
  })

  it('con acción pendiente no avanza, y dice qué falta', async () => {
    const { motor } = await montar()
    await motor.iniciar('con-accion')
    await motor.avanzar()
    expect(motor.indice()).toBe(0)
    expect(motor.problema()?.motivo).toBe('accion-pendiente')
    expect(motor.problema()?.detalle).toBe('Pulsá el botón')
  })

  it('cumplida la acción, avanza', async () => {
    const { motor } = await montar()
    await motor.iniciar('con-accion')
    motor.accionCumplida.set(true)
    expect(motor.puedeAvanzar()).toBe(true)
    await motor.avanzar()
    expect(motor.indice()).toBe(1)
  })

  it('el primer paso hereda la ruta del tutorial: abrirlo desde otra pantalla lleva a la suya', async () => {
    const { motor, router } = await montar()
    await router.navigateByUrl('/operacion')
    // `tres` declara `ruta: '/tablero'` y su primer paso no declara ninguna.
    await motor.iniciar('tres')
    expect(router.url).toBe('/tablero')
    expect(motor.problema()).toBeNull()
  })

  it('navega solo cuando el paso vive en otra pantalla', async () => {
    const { motor, router } = await montar()
    await router.navigateByUrl('/tablero')
    await motor.iniciar('viaja')
    expect(router.url).toBe('/tablero')
    await motor.avanzar()
    expect(router.url).toBe('/operacion')
  })

  it('la bitácora deja el rastro del recorrido', async () => {
    const { motor } = await montar()
    const bitacora = TestBed.inject(BITACORA_DE_TUTORIALES) as BitacoraEnMemoria
    await motor.iniciar('tres')
    await motor.avanzar()
    expect(bitacora.ultimos().map((e) => e.tipo)).toEqual(['inicio', 'paso', 'paso'])
  })
})
