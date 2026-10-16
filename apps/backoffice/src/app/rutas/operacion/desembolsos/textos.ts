/** Los textos de la bandeja de desembolsos (CU-28), en voz de marca. */
export const textosDesembolsos = {
  titulo: 'Desembolsos',
  proposito: 'Mirar los desembolsos que necesitan a alguien: rechazados, devueltos por el banco o todavía en proceso. Sin filtro se ven solo esas excepciones.',
  vacio: 'No hay desembolsos en estos estados.',
  explicacionVacio: 'Sin filtro se muestran las excepciones: rechazados, devueltos por el banco y en proceso.',
  denegado: 'Los desembolsos los consulta quien ejecuta entregas o tiene el permiso de auditoría.',
  filtroEtiqueta: 'Estado',
  ninguno: '—',
  estados: { CREADA: 'Creada', ENVIADA_A_PROVEEDOR: 'Enviada al proveedor', EN_PROCESO: 'En proceso', ACREDITADA: 'Acreditada', RECHAZADA: 'Rechazada', DEVUELTA_POR_BANCO: 'Devuelta por el banco', CANCELADA: 'Cancelada' },
  resultados: { EXITOSO: 'Exitoso', FALLIDO: 'Fallido', PENDIENTE: 'Pendiente', TIMEOUT: 'Sin respuesta a tiempo' },
  columnas: { creadaEn: 'Creada el', monto: 'Monto', estado: 'Estado', intentos: 'Intentos', ultimoResultado: 'Último resultado', ultimoCodigoError: 'Código del proveedor', reintentableEn: 'Reintentable desde', entregaId: 'Entrega' },
} as const
