import type { Factor } from '@aportaya/ui/panel-de-factores/panel-de-factores'

/**
 * CU-97 · Anticipar el riesgo con alertas tempranas.
 *
 * Regla del carril (skill `alertas-riesgo-temprano`): una alerta es una razón para
 * ACOMPAÑAR, nunca una condena. Por eso el modelo del dominio separa dos listas que
 * nunca se mezclan en un solo texto: `hechos` (algo que ya ocurrió y quedó registrado
 * — una mora, un aporte tardío, un reclamo) y `estimaciones` (lo que el modelo
 * calculó, con su explicación, nunca en términos de probabilidad de la persona).
 *
 * **Contrato pendiente:** `cumplimiento.yaml` no declara una ruta para consultar
 * alertas tempranas ni el detalle de una alerta (CU-97 no tiene operación HTTP en el
 * contrato real, a diferencia de CU-46/54/55 que sí la tienen). Se declara como hueco
 * — ver `planes/informes/carril-B2.md` — y esta pantalla trabaja sobre el tipo de
 * dominio propio, listo para conectarse el día que el contrato exista.
 */
export type AlertaDeRiesgo = {
  id: string
  grupoId: string
  hechos: { nombre: string; explicacion: string }[]
  estimaciones: { nombre: string; explicacion: string }[]
  generadaEn: string
}

const PALABRAS_PROHIBIDAS = /probable|probablemente|seguro que|va a incumplir|incumplirá|condena/i

/** Guarda de redacción: ningún texto de una alerta puede sonar a condena. */
export function sinLenguajeDeCondena(alerta: AlertaDeRiesgo): boolean {
  const textos = [...alerta.hechos, ...alerta.estimaciones].map((f) => f.explicacion)
  return textos.every((t) => !PALABRAS_PROHIBIDAS.test(t))
}

export function factoresDeHechos(alerta: AlertaDeRiesgo): Factor[] {
  return alerta.hechos.map((h) => ({ nombre: h.nombre, peso: 'enContra', explicacion: h.explicacion }))
}

export function factoresDeEstimaciones(alerta: AlertaDeRiesgo): Factor[] {
  return alerta.estimaciones.map((h) => ({ nombre: h.nombre, peso: 'neutro', explicacion: h.explicacion }))
}
