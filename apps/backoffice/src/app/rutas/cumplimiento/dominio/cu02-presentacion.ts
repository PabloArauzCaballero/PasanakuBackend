import { CaraDelExpediente, ExpedienteEnRevisionEstadoEnum, type ExpedienteEnRevision } from 'clientes/angular/identidad'
import { vencimientoDe } from './cu02-expedientes'

/**
 * CU-02 · cómo se **lee** un expediente de identidad. Lo que el contrato manda en códigos (`EN_REVISION`, `CI SC`,
 * `PERFIL_IZQUIERDO`) se dice en palabras, en la tabla, en la ficha y en el archivo descargado. Funciones puras: la
 * pantalla no traduce nada por su cuenta, y un código que el contrato agregue mañana se muestra tal cual en vez de
 * desaparecer.
 */

type Explicado = { texto: string; explicacion: string }

const ESTADOS: Record<string, Explicado> = {
  [ExpedienteEnRevisionEstadoEnum.Pendiente]: {
    texto: 'Pendiente',
    explicacion: 'El expediente existe pero todavía no entró a revisión. Espera a una persona igual que uno en revisión.',
  },
  [ExpedienteEnRevisionEstadoEnum.EnRevision]: {
    texto: 'En revisión',
    explicacion: 'La persona terminó su alta y espera que alguien de Cumplimiento mire su documento y su cara. Mientras tanto no puede operar.',
  },
  [ExpedienteEnRevisionEstadoEnum.Aprobada]: {
    texto: 'Aprobada',
    explicacion: 'Una persona de Cumplimiento comparó el documento con la cara y aprobó: la identidad quedó verificada y deja de esperar en la cola.',
  },
  [ExpedienteEnRevisionEstadoEnum.Rechazada]: {
    texto: 'Rechazada',
    explicacion: 'Una persona de Cumplimiento rechazó el expediente con un motivo escrito. El titular puede consultar ese motivo con su sesión.',
  },
}

export const estadoEnPalabras = (estado: string): string => ESTADOS[estado]?.texto ?? estado
export const explicacionDelEstado = (estado: string): string => ESTADOS[estado]?.explicacion ?? 'Estado que este backoffice todavía no conoce: se muestra tal como lo manda el servidor.'

/** Las opciones del filtro de estado, en el orden en que un expediente las recorre. */
export const OPCIONES_DE_ESTADO = Object.values(ExpedienteEnRevisionEstadoEnum).map((valor) => ({ valor, texto: estadoEnPalabras(valor) }))

const TIPOS: Record<string, string> = {
  CI: 'Cédula de identidad',
  CEX: 'Cédula de identidad de extranjero',
  CARNET_EXTRANJERO: 'Cédula de identidad de extranjero',
  PASAPORTE: 'Pasaporte',
}

/**
 * Los nueve departamentos con la sigla que lleva el carnet: el número se repite entre departamentos y la extensión
 * lo desambigua. Misma lista que la app (`apps/movil/lib/pantallas/identidad/pasos_alta/departamentos.dart`) y que el
 * contrato (`identidad.yaml`, `lugarExpedicion`).
 */
const DEPARTAMENTOS: Record<string, string> = {
  LP: 'La Paz',
  SC: 'Santa Cruz',
  CB: 'Cochabamba',
  OR: 'Oruro',
  PT: 'Potosí',
  CH: 'Chuquisaca',
  TJ: 'Tarija',
  BE: 'Beni',
  PD: 'Pando',
}

export type DocumentoLeido = { tipo: string | null; lugar: string | null; texto: string }

/**
 * `documento` lo arma el servidor como `<tipo> <lugar>` (`FilaDeExpediente.java`): «CI SC» es una cédula expedida en
 * Santa Cruz. **El número nunca viaja**, a propósito. Sin documento cargado, se dice; un código desconocido se
 * muestra tal cual en vez de inventarle un nombre.
 */
export function documentoEnPalabras(documento: string | undefined | null): DocumentoLeido {
  const crudo = documento?.trim()
  if (!crudo) return { tipo: null, lugar: null, texto: 'Sin documento cargado' }
  const [codigo = '', sigla] = crudo.split(/\s+/)
  const tipo = TIPOS[codigo] ?? codigo
  const lugar = sigla ? (DEPARTAMENTOS[sigla] ?? sigla) : null
  return { tipo, lugar, texto: lugar ? `${tipo} · ${lugar}` : tipo }
}

