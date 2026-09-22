import { Component, provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideHttpClient } from '@angular/common/http'
import { provideHttpClientTesting } from '@angular/common/http/testing'
import { Router, provideRouter } from '@angular/router'
import { RouterTestingHarness } from '@angular/router/testing'
import { describe, expect, it } from 'vitest'
import { Sesion } from '../../../nucleo/sesion'
import { GATEWAY } from '../../../nucleo/gateway'
import { PantallaDeEstado } from './pantalla-de-estado'

@Component({ selector: 'ap-anfitrion', imports: [PantallaDeEstado], template: `<ap-pantalla-de-estado />` })
class Anfitrion {}

/**
 * `operacion/estado`: prueba que el estado de la tabla (filtro de nivel, en la URL)
 * sobrevive a "recargar" — acá, a volver a crear el arnés de ruta con la misma URL — y
 * que el botón de exportar respeta el permiso del rol (CU-58).
 */
describe('PantallaDeEstado · estado en la URL y exportación con permiso', () => {
  function montarProviders() {
    return [
      provideZonelessChangeDetection(),
      provideHttpClient(),
      provideHttpClientTesting(),
      provideRouter([{ path: '**', component: Anfitrion }]),
      { provide: GATEWAY, useValue: 'http://gw/api/v1' },
    ]
  }

  it('sin el permiso de exportación, el botón no aparece', async () => {
    TestBed.configureTestingModule({ providers: montarProviders() })
    TestBed.inject(Sesion).abrir('t', [], 'consulta')
    const arnes = await RouterTestingHarness.create('/')
    await arnes.fixture.whenStable()
    const botones = Array.from(arnes.routeNativeElement?.querySelectorAll('.cabecera button') ?? [])
    expect(botones).toHaveLength(0)
  })

  it('con el permiso, el botón de exportar aparece', async () => {
    TestBed.configureTestingModule({ providers: montarProviders() })
    TestBed.inject(Sesion).abrir('t', ['exportar:estado-plataforma'], 'oficial')
    const arnes = await RouterTestingHarness.create('/')
    await arnes.fixture.whenStable()
    expect(arnes.routeNativeElement?.querySelector('.cabecera button')?.textContent).toContain('Exportar')
  })

  it('pegar la URL con un filtro de nivel ya aplicado muestra solo esa página filtrada', async () => {
    TestBed.configureTestingModule({ providers: montarProviders() })
    TestBed.inject(Sesion).abrir('t', [], 'oficial')
    const arnes = await RouterTestingHarness.create('/?filtro=N1')
    await arnes.fixture.whenStable()
    const router = TestBed.inject(Router)
    expect(router.url).toContain('filtro=N1')
    const texto = arnes.routeNativeElement?.textContent ?? ''
    expect(texto).toContain('identidad')
    expect(texto).not.toContain('publicidad')
  })
})
