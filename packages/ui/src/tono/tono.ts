/**
 * Los cinco tonos semánticos que un componente puede pedir. Cada uno resuelve a un par
 * (fondo, frente) de roles del tema: el componente nunca elige un color.
 */
export type Tono = 'ok' | 'aviso' | 'error' | 'info' | 'neutro'

export const TONOS: readonly Tono[] = ['ok', 'aviso', 'error', 'info', 'neutro']

/** Variables CSS del tono: relleno y frente (texto/ícono/borde), siempre AA en el par. */
export function coloresDe(tono: Tono): { fondo: string; frente: string } {
  switch (tono) {
    case 'ok':
      return { fondo: 'var(--ok-bg)', frente: 'var(--ok-texto)' }
    case 'aviso':
      return { fondo: 'var(--warn-bg)', frente: 'var(--aviso-texto)' }
    case 'error':
      return { fondo: 'var(--err-bg)', frente: 'var(--err-texto)' }
    case 'info':
      return { fondo: 'var(--info-bg)', frente: 'var(--info)' }
    case 'neutro':
      return { fondo: 'var(--surface-2)', frente: 'var(--text-2)' }
  }
}
