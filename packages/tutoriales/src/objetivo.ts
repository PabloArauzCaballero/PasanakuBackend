import { DOCUMENT } from '@angular/common'
import { Injectable, inject } from '@angular/core'

/** El atributo con el que la interfaz se deja señalar. Nunca una clase de CSS. */
export const ATRIBUTO = 'data-tutorial-id'

/** Cuánto se espera por omisión a un elemento que llega después de una petición. */
export const ESPERA_POR_OMISION = 4000

/** El recuadro del elemento en la ventana, en píxeles de pantalla. */
export interface Recuadro {
  readonly x: number
  readonly y: number
  readonly ancho: number
  readonly alto: number
}

/**
 * **Encontrar el elemento que el paso señala, incluso si todavía no existe.**
 *
 * Un backoffice pinta casi todo después de una petición: la fila de una tabla, el panel
 * de un expediente, el contenido de un modal. Buscar una sola vez y rendirse es cómo un
 * tour se rompe en la primera pantalla con datos. Acá se busca, y si no está, se
 * **observa el DOM** hasta que aparezca o hasta que se acabe el plazo.
 *
 * Vive en `nucleo/` porque toca el DOM directamente, que es justo lo que un componente
 * no debe hacer. El motor le pregunta; nunca busca por su cuenta.
 */
@Injectable({ providedIn: 'root' })
export class LocalizadorDeObjetivo {
  private readonly doc = inject(DOCUMENT)

  /** Lo que hay AHORA mismo, sin esperar. */
  buscar(id: string): HTMLElement | null {
    return this.doc.querySelector<HTMLElement>(`[${ATRIBUTO}="${escapar(id)}"]`)
  }

  /**
   * Espera a que aparezca. Devuelve `null` al vencerse el plazo — **nunca lanza**: un
   * elemento que no está es un problema del tutorial, no una excepción de la aplicación,
   * y quien decide qué hacer con eso es el motor.
   */
  esperar(id: string, ms = ESPERA_POR_OMISION): Promise<HTMLElement | null> {
    const yaEsta = this.buscar(id)
    if (yaEsta !== null) return Promise.resolve(yaEsta)
    const cuerpo = this.doc.body
    if (typeof MutationObserver === 'undefined') return Promise.resolve(null)
    return new Promise((resolver) => {
      const observador = new MutationObserver(() => {
        const encontrado = this.buscar(id)
        if (encontrado === null) return
        cerrar()
        resolver(encontrado)
      })
      const reloj = setTimeout(() => {
        cerrar()
        resolver(this.buscar(id))
      }, ms)
      const cerrar = () => {
        clearTimeout(reloj)
        observador.disconnect()
      }
      observador.observe(cuerpo, { childList: true, subtree: true, attributes: true, attributeFilter: [ATRIBUTO] })
    })
  }

  /** El recuadro del elemento. `null` si dejó de estar en pantalla. */
  recuadroDe(elemento: HTMLElement): Recuadro | null {
    const r = elemento.getBoundingClientRect()
    if (r.width === 0 && r.height === 0) return null
    return { x: r.left, y: r.top, ancho: r.width, alto: r.height }
  }

  /**
   * Trae el elemento a la vista. Es la única acción automática que el motor ejecuta
   * sobre la aplicación, y no cambia ningún dato: desplaza y, si el paso lo pide,
   * enfoca. Respeta `prefers-reduced-motion` porque un desplazamiento suave le revuelve
   * el estómago a más gente de la que uno cree.
   */
  acercar(elemento: HTMLElement, enfocar: boolean): void {
    // Todo se pregunta antes de usarlo: `matchMedia` y `scrollIntoView` no existen en
    // todos los entornos —un jsdom, un renderizado en servidor— y acercar un elemento
    // no puede ser la razón por la que un tutorial se caiga.
    const ventana = this.doc.defaultView
    const quieto = typeof ventana?.matchMedia === 'function' && ventana.matchMedia('(prefers-reduced-motion: reduce)').matches
    if (typeof elemento.scrollIntoView === 'function') {
      elemento.scrollIntoView({ behavior: quieto ? 'auto' : 'smooth', block: 'center', inline: 'nearest' })
    }
    if (enfocar && typeof elemento.focus === 'function') elemento.focus({ preventScroll: true })
  }
}

/**
 * El id viaja dentro de un selector de atributo entre comillas: alcanza con escapar la
 * comilla y la barra invertida. No se usa `CSS.escape` porque no existe en el entorno
 * de pruebas, y una función que solo anda en el navegador es una que no se prueba.
 */
const escapar = (id: string): string => id.replace(/["\\]/g, '\\$&')
