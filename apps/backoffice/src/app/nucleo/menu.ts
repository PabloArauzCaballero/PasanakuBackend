import type { Miga } from '@aportaya/ui/migas/migas'
import type { Seccion as NombreDeSeccion } from '../layout/icono-de-seccion'

/** Una pantalla dentro de una sección: la ruta relativa a la sección, su nombre en el menú y su respectivo ícono. */
export type Subruta = { ruta: string; texto: string; icono?: NombreDeSeccion }
/** Una sección del backoffice: su ruta, su ícono, el permiso `ver:*` que la abre y sus pantallas. */
export type Entrada = { ruta: NombreDeSeccion; destino: string; texto: string; permiso: string; hijos?: Subruta[] }
export type Grupo = { titulo: string | null; icono: NombreDeSeccion; entradas: Entrada[] }

/**
 * El árbol del menú, con los mismos grupos que el portal interno de Atlas adaptados a lo que
 * tiene AportaYa: grupos organizados, una entrada por sección y, debajo, sus pantallas siempre abiertas con ícono propio.
 *
 * **El menú oculta por comodidad**; el permiso real lo exige `canMatch` en cada ruta y lo verifica
 * el servidor. Los `ver:*` se traducen en `secciones.ts`.
 */
export const MENU: readonly Grupo[] = [
  { titulo: null, icono: 'tablero', entradas: [{ ruta: 'tablero', destino: '/tablero', texto: 'Inicio', permiso: 'ver:tablero' }] },
  {
    titulo: 'Operación',
    icono: 'operacion',
    entradas: [
      {
        ruta: 'operacion',
        destino: '/operacion',
        texto: 'Operación',
        permiso: 'ver:operacion',
        hijos: [
          { ruta: 'estado', texto: 'Arquitectura y estado', icono: 'estado' },
          { ruta: 'retiros', texto: 'Retiros en revisión', icono: 'retiros' },
          { ruta: 'reclamos', texto: 'Reclamos', icono: 'reclamos' },
          { ruta: 'solicitudes-escaladas', texto: 'Solicitudes escaladas', icono: 'solicitudesEscaladas' },
          { ruta: 'custodia', texto: 'Conciliación de custodia', icono: 'custodia' },
          { ruta: 'cierres-diarios', texto: 'Cierres diarios', icono: 'cierresDiarios' },
          { ruta: 'reversos', texto: 'Reversos', icono: 'reversos' },
          { ruta: 'desembolsos', texto: 'Desembolsos', icono: 'desembolsos' },
        ],
      },
      { ruta: 'admisiones', destino: '/admisiones', texto: 'Admisiones', permiso: 'ver:admisiones' },
    ],
  },
  {
    titulo: 'Control',
    icono: 'cumplimiento',
    entradas: [
      {
        ruta: 'cumplimiento',
        destino: '/cumplimiento',
        texto: 'Cumplimiento',
        permiso: 'ver:cumplimiento',
        hijos: [
          { ruta: 'verificaciones', texto: 'Verificación de identidad', icono: 'verificaciones' },
          { ruta: 'alertas', texto: 'Alertas de monitoreo', icono: 'alertas' },
          { ruta: 'casos', texto: 'Casos de investigación', icono: 'casos' },
          { ruta: 'uif/reportes', texto: 'Reportes a la UIF', icono: 'uifReportes' },
          { ruta: 'uif/requerimientos', texto: 'Requerimientos de autoridad', icono: 'uifRequerimientos' },
          { ruta: 'continuidad', texto: 'Pruebas de continuidad', icono: 'continuidad' },
          { ruta: 'actas', texto: 'Actas de comité', icono: 'actas' },
          { ruta: 'riesgos/eventos', texto: 'Eventos de riesgo operativo', icono: 'riesgosEventos' },
          { ruta: 'seguridad/incidentes', texto: 'Incidentes de seguridad', icono: 'seguridadIncidentes' },
        ],
      },
      {
        ruta: 'sistemas',
        destino: '/sistemas',
        texto: 'Sistemas',
        permiso: 'ver:sistemas',
        hijos: [
          { ruta: 'servicios', texto: 'Servicios y SLO', icono: 'servicios' },
          { ruta: 'despliegues', texto: 'Despliegues e interruptores', icono: 'despliegues' },
          { ruta: 'base-de-datos', texto: 'Base y migraciones', icono: 'baseDeDatos' },
          { ruta: 'respaldos', texto: 'Respaldos', icono: 'respaldos' },
          { ruta: 'proveedores', texto: 'Proveedores', icono: 'proveedores' },
          { ruta: 'outbox', texto: 'Outbox y descartados', icono: 'outbox' },
          { ruta: 'webhooks', texto: 'Webhooks', icono: 'webhooks' },
          { ruta: 'accesos', texto: 'Accesos', icono: 'accesos' },
          { ruta: 'incidentes', texto: 'Incidentes', icono: 'incidentes' },
          { ruta: 'usuarios-internos', texto: 'Usuarios internos', icono: 'usuariosInternos' },
          { ruta: 'roles-internos', texto: 'Roles internos', icono: 'rolesInternos' },
          { ruta: 'permisos-internos', texto: 'Permisos internos', icono: 'permisosInternos' },
          { ruta: 'operaciones', texto: 'Operaciones del sistema', icono: 'operaciones' },
          { ruta: 'flujos', texto: 'Mapa de flujos', icono: 'flujos' },
          { ruta: 'bitacora', texto: 'Registro del sistema', icono: 'bitacora' },
          { ruta: 'catalogos-operativos', texto: 'Catálogos operativos', icono: 'catalogosOperativos' },
        ],
      },
    ],
  },
  {
    titulo: 'Datos',
    icono: 'catalogo',
    entradas: [{ ruta: 'catalogo', destino: '/tablero/catalogo-de-datos', texto: 'Catálogo de datos', permiso: 'ver:catalogo' }],
  },
  {
    titulo: 'Finanzas',
    icono: 'contabilidad',
    entradas: [
      {
        ruta: 'contabilidad',
        destino: '/contabilidad',
        texto: 'Contabilidad',
        permiso: 'ver:contabilidad',
        hijos: [
          { ruta: '', texto: 'Períodos contables', icono: 'periodos' },
          { ruta: 'presupuesto', texto: 'Presupuesto', icono: 'presupuesto' },
          { ruta: 'compras', texto: 'Compras y cuentas por pagar', icono: 'compras' },
          { ruta: 'cobros', texto: 'Cuentas por cobrar', icono: 'cobros' },
        ],
      },
      {
        ruta: 'publicidad',
        destino: '/publicidad',
        texto: 'Publicidad',
        permiso: 'ver:publicidad',
        hijos: [
          { ruta: 'anunciantes', texto: 'Anunciantes', icono: 'anunciantes' },
          { ruta: 'campanas', texto: 'Campañas', icono: 'campanas' },
          { ruta: 'moderacion', texto: 'Moderación', icono: 'moderacion' },
          { ruta: 'desempeno', texto: 'Desempeño', icono: 'desempeno' },
          { ruta: 'liquidacion', texto: 'Liquidación', icono: 'liquidacion' },
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
        ruta: 'configuracion',
        destino: '/configuracion',
        texto: 'Configuración',
        permiso: 'ver:configuracion',
        hijos: [
          { ruta: 'tarifario', texto: 'Tarifario', icono: 'tarifario' },
          { ruta: 'mensajeria', texto: 'Mensajería', icono: 'mensajeria' },
          { ruta: 'reportes', texto: 'Reportes', icono: 'reportes' },
        ],
      },
      { ruta: 'ayuda', destino: '/ayuda', texto: 'Ayuda', permiso: 'ver:ayuda' },
    ],
  },
]

/** La entrada del menú que contiene la URL (la de destino más largo que la prefija). */
export function entradaDe(url: string): Entrada | undefined {
  const ruta = url.split('?')[0]!
  return MENU.flatMap((g) => g.entradas)
    .filter((e) => ruta === e.destino || ruta.startsWith(e.destino + '/'))
    .sort((a, b) => b.destino.length - a.destino.length)[0]
}

/**
 * Las migas de pan de una URL: Inicio › Sección › Pantalla. La pantalla sale del menú si la URL es
 * una de sus subrutas; si es una ficha (`/casos/123`), sale del título de la página que trae la
 * ruta (sin el sufijo de marca). En el inicio no hay migas que mostrar.
 */
export function migasDe(url: string, tituloDePagina: string): Miga[] {
  const ruta = url.split('?')[0]!
  const entrada = entradaDe(ruta)
  const titulo = tituloDePagina.replace(/\s*·\s*(Sistemas\s*·\s*)?AportaYa\s*$/, '').trim()
  if (!entrada || entrada.destino === '/tablero') return []
  const migas: Miga[] = [{ texto: 'Inicio', ruta: '/tablero' }]
  const resto = ruta.slice(entrada.destino.length).replace(/^\//, '')
  const hijo = entrada.hijos?.filter((h) => h.ruta !== '' && (resto === h.ruta || resto.startsWith(h.ruta + '/'))).sort((a, b) => b.ruta.length - a.ruta.length)[0]
  if (!hijo && (resto === '' || entrada.hijos?.some((h) => h.ruta === '' && resto === ''))) {
    migas.push({ texto: titulo || entrada.texto })
    return migas
  }
  migas.push({ texto: entrada.texto, ruta: entrada.destino })
  if (hijo && resto !== hijo.ruta) {
    migas.push({ texto: hijo.texto, ruta: `${entrada.destino}/${hijo.ruta}` })
    migas.push({ texto: titulo || hijo.texto })
  } else {
    migas.push({ texto: hijo?.texto ?? titulo ?? entrada.texto })
  }
  return migas
}
