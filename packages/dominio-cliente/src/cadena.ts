import { sha256Hex } from './sha256'

/**
 * `serializarCanonico` y `hashDeBloque` — los átomos que CU-72 y CU-73 exigen
 * compartidos entre sellar y verificar (planes/14 F9.3).
 *
 * **Supuesto declarado (regla cero):** al momento de escribir esto no existe en
 * `servicios/transparencia` una clase Java `serializarCanonico`/`hashDeBloque`
 * equivalente a `SorteoVerificable` — CU-72 describe el contrato ("orden fijo de
 * campos, importes como cadena, fechas en UTC") pero no fija el formato byte a byte
 * como sí lo hace `SorteoVerificable` para el sorteo. Mientras eso no exista:
 *
 * - Se implementa acá una serialización canónica razonable (JSON con claves
 *   ordenadas alfabéticamente, recursiva, sin espacios) y `hashBloque =
 *   SHA-256(numeroBloque + "|" + hashAnterior + "|" + hashContenido)`, que es
 *   exactamente la fórmula que sí publica CU-72 (`hash_bloque = SHA256(numero ||
 *   hash_anterior || hash_contenido)`).
 * - **No se garantiza que esto coincida byte a byte con lo que calcule el
 *   backend** hasta que `servicios/transparencia` publique su propia clase y sus
 *   vectores dorados (igual que hizo `plataforma/comun-dominio` para el sorteo).
 * - Pedido al backend: extraer `serializarCanonico`/`hashDeBloque` de CU72 a
 *   `plataforma/comun-dominio`, publicar sus vectores en JSON, y esta clase pasa a
 *   consumirlos exactamente como hace `sorteo.ts`. Carril destino: quien tenga
 *   `servicios/transparencia` (carril de backend de transparencia, ola de F1
 *   backend).
 *
 * Hasta entonces, `VerificadorDeCadena` (la página `/publico/grupos/:codigo`) muestra
 * el veredicto del **servidor** (`verificarCadena`, CU-73, que sí corre en el backend
 * real) como fuente de verdad, y usa esta reimplementación solo para la recomputación
 * *ilustrativa* en el cliente, marcada explícitamente como tal en la UI.
 */

export type ValorCanonico = string | number | boolean | null | { readonly [clave: string]: ValorCanonico } | readonly ValorCanonico[]

export function serializarCanonico(valor: ValorCanonico): string {
  if (valor === null || typeof valor !== 'object') return JSON.stringify(valor)
  if (Array.isArray(valor)) return `[${valor.map(serializarCanonico).join(',')}]`
  const claves = Object.keys(valor).sort()
  const campos = claves.map((clave) => `${JSON.stringify(clave)}:${serializarCanonico((valor as Record<string, ValorCanonico>)[clave] as ValorCanonico)}`)
  return `{${campos.join(',')}}`
}

export async function hashDeContenido(contenido: ValorCanonico): Promise<string> {
  return sha256Hex(serializarCanonico(contenido))
}

export async function hashDeBloque(numeroBloque: number, hashAnterior: string | null, hashContenido: string): Promise<string> {
  return sha256Hex(`${numeroBloque}|${hashAnterior ?? ''}|${hashContenido}`)
}

export type BloqueParaVerificar = {
  readonly numeroBloque: number
  readonly hashAnterior: string | null
  readonly hashContenido: string
  readonly hashBloque: string
  readonly contenido?: ValorCanonico
}

export type ResultadoCadena = {
  readonly integra: boolean
  readonly bloquesVerificados: number
  readonly primerBloqueFallido: number | null
  readonly componenteFallido: 'HASH_CONTENIDO' | 'HASH_ANTERIOR' | 'HASH_BLOQUE' | 'SECUENCIA' | null
}

/** Recorre la cadena en orden y devuelve el primer fallo, igual que CU-73 lo exige del backend. */
export async function recorrerCadena(bloques: readonly BloqueParaVerificar[]): Promise<ResultadoCadena> {
  let anterior: BloqueParaVerificar | undefined
  for (let i = 0; i < bloques.length; i++) {
    const bloque = bloques[i] as BloqueParaVerificar
    if (anterior && bloque.numeroBloque !== anterior.numeroBloque + 1) {
      return { integra: false, bloquesVerificados: i, primerBloqueFallido: bloque.numeroBloque, componenteFallido: 'SECUENCIA' }
    }
    if (bloque.contenido !== undefined) {
      const hashContenido = await hashDeContenido(bloque.contenido)
      if (hashContenido !== bloque.hashContenido) {
        return { integra: false, bloquesVerificados: i, primerBloqueFallido: bloque.numeroBloque, componenteFallido: 'HASH_CONTENIDO' }
      }
    }
    if (anterior && bloque.hashAnterior !== anterior.hashBloque) {
      return { integra: false, bloquesVerificados: i, primerBloqueFallido: bloque.numeroBloque, componenteFallido: 'HASH_ANTERIOR' }
    }
    const hashBloque = await hashDeBloque(bloque.numeroBloque, bloque.hashAnterior, bloque.hashContenido)
    if (hashBloque !== bloque.hashBloque) {
      return { integra: false, bloquesVerificados: i, primerBloqueFallido: bloque.numeroBloque, componenteFallido: 'HASH_BLOQUE' }
    }
    anterior = bloque
  }
  return { integra: true, bloquesVerificados: bloques.length, primerBloqueFallido: null, componenteFallido: null }
}
