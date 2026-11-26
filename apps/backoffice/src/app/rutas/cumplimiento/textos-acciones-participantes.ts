/** CU-134 · textos de los actos sobre un participante y sus medidas. */
export const textosAcciones = {
  revelar: {
    telefono: 'Revelar teléfono',
    correo: 'Revelar correo',
    titulo: (campo: string) => `Revelar el ${campo} completo`,
    confirmar: 'Revelar',
    efecto:
      'Verás el dato completo una sola vez en esta pantalla. La lectura queda registrada con tu justificación.',
    motivo: 'Por qué necesitás verlo',
    revelado: (campo: string) => `${campo} completo`,
  },
  decidir: {
    confirmar: 'Confirmar medida',
    desestimar: 'Desestimar medida',
    tituloConfirmar: 'Confirmar la medida',
    tituloDesestimar: 'Desestimar la medida',
    efectoConfirmar:
      'La medida queda decidida y corre el plazo de apelación. Si nadie apela, queda firme.',
    efectoDesestimar: 'La medida se cierra y la cuenta vuelve a su estado anterior.',
    motivo: 'Fundamento de la decisión',
  },
  resolver: {
    aceptar: 'Aceptar apelación',
    rechazar: 'Rechazar apelación',
    tituloAceptar: 'Aceptar la apelación',
    tituloRechazar: 'Rechazar la apelación',
    efectoAceptar: 'La medida se revoca y la cuenta vuelve a su estado anterior.',
    efectoRechazar: 'La medida queda firme.',
    motivo: 'Resolución',
  },
  liberar: {
    boton: 'Liberar entrega',
    titulo: 'Liberar la entrega retenida',
    efecto:
      'La entrega deja de estar retenida y el desembolso sigue su curso normal. No se aplica a ninguna deuda.',
    motivo: 'Fundamento de la liberación',
  },
  proponer: {
    boton: 'Proponer medida',
    titulo: 'Proponer una medida sobre la cuenta',
    confirmar: 'Proponer',
    causal: 'Causal',
    medida: 'Medida',
    cautelar: 'Aplicar ya, antes del descargo (solo cuenta comprometida o fraude con evidencia)',
    motivo: 'Motivo',
    ayudaMotivo: 'Entre 30 y 500 caracteres. La persona lo verá en su aviso.',
    evidencia: 'Evidencia',
    ayudaEvidencia: 'Describí la evidencia principal (ticket, registro o hallazgo).',
    errorMotivo: 'Escribí el motivo, con al menos 30 caracteres.',
    errorEvidencia: 'Describí al menos una evidencia.',
  },
} as const;
