import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter, Router } from '@angular/router'
import { beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../nucleo/gateway'
import { Sesion } from '../nucleo/sesion'
import { rolLegible } from './dominio/cerrar-sesion'
import { ShellFinanciero } from './shell-financiero'

const GATEWAY_TEST = 'https://gateway.test/api/v1'

describe('ShellFinanciero · salir y rol legible', () => {
  let http: HttpTestingController
  let sesion: Sesion

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: GATEWAY_TEST },
      ],
    })
    http = TestBed.inject(HttpTestingController)
    sesion = TestBed.inject(Sesion)
    sesion.abrir('token', ['ver:tablero', 'ver:operacion'], 'ADMIN_PLATAFORMA')
  })

  function montar(): HTMLElement {
    const fixture = TestBed.createComponent(ShellFinanciero)
    fixture.detectChanges()
    return fixture.nativeElement as HTMLElement
  }

  it('«Salir» llama a DELETE /sesion, cierra la sesión local y vuelve al ingreso', async () => {
    const router = TestBed.inject(Router)
    const destinos: string[] = []
    router.navigateByUrl = ((url: string) => { destinos.push(url); return Promise.resolve(true) }) as Router['navigateByUrl']
    const el = montar()

    ;(Array.from(el.querySelectorAll('button')).find((b) => b.textContent?.includes('Salir')) as HTMLButtonElement).click()
    http.expectOne({ method: 'DELETE', url: `${GATEWAY_TEST}/sesion` }).flush(null, { status: 204, statusText: 'No Content' })

    expect(sesion.abierta()).toBe(false)
    expect(destinos).toEqual(['/ingreso'])
  })

  it('si el servidor falla, igual sale: quien pide salir, sale', () => {
    const router = TestBed.inject(Router)
    router.navigateByUrl = (() => Promise.resolve(true)) as Router['navigateByUrl']
    const el = montar()

    ;(Array.from(el.querySelectorAll('button')).find((b) => b.textContent?.includes('Salir')) as HTMLButtonElement).click()
    http.expectOne(`${GATEWAY_TEST}/sesion`).flush({ codigo: 'AP-SES-01' }, { status: 401, statusText: 'Unauthorized' })

    expect(sesion.abierta()).toBe(false)
  })

  it('el menú solo ofrece lo que el permiso habilita y la cabecera muestra el rol legible', () => {
    const el = montar()
    const enlaces = Array.from(el.querySelectorAll('nav a')).map((a) => a.textContent?.trim())
    expect(enlaces).toEqual(['Tablero', 'Operación', 'Ayuda']) // Ayuda es de todos
    expect(el.querySelector('[data-tutorial-id="cabecera-rol"]')?.textContent?.trim()).toBe('Administración de plataforma')
  })

  it('rolLegible convierte el código en texto para personas', () => {
    expect(rolLegible('OFICIAL_CUMPLIMIENTO')).toBe('Oficial cumplimiento')
    expect(rolLegible('COMERCIAL')).toBe('Comercial')
  })
})
