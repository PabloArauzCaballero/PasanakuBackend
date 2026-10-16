// Doble CON ESTADO para la prueba de dos emuladores (carril H, cuarta vuelta).
// NO es el backend real: implementa en memoria solo identidad (sesiones/usuarios) y grupos (crear,
// invitar, consultar/aceptar enlace, revocar, participaciones). Todo lo demas se reenvia al simulado
// Scalar (packages/simulado) en SIMULADO_PORT. Contratos: clientes OpenAPI de identidad y grupos.
import http from 'node:http'
import crypto from 'node:crypto'

const PORT = Number(process.env.PORT || 4010)
const SIM = Number(process.env.SIMULADO_PORT || 4011)
const b64 = (o) => Buffer.from(JSON.stringify(o)).toString('base64url')
const uid = () => crypto.randomUUID()
const usuarios = new Map() // telefono -> {id, pass, nombre}
const grupos = new Map() // id -> {id, nombre, monto, periodicidad, organizadorId, participantes:[]}
const invitaciones = new Map() // tokenId -> {...}
const bitacora = []
const sembrar = (tel, nombre) => usuarios.set(tel, { id: uid(), pass: 'Clave-Segura-123', nombre })
sembrar('+59170000001', 'Ana Organizadora')
sembrar('+59170000002', 'Beto Invitado')
const log = (m) => {
  bitacora.push(`${new Date().toISOString()} ${m}`)
  console.log(m)
}

const trazar = () => 'doble-' + uid().slice(0, 8)
const err = (res, estado, codigo, mensaje) => {
  res.writeHead(estado, { 'content-type': 'application/json', 'x-request-id': trazar() })
  res.end(JSON.stringify({ codigo, mensaje, trazaId: trazar() }))
}
const ok = (res, estado, cuerpo) => {
  res.writeHead(estado, { 'content-type': 'application/json', 'x-request-id': trazar() })
  res.end(JSON.stringify(cuerpo))
}
const quien = (req) => {
  const a = req.headers.authorization || ''
  const t = a.replace(/^Bearer /, '').split('.')
  if (t.length !== 3) return null
  try {
    const p = JSON.parse(Buffer.from(t[1], 'base64url').toString())
    return [...usuarios.entries()].find(([, u]) => u.id === p.sub) ?? null
  } catch {
    return null
  }
}
const leer = (req) =>
  new Promise((r) => {
    let d = ''
    req.on('data', (c) => (d += c))
    req.on('end', () => {
      try {
        r(d ? JSON.parse(d) : {})
      } catch {
        r({})
      }
    })
  })

const REGLAMENTO = 'Reglamento sintético del doble: aporto a tiempo cada periodo; si falto, pierdo el turno. (Texto de prueba, no es un reglamento real.)'
const hashReglamento = crypto.createHash('sha256').update(REGLAMENTO).digest('hex')

