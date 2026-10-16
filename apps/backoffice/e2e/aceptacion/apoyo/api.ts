import { randomUUID } from 'node:crypto'
import { request, type APIRequestContext } from '@playwright/test'
import { API } from './entorno'

/** Una cuenta sintética de la corrida. Las credenciales se generan en el momento: no están en el repositorio. */
export type Cuenta = { id?: string; telefono: string; clave: string; rol: string; factor?: string | (() => string) }
export type Respuesta<T = unknown> = { estado: number; cuerpo: T; texto: string }
export type ErrorApi = { codigo?: string; mensaje?: string; trazaId?: string }

export type DecisionApi = {
  id: string; solicitudId: string; fase: string; decision: string; actorId: string; motivo: string
  propuestaId?: string; participanteId?: string; revision: number; ocurridaEn: string
}

/** Cliente HTTP de UNA cuenta, con su propia sesión (su propio token y su propio contexto de peticiones). */
export class ClienteApi {
  private constructor(private readonly ctx: APIRequestContext, readonly cuenta: Cuenta, private token: string | null) {}

  static async crear(cuenta: Cuenta): Promise<ClienteApi> {
    return new ClienteApi(await request.newContext({ baseURL: API + '/', extraHTTPHeaders: { accept: 'application/json' } }), cuenta, null)
  }

  /** `POST /sesiones` como lo hace el backoffice: sin factor, y si pide `AP-CU04-03`, con el código del segundo factor. */
  async ingresar(): Promise<string> {
    const base = { telefonoE164: this.cuenta.telefono, credencial: this.cuenta.clave, huellaDispositivo: `acept-${randomUUID()}`, plataforma: 'WEB' }
    let r = await this.enviar('POST', 'sesiones', base)
    if (r.estado >= 400 && (r.cuerpo as ErrorApi).codigo === 'AP-CU04-03' && this.cuenta.factor) {
      const valor = typeof this.cuenta.factor === 'function' ? this.cuenta.factor() : this.cuenta.factor
      r = await this.enviar('POST', 'sesiones', { ...base, factor: { tipo: 'TOTP', valor } })
    }
    const token = (r.cuerpo as { tokenAcceso?: string }).tokenAcceso
    if (r.estado !== 200 || !token) throw new Error(`No se pudo abrir sesión (${r.estado}): ${r.texto.slice(0, 200)}`)
    this.token = token
    return token
  }

  get tokenActual(): string | null {
    return this.token
  }

  async enviar<T = unknown>(metodo: 'GET' | 'POST', ruta: string, cuerpo?: unknown, clave?: string): Promise<Respuesta<T>> {
    const headers: Record<string, string> = {}
    if (this.token) headers['authorization'] = `Bearer ${this.token}`
    if (clave) headers['idempotency-key'] = clave
    const r = await this.ctx.fetch(ruta.replace(/^\//, ''), { method: metodo, headers, ...(cuerpo !== undefined ? { data: cuerpo } : {}) })
    const texto = await r.text()
    let json: unknown = {}
    try {
      json = texto ? JSON.parse(texto) : {}
    } catch {
      json = { texto }
    }
    return { estado: r.status(), cuerpo: json as T, texto }
  }

  // ── Operaciones del contrato `grupos` (rutas de servicios/grupos/src/main/resources/openapi/grupos.yaml) ──

  invitar(grupoId: string, telefonoInvitado: string, clave: string = randomUUID()) {
    return this.enviar<{ invitacionId: string; token: string; expiraEn?: string } & ErrorApi>('POST', `grupos/${grupoId}/invitaciones`, { telefonoInvitado, canal: 'ENLACE' }, clave)
  }
  canjear(grupoId: string, invitacionId: string, token: string, clave: string = randomUUID()) {
    return this.enviar<{ solicitudId: string; estado: string } & ErrorApi>('POST', `grupos/${grupoId}/invitaciones/${invitacionId}/canje`, { token, cuposSolicitados: 1 }, clave)
  }
  proponer(solicitudId: string, decision: 'ACEPTAR' | 'RECHAZAR', motivo: string, revisionEsperada: number, clave: string = randomUUID()) {
    return this.enviar<DecisionApi & ErrorApi>('POST', `grupos/solicitudes/${solicitudId}/propuestas`, { decision, motivo, revisionEsperada }, clave)
  }
  resolver(solicitudId: string, decision: 'ACEPTAR' | 'RECHAZAR', motivo: string, revisionEsperada: number, propuestaId: string, clave: string = randomUUID()) {
    return this.enviar<DecisionApi & ErrorApi>('POST', `grupos/solicitudes/${solicitudId}/resoluciones`, { decision, motivo, revisionEsperada, propuestaId }, clave)
  }
  historial(solicitudId: string) {
    return this.enviar<DecisionApi[] & ErrorApi>('GET', `grupos/solicitudes/${solicitudId}/decisiones`)
  }
  solicitudesPendientes(grupoId: string) {
    return this.enviar<{ solicitudId: string; usuarioId: string; estado: string }[] & ErrorApi>('GET', `grupos/${grupoId}/solicitudes`)
  }
  misParticipaciones() {
    return this.enviar<{ grupoId: string; participanteId: string; estado: string }[]>('GET', 'grupos/participaciones')
  }

  async cerrar(): Promise<void> {
    await this.ctx.dispose()
  }
}
