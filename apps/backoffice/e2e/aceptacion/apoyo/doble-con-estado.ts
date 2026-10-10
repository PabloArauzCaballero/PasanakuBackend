import { createHash, randomBytes, randomUUID } from 'node:crypto'
import { createServer, type IncomingMessage, type Server, type ServerResponse } from 'node:http'

/**
 * DOBLE CON ESTADO del contrato `grupos` + `identidad` (solo el subconjunto de la admisión).
 *
 * Etiqueta de la evidencia que sale de acá: VERIFIED_FUNCTIONAL_ONLY. No es el servicio: es un servidor
 * en memoria que aplica las MISMAS reglas que `CU68AceptarIngreso` / `CanjearInvitacion` del servicio
 * `grupos` (las citadas en cada método) para que la MISMA suite pueda correr rápido sin el stack.
 * Prism/Scalar (`@aportaya/simulado`) no sirve para esto: no tiene estado, así que no puede probar
 * «se consume una sola vez», «una sola decisión» ni «la clave repetida no duplica».
 *
 * Las cuentas no están en el repositorio: las genera `POST /__doble/sembrar` en el momento.
 */
export type Cuenta = { id: string; telefono: string; clave: string; rol: string; permisos: string[]; factor?: string }
type Invitacion = { id: string; grupoId: string; telefono: string; token: string; revocada: boolean; solicitudId?: string; claveConsumo?: string; consumidor?: string; huella?: string }
type Decision = {
  id: string; solicitudId: string; clave: string; fase: 'PROPUESTA' | 'RESOLUCION'; decision: string; actorId: string; motivo: string
  propuestaId?: string; participanteId?: string; revision: number; evidenciaAlgoritmo: string; ocurridaEn: string; correlacionId: string
}
type Solicitud = { id: string; grupoId: string; usuarioId: string; estado: 'PENDIENTE' | 'APROBADA' | 'RECHAZADA'; cupos: number }
type Participante = { id: string; grupoId: string; usuarioId: string; estado: string }
type Salida = { estado: number; cuerpo?: unknown }

const E = (estado: number, codigo: string, mensaje: string): Salida => ({ estado, cuerpo: { codigo, mensaje, trazaId: randomUUID() } })
const REGLA = (n: number, m: string): Salida => E(422, `AP-CU68-${String(n).padStart(2, '0')}`, m)

export class DobleConEstado {
  private cuentas = new Map<string, Cuenta>()
  private invitaciones = new Map<string, Invitacion>()
  private solicitudes = new Map<string, Solicitud>()
  private decisiones: Decision[] = []
  private participantes: Participante[] = []
  private respuestasPorClave = new Map<string, Salida>()
  private grupoOrganizador = new Map<string, string>()
  /** Cuántas peticiones llegaron a cada ruta: la suite lo usa para probar «una sola decisión» sin mirar el cuerpo. */
  readonly llamadas: string[] = []
  /** Pausa artificial (ms) antes de responder una resolución: para provocar la carrera de dos pestañas. */
  demoraResolucionMs = 0
  /** Descarta la respuesta de la próxima resolución DESPUÉS de aplicarla (respuesta perdida). */
  perderRespuestaDeResolucion = false

  sembrar(): { grupoId: string; organizador: Cuenta; participante: Cuenta; participante2: Cuenta; operador: Cuenta; operador2: Cuenta; sinPermiso: Cuenta } {
    const nueva = (rol: string, permisos: string[], factor: boolean): Cuenta => {
      const c: Cuenta = {
        id: randomUUID(),
        telefono: `+5917${String(Math.floor(Math.random() * 1e7)).padStart(7, '0')}`,
        clave: randomBytes(9).toString('base64url'),
        rol,
        permisos,
        ...(factor ? { factor: String(100000 + Math.floor(Math.random() * 899999)) } : {}),
      }
      this.cuentas.set(c.id, c)
      return c
    }
    const organizador = nueva('ORGANIZADOR', ['GRUPO_ADMINISTRAR', 'PARTICIPANTE'], false)
    const grupoId = randomUUID()
    this.grupoOrganizador.set(grupoId, organizador.id)
    return {
      grupoId,
      organizador,
      participante: nueva('PARTICIPANTE', ['PARTICIPANTE'], false),
      participante2: nueva('PARTICIPANTE', ['PARTICIPANTE'], false),
      // Igual que el servidor real, el backoffice declara su rol y también su código como permiso.
      operador: nueva('ADMIN_PLATAFORMA', ['ADMIN_PLATAFORMA', 'AUDITORIA_LEER'], true),
      operador2: nueva('ADMIN_PLATAFORMA', ['ADMIN_PLATAFORMA', 'AUDITORIA_LEER'], true),
      sinPermiso: nueva('SOPORTE', ['RECLAMO_ATENDER'], true),
    }
  }

