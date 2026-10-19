import { Component, provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import { CabeceraDePagina } from './cabecera-de-pagina'
import { CabeceraDeSeccion } from '../cabecera-de-seccion/cabecera-de-seccion'

@Component({
  imports: [CabeceraDePagina, CabeceraDeSeccion],
  template: `
    <ap-cabecera-de-pagina icono="baseDeDatos" eyebrow="Sistemas" titulo="Catálogo de datos" descripcion="Cada tabla con su estado.">
      <button acciones type="button">Exportar</button>
      <button acciones type="button">Nueva</button>
    </ap-cabecera-de-pagina>
    <ap-cabecera-de-seccion icono="libro" titulo="Ficha de negocio" descripcion="Qué es y quién responde." />
  `,
})
class Anfitrion {}

describe('CabeceraDePagina y CabeceraDeSeccion', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] }))

  function montar(): HTMLElement {
    const fixture = TestBed.createComponent(Anfitrion)
    fixture.detectChanges()
    return fixture.nativeElement as HTMLElement
  }

  it('correcto: eyebrow, un solo h1 con el título, descripción, ícono decorativo y las acciones proyectadas', () => {
    const el = montar()
    expect(el.querySelector('.eyebrow')?.textContent?.trim()).toBe('Sistemas')
    expect(el.querySelectorAll('h1')).toHaveLength(1)
    expect(el.querySelector('h1')?.textContent?.trim()).toBe('Catálogo de datos')
    expect(el.querySelector('.descripcion')?.textContent?.trim()).toBe('Cada tabla con su estado.')
    expect(el.querySelector('.loseta')?.getAttribute('aria-hidden')).toBe('true')
    expect(Array.from(el.querySelectorAll('.acciones button')).map((b) => b.textContent?.trim())).toEqual(['Exportar', 'Nueva'])
  })

  it('límite: sin eyebrow, descripción ni ícono no quedan nodos vacíos', () => {
    TestBed.resetTestingModule()
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
    const fixture = TestBed.createComponent(CabeceraDePagina)
    fixture.componentRef.setInput('titulo', 'Solo título')
    fixture.detectChanges()
    const el = fixture.nativeElement as HTMLElement
    expect(el.querySelector('.eyebrow')).toBeNull()
    expect(el.querySelector('.descripcion')).toBeNull()
    expect(el.querySelector('.loseta')).toBeNull()
    expect(el.querySelector('h1')?.textContent?.trim()).toBe('Solo título')
  })

  it('sección: h2 con ícono en el acento y descripción', () => {
    const el = montar()
    const h2 = el.querySelector('ap-cabecera-de-seccion h2')
    expect(h2?.textContent?.trim()).toBe('Ficha de negocio')
    expect(h2?.querySelector('ap-icono')).not.toBeNull()
    expect(el.querySelector('ap-cabecera-de-seccion p')?.textContent?.trim()).toBe('Qué es y quién responde.')
  })
})
