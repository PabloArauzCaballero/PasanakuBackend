/**
 * H12.S1.M1 · recorrido por roles sin acciones ajenas.
 *
 * La fuente es el catálogo REAL de roles y permisos (`sql/60_semillas/10-roles-y-permisos.sql`, generado desde
 * `seeders/minimos`): el token de `identidad` lleva los permisos efectivos del rol MÁS el código de cada rol vigente
 * (`AccesosRepositorio.codigosDeRolesVigentes`). Esta prueba arma ese token para cada rol y comprueba qué secciones
 * abre y qué acciones habilita. Es una prueba de la LÓGICA DEL CLIENTE sobre el catálogo versionado; no consulta un
 * `identidad` real ni prueba que el servidor niegue lo que el cliente oculta (eso es de cada servicio).
 */
export function leerCatalogoDeRoles(semilla: string) {
  const roles = new Set([...semilla.slice(0, semilla.indexOf('INSERT INTO permiso')).matchAll(/^\s*\('([A-Z_]+)',/gm)].map((m) => m[1]!))
  const permisos = new Set(
    [...semilla.slice(semilla.indexOf('INSERT INTO permiso'), semilla.indexOf('INSERT INTO rol_permiso')).matchAll(/^\s*\('([A-Z_]+)',/gm)].map((m) => m[1]!),
  )
  const concedidos = new Map<string, string[]>()
  for (const m of semilla.matchAll(/FROM rol WHERE codigo = '([A-Z_]+)'\)\s*,\s*\(SELECT id FROM permiso WHERE codigo = '([A-Z_]+)'\)/g)) {
    concedidos.set(m[1]!, [...(concedidos.get(m[1]!) ?? []), m[2]!])
  }
  const tokenDe = (rol: string): string[] => [rol, ...(concedidos.get(rol) ?? [])]
  return { roles, permisos, tokenDe }
}
