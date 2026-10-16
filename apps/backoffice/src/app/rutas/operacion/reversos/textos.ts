/** Los textos del listado de reversos (CU-14), en voz de marca. */
export const textosReversos = {
  titulo: 'Reversos',
  proposito: 'Consultar cada reverso de una transacción: quién lo autorizó, por qué y en qué quedó. Desde acá no se reversa nada.',
  vacio: 'No hay reversos en estos estados.',
  explicacionVacio: 'Sin filtro se muestran todos los reversos.',
  denegado: 'Los reversos los consulta quien tiene el permiso de auditoría.',
  filtroEtiqueta: 'Estado',
  pendiente: 'Sin ejecutar',
  estados: { SOLICITADO: 'Solicitado', AUTORIZADO: 'Autorizado', EJECUTADO: 'Ejecutado', RECHAZADO: 'Rechazado' },
  tipos: { ANULACION: 'Anulación', CONTRACARGO: 'Contracargo', ERROR_OPERATIVO: 'Error operativo', ORDEN_AUTORIDAD: 'Orden de autoridad' },
  columnas: { solicitadaEn: 'Solicitado el', tipo: 'Tipo', estado: 'Estado', montoReversado: 'Monto reversado', motivo: 'Motivo', autorizadaPor: 'Autorizado por', transaccionOriginalId: 'Transacción original', ejecutadaEn: 'Ejecutado el' },
} as const
