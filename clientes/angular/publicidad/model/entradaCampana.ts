/**
 * DOBLE TEMPORAL (regla 65) — ver `clientes/angular/identidad.ts`. Tipo permisivo
 * (`Record<string, unknown>`) a propósito: no inventa la forma real del contrato.
 */
export type EntradaCampana = Record<string, unknown>
export const EntradaCampanaMonedaEnum = { Bob: 'Bob' } as const
export const EntradaCampanaObjetivoEnum = { VisibilidadMarca: 'VisibilidadMarca' } as const
