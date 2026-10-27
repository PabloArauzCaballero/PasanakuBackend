import { HttpTestingController } from '@angular/common/http/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import type { ActivoFijo } from '../dominio/cu105-activos'
import { configurarListado, esperar, GW, montarListado, textoDe } from '../listado/listado.apoyo'
import { PantallaDeActivos } from './pantalla-de-activos'

/**
 * H5.S1.M5 · CU-105 sobre el listado por cursor, contra `HttpTestingController` (doble declarado, regla 65). Datos
 * sintéticos con la forma de `ActivoEnLista`. Tres niveles y la acción de fila.
 */
const URL = `${GW}/erp/activos?limite=10`

const activo = (p: Partial<ActivoFijo> = {}): ActivoFijo => ({
  activoFijoId: 'a-1',
  codigo: 'AF-001',
  descripcion: 'Servidor de oficina',
  categoriaNombre: 'Equipos de computación',
  vidaUtilMeses: 36,
  fechaAdquisicion: '2026-01-15',
  estado: 'ACTIVO',
  costoAdquisicion: { monto: '3600.00', moneda: 'BOB' },
  valorResidual: { monto: '0.00', moneda: 'BOB' },
  depreciacionAcumulada: { monto: '900.00', moneda: 'BOB' },
  valorEnLibros: { monto: '2700.00', moneda: 'BOB' },
  depreciadoEnElPeriodoVigente: false,
  ...p,
})

const depreciar = (el: HTMLElement) => (el.querySelector('tbody button[aria-label^="Depreciar"]') as HTMLButtonElement | null) ?? undefined

describe('PantallaDeActivos · CU-105 por cursor', () => {
  let http: HttpTestingController
  beforeEach(() => (http = configurarListado(['CONTABILIDAD_ERP_ACTIVOS_FIJOS'])))
  const montar = (cuerpo: unknown, status?: number) => montarListado(http, PantallaDeActivos, URL, { cuerpo, status }, { periodoVigenteId: 'p-10' })

  it('aceptado · depreciar llama a la acción con el período de la URL, recarga la lista y lo dice', async () => {
    const f = await montar({ items: [activo()] })
    const boton = depreciar(f.nativeElement)!
    expect(boton.querySelector('ap-icono')).not.toBeNull()
    expect(boton.getAttribute('aria-label')).toBe('Depreciar en el período el activo AF-001')
    boton.click()
    await esperar(f)
    expect(depreciar(f.nativeElement)!.disabled).toBe(true)
    const req = http.expectOne(`${GW}/erp/activos/a-1/depreciaciones`)
    expect(req.request.body).toEqual({ periodoContableId: 'p-10' })
    req.flush({ activoFijoId: 'a-1', depreciacionId: 'd-1', monto: '100.00', valorEnLibros: '2600.00', totalmenteDepreciado: false })
    await esperar(f)
    http.expectOne(URL).flush({ items: [activo({ depreciadoEnElPeriodoVigente: true, valorEnLibros: { monto: '2600.00', moneda: 'BOB' } })] })
    await esperar(f)
    await esperar(f)
    expect(textoDe(f)).toContain('El activo AF-001 ya tiene su cuota del período')
    expect(depreciar(f.nativeElement)).toBeUndefined()
  })

  it('aceptado · la categoría filtra la página cargada y las cifras cuentan la página', async () => {
    const f = await montar({ items: [activo(), activo({ activoFijoId: 'a-2', codigo: 'AF-002', categoriaNombre: 'Vehículos', estado: 'DADO_DE_BAJA' })] })
    const kpis = Array.from(f.nativeElement.querySelectorAll('ap-tarjeta-kpi .valor') as NodeListOf<HTMLElement>).map((k) => k.textContent?.trim())
    expect(kpis).toEqual(['2', '1', '1'])
    ;(f.componentInstance as unknown as { listado: { filtrar(n: string, v: string): void } }).listado.filtrar('categoria', 'Vehículos')
    await esperar(f)
    expect(f.nativeElement.querySelectorAll('tbody tr')).toHaveLength(1)
    expect(textoDe(f)).toContain('AF-002')
  })

  it('límite · depreciadoEnElPeriodoVigente (lo dice el servidor): no ofrece depreciar, con el motivo', async () => {
    const f = await montar({ items: [activo({ depreciadoEnElPeriodoVigente: true })] })
    expect(textoDe(f)).toContain('Ya tiene cuota calculada en este período.')
    expect(depreciar(f.nativeElement)).toBeUndefined()
  })

  it('error · 403 del listado', async () => {
    const f = await montar({ mensaje: 'interno' }, 403)
    expect(f.nativeElement.querySelector('ap-estado-de-error[role="alert"]')?.textContent).toContain('No tenés acceso a esto.')
    expect(textoDe(f)).not.toContain('interno')
  })

  it('error · la depreciación rechazada se dice, no recarga y el botón vuelve a estar disponible', async () => {
    const f = await montar({ items: [activo()] })
    depreciar(f.nativeElement)!.click()
    await esperar(f)
    http.expectOne(`${GW}/erp/activos/a-1/depreciaciones`).flush({ codigo: 'AP-CU105-01', mensaje: 'interno', trazaId: 'tz-1' }, { status: 422, statusText: 'x' })
    await esperar(f)
    http.expectNone(URL)
    expect(f.nativeElement.querySelector('ap-error-de-envio')?.textContent?.trim()).not.toBe('')
    expect(depreciar(f.nativeElement)!.disabled).toBe(false)
  })
})
