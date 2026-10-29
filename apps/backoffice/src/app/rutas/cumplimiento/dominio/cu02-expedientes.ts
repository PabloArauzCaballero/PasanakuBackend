import { HttpClient, httpResource } from '@angular/common/http'
import { computed, inject, Signal } from '@angular/core'
import type { Observable } from 'rxjs'
import {
  ExpedienteEnRevisionEstadoEnum,
  type DecisionDeVerificacion,
  type EnlaceDeFoto,
  type ExpedienteEnRevision,
} from 'clientes/angular/identidad'
import { GATEWAY } from '../../../nucleo/gateway'
import { contextoIdempotente } from '../../../nucleo/idempotencia.interceptor'

/**
 * CU-02 · el portal de riesgo: la cola de expedientes de identidad, sus fotos y la
 * decisión manual. Contrato real de `identidad`: `listarVerificaciones`,
 * `verFotoDelExpediente` y `resolverVerificacion`.
 *
 * El HTTP vive acá y nunca en la pantalla (`scripts/verificar_frontend.py`: «sin red
 * en vista»), y los tipos son los GENERADOS del contrato (invariante 2).
 */
export function expedientesEnEstado(estado: Signal<string>) {
  const gateway = inject(GATEWAY)
  return httpResource<ExpedienteEnRevision[]>(() => {
    const e = estado()
    // `POR_DECIDIR` no es un estado del contrato: es la cola de trabajo. Se pide sin
    // filtro y se recorta acá, porque el backend filtra por UN estado y los que
    // esperan una persona son DOS (ver `POR_DECIDIR` abajo).
    return e === POR_DECIDIR
      ? `${gateway}/identidad/verificaciones`
      : `${gateway}/identidad/verificaciones?estado=${e}`
  })
}

/**
 * Un expediente por su `verificacionId`, para la ficha. El contrato no publica «consultar un expediente»: se pide la
 * cola **sin filtro de estado** (trae los cuatro) y se elige el pedido. `undefined` = no está en la cola: o el id no
 * existe, o no es visible para esta sesión — la ficha lo dice así, sin adivinar cuál de los dos.
 */
export function expedienteDe(verificacionId: Signal<string>) {
  const gateway = inject(GATEWAY)
  const recurso = httpResource<ExpedienteEnRevision[]>(() => (verificacionId() ? `${gateway}/identidad/verificaciones` : undefined))
  const expediente = computed(() => (recurso.hasValue() ? (recurso.value() ?? []).find((e) => e.verificacionId === verificacionId()) : undefined))
  return { recurso, expediente }
}

/**
 * La cola de trabajo: todo lo que espera una decisión humana.
 *
 * Son dos estados, no uno. El alta abre el expediente en `EN_REVISION`
 * (`RegistroRepositorio.iniciarVerificacion`) y el modelo además admite `PENDIENTE`.
 * Una pantalla que arrancaba filtrando `PENDIENTE` mostraba «no hay expedientes
 * esperando» con la cola llena — el peor mensaje posible: dice que no hay trabajo
 * cuando hay gente esperando su cuenta.
 */
export const POR_DECIDIR = 'POR_DECIDIR'

const ESPERAN_A_UNA_PERSONA: readonly string[] = [
  ExpedienteEnRevisionEstadoEnum.Pendiente,
  ExpedienteEnRevisionEstadoEnum.EnRevision,
]

/** ¿Este expediente espera que alguien lo mire? */
export const esperaDecision = (e: ExpedienteEnRevision): boolean => ESPERAN_A_UNA_PERSONA.includes(e.estado)

/** Lo que se muestra con el filtro elegido. `POR_DECIDIR` recorta; el resto ya viene filtrado. */
export function recortar(cola: readonly ExpedienteEnRevision[], estado: string): ExpedienteEnRevision[] {
  return estado === POR_DECIDIR ? cola.filter(esperaDecision) : [...cola]
}

/** Lo que el servidor dijo del vencimiento del documento. */
export type Vencimiento = 'vigente' | 'vencido' | 'sin-fecha'

/**
 * Lo decide el servidor (`documentoVigente`, con su reloj en hora de Bolivia), no el
 * reloj de quien mira: un operador con la hora corrida no puede «revivir» un carnet.
 * Sin fecha no hay forma de comprobarlo, y el servidor tampoco deja aprobar (AP-CU01-10).
 */
