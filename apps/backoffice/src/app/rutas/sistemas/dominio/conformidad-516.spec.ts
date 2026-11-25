import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'
import type {
  EntradaAsignacionDeRol as GEntradaAsignacion,
  EntradaBajaUsuarioInterno as GEntradaBaja,
  EntradaRevocacionDeRol as GEntradaRevocacion,
  FilaDeAcceso as GFila,
  PaginaDeAccesos as GPagina,
  SalidaAsignacionDeRol as GSalidaAsignacion,
  SalidaBajaUsuarioInterno as GSalidaBaja,
  SalidaRevocacionDeRol as GSalidaRevocacion,
} from 'clientes/angular/identidad'
import type {
  EntradaAsignacionDeRol,
  EntradaBajaUsuarioInterno,
  EntradaRevocacionDeRol,
  FilaDeAcceso,
  PaginaDeAccesos,
  SalidaAsignacionDeRol,
  SalidaBajaUsuarioInterno,
  SalidaRevocacionDeRol,
} from './gobierno-de-accesos'

/**
 * H6.S1.M5 · el doble de `gobierno-de-accesos.ts` contra el contrato REAL de B (#516, ya en `dev`).
 * Nivel de tipos: mis tipos locales y los del cliente generado de `identidad.yaml` tienen las mismas claves y las
 * mismas obligatorias (si divergen, `yarn typecheck` falla acá). Nivel de datos: las respuestas de ejemplo del contrato
 * (`packages/simulado/ejemplos/identidad`) traen exactamente las claves que el doble declara.
 */
type Claves<T> = keyof T & string
type Obligatorias<T> = { [K in keyof T]-?: object extends Pick<T, K> ? never : K }[keyof T] & string
/** `true` solo si A y B declaran las mismas claves y las mismas son obligatorias. */
type Conforme<A, B> = [Claves<A>] extends [Claves<B>] ? ([Claves<B>] extends [Claves<A>] ? ([Obligatorias<A>] extends [Obligatorias<B>] ? ([Obligatorias<B>] extends [Obligatorias<A>] ? true : false) : false) : false) : false
const conformes: [
  Conforme<EntradaAsignacionDeRol, GEntradaAsignacion>,
  Conforme<SalidaAsignacionDeRol, GSalidaAsignacion>,
  Conforme<EntradaRevocacionDeRol, GEntradaRevocacion>,
  Conforme<SalidaRevocacionDeRol, GSalidaRevocacion>,
  Conforme<EntradaBajaUsuarioInterno, GEntradaBaja>,
  Conforme<SalidaBajaUsuarioInterno, GSalidaBaja>,
  Conforme<FilaDeAcceso, GFila>,
  Conforme<PaginaDeAccesos, GPagina>,
] = [true, true, true, true, true, true, true, true]

const EJEMPLOS = join(__dirname, '../../../../../../../packages/simulado/ejemplos/identidad')
const ok = (operacion: string): unknown => (JSON.parse(readFileSync(join(EJEMPLOS, `${operacion}.json`), 'utf8')) as { escenarios: { ok: { cuerpo: unknown } } }).escenarios.ok.cuerpo
const claves = (o: unknown) => Object.keys(o as object).sort()

describe('conformidad con el contrato real de B (#516)', () => {
  it('correcto: los ocho tipos del doble coinciden con los del cliente generado (claves y obligatorias)', () => {
    expect(conformes.every(Boolean)).toBe(true)
  })

  it('correcto: las respuestas de ejemplo del contrato traen las claves que el doble declara', () => {
    const asignacion = claves(ok('asignarRolDeOperador'))
    for (const k of ['asignacionId', 'usuarioId', 'rolCodigo', 'ambito', 'permisosEfectivos', 'requiereMfa', 'yaExistia']) expect(asignacion, k).toContain(k)
    const revocacion = claves(ok('revocarAsignacionDeRol'))
    for (const k of ['asignacionId', 'usuarioId', 'revocadaEn', 'sesionesCerradas', 'yaEstabaRevocada']) expect(revocacion, k).toContain(k)
    const baja = claves(ok('darDeBajaUsuarioInterno'))
    for (const k of ['usuarioId', 'estado', 'asignacionesRevocadas', 'sesionesCerradas', 'yaEstabaDeBaja']) expect(baja, k).toContain(k)
    const informe = ok('informeDeAccesos') as { items: unknown[] }
    for (const k of ['seccion', 'generadoEn', 'items', 'pagina', 'limite', 'total', 'totalPaginas']) expect(claves(informe), k).toContain(k)
    for (const k of ['asignacionId', 'usuarioId', 'nombre', 'estadoCuenta', 'rolCodigo', 'ambito', 'otorgadaPor', 'otorgadaEn', 'totpConfirmado']) expect(claves(informe.items[0]), k).toContain(k)
  })

  it('inválido: el informe de ejemplo no trae datos de contacto de la persona (solo el nombre)', () => {
    const fila = (ok('informeDeAccesos') as { items: object[] }).items[0]!
    expect(claves(fila)).not.toContain('correo')
    expect(claves(fila)).not.toContain('telefono')
  })
})
