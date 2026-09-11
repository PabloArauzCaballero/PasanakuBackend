import type { Etapa } from '@aportaya/ui/escalera-de-etapas/escalera-de-etapas'
import type { OpcionDeRadio } from '@aportaya/ui/grupo-radio/grupo-radio'

/**
 * CU-44 · De alerta de monitoreo a reporte de operación sospechosa (ROS), y el mismo
 * patrón para cualquier caso que termine en una decisión que perjudica a alguien
 * (skill `debido-proceso`): causal → notificación → plazo → descargo → decisión
 * motivada → apelación.
 *
 * **Contrato pendiente:** `cumplimiento.yaml` documenta los códigos de error de CU-44
 * (AP-CU44-01..04) pero no declara una ruta HTTP para el caso ni el ROS — solo
 * quedaron especificados los flujos de diligencia/PEP/incidente/riesgo operativo. Se
 * declara como hueco (ver informe del carril) y el dominio queda listo para
 * conectarse: la forma de `CasoDeCumplimiento` sigue el esquema de la bóveda
 * (`caso_cumplimiento`, `alerta_monitoreo`, `reporte_operacion_sospechosa`).
 */
export type CasoDeCumplimiento = {
  id: string
  causal: string | null
  notificadoEn: string | null
  plazoVenceEn: string | null
  descargo: string | null
  decisionMotivada: string | null
  apelacionResueltaPor: string | null
}

/** El catálogo cerrado de causales de CU-44. Nunca se escribe una causal libre. */
export const CATALOGO_DE_CAUSALES: OpcionDeRadio[] = [
  { valor: 'PATRON_FRACCIONAMIENTO', texto: 'Fraccionamiento de operaciones', detalle: 'Varias operaciones bajo el umbral en el mismo período' },
  { valor: 'CONTRAPARTE_EN_LISTA', texto: 'Contraparte en lista restrictiva', detalle: 'La contraparte aparece en una lista de sanciones' },
  { valor: 'INCOHERENCIA_CON_PERFIL', texto: 'Incoherencia con el perfil declarado', detalle: 'El monto o la frecuencia no calzan con la actividad declarada' },
  { valor: 'ORIGEN_NO_JUSTIFICADO', texto: 'Origen de fondos no justificado', detalle: 'No se pudo acreditar el origen lícito de los fondos' },
]

export function etapasDelCaso(caso: CasoDeCumplimiento): { etapas: Etapa[]; actual: number } {
  const etapas: Etapa[] = [
    { nombre: 'Causal', descripcion: 'La regla invocada, anterior al hecho', desdeIso: caso.causal ? caso.notificadoEn ?? undefined : undefined },
    { nombre: 'Notificación', descripcion: 'Probada con el acuse del canal, no con "se mandó el mensaje"', desdeIso: caso.notificadoEn ?? undefined },
    { nombre: 'Plazo', descripcion: 'Calculado al notificar y guardado, nunca recalculado', desdeIso: caso.plazoVenceEn ?? undefined },
    { nombre: 'Descargo', descripcion: 'Con evidencia de ambas partes, inmutable', desdeIso: caso.descargo ? caso.plazoVenceEn ?? undefined : undefined },
    { nombre: 'Decisión motivada', descripcion: 'Con el fundamento escrito', desdeIso: caso.decisionMotivada ?? undefined },
    { nombre: 'Apelación', descripcion: 'Única, resuelta por otro', desdeIso: caso.apelacionResueltaPor ?? undefined },
  ]
  const actual = [caso.causal, caso.notificadoEn, caso.plazoVenceEn, caso.descargo, caso.decisionMotivada, caso.apelacionResueltaPor].filter(Boolean).length
  return { etapas, actual: Math.min(actual, etapas.length - 1) }
}

/** La confirmación exige una causal del catálogo. Sin ella, nunca se habilita. */
export function puedeConfirmar(causalElegida: string | null): boolean {
  return CATALOGO_DE_CAUSALES.some((c) => c.valor === causalElegida)
}
