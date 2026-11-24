import { signal } from '@angular/core'
import { crearResolverNombresDeOperadores, MAXIMO_DE_OPERADORES_POR_LOTE } from '../dominio/cu132-parametros'

type TextosDeOperador = { buscandoOperador: string; operadorNoDisponible: string }

/**
 * Quién propuso y quién aprobó, con nombre. Una llamada a identidad por carga, solo por los ids que todavía no
 * tienen nombre. Si falla, o un id no vuelve (no es un operador interno activo), se dice «no disponible» y la
 * pantalla sigue: el nombre ayuda a leer, la firma la sostiene el identificador que guarda la auditoría.
 * Se crea en un contexto de inyección (lo usa el constructor de la pantalla).
 */
export function crearNombresDeOperadores(textos: TextosDeOperador) {
  const resolver = crearResolverNombresDeOperadores()
  const nombres = signal<Record<string, string>>({})
  const resolviendo = signal<Record<string, boolean>>({})

  function resolverIds(ids: string[]): void {
    const pendientes = ids.filter((id) => !nombres()[id] && !resolviendo()[id]).slice(0, MAXIMO_DE_OPERADORES_POR_LOTE)
    if (!pendientes.length) return
    const marcar = (valor: boolean) => resolviendo.update((r) => ({ ...r, ...Object.fromEntries(pendientes.map((id) => [id, valor])) }))
    marcar(true)
    resolver(pendientes).subscribe({
      next: (lista) => {
        nombres.update((n) => ({ ...n, ...Object.fromEntries(lista.map((o) => [o.usuarioId, o.nombre])) }))
        marcar(false)
      },
      error: () => marcar(false),
    })
  }

  return {
    resolver: resolverIds,
    disponible: (id: string): boolean => Boolean(nombres()[id]),
    nombreDe: (id: string): string => nombres()[id] ?? (resolviendo()[id] ? textos.buscandoOperador : textos.operadorNoDisponible),
  }
}