  /** Estado interno para que la suite compruebe «una sola membresía» sin pasar por la API. */
  instantanea(): { participantes: Participante[]; decisiones: Decision[]; solicitudes: Solicitud[] } {
    return { participantes: [...this.participantes], decisiones: [...this.decisiones], solicitudes: [...this.solicitudes.values()] }
  }

  private quien(req: IncomingMessage): Cuenta | null {
    const h = req.headers['authorization']
    const partes = typeof h === 'string' && h.startsWith('Bearer ') ? h.slice(7).split('.') : []
    if (partes.length !== 3 || partes[2] !== 'firma-del-doble') return null
    try {
      const sub = (JSON.parse(Buffer.from(partes[1] ?? '', 'base64url').toString()) as { sub?: string }).sub
      return (sub && this.cuentas.get(sub)) || null
    } catch {
      return null
    }
  }

  private token(c: Cuenta): string {
    const cuerpo = Buffer.from(JSON.stringify({ sub: c.id, rol: c.rol, permisos: c.permisos, exp: Math.floor(Date.now() / 1000) + 900 })).toString('base64url')
    return `eyJhbGciOiJub25lIn0.${cuerpo}.firma-del-doble`
  }

  /** `@Permiso`: el rol o un permiso coinciden con el código (GuardiaDePermiso). */
  private permitido(c: Cuenta, codigo: string): boolean {
    return c.rol === codigo || c.permisos.includes(codigo)
  }

  async atender(req: IncomingMessage, cuerpo: unknown): Promise<Salida> {
    const url = new URL(req.url ?? '/', 'http://doble')
    const ruta = url.pathname.replace(/^\/api\/v1/, '')
    const metodo = req.method ?? 'GET'
    this.llamadas.push(`${metodo} ${ruta}`)
    const c = this.quien(req)
    const clave = req.headers['idempotency-key']
    const claveStr = typeof clave === 'string' ? clave : null
    let m: RegExpMatchArray | null

    if (metodo === 'POST' && ruta === '/sesiones') return this.iniciarSesion(cuerpo as Record<string, unknown>)
    if (c === null) return E(401, 'AP-AUTH-401', 'Sesión inexistente o vencida.')

    if (metodo === 'POST' && (m = ruta.match(/^\/grupos\/([^/]+)\/invitaciones$/))) return this.invitar(c, m[1] ?? '', cuerpo as Record<string, string>, claveStr)
    if (metodo === 'POST' && (m = ruta.match(/^\/grupos\/([^/]+)\/invitaciones\/([^/]+)\/canje$/))) return this.canjear(c, m[1] ?? '', m[2] ?? '', cuerpo as Record<string, unknown>, claveStr)
    if (metodo === 'POST' && (m = ruta.match(/^\/grupos\/solicitudes\/([^/]+)\/(propuestas|resoluciones)$/))) {
      return this.decidir(c, m[1] ?? '', m[2] === 'resoluciones', cuerpo as Record<string, unknown>, claveStr)
    }
    if (metodo === 'GET' && (m = ruta.match(/^\/grupos\/solicitudes\/([^/]+)\/decisiones$/))) return this.historial(c, m[1] ?? '')
    if (metodo === 'GET' && (m = ruta.match(/^\/grupos\/([^/]+)\/solicitudes$/))) {
      // CU-68 · listarSolicitudesDeIngreso: la cola del organizador, solo PENDIENTES.
      if (this.grupoOrganizador.get(m[1] ?? '') !== c.id) return REGLA(8, 'Solo el administrador del grupo ve su cola.')
      const cola = [...this.solicitudes.values()].filter((x) => x.grupoId === m![1] && x.estado === 'PENDIENTE')
      return { estado: 200, cuerpo: cola.map((x) => ({ solicitudId: x.id, usuarioId: x.usuarioId, cuposSolicitados: x.cupos, estado: x.estado, fechaSolicitud: new Date().toISOString() })) }
    }
    if (metodo === 'GET' && ruta === '/grupos/participaciones') {
      return { estado: 200, cuerpo: this.participantes.filter((p) => p.usuarioId === c.id).map((p) => ({ grupoId: p.grupoId, participanteId: p.id, estado: p.estado })) }
    }
    return E(404, 'AP-DOBLE-404', `El doble no implementa ${metodo} ${ruta}.`)
  }

