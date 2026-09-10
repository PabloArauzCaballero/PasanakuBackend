import { Component, provideZonelessChangeDetection, signal } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { describe, expect, it } from 'vitest'
import { SelectorSegmentado } from './selector-segmentado'

@Component({ imports: [SelectorSegmentado], template: `<ap-selector-segmentado etiqueta="Vista" [segmentos]="[{ valor: 'lista', texto: 'Lista' }, { valor: 'calendario', texto: 'Calendario' }]" [(elegido)]="elegido" />` })
class Anfitrion {
  readonly elegido = signal('lista')
}

describe('ap-selector-segmentado', () => {
  it('es un radiogroup: click y flechas cambian el elegido', async () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
    const fixture = TestBed.createComponent(Anfitrion)
    fixture.detectChanges()
    const botones = fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>
    expect(botones[0]!.getAttribute('aria-checked')).toBe('true')
    botones[1]!.click()
    await fixture.whenStable()
    expect(fixture.componentInstance.elegido()).toBe('calendario')
    botones[1]!.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight', bubbles: true }))
    await fixture.whenStable()
    expect(fixture.componentInstance.elegido()).toBe('lista')
  })
})
