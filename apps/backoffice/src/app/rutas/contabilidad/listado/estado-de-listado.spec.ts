import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import type { ErrorTraducido } from '../../../nucleo/errores'
import type { PedidoDePagina } from '../../../nucleo/tabla/tipos'
import { EstadoDeListado, observarListado } from './estado-de-listado'

const pedido = (pagina: number): PedidoDePagina => ({ pagina, tamano: 25, orden: null, filtros: {} })

describe('observarListado · vacío y error de una bandeja por cursor', () => {
  it('correcto · deja pasar la página tal cual y no marca nada', async () => {
    const l = observarListado(async () => ({ filas: [1, 2], total: 2 }))
    expect(await l.cargador(pedido(1))).toEqual({ filas: [1, 2], total: 2 })
    expect(l.vacio()).toBe(false)
    expect(l.error()).toBeNull()
  })

  it('límite · primera página sin filas: vacío', async () => {
    const l = observarListado(async () => ({ filas: [], total: 0 }))
    await l.cargador(pedido(1))
    expect(l.vacio()).toBe(true)
  })

  it('inválido · el cargador rechaza: guarda el error traducido y la tabla recibe una página vacía, no una promesa rota', async () => {
    const error: ErrorTraducido = { mensaje: 'No tenés acceso a esto.', estado: 403, trazaId: 'tz' }
    const l = observarListado(async () => Promise.reject(error))
    expect(await l.cargador(pedido(1))).toEqual({ filas: [], total: 0 })
    expect(l.error()).toEqual(error)
    expect(l.vacio()).toBe(false)
  })

  it('inválido · un código de filtro que el catálogo no conoce usa el texto propio del dominio', async () => {
    const l = observarListado(async () => Promise.reject({ mensaje: 'genérico', codigo: 'AP-CU101-04', estado: 422 }), { 'AP-CU101-04': 'Filtro inválido.' })
    await l.cargador(pedido(1))
    expect(l.error()?.mensaje).toBe('Filtro inválido.')
  })

  it('un error que no vino del interceptor no se muestra crudo', async () => {
    const l = observarListado(async () => Promise.reject(new Error('stack interno')))
    await l.cargador(pedido(1))
    expect(l.error()?.mensaje).not.toContain('stack')
  })

  it('después de un error, una carga buena limpia el error', async () => {
    let falla = true
    const l = observarListado(async () => (falla ? Promise.reject({ mensaje: 'x' }) : { filas: [1], total: 1 }))
    await l.cargador(pedido(1))
    falla = false
    await l.cargador(pedido(1))
    expect(l.error()).toBeNull()
  })
})

describe('EstadoDeListado', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] }))

  function montar(entradas: Record<string, unknown>) {
    const fixture = TestBed.createComponent(EstadoDeListado)
    fixture.componentRef.setInput('mensajeVacio', 'No hay nada.')
    for (const [k, v] of Object.entries(entradas)) fixture.componentRef.setInput(k, v)
    fixture.detectChanges()
    return fixture.nativeElement as HTMLElement
  }

  it('error: alerta con mensaje y traza', () => {
    const el = montar({ error: { mensaje: 'No tenés acceso a esto.', trazaId: 'tz-1' } })
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('No tenés acceso a esto.')
    expect(el.textContent).toContain('Código de seguimiento: tz-1')
  })

  it('vacío: el mensaje del dominio, anunciado', () => {
    const el = montar({ vacio: true })
    expect(el.querySelector('[role="status"]')?.textContent).toContain('No hay nada.')
  })

  it('con datos: no dibuja nada', () => {
    expect(montar({}).textContent?.trim()).toBe('')
  })
})
