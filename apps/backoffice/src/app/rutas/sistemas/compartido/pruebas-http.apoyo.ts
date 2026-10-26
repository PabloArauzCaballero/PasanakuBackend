import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection, type Provider, type Type } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { conteoDeExportacion, elegir, estabilizar, filasVisibles, listadoDe, pulsar, resumenDePaginas, texto } from './pruebas.apoyo'

export const GW_DE_PRUEBA = 'http://gw/api/v1'

export type CasoDeListadoHttp<T> = {
  componente: Type<unknown>
  /** La URL exacta que la vista pide al montar. */
  url: string
  /** Envuelve las filas en la forma del contrato (p. ej. `{ items }`). */
  respuesta?: (filas: T[]) => object
  filas: (n: number) => T[]
  textoVacio: string
  busqueda: { termino: string; esperadas: number }
  filtro: { etiqueta: string; valor: string; esperadas: number }
  extra?: Provider[]
}

/**
 * Los tres niveles del DoD para una vista que lee un contrato HTTP sin paginar (la colección entera llega y el kit la
 * pagina): contra `HttpTestingController`, que es un doble del gateway (regla 65), nunca el servicio real.
 */
export function probarListadoHttp<T>(caso: CasoDeListadoHttp<T>): void {
  const envolver: (f: T[]) => object = caso.respuesta ?? ((f: T[]) => f)
  function montar() {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideRouter([]), provideHttpClient(), provideHttpClientTesting(), { provide: GATEWAY, useValue: GW_DE_PRUEBA }, ...(caso.extra ?? [])],
    })
    const http = TestBed.inject(HttpTestingController)
    const f = TestBed.createComponent(caso.componente)
    f.detectChanges()
    return { f, http }
  }

  it('aceptado: 25 filas a 10 por página, «Ir a 3» muestra 5, buscar y filtrar achican lo descargable', async () => {
    const { f, http } = montar()
    http.expectOne(caso.url).flush(envolver(caso.filas(25)))
    const el = await estabilizar(f)
    expect(filasVisibles(el)).toBe(10)
    expect(resumenDePaginas(el)).toContain('Página 1 de 3')
    listadoDe(f).irAPagina(3)
    await estabilizar(f)
    expect(filasVisibles(el)).toBe(5)
    listadoDe(f).buscar(caso.busqueda.termino)
    await estabilizar(f)
    expect(conteoDeExportacion(el)).toBe(`${caso.busqueda.esperadas} ${caso.busqueda.esperadas === 1 ? 'fila' : 'filas'}`)
    listadoDe(f).limpiar()
    elegir(el, caso.filtro.etiqueta, caso.filtro.valor)
    await estabilizar(f)
    expect(listadoDe(f).total()).toBe(caso.filtro.esperadas)
    http.verify()
  })

  it('límite: vacío orientado sin tabla; una sola fila y una búsqueda sin coincidencias con «Quitar filtros»', async () => {
    const { f, http } = montar()
    http.expectOne(caso.url).flush(envolver([]))
    const el = await estabilizar(f)
    expect(texto(el)).toContain(caso.textoVacio)
    expect(el.querySelector('ap-listado table')).toBeNull()
    f.destroy()
    TestBed.resetTestingModule()
    const otro = montar()
    otro.http.expectOne(caso.url).flush(envolver(caso.filas(1)))
    const el2 = await estabilizar(otro.f)
    expect(resumenDePaginas(el2)).toContain('Página 1 de 1')
    listadoDe(otro.f).buscar('zzz-no-existe-zzz')
    await estabilizar(otro.f)
    pulsar(el2, 'Quitar filtros')
    await estabilizar(otro.f)
    expect(filasVisibles(el2)).toBe(1)
    otro.http.verify()
  })

  it('error: un 500 muestra el mensaje traducido (no el cuerpo crudo) y «Volver a intentar» repite el GET', async () => {
    const { f, http } = montar()
    http.expectOne(caso.url).flush({ mensaje: 'NullPointerException en Repo' }, { status: 500, statusText: 'Error' })
    const el = await estabilizar(f)
    expect(el.querySelector('ap-estado-de-error')).not.toBeNull()
    expect(texto(el)).not.toContain('NullPointerException')
    pulsar(el, 'Volver a intentar')
    f.detectChanges()
    http.expectOne(caso.url).flush(envolver(caso.filas(2)))
    await estabilizar(f)
    expect(filasVisibles(el)).toBe(2)
    http.verify()
  })
}
