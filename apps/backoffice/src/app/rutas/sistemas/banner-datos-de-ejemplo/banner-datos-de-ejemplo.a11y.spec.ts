import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { BannerDatosDeEjemplo } from './banner-datos-de-ejemplo'
import { ES_FUENTE_SIMULADA } from '../dominio/proveedor-fuentes'

describe('BannerDatosDeEjemplo · accesibilidad', () => {
  it('con fuente simulada: se renderiza y sin violaciones serias', async () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), { provide: ES_FUENTE_SIMULADA, useValue: true }] })
    const fixture = TestBed.createComponent(BannerDatosDeEjemplo)
    fixture.detectChanges()
    await fixture.whenStable()
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Datos de ejemplo')
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })

  it('con la fuente no disponible: no se renderiza nada', () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), { provide: ES_FUENTE_SIMULADA, useValue: false }] })
    const fixture = TestBed.createComponent(BannerDatosDeEjemplo)
    fixture.detectChanges()
    expect((fixture.nativeElement as HTMLElement).textContent?.trim()).toBe('')
  })
})
