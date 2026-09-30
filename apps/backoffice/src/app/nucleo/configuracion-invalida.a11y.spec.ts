import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { ConfiguracionInvalida } from './configuracion-invalida'

describe('ConfiguracionInvalida · accesibilidad', () => {
  it('sin violaciones serias', async () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
    const fixture = TestBed.createComponent(ConfiguracionInvalida)
    fixture.detectChanges()
    await fixture.whenStable()
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })

  it('no muestra ninguna URL en el texto visible', () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
    const fixture = TestBed.createComponent(ConfiguracionInvalida)
    fixture.detectChanges()
    const texto = (fixture.nativeElement as HTMLElement).textContent ?? ''
    expect(texto).not.toMatch(/https?:\/\//)
    expect(texto.toLowerCase()).not.toContain('localhost')
  })
})
