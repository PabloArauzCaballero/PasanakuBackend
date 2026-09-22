import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { describe, expect, it } from 'vitest'
import { TablaDeDatos } from './tabla-de-datos'

type Fila = { id: string; grupo: string; cupos: number }

describe('ap-tabla-de-datos', () => {
  it('ordena con aria-sort y selecciona todas', async () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
    const fixture = TestBed.createComponent(TablaDeDatos<Fila>)
    fixture.componentRef.setInput('titulo', 'Grupos')
    fixture.componentRef.setInput('columnas', [{ clave: 'grupo', titulo: 'Grupo', ordenable: true }, { clave: 'cupos', titulo: 'Cupos', numerica: true }])
    fixture.componentRef.setInput('filas', [{ id: 'a', grupo: 'A', cupos: 1 }, { id: 'b', grupo: 'B', cupos: 2 }])
    fixture.componentRef.setInput('seleccionable', true)
    fixture.detectChanges()
    const th = fixture.nativeElement.querySelector('th[aria-sort]') as HTMLElement
    expect(th.getAttribute('aria-sort')).toBe('none')
    th.querySelector('button')!.click()
    await fixture.whenStable()
    expect(th.getAttribute('aria-sort')).toBe('ascending')
    th.querySelector('button')!.click()
    await fixture.whenStable()
    expect(fixture.componentInstance.orden()).toEqual({ clave: 'grupo', sentido: 'desc' })
    const todas = fixture.nativeElement.querySelector('thead input[type="checkbox"]') as HTMLInputElement
    todas.click()
    await fixture.whenStable()
    expect(fixture.componentInstance.elegidas()).toEqual(['a', 'b'])
  })
})
