import type { Router } from '@angular/router'
import { NavigationEnd } from '@angular/router'
import { filter } from 'rxjs'
import type { LocalizadorDeObjetivo } from './objetivo'
import type { AccionEsperada, PasoDeTutorial } from './tipos'

/** Todo lo que hace falta para mirar sin tocar. */
export interface Contexto {
  readonly elemento: HTMLElement | null
  readonly router: Router
  readonly localizador: LocalizadorDeObjetivo
}

/** Cuántos caracteres cuentan como «escribió algo», si el paso no dice otra cosa. */
const MINIMO_POR_OMISION = 1

/**
 * **Mirar si la persona hizo lo que el paso pedía, sin hacerlo por ella.**
 *
 * Se enganchan escuchas a lo que la aplicación ya emite —un clic, un `input`, un
 * `change`, una navegación— y se avisa cuando se cumplió. No se simula nada: si el
 * tutorial dice «pulsá Aprobar», tiene que pulsarlo una persona, y el servidor sigue
 * decidiendo si puede.
 *
 * Devuelve la función que suelta todas las escuchas. Llamarla dos veces es inofensivo.
 */
export function vigilarAccion(paso: PasoDeTutorial, contexto: Contexto, alCumplir: () => void): () => void {
  const accion: AccionEsperada = paso.accion ?? { tipo: 'ninguna' }
  switch (accion.tipo) {
    case 'ninguna':
      return sinNada
    case 'clic':
      return escuchar(contexto.elemento, 'click', alCumplir)
    case 'escribir':
      return escuchar(contexto.elemento, 'input', (e) => {
        if (valorDe(e.target).length >= (accion.minimo ?? MINIMO_POR_OMISION)) alCumplir()
      })
    case 'elegir':
      return escuchar(contexto.elemento, 'change', (e) => {
        if (valorDe(e.target).length > 0) alCumplir()
      })
    case 'navegar':
      return vigilarRuta(contexto.router, accion.ruta, alCumplir)
    case 'aparezca':
      return vigilarAparicion(contexto.localizador, accion.objetivo, paso.esperaMs, alCumplir)
  }
}

const sinNada = (): void => undefined

function escuchar(elemento: HTMLElement | null, evento: string, alPasar: (e: Event) => void): () => void {
  if (elemento === null) return sinNada
  elemento.addEventListener(evento, alPasar)
  return () => elemento.removeEventListener(evento, alPasar)
}

function vigilarRuta(router: Router, ruta: string, alCumplir: () => void): () => void {
  if (llegó(router.url, ruta)) {
    alCumplir()
    return sinNada
  }
  const suscripcion = router.events
    .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
    .subscribe((e) => {
      if (llegó(e.urlAfterRedirects, ruta)) alCumplir()
    })
  return () => suscripcion.unsubscribe()
}

function vigilarAparicion(localizador: LocalizadorDeObjetivo, objetivo: string, esperaMs: number | undefined, alCumplir: () => void): () => void {
  let vivo = true
  void localizador.esperar(objetivo, esperaMs).then((encontrado) => {
    if (vivo && encontrado !== null) alCumplir()
  })
  return () => {
    vivo = false
  }
}

function llegó(url: string, ruta: string): boolean {
  const limpia = url.split('?')[0] ?? url
  return limpia === ruta || limpia.startsWith(`${ruta}/`)
}

function valorDe(destino: EventTarget | null): string {
  return destino !== null && 'value' in destino && typeof destino.value === 'string' ? destino.value : ''
}
