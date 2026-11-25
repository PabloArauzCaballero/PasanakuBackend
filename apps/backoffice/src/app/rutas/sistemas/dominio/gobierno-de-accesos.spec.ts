import { HttpClient, provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { olvidarIntentos } from '../../../nucleo/idempotencia.interceptor'
import { idempotenciaInterceptor } from '../../../nucleo/idempotencia.interceptor'
import { asignarRolDeOperador, cursorDespuesDe, darDeBajaUsuarioInterno, paginaDeUsuariosInternos, revocarAsignacionDeRol, type UsuarioInternoDeLista } from './gobierno-de-accesos'
import { esCodigoValido, obtenerEvidenciaDeStepUp } from './paso-de-seguridad'
import { esRechazoDefinitivo, traducirErrorDeGobierno } from './errores-de-gobierno'
import { withInterceptors } from '@angular/common/http'

const GW = 'http://gw/api/v1'
const ID = '4f6c1a52-0000-4000-8000-000000000001'
const ROL = '4f6c1a52-0000-4000-8000-000000000002'
const ASIGNACION = '4f6c1a52-0000-4000-8000-000000000003'
const EVIDENCIA = 'e'.repeat(40)

/**
 * Las llamadas nuevas de B (PR #516) contra un doble del contrato: el cuerpo y la ruta salen de `identidad.yaml` de esa
 * rama. Tres niveles (regla 65): correcto, límite e inválido. Todo esto queda verificado SOLO contra el doble.
 */
describe('gobierno de accesos · llamadas del contrato de B (doble declarado)', () => {
  let http: HttpClient
  let backend: HttpTestingController
  beforeEach(() => {
    olvidarIntentos()
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideHttpClient(withInterceptors([idempotenciaInterceptor])), provideHttpClientTesting()] })
    http = TestBed.inject(HttpClient)
    backend = TestBed.inject(HttpTestingController)
  })
  afterEach(() => backend.verify())

  it('correcto: asignar un rol POST /usuarios-internos/{id}/asignaciones con Idempotency-Key y el cuerpo del contrato', () => {
    let salida: unknown
    asignarRolDeOperador(http, GW, ID, { rolId: ROL, ambito: 'GLOBAL', justificacion: 'Cubre la guardia de agosto', evidenciaMfa: EVIDENCIA }).subscribe((s) => (salida = s))
    const r = backend.expectOne(`${GW}/usuarios-internos/${ID}/asignaciones`)
    expect(r.request.method).toBe('POST')
    expect(r.request.headers.get('Idempotency-Key')).toMatch(/^[0-9a-f-]{36}$/)
    expect(r.request.body).toEqual({ rolId: ROL, ambito: 'GLOBAL', justificacion: 'Cubre la guardia de agosto', evidenciaMfa: EVIDENCIA })
    r.flush({ asignacionId: ASIGNACION, usuarioId: ID, rolCodigo: 'SOPORTE', ambito: 'GLOBAL', permisosEfectivos: [], requiereMfa: true, yaExistia: false })
    expect(salida).toMatchObject({ asignacionId: ASIGNACION, requiereMfa: true, yaExistia: false })
  })

  it('límite: el mismo contenido reintentado (sin respuesta definitiva) reusa la misma Idempotency-Key; tras la respuesta, una nueva', () => {
    const entrada = { rolId: ROL, ambito: 'GRUPO' as const, ambitoId: ID, vigenteHasta: '2026-12-31T23:59:59Z', justificacion: 'Auditoría del grupo', evidenciaMfa: EVIDENCIA }
    asignarRolDeOperador(http, GW, ID, entrada).subscribe({ error: () => undefined })
    const primera = backend.expectOne(`${GW}/usuarios-internos/${ID}/asignaciones`)
    const clave = primera.request.headers.get('Idempotency-Key')
    primera.error(new ProgressEvent('error'), { status: 0 })
    asignarRolDeOperador(http, GW, ID, entrada).subscribe()
    const segunda = backend.expectOne(`${GW}/usuarios-internos/${ID}/asignaciones`)
    expect(segunda.request.headers.get('Idempotency-Key')).toBe(clave)
    segunda.flush({ asignacionId: ASIGNACION, usuarioId: ID, rolCodigo: 'SOPORTE', ambito: 'GRUPO', ambitoId: ID, permisosEfectivos: [], requiereMfa: false, yaExistia: true })
    asignarRolDeOperador(http, GW, ID, entrada).subscribe()
    expect(backend.expectOne(`${GW}/usuarios-internos/${ID}/asignaciones`).request.headers.get('Idempotency-Key')).not.toBe(clave)
  })

  it('correcto: revocar POST /usuarios-internos/asignaciones/{id}/revocacion y baja POST /usuarios-internos/{id}/baja', () => {
    revocarAsignacionDeRol(http, GW, ASIGNACION, { motivoRevocacion: 'Cambió de área', cerrarSesiones: false, evidenciaMfa: EVIDENCIA }).subscribe()
    const rev = backend.expectOne(`${GW}/usuarios-internos/asignaciones/${ASIGNACION}/revocacion`)
    expect(rev.request.method).toBe('POST')
    expect(rev.request.body).toEqual({ motivoRevocacion: 'Cambió de área', cerrarSesiones: false, evidenciaMfa: EVIDENCIA })
    rev.flush({ asignacionId: ASIGNACION, usuarioId: ID, revocadaEn: '2026-11-25T10:00:00Z', sesionesCerradas: 0, yaEstabaRevocada: false })
    darDeBajaUsuarioInterno(http, GW, ID, { motivo: 'Renuncia presentada', evidenciaMfa: EVIDENCIA }).subscribe()
    const baja = backend.expectOne(`${GW}/usuarios-internos/${ID}/baja`)
    expect(baja.request.method).toBe('POST')
    expect(baja.request.headers.has('Idempotency-Key')).toBe(true)
    baja.flush({ usuarioId: ID, estado: 'BAJA_LABORAL', asignacionesRevocadas: 2, sesionesCerradas: 1, yaEstabaDeBaja: false })
  })

  it('correcto/límite: el listado pide limite y, desde la segunda página, el cursor (antesDe + antesDeId); el cursor sale de la última fila', () => {
    paginaDeUsuariosInternos(http, GW, null).subscribe()
    backend.expectOne(`${GW}/usuarios-internos?limite=50`).flush([])
    const fila = (id: string, creadoEn?: string) => ({ id, creadoEn }) as UsuarioInternoDeLista
    expect(cursorDespuesDe([fila('a', '2026-11-20T10:00:00Z'), fila('b', '2026-11-19T10:00:00Z')])).toEqual({ antesDe: '2026-11-19T10:00:00Z', antesDeId: 'b' })
    expect(cursorDespuesDe([])).toBeNull()
    expect(cursorDespuesDe([fila('c')])).toBeNull()
    paginaDeUsuariosInternos(http, GW, { antesDe: '2026-11-19T10:00:00Z', antesDeId: 'b' }, 20).subscribe()
    const r = backend.expectOne((q) => q.url === `${GW}/usuarios-internos`)
    expect(r.request.params.get('limite')).toBe('20')
    expect(r.request.params.get('antesDe')).toBe('2026-11-19T10:00:00Z')
    expect(r.request.params.get('antesDeId')).toBe('b')
    r.flush([])
  })

  it('inválido: un 422 AP-CU08-06 llega como error y el traductor lo vuelve un mensaje accionable (nunca el crudo)', () => {
    let error: unknown
    darDeBajaUsuarioInterno(http, GW, ID, { motivo: 'Renuncia presentada', evidenciaMfa: EVIDENCIA }).subscribe({ error: (e) => (error = e) })
    backend.expectOne(`${GW}/usuarios-internos/${ID}/baja`).flush({ codigo: 'AP-CU08-06', mensaje: 'ULTIMO_ADMINISTRADOR crudo' }, { status: 422, statusText: 'Unprocessable' })
    const t = traducirErrorDeGobierno({ codigo: 'AP-CU08-06', mensaje: 'ULTIMO_ADMINISTRADOR crudo', estado: 422 }, 'x')
    expect(error).toBeTruthy()
    expect(t.mensaje).toContain('sin ningún administrador de accesos')
    expect(t.mensaje).not.toContain('crudo')
    expect(esRechazoDefinitivo(t)).toBe(true)
  })
})

