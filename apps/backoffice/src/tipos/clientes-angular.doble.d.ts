/**
 * DOBLE TEMPORAL (regla 65) — no es el contrato real.
 *
 * `clientes/angular/**` es el cliente tipado que genera el carril de contratos de Pablo
 * (OUT de este carril: PR15-Contratos.Frontend). Al 2026-09-21 ese paquete no existe en
 * el árbol de `dev` (ni el directorio `clientes/` está presente), así que TODO el
 * programa de TypeScript de `apps/backoffice` fallaba a compilar con TS2307 — incluidos
 * specs que no tocan sesión ni permisos (reproducido corriendo `permisos.spec.ts`, que ya
 * existía antes de este carril, contra el mismo error, con `clientes/` ausente en `dev`).
 * Es un hallazgo de baseline, no una regresión de este carril.
 *
 * Esta declaración ambiente NO inventa la forma real del cliente (regla 00, anti-invención):
 * cada símbolo se tipa `any`. Nombra exactamente los identificadores que el código hoy
 * importa (relevados con `grep` sobre `src/`, no supuestos), sin afirmar campos, valores
 * de enum ni formas reales — nada de esto se usa en el código que este carril toca ni
 * verifica (sesión, permisos, auditoría de acceso); solo existe para que el resto del
 * programa (publicidad, cumplimiento, contabilidad, ingreso — fuera de mi alcance) deje
 * de bloquear la compilación de TODO el workspace de tests.
 *
 * Se borra cuando el carril de contratos de Pablo publique `clientes/angular` real. Ver
 * `entregables/decision-doble-clientes-angular.md`.
 */

declare module 'clientes/angular/identidad' {
  export type EntradaAutenticacion = any
  export type SalidaAutenticacion = any
  export type ExpedienteEnRevision = any
  export type DecisionDeVerificacion = any
  export type EnlaceDeFoto = any
  export const EntradaAutenticacionPlataformaEnum: any
  export const FactorPresentadoTipoEnum: any
  export const ExpedienteEnRevisionEstadoEnum: any
  export const ExpedienteEnRevisionFotosEnum: any
}

declare module 'clientes/angular/organizador' {
  export type Habilitacion = any
}

declare module 'clientes/angular/nucleo-financiero' {
  export type SaldoBilletera = any
}

declare module 'clientes/angular/erp' {
  export type SalidaCierre = any
  export type SalidaEstadoFinanciero = any
}

declare module 'clientes/angular/publicidad/model/entradaAnunciante' {
  export type EntradaAnunciante = any
  export const EntradaAnuncianteMonedaEnum: any
  export const EntradaAnuncianteTipoEnum: any
}

declare module 'clientes/angular/publicidad/model/salidaAnunciante' {
  export type SalidaAnunciante = any
}

declare module 'clientes/angular/publicidad/model/entradaCampana' {
  export type EntradaCampana = any
  export const EntradaCampanaMonedaEnum: any
  export const EntradaCampanaObjetivoEnum: any
}

declare module 'clientes/angular/publicidad/model/salidaCampana' {
  export type SalidaCampana = any
  export const SalidaCampanaEstadoEnum: any
}

declare module 'clientes/angular/publicidad/model/entradaRechazo' {
  export type EntradaRechazo = any
}

declare module 'clientes/angular/publicidad/model/entradaRevision' {
  export type EntradaRevision = any
  export const EntradaRevisionDecisionEnum: any
}

declare module 'clientes/angular/publicidad/model/salidaRevision' {
  export type SalidaRevision = any
}

declare module 'clientes/angular/publicidad/model/entradaSocio' {
  export type EntradaSocio = any
}

declare module 'clientes/angular/publicidad/model/salidaSocio' {
  export type SalidaSocio = any
}

declare module 'clientes/angular/publicidad/model/salidaConsumo' {
  export type SalidaConsumo = any
}

declare module 'clientes/angular/publicidad/model/entradaLiquidacion' {
  export type EntradaLiquidacion = any
}

declare module 'clientes/angular/publicidad/model/salidaLiquidacion' {
  export type SalidaLiquidacion = any
}