  private iniciarSesion(cuerpo: Record<string, unknown>): Salida {
    const tel = String(cuerpo['telefonoE164'] ?? '')
    const c = [...this.cuentas.values()].find((x) => x.telefono === tel)
    // Igual que identidad: no distingue «no existe» de «no coincide».
    if (!c || c.clave !== cuerpo['credencial']) return E(401, 'AP-CU04-01', 'Teléfono o contraseña incorrectos.')
    if (c.factor) {
      const f = cuerpo['factor'] as { valor?: string } | undefined
      if (!f) return E(422, 'AP-CU04-03', 'Falta el segundo factor.')
      if (f.valor !== c.factor) return E(401, 'AP-CU04-04', 'El código no es válido.')
    }
    return { estado: 200, cuerpo: { tokenAcceso: this.token(c), requiereFactorAdicional: false } }
  }

  private conClave(clave: string | null, c: Cuenta, ruta: string, hacer: () => Salida): Salida {
    if (!clave || !/^[0-9a-f-]{36}$/.test(clave)) return E(400, 'AP-IDEM-400', 'Falta la clave de idempotencia (Idempotency-Key).')
    const k = `${c.id}|${ruta}|${clave}`
    const previa = this.respuestasPorClave.get(k)
    if (previa) return previa
    const salida = hacer()
    if (salida.estado < 400) this.respuestasPorClave.set(k, salida)
    return salida
  }

  /** CU-69 · invitarAlGrupo (`@Permiso("GRUPO_ADMINISTRAR")`). */
  private invitar(c: Cuenta, grupoId: string, cuerpo: Record<string, string>, clave: string | null): Salida {
    if (!this.permitido(c, 'GRUPO_ADMINISTRAR')) return E(403, 'AP-AUTZ-403', 'Falta GRUPO_ADMINISTRAR.')
    return this.conClave(clave, c, `inv:${grupoId}`, () => {
      if (this.grupoOrganizador.get(grupoId) !== c.id) return E(422, 'AP-CU69-07', 'Quien emite la invitación no está habilitado en este grupo.')
      if (!/^\+591\d{8}$/.test(cuerpo['telefonoInvitado'] ?? '')) return E(422, 'AP-CU69-VAL', 'El teléfono invitado no es válido.')
      const vigente = [...this.invitaciones.values()].find((i) => i.grupoId === grupoId && i.telefono === cuerpo['telefonoInvitado'] && !i.revocada && !i.solicitudId)
      if (vigente) return E(422, 'AP-CU69-09', 'Ya hay una invitación vigente para ese número; revocala antes de emitir otra.')
      const inv: Invitacion = { id: randomUUID(), grupoId, telefono: cuerpo['telefonoInvitado'] ?? '', token: randomBytes(32).toString('hex'), revocada: false }
      this.invitaciones.set(inv.id, inv)
      return { estado: 201, cuerpo: { invitacionId: inv.id, mensaje: 'Invitación emitida.', enlace: null, token: inv.token, expiraEn: new Date(Date.now() + 7 * 864e5).toISOString() } }
    })
  }

