import { DestroyRef, inject, signal, type Signal } from '@angular/core'

/**
 * ¿El navegador dice que no hay red? Se actualiza con los eventos `online`/`offline` y se desuscribe
 * solo al destruirse el componente. Se llama en un campo de la clase (contexto de inyección).
 *
 * Es una pista, no una verdad: `navigator.onLine` solo sabe si hay una interfaz de red, no si el gateway
 * responde. Sirve para avisar «estás sin conexión»; lo que decide si una operación llegó es la consulta
 * al servidor, nunca esta señal.
 */
export function senalDeRed(): Signal<boolean> {
  const sinRed = signal(typeof navigator !== 'undefined' ? !navigator.onLine : false)
  if (typeof window === 'undefined') return sinRed
  const sinSenal = () => sinRed.set(true)
  const conSenal = () => sinRed.set(false)
  window.addEventListener('offline', sinSenal)
  window.addEventListener('online', conSenal)
  inject(DestroyRef).onDestroy(() => {
    window.removeEventListener('offline', sinSenal)
    window.removeEventListener('online', conSenal)
  })
  return sinRed.asReadonly()
}
