import { HttpTestingController } from '@angular/common/http/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import type { ActivoFijo } from '../dominio/cu105-activos'
import { configurarListado, esperar, GW, montarListado, textoDe } from '../listado/listado.apoyo'
import { PantallaDeActivos } from './pantalla-de-activos'

/** H1.S3.M2 · CU-105 sobre el listado por cursor real. Datos sintéticos con la forma de `ActivoEnLista`. */
const URL = `${GW}/erp/activos?limite=50`

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

describe('PantallaDeActivos · CU-105 por cursor', () => {
  let http: HttpTestingController
  beforeEach(() => (http = configurarListado(['CONTABILIDAD_ERP_ACTIVOS_FIJOS'])))

  it('correcto · depreciar sigue llamando a la acción con el período de la URL', async () => {
    const f = await montarListado(http, PantallaDeActivos, URL, { cuerpo: { items: [activo()] } }, { periodoVigenteId: 'p-10' })
    const boton = [...f.nativeElement.querySelectorAll('ap-boton button')].find((b) => (b as HTMLElement).textContent?.includes('Depreciar')) as HTMLButtonElement
    boton.click()
    await esperar(f)
    const req = http.expectOne(`${GW}/erp/activos/a-1/depreciaciones`)
    expect(req.request.body).toEqual({ periodoContableId: 'p-10' })
    req.flush({ activoFijoId: 'a-1', depreciacionId: 'd-1', monto: '100.00', valorEnLibros: '2600.00', totalmenteDepreciado: false })
  })

  it('límite · depreciadoEnElPeriodoVigente (lo dice el servidor): no ofrece depreciar, con el motivo', async () => {
    const f = await montarListado(http, PantallaDeActivos, URL, { cuerpo: { items: [activo({ depreciadoEnElPeriodoVigente: true })] } }, { periodoVigenteId: 'p-10' })
    expect(textoDe(f)).toContain('Ya tiene cuota calculada en este período.')
    expect(textoDe(f)).not.toContain('Depreciar en el período')
  })

  it('inválido · 403', async () => {
    const f = await montarListado(http, PantallaDeActivos, URL, { status: 403, cuerpo: { mensaje: 'interno' } }, { periodoVigenteId: 'p-10' })
    expect(f.nativeElement.querySelector('ap-estado-de-listado [role="alert"]')?.textContent).toContain('No tenés acceso a esto.')
  })
})
