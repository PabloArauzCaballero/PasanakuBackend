/** Los textos del tablero, en voz de marca. */
export const textosTablero = {
  titulo: 'Tablero',
  proposito: 'Qué requiere tu acción hoy, según tu rol, y a qué sección podés entrar.',
  accion: {
    titulo: 'Requiere tu acción',
    explicacion: 'Miramos la primera fila de cada bandeja que tu rol puede atender. Decimos si hay algo pendiente; la cantidad la ves en la bandeja.',
    actualizar: 'Actualizar',
    sinBandejas: 'Tu rol no atiende ninguna bandeja con pendientes. Usá los accesos de abajo para entrar a tu sección.',
    cargando: 'Consultando la bandeja',
    pendientes: 'Hay pendientes',
    alDia: 'Al día',
    error: 'No pudimos consultarla',
    abrir: 'Abrir bandeja',
    consultadaEl: 'Consultada',
    traza: 'Código de seguimiento',
  },
  monitoreo: {
    titulo: 'Monitoreo',
    explicacion: 'Indicadores de la plataforma. Solo mostramos los que un servicio publica; el resto lo decimos.',
    noDisponible: 'Indicador no disponible',
    indicadores: [
      { id: 'integraciones', titulo: 'Salud de integraciones', motivo: 'Ningún servicio publica todavía una proyección del estado de los proveedores externos.' },
      { id: 'conciliacion', titulo: 'Conciliación del día', motivo: 'No hay proyección de la conciliación diaria: el resultado se ve en la bandeja de operación.' },
    ],
  },
  accesos: { titulo: 'Accesos' },
}
