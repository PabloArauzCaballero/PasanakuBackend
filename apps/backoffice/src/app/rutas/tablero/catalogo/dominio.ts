import { httpResource } from '@angular/common/http'
import { Injectable, inject } from '@angular/core'
import { ColumnaDelCatalogoOrigenNegocioEnum, type CatalogoDeDatos, type ColumnaDelCatalogo, type TablaDelCatalogo } from 'clientes/angular/auditoria'
import { GATEWAY } from '../../../nucleo/gateway'

/**
 * Catálogo de datos (H7) · el contrato `obtenerCatalogoDeDatos` de auditoria.yaml, con los tipos del cliente generado.
 * Es estructura y narrativa, no datos de personas: lo genera `scripts/catalogo_de_datos.py` y cambia con el modelo.
 */
export type { CatalogoDeDatos, ColumnaDelCatalogo, TablaDelCatalogo }

/** Una sola descarga para la lista y la ficha: el catálogo no cambia mientras el backoffice está abierto. */
@Injectable({ providedIn: 'root' })
export class CatalogoDeDatosRemoto {
  private readonly gateway = inject(GATEWAY)
  readonly recurso = httpResource<CatalogoDeDatos>(() => `${this.gateway}/indicadores/catalogo-de-datos`)
}

/** Sin acentos ni mayúsculas: «Obligación» encuentra «obligacion_aporte». */
export function normalizar(valor: string): string {
  return valor
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLocaleLowerCase('es')
    .trim()
}

/** Una fila de la lista, ya en texto (la tabla no formatea). */
export type FilaDeTabla = { id: string; tabla: string; esquema: string; servicio: string; columnas: string; queEs: string }

/**
 * Busca por nombre de tabla, de columna, esquema y por lo que la ficha dice que es. Con esquemas elegidos, solo esos.
 * Orden determinista: esquema y después tabla.
 */
export function filtrarTablas(catalogo: CatalogoDeDatos | undefined, busqueda: string, esquemas: readonly string[]): TablaDelCatalogo[] {
  const termino = normalizar(busqueda)
  return (catalogo?.tablas ?? [])
    .filter((t) => esquemas.length === 0 || esquemas.includes(t.esquema))
    .filter((t) => !termino || normalizar(textoBuscable(t)).includes(termino))
    .sort((a, b) => a.esquema.localeCompare(b.esquema) || a.tabla.localeCompare(b.tabla))
}

function textoBuscable(t: TablaDelCatalogo): string {
  // Los nombres van también con espacios: «obligacion aporte» encuentra `obligacion_aporte`.
  const nombres = [t.tabla, ...t.columnas.map((c) => c.nombre)].flatMap((n) => [n, n.replaceAll('_', ' ')])
  return [t.esquema, ...nombres, t.negocio.queEs ?? ''].join(' ')
}

export function filasDeTablas(tablas: readonly TablaDelCatalogo[]): FilaDeTabla[] {
  return tablas.map((t) => ({
    id: `${t.esquema}.${t.tabla}`,
    tabla: t.tabla,
    esquema: t.esquema,
    servicio: t.servicio ?? 'Compartido',
    columnas: String(t.columnas.length),
    queEs: t.negocio.queEs ?? 'Sin ficha',
  }))
}

/** La página pedida dentro de la lista filtrada (20 por página); fuera de rango, la última que existe. */
export function pagina<T>(filas: readonly T[], numero: number, porPagina = 20): T[] {
  const ultima = Math.max(1, Math.ceil(filas.length / porPagina))
  const n = Math.min(Math.max(1, numero), ultima)
  return filas.slice((n - 1) * porPagina, n * porPagina)
}

const SENSIBILIDAD: Record<ColumnaDelCatalogo['sensibilidad'], string> = {
  NINGUNA: 'Ninguna',
  PERSONAL: 'Dato personal',
  FINANCIERO: 'Dato financiero',
  SECRETO: 'Secreto',
  DERIVADO: 'Derivado de un secreto (hash)',
}

/** La sensibilidad en palabras: la ficha no la transmite solo con color (95.4.6). */
export const sensibilidadEnPalabras = (c: ColumnaDelCatalogo): string => SENSIBILIDAD[c.sensibilidad]

/** Una fila de la tabla de columnas de la ficha. */
export type FilaDeColumna = { id: string; nombre: string; tipo: string; negocio: string; sistema: string; sensibilidad: string }

export function filasDeColumnas(t: TablaDelCatalogo): FilaDeColumna[] {
  return t.columnas.map((c) => ({
    id: c.nombre,
    nombre: c.nombre,
    tipo: c.tipo,
    negocio: c.negocio ? (c.origenNegocio === ColumnaDelCatalogoOrigenNegocioEnum.Convencion ? `${c.negocio} (convención)` : c.negocio) : 'Sin nota de negocio',
    sistema: c.sistema,
    sensibilidad: sensibilidadEnPalabras(c),
  }))
}
