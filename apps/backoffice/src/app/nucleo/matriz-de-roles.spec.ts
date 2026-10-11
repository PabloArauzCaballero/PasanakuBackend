import { readdirSync, readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'
import { alcanza, PERMISOS_POR_SECCION } from './secciones'

/**
 * H12.S1.M1 · recorrido por roles sin acciones ajenas.
 *
 * La fuente es el catálogo REAL de roles y permisos (`sql/60_semillas/10-roles-y-permisos.sql`, generado desde
 * `seeders/minimos`): el token de `identidad` lleva los permisos efectivos del rol MÁS el código de cada rol vigente
 * (`AccesosRepositorio.codigosDeRolesVigentes`). Esta prueba arma ese token para cada rol y comprueba qué secciones
 * abre y qué acciones habilita. Es una prueba de la LÓGICA DEL CLIENTE sobre el catálogo versionado; no consulta un
 * `identidad` real ni prueba que el servidor niegue lo que el cliente oculta (eso es de cada servicio).
 */
const RAIZ = join(__dirname, '../../../../..')
const SEMILLA = readFileSync(join(RAIZ, 'sql/60_semillas/10-roles-y-permisos.sql'), 'utf8')

function leerCatalogo() {
  const roles = new Set([...SEMILLA.slice(0, SEMILLA.indexOf('INSERT INTO permiso')).matchAll(/^\s*\('([A-Z_]+)',/gm)].map((m) => m[1]!))
  const permisos = new Set(
    [...SEMILLA.slice(SEMILLA.indexOf('INSERT INTO permiso'), SEMILLA.indexOf('INSERT INTO rol_permiso')).matchAll(/^\s*\('([A-Z_]+)',/gm)].map((m) => m[1]!),
  )
  const concedidos = new Map<string, string[]>()
  for (const m of SEMILLA.matchAll(/FROM rol WHERE codigo = '([A-Z_]+)'\)\s*,\s*\(SELECT id FROM permiso WHERE codigo = '([A-Z_]+)'\)/g)) {
    concedidos.set(m[1]!, [...(concedidos.get(m[1]!) ?? []), m[2]!])
  }
  return { roles, permisos, concedidos }
}
const CATALOGO = leerCatalogo()

/** El token de un rol: su código más los permisos del catálogo que ese rol concede. */
const tokenDe = (rol: string): string[] => [rol, ...(CATALOGO.concedidos.get(rol) ?? [])]
const SECCIONES = ['ver:operacion', 'ver:cumplimiento', 'ver:admisiones', 'ver:sistemas', 'ver:contabilidad', 'ver:publicidad'] as const
const seccionesDe = (rol: string) => SECCIONES.filter((s) => alcanza(s, tokenDe(rol), true)).map((s) => s.slice(4))

/** Lo que cada rol debe ver. Escrito a mano desde las concesiones del catálogo, para que un cambio en cualquiera de los dos se note. */
const ESPERADO: Record<string, string[]> = {
  ADMIN_PLATAFORMA: ['operacion', 'cumplimiento', 'admisiones', 'sistemas'],
  OFICIAL_CUMPLIMIENTO: ['operacion', 'cumplimiento'],
  ANALISTA_CUMPLIMIENTO: ['operacion', 'cumplimiento'],
  AUDITOR_INTERNO: ['operacion', 'contabilidad'],
  RESPONSABLE_RIESGOS: ['operacion'],
  RESPONSABLE_SEGURIDAD: ['sistemas'],
  TESORERIA: ['operacion', 'contabilidad'],
  CONTABILIDAD: ['operacion', 'contabilidad'],
  SOPORTE: ['operacion'],
  PUNTO_RECLAMO: ['operacion'],
  OPERADOR_PUBLICIDAD: ['publicidad'],
  MODERADOR_CONTENIDO: ['publicidad'],
  // Roles de la app: no son de backoffice. Un participante o un organizador que llegara con su token no abre NINGUNA sección.
  PARTICIPANTE: [],
  ORGANIZADOR: [],
  ANUNCIANTE: [],
  // Identidades de servicio (otro agente las sumó al catálogo): no son personas ni abren pantallas del backoffice.
  SERVICIO_ENTREGAS: [],
  SERVICIO_INVERSIONES: [],
}

describe('matriz de roles · qué secciones abre cada rol del catálogo', () => {
  it('el catálogo parseado tiene los diecisiete roles y la prueba cubre cada uno (si se agrega un rol, hay que decidir qué ve)', () => {
    expect(CATALOGO.roles.size).toBe(17)
    expect([...CATALOGO.roles].sort()).toEqual(Object.keys(ESPERADO).sort())
  })

  for (const [rol, esperado] of Object.entries(ESPERADO)) {
    it(`${rol} abre ${esperado.length ? esperado.join(', ') : 'ninguna sección'}`, () => {
      expect(seccionesDe(rol)).toEqual(esperado)
    })
  }

  it('participante y organizador (los roles de la app) solo tienen tablero y ayuda en el backoffice', () => {
    for (const rol of ['PARTICIPANTE', 'ORGANIZADOR']) {
      expect(alcanza('ver:tablero', tokenDe(rol), true)).toBe(true)
      expect(alcanza('ver:ayuda', tokenDe(rol), true)).toBe(true)
      expect(SECCIONES.some((s) => alcanza(s, tokenDe(rol), true))).toBe(false)
      for (const accion of ['BILLETERA_VER_TERCEROS', 'RECLAMO_ATENDER', 'ADMIN_PLATAFORMA', 'VERIFICACION_RESOLVER']) {
        expect(alcanza(accion, tokenDe(rol), true), `${rol} no debe poder ${accion}`).toBe(false)
      }
    }
  })

  it('sin sesión abierta nadie tiene nada, ni siquiera con un token armado', () => {
    for (const rol of CATALOGO.roles) for (const s of [...SECCIONES, 'ver:tablero', 'ver:ayuda']) expect(alcanza(s, tokenDe(rol), false)).toBe(false)
  })

  it('segregación de funciones: quien resuelve admisiones (ADMIN_PLATAFORMA) no atiende reclamos ni ve billeteras de terceros, y viceversa', () => {
    const admin = tokenDe('ADMIN_PLATAFORMA')
    expect(alcanza('ADMIN_PLATAFORMA', admin, true)).toBe(true)
    expect(alcanza('RECLAMO_ATENDER', admin, true)).toBe(false)
    expect(alcanza('BILLETERA_VER_TERCEROS', admin, true)).toBe(false)
    expect(alcanza('ADMIN_PLATAFORMA', tokenDe('SOPORTE'), true)).toBe(false)
    expect(alcanza('ver:admisiones', tokenDe('SOPORTE'), true)).toBe(false)
  })

  it('billetera de un tercero: solo la ven los roles que el catálogo habilita, y quien solo atiende reclamos por otra vía no', () => {
    const quienes = [...CATALOGO.roles].filter((r) => alcanza('BILLETERA_VER_TERCEROS', tokenDe(r), true)).sort()
    expect(quienes).toEqual(['ANALISTA_CUMPLIMIENTO', 'AUDITOR_INTERNO', 'CONTABILIDAD', 'OFICIAL_CUMPLIMIENTO', 'PUNTO_RECLAMO', 'RESPONSABLE_RIESGOS', 'SOPORTE', 'TESORERIA'])
  })
})

function fuentes(dir: string): string[] {
  return readdirSync(dir, { withFileTypes: true }).flatMap((e) => {
    const ruta = join(dir, e.name)
    if (e.isDirectory()) return fuentes(ruta)
    return e.name.endsWith('.ts') && !e.name.endsWith('.spec.ts') ? [ruta] : []
  })
}

/**
 * Permisos que el backoffice pide y que el catálogo NO tiene. Hoy: ninguno. `RECLAMO_VER`, `SOLICITUD_INGRESO_VER` y
 * `SOLICITUD_INGRESO_RESOLVER` entraron al catálogo (`sql/60_semillas/10-roles-y-permisos.sql`) y esta lista se vació.
 * Solo puede ENCOGER: un permiso nuevo pedido sin catálogo hace fallar la prueba, no se agrega acá en silencio.
 */
const SIN_CATALOGO_DECLARADOS: string[] = []

describe('contrato de permisos · lo que el backoffice pide existe en el catálogo', () => {
  const literales = new Set(
    fuentes(join(__dirname, '..'))
      .flatMap((f) => [...readFileSync(f, 'utf8').matchAll(/\b(?:puede|requierePermiso|alcanza)\('([A-Za-z_:]+)'/g)].map((m) => m[1]!))
      .filter((p) => !p.startsWith('ver:')),
  )

  it('hay literales que revisar (la búsqueda no está vacía)', () => {
    expect(literales.size).toBeGreaterThan(10)
  })

  it('cada permiso o rol que el código pide está en el catálogo, salvo los declarados como pendientes', () => {
    const desconocidos = [...literales].filter((p) => !CATALOGO.permisos.has(p) && !CATALOGO.roles.has(p)).sort()
    expect(desconocidos).toEqual([...SIN_CATALOGO_DECLARADOS].sort())
  })

  it('cada permiso de PERMISOS_POR_SECCION existe en el catálogo (un nombre inventado dejaría una sección cerrada para todos)', () => {
    const todos = Object.values(PERMISOS_POR_SECCION).flat()
    expect(todos.filter((p) => !CATALOGO.permisos.has(p) && !CATALOGO.roles.has(p))).toEqual([])
  })

  it('la lista de pendientes no se pudre: ninguno de los declarados está ya en el catálogo', () => {
    expect(SIN_CATALOGO_DECLARADOS.filter((p) => CATALOGO.permisos.has(p) || CATALOGO.roles.has(p))).toEqual([])
  })

  it('los tres permisos que antes faltaban (RECLAMO_VER y SOLICITUD_INGRESO_*) están sembrados y los concede el rol que corresponde', () => {
    const quienes = (permiso: string) => [...CATALOGO.roles].filter((r) => tokenDe(r).includes(permiso)).sort()
    expect(CATALOGO.permisos.has('RECLAMO_VER')).toBe(true)
    expect(quienes('RECLAMO_VER')).toEqual(['ADMIN_PLATAFORMA', 'OFICIAL_CUMPLIMIENTO', 'PUNTO_RECLAMO', 'SOPORTE'])
    expect(quienes('SOLICITUD_INGRESO_VER')).toEqual(['ADMIN_PLATAFORMA', 'SOPORTE'])
    // Resolver es de quien resuelve admisiones (ADMIN_PLATAFORMA) y de nadie más: quien solo mira no decide.
    expect(quienes('SOLICITUD_INGRESO_RESOLVER')).toEqual(['ADMIN_PLATAFORMA'])
  })

  it('cada ruta de operación que pide uno de esos permisos queda alcanzable desde la sección: quien lo tiene ve «operacion»', () => {
    for (const permiso of ['RECLAMO_VER', 'SOLICITUD_INGRESO_VER', 'SOLICITUD_INGRESO_RESOLVER']) {
      expect(PERMISOS_POR_SECCION['ver:operacion']).toContain(permiso)
      expect(alcanza('ver:operacion', [permiso], true), `un token con solo ${permiso} debe abrir operación`).toBe(true)
    }
  })
})