  /** `CanjearInvitacion`: una sola transacción; el segundo uso con otra clave es «ya no es valida» (AP-CU69-05). */
  private canjear(c: Cuenta, grupoId: string, invitacionId: string, cuerpo: Record<string, unknown>, clave: string | null): Salida {
    if (!this.permitido(c, 'PARTICIPANTE')) return E(403, 'AP-AUTZ-403', 'Falta PARTICIPANTE.')
    if (!clave || !/^[0-9a-f-]{36}$/.test(clave)) return E(400, 'AP-IDEM-400', 'Falta la clave de idempotencia (Idempotency-Key).')
    const invalida = E(422, 'AP-CU69-05', 'Esa invitacion ya no es valida.')
    const inv = this.invitaciones.get(invitacionId)
    const token = String(cuerpo['token'] ?? '')
    if (!/^[0-9a-f]{64}$/.test(token)) return E(400, 'AP-VAL-400', 'El token no tiene el formato esperado.')
    if (!inv || inv.grupoId !== grupoId || inv.token !== token || inv.revocada || inv.telefono !== c.telefono) return invalida
    const huella = createHash('sha256').update(`${cuerpo['cuposSolicitados']}:${cuerpo['mensaje']}`).digest('hex')
    if (inv.solicitudId) {
      const mismo = inv.claveConsumo === clave && inv.consumidor === c.id && inv.huella === huella
      return mismo ? { estado: 200, cuerpo: { solicitudId: inv.solicitudId, estado: 'PENDIENTE_BACKOFFICE' } } : invalida
    }
    const s: Solicitud = { id: randomUUID(), grupoId, usuarioId: c.id, estado: 'PENDIENTE', cupos: Number(cuerpo['cuposSolicitados'] ?? 1) }
    this.solicitudes.set(s.id, s)
    Object.assign(inv, { solicitudId: s.id, claveConsumo: clave, consumidor: c.id, huella })
    return { estado: 200, cuerpo: { solicitudId: s.id, estado: 'PENDIENTE_BACKOFFICE' } }
  }

  /** `CU68AceptarIngreso.decidir`: proponer (organizador) o resolver (backoffice). */
  private async decidir(c: Cuenta, solicitudId: string, definitiva: boolean, cuerpo: Record<string, unknown>, clave: string | null): Promise<Salida> {
    if (!this.permitido(c, definitiva ? 'ADMIN_PLATAFORMA' : 'GRUPO_ADMINISTRAR')) return E(403, 'AP-AUTZ-403', `Falta ${definitiva ? 'ADMIN_PLATAFORMA' : 'GRUPO_ADMINISTRAR'}.`)
    if (!clave || !/^[0-9a-f-]{36}$/.test(clave)) return E(400, 'AP-IDEM-400', 'Falta la clave de idempotencia (Idempotency-Key).')
    const motivo = typeof cuerpo['motivo'] === 'string' ? cuerpo['motivo'] : ''
    const decision = cuerpo['decision']
    const revision = cuerpo['revisionEsperada']
    if (!motivo.trim() || motivo.length > 1000 || (decision !== 'ACEPTAR' && decision !== 'RECHAZAR') || typeof revision !== 'number' || revision < 0) {
      return REGLA(10, 'La decisión requiere clave, revisión, resultado y motivo explícitos.')
    }
    const sol = this.solicitudes.get(solicitudId)
    if (!sol) return REGLA(8, 'Solicitud no encontrada.')
    if (!definitiva && this.grupoOrganizador.get(sol.grupoId) !== c.id) return REGLA(8, 'Solo el administrador de este grupo puede proponer la admisión.')
    if (sol.usuarioId === c.id) return REGLA(8, 'Nadie puede decidir su propia admisión.')
    const fase = definitiva ? 'RESOLUCION' : 'PROPUESTA'
    const repetida = this.decisiones.find((d) => d.clave === clave)
    if (repetida) {
      const igual = repetida.solicitudId === solicitudId && repetida.actorId === c.id && repetida.fase === fase && repetida.decision === decision && repetida.motivo === motivo && repetida.revision === revision + 1 && repetida.propuestaId === (cuerpo['propuestaId'] as string | undefined)
      return igual ? { estado: 200, cuerpo: repetida } : REGLA(9, 'La clave de idempotencia pertenece a otra decisión.')
    }
    if (definitiva && this.demoraResolucionMs > 0) await new Promise((r) => setTimeout(r, this.demoraResolucionMs))
    // A partir de acá no hay `await`: es la sección crítica (en el servicio real, el FOR UPDATE de la solicitud).
    const previas = this.decisiones.filter((d) => d.solicitudId === solicitudId)
    const anterior = previas.at(-1)
    if (sol.estado !== 'PENDIENTE' || (anterior?.revision ?? 0) !== revision) return REGLA(9, 'La solicitud cambió o ya fue resuelta; actualiza el expediente.')
    if (definitiva) {
      if (!anterior) return REGLA(9, 'Falta la propuesta del administrador.')
      if (anterior.fase !== 'PROPUESTA' || anterior.id !== cuerpo['propuestaId'] || anterior.actorId === c.id) return REGLA(9, 'La resolución requiere la propuesta vigente y otro responsable humano.')
    } else if (cuerpo['propuestaId'] != null) return REGLA(9, 'Una propuesta no puede suplantar una resolución.')
    let participanteId: string | undefined
    if (definitiva && decision === 'ACEPTAR') {
      participanteId = randomUUID()
      this.participantes.push({ id: participanteId, grupoId: sol.grupoId, usuarioId: sol.usuarioId, estado: 'PENDIENTE_FIRMA' })
    }
    const d: Decision = {
      id: randomUUID(), solicitudId, clave, fase, decision, actorId: c.id, motivo, revision: revision + 1, evidenciaAlgoritmo: 'Sin alertas: puntaje sintético del doble.',
      ocurridaEn: new Date().toISOString(), correlacionId: randomUUID(), ...(cuerpo['propuestaId'] ? { propuestaId: cuerpo['propuestaId'] as string } : {}), ...(participanteId ? { participanteId } : {}),
    }
    this.decisiones.push(d)
    if (definitiva) sol.estado = decision === 'ACEPTAR' ? 'APROBADA' : 'RECHAZADA'
    if (definitiva && this.perderRespuestaDeResolucion) {
      this.perderRespuestaDeResolucion = false
      return { estado: -1 }
    }
    return { estado: 200, cuerpo: d }
  }

