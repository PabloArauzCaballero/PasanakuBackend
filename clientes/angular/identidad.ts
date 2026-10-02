/**
 * DOBLE TEMPORAL (regla 65) — no es el cliente generado real.
 *
 * `clientes/angular/**` lo genera el carril de contratos de Pablo (OUT de este carril:
 * PR15-Contratos.Frontend) desde el OpenAPI de cada servicio. Al 2026-09-21 no existe en
 * `dev`. A diferencia de un `.d.ts` ambiental (que solo satisface al type-checker), los
 * símbolos que el código usa como VALOR en runtime (los `Enum` de acá abajo) necesitan un
 * archivo real: si no, el bundler de Vite falla al ejecutar los tests con "Failed to
 * resolve import", aunque TypeScript ya haya compilado.
 *
 * Los nombres de los miembros de cada enum salen de USO REAL ya presente en el código
 * (`grep -rhoE "EnumX\.[A-Za-z0-9_]+" src`), no se inventan — es lo mínimo que hace
 * falta para que ese código compile y corra. El valor de cada miembro es el propio
 * nombre del miembro: no se afirma cuál es el valor real que espera el backend.
 *
 * Se borra cuando el carril de contratos publique `clientes/angular/identidad` real.
 */

export type EntradaAutenticacion = { telefonoE164: string; credencial: string; huellaDispositivo: string; plataforma: string; factor?: { tipo: string; valor: string } }
export type SalidaAutenticacion = { tokenAcceso?: string; requiereFactorAdicional?: boolean }
export type ExpedienteEnRevision = {
  verificacionId: string
  usuarioId: string
  nombreCompleto: string
  documento: string
  estado: string
  iniciadaEn: string
  fotos: string[]
  // Campo concreto porque el código lo accede con notación de punto
  // (`noPropertyAccessFromIndexSignature` no lo permite si viniera solo del índice).
  motivoRechazo?: string
  // Permisivo para cualquier otro campo que el código real lea de acá sin enumerarlo.
  [campo: string]: unknown
}
export type DecisionDeVerificacion = { decision: string; motivo?: string }
export type EnlaceDeFoto = { cara: string; url: string }

// Estos dos SÍ son el contrato real, no un doble: verificados contra
// servicios/identidad/src/main/resources/openapi/identidad.yaml (`EntradaAutenticacion.plataforma`
// y `FactorPresentado.tipo`) — Prism los valida y rechaza con 422 cualquier otro valor.
export const EntradaAutenticacionPlataformaEnum = { Web: 'WEB', Android: 'ANDROID', Ios: 'IOS' } as const
export const FactorPresentadoTipoEnum = { Totp: 'TOTP', Otp: 'OTP', Biometria: 'BIOMETRIA' } as const
/**
 * Valores del contrato (`identidad.yaml`, `ExpedienteEnRevision.estado`), no los nombres
 * de miembro: con `'EnRevision'` la cola filtraba `?estado=EnRevision` (el servidor
 * devuelve `[]`) y «Por decidir» nunca coincidía con lo que llega (`EN_REVISION`).
 */
export const ExpedienteEnRevisionEstadoEnum = {
  Pendiente: 'PENDIENTE',
  EnRevision: 'EN_REVISION',
  Aprobada: 'APROBADA',
  Rechazada: 'RECHAZADA',
} as const
/**
 * Contrato real, no un doble: copiado de `components.schemas.CaraDelExpediente` en
 * identidad.yaml (5 caras desde el escáner de identidad, 2026-10-02) y de lo que emite
 * el generador para ese schema. Mismos nombres de miembro que el enum generado.
 */
export const CaraDelExpediente = {
  Anverso: 'ANVERSO',
  Reverso: 'REVERSO',
  Selfie: 'SELFIE',
  PerfilIzquierdo: 'PERFIL_IZQUIERDO',
  PerfilDerecho: 'PERFIL_DERECHO',
} as const
export type CaraDelExpediente = (typeof CaraDelExpediente)[keyof typeof CaraDelExpediente]
/** Nombre anterior del mismo enum, para el código que todavía lo importe. */
export const ExpedienteEnRevisionFotosEnum = CaraDelExpediente
/** CU del debido proceso de verificación: aprobar u observar (regla 91.4). */
export const DecisionDeVerificacionDecisionEnum = {
  Aprobar: 'APROBAR',
  Rechazar: 'RECHAZAR',
} as const

// El código real usa estos nombres también como TIPO (`Record<...Enum, T>`, parámetros
// de función) — `type` y `const` del mismo nombre no chocan (namespaces distintos).
// `string` es el supertipo más permisivo, no afirma los valores reales.
export type ExpedienteEnRevisionEstadoEnum = string
export type ExpedienteEnRevisionFotosEnum = string
export type DecisionDeVerificacionDecisionEnum = string
