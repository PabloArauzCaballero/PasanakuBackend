import { Injectable, signal } from '@angular/core'

/**
 * **Lo único que el shell necesita saber de los tutoriales: si hay uno corriendo.**
 *
 * Existe para que `ShellFinanciero` —que se monta en todas las pantallas— no tenga que
 * importar el motor. Con esta señal, el motor entero, el velo, el globo y el catálogo
 * viajan en paquetes aparte que se traen recién cuando alguien pide ayuda; el arranque
 * del backoffice paga por esto unas pocas líneas, no un módulo.
 *
 * La escribe el motor y la lee el shell. Nadie más.
 */
@Injectable({ providedIn: 'root' })
export class EncendidoDeTutorial {
  readonly activo = signal(false)
}
