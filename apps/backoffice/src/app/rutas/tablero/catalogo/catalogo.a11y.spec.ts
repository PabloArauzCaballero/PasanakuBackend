import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { ApplicationRef, provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { RouterTestingHarness } from '@angular/router/testing'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { catalogo } from './catalogo-sintetico'
import { FichaDeTabla } from './ficha-de-tabla'
import { PantallaCatalogo } from './pantalla-catalogo'

describe('Catálogo de datos · accesibilidad', () => {
  function configurar() {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: '**', component: PantallaCatalogo }]),
        { provide: GATEWAY, useValue: 'http://gw/api/v1' },
      ],
    })
  }

  it('la lista, con filtros, tabla y paginación, sin violaciones serias', async () => {
    configurar()
    const arnes = await RouterTestingHarness.create('/')
    TestBed.inject(ApplicationRef).tick()
    TestBed.inject(HttpTestingController).expectOne('http://gw/api/v1/indicadores/catalogo-de-datos').flush(catalogo(25))
    await arnes.fixture.whenStable()
    arnes.fixture.detectChanges()
    expect(await axe(arnes.routeNativeElement!)).toHaveNoViolations()
  })

  it('la ficha, con narrativa, columnas y relaciones, sin violaciones serias', async () => {
    configurar()
    const fixture = TestBed.createComponent(FichaDeTabla)
    fixture.componentRef.setInput('tabla', 'obligacion_aporte')
    fixture.detectChanges()
    TestBed.inject(ApplicationRef).tick()
    TestBed.inject(HttpTestingController).expectOne('http://gw/api/v1/indicadores/catalogo-de-datos').flush(catalogo())
    await fixture.whenStable()
    fixture.detectChanges()
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })
})
