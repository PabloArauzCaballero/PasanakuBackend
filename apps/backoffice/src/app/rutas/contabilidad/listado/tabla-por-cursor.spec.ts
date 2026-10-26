import { Component, provideZonelessChangeDetection } from '@angular/core'
import { TestBed, type ComponentFixture } from '@angular/core/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import { aCsv } from '@aportaya/ui/exportar-listado/exportar'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { cargadorPorCursor, type PedidoPorCursor } from '../../../nucleo/tabla/cursor'
import { crearListadoPorCursor } from './listado-por-cursor'
import { TablaPorCursor } from './tabla-por-cursor'

/**
 * H5.S0.M1 · El listado por cursor compartido de Contabilidad y Configuración, contra un **doble del servidor** (regla
 * 65): pagina 25 filas sintéticas por cursor (índice de la última fila), filtra por `estado` como lo haría `estados` y
 * registra cada pedido para afirmar qué viajó. Tres niveles: aceptado, límite, error.
 */
type Fila = { id: string; nombre: string; estado: 'ABIERTA' | 'CERRADA'; monto: string }

const FILAS: Fila[] = Array.from({ length: 25 }, (_, i) => ({ id: `F-${i + 1}`, nombre: i === 3 ? 'José Ñandú' : `Fila ${i + 1}`, estado: i % 2 ? 'CERRADA' : 'ABIERTA', monto: `${i + 1}.50` }))

const servidor = { datos: FILAS, pedidos: [] as PedidoPorCursor<number>[], falla: null as unknown }

@Component({
  imports: [TablaPorCursor],
  template: `
    <ap-tabla-por-cursor titulo="Filas" [columnas]="columnas" [listado]="listado" [identidad]="id" [exportables]="exportables" textoVacio="No hay filas.">
      <ng-template #celda let-f let-c="columna">{{ f[c.clave] }}</ng-template>
    </ap-tabla-por-cursor>
  `,
})
class Anfitrion {
  readonly columnas: Columna<Fila>[] = [{ clave: 'id', titulo: 'Código' }, { clave: 'nombre', titulo: 'Nombre' }, { clave: 'estado', titulo: 'Estado' }]
  readonly exportables = [{ clave: 'id', titulo: 'Código' }, { clave: 'monto', titulo: 'Monto', valor: (f: Fila) => `${f.monto} BOB` }]
  readonly id = (f: Fila) => f.id
  readonly listado = crearListadoPorCursor<Fila>({
    cargador: cargadorPorCursor<Fila, number>(async (pedido) => {
      servidor.pedidos.push(pedido)
      if (servidor.falla) throw servidor.falla
      const todas = servidor.datos.filter((f) => !pedido.filtros['estado'] || f.estado === pedido.filtros['estado'])
      const desde = pedido.cursor ?? 0
      const items = todas.slice(desde, desde + pedido.limite)
      return { items, siguiente: desde + pedido.limite < todas.length ? desde + pedido.limite : null }
    }),
    camposDeBusqueda: ['id', 'nombre'],
    servidor: { nombre: 'estado', etiqueta: 'Estado', opciones: [{ valor: 'ABIERTA', texto: 'Abierta' }, { valor: 'CERRADA', texto: 'Cerrada' }] },
  })
}

async function estable(f: ComponentFixture<unknown>): Promise<void> {
  for (let i = 0; i < 4; i++) {
    await new Promise((r) => setTimeout(r, 0))
    f.detectChanges()
  }
}
const texto = (el: Element | null) => el?.textContent?.replace(/\s+/g, ' ').trim() ?? ''

