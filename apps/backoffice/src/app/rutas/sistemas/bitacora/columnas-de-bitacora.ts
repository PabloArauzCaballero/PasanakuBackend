import type { ColumnaExportable } from '@aportaya/ui/exportar-listado/exportar'
import type { Opcion } from '@aportaya/ui/lista-de-opciones/lista-de-opciones'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { Tono } from '@aportaya/ui/tono/tono'
import type { EventoBitacora } from '../dominio/bitacora'

/** La fila de la tabla: el evento más las dos columnas que no son campos (actor resumido y acciones de fila). */
export type FilaBitacora = EventoBitacora & { actor?: string; acciones?: string }

export const COLUMNAS: Columna<FilaBitacora>[] = [
  { clave: 'secuencia', titulo: '#', numerica: true, ordenable: true },
  { clave: 'fechaHora', titulo: 'Fecha y hora', ordenable: true },
  { clave: 'entidad', titulo: 'Entidad', ordenable: true },
  { clave: 'accion', titulo: 'Acción', ordenable: true },
  { clave: 'actor', titulo: 'Actor' },
  { clave: 'origen', titulo: 'Origen', ordenable: true },
  { clave: 'hashRegistro', titulo: 'Hash' },
  { clave: 'acciones', titulo: 'Acciones', fijaDerecha: true },
]

/** CSV/JSON: solo las columnas que la tabla muestra (regla 90.2: no se exporta lo que no se ve), con el hash completo. */
export const EXPORTABLES: ColumnaExportable<FilaBitacora>[] = [
  { clave: 'secuencia', titulo: 'Secuencia' },
  { clave: 'fechaHora', titulo: 'Fecha y hora' },
  { clave: 'entidad', titulo: 'Entidad' },
  { clave: 'accion', titulo: 'Acción' },
  { clave: 'actorRol', titulo: 'Rol del actor' },
  { clave: 'origen', titulo: 'Origen' },
  { clave: 'hashRegistro', titulo: 'Hash del registro' },
]

const opciones = (valores: readonly string[]): Opcion[] => valores.map((v) => ({ valor: v, texto: v }))

/** Los valores que ya ofrecía el filtro (el contrato los declara como texto libre de hasta 50/30 caracteres). */
export const ENTIDADES = opciones(['USUARIO', 'GRUPO', 'CONFIGURACION', 'OBLIGACION_APORTE', 'ENTREGA_FONDO', 'CUENTA_BANCARIA', 'ORGANIZADOR', 'SOLICITUD_INGRESO', 'RECLAMO', 'CASO_INVESTIGACION'])
export const ACCIONES = opciones([
  'CREACION', 'ACTUALIZACION', 'CAMBIO_CONFIGURACION', 'CAMBIO_ESTADO', 'CAMBIO_PERMISOS', 'APROBACION', 'RECHAZO', 'SANCION',
  'ELIMINACION_LOGICA', 'INICIO_SESION', 'ACCESO_DATOS', 'EXPORTACION', 'CONCILIACION', 'DESEMBOLSO', 'LIQUIDACION', 'ANULACION',
])

export function tonoAccion(accion: string): Tono {
  if (accion === 'CREACION' || accion === 'APROBACION' || accion === 'CONCILIACION') return 'ok'
  if (accion === 'SANCION' || accion === 'RECHAZO' || accion === 'ANULACION') return 'error'
  if (accion.startsWith('CAMBIO_') || accion === 'ACTUALIZACION') return 'info'
  return 'neutro'
}

export function acortar(valor: string | undefined | null): string {
  if (!valor) return ''
  return valor.length > 12 ? `${valor.slice(0, 10)}…` : valor
}