http
  .createServer(async (req, res) => {
    const url = new URL(req.url, 'http://x')
    const ruta = url.pathname.replace(/^\/api\/v1/, '')
    const m = req.method
    if (ruta === '/__estado') {
      return ok(res, 200, {
        usuarios: [...usuarios.entries()].map(([t, u]) => ({ t, id: u.id, nombre: u.nombre })),
        grupos: [...grupos.values()],
        invitaciones: [...invitaciones.values()].map((i) => ({ ...i, token: '***' })),
        bitacora,
      })
    }
    if (ruta === '/__revocar' && m === 'POST') {
      const b = await leer(req)
      const i = invitaciones.get(b.tokenId)
      if (i) i.estado = 'REVOCADA'
      log(`INVITACION ${b.tokenId} REVOCADA (accion del organizador, via API)`)
      return ok(res, 200, { ok: !!i })
    }
    const cuerpoCrudo = await leer(req)
    try {
      if (m === 'POST' && ruta === '/sesiones') {
        const u = usuarios.get(cuerpoCrudo.telefonoE164)
        if (!u || u.pass !== cuerpoCrudo.credencial) {
          log(`LOGIN rechazado ${cuerpoCrudo.telefonoE164}`)
          return err(res, 401, 'AP-CU04-02', 'Credenciales invalidas')
        }
        const exp = Math.floor(Date.now() / 1000) + 3600
        const tok = `${b64({ alg: 'none', typ: 'JWT' })}.${b64({ sub: u.id, exp, iat: exp - 3600, tel: cuerpoCrudo.telefonoE164 })}.doble`
        log(`LOGIN ok ${cuerpoCrudo.telefonoE164} (${u.nombre})`)
        return ok(res, 200, {
          sesionId: uid(),
          expiraEn: new Date(exp * 1000).toISOString(),
          requiereFactorAdicional: false,
          dispositivoConfiable: true,
          tokenAcceso: tok,
        })
      }
      if (m === 'POST' && ruta === '/usuarios') {
        const tel = cuerpoCrudo.telefonoE164 || cuerpoCrudo.telefono
        if (tel && usuarios.has(tel)) return err(res, 409, 'AP-CU01-03', 'Ese celular ya tiene una cuenta')
        if (tel) {
          usuarios.set(tel, {
            id: uid(),
            pass: cuerpoCrudo.credencial || cuerpoCrudo.contrasena || 'Clave-Segura-123',
            nombre: cuerpoCrudo.nombre || tel,
          })
          log(`REGISTRO ${tel}`)
        }
      }
      const yo = quien(req)
      if (m === 'POST' && ruta === '/grupos') {
        if (!yo) return err(res, 401, 'AP-CU20-01', 'sin sesion')
        const g = {
          id: uid(),
          nombre: cuerpoCrudo.nombre,
          monto: cuerpoCrudo.montoAporte,
          periodicidad: cuerpoCrudo.periodicidad,
          organizadorId: yo[1].id,
          participantes: [{ participanteId: uid(), usuarioId: yo[1].id, tel: yo[0], rol: 'ORGANIZADOR' }],
        }
        grupos.set(g.id, g)
        log(`GRUPO creado ${g.id} por ${yo[0]}`)
        return ok(res, 201, { grupoId: g.id, fondoPorPeriodo: '0.00' })
      }
      let mm
      if (m === 'POST' && (mm = ruta.match(/^\/grupos\/([0-9a-f-]{36})\/invitaciones$/))) {
        if (!yo) return err(res, 401, 'AP-CU69-01', 'sin sesion')
        const g = grupos.get(mm[1])
        if (!g || g.organizadorId !== yo[1].id) return err(res, 403, 'AP-CU69-02', 'No sos organizador de ese grupo')
        if (!/^\+591\d{8}$/.test(cuerpoCrudo.telefonoInvitado || '')) return err(res, 422, 'AP-CU69-03', 'telefono invalido')
        const tokenId = uid()
        const token = crypto.randomBytes(32).toString('hex')
        invitaciones.set(tokenId, {
          tokenId,
          token,
          grupoId: g.id,
          tel: cuerpoCrudo.telefonoInvitado,
          estado: 'PENDIENTE',
          creada: new Date().toISOString(),
        })
        log(`INVITACION ${tokenId} a ${cuerpoCrudo.telefonoInvitado} (grupo ${g.nombre})`)
        return ok(res, 201, {
          invitacionId: tokenId,
          mensaje: `Invitación creada para ${cuerpoCrudo.telefonoInvitado}.`,
          enlace: `aportaya://unirse/${tokenId}.${token}`,
          token,
          expiraEn: new Date(Date.now() + 72 * 3600e3).toISOString(),
        })
      }
      if (m === 'GET' && ruta === '/grupos/participaciones') {
        if (!yo) return err(res, 401, 'AP-CU69-01', 'sin sesion')
        const l = []
        for (const g of grupos.values())
          for (const p of g.participantes)
            if (p.usuarioId === yo[1].id)
              l.push({ grupoId: g.id, participanteId: p.participanteId, estado: p.rol === 'ORGANIZADOR' ? 'ORGANIZADOR' : 'ACTIVO' })
        return ok(res, 200, l)
      }
      if (m === 'POST' && (ruta === '/grupos/invitaciones/enlace/consultar' || ruta === '/grupos/invitaciones/enlace/aceptar')) {
        if (!yo) return err(res, 401, 'AP-CU69-01', 'sin sesion')
        const i = invitaciones.get(cuerpoCrudo.tokenId)
        if (!i || i.token !== cuerpoCrudo.token) {
          log(`ENLACE invalido por ${yo[0]}`)
          return err(res, 422, 'AP-CU69-04', 'enlace invalido')
        }
        if (i.estado !== 'PENDIENTE') {
          log(`ENLACE ${i.estado} rechazado a ${yo[0]}`)
          return err(res, 422, 'AP-CU69-05', `invitación ${i.estado}`)
        }
        if (i.tel !== yo[0]) {
          log(`ENLACE de otro telefono: ${yo[0]} != ${i.tel}`)
          return err(res, 422, 'AP-CU69-06', 'la invitacion es para otro telefono')
        }
        const g = grupos.get(i.grupoId)
        if (ruta.endsWith('/consultar')) {
          log(`CONSULTA enlace por ${yo[0]}`)
          return ok(res, 200, {
            grupoId: g.id,
            nombre: g.nombre,
            montoAporte: g.monto?.monto ?? g.monto ?? '100.00',
            moneda: g.monto?.moneda ?? 'BOB',
            periodicidad: g.periodicidad ?? 'MENSUAL',
            reglamento: REGLAMENTO,
            hashReglamento,
          })
        }
        if (!cuerpoCrudo.aceptaReglamento || cuerpoCrudo.hashReglamento !== hashReglamento)
          return err(res, 422, 'AP-CU69-07', 'reglamento no aceptado')
        i.estado = 'ACEPTADA'
        const p = { participanteId: uid(), usuarioId: yo[1].id, tel: yo[0], rol: 'PARTICIPANTE' }
        g.participantes.push(p)
        log(`ACEPTADA por ${yo[0]} -> participante ${p.participanteId}`)
        return ok(res, 201, { grupoId: g.id, participanteId: p.participanteId })
      }
    } catch (e) {
      return err(res, 500, 'AP-CU00-01', String(e))
    }
    // Reenvio al simulado Scalar.
    const cuerpo = Buffer.from(JSON.stringify(cuerpoCrudo))
    const headers = { ...req.headers, host: `127.0.0.1:${SIM}` }
    if (m !== 'GET' && m !== 'HEAD') headers['content-length'] = cuerpo.length
    const p = http.request({ host: '127.0.0.1', port: SIM, path: req.url, method: m, headers }, (r) => {
      res.writeHead(r.statusCode, r.headers)
      r.pipe(res)
    })
    p.on('error', (e) => err(res, 502, 'AP-CU00-02', 'simulado caido: ' + e.message))
    if (m !== 'GET' && m !== 'HEAD') p.write(cuerpo)
    p.end()
  })
  .listen(PORT, '0.0.0.0', () => console.log(`doble con estado en :${PORT} -> simulado :${SIM}`))
