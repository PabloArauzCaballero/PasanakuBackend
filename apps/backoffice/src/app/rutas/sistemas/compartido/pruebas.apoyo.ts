import { provideZonelessChangeDetection, type InjectionToken, type Provider, type Type } from '@angular/core'
import { TestBed, type ComponentFixture } from '@angular/core/testing'
import { By } from '@angular/platform-browser'
import { ListadoDeDatos } from '@aportaya/ui/listado/listado'
import { expect, it, vi } from 'vitest'

/** Apoyo compartido de los specs de `sistemas/`: leer el DOM del kit de listado sin repetir selectores en cada vista. */
export const texto = (el: Element | null | undefined) => el?.textContent?.replace(/\s+/g, ' ').trim() ?? ''

export async function estabilizar(f: ComponentFixture<unknown>): Promise<HTMLElement> {
  f.detectChanges()
  await f.whenStable()
  f.detectChanges()
  return f.nativeElement as HTMLElement
}

/** El `ap-listado` número `i` de la vista (en orden de aparición) con su estado de señales. */
export function listadoDe(f: ComponentFixture<unknown>, i = 0) {
  const de = f.debugElement.queryAll(By.directive(ListadoDeDatos))[i]
  if (!de) throw new Error(`No hay ap-listado número ${i}`)
  return (de.componentInstance as ListadoDeDatos<object>).listado()
}

export const filasVisibles = (el: Element, i = 0) => el.querySelectorAll('ap-listado')[i]?.querySelectorAll('tbody tr').length ?? 0
export const resumenDePaginas = (el: Element, i = 0) => texto(el.querySelectorAll('ap-listado')[i]?.querySelector('ap-paginacion .resumen'))
export const conteoDeExportacion = (el: Element, i = 0) => texto(el.querySelectorAll('ap-listado')[i]?.querySelector('ap-exportar-listado .cuenta'))

/** Pulsa el botón cuyo texto visible es `nombre` dentro de `raiz`. */
export function pulsar(raiz: Element, nombre: string | RegExp): HTMLButtonElement {
  const b = Array.from(raiz.querySelectorAll('button')).find((x) => (typeof nombre === 'string' ? texto(x) === nombre : nombre.test(texto(x))))
  if (!b) throw new Error(`No hay botón «${String(nombre)}»`)
  b.click()
  return b as HTMLButtonElement
}

/** Elige en el selector de la barra de filtros cuyo rótulo es `etiqueta`, como lo haría la persona. */
export function elegir(el: Element, etiqueta: string, valor: string): void {
  const campo = Array.from(el.querySelectorAll('ap-barra-de-filtros ap-lista-de-opciones')).find((x) => texto(x.querySelector('label')).startsWith(etiqueta))
  const select = campo?.querySelector('select') as HTMLSelectElement | null
  if (!select) throw new Error(`No hay filtro «${etiqueta}»`)
  select.value = valor
  select.dispatchEvent(new Event('change'))
}

export type CasoDeVistaDePuerto<T> = {
  componente: Type<unknown>
  puerto: InjectionToken<{ obtener: () => Promise<unknown> }>
  /** Envuelve las filas en la forma que devuelve el puerto (algunos devuelven un objeto con dos colecciones). */
  respuesta?: (filas: T[]) => unknown
  /** `n` filas sintéticas y deterministas. */
  filas: (n: number) => T[]
  textoVacio: string
  /** Un término y cuántas de las 25 filas lo contienen en algún campo buscable. */
  busqueda: { termino: string; esperadas: number }
  /** Un filtro (rótulo + valor) y cuántas de las 25 filas lo cumplen. */
  filtro: { etiqueta: string; valor: string; esperadas: number }
  extra?: Provider[]
}

/**
 * Los tres niveles del DoD para una vista de puerto (regla 65: contra un doble del puerto, nunca contra la fuente real):
 * **aceptado** (paginación, búsqueda, filtro, exportación), **límite** (vacío, sin coincidencias, una fila, última página
 * parcial) y **error** (mensaje accionable y «Volver a intentar» que vuelve a pedir).
 */
