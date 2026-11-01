import { ActivatedRoute, convertToParamMap } from '@angular/router'
import { of } from 'rxjs'
import { describe, expect, it } from 'vitest'
import { anunciante, boton, configurar, esperar, GW, montar, pagina, texto } from '../publicidad.apoyo'
import { PantallaDeLiquidacion } from './pantalla-de-liquidacion'

const CUENTA = '1c2d00f1-f282-4b27-8115-0942a0eaf6c1'
const LIQUIDAR = `${GW}/publicidad/cuentas/${CUENTA}/liquidaciones`

async function consultado(total: string) {
  const http = configurar([], [{ provide: ActivatedRoute, useValue: { queryParamMap: of(convertToParamMap({ cuenta: CUENTA, periodo: '2026-09' })) } }])
  const f = await montar(PantallaDeLiquidacion)
  http.expectOne(`${GW}/publicidad/anunciantes?limite=100`).flush(pagina([anunciante({ cuentaPublicitariaId: CUENTA })]))
  await esperar(f)
  http.expectOne(`${GW}/publicidad/cuentas/${CUENTA}/consumo?periodo=2026-09`).flush({ cuentaPublicitariaId: CUENTA, periodo: '2026-09', total })
  await esperar(f)
  return { http, f }
}

/** CU-114 · Liquidación: append-only, sin editar ni eliminar; el error ya no se pierde y la factura se muestra con su moneda. */
describe('PantallaDeLiquidacion · CU-114', () => {
  it('aceptado · liquidar manda el período, muestra la factura con su moneda y deja de ofrecer liquidar', async () => {
    const { http, f } = await consultado('320.00')
    boton(f.nativeElement.querySelector('section')!, 'Liquidar período')!.click()
    await esperar(f)
    boton(document.querySelector('ap-dialogo')!, 'Liquidar período')!.click()
    await esperar(f)
    const req = http.expectOne(LIQUIDAR)
    expect(req.request.body).toEqual({ periodo: '2026-09' })
    req.flush({ facturaPublicidadId: 'fp-1', montoTotal: '320.00', moneda: 'USD', estado: 'GENERADA' }, { status: 201, statusText: 'Created' })
    await esperar(f)
    expect(texto(f)).toContain('320.00 USD')
    expect(texto(f)).toContain('Generada (sin comprobante)')
    expect(boton(f.nativeElement.querySelector('section')!, 'Liquidar período')).toBeUndefined()
    expect(boton(f.nativeElement, /^Eliminar/)).toBeUndefined()
  })

  it('límite · consumo cero: no ofrece liquidar y lo explica', async () => {
    const { f } = await consultado('0.00')
    expect(boton(f.nativeElement.querySelector('section')!, 'Liquidar período')).toBeUndefined()
    expect(texto(f)).toContain('no hay obligación de facturar cero')
  })

  it('error · R-PUB-06 (período ya liquidado) queda en el diálogo, que sigue abierto y libre', async () => {
    const { http, f } = await consultado('320.00')
    boton(f.nativeElement.querySelector('section')!, 'Liquidar período')!.click()
    await esperar(f)
    boton(document.querySelector('ap-dialogo')!, 'Liquidar período')!.click()
    await esperar(f)
    http.expectOne(LIQUIDAR).flush({ codigo: 'AP-CU114-01', mensaje: 'El período ya fue liquidado', trazaId: 'tz-l' }, { status: 422, statusText: 'x' })
    await esperar(f)
    expect((document.querySelector('ap-dialogo dialog') as HTMLDialogElement).open).toBe(true)
    expect(document.querySelector('ap-error-de-envio')?.textContent).toContain('tz-l')
    expect(boton(document.querySelector('ap-dialogo')!, 'Liquidar período')!.disabled).toBe(false)
  })
})