describe('ap-tabla-por-cursor · listado paginado en el servidor', () => {
  let f: ComponentFixture<Anfitrion>
  let el: HTMLElement
  beforeEach(async () => {
    servidor.datos = FILAS
    servidor.pedidos = []
    servidor.falla = null
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
    f = TestBed.createComponent(Anfitrion)
    el = f.nativeElement
    f.detectChanges()
    await estable(f)
  })

  it('aceptado · pide 10 por página sin cursor y dibuja la página 1 con búsqueda, filtro, exportar y paginación', () => {
    expect(servidor.pedidos[0]).toEqual({ limite: 10, cursor: null, filtros: {} })
    expect(el.querySelectorAll('tbody tr').length).toBe(10)
    expect(el.querySelector('ap-barra-de-filtros input[type=search]')).not.toBeNull()
    expect(texto(el.querySelector('ap-exportar-listado'))).toContain('Descargar CSV')
    expect(texto(el.querySelector('ap-paginacion .resumen'))).toBe('Página 1 de 2')
    expect(texto(el.querySelector('.resumen'))).toBe('10 registros en la página 1')
  })

  it('aceptado · Siguiente pide con el cursor de la página 1, e «Ir a 3» llega a la última parcial (5 filas)', async () => {
    ;(Array.from(el.querySelectorAll('ap-paginacion .pasos button')).at(1) as HTMLButtonElement).click()
    await estable(f)
    expect(servidor.pedidos.at(-1)).toEqual({ limite: 10, cursor: 10, filtros: {} })
    expect(texto(el.querySelector('tbody tr td'))).toContain('F-11')
    f.componentInstance.listado.irAPagina(3)
    await estable(f)
    expect(servidor.pedidos.at(-1)?.cursor).toBe(20)
    expect(el.querySelectorAll('tbody tr').length).toBe(5)
    expect(texto(el.querySelector('ap-paginacion .resumen'))).toBe('Página 3 de 3')
  })

  it('aceptado · volver atrás después de conocer la última página no esconde las siguientes (Anterior desde la 3 → «2 de 3», 1 → «1 de 3»)', async () => {
    ;(Array.from(el.querySelectorAll('ap-paginacion .pasos button')).at(1) as HTMLButtonElement).click()
    await estable(f)
    f.componentInstance.listado.irAPagina(3)
    await estable(f)
    f.componentInstance.listado.irAPagina(2)
    await estable(f)
    expect(texto(el.querySelector('ap-paginacion .resumen'))).toBe('Página 2 de 3')
    expect((Array.from(el.querySelectorAll('ap-paginacion .pasos button')).at(1) as HTMLButtonElement).disabled).toBe(false)
    f.componentInstance.listado.irAPagina(1)
    await estable(f)
    expect(texto(el.querySelector('ap-paginacion .resumen'))).toBe('Página 1 de 3')
  })

  it('aceptado · recargar en la página 1 después de conocer la última no apaga «Siguiente» (tras una mutación)', async () => {
    f.componentInstance.listado.irAPagina(2)
    await estable(f)
    f.componentInstance.listado.irAPagina(3)
    await estable(f)
    f.componentInstance.listado.irAPagina(1)
    await estable(f)
    f.componentInstance.listado.recargar()
    await estable(f)
    expect(servidor.pedidos.at(-1)?.cursor).toBeNull()
    expect(texto(el.querySelector('ap-paginacion .resumen'))).toBe('Página 1 de 2')
    expect((Array.from(el.querySelectorAll('ap-paginacion .pasos button')).at(1) as HTMLButtonElement).disabled).toBe(false)
  })

  it('aceptado · filas por página = 20 vuelve a la 1 y lo pide al servidor', async () => {
    f.componentInstance.listado.cambiarPorPagina(20)
    await estable(f)
    expect(servidor.pedidos.at(-1)).toEqual({ limite: 20, cursor: null, filtros: {} })
    expect(el.querySelectorAll('tbody tr').length).toBe(20)
  })

  it('aceptado · el filtro del servidor viaja en la consulta; la búsqueda sin acentos filtra la página cargada', async () => {
    f.componentInstance.listado.filtrar('estado', 'CERRADA')
    await estable(f)
    expect(servidor.pedidos.at(-1)?.filtros).toEqual({ estado: 'CERRADA' })
    f.componentInstance.listado.filtrar('estado', '')
    await estable(f)
    const pedidos = servidor.pedidos.length
    f.componentInstance.listado.buscar('jose nandu')
    await estable(f)
    expect(servidor.pedidos.length).toBe(pedidos)
    expect(el.querySelectorAll('tbody tr').length).toBe(1)
    expect(texto(el.querySelector('.resumen'))).toBe('1 de 10 registros en esta página')
  })

  it('aceptado · CSV de lo filtrado con el importe como texto exacto y su moneda', () => {
    f.componentInstance.listado.buscar('F-2')
    const csv = aCsv(f.componentInstance.exportables, f.componentInstance.listado.visibles())
    expect(csv.split('\r\n').slice(0, 2)).toEqual(['﻿Código,Monto', 'F-2,2.50 BOB'])
  })

  it('límite · búsqueda sin coincidencias ofrece «Quitar filtros» y lo vuelve a mostrar todo', async () => {
    f.componentInstance.listado.buscar('zzz')
    await estable(f)
    const quitar = Array.from(el.querySelectorAll('ap-estado-vacio button')).find((b) => texto(b).includes('Quitar filtros')) as HTMLButtonElement
    expect(quitar).toBeTruthy()
    quitar.click()
    await estable(f)
    expect(el.querySelectorAll('tbody tr').length).toBe(10)
  })

  it('límite · colección vacía: vacío orientado, sin paginación', async () => {
    servidor.datos = []
    f.componentInstance.listado.recargar()
    await estable(f)
    expect(texto(el.querySelector('ap-estado-vacio'))).toContain('No hay filas.')
    expect(el.querySelector('ap-paginacion')).toBeNull()
  })

  it('límite · una sola fila: una página, sin «Ir a»', async () => {
    servidor.datos = FILAS.slice(0, 1)
    f.componentInstance.listado.recargar()
    await estable(f)
    expect(el.querySelectorAll('tbody tr').length).toBe(1)
    expect(texto(el.querySelector('ap-paginacion .resumen'))).toBe('Página 1 de 1')
    expect(el.querySelector('ap-paginacion input[type=number]')).toBeNull()
  })

  it('error · el rechazo se dice traducido con su código de seguimiento, y «Volver a intentar» repite el pedido', async () => {
    servidor.falla = { mensaje: 'No tenés acceso a esto.', trazaId: 'tz-403' }
    f.componentInstance.listado.recargar()
    await estable(f)
    const error = el.querySelector('ap-estado-de-error')
    expect(texto(error)).toContain('No tenés acceso a esto.')
    expect(texto(error)).toContain('tz-403')
    const antes = servidor.pedidos.length
    servidor.falla = null
    ;(error!.querySelector('button') as HTMLButtonElement).click()
    await estable(f)
    expect(servidor.pedidos.length).toBe(antes + 1)
    expect(el.querySelectorAll('tbody tr').length).toBe(10)
  })

  it('error · un rechazo que no trae mensaje no muestra nada crudo', async () => {
    servidor.falla = new Error('stack interno')
    f.componentInstance.listado.recargar()
    await estable(f)
    expect(texto(el.querySelector('ap-estado-de-error'))).not.toContain('stack interno')
  })
})
