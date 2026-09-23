// Extensión `.ts` explícita (regla 65): el optimizador de dependencias de Vite
// (Rolldown) que usa el dev-server de Angular resuelve este subpath del paquete con
// reglas ESM estrictas de Node — que exigen extensión en un import relativo — a
// diferencia del resto del árbol de `apps/backoffice`, que esbuild resuelve sin
// extensión sin problema. Solo hace falta acá: es el único subpath de `@aportaya/
// simulado` que un import dinámico del navegador llega a tocar (`proveedor-fuentes.
// demo.ts`); el resto del paquete lo ejecutan los scripts de Node vía `tsx`, que no
// tiene este requisito.
export { adaptadorSimuladoSistemas } from './adaptador.ts'
export {
  accesosSimulados,
  descartadosSimulados,
  desplieguesSimulados,
  incidentesSimulados,
  interruptoresSimulados,
  migracionesSimuladas,
  outboxSimulado,
  proveedoresSimulados,
  respaldosSimulados,
  serviciosSimulados,
  webhooksSimulados,
} from './datos.ts'
export type {
  AccesoSimulado,
  DespliegueSimulado,
  EstadoServicioSimulado,
  IncidenteSimulado,
  InterruptorSimulado,
  MensajeDescartadoSimulado,
  MensajeOutboxSimulado,
  MigracionSimulada,
  ProveedorSimulado,
  RespaldoSimulado,
  WebhookSimulado,
} from './tipos'