  /** `historial`: el administrador del grupo y backoffice. La persona que pidió entrar NO (por eso no ve su motivo). */
  private historial(c: Cuenta, solicitudId: string): Salida {
    // OJO (a verificar en modo real): en servicios/grupos RutasDeAdmision.historialAdmision lleva `@Permiso("PARTICIPANTE")`, pero PARTICIPANTE es un
    // ROL del catálogo y el token de un operador trae rol ADMIN_PLATAFORMA y permisos de catálogo, sin ese código: el guardián lo rechazaría (403).
    // El doble sigue la intención declarada del contrato y del caso de uso («accesible al administrador del grupo y al backoffice»).
    const sol = this.solicitudes.get(solicitudId)
    if (!sol) return REGLA(8, 'Solicitud no encontrada.')
    const backoffice = c.rol === 'BACKOFFICE' || c.rol === 'ADMIN_PLATAFORMA'
    if (!backoffice && this.grupoOrganizador.get(sol.grupoId) !== c.id) return REGLA(6, 'El expediente de decisión requiere permisos de revisión.')
    return { estado: 200, cuerpo: this.decisiones.filter((d) => d.solicitudId === solicitudId) }
  }
}

/** Sirve el doble por HTTP (`/api/v1/**` y `/__doble/**`). Devuelve el servidor y la instancia para inspeccionarla. */
export function servirDoble(puerto: number): Promise<{ servidor: Server; doble: DobleConEstado }> {
  const doble = new DobleConEstado()
  const servidor = createServer(async (req: IncomingMessage, res: ServerResponse) => {
    const trozos: Buffer[] = []
    for await (const t of req) trozos.push(t as Buffer)
    let cuerpo: unknown = undefined
    try {
      cuerpo = trozos.length ? JSON.parse(Buffer.concat(trozos).toString()) : undefined
    } catch {
      res.writeHead(400).end()
      return
    }
    const ruta = new URL(req.url ?? '/', 'http://doble').pathname
    let salida: Salida
    if (ruta === '/__doble/sembrar' && req.method === 'POST') salida = { estado: 200, cuerpo: doble.sembrar() }
    else if (ruta === '/__doble/instantanea') salida = { estado: 200, cuerpo: doble.instantanea() }
    else if (ruta === '/__doble/ajustes' && req.method === 'POST') {
      const a = cuerpo as { demoraResolucionMs?: number; perderRespuestaDeResolucion?: boolean }
      if (a.demoraResolucionMs !== undefined) doble.demoraResolucionMs = a.demoraResolucionMs
      if (a.perderRespuestaDeResolucion !== undefined) doble.perderRespuestaDeResolucion = a.perderRespuestaDeResolucion
      salida = { estado: 204 }
    } else if (ruta === '/__doble/llamadas') salida = { estado: 200, cuerpo: doble.llamadas }
    else salida = await doble.atender(req, cuerpo)
    if (salida.estado === -1) {
      req.socket.destroy() // la respuesta se pierde: el cliente ve un corte de red
      return
    }
    res.writeHead(salida.estado, { 'content-type': 'application/json' })
    res.end(salida.cuerpo === undefined ? undefined : JSON.stringify(salida.cuerpo))
  })
  return new Promise((ok) => servidor.listen(puerto, '127.0.0.1', () => ok({ servidor, doble })))
}
