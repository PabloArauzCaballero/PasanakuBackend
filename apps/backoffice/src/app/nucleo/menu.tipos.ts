import type { NombreDeIcono } from '@aportaya/ui/icono/icono'
import type { Seccion as NombreDeSeccion } from '../layout/icono-de-seccion'

/**
 * Una condición de acceso: alcanza con **uno** de sus permisos (`Sesion.puede`) o con que el rol de la sesión sea
 * uno de sus `roles`. Es la misma forma que tienen las guardias de las rutas (`requierePermiso`, `requiereAlguno`,
 * `soloRolesDeSistemas`), escrita como dato para que el menú pueda preguntar sin navegar.
 */
export type Requisito = { readonly permisos?: readonly string[]; readonly roles?: readonly string[] }
/** Lo que el menú necesita de la sesión. `Sesion` lo cumple tal cual. */
export type Credenciales = { puede(permiso: string): boolean; rol(): string | null }

/**
 * Una pestaña de sección. `ruta` es absoluta (una pestaña puede vivir en otra rama de URL: «Admisiones a grupos» es
 * `/admisiones` y vive en Operación; «Catálogo de datos» es `/tablero/...` y vive en Configuración). `requiere` es la **cadena entera** de guardias que la ruta
 * atraviesa (sección y pantalla): todas tienen que cumplirse. Una pestaña con `grupo` no tiene ruta propia: lleva a la
 * primera de su grupo que la sesión puede abrir y despliega las demás como subpestañas.
 */
export type Pantalla = {
  readonly ruta: string
  readonly texto: string
  readonly icono: NombreDeIcono
  readonly requiere?: readonly Requisito[]
  readonly grupo?: readonly Pantalla[]
}
/**
 * Una entrada del menú lateral. Con `pantallas`, el enlace va a la primera pestaña que la sesión puede abrir, y la
 * entrada se oculta si no puede abrir ninguna; sin `pantallas`, va a `destino` si cumple `requiere`. `prefijos` son las
 * URLs que pertenecen a la sección aunque no sean una pestaña (fichas, raíces, pantallas que solo se abren por enlace).
 */
export type Entrada = {
  readonly ruta: NombreDeSeccion
  readonly texto: string
  readonly prefijos: readonly string[]
  readonly destino?: string
  readonly requiere?: readonly Requisito[]
  readonly pantallas?: readonly Pantalla[]
}
export type Grupo = { titulo: string | null; icono: NombreDeSeccion; entradas: readonly Entrada[] }
