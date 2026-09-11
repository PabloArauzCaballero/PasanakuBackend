/**
 * SHA-256 sobre `SubtleCrypto`, disponible tanto en el navegador como en el Node del
 * SSR (`globalThis.crypto.subtle` desde Node 19). Un solo punto para no repetir la
 * conversión a hexadecimal en cada átomo.
 */
export async function sha256Hex(texto: string): Promise<string> {
  const bytes = new TextEncoder().encode(texto)
  const digest = await crypto.subtle.digest('SHA-256', bytes)
  return Array.from(new Uint8Array(digest), (b) => b.toString(16).padStart(2, '0')).join('')
}

/** Igual que {@link sha256Hex} pero devuelve el dígest crudo, para componerlo con otro hash. */
export async function sha256Bytes(texto: string): Promise<Uint8Array> {
  const bytes = new TextEncoder().encode(texto)
  return new Uint8Array(await crypto.subtle.digest('SHA-256', bytes))
}

/** `SHA-256(texto) mod modulo`, leyendo el dígest como entero sin signo grande-endian. */
export async function indiceDelPaso(semilla: string, paso: number, modulo: number): Promise<number> {
  const digest = await sha256Bytes(`${semilla}:${paso}`)
  let n = 0n
  for (const byte of digest) n = (n << 8n) | BigInt(byte)
  return Number(n % BigInt(modulo))
}
