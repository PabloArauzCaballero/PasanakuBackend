import { inject } from '@angular/core'
import { CanMatchFn, Router } from '@angular/router'
import { Sesion } from '../../../nucleo/sesion'

/**
 * Espejo, en la interfaz, de `GuardiaDePermiso` del servidor
 * (`plataforma/comun-web/.../GuardiaDePermiso.java`): un código alcanza si es el ROL de la
 * sesión o si está entre sus permisos, y `@PermisoAlternativo` suma otros códigos que
 * también alcanzan. Varias rutas de `cumplimiento.yaml` piden «RESPONSABLE_RIESGOS o
 * AUDITORIA_LEER» —un rol o un permiso—, y `requierePermiso` de `nucleo/` mira un único
 * permiso. **El rol oculta, no protege**: cada endpoint vuelve a decidir.
 */
export function tieneAlguno(sesion: Sesion, codigos: readonly string[]): boolean {
  return codigos.some((codigo) => sesion.rol() === codigo || sesion.puede(codigo))
}

/** `canMatch` con la misma regla: sin ninguno de los códigos, la ruta no monta y se vuelve al tablero. */
export function requiereAlguno(...codigos: string[]): CanMatchFn {
  return () => {
    if (tieneAlguno(inject(Sesion), codigos)) return true
    return inject(Router).parseUrl('/tablero')
  }
}