describe('paso de seguridad · step-up (abrir desafío, presentar el código TOTP)', () => {
  let http: HttpClient
  let backend: HttpTestingController
  beforeEach(() => {
    olvidarIntentos()
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideHttpClient(withInterceptors([idempotenciaInterceptor])), provideHttpClientTesting()] })
    http = TestBed.inject(HttpClient)
    backend = TestBed.inject(HttpTestingController)
  })
  afterEach(() => backend.verify())

  it('correcto: abre un desafío ADMIN, presenta el TOTP y devuelve la evidencia', () => {
    let evidencia = ''
    obtenerEvidenciaDeStepUp(http, GW, '123456').subscribe((e) => (evidencia = e))
    const abrir = backend.expectOne(`${GW}/sesiones/desafios`)
    expect(abrir.request.body).toEqual({ proposito: 'ADMIN' })
    expect(abrir.request.headers.has('Idempotency-Key')).toBe(true)
    abrir.flush({ desafioId: ID, expiraEn: '2026-11-25T10:05:00Z' })
    const verificar = backend.expectOne(`${GW}/sesiones/desafios/${ID}/verificacion`)
    expect(verificar.request.body).toEqual({ factor: { tipo: 'TOTP', valor: '123456' } })
    verificar.flush({ evidencia: EVIDENCIA, expiraEn: '2026-11-25T10:05:00Z', jti: ID })
    expect(evidencia).toBe(EVIDENCIA)
  })

  it('límite: el código tiene que ser de exactamente seis dígitos', () => {
    for (const ok of ['000000', '123456']) expect(esCodigoValido(ok), ok).toBe(true)
    for (const mal of ['', '12345', '1234567', 'abcdef', '12 456', ' 123456']) expect(esCodigoValido(mal), mal).toBe(false)
  })

  it('inválido: código equivocado (422 AP-CU04-04) → error de campo «codigo»; factor sin enrolar (AP-CU04-06) también; sin abrir desafío no hay segunda llamada', () => {
    let error: unknown
    obtenerEvidenciaDeStepUp(http, GW, '654321').subscribe({ error: (e) => (error = e) })
    backend.expectOne(`${GW}/sesiones/desafios`).flush({ codigo: 'AP-CU04-06', mensaje: 'FACTOR_NO_ENROLADO' }, { status: 422, statusText: 'Unprocessable' })
    backend.expectNone((r) => r.url.includes('/verificacion'))
    expect(error).toBeTruthy()
    expect(traducirErrorDeGobierno({ codigo: 'AP-CU04-06', estado: 422 }, 'x')).toMatchObject({ campo: 'codigo' })
    expect(traducirErrorDeGobierno({ codigo: 'AP-CU04-04', estado: 422 }, 'x')).toMatchObject({ campo: 'codigo' })
  })
})

