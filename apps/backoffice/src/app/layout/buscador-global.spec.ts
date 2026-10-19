import { provideHttpClient } from '@angular/common/http'
import { provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { Router, provideRouter } from '@angular/router'
import { beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../nucleo/gateway'
import { Sesion } from '../nucleo/sesion'
import { BuscadorGlobal } from './buscador-global'

describe('BuscadorGlobal', () => {
  let destinos: string[]
  beforeEach(() => {
    sessionStorage.clear()
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideRouter([]), provideHttpClient(), provideHttpClientTesting(), { provide: GATEWAY, useValue: 'http://gw/api/v1' }],
    })
    TestBed.inject(Sesion).abrir('token', ['ver:tablero', 'ver:sistemas'], 'ADMIN_PLATAFORMA')
    destinos = []
    const router = TestBed.inject(Router)
    router.navigateByUrl = ((url: string) => {
      destinos.push(url)
      return Promise.resolve(true)
    }) as Router['navigateByUrl']
  })

  function montar() {
    const fixture = TestBed.createComponent(BuscadorGlobal)
    fixture.detectChanges()
    const el = fixture.nativeElement as HTMLElement
    return { fixture, el, input: el.querySelector('input') as HTMLInputElement }
  }

  function escribir(fixture: ReturnType<typeof montar>['fixture'], input: HTMLInputElement, v: string) {
    input.value = v
    input.dispatchEvent(new Event('input'))
    fixture.detectChanges()
  }

  it('correcto: combobox con lista; escribir dos letras muestra pantallas permitidas (sin acentos); Enter abre la primera', () => {
    const { fixture, el, input } = montar()
    expect(input.getAttribute('role')).toBe('combobox')
    escribir(fixture, input, 'outb')
    const opciones = Array.from(el.querySelectorAll('[role="option"]')).map((o) => o.querySelector('.texto')?.textContent)
    expect(opciones).toEqual(['Outbox y descartados'])
    input.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }))
    fixture.detectChanges()
    expect(destinos).toEqual(['/sistemas/outbox'])
    expect(JSON.parse(sessionStorage.getItem('ap-buscador:recientes') ?? '[]')).toEqual(['outb'])
  })

  it('límite: las flechas recorren en círculo y aria-activedescendant sigue al activo; Escape cierra', () => {
    const { fixture, el, input } = montar()
    escribir(fixture, input, 'sis')
    const n = el.querySelectorAll('[role="option"]').length
    expect(n).toBeGreaterThan(1)
    input.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowUp' }))
    fixture.detectChanges()
    expect(input.getAttribute('aria-activedescendant')).toBe(`${el.querySelector('ul')?.id}-${n - 1}`)
    input.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    fixture.detectChanges()
    expect(el.querySelector('ul')).toBeNull()
  })

  it('inválido: una pantalla sin permiso no aparece; sin coincidencias lo dice', () => {
    const { fixture, el, input } = montar()
    escribir(fixture, input, 'cumplim')
    expect(el.querySelectorAll('[role="option"]')).toHaveLength(0)
    expect(el.querySelector('.nada')?.textContent).toContain('Nada coincide')
  })
})
