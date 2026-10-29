import type { NombreDeIcono } from '@aportaya/ui/icono/icono'
import type { Seccion as NombreDeSeccion } from '../layout/icono-de-seccion'
import { ENTRADAS_DE_CUMPLIMIENTO } from '../rutas/cumplimiento/compartido/navegacion'

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

// La cadena de cada sección, copiada de `app.routes.ts` y de cada `*.routes.ts`. `menu-y-guardias.spec.ts` navega con
// el router real y falla si una pestaña visible rebota o una oculta abre: así esta copia no puede divergir en silencio.
const p = (...permisos: string[]): Requisito => ({ permisos })
/** `requiereAlguno` de cumplimiento: el código vale como permiso o como rol. */
const pr = (...codigos: string[]): Requisito => ({ permisos: codigos, roles: codigos })
const OPERACION = p('ver:operacion')
const CUMPLIMIENTO = p('ver:cumplimiento')
const CONTABILIDAD = p('ver:contabilidad')
const PUBLICIDAD = p('ver:publicidad')
const CONFIGURACION = p('ver:configuracion')
/**
 * `ver:sistemas` y además rol PLATAFORMA o SEGURIDAD, o el rol de catálogo que identidad agrega a `permisos`
 * (`rutas/sistemas/guardia-rol-sistemas.ts`: el claim `rol` del token real es siempre `OPERADOR`).
 */
const SISTEMAS = [p('ver:sistemas'), { roles: ['PLATAFORMA', 'SEGURIDAD'], permisos: ['ADMIN_PLATAFORMA', 'RESPONSABLE_SEGURIDAD'] }] as const

const sis = (ruta: string, texto: string, icono: NombreDeIcono): Pantalla => ({ ruta: `/sistemas/${ruta}`, texto, icono, requiere: SISTEMAS })
/**
 * Las pestañas de Cumplimiento toman sus códigos de `ENTRADAS_DE_CUMPLIMIENTO` (la lista de la franja C, que también
 * usa `cumplimiento.routes.ts` en sus guardias): una sola lista para la ruta y para la pestaña.
 */
const cum = (ruta: string, texto: string, icono: NombreDeIcono): Pantalla => {
  const codigos = ENTRADAS_DE_CUMPLIMIENTO.find((e) => e.ruta === `/cumplimiento/${ruta}`)?.codigos ?? []
  return { ruta: `/cumplimiento/${ruta}`, texto, icono, requiere: [CUMPLIMIENTO, pr(...codigos)] }
}

/**
 * El árbol del menú: seis entradas (sin «Inicio» desde 2026-10-28: se vuelve con el logo) y hasta ocho pestañas por
 * sección (A1, 2026-10-26: antes eran diez entradas y hasta catorce pestañas, con duplicados). Lo que salió del menú **sigue respondiendo por URL**: ni una ruta ni un
 * permiso cambió. El porqué de cada recorte está en `docs/trabajo/2026-10-26-backoffice-listados-y-navegacion/franjas/a.md`.
 *
 * **El menú oculta por comodidad**; el permiso real lo exige `canMatch` en cada ruta y lo verifica el servidor.
 */
