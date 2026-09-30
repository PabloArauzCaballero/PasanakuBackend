/**
 * `validarConfiguracion` — la función pura y compartida que decide si una URL de
 * gateway es utilizable, sin efectos y sin depender de ningún framework (D-A6).
 *
 * Historia: los tres `gateway.ts` (backoffice, web, y su equivalente en la app) caían
 * en silencio a `http://localhost:4010/api/v1` cuando faltaba la etiqueta/variable de
 * configuración. En el navegador de un operador, "localhost" es **su propia máquina**:
 * un despliegue mal configurado terminaba mandando tráfico financiero a donde sea que
 * corra el navegador, sin ningún aviso. Esta función es el único lugar donde se decide
 * qué es una configuración válida; los `gateway.ts` de cada app solo la llaman y, si
 * es inválida, devuelven vacío — **nunca** un valor por omisión que apunte a algún host.
 *
 * Reglas (D-A6):
 *  1. En producción, se acepta una ruta relativa (mismo origen implícito) o una URL
 *     absoluta de exactamente el mismo origen que `origenPropio` (con TLS).
 *  2. `localhost` y `127.0.0.1` (o cualquier variante de loopback) nunca se aceptan,
 *     salvo en modo `desarrollo` — ahí es Prism, el simulado local (`yarn dev:mock`).
 *  3. La ruta debe terminar en un segmento de versión (`/v1`, `/v2`, ...): sin eso no
 *     hay contrato al que apuntar.
 *  4. Fuera del origen propio, solo se acepta un host de la lista explícita
 *     `hostsPermitidos` (quién la carga: infra, en el despliegue — Q-J4).
 */

/** Los tres modos posibles en los que se resuelve el gateway. `demo` es *dato*, no red: ver `proveedor-fuentes.ts`. */
export type ModoGateway = 'produccion' | 'desarrollo'

export type OpcionesValidacionGateway = {
  /** `produccion`: TLS y mismo origen (o lista) obligatorios. `desarrollo`: se acepta loopback (Prism). */
  readonly modo: ModoGateway
  /** El origen del propio front (`location.origin` en el navegador; la URL pública del sitio en SSR). */
  readonly origenPropio: string
  /** Hosts ajenos al origen propio explícitamente permitidos (infra los carga en el despliegue). */
  readonly hostsPermitidos?: readonly string[]
}

export type ResultadoValidacionGateway =
  | { readonly valida: true; readonly url: string }
  | { readonly valida: false; readonly motivo: MotivoRechazo }

export type MotivoRechazo =
  | 'vacia'
  | 'esquema-no-soportado'
  | 'esquema-relativo-a-protocolo'
  | 'localhost-no-permitido'
  | 'sin-tls-en-produccion'
  | 'host-ajeno'
  | 'sin-ruta-versionada'
  | 'formato-invalido'

const ESQUEMAS_SOPORTADOS = new Set(['http:', 'https:'])
const LOOPBACK = new Set(['localhost', '127.0.0.1', '::1', '0.0.0.0'])
const RUTA_VERSIONADA = /\/v\d+\/?$/

function sinBarraFinal(ruta: string): string {
  return ruta.length > 1 && ruta.endsWith('/') ? ruta.slice(0, -1) : ruta
}

function normalizarOrigen(origen: string): string {
  try {
    const u = new URL(origen)
    return `${u.protocol}//${u.host}`.toLowerCase()
  } catch {
    return origen.trim().toLowerCase().replace(/\/$/, '')
  }
}

function normalizarHosts(hosts: readonly string[] | undefined): readonly string[] {
  return (hosts ?? []).map((h) => h.trim().toLowerCase()).filter((h) => h.length > 0)
}

function esLoopback(hostname: string): boolean {
  const h = hostname.toLowerCase()
  return LOOPBACK.has(h) || h.startsWith('127.')
}

export function validarConfiguracion(url: string | null | undefined, opciones: OpcionesValidacionGateway): ResultadoValidacionGateway {
  const cruda = (url ?? '').trim()
  if (cruda.length === 0) return { valida: false, motivo: 'vacia' }

  // "//otro-host/..." hereda el protocolo de la página: nunca se acepta a ciegas, sea
  // cual sea el origen — es la forma que un atacante usa para camuflar un host ajeno.
  if (cruda.startsWith('//')) return { valida: false, motivo: 'esquema-relativo-a-protocolo' }

  // Ruta relativa: mismo origen implícito. Solo se exige la versión al final.
  if (cruda.startsWith('/')) {
    const ruta = sinBarraFinal(cruda)
    if (!RUTA_VERSIONADA.test(ruta)) return { valida: false, motivo: 'sin-ruta-versionada' }
    return { valida: true, url: ruta }
  }

  let analizada: URL
  try {
    analizada = new URL(cruda)
  } catch {
    return { valida: false, motivo: 'formato-invalido' }
  }

  if (!ESQUEMAS_SOPORTADOS.has(analizada.protocol)) return { valida: false, motivo: 'esquema-no-soportado' }

  const hostname = analizada.hostname.toLowerCase()
  if (esLoopback(hostname) && opciones.modo !== 'desarrollo') return { valida: false, motivo: 'localhost-no-permitido' }

  const origenPropioNormalizado = normalizarOrigen(opciones.origenPropio)
  const origenDeUrl = `${analizada.protocol}//${analizada.host}`.toLowerCase()
  const esMismoOrigen = origenPropioNormalizado.length > 0 && origenDeUrl === origenPropioNormalizado
  const hostsPermitidos = normalizarHosts(opciones.hostsPermitidos)
  const esHostPermitido = hostsPermitidos.includes(hostname)
  const esLoopbackEnDesarrollo = esLoopback(hostname) && opciones.modo === 'desarrollo'

  if (opciones.modo === 'produccion' && analizada.protocol !== 'https:' && !esLoopbackEnDesarrollo) {
    return { valida: false, motivo: 'sin-tls-en-produccion' }
  }

  if (!esMismoOrigen && !esHostPermitido && !esLoopbackEnDesarrollo) {
    return { valida: false, motivo: 'host-ajeno' }
  }

  const ruta = sinBarraFinal(analizada.pathname)
  if (!RUTA_VERSIONADA.test(ruta)) return { valida: false, motivo: 'sin-ruta-versionada' }

  return { valida: true, url: sinBarraFinal(cruda) }
}