describe('errores de gobierno · traducción por campo', () => {
  it('cada código AP-CU08-xx conocido tiene un mensaje propio y los que son de un campo lo señalan', () => {
    const campos: Record<string, string | undefined> = { 'AP-CU08-01': 'rol', 'AP-CU08-02': 'ambito', 'AP-CU08-03': 'rol', 'AP-CU08-08': 'rol', 'AP-CU08-09': 'codigo', 'AP-CU08-11': 'motivo', 'AP-CU08-04': undefined, 'AP-CU08-06': undefined, 'AP-CU08-10': undefined, 'AP-CU08-12': undefined }
    for (const [codigo, campo] of Object.entries(campos)) {
      const e = traducirErrorDeGobierno({ codigo, mensaje: 'CRUDO', estado: 422 }, 'x')
      expect(e.campo, codigo).toBe(campo)
      expect(e.mensaje, codigo).not.toBe('CRUDO')
    }
  })

  it('límite: un código desconocido conserva el mensaje ya traducido por estado; sin nada, el genérico; sin conexión no es un rechazo definitivo', () => {
    expect(traducirErrorDeGobierno({ codigo: 'AP-CU99-01', mensaje: 'No tenés acceso a esto.', estado: 403 }, 'g').mensaje).toBe('No tenés acceso a esto.')
    expect(traducirErrorDeGobierno(new Error('boom'), 'No se pudo guardar.').mensaje).toBe('No se pudo guardar.')
    expect(esRechazoDefinitivo(traducirErrorDeGobierno({ estado: 0, sinConexion: true }, 'x'))).toBe(false)
    expect(esRechazoDefinitivo(traducirErrorDeGobierno({ estado: 503 }, 'x'))).toBe(false)
    expect(esRechazoDefinitivo(traducirErrorDeGobierno({ estado: 429 }, 'x'))).toBe(false)
  })

  it('inválido: el error envuelto por resource() (cause) también se traduce', () => {
    const envuelto = new Error('wrapped', { cause: { codigo: 'AP-CU08-11', estado: 422, trazaId: 'tz-1' } })
    expect(traducirErrorDeGobierno(envuelto, 'x')).toMatchObject({ campo: 'motivo', trazaId: 'tz-1' })
  })
})
