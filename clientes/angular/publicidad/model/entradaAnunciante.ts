/**
 * DOBLE TEMPORAL (regla 65) — ver `clientes/angular/identidad.ts`. Tipo permisivo
 * (`Record<string, unknown>`) a propósito: enumerar cada campo exacto sería inventar la
 * forma real del contrato (regla 00), y esto solo necesita no bloquear la compilación de
 * un dominio ajeno a este carril.
 */
export type EntradaAnunciante = Record<string, unknown>
export const EntradaAnuncianteMonedaEnum = { Bob: 'Bob' } as const
export const EntradaAnuncianteTipoEnum = { SocioComercial: 'SocioComercial' } as const
