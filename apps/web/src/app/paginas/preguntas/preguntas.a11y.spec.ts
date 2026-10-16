import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { Preguntas } from './preguntas'

describe('Preguntas · accesibilidad', () => {
  for (const tema of ['light', 'dark'] as const) {
    it(`sin violaciones en tema ${tema}`, async () => {
      document.documentElement.setAttribute('data-theme', tema)
      TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideRouter([])] })
      const fixture = TestBed.createComponent(Preguntas)
      fixture.detectChanges()
      expect(await axe(fixture.nativeElement, { rules: { 'color-contrast': { enabled: false } } })).toHaveNoViolations()
    })
  }
})
