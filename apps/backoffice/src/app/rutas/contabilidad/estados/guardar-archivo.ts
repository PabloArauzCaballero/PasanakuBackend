/**
 * Entrega al navegador un archivo que ya llegó del servidor (un `Blob`), con el nombre que se le pasa. No hay red acá:
 * el documento lo trajo `crearDescarga` y ya se comparó su hash. Devuelve `false` si el entorno no puede guardar
 * (sin `document` o sin `URL.createObjectURL`, como en el render del servidor), y la pantalla lo dice.
 */
export function guardarArchivo(archivo: Blob, nombre: string): boolean {
  if (typeof document === 'undefined' || typeof URL === 'undefined' || typeof URL.createObjectURL !== 'function') return false
  const url = URL.createObjectURL(archivo)
  const a = document.createElement('a')
  a.href = url
  a.download = nombre
  a.rel = 'noopener'
  document.body.appendChild(a)
  a.click()
  a.remove()
  setTimeout(() => URL.revokeObjectURL(url), 0)
  return true
}
