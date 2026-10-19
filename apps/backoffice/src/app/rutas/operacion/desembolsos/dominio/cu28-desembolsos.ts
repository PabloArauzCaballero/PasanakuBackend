import { HttpClient } from '@angular/common/http'
import { inject } from '@angular/core'
import type { DesembolsoEnLista } from 'clientes/angular/entregas/model/desembolsoEnLista'
import type { PaginaDeDesembolsos } from 'clientes/angular/entregas/model/paginaDeDesembolsos'
import { GATEWAY } from '../../../../nucleo/gateway'
import type { PedirPorCursor } from '../../../../nucleo/tabla/cursor'
import { bandejaPorCursor, cursorDe, estadosDe, pedirJson, type Bandeja } from '../../dominio/bandeja-por-cursor'

export type { DesembolsoEnLista }

/**
 * CU-28 · bandeja de desembolsos de Tesorería (`listarDesembolsos`, `GET /desembolsos` de `entregas.yaml`, permiso
 * ENTREGA_EJECUTAR o AUDITORIA_LEER). Sin `estados` el servidor devuelve las excepciones (RECHAZADA,
 * DEVUELTA_POR_BANCO, EN_PROCESO). A diferencia de las otras bandejas, `estados` es `style: form, explode: true`:
 * se repite la clave por cada valor. La fila no trae cuenta destino, glosa ni referencia del proveedor.
 */
type Cursor = { a: string; b: string }

export function pedirDesembolsos(http: HttpClient, gateway: string): PedirPorCursor<DesembolsoEnLista, Cursor> {
  return async ({ limite, cursor, filtros }) => {
    const p = await pedirJson<PaginaDeDesembolsos>(http, `${gateway}/desembolsos`, {
      estados: estadosDe(filtros),
      limite,
      antesDe: cursor?.a,
      antesDeId: cursor?.b,
    })
    return { items: p.items, siguiente: cursorDe(p.items, p.proximoAntesDe, p.proximoAntesDeId) }
  }
}

export function bandejaDeDesembolsos(): Bandeja<DesembolsoEnLista> {
  return bandejaPorCursor(pedirDesembolsos(inject(HttpClient), inject(GATEWAY)))
}
