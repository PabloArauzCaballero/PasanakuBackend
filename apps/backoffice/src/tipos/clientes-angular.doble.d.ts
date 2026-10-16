/* eslint-disable @typescript-eslint/no-explicit-any -- doble ambiental: `any` es el punto,
   no un descuido (ver el comentario de abajo). Se borra junto con este archivo. */
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
 *
 * Los módulos que el código real importa como VALOR en runtime (enums usados con
 * `.Miembro`, no solo `import type`) NO están acá: un `.d.ts` ambiental solo satisface al
 * type-checker, y el bundler de Vite necesita un archivo real para ejecutar los tests
 * ("Failed to resolve import" en runtime, aunque TypeScript ya compile). Esos viven como
 * archivos `.ts` reales en `clientes/angular/identidad.ts` y
 * `clientes/angular/publicidad/model/{entradaAnunciante,entradaCampana,salidaCampana,
 * entradaRevision}.ts` — mismo doble, mismo motivo, forma distinta.
 */

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

declare module 'clientes/angular/publicidad/model/salidaAnunciante' {
  export type SalidaAnunciante = any
}

declare module 'clientes/angular/publicidad/model/entradaRechazo' {
  export type EntradaRechazo = any
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
