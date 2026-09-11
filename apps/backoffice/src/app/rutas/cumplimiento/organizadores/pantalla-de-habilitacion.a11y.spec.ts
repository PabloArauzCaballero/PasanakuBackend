import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { axe } from 'vitest-axe'
import { beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import { idempotenciaInterceptor } from '../../../nucleo/idempotencia.interceptor'
import { PantallaDeHabilitacionOrganizador } from './pantalla-de-habilitacion'

const ORGANIZADOR = '22222222-2222-4222-8222-222222222222'
const URL = `http://gw/api/v1/organizadores/${ORGANIZADOR}/habilitacion`

describe('PantallaDeHabilitacionOrganizador · accesibilidad', () => {
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

  it('sin violaciones serias, habilitado o no', async () => {
    const fixture = TestBed.createComponent(PantallaDeHabilitacionOrganizador)
    fixture.componentRef.setInput('organizadorId', ORGANIZADOR)
    fixture.detectChanges()
    http.expectOne(URL).flush({ habilitado: false, nivel: 'BASICO', limiteDeGrupos: 3, limiteDeMonto: '5000.00', gruposActivos: 0 })
    await fixture.whenStable()
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })
})
