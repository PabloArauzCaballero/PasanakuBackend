import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { beforeEach, describe, expect, it } from 'vitest'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import { GATEWAY } from '../../../nucleo/gateway'
import { idempotenciaInterceptor } from '../../../nucleo/idempotencia.interceptor'
import { Sesion } from '../../../nucleo/sesion'
import type { DefinicionDeReporteEnLista } from '../dominio/cu58-reportes'
import { FormularioDeEjecucion } from './formulario-de-ejecucion'
import { PantallaDeReportes } from './pantalla-de-reportes'

/** **Prueba de UI contra HTTP simulado** (regla 65): ejercita la pantalla y su cliente, NO la integración con `auditoria`. Sintéticos. */
const GW = 'http://gw/api/v1'
const ID = 'd-1'
const URL = `${GW}/reportes/${ID}/ejecuciones`
const CONCILIACION: DefinicionDeReporteEnLista = {
  definicionId: ID,
  tipo: 'CONCILIACION_DIARIA',
  nombre: 'Conciliación sintética',
  descripcion: 'Sintético',
  permisoRequerido: 'AUDITORIA_LEER',
  contieneDatosSensibles: false,
  parametrosEsperados: [
    { nombre: 'fecha', tipo: 'FECHA', obligatorio: true },
    { nombre: 'grupo_id', tipo: 'UUID', obligatorio: false },
  ],
}
const SALIDA = { ejecucionId: 'x-1', estado: 'COMPLETADA', filasGeneradas: 0, hashResultado: 'a'.repeat(64) }

describe('FormularioDeEjecucion · CU-58 [UI contra HTTP simulado]', () => {
  let http: HttpTestingController

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideHttpClient(withInterceptors([idempotenciaInterceptor, erroresInterceptor])),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: GW },
      ],
    })
    http = TestBed.inject(HttpTestingController)
  })

  function montar(definicion: DefinicionDeReporteEnLista): ComponentFixture<FormularioDeEjecucion> {
    const f = TestBed.createComponent(FormularioDeEjecucion)
    f.componentRef.setInput('definicion', definicion)
    f.detectChanges()
    return f
  }
  const c = (f: ComponentFixture<FormularioDeEjecucion>) =>
    f.componentInstance as unknown as { poner(n: string, v: string): void; enviar(): void; justificacion: { set(v: string): void }; formato: { set(v: string): void }; errores(): Record<string, string>; errorDeJustificacion(): string }
  const texto = (f: ComponentFixture<unknown>) => (f.nativeElement as HTMLElement).textContent ?? ''

  it('correcto: un campo por parámetro; manda solo lo declarado, con clave, y muestra filas (cero es respuesta) y huella', async () => {
    const f = montar(CONCILIACION)
    expect((f.nativeElement as HTMLElement).querySelector('input[type="date"]')).not.toBeNull()
    c(f).poner('fecha', '2026-10-28')
    c(f).formato.set('CSV')
    c(f).enviar()
    const pedido = http.expectOne(URL)
    expect(pedido.request.method).toBe('POST')
    expect(pedido.request.body).toEqual({ parametros: { fecha: '2026-10-28' }, formato: 'CSV' })
    expect(pedido.request.headers.get('Idempotency-Key')).toBeTruthy()
    pedido.flush(SALIDA)
    await f.whenStable()
    f.detectChanges()
    expect(texto(f)).toContain('sin filas')
    expect(texto(f)).toContain('a'.repeat(64))
  })

  it('límite: el opcional puede ir vacío, el obligatorio no; sin parámetros declarados lo dice', async () => {
    const f = montar(CONCILIACION)
    c(f).enviar()
    expect(c(f).errores()['fecha']).toContain('Completá')
    expect(c(f).errores()['grupo_id']).toBeUndefined()
    http.expectNone(URL)
    const g = montar({ ...CONCILIACION, parametrosEsperados: [] })
    expect(texto(g)).toContain('no pide parámetros')
  })

  it('límite: con datos sensibles exige justificación de 10 a 300 y la manda', async () => {
    const f = montar({ ...CONCILIACION, contieneDatosSensibles: true, parametrosEsperados: [] })
    c(f).justificacion.set('corta')
    c(f).enviar()
    expect(c(f).errorDeJustificacion()).toContain('al menos 10')
    c(f).justificacion.set('x'.repeat(301))
    c(f).enviar()
    expect(c(f).errorDeJustificacion()).toContain('hasta 300')
    http.expectNone(URL)
    c(f).justificacion.set('Auditoría mensual de cierre.')
    c(f).enviar()
    expect(http.expectOne(URL).request.body).toEqual({ parametros: {}, justificacion: 'Auditoría mensual de cierre.' })
  })

  it('inválido: si el servidor lo rechaza (sin permiso o parámetro inválido) lo explica sin el texto crudo', async () => {
    const f = montar(CONCILIACION)
    c(f).poner('fecha', '2026-10-28')
    c(f).enviar()
    http.expectOne(URL).flush({ codigo: 'AP-SEG-01', mensaje: 'crudo del backend', trazaId: 't' }, { status: 403, statusText: 'x' })
    await f.whenStable()
    f.detectChanges()
    expect(texto(f)).not.toContain('crudo del backend')
    expect((f.nativeElement as HTMLElement).querySelector('ap-alerta')).not.toBeNull()
  })

  it('pantalla: «Ejecutar» solo en los reportes que el permiso de la sesión alcanza; en el resto dice cuál falta', async () => {
    TestBed.inject(Sesion).abrir('t', ['AUDITORIA_LEER'], 'oficial', 'op-1')
    const f = TestBed.createComponent(PantallaDeReportes)
    f.detectChanges()
    await f.whenStable()
    http.expectOne((r) => r.url === `${GW}/reportes`).flush({ items: [CONCILIACION, { ...CONCILIACION, definicionId: 'd-2', nombre: 'De billetera', permisoRequerido: 'BILLETERA_VER' }] })
    http.expectOne((r) => r.url === `${GW}/reportes/ejecuciones`).flush({ items: [] })
    // El mismo ritmo que `pantalla-de-reportes.spec.ts` (`pintar`): el listado por cursor resuelve en microtareas.
    for (let i = 0; i < 2; i++) {
      await new Promise<void>((r) => queueMicrotask(r))
      await Promise.resolve()
      await f.whenStable()
      f.detectChanges()
    }
    const botones = Array.from((f.nativeElement as HTMLElement).querySelectorAll('button')).filter((b) => b.textContent?.trim() === 'Ejecutar')
    expect(botones).toHaveLength(1)
    expect(texto(f)).toContain('Requiere BILLETERA_VER')
  })
})