export function vencimientoDe(e: ExpedienteEnRevision): Vencimiento {
  if (!e.fechaExpiracionDocumento) return 'sin-fecha'
  return e.documentoVigente ? 'vigente' : 'vencido'
}

/** Un expediente ya resuelto no se vuelve a decidir desde el portal. */
export function esPendiente(e: ExpedienteEnRevision): boolean {
  return e.estado === ExpedienteEnRevisionEstadoEnum.Pendiente || e.estado === ExpedienteEnRevisionEstadoEnum.EnRevision
}

/** El tono del chip de estado: el color acompaña al texto del estado, nunca lo reemplaza. */
export function tonoDelExpediente(e: ExpedienteEnRevision): 'ok' | 'error' | 'aviso' | 'neutro' {
  if (e.estado === ExpedienteEnRevisionEstadoEnum.Aprobada) return 'ok'
  if (e.estado === ExpedienteEnRevisionEstadoEnum.Rechazada) return 'error'
  return e.estado === ExpedienteEnRevisionEstadoEnum.EnRevision ? 'aviso' : 'neutro'
}

/** Una cola vacía es una cola vacía: no hay nada que revisar, y eso es una buena noticia. */
export const colaVacia = (cola: ExpedienteEnRevision[]): boolean => cola.length === 0

/** La búsqueda del operador cubre el nombre, teléfono y correo del titular. */
export function coincideConBusqueda(e: ExpedienteEnRevision, busqueda: string): boolean {
  const normalizar = (valor: string) =>
    valor
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLocaleLowerCase('es')
      .trim()
  const termino = normalizar(busqueda)
  return (
    !termino ||
    normalizar(`${e.nombreCompleto} ${e.telefono ?? ''} ${e.correo ?? ''}`).includes(termino)
  )
}

/**
 * La carpeta del expediente en el servidor de archivos: `identidad/<usuarioId>/`.
 *
 * Se arma acá y no se pide al backend porque es una convención del almacén
 * (`DestinoDeObjeto.deExpediente`), no un dato de la fila. Sirve para ir a buscar el
 * expediente entero cuando hay que entregarlo o auditarlo.
 */
export const carpetaDelExpediente = (usuarioId: string): string => `identidad/${usuarioId}/`

/**
 * El enlace de una foto. Se pide de a una y **al mirarla**, no al listar: cada foto
 * de una cédula es un dato personal sensible, y cada lectura queda registrada. El
 * enlace vive diez minutos.
 */
export function crearPedirFoto(): (verificacionId: string, cara: string) => Observable<EnlaceDeFoto> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (verificacionId: string, cara: string) =>
    http.get<EnlaceDeFoto>(`${gateway}/identidad/verificaciones/${verificacionId}/fotos/${cara}`)
}

/**
 * Los **bytes** de una foto, no un enlace.
 *
 * `verFotoDelExpediente` devuelve una URL prefirmada contra el almacén, y el almacén
 * no es público: vive en la red interna, sin puerto publicado ni dominio (ADR-034).
 * Esa URL sirve entre servicios y es inservible acá — el navegador no resuelve
 * `minio:9000` y la imagen queda rota.
 *
 * Se pide como `blob` por `HttpClient` para que el interceptor le ponga el token: un
 * `<img src>` no manda cabeceras, y este binario exige permiso.
 */
export function crearPedirContenido(): (verificacionId: string, cara: string) => Observable<Blob> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (verificacionId: string, cara: string) =>
    http.get(`${gateway}/identidad/verificaciones/${verificacionId}/fotos/${cara}/contenido`, {
      responseType: 'blob',
    })
}

/**
 * La decisión, a mano. Clave de idempotencia nueva por intento (invariante 7): una
 * decisión que se reenvía por un doble clic no puede resolverse dos veces.
 */
export function crearResolver(): (verificacionId: string, cuerpo: DecisionDeVerificacion) => Observable<ExpedienteEnRevision> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (verificacionId: string, cuerpo: DecisionDeVerificacion) => {
    const contexto = contextoIdempotente()
    return http.post<ExpedienteEnRevision>(`${gateway}/identidad/verificaciones/${verificacionId}/decision`, cuerpo, {
      context: contexto,
    })
  }
}
