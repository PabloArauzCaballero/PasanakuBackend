import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import { idempotenciaInterceptor } from '../../../nucleo/idempotencia.interceptor'
import { PantallaDeHabilitacionOrganizador } from './pantalla-de-habilitacion'

const ORGANIZADOR = '22222222-2222-4222-8222-222222222222'
const URL = `http://gw/api/v1/organizadores/${ORGANIZADOR}/habilitacion`

async function montar() {
  const fixture = TestBed.createComponent(PantallaDeHabilitacionOrganizador)
  fixture.componentRef.setInput('organizadorId', ORGANIZADOR)
  fixture.detectChanges()
  return fixture
}

describe('PantallaDeHabilitacionOrganizador · CU-90 sobre el contrato real de organizador', () => {
  let http: HttpTestingController

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(withInterceptors([idempotenciaInterceptor, erroresInterceptor])),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: 'http://gw/api/v1' },
      ],
    })
    http = TestBed.inject(HttpTestingController)
  })

  it('un organizador no habilitado muestra el botón de habilitar, y habilitar vuelve a pedir el estado al servidor', async () => {
    const fixture = await montar()
    http.expectOne(URL).flush({ habilitado: false, nivel: 'BASICO', limiteDeGrupos: 3, limiteDeMonto: '5000.00', gruposActivos: 0 })
    await fixture.whenStable()

    const boton = fixture.nativeElement.querySelector('ap-boton button') as HTMLButtonElement
    expect(boton.textContent?.trim()).toBe('Habilitar tras capacitación')
    boton.click()
    fixture.detectChanges()
    await Promise.resolve()

    const peticion = http.expectOne(URL)
    expect(peticion.request.method).toBe('POST')
    expect(peticion.request.headers.get('Idempotency-Key')).toBeTruthy()
    peticion.flush({ habilitado: true, nivel: 'BASICO', limiteDeGrupos: 3, limiteDeMonto: '5000.00', gruposActivos: 0 })
    fixture.detectChanges()
    await Promise.resolve()

    // Tras habilitar, la pantalla vuelve a pedir el estado al servidor: nunca lo ajusta en memoria.
    http.expectOne(URL).flush({ habilitado: true, nivel: 'BASICO', limiteDeGrupos: 3, limiteDeMonto: '5000.00', gruposActivos: 0 })
    await fixture.whenStable()
    expect(fixture.nativeElement.textContent).toContain('Habilitado')
  }, 10000)

  it('un organizador ya habilitado no muestra el botón de habilitar', async () => {
    const fixture = await montar()
    http.expectOne(URL).flush({ habilitado: true, nivel: 'PLATA', limiteDeGrupos: 8, limiteDeMonto: '20000.00', gruposActivos: 2 })
    await fixture.whenStable()
    expect(fixture.nativeElement.querySelector('ap-boton')).toBeNull()
  })
})
