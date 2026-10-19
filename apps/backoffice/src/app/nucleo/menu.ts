import type { Miga } from '@aportaya/ui/migas/migas'
import type { Seccion as NombreDeSeccion } from '../layout/icono-de-seccion'

/** Una pantalla dentro de una sección: la ruta relativa a la sección y su nombre en el menú. */
export type Subruta = { ruta: string; texto: string }
/** Una sección del backoffice: su ruta, su ícono, el permiso `ver:*` que la abre y sus pantallas. */
export type Entrada = { ruta: NombreDeSeccion; destino: string; texto: string; permiso: string; hijos?: Subruta[] }
export type Grupo = { titulo: string | null; icono: NombreDeSeccion; entradas: Entrada[] }

/**
 * El árbol del menú, con los mismos grupos que el portal interno de Atlas adaptados a lo que
 * tiene AportaYa: grupos plegables, una entrada por sección y, debajo, sus pantallas.
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
          { ruta: 'estado', texto: 'Arquitectura y estado' },
          { ruta: 'retiros', texto: 'Retiros en revisión' },
          { ruta: 'reclamos', texto: 'Reclamos' },
          { ruta: 'solicitudes-escaladas', texto: 'Solicitudes escaladas' },
          { ruta: 'custodia', texto: 'Conciliación de custodia' },
          { ruta: 'cierres-diarios', texto: 'Cierres diarios' },
          { ruta: 'reversos', texto: 'Reversos' },
          { ruta: 'desembolsos', texto: 'Desembolsos' },
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
          { ruta: 'verificaciones', texto: 'Verificación de identidad' },
          { ruta: 'alertas', texto: 'Alertas de monitoreo' },
          { ruta: 'casos', texto: 'Casos de investigación' },
          { ruta: 'uif/reportes', texto: 'Reportes a la UIF' },
          { ruta: 'uif/requerimientos', texto: 'Requerimientos de autoridad' },
          { ruta: 'continuidad', texto: 'Pruebas de continuidad' },
          { ruta: 'actas', texto: 'Actas de comité' },
          { ruta: 'riesgos/eventos', texto: 'Eventos de riesgo operativo' },
          { ruta: 'seguridad/incidentes', texto: 'Incidentes de seguridad' },
        ],
      },
      {
        ruta: 'sistemas',
        destino: '/sistemas',
        texto: 'Sistemas',
        permiso: 'ver:sistemas',
        hijos: [
          { ruta: 'servicios', texto: 'Servicios y SLO' },
          { ruta: 'despliegues', texto: 'Despliegues e interruptores' },
          { ruta: 'base-de-datos', texto: 'Base y migraciones' },
          { ruta: 'respaldos', texto: 'Respaldos' },
          { ruta: 'proveedores', texto: 'Proveedores' },
          { ruta: 'outbox', texto: 'Outbox y descartados' },
          { ruta: 'webhooks', texto: 'Webhooks' },
          { ruta: 'accesos', texto: 'Accesos' },
          { ruta: 'incidentes', texto: 'Incidentes' },
          { ruta: 'usuarios-internos', texto: 'Usuarios internos' },
          { ruta: 'roles-internos', texto: 'Roles internos' },
          { ruta: 'permisos-internos', texto: 'Permisos internos' },
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
          { ruta: '', texto: 'Períodos contables' },
          { ruta: 'presupuesto', texto: 'Presupuesto' },
          { ruta: 'compras', texto: 'Compras y cuentas por pagar' },
          { ruta: 'cobros', texto: 'Cuentas por cobrar' },
        ],
      },
      {
        ruta: 'publicidad',
        destino: '/publicidad',
        texto: 'Publicidad',
        permiso: 'ver:publicidad',
        hijos: [
          { ruta: 'anunciantes', texto: 'Anunciantes' },
          { ruta: 'campanas', texto: 'Campañas' },
          { ruta: 'moderacion', texto: 'Moderación' },
          { ruta: 'desempeno', texto: 'Desempeño' },
          { ruta: 'liquidacion', texto: 'Liquidación' },
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
          { ruta: 'tarifario', texto: 'Tarifario' },
          { ruta: 'mensajeria', texto: 'Mensajería' },
          { ruta: 'reportes', texto: 'Reportes' },
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
