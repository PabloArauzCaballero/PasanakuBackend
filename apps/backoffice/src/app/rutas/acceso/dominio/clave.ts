/**
 * Lo que la pantalla puede comprobar de una clave **sin ser la política**: el piso de largo (`identidad.clave.largo-minimo`,
 * nunca menor a 8) y que la repetición coincida. Qué más exige el servidor (no contener el teléfono ni el documento, no
 * repetir claves anteriores) solo lo sabe él: llega como `AP-CU09-02` / `AP-CU09-03` y se muestra en el campo.
 */
export const LARGO_MINIMO_DE_CLAVE = 8

export type Requisito = { id: string; texto: string; cumplido: boolean }

export function requisitosDeClave(nueva: string, repetida: string, actual?: string): Requisito[] {
  const lista: Requisito[] = [
    { id: 'largo', texto: `Al menos ${LARGO_MINIMO_DE_CLAVE} caracteres`, cumplido: nueva.length >= LARGO_MINIMO_DE_CLAVE },
    { id: 'repeticion', texto: 'La repetición coincide', cumplido: nueva.length > 0 && nueva === repetida },
  ]
  if (actual !== undefined) lista.splice(1, 0, { id: 'distinta', texto: 'Es distinta de la actual', cumplido: nueva.length > 0 && nueva !== actual })
  return lista
}

export const claveValida = (nueva: string, repetida: string, actual?: string): boolean => requisitosDeClave(nueva, repetida, actual).every((r) => r.cumplido)