const CARAS: Record<CaraDelExpediente, Explicado> = {
  ANVERSO: { texto: 'Anverso del documento', explicacion: 'El frente del documento. Contra él se coteja la cara y la fecha de vencimiento declarada.' },
  REVERSO: { texto: 'Reverso del documento', explicacion: 'El dorso del documento. Completa lo que el frente no muestra.' },
  SELFIE: { texto: 'Selfie de frente', explicacion: 'La cara de frente, tomada en el momento del alta. Es la que se compara con la foto del carnet.' },
  PERFIL_IZQUIERDO: { texto: 'Perfil izquierdo', explicacion: 'La cara girada a la izquierda: prueba que hay una persona delante de la cámara y no una foto.' },
  PERFIL_DERECHO: { texto: 'Perfil derecho', explicacion: 'La cara girada a la derecha: completa la prueba de vida.' },
}

/** Las cinco caras en el orden del contrato: el documento primero, después la persona. */
export const CARAS_DEL_EXPEDIENTE = Object.values(CaraDelExpediente) as CaraDelExpediente[]
export const caraEnPalabras = (cara: CaraDelExpediente): string => CARAS[cara]?.texto ?? cara
export const explicacionDeLaCara = (cara: CaraDelExpediente): string => CARAS[cara]?.explicacion ?? ''

export const fotosFaltantes = (e: ExpedienteEnRevision): CaraDelExpediente[] => CARAS_DEL_EXPEDIENTE.filter((c) => !e.fotos.includes(c))
export const fotosCompletas = (e: ExpedienteEnRevision): boolean => fotosFaltantes(e).length === 0
/** «3 de 5»: cuántas caras hay, no solo si están todas. Un expediente con cuatro no es lo mismo que uno con ninguna. */
export const fotosEnNumeros = (e: ExpedienteEnRevision): string => `${CARAS_DEL_EXPEDIENTE.length - fotosFaltantes(e).length} de ${CARAS_DEL_EXPEDIENTE.length}`

const VENCIMIENTO: Record<string, string> = { vigente: 'Vigente', vencido: 'Vencido', 'sin-fecha': 'Sin fecha de vencimiento' }
export const vencimientoEnPalabras = (e: ExpedienteEnRevision): string => VENCIMIENTO[vencimientoDe(e)] ?? ''

/**
 * Lo que impide aprobar, en el mismo orden en que lo comprueba el servidor (AP-CU01-10/11). La pantalla apaga el botón
 * con esta lista y la muestra; si alguien la esquiva, el servidor rechaza igual.
 */
export function impedimentosParaAprobar(e: ExpedienteEnRevision): string[] {
  const motivos: string[] = []
  const faltan = fotosFaltantes(e)
  if (faltan.length > 0) motivos.push(`Faltan ${faltan.length === 1 ? 'una foto' : faltan.length + ' fotos'}: ${faltan.map(caraEnPalabras).join(', ')}.`)
  const v = vencimientoDe(e)
  if (v === 'vencido') motivos.push('El documento está vencido según el servidor.')
  if (v === 'sin-fecha') motivos.push('El documento no tiene fecha de vencimiento: no hay forma de comprobar que esté vigente.')
  return motivos
}

/**
 * Cuánto lleva el expediente en la cola: desde que se inició hasta que se resolvió, o hasta `ahora` si sigue abierto.
 * Es una referencia para ordenar el trabajo, no un dato del servidor; por eso recibe `ahora` y no lee el reloj.
 */
export function tiempoEnCola(e: ExpedienteEnRevision, ahora: Date): string {
  const inicio = Date.parse(e.iniciadaEn)
  const fin = e.resueltaEn ? Date.parse(e.resueltaEn) : ahora.getTime()
  if (Number.isNaN(inicio) || Number.isNaN(fin) || fin < inicio) return 'Sin dato'
  const minutos = Math.floor((fin - inicio) / 60_000)
  if (minutos < 60) return minutos <= 1 ? 'Menos de un minuto' : `${minutos} minutos`
  const horas = Math.floor(minutos / 60)
  if (horas < 48) return horas === 1 ? '1 hora' : `${horas} horas`
  return `${Math.floor(horas / 24)} días`
}
