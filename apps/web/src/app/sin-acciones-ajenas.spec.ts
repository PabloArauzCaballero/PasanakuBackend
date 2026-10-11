import { readdirSync, readFileSync, statSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'
import { routes } from './app.routes'

/**
 * H12.S1.M1 · lo que la web pública NO es: no es un backoffice ni una app con sesión. Un participante, un
 * administrador de grupo o un operador de backoffice que abre este sitio no encuentra acciones de nadie: ni rutas
 * de operación, ni sesión, ni guardas de rol, ni llamadas que muten (la única acción del sitio —canjear una
 * invitación— se hace en la app). Es una prueba de ESTRUCTURA del cliente, no de autorización del servidor.
 */
const ARCHIVOS = (c: string): string[] =>
  readdirSync(c).flatMap((e) => (statSync(join(c, e)).isDirectory() ? ARCHIVOS(join(c, e)) : [join(c, e)]))
const FUENTES = ARCHIVOS(__dirname).filter((f) => f.endsWith('.ts') && !f.endsWith('.spec.ts'))

describe('web pública · sin acciones ajenas a ningún rol', () => {
  it('no expone ninguna ruta de operación del backoffice', () => {
    const paths = routes.map((r) => r.path ?? '')
    for (const prohibida of ['operacion', 'cumplimiento', 'admisiones', 'sistemas', 'contabilidad', 'publicidad', 'ingreso', 'tablero']) {
      expect(paths.filter((p) => p === prohibida || p.startsWith(`${prohibida}/`)), prohibida).toEqual([])
    }
  })

  it('ninguna ruta está detrás de una guarda de sesión o de rol: todo es público y lo que muestra lo decide el servidor', () => {
    for (const r of routes) {
      expect(r.canMatch ?? r.canActivate ?? r.canLoad, `la ruta "${r.path}" no debe depender de una guarda de rol en el cliente`).toBeUndefined()
    }
  })

  it('el código del sitio no hace escrituras HTTP (POST/PUT/PATCH/DELETE): solo lee contenido y verificadores', () => {
    const escrituras = FUENTES.filter((f) => /\.(post|put|patch|delete)\s*[<(]/.test(readFileSync(f, 'utf8')))
    expect(escrituras.map((f) => f.replace(__dirname, ''))).toEqual([])
  })

  it('no hay botón que dispare una acción en la página de invitación: aceptar se hace en la app', () => {
    const invitacion = readFileSync(join(__dirname, 'paginas/invitacion/invitacion.ts'), 'utf8')
    expect(invitacion).not.toMatch(/<button|\(click\)|\(submit\)/)
    expect(invitacion).toContain('ap-enlace-a-la-app')
  })
})
