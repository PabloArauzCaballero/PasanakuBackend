import { HttpTestingController } from '@angular/common/http/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import { configurar, esperar, mes, montar, periodos, textosDeBotones, urlPeriodos, vigente } from './periodo.apoyo'

const HOY = vigente(1)

/**
 * **El gate propio del carril B3, punto 1.** Un período cerrado se ve cerrado: la pantalla NO ofrece la acción de
 * asentar en él, y el motivo queda escrito a la vista. Es la prueba que exige la ficha `F13` de `planes/18`. Desde
 * L3 los períodos llegan del ejercicio del período de HOY (`GET /erp/periodos/vigente`), no de un identificador fijo.
 */
describe('PantallaDePeriodo · un período CERRADO no ofrece asentar', () => {
  let http: HttpTestingController
  beforeEach(() => (http = configurar()))

  it('sobre un período cerrado no hay botón de asentar, y el motivo está a la vista', async () => {
    const fixture = await montar(http, { vigente: HOY, periodos: periodos('e-2026', [mes(1, 'CERRADO')]) })
    expect(textosDeBotones(fixture).some((texto) => texto.includes('Asentar'))).toBe(false)
    expect(fixture.nativeElement.textContent).toContain('Período cerrado: no se puede asentar en él')
  })

  it('sobre un período abierto se dice que admite asientos, sin un botón «Asentar» que no tiene operación detrás', async () => {
    const fixture = await montar(http, { vigente: HOY, periodos: periodos('e-2026', [mes(1, 'ABIERTO')]) })
    expect(fixture.nativeElement.textContent).toContain('Admite asientos')
    expect(textosDeBotones(fixture).some((texto) => texto.includes('Asentar'))).toBe(false)
  })

  it('con un mes cerrado y el siguiente abierto, solo el abierto admite asientos', async () => {
    const fixture = await montar(http, { vigente: HOY, periodos: periodos('e-2026', [mes(1, 'CERRADO'), mes(2, 'ABIERTO')]) })
    expect(fixture.nativeElement.querySelectorAll('.admite')).toHaveLength(1)
    expect(fixture.nativeElement.textContent).toContain('Período cerrado: no se puede asentar en él')
  })

  it('solo el período más antiguo abierto ofrece cerrarse: los períodos se cierran en orden', async () => {
    const fixture = await montar(http, { vigente: HOY, periodos: periodos('e-2026', [mes(3, 'ABIERTO'), mes(2, 'ABIERTO')]) })
    expect(textosDeBotones(fixture).filter((texto) => texto.includes('Cerrar el período'))).toHaveLength(1)
  })

  it('sin CONTABILIDAD_ERP_CERRAR no aparece el botón de cerrar, aunque el mes sea el más antiguo', async () => {
    const fixture = await montar(http, { permisos: [], vigente: HOY, periodos: periodos('e-2026', [mes(1, 'ABIERTO')]) })
    expect(textosDeBotones(fixture).some((texto) => texto.includes('Cerrar el período'))).toBe(false)
  })
})

describe('PantallaDePeriodo · totales sellados del cierre', () => {
  let http: HttpTestingController
  beforeEach(() => (http = configurar()))

  it('límite · período abierto con totales null: lo dice, sin inventar un cero', async () => {
    const fixture = await montar(http, { vigente: HOY, periodos: periodos('e-2026', [mes(1, 'ABIERTO')]) })
    expect(fixture.nativeElement.textContent).toContain('Sin cierre todavía')
    expect(fixture.nativeElement.querySelector('ap-monto')).toBeNull()
  })

  it('cerrado y cuadrado: muestra el veredicto y no dibuja un importe con una moneda que el contrato no trae', async () => {
    const fixture = await montar(http, { vigente: HOY, periodos: periodos('e-2026', [mes(1, 'CERRADO')]) })
    expect(fixture.nativeElement.textContent).toContain('Cuadró al cierre')
    expect(fixture.nativeElement.textContent).not.toContain('Bs')
  })

  it('cerrado con descuadre sellado: se marca como incidente', async () => {
    const fixture = await montar(http, { vigente: HOY, periodos: periodos('e-2026', [mes(1, 'CERRADO', '100.00', '100.01')]) })
    expect(fixture.nativeElement.querySelector('.cuadre.descuadre')?.textContent).toContain('incidente')
  })
})

