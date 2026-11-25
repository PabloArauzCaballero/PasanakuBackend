/**
 * El menú «Más» de una fila: va en la capa superior del navegador (`popover`) y se ubica fijo junto a su botón,
 * alineado a su borde derecho; si abajo no entra, arriba. Se mide en su ancho natural (arrancando en la esquina):
 * pegado al borde derecho el menú `fixed` se encoge y parte el texto en varias líneas.
 */
export function ubicarMenuDeFila(d: HTMLDetailsElement): void {
  const menu = d.querySelector<HTMLElement>('.menu')
  if (!menu) return
  if (!d.open) {
    if (menu.matches(':popover-open')) menu.hidePopover()
    return
  }
  if (!menu.matches(':popover-open')) menu.showPopover()
  const boton = d.querySelector('summary')!.getBoundingClientRect()
  const separacion = 4
  menu.style.left = '0'
  menu.style.top = '0'
  const alto = menu.offsetHeight
  const ancho = menu.offsetWidth
  const arriba = boton.bottom + separacion + alto > window.innerHeight
  menu.style.top = `${arriba ? boton.top - separacion - alto : boton.bottom + separacion}px`
  menu.style.left = `${Math.max(separacion, Math.min(boton.right - ancho, window.innerWidth - ancho - separacion))}px`
}
