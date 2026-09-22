import { inject } from '@angular/core'
import { CanMatchFn, Router } from '@angular/router'
import { Sesion } from '../../nucleo/sesion'

/**
 * El backoffice de sistemas es un producto propio de roles PLATAFORMA y SEGURIDAD — no
 * comparte usuarios con el financiero (delta D-2). Esta es la barrera de interfaz; la
 * de servidor la pone cada endpoint simulado devolviendo 403 (doble barrera). Un rol
 * financiero que intenta entrar por URL directa nunca llega a montar una pantalla de
 * sistemas: `canMatch` corta antes de cargar el chunk.
 */
const ROLES_PERMITIDOS = ['PLATAFORMA', 'SEGURIDAD'] as const

export const soloRolesDeSistemas: CanMatchFn = () => {
  const sesion = inject(Sesion)
  const router = inject(Router)
  const rol = sesion.rol()
  if (rol && (ROLES_PERMITIDOS as readonly string[]).includes(rol)) return true
  return router.parseUrl('/operacion')
}
