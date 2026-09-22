import { indiceDelPaso, sha256Hex } from './sha256'

/**
 * Reimplementación en TypeScript de `plataforma/comun-dominio` ·
 * `bo.aportaya.plataforma.dominio.SorteoVerificable` (Java), que es la única
 * implementación que sortea (CU-60) y la que verifica en el backend (CU-61).
 *
 * **El protocolo se copió byte a byte de esa clase, no se reinventó:**
 * 1. Preimagen canónica: `semilla` seguida de cada entropía, en el orden en que se
 *    aportaron, separadas por `\n`, codificadas en UTF-8.
 * 2. Compromiso: `hash_semilla = SHA-256(preimagen)`, hexadecimal minúscula.
 * 3. Barajado: Fisher-Yates desde el final. El índice del paso `i` es
 *    `SHA-256(semilla + ":" + i) mod (i + 1)`, leído como entero sin signo.
 *
 * Los vectores de `vectores/sorteo.vectores.json` prueban que esta reimplementación
 * coincide con el algoritmo del backend — ver el README de `vectores/` sobre su origen.
 */

export async function hashDelCompromiso(semilla: string, entropias: readonly string[] = []): Promise<string> {
  if (!semilla) throw new Error('la semilla es obligatoria')
  let preimagen = semilla
  for (const entropia of entropias) {
    if (!entropia) throw new Error('una entropía vacía no se puede comprometer')
    preimagen += `\n${entropia}`
  }
  return sha256Hex(preimagen)
}

export async function verificarCompromiso(
  semilla: string | undefined,
  entropias: readonly string[] | undefined,
  hashComprometido: string | undefined,
): Promise<boolean> {
  if (!semilla || !hashComprometido) return false
  const recomputado = await hashDelCompromiso(semilla, entropias ?? [])
  // El hash es público; comparar con `===` es aceptable acá porque no hay secreto que
  // filtrar por temporización (a diferencia de una clave). Se documenta la diferencia
  // con el backend, que sí usa comparación en tiempo constante sobre material propio.
  return recomputado === hashComprometido
}

export async function barajarDeterminista<T>(semilla: string, cupos: readonly T[]): Promise<T[]> {
  if (!semilla) throw new Error('la semilla es obligatoria')
  if (!cupos) throw new Error('no hay cupos que ordenar')
  const orden = [...cupos]
  for (let i = orden.length - 1; i > 0; i--) {
    const j = await indiceDelPaso(semilla, i, i + 1)
    ;[orden[i], orden[j]] = [orden[j] as T, orden[i] as T]
  }
  return orden
}
