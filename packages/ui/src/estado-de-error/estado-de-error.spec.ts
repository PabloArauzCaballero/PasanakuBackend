import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import { EstadoDeError } from './estado-de-error'
import { TarjetaKPI } from '../tarjeta-kpi/tarjeta-kpi'
import { BotonCopiar } from '../boton-copiar/boton-copiar'

describe('EstadoDeError, TarjetaKPI y BotonCopiar', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] }))

  it('error: es una alerta con título, código de seguimiento y Reintentar que emite', () => {
    const fixture = TestBed.createComponent(EstadoDeError)
    fixture.componentRef.setInput('mensaje', 'Probá de nuevo en un momento.')
    fixture.componentRef.setInput('trazaId', 'abc-123')
    let reintentos = 0
    fixture.componentInstance.reintentar.subscribe(() => reintentos++)
    fixture.detectChanges()
    const el = fixture.nativeElement as HTMLElement
    expect(el.getAttribute('role')).toBe('alert')
    expect(el.textContent).toContain('No se pudo cargar la información.')
    expect(el.querySelector('code')?.textContent).toBe('abc-123')
    ;(el.querySelector('ap-boton button') as HTMLButtonElement).click()
    expect(reintentos).toBe(1)
  })

  it('error dentro de otra alerta: con anunciar=false no repite role=alert', () => {
    const fixture = TestBed.createComponent(EstadoDeError)
    fixture.componentRef.setInput('anunciar', false)
    fixture.detectChanges()
    expect((fixture.nativeElement as HTMLElement).hasAttribute('role')).toBe(false)
  })

  it('error sin reintento: no hay botón (un 404 no se reintenta)', () => {
    const fixture = TestBed.createComponent(EstadoDeError)
    fixture.componentRef.setInput('conReintento', false)
    fixture.detectChanges()
    expect((fixture.nativeElement as HTMLElement).querySelector('button')).toBeNull()
  })

  it('kpi: rótulo, cifra, pista e ícono; el tono pinta la cifra', () => {
    const fixture = TestBed.createComponent(TarjetaKPI)
    fixture.componentRef.setInput('etiqueta', 'Sin revisar')
    fixture.componentRef.setInput('valor', '335')
    fixture.componentRef.setInput('pista', 'de 338 tablas')
    fixture.componentRef.setInput('icono', 'alerta')
    fixture.componentRef.setInput('tono', 'aviso')
    fixture.detectChanges()
    const el = fixture.nativeElement as HTMLElement
    expect(el.getAttribute('aria-label')).toBe('Sin revisar')
    expect(el.querySelector('.valor')?.textContent?.trim()).toBe('335')
    expect(el.querySelector('.pista')?.textContent).toBe('de 338 tablas')
    expect(el.querySelector('.loseta ap-icono')).not.toBeNull()
    expect(el.classList.contains('con-tono')).toBe(true)
  })

  it('copiar: escribe en el portapapeles, lo anuncia y vuelve a la calma; si falla, lo dice', async () => {
    const escrito: string[] = []
    Object.defineProperty(navigator, 'clipboard', { configurable: true, value: { writeText: (t: string) => { escrito.push(t); return Promise.resolve() } } })
    const fixture = TestBed.createComponent(BotonCopiar)
    fixture.componentRef.setInput('valor', () => 'a\tb')
    fixture.detectChanges()
    const el = fixture.nativeElement as HTMLElement
    ;(el.querySelector('button') as HTMLButtonElement).click()
    await fixture.whenStable()
    fixture.detectChanges()
    expect(escrito).toEqual(['a\tb'])
    expect(el.querySelector('[role="status"]')?.textContent).toBe('Copiado al portapapeles')
    Object.defineProperty(navigator, 'clipboard', { configurable: true, value: { writeText: () => Promise.reject(new Error('negado')) } })
    ;(el.querySelector('button') as HTMLButtonElement).click()
    await fixture.whenStable()
    fixture.detectChanges()
    expect(el.querySelector('[role="status"]')?.textContent).toBe('No se pudo copiar')
  })
})