describe('PantallaDePeriodo · los cuatro estados de la lista de meses', () => {
  let http: HttpTestingController
  beforeEach(() => (http = configurar()))

  it('cargando: lo dice mientras espera los meses, sin pantalla en blanco', async () => {
    const fixture = await montar(http, { vigente: HOY })
    const estado = fixture.nativeElement.querySelector('ap-estado-de-pantalla [role="status"]') as HTMLElement
    expect(estado?.getAttribute('aria-label')).toBe('Cargando los períodos del ejercicio')
    http.expectOne(urlPeriodos('e-2026')).flush(periodos('e-2026', []))
  })

  it('vacío: un ejercicio sin períodos lo dice', async () => {
    const fixture = await montar(http, { vigente: HOY, periodos: periodos('e-2026', []) })
    expect(fixture.nativeElement.textContent).toContain('todavía no tiene períodos')
  })

  it('error: mensaje humano, traza para soporte y reintento a la vista', async () => {
    const fixture = await montar(http, {
      vigente: HOY,
      periodos: { status: 401, cuerpo: { codigo: 'AP-CU100-01', mensaje: 'tecnico', trazaId: 'traza-100' } },
    })
    expect(fixture.nativeElement.textContent).toContain('Código de seguimiento: traza-100')
    expect(fixture.nativeElement.textContent).not.toContain('tecnico')
  })

  it('inválido · 422 AP-CU100-05: el ejercicio de la URL no existe y la pantalla lo dice con su texto', async () => {
    const fixture = await montar(http, {
      ejercicioId: 'e-inexistente',
      vigente: HOY,
      periodos: { status: 422, cuerpo: { codigo: 'AP-CU100-05', mensaje: 'tecnico', trazaId: 'traza-105' } },
    })
    expect(fixture.nativeElement.textContent).toContain('Ese ejercicio fiscal no existe')
    expect(fixture.nativeElement.textContent).not.toContain('tecnico')
  })

  it('con datos: los meses del ejercicio, cada uno con su estado y nombre en español', async () => {
    const fixture = await montar(http, { vigente: HOY, periodos: periodos('e-2026', [mes(1, 'CERRADO'), mes(2, 'ABIERTO')]) })
    expect(fixture.nativeElement.querySelectorAll('.mes')).toHaveLength(2)
    expect(fixture.nativeElement.textContent).toContain('enero 2026')
    expect(fixture.nativeElement.textContent).toContain('febrero 2026')
  })

  it('tras cerrar un mes recarga los períodos (la UI no afirma el cierre antes del 2xx)', async () => {
    const fixture = await montar(http, { vigente: HOY, periodos: periodos('e-2026', [mes(1, 'ABIERTO')]) })
    const cerrar = [...fixture.nativeElement.querySelectorAll('ap-boton button')].find((b) => (b as HTMLElement).textContent?.includes('Cerrar el período')) as HTMLButtonElement
    cerrar.click()
    await esperar(fixture)
    const confirmar = [...document.querySelectorAll('ap-dialogo button')].find((b) => (b as HTMLElement).textContent?.includes('Cerrar el período enero 2026')) as HTMLButtonElement
    confirmar.click()
    await esperar(fixture)
    const req = http.expectOne(`http://gw/api/v1/erp/periodos/p-1/cierre`)
    expect(req.request.body).toEqual({ glosa: 'Cierre de enero 2026' })
    expect(http.match(urlPeriodos('e-2026'))).toHaveLength(0)
    req.flush({ cierreId: 'c-1', periodoId: 'p-1', totalDebe: '1.00', totalHaber: '1.00', cerradoEn: '2026-02-01T10:00:00Z' }, { status: 201, statusText: 'Created' })
    await esperar(fixture)
    http.expectOne(urlPeriodos('e-2026')).flush(periodos('e-2026', [mes(1, 'CERRADO')]))
  })
})
