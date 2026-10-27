import { Router } from '@angular/router'
import { TestBed } from '@angular/core/testing'
import { describe, expect, it, vi } from 'vitest'
import { PantallaDeAnunciantes } from './pantalla-de-anunciantes'
import { anunciante, boton, buscar, configurar, elegir, esperar, GW, montar, pagina, texto } from '../publicidad.apoyo'

const URL = `${GW}/publicidad/anunciantes?limite=100`
const veinticinco = Array.from({ length: 25 }, (_, i) => anunciante({ anuncianteId: `a-${i}`, cuentaPublicitariaId: `cp-${i}`, razonSocialFacturacion: `Anunciante ${i + 1}`, tipo: i % 2 ? 'ORGANIZADOR' : 'SOCIO_COMERCIAL' }))
const filas = (f: { nativeElement: HTMLElement }) => f.nativeElement.querySelectorAll('ap-tabla-de-datos tbody tr').length

/**
 * CU-110 · Anunciantes con la anatomía única del listado (2026-10-26), contra el doble del gateway
 * (`HttpTestingController`, regla 65): lotes de 100 por cursor, búsqueda/filtros/paginación en lo cargado, estado en el
 * servidor, acciones de fila de lo que el contrato tiene (desempeño y liquidación de la cuenta) y el alta.
 */
