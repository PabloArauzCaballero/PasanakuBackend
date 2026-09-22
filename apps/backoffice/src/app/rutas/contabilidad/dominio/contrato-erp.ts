import { HttpClient, HttpContext } from '@angular/common/http'
import { firstValueFrom } from 'rxjs'
import { CLAVE_IDEMPOTENCIA, claveDeIdempotencia } from '../../../nucleo/idempotencia.interceptor'
import type { CargadorDePagina, PaginaServidor, PedidoDePagina } from '../../../nucleo/tabla/tipos'

/**
 * Lo que `servicios/erp/src/main/resources/openapi/erp.yaml` comparte entre sus quince
 * operaciones, más el adaptador de lectura que este carril necesita y el contrato
 * todavía no tiene.
 *
 * **Hueco declarado — pedido pendiente al carril `5A` (backend `erp`).** Las quince
 * rutas de `erp.yaml` son de ESCRITURA (catorce `POST` y un `GET` de validación de
 * plantilla). No existe ninguna operación de listado: ni `GET /erp/periodos`, ni
 * `/erp/presupuestos`, ni `/erp/ordenes-de-compra`, ni `/erp/facturas-de-proveedor`,
 * ni `/erp/cuentas-por-cobrar`, ni `/erp/activos`, ni `/erp/estados-financieros`. Las
 * seis pantallas de este carril son, en su mayor parte, pantallas de listado: sin esas
 * operaciones no hay nada que mostrar. Se piden al carril de backend con la forma de
 * abajo; hasta que existan, cada pantalla llama al gateway con esta forma y el contrato
 * real puede diferir en el nombre exacto de la ruta o del campo.
 *
 * Ninguna de esas rutas pagina ni ordena del lado del servidor todavía, así que el
 * adaptador pide la lista completa una vez y pagina, ordena y filtra acá — **nunca**
 * dentro de `TablaDeDatosVirtualizada`, que sigue sin saber de HTTP ni de memoria.
 * Cuando el backend publique paginación real, este archivo es el único que cambia.
 */
export type Moneda = 'BOB' | 'USD'

/** Importe como CADENA decimal, igual que el esquema `Monto` de `erp.yaml`. Nunca un number. */
export type Importe = { monto: string; moneda: Moneda }

/** El contexto HTTP que exige toda operación con efecto de `erp.yaml` (invariante 7). */
export function contextoConClave(): HttpContext {
  return new HttpContext().set(CLAVE_IDEMPOTENCIA, claveDeIdempotencia())
}

function comparar<T>(a: T, b: T, clave: keyof T): number {
  const [va, vb] = [a[clave], b[clave]]
  if (va && typeof va === 'object' && 'monto' in va && vb && typeof vb === 'object' && 'monto' in vb) {
    return Number((va as { monto: string }).monto) - Number((vb as { monto: string }).monto)
  }
  return String(va ?? '').localeCompare(String(vb ?? ''))
}

/** Pagina, ordena y filtra en el adaptador una lista que el servidor todavía entrega entera. */
export function paginarEnElAdaptador<T extends object>(
  todos: readonly T[],
  pedido: PedidoDePagina,
  filtrar: (fila: T, filtros: Readonly<Record<string, string>>) => boolean,
  ordenPorOmision?: (a: T, b: T) => number,
): PaginaServidor<T> {
  const filtrados = todos.filter((f) => filtrar(f, pedido.filtros))
  const ordenados = pedido.orden
    ? [...filtrados].sort((a, b) => {
        const signo = pedido.orden!.sentido === 'asc' ? 1 : -1
        return signo * comparar(a, b, pedido.orden!.clave as keyof T)
      })
    : ordenPorOmision
      ? [...filtrados].sort(ordenPorOmision)
      : filtrados
  const inicio = (pedido.pagina - 1) * pedido.tamano
  return { filas: ordenados.slice(inicio, inicio + pedido.tamano), total: ordenados.length }
}

/** El `cargador` que pide `TablaDeDatosVirtualizada`, sobre una lista completa del gateway. */
export function cargadorDeLista<T extends object>(
  http: HttpClient,
  url: string,
  filtrar: (fila: T, filtros: Readonly<Record<string, string>>) => boolean,
  ordenPorOmision?: (a: T, b: T) => number,
): CargadorDePagina<T> {
  return async (pedido) => {
    const todos = await firstValueFrom(http.get<T[]>(url))
    return paginarEnElAdaptador(todos ?? [], pedido, filtrar, ordenPorOmision)
  }
}

/** Filtro por un campo de estado, con el valor que la `BarraDeFiltros` dejó en la URL. */
export function filtroPorEstado<T extends { estado: string }>(fila: T, filtros: Readonly<Record<string, string>>): boolean {
  return !filtros['estado'] || fila.estado === filtros['estado']
}
