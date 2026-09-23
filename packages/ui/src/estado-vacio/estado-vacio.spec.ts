import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { describe, expect, it } from 'vitest'
import { EstadoVacio } from './estado-vacio'

function montar(inputs: Partial<{ motivo: string; titulo: string; explicacion: string; accionPropia: string }>) {
  TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
  const fixture = TestBed.createComponent(EstadoVacio)
  fixture.componentRef.setInput('titulo', inputs.titulo ?? 'Título')
  if (inputs.motivo !== undefined) fixture.componentRef.setInput('motivo', inputs.motivo)
  if (inputs.explicacion !== undefined) fixture.componentRef.setInput('explicacion', inputs.explicacion)
  if (inputs.accionPropia !== undefined) fixture.componentRef.setInput('accionPropia', inputs.accionPropia)
  fixture.detectChanges()
  return fixture
}

describe('EstadoVacio', () => {
  it('sin explicacion: deduce el porqué del motivo (comportamiento de siempre, catálogo del organismo)', () => {
    const fixture = montar({ motivo: 'porFiltro' })
    expect(fixture.nativeElement.querySelector('.porque')?.textContent).toBe('Con estos filtros no aparece nada.')
  })

  it('con explicacion="" explícito: NO renderiza el párrafo — lo usa EstadoDePantalla para no duplicar el mensaje', () => {
    const fixture = montar({ motivo: 'porFiltro', explicacion: '' })
    expect(fixture.nativeElement.querySelector('.porque')).toBeNull()
  })

  it('con explicacion con texto: la usa tal cual, no el catálogo por motivo', () => {
    const fixture = montar({ motivo: 'porPermiso', explicacion: 'Pedile acceso a Contabilidad.' })
    expect(fixture.nativeElement.querySelector('.porque')?.textContent).toBe('Pedile acceso a Contabilidad.')
  })

  it('sinDatos sin accionPropia: no hay botón (nadie inventa una acción que el consumidor no pidió)', () => {
    const fixture = montar({})
    expect(fixture.nativeElement.querySelector('ap-boton')).toBeNull()
  })

  it('porFiltro sin accionPropia: el catálogo ya trae "Quitar los filtros"', () => {
    const fixture = montar({ motivo: 'porFiltro' })
    expect(fixture.nativeElement.querySelector('ap-boton button')?.textContent?.trim()).toBe('Quitar los filtros')
  })
})
