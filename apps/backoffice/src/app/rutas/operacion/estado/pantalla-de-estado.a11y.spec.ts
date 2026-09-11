import { Component, provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideHttpClient } from '@angular/common/http'
import { provideHttpClientTesting } from '@angular/common/http/testing'
import { provideRouter } from '@angular/router'
import { RouterTestingHarness } from '@angular/router/testing'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { Sesion } from '../../../nucleo/sesion'
import { GATEWAY } from '../../../nucleo/gateway'
import { PantallaDeEstado } from './pantalla-de-estado'

@Component({ selector: 'ap-anfitrion', imports: [PantallaDeEstado], template: `<ap-pantalla-de-estado />` })
class Anfitrion {}

describe('PantallaDeEstado · accesibilidad', () => {
  it('sin violaciones serias, con la tabla virtualizada y los filtros ya renderizados', async () => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: '**', component: Anfitrion }]),
        { provide: GATEWAY, useValue: 'http://gw/api/v1' },
      ],
    })
    TestBed.inject(Sesion).abrir('t', ['exportar:estado-plataforma'], 'oficial')
    const arnes = await RouterTestingHarness.create('/')
    await arnes.fixture.whenStable()
    expect(await axe(arnes.routeNativeElement as HTMLElement)).toHaveNoViolations()
  })
})
