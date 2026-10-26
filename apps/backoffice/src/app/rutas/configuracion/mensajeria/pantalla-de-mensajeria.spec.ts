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
// H5.S2.M2 · Doble de `HttpTestingController` (regla 65). Sintéticos con la forma de PlantillaEnLista / EnvioEnLista (notificaciones.yaml). Ninguno es un mensaje real.
const PLANTILLA = { plantillaId: 'p-1', codigo: 'PLANTILLA_EJEMPLO', evento: 'EVENTO_EJEMPLO', canal: 'WHATSAPP', categoriaProveedor: 'UTILITY', estadoAprobacion: 'APROBADA', activa: true, versionId: 'v-1', version: 2, idioma: 'es', vigenteDesde: '2026-09-01T04:00:00Z' }
const ENVIO = { envioId: 'e-1', notificacionId: 'n-1', proveedorId: 'pr-1', proveedor: 'PROVEEDOR_EJEMPLO', versionPlantillaId: 'v-1', canal: 'SMS', estado: 'FALLIDO', orden: 1, intentos: 3, maxIntentos: 3, costo: '0.0000', moneda: 'USD', codigoError: 'E-EJEMPLO', encoladoEn: '2026-10-16T12:00:00Z' }

async function montar(url = '/') {
  TestBed.configureTestingModule({
    providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting(), provideRouter([]), { provide: GATEWAY, useValue: GW }],
  })
  await TestBed.inject(Router).navigateByUrl(url)
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

async function irA(fixture: Parameters<typeof pintar>[0], raiz: HTMLElement, pestana: string) {
  ;(Array.from(raiz.querySelectorAll('[role="tab"]')).find((b) => b.textContent?.includes(pestana)) as HTMLElement).click()
  await pintar(fixture)
}

describe('PantallaDeMensajeria · plantillas y envíos de solo lectura, en pestañas', () => {
  it('aceptado: pinta plantilla y envío sin ningún dato de destinatario; ausente = «no disponible»; costo exacto con su moneda', async () => {
    const { fixture, http, raiz } = await montar()
    http.expectOne((r) => r.url === PLANTILLAS).flush({ items: [PLANTILLA] })
    await pintar(fixture)
    expect(raiz.textContent).toContain('PLANTILLA_EJEMPLO')
    expect(raiz.textContent).toContain('Aprobada')
    expect(raiz.textContent).toContain('no disponible') // vigenteHasta ausente
    await irA(fixture, raiz, 'Envíos')
    http.expectOne((r) => r.url === ENVIOS).flush({ items: [ENVIO] })
    await pintar(fixture)
    const texto = raiz.textContent ?? ''
    expect(texto).toContain('Fallido')
    expect(texto).toContain('3 / 3')
    expect(texto).toContain('0.0000 USD')
    expect(texto).toContain('no disponible') // entregadoEn ausente
    // La tabla (cabeceras y celdas) no tiene dónde mostrar a la persona ni lo que se le mandó.
    const tabla = (raiz.querySelector('table')?.textContent ?? '').toLowerCase()
    for (const prohibido of ['destinatario', 'telefono', 'teléfono', 'correo electrónico', 'contenido', 'n-1', 'pr-1']) expect(tabla).not.toContain(prohibido)
    expect(raiz.querySelector('ap-acciones-de-fila')).toBeNull()
    http.verify()
  })

  it('aceptado: canal filtra la página de envíos y el estado viaja como `estados`', async () => {
    const { fixture, http, raiz } = await montar()
    http.expectOne((r) => r.url === PLANTILLAS).flush({ items: [] })
    await irA(fixture, raiz, 'Envíos')
    http.expectOne((r) => r.url === ENVIOS).flush({ items: [ENVIO, { ...ENVIO, envioId: 'e-2', canal: 'WHATSAPP', estado: 'ENTREGADO' }] })
    await pintar(fixture)
    const tabla = fixture.debugElement.query((d) => d.name === 'ap-tabla-de-envios').componentInstance as { listado: { filtrar(n: string, v: string): void } }
    tabla.listado.filtrar('canal', 'WHATSAPP')
    await pintar(fixture)
    expect(raiz.querySelectorAll('tbody tr')).toHaveLength(1)
    tabla.listado.filtrar('estado', 'FALLIDO')
    await pintar(fixture)
    expect(http.expectOne((r) => r.url === ENVIOS).request.params.getAll('estados')).toEqual(['FALLIDO'])
  })

  it('límite: un enlace con `?filtro=EXPIRADO,INVENTADO` abre Envíos filtrado solo por el valor del contrato; vacío lo dice', async () => {
    const { fixture, http, raiz } = await montar('/?filtro=EXPIRADO,INVENTADO')
    const req = http.expectOne((r) => r.url === ENVIOS)
    expect(req.request.params.getAll('estados')).toEqual(['EXPIRADO'])
    req.flush({ items: [] })
    await pintar(fixture)
    expect(raiz.querySelector('[role="tab"][aria-selected="true"]')?.textContent).toContain('Envíos')
    expect(raiz.textContent).toContain('Ningún registro de esta página coincide')
    http.verify()
  })

  it('límite: sin plantillas, el vacío lo dice', async () => {
    const { fixture, http, raiz } = await montar()
    http.expectOne((r) => r.url === PLANTILLAS).flush({ items: [] })
    await pintar(fixture)
    expect(raiz.textContent).toContain('Todavía no hay plantillas cargadas.')
  })

  it('error: 500 en plantillas y 403 en envíos → error accionable en cada pestaña, sin el crudo', async () => {
    const { fixture, http, raiz } = await montar()
    http.expectOne((r) => r.url === PLANTILLAS).flush({ mensaje: 'SQLException' }, { status: 500, statusText: 'Error' })
    await pintar(fixture)
    expect(raiz.querySelector('[role="alert"]')).not.toBeNull()
    expect(raiz.textContent).not.toContain('SQLException')
    await irA(fixture, raiz, 'Envíos')
    http.expectOne((r) => r.url === ENVIOS).flush({ codigo: 'AP-SEG-01' }, { status: 403, statusText: 'Forbidden' })
    await pintar(fixture)
    expect(raiz.querySelector('[role="alert"]')?.textContent).toContain('No tenés acceso a esto.')
    expect(raiz.querySelector('tbody')).toBeNull()
  })
})
