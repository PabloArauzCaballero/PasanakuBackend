import { provideHttpClient } from '@angular/common/http'
import { provideHttpClientTesting } from '@angular/common/http/testing'
import { provideLocationMocks } from '@angular/common/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { GATEWAY } from './gateway'
import { RestaurandoSesion } from './restaurando-sesion'
import { Sesion } from './sesion'

function configurar() {
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideHttpClient(),
      provideHttpClientTesting(),
      provideRouter([]),
      provideLocationMocks(),
      { provide: GATEWAY, useValue: 'https://gateway.test/api/v1' },
    ],
  })
}

describe('RestaurandoSesion · accesibilidad', () => {
  it('sin violaciones serias, restaurando', async () => {
    configurar()
    TestBed.inject(Sesion).restaurando()
    const fixture = TestBed.createComponent(RestaurandoSesion)
    fixture.detectChanges()
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })

  it('sin violaciones serias, en ERROR con el botón de reintentar', async () => {
    configurar()
    const sesion = TestBed.inject(Sesion)
    sesion.restaurando()
    sesion.fallo()
    const fixture = TestBed.createComponent(RestaurandoSesion)
    fixture.detectChanges()
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })
})
