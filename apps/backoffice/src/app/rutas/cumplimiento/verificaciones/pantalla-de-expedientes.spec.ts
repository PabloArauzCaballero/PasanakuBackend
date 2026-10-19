import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import {
  ExpedienteEnRevisionEstadoEnum,
  type ExpedienteEnRevision,
} from 'clientes/angular/identidad'
import { GATEWAY } from '../../../nucleo/gateway'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import { trazaInterceptor } from '../../../nucleo/traza.interceptor'
import { PantallaDeExpedientes } from './pantalla-de-expedientes'

const URL = 'http://gw/api/v1/identidad/verificaciones'

function expediente(correo?: string): ExpedienteEnRevision {
  return {
    verificacionId: 'v-1',
    usuarioId: 'u-1',
    nombreCompleto: 'Ana Torres',
    telefono: '+59170000001',
    correo,
    documento: 'CI SC',
    estado: ExpedienteEnRevisionEstadoEnum.EnRevision,
    iniciadaEn: '2026-10-10T10:00:00Z',
    documentoVigente: true,
    fotos: [],
  }
}

describe('PantallaDeExpedientes · correo de contacto', () => {
  let http: HttpTestingController

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideHttpClient(withInterceptors([trazaInterceptor, erroresInterceptor])),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: 'http://gw/api/v1' },
      ],
    })
    http = TestBed.inject(HttpTestingController)
  })

  afterEach(() => http.verify())

  async function renderizar(correo?: string): Promise<string> {
    const fixture = TestBed.createComponent(PantallaDeExpedientes)
    fixture.detectChanges()
    http.expectOne(URL).flush([expediente(correo)])
    await fixture.whenStable()
    fixture.detectChanges()
    return (fixture.nativeElement as HTMLElement).textContent ?? ''
  }

  it('muestra el correo recibido del expediente y lo nombra en el buscador', async () => {
    const texto = await renderizar('ana@example.test')

    expect(texto).toContain('Correo: ana@example.test')
    expect(texto).toContain('Buscar por nombre, teléfono o correo')
  })

  it('explica cuando el expediente antiguo no trae correo', async () => {
    const texto = await renderizar()

    expect(texto).toContain('Correo: No registrado')
  })
})
