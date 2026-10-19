import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import type { PaginaDeBitacora } from '../dominio/bitacora'
import { PantallaBitacora } from './pantalla-bitacora'

const PAGINA_EJEMPLO: PaginaDeBitacora = {
  items: [
    {
      id: 'b17a0000-0000-4000-8000-000000000001',
      secuencia: 101,
      entidad: 'USUARIO',
      entidadId: 'e17a0000-0000-4000-8000-000000000001',
      accion: 'ACTUALIZACION',
      actorRol: 'ADMIN_PLATAFORMA',
      actorUsuarioId: 'u17a0000-0000-4000-8000-000000000001',
      origen: 'PANEL_ADMIN',
      ipOrigen: '192.168.1.1',
      agenteUsuario: 'Mozilla/5.0',
      correlationId: 'c17a0000-0000-4000-8000-000000000001',
      hashRegistro: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
      fechaHora: '2026-10-18T14:30:00Z',
      motivo: 'Actualización de datos de contacto',
      valorAnterior: { correo: 'viejo@aportaya.bo' },
      valorNuevo: { correo: 'nuevo@aportaya.bo' },
    },
    {
      id: 'b17a0000-0000-4000-8000-000000000002',
      secuencia: 102,
      entidad: 'CONFIGURACION',
      entidadId: 'e17a0000-0000-4000-8000-000000000002',
      accion: 'CAMBIO_CONFIGURACION',
      actorRol: 'SYSTEMS_ADMIN',
      origen: 'PANEL_ADMIN',
      correlationId: 'c17a0000-0000-4000-8000-000000000002',
      hashRegistro: 'f3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b866',
      fechaHora: '2026-10-18T15:00:00Z',
      motivo: 'Ajuste de límite operativo',
    },
  ],
  totalAproximado: 2,
}

describe('PantallaBitacora', () => {
  let http: HttpTestingController

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: 'http://gw/api/v1' },
      ],
    })
    http = TestBed.inject(HttpTestingController)
  })

  afterEach(() => http.verify())

  it('renderiza la bitácora con sus eventos y cabecera', async () => {
    const fixture = TestBed.createComponent(PantallaBitacora)
    fixture.detectChanges()

    http.expectOne((req) => req.url.startsWith('http://gw/api/v1/auditoria/bitacora')).flush(PAGINA_EJEMPLO)
    await fixture.whenStable()
    fixture.detectChanges()

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? ''
    expect(texto).toContain('Registro del sistema')
    expect(texto).toContain('USUARIO')
    expect(texto).toContain('ACTUALIZACION')
    expect(texto).toContain('CONFIGURACION')
    expect(texto).toContain('CAMBIO_CONFIGURACION')
    expect(texto).toContain('ADMIN_PLATAFORMA')
  })

  it('abre el cajón lateral al pulsar Ver detalle', async () => {
    const fixture = TestBed.createComponent(PantallaBitacora)
    fixture.detectChanges()

    http.expectOne((req) => req.url.startsWith('http://gw/api/v1/auditoria/bitacora')).flush(PAGINA_EJEMPLO)
    await fixture.whenStable()
    fixture.detectChanges()

    const botones = (fixture.nativeElement as HTMLElement).querySelectorAll('button')
    const botonDetalle = Array.from(botones).find((b) => b.textContent?.includes('Ver detalle'))
    expect(botonDetalle).toBeDefined()

    botonDetalle?.click()
    await fixture.whenStable()
    fixture.detectChanges()

    const cajon = (fixture.nativeElement as HTMLElement).querySelector('ap-cajon-detalle-evento')
    expect(cajon).toBeDefined()
    expect(cajon?.textContent).toContain('#101')
    expect(cajon?.textContent).toContain('Actualización de datos de contacto')
    expect(cajon?.textContent).toContain('nuevo@aportaya.bo')
  })

  it('muestra estado vacío si el servidor no devuelve registros', async () => {
    const fixture = TestBed.createComponent(PantallaBitacora)
    fixture.detectChanges()

    http.expectOne((req) => req.url.startsWith('http://gw/api/v1/auditoria/bitacora')).flush({ items: [], totalAproximado: 0 })
    await fixture.whenStable()
    fixture.detectChanges()

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? ''
    expect(texto).toContain('No se encontraron eventos de bitácora registrados')
  })
})
