import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { ejemploDe } from '@aportaya/simulado'
import { beforeEach, describe, expect, it } from 'vitest'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import { GATEWAY } from '../../../nucleo/gateway'
import { idempotenciaInterceptor, olvidarIntentos } from '../../../nucleo/idempotencia.interceptor'
import type { ReclamoEnBandeja } from '../dominio/cu52-reclamos'
import { RespuestaAReclamo } from './respuesta-a-reclamo'

/** **UI contra HTTP simulado** de `responderReclamo` (CU-52 paso 5). Datos del ejemplo generado del contrato (sintéticos). */
const GW = 'http://gw/api/v1'
const BASE = (ejemploDe('cumplimiento', 'listarBandejaDeReclamos', 'ok').cuerpo as { items: ReclamoEnBandeja[] }).items[0]!
const CON_MONTO = { ...BASE, montoReclamado: '120.00' } as ReclamoEnBandeja
const URL = `${GW}/reclamos/${BASE.reclamoId}/respuesta`
const DEVOLUCION = '7b2f9c1e-4d3a-4b8e-9f10-2a6c5d8e1f03'
const SALIDA = ejemploDe('cumplimiento', 'responderReclamo', 'ok').cuerpo as object

describe('RespuestaAReclamo · [UI contra HTTP simulado] CU-52', () => {
  let http: HttpTestingController
  beforeEach(() => {
    olvidarIntentos()
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(withInterceptors([idempotenciaInterceptor, erroresInterceptor])), provideHttpClientTesting(), { provide: GATEWAY, useValue: GW }],
    })
    http = TestBed.inject(HttpTestingController)
  })

  function montar(reclamo: ReclamoEnBandeja = CON_MONTO): { f: ComponentFixture<RespuestaAReclamo>; emitido: unknown[] } {
    const f = TestBed.createComponent(RespuestaAReclamo)
    f.componentRef.setInput('reclamo', reclamo)
    const emitido: unknown[] = []
    f.componentInstance.respondido.subscribe((s) => emitido.push(s))
    f.detectChanges()
    http.expectOne(`${GW}/reclamos/${reclamo.reclamoId}`).flush(ejemploDe('cumplimiento', 'consultarReclamo', 'ok').cuerpo as object)
    f.detectChanges()
    return { f, emitido }
  }
  const llenar = (f: ComponentFixture<RespuestaAReclamo>, resultado: string | null, texto: string, devolucion = '') => {
    f.componentInstance['resultado'].set(resultado)
    f.componentInstance['texto'].set(texto)
    f.componentInstance['devolucion'].set(devolucion)
    f.detectChanges()
  }
  const texto = (f: ComponentFixture<unknown>) => (f.nativeElement as HTMLElement).textContent ?? ''

  it('correcto: FAVORABLE con monto manda resultado, respuesta y devolucionId exactos, con Idempotency-Key; bloquea mientras envía', () => {
    const { f, emitido } = montar()
    llenar(f, 'FAVORABLE', '  Se devolvió la comisión cobrada de más.  ', DEVOLUCION.toUpperCase())
    f.componentInstance['enviar']()
    f.detectChanges()
    f.componentInstance['enviar']()
    const req = http.expectOne(URL)
    expect(req.request.body).toEqual({ resultado: 'FAVORABLE', respuesta: 'Se devolvió la comisión cobrada de más.', devolucionId: DEVOLUCION })
    expect(req.request.headers.get('Idempotency-Key')).toMatch(/^[0-9a-f-]{36}$/)
    expect(f.componentInstance['enviando']()).toBe(true)
    expect((f.nativeElement as HTMLElement).querySelector('textarea')!.disabled).toBe(true)
    expect(emitido).toEqual([])
    req.flush(SALIDA)
    expect(emitido).toEqual([SALIDA])
  })

  it('límite: sin resultado ni texto, o FAVORABLE con monto sin devolución, marca cada campo y no envía', () => {
    const { f } = montar()
    llenar(f, null, '   ')
    f.componentInstance['enviar']()
    f.detectChanges()
    expect(texto(f)).toContain('Elegí el resultado del reclamo.')
    expect(texto(f)).toContain('Escribí la respuesta')
    llenar(f, 'FAVORABLE', 'ok')
    f.componentInstance['enviar']()
    f.detectChanges()
    expect(texto(f)).toContain('necesita el identificador de la devolución')
    llenar(f, 'PARCIAL', 'x'.repeat(4001))
    f.componentInstance['enviar']()
    f.detectChanges()
    expect(texto(f)).toContain('hasta 4000 caracteres')
    http.expectNone(URL)
  })

  it('límite: un resultado distinto de FAVORABLE no manda devolucionId aunque se haya escrito antes', () => {
    const { f } = montar()
    llenar(f, 'PARCIAL', 'Se reconoce una parte.', DEVOLUCION)
    f.componentInstance['enviar']()
    expect(http.expectOne(URL).request.body).toEqual({ resultado: 'PARCIAL', respuesta: 'Se reconoce una parte.' })
  })

  it('inválido: 422 AP-CU52-03 marca la devolución, conserva lo escrito y no anuncia éxito', () => {
    const { f, emitido } = montar()
    llenar(f, 'FAVORABLE', 'Texto escrito', DEVOLUCION)
    f.componentInstance['enviar']()
    http.expectOne(URL).flush({ codigo: 'AP-CU52-03', mensaje: 'crudo', trazaId: 't' }, { status: 422, statusText: 'x' })
    f.detectChanges()
    expect(texto(f)).toContain('El servidor no encontró la reparación')
    expect(f.componentInstance['texto']()).toBe('Texto escrito')
    expect(f.componentInstance['enviando']()).toBe(false)
    expect(emitido).toEqual([])
  })

  it('inválido: 403 no anuncia éxito; sin respuesta (0) queda «sin confirmar» y el reintento usa la misma clave', () => {
    const { f, emitido } = montar(BASE)
    llenar(f, 'DESISTIDO', 'La persona desistió por escrito.')
    f.componentInstance['enviar']()
    http.expectOne(URL).flush({ codigo: 'AP-GW-403', mensaje: 'x', trazaId: 't' }, { status: 403, statusText: 'x' })
    f.detectChanges()
    expect(texto(f)).toContain('No tenés acceso a esto.')
    f.componentInstance['enviar']()
    const primera = http.expectOne(URL)
    const clave = primera.request.headers.get('Idempotency-Key')
    primera.error(new ProgressEvent('error'), { status: 0 })
    f.detectChanges()
    expect(texto(f)).toContain('No pudimos confirmar si la respuesta quedó registrada')
    f.componentInstance['enviar']()
    expect(http.expectOne(URL).request.headers.get('Idempotency-Key')).toBe(clave)
    expect(emitido).toEqual([])
  })
})