describe('PantallaDeAnunciantes · CU-110', () => {
  it('aceptado · pide el primer lote de 100, pagina de a 10 y busca sin acentos', async () => {
    const http = configurar(['PUBLICIDAD_ANUNCIANTES'])
    const f = await montar(PantallaDeAnunciantes)
    http.expectOne(URL).flush(pagina(veinticinco))
    await esperar(f)
    expect(texto(f)).toContain('Página 1 de 3')
    expect(filas(f)).toBe(10)
    boton(f.nativeElement, 'Siguiente')!.click()
    await esperar(f)
    expect(texto(f)).toContain('Página 2 de 3')
    await buscar(f, 'anunciante 25')
    expect(texto(f)).toContain('1 de 25 anunciantes')
  })

  it('aceptado · el estado viaja al servidor como `estados` y reinicia los lotes', async () => {
    const http = configurar(['PUBLICIDAD_ANUNCIANTES'])
    const f = await montar(PantallaDeAnunciantes)
    http.expectOne(URL).flush(pagina([anunciante()]))
    await esperar(f)
    await elegir(f, 'Estado', 'SUSPENDIDO')
    http.expectOne(`${URL}&estados=SUSPENDIDO`).flush(pagina([anunciante({ estado: 'SUSPENDIDO', razonSocialFacturacion: 'Suspendida SA' })]))
    await esperar(f)
    expect(texto(f)).toContain('Suspendida SA')
  })

  it('aceptado · «Cargar 100 más» pide el lote siguiente con las dos mitades del cursor y suma las filas', async () => {
    const http = configurar([])
    const f = await montar(PantallaDeAnunciantes)
    http.expectOne(URL).flush(pagina([anunciante()], '2026-09-01T12:00:00Z', 'a-1'))
    await esperar(f)
    boton(f.nativeElement, 'Cargar 100 más')!.click()
    await esperar(f)
    http.expectOne(`${URL}&despuesDeFecha=2026-09-01T12:00:00Z&despuesDeId=a-1`).flush(pagina([anunciante({ anuncianteId: 'a-2', razonSocialFacturacion: 'Segunda SA' })]))
    await esperar(f)
    expect(texto(f)).toContain('Segunda SA')
    expect(texto(f)).toContain('2 anunciantes')
    expect(boton(f.nativeElement, 'Cargar 100 más')).toBeUndefined()
  })

  it('aceptado · la fila lleva al desempeño de su cuenta; el contrato no tiene editar ni eliminar', async () => {
    const http = configurar([])
    const f = await montar(PantallaDeAnunciantes)
    http.expectOne(URL).flush(pagina([anunciante()]))
    await esperar(f)
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true)
    boton(f.nativeElement, 'Desempeño de Anunciante SA')!.click()
    expect(navegar).toHaveBeenCalledWith(['/publicidad', 'desempeno'], { queryParams: { cuenta: 'cp-1' } })
    expect(boton(f.nativeElement, /^Editar/)).toBeUndefined()
    expect(boton(f.nativeElement, /^Eliminar/)).toBeUndefined()
  })

  it('aceptado · el alta manda el identificador que exige R-PUB-01 y recarga; sin permiso no hay botón', async () => {
    const http = configurar(['PUBLICIDAD_ANUNCIANTES'])
    const f = await montar(PantallaDeAnunciantes)
    http.expectOne(URL).flush(pagina([]))
    await esperar(f)
    boton(f.nativeElement, 'Dar de alta anunciante')!.click()
    await esperar(f)
    const campos = [...document.querySelectorAll('ap-formulario-de-anunciante ap-campo input')] as HTMLInputElement[]
    campos[0]!.value = '6e7a3f12-8b4c-4d2e-9f1a-0c5b7d9e2a41'
    campos[0]!.dispatchEvent(new Event('input'))
    campos[1]!.value = 'Socio Sintético SRL'
    campos[1]!.dispatchEvent(new Event('input'))
    boton(document, 'Guardar')!.click()
    await esperar(f)
    const alta = http.expectOne({ method: 'POST', url: `${GW}/publicidad/anunciantes` })
    expect(alta.request.body).toEqual({ tipo: 'SOCIO_COMERCIAL', socioComercialId: '6e7a3f12-8b4c-4d2e-9f1a-0c5b7d9e2a41', razonSocialFacturacion: 'Socio Sintético SRL', moneda: 'BOB' })
    alta.flush({ anuncianteId: 'a-9', cuentaPublicitariaId: 'cp-9' }, { status: 201, statusText: 'Created' })
    await esperar(f)
    http.expectOne(URL).flush(pagina([anunciante({ razonSocialFacturacion: 'Socio Sintético SRL' })]))
    await esperar(f)
    expect(texto(f)).toContain('Anunciante dado de alta')
    expect(texto(f)).toContain('Socio Sintético SRL')
  })

  it('límite · vacío sin datos orienta; búsqueda sin coincidencias ofrece «Quitar filtros» y lo restablece', async () => {
    const http = configurar([])
    const f = await montar(PantallaDeAnunciantes)
    http.expectOne(URL).flush(pagina([]))
    await esperar(f)
    expect(texto(f)).toContain('No hay anunciantes dados de alta todavía.')
    f.destroy()
    const g = await montar(PantallaDeAnunciantes)
    http.expectOne(URL).flush(pagina([anunciante()]))
    await esperar(g)
    await buscar(g, 'nada parecido')
    boton(g.nativeElement, 'Quitar filtros')!.click()
    await esperar(g)
    expect(texto(g)).toContain('Anunciante SA')
  })

  it('límite · el alta sin identificador válido no envía nada y lo dice en el campo', async () => {
    const http = configurar(['PUBLICIDAD_ANUNCIANTES'])
    const f = await montar(PantallaDeAnunciantes)
    http.expectOne(URL).flush(pagina([]))
    await esperar(f)
    boton(f.nativeElement, 'Dar de alta anunciante')!.click()
    await esperar(f)
    const id = document.querySelector('ap-formulario-de-anunciante ap-campo input') as HTMLInputElement
    id.value = 'no-es-uuid'
    id.dispatchEvent(new Event('input'))
    boton(document, 'Guardar')!.click()
    await esperar(f)
    expect(http.match({ method: 'POST' })).toHaveLength(0)
    expect(document.body.textContent).toContain('Ese identificador no tiene la forma 8-4-4-4-12.')
  })

  it('error · 500 con su código de seguimiento; «Volver a intentar» repite la petición', async () => {
    const http = configurar([])
    const f = await montar(PantallaDeAnunciantes)
    http.expectOne(URL).flush({ codigo: 'AP-X', mensaje: 'crudo', trazaId: 'tz-500' }, { status: 500, statusText: 'x' })
    await esperar(f)
    const alerta = f.nativeElement.querySelector('[role="alert"]') as HTMLElement
    expect(alerta.textContent).toContain('tz-500')
    expect(alerta.textContent).not.toContain('crudo')
    boton(f.nativeElement, /Volver a intentar|Reintentar/)!.click()
    await esperar(f)
    http.expectOne(URL).flush(pagina([anunciante()]))
    await esperar(f)
    expect(texto(f)).toContain('Anunciante SA')
  })

  it('error · 403 se traduce; 422 AP-CU110-04 dice el texto del dominio', async () => {
    const http = configurar([])
    const f = await montar(PantallaDeAnunciantes)
    http.expectOne(URL).flush({}, { status: 403, statusText: 'Forbidden' })
    await esperar(f)
    expect(f.nativeElement.querySelector('[role="alert"]')?.textContent).toContain('No tenés acceso')
    boton(f.nativeElement, /Volver a intentar|Reintentar/)!.click()
    await esperar(f)
    http.expectOne(URL).flush({ codigo: 'AP-CU110-04', mensaje: 'x', trazaId: 'tz' }, { status: 422, statusText: 'x' })
    await esperar(f)
    expect(f.nativeElement.querySelector('[role="alert"]')?.textContent).toContain('El filtro de la lista no es válido')
  })

  it('error · el alta rechazada deja el diálogo abierto con lo escrito y el botón libre', async () => {
    const http = configurar(['PUBLICIDAD_ANUNCIANTES'])
    const f = await montar(PantallaDeAnunciantes)
    http.expectOne(URL).flush(pagina([]))
    await esperar(f)
    boton(f.nativeElement, 'Dar de alta anunciante')!.click()
    await esperar(f)
    const campos = [...document.querySelectorAll('ap-formulario-de-anunciante ap-campo input')] as HTMLInputElement[]
    campos[0]!.value = '6e7a3f12-8b4c-4d2e-9f1a-0c5b7d9e2a41'
    campos[0]!.dispatchEvent(new Event('input'))
    campos[1]!.value = 'Rechazada SRL'
    campos[1]!.dispatchEvent(new Event('input'))
    boton(document, 'Guardar')!.click()
    await esperar(f)
    http.expectOne({ method: 'POST', url: `${GW}/publicidad/anunciantes` }).flush({ codigo: 'AP-CU110-01', mensaje: 'Regla', trazaId: 'tz-1' }, { status: 422, statusText: 'x' })
    await esperar(f)
    expect((document.querySelector('ap-formulario-de-anunciante dialog') as HTMLDialogElement).open).toBe(true)
    expect(campos[1]!.value).toBe('Rechazada SRL')
    expect(document.querySelector('ap-formulario-de-anunciante ap-error-de-envio')?.textContent?.trim().length).toBeGreaterThan(0)
    expect(boton(document, 'Guardar')!.disabled).toBe(false)
  })
})
