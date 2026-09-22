import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { describe, expect, it } from 'vitest'
import { ChipsDeFiltro } from './chips-de-filtro'

describe('ap-chips-de-filtro', () => {
  it('elige y quita con aria-pressed, sin chip «otros»', async () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
    const fixture = TestBed.createComponent(ChipsDeFiltro)
    fixture.componentRef.setInput('filtros', [{ valor: 'aporte', texto: 'Aportes' }, { valor: 'mora', texto: 'Moras' }])
    fixture.detectChanges()
    const botones = fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>
    expect(fixture.nativeElement.textContent).not.toContain('Otros')
    botones[1]!.click()
    await fixture.whenStable()
    expect(fixture.componentInstance.elegidos()).toEqual(['mora'])
    expect(botones[1]!.getAttribute('aria-pressed')).toBe('true')
    botones[1]!.click()
    await fixture.whenStable()
    expect(fixture.componentInstance.elegidos()).toEqual([])
  })
})
