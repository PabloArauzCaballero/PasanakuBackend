import { ActivatedRoute, convertToParamMap } from '@angular/router'
import { of } from 'rxjs'
import { describe, expect, it } from 'vitest'
import { anunciante, boton, configurar, esperar, GW, montar, pagina, texto } from '../publicidad.apoyo'
import { mesAnterior } from '../consulta-de-consumo'
import { PantallaDeDesempeno } from './pantalla-de-desempeno'

const ANUNCIANTES = `${GW}/publicidad/anunciantes?limite=100`
const CUENTA = '1c2d00f1-f282-4b27-8115-0942a0eaf6c1'
const enUrl = (q: Record<string, string>) => [{ provide: ActivatedRoute, useValue: { queryParamMap: of(convertToParamMap(q)) } }]

/** CU-113 · Desempeño: la cuenta se elige por anunciante (antes, un UUID pegado a mano) y el total no inventa moneda. */
describe('PantallaDeDesempeno · CU-113', () => {
  it('aceptado · con ?cuenta= en la URL consulta sola el mes anterior y muestra el total sin moneda inventada', async () => {
    const http = configurar([], enUrl({ cuenta: CUENTA }))
    const f = await montar(PantallaDeDesempeno)
    http.expectOne(ANUNCIANTES).flush(pagina([anunciante({ cuentaPublicitariaId: CUENTA, razonSocialFacturacion: 'Cuenta Sintética SA' })]))
    await esperar(f)
    http.expectOne(`${GW}/publicidad/cuentas/${CUENTA}/consumo?periodo=${mesAnterior()}`).flush({ cuentaPublicitariaId: CUENTA, periodo: mesAnterior(), total: '1240.50' })
    await esperar(f)
    expect(texto(f)).toContain('1240.50')
    expect(texto(f)).toContain('Cuenta Sintética SA')
    expect(texto(f)).not.toContain('Bs')
    expect(f.nativeElement.querySelector('a[href*="/publicidad/liquidacion"]')?.getAttribute('href')).toContain(`cuenta=${CUENTA}`)
  })

  it('límite · sin cuenta elegida no consulta y marca el selector', async () => {
    const http = configurar([], enUrl({}))
    const f = await montar(PantallaDeDesempeno)
    http.expectOne(ANUNCIANTES).flush(pagina([anunciante()]))
    await esperar(f)
    boton(f.nativeElement, 'Consultar desempeño')!.click()
    await esperar(f)
    expect(http.match((r) => r.url.includes('/consumo'))).toHaveLength(0)
    expect(texto(f)).toContain('Elegí una cuenta.')
  })

  it('error · el consumo que falla se dice con reintento, y fallar las cuentas se dice en el selector', async () => {
    const http = configurar([], enUrl({ cuenta: CUENTA }))
    const f = await montar(PantallaDeDesempeno)
    http.expectOne(ANUNCIANTES).flush(pagina([anunciante({ cuentaPublicitariaId: CUENTA })]))
    await esperar(f)
    http.expectOne((r) => r.url.includes('/consumo')).flush({ trazaId: 'tz-c' }, { status: 500, statusText: 'x' })
    await esperar(f)
    expect(f.nativeElement.querySelector('[role="alert"]')).not.toBeNull()
    f.destroy()
    const g = await montar(PantallaDeDesempeno)
    http.expectOne(ANUNCIANTES).flush({}, { status: 403, statusText: 'x' })
    await esperar(g)
    expect(texto(g)).toContain('No pudimos traer las cuentas')
  })
})
