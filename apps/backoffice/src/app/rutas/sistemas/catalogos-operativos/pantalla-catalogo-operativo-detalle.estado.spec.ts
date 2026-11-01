import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { ActivatedRoute, provideRouter } from '@angular/router'
import { CatalogoOperativoDetalleEstadoEnum } from 'clientes/angular/auditoria'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { elegir, estabilizar, filasVisibles, listadoDe, pulsar, texto } from '../compartido/pruebas.apoyo'
import type { CatalogoOperativoDetalle, ElementoCatalogoOperativo } from '../dominio/catalogos-operativos'
import { PantallaCatalogoOperativoDetalle } from './pantalla-catalogo-operativo-detalle'

/** Datos sintéticos (regla 97.6): la misma forma que los del spec de la ficha. */
const DETALLE_EJEMPLO: CatalogoOperativoDetalle = {
  codigo: 'UMBRALES_UIF', nombre: 'Umbrales UIF', dueno: 'Cumplimiento Normativo', version: '2.0.0', totalElementos: 2, elementosActivos: 1,
  vigenteDesde: '2026-03-01', estado: CatalogoOperativoDetalleEstadoEnum.Vigente, descripcion: 'Límites normativos de debida diligencia.',
  tablaFisica: 'cumplimiento.parametro_uif', baseNormativa: 'UIF Res. Adm. 045/2024',
}
const ELEMENTOS_EJEMPLO: ElementoCatalogoOperativo[] = [
  { id: 'elem-01', catalogoCodigo: 'UMBRALES_UIF', codigo: 'PCC_01_SIMPLIFICADA', nombre: 'Debida Diligencia Simplificada', descripcion: 'Monto máximo acumulado.', activo: true, orden: 1, actualizadoEn: '2026-10-18T10:00:00Z' },
  { id: 'elem-02', catalogoCodigo: 'UMBRALES_UIF', codigo: 'ROG_LIMITE_MENSUAL', nombre: 'Límite Mensual ROG', descripcion: 'Reporte acumulado.', activo: false, orden: 2, actualizadoEn: '2026-10-18T10:00:00Z' },
]

describe('PantallaCatalogoOperativoDetalle · Activar/Desactivar desde la fila', () => {
  let http: HttpTestingController
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting(), provideRouter([]), { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'UMBRALES_UIF' } } } }, { provide: GATEWAY, useValue: 'http://gw/api/v1' }],
    })
    http = TestBed.inject(HttpTestingController)
  })
  afterEach(() => http.verify())
  async function montar() {
    const f = TestBed.createComponent(PantallaCatalogoOperativoDetalle)
    f.detectChanges()
    http.expectOne('http://gw/api/v1/auditoria/catalogos/UMBRALES_UIF').flush(DETALLE_EJEMPLO)
    f.detectChanges()
    http.expectOne('http://gw/api/v1/auditoria/catalogos/UMBRALES_UIF/elementos').flush(ELEMENTOS_EJEMPLO)
    return { f, el: await estabilizar(f) }
  }
  function motivo(d: Element, valor: string) {
    const a = d.querySelector('textarea') as HTMLTextAreaElement
    a.value = valor
    a.dispatchEvent(new Event('input'))
  }

  it('aceptado: el listado pide la colección entera, filtra por estado y Desactivar manda el PATCH y recarga', async () => {
    const { f, el } = await montar()
    elegir(el, 'Estado', 'Inactivo')
    await estabilizar(f)
    expect(filasVisibles(el)).toBe(1)
    listadoDe(f).limpiar()
    await estabilizar(f)
    pulsar(el.querySelector('tbody tr')!, 'Desactivar')
    await estabilizar(f)
    const d = el.querySelector('dialog[open]')!
    motivo(d, 'Resolución del comité de riesgos')
    await estabilizar(f)
    pulsar(d, 'Desactivar elemento')
    const req = http.expectOne('http://gw/api/v1/auditoria/catalogos/UMBRALES_UIF/elementos/elem-01')
    expect(req.request.body).toEqual({ activo: false, motivo: 'Resolución del comité de riesgos' })
    req.flush({ ...ELEMENTOS_EJEMPLO[0], activo: false })
    f.detectChanges()
    http.expectOne('http://gw/api/v1/auditoria/catalogos/UMBRALES_UIF').flush(DETALLE_EJEMPLO)
    http.expectOne('http://gw/api/v1/auditoria/catalogos/UMBRALES_UIF/elementos').flush(ELEMENTOS_EJEMPLO)
    await estabilizar(f)
    expect(el.querySelector('dialog[open]')).toBeNull()
  })

  it('error: un 403 en el PATCH deja el diálogo abierto, el motivo escrito y el botón libre; no hay Eliminar', async () => {
    const { f, el } = await montar()
    expect(Array.from(el.querySelectorAll('button')).some((b) => texto(b) === 'Eliminar' || texto(b) === 'Editar')).toBe(false)
    pulsar(el.querySelectorAll('tbody tr')[1]!, 'Activar')
    await estabilizar(f)
    const d = el.querySelector('dialog[open]')!
    motivo(d, 'Reactivación por norma nueva')
    await estabilizar(f)
    pulsar(d, 'Activar elemento')
    http.expectOne('http://gw/api/v1/auditoria/catalogos/UMBRALES_UIF/elementos/elem-02').flush({ codigo: 'X' }, { status: 403, statusText: 'Forbidden' })
    await estabilizar(f)
    expect(el.querySelector('dialog[open]')).not.toBeNull()
    expect((d.querySelector('textarea') as HTMLTextAreaElement).value).toBe('Reactivación por norma nueva')
    expect(texto(d.querySelector('ap-error-de-envio'))).toContain('No se pudo actualizar')
    expect((Array.from(d.querySelectorAll('button')).find((b) => texto(b) === 'Activar elemento') as HTMLButtonElement).disabled).toBe(false)
  })
})
