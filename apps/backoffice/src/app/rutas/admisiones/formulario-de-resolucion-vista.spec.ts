import { provideZonelessChangeDetection } from '@angular/core'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { describe, expect, it } from 'vitest'
import { FormularioDeResolucionVista } from './formulario-de-resolucion-vista'

describe('FormularioDeResolucionVista · [Componente Dumb]', () => {
  function montar(): ComponentFixture<FormularioDeResolucionVista> {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection()],
    })
    const fixture = TestBed.createComponent(FormularioDeResolucionVista)
    fixture.componentRef.setInput('opciones', [
      { valor: 'ACEPTAR', texto: 'Aceptar' },
      { valor: 'RECHAZAR', texto: 'Rechazar' },
    ])
    fixture.detectChanges()
    return fixture
  }

  const texto = (f: ComponentFixture<unknown>) => (f.nativeElement as HTMLElement).textContent ?? ''

  it('muestra mensaje de confirmación cuando envio es confirmado', () => {
    const f = montar()
    f.componentRef.setInput('envio', 'confirmado')
    f.detectChanges()
    expect(texto(f)).toContain('Resolución registrada')
    expect(f.nativeElement.querySelector('form')).toBeNull()
  })

  it('permite two-way data binding para decision y motivo', () => {
    const f = montar()
    f.componentInstance.decision.set('ACEPTAR')
    f.componentInstance.motivo.set('Cumple con todos los requisitos evaluados')
    f.detectChanges()
    expect(f.componentInstance.decision()).toBe('ACEPTAR')
    expect(f.componentInstance.motivo()).toBe('Cumple con todos los requisitos evaluados')
  })

  it('muestra errores de validación de campos', () => {
    const f = montar()
    f.componentRef.setInput('errorDecision', 'Elegí si aceptás o rechazás')
    f.componentRef.setInput('errorMotivo', 'Escribí el motivo')
    f.detectChanges()
    expect(texto(f)).toContain('Elegí si aceptás o rechazás')
    expect(texto(f)).toContain('Escribí el motivo')
  })

  it('emite evento enviar al hacer submit', () => {
    const f = montar()
    let enviado = false
    f.componentInstance.enviar.subscribe(() => {
      enviado = true
    })
    const form = f.nativeElement.querySelector('form')
    form?.dispatchEvent(new Event('submit'))
    expect(enviado).toBe(true)
  })

  it('muestra botón de verificar cuando está sinConfirmar y emite verificar', () => {
    const f = montar()
    f.componentRef.setInput('envio', 'sinConfirmar')
    f.detectChanges()
    let verificado = false
    f.componentInstance.verificar.subscribe(() => {
      verificado = true
    })
    expect(texto(f)).toContain('No pudimos confirmar')
    f.componentInstance.verificar.emit()
    expect(verificado).toBe(true)
  })
})
