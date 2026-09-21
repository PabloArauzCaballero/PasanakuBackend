import { Component, provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { Router, provideRouter } from '@angular/router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ATRIBUTO, LocalizadorDeObjetivo } from '@aportaya/tutoriales/objetivo'
import type { PasoDeTutorial } from '@aportaya/tutoriales/tipos'
import { vigilarAccion } from '@aportaya/tutoriales/vigilar-accion'

@Component({ template: 'a' })
class A {}
@Component({ template: 'b' })
class B {}

const paso = (accion?: PasoDeTutorial['accion']): PasoDeTutorial => ({ id: 'p', titulo: 't', descripcion: 'd', objetivo: 'x', accion, esperaMs: 50 })

function contexto(elemento: HTMLElement | null): { elemento: HTMLElement | null; router: Router; localizador: LocalizadorDeObjetivo } {
  TestBed.resetTestingModule()
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideRouter([
        { path: 'a', component: A },
        { path: 'b', component: B },
      ]),
    ],
  })
  return { elemento, router: TestBed.inject(Router), localizador: TestBed.inject(LocalizadorDeObjetivo) }
}

describe('vigilarAccion', () => {
  beforeEach(() => {
    document.body.innerHTML = ''
  })

  it('sin acción declarada no hay nada que vigilar', () => {
    const cumplir = vi.fn()
    vigilarAccion(paso(), contexto(null), cumplir)
    expect(cumplir).not.toHaveBeenCalled()
  })

  it('el clic sobre el elemento cumple el paso', () => {
    const boton = document.createElement('button')
    document.body.appendChild(boton)
    const cumplir = vi.fn()
    vigilarAccion(paso({ tipo: 'clic' }), contexto(boton), cumplir)
    boton.click()
    expect(cumplir).toHaveBeenCalledOnce()
  })

  it('soltar la vigilancia deja de escuchar: un clic posterior no cuenta', () => {
    const boton = document.createElement('button')
    document.body.appendChild(boton)
    const cumplir = vi.fn()
    const soltar = vigilarAccion(paso({ tipo: 'clic' }), contexto(boton), cumplir)
    soltar()
    boton.click()
    expect(cumplir).not.toHaveBeenCalled()
  })

  it('escribir cumple recién al llegar al mínimo de caracteres', () => {
    const campo = document.createElement('input')
    document.body.appendChild(campo)
    const cumplir = vi.fn()
    vigilarAccion(paso({ tipo: 'escribir', minimo: 3 }), contexto(campo), cumplir)
    campo.value = 'ab'
    campo.dispatchEvent(new Event('input'))
    expect(cumplir).not.toHaveBeenCalled()
    campo.value = 'abc'
    campo.dispatchEvent(new Event('input'))
    expect(cumplir).toHaveBeenCalledOnce()
  })

  it('elegir cumple con cualquier valor no vacío', () => {
    const select = document.createElement('select')
    const opcion = document.createElement('option')
    opcion.value = 'uno'
    select.appendChild(opcion)
    document.body.appendChild(select)
    const cumplir = vi.fn()
    vigilarAccion(paso({ tipo: 'elegir' }), contexto(select), cumplir)
    select.value = 'uno'
    select.dispatchEvent(new Event('change'))
    expect(cumplir).toHaveBeenCalledOnce()
  })

  it('navegar cumple al llegar a la ruta, y de entrada si ya se está ahí', async () => {
    const ctx = contexto(null)
    await ctx.router.navigateByUrl('/a')
    const yaEstaba = vi.fn()
    vigilarAccion(paso({ tipo: 'navegar', ruta: '/a' }), ctx, yaEstaba)
    expect(yaEstaba).toHaveBeenCalledOnce()

    const alLlegar = vi.fn()
    vigilarAccion(paso({ tipo: 'navegar', ruta: '/b' }), ctx, alLlegar)
    expect(alLlegar).not.toHaveBeenCalled()
    await ctx.router.navigateByUrl('/b')
    expect(alLlegar).toHaveBeenCalledOnce()
  })

  it('«que aparezca» cumple cuando el elemento llega', async () => {
    const ctx = contexto(null)
    const cumplir = vi.fn()
    vigilarAccion(paso({ tipo: 'aparezca', objetivo: 'panel' }), ctx, cumplir)
    const panel = document.createElement('div')
    panel.setAttribute(ATRIBUTO, 'panel')
    document.body.appendChild(panel)
    await new Promise((r) => setTimeout(r, 60))
    expect(cumplir).toHaveBeenCalledOnce()
  })

  it('si el elemento nunca aparece, no se cumple y tampoco se rompe', async () => {
    const cumplir = vi.fn()
    vigilarAccion(paso({ tipo: 'aparezca', objetivo: 'jamas' }), contexto(null), cumplir)
    await new Promise((r) => setTimeout(r, 80))
    expect(cumplir).not.toHaveBeenCalled()
  })
})
