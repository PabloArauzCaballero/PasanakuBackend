import { HttpTestingController } from '@angular/common/http/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import { configurar, ejercicio, esperar, mes, montar, periodos, URL_EJERCICIOS, urlPeriodos, vigente } from './periodo.apoyo'

/**
 * H1.S3.M1 · CU-100 · El período de HOY reemplaza al identificador de demostración. Kill-test del lote: si la pantalla
 * eligiera un ejercicio por su cuenta cuando no hay período vigente, la prueba «AP-CU100-04» falla.
 */
describe('PantallaDePeriodo · período vigente (GET /erp/periodos/vigente)', () => {
  let http: HttpTestingController
  beforeEach(() => (http = configurar()))

  it('correcto · sin ejercicio en la URL muestra el ejercicio del período de hoy y marca su mes', async () => {
    const fixture = await montar(http, {
      vigente: vigente(10, 'e-2026'),
      periodos: periodos('e-2026', [mes(9, 'CERRADO'), mes(10, 'ABIERTO')]),
    })
    const texto = fixture.nativeElement.textContent as string
    expect(texto).toContain('Hoy estás en octubre 2026 (abierto).')
    const marcados = [...fixture.nativeElement.querySelectorAll('.mes')].filter((m) => (m as HTMLElement).textContent?.includes('Hoy'))
    expect(marcados).toHaveLength(1)
    expect((marcados[0] as HTMLElement).textContent).toContain('octubre 2026')
  })

  it('límite · el período de hoy puede estar CERRADO (cerrado antes de fin de mes): se muestra igual, con su estado', async () => {
    const fixture = await montar(http, { vigente: vigente(10, 'e-2026', 'CERRADO'), periodos: periodos('e-2026', [mes(10, 'CERRADO')]) })
    expect(fixture.nativeElement.textContent).toContain('Hoy estás en octubre 2026 (cerrado).')
  })

  it('con un ejercicio elegido en la URL, se muestra ese aunque hoy caiga en otro', async () => {
    const fixture = await montar(http, { ejercicioId: 'e-2025', vigente: vigente(10, 'e-2026'), periodos: periodos('e-2025', [mes(12, 'CERRADO')]) })
    expect(http.match(urlPeriodos('e-2026'))).toHaveLength(0)
    expect(fixture.nativeElement.querySelectorAll('.mes')).toHaveLength(1)
  })

  it('inválido · 422 AP-CU100-04: lo dice y NO elige otro ejercicio (no pide ningún /periodos)', async () => {
    const fixture = await montar(http, {
      vigente: { status: 422, cuerpo: { codigo: 'AP-CU100-04', mensaje: 'tecnico', trazaId: 'traza-104' } },
      ejercicios: { items: [ejercicio(2025, 'e-2025', 'CERRADO')], proximoAntesDeAnio: null },
    })
    const texto = fixture.nativeElement.textContent as string
    expect(texto).toContain('Hoy no cae dentro de ningún período contable')
    expect(texto).toContain('Elegí un ejercicio de la lista')
    expect(texto).not.toContain('tecnico')
    expect(http.match((r) => r.url.includes('/periodos') && !r.url.endsWith('/vigente'))).toHaveLength(0)
    expect(fixture.nativeElement.querySelectorAll('.mes')).toHaveLength(0)
    expect(fixture.nativeElement.querySelector('ap-selector-de-ejercicios a')?.getAttribute('href')).toBe('/contabilidad/periodo/e-2025')
  })

  it('inválido · 403 en el período de hoy: mensaje traducido con traza, sin pedir períodos', async () => {
    const fixture = await montar(http, { vigente: { status: 403, cuerpo: { mensaje: 'detalle interno', trazaId: 'traza-403' } } })
    const alerta = fixture.nativeElement.querySelector('ap-periodo-de-hoy [role="alert"]') as HTMLElement
    expect(alerta.textContent).toContain('No tenés acceso a esto.')
    expect(alerta.textContent).toContain('traza-403')
    expect(alerta.textContent).not.toContain('detalle interno')
    expect(http.match((r) => r.url.includes('/ejercicios/'))).toHaveLength(0)
  })

  it('los accesos a otras pantallas salen solo con permiso; activos y estados van al período de hoy', async () => {
    const fixture = await montar(http, {
      permisos: ['CONTABILIDAD_ERP_ACTIVOS_FIJOS', 'CONTABILIDAD_ERP_REPORTES'],
      vigente: vigente(10, 'e-2026'),
      periodos: periodos('e-2026', [mes(10, 'ABIERTO')]),
    })
    const enlaces = [...fixture.nativeElement.querySelectorAll('ap-periodo-de-hoy nav a')].map((a) => (a as HTMLElement).getAttribute('href'))
    expect(enlaces).toEqual(['/contabilidad/activos/p-10', '/contabilidad/estados/p-10'])
  })
})

describe('SelectorDeEjercicios · GET /erp/ejercicios por cursor antesDeAnio', () => {
  let http: HttpTestingController
  beforeEach(() => (http = configurar()))

  it('lista los ejercicios como enlaces y marca el que se está viendo', async () => {
    const fixture = await montar(http, {
      vigente: vigente(10, 'e-2026'),
      periodos: periodos('e-2026', []),
      ejercicios: { items: [ejercicio(2026, 'e-2026'), ejercicio(2025, 'e-2025', 'CERRADO')], proximoAntesDeAnio: null },
    })
    const enlaces = [...fixture.nativeElement.querySelectorAll('ap-selector-de-ejercicios a')] as HTMLElement[]
    expect(enlaces.map((a) => a.textContent?.trim())).toEqual(['Ejercicio 2026 · abierto', 'Ejercicio 2025 · cerrado'])
    expect(enlaces[0]?.getAttribute('aria-current')).toBe('page')
    expect(fixture.nativeElement.textContent).not.toContain('Ver ejercicios anteriores')
  })

  it('con próxima página pide la siguiente con el cursor del servidor, y en la última deja de ofrecerla', async () => {
    const fixture = await montar(http, {
      vigente: vigente(10, 'e-2026'),
      periodos: periodos('e-2026', []),
      ejercicios: { items: [ejercicio(2026, 'e-2026')], proximoAntesDeAnio: 2026 },
    })
    const boton = [...fixture.nativeElement.querySelectorAll('ap-selector-de-ejercicios ap-boton button')][0] as HTMLButtonElement
    expect(boton.textContent).toContain('Ver ejercicios anteriores')
    boton.click()
    await esperar(fixture)
    http.expectOne(`${URL_EJERCICIOS}&antesDeAnio=2026`).flush({ items: [ejercicio(2025, 'e-2025', 'CERRADO')], proximoAntesDeAnio: null })
    await esperar(fixture)
    expect(fixture.nativeElement.querySelectorAll('ap-selector-de-ejercicios a')).toHaveLength(2)
    expect(fixture.nativeElement.textContent).not.toContain('Ver ejercicios anteriores')
  })

  it('vacío: sin ningún ejercicio abierto lo dice', async () => {
    const fixture = await montar(http, { vigente: { status: 422, cuerpo: { codigo: 'AP-CU100-04', mensaje: 'x', trazaId: 't' } } })
    expect(fixture.nativeElement.textContent).toContain('Todavía no se abrió ningún ejercicio fiscal.')
  })
})
