/** Formas del contrato que la interfaz valida ANTES de enviar (el servidor vuelve a validar todo). */
export const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

export const esUuid = (valor: string): boolean => UUID.test(valor.trim())

/** `pattern: '^\d{4}-(0[1-9]|1[0-2])$'` de `EntradaGeneracionReporte.periodo`. */
export const esPeriodo = (valor: string): boolean => /^\d{4}-(0[1-9]|1[0-2])$/.test(valor.trim())

/**
 * Un `<input type="datetime-local">` da «2026-10-20T17:30» sin zona. Los plazos se cuentan en
 * La Paz (UTC−4 todo el año, sin horario de verano), así que se fija esa zona y no la del
 * navegador de quien carga.
 */
export function aIsoLaPaz(local: string): string | null {
  return /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(local) ? `${local}:00-04:00` : null
}

export type Errores<C extends string> = Partial<Record<C, string>>

export const sinErrores = (errores: Errores<string>): boolean => Object.keys(errores).length === 0
