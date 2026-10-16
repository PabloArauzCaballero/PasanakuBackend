import { inject } from '@angular/core'
import { CanMatchFn, Router } from '@angular/router'
import { Sesion } from '../../nucleo/sesion'

/**
 * Como `requierePermiso` (`nucleo/permisos.ts`), pero con **alternativas**: la ruta monta si la sesión trae al menos uno.
 * Lo exige el contrato de `listarTarifarios` («TARIFARIO_PUBLICAR o AUDITORIA_LEER»); `canMatch` con dos guardias sería un
 * Y, no un O. Sin ninguno, al tablero (nunca una pantalla vacía). El servidor vuelve a decidir con su 403.
 */
export function requiereAlgunPermiso(...permisos: readonly string[]): CanMatchFn {
  return () => {
    if (permisos.some((p) => inject(Sesion).puede(p))) return true
    return inject(Router).parseUrl('/tablero')
  }
}
