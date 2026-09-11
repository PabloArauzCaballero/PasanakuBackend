import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { describe, expect, it } from 'vitest'
import type { ActaDeComite } from '../dominio/cu94-acta'
import { PantallaDeActa } from './pantalla-de-acta'

function montar(acta: ActaDeComite) {
  TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
  const fixture = TestBed.createComponent(PantallaDeActa)
  fixture.componentRef.setInput('acta', acta)
  fixture.detectChanges()
  return fixture
}

describe('PantallaDeActa · el acta de comité NO SE CIERRA SIN QUÓRUM', () => {
  it('intentar cerrar una acta simulada sin quórum: la interfaz lo bloquea', () => {
    const actaSinQuorum: ActaDeComite = {
      id: 'acta-1',
      comiteId: 'comite-cumplimiento',
      quorumMinimo: 3,
      composicionRequerida: ['OFICIAL_CUMPLIMIENTO', 'GERENCIA', 'DIRECTORIO'],
      asistentes: [
        { integranteId: 'u1', rol: 'OFICIAL_CUMPLIMIENTO', voto: 'A_FAVOR' },
        { integranteId: 'u2', rol: 'GERENCIA', voto: 'A_FAVOR' },
      ],
    }
    const fixture = montar(actaSinQuorum)
    const boton = fixture.nativeElement.querySelector('ap-boton button') as HTMLButtonElement
    expect(boton.disabled).toBe(true)

    boton.click()
    fixture.detectChanges()
    expect(fixture.componentInstance['cerrada']).toBe(false)
    expect(fixture.nativeElement.textContent).toContain('No hay quórum')
  })

  it('con quórum, composición completa y abstenciones con motivo: el acta se puede cerrar', () => {
    const actaCompleta: ActaDeComite = {
      id: 'acta-2',
      comiteId: 'comite-cumplimiento',
      quorumMinimo: 3,
      composicionRequerida: ['OFICIAL_CUMPLIMIENTO', 'GERENCIA', 'DIRECTORIO'],
      asistentes: [
        { integranteId: 'u1', rol: 'OFICIAL_CUMPLIMIENTO', voto: 'A_FAVOR' },
        { integranteId: 'u2', rol: 'GERENCIA', voto: 'EN_CONTRA' },
        { integranteId: 'u3', rol: 'DIRECTORIO', voto: 'ABSTENCION', motivoAbstencion: 'Es parte interesada en el asunto' },
      ],
    }
    const fixture = montar(actaCompleta)
    const boton = fixture.nativeElement.querySelector('ap-boton button') as HTMLButtonElement
    expect(boton.disabled).toBe(false)

    boton.click()
    fixture.detectChanges()
    expect(fixture.componentInstance['cerrada']).toBe(true)
  })

  it('una abstención sin motivo bloquea el cierre igual que la falta de quórum', () => {
    const actaSinMotivo: ActaDeComite = {
      id: 'acta-3',
      comiteId: 'comite-cumplimiento',
      quorumMinimo: 2,
      composicionRequerida: ['OFICIAL_CUMPLIMIENTO'],
      asistentes: [
        { integranteId: 'u1', rol: 'OFICIAL_CUMPLIMIENTO', voto: 'A_FAVOR' },
        { integranteId: 'u2', rol: 'GERENCIA', voto: 'ABSTENCION' },
      ],
    }
    const fixture = montar(actaSinMotivo)
    const boton = fixture.nativeElement.querySelector('ap-boton button') as HTMLButtonElement
    expect(boton.disabled).toBe(true)
  })
})
