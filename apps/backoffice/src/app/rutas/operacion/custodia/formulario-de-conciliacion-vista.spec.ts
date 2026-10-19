import { provideZonelessChangeDetection } from '@angular/core'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { describe, expect, it } from 'vitest'
import { FormularioDeConciliacionVista } from './formulario-de-conciliacion-vista'

describe('FormularioDeConciliacionVista · [Componente Dumb]', () => {
  function montar(): ComponentFixture<FormularioDeConciliacionVista> {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection()],
    })
    const fixture = TestBed.createComponent(FormularioDeConciliacionVista)
    fixture.detectChanges()
    return fixture
  }

  const texto = (f: ComponentFixture<unknown>) => (f.nativeElement as HTMLElement).textContent ?? ''

  it('permite modificar valores mediante two-way data binding (models)', () => {
    const f = montar()
    f.componentInstance.cuenta.set('66d81a5c-e974-4f02-8705-39aa2504c995')
    f.componentInstance.fecha.set('2026-10-15')
    f.componentInstance.moneda.set('BOB')
    f.componentInstance.custodia.set('1250000.50')
    f.componentInstance.transito.set('0.00')
    f.detectChanges()

    expect(f.componentInstance.cuenta()).toBe('66d81a5c-e974-4f02-8705-39aa2504c995')
    expect(f.componentInstance.fecha()).toBe('2026-10-15')
    expect(f.componentInstance.moneda()).toBe('BOB')
  })

  it('muestra mensajes de error pasados como input', () => {
    const f = montar()
    f.componentRef.setInput('errores', {
      cuenta: 'Identificador incorrecto',
      fecha: 'Fecha obligatoria',
      moneda: 'Moneda requerida',
      custodia: 'Monto inválido',
      transito: 'Monto inválido',
    })
    f.detectChanges()

    const contenido = texto(f)
    expect(contenido).toContain('Identificador incorrecto')
    expect(contenido).toContain('Fecha obligatoria')
    expect(contenido).toContain('Moneda requerida')
  })

  it('muestra aviso superior cuando está definido', () => {
    const f = montar()
    f.componentRef.setInput('aviso', 'Ocurrió un error de validación previa')
    f.componentRef.setInput('tonoAviso', 'error')
    f.detectChanges()

    expect(texto(f)).toContain('Ocurrió un error de validación previa')
  })

  it('emite el evento solicitarRevision al enviar el formulario', () => {
    const f = montar()
    let solicitado = false
    f.componentInstance.solicitarRevision.subscribe(() => {
      solicitado = true
    })
    const form = f.nativeElement.querySelector('form')
    form?.dispatchEvent(new Event('submit'))
    expect(solicitado).toBe(true)
  })

  it('respeta el estado deshabilitado en controles y botón', () => {
    const f = montar()
    f.componentRef.setInput('deshabilitado', true)
    f.detectChanges()

    const boton = f.nativeElement.querySelector('button[type="submit"]')
    expect(boton?.disabled).toBe(true)
    const fieldset = f.nativeElement.querySelector('fieldset')
    expect(fieldset?.disabled).toBe(true)
  })
})
