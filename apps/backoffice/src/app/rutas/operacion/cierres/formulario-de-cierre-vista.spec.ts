import { provideZonelessChangeDetection } from '@angular/core'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { describe, expect, it } from 'vitest'
import { FormularioDeCierreVista } from './formulario-de-cierre-vista'

describe('FormularioDeCierreVista · [Componente Dumb]', () => {
  function montar(): ComponentFixture<FormularioDeCierreVista> {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection()],
    })
    const fixture = TestBed.createComponent(FormularioDeCierreVista)
    fixture.detectChanges()
    return fixture
  }

  const texto = (f: ComponentFixture<unknown>) => (f.nativeElement as HTMLElement).textContent ?? ''

  it('permite modificar la fecha vía two-way data binding (model)', () => {
    const f = montar()
    f.componentInstance.fecha.set('2026-10-15')
    f.detectChanges()
    expect(f.componentInstance.fecha()).toBe('2026-10-15')
  })

  it('muestra los requisitos de cierre diario', () => {
    const f = montar()
    const contenido = texto(f)
    expect(contenido).toContain('Si la custodia cuadró')
  })

  it('muestra el mensaje de error cuando errorFecha está definido', () => {
    const f = montar()
    f.componentRef.setInput('errorFecha', 'Elegí un día anterior a hoy.')
    f.detectChanges()
    expect(texto(f)).toContain('Elegí un día anterior a hoy.')
  })

  it('emite solicitarRevision al enviar el formulario', () => {
    const f = montar()
    let solicitado = false
    f.componentInstance.solicitarRevision.subscribe(() => {
      solicitado = true
    })
    const form = f.nativeElement.querySelector('form')
    form?.dispatchEvent(new Event('submit'))
    expect(solicitado).toBe(true)
  })

  it('bloquea los controles cuando deshabilitado es true', () => {
    const f = montar()
    f.componentRef.setInput('deshabilitado', true)
    f.detectChanges()
    const boton = f.nativeElement.querySelector('button[type="submit"]')
    expect(boton?.disabled).toBe(true)
  })
})
