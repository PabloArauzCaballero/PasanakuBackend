import { Router } from '@angular/router'
import { TestBed } from '@angular/core/testing'
import { describe, expect, it, vi } from 'vitest'
import { anunciante, boton, buscar, campana, configurar, elegir, esperar, GW, montar, pagina, texto } from '../publicidad.apoyo'
import { PantallaDeCampanas } from './pantalla-de-campanas'

/** H1.S3.M3 · CU-111 · `listarCampanas` por lotes; exige PUBLICIDAD_APROBAR_CAMPANA (cola de Operaciones). */
const URL = `${GW}/publicidad/campanas?limite=100&estados=EN_REVISION`
const TODOS = `${GW}/publicidad/campanas?limite=100&estados=BORRADOR,EN_REVISION,ACTIVA,PAUSADA,RECHAZADA,FINALIZADA`
const ANUNCIANTES = `${GW}/publicidad/anunciantes?limite=100`

describe('PantallaDeCampanas · CU-111', () => {
  it('aceptado · abre en «En revisión», con presupuesto y consumido como Dinero', async () => {
    const http = configurar(['PUBLICIDAD_ANUNCIANTES', 'PUBLICIDAD_APROBAR_CAMPANA'])
    const f = await montar(PantallaDeCampanas)
    http.expectOne(URL).flush(pagina([campana()]))
    await esperar(f)
    expect(texto(f)).toContain('Lanzamiento')
    expect(f.nativeElement.querySelectorAll('ap-tabla-de-datos ap-monto')).toHaveLength(2)
  })

  it('aceptado · «Todos» pide los seis estados explícitos (sin estados el contrato devuelve solo EN_REVISION)', async () => {
    const http = configurar(['PUBLICIDAD_APROBAR_CAMPANA'])
    const f = await montar(PantallaDeCampanas)
    http.expectOne(URL).flush(pagina([campana()]))
    await esperar(f)
    await elegir(f, 'Estado', '')
    http.expectOne(TODOS).flush(pagina([campana(), campana({ campanaId: 'c-2', nombre: 'Activa de verano', estado: 'ACTIVA' })]))
    await esperar(f)
    expect(texto(f)).toContain('Activa de verano')
    await buscar(f, 'verano')
    expect(texto(f)).toContain('1 de 2 campañas')
  })

  it('aceptado · «Revisar» va a la aprobación con la campaña en el estado de la navegación; solo en revisión', async () => {
    const http = configurar(['PUBLICIDAD_APROBAR_CAMPANA'])
    const f = await montar(PantallaDeCampanas)
    await elegir(f, 'Estado', '')
    http.match(() => true).forEach((r) => r.flush(pagina([campana(), campana({ campanaId: 'c-2', nombre: 'Activa', estado: 'ACTIVA' })])))
    await esperar(f)
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true)
    expect([...f.nativeElement.querySelectorAll('button')].filter((b: Element) => b.textContent?.includes('Revisar'))).toHaveLength(1)
    boton(f.nativeElement, 'Revisar Lanzamiento')!.click()
    expect(navegar).toHaveBeenCalledWith(['/publicidad/campanas', 'c-1', 'aprobacion'], { state: { campana: campana() } })
  })

  it('aceptado · crear manda cuenta elegida y un conjunto (el contrato exige minItems 1)', async () => {
    const http = configurar(['PUBLICIDAD_ANUNCIANTES'])
    const f = await montar(PantallaDeCampanas)
    boton(f.nativeElement, 'Crear campaña')!.click()
    await esperar(f)
    http.expectOne(ANUNCIANTES).flush(pagina([anunciante({ cuentaPublicitariaId: '1c2d00f1-f282-4b27-8115-0942a0eaf6c1' })]))
    await esperar(f)
    const dlg = document.querySelector('ap-formulario-de-campana') as HTMLElement
    const sel = dlg.querySelector('select') as HTMLSelectElement
    sel.value = '1c2d00f1-f282-4b27-8115-0942a0eaf6c1'
    sel.dispatchEvent(new Event('change'))
    const escribir = (i: number, v: string) => {
      const input = dlg.querySelectorAll('input')[i] as HTMLInputElement
      input.value = v
      input.dispatchEvent(new Event('input'))
    }
    ;['Lanzamiento sintético', '500', '2026-11-01', 'Conjunto A', '6e7a3f12-8b4c-4d2e-9f1a-0c5b7d9e2a41', '6e7a3f12-8b4c-4d2e-9f1a-0c5b7d9e2a42', '20', '1,50'].forEach((v, i) => escribir(i, v))
    await esperar(f)
    boton(document, 'Crear y enviar a revisión')!.click()
    await esperar(f)
    const post = http.expectOne({ method: 'POST', url: `${GW}/publicidad/campanas` })
    expect(post.request.body).toMatchObject({ cuentaPublicitariaId: '1c2d00f1-f282-4b27-8115-0942a0eaf6c1', presupuestoTotal: '500.00', moneda: 'BOB', conjuntos: [{ nombre: 'Conjunto A', presupuestoDiario: '20.00', pujaMaxima: '1.50', modeloPuja: 'CPM' }] })
    post.flush({ campanaPublicitariaId: 'c-9', estado: 'EN_REVISION', presupuestoTotal: '500.00' }, { status: 201, statusText: 'Created' })
    await esperar(f)
    expect(texto(f)).toContain('Campaña creada y enviada a revisión')
  })

  it('límite · sin PUBLICIDAD_APROBAR_CAMPANA no pide la cola (sería un 403 seguro) y dice por qué', async () => {
    const http = configurar(['PUBLICIDAD_ANUNCIANTES'])
    const f = await montar(PantallaDeCampanas)
    expect(http.match(() => true)).toHaveLength(0)
    expect(texto(f)).toContain('La lista de campañas es la cola de Operaciones')
  })

  it('límite · cola en revisión vacía ofrece «Quitar filtros» (→ todos los estados); crear sin completar no envía', async () => {
    const http = configurar(['PUBLICIDAD_ANUNCIANTES', 'PUBLICIDAD_APROBAR_CAMPANA'])
    const f = await montar(PantallaDeCampanas)
    http.expectOne(URL).flush(pagina([]))
    await esperar(f)
    boton(f.nativeElement, 'Quitar filtros')!.click()
    await esperar(f)
    http.expectOne(TODOS).flush(pagina([]))
    await esperar(f)
    expect(texto(f)).toContain('No hay campañas registradas todavía.')
    boton(f.nativeElement, 'Crear campaña')!.click()
    await esperar(f)
    http.expectOne(ANUNCIANTES).flush(pagina([]))
    boton(document, 'Crear y enviar a revisión')!.click()
    await esperar(f)
    expect(http.match({ method: 'POST' })).toHaveLength(0)
    expect(document.querySelector('ap-formulario-de-campana')?.textContent).toContain('Obligatorio.')
  })

  it('error · 422 AP-CU111-04 con el texto del dominio; 403 traducido', async () => {
    const http = configurar(['PUBLICIDAD_APROBAR_CAMPANA'])
    const f = await montar(PantallaDeCampanas)
    http.expectOne(URL).flush({ codigo: 'AP-CU111-04', mensaje: 'x', trazaId: 'tz' }, { status: 422, statusText: 'x' })
    await esperar(f)
    expect(f.nativeElement.querySelector('[role="alert"]')?.textContent).toContain('El filtro de la lista no es válido')
    boton(f.nativeElement, /Volver a intentar|Reintentar/)!.click()
    await esperar(f)
    http.expectOne(URL).flush({ mensaje: 'interno' }, { status: 403, statusText: 'x' })
    await esperar(f)
    expect(f.nativeElement.querySelector('[role="alert"]')?.textContent).toContain('No tenés acceso a esto.')
  })

  it('error · si fallan las cuentas, el selector lo dice en vez de quedar vacío', async () => {
    const http = configurar(['PUBLICIDAD_ANUNCIANTES'])
    const f = await montar(PantallaDeCampanas)
    boton(f.nativeElement, 'Crear campaña')!.click()
    await esperar(f)
    http.expectOne(ANUNCIANTES).flush({}, { status: 500, statusText: 'x' })
    await esperar(f)
    expect(document.querySelector('ap-formulario-de-campana')?.textContent).toContain('No pudimos traer las cuentas publicitarias.')
  })
})
