import { Component, provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { RouterTestingHarness } from '@angular/router/testing'
import { describe, expect, it } from 'vitest'
import { axe } from 'vitest-axe'
import { Sesion } from '../nucleo/sesion'
import { MenuDeUsuario } from './menu-de-usuario'
import { MenuLateral } from './menu-lateral'
import { PestanasDeSeccion } from './pestanas-de-seccion'

@Component({ imports: [MenuLateral, PestanasDeSeccion, MenuDeUsuario], template: '<ap-menu-lateral /><ap-pestanas-de-seccion /><ap-menu-de-usuario />' })
class Pagina {}

/** Menú lateral, pestañas con subpestañas y menú de usuario abierto, sin violaciones WCAG detectables por axe. */
describe('navegación del shell · accesibilidad', () => {
  it('sin violaciones en una pantalla de grupo (dos filas de pestañas) y con el menú de usuario abierto', async () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideRouter([{ path: '**', component: Pagina }])] })
    TestBed.inject(Sesion).abrir('token', ['ver:sistemas', 'RECLAMO_VER'], 'PLATAFORMA')
    const arnes = await RouterTestingHarness.create('/sistemas/outbox')
    await arnes.fixture.whenStable()
    arnes.fixture.detectChanges()
    const el = arnes.routeNativeElement as HTMLElement
    expect(el.querySelectorAll('ap-pestanas-de-seccion nav')).toHaveLength(2)
    expect(await axe(el)).toHaveNoViolations()
    ;(el.querySelector('ap-menu-de-usuario .disparador') as HTMLButtonElement).click()
    arnes.fixture.detectChanges()
    await arnes.fixture.whenStable()
    expect(el.querySelectorAll('[role="menuitem"]')).toHaveLength(2)
    expect(await axe(el)).toHaveNoViolations()
  })
})
