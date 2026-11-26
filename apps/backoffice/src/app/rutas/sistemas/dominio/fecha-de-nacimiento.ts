/**
 * La regla de la fecha de nacimiento del personal interno (GU-17, decisión de Pablo 2026-11-26): obligatoria, no futura
 * y de alguien que ya cumplió 18 años. Es la misma que aplica el servidor (`AP-CU08-13` y `AP-CU08-14`); acá solo
 * adelanta el error al campo, no autoriza nada.
 */
export const EDAD_MINIMA = 18

/** Hoy (o hoy menos `anios`) como `AAAA-MM-DD` en hora local, el valor de un `<input type="date">`. */
export function hoy(anios = 0): string {
  const d = new Date()
  d.setFullYear(d.getFullYear() - anios)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/** El error de un valor `AAAA-MM-DD`; vacío no es error de campo (un formulario vacío no arranca en rojo). */
export function errorDeFechaDeNacimiento(valor: string): string | undefined {
  if (valor === '') return undefined
  if (valor > hoy()) return 'La fecha de nacimiento no puede ser futura.'
  return valor > hoy(EDAD_MINIMA) ? `El personal interno tiene que ser mayor de ${EDAD_MINIMA} años.` : undefined
}
