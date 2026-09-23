import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { describe, expect, it } from 'vitest'
import { PUERTO_SERVICIOS, type EstadoServicio } from '../dominio/puertos'
import { PantallaServicios } from './pantalla-servicios'

const SERVICIO: EstadoServicio = {
  id: 's-1',
  nombre: 'nucleo-financiero',
  estado: 'operativo',
  disponibilidad30d: '99.95%',
  presupuestoErrorRestante: '68%',
  ultimaInterrupcion: '2026-07-02',
}

function montar(obtener: () => Promise<EstadoServicio[]>) {
  TestBed.configureTestingModule({
    providers: [provideZonelessChangeDetection(), { provide: PUERTO_SERVICIOS, useValue: { obtener } }],
  })
  return TestBed.createComponent(PantallaServicios)
}

describe('PantallaServicios · los cuatro estados', () => {
  it('cargando: se ve el estado de carga antes de que resuelva la promesa', () => {
    const fixture = montar(() => new Promise(() => {}))
    fixture.detectChanges()
    expect(fixture.nativeElement.querySelector('[role="status"]')).toBeTruthy()
  })

  it('datos: pinta la tabla con las filas que trae el puerto', async () => {
    const fixture = montar(() => Promise.resolve([SERVICIO]))
    fixture.detectChanges()
    await fixture.whenStable()
    fixture.detectChanges()
    expect(fixture.nativeElement.textContent).toContain('nucleo-financiero')
  })

  it('vacío: sin servicios, muestra el mensaje de vacío del dominio (nunca la tabla)', async () => {
    const fixture = montar(() => Promise.resolve([]))
    fixture.detectChanges()
    await fixture.whenStable()
    fixture.detectChanges()
    expect(fixture.nativeElement.textContent).toContain('Todavía no hay servicios reportando estado.')
  })

  it('error: con la fuente no disponible, muestra el estado de error accionable, nunca datos de ejemplo', async () => {
    const fixture = montar(() => Promise.reject({ tipo: 'fuente-no-disponible', mensaje: 'No pudimos traer «Estado de servicios».' }))
    fixture.detectChanges()
    await fixture.whenStable()
    fixture.detectChanges()
    const alerta = fixture.nativeElement.querySelector('[role="alert"]')
    expect(alerta).toBeTruthy()
    expect(fixture.nativeElement.textContent).not.toContain('99.95')
  })
})
