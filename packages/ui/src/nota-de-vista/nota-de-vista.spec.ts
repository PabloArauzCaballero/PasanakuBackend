import { Component, provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import { NotaDeVista } from './nota-de-vista'

@Component({
  imports: [NotaDeVista],
  template: `<ap-nota-de-vista clave="prueba">Esta vista muestra las tablas de la plataforma.</ap-nota-de-vista>`,
})
class Anfitrion {}

describe('NotaDeVista', () => {
  beforeEach(() => {
    sessionStorage.clear()
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
  })

  function montar() {
    const fixture = TestBed.createComponent(Anfitrion)
    fixture.detectChanges()
    const el = fixture.nativeElement as HTMLElement
    return { fixture, el, boton: el.querySelector('button') as HTMLButtonElement }
  }

  it('correcto: arranca plegada, con aria-expanded=false y sin el cuerpo en el DOM', () => {
    const { el, boton } = montar()
    expect(boton.getAttribute('aria-expanded')).toBe('false')
    expect(boton.textContent).toContain('Notas de la vista')
    expect(el.querySelector('.cuerpo')).toBeNull()
  })

  it('correcto: al abrir muestra el contenido proyectado, vincula aria-controls y recuerda el estado en la sesión', () => {
    const { fixture, el, boton } = montar()
    boton.click()
    fixture.detectChanges()
    expect(boton.getAttribute('aria-expanded')).toBe('true')
    const cuerpo = el.querySelector('.cuerpo')
    expect(cuerpo?.textContent).toContain('Esta vista muestra')
    expect(boton.getAttribute('aria-controls')).toBe(cuerpo?.id)
    expect(sessionStorage.getItem('ap-nota-de-vista:prueba')).toBe('1')
  })

  it('límite: con el estado recordado como abierta, la próxima instancia arranca abierta', () => {
    sessionStorage.setItem('ap-nota-de-vista:prueba', '1')
    const { boton } = montar()
    expect(boton.getAttribute('aria-expanded')).toBe('true')
  })

  it('inválido: si el almacenamiento falla, la nota abre y cierra igual', () => {
    const original = Storage.prototype.setItem
    Storage.prototype.setItem = () => {
      throw new Error('cuota')
    }
    try {
      const { fixture, boton } = montar()
      boton.click()
      fixture.detectChanges()
      expect(boton.getAttribute('aria-expanded')).toBe('true')
    } finally {
      Storage.prototype.setItem = original
    }
  })
})
