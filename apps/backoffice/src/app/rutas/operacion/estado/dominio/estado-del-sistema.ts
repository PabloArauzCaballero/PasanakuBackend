export type Nivel = 'N1' | 'N2' | 'N3'
export type ServicioDeArquitectura = { servicio: string; nivel: Nivel; replicas: number; porQue: string; salud: 'arriba' | 'degradado' | 'caido' }

/**
 * La misma tabla de `docs/Views/AportaYa-Maqueta.html` (pantalla «Arquitectura y estado
 * del proyecto», ruta `operacion/estado`, línea ~6394): catorce servicios con su nivel
 * de criticidad (ADR-037 — «el nivel no lo elige el dueño del servicio, lo impone su
 * peor dependiente») y sus réplicas mínimas.
 *
 * **Supuesto declarado.** Esto es contenido de arquitectura documentado (`planes/`,
 * ADR-037), no un dato operativo: por eso es una constante y no un `httpResource`. La
 * maqueta lo confirma («Código de los 14 servicios: 0 de 14» — no hay contrato
 * `/auditoria/estado` que consultar todavía; ese servicio no tiene código de aplicación
 * en `dev`). Cuando `auditoria` publique ese endpoint, este archivo pasa a `httpResource`
 * igual que `cu13-consultar-saldo.ts` — es un cambio de una función, no de la pantalla.
 */
export const SERVICIOS: readonly ServicioDeArquitectura[] = [
  { servicio: 'identidad', nivel: 'N1', replicas: 4, porQue: 'Autenticar y autorizar: sin esto nadie entra', salud: 'arriba' },
  { servicio: 'nucleo-financiero', nivel: 'N1', replicas: 4, porQue: 'El libro contable y la billetera: sin esto no se mueve plata', salud: 'arriba' },
  { servicio: 'aportes', nivel: 'N1', replicas: 4, porQue: 'Cobrar el aporte, el flujo de todos los períodos', salud: 'arriba' },
  { servicio: 'tarifas', nivel: 'N1', replicas: 4, porQue: '«¿Cuánto es la comisión?» está en el camino del cobro', salud: 'arriba' },
  { servicio: 'grupos', nivel: 'N2', replicas: 3, porQue: 'Gobernanza: se atrasa un acuerdo, no se pierde plata', salud: 'arriba' },
  { servicio: 'entregas', nivel: 'N2', replicas: 3, porQue: 'La entrega tiene fecha, no instante: tolera minutos', salud: 'arriba' },
  { servicio: 'garantia', nivel: 'N2', replicas: 3, porQue: 'La cobertura se aplica en el barrido, no en línea', salud: 'arriba' },
  { servicio: 'notificaciones', nivel: 'N2', replicas: 3, porQue: 'El outbox retiene: la caída se vuelve atraso, no aviso perdido', salud: 'arriba' },
  { servicio: 'organizador', nivel: 'N2', replicas: 3, porQue: 'Habilitación y automatización: fuera del camino crítico', salud: 'arriba' },
  { servicio: 'cumplimiento', nivel: 'N3', replicas: 2, porQue: 'Los límites se leen del catálogo; monitorear llega por evento', salud: 'arriba' },
  { servicio: 'auditoria', nivel: 'N3', replicas: 2, porQue: 'Consultas y exportes: se difieren sin consecuencia', salud: 'arriba' },
  { servicio: 'transparencia', nivel: 'N3', replicas: 2, porQue: 'Reputación y certificados: nadie afuera nota diez minutos', salud: 'arriba' },
  { servicio: 'erp', nivel: 'N3', replicas: 2, porQue: 'Contabilidad de gestión: trabaja sobre períodos cerrados', salud: 'arriba' },
  { servicio: 'publicidad', nivel: 'N3', replicas: 2, porQue: 'Lo primero que se apaga en degradación controlada', salud: 'arriba' },
] as const

export const NIVELES: readonly { valor: Nivel; texto: string }[] = [
  { valor: 'N1', texto: 'N1 · crítico' },
  { valor: 'N2', texto: 'N2 · esencial' },
  { valor: 'N3', texto: 'N3 · diferible' },
]
