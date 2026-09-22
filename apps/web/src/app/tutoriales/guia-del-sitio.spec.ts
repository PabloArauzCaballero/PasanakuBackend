import { Component, provideZonelessChangeDetection } from '@angular/core'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { beforeEach, describe, expect, it } from 'vitest'
import { ALMACEN_DE_PROGRESO } from '@aportaya/tutoriales/almacen'
import { AlmacenLocal } from '@aportaya/tutoriales/almacen-local'
import { BITACORA_DE_TUTORIALES, BitacoraEnMemoria } from '@aportaya/tutoriales/bitacora'
import { IDENTIDAD_DE_TUTORIALES, visitanteAnonimo } from '@aportaya/tutoriales/identidad'
import { MotorDeTutoriales } from '@aportaya/tutoriales/motor'
import { CARGADORES_DE_TUTORIALES, RegistroDeTutoriales, RUTAS_DEL_PRODUCTO } from '@aportaya/tutoriales/registro'
import { PantallaCentroDeAyuda } from '@aportaya/tutoriales/centro/pantalla-centro-de-ayuda'
import { CATALOGO_DEL_SITIO } from './catalogo/catalogo'
import { RUTAS_DEL_SITIO } from './proveer'

@Component({ template: 'inicio' })
class Inicio {}

/**
 * Las anclas de verdad viven en las páginas del sitio, que acá no se montan: se siembran
 * para que el motor las encuentre al instante. Sin esto cada paso espera su plazo
 * completo y la prueba vence por reloj, no por un fallo.
 */
function sembrarAnclas(): void {
  document.body.innerHTML = ''
  const objetivos = new Set(CATALOGO_DEL_SITIO.flatMap((t) => t.pasos.map((p) => p.objetivo)))
  for (const objetivo of objetivos) {
    if (objetivo === undefined) continue
    const div = document.createElement('div')
    div.setAttribute('data-tutorial-id', objetivo)
    document.body.appendChild(div)
  }
}

async function montar(): Promise<ComponentFixture<PantallaCentroDeAyuda>> {
  sembrarAnclas()
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
  const fixture = TestBed.createComponent(PantallaCentroDeAyuda)
  await TestBed.inject(RegistroDeTutoriales).cargar()
  await fixture.whenStable()
  fixture.detectChanges()
  return fixture
}

const texto = (f: ComponentFixture<unknown>): string => (f.nativeElement as HTMLElement).textContent ?? ''

describe('la guía del sitio público', () => {
  beforeEach(() => localStorage.clear())

  it('un visitante sin cuenta ve TODOS los recorridos del sitio', async () => {
    const fixture = await montar()
    for (const t of CATALOGO_DEL_SITIO) {
      expect(texto(fixture), `falta ${t.id}`).toContain(t.titulo)
    }
  })

  it('el avance arranca en cero y se cuenta sobre lo disponible', async () => {
    const fixture = await montar()
    expect(texto(fixture)).toContain(`0 de ${CATALOGO_DEL_SITIO.length} tutoriales completados`)
  })

  it('recomienda por dónde empezar', async () => {
    const fixture = await montar()
    expect(texto(fixture)).toContain('Recomendado para vos')
    expect(texto(fixture)).toContain('Qué es AportaYa')
  })

  it('el catálogo del sitio no reporta problemas en pantalla', async () => {
    const fixture = await montar()
    expect(texto(fixture)).not.toContain('Problemas en el catálogo')
  })

  it('comenzar enciende el motor sobre el sitio de verdad', async () => {
    const fixture = await montar()
    const motor = TestBed.inject(MotorDeTutoriales)
    await motor.iniciar('sitio-que-es')
    await fixture.whenStable()
    expect(motor.activo()).toBe(true)
    expect(motor.paso()?.titulo).toBe('Un pasanaku, pero sin cuaderno')
  })

  it('el avance del visitante se guarda en este navegador', async () => {
    await montar()
    const motor = TestBed.inject(MotorDeTutoriales)
    await motor.iniciar('sitio-que-es')
    await motor.avanzar()
    expect(Object.keys(localStorage).some((c) => c.startsWith('aportaya.tutoriales.'))).toBe(true)
  })
})
