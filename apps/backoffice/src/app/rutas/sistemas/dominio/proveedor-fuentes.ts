import type { Provider } from '@angular/core'
import { proveedoresFuenteNoDisponible, type ModoFuentesDeSistemas } from './proveedor-fuentes.compartido'

export { ES_FUENTE_SIMULADA, detectarModoFuentesDeSistemas, type ModoFuentesDeSistemas } from './proveedor-fuentes.compartido'

/**
 * H2.S2.M1/M2 — la versión SEGURA de `provideFuentesDeSistemas`, la que se compila
 * **por omisión, incluida `production`**: entrega la fuente no disponible en los
 * nueve puertos, pase lo que pase en `modo`.
 *
 * Este archivo nunca importa el paquete de simulado — ni siquiera con import
 * dinámico — así el build de producción no puede incluir los mocks por
 * construcción: no depende de que un optimizador elimine código muerto, el código
 * que los importaría directamente no está en este archivo. (Ni siquiera se nombra el
 * paquete acá arriba: `sin-simulado-en-pantallas.spec.ts` verifica textualmente que
 * este archivo no contenga esa cadena.)
 *
 * La versión con datos de ejemplo (`proveedor-fuentes.demo.ts`, misma firma) reemplaza
 * a este archivo por `fileReplacements` — ver `angular.json` — solo en las
 * configuraciones `development` y `demo`. La configuración `production` (la que usa
 * `yarn workspace @aportaya/backoffice build`, ver `defaultConfiguration`) no tiene
 * ningún `fileReplacements` para este archivo: se queda con este, el seguro.
 */
export function provideFuentesDeSistemas(_modo: ModoFuentesDeSistemas): Provider[] {
  return proveedoresFuenteNoDisponible()
}
