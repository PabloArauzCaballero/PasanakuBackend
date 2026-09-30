/** DOBLE TEMPORAL (regla 65) — ver `clientes/angular/identidad.ts` para la explicación completa. */
export type EntradaRevision = { decision: string; motivo?: string }
export const EntradaRevisionDecisionEnum = {
  Aprobada: 'APROBADA',
  Rechazada: 'RECHAZADA',
} as const