export const MENU: readonly Grupo[] = [
  {
    titulo: null,
    icono: 'tablero',
    entradas: [
      {
        ruta: 'operacion',
        texto: 'Operación',
        prefijos: ['/operacion', '/admisiones'],
        pantallas: [
          { ruta: '/operacion/retiros', texto: 'Retiros en revisión', icono: 'retiro', requiere: [OPERACION, p('RETIRO_APROBAR')] },
          { ruta: '/operacion/desembolsos', texto: 'Desembolsos', icono: 'entrega', requiere: [OPERACION, p('ENTREGA_EJECUTAR', 'AUDITORIA_LEER')] },
          { ruta: '/operacion/reversos', texto: 'Reversos', icono: 'deshacer', requiere: [OPERACION, p('AUDITORIA_LEER')] },
          { ruta: '/operacion/reclamos', texto: 'Reclamos', icono: 'mensaje', requiere: [OPERACION, p('RECLAMO_VER')] },
          // Custodia y cierre diario siguen en Operación: los abre `ver:operacion` (TESORERIA, CONTABILIDAD). En la entrada
          // Contabilidad le mostraban esa sección a roles sin `ver:contabilidad` (el administrador de plataforma).
          { ruta: '/operacion/custodia', texto: 'Conciliación de custodia', icono: 'banco', requiere: [OPERACION, p('AUDITORIA_LEER')] },
          { ruta: '/operacion/cierres-diarios', texto: 'Cierres diarios', icono: 'reloj', requiere: [OPERACION, p('AUDITORIA_LEER')] },
          { ruta: '/admisiones', texto: 'Admisiones a grupos', icono: 'personaMas', requiere: [p('ver:admisiones')] },
          { ruta: '/operacion/solicitudes-escaladas', texto: 'Solicitudes escaladas', icono: 'bandera', requiere: [OPERACION, p('SOLICITUD_INGRESO_VER')] },
        ],
      },
      {
        ruta: 'cumplimiento',
        texto: 'Cumplimiento',
        prefijos: ['/cumplimiento'],
        pantallas: [
          cum('verificaciones', 'Verificación de identidad', 'verificado'),
          cum('alertas', 'Alertas de monitoreo', 'alerta'),
          cum('casos', 'Casos de investigación', 'carpeta'),
          cum('uif/reportes', 'Reportes a la UIF', 'enviar'),
          cum('uif/requerimientos', 'Requerimientos de autoridad', 'balanza'),
          cum('actas', 'Actas de comité', 'documento'),
          {
            ruta: '/cumplimiento/riesgos/eventos',
            texto: 'Riesgo y seguridad',
            icono: 'escudo',
            grupo: [
              cum('riesgos/eventos', 'Eventos de riesgo operativo', 'advertencia'),
              cum('continuidad', 'Pruebas de continuidad', 'actividad'),
              cum('seguridad/incidentes', 'Incidentes de seguridad', 'candado'),
            ],
          },
        ],
      },
      {
        ruta: 'contabilidad',
        texto: 'Contabilidad',
        prefijos: ['/contabilidad'],
        pantallas: [
          { ruta: '/contabilidad', texto: 'Períodos contables', icono: 'calendario', requiere: [CONTABILIDAD] },
          { ruta: '/contabilidad/presupuesto', texto: 'Presupuesto', icono: 'grafico', requiere: [CONTABILIDAD, p('CONTABILIDAD_ERP_PRESUPUESTO')] },
          { ruta: '/contabilidad/compras', texto: 'Compras y cuentas por pagar', icono: 'recibo', requiere: [CONTABILIDAD, p('CONTABILIDAD_ERP_COMPRAS')] },
          { ruta: '/contabilidad/cobros', texto: 'Cuentas por cobrar', icono: 'moneda', requiere: [CONTABILIDAD, p('CONTABILIDAD_ERP_COBRAR')] },
        ],
      },
      {
        ruta: 'publicidad',
        texto: 'Publicidad',
        prefijos: ['/publicidad'],
        pantallas: [
          { ruta: '/publicidad/anunciantes', texto: 'Anunciantes', icono: 'megafono', requiere: [PUBLICIDAD, p('PUBLICIDAD_ANUNCIANTES')] },
          { ruta: '/publicidad/campanas', texto: 'Campañas', icono: 'bandera', requiere: [PUBLICIDAD, p('PUBLICIDAD_ANUNCIANTES')] },
          { ruta: '/publicidad/moderacion', texto: 'Moderación', icono: 'imagen', requiere: [PUBLICIDAD, p('PUBLICIDAD_MODERAR')] },
          { ruta: '/publicidad/desempeno', texto: 'Desempeño', icono: 'tendencia', requiere: [PUBLICIDAD, p('PUBLICIDAD_ANUNCIANTES')] },
          { ruta: '/publicidad/liquidacion', texto: 'Liquidación', icono: 'recibo', requiere: [PUBLICIDAD, p('PUBLICIDAD_ANUNCIANTES')] },
        ],
      },
    ],
  },
  // Última a propósito: es la sección a la que se vuelve, no por la que se empieza.
  {
    titulo: 'Plataforma',
    icono: 'configuracion',
    entradas: [
      {
        ruta: 'sistemas',
        texto: 'Sistemas',
        prefijos: ['/sistemas'],
        pantallas: [
          sis('servicios', 'Servicios y SLO', 'servidor'),
          sis('despliegues', 'Despliegues', 'interruptor'),
          sis('base-de-datos', 'Base de datos', 'baseDeDatos'),
          sis('respaldos', 'Respaldos', 'nube'),
          {
            ruta: '/sistemas/proveedores',
            texto: 'Integraciones',
            icono: 'enlace',
            grupo: [sis('proveedores', 'Proveedores', 'paquete'), sis('outbox', 'Outbox por servicio', 'bandeja'), sis('webhooks', 'Webhooks', 'enlace')],
          },
          {
            ruta: '/sistemas/usuarios-internos',
            texto: 'Personal y accesos',
            icono: 'escudo',
            grupo: [
              sis('usuarios-internos', 'Usuarios internos', 'persona'),
              sis('roles-internos', 'Roles internos', 'grupo'),
              sis('permisos-internos', 'Permisos internos', 'llave'),
              sis('accesos', 'Revisión de accesos', 'candado'),
            ],
          },
          // Metadata real de endpoints y flujos (fuera del menú desde A1); en grupo para no pasar de ocho pestañas.
          {
            ruta: '/sistemas/operaciones',
            texto: 'Endpoints y flujos',
            icono: 'libro',
            grupo: [sis('operaciones', 'Operaciones del sistema', 'lista'), sis('flujos', 'Mapa de flujos', 'relacion')],
          },
          sis('bitacora', 'Registro del sistema', 'historial'),
        ],
      },
      {
        ruta: 'configuracion',
        texto: 'Configuración',
        prefijos: ['/configuracion'],
        pantallas: [
          { ruta: '/configuracion/tarifario', texto: 'Tarifario', icono: 'etiqueta', requiere: [CONFIGURACION, p('TARIFARIO_PUBLICAR', 'AUDITORIA_LEER')] },
          { ruta: '/configuracion/mensajeria', texto: 'Mensajería', icono: 'correo', requiere: [CONFIGURACION, p('AUDITORIA_LEER')] },
          { ruta: '/configuracion/reportes', texto: 'Reportes', icono: 'documento', requiere: [CONFIGURACION, p('AUDITORIA_LEER')] },
          sis('catalogos-operativos', 'Catálogos operativos', 'engranaje'),
          { ruta: '/tablero/catalogo-de-datos', texto: 'Catálogo de datos', icono: 'baseDeDatos', requiere: [p('TABLERO_SISTEMAS_VER')] },
        ],
      },
    ],
  },
]
