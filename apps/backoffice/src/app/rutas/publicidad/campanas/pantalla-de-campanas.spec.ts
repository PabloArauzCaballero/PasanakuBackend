import { TestBed } from '@angular/core/testing'
import { describe, expect, it } from 'vitest'
import { configurarListado, esperar, GW, montarListado, textoDe } from '../../contabilidad/listado/listado.apoyo'
import { campana, pagina } from '../publicidad.apoyo'
import { PantallaDeCampanas } from './pantalla-de-campanas'

/** H1.S3.M3 · CU-111 · `listarCampanas` por cursor; exige PUBLICIDAD_APROBAR_CAMPANA (cola de Operaciones). */
const URL = `${GW}/publicidad/campanas?limite=50`

describe('PantallaDeCampanas · CU-111 por cursor', () => {
  it('correcto · presupuesto y consumido como Dinero; una campaña en revisión lleva a su panel de aprobación', async () => {
    const http = configurarListado(['PUBLICIDAD_ANUNCIANTES', 'PUBLICIDAD_APROBAR_CAMPANA'])
    const f = await montarListado(http, PantallaDeCampanas, URL, { cuerpo: pagina([campana(), campana({ campanaId: 'c-2', nombre: 'Activa', estado: 'ACTIVA' })]) })
    expect(textoDe(f)).toContain('Lanzamiento')
    expect(f.nativeElement.querySelectorAll('ap-monto')).toHaveLength(4)
    const enlaces = [...f.nativeElement.querySelectorAll('a')].map((a) => (a as HTMLElement).getAttribute('href'))
    expect(enlaces).toEqual(['/publicidad/campanas/c-1/aprobacion'])
  })

  it('sin PUBLICIDAD_APROBAR_CAMPANA no pide la cola (sería un 403 seguro) y dice por qué', async () => {
    const http = configurarListado(['PUBLICIDAD_ANUNCIANTES'])
    const f = TestBedCrear()
    f.detectChanges()
    await esperar(f)
    expect(http.match(() => true)).toHaveLength(0)
    expect(textoDe(f)).toContain('La lista de campañas es la cola de Operaciones')
  })

  it('límite · vacío', async () => {
    const http = configurarListado(['PUBLICIDAD_APROBAR_CAMPANA'])
    const f = await montarListado(http, PantallaDeCampanas, URL, { cuerpo: pagina([]) })
    expect(textoDe(f)).toContain('No hay campañas registradas todavía.')
  })

  it('inválido · 422 AP-CU111-04 con el texto del dominio', async () => {
    const http = configurarListado(['PUBLICIDAD_APROBAR_CAMPANA'])
    const f = await montarListado(http, PantallaDeCampanas, URL, { status: 422, cuerpo: { codigo: 'AP-CU111-04', mensaje: 'x', trazaId: 'tz' } })
    expect(f.nativeElement.querySelector('[role="alert"]')?.textContent).toContain('El filtro de la lista no es válido')
  })

  it('inválido · 403', async () => {
    const http = configurarListado(['PUBLICIDAD_APROBAR_CAMPANA'])
    const f = await montarListado(http, PantallaDeCampanas, URL, { status: 403, cuerpo: { mensaje: 'interno' } })
    expect(f.nativeElement.querySelector('[role="alert"]')?.textContent).toContain('No tenés acceso a esto.')
  })
})

function TestBedCrear() {
  return TestBed.createComponent(PantallaDeCampanas)
}
