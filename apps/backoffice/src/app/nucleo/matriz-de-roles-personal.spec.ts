import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'
import { PERMISOS_DE_PERSONAL } from '../rutas/sistemas/personal-y-accesos'
import { leerCatalogoDeRoles } from './catalogo-de-roles.pruebas'
import { MENU } from './menu'
import { entradasVisibles, pantallasVisibles } from './navegacion'
import { alcanza } from './secciones'

const CATALOGO = leerCatalogoDeRoles(readFileSync(join(process.cwd(), '../../sql/60_semillas/10-roles-y-permisos.sql'), 'utf8'))
const { tokenDe } = CATALOGO

/**
 * GU-09 · «Personal y accesos» se abre por permiso. Se arma el token de cada rol del catálogo y se pregunta al menú real
 * qué pestañas de la sección ve: nadie ve una pantalla cuyo endpoint le respondería 403.
 */
describe('matriz de roles · «Personal y accesos» (GU-09)', () => {
  const credenciales = (rol: string) => ({ puede: (p: string) => alcanza(p, tokenDe(rol), true), rol: () => 'OPERADOR' })
  const entrada = MENU.flatMap((g) => g.entradas).find((e) => e.ruta === 'personal')!
  const pestanasDe = (rol: string) => pantallasVisibles(entrada, credenciales(rol)).map((p) => p.ruta.replace('/sistemas/', ''))
  const veLaSeccion = (rol: string) => entradasVisibles(credenciales(rol)).some((g) => g.entradas.some((e) => e.ruta === 'personal'))

  it('correcto: INTERNAL_IDENTITY_ADMIN y ADMIN_PLATAFORMA ven la sección con sus cuatro pantallas', () => {
    for (const rol of ['INTERNAL_IDENTITY_ADMIN', 'ADMIN_PLATAFORMA']) {
      expect(veLaSeccion(rol), rol).toBe(true)
      expect(pestanasDe(rol), rol).toEqual(['usuarios-internos', 'roles-internos', 'permisos-internos', 'accesos'])
    }
  })

  it('inválido: RESPONSABLE_SEGURIDAD entra a Sistemas pero NO ve «Personal y accesos» (el servidor le respondería 403)', () => {
    expect(alcanza('ver:sistemas', tokenDe('RESPONSABLE_SEGURIDAD'), true)).toBe(true)
    expect(veLaSeccion('RESPONSABLE_SEGURIDAD')).toBe(false)
    expect(pestanasDe('RESPONSABLE_SEGURIDAD')).toEqual([])
  })

  it('límite: cada pestaña aparece si y solo si el rol tiene un permiso que el servidor acepta para ella', () => {
    for (const rol of CATALOGO.roles) {
      const visibles = pestanasDe(rol)
      for (const [pantalla, permisos] of Object.entries(PERMISOS_DE_PERSONAL)) {
        const alcanzaUno = permisos.some((p) => tokenDe(rol).includes(p))
        expect(visibles.includes(pantalla), `${rol} · ${pantalla}`).toBe(alcanzaUno && alcanza('ver:sistemas', tokenDe(rol), true))
      }
    }
  })

  it('el conjunto exacto de roles que ven la sección queda fijado (si cambia el catálogo, hay que decidirlo)', () => {
    expect([...CATALOGO.roles].filter(veLaSeccion).sort()).toEqual(['ADMIN_PLATAFORMA', 'INTERNAL_IDENTITY_ADMIN'])
  })
})
