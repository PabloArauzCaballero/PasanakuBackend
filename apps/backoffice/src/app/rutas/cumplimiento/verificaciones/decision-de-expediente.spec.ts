import { Component, provideZonelessChangeDetection, signal } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { describe, expect, it } from 'vitest'
import type { Vencimiento } from '../dominio/cu02-expedientes'
import { DecisionDeExpediente, type Decision } from './decision-de-expediente'

/** Dos tarjetas en la misma cola, como en la pantalla real. */
@Component({
  imports: [DecisionDeExpediente],
  template: `
    <ap-decision-de-expediente id="a" verificacionId="v-a" [completo]="true" vencimiento="vigente" [error]="errorA()" (decidir)="decididas.push(['v-a', $event])" />
    <ap-decision-de-expediente id="b" verificacionId="v-b" [completo]="completoB()" [vencimiento]="vencimientoB()" (decidir)="decididas.push(['v-b', $event])" />
  `,
})
class DosTarjetas {
  readonly errorA = signal<string | undefined>(undefined)
  readonly completoB = signal(true)
  readonly vencimientoB = signal<Vencimiento>('vigente')
  readonly decididas: [string, Decision][] = []
}

async function montar() {
  TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
  const fixture = TestBed.createComponent(DosTarjetas)
  await fixture.whenStable()
  const raiz = fixture.nativeElement as HTMLElement
  const tarjeta = (id: 'a' | 'b') => raiz.querySelector<HTMLElement>(`#${id}`)!
  const motivo = (id: 'a' | 'b') => tarjeta(id).querySelector<HTMLInputElement>('input')!
  const boton = (id: 'a' | 'b', texto: string) =>
    [...tarjeta(id).querySelectorAll('button')].find((b) => b.textContent?.trim() === texto)!
  const escribir = async (id: 'a' | 'b', valor: string) => {
    motivo(id).value = valor
    motivo(id).dispatchEvent(new Event('input'))
    await fixture.whenStable()
  }
  return { fixture, raiz, motivo, boton, escribir }
}

describe('DecisionDeExpediente', () => {
  it('el motivo escrito en una tarjeta no aparece ni habilita «Rechazar» en otra', async () => {
    const { motivo, boton, escribir } = await montar()
    await escribir('a', 'Foto borrosa')

    expect(boton('a', 'Rechazar').disabled).toBe(false)
    expect(motivo('b').value).toBe('')
    expect(boton('b', 'Rechazar').disabled).toBe(true)
  })

  it('sin motivo (o solo espacios) no se puede rechazar', async () => {
    const { boton, escribir } = await montar()
    expect(boton('a', 'Rechazar').disabled).toBe(true)
    await escribir('a', '   ')
    expect(boton('a', 'Rechazar').disabled).toBe(true)
  })

  it('rechazar emite el motivo recortado; aprobar no lleva motivo', async () => {
    const { fixture, boton, escribir } = await montar()
    await escribir('a', '  Foto borrosa  ')
    boton('a', 'Rechazar').click()
    boton('a', 'Aprobar').click()

    expect(fixture.componentInstance.decididas).toEqual([
      ['v-a', { decision: 'RECHAZAR', motivo: 'Foto borrosa' }],
      ['v-a', { decision: 'APROBAR' }],
    ])
  })

  it('incompleto: Aprobar deshabilitado y explica por qué', async () => {
    const { fixture, raiz, boton } = await montar()
    fixture.componentInstance.completoB.set(false)
    await fixture.whenStable()

    expect(boton('b', 'Aprobar').disabled).toBe(true)
    expect(raiz.querySelector('#b')!.textContent).toContain('Faltan fotos')
  })

  it('documento vencido: Aprobar deshabilitado, lo dice, y se puede rechazar con motivo', async () => {
    const { fixture, raiz, boton, escribir } = await montar()
    fixture.componentInstance.vencimientoB.set('vencido')
    await fixture.whenStable()

    expect(boton('b', 'Aprobar').disabled).toBe(true)
    expect(raiz.querySelector('#b')!.textContent).toContain('El documento está vencido')
    await escribir('b', 'Documento vencido')
    boton('b', 'Rechazar').click()
    expect(fixture.componentInstance.decididas).toEqual([['v-b', { decision: 'RECHAZAR', motivo: 'Documento vencido' }]])
  })

  it('sin fecha de vencimiento: Aprobar deshabilitado y explica por qué', async () => {
    const { fixture, raiz, boton } = await montar()
    fixture.componentInstance.vencimientoB.set('sin-fecha')
    await fixture.whenStable()

    expect(boton('b', 'Aprobar').disabled).toBe(true)
    expect(raiz.querySelector('#b')!.textContent).toContain('no tiene fecha de vencimiento')
  })

  it('vigente y completo: el aviso de vencimiento no aparece', async () => {
    const { raiz, boton } = await montar()
    expect(boton('b', 'Aprobar').disabled).toBe(false)
    expect(raiz.querySelector('#b')!.textContent).not.toContain('vencimiento')
    expect(raiz.querySelector('#b')!.textContent).not.toContain('vencido')
  })

  it('un error de la decisión se anuncia en esa tarjeta', async () => {
    const { fixture, raiz } = await montar()
    fixture.componentInstance.errorA.set('No se pudo guardar la decisión.')
    await fixture.whenStable()

    expect(raiz.querySelector('#a [role="alert"]')?.textContent).toContain('No se pudo guardar la decisión.')
    expect(raiz.querySelector('#b [role="alert"]')).toBeNull()
  })
})