export function probarVistaDePuerto<T>(caso: CasoDeVistaDePuerto<T>): void {
  const envolver = caso.respuesta ?? ((f: T[]) => f)
  function montar(obtener: () => Promise<unknown>) {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), { provide: caso.puerto, useValue: { obtener } }, ...(caso.extra ?? [])] })
    return TestBed.createComponent(caso.componente)
  }

  it('aceptado: 25 filas a 10 por página dan 3 páginas; «Ir a 3» muestra 5 y Anterior vuelve a 10', async () => {
    const f = montar(() => Promise.resolve(envolver(caso.filas(25))))
    const el = await estabilizar(f)
    expect(filasVisibles(el)).toBe(10)
    expect(resumenDePaginas(el)).toContain('Página 1 de 3')
    const ir = el.querySelector('ap-paginacion input[type=number]') as HTMLInputElement
    ir.value = '3'
    ir.form!.dispatchEvent(new Event('submit'))
    await estabilizar(f)
    expect(filasVisibles(el)).toBe(5)
    pulsar(el.querySelector('ap-paginacion')!, 'Anterior')
    await estabilizar(f)
    expect(resumenDePaginas(el)).toContain('Página 2 de 3')
    expect(conteoDeExportacion(el)).toBe('25 filas')
  })

  it(`aceptado: buscar «${caso.busqueda.termino}» y filtrar por ${caso.filtro.etiqueta} achican lo que se ve y lo que se descarga`, async () => {
    const f = montar(() => Promise.resolve(envolver(caso.filas(25))))
    const el = await estabilizar(f)
    listadoDe(f).buscar(caso.busqueda.termino)
    await estabilizar(f)
    expect(conteoDeExportacion(el)).toBe(`${caso.busqueda.esperadas} ${caso.busqueda.esperadas === 1 ? 'fila' : 'filas'}`)
    listadoDe(f).limpiar()
    await estabilizar(f)
    elegir(el, caso.filtro.etiqueta, caso.filtro.valor)
    await estabilizar(f)
    expect(listadoDe(f).total()).toBe(caso.filtro.esperadas)
  })

  it('límite: sin datos muestra el vacío del dominio, nunca una tabla', async () => {
    const el = await estabilizar(montar(() => Promise.resolve(envolver([]))))
    expect(texto(el)).toContain(caso.textoVacio)
    expect(el.querySelector('ap-listado table')).toBeNull()
  })

  it('límite: una búsqueda sin coincidencias ofrece «Quitar filtros» y lo vuelve todo; una fila sola es «Página 1 de 1»', async () => {
    const f = montar(() => Promise.resolve(envolver(caso.filas(1))))
    const el = await estabilizar(f)
    expect(resumenDePaginas(el)).toContain('Página 1 de 1')
    listadoDe(f).buscar('zzz-no-existe-zzz')
    await estabilizar(f)
    expect(conteoDeExportacion(el)).toBe('0 filas')
    pulsar(el, 'Quitar filtros')
    await estabilizar(f)
    expect(filasVisibles(el)).toBe(1)
  })

  // Sin contrato no es una falla: se dice en un estado neutro (2026-10-27). La caja roja con «Volver a intentar» se leía
  // como una caída y reintentar no podía cambiar nada.
  it('sin fuente: estado neutro con su explicación, sin caja de error, sin reintento y sin cifras', async () => {
    const obtener = vi.fn(() => Promise.reject({ tipo: 'fuente-no-disponible', mensaje: 'Ningún servicio publica todavía estos datos.' }))
    const el = await estabilizar(montar(obtener))
    expect(el.querySelector('ap-estado-de-error')).toBeNull()
    expect(texto(el.querySelector('ap-listado ap-estado-vacio'))).toContain('Ningún servicio publica todavía estos datos.')
    expect(Array.from(el.querySelectorAll('button')).some((b) => b.textContent?.includes('Volver a intentar'))).toBe(false)
    expect(el.querySelectorAll('ap-tarjeta-kpi').length).toBe(0)
  })

  it('error: una falla real del servicio sí muestra el error accionable y «Volver a intentar» vuelve a pedir', async () => {
    const obtener = vi.fn(() => Promise.reject({ mensaje: 'El servicio no respondió a tiempo.' }))
    const f = montar(obtener)
    const el = await estabilizar(f)
    expect(texto(el.querySelector('ap-estado-de-error'))).toContain('El servicio no respondió a tiempo.')
    pulsar(el, 'Volver a intentar')
    await estabilizar(f)
    expect(obtener).toHaveBeenCalledTimes(2)
  })
}
