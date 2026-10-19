import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter, Router } from '@angular/router'
import { describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { PantallaDeMensajeria } from './pantalla-de-mensajeria'

const GW = 'http://gw/api/v1'
const PLANTILLAS = `${GW}/notificaciones/plantillas`
const ENVIOS = `${GW}/notificaciones/envios`
// Sintéticos con la forma de PlantillaEnLista / EnvioEnLista (notificaciones.yaml). Ninguno es un mensaje real.
const PLANTILLA = { plantillaId: 'p-1', codigo: 'PLANTILLA_EJEMPLO', evento: 'EVENTO_EJEMPLO', canal: 'WHATSAPP', categoriaProveedor: 'UTILITY', estadoAprobacion: 'APROBADA', activa: true, versionId: 'v-1', version: 2, idioma: 'es', vigenteDesde: '2026-09-01T04:00:00Z' }
const ENVIO = { envioId: 'e-1', notificacionId: 'n-1', proveedorId: 'pr-1', proveedor: 'PROVEEDOR_EJEMPLO', versionPlantillaId: 'v-1', canal: 'SMS', estado: 'FALLIDO', orden: 1, intentos: 3, maxIntentos: 3, costo: '0.0000', moneda: 'USD', codigoError: 'E-EJEMPLO', encoladoEn: '2026-10-16T12:00:00Z' }

async function montar() {
  TestBed.configureTestingModule({
    providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting(), provideRouter([]), { provide: GATEWAY, useValue: GW }],
  })
  const fixture = TestBed.createComponent(PantallaDeMensajeria)
  fixture.detectChanges()
  await fixture.whenStable()
  return { fixture, http: TestBed.inject(HttpTestingController), raiz: fixture.nativeElement as HTMLElement }
}

async function pintar(fixture: { whenStable: () => Promise<unknown>; detectChanges: () => void }) {
  await new Promise<void>((r) => queueMicrotask(r))
  await Promise.resolve()
  await fixture.whenStable()
  fixture.detectChanges()
}

describe('PantallaDeMensajeria · plantillas y envíos de solo lectura', () => {
  it('correcto: pinta plantilla y envío sin ningún dato de destinatario; ausente = «no disponible»', async () => {
    const { fixture, http, raiz } = await montar()
    http.expectOne((r) => r.url === PLANTILLAS).flush({ items: [PLANTILLA] })
    http.expectOne((r) => r.url === ENVIOS).flush({ items: [ENVIO] })
    await pintar(fixture)
    const texto = raiz.textContent ?? ''
    expect(texto).toContain('PLANTILLA_EJEMPLO')
    expect(texto).toContain('Aprobada')
    expect(texto).toContain('Fallido')
    expect(texto).toContain('3 / 3')
    expect(texto).toContain('0.0000 USD')
    expect(texto).toContain('no disponible') // vigenteHasta y entregadoEn ausentes
    // Las tablas (cabeceras y celdas) no tienen dónde mostrar a la persona ni lo que se le mandó.
    const tablas = Array.from(raiz.querySelectorAll('ap-tabla-de-datos-virtualizada')).map((t) => t.textContent ?? '').join(' ').toLowerCase()
    expect(tablas).toContain('plantilla_ejemplo')
    for (const prohibido of ['destinatario', 'telefono', 'teléfono', 'correo electrónico', 'contenido', 'n-1', 'pr-1']) expect(tablas).not.toContain(prohibido)
    http.verify()
  })

  it('límite: el filtro de estado de la URL viaja como `estados` solo a envíos; vacío lo dice', async () => {
    const { fixture, http, raiz } = await montar()
    http.expectOne((r) => r.url === PLANTILLAS).flush({ items: [] })
    http.expectOne((r) => r.url === ENVIOS).flush({ items: [ENVIO] })
    await pintar(fixture)
    await TestBed.inject(Router).navigate([], { queryParams: { filtro: 'EXPIRADO,INVENTADO' } })
    await pintar(fixture)
    const req = http.expectOne((r) => r.url === ENVIOS)
    expect(req.request.params.getAll('estados')).toEqual(['EXPIRADO'])
    req.flush({ items: [] })
    await pintar(fixture)
    expect(raiz.textContent).toContain('Todavía no hay plantillas cargadas.')
    expect(raiz.textContent).toContain('Nada coincide con ese filtro.')
    http.verify()
  })

  it('inválido: 403 en envíos y 500 en plantillas → cada tabla en error, sin filas', async () => {
    const { fixture, http, raiz } = await montar()
    http.expectOne((r) => r.url === PLANTILLAS).flush({ mensaje: 'SQLException' }, { status: 500, statusText: 'Error' })
    http.expectOne((r) => r.url === ENVIOS).flush({ codigo: 'AP-SEG-01' }, { status: 403, statusText: 'Forbidden' })
    await pintar(fixture)
    const alertas = Array.from(raiz.querySelectorAll('[role="alert"]')).map((a) => a.textContent ?? '')
    expect(alertas).toHaveLength(2)
    expect(alertas.join(' ')).toContain('No tenés acceso a esto.')
    expect(raiz.textContent).not.toContain('SQLException')
    expect(raiz.querySelector('ap-tabla-de-datos-virtualizada')).toBeNull()
  })
})
