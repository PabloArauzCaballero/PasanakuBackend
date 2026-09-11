/** Tipos compartidos de la tabla de datos virtualizada. Los usan las 64 pantallas de F7 y F8. */
export type ColumnaVirtual<T> = {
  clave: keyof T & string
  titulo: string
  ordenable?: boolean
  numerica?: boolean
  /** Ancho de la columna en la grilla CSS; por omisión, `1fr`. */
  ancho?: string
}

export type OrdenVirtual = { clave: string; sentido: 'asc' | 'desc' }

export type PaginaServidor<T> = { filas: T[]; total: number }

export type PedidoDePagina = { pagina: number; tamano: number; orden: OrdenVirtual | null; filtros: Readonly<Record<string, string>> }

/** Quien usa la tabla trae el cargador: la tabla no sabe de HTTP ni de ningún servicio. */
export type CargadorDePagina<T> = (pedido: PedidoDePagina) => Promise<PaginaServidor<T>>
