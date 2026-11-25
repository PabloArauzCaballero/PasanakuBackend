/**
 * **Personal y accesos** (GU-09, decisión de Pablo 2026-11-24): la sección propia de quien gobierna a los operadores
 * internos. Se abre **por permiso**, no por rol de Sistemas: es lo que el servidor exige en cada endpoint
 * (`UsuariosInternosController`, `RolesController`, `PermisosController` y `RutasDeAccesos` de identidad), así la
 * interfaz no le muestra a nadie una pantalla que le respondería 403. Datos puros: los comparten el menú
 * (`nucleo/menu.ts`), la guardia (`guardia-rol-sistemas.ts`) y las pantallas (para esconder las acciones de gestión).
 */

/** Primer segmento de cada pantalla de la sección → permisos reales que la abren (alcanza **uno**). */
export const PERMISOS_DE_PERSONAL: Readonly<Record<string, readonly string[]>> = {
  'usuarios-internos': ['USUARIOS_INTERNOS_LEER', 'ACCESOS_ADMINISTRAR'],
  'roles-internos': ['ROLES_INTERNOS_LEER', 'ACCESOS_ADMINISTRAR'],
  'permisos-internos': ['PERMISOS_INTERNOS_LEER', 'ACCESOS_ADMINISTRAR'],
  accesos: ['ACCESOS_ADMINISTRAR', 'USUARIOS_INTERNOS_LEER'],
  'cuentas-de-servicio': ['ACCESOS_ADMINISTRAR', 'USUARIOS_INTERNOS_LEER'],
}

/** Todos los permisos que abren al menos una pantalla de la sección (los suma `nucleo/secciones.ts` a `ver:sistemas`). */
export const PERMISOS_QUE_ABREN_PERSONAL: readonly string[] = [...new Set(Object.values(PERMISOS_DE_PERSONAL).flat())]

/** Dar de alta, suspender, reactivar, desbloquear, dar de baja, asignar y revocar: lo que el servidor pide para mutar. */
export const PERMISOS_DE_GESTION_DE_PERSONAL: readonly string[] = ['USUARIOS_INTERNOS_GESTIONAR', 'ACCESOS_ADMINISTRAR']
