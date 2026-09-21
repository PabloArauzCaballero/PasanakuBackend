import { InjectionToken } from '@angular/core'

/**
 * **Quién está aprendiendo.** Es lo único que el motor necesita saber de la sesión, y
 * el único lugar donde los dos productos web difieren de verdad.
 *
 * El backoffice lo cumple con `Sesion` (permisos del token). El sitio público lo cumple
 * con [[visitanteAnonimo]]: no hay cuenta, no hay permisos, y el avance se guarda por
 * navegador.
 */
export interface IdentidadDeQuienAprende {
  /**
   * Con qué se separa el avance de una persona del de otra en la misma máquina.
   * `null` es «no sé quién sos», que en el sitio público es lo normal.
   */
  sujeto(): string | null

  /**
   * ¿Alcanza este permiso? **Niega por omisión**: un permiso que nadie sabe traducir
   * no abre nada. Un tutorial sin `permisos` declarados no pregunta y se muestra igual.
   */
  puede(permiso: string): boolean
}

export const IDENTIDAD_DE_TUTORIALES = new InjectionToken<IdentidadDeQuienAprende>('IdentidadDeTutoriales')

/**
 * Quien entra al sitio público sin cuenta. No tiene permisos —ninguna pantalla del
 * sitio los pide— y su avance vive en este navegador y en ninguna otra parte.
 */
export const visitanteAnonimo: IdentidadDeQuienAprende = {
  sujeto: () => null,
  puede: () => false,
}
