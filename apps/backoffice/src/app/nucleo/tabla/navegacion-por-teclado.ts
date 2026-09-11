/**
 * La parte pura de la navegación fila a fila: qué índice corresponde según la tecla.
 * Separado del componente para que `tabla-de-datos-virtualizada.ts` no crezca con lógica
 * que no depende de Angular, y para poder probarlo sin montar la tabla.
 */
export type AccionDeTeclado = { tipo: 'mover'; indice: number } | { tipo: 'alternar' } | null

export function accionParaTecla(tecla: string, indiceActual: number, ultimoIndice: number, seleccionable: boolean): AccionDeTeclado {
  switch (tecla) {
    case 'ArrowDown':
      return { tipo: 'mover', indice: Math.min(indiceActual + 1, ultimoIndice) }
    case 'ArrowUp':
      return { tipo: 'mover', indice: Math.max(indiceActual - 1, 0) }
    case 'Home':
      return { tipo: 'mover', indice: 0 }
    case 'End':
      return { tipo: 'mover', indice: ultimoIndice }
    case ' ':
    case 'Enter':
      return seleccionable ? { tipo: 'alternar' } : null
    default:
      return null
  }
}

/**
 * Reintenta enfocar una fila por `data-fila-id`: tras `scrollToIndex` el DOM virtualizado
 * puede tardar un tick en traer esa fila al rango visible.
 */
function escaparAtributo(valor: string): string {
  return typeof CSS !== 'undefined' && CSS.escape ? CSS.escape(valor) : valor.replaceAll('"', '\\"')
}

export function enfocarFilaPorId(documento: Document, id: string, intentos = 5, esperaMs = 16): void {
  const intentar = (restantes: number) => {
    const el = documento.querySelector<HTMLElement>(`[data-fila-id="${escaparAtributo(id)}"]`)
    if (el) el.focus()
    else if (restantes > 0) setTimeout(() => intentar(restantes - 1), esperaMs)
  }
  setTimeout(() => intentar(intentos), 0)
}
