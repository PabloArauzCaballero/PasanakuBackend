/**
 * Los tonos semánticos que un componente puede pedir. Cada uno resuelve a un par
 * (fondo, frente) de roles del tema: el componente nunca elige un color.
 *
 * `pii` es la señal de datos personales (morado, como en el portal de Atlas: ni aviso ni
 * error, porque no es un problema, es una categoría); `apagado` es un neutro más tenue para
 * lo que no pide atención (una insignia «Detectada», un «No»).
 */
export type Tono = 'ok' | 'aviso' | 'error' | 'info' | 'neutro' | 'pii' | 'apagado'

export const TONOS: readonly Tono[] = ['ok', 'aviso', 'error', 'info', 'neutro', 'pii', 'apagado']

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
      return { fondo: 'var(--info-bg)', frente: 'var(--info-texto)' }
    case 'neutro':
      return { fondo: 'var(--surface-2)', frente: 'var(--text-2)' }
    case 'pii':
      return { fondo: 'var(--pii-bg)', frente: 'var(--pii-texto)' }
    case 'apagado':
      return { fondo: 'var(--surface-2)', frente: 'var(--text-3)' }
  }
}
