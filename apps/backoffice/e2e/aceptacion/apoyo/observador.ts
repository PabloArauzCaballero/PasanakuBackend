import type { Page, Request } from '@playwright/test'

export type Peticion = { metodo: string; url: string; cuerpo: string; cabeceras: string; estado?: number; fallo?: string }

/**
 * Consola y red de UNA página (de un actor). Regla 80.3.9: errores de consola y 4xx/5xx inesperados HACEN FALLAR el test;
 * lo esperado se declara en `permitir` (con la razón), no se ignora en silencio.
 */
export class Observador {
  readonly consola: string[] = []
  readonly peticiones: Peticion[] = []
  private readonly porSolicitud = new Map<Request, Peticion>()

  constructor(page: Page, readonly nombre: string) {
    page.on('console', (m) => {
      if (m.type() === 'error') this.consola.push(`console.error: ${m.text()}`)
    })
    page.on('pageerror', (e) => this.consola.push(`pageerror: ${e.message}`))
    page.on('request', (r) => {
      const p: Peticion = { metodo: r.method(), url: r.url(), cuerpo: r.postData() ?? '', cabeceras: JSON.stringify(r.headers()) }
      this.peticiones.push(p)
      this.porSolicitud.set(r, p)
    })
    page.on('response', (resp) => {
      const p = this.porSolicitud.get(resp.request())
      if (p) p.estado = resp.status()
    })
    page.on('requestfailed', (r) => {
      const p = this.porSolicitud.get(r)
      if (p) p.fallo = r.failure()?.errorText ?? 'fallo'
    })
  }

  /** Marca desde dónde mirar (para aislar los pasos de una fase). */
  marca(): number {
    return this.peticiones.length
  }

  /** ¿Algún texto de petición (url, cuerpo, cabeceras) contiene el secreto? */
  conSecreto(secreto: string, desde = 0): Peticion[] {
    return this.peticiones.slice(desde).filter((p) => `${p.url} ${p.cuerpo} ${p.cabeceras}`.includes(secreto))
  }

  /** Respuestas 4xx/5xx o caídas de red que no estén declaradas como esperadas. */
  inesperadas(permitir: Permitida[] = [], desde = 0): string[] {
    return this.peticiones
      .slice(desde)
      .filter((p) => (p.estado !== undefined && p.estado >= 400) || p.fallo !== undefined)
      .filter((p) => !permitir.some((a) => a.url.test(p.url) && (a.estados === undefined || (p.estado !== undefined && a.estados.includes(p.estado)))))
      .map((p) => `${p.metodo} ${p.url.replace(/\?.*$/, '')} -> ${p.estado ?? p.fallo}`)
  }

  /**
   * Mensajes de consola inesperados. Chromium escribe «Failed to load resource: ... status of NNN» por cada respuesta 4xx/5xx: cada uno se
   * descuenta SOLO contra una petición real de esa misma respuesta que esté declarada en `permitir` (se empareja uno a uno, por conteo).
   */
  consolaInesperada(permitir: Permitida[] = []): string[] {
    const disponibles = this.peticiones.filter((p) => p.estado !== undefined && permitir.some((a) => a.url.test(p.url) && (a.estados === undefined || a.estados.includes(p.estado as number))))
    const usadas = new Set<Peticion>()
    return this.consola.filter((c) => {
      const estado = Number(/status of (\d{3})/.exec(c)?.[1])
      if (!estado) return true
      const par = disponibles.find((p) => p.estado === estado && !usadas.has(p))
      if (!par) return true
      usadas.add(par)
      return false
    })
  }
}

export type Permitida = { url: RegExp; estados?: number[]; razon: string }

/** Lo que el backoffice produce por diseño en cada ingreso: el refresco sin cookie (401) y el primer `POST /sesiones` sin factor (422 AP-CU04-03). */
export const PERMITIDAS_DEL_INGRESO: Permitida[] = [
  { url: /\/sesion\/refrescar/, estados: [401], razon: 'sin cookie de refresco el arranque termina anónimo (prepararArranque)' },
  { url: /\/sesiones$/, estados: [422], razon: 'identidad contesta AP-CU04-03 «falta el factor» y la pantalla pide el segundo paso' },
]
