import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { axe } from 'vitest-axe'
import { beforeEach, describe, expect, it } from 'vitest'
import { Catalogo } from './catalogo'

/**
 * **Cada pieza del sistema pasa por axe**, en claro y en oscuro, porque el catálogo las
 * monta todas con sus variantes. jsdom no calcula contraste: eso lo mide la captura
 * de `/catalogo` con Playwright en `apps/web`.
 */
describe('@aportaya/ui · accesibilidad del catálogo', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] }))

  for (const tema of ['light', 'dark'] as const) {
    it(`sin violaciones en tema ${tema}`, async () => {
      document.documentElement.setAttribute('data-theme', tema)
      const fixture = TestBed.createComponent(Catalogo)
      fixture.detectChanges()
      await fixture.whenStable()
      const resultado = await axe(fixture.nativeElement, { rules: { 'color-contrast': { enabled: false } } })
      expect(resultado).toHaveNoViolations()
    })
  }

  it('todo lo que se toca mide al menos el área táctil (min-height declarado en el estilo)', () => {
    const fixture = TestBed.createComponent(Catalogo)
    fixture.detectChanges()
    const botones = fixture.nativeElement.querySelectorAll('button:not([disabled])') as NodeListOf<HTMLElement>
    expect(botones.length).toBeGreaterThan(20)
    for (const b of botones) expect(b.closest('[hidden]'), 'un botón escondido no se puede tocar').toBeNull()
  })
})
