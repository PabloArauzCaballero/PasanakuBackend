import { Component, provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter, Router } from '@angular/router'
import { beforeEach, describe, expect, it } from 'vitest'
import { NotaDeLaVista } from './nota-de-la-vista'

@Component({ template: '' })
class Vacia {}

describe('NotaDeLaVista · «Sobre esta pantalla»', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideRouter([{ path: 'a', component: Vacia }, { path: 'b', component: Vacia }])],
    })
  })

  async function montar() {
    await TestBed.inject(Router).navigateByUrl('/a')
    const fixture = TestBed.createComponent(NotaDeLaVista)
    fixture.componentRef.setInput('texto', 'Qué muestra esta pantalla.')
    await fixture.whenStable()
    const el = fixture.nativeElement as HTMLElement
    const boton = el.querySelector('button')!
    return { fixture, el, boton }
  }

  it('correcto: plegada de entrada; el botón tiene nombre y abre el panel con el texto', async () => {
    const { fixture, el, boton } = await montar()
    expect(boton.getAttribute('aria-label')).toBe('Sobre esta pantalla')
    expect(boton.getAttribute('aria-expanded')).toBe('false')
    expect(el.querySelector('[role="note"]')).toBeNull()
    boton.click()
    await fixture.whenStable()
    expect(boton.getAttribute('aria-expanded')).toBe('true')
    expect(el.querySelector('[role="note"]')?.textContent?.trim()).toBe('Qué muestra esta pantalla.')
  })

  it('límite: Escape la cierra', async () => {
    const { fixture, el, boton } = await montar()
    boton.click()
    await fixture.whenStable()
    el.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    await fixture.whenStable()
    expect(el.querySelector('[role="note"]')).toBeNull()
  })

  it('inválido: al navegar a otra pantalla la nota de la anterior no queda abierta', async () => {
    const { fixture, el, boton } = await montar()
    boton.click()
    await fixture.whenStable()
    await TestBed.inject(Router).navigateByUrl('/b')
    await fixture.whenStable()
    expect(el.querySelector('[role="note"]')).toBeNull()
  })
})
