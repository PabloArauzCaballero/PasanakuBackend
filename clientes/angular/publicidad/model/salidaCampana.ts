/**
 * DOBLE TEMPORAL (regla 65) — ver `clientes/angular/identidad.ts`. `campanaPublicitariaId`
 * y `estado` quedan tipados (se leen con comparaciones estrictas en el código real); el
 * resto es permisivo (índice de firma) para no inventar el resto del contrato, y de paso
 * deja que `ColumnaVirtual<Campana>` acepte cualquier clave de columna real del código
 * (`presupuestoTotal`, etc.) sin enumerarlas. Los valores de `estado` salen de `textos.ts`
 * de este mismo carril, ya usados como literales reales en `pantalla-de-campanas.ts`.
 */
export type SalidaCampana = {
  campanaPublicitariaId: string
  estado: 'BORRADOR' | 'EN_REVISION' | 'ACTIVA' | 'PAUSADA' | 'FINALIZADA' | 'RECHAZADA'
  [campo: string]: unknown
}
export const SalidaCampanaEstadoEnum = {
  Borrador: 'BORRADOR',
  EnRevision: 'EN_REVISION',
  Activa: 'ACTIVA',
  Pausada: 'PAUSADA',
  Finalizada: 'FINALIZADA',
  Rechazada: 'RECHAZADA',
} as const
