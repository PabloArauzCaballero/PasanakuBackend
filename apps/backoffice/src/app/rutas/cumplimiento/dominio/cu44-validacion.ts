import { EntradaDecisionCasoLftDecisionEnum, type EntradaCasoLft, type EntradaDecisionCasoLft } from 'clientes/angular/cumplimiento'
import { esUuid, type Errores } from './validacion'

/** Límites de `EntradaCasoLft` y `EntradaDecisionCasoLft` (cumplimiento.yaml). */
export const MAX_ALERTAS_POR_CASO = 50
export const MAX_RESUMEN = 500
export const MIN_CONCLUSION = 20
export const MAX_CONCLUSION = 2000

export type Decision = EntradaDecisionCasoLftDecisionEnum
export const DecisionDeCaso = EntradaDecisionCasoLftDecisionEnum
export const DECISIONES: readonly Decision[] = Object.values(EntradaDecisionCasoLftDecisionEnum)

export type CampoDeApertura = 'alertas' | 'analistaId' | 'resumen'
export type CampoDeDecision = 'decision' | 'conclusion' | 'reporteSospechosoId'

export function validarApertura(alertaIds: readonly string[], analistaId: string, resumen: string): Errores<CampoDeApertura> {
  const errores: Errores<CampoDeApertura> = {}
  if (alertaIds.length < 1) errores.alertas = 'Elegí al menos una alerta.'
  else if (alertaIds.length > MAX_ALERTAS_POR_CASO) errores.alertas = `Un caso agrupa hasta ${MAX_ALERTAS_POR_CASO} alertas.`
  if (!esUuid(analistaId)) errores.analistaId = 'Pegá el identificador del analista (formato UUID).'
  const r = resumen.trim()
  if (!r) errores.resumen = 'Escribí el resumen del caso.'
  else if (r.length > MAX_RESUMEN) errores.resumen = `El resumen admite hasta ${MAX_RESUMEN} caracteres.`
  return errores
}

export const cuerpoDeApertura = (alertaIds: readonly string[], analistaId: string, resumen: string): EntradaCasoLft => ({
  alertaIds: [...alertaIds],
  analistaId: analistaId.trim(),
  resumen: resumen.trim(),
})

export function validarDecision(decision: Decision | null, conclusion: string, reporteSospechosoId: string): Errores<CampoDeDecision> {
  const errores: Errores<CampoDeDecision> = {}
  if (!decision || !DECISIONES.includes(decision)) errores.decision = 'Elegí la decisión.'
  const c = conclusion.trim()
  if (c.length < MIN_CONCLUSION) errores.conclusion = `La conclusión necesita al menos ${MIN_CONCLUSION} caracteres: es el fundamento de la decisión.`
  else if (c.length > MAX_CONCLUSION) errores.conclusion = `La conclusión admite hasta ${MAX_CONCLUSION} caracteres.`
  if (decision === DecisionDeCaso.Reportar && !esUuid(reporteSospechosoId)) errores.reporteSospechosoId = 'Para reportar, pegá el identificador del ROS ya radicado (formato UUID).'
  return errores
}

/** El revisor NO viaja: es la sesión (cumplimiento.yaml). El ROS solo acompaña a REPORTAR. */
export function cuerpoDeDecision(decision: Decision, conclusion: string, reporteSospechosoId: string): EntradaDecisionCasoLft {
  const cuerpo: EntradaDecisionCasoLft = { decision, conclusion: conclusion.trim() }
  if (decision === DecisionDeCaso.Reportar) cuerpo.reporteSospechosoId = reporteSospechosoId.trim()
  return cuerpo
}

/** Rechazos del servidor que tienen un campo propio: se muestran ahí, no como error genérico. */
export const CAMPO_DE_CODIGO_CU44: Readonly<Record<string, CampoDeDecision | CampoDeApertura>> = {
  'AP-CU44-01': 'conclusion',
  'AP-CU44-03': 'reporteSospechosoId',
  'AP-CU44-08': 'alertas',
}
